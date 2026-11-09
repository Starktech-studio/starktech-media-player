// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import java.util.ArrayList;
import java.util.List;

import br.com.starktechstudio.mediaplayer.dados.Modelo.SecaoShows;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Titulo;

/**
 * Capas em linhas, para o que não cabe numa grade simples: as seções da aba Shows por grupo
 * ({@link SecoesShowsAdapter}, D7) e a lista da tela de coletânea e de artista
 * ({@link ColetaneaArtistaActivity}).
 */
final class LinhasDeCapas {

    /** Uma linha da aba Shows por grupo; o nome da seção vem só na primeira linha dela. */
    static final class Linha {
        /** Identifica a linha entre as recargas (o foco fica na mesma capa). */
        final String chave;
        /** Nome da seção; null nas demais linhas dela. */
        final String secao;
        final List<Titulo> capas;

        Linha(String chave, String secao, List<Titulo> capas) {
            this.chave = chave;
            this.secao = secao;
            this.capas = capas;
        }
    }

    private LinhasDeCapas() {}

    /** Quantas capas de {@code capa} px, com {@code espaco} px entre elas, cabem em {@code largura} px. */
    static int colunas(int largura, int capa, int espaco) {
        return Math.max(1, (largura + espaco) / (capa + espaco));
    }

    /** Os itens em linhas de até {@code colunas}. */
    static <T> List<List<T>> dividir(List<T> itens, int colunas) {
        int porLinha = Math.max(1, colunas);
        List<List<T>> linhas = new ArrayList<>();
        for (int inicio = 0; inicio < itens.size(); inicio += porLinha) {
            linhas.add(new ArrayList<>(itens.subList(inicio, Math.min(inicio + porLinha, itens.size()))));
        }
        return linhas;
    }

    /** As seções, na ordem, em linhas de até {@code colunas} capas; seção sem artista não aparece. */
    static List<Linha> dasSecoes(List<SecaoShows> secoes, int colunas) {
        List<Linha> linhas = new ArrayList<>();
        for (SecaoShows secao : secoes) {
            List<List<Titulo>> capas = dividir(secao.artistas, colunas);
            for (int i = 0; i < capas.size(); i++) {
                linhas.add(new Linha(secao.nome + "#" + i, i == 0 ? secao.nome : null, capas.get(i)));
            }
        }
        return linhas;
    }
}
