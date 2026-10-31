// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.leanback.widget.VerticalGridView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.archos.mediacenter.video.R;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import br.com.starktechstudio.mediaplayer.dados.Modelo.Tipo;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Titulo;
import br.com.starktechstudio.mediaplayer.dados.RepositorioBiblioteca;

/**
 * Abas Filmes, Séries e Shows (D9): grade de capas 2:3 no padrão do Menu da capa
 * (tv-menu-capa.dc.html), sem filtros. TV: grade Leanback com foco pelo controle, em ordem
 * alfabética do título; tablet: grade por toque. As colunas saem da largura disponível.
 */
public class ListaFragment extends Fragment {

    private static final String ARG_TIPO = "tipo";

    private CapaAdapter adapter;
    private Tipo tipo;

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
        boolean tv = Aparelho.ehTv(requireContext());
        return inflater.inflate(tv ? R.layout.st_tv_lista : R.layout.st_tablet_lista, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View raiz, @Nullable Bundle savedInstanceState) {
        boolean tv = Aparelho.ehTv(requireContext());
        Bundle args = getArguments();
        tipo = Tipo.valueOf(args == null ? Tipo.FILME.name() : args.getString(ARG_TIPO, Tipo.FILME.name()));

        TextView titulo = raiz.findViewById(R.id.st_lista_titulo);
        titulo.setText(tipo == Tipo.SERIE ? R.string.st_aba_series
                : tipo == Tipo.SHOW ? R.string.st_aba_shows : R.string.st_aba_filmes);

        adapter = new CapaAdapter(requireActivity(), tv);
        if (tv) configurarTv(raiz.findViewById(R.id.st_lista_grade));
        else configurarTablet(raiz.findViewById(R.id.st_lista_grade));
    }

    @Override
    public void onResume() {
        super.onResume();
        // Volta de outra tela: estado simulado (assistido, capa, título) pode ter mudado
        List<Titulo> titulos = new ArrayList<>(RepositorioBiblioteca.atual(requireContext()).titulos(tipo));
        if (Aparelho.ehTv(requireContext())) {
            // TV: ordem alfabética do título (acento e maiúscula não contam)
            Collator ordem = Collator.getInstance(Locale.forLanguageTag("pt-BR"));
            ordem.setStrength(Collator.PRIMARY);
            Collections.sort(titulos, (a, b) -> ordem.compare(
                    a.titulo == null ? "" : a.titulo, b.titulo == null ? "" : b.titulo));
        }
        adapter.atualizar(titulos);
    }

    /** TV: capa 98 dp, 14 dp entre colunas (28 px); colunas pela largura da tela. */
    private void configurarTv(VerticalGridView grade) {
        int capa = getResources().getDimensionPixelSize(R.dimen.st_tv_capa_largura);
        int espaco = getResources().getDimensionPixelSize(R.dimen.st_tv_capa_espaco);
        int largura = getResources().getDisplayMetrics().widthPixels
                - grade.getPaddingStart() - grade.getPaddingEnd();
        grade.setNumColumns(colunas(largura, capa, espaco));
        grade.setColumnWidth(capa);
        grade.setHorizontalSpacing(espaco);
        grade.setVerticalSpacing(getResources().getDimensionPixelSize(R.dimen.st_tv_lista_espaco_vertical));
        grade.setAdapter(adapter);
    }

    /** Tablet: capa 196 dp, 22 dp de espaço; colunas pela largura da área de conteúdo. */
    private void configurarTablet(RecyclerView grade) {
        final int capa = getResources().getDimensionPixelSize(R.dimen.st_tablet_capa_largura);
        final int espaco = getResources().getDimensionPixelSize(R.dimen.st_tablet_capa_espaco);
        final GridLayoutManager gerente = new GridLayoutManager(requireContext(), 5);
        grade.setLayoutManager(gerente);
        grade.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect saida, @NonNull View item, @NonNull RecyclerView pai,
                                       @NonNull RecyclerView.State estado) {
                saida.set(0, 0, 0, espaco);
            }
        });
        grade.addOnLayoutChangeListener((v, esq, topo, dir, base, esqAntes, topoAntes, dirAntes, baseAntes) -> {
            if (dir - esq == dirAntes - esqAntes) return;
            int novas = colunas(dir - esq - v.getPaddingStart() - v.getPaddingEnd(), capa, espaco);
            if (novas != gerente.getSpanCount()) v.post(() -> gerente.setSpanCount(novas));
        });
        grade.setAdapter(adapter);
        // A grade passa por trás da barra de navegação do sistema
        BordasSistema.aplicar(grade, false, false, false, true);
    }

    private static int colunas(int largura, int capa, int espaco) {
        return Math.max(1, (largura + espaco) / (capa + espaco));
    }
}
