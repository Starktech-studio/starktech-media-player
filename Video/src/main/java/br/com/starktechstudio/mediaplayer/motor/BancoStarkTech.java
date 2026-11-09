// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Banco próprio do app (D2): {@code starktech.db}, separado do banco do Nova. Tabela
 * {@code edicao}: uma linha por dado editado à mão (chave do título, campo, valor); sem linha, o
 * dado não foi editado. Tabela {@code pasta}: as pastas da biblioteca, com a origem e o tipo (nulo
 * = "escolha o tipo"), reconhecido pelo nome ou escolhido pelo usuário.
 *
 * <p>Versão 2 (F3, biblioteca real — {@link Acervo}): {@code video}, um arquivo de vídeo achado nas
 * pastas, com a classificação e os dados técnicos lidos pelo motor; {@code tmdb}, os dados do TMDB
 * de um filme, show, episódio ou série; {@code estado}, favorito, assistido e a posição.
 *
 * <p>Versão 3 (só acrescenta, sem perder posição nem edição): {@code video.grupo}, a subpasta de
 * Shows; {@code tmdb.colecao_*}, a coleção do TMDB ({@code colecao_id} -1 = ainda não consultada);
 * {@code estado.nota}, nula = sem nota; {@code edicao.nome}, o nome em comum de quem editou
 * ({@link Classificador#chaveNome}); {@code artista}, a capa de cada artista ({@link BuscaArtista}).
 *
 * <p>Versão 4 (só acrescenta): {@code estado.faixa_audio} e {@code estado.faixa_legenda}, as faixas
 * escolhidas à mão; nulas = as preferências.
 *
 * <p>Versão 5 (só acrescenta): {@code artista.descricao} e {@code artista.resumo}, da Wikipédia. As capas
 * do Deezer voltam a ser buscadas: pegavam o primeiro homônimo, não o artista de verdade.
 */
public final class BancoStarkTech extends SQLiteOpenHelper {

    public static final String NOME = "starktech.db";
    private static final int VERSAO = 5;

    static final String EDICAO = "edicao";
    static final String CHAVE = "chave";
    static final String CAMPO = "campo";
    static final String VALOR = "valor";
    static final String ALTERADO = "alterado";
    static final String NOME_COMUM = "nome";

    static final String PASTA = "pasta";
    static final String CAMINHO = "caminho";
    static final String ORIGEM = "origem";
    static final String TIPO = "tipo";
    static final String ESCOLHIDO = "escolhido";

    static final String VIDEO = "video";
    static final String TMDB = "tmdb";
    static final String ESTADO = "estado";
    static final String ARTISTA = "artista";

    private static BancoStarkTech instancia;

    /** Um só por processo (o SQLite cuida da concorrência dentro dele). */
    public static synchronized BancoStarkTech de(Context contexto) {
        if (instancia == null) instancia = new BancoStarkTech(contexto.getApplicationContext(), NOME);
        return instancia;
    }

    /** {@code nome} nulo: banco só na memória (testes). */
    BancoStarkTech(Context contexto, String nome) {
        super(contexto, nome, null, VERSAO);
    }

    @Override
    public void onCreate(SQLiteDatabase banco) {
        criarAteVersao2(banco);
        criarVersao3(banco);
        criarVersao4(banco);
        criarVersao5(banco);
    }

    @Override
    public void onUpgrade(SQLiteDatabase banco, int versaoAntiga, int versaoNova) {
        if (versaoAntiga < 2) criarBiblioteca(banco);
        if (versaoAntiga < 3) criarVersao3(banco);
        if (versaoAntiga < 4) criarVersao4(banco);
        if (versaoAntiga < 5) criarVersao5(banco);
    }

    /** Versão 5: o texto do artista; quem veio do Deezer entra na fila de novo, e a foto só troca se achar outra. */
    private static void criarVersao5(SQLiteDatabase banco) {
        banco.execSQL("ALTER TABLE " + ARTISTA + " ADD COLUMN descricao TEXT");
        banco.execSQL("ALTER TABLE " + ARTISTA + " ADD COLUMN resumo TEXT");
        banco.execSQL("UPDATE " + ARTISTA + " SET situacao = " + Acervo.PENDENTE + " WHERE fonte = '" + BuscaArtista.DEEZER + "'");
    }

    /** Versão 4: as faixas escolhidas à mão. */
    private static void criarVersao4(SQLiteDatabase banco) {
        banco.execSQL("ALTER TABLE " + ESTADO + " ADD COLUMN faixa_audio TEXT");
        banco.execSQL("ALTER TABLE " + ESTADO + " ADD COLUMN faixa_legenda TEXT");
    }

    /** O banco como era até a versão 2 (o teste da migração parte dele). */
    static void criarAteVersao2(SQLiteDatabase banco) {
        banco.execSQL("CREATE TABLE " + EDICAO + " ("
                + CHAVE + " TEXT NOT NULL, "
                + CAMPO + " TEXT NOT NULL, "
                + VALOR + " TEXT NOT NULL, "
                + ALTERADO + " INTEGER NOT NULL, "
                + "PRIMARY KEY (" + CHAVE + ", " + CAMPO + "))");
        banco.execSQL("CREATE TABLE " + PASTA + " ("
                + CAMINHO + " TEXT PRIMARY KEY NOT NULL, "
                + ORIGEM + " TEXT NOT NULL, "
                + TIPO + " TEXT, "
                + ESCOLHIDO + " INTEGER NOT NULL, "
                + ALTERADO + " INTEGER NOT NULL)");
        criarBiblioteca(banco);
    }

    /** Versão 2: biblioteca real (F3). */
    private static void criarBiblioteca(SQLiteDatabase banco) {
        banco.execSQL("CREATE TABLE " + VIDEO + " ("
                + "caminho TEXT PRIMARY KEY NOT NULL, "
                + "pasta TEXT NOT NULL, "
                + "tipo TEXT NOT NULL, "
                + "titulo TEXT, "
                + "ano INTEGER, "
                + "serie TEXT, "
                + "temporada INTEGER NOT NULL DEFAULT 0, "
                + "episodio INTEGER NOT NULL DEFAULT 0, "
                + "artista TEXT, "
                + "motivo TEXT, "
                + "tamanho INTEGER NOT NULL DEFAULT 0, "
                + "modificado INTEGER NOT NULL DEFAULT 0, "
                + "adicionado INTEGER NOT NULL, "
                + "lido INTEGER NOT NULL DEFAULT 0, "
                + "duracao_ms INTEGER NOT NULL DEFAULT 0, "
                + "largura INTEGER NOT NULL DEFAULT 0, "
                + "altura INTEGER NOT NULL DEFAULT 0, "
                + "info_video TEXT, "
                + "audios TEXT, "
                + "legendas TEXT, "
                + "capitulos TEXT, "
                + "miniatura TEXT)");
        banco.execSQL("CREATE TABLE " + TMDB + " ("
                + "chave TEXT PRIMARY KEY NOT NULL, "
                + "situacao INTEGER NOT NULL, "
                + "id_tmdb INTEGER NOT NULL DEFAULT 0, "
                + "filme INTEGER NOT NULL DEFAULT 1, "
                + "titulo TEXT, "
                + "titulo_original TEXT, "
                + "ano TEXT, "
                + "sinopse TEXT, "
                + "generos TEXT, "
                + "duracao_min INTEGER NOT NULL DEFAULT 0, "
                + "capa TEXT, "
                + "fundo TEXT, "
                + "capas TEXT, "
                + "fundos TEXT, "
                + "elenco TEXT, "
                + "nome_episodio TEXT, "
                + "miniatura TEXT, "
                + "escolhido INTEGER NOT NULL DEFAULT 0, "
                + "buscado INTEGER NOT NULL)");
        banco.execSQL("CREATE TABLE " + ESTADO + " ("
                + "chave TEXT PRIMARY KEY NOT NULL, "
                + "favorito INTEGER NOT NULL DEFAULT 0, "
                + "assistido INTEGER NOT NULL DEFAULT 0, "
                + "posicao INTEGER NOT NULL DEFAULT 0, "
                + "visto INTEGER NOT NULL DEFAULT 0, "
                + "favoritado INTEGER NOT NULL DEFAULT 0)");
    }

    /** Versão 3: coletânea, nota, grupo de shows, capa de artista e edição por nome em comum. */
    private static void criarVersao3(SQLiteDatabase banco) {
        banco.execSQL("ALTER TABLE " + VIDEO + " ADD COLUMN grupo TEXT");
        banco.execSQL("ALTER TABLE " + TMDB + " ADD COLUMN colecao_id INTEGER NOT NULL DEFAULT " + Acervo.COLECAO_NAO_CONSULTADA);
        banco.execSQL("ALTER TABLE " + TMDB + " ADD COLUMN colecao_nome TEXT");
        banco.execSQL("ALTER TABLE " + TMDB + " ADD COLUMN colecao_capa TEXT");
        banco.execSQL("ALTER TABLE " + TMDB + " ADD COLUMN colecao_fundo TEXT");
        banco.execSQL("ALTER TABLE " + ESTADO + " ADD COLUMN nota INTEGER");
        banco.execSQL("ALTER TABLE " + EDICAO + " ADD COLUMN " + NOME_COMUM + " TEXT");
        banco.execSQL("CREATE TABLE " + ARTISTA + " ("
                + "chave TEXT PRIMARY KEY NOT NULL, "
                + "nome TEXT, "
                + "situacao INTEGER NOT NULL, "
                + "fonte TEXT, "
                + "foto TEXT, "
                + "fundo TEXT, "
                + "pais TEXT, "
                + "buscado INTEGER NOT NULL)");
        preencherNomesDasEdicoes(banco);
    }

    /** O que já foi editado passa a valer para os outros vídeos de mesmo nome. */
    private static void preencherNomesDasEdicoes(SQLiteDatabase banco) {
        try (Cursor c = banco.query(VIDEO, new String[]{"caminho", "tipo", "titulo", "ano", "serie", "temporada", "episodio", "artista"},
                null, null, null, null, null)) {
            while (c.moveToNext()) {
                Classificador.Tipo tipo;
                try {
                    tipo = Classificador.Tipo.valueOf(c.getString(1));
                } catch (IllegalArgumentException e) {
                    continue; // valor de uma versão futura
                }
                String nome = Classificador.chaveNome(tipo, c.getString(2), c.isNull(3) ? null : Integer.valueOf(c.getInt(3)),
                        c.getString(4), c.getInt(5), c.getInt(6), c.getString(7));
                if (nome == null) continue;
                ContentValues linha = new ContentValues();
                linha.put(NOME_COMUM, nome);
                banco.update(EDICAO, linha, CHAVE + " = ?", new String[]{c.getString(0)});
            }
        }
    }
}
