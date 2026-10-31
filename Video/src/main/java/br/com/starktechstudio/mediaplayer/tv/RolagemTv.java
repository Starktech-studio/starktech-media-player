// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.tv;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.FocusFinder;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ScrollView;

/**
 * Rolagem vertical do Início da TV. O foco manda na rolagem: a seção (filho direto do
 * conteúdo) que recebe o foco sobe para o alto da tela, com o título visível; a primeira
 * seção (destaque) fica no topo da página. Sem rolagem por página nem por tecla solta,
 * para a tela nunca parar entre duas seções.
 */
public class RolagemTv extends ScrollView {

    /** Espaço acima do título da seção em foco (40 px do desenho = 20 dp). */
    private static final float RESPIRO_DP = 20f;

    private final int respiro;

    public RolagemTv(Context contexto) {
        this(contexto, null);
    }

    public RolagemTv(Context contexto, AttributeSet atributos) {
        this(contexto, atributos, android.R.attr.scrollViewStyle);
    }

    public RolagemTv(Context contexto, AttributeSet atributos, int estiloPadrao) {
        super(contexto, atributos, estiloPadrao);
        respiro = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, RESPIRO_DP,
                contexto.getResources().getDisplayMetrics()));
        // A própria rolagem nunca fica com o foco: só os itens de dentro
        setFocusable(false);
        setSmoothScrollingEnabled(true);
    }

    /** A rolagem automática do ScrollView (deixar o item só visível) fica desligada. */
    @Override
    protected int computeScrollDeltaToGetChildRectOnScreen(Rect retangulo) {
        return 0;
    }

    @Override
    public void requestChildFocus(View filho, View focado) {
        super.requestChildFocus(filho, focado);
        if (isLaidOut() && !isLayoutRequested()) alinhar(focado, true);
        else post(() -> alinhar(focado, false));
    }

    /** ↑ / ↓ só trocam o foco (a rolagem vem do alinhamento); sem rolagem por página. */
    @Override
    public boolean executeKeyEvent(KeyEvent evento) {
        if (evento.getAction() != KeyEvent.ACTION_DOWN) return false;
        int tecla = evento.getKeyCode();
        if (tecla != KeyEvent.KEYCODE_DPAD_UP && tecla != KeyEvent.KEYCODE_DPAD_DOWN) return false;
        int direcao = tecla == KeyEvent.KEYCODE_DPAD_UP ? View.FOCUS_UP : View.FOCUS_DOWN;
        View atual = findFocus();
        if (atual == null) return false;
        View proximo = FocusFinder.getInstance().findNextFocus(this, atual, direcao);
        return proximo != null && proximo != atual && proximo.requestFocus(direcao);
    }

    /** Volta ao alto da página (foco saiu para a barra do topo). */
    public void voltarAoTopo() {
        if (getScrollY() != 0) smoothScrollTo(0, 0);
    }

    private void alinhar(View focado, boolean suave) {
        if (getChildCount() == 0 || focado == null) return;
        ViewGroup conteudo = (ViewGroup) getChildAt(0);
        View secao = secaoDe(focado, conteudo);
        if (secao == null) return;
        int alvo = secao == primeiraVisivel(conteudo) ? 0 : secao.getTop() - respiro;
        int maximo = Math.max(0, conteudo.getHeight() - (getHeight() - getPaddingTop() - getPaddingBottom()));
        alvo = Math.max(0, Math.min(alvo, maximo));
        if (alvo == getScrollY()) return;
        if (suave) smoothScrollTo(0, alvo);
        else scrollTo(0, alvo);
    }

    private static View secaoDe(View view, ViewGroup conteudo) {
        View atual = view;
        while (atual != null) {
            ViewParent pai = atual.getParent();
            if (pai == conteudo) return atual;
            atual = pai instanceof View ? (View) pai : null;
        }
        return null;
    }

    private static View primeiraVisivel(ViewGroup conteudo) {
        for (int i = 0; i < conteudo.getChildCount(); i++) {
            View filho = conteudo.getChildAt(i);
            if (filho.getVisibility() == View.VISIBLE) return filho;
        }
        return null;
    }
}
