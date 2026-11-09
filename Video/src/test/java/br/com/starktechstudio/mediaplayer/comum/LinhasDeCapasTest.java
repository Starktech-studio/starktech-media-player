// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import br.com.starktechstudio.mediaplayer.dados.Modelo.SecaoShows;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Titulo;

/** Capas em linhas: colunas pela largura e as seções da aba Shows por grupo. */
public class LinhasDeCapasTest {

    private static List<Titulo> artistas(int quantos) {
        List<Titulo> lista = new ArrayList<>();
        for (int i = 0; i < quantos; i++) {
            Titulo t = new Titulo();
            t.id = "artista:" + i;
            lista.add(t);
        }
        return lista;
    }

    @Test
    public void colunasCabemNaLargura() {
        assertEquals(5, LinhasDeCapas.colunas(1068, 196, 22));
        assertEquals(4, LinhasDeCapas.colunas(1067, 196, 22));
        assertEquals(1, LinhasDeCapas.colunas(100, 196, 22));
    }

    @Test
    public void divideEmLinhasDeTamanhoCerto() {
        List<List<Integer>> linhas = LinhasDeCapas.dividir(Arrays.asList(1, 2, 3, 4, 5, 6, 7), 3);
        assertEquals(3, linhas.size());
        assertEquals(Arrays.asList(1, 2, 3), linhas.get(0));
        assertEquals(Arrays.asList(7), linhas.get(2));
    }

    @Test
    public void listaVaziaNaoTemLinhas() {
        assertTrue(LinhasDeCapas.dividir(Collections.emptyList(), 6).isEmpty());
    }

    @Test
    public void colunasInvalidasViramUmaPorLinha() {
        assertEquals(2, LinhasDeCapas.dividir(Arrays.asList(1, 2), 0).size());
    }

    @Test
    public void nomeDaSecaoSoNaPrimeiraLinhaDela() {
        List<LinhasDeCapas.Linha> linhas = LinhasDeCapas.dasSecoes(Arrays.asList(
                new SecaoShows("Internacionais", false, artistas(7)),
                new SecaoShows("Nacionais", false, artistas(2))), 6);
        assertEquals(3, linhas.size());
        assertEquals("Internacionais", linhas.get(0).secao);
        assertEquals(6, linhas.get(0).capas.size());
        assertNull(linhas.get(1).secao);
        assertEquals(1, linhas.get(1).capas.size());
        assertEquals("Nacionais", linhas.get(2).secao);
    }

    @Test
    public void chavesDasLinhasNaoSeRepetem() {
        List<LinhasDeCapas.Linha> linhas = LinhasDeCapas.dasSecoes(Arrays.asList(
                new SecaoShows("Nacionais", false, artistas(13)),
                new SecaoShows("Sem grupo", true, artistas(1))), 6);
        assertEquals(4, linhas.size());
        assertEquals("Nacionais#0", linhas.get(0).chave);
        assertEquals("Nacionais#2", linhas.get(2).chave);
        assertEquals("Sem grupo#0", linhas.get(3).chave);
    }

    @Test
    public void secaoSemArtistaNaoAparece() {
        assertTrue(LinhasDeCapas.dasSecoes(Collections.singletonList(
                new SecaoShows("Vazia", false, artistas(0))), 6).isEmpty());
    }
}
