// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.content.Context;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.FontRes;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.archos.mediacenter.video.R;

/** Textos das telas montadas no código, com a fonte Inter do peso pedido e a medida do desenho ({@link Medidas}). */
public final class Textos {

    private Textos() {}

    public static TextView novo(Context contexto, CharSequence conteudo, @FontRes int fonte, float pxHtml, @ColorRes int cor) {
        TextView texto = new TextView(contexto);
        texto.setText(conteudo);
        texto.setTypeface(ResourcesCompat.getFont(contexto, fonte));
        texto.setIncludeFontPadding(false);
        texto.setTextColor(ContextCompat.getColor(contexto, cor));
        Medidas.texto(texto, pxHtml);
        return texto;
    }

    /** Rótulo em caixa alta (Inter 900, espaçamento 0,15, #94a3b8), como "HD EXTERNO (USB)". */
    public static TextView rotulo(Context contexto, CharSequence conteudo, float pxHtml) {
        TextView texto = novo(contexto, conteudo, R.font.st_inter_black, pxHtml, R.color.st_texto_apagado);
        texto.setAllCaps(true);
        texto.setLetterSpacing(0.15f);
        return texto;
    }
}
