// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.musica;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import br.com.starktechstudio.mediaplayer.dados.ModeloMusicas.Musica;

/** Fila do player de música (1.5): ordem, fim da fila, repetir, aleatório, tocar a seguir e pôr na fila. */
public class FilaTest {

    private static Musica m(String id) {
        Musica musica = new Musica();
        musica.id = id;
        musica.titulo = id;
        return musica;
    }

    private static List<Musica> tres() {
        return Arrays.asList(m("a"), m("b"), m("c"));
    }

    private static String ids(List<Musica> lista) {
        StringBuilder s = new StringBuilder();
        for (Musica x : lista) s.append(x.id);
        return s.toString();
    }

    @Test
    public void ordem_eFimDaFila() {
        Fila fila = new Fila();
        fila.definir(tres(), 1, false);
        assertEquals("b", fila.atual().id);
        assertEquals("c", fila.proxima(false).id);
        assertNull("sem repetir, a fila acaba", fila.proxima(false));
        assertEquals("c", fila.atual().id);
        assertEquals("b", fila.anterior().id);
        assertEquals("a", fila.anterior().id);
        assertEquals("na primeira, fica nela", "a", fila.anterior().id);
    }

    @Test
    public void repetirTodas_voltaAoComeco() {
        Fila fila = new Fila();
        fila.definir(tres(), 2, false);
        assertEquals(Fila.Repetir.TODAS, fila.trocarRepetir());
        assertEquals("a", fila.proxima(false).id);
        assertEquals("c", fila.anterior().id);
    }

    @Test
    public void repetirUma_noFimDaMusicaRepete_oBotaoPassa() {
        Fila fila = new Fila();
        fila.definir(tres(), 0, false);
        fila.trocarRepetir();
        assertEquals(Fila.Repetir.UMA, fila.trocarRepetir());
        assertEquals("a", fila.proxima(false).id);
        assertEquals("b", fila.proxima(true).id);
        assertEquals(Fila.Repetir.NAO, fila.trocarRepetir());
    }

    @Test
    public void aleatorio_comAAtualNaFrente_eDesligarVoltaAOrdem() {
        List<Musica> muitas = new ArrayList<>();
        for (int i = 0; i < 20; i++) muitas.add(m("m" + i));
        Fila fila = new Fila(new Random(7));
        fila.definir(muitas, 5, true);
        assertEquals("m5", fila.atual().id);
        assertEquals(0, fila.posicao());
        assertEquals(20, new HashSet<>(fila.naOrdem()).size());
        fila.proxima(true);
        String segunda = fila.atual().id;
        fila.alternarAleatorio();
        assertEquals("continua na mesma música", segunda, fila.atual().id);
        assertEquals(ids(muitas), ids(fila.naOrdem()));
    }

    @Test
    public void tocarASeguir_ePorNoFim() {
        Fila fila = new Fila();
        fila.definir(tres(), 0, false);
        fila.tocarASeguir(Arrays.asList(m("x")));
        fila.porNoFim(Arrays.asList(m("z")));
        assertEquals("axbcz", ids(fila.naOrdem()));
        assertEquals("x", fila.proxima(true).id);
    }

    @Test
    public void filaVazia_viraAFilaNova() {
        Fila fila = new Fila();
        assertTrue(fila.vazia());
        assertNull(fila.proxima(true));
        fila.porNoFim(Arrays.asList(m("q")));
        assertEquals("q", fila.atual().id);
        fila.limpar();
        assertTrue(fila.vazia());
    }
}
