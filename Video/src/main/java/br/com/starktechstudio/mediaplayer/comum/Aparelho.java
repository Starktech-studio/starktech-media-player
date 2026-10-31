// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.content.Context;
import android.content.pm.PackageManager;

import com.archos.mediacenter.video.R;

/**
 * TV ou tablet (D3): TV = aparelho com FEATURE_LEANBACK, como o Nova
 * (UiChoiceDialog.java:96), sem a escolha manual do Nova no tablet.
 * Celular usa o layout do tablet, deitado (D23).
 */
public final class Aparelho {

    private Aparelho() {}

    public static boolean ehTv(Context contexto) {
        return contexto.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK);
    }

    /** Tema das telas novas para o aparelho. */
    public static int tema(Context contexto) {
        return ehTv(contexto) ? R.style.Theme_StarkTech_Tv : R.style.Theme_StarkTech_Tablet;
    }
}
