// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

/** Ordem dos vídeos de música (1.6): na sequência ou sorteada, com o vídeo aberto na frente e guardada. */
public class SequenciaClipesTest {

    private static final List<String> BASE = Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h");

    @Before
    public void preparar() {
        SequenciaClipes.definirSorteio(new Random(3));
    }

    @Test
    public void semAleatorio_aOrdemDaSequencia() {
        assertEquals(BASE, SequenciaClipes.montar(BASE, "c", false));
    }

    @Test
    public void aleatorio_comOAbertoNaFrente_eAMesmaOrdemDepois() {
        List<String> sorteada = new ArrayList<>(SequenciaClipes.montar(BASE, "c", true));
        assertEquals("c", sorteada.get(0));
        assertEquals(new HashSet<>(BASE), new HashSet<>(sorteada));
        assertNotEquals(BASE, sorteada);
        // Andando pela ordem (o próximo abre), ela fica a mesma
        assertSame("a ordem guardada vale", SequenciaClipes.montar(BASE, sorteada.get(1), true),
                SequenciaClipes.montar(BASE, sorteada.get(2), true));
        assertEquals(sorteada, SequenciaClipes.montar(BASE, sorteada.get(3), true));
    }

    @Test
    public void listaDaTela_naOrdem() {
        assertEquals("a", SequenciaClipes.usar(BASE, false));
        assertEquals(BASE, SequenciaClipes.montar(BASE, "a", false));
    }

    @Test
    public void botaoAleatorio_sorteiaTudo() {
        String primeiro = SequenciaClipes.sortear(BASE);
        List<String> ordem = SequenciaClipes.montar(BASE, primeiro, true);
        assertEquals(primeiro, ordem.get(0));
        assertEquals(BASE.size(), ordem.size());
    }
}
