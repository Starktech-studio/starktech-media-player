// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Músicas e playlists no {@link BancoStarkTech} (1.5, versão 7): uma linha por arquivo das pastas de
 * Músicas, com as tags ({@link TagsMusica}) e a capa ({@link CapasMusica}); as playlists criadas no app,
 * com as músicas pelo caminho, na ordem. Música que saiu da biblioteca continua na playlist: volta quando
 * o arquivo voltar (HD ligado de novo).
 */
public final class AcervoMusicas {

    /** Versão da leitura: música lida com uma anterior é lida de novo. */
    public static final int LEITURA = 1;

    public static final class Musica {
        public String caminho;
        /** Pasta de Músicas cadastrada que tem o arquivo. */
        public String pasta;
        public String titulo;
        public String artista;
        public String artistaAlbum;
        public String album;
        public int faixa;
        public int disco;
        public Integer ano;
        public String genero;
        public long duracaoMs;
        public long tamanho;
        public long modificado;
        public long adicionado;
        /** {@link #LEITURA} com que foi lida; 0 = falta ler. */
        public int lido;
        /** Arquivo da capa do álbum; nulo = sem capa. */
        public String capa;
    }

    public static final class Playlist {
        public long id;
        public String nome;
        public long criada;
        public long alterada;
        /** Caminhos das músicas, na ordem. */
        public final List<String> musicas = new ArrayList<>();
    }

    private static AcervoMusicas instancia;

    private final BancoStarkTech banco;

    public static synchronized AcervoMusicas de(Context contexto) {
        if (instancia == null) instancia = new AcervoMusicas(BancoStarkTech.de(contexto));
        return instancia;
    }

    AcervoMusicas(BancoStarkTech banco) {
        this.banco = banco;
    }

    // ---- Músicas ----

    public synchronized List<Musica> musicas() {
        List<Musica> lista = new ArrayList<>();
        try (Cursor c = banco.getReadableDatabase().query(BancoStarkTech.MUSICA, null, null, null, null, null, null)) {
            while (c.moveToNext()) {
                Musica m = new Musica();
                m.caminho = texto(c, "caminho");
                m.pasta = texto(c, "pasta");
                m.titulo = texto(c, "titulo");
                m.artista = texto(c, "artista");
                m.artistaAlbum = texto(c, "artista_album");
                m.album = texto(c, "album");
                m.faixa = inteiro(c, "faixa");
                m.disco = inteiro(c, "disco");
                int iAno = c.getColumnIndexOrThrow("ano");
                m.ano = c.isNull(iAno) ? null : Integer.valueOf(c.getInt(iAno));
                m.genero = texto(c, "genero");
                m.duracaoMs = longo(c, "duracao_ms");
                m.tamanho = longo(c, "tamanho");
                m.modificado = longo(c, "modificado");
                m.adicionado = longo(c, "adicionado");
                m.lido = inteiro(c, "lido");
                m.capa = texto(c, "capa");
                lista.add(m);
            }
        }
        return lista;
    }

    /** Grava as novas e as que mudaram e apaga as que saíram, numa transação só. */
    public synchronized void gravar(List<Musica> gravar, List<String> remover) {
        SQLiteDatabase escrita = banco.getWritableDatabase();
        escrita.beginTransaction();
        try {
            for (Musica m : gravar) {
                ContentValues l = new ContentValues();
                l.put("caminho", m.caminho);
                l.put("pasta", m.pasta);
                l.put("titulo", m.titulo);
                l.put("artista", m.artista);
                l.put("artista_album", m.artistaAlbum);
                l.put("album", m.album);
                l.put("faixa", m.faixa);
                l.put("disco", m.disco);
                if (m.ano == null) l.putNull("ano");
                else l.put("ano", m.ano);
                l.put("genero", m.genero);
                l.put("duracao_ms", m.duracaoMs);
                l.put("tamanho", m.tamanho);
                l.put("modificado", m.modificado);
                l.put("adicionado", m.adicionado);
                l.put("lido", m.lido);
                l.put("capa", m.capa);
                escrita.insertWithOnConflict(BancoStarkTech.MUSICA, null, l, SQLiteDatabase.CONFLICT_REPLACE);
            }
            for (String caminho : remover) {
                escrita.delete(BancoStarkTech.MUSICA, "caminho = ?", new String[]{caminho});
            }
            escrita.setTransactionSuccessful();
        } finally {
            escrita.endTransaction();
        }
    }

