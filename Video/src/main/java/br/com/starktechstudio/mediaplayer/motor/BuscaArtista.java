// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import com.archos.mediacenter.video.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Capa de artista (aba Shows), da 1.1.1 em diante: o artista é achado pelo nome no MusicBrainz (gratuito, sem
 * chave, um pedido por segundo e com identificação), e a foto e o fundo vêm do fanart.tv, pelo código dele
 * (MBID), com a chave de projeto que entra no APK na hora do build — linha {@code starktech.fanart.chave=} do
 * {@code ~/.gradle/gradle.properties} do WSL, nunca do repositório; sem ela, nada do fanart.tv. Sem foto lá, a
 * primeira foto da {@link WikipediaArtista}. O Deezer e a TheAudioDB saíram (termos de uso). Só aceita o artista
 * de nome igual ({@link #mesmoNome}); entre os homônimos, vale o primeiro do MusicBrainz que tem imagem no
 * fanart.tv. Se ainda vier errado, ficam a troca da capa no Editar ({@link #fotos}) e a busca pelo nome
 * ({@link #artistas}). As imagens são baixadas uma vez, para {@code files/artistas/}. O texto vem da
 * {@link WikipediaArtista}. Sem resposta, fica o bloco de cor. Tudo aqui usa a rede: só fora da thread da tela.
 */
public final class BuscaArtista {

    public static final String FANART = "fanart";
    public static final String WIKIPEDIA = "wikipedia";
    /** Fontes até a 1.1.0: quem veio delas volta a ser buscado (banco v6). */
    public static final String THEAUDIODB = "theaudiodb";
    public static final String DEEZER = "deezer";
    static final String MUSICBRAINZ = "musicbrainz";

    private static final String LOG = "StarkTechVarredura";
    private static final String ENDERECO_MUSICBRAINZ = "https://musicbrainz.org/ws/2/artist/?query=%s&fmt=json&limit=%d";
    private static final String ENDERECO_FANART = "https://webservice.fanart.tv/v3/music/%s?api_key=%s";
    /** Identifica o app nos pedidos: o MusicBrainz e a Wikipédia recusam quem não se identifica. */
    public static final String IDENTIFICACAO = "StarkTechMediaPlayer/1.0 (https://starktechstudio.com.br)";
    /** Homônimos de nome igual testados no fanart.tv, do mais parecido ao menos. */
    private static final int CANDIDATOS = 3;
    private static final int MAXIMO_RESULTADOS = 12;
    /** Resultados da busca pelo nome que ganham foto do fanart.tv (os primeiros). */
    private static final int RESULTADOS_COM_FOTO = 5;
    /** MusicBrainz: um pedido por segundo. */
    private static final long INTERVALO_MS = 1100;
    private static final long LIMITE_RESPOSTA = 2L * 1024 * 1024;

    /** O que o fanart.tv trouxe: endereços da foto e do fundo. */
    static final class Achado {
        String foto;
        String fundo;
    }

    /** Artista do MusicBrainz: o código (MBID), o nome e o que ajuda a reconhecer (país, descrição). */
    static final class ArtistaMb {
        final String id;
        final String nome;
        final String area;
        final String descricao;

        ArtistaMb(String id, String nome, String area, String descricao) {
            this.id = id;
            this.nome = nome;
            this.area = area;
            this.descricao = descricao;
        }
    }

    /** Artista da busca pelo nome, para escolher pelo nome, pela linha de apoio e pela foto (nula sem foto). */
    public static final class Encontrado {
        public final String nome;
        public final String foto;
        /** "United Kingdom · rock band"; vazio se o MusicBrainz não diz. */
        public final String detalhe;

        Encontrado(String nome, String foto, String detalhe) {
            this.nome = nome;
            this.foto = foto;
            this.detalhe = detalhe;
        }
    }

    private final File pasta;
    private final String chave;
    private long ultimoPedido;

    public BuscaArtista(Context contexto) {
        pasta = new File(contexto.getFilesDir(), "artistas");
        chave = contexto.getString(R.string.st_chave_fanart).trim();
    }

    /**
     * Linha do artista: {@link Acervo#ACHADO} com a imagem já baixada, {@link Acervo#NAO_ACHADO} se as
     * fontes responderam sem ele, {@link Acervo#ERRO} se alguma não respondeu e nada veio.
     */
    public Acervo.Artista buscar(String chaveArtista, String nome) {
        Acervo.Artista linha = new Acervo.Artista();
        linha.chave = chaveArtista;
        linha.nome = nome;
        linha.buscado = System.currentTimeMillis();
        boolean falhou = false;

        List<ArtistaMb> candidatos = new ArrayList<>();
        try {
            candidatos = mesmoNome(lerMusicBrainz(pedirMusicBrainz(nome, 10)), nome);
        } catch (IOException | JSONException e) {
            semResposta(MUSICBRAINZ, e);
            falhou = true;
        }
        if (!candidatos.isEmpty()) linha.pais = candidatos.get(0).area;
        Achado achado = null;
        for (int i = 0; i < Math.min(CANDIDATOS, candidatos.size()) && !chave.isEmpty(); i++) {
            try {
                Achado deste = lerFanart(lerTexto(String.format(Locale.ROOT, ENDERECO_FANART, candidatos.get(i).id, chave)));
                if (deste != null && (deste.foto != null || deste.fundo != null)) {
                    achado = deste;
                    if (candidatos.get(i).area != null) linha.pais = candidatos.get(i).area;
                    break;
                }
            } catch (IOException | JSONException e) {
                semResposta(FANART, e);
                falhou = true;
                break;
            }
        }
        if (achado != null) {
            if (achado.fundo != null) {
                linha.fundo = baixar(achado.fundo);
                if (linha.fundo == null) falhou = true;
            }
            if (achado.foto != null) {
                linha.foto = baixar(achado.foto);
                if (linha.foto != null) linha.fonte = FANART;
                else falhou = true;
            }
        }
        if (linha.foto == null) {
            // Reserva: a foto do artigo da Wikipédia (ou do Commons com o nome dele)
            List<String> daWikipedia = new WikipediaArtista().fotos(nome);
            if (!daWikipedia.isEmpty()) {
                linha.foto = baixar(daWikipedia.get(0));
                if (linha.foto != null) linha.fonte = WIKIPEDIA;
            }
        }
        linha.situacao = linha.foto != null || linha.fundo != null ? Acervo.ACHADO : falhou ? Acervo.ERRO : Acervo.NAO_ACHADO;
        lerTextoDoArtista(linha, nome);
        return linha;
    }

    /** Texto do artista: sem ele a capa continua valendo, então falha aqui não conta como falha da busca. */
    private static void lerTextoDoArtista(Acervo.Artista linha, String nome) {
        try {
            WikipediaArtista.Artigo artigo = new WikipediaArtista().artigo(nome);
            if (artigo != null) {
                linha.descricao = artigo.texto;
                linha.resumo = artigo.resumo;
            }
        } catch (IOException | JSONException e) {
            semResposta("wikipedia", e);
        }
    }

    /**
     * Fotos do fanart.tv para escolher no Editar: as do primeiro homônimo de nome igual que tem foto, da mais
     * curtida à menos; nula = sem resposta (ou sem a chave).
     */
    public List<String> fotos(String nome) {
        if (chave.isEmpty()) return null;
        try {
            for (ArtistaMb c : primeiros(mesmoNome(lerMusicBrainz(pedirMusicBrainz(nome, 10)), nome), CANDIDATOS)) {
                List<String> fotos = lerFotosFanart(lerTexto(String.format(Locale.ROOT, ENDERECO_FANART, c.id, chave)));
                if (!fotos.isEmpty()) return fotos;
            }
            return new ArrayList<>();
        } catch (IOException | JSONException e) {
            semResposta(FANART, e);
            return null;
        }
    }

    /** Busca livre por nome, para "Buscar artista" no Editar: os do MusicBrainz, os primeiros com a foto do fanart.tv; vazia sem resposta. */
    public List<Encontrado> artistas(String consulta) {
        List<Encontrado> achados = new ArrayList<>();
        try {
            List<ArtistaMb> lista = lerMusicBrainz(pedirMusicBrainz(consulta, MAXIMO_RESULTADOS));
            for (int i = 0; i < lista.size(); i++) {
                ArtistaMb a = lista.get(i);
                String foto = null;
                if (i < RESULTADOS_COM_FOTO && !chave.isEmpty()) {
                    try {
                        foto = lerFanart(lerTexto(String.format(Locale.ROOT, ENDERECO_FANART, a.id, chave))).foto;
                    } catch (IOException | JSONException | RuntimeException e) {
                        // sem foto: o nome e a linha de apoio já servem
                    }
                }
                achados.add(new Encontrado(a.nome, foto, detalhe(a)));
            }
        } catch (IOException | JSONException e) {
            semResposta(MUSICBRAINZ, e);
        }
        return achados;
    }

    /** O motivo sem o endereço: nele vai a chave. */
    private static void semResposta(String fonte, Exception e) {
        Log.w(LOG, "artista: sem resposta de " + fonte + " (" + e.getClass().getSimpleName() + ")");
    }

    // ---- Respostas ----

    /**
     * {@code {"artists": [{"id", "name", "score", "area": {"name"}, "disambiguation", "aliases": [...]}]}}, na
     * ordem do MusicBrainz (do mais parecido ao menos). Ocupado, ele responde {@code {"error": "..."}}: vale como
     * sem resposta.
     */
    static List<ArtistaMb> lerMusicBrainz(String json) throws JSONException, IOException {
        JSONObject raiz = new JSONObject(json);
        if (raiz.has("error")) throw new IOException("MusicBrainz ocupado");
        List<ArtistaMb> lista = new ArrayList<>();
        JSONArray artistas = raiz.optJSONArray("artists");
        for (int i = 0; artistas != null && i < artistas.length(); i++) {
            JSONObject a = artistas.getJSONObject(i);
            String id = texto(a, "id");
            String nome = texto(a, "name");
            if (id == null || nome == null) continue;
            JSONObject area = a.optJSONObject("area");
            lista.add(new ArtistaMb(id, nome, area == null ? null : texto(area, "name"), texto(a, "disambiguation")));
        }
        return lista;
    }

    /** Só os de nome igual ao procurado, na mesma ordem. */
    static List<ArtistaMb> mesmoNome(List<ArtistaMb> lista, String nome) {
        List<ArtistaMb> iguais = new ArrayList<>();
        for (ArtistaMb a : lista) if (mesmoNome(nome, a.nome)) iguais.add(a);
        return iguais;
    }

    private static List<ArtistaMb> primeiros(List<ArtistaMb> lista, int quantos) {
        return lista.size() > quantos ? lista.subList(0, quantos) : lista;
    }

    /**
     * {@code {"artistthumb": [{"url", "likes"}], "artistbackground": [...], "artist4kbackground": [...]}}: a foto
     * e o fundo mais curtidos (sem o fundo comum, o 4K); {@code {}} = o fanart.tv não tem nada do artista.
     * Erro vem como {@code {"status": "error"}}: vale como sem resposta.
     */
    static Achado lerFanart(String json) throws JSONException, IOException {
        JSONObject raiz = new JSONObject(json);
        if ("error".equals(texto(raiz, "status"))) throw new IOException("erro do fanart.tv");
        Achado achado = new Achado();
        achado.foto = maisCurtida(raiz.optJSONArray("artistthumb"));
        achado.fundo = maisCurtida(raiz.optJSONArray("artistbackground"));
        if (achado.fundo == null) achado.fundo = maisCurtida(raiz.optJSONArray("artist4kbackground"));
        return achado;
    }

    /** As fotos do artista, da mais curtida à menos. */
    static List<String> lerFotosFanart(String json) throws JSONException, IOException {
        JSONObject raiz = new JSONObject(json);
        if ("error".equals(texto(raiz, "status"))) throw new IOException("erro do fanart.tv");
        return porCurtidas(raiz.optJSONArray("artistthumb"));
    }

    private static String maisCurtida(JSONArray imagens) throws JSONException {
        List<String> lista = porCurtidas(imagens);
        return lista.isEmpty() ? null : lista.get(0);
    }

    private static List<String> porCurtidas(JSONArray imagens) throws JSONException {
        List<JSONObject> validas = new ArrayList<>();
        for (int i = 0; imagens != null && i < imagens.length(); i++) {
            JSONObject imagem = imagens.getJSONObject(i);
            if (endereco(texto(imagem, "url")) != null) validas.add(imagem);
        }
        // "likes" vem como texto ("8"); a ordem original desempata
        Collections.sort(validas, (a, b) -> Integer.compare(curtidas(b), curtidas(a)));
        List<String> enderecos = new ArrayList<>();
        for (JSONObject imagem : validas) {
            String url = texto(imagem, "url");
            if (!enderecos.contains(url)) enderecos.add(url);
        }
        return enderecos;
    }

    private static int curtidas(JSONObject imagem) {
        try {
            return Integer.parseInt(imagem.optString("likes", "0").trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** "United Kingdom · rock band": o país e a descrição do MusicBrainz, os que houver. */
    static String detalhe(ArtistaMb a) {
        StringBuilder texto = new StringBuilder();
        if (a.area != null) texto.append(a.area);
        if (a.descricao != null) texto.append(texto.length() > 0 ? " · " : "").append(a.descricao);
        return texto.toString();
    }

    /** Só https. */
    private static String endereco(String texto) {
        return texto != null && texto.startsWith("https://") ? texto : null;
    }

    /** Valor de texto; JSON nulo e vazio viram nulo. */
    private static String texto(JSONObject objeto, String campo) {
        if (objeto.isNull(campo)) return null;
        String valor = objeto.optString(campo, "").trim();
        return valor.isEmpty() ? null : valor;
    }

    /**
     * Mesmo artista pelo nome, com grafias diferentes: sem acento, maiúscula, pontuação e "The" no começo;
     * "&" vale "and" ("AC/DC" = "ACDC", "The Beatles" = "Beatles", "Simon & Garfunkel" = "Simon and Garfunkel").
     */
    static boolean mesmoNome(String a, String b) {
        String x = soLetras(a);
        return !x.isEmpty() && x.equals(soLetras(b));
    }

    static String soLetras(String nome) {
        return Nomes.normalizar(nome).replace("&", " and ").replaceAll("^the\\s+", "").replaceAll("[^\\p{L}\\p{N}]", "");
    }

    // ---- Rede ----

    static String codificar(String nome) throws IOException {
        return URLEncoder.encode(nome, "UTF-8").replace("+", "%20");
    }

    /** O nome como frase do MusicBrainz ("AC/DC" entre aspas: a barra não vira expressão). */
    static String consultaMusicBrainz(String nome) {
        return "\"" + nome.trim().replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private void esperarVez() throws IOException {
        long falta = ultimoPedido + INTERVALO_MS - SystemClock.elapsedRealtime();
        if (ultimoPedido > 0 && falta > 0) {
            try {
                Thread.sleep(falta);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("interrompido");
            }
        }
        ultimoPedido = SystemClock.elapsedRealtime();
    }

    /** Pedido ao MusicBrainz, no passo dele (um por segundo). */
    private String pedirMusicBrainz(String nome, int limite) throws IOException {
        esperarVez();
        return lerTexto(String.format(Locale.ROOT, ENDERECO_MUSICBRAINZ, codificar(consultaMusicBrainz(nome)), limite));
    }

    static String lerTexto(String endereco) throws IOException {
        HttpURLConnection conexao = null;
        try {
            conexao = (HttpURLConnection) new URL(endereco).openConnection();
            conexao.setConnectTimeout(15_000);
            conexao.setReadTimeout(20_000);
            conexao.setRequestProperty("User-Agent", IDENTIFICACAO);
            conexao.setRequestProperty("Accept", "application/json");
            // Inclui o 503 (MusicBrainz ocupado) e o 429: sem resposta agora, tenta de novo mais tarde
            if (conexao.getResponseCode() != HttpURLConnection.HTTP_OK) throw new IOException("HTTP " + conexao.getResponseCode());
            try (InputStream entrada = conexao.getInputStream()) {
                ByteArrayOutputStream memoria = new ByteArrayOutputStream();
                if (ImagemDoAparelho.copiar(entrada, memoria, LIMITE_RESPOSTA) < 0) throw new IOException("resposta grande demais");
                return memoria.toString("UTF-8");
            }
        } catch (RuntimeException e) {
            throw new IOException("resposta estranha", e);
        } finally {
            if (conexao != null) conexao.disconnect();
        }
    }

    /** Baixa a imagem para {@code files/artistas/}; devolve o caminho ou nulo. */
    private String baixar(String endereco) {
        HttpURLConnection conexao = null;
        try {
            conexao = (HttpURLConnection) new URL(endereco).openConnection();
            conexao.setConnectTimeout(15_000);
            conexao.setReadTimeout(30_000);
            conexao.setRequestProperty("User-Agent", IDENTIFICACAO);
            if (conexao.getResponseCode() != HttpURLConnection.HTTP_OK) return null;
            try (InputStream entrada = conexao.getInputStream()) {
                ImagemDoAparelho.Resultado imagem = ImagemDoAparelho.importar(pasta, entrada, conexao.getContentLength());
                return imagem.ok() ? Edicoes.arquivoDe(imagem.referencia) : null;
            }
        } catch (IOException | RuntimeException e) {
            return null;
        } finally {
            if (conexao != null) conexao.disconnect();
        }
    }
}
