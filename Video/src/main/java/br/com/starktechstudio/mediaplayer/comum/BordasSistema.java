// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Com targetSdk 36 o app desenha sob as barras do sistema (edge-to-edge obrigatório).
 * Soma ao padding da view a área das barras (e do recorte da câmera) nos lados pedidos,
 * mantendo o padding do layout. Na TV as barras não ocupam espaço: não muda nada.
 */
public final class BordasSistema {

    private BordasSistema() {}

    public static void aplicar(View view, boolean esquerda, boolean topo, boolean direita, boolean base) {
        final int esquerdaOriginal = view.getPaddingLeft();
        final int topoOriginal = view.getPaddingTop();
        final int direitaOriginal = view.getPaddingRight();
        final int baseOriginal = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets barras = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(
                    esquerdaOriginal + (esquerda ? barras.left : 0),
                    topoOriginal + (topo ? barras.top : 0),
                    direitaOriginal + (direita ? barras.right : 0),
                    baseOriginal + (base ? barras.bottom : 0));
            // Não consome: as views irmãs também recebem
            return insets;
        });
    }
}
