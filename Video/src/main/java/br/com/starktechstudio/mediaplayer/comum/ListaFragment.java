// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.graphics.Rect;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.leanback.widget.VerticalGridView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.archos.mediacenter.video.R;

import java.util.List;

import br.com.starktechstudio.mediaplayer.dados.Modelo;
import br.com.starktechstudio.mediaplayer.dados.Modelo.SecaoShows;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Tipo;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Titulo;
import br.com.starktechstudio.mediaplayer.dados.RepositorioBiblioteca;

/**
 * Abas Filmes, Séries, Desenhos, Shows e Stand-up (D9; Desenhos e Stand-up na F3): grade de capas 2:3 no padrão do Menu da capa
 * (tv-menu-capa.dc.html), em ordem alfabética do título, com o filtro "Só não assistidos" no topo,
 * que vale para todas as abas. TV: grade Leanback com foco pelo controle; tablet: grade por toque.
 * As colunas saem da largura disponível. Sem títulos do tipo, o aviso ({@link AvisoVazio}). Filmes
 * lista os filmes e as coletâneas (D5); Shows lista os artistas e, com "por grupo" ligado em
 * Configurações, em seções por subpasta (D7).
 */
public class ListaFragment extends Fragment {

    private static final String ARG_TIPO = "tipo";
    /** Capas por linha na TV. */
    private static final int COLUNAS_TV = 6;

    private CapaAdapter adapter;
    private SecoesShowsAdapter secoes;
    private RecyclerView grade;
    /** A grade está nas seções por grupo (a preferência pode mudar com a aba aberta, na TV). */
    private boolean porGrupo;
    private boolean tv;
    private int capaTv;
    private int espacoTv;
    private GridLayoutManager gerenteTablet;
    private int colunasTablet = 5;
    private Tipo tipo;
    private TextView vazio;
    private Button filtro;
    private IndicadorAtualizacao indicador;
    private final Runnable aoMudarBiblioteca = this::recarregar;

