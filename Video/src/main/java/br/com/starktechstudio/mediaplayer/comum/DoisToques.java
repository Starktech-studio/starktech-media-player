// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.os.SystemClock;

/**
 * Vídeo anterior em dois toques (1.7, D4 do plano 008): o 1º toque volta ao começo do vídeo; outro em até
 * {@link #JANELA_MS} vai ao vídeo anterior. Vale para o botão da tela, a tecla do controle e o botão de fora
 * (volante do carro, fone), no vídeo de música e, desde a 1.8.0, no episódio: no 1º episódio, que não tem
 * anterior, todo toque volta ao começo ({@link #vaiAoAnterior}).
 */
public final class DoisToques {

    public static final long JANELA_MS = 3000;

    private long ultimo;

    /** true = este é o 2º toque (vai ao anterior); false = o 1º (volta ao começo). */
    public boolean segundo() {
        return segundo(SystemClock.uptimeMillis());
    }

    boolean segundo(long agora) {
        if (ultimo > 0 && agora - ultimo <= JANELA_MS) {
            ultimo = 0;
            return true;
        }
        ultimo = agora;
        return false;
    }

    /**
     * true = abre o anterior (2º toque na janela); false = volta ao começo deste vídeo. Sem anterior (1º
     * episódio, começo da sequência), sempre false, e a conta recomeça.
     */
    public boolean vaiAoAnterior(boolean haAnterior) {
        return vaiAoAnterior(haAnterior, SystemClock.uptimeMillis());
    }

    boolean vaiAoAnterior(boolean haAnterior, long agora) {
        if (!haAnterior) {
            ultimo = 0;
            return false;
        }
        return segundo(agora);
    }
}
