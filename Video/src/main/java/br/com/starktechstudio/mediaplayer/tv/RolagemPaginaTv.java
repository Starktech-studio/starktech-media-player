// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.tv;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.ScrollView;

/**
 * Rolagem das páginas da TV com uma grade embaixo do detalhe: Série e Coletânea e artista (2.0.1). Desde a 2.0
 * o topo não tem nada com foco (D1), e o primeiro item com foco é a fileira de botões: a rolagem comum do
 * ScrollView só trazia o botão para a beira da tela, e a capa, o título, os selos e a sinopse acima dele ficavam
 * cortados ao subir dos episódios. Aqui, o item em foco que cabe na tela com a página no alto (os botões, as
 * pílulas das temporadas, a primeira linha que couber) leva a página ao alto, com o detalhe inteiro; os de baixo
 * seguem a rolagem comum (o item em foco só fica à vista), de linha em linha. O Detalhe usa a {@link RolagemTv},
 * que já leva a primeira seção ao alto.
 */
public class RolagemPaginaTv extends ScrollView {

    public RolagemPaginaTv(Context contexto) {
        this(contexto, null);
    }

    public RolagemPaginaTv(Context contexto, AttributeSet atributos) {
        this(contexto, atributos, android.R.attr.scrollViewStyle);
    }

    public RolagemPaginaTv(Context contexto, AttributeSet atributos, int estiloPadrao) {
        super(contexto, atributos, estiloPadrao);
        // A própria rolagem nunca fica com o foco: só os itens de dentro
        setFocusable(false);
        setSmoothScrollingEnabled(true);
    }

    /**
     * {@code retangulo}: o item em foco, nas medidas do conteúdo. Se ele cabe inteiro com a página no alto, a
     * rolagem volta ao alto (o mesmo caminho da rolagem comum: suave pelas setas e na hora pelo foco pedido).
     */
    @Override
    protected int computeScrollDeltaToGetChildRectOnScreen(Rect retangulo) {
        if (getChildCount() == 0) return 0;
        if (retangulo.bottom <= getHeight() - getPaddingBottom()) return -getScrollY();
        return super.computeScrollDeltaToGetChildRectOnScreen(retangulo);
    }
}
