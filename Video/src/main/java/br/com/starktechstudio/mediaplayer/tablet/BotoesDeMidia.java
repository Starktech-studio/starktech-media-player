// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.tablet;

import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;

/**
 * Botões de mídia de fora do app (multimídia do carro, fone Bluetooth, controle): o Android entrega
 * esses botões à sessão de mídia ativa, com o player na tela ou na miniatura. Próxima e anterior
 * trocam de capítulo; sem capítulos, a sessão não oferece essas ações e o player ignora os botões.
 */
final class BotoesDeMidia {

    /** O que os botões pedem ao player. */
    interface Acoes {
        void tocar();

        void pausar();

        void capituloAnterior();

        void proximoCapitulo();
    }

    private final MediaSession sessao;
    private boolean mostrouAlgo;
    private boolean tocandoMostrado;
    private boolean capitulosMostrados;

    BotoesDeMidia(Context contexto, String titulo, Acoes acoes) {
        sessao = new MediaSession(contexto, "StarkTechPlayer");
        // Sem efeito a partir do Android 8; antes, sem eles a sessão não recebe os botões
        sessao.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        sessao.setCallback(new MediaSession.Callback() {
            @Override
            public void onPlay() {
                acoes.tocar();
            }

            @Override
            public void onPause() {
                acoes.pausar();
            }

            @Override
            public void onSkipToPrevious() {
                acoes.capituloAnterior();
            }

            @Override
            public void onSkipToNext() {
                acoes.proximoCapitulo();
            }
        });
        // O título aparece na tela do carro
        sessao.setMetadata(new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE, titulo).build());
    }

    /** Liga com o player à vista (tela ou miniatura); desligada, os botões voltam para os outros apps. */
    void ligar(boolean ligada) {
        sessao.setActive(ligada);
    }

    /** Chamado a cada aviso do player; só fala com o sistema quando algo muda. */
    void atualizar(boolean tocando, int posicaoSegundos, boolean comCapitulos) {
        if (mostrouAlgo && tocando == tocandoMostrado && comCapitulos == capitulosMostrados) return;
        mostrouAlgo = true;
        tocandoMostrado = tocando;
        capitulosMostrados = comCapitulos;
        long acoes = PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_PLAY_PAUSE;
        if (comCapitulos) acoes |= PlaybackState.ACTION_SKIP_TO_PREVIOUS | PlaybackState.ACTION_SKIP_TO_NEXT;
        sessao.setPlaybackState(new PlaybackState.Builder()
                .setActions(acoes)
                .setState(tocando ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED,
                        posicaoSegundos * 1000L, tocando ? 1f : 0f)
                .build());
    }

    void liberar() {
        sessao.release();
    }
}
