// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.tv;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.SuperscriptSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.leanback.widget.HorizontalGridView;
import androidx.leanback.widget.OnChildViewHolderSelectedListener;
import androidx.recyclerview.widget.RecyclerView;

import com.archos.mediacenter.video.R;

import java.util.ArrayList;
import java.util.List;

import br.com.starktechstudio.mediaplayer.comum.AvisoVazio;
import br.com.starktechstudio.mediaplayer.comum.CapaAdapter;
import br.com.starktechstudio.mediaplayer.comum.CartaoAdapter;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Titulo;
import br.com.starktechstudio.mediaplayer.dados.RepositorioBiblioteca;

/**
 * Início da TV (Main.dc.html): destaque "Novidades na biblioteca" em slide ({@link DestaqueTv}),
 * fileiras Continuar assistindo, Favoritos e Filmes adicionados recentemente e o rodapé com o
 * logo StarkTech, a versão (lida do pacote) e o site. Fileira vazia some.
 * Foco: ↓ / ↑ entre o destaque e as fileiras ({@link RolagemTv} sobe a seção em foco); no destaque,
 * o primeiro ↓ recolhe o item e o segundo desce (D2); quando o foco volta à barra do topo, a página
 * volta ao alto. A prévia em vídeo ({@link PreviaVideoTv}) segue o item expandido e se solta com ele.
 */
public class InicioTvFragment extends Fragment {

    private RepositorioBiblioteca repositorio;
    private RolagemTv rolagem;
    private View destaqueView;
    private DestaqueTv destaque;
    private PreviaVideoTv previa;
    private View secaoContinuar;
    private View secaoFavoritos;
    private View secaoFilmes;
    private CartaoAdapter continuar;
    private CartaoAdapter favoritos;
    private CapaAdapter filmes;
    /** Filmes adicionados recentemente: todos, e quantos a fileira mostra (de 6 em 6). */
    private List<Titulo> todosFilmes = new ArrayList<>();
    private int filmesMostrados = FILMES_POR_VEZ;
    private static final int FILMES_POR_VEZ = 6;
    private TextView vazio;
    private final Runnable aoMudarBiblioteca = this::recarregar;

