// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Biblioteca real (F3) no {@link BancoStarkTech}: os vídeos achados nas pastas, com a classificação
 * ({@link Classificador}) e os dados técnicos lidos pelo motor ({@link Varredura}); os dados do
 * TMDB de cada filme, show, episódio e série ({@link BuscaTmdb}); e o estado de cada um (favorito,
 * assistido, posição, nota). Chave: o caminho do arquivo; série: {@link #chaveSerie}; coletânea:
 * {@link #chaveColetanea}; artista: {@link #chaveArtista}, com a capa em {@link Artista}.
 */
public final class Acervo {

    /** Um arquivo de vídeo de uma pasta da biblioteca. */
    public static final class Video {
        public String caminho;
        public String pasta;
        public Classificador.Tipo tipo;
        public String titulo;
        public Integer ano;
        public String serie;
        public int temporada;
        public int episodio;
        public String artista;
        /** Show: a subpasta de Shows (D7); nulo = sem grupo. */
        public String grupo;
        public Classificador.Motivo motivo;
        public long tamanho;
        public long modificado;
        /** Quando o arquivo entrou na biblioteca (Adicionados recentemente). */
        public long adicionado;
        /** Versão da leitura dos dados técnicos pelo motor (0 = nunca tentado; {@link Varredura#TENTOU} = falhou; ver {@link Varredura#LEITURA}). */
        public int lido;
        public long duracaoMs;
        public int largura;
        public int altura;
        /** "H.265 · HDR10" (vazio se não deu para ler). */
        public String infoVideo;
        public List<String> audios = Collections.emptyList();
        public List<String> legendas = Collections.emptyList();
        public Capitulos capitulos = Capitulos.nenhum();
        /** Quadro do vídeo (JPG na pasta do app), para quem fica sem imagem do TMDB; "" = tentou e não deu. */
        public String miniatura;
    }

    /** Situação da busca no TMDB. */
    public static final int PENDENTE = 0;
    public static final int ACHADO = 1;
    public static final int NAO_ACHADO = 2;
    /** Sem rede ou erro do TMDB: tenta de novo na próxima atualização. */
    public static final int ERRO = 3;

    /** {@link Tmdb#colecaoId}: linha de antes da versão 3 do banco, a consultar uma vez. */
    public static final int COLECAO_NAO_CONSULTADA = -1;
    /** {@link Tmdb#colecaoId}: o filme não faz parte de nenhuma coleção. */
    public static final int SEM_COLECAO = 0;

    /** Dados do TMDB de um título (filme, show, série) ou de um episódio. */
    public static final class Tmdb {
        public String chave;
        public int situacao = PENDENTE;
        public long idTmdb;
        public boolean filme = true;
        public String titulo;
        public String tituloOriginal;
        public String ano;
        public String sinopse;
        public String generos;
        public int duracaoMin;
        /** Imagens baixadas (caminho do arquivo). */
        public String capa;
        public String fundo;
        /** Outras opções do TMDB (endereço), para o Editar. */
        public List<String> capas = Collections.emptyList();
        public List<String> fundos = Collections.emptyList();
        /** Elenco: {nome, personagem}. */
        public List<String[]> elenco = Collections.emptyList();
        public String nomeEpisodio;
        public String miniatura;
        /** true = escolhido em "Buscar no TMDB" (a atualização não troca). */
        public boolean escolhido;
        public long buscado;
        /** Coleção do TMDB (coletânea, D5): o id, {@link #SEM_COLECAO} ou {@link #COLECAO_NAO_CONSULTADA}. */
        public int colecaoId = COLECAO_NAO_CONSULTADA;
        public String colecaoNome;
        /** Capa e fundo da coleção, baixados (caminho do arquivo); nulo = sem imagem. */
        public String colecaoCapa;
        public String colecaoFundo;
    }

    /** Capa de um artista ({@link BuscaArtista}). */
    public static final class Artista {
        public String chave;
        public String nome;
        public int situacao = PENDENTE;
        /** De onde veio a foto: {@link BuscaArtista#FANART} ou {@link BuscaArtista#WIKIPEDIA}; até a 1.1.0, THEAUDIODB ou DEEZER. */
        public String fonte;
        /** Imagens baixadas (caminho do arquivo); nulo = bloco de cor. */
        public String foto;
        public String fundo;
        public String pais;
        public long buscado;
        /** Texto da Wikipédia ({@link WikipediaArtista}); nulo = sem artigo em português. */
        public String descricao;
        /** "Banda britânica de rock". */
        public String resumo;
    }

    /** Favorito, assistido, posição e nota de um título ou episódio. */
    public static final class Estado {
        public boolean favorito;
        public boolean assistido;
        public int posicao;
        /** Última vez que tocou (Continuar assistindo, em ordem). */
        public long visto;
        /** Quando virou favorito (Favoritos, em ordem). */
        public long favoritado;
        /** De 0 a 5; nula = sem nota, diferente de 0 (D6). */
        public Integer nota;
        /** Faixa de áudio e legenda escolhidas à mão ("a1", "l2", "l-1", "arquivo:Nome.srt"); nula = as preferências. */
        public String faixaAudio;
        public String faixaLegenda;

        public Estado copia() {
            Estado e = new Estado();
            e.favorito = favorito;
            e.assistido = assistido;
            e.posicao = posicao;
            e.visto = visto;
            e.favoritado = favoritado;
            e.nota = nota;
            e.faixaAudio = faixaAudio;
            e.faixaLegenda = faixaLegenda;
            return e;
        }
    }

    private static Acervo instancia;

    private final BancoStarkTech banco;

    public static synchronized Acervo de(Context contexto) {
        if (instancia == null) instancia = new Acervo(BancoStarkTech.de(contexto));
        return instancia;
    }

    Acervo(BancoStarkTech banco) {
        this.banco = banco;
    }

    /** Chave da série no banco (TMDB, estado, edição): pelo nome, sem acento e maiúscula. */
    public static String chaveSerie(String nome) {
        return "serie:" + Nomes.normalizar(nome);
    }

    public static boolean ehSerie(String chave) {
        return chave != null && chave.startsWith("serie:");
    }

    /** Chave da coletânea (estado, edição): pelo nome, sem acento e maiúscula. */
    public static String chaveColetanea(String nome) {
        return "coletanea:" + Nomes.normalizar(nome);
    }

    public static boolean ehColetanea(String chave) {
        return chave != null && chave.startsWith("coletanea:");
    }

    /** Chave do artista (capa, edição): pelo nome, sem acento e maiúscula; sem nome = "Sem artista". */
    public static String chaveArtista(String nome) {
        return "artista:" + Nomes.normalizar(nome);
    }

    public static boolean ehArtista(String chave) {
        return chave != null && chave.startsWith("artista:");
    }

    /** Nome em comum do vídeo (D13). */
    public static String chaveNome(Video v) {
        return Classificador.chaveNome(v.tipo, v.titulo, v.ano, v.serie, v.temporada, v.episodio, v.artista);
    }

    // ---- Vídeos ----

    public synchronized List<Video> videos() {
        List<Video> lista = new ArrayList<>();
        try (Cursor c = banco.getReadableDatabase().query(BancoStarkTech.VIDEO, null, null, null, null, null, null)) {
            while (c.moveToNext()) {
                Video v = lerVideo(c);
                if (v != null) lista.add(v);
            }
        }
        return lista;
    }

    /** Um vídeo pelo caminho; nulo se não está na biblioteca. */
    public synchronized Video video(String caminho) {
        try (Cursor c = banco.getReadableDatabase().query(BancoStarkTech.VIDEO, null, "caminho = ?",
                new String[]{caminho}, null, null, null)) {
            return c.moveToFirst() ? lerVideo(c) : null;
        }
    }

    /** Grava (troca) os vídeos e apaga os caminhos de {@code remover}, numa transação. */
    public synchronized void gravarVideos(Collection<Video> gravar, Collection<String> remover) {
        SQLiteDatabase escrita = banco.getWritableDatabase();
        escrita.beginTransaction();
        try {
            for (Video v : gravar) {
                escrita.insertWithOnConflict(BancoStarkTech.VIDEO, null, valores(v), SQLiteDatabase.CONFLICT_REPLACE);
            }
            for (String caminho : remover) {
                escrita.delete(BancoStarkTech.VIDEO, "caminho = ?", new String[]{caminho});
            }
            escrita.setTransactionSuccessful();
        } finally {
            escrita.endTransaction();
        }
    }

    private static Video lerVideo(Cursor c) {
        Video v = new Video();
        v.caminho = texto(c, "caminho");
        v.pasta = texto(c, "pasta");
        try {
            v.tipo = Classificador.Tipo.valueOf(texto(c, "tipo"));
            String motivo = texto(c, "motivo");
            v.motivo = motivo == null ? null : Classificador.Motivo.valueOf(motivo);
        } catch (IllegalArgumentException e) {
            return null; // valor de uma versão futura
        }
        v.titulo = texto(c, "titulo");
        int iAno = c.getColumnIndexOrThrow("ano");
        v.ano = c.isNull(iAno) ? null : c.getInt(iAno);
        v.serie = texto(c, "serie");
        v.temporada = inteiro(c, "temporada");
        v.episodio = inteiro(c, "episodio");
        v.artista = texto(c, "artista");
        v.grupo = texto(c, "grupo");
        v.tamanho = longo(c, "tamanho");
        v.modificado = longo(c, "modificado");
        v.adicionado = longo(c, "adicionado");
        v.lido = inteiro(c, "lido");
        v.duracaoMs = longo(c, "duracao_ms");
        v.largura = inteiro(c, "largura");
        v.altura = inteiro(c, "altura");
        v.infoVideo = texto(c, "info_video");
        v.audios = linhas(texto(c, "audios"));
        v.legendas = linhas(texto(c, "legendas"));
        v.capitulos = Capitulos.deTexto(texto(c, "capitulos"));
        v.miniatura = texto(c, "miniatura");
        return v;
    }

    private static ContentValues valores(Video v) {
        ContentValues l = new ContentValues();
        l.put("caminho", v.caminho);
        l.put("pasta", v.pasta);
        l.put("tipo", v.tipo.name());
        l.put("titulo", v.titulo);
        if (v.ano == null) l.putNull("ano");
        else l.put("ano", v.ano);
        l.put("serie", v.serie);
        l.put("temporada", v.temporada);
        l.put("episodio", v.episodio);
        l.put("artista", v.artista);
        l.put("grupo", v.grupo);
        l.put("motivo", v.motivo == null ? null : v.motivo.name());
        l.put("tamanho", v.tamanho);
        l.put("modificado", v.modificado);
        l.put("adicionado", v.adicionado);
        l.put("lido", v.lido);
        l.put("duracao_ms", v.duracaoMs);
        l.put("largura", v.largura);
        l.put("altura", v.altura);
        l.put("info_video", v.infoVideo);
        l.put("audios", juntar(v.audios));
        l.put("legendas", juntar(v.legendas));
        l.put("capitulos", v.capitulos.paraTexto());
        l.put("miniatura", v.miniatura);
        return l;
    }

    // ---- TMDB ----

    public synchronized Map<String, Tmdb> tmdb() {
        Map<String, Tmdb> mapa = new HashMap<>();
        try (Cursor c = banco.getReadableDatabase().query(BancoStarkTech.TMDB, null, null, null, null, null, null)) {
            while (c.moveToNext()) {
                Tmdb t = lerTmdb(c);
                mapa.put(t.chave, t);
            }
        }
        return mapa;
    }

    public synchronized void gravarTmdb(Tmdb t) {
        ContentValues l = new ContentValues();
        l.put("chave", t.chave);
        l.put("situacao", t.situacao);
        l.put("id_tmdb", t.idTmdb);
        l.put("filme", t.filme ? 1 : 0);
        l.put("titulo", t.titulo);
        l.put("titulo_original", t.tituloOriginal);
        l.put("ano", t.ano);
        l.put("sinopse", t.sinopse);
        l.put("generos", t.generos);
        l.put("duracao_min", t.duracaoMin);
        l.put("capa", t.capa);
        l.put("fundo", t.fundo);
        l.put("capas", juntar(t.capas));
        l.put("fundos", juntar(t.fundos));
        List<String> elenco = new ArrayList<>();
        for (String[] pessoa : t.elenco) elenco.add(limpo(pessoa[0]) + "\t" + limpo(pessoa.length > 1 ? pessoa[1] : ""));
        l.put("elenco", juntar(elenco));
        l.put("nome_episodio", t.nomeEpisodio);
        l.put("miniatura", t.miniatura);
        l.put("escolhido", t.escolhido ? 1 : 0);
        l.put("buscado", t.buscado);
        l.put("colecao_id", t.colecaoId);
        l.put("colecao_nome", t.colecaoNome);
        l.put("colecao_capa", t.colecaoCapa);
        l.put("colecao_fundo", t.colecaoFundo);
        banco.getWritableDatabase().insertWithOnConflict(BancoStarkTech.TMDB, null, l, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public synchronized void apagarTmdb(String chave) {
        banco.getWritableDatabase().delete(BancoStarkTech.TMDB, "chave = ?", new String[]{chave});
    }

    private static Tmdb lerTmdb(Cursor c) {
        Tmdb t = new Tmdb();
        t.chave = texto(c, "chave");
        t.situacao = inteiro(c, "situacao");
        t.idTmdb = longo(c, "id_tmdb");
        t.filme = inteiro(c, "filme") == 1;
        t.titulo = texto(c, "titulo");
        t.tituloOriginal = texto(c, "titulo_original");
        t.ano = texto(c, "ano");
        t.sinopse = texto(c, "sinopse");
        t.generos = texto(c, "generos");
        t.duracaoMin = inteiro(c, "duracao_min");
        t.capa = texto(c, "capa");
        t.fundo = texto(c, "fundo");
        t.capas = linhas(texto(c, "capas"));
        t.fundos = linhas(texto(c, "fundos"));
        List<String[]> elenco = new ArrayList<>();
        for (String linha : linhas(texto(c, "elenco"))) elenco.add(linha.split("\t", 2));
        t.elenco = elenco;
        t.nomeEpisodio = texto(c, "nome_episodio");
        t.miniatura = texto(c, "miniatura");
        t.escolhido = inteiro(c, "escolhido") == 1;
        t.buscado = longo(c, "buscado");
        t.colecaoId = inteiro(c, "colecao_id");
        t.colecaoNome = texto(c, "colecao_nome");
        t.colecaoCapa = texto(c, "colecao_capa");
        t.colecaoFundo = texto(c, "colecao_fundo");
        return t;
    }

    // ---- Artistas ----

    public synchronized Map<String, Artista> artistas() {
        Map<String, Artista> mapa = new HashMap<>();
        try (Cursor c = banco.getReadableDatabase().query(BancoStarkTech.ARTISTA, null, null, null, null, null, null)) {
            while (c.moveToNext()) {
                Artista a = new Artista();
                a.chave = texto(c, "chave");
                a.nome = texto(c, "nome");
                a.situacao = inteiro(c, "situacao");
                a.fonte = texto(c, "fonte");
                a.foto = texto(c, "foto");
                a.fundo = texto(c, "fundo");
                a.pais = texto(c, "pais");
                a.buscado = longo(c, "buscado");
                a.descricao = texto(c, "descricao");
                a.resumo = texto(c, "resumo");
                mapa.put(a.chave, a);
            }
        }
        return mapa;
    }

    public synchronized void gravarArtista(Artista a) {
        ContentValues l = new ContentValues();
        l.put("chave", a.chave);
        l.put("nome", a.nome);
        l.put("situacao", a.situacao);
        l.put("fonte", a.fonte);
        l.put("foto", a.foto);
        l.put("fundo", a.fundo);
        l.put("pais", a.pais);
        l.put("buscado", a.buscado);
        l.put("descricao", a.descricao);
        l.put("resumo", a.resumo);
        banco.getWritableDatabase().insertWithOnConflict(BancoStarkTech.ARTISTA, null, l, SQLiteDatabase.CONFLICT_REPLACE);
    }

    // ---- Estado ----

    public synchronized Map<String, Estado> estados() {
        Map<String, Estado> mapa = new HashMap<>();
        try (Cursor c = banco.getReadableDatabase().query(BancoStarkTech.ESTADO, null, null, null, null, null, null)) {
            while (c.moveToNext()) {
                Estado e = new Estado();
                e.favorito = inteiro(c, "favorito") == 1;
                e.assistido = inteiro(c, "assistido") == 1;
                e.posicao = inteiro(c, "posicao");
                e.visto = longo(c, "visto");
                e.favoritado = longo(c, "favoritado");
                int iNota = c.getColumnIndexOrThrow("nota");
                e.nota = c.isNull(iNota) ? null : Integer.valueOf(c.getInt(iNota));
                e.faixaAudio = texto(c, "faixa_audio");
                e.faixaLegenda = texto(c, "faixa_legenda");
                mapa.put(texto(c, "chave"), e);
            }
        }
        return mapa;
    }

    public synchronized void gravarEstado(String chave, Estado e) {
        ContentValues l = new ContentValues();
        l.put("chave", chave);
        l.put("favorito", e.favorito ? 1 : 0);
        l.put("assistido", e.assistido ? 1 : 0);
        l.put("posicao", e.posicao);
        l.put("visto", e.visto);
        l.put("favoritado", e.favoritado);
        if (e.nota == null) l.putNull("nota");
        else l.put("nota", e.nota);
        l.put("faixa_audio", e.faixaAudio);
        l.put("faixa_legenda", e.faixaLegenda);
        banco.getWritableDatabase().insertWithOnConflict(BancoStarkTech.ESTADO, null, l, SQLiteDatabase.CONFLICT_REPLACE);
    }

    // ---- Texto ----

    private static String texto(Cursor c, String coluna) {
        int i = c.getColumnIndexOrThrow(coluna);
        return c.isNull(i) ? null : c.getString(i);
    }

    private static int inteiro(Cursor c, String coluna) {
        return c.getInt(c.getColumnIndexOrThrow(coluna));
    }

    private static long longo(Cursor c, String coluna) {
        return c.getLong(c.getColumnIndexOrThrow(coluna));
    }

    private static String limpo(String texto) {
        return texto == null ? "" : texto.replace('\n', ' ').replace('\t', ' ').trim();
    }

    private static String juntar(List<String> lista) {
        if (lista == null || lista.isEmpty()) return null;
        StringBuilder texto = new StringBuilder();
        for (String item : lista) {
            if (texto.length() > 0) texto.append('\n');
            texto.append(item.replace('\n', ' '));
        }
        return texto.toString();
    }

    private static List<String> linhas(String texto) {
        if (texto == null || texto.isEmpty()) return Collections.emptyList();
        return Collections.unmodifiableList(new ArrayList<>(Arrays.asList(texto.split("\n"))));
    }
}
