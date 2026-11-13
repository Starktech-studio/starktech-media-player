// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.os.SystemClock;

/**
 * Vídeo anterior em dois toques (1.7, D4 do plano 008): o 1º toque volta ao começo do vídeo; outro em até
 * {@link #JANELA_MS} vai ao vídeo anterior. Vale para o botão da tela, a tecla do controle e o botão de fora
 * (volante do carro, fone).
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
}
