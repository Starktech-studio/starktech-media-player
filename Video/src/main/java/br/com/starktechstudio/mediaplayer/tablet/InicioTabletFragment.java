// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.tablet;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.archos.mediacenter.video.R;

import java.util.ArrayList;
import java.util.List;

import br.com.starktechstudio.mediaplayer.comum.BordasSistema;
import br.com.starktechstudio.mediaplayer.comum.CapaAdapter;
import br.com.starktechstudio.mediaplayer.comum.CartaoAdapter;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Titulo;
import br.com.starktechstudio.mediaplayer.dados.RepositorioBiblioteca;

/**
 * Início do tablet (tablet-biblioteca.dc.html): Continuar assistindo, com rolagem lateral, e
 * Adicionados recentemente em páginas de 5 capas — arrastar traz mais 5 ({@link PagerSnapHelper}),
 * com pontos de página. Sem Favoritos, como no desenho (D12). A marca e o selo do HD ficam na
 * {@link PrincipalTabletActivity}.
 */
public class InicioTabletFragment extends Fragment {

    /** Capas por página (5 por vez, como no desenho). */
    private static final int POR_PAGINA = 5;

    private RepositorioBiblioteca repositorio;
    private View secaoContinuar;
    private View secaoAdicionados;
    private CartaoAdapter continuar;
    private PaginaAdapter paginas;
    private RecyclerView listaPaginas;
    private LinearLayoutManager gerentePaginas;
    private PagerSnapHelper encaixe;
    private LinearLayout pontos;
    private int paginaAtual;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.st_tablet_inicio, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View raiz, @Nullable Bundle savedInstanceState) {
        repositorio = RepositorioBiblioteca.atual(requireContext());
        secaoContinuar = raiz.findViewById(R.id.st_secao_continuar);
        secaoAdicionados = raiz.findViewById(R.id.st_secao_adicionados);
        pontos = raiz.findViewById(R.id.st_adicionados_pontos);
        // A rolagem passa por trás da barra de navegação do sistema
        BordasSistema.aplicar(raiz, false, false, false, true);

        // Continuar assistindo: 22 dp entre os cartões
        RecyclerView fileira = raiz.findViewById(R.id.st_fileira_continuar);
        fileira.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        final int espaco = getResources().getDimensionPixelSize(R.dimen.st_tablet_cartao_espaco);
        fileira.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect saida, @NonNull View item, @NonNull RecyclerView pai,
                                       @NonNull RecyclerView.State estado) {
                saida.set(pai.getChildAdapterPosition(item) > 0 ? espaco : 0, 0, 0, 0);
            }
        });
        continuar = new CartaoAdapter(requireActivity(), repositorio, false, CartaoAdapter.Modo.CONTINUAR);
        fileira.setAdapter(continuar);

        // Adicionados recentemente: uma página = 5 capas; o encaixe para em página inteira
        listaPaginas = raiz.findViewById(R.id.st_paginas_adicionados);
        gerentePaginas = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        listaPaginas.setLayoutManager(gerentePaginas);
        paginas = new PaginaAdapter(requireActivity());
        listaPaginas.setAdapter(paginas);
        encaixe = new PagerSnapHelper();
        encaixe.attachToRecyclerView(listaPaginas);
        listaPaginas.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView lista, int estado) {
                if (estado != RecyclerView.SCROLL_STATE_IDLE) return;
                View pagina = encaixe.findSnapView(gerentePaginas);
                if (pagina != null) marcarPagina(gerentePaginas.getPosition(pagina));
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Volta de outra tela: o estado simulado (assistido, posição) pode ter mudado
        List<Titulo> lista = repositorio.continuarAssistindo();
        continuar.atualizar(lista);
        secaoContinuar.setVisibility(lista.isEmpty() ? View.GONE : View.VISIBLE);

        List<Titulo> adicionados = repositorio.adicionadosRecentemente();
        paginas.atualizar(adicionados);
        secaoAdicionados.setVisibility(adicionados.isEmpty() ? View.GONE : View.VISIBLE);
        criarPontos(paginas.getItemCount());
        marcarPagina(Math.min(paginaAtual, Math.max(0, paginas.getItemCount() - 1)));
    }

    /** Pontos de página: ativo 28 x 8 dp branco; os outros 8 x 8 dp branco 35 %; 6 dp entre eles. */
    private void criarPontos(int quantidade) {
        if (pontos.getChildCount() == quantidade) return;
        pontos.removeAllViews();
        if (quantidade < 2) return;
        Resources res = getResources();
        int altura = res.getDimensionPixelSize(R.dimen.st_tablet_ponto);
        int margem = res.getDimensionPixelSize(R.dimen.st_tablet_ponto_espaco);
        for (int i = 0; i < quantidade; i++) {
            View ponto = new View(requireContext());
            ponto.setBackgroundResource(R.drawable.st_ponto);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(altura, altura);
            if (i > 0) lp.setMarginStart(margem);
            pontos.addView(ponto, lp);
        }
    }

    private void marcarPagina(int pagina) {
        paginaAtual = pagina;
        int altura = getResources().getDimensionPixelSize(R.dimen.st_tablet_ponto);
        int ativo = getResources().getDimensionPixelSize(R.dimen.st_tablet_ponto_ativo);
        for (int i = 0; i < pontos.getChildCount(); i++) {
            View ponto = pontos.getChildAt(i);
            ponto.setSelected(i == pagina);
            ViewGroup.LayoutParams lp = ponto.getLayoutParams();
            int largura = i == pagina ? ativo : altura;
            if (lp.width != largura) {
                lp.width = largura;
                ponto.setLayoutParams(lp);
            }
        }
    }

    /** Páginas de até 5 capas, com 22 dp entre elas; cada página ocupa a largura da lista. */
    private static final class PaginaAdapter extends RecyclerView.Adapter<PaginaAdapter.Pagina> {

        private final Activity atividade;
        private final List<List<Titulo>> grupos = new ArrayList<>();

        PaginaAdapter(Activity atividade) {
            this.atividade = atividade;
        }

        void atualizar(List<Titulo> titulos) {
            grupos.clear();
            for (int i = 0; i < titulos.size(); i += POR_PAGINA) {
                grupos.add(new ArrayList<>(titulos.subList(i, Math.min(titulos.size(), i + POR_PAGINA))));
            }
            notifyDataSetChanged();
        }

        @Override
        public int getItemCount() {
            return grupos.size();
        }

        @NonNull
        @Override
        public Pagina onCreateViewHolder(@NonNull ViewGroup pai, int tipo) {
            LinearLayout linha = new LinearLayout(pai.getContext());
            linha.setOrientation(LinearLayout.HORIZONTAL);
            linha.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            int espaco = pai.getResources().getDimensionPixelSize(R.dimen.st_tablet_capa_espaco);
            List<CapaAdapter.Capa> capas = new ArrayList<>();
            for (int i = 0; i < POR_PAGINA; i++) {
                CapaAdapter.Capa capa = CapaAdapter.Capa.criar(atividade, linha, false);
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) capa.itemView.getLayoutParams();
                if (i > 0) lp.setMarginStart(espaco);
                linha.addView(capa.itemView, lp);
                capas.add(capa);
            }
            return new Pagina(linha, capas);
        }

        @Override
        public void onBindViewHolder(@NonNull Pagina pagina, int posicao) {
            List<Titulo> grupo = grupos.get(posicao);
            for (int i = 0; i < pagina.capas.size(); i++) {
                CapaAdapter.Capa capa = pagina.capas.get(i);
                if (i < grupo.size()) {
                    capa.vincular(grupo.get(i));
                    capa.itemView.setVisibility(View.VISIBLE);
                } else {
                    capa.itemView.setVisibility(View.INVISIBLE);
                }
            }
        }

        static final class Pagina extends RecyclerView.ViewHolder {
            final List<CapaAdapter.Capa> capas;

            Pagina(View linha, List<CapaAdapter.Capa> capas) {
                super(linha);
                this.capas = capas;
            }
        }
    }
}
