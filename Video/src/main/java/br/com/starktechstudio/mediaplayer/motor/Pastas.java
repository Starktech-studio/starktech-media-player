// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pastas da biblioteca e o tipo de cada uma (F2, etapa 3), no {@link BancoStarkTech}: Filmes,
 * Séries, TV Shows (como Séries, em aba própria), Desenhos, Shows, Stand-up (F3), Músicas (1.5) ou Ignorar. Ao adicionar, o tipo vem do nome da pasta (D4) — sem diferenciar
 * maiúscula, minúscula e acento — ou fica em branco ("escolha o tipo"); o usuário pode trocar.
 * Um arquivo pertence à pasta cadastrada mais próxima acima dele. Pasta de Músicas fica fora da
 * varredura de vídeo ({@link Classificador}) e é lida pela {@link VarreduraMusicas}.
 */
public final class Pastas {

    public enum Tipo { FILMES, SERIES, TVSHOWS, DESENHOS, SHOWS, STANDUP, MUSICAS, IGNORAR }

    public enum Origem { HD_EXTERNO, INTERNO }

    /** Uma pasta cadastrada; {@link #tipo} nulo = ainda sem tipo ("escolha o tipo"). */
    public static final class Pasta {
        public final String caminho;
        public final Origem origem;
        public final Tipo tipo;
        /** true = o usuário escolheu o tipo; false = reconhecido pelo nome (ou ainda sem tipo). */
        public final boolean escolhido;

        Pasta(String caminho, Origem origem, Tipo tipo, boolean escolhido) {
            this.caminho = caminho;
            this.origem = origem;
            this.tipo = tipo;
            this.escolhido = escolhido;
        }

        public String nome() {
            return new File(caminho).getName();
        }
    }

    /** D4: nomes reconhecidos, já normalizados (sem acento, minúsculos). */
    private static final Map<String, Tipo> NOMES = new LinkedHashMap<>();

    static {
        for (String nome : new String[]{"filmes", "filme", "movies", "films"}) NOMES.put(nome, Tipo.FILMES);
        for (String nome : new String[]{"series", "serie"}) NOMES.put(nome, Tipo.SERIES);
        for (String nome : new String[]{"tv shows", "tv show", "tvshows"}) NOMES.put(nome, Tipo.TVSHOWS);
        for (String nome : new String[]{"desenhos", "desenho", "desenhos animados", "animacao", "animacoes",
                "cartoons", "cartoon", "animation", "animations"}) NOMES.put(nome, Tipo.DESENHOS);
        for (String nome : new String[]{"shows", "show", "concerts", "concertos"}) NOMES.put(nome, Tipo.SHOWS);
        for (String nome : new String[]{"stand-up", "standup", "stand up", "stand-ups", "standups"}) NOMES.put(nome, Tipo.STANDUP);
        for (String nome : new String[]{"musicas", "musica", "music", "musics"}) NOMES.put(nome, Tipo.MUSICAS);
    }

    /** Pastas de Músicas cadastradas (1.5), na ordem em que foram adicionadas. */
    public synchronized List<Pasta> deMusicas() {
        List<Pasta> lista = new ArrayList<>();
        for (Pasta p : carregar().values()) if (p.tipo == Tipo.MUSICAS) lista.add(p);
        return lista;
    }

    private static Pastas instancia;

    private final BancoStarkTech banco;
    private Map<String, Pasta> cache;

    public static synchronized Pastas de(Context contexto) {
        if (instancia == null) instancia = new Pastas(BancoStarkTech.de(contexto));
        return instancia;
    }

    Pastas(BancoStarkTech banco) {
        this.banco = banco;
    }

    /** Tipo pelo nome da pasta (D4); nulo se o nome não é um dos reconhecidos. */
    public static Tipo reconhecer(String nomeDaPasta) {
        return NOMES.get(Nomes.normalizar(nomeDaPasta));
    }

    /** Pastas cadastradas, na ordem em que foram adicionadas. */
    public synchronized List<Pasta> lista() {
        return Collections.unmodifiableList(new ArrayList<>(carregar().values()));
    }

