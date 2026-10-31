// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.ArrayRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import com.archos.mediacenter.video.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Informações de uso (Configurações, sem desenho; no padrão de Sobre): o que cada parte do app faz,
 * em cartões de duas colunas (Navegar, Biblioteca, Filme / série / show, Assistindo, Pastas, Editar
 * e Configurações), com o texto da TV (controle remoto) ou do tablet (toque) onde o uso muda. Na
 * TV cada cartão recebe o foco: o controle desce a página por eles.
 */
public class InformacoesUsoActivity extends AppCompatActivity {

    private boolean tv;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(Aparelho.tema(this));
        super.onCreate(savedInstanceState);
        tv = Aparelho.ehTv(this);

        FrameLayout raiz = new FrameLayout(this);
        raiz.setBackgroundResource(R.color.st_fundo);
        raiz.addView(new FundoAnimado(this), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        LinearLayout pagina = new LinearLayout(this);
        pagina.setOrientation(LinearLayout.VERTICAL);
        int lateral = getResources().getDimensionPixelSize(tv ? R.dimen.st_tv_margem_lateral : R.dimen.st_tablet_margem_h);
        pagina.setPadding(lateral, Medidas.px(this, 44), lateral, Medidas.px(this, 44));
        if (!tv) BordasSistema.aplicar(pagina, true, true, true, true);

        pagina.addView(topo());
        pagina.addView(Textos.novo(this, getString(R.string.st_uso_dica), R.font.st_inter_semibold, 20, R.color.st_texto_apagado),
                comTopo(Medidas.px(this, 12)));

        List<View> cartoes = new ArrayList<>();
        cartoes.add(cartao(R.drawable.st_ic_inicio, R.string.st_uso_navegar, tv ? R.array.st_uso_navegar_tv : R.array.st_uso_navegar_tablet));
        cartoes.add(cartao(R.drawable.st_ic_filmes, R.string.st_uso_biblioteca, tv ? R.array.st_uso_biblioteca_tv : R.array.st_uso_biblioteca_tablet));
        cartoes.add(cartao(R.drawable.st_ic_detalhes, R.string.st_uso_titulo, tv ? R.array.st_uso_titulo_tv : R.array.st_uso_titulo_tablet));
        cartoes.add(cartao(R.drawable.st_ic_assistir, R.string.st_uso_player, tv ? R.array.st_uso_player_tv : R.array.st_uso_player_tablet));
        cartoes.add(cartao(R.drawable.st_ic_pastas, R.string.st_uso_pastas, R.array.st_uso_pastas));
        cartoes.add(cartao(R.drawable.st_ic_editar, R.string.st_uso_editar, R.array.st_uso_editar));
        cartoes.add(cartao(R.drawable.st_ic_ajustes, R.string.st_uso_configuracoes, R.array.st_uso_configuracoes));
        LinearLayout grade = new LinearLayout(this);
        grade.setOrientation(LinearLayout.VERTICAL);
        int espaco = Medidas.px(this, 24);
        Grade.preencher(grade, cartoes, 2, espaco, espaco);
        // Cartões da mesma fileira com a mesma altura
        for (int i = 0; i < grade.getChildCount(); i++) {
            ViewGroup fileira = (ViewGroup) grade.getChildAt(i);
            for (int j = 0; j < fileira.getChildCount(); j++) {
                fileira.getChildAt(j).getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            }
        }
        LinearLayout.LayoutParams lpGrade = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpGrade.topMargin = Medidas.px(this, 32);
        pagina.addView(grade, lpGrade);

        ScrollView rolagem = new ScrollView(this);
        rolagem.setFocusable(false);
        rolagem.setVerticalScrollBarEnabled(false);
        rolagem.setOverScrollMode(View.OVER_SCROLL_NEVER);
        rolagem.addView(pagina, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        raiz.addView(rolagem, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(raiz);
    }

    private View topo() {
        LinearLayout topo = new LinearLayout(this);
        topo.setOrientation(LinearLayout.HORIZONTAL);
        topo.setGravity(Gravity.CENTER_VERTICAL);
        ImageButton voltar = new ImageButton(this);
        voltar.setImageResource(R.drawable.st_ic_voltar);
        ImageViewCompat.setImageTintList(voltar, ContextCompat.getColorStateList(this, R.color.st_texto));
        voltar.setBackgroundResource(R.drawable.st_botao_secundario);
        voltar.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int lado = getResources().getDimensionPixelSize(tv ? R.dimen.st_tv_botao_icone_altura : R.dimen.st_tablet_detalhe_topo_botao_altura);
        voltar.setPadding(lado / 4, lado / 4, lado / 4, lado / 4);
        voltar.setContentDescription(getString(R.string.st_voltar));
        if (tv) voltar.setForeground(ContextCompat.getDrawable(this, R.drawable.st_foco_tv));
        voltar.setOnClickListener(v -> finish());
        topo.addView(voltar, new LinearLayout.LayoutParams(lado, lado));
        TextView titulo = Textos.novo(this, getString(R.string.st_cfg_uso), R.font.st_inter_black, tv ? 48 : 43, R.color.st_texto);
        titulo.setLetterSpacing(-0.02f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMarginStart(Medidas.px(this, 24));
        topo.addView(titulo, lp);
        if (tv) voltar.post(voltar::requestFocus);
        return topo;
    }

    /** Cartão: ícone e nome da parte do app; embaixo, um item por linha, com marcador. */
    private View cartao(@DrawableRes int icone, @StringRes int nome, @ArrayRes int itens) {
        LinearLayout cartao = new LinearLayout(this);
        cartao.setOrientation(LinearLayout.VERTICAL);
        cartao.setBackgroundResource(R.drawable.st_fundo_cartao);
        int folga = Medidas.px(this, 28);
        cartao.setPadding(folga, folga, folga, folga);
        if (tv) {
            cartao.setFocusable(true);
            cartao.setForeground(ContextCompat.getDrawable(this, R.drawable.st_foco_tv));
        }

        TextView titulo = Textos.novo(this, getString(nome), R.font.st_inter_extrabold, 24, R.color.st_texto);
        titulo.setGravity(Gravity.CENTER_VERTICAL);
        titulo.setCompoundDrawablePadding(Medidas.px(this, 12));
        Icones.noTexto(titulo, icone, Medidas.px(this, 26), ContextCompat.getColor(this, R.color.st_azul));
        cartao.addView(titulo);

        for (String item : getResources().getStringArray(itens)) {
            LinearLayout linha = new LinearLayout(this);
            linha.setOrientation(LinearLayout.HORIZONTAL);
            linha.addView(Textos.novo(this, "•", R.font.st_inter_black, 18, R.color.st_ciano));
            TextView texto = Textos.novo(this, item, R.font.st_inter_medium, 18, R.color.st_texto_secundario);
            texto.setLineSpacing(0f, 1.3f);
            LinearLayout.LayoutParams lpTexto = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lpTexto.setMarginStart(Medidas.px(this, 12));
            linha.addView(texto, lpTexto);
            LinearLayout.LayoutParams lpLinha = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lpLinha.topMargin = Medidas.px(this, 14);
            cartao.addView(linha, lpLinha);
        }
        return cartao;
    }

    private static LinearLayout.LayoutParams comTopo(int topo) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = topo;
        return lp;
    }
}
