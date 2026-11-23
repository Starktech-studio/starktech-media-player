// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Pontos do quadro do capítulo e o quadro estragado (faixas verdes), 2.1.2. */
public class QuadrosDeCapituloTest {

    private static final int VERDE = 0xff00b400;
    private static final int CENA = 0xff803c28;

    @Test
    public void momentoFicaCincoSegundosDepoisDoInicio() {
        assertEquals(65_000, QuadrosDeCapitulo.momentoMs(60, 300));
        // Capítulo curto: um quarto dele
        assertEquals(62_000, QuadrosDeCapitulo.momentoMs(60, 68));
    }

    @Test
    public void outrosPontosAFrenteSemPassarDoFim() {
        assertArrayEquals(new long[]{65_000, 68_000, 73_000}, QuadrosDeCapitulo.momentosMs(60, 300));
        // Fim em 70 s: o 1º em 62,5 s (um quarto) e só cabe o de +3 s (65,5 s; o de +8 s passa de 69,5 s)
        assertArrayEquals(new long[]{62_500, 65_500}, QuadrosDeCapitulo.momentosMs(60, 70));
        // Fim desconhecido: todos
        assertArrayEquals(new long[]{5_000, 8_000, 13_000}, QuadrosDeCapitulo.momentosMs(0, 0));
    }

    @Test
    public void quadroComFaixasVerdesEstaEstragado() {
        int largura = 64;
        int altura = 64;
        int[] pontos = cena(largura, altura);
        // Quatro faixas de 3 linhas
        for (int faixa = 0; faixa < 4; faixa++) {
            for (int y = faixa * 16; y < faixa * 16 + 3; y++) {
                for (int x = 0; x < largura; x++) pontos[y * largura + x] = VERDE;
            }
        }
        assertTrue(QuadrosDeCapitulo.comFaixasVerdes(pontos, largura, altura));
    }

    @Test
    public void quadroNormalNaoEstaEstragado() {
        assertFalse(QuadrosDeCapitulo.comFaixasVerdes(cena(64, 64), 64, 64));
    }

    @Test
    public void verdeDeCenaNaoEnganaODetector() {
        int largura = 64;
        int altura = 64;
        int[] pontos = cena(largura, altura);
        // Gramado: verde com vermelho e azul, metade de baixo
        for (int i = pontos.length / 2; i < pontos.length; i++) pontos[i] = 0xff3c9632;
        assertFalse(QuadrosDeCapitulo.comFaixasVerdes(pontos, largura, altura));
    }

    private static int[] cena(int largura, int altura) {
        int[] pontos = new int[largura * altura];
        java.util.Arrays.fill(pontos, CENA);
        return pontos;
    }
}