    private final ViewTreeObserver.OnGlobalFocusChangeListener aoMudarFoco = (antes, depois) -> {
        if (destaque == null || depois == null) return;
        destaque.definirFocoDentro(dentroDe(depois, destaqueView));
        if (!dentroDe(depois, rolagem)) rolagem.voltarAoTopo();
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.st_tv_inicio, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View raiz, @Nullable Bundle savedInstanceState) {
        repositorio = RepositorioBiblioteca.atual(requireContext());
        rolagem = raiz.findViewById(R.id.st_inicio_rolagem);
        destaqueView = raiz.findViewById(R.id.st_destaque);
        secaoContinuar = raiz.findViewById(R.id.st_secao_continuar);
        secaoFavoritos = raiz.findViewById(R.id.st_secao_favoritos);
        secaoFilmes = raiz.findViewById(R.id.st_secao_filmes);

        continuar = new CartaoAdapter(requireActivity(), repositorio, true, CartaoAdapter.Modo.CONTINUAR);
        favoritos = new CartaoAdapter(requireActivity(), repositorio, true, CartaoAdapter.Modo.FAVORITOS);
        filmes = new CapaAdapter(requireActivity(), true);
        configurarFileira(raiz.findViewById(R.id.st_fileira_continuar), continuar, R.dimen.st_tv_cartao_espaco);
        configurarFileira(raiz.findViewById(R.id.st_fileira_favoritos), favoritos, R.dimen.st_tv_cartao_espaco);
        HorizontalGridView fileiraFilmes = raiz.findViewById(R.id.st_fileira_filmes);
        configurarFileira(fileiraFilmes, filmes, R.dimen.st_tv_capa_espaco);
        // Foco no último filme da fileira: carrega mais 6
        fileiraFilmes.setOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(RecyclerView pai, RecyclerView.ViewHolder filho, int posicao, int subposicao) {
                if (posicao < filmes.getItemCount() - 1 || filmesMostrados >= todosFilmes.size()) return;
                int antes = filmesMostrados;
                filmesMostrados = Math.min(todosFilmes.size(), filmesMostrados + FILMES_POR_VEZ);
                pai.post(() -> filmes.acrescentar(new ArrayList<>(todosFilmes.subList(antes, filmesMostrados))));
            }
        });

        destaque = new DestaqueTv(requireActivity(), destaqueView, repositorio);
        previa = new PreviaVideoTv(requireActivity(), destaqueView, repositorio);
        destaque.definirExpansao(previa);
        montarRodape(raiz);
        // Biblioteca vazia: o aviso no lugar da página
        vazio = AvisoVazio.criar(requireContext());
        ((ViewGroup) raiz).addView(vazio, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        raiz.getViewTreeObserver().addOnGlobalFocusChangeListener(aoMudarFoco);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Volta de outra tela (favorito, assistido, posição) ou a varredura / o TMDB mudaram a biblioteca
        recarregar();
        repositorio.ouvir(aoMudarBiblioteca);
    }

    private void recarregar() {
        if (getView() == null) return;
        boolean nada = repositorio.vazia();
        rolagem.setVisibility(nada ? View.GONE : View.VISIBLE);
        vazio.setVisibility(nada ? View.VISIBLE : View.GONE);
        if (nada) vazio.setText(AvisoVazio.texto(requireContext(), repositorio, R.string.st_vazio_sem_videos));
        destaque.recarregar();
        atualizar(secaoContinuar, continuar, repositorio.continuarAssistindo());
        atualizar(secaoFavoritos, favoritos, repositorio.favoritos());
        todosFilmes = repositorio.filmesAdicionadosRecentemente();
        filmesMostrados = Math.max(FILMES_POR_VEZ, Math.min(filmesMostrados, todosFilmes.size()));
        filmes.atualizar(todosFilmes.subList(0, Math.min(filmesMostrados, todosFilmes.size())));
        secaoFilmes.setVisibility(filmes.getItemCount() == 0 ? View.GONE : View.VISIBLE);
        atualizarDescida();
        View foco = getView() == null ? null : getView().findFocus();
        destaque.definirFocoDentro(foco != null && dentroDe(foco, destaqueView));
        destaque.retomar();
    }

    @Override
    public void onPause() {
        repositorio.deixarDeOuvir(aoMudarBiblioteca);
        super.onPause();
        destaque.pausar();
    }

    @Override
    public void onDestroyView() {
        View raiz = getView();
        if (raiz != null) raiz.getViewTreeObserver().removeOnGlobalFocusChangeListener(aoMudarFoco);
        destaque.pausar();
        destaque.definirExpansao(null);
        previa.soltar();
        super.onDestroyView();
    }

    /** ↓ no destaque, já recolhido, vai à primeira fileira visível. */
    private void atualizarDescida() {
        int alvo = View.NO_ID;
        if (secaoContinuar.getVisibility() == View.VISIBLE) alvo = R.id.st_fileira_continuar;
        else if (secaoFavoritos.getVisibility() == View.VISIBLE) alvo = R.id.st_fileira_favoritos;
        else if (secaoFilmes.getVisibility() == View.VISIBLE) alvo = R.id.st_fileira_filmes;
        destaque.definirDescida(alvo);
    }

    private static void atualizar(View secao, CartaoAdapter adapter, List<Titulo> lista) {
        adapter.atualizar(lista);
        secao.setVisibility(lista.isEmpty() ? View.GONE : View.VISIBLE);
    }

    /** Fileira Leanback: altura pelo conteúdo, espaço do desenho entre os itens. */
    private void configurarFileira(HorizontalGridView fileira, RecyclerView.Adapter<?> adapter, int espaco) {
        fileira.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        fileira.setHorizontalSpacing(getResources().getDimensionPixelSize(espaco));
        fileira.setAdapter(adapter);
    }

    /** "StarkTech Studio®" (® menor, sobrescrito) e "StarkTech Media Player · versão X". */
    private void montarRodape(View raiz) {
        TextView empresa = raiz.findViewById(R.id.st_rodape_empresa);
        SpannableStringBuilder texto = new SpannableStringBuilder(getString(R.string.st_rodape_empresa));
        int inicio = texto.length();
        texto.append(getString(R.string.st_rodape_registrada));
        texto.setSpan(new SuperscriptSpan(), inicio, texto.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        texto.setSpan(new AbsoluteSizeSpan(getResources().getDimensionPixelSize(R.dimen.st_tv_texto_rodape_registrada)),
                inicio, texto.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        empresa.setText(texto);

        TextView versao = raiz.findViewById(R.id.st_rodape_versao);
        versao.setText(getString(R.string.st_rodape_versao, versaoDoPacote()));
    }

    private String versaoDoPacote() {
        try {
            String nome = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0).versionName;
            return nome == null ? "" : nome;
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }

    private static boolean dentroDe(View view, View grupo) {
        View atual = view;
        while (atual != null) {
            if (atual == grupo) return true;
            ViewParent pai = atual.getParent();
            atual = pai instanceof View ? (View) pai : null;
        }
        return false;
    }
}
