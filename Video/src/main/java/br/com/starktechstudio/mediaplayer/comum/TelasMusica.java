// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.app.Activity;
import android.content.Intent;

/** Rotas das telas de Músicas (1.5), como a {@link Navegacao} das telas de vídeo. */
public final class TelasMusica {

    private TelasMusica() {}

    public static void abrirAlbum(Activity origem, String idAlbum) {
        origem.startActivity(new Intent(origem, ColecaoMusicaActivity.class)
                .putExtra(ColecaoMusicaActivity.EXTRA_TIPO, ColecaoMusicaActivity.ALBUM)
                .putExtra(Navegacao.EXTRA_ID, idAlbum));
    }

    public static void abrirArtista(Activity origem, String idArtista) {
        origem.startActivity(new Intent(origem, ColecaoMusicaActivity.class)
                .putExtra(ColecaoMusicaActivity.EXTRA_TIPO, ColecaoMusicaActivity.ARTISTA)
                .putExtra(Navegacao.EXTRA_ID, idArtista));
    }

    /** O artista já na aba Vídeos (1.7). */
    public static void abrirVideosDoArtista(Activity origem, String idArtista) {
        origem.startActivity(new Intent(origem, ColecaoMusicaActivity.class)
                .putExtra(ColecaoMusicaActivity.EXTRA_TIPO, ColecaoMusicaActivity.ARTISTA)
                .putExtra(ColecaoMusicaActivity.EXTRA_VIDEOS, true)
                .putExtra(Navegacao.EXTRA_ID, idArtista));
    }

    public static void abrirPlaylist(Activity origem, long idPlaylist) {
        origem.startActivity(new Intent(origem, ColecaoMusicaActivity.class)
                .putExtra(ColecaoMusicaActivity.EXTRA_TIPO, ColecaoMusicaActivity.PLAYLIST)
                .putExtra(ColecaoMusicaActivity.EXTRA_PLAYLIST, idPlaylist));
    }

    public static void abrirTocandoAgora(Activity origem) {
        origem.startActivity(new Intent(origem, TocandoAgoraActivity.class));
    }
}
