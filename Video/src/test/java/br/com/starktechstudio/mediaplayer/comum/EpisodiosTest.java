// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import br.com.starktechstudio.mediaplayer.dados.Modelo;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Episodio;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Temporada;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Tipo;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Titulo;

/**
 * Episódio anterior e seguinte no player (1.8.0, pedido 4 do plano 009), com ou sem "Próximo episódio
 * automático": de temporada em temporada, pulando o episódio sem arquivo; e o desenho com episódios fora
 * das abas Séries e TV Shows (D7).
 */
public class EpisodiosTest {

    private static Episodio ep(int temporada, int numero, boolean comArquivo) {
        Episodio e = new Episodio();
        e.id = "/hd/Série/T" + temporada + "/" + numero + ".mkv";
        e.caminho = comArquivo ? e.id : null;
        e.temporada = temporada;
        e.numero = numero;
        return e;
    }

    private static final Episodio T1E1 = ep(1, 1, true);
    private static final Episodio T1E2 = ep(1, 2, true);
    private static final Episodio T1E3_SEM_ARQUIVO = ep(1, 3, false);
    private static final Episodio T2E1 = ep(2, 1, true);
    private static final Episodio T2E2 = ep(2, 2, true);

    private static List<Temporada> temporadas() {
        return Arrays.asList(new Temporada(1, Arrays.asList(T1E1, T1E2, T1E3_SEM_ARQUIVO)),
                new Temporada(2, Arrays.asList(T2E1, T2E2)));
    }

    @Test
    public void seguinte_passaDeTemporadaEPulaOSemArquivo() {
        assertEquals(T1E2, Reproducao.episodioSeguinte(temporadas(), T1E1.id));
        assertEquals("o T1E3 não tem arquivo: vai ao T2E1", T2E1, Reproducao.episodioSeguinte(temporadas(), T1E2.id));
        assertEquals(T2E2, Reproducao.episodioSeguinte(temporadas(), T2E1.id));
        assertNull("o último não tem seguinte", Reproducao.episodioSeguinte(temporadas(), T2E2.id));
    }

    @Test
    public void anterior_voltaDeTemporadaEPulaOSemArquivo() {
        assertNull("o 1º episódio não tem anterior", Reproducao.episodioAnterior(temporadas(), T1E1.id));
        assertEquals(T1E1, Reproducao.episodioAnterior(temporadas(), T1E2.id));
        assertEquals("o último da temporada anterior, sem o que não tem arquivo", T1E2,
                Reproducao.episodioAnterior(temporadas(), T2E1.id));
        assertEquals(T2E1, Reproducao.episodioAnterior(temporadas(), T2E2.id));
    }

    @Test
    public void foraDaLista_semAnteriorNemSeguinte() {
        assertNull(Reproducao.episodioAnterior(temporadas(), "/hd/outro.mkv"));
        assertNull(Reproducao.episodioSeguinte(temporadas(), "/hd/outro.mkv"));
        assertNull(Reproducao.episodioAnterior(new ArrayList<Temporada>(), T1E1.id));
    }

    private static Titulo serie(String nome, boolean tvShow, boolean desenho) {
        Titulo t = new Titulo();
        t.id = "serie:" + nome.toLowerCase();
        t.tipo = Tipo.SERIE;
        t.titulo = nome;
        t.tvShow = tvShow;
        t.desenho = desenho;
        return t;
    }

    @Test
    public void desenhoComEpisodios_foraDeSeriesEDeTvShows() {
        Titulo serie = serie("Série", false, false);
        Titulo programa = serie("Programa", true, false);
        Titulo desenho = serie("Desenho", false, true);
        List<Titulo> todas = Arrays.asList(serie, programa, desenho);
        assertEquals(Arrays.asList(serie), Modelo.daAbaDeSeries(todas, false));
        assertEquals(Arrays.asList(programa), Modelo.daAbaDeSeries(todas, true));
        assertEquals("a cópia leva a marca", true, desenho.copia().desenho);
    }
}
