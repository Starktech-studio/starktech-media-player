// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.content.Intent;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import br.com.starktechstudio.mediaplayer.dados.Modelo.ResultadoTmdb;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Tipo;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Titulo;
import br.com.starktechstudio.mediaplayer.dados.RepositorioBiblioteca;

/**
 * Buscar no TMDB (tv-busca-tmdb.dc.html; tablet sem desenho, D12): janela sobre Editar com o
 * campo de busca, os resultados (o atual com "Atual") e o texto de ajuda. Selecionar troca título,
 * sinopse, capa e fundo e não toca nos campos editados à mão (RepositorioBiblioteca); a tela
 * Editar relê ao voltar. F3: busca de verdade no TMDB (fora da tela), com a capa de cada resultado,
 * 4 por página (Anterior / Próxima: a lista grande cobria o Editar); sem a chave no APK, avisa em
 * vez de buscar. Selecionar devolve o resultado ao Editar ({@link #EXTRA_RESULTADO}): a troca só
 * vale no Salvar de lá. Capa de artista: busca artistas, na biblioteca e no MusicBrainz, pelo nome.
 */
public class BuscaTmdbActivity extends AppCompatActivity {

    /** Resultado escolhido (id da busca), com o título e o ano para o aviso do Editar. */
    public static final String EXTRA_RESULTADO = "resultado_tmdb";
    public static final String EXTRA_RESULTADO_TITULO = "resultado_tmdb_titulo";
    public static final String EXTRA_RESULTADO_ANO = "resultado_tmdb_ano";
    /** Capa de artista: endereço da foto do escolhido; nulo = artista que já está na biblioteca. */
    public static final String EXTRA_RESULTADO_FOTO = "resultado_foto";
    private static final int POR_PAGINA = 4;

    private RepositorioBiblioteca repositorio;
    private String id;
    private boolean tv;
    /** Capa de artista: a mesma janela busca artistas, na biblioteca e no MusicBrainz, e não no TMDB. */
    private boolean deArtista;
    private EditText consulta;
    private LinearLayout resultados;
    /** A busca e a troca usam a rede: fora da tela, uma de cada vez. */
    private final ExecutorService rede = Executors.newSingleThreadExecutor();
    /** Só o resultado da última busca aparece. */
    private int buscaAtual;
    private List<ResultadoTmdb> achados = new ArrayList<>();
    private int pagina;

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

