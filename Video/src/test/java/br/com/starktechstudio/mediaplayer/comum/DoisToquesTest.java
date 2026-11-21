// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Vídeo anterior (1.7): 1º toque volta ao começo; 2º em até 3 s vai ao anterior; sem anterior (1.8.0), sempre o começo. */
public class DoisToquesTest {

    @Test
    public void segundoToqueNaJanela_vaiAoAnterior() {
        DoisToques toques = new DoisToques();
        assertFalse("1º: começo do vídeo", toques.segundo(10_000));
        assertTrue("2º em 2 s: anterior", toques.segundo(12_000));
        assertFalse("depois do anterior, conta de novo", toques.segundo(12_500));
    }

    @Test
    public void primeiroEpisodio_todoToqueVoltaAoComeco() {
        DoisToques toques = new DoisToques();
        assertFalse("1º episódio, 1º toque: começo", toques.vaiAoAnterior(false, 10_000));
        assertFalse("1º episódio, 2º toque na janela: começo de novo", toques.vaiAoAnterior(false, 11_000));
        assertFalse(toques.vaiAoAnterior(false, 11_500));
    }

    @Test
    public void comAnterior_doisToquesNaJanelaAbremOAnterior() {
        DoisToques toques = new DoisToques();
        assertFalse("1º toque: começo do episódio", toques.vaiAoAnterior(true, 10_000));
        assertTrue("2º em 2 s: episódio anterior", toques.vaiAoAnterior(true, 12_000));
        assertFalse("passou de 3 s: outro 1º toque", toques.vaiAoAnterior(true, 16_000));
        // Um toque sem anterior no meio zera a conta
        assertFalse(toques.vaiAoAnterior(false, 16_500));
        assertFalse(toques.vaiAoAnterior(true, 17_000));
        assertTrue(toques.vaiAoAnterior(true, 18_000));
    }

    @Test
    public void foraDaJanela_voltaAoComecoDeNovo() {
        DoisToques toques = new DoisToques();
        assertFalse(toques.segundo(10_000));
        assertFalse("passou de 3 s: outro 1º toque", toques.segundo(13_500));
        assertTrue(toques.segundo(14_000));
    }
}
