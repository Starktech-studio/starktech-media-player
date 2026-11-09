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

/** Respostas da TheAudioDB e do Deezer e a comparação do nome do artista (sem rede). */
@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, manifest = Config.NONE, sdk = 34)
public class BuscaArtistaTest {

    private static final String THEAUDIODB = "{\"artists\":[{\"idArtist\":\"1\",\"strArtist\":\"Coldplay\",\"strArtistAlternate\":null,"
            + "\"strCountry\":\"London, England\",\"strArtistThumb\":\"https://r2.theaudiodb.com/thumb.jpg\","
            + "\"strArtistFanart\":\"https://r2.theaudiodb.com/fanart.jpg\",\"strArtistWideThumb\":\"https://r2.theaudiodb.com/wide.jpg\"}]}";

    private static final String DEEZER = "{\"data\":["
            + "{\"id\":2,\"name\":\"Coldplay Tribute\",\"picture_big\":\"https://cdn.dzcdn.net/images/artist/errado/500x500.jpg\"},"
            + "{\"id\":892,\"name\":\"Coldplay\",\"picture_big\":\"https://cdn.dzcdn.net/images/artist/certo/500x500.jpg\","
            + "\"picture_xl\":\"https://cdn.dzcdn.net/images/artist/certo/1000x1000.jpg\"}],\"total\":2}";

    @Test
    public void theAudioDb_fotoFundoEPais() throws JSONException {
        BuscaArtista.Achado achado = BuscaArtista.lerTheAudioDb(THEAUDIODB, "coldplay");
        assertEquals("https://r2.theaudiodb.com/thumb.jpg", achado.foto);
        assertEquals("https://r2.theaudiodb.com/fanart.jpg", achado.fundo);
        assertEquals("London, England", achado.pais);
    }

    @Test
    public void theAudioDb_semFanart_usaALarga() throws JSONException {
        String semFanart = "{\"artists\":[{\"strArtist\":\"Coldplay\",\"strArtistThumb\":null,\"strArtistFanart\":null,"
                + "\"strArtistWideThumb\":\"https://r2.theaudiodb.com/wide.jpg\"}]}";
        BuscaArtista.Achado achado = BuscaArtista.lerTheAudioDb(semFanart, "Coldplay");
        assertNull("nulo no JSON não vira o texto \"null\"", achado.foto);
        assertEquals("https://r2.theaudiodb.com/wide.jpg", achado.fundo);
        assertNull(achado.pais);
    }

    @Test
    public void theAudioDb_semArtista_ouOutroNome() throws JSONException {
        assertNull(BuscaArtista.lerTheAudioDb("{\"artists\":null}", "Coldplay"));
        assertNull(BuscaArtista.lerTheAudioDb(THEAUDIODB, "Outra banda"));
    }

    @Test
    public void deezer_soONomeIgual() throws JSONException, IOException {
        assertEquals("https://cdn.dzcdn.net/images/artist/certo/500x500.jpg", BuscaArtista.lerDeezer(DEEZER, "COLDPLAY").foto);
        assertNull(BuscaArtista.lerDeezer(DEEZER, "Queen"));
        assertNull(BuscaArtista.lerDeezer("{\"data\":[],\"total\":0}", "Coldplay"));
    }

    @Test
    public void deezer_imagemPadraoNaoVale() throws JSONException, IOException {
        String semFoto = "{\"data\":[{\"name\":\"Coldplay\",\"picture_big\":\"https://cdn.dzcdn.net/images/artist//500x500.jpg\","
                + "\"picture_xl\":\"https://cdn.dzcdn.net/images/artist//1000x1000.jpg\"}]}";
        assertNull(BuscaArtista.lerDeezer(semFoto, "Coldplay").foto);
    }

    @Test(expected = IOException.class)
    public void deezer_erroComHttp200_valeComoSemResposta() throws JSONException, IOException {
        BuscaArtista.lerDeezer("{\"error\":{\"type\":\"Exception\",\"message\":\"Quota limit exceeded\",\"code\":4}}", "Coldplay");
    }

