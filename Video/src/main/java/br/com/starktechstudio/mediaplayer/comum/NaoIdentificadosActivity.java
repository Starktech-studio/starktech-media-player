// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import com.archos.mediacenter.video.R;

import java.util.List;

import br.com.starktechstudio.mediaplayer.dados.Modelo.Titulo;
import br.com.starktechstudio.mediaplayer.dados.RepositorioBiblioteca;

/**
 * Não identificados (D15, sem desenho próprio; no padrão de Pastas): vídeos sem tipo ou fora do
 * padrão, com o caminho e a duração; OK ou toque abre Editar, onde se escolhe o título certo
 * (Buscar no TMDB) e o tipo. Montada no código, medidas do desenho da TV ({@link Medidas}).
 */
public class NaoIdentificadosActivity extends AppCompatActivity {

    private LinearLayout lista;
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

        // Topo: Voltar, "Não identificados" e a dica
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
        LinearLayout titulos = new LinearLayout(this);
        titulos.setOrientation(LinearLayout.VERTICAL);
        TextView titulo = Textos.novo(this, getString(R.string.st_nao_identificados), R.font.st_inter_black, tv ? 44 : 43, R.color.st_texto);
        titulo.setLetterSpacing(-0.02f);
        titulos.addView(titulo);
        TextView dica = Textos.novo(this, getString(R.string.st_nao_identificados_dica), R.font.st_inter_semibold, 20, R.color.st_texto_apagado);
        LinearLayout.LayoutParams lpDica = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpDica.topMargin = Medidas.px(this, 6);
        titulos.addView(dica, lpDica);
        LinearLayout.LayoutParams lpTitulos = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lpTitulos.setMarginStart(Medidas.px(this, 24));
        topo.addView(titulos, lpTitulos);
        pagina.addView(topo);

        lista = new LinearLayout(this);
        lista.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpLista = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpLista.topMargin = Medidas.px(this, 32);
        pagina.addView(lista, lpLista);

        ScrollView rolagem = new ScrollView(this);
        rolagem.setFocusable(false);
        rolagem.setVerticalScrollBarEnabled(false);
        rolagem.setOverScrollMode(View.OVER_SCROLL_NEVER);
        rolagem.addView(pagina, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        raiz.addView(rolagem, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(raiz);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Volta de Editar: o vídeo pode ter saído da lista
        montar();
    }

    private void montar() {
        List<Titulo> titulos = RepositorioBiblioteca.atual(this).naoIdentificados();
        lista.removeAllViews();
        View primeiro = null;
        for (Titulo t : titulos) {
            View item = item(t);
            if (primeiro == null) primeiro = item;
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (lista.getChildCount() > 0) lp.topMargin = Medidas.px(this, 12);
            lista.addView(item, lp);
        }
        if (tv && primeiro != null && getCurrentFocus() == null) primeiro.requestFocus();
    }

    /** Ícone de arquivo, nome, caminho e duração; Editar à direita. */
    private View item(Titulo t) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setBackgroundResource(R.drawable.st_fundo_cartao);
        item.setPadding(Medidas.px(this, 22), Medidas.px(this, 16), Medidas.px(this, 22), Medidas.px(this, 16));
        item.setFocusable(true);
        item.setClickable(true);
        if (tv) {
            item.setForeground(ContextCompat.getDrawable(this, R.drawable.st_foco_tv));
        } else {
            // Toque: ripple do tema por cima do cartão
            TypedValue valor = new TypedValue();
            getTheme().resolveAttribute(android.R.attr.selectableItemBackground, valor, true);
            item.setForeground(ContextCompat.getDrawable(this, valor.resourceId));
        }
        item.setOnClickListener(v -> Navegacao.abrirEditar(this, t.id));

        ImageView icone = new ImageView(this);
        icone.setImageResource(R.drawable.st_ic_arquivo);
        ImageViewCompat.setImageTintList(icone, ContextCompat.getColorStateList(this, R.color.st_aviso_claro));
        item.addView(icone, new LinearLayout.LayoutParams(Medidas.px(this, 28), Medidas.px(this, 28)));

        LinearLayout textos = new LinearLayout(this);
        textos.setOrientation(LinearLayout.VERTICAL);
        TextView nome = Textos.novo(this, t.titulo, R.font.st_inter_extrabold, 23, R.color.st_texto);
        nome.setSingleLine(true);
        nome.setEllipsize(TextUtils.TruncateAt.END);
        textos.addView(nome);
        TextView linha = Textos.novo(this, getString(R.string.st_pasta_linha, t.arquivo, getString(R.string.st_duracao_min, t.duracaoMin)),
                R.font.st_inter_semibold, 17, R.color.st_texto_apagado);
        linha.setSingleLine(true);
        linha.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        LinearLayout.LayoutParams lpLinha = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpLinha.topMargin = Medidas.px(this, 4);
        textos.addView(linha, lpLinha);
        LinearLayout.LayoutParams lpTextos = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lpTextos.setMarginStart(Medidas.px(this, 22));
        item.addView(textos, lpTextos);

        TextView editar = Textos.novo(this, getString(R.string.st_editar), R.font.st_inter_extrabold, 18, R.color.st_texto_secundario);
        editar.setCompoundDrawablePadding(Medidas.px(this, 8));
        Icones.noTexto(editar, R.drawable.st_ic_editar, Medidas.px(this, 22));
        LinearLayout.LayoutParams lpEditar = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpEditar.setMarginStart(Medidas.px(this, 22));
        item.addView(editar, lpEditar);
        item.setContentDescription(t.titulo);
        return item;
    }
}
