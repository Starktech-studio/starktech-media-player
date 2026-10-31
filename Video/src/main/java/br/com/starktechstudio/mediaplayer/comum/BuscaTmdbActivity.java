// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.content.res.Resources;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.widget.ImageViewCompat;

import com.archos.mediacenter.video.R;

import java.util.List;

import br.com.starktechstudio.mediaplayer.dados.Modelo.ResultadoTmdb;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Tipo;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Titulo;
import br.com.starktechstudio.mediaplayer.dados.RepositorioBiblioteca;

/**
 * Buscar no TMDB (tv-busca-tmdb.dc.html; tablet sem desenho, D12): janela sobre Editar com o
 * campo de busca, os resultados (o atual com "Atual") e o texto de ajuda. Selecionar troca título,
 * sinopse, capa e fundo e não toca nos campos editados à mão (RepositorioBiblioteca); a tela
 * Editar relê ao voltar. Na F1 os resultados são simulados e não dependem do texto buscado.
 */
public class BuscaTmdbActivity extends AppCompatActivity {

    private RepositorioBiblioteca repositorio;
    private String id;
    private boolean tv;
    private EditText consulta;
    private LinearLayout resultados;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        tv = Aparelho.ehTv(this);
        setTheme(tv ? R.style.Theme_StarkTech_Menu_Tv : R.style.Theme_StarkTech_Menu);
        super.onCreate(savedInstanceState);
        repositorio = RepositorioBiblioteca.atual(this);
        id = getIntent().getStringExtra(Navegacao.EXTRA_ID);
        Titulo titulo = repositorio.titulo(id);
        if (titulo == null) {
            finish();
            return;
        }
        setContentView(R.layout.st_busca_tmdb);
        setFinishOnTouchOutside(true);

        Resources res = getResources();
        int padding = res.getDimensionPixelSize(tv ? R.dimen.st_tv_tmdb_padding : R.dimen.st_tablet_tmdb_padding);
        int espaco = padding * 5 / 9;
        View caixa = findViewById(R.id.st_tmdb_caixa);
        ViewGroup.LayoutParams lpCaixa = caixa.getLayoutParams();
        lpCaixa.width = res.getDimensionPixelSize(tv ? R.dimen.st_tv_tmdb_largura : R.dimen.st_tablet_tmdb_largura);
        caixa.setLayoutParams(lpCaixa);
        caixa.setPadding(padding, padding, padding, padding);

