// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.content.res.Resources;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DimenRes;
import androidx.annotation.FontRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.widget.ImageViewCompat;

import com.archos.mediacenter.video.R;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import br.com.starktechstudio.mediaplayer.dados.Modelo.Campo;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Tipo;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Titulo;
import br.com.starktechstudio.mediaplayer.dados.RepositorioBiblioteca;

/**
 * Editar (tv-editar.dc.html, tablet-editar.dc.html), igual nos dois aparelhos com o layout de
 * cada um: capa e fundo escolhidos entre as opções do TMDB, a escolhida em destaque e as outras
 * embaixo (Do HD / Do aparelho, D14: navegador de pastas da fase H), campos Título, Título original, Tipo, Ano, Duração, Gêneros e Sinopse;
 * Buscar no TMDB, Restaurar original e Salvar. Salvar marca os campos mudados como "editados à mão"
 * (o Detalhe passa a mostrar "corrigidos à mão"). Na volta da busca no TMDB, os campos já mexidos
 * nesta tela ficam como estão.
 */
public class EditarActivity extends AppCompatActivity {

    private RepositorioBiblioteca repositorio;
    private String id;
    private boolean tv;
    private final Map<Campo, EditText> campos = new EnumMap<>(Campo.class);
    /** Campos mexidos nesta tela e ainda não salvos. */
    private final Set<Campo> mexidos = EnumSet.noneOf(Campo.class);
    private final List<TextView> segmentos = new ArrayList<>();
    private Tipo tipo;
    private int capa;
    private int fundo;
    private int capaSalva;
    private int fundoSalvo;
    /** Opções em fileira (para o foco da TV entre capa e fundo). */
    private final List<View> opcoesCapa = new ArrayList<>();
    private final List<View> opcoesFundo = new ArrayList<>();
    private int porFileiraCapa = 1;
    private int porFileiraFundo = 1;
    private boolean carregando;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(Aparelho.tema(this));
        super.onCreate(savedInstanceState);
        tv = Aparelho.ehTv(this);
        repositorio = RepositorioBiblioteca.atual(this);
        id = getIntent().getStringExtra(Navegacao.EXTRA_ID);
        if (repositorio.titulo(id) == null) {
            finish();
            return;
        }
        setContentView(tv ? R.layout.st_tv_editar : R.layout.st_tablet_editar);
        if (!tv) BordasSistema.aplicar(findViewById(R.id.st_editar_corpo), true, true, true, true);

        findViewById(R.id.st_editar_voltar).setOnClickListener(v -> finish());
        findViewById(R.id.st_editar_buscar_tmdb).setOnClickListener(v -> Navegacao.abrirBuscaTmdb(this, id));
        findViewById(R.id.st_editar_restaurar).setOnClickListener(v -> {
            repositorio.restaurarOriginal(id);
            mexidos.clear();
            carregar();
        });
        findViewById(R.id.st_editar_salvar).setOnClickListener(v -> salvar());
        View cancelar = findViewById(R.id.st_editar_cancelar);
        if (cancelar != null) cancelar.setOnClickListener(v -> finish());