    // ---- Playlists ----

    /** Em ordem de criação, cada uma com as músicas na ordem dela. */
    public synchronized List<Playlist> playlists() {
        List<Playlist> lista = new ArrayList<>();
        SQLiteDatabase leitura = banco.getReadableDatabase();
        try (Cursor c = leitura.query(BancoStarkTech.PLAYLIST, null, null, null, null, null, "id")) {
            while (c.moveToNext()) {
                Playlist p = new Playlist();
                p.id = longo(c, "id");
                p.nome = texto(c, "nome");
                p.criada = longo(c, "criada");
                p.alterada = longo(c, "alterada");
                lista.add(p);
            }
        }
        for (Playlist p : lista) {
            try (Cursor c = leitura.query(BancoStarkTech.PLAYLIST_MUSICA, new String[]{"caminho"}, "playlist = ?",
                    new String[]{String.valueOf(p.id)}, null, null, "posicao")) {
                while (c.moveToNext()) p.musicas.add(c.getString(0));
            }
        }
        return lista;
    }

    @Nullable
    public synchronized Playlist playlist(long id) {
        for (Playlist p : playlists()) if (p.id == id) return p;
        return null;
    }

    /** Devolve o id da nova. */
    public synchronized long criarPlaylist(String nome, List<String> musicas) {
        long agora = System.currentTimeMillis();
        ContentValues l = new ContentValues();
        l.put("nome", nome);
        l.put("criada", agora);
        l.put("alterada", agora);
        SQLiteDatabase escrita = banco.getWritableDatabase();
        long id = escrita.insertOrThrow(BancoStarkTech.PLAYLIST, null, l);
        gravarMusicas(id, musicas);
        return id;
    }

    public synchronized void renomearPlaylist(long id, String nome) {
        ContentValues l = new ContentValues();
        l.put("nome", nome);
        l.put("alterada", System.currentTimeMillis());
        banco.getWritableDatabase().update(BancoStarkTech.PLAYLIST, l, "id = ?", new String[]{String.valueOf(id)});
    }

    public synchronized void apagarPlaylist(long id) {
        SQLiteDatabase escrita = banco.getWritableDatabase();
        escrita.beginTransaction();
        try {
            escrita.delete(BancoStarkTech.PLAYLIST_MUSICA, "playlist = ?", new String[]{String.valueOf(id)});
            escrita.delete(BancoStarkTech.PLAYLIST, "id = ?", new String[]{String.valueOf(id)});
            escrita.setTransactionSuccessful();
        } finally {
            escrita.endTransaction();
        }
    }

    /** Troca as músicas da playlist pela lista, na ordem dela. */
    public synchronized void gravarMusicas(long id, List<String> musicas) {
        SQLiteDatabase escrita = banco.getWritableDatabase();
        escrita.beginTransaction();
        try {
            escrita.delete(BancoStarkTech.PLAYLIST_MUSICA, "playlist = ?", new String[]{String.valueOf(id)});
            int posicao = 0;
            for (String caminho : musicas) {
                ContentValues l = new ContentValues();
                l.put("playlist", id);
                l.put("posicao", posicao++);
                l.put("caminho", caminho);
                escrita.insertOrThrow(BancoStarkTech.PLAYLIST_MUSICA, null, l);
            }
            ContentValues alterada = new ContentValues();
            alterada.put("alterada", System.currentTimeMillis());
            escrita.update(BancoStarkTech.PLAYLIST, alterada, "id = ?", new String[]{String.valueOf(id)});
            escrita.setTransactionSuccessful();
        } finally {
            escrita.endTransaction();
        }
    }

    /** Lista com o item de {@code de} movido para {@code para} (subir e descer na playlist). */
    public static List<String> mover(List<String> musicas, int de, int para) {
        List<String> nova = new ArrayList<>(musicas);
        if (de < 0 || de >= nova.size() || para < 0 || para >= nova.size() || de == para) return nova;
        String item = nova.remove(de);
        nova.add(para, item);
        return nova;
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
}
