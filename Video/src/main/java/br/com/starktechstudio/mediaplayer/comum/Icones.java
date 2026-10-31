// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.graphics.drawable.Drawable;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;

/** Ícones de traço (res/drawable/st_ic_*) ao lado do texto de botões e selos. */
public final class Icones {

    private Icones() {}

    /** Ícone à esquerda do texto, no tamanho dado (px), na cor do texto. */
    public static void noTexto(TextView texto, @DrawableRes int recurso, int tamanho) {
        noTexto(texto, recurso, tamanho, texto.getCurrentTextColor());
    }

    /** Ícone à esquerda do texto, no tamanho dado (px), na cor dada. */
    public static void noTexto(TextView texto, @DrawableRes int recurso, int tamanho, int cor) {
        Drawable desenho = tingido(texto, recurso, tamanho, cor);
        if (desenho != null) texto.setCompoundDrawablesRelative(desenho, null, null, null);
    }

    /** Ícone acima do texto (botões do player do tablet: ícone e nome embaixo). */
    public static void acimaDoTexto(TextView texto, @DrawableRes int recurso, int tamanho, int cor) {
        Drawable desenho = tingido(texto, recurso, tamanho, cor);
        if (desenho != null) texto.setCompoundDrawablesRelative(null, desenho, null, null);
    }

    private static Drawable tingido(TextView texto, @DrawableRes int recurso, int tamanho, int cor) {
        Drawable desenho = AppCompatResources.getDrawable(texto.getContext(), recurso);
        if (desenho == null) return null;
        desenho = DrawableCompat.wrap(desenho.mutate());
        DrawableCompat.setTint(desenho, cor);
        desenho.setBounds(0, 0, tamanho, tamanho);
        return desenho;
    }
}
