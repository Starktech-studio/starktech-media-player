// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Passos da barra da TV com a seta segurada e a volta das opções de zoom. */
public class ReproducaoBarraTest {

    @Test
    public void apertarMoveDezSegundos() {
        assertEquals(Reproducao.PULO_SEGUNDOS, Reproducao.passoDaBarra(0));
        assertEquals(10, Reproducao.passoDaBarra(1999));
    }

    @Test
    public void segurandoSobeParaTrintaESessenta() {
        assertEquals(30, Reproducao.passoDaBarra(2000));
        assertEquals(30, Reproducao.passoDaBarra(4999));
        assertEquals(60, Reproducao.passoDaBarra(5000));
        assertEquals(60, Reproducao.passoDaBarra(600000));
    }

    @Test
    public void passoFicaDentroDoVideo() {
        assertEquals(110, Reproducao.alvoDaBarra(100, 10, 3000));
        assertEquals(90, Reproducao.alvoDaBarra(100, -10, 3000));
        assertEquals(0, Reproducao.alvoDaBarra(5, -10, 3000));
        assertEquals(3000, Reproducao.alvoDaBarra(2990, 60, 3000));
    }

    @Test
    public void zoomPassaPelasOpcoesEVoltaAoOriginal() {
        assertEquals(7, Reproducao.ZOOM_OPCOES);
        assertEquals(1, Reproducao.proximoZoom(0));
        assertEquals(6, Reproducao.proximoZoom(5));
        assertEquals(0, Reproducao.proximoZoom(6));
    }

    @Test
    public void zoomGuardadoForaDasOpcoesViraOriginal() {
        assertEquals(0, Reproducao.zoomValido(-1));
        assertEquals(0, Reproducao.zoomValido(7));
        assertEquals(6, Reproducao.zoomValido(6));
        assertEquals(1, Reproducao.proximoZoom(99));
    }
}
