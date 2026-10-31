// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;

/** Animações da interface: desligadas quando o aparelho está com "remover animações". */
public final class Animacoes {

    /** Troca de aba no topo da TV: a pílula estica até a aba nova e encolhe nela. */
    public static final long PILULA_MS = 260;
    /** Conteúdo da aba anterior saindo (esmaece e desliza para o lado oposto). */
    public static final long CONTEUDO_SAI_MS = 120;
    /** Espera antes de montar a aba nova: andar rápido pelas pílulas não monta as do caminho. */
    public static final long CONTEUDO_ESPERA_MS = 200;
    /** Conteúdo da aba nova entrando (desliza do lado para onde o menu andou). */
    public static final long CONTEUDO_ENTRA_MS = 220;

    private Animacoes() {}

    /** false com "remover animações" (escala de duração dos animadores = 0). */
    public static boolean ligadas(Context contexto) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) return ValueAnimator.areAnimatorsEnabled();
        return Settings.Global.getFloat(contexto.getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f;
    }
}