        montarCampos();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Primeira vez e volta da busca no TMDB (título, sinopse, capa e fundo podem ter mudado)
        carregar();
    }

    private void carregar() {
        Titulo t = repositorio.titulo(id);
        if (t == null) {
            finish();
            return;
        }
        ((TextView) findViewById(R.id.st_editar_nome)).setText(t.titulo);
        carregando = true;
        for (Map.Entry<Campo, EditText> item : campos.entrySet()) {
            if (!mexidos.contains(item.getKey())) item.getValue().setText(t.valor(item.getKey()));
        }
        carregando = false;
        if (!mexidos.contains(Campo.TIPO)) marcarTipo(t.tipo == null ? Tipo.FILME : t.tipo);
        capa = capaSalva = t.corCapa;
        fundo = fundoSalvo = t.corFundo;
        montarOpcoes(findViewById(R.id.st_editar_capas), repositorio.opcoesCapa(id), true, -1);
        montarOpcoes(findViewById(R.id.st_editar_fundos), repositorio.opcoesFundo(id), false, -1);
    }

    private void salvar() {
        Map<Campo, String> valores = new EnumMap<>(Campo.class);
        for (Map.Entry<Campo, EditText> item : campos.entrySet()) valores.put(item.getKey(), item.getValue().getText().toString());
        valores.put(Campo.TIPO, tipo.name());
        repositorio.salvarEdicao(id, valores);
        Titulo t = repositorio.titulo(id);
        if (t != null && t.corCapa != capa) repositorio.escolherCapa(id, capa);
        if (t != null && t.corFundo != fundo) repositorio.escolherFundo(id, fundo);
        finish();
    }

    // ---- Campos (2 colunas, como no desenho) ----

    private void montarCampos() {
        LinearLayout alvo = findViewById(R.id.st_editar_campos);
        Resources res = getResources();
        int espacoH = res.getDimensionPixelSize(tv ? R.dimen.st_tv_campo_espaco_h : R.dimen.st_tablet_campo_espaco_h);
        int espacoV = res.getDimensionPixelSize(tv ? R.dimen.st_tv_campo_espaco_v : R.dimen.st_tablet_campo_espaco_v);

        linha(alvo, 0, espacoH, campo(Campo.TITULO, R.string.st_campo_titulo, false), campo(Campo.TITULO_ORIGINAL, R.string.st_campo_titulo_original, false));
        LinearLayout anoDuracao = new LinearLayout(this);
        anoDuracao.setOrientation(LinearLayout.HORIZONTAL);
        anoDuracao.setBaselineAligned(false);
        adicionarComPeso(anoDuracao, campo(Campo.ANO, R.string.st_campo_ano, false), 0);
        adicionarComPeso(anoDuracao, campo(Campo.DURACAO, R.string.st_campo_duracao, false), espacoH);
        linha(alvo, espacoV, espacoH, tipoSegmentado(), anoDuracao);
        linha(alvo, espacoV, espacoH, campo(Campo.GENEROS, R.string.st_campo_generos, false));
        linha(alvo, espacoV, espacoH, campo(Campo.SINOPSE, R.string.st_campo_sinopse, true));
    }

    private void linha(LinearLayout alvo, int topo, int espaco, View... celulas) {
        LinearLayout linha = new LinearLayout(this);
        linha.setOrientation(LinearLayout.HORIZONTAL);
        linha.setBaselineAligned(false);
        for (int i = 0; i < celulas.length; i++) adicionarComPeso(linha, celulas[i], i > 0 ? espaco : 0);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = topo;
        alvo.addView(linha, lp);
    }

    private static void adicionarComPeso(LinearLayout linha, View celula, int inicio) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMarginStart(inicio);
        linha.addView(celula, lp);
    }

    /** Rótulo em caixa alta + campo (#0f172a, borda #1e293b, azul em foco). */
    private View campo(Campo qual, @StringRes int rotulo, boolean sinopse) {
        Resources res = getResources();
        LinearLayout celula = new LinearLayout(this);
        celula.setOrientation(LinearLayout.VERTICAL);
        celula.addView(rotulo(rotulo));

        EditText texto = new EditText(this);
        texto.setBackgroundResource(R.drawable.st_campo_texto);
        texto.setTypeface(ResourcesCompat.getFont(this, sinopse ? R.font.st_inter_medium : R.font.st_inter_semibold));
        texto.setTextColor(ContextCompat.getColor(this, R.color.st_texto));
        texto.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(tv ? R.dimen.st_tv_texto_campo : R.dimen.st_tablet_texto_campo));
        int padding = res.getDimensionPixelSize(tv ? R.dimen.st_tv_campo_padding : R.dimen.st_tablet_campo_padding);
        int altura = res.getDimensionPixelSize(tv ? R.dimen.st_tv_campo_altura : R.dimen.st_tablet_campo_altura);
        if (sinopse) {
            texto.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            texto.setMinLines(4);
            texto.setMaxLines(6);
            texto.setGravity(Gravity.TOP | Gravity.START);
            texto.setLineSpacing(0f, 1.24f);
            texto.setPadding(padding, padding * 3 / 4, padding, padding * 3 / 4);
        } else {
            texto.setInputType(qual == Campo.ANO ? InputType.TYPE_CLASS_NUMBER
                    : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            texto.setSingleLine(true);
            texto.setGravity(Gravity.CENTER_VERTICAL);
            texto.setPadding(padding, 0, padding, 0);
        }
        if (tv) texto.setForeground(ContextCompat.getDrawable(this, R.drawable.st_foco_tv));
        texto.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!carregando) mexidos.add(qual);
            }
        });
        campos.put(qual, texto);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                sinopse ? ViewGroup.LayoutParams.WRAP_CONTENT : altura);
        lp.topMargin = res.getDimensionPixelSize(tv ? R.dimen.st_tv_detalhe_dados_rotulo_espaco : R.dimen.st_tablet_dados_rotulo_espaco);
        celula.addView(texto, lp);
        return celula;
    }

    /** Tipo: Filme / Série / Show, a escolhida em branco. */
    private View tipoSegmentado() {
        Resources res = getResources();
        LinearLayout celula = new LinearLayout(this);
        celula.setOrientation(LinearLayout.VERTICAL);
        celula.addView(rotulo(R.string.st_campo_tipo));

        LinearLayout grupo = new LinearLayout(this);
        grupo.setOrientation(LinearLayout.HORIZONTAL);
        grupo.setBackgroundResource(R.drawable.st_campo_texto);
        int folga = res.getDimensionPixelSize(tv ? R.dimen.st_tv_detalhe_dados_rotulo_espaco : R.dimen.st_tablet_dados_rotulo_espaco);
        grupo.setPadding(folga, folga, folga, folga);
        int altura = res.getDimensionPixelSize(tv ? R.dimen.st_tv_segmento_altura : R.dimen.st_tablet_segmento_altura);
        Tipo[] tipos = {Tipo.FILME, Tipo.SERIE, Tipo.SHOW};
        for (int i = 0; i < tipos.length; i++) {
            final Tipo opcao = tipos[i];
            TextView segmento = new TextView(this);
            segmento.setText(PecasDetalhe.rotuloTipo(opcao));
            segmento.setTypeface(ResourcesCompat.getFont(this, R.font.st_inter_extrabold));
            segmento.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(tv ? R.dimen.st_tv_texto_botao : R.dimen.st_tablet_texto_botao));
            segmento.setTextColor(ContextCompat.getColorStateList(this, R.color.st_cor_segmento));
            segmento.setBackgroundResource(R.drawable.st_segmento);
            segmento.setGravity(Gravity.CENTER);
            segmento.setFocusable(true);
            segmento.setClickable(true);
            segmento.setTag(opcao);
            if (tv) segmento.setForeground(ContextCompat.getDrawable(this, R.drawable.st_foco_tv));
            segmento.setOnClickListener(v -> {
                mexidos.add(Campo.TIPO);
                marcarTipo(opcao);
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, altura, 1f);
            if (i > 0) lp.setMarginStart(folga);
            grupo.addView(segmento, lp);
            segmentos.add(segmento);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = folga;
        celula.addView(grupo, lp);
        return celula;
    }

    private void marcarTipo(Tipo novo) {
        tipo = novo;
        for (TextView segmento : segmentos) segmento.setSelected(segmento.getTag() == novo);
    }

    private TextView rotulo(@StringRes int texto) {
        TextView rotulo = new TextView(this);
        rotulo.setText(texto);
        rotulo.setTypeface(ResourcesCompat.getFont(this, R.font.st_inter_black));
        rotulo.setIncludeFontPadding(false);
        rotulo.setAllCaps(true);
        rotulo.setLetterSpacing(0.15f);
        rotulo.setTextColor(ContextCompat.getColor(this, R.color.st_texto_apagado));
        rotulo.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(
                tv ? R.dimen.st_tv_texto_dados_rotulo : R.dimen.st_tablet_texto_dados_rotulo));
        return rotulo;
    }

    // ---- Capa e fundo ----

    /**
     * Capa ou fundo: a escolhida em destaque (maior, anel azul e ✓), com "atual" / "nova" e as
     * regras de formato ao lado; embaixo, as outras opções do TMDB (blocos de cor na F1) e "Do
     * aparelho" (tablet) / "Do HD" (TV, D14), em fileiras que cabem na coluna. Tocar numa opção
     * a traz para o destaque; {@code focar} (TV) devolve o foco à mesma posição da fileira.
     */
    private void montarOpcoes(LinearLayout alvo, List<Integer> cores, boolean ehCapa, int focar) {
        alvo.removeAllViews();
        Resources res = getResources();
        int espaco = px(R.dimen.st_tv_editar_opcao_espaco, R.dimen.st_tablet_editar_opcao_espaco);
        int escolhida = ehCapa ? capa : fundo;
        boolean mudou = escolhida != (ehCapa ? capaSalva : fundoSalvo);

        LinearLayout destaque = new LinearLayout(this);
        destaque.setOrientation(LinearLayout.HORIZONTAL);
        destaque.setGravity(Gravity.CENTER_VERTICAL);
        FrameLayout previa = bloco(escolhida, ehCapa, true);
        destaque.addView(previa, new LinearLayout.LayoutParams(
                ehCapa ? px(R.dimen.st_tv_editar_previa_capa_largura, R.dimen.st_tablet_editar_previa_capa_largura)
                        : px(R.dimen.st_tv_editar_previa_fundo_largura, R.dimen.st_tablet_editar_previa_fundo_largura),
                ehCapa ? px(R.dimen.st_tv_editar_previa_capa_altura, R.dimen.st_tablet_editar_previa_capa_altura)
                        : px(R.dimen.st_tv_editar_previa_fundo_altura, R.dimen.st_tablet_editar_previa_fundo_altura)));
        LinearLayout textos = new LinearLayout(this);
        textos.setOrientation(LinearLayout.VERTICAL);
        TextView nome = texto(ehCapa ? (mudou ? R.string.st_capa_nova : R.string.st_capa_atual)
                : (mudou ? R.string.st_fundo_novo : R.string.st_fundo_atual),
                R.font.st_inter_extrabold, R.color.st_texto, res.getDimension(tv ? R.dimen.st_tv_texto_campo : R.dimen.st_tablet_texto_campo));
        textos.addView(nome);
        previa.setContentDescription(nome.getText());
        TextView regra = texto(ehCapa ? R.string.st_regra_capa : R.string.st_regra_fundo, R.font.st_inter_semibold,
                R.color.st_texto_apagado, res.getDimension(tv ? R.dimen.st_tv_texto_editar_dica : R.dimen.st_tablet_texto_editar_dica));
        LinearLayout.LayoutParams lpRegra = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpRegra.topMargin = espaco / 2;
        textos.addView(regra, lpRegra);
        LinearLayout.LayoutParams lpTextos = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lpTextos.setMarginStart(espaco + espaco / 2);
        destaque.addView(textos, lpTextos);
        alvo.addView(destaque);

        TextView outras = texto(R.string.st_outras_opcoes, R.font.st_inter_semibold, R.color.st_texto_apagado,
                res.getDimension(tv ? R.dimen.st_tv_texto_editar_dica : R.dimen.st_tablet_texto_editar_dica));
        LinearLayout.LayoutParams lpOutras = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpOutras.topMargin = espaco + espaco / 2;
        alvo.addView(outras, lpOutras);

        List<View> itens = new ArrayList<>();
        int numero = 0;
        for (int cor : cores) {
            if (cor == escolhida) continue;
            final int posicao = itens.size();
            FrameLayout opcao = bloco(cor, ehCapa, false);
            opcao.setFocusable(true);
            opcao.setClickable(true);
            opcao.setContentDescription(getString(R.string.st_opcao_imagem, ++numero));
            opcao.setOnClickListener(v -> {
                if (ehCapa) capa = cor;
                else fundo = cor;
                montarOpcoes(alvo, cores, ehCapa, posicao);
            });
            itens.add(opcao);
        }
        itens.add(doAparelho());
        int porFileira = fileiras(alvo, itens,
                ehCapa ? px(R.dimen.st_tv_editar_capa_largura, R.dimen.st_tablet_editar_capa_largura)
                        : px(R.dimen.st_tv_editar_fundo_largura, R.dimen.st_tablet_editar_fundo_largura),
                ehCapa ? px(R.dimen.st_tv_editar_capa_altura, R.dimen.st_tablet_editar_capa_altura)
                        : px(R.dimen.st_tv_editar_fundo_altura, R.dimen.st_tablet_editar_fundo_altura),
                espaco);
        List<View> opcoes = ehCapa ? opcoesCapa : opcoesFundo;
        opcoes.clear();
        opcoes.addAll(itens);
        if (ehCapa) porFileiraCapa = porFileira;
        else porFileiraFundo = porFileira;
        ligarFocoTv();
        if (tv && focar >= 0) itens.get(Math.min(focar, itens.size() - 1)).requestFocus();
    }

    /** Bloco de cor da opção (F1), com o ícone de imagem; a escolhida com anel azul e ✓. */
    private FrameLayout bloco(int cor, boolean ehCapa, boolean escolhida) {
        int selo = px(R.dimen.st_tv_editar_selo, R.dimen.st_tablet_editar_selo);
        float canto = getResources().getDimension(tv ? R.dimen.st_tv_botao_canto : R.dimen.st_tablet_botao_canto);
        FrameLayout opcao = new FrameLayout(this);
        opcao.setBackground(CapaAdapter.blocoDeCor(this, cor, canto, tv));
        opcao.setForeground(ContextCompat.getDrawable(this, R.drawable.st_opcao_imagem));
        opcao.setSelected(escolhida);

        ImageView imagem = new ImageView(this);
        imagem.setImageResource(R.drawable.st_ic_imagem);
        ImageViewCompat.setImageTintList(imagem, ContextCompat.getColorStateList(this, R.color.st_branco_35));
        FrameLayout.LayoutParams lpImagem = new FrameLayout.LayoutParams(selo, selo,
                ehCapa ? Gravity.TOP | Gravity.END : Gravity.CENTER);
        lpImagem.setMargins(selo / 3, selo / 3, selo / 3, selo / 3);
        opcao.addView(imagem, lpImagem);

        if (escolhida) {
            ImageView marca = new ImageView(this);
            marca.setImageResource(R.drawable.st_ic_check);
            marca.setBackgroundResource(R.drawable.st_selo_escolhida);
            ImageViewCompat.setImageTintList(marca, ContextCompat.getColorStateList(this, R.color.st_texto));
            marca.setPadding(selo / 5, selo / 5, selo / 5, selo / 5);
            FrameLayout.LayoutParams lpMarca = new FrameLayout.LayoutParams(selo, selo, Gravity.TOP | Gravity.START);
            lpMarca.setMargins(selo / 3, selo / 3, 0, 0);
            opcao.addView(marca, lpMarca);
        }
        return opcao;
    }

    /** "Do aparelho" (tablet) / "Do HD" (TV) no fim das opções, ícone e nome centrados juntos (D14; navegador de pastas na fase H). */
    private View doAparelho() {
        int selo = px(R.dimen.st_tv_editar_selo, R.dimen.st_tablet_editar_selo);
        LinearLayout botao = new LinearLayout(this);
        botao.setOrientation(LinearLayout.VERTICAL);
        botao.setGravity(Gravity.CENTER);
        botao.setBackgroundResource(R.drawable.st_botao_secundario);
        botao.setFocusable(true);
        botao.setClickable(true);
        if (tv) botao.setForeground(ContextCompat.getDrawable(this, R.drawable.st_foco_tv));
        botao.setOnClickListener(v -> Navegacao.abrirImagemDoAparelho(this, id));

        ImageView icone = new ImageView(this);
        icone.setImageResource(R.drawable.st_ic_pastas);
        ImageViewCompat.setImageTintList(icone, ContextCompat.getColorStateList(this, R.color.st_texto));
        botao.addView(icone, new LinearLayout.LayoutParams(selo, selo));
        TextView nome = texto(tv ? R.string.st_do_hd : R.string.st_do_aparelho, R.font.st_inter_extrabold, R.color.st_texto_secundario,
                getResources().getDimension(tv ? R.dimen.st_tv_texto_editar_dica : R.dimen.st_tablet_texto_editar_dica));
        nome.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = px(R.dimen.st_tv_editar_opcao_espaco, R.dimen.st_tablet_editar_opcao_espaco) / 2;
        botao.addView(nome, lp);
        botao.setContentDescription(nome.getText());
        return botao;
    }

    /**
     * TV: com a escolhida em destaque, as fileiras de opções ficam mais baixas que os campos ao
     * lado e o foco pularia para eles; ↑ / ↓ andam Voltar → opções da capa → opções do fundo.
     */
    private void ligarFocoTv() {
        if (!tv || opcoesCapa.isEmpty() || opcoesFundo.isEmpty()) return;
        View voltar = findViewById(R.id.st_editar_voltar);
        voltar.setNextFocusDownId(idDe(opcoesCapa.get(0)));
        int inicioUltima = (opcoesCapa.size() - 1) / porFileiraCapa * porFileiraCapa;
        for (int i = 0; i < opcoesCapa.size(); i++) {
            View opcao = opcoesCapa.get(i);
            if (i < porFileiraCapa) opcao.setNextFocusUpId(voltar.getId());
            if (i >= inicioUltima) opcao.setNextFocusDownId(idDe(opcoesFundo.get(Math.min(i - inicioUltima, opcoesFundo.size() - 1))));
        }
        for (int i = 0; i < Math.min(porFileiraFundo, opcoesFundo.size()); i++) {
            opcoesFundo.get(i).setNextFocusUpId(idDe(opcoesCapa.get(Math.min(inicioUltima + i, opcoesCapa.size() - 1))));
        }
    }

    private static int idDe(View view) {
        if (view.getId() == View.NO_ID) view.setId(View.generateViewId());
        return view.getId();
    }

    /**
     * Opções no tamanho fixo, quantas couberem por fileira na coluna (nada passa para baixo dos
     * campos). Devolve quantas couberam por fileira.
     */
    private int fileiras(LinearLayout alvo, List<View> itens, int largura, int altura, int espaco) {
        int coluna = px(R.dimen.st_tv_editar_coluna, R.dimen.st_tablet_editar_coluna);
        int porFileira = Math.max(1, (coluna + espaco) / (largura + espaco));
        LinearLayout fileira = null;
        for (int i = 0; i < itens.size(); i++) {
            if (i % porFileira == 0) {
                fileira = new LinearLayout(this);
                fileira.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.topMargin = espaco;
                alvo.addView(fileira, lp);
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(largura, altura);
            if (i % porFileira > 0) lp.setMarginStart(espaco);
            fileira.addView(itens.get(i), lp);
        }
        return porFileira;
    }

    private TextView texto(@StringRes int texto, @FontRes int fonte, @ColorRes int cor, float tamanho) {
        TextView view = new TextView(this);
        view.setText(texto);
        view.setTypeface(ResourcesCompat.getFont(this, fonte));
        view.setTextColor(ContextCompat.getColor(this, cor));
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, tamanho);
        return view;
    }

    private int px(@DimenRes int naTv, @DimenRes int noTablet) {
        return getResources().getDimensionPixelSize(tv ? naTv : noTablet);
    }
}
