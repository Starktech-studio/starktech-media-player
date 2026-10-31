// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.archos.mediacenter.video.R;

/**
 * Opção dos painéis flutuantes (Menu da capa, Áudio e legendas, Adicionar pasta), no padrão do
 * tv-menu-capa.dc.html: 60 px de altura, ícone e texto Inter 800; em foco ou tocada, fundo branco
 * e texto escuro. Medidas da TV ou do tablet.
 */
public final class OpcaoMenu {

    private OpcaoMenu() {}

    /** {@code icone} 0 = espaço vazio do tamanho do ícone (alinha com as opções que têm ✓). */
    public static TextView adicionar(LinearLayout opcoes, @DrawableRes int icone, CharSequence texto, Runnable acao) {
        Context contexto = opcoes.getContext();
        boolean tv = Aparelho.ehTv(contexto);
        Resources res = contexto.getResources();
        ColorStateList cores = ContextCompat.getColorStateList(contexto, R.color.st_cor_item_menu);
        TextView item = new TextView(contexto);
        item.setText(texto);
        item.setTypeface(ResourcesCompat.getFont(contexto, R.font.st_inter_extrabold));
        item.setIncludeFontPadding(false);
        item.setSingleLine(true);
        item.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(tv ? R.dimen.st_tv_texto_menu_item : R.dimen.st_tablet_texto_menu_item));
        item.setTextColor(cores);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setBackgroundResource(R.drawable.st_item_menu);
        int padding = res.getDimensionPixelSize(tv ? R.dimen.st_tv_menu_item_padding : R.dimen.st_tablet_menu_item_padding);
        item.setPadding(padding, 0, padding, 0);
        item.setCompoundDrawablePadding(res.getDimensionPixelSize(tv ? R.dimen.st_tv_menu_icone_espaco : R.dimen.st_tablet_menu_icone_espaco));
        Drawable desenho = icone == 0 ? new ColorDrawable(0) : AppCompatResources.getDrawable(contexto, icone);
        if (desenho != null) {
            int tamanho = res.getDimensionPixelSize(tv ? R.dimen.st_tv_menu_icone : R.dimen.st_tablet_menu_icone);
            desenho = DrawableCompat.wrap(desenho.mutate());
            DrawableCompat.setTintList(desenho, cores);
            desenho.setBounds(0, 0, tamanho, tamanho);
            item.setCompoundDrawablesRelative(desenho, null, null, null);
        }
        item.setFocusable(true);
        item.setClickable(true);
        item.setOnClickListener(v -> acao.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                res.getDimensionPixelSize(tv ? R.dimen.st_tv_menu_item_altura : R.dimen.st_tablet_menu_item_altura));
        if (opcoes.getChildCount() > 0) {
            lp.topMargin = res.getDimensionPixelSize(tv ? R.dimen.st_tv_menu_item_espaco : R.dimen.st_tablet_menu_item_espaco);
        }
        opcoes.addView(item, lp);
        return item;
    }
}
