// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.archos.mediacenter.video.R;

/**
 * Aba Músicas (1.1.1: só a navegação, sem motor; o motor é da 1.5). Título, as sub-abas Artistas, Álbuns e
 * Playlists, como numa biblioteca de música, e o que cada uma vai ter, com "Em breve". Desligada de fábrica:
 * aparece no menu depois de ligada em Configurações &gt; Navegação. Na TV, a sub-aba em foco é a mostrada.
 */
public class MusicasFragment extends Fragment {

    private static final String ESTADO_SUBABA = "subaba";

    private static final int[] NOMES = {R.string.st_musicas_artistas, R.string.st_musicas_albuns, R.string.st_musicas_playlists};
    private static final int[] TEXTOS = {R.string.st_musicas_artistas_texto, R.string.st_musicas_albuns_texto,
            R.string.st_musicas_playlists_texto};
    @DrawableRes
    private static final int[] ICONES = {R.drawable.st_ic_shows, R.drawable.st_ic_musicas, R.drawable.st_ic_capitulos};

    private boolean tv;
    private final TextView[] subabas = new TextView[NOMES.length];
    private ImageView icone;
    private TextView nome;
    private TextView texto;
    private int ativa;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Context contexto = requireContext();
        tv = Aparelho.ehTv(contexto);
        if (savedInstanceState != null) ativa = savedInstanceState.getInt(ESTADO_SUBABA, 0);

        LinearLayout pagina = new LinearLayout(contexto);
        pagina.setOrientation(LinearLayout.VERTICAL);
        int lateral = getResources().getDimensionPixelSize(tv ? R.dimen.st_tv_margem_lateral : R.dimen.st_tablet_margem_h);
        int topo = tv ? Medidas.px(contexto, 44) : getResources().getDimensionPixelSize(R.dimen.st_tablet_margem_v);
        pagina.setPadding(lateral, topo, lateral, Medidas.px(contexto, 44));

        TextView titulo = Textos.novo(contexto, getString(R.string.st_aba_musicas), R.font.st_inter_black,
                tv ? Medidas.TITULO_PAGINA_TV : Medidas.TITULO_PAGINA_TABLET, R.color.st_texto);
        titulo.setLetterSpacing(-0.02f);
        pagina.addView(titulo);
        pagina.addView(Textos.novo(contexto, getString(R.string.st_musicas_dica), R.font.st_inter_semibold, 20, R.color.st_texto_apagado),
                margemTopo(contexto, 6));

        // Sub-abas Artistas, Álbuns e Playlists, como as pílulas de temporada da Série
        LinearLayout linha = new LinearLayout(contexto);
        linha.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < NOMES.length; i++) {
            final int indice = i;
            TextView subaba = Textos.novo(contexto, getString(NOMES[i]), R.font.st_inter_extrabold, 22, R.color.st_texto);
            subaba.setTextColor(ContextCompat.getColorStateList(contexto, R.color.st_cor_pilula));
            subaba.setBackgroundResource(R.drawable.st_pilula_temporada);
            subaba.setGravity(Gravity.CENTER);
            subaba.setPadding(Medidas.px(contexto, 30), 0, Medidas.px(contexto, 30), 0);
            subaba.setFocusable(true);
            subaba.setClickable(true);
            subaba.setOnClickListener(v -> mostrar(indice));
            if (tv) subaba.setOnFocusChangeListener((v, temFoco) -> {
                if (temFoco) mostrar(indice);
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, Medidas.px(contexto, 56));
            if (i > 0) lp.setMarginStart(Medidas.px(contexto, 12));
            linha.addView(subaba, lp);
            subabas[i] = subaba;
        }
        pagina.addView(linha, margemTopo(contexto, 32));

        // O que a sub-aba vai ter, num cartão, com "Em breve"
        LinearLayout cartao = new LinearLayout(contexto);
        cartao.setOrientation(LinearLayout.VERTICAL);
        cartao.setGravity(Gravity.CENTER_HORIZONTAL);
        cartao.setBackgroundResource(R.drawable.st_fundo_cartao);
        int folga = Medidas.px(contexto, 56);
        cartao.setPadding(folga, folga, folga, folga);
        icone = new ImageView(contexto);
        ImageViewCompat.setImageTintList(icone, ContextCompat.getColorStateList(contexto, R.color.st_azul));
        icone.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        cartao.addView(icone, new LinearLayout.LayoutParams(Medidas.px(contexto, 72), Medidas.px(contexto, 72)));
        TextView emBreve = Textos.rotulo(contexto, getString(R.string.st_musicas_em_breve), 18);
        emBreve.setTextColor(ContextCompat.getColor(contexto, R.color.st_amarelo_marca));
        cartao.addView(emBreve, margemTopo(contexto, 24));
        nome = Textos.novo(contexto, "", R.font.st_inter_black, 34, R.color.st_texto);
        cartao.addView(nome, margemTopo(contexto, 10));
        texto = Textos.novo(contexto, "", R.font.st_inter_semibold, 22, R.color.st_texto_secundario);
        texto.setGravity(Gravity.CENTER_HORIZONTAL);
        texto.setLineSpacing(0f, 1.25f);
        texto.setMaxWidth(Medidas.px(contexto, 1100));
        cartao.addView(texto, margemTopo(contexto, 12));
        LinearLayout.LayoutParams lpCartao = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpCartao.topMargin = Medidas.px(contexto, 28);
        pagina.addView(cartao, lpCartao);

        mostrar(ativa);

        ViewGroup rolagem;
        if (tv) {
            ScrollView sv = new ScrollView(contexto);
            sv.setFocusable(false);
            rolagem = sv;
        } else {
            NestedScrollView sv = new NestedScrollView(contexto);
            sv.setClipToPadding(false);
            BordasSistema.aplicar(sv, false, false, false, true);
            rolagem = sv;
        }
        rolagem.setVerticalScrollBarEnabled(false);
        rolagem.setOverScrollMode(View.OVER_SCROLL_NEVER);
        rolagem.addView(pagina, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return rolagem;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ESTADO_SUBABA, ativa);
    }

    private void mostrar(int indice) {
        ativa = indice;
        for (int i = 0; i < subabas.length; i++) subabas[i].setSelected(i == indice);
        icone.setImageResource(ICONES[indice]);
        nome.setText(NOMES[indice]);
        texto.setText(TEXTOS[indice]);
    }

    private static LinearLayout.LayoutParams margemTopo(Context contexto, float pxHtml) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Medidas.px(contexto, pxHtml);
        return lp;
    }
}