    public static ListaFragment novo(Tipo tipo) {
        ListaFragment fragmento = new ListaFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TIPO, tipo.name());
        fragmento.setArguments(args);
        return fragmento;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(Aparelho.ehTv(requireContext()) ? R.layout.st_tv_lista : R.layout.st_tablet_lista, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View raiz, @Nullable Bundle savedInstanceState) {
        tv = Aparelho.ehTv(requireContext());
        Bundle args = getArguments();
        tipo = Tipo.valueOf(args == null ? Tipo.FILME.name() : args.getString(ARG_TIPO, Tipo.FILME.name()));

        TextView titulo = raiz.findViewById(R.id.st_lista_titulo);
        titulo.setText(tipo == Tipo.SERIE ? R.string.st_aba_series
                : tipo == Tipo.DESENHO ? R.string.st_aba_desenhos
                : tipo == Tipo.SHOW ? R.string.st_aba_shows : tipo == Tipo.STANDUP ? R.string.st_aba_standup
                : R.string.st_aba_filmes);
        // Título com mais destaque (o mesmo de Pastas e Configurações) e, do lado oposto, o andamento
        // da atualização da biblioteca (ou ✓ verde)
        titulo.setTextSize(TypedValue.COMPLEX_UNIT_PX, Medidas.px(requireContext(),
                tv ? Medidas.TITULO_PAGINA_TV : Medidas.TITULO_PAGINA_TABLET));
        ViewGroup topo = (ViewGroup) titulo.getParent();
        int posicao = topo.indexOfChild(titulo);
        LinearLayout.LayoutParams lpTitulo = (LinearLayout.LayoutParams) titulo.getLayoutParams();
        topo.removeView(titulo);
        LinearLayout linha = new LinearLayout(requireContext());
        linha.setOrientation(LinearLayout.HORIZONTAL);
        linha.setGravity(Gravity.CENTER_VERTICAL);
        linha.addView(titulo, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        filtro = (Button) getLayoutInflater().inflate(tv ? R.layout.st_tv_filtro_lista : R.layout.st_tablet_filtro_lista, linha, false);
        filtro.setOnClickListener(v -> {
            Preferencias.definirSoNaoAssistidos(v.getContext(), !Preferencias.soNaoAssistidos(v.getContext()));
            recarregar();
        });
        // TV: saiu dele, volta a ficar sem foco ({@link #subirParaFiltro})
        if (tv) filtro.setOnFocusChangeListener((v, temFoco) -> {
            if (!temFoco) v.post(() -> v.setFocusable(false));
        });
        linha.addView(filtro);
        indicador = new IndicadorAtualizacao(requireContext(), RepositorioBiblioteca.atual(requireContext()));
        LinearLayout.LayoutParams lpIndicador = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpIndicador.setMarginStart(getResources().getDimensionPixelSize(tv ? R.dimen.st_tv_capa_espaco : R.dimen.st_tablet_capa_espaco));
        linha.addView(indicador.view, lpIndicador);
        LinearLayout.LayoutParams lpLinha = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpLinha.setMargins(lpTitulo.leftMargin, lpTitulo.topMargin, lpTitulo.rightMargin, lpTitulo.bottomMargin);
        lpLinha.setMarginStart(lpTitulo.getMarginStart());
        lpLinha.setMarginEnd(lpTitulo.getMarginEnd());
        topo.addView(linha, posicao, lpLinha);

        // Nenhum título do tipo: o aviso embaixo do título da aba
        vazio = AvisoVazio.criar(requireContext());
        ((ViewGroup) raiz).addView(vazio, 1, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        adapter = new CapaAdapter(requireActivity(), tv);
        secoes = new SecoesShowsAdapter(requireActivity(), tv);
        grade = raiz.findViewById(R.id.st_lista_grade);
        if (tv) configurarTv((VerticalGridView) grade);
        else configurarTablet(grade);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Volta de outra tela (assistido, capa, título) ou a varredura / o TMDB mudaram a biblioteca
        recarregar();
        RepositorioBiblioteca.atual(requireContext()).ouvir(aoMudarBiblioteca);
        indicador.ligar();
    }

    @Override
    public void onPause() {
        RepositorioBiblioteca.atual(requireContext()).deixarDeOuvir(aoMudarBiblioteca);
        indicador.desligar();
        super.onPause();
    }

    /** Em ordem alfabética do título (acento e maiúscula não contam), já vem assim do repositório. */
    private void recarregar() {
        if (getView() == null) return;
        RepositorioBiblioteca repositorio = RepositorioBiblioteca.atual(requireContext());
        boolean grupos = tipo == Tipo.SHOW && Preferencias.showsPorGrupo(requireContext());
        boolean soNaoAssistidos = Preferencias.soNaoAssistidos(requireContext());
        trocarGrade(grupos);
        boolean nada;
        boolean temTitulos;
        if (grupos) {
            List<SecaoShows> todas = repositorio.gruposDeShows();
            List<SecaoShows> lista = soNaoAssistidos ? Modelo.secoesNaoAssistidas(todas) : todas;
            secoes.atualizar(lista);
            temTitulos = !todas.isEmpty();
            nada = lista.isEmpty();
        } else {
            List<Titulo> todos = tipo == Tipo.FILME ? repositorio.filmesEColetaneas()
                    : tipo == Tipo.SHOW ? repositorio.artistas() : repositorio.titulos(tipo);
            List<Titulo> titulos = soNaoAssistidos ? Modelo.naoAssistidos(todos) : todos;
            adapter.atualizar(titulos);
            temTitulos = !todos.isEmpty();
            nada = titulos.isEmpty();
        }
        mostrarFiltro(soNaoAssistidos, temTitulos);
        vazio.setVisibility(nada ? View.VISIBLE : View.GONE);
        if (nada && temTitulos) {
            vazio.setText(R.string.st_lista_tudo_assistido);
        } else if (nada) {
            vazio.setText(AvisoVazio.texto(requireContext(), repositorio, tipo == Tipo.SERIE ? R.string.st_lista_vazia_series
                    : tipo == Tipo.DESENHO ? R.string.st_lista_vazia_desenhos
                    : tipo == Tipo.SHOW ? R.string.st_lista_vazia_shows : tipo == Tipo.STANDUP ? R.string.st_lista_vazia_standup
                    : R.string.st_lista_vazia_filmes));
        }
    }

    /** Só com títulos na aba; ligado: em azul e com ✓. */
    private void mostrarFiltro(boolean ligado, boolean temTitulos) {
        filtro.setVisibility(temTitulos ? View.VISIBLE : View.GONE);
        filtro.setSelected(ligado);
        filtro.setTextColor(ContextCompat.getColor(requireContext(), ligado ? R.color.st_azul : R.color.st_texto_secundario));
        if (ligado) {
            int icone = Math.round(filtro.getTextSize());
            Icones.noTexto(filtro, R.drawable.st_ic_check, icone);
        } else {
            filtro.setCompoundDrawablesRelative(null, null, null, null);
        }
    }

    /**
     * TV, ↑ na fileira de cima: só a capa embaixo do botão "Só não assistidos" sobe para ele. Devolve
     * false para as outras, e aí o foco vai ao menu do topo.
     */
    public boolean subirParaFiltro(View foco) {
        if (filtro == null || foco == filtro || filtro.getVisibility() != View.VISIBLE) return false;
        Rect capa = new Rect();
        Rect botao = new Rect();
        foco.getGlobalVisibleRect(capa);
        filtro.getGlobalVisibleRect(botao);
        if (capa.right <= botao.left || capa.left >= botao.right) return false;
        filtro.setFocusable(true);
        return filtro.requestFocus();
    }

    /**
     * Grade de capas ou seções por grupo. Nas seções, cada item da grade é uma linha inteira, com as
     * capas dentro ({@link SecoesShowsAdapter}): a grade fica com uma coluna só.
     */
    private void trocarGrade(boolean grupos) {
        if (grupos == porGrupo) return;
        porGrupo = grupos;
        if (tv) {
            VerticalGridView gradeTv = (VerticalGridView) grade;
            gradeTv.setNumColumns(grupos ? 1 : COLUNAS_TV);
            // Largura 0 = a coluna ocupa a grade inteira
            gradeTv.setColumnWidth(grupos ? 0 : capaTv);
            gradeTv.setHorizontalSpacing(grupos ? 0 : espacoTv);
        } else {
            gerenteTablet.setSpanCount(grupos ? 1 : colunasTablet);
        }
        grade.setAdapter(grupos ? secoes : adapter);
    }

    /**
     * TV: 6 capas por linha, 14 dp entre elas (28 px), esticadas até a margem direita (sem sobra);
     * a altura segue o 2:3 da capa.
     */
    private void configurarTv(VerticalGridView grade) {
        espacoTv = getResources().getDimensionPixelSize(R.dimen.st_tv_capa_espaco);
        int largura = getResources().getDisplayMetrics().widthPixels
                - grade.getPaddingStart() - grade.getPaddingEnd();
        capaTv = (largura - espacoTv * (COLUNAS_TV - 1)) / COLUNAS_TV;
        adapter.definirTamanho(capaTv, capaTv * 3 / 2);
        secoes.definirTamanho(capaTv, capaTv * 3 / 2);
        secoes.definirGrade(COLUNAS_TV, espacoTv);
        grade.setNumColumns(COLUNAS_TV);
        grade.setColumnWidth(capaTv);
        grade.setHorizontalSpacing(espacoTv);
        grade.setVerticalSpacing(getResources().getDimensionPixelSize(R.dimen.st_tv_lista_espaco_vertical));
        grade.setAdapter(adapter);
    }

    /** Tablet: capa 196 dp, 22 dp de espaço; colunas pela largura da área de conteúdo. */
    private void configurarTablet(RecyclerView grade) {
        final int capa = getResources().getDimensionPixelSize(R.dimen.st_tablet_capa_largura);
        final int espaco = getResources().getDimensionPixelSize(R.dimen.st_tablet_capa_espaco);
        gerenteTablet = new GridLayoutManager(requireContext(), colunasTablet);
        secoes.definirGrade(colunasTablet, espaco);
        grade.setLayoutManager(gerenteTablet);
        grade.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect saida, @NonNull View item, @NonNull RecyclerView pai,
                                       @NonNull RecyclerView.State estado) {
                saida.set(0, 0, 0, espaco);
            }
        });
        grade.addOnLayoutChangeListener((v, esq, topo, dir, base, esqAntes, topoAntes, dirAntes, baseAntes) -> {
            if (dir - esq == dirAntes - esqAntes) return;
            int util = dir - esq - v.getPaddingStart() - v.getPaddingEnd();
            int novas = LinhasDeCapas.colunas(util, capa, espaco);
            colunasTablet = novas;
            v.post(() -> {
                // Nas seções, como na grade: cada capa no começo da sua coluna
                secoes.definirGrade(novas, util / novas - capa);
                if (!porGrupo && novas != gerenteTablet.getSpanCount()) gerenteTablet.setSpanCount(novas);
            });
        });
        grade.setAdapter(adapter);
        // A grade passa por trás da barra de navegação do sistema
        BordasSistema.aplicar(grade, false, false, false, true);
    }
}
