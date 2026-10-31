// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.archos.mediacenter.video.R;

/**
 * Foco na TV (INTERFACE.md §2): borda branca de 2 dp (4 px no desenho) no item em foco;
 * capas também aumentam 6%.
 */
public final class FocoTv {

    private static final long DURACAO_MS = 150;

    private FocoTv() {}

    /** Borda de foco, sem escala (botões, cartões, painéis). */
    public static void aplicar(View item) {
        item.setFocusable(true);
        item.setForeground(ContextCompat.getDrawable(item.getContext(), R.drawable.st_foco_tv));
    }

    /** Borda de foco + escala de 6% (capas). Chama {@code extra} depois, se houver. */
    public static void aplicarComEscala(View item, View.OnFocusChangeListener extra) {
        aplicar(item);
        final float escala = ResourcesCompat.getFloat(item.getResources(), R.dimen.st_tv_foco_escala);
        item.setOnFocusChangeListener((v, temFoco) -> {
            float alvo = temFoco ? escala : 1f;
            v.animate().scaleX(alvo).scaleY(alvo).setDuration(DURACAO_MS).start();
            v.setTranslationZ(temFoco ? 1f : 0f);
            if (extra != null) extra.onFocusChange(v, temFoco);
        });
    }
}
