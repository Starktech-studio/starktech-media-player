// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.content.Context;
import android.util.TypedValue;
import android.widget.TextView;

/**
 * Medidas do desenho (px do HTML da TV) para telas montadas no código (Pastas, Não identificados,
 * Adicionar pasta): TV = px / 2 (docs/arquitetura/interface.md §3); tablet = px × 0,7, a versão
 * "compacta" do tablet-pastas.dc.html (nome 23 → 16 px, opções 17 → 11 px).
 */
public final class Medidas {

    /** Título de página (Filmes, Séries, Desenhos, Shows, Stand-up, Pastas, Configurações, Sobre), px do desenho. */
    public static final int TITULO_PAGINA_TV = 56;
    public static final int TITULO_PAGINA_TABLET = 50;

    private Medidas() {}

    private static float fator(Context contexto) {
        return Aparelho.ehTv(contexto) ? 0.5f : 0.7f;
    }

    /** Medida em pixels da tela para {@code pxHtml} do desenho da TV. */
    public static int px(Context contexto, float pxHtml) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, pxHtml * fator(contexto),
                contexto.getResources().getDisplayMetrics()));
    }

    /** Tamanho de texto (sp) para {@code pxHtml} do desenho da TV. */
    public static void texto(TextView texto, float pxHtml) {
        texto.setTextSize(TypedValue.COMPLEX_UNIT_SP, pxHtml * fator(texto.getContext()));
    }
}
