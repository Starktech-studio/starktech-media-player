// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Banco próprio do app (D2): {@code starktech.db}, separado do banco do Nova (que segue com a
 * biblioteca e os dados do TMDB). Tabela {@code edicao}: uma linha por dado editado à mão
 * (chave do título, campo, valor); sem linha, o dado não foi editado. Tabela {@code pasta}: as
 * pastas da biblioteca, com a origem e o tipo (nulo = "escolha o tipo"), reconhecido pelo nome
 * ou escolhido pelo usuário.
 */
public final class BancoStarkTech extends SQLiteOpenHelper {

    public static final String NOME = "starktech.db";
    private static final int VERSAO = 1;

    static final String EDICAO = "edicao";
    static final String CHAVE = "chave";
    static final String CAMPO = "campo";
    static final String VALOR = "valor";
    static final String ALTERADO = "alterado";

    static final String PASTA = "pasta";
    static final String CAMINHO = "caminho";
    static final String ORIGEM = "origem";
    static final String TIPO = "tipo";
    static final String ESCOLHIDO = "escolhido";

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
    }

    @Override
    public void onUpgrade(SQLiteDatabase banco, int versaoAntiga, int versaoNova) {
        // Versão 1 é a primeira: nada a migrar ainda
    }
}