    public synchronized Pasta pasta(String caminho) {
        return carregar().get(caminho);
    }

    /** Cadastra a pasta com o tipo reconhecido pelo nome; já cadastrada, fica como está. */
    public synchronized Pasta adicionar(String caminho, Origem origem) {
        String limpo = semBarraNoFim(caminho);
        Pasta existente = carregar().get(limpo);
        if (existente != null) return existente;
        Pasta nova = new Pasta(limpo, origem, reconhecer(new File(limpo).getName()), false);
        gravar(nova);
        return nova;
    }

    /** Tipo escolhido pelo usuário (vale mais que o nome). */
    public synchronized void definirTipo(String caminho, Tipo tipo) {
        Pasta atual = carregar().get(caminho);
        if (atual == null) return;
        gravar(new Pasta(atual.caminho, atual.origem, tipo, true));
    }

    public synchronized void remover(String caminho) {
        if (carregar().remove(caminho) == null) return;
        banco.getWritableDatabase().delete(BancoStarkTech.PASTA, BancoStarkTech.CAMINHO + " = ?", new String[]{caminho});
    }

    /** Pasta cadastrada mais próxima acima do arquivo; nula se o arquivo está fora de todas. */
    public synchronized Pasta pastaDoArquivo(String caminhoArquivo) {
        Pasta melhor = null;
        for (Pasta p : carregar().values()) {
            if (dentro(caminhoArquivo, p.caminho) && (melhor == null || p.caminho.length() > melhor.caminho.length())) {
                melhor = p;
            }
        }
        return melhor;
    }

    static boolean dentro(String caminhoArquivo, String pasta) {
        return caminhoArquivo.startsWith(pasta + "/");
    }

    private static String semBarraNoFim(String caminho) {
        return caminho.length() > 1 && caminho.endsWith("/") ? caminho.substring(0, caminho.length() - 1) : caminho;
    }

    private void gravar(Pasta pasta) {
        ContentValues linha = new ContentValues();
        linha.put(BancoStarkTech.CAMINHO, pasta.caminho);
        linha.put(BancoStarkTech.ORIGEM, pasta.origem.name());
        if (pasta.tipo == null) linha.putNull(BancoStarkTech.TIPO);
        else linha.put(BancoStarkTech.TIPO, pasta.tipo.name());
        linha.put(BancoStarkTech.ESCOLHIDO, pasta.escolhido ? 1 : 0);
        linha.put(BancoStarkTech.ALTERADO, System.currentTimeMillis());
        SQLiteDatabase escrita = banco.getWritableDatabase();
        // Atualizar (e não substituir) mantém a ordem em que a pasta foi adicionada
        if (carregar().containsKey(pasta.caminho)) {
            escrita.update(BancoStarkTech.PASTA, linha, BancoStarkTech.CAMINHO + " = ?", new String[]{pasta.caminho});
        } else {
            escrita.insertWithOnConflict(BancoStarkTech.PASTA, null, linha, SQLiteDatabase.CONFLICT_REPLACE);
        }
        carregar().put(pasta.caminho, pasta);
    }

    private Map<String, Pasta> carregar() {
        if (cache != null) return cache;
        cache = new LinkedHashMap<>();
        try (Cursor linhas = banco.getReadableDatabase().query(BancoStarkTech.PASTA,
                new String[]{BancoStarkTech.CAMINHO, BancoStarkTech.ORIGEM, BancoStarkTech.TIPO, BancoStarkTech.ESCOLHIDO},
                null, null, null, null, "rowid")) {
            while (linhas.moveToNext()) {
                Origem origem;
                Tipo tipo = null;
                try {
                    origem = Origem.valueOf(linhas.getString(1));
                    if (!linhas.isNull(2)) tipo = Tipo.valueOf(linhas.getString(2));
                } catch (IllegalArgumentException e) {
                    continue; // valor de uma versão futura: ignora a linha
                }
                cache.put(linhas.getString(0), new Pasta(linhas.getString(0), origem, tipo, linhas.getInt(3) == 1));
            }
        }
        return cache;
    }
}
