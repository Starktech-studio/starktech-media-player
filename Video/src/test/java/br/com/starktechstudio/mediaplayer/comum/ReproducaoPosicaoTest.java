// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Quando a posição guardada é trocada e quando o vídeo conta como assistido. */
public class ReproducaoPosicaoTest {

    @Test
    public void posicaoMaiorSempreTroca() {
        assertTrue(Reproducao.trocaPosicao(120, 100, 100, false));
    }

    @Test
    public void mesmaPosicaoNaoGravaDeNovo() {
        assertFalse(Reproducao.trocaPosicao(100, 100, 100, false));
    }

    @Test
    public void abrirDoInicioESairLogoNaoApagaAPosicao() {
        assertFalse(Reproducao.trocaPosicao(5, 3000, 0, false));
    }

    @Test
    public void retomadaQueCaiuAntesDaPosicaoNaoTroca() {
        // A busca cai no quadro-chave de antes: a sessão não andou
        assertFalse(Reproducao.trocaPosicao(95, 100, 100, false));
    }

    @Test
    public void sessaoQueAndouTrocaMesmoComPosicaoMenor() {
        assertFalse(Reproducao.trocaPosicao(59, 3000, 0, false));
        assertTrue(Reproducao.trocaPosicao(60, 3000, 0, false));
    }

    @Test
    public void idaManualTrocaMesmoSemAndar() {
        assertTrue(Reproducao.trocaPosicao(1200, 3000, 3000, true));
    }

    @Test
    public void posicaoGuardadaNoFimRecomecaDoZero() {
        assertTrue(Reproducao.recomecaDoZero(97, 97, 100, false));
    }

    @Test
    public void naoRecomecaLongeDoFimSemDuracaoOuComPontoPedido() {
        assertFalse(Reproducao.recomecaDoZero(50, 50, 100, false));
        assertFalse(Reproducao.recomecaDoZero(97, 97, 0, false));
        // Capítulo pedido, em vez da posição guardada
        assertFalse(Reproducao.recomecaDoZero(97, 50, 100, false));
        assertFalse(Reproducao.recomecaDoZero(97, 97, 100, true));
    }

    @Test
    public void recomecoGravaDesdeOZero() {
        // Recomeçou como ida manual: sair logo já tira o ponto antigo, e o que assistir entra no lugar
        assertTrue(Reproducao.trocaPosicao(0, 97, 97, true));
        assertTrue(Reproducao.trocaPosicao(30, 97, 97, true));
        // Sem ela, nada seria gravado até passar do ponto antigo
        assertFalse(Reproducao.trocaPosicao(30, 97, 97, false));
    }

    @Test
    public void fimDoVideoSoComADuracaoDoMotor() {
        assertTrue(Reproducao.chegouAoFim(95, 100));
        assertTrue(Reproducao.chegouAoFim(100, 100));
        assertFalse(Reproducao.chegouAoFim(94, 100));
    }

    @Test
    public void duracaoDesconhecidaNuncaEOFim() {
        assertFalse(Reproducao.chegouAoFim(50, 0));
    }

    @Test
    public void videoCurtoNoComecoNaoEOFim() {
        assertFalse(Reproducao.chegouAoFim(0, 3));
    }
}
