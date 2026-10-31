// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Space;

import java.util.List;

/**
 * Grade de colunas iguais (grid do desenho) montada com linhas de LinearLayout: para listas
 * curtas dentro de uma página que já rola (dados técnicos, capítulos e elenco do tablet).
 */
public final class Grade {

    private Grade() {}

    /** Troca o conteúdo de {@code alvo} (vertical) pelos itens, {@code colunas} por linha. */
    public static void preencher(LinearLayout alvo, List<View> itens, int colunas, int espacoH, int espacoV) {
        alvo.removeAllViews();
        Context contexto = alvo.getContext();
        for (int inicio = 0; inicio < itens.size(); inicio += colunas) {
            LinearLayout linha = new LinearLayout(contexto);
            linha.setOrientation(LinearLayout.HORIZONTAL);
            linha.setBaselineAligned(false);
            for (int coluna = 0; coluna < colunas; coluna++) {
                int indice = inicio + coluna;
                // Linha incompleta: espaço vazio, para as colunas não esticarem
                View item = indice < itens.size() ? itens.get(indice) : new Space(contexto);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                if (coluna > 0) lp.setMarginStart(espacoH);
                linha.addView(item, lp);
            }
            LinearLayout.LayoutParams lpLinha = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (inicio > 0) lpLinha.topMargin = espacoV;
            alvo.addView(linha, lpLinha);
        }
    }
}
