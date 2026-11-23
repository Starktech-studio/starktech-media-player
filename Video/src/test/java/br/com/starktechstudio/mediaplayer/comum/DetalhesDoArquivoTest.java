// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

/** Os textos da janela Detalhes do arquivo do player (2.1.2). */
public class DetalhesDoArquivoTest {

    @Test
    public void tamanhoEmKbMbEGb() {
        assertEquals("488 KB", DetalhesDoArquivo.tamanho(500_000));
        assertEquals("734,3 MB", DetalhesDoArquivo.tamanho(770_000_000L));
        assertEquals("6,21 GB", DetalhesDoArquivo.tamanho(6_670_000_000L));
        assertEquals("", DetalhesDoArquivo.tamanho(0));
    }

    @Test
    public void taxaMediaPeloTamanhoEPelaDuracao() {
        // 6,67 GB em 58:22
        assertEquals("15,2 Mb/s", DetalhesDoArquivo.taxa(6_670_000_000L, 3_502_000));
        assertEquals("800 kb/s", DetalhesDoArquivo.taxa(1_000_000, 10_000));
        assertEquals("", DetalhesDoArquivo.taxa(0, 10_000));
        assertEquals("", DetalhesDoArquivo.taxa(1_000_000, 0));
    }

    @Test
    public void quadrosPorSegundoSemZerosAMais() {
        assertEquals("23,976", DetalhesDoArquivo.quadros(24000, 1001, 0));
        assertEquals("29,97", DetalhesDoArquivo.quadros(30000, 1001, 0));
        assertEquals("25", DetalhesDoArquivo.quadros(25, 1, 0));
        assertEquals("30", DetalhesDoArquivo.quadros(0, 0, 30));
        assertEquals("", DetalhesDoArquivo.quadros(0, 0, 0));
    }

    @Test
    public void resolucaoComAQualidade() {
        assertEquals("3840 × 2160 (4K)", DetalhesDoArquivo.resolucao(3840, 2160));
        assertEquals("1920 × 1080 (1080p)", DetalhesDoArquivo.resolucao(1920, 1080));
        assertEquals("", DetalhesDoArquivo.resolucao(0, 0));
    }

    @Test
    public void listaComOResto() {
        assertEquals("a\nb\nc  +2", DetalhesDoArquivo.lista(Arrays.asList("a", "b", "c", "d", "e")));
        assertEquals("a", DetalhesDoArquivo.lista(Collections.singletonList("a")));
        assertEquals("", DetalhesDoArquivo.lista(Collections.emptyList()));
    }
}
