// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Application;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/** Respostas do MusicBrainz e do fanart.tv e a comparação do nome do artista (sem rede). */
@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, manifest = Config.NONE, sdk = 34)
public class BuscaArtistaTest {

    private static final String MUSICBRAINZ = "{\"created\":\"2026-11-09\",\"count\":3,\"artists\":["
            + "{\"id\":\"0383dadf-2a4e-4d10-a46a-e9e041da8eb3\",\"score\":100,\"name\":\"Queen\",\"country\":\"GB\","
            + "\"area\":{\"name\":\"United Kingdom\"},\"disambiguation\":\"UK rock group\"},"
            + "{\"id\":\"2\",\"score\":100,\"name\":\"Queen\",\"area\":null},"
            + "{\"id\":\"3\",\"score\":80,\"name\":\"Queen + Adam Lambert\"}]}";

    private static final String FANART = "{\"name\":\"Queen\",\"artistthumb\":["
            + "{\"id\":\"1\",\"url\":\"https://assets.fanart.tv/fanart/queen-pouco.jpg\",\"likes\":\"2\"},"
            + "{\"id\":\"2\",\"url\":\"https://assets.fanart.tv/fanart/queen-mais.jpg\",\"likes\":\"8\"},"
            + "{\"id\":\"3\",\"url\":\"http://assets.fanart.tv/fanart/inseguro.jpg\",\"likes\":\"99\"}],"
            + "\"artistbackground\":[{\"id\":\"4\",\"url\":\"https://assets.fanart.tv/fanart/queen-fundo.jpg\",\"likes\":\"4\"}]}";

    @Test
    public void musicBrainz_naOrdemDele_comPaisEDescricao() throws JSONException, IOException {
        List<BuscaArtista.ArtistaMb> lista = BuscaArtista.lerMusicBrainz(MUSICBRAINZ);
        assertEquals(3, lista.size());
        assertEquals("0383dadf-2a4e-4d10-a46a-e9e041da8eb3", lista.get(0).id);
        assertEquals("United Kingdom", lista.get(0).area);
        assertEquals("United Kingdom · UK rock group", BuscaArtista.detalhe(lista.get(0)));
        assertNull(lista.get(1).area);
        assertEquals("", BuscaArtista.detalhe(lista.get(2)));
    }

    @Test
    public void musicBrainz_soONomeIgual() throws JSONException, IOException {
        List<BuscaArtista.ArtistaMb> iguais = BuscaArtista.mesmoNome(BuscaArtista.lerMusicBrainz(MUSICBRAINZ), "queen");
        assertEquals(2, iguais.size());
        assertEquals("0383dadf-2a4e-4d10-a46a-e9e041da8eb3", iguais.get(0).id);
    }

    @Test(expected = IOException.class)
    public void musicBrainz_ocupado_valeComoSemResposta() throws JSONException, IOException {
        BuscaArtista.lerMusicBrainz("{\"error\": \"The MusicBrainz web server is currently busy. Please try again later.\"}");
    }

    @Test
    public void musicBrainz_nomeVaiComoFrase() {
        assertEquals("\"AC/DC\"", BuscaArtista.consultaMusicBrainz(" AC/DC "));
        assertEquals("\"Guns N\\\" Roses\"", BuscaArtista.consultaMusicBrainz("Guns N\" Roses"));
    }

    @Test
    public void fanart_aFotoEOFundoMaisCurtidos_soHttps() throws JSONException, IOException {
        BuscaArtista.Achado achado = BuscaArtista.lerFanart(FANART);
        assertEquals("https://assets.fanart.tv/fanart/queen-mais.jpg", achado.foto);
        assertEquals("https://assets.fanart.tv/fanart/queen-fundo.jpg", achado.fundo);
    }

    @Test
    public void fanart_semNada_semFotoNemFundo() throws JSONException, IOException {
        BuscaArtista.Achado achado = BuscaArtista.lerFanart("{}");
        assertNull(achado.foto);
        assertNull(achado.fundo);
    }

    @Test
    public void fanart_semFundoComum_usaO4k() throws JSONException, IOException {
        BuscaArtista.Achado achado = BuscaArtista.lerFanart("{\"artist4kbackground\":[{\"url\":\"https://assets.fanart.tv/fanart/4k.jpg\",\"likes\":\"1\"}]}");
        assertEquals("https://assets.fanart.tv/fanart/4k.jpg", achado.fundo);
    }

    @Test(expected = IOException.class)
    public void fanart_erro_valeComoSemResposta() throws JSONException, IOException {
        BuscaArtista.lerFanart("{\"status\":\"error\",\"error message\":\"Not found\"}");
    }

    @Test
    public void fanart_fotosDaMaisCurtidaAMenos() throws JSONException, IOException {
        assertEquals(Arrays.asList("https://assets.fanart.tv/fanart/queen-mais.jpg", "https://assets.fanart.tv/fanart/queen-pouco.jpg"),
                BuscaArtista.lerFotosFanart(FANART));
    }

    @Test
    public void mesmoNome_grafiasDiferentes() {
        assertTrue(BuscaArtista.mesmoNome("AC/DC", "ACDC"));
        assertTrue(BuscaArtista.mesmoNome("The Beatles", "Beatles"));
        assertTrue(BuscaArtista.mesmoNome("Simon & Garfunkel", "Simon and Garfunkel"));
        assertTrue(BuscaArtista.mesmoNome("Beyoncé", "BEYONCE"));
        assertTrue(BuscaArtista.mesmoNome("Guns N' Roses", "Guns N Roses"));
        assertTrue("dupla: \"e\" vale \"&\" (1.5.1)", BuscaArtista.mesmoNome("Henrique e Juliano", "Henrique & Juliano"));
    }

    @Test
    public void mesmoNome_outroArtistaOuVazio() {
        assertFalse(BuscaArtista.mesmoNome("Coldplay", "Coldplay Tribute"));
        assertFalse(BuscaArtista.mesmoNome("", ""));
        assertFalse(BuscaArtista.mesmoNome((String) null, "Coldplay"));
        assertFalse(BuscaArtista.mesmoNome("!!!", "???"));
    }
}