    /** Como vem de verdade: vários "Queen" pequenos antes do maior. */
    private static final String HOMONIMOS = "{\"data\":["
            + "{\"name\":\"Queen\",\"nb_fan\":44,\"picture_big\":\"https://cdn.dzcdn.net/images/artist/pequeno/500x500.jpg\"},"
            + "{\"name\":\"Queen\",\"nb_fan\":471,\"picture_big\":\"https://cdn.dzcdn.net/images/artist//500x500.jpg\"},"
            + "{\"name\":\"Queen\",\"nb_fan\":12862509,\"picture_big\":\"https://cdn.dzcdn.net/images/artist/banda/500x500.jpg\"},"
            + "{\"name\":\"Queen\",\"nb_fan\":2000000,\"picture_big\":\"https://cdn.dzcdn.net/images/artist/parecido/500x500.jpg\"},"
            + "{\"name\":\"Josiah Queen\",\"nb_fan\":12207,\"picture_big\":\"https://cdn.dzcdn.net/images/artist/outro/500x500.jpg\"}]}";

    @Test
    public void deezer_entreHomonimos_oDeMaisFas() throws JSONException, IOException {
        assertEquals("https://cdn.dzcdn.net/images/artist/banda/500x500.jpg", BuscaArtista.lerDeezer(HOMONIMOS, "Queen").foto);
    }

    @Test
    public void fotosDeezer_soONomeIgualEQuemTemPublicoPertoDoMaior() throws JSONException, IOException {
        assertEquals(Arrays.asList("https://cdn.dzcdn.net/images/artist/banda/500x500.jpg",
                "https://cdn.dzcdn.net/images/artist/parecido/500x500.jpg"), BuscaArtista.lerFotosDeezer(HOMONIMOS, "Queen"));
        assertTrue(BuscaArtista.lerFotosDeezer("{\"data\":[]}", "Coldplay").isEmpty());
    }

    @Test
    public void artistasDeezer_doMaisConhecidoAoMenos_comNomeESemQuemNaoTemFoto() throws JSONException, IOException {
        List<BuscaArtista.Encontrado> achados = BuscaArtista.lerArtistasDeezer(HOMONIMOS);
        assertEquals(4, achados.size());
        assertEquals(12862509, achados.get(0).fas);
        assertEquals("https://cdn.dzcdn.net/images/artist/banda/500x500.jpg", achados.get(0).foto);
        assertEquals("Josiah Queen", achados.get(2).nome);
    }

    @Test(expected = IOException.class)
    public void fotosDeezer_erroComHttp200_valeComoSemResposta() throws JSONException, IOException {
        BuscaArtista.lerFotosDeezer("{\"error\":{\"code\":4}}", "Coldplay");
    }

    @Test
    public void soHttps() throws JSONException {
        String inseguro = "{\"artists\":[{\"strArtist\":\"Coldplay\",\"strArtistThumb\":\"http://r2.theaudiodb.com/thumb.jpg\"}]}";
        assertNull(BuscaArtista.lerTheAudioDb(inseguro, "Coldplay").foto);
    }

    @Test
    public void mesmoNome_grafiasDiferentes() {
        assertTrue(BuscaArtista.mesmoNome("AC/DC", "ACDC"));
        assertTrue(BuscaArtista.mesmoNome("The Beatles", "Beatles"));
        assertTrue(BuscaArtista.mesmoNome("Simon & Garfunkel", "Simon and Garfunkel"));
        assertTrue(BuscaArtista.mesmoNome("Beyoncé", "BEYONCE"));
        assertTrue(BuscaArtista.mesmoNome("Guns N' Roses", "Guns N Roses"));
    }

    @Test
    public void mesmoNome_outroArtistaOuVazio() {
        assertFalse(BuscaArtista.mesmoNome("Coldplay", "Coldplay Tribute"));
        assertFalse(BuscaArtista.mesmoNome("", ""));
        assertFalse(BuscaArtista.mesmoNome(null, "Coldplay"));
        assertFalse(BuscaArtista.mesmoNome("!!!", "???"));
    }
}
