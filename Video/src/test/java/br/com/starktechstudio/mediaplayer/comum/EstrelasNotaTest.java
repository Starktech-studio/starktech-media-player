// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Escala da nota no controle da TV: sem nota, 0, 1... 5; nas pontas, o foco sai. */
public class EstrelasNotaTest {

    @Test
    public void direitaSaiDeSemNotaParaZeroESobeAteCinco() {
        assertEquals(Integer.valueOf(0), EstrelasNota.vizinha(null, 1));
        for (int nota = 0; nota < 5; nota++) {
            assertEquals(Integer.valueOf(nota + 1), EstrelasNota.vizinha(nota, 1));
        }
    }

    @Test
    public void esquerdaDesceAteZeroEDepoisTiraANota() {
        for (int nota = 5; nota > 0; nota--) {
            assertEquals(Integer.valueOf(nota - 1), EstrelasNota.vizinha(nota, -1));
        }
        assertNull(EstrelasNota.vizinha(0, -1));
    }

    @Test
    public void soAsPontasDeixamOFocoSair() {
        assertTrue(EstrelasNota.naPonta(null, -1));
        assertTrue(EstrelasNota.naPonta(5, 1));
        assertFalse(EstrelasNota.naPonta(null, 1));
        assertFalse(EstrelasNota.naPonta(0, -1));
        assertFalse(EstrelasNota.naPonta(0, 1));
        assertFalse(EstrelasNota.naPonta(5, -1));
    }
}