        TextView nome = findViewById(R.id.st_tmdb_titulo);
        nome.setText(titulo.tipo == Tipo.SERIE ? R.string.st_escolher_serie
                : titulo.tipo == Tipo.SHOW ? R.string.st_escolher_show : R.string.st_escolher_filme);
        nome.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(tv ? R.dimen.st_tv_texto_tmdb_titulo : R.dimen.st_tablet_texto_tmdb_titulo));

        ImageButton fechar = findViewById(R.id.st_tmdb_fechar);
        int alturaBotao = res.getDimensionPixelSize(tv ? R.dimen.st_tv_botao_icone_altura : R.dimen.st_tablet_botao_altura);
        ViewGroup.LayoutParams lpFechar = fechar.getLayoutParams();
        lpFechar.height = alturaBotao;
        fechar.setLayoutParams(lpFechar);
        int folgaIcone = alturaBotao / 4;
        fechar.setPadding(folgaIcone, folgaIcone, folgaIcone, folgaIcone);
        fechar.setOnClickListener(v -> finish());

        TextView rotulo = findViewById(R.id.st_tmdb_rotulo);
        rotulo.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(tv ? R.dimen.st_tv_texto_dados_rotulo : R.dimen.st_tablet_texto_dados_rotulo));
        ((ViewGroup.MarginLayoutParams) rotulo.getLayoutParams()).topMargin = espaco;

        // Campo e Buscar (64 px)
        View linha = findViewById(R.id.st_tmdb_linha_busca);
        ((ViewGroup.MarginLayoutParams) linha.getLayoutParams()).topMargin = espaco / 2;
        consulta = findViewById(R.id.st_tmdb_consulta);
        consulta.setText(titulo.titulo);
        consulta.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(tv ? R.dimen.st_tv_texto_campo : R.dimen.st_tablet_texto_campo));
        int paddingCampo = res.getDimensionPixelSize(tv ? R.dimen.st_tv_campo_padding : R.dimen.st_tablet_campo_padding);
        consulta.setPadding(paddingCampo, 0, paddingCampo, 0);
        if (tv) consulta.setForeground(ContextCompat.getDrawable(this, R.drawable.st_foco_tv));
        consulta.setOnEditorActionListener((v, acao, evento) -> {
            if (acao != EditorInfo.IME_ACTION_SEARCH) return false;
            buscar();
            return true;
        });
        Button buscar = findViewById(R.id.st_tmdb_buscar);
        buscar.setBackgroundResource(R.drawable.st_botao_primario);
        buscar.setStateListAnimator(null);
        buscar.setAllCaps(false);
        int alturaCampo = res.getDimensionPixelSize(tv ? R.dimen.st_tv_campo_altura : R.dimen.st_tablet_campo_altura);
        LinearLayout.LayoutParams lpBuscar = (LinearLayout.LayoutParams) buscar.getLayoutParams();
        lpBuscar.height = alturaCampo;
        lpBuscar.setMarginStart(espaco);
        buscar.setLayoutParams(lpBuscar);
        buscar.setMinHeight(0);
        buscar.setMinimumHeight(0);
        int paddingBotao = res.getDimensionPixelSize(tv ? R.dimen.st_tv_botao_padding : R.dimen.st_tablet_botao_padding);
        buscar.setPadding(paddingBotao, 0, paddingBotao, 0);
        buscar.setTypeface(ResourcesCompat.getFont(this, R.font.st_inter_extrabold));
        buscar.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(tv ? R.dimen.st_tv_texto_botao : R.dimen.st_tablet_texto_botao));
        buscar.setTextColor(ContextCompat.getColor(this, R.color.st_texto));
        buscar.setCompoundDrawablePadding(espaco / 2);
        Icones.noTexto(buscar, R.drawable.st_ic_buscar, res.getDimensionPixelSize(tv ? R.dimen.st_tv_botao_icone : R.dimen.st_tablet_botao_icone));
        if (tv) buscar.setForeground(ContextCompat.getDrawable(this, R.drawable.st_foco_tv));
        buscar.setOnClickListener(v -> buscar());
        ViewGroup.LayoutParams lpLinha = linha.getLayoutParams();
        lpLinha.height = alturaCampo;
        linha.setLayoutParams(lpLinha);

        resultados = findViewById(R.id.st_tmdb_resultados);
        ((ViewGroup.MarginLayoutParams) resultados.getLayoutParams()).topMargin = espaco;
        TextView ajuda = findViewById(R.id.st_tmdb_ajuda);
        ajuda.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(tv ? R.dimen.st_tv_texto_tmdb_linha : R.dimen.st_tablet_texto_tmdb_linha));
        ((ViewGroup.MarginLayoutParams) ajuda.getLayoutParams()).topMargin = espaco;

        buscar();
    }

    private void buscar() {
        List<ResultadoTmdb> lista = repositorio.buscarTmdb(id, consulta.getText().toString());
        resultados.removeAllViews();
        View primeiro = null;
        for (ResultadoTmdb r : lista) {
            View item = resultado(r);
            if (primeiro == null) primeiro = item;
            resultados.addView(item);
        }
        if (tv && primeiro != null) primeiro.requestFocus();
    }

    /** Capa 80x120 px, título, "2012 · Filme · TMDB", "Atual" e "Selecionar". */
    private View resultado(ResultadoTmdb r) {
        Resources res = getResources();
        int padding = res.getDimensionPixelSize(tv ? R.dimen.st_tv_editar_opcao_espaco : R.dimen.st_tablet_editar_opcao_espaco);
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setBackgroundResource(R.drawable.st_item_resultado_tmdb);
        item.setPadding(padding, padding * 3 / 4, padding, padding * 3 / 4);
        item.setFocusable(true);
        item.setClickable(true);
        if (tv) item.setForeground(ContextCompat.getDrawable(this, R.drawable.st_foco_tv));
        item.setOnClickListener(v -> {
            repositorio.selecionarResultadoTmdb(id, r.id);
            finish();
        });

        FrameLayout capa = new FrameLayout(this);
        capa.setBackground(CapaAdapter.blocoDeCor(this, r.corCapa, res.getDimension(tv ? R.dimen.st_tv_botao_canto : R.dimen.st_tablet_botao_canto), tv));
        ImageView imagem = new ImageView(this);
        imagem.setImageResource(R.drawable.st_ic_imagem);
        ImageViewCompat.setImageTintList(imagem, ContextCompat.getColorStateList(this, R.color.st_branco_35));
        int icone = res.getDimensionPixelSize(tv ? R.dimen.st_tv_editar_selo : R.dimen.st_tablet_editar_selo);
        FrameLayout.LayoutParams lpImagem = new FrameLayout.LayoutParams(icone, icone, Gravity.TOP | Gravity.END);
        lpImagem.setMargins(0, icone / 3, icone / 3, 0);
        capa.addView(imagem, lpImagem);
        item.addView(capa, new LinearLayout.LayoutParams(
                res.getDimensionPixelSize(tv ? R.dimen.st_tv_tmdb_capa_largura : R.dimen.st_tablet_tmdb_capa_largura),
                res.getDimensionPixelSize(tv ? R.dimen.st_tv_tmdb_capa_altura : R.dimen.st_tablet_tmdb_capa_altura)));

        LinearLayout textos = new LinearLayout(this);
        textos.setOrientation(LinearLayout.VERTICAL);
        TextView nome = texto(r.titulo, R.font.st_inter_extrabold, tv ? R.dimen.st_tv_texto_tmdb_nome : R.dimen.st_tablet_texto_tmdb_nome, R.color.st_texto);
        textos.addView(nome);
        TextView linha = texto(getString(R.string.st_resultado_linha, r.ano, getString(PecasDetalhe.rotuloTipo(r.tipo))),
                R.font.st_inter_semibold, tv ? R.dimen.st_tv_texto_tmdb_linha : R.dimen.st_tablet_texto_tmdb_linha, R.color.st_texto_apagado);
        LinearLayout.LayoutParams lpLinha = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpLinha.topMargin = padding / 3;
        textos.addView(linha, lpLinha);
        LinearLayout.LayoutParams lpTextos = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lpTextos.setMarginStart(padding * 3 / 2);
        item.addView(textos, lpTextos);

        if (r.atual) {
            TextView atual = texto(getString(R.string.st_atual), R.font.st_inter_bold,
                    tv ? R.dimen.st_tv_texto_selo : R.dimen.st_tablet_texto_detalhe_chip, R.color.st_texto);
            atual.setBackgroundResource(R.drawable.st_selo_destaque);
            int ph = res.getDimensionPixelSize(tv ? R.dimen.st_tv_detalhe_selo_padding_h : R.dimen.st_tablet_detalhe_chip_padding_h);
            int pv = res.getDimensionPixelSize(tv ? R.dimen.st_tv_detalhe_selo_padding_v : R.dimen.st_tablet_detalhe_chip_padding_v);
            atual.setPadding(ph, pv, ph, pv);
            item.addView(atual);
        }
        TextView selecionar = texto(getString(R.string.st_selecionar), R.font.st_inter_extrabold,
                tv ? R.dimen.st_tv_texto_detalhe_topo_botao : R.dimen.st_tablet_texto_botao, R.color.st_texto_secundario);
        LinearLayout.LayoutParams lpSelecionar = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpSelecionar.setMarginStart(padding * 3 / 2);
        lpSelecionar.setMarginEnd(padding / 2);
        item.addView(selecionar, lpSelecionar);
        item.setContentDescription(r.titulo + ", " + r.ano);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = resultados.getChildCount() > 0 ? padding * 2 / 3 : 0;
        item.setLayoutParams(lp);
        return item;
    }

    private TextView texto(String conteudo, int fonte, int tamanho, int cor) {
        TextView texto = new TextView(this);
        texto.setText(conteudo);
        texto.setTypeface(ResourcesCompat.getFont(this, fonte));
        texto.setIncludeFontPadding(false);
        texto.setSingleLine(true);
        texto.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(tamanho));
        texto.setTextColor(ContextCompat.getColor(this, cor));
        return texto;
    }
}
