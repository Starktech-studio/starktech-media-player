// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.dados;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import br.com.starktechstudio.mediaplayer.dados.Modelo.SecaoShows;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Titulo;

/** Selos da capa, filtro "Só não assistidos" e a imagem do cartão 16:9. */
public class ModeloCapaTest {

    private static Titulo titulo(String id, boolean assistido) {
        Titulo t = new Titulo();
        t.id = id;
        t.assistido = assistido;
        return t;
    }

    private static Titulo comQualidade(String qualidade) {
        Titulo t = new Titulo();
        t.qualidade = qualidade;
        return t;
    }

    @Test
    public void series_cadaUmaNaSuaAba() {
        Titulo serie = titulo("serie:a", false);
        Titulo programa = titulo("serie:b", false);
        programa.tvShow = true;
        List<Titulo> todos = Arrays.asList(serie, programa);
        assertEquals(Arrays.asList(serie), Modelo.daAbaDeSeries(todos, false));
        assertEquals(Arrays.asList(programa), Modelo.daAbaDeSeries(todos, true));
        assertTrue(programa.copia().tvShow);
    }

    @Test
    public void selos_pelaQualidade() {
        assertTrue(comQualidade("4K").eh4k());
        assertFalse(comQualidade("4K").eh1080p());
        assertTrue(comQualidade("4K HDR").eh4k());
        assertTrue(comQualidade("1080p").eh1080p());
        assertTrue(comQualidade("1080p HDR").eh1080p());
        assertFalse(comQualidade("1080p HDR").eh4k());
        assertFalse(comQualidade("720p").eh1080p());
        assertFalse(comQualidade("HDR10").eh1080p());
        assertFalse(comQualidade("").eh4k());
        assertFalse(comQualidade(null).eh1080p());
    }

    @Test
    public void filtro_coletaneaEArtistaSoSaemComTudoAssistido() {
        Titulo coletanea = titulo("coletanea:x", false);
        Titulo artistaVisto = titulo("artista:y", false);
        artistaVisto.todosAssistidos = true;
        Titulo filmeVisto = titulo("/hd/a.mkv", true);
        Titulo filmeNovo = titulo("/hd/b.mkv", false);

        List<Titulo> lista = Modelo.naoAssistidos(Arrays.asList(coletanea, artistaVisto, filmeVisto, filmeNovo));

        assertEquals(Arrays.asList(coletanea, filmeNovo), lista);
    }

    @Test
    public void filtro_secaoSemNadaAAssistirSai() {
        Titulo visto = titulo("artista:a", false);
        visto.todosAssistidos = true;
        Titulo novo = titulo("artista:b", false);
        SecaoShows soVistos = new SecaoShows("Rock", false, Arrays.asList(visto));
        SecaoShows comNovo = new SecaoShows("Pop", false, Arrays.asList(visto, novo));

        List<SecaoShows> lista = Modelo.secoesNaoAssistidas(Arrays.asList(soVistos, comNovo));

        assertEquals(1, lista.size());
        assertEquals("Pop", lista.get(0).nome);
        assertEquals(Arrays.asList(novo), lista.get(0).artistas);
    }

    @Test
    public void cartao_fundoDepoisQuadroDepoisCapa() {
        Titulo t = new Titulo();
        assertNull(t.imagemCartao());
        t.capaTmdb = "/capa.jpg";
        assertEquals("/capa.jpg", t.imagemCartao());
        t.quadro = "/quadro.jpg";
        assertEquals("/quadro.jpg", t.imagemCartao());
        t.fundoTmdb = "/fundo.jpg";
        assertEquals("/fundo.jpg", t.imagemCartao());
    }

    @Test
    public void cartao_fundoEmCorEscolhidoNoEditarFicaSemImagem() {
        Titulo t = new Titulo();
        t.capaTmdb = "/capa.jpg";
        t.quadro = "/quadro.jpg";
        t.fundoImagem = "cor:#112233";
        assertNull(t.imagemCartao());
    }

    @Test
    public void copia_levaOQueOFiltroUsa() {
        Titulo t = titulo("coletanea:x", false);
        t.todosAssistidos = true;
        assertTrue(t.copia().tudoAssistido());
    }
}