        deArtista = titulo.ehArtista();
        TextView nome = findViewById(R.id.st_tmdb_titulo);
        nome.setText(deArtista ? R.string.st_escolher_artista : titulo.tipo == Tipo.SERIE ? R.string.st_escolher_serie
                : titulo.tipo == Tipo.DESENHO ? R.string.st_escolher_desenho
                : titulo.tipo == Tipo.SHOW ? R.string.st_escolher_show
                : titulo.tipo == Tipo.STANDUP ? R.string.st_escolher_standup : R.string.st_escolher_filme);
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
        if (deArtista) rotulo.setText(R.string.st_buscar_artista);
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
        if (deArtista) ajuda.setText(R.string.st_artista_ajuda);
        ajuda.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(tv ? R.dimen.st_tv_texto_tmdb_linha : R.dimen.st_tablet_texto_tmdb_linha));
        ((ViewGroup.MarginLayoutParams) ajuda.getLayoutParams()).topMargin = espaco;

        buscar();
    }

    @Override
    protected void onDestroy() {
        rede.shutdown();
        super.onDestroy();
    }

    private void buscar() {
        resultados.removeAllViews();
        if (!deArtista && !repositorio.temChaveTmdb()) {
            mensagem(R.string.st_tmdb_sem_chave);
            return;
        }
        mensagem(deArtista ? R.string.st_artista_buscando : R.string.st_tmdb_buscando);
        final int pedido = ++buscaAtual;
        final String texto = consulta.getText().toString();
        rede.execute(() -> {
            List<ResultadoTmdb> lista = deArtista ? repositorio.buscarArtista(id, texto) : repositorio.buscarTmdb(id, texto);
            runOnUiThread(() -> {
                if (pedido != buscaAtual || isFinishing()) return;
                mostrar(lista);
            });
        });
    }

    private void mostrar(List<ResultadoTmdb> lista) {
        resultados.removeAllViews();
        if (lista.isEmpty()) {
            mensagem(deArtista ? R.string.st_artista_nada : R.string.st_tmdb_nada);
            return;
        }
        achados = lista;
        // Abre na página do resultado atual (o ligado ao título hoje)
        pagina = 0;
        for (int i = 0; i < lista.size(); i++) {
            if (lista.get(i).atual) pagina = i / POR_PAGINA;
        }
        mostrarPagina(true);
    }

    /** Até 4 resultados e, com mais de uma página, Anterior · "1 de 3" · Próxima embaixo. */
    private void mostrarPagina(boolean focarResultado) {
        resultados.removeAllViews();
        int paginas = (achados.size() + POR_PAGINA - 1) / POR_PAGINA;
        int inicio = pagina * POR_PAGINA;
        View primeiro = null;
        for (int i = inicio; i < Math.min(achados.size(), inicio + POR_PAGINA); i++) {
            View item = resultado(achados.get(i));
            if (primeiro == null) primeiro = item;
            resultados.addView(item);
        }
        View navegacao = paginas > 1 ? navegacao(paginas) : null;
        if (navegacao != null) resultados.addView(navegacao);
        if (tv && focarResultado && primeiro != null) primeiro.requestFocus();
    }

    private View navegacao(int paginas) {
        Resources res = getResources();
        int padding = res.getDimensionPixelSize(tv ? R.dimen.st_tv_editar_opcao_espaco : R.dimen.st_tablet_editar_opcao_espaco);
        LinearLayout linha = new LinearLayout(this);
        linha.setOrientation(LinearLayout.HORIZONTAL);
        linha.setGravity(Gravity.CENTER_VERTICAL);
        TextView anterior = botaoDePagina(getString(R.string.st_pagina_anterior), pagina > 0, () -> {
            pagina--;
            mostrarPagina(false);
            focarNavegacao(true);
        });
        linha.addView(anterior);
        TextView conta = texto(getString(R.string.st_pagina_de, pagina + 1, paginas), R.font.st_inter_semibold,
                tv ? R.dimen.st_tv_texto_tmdb_linha : R.dimen.st_tablet_texto_tmdb_linha, R.color.st_texto_apagado);
        conta.setGravity(Gravity.CENTER);
        linha.addView(conta, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView proxima = botaoDePagina(getString(R.string.st_pagina_proxima), pagina < paginas - 1, () -> {
            pagina++;
            mostrarPagina(false);
            focarNavegacao(false);
        });
        linha.addView(proxima);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = padding;
        linha.setLayoutParams(lp);
        return linha;
    }

    /** TV: depois de trocar de página, o foco fica no mesmo botão (ou no outro, se este apagou). */
    private void focarNavegacao(boolean anterior) {
        if (!tv || resultados.getChildCount() == 0) return;
        View ultima = resultados.getChildAt(resultados.getChildCount() - 1);
        if (!(ultima instanceof LinearLayout)) return;
        LinearLayout linha = (LinearLayout) ultima;
        View alvo = linha.getChildAt(anterior ? 0 : 2);
        if (alvo == null || !alvo.isEnabled()) alvo = linha.getChildAt(anterior ? 2 : 0);
        if (alvo != null && alvo.isEnabled()) alvo.requestFocus();
    }

    private TextView botaoDePagina(String nome, boolean ativo, Runnable acao) {
        Resources res = getResources();
        TextView botao = texto(nome, R.font.st_inter_extrabold, tv ? R.dimen.st_tv_texto_botao : R.dimen.st_tablet_texto_botao,
                R.color.st_texto);
        botao.setBackgroundResource(R.drawable.st_botao_secundario);
        botao.setGravity(Gravity.CENTER);
        int ph = res.getDimensionPixelSize(tv ? R.dimen.st_tv_botao_padding : R.dimen.st_tablet_botao_padding);
        int altura = res.getDimensionPixelSize(tv ? R.dimen.st_tv_botao_altura : R.dimen.st_tablet_botao_altura);
        botao.setPadding(ph, 0, ph, 0);
        botao.setMinHeight(altura);
        botao.setEnabled(ativo);
        botao.setAlpha(ativo ? 1f : 0.35f);
        botao.setFocusable(ativo);
        botao.setClickable(ativo);
        if (tv) botao.setForeground(ContextCompat.getDrawable(this, R.drawable.st_foco_tv));
        botao.setOnClickListener(v -> acao.run());
        return botao;
    }

    /** Selecionar: volta ao Editar com o resultado; os dados só trocam no Salvar de lá. */
    private void selecionar(ResultadoTmdb r) {
        setResult(RESULT_OK, new Intent()
                .putExtra(EXTRA_RESULTADO, r.id)
                .putExtra(EXTRA_RESULTADO_TITULO, r.titulo)
                .putExtra(EXTRA_RESULTADO_ANO, r.ano)
                // A capa de quem está na biblioteca é um arquivo daqui: ela continua valendo
                .putExtra(EXTRA_RESULTADO_FOTO, r.capaEndereco != null && r.capaEndereco.startsWith("https://") ? r.capaEndereco : null));
        finish();
    }

    /** Linha de aviso no lugar dos resultados (buscando, nada achado, sem chave). */
    private void mensagem(int texto) {
        TextView linha = texto(getString(texto), R.font.st_inter_semibold,
                tv ? R.dimen.st_tv_texto_tmdb_linha : R.dimen.st_tablet_texto_tmdb_linha, R.color.st_texto_secundario);
        linha.setSingleLine(false);
        resultados.addView(linha);
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
        item.setOnClickListener(v -> selecionar(r));

        FrameLayout capa = new FrameLayout(this);
        float canto = res.getDimension(tv ? R.dimen.st_tv_botao_canto : R.dimen.st_tablet_botao_canto);
        capa.setBackground(CapaAdapter.blocoDeCor(this, r.corCapa, canto, tv));
        ImageView imagem = new ImageView(this);
        imagem.setImageResource(R.drawable.st_ic_imagem);
        ImageViewCompat.setImageTintList(imagem, ContextCompat.getColorStateList(this, R.color.st_branco_35));
        int icone = res.getDimensionPixelSize(tv ? R.dimen.st_tv_editar_selo : R.dimen.st_tablet_editar_selo);
        FrameLayout.LayoutParams lpImagem = new FrameLayout.LayoutParams(icone, icone, Gravity.TOP | Gravity.END);
        lpImagem.setMargins(0, icone / 3, icone / 3, 0);
        capa.addView(imagem, lpImagem);
        Imagens.doEndereco(capa, r.capaEndereco, Imagens.CAPA, canto, imagem);
        item.addView(capa, new LinearLayout.LayoutParams(
                res.getDimensionPixelSize(tv ? R.dimen.st_tv_tmdb_capa_largura : R.dimen.st_tablet_tmdb_capa_largura),
                res.getDimensionPixelSize(tv ? R.dimen.st_tv_tmdb_capa_altura : R.dimen.st_tablet_tmdb_capa_altura)));

        LinearLayout textos = new LinearLayout(this);
        textos.setOrientation(LinearLayout.VERTICAL);
        TextView nome = texto(r.titulo, R.font.st_inter_extrabold, tv ? R.dimen.st_tv_texto_tmdb_nome : R.dimen.st_tablet_texto_tmdb_nome, R.color.st_texto);
        textos.addView(nome);
        TextView linha = texto(deArtista ? r.sinopse : getString(R.string.st_resultado_linha, r.ano, getString(PecasDetalhe.rotuloTipo(r.tipo))),
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
