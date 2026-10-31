// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.archos.mediacenter.video.R;

/** Marca do topo: "Stark" branco + "Tech" azul (#3b82f6), como no desenho. */
public final class Marca {

    private Marca() {}

    public static void aplicar(TextView texto) {
        String stark = texto.getContext().getString(R.string.st_marca_stark);
        String tech = texto.getContext().getString(R.string.st_marca_tech);
        SpannableStringBuilder marca = new SpannableStringBuilder(stark).append(tech);
        marca.setSpan(new ForegroundColorSpan(ContextCompat.getColor(texto.getContext(), R.color.st_azul)),
                stark.length(), marca.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        texto.setText(marca);
    }
}
