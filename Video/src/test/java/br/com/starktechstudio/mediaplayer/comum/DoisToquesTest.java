// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Vídeo anterior (1.7): 1º toque volta ao começo; 2º em até 3 s vai ao anterior. */
public class DoisToquesTest {

    @Test
    public void segundoToqueNaJanela_vaiAoAnterior() {
        DoisToques toques = new DoisToques();
        assertFalse("1º: começo do vídeo", toques.segundo(10_000));
        assertTrue("2º em 2 s: anterior", toques.segundo(12_000));
        assertFalse("depois do anterior, conta de novo", toques.segundo(12_500));
    }

    @Test
    public void foraDaJanela_voltaAoComecoDeNovo() {
        DoisToques toques = new DoisToques();
        assertFalse(toques.segundo(10_000));
        assertFalse("passou de 3 s: outro 1º toque", toques.segundo(13_500));
        assertTrue(toques.segundo(14_000));
    }
}
