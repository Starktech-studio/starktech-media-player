// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import br.com.starktechstudio.mediaplayer.dados.Modelo.Episodio;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Temporada;

/** Forma da legenda (D8) e o episódio seguinte do "Próximo episódio automático". */
public class ReproducaoLegendaTest {

    @Test
    public void cadaValorDasConfiguracoesTemSeuAjuste() {
        assertEquals(Preferencias.Item.TAMANHO_LEGENDA.valores.length, Reproducao.LEGENDA_TAMANHO_PT.length);
        assertEquals(Preferencias.Item.FONTE_LEGENDA.valores.length, Reproducao.LEGENDA_FONTE.length);
        assertEquals(Preferencias.Item.COR_LEGENDA.valores.length, Reproducao.LEGENDA_COR.length);
        assertEquals(Preferencias.Item.CONTORNO_LEGENDA.valores.length, Reproducao.LEGENDA_CONTORNO.length);
        assertEquals(Preferencias.Item.SOMBRA_LEGENDA.valores.length, Reproducao.LEGENDA_SOMBRA.length);
        assertEquals(Preferencias.Item.FUNDO_LEGENDA.valores.length, Reproducao.LEGENDA_FUNDO.length);
        assertEquals(Preferencias.Item.POSICAO_LEGENDA.valores.length, Reproducao.LEGENDA_POSICAO_PORCENTO.length);
    }

    @Test
    public void cincoTamanhosCrescentes() {
        assertEquals(5, Reproducao.LEGENDA_TAMANHO_PT.length);
        for (int i = 1; i < Reproducao.LEGENDA_TAMANHO_PT.length; i++) {
            assertTrue(Reproducao.LEGENDA_TAMANHO_PT[i] > Reproducao.LEGENDA_TAMANHO_PT[i - 1]);
        }
    }

    @Test
    public void tamanhoDeFabricaNaoMudaALegendaAss() {
        assertEquals(1f, Reproducao.escalaDaLegenda(Preferencias.Item.TAMANHO_LEGENDA.padrao), 0.001f);
        assertTrue(Reproducao.escalaDaLegenda(0) < 1f);
        assertTrue(Reproducao.escalaDaLegenda(4) > 1f);
    }

    @Test
    public void posicaoDeFabricaFicaNaBase() {
        assertEquals(0, Reproducao.distanciaDaBase(Preferencias.Item.POSICAO_LEGENDA.padrao, 1080));
        assertEquals(64, Reproducao.distanciaDaBase(1, 1080));
        assertEquals(129, Reproducao.distanciaDaBase(2, 1080));
    }

    private static Episodio episodio(String id, String caminho) {
        Episodio e = new Episodio();
        e.id = id;
        e.caminho = caminho;
        return e;
    }

    @Test
    public void passaAoSeguinteDaMesmaTemporadaEDaOutra() {
        List<Temporada> temporadas = Arrays.asList(
                new Temporada(1, Arrays.asList(episodio("a", "/a"), episodio("b", "/b"))),
                new Temporada(2, Collections.singletonList(episodio("c", "/c"))));
        assertEquals("b", Reproducao.episodioSeguinte(temporadas, "a").id);
        assertEquals("c", Reproducao.episodioSeguinte(temporadas, "b").id);
    }

    @Test
    public void noUltimoEpisodioNaoPassa() {
        List<Temporada> temporadas = Collections.singletonList(
                new Temporada(1, Arrays.asList(episodio("a", "/a"), episodio("b", "/b"))));
        assertNull(Reproducao.episodioSeguinte(temporadas, "b"));
        assertNull(Reproducao.episodioSeguinte(temporadas, "fora da lista"));
    }

    @Test
    public void pulaEpisodioSemArquivo() {
        List<Temporada> temporadas = Collections.singletonList(new Temporada(1,
                Arrays.asList(episodio("a", "/a"), episodio("b", null), episodio("c", "/c"))));
        assertEquals("c", Reproducao.episodioSeguinte(temporadas, "a").id);
    }
}
