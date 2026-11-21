// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.tablet;

import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;

/**
 * Botões de mídia de fora do app (multimídia do carro, fone Bluetooth, controle): o Android entrega
 * esses botões à sessão de mídia ativa, com o player na tela ou na miniatura. Próxima e anterior
 * trocam de capítulo e, no episódio (1.8.0, D5 do plano 009) e no vídeo de música (1.6), de vídeo; sem
 * capítulos e fora deles, a sessão não oferece essas ações e o player ignora os botões.
 */
final class BotoesDeMidia {

    /** O que os botões pedem ao player; anterior e próximo: o capítulo ou, no episódio e no vídeo de música, o vídeo. */
    interface Acoes {
        void tocar();

        void pausar();

        void anterior();

        void proximo();
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
                acoes.anterior();
            }

            @Override
            public void onSkipToNext() {
                acoes.proximo();
            }
        });
        // O título aparece na tela do carro
        sessao.setMetadata(new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE, titulo).build());
    }

    /** Liga com o player à vista (tela ou miniatura); desligada, os botões voltam para os outros apps. */
    void ligar(boolean ligada) {
        sessao.setActive(ligada);
    }

    /**
     * Chamado a cada aviso do player; só fala com o sistema quando algo muda. {@code comAnteriorEProximo}:
     * o vídeo tem capítulos ou troca de vídeo (episódio, vídeo de música).
     */
    void atualizar(boolean tocando, int posicaoSegundos, boolean comAnteriorEProximo) {
        if (mostrouAlgo && tocando == tocandoMostrado && comAnteriorEProximo == capitulosMostrados) return;
        mostrouAlgo = true;
        tocandoMostrado = tocando;
        capitulosMostrados = comAnteriorEProximo;
        long acoes = PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_PLAY_PAUSE;
        if (comAnteriorEProximo) acoes |= PlaybackState.ACTION_SKIP_TO_PREVIOUS | PlaybackState.ACTION_SKIP_TO_NEXT;
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
