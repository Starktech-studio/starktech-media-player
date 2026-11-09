// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Application;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;

/** O artigo certo do artista e as fotos dele (sem rede), com respostas como as de verdade. */
@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, manifest = Config.NONE, sdk = 34)
public class WikipediaArtistaTest {

    private static final String BUSCA = "{\"query\":{\"pages\":["
            + "{\"title\":\"Queen (desambiguação)\",\"index\":3,\"description\":\"página de desambiguação\",\"extract\":\"Queen pode referir-se a: banda\"},"
            + "{\"title\":\"Freddie Mercury\",\"index\":2,\"description\":\"cantor britânico\",\"extract\":\"Farrokh Bulsara foi um cantor.\"},"
            + "{\"title\":\"Queen\",\"index\":1,\"description\":\"banda britânica de rock\",\"extract\":\"Queen foi uma banda britânica de rock, fundada em 1970.\"}]}}";

    @Test
    public void artigo_oDeNomeIgualQueFalaDeMusica() throws JSONException {
        WikipediaArtista.Artigo artigo = WikipediaArtista.lerArtigo(BUSCA, "QUEEN");
        assertEquals("Queen", artigo.titulo);
        assertEquals("Banda britânica de rock", artigo.resumo);
        assertTrue(artigo.texto.startsWith("Queen foi uma banda"));
    }

    @Test
    public void artigo_foraDeMusica_ouDeOutraCoisa_naoVale() throws JSONException {
        String rainha = "{\"query\":{\"pages\":[{\"title\":\"Queen\",\"index\":1,\"description\":\"título de nobreza\",\"extract\":\"Rainha é o título.\"}]}}";
        assertNull(WikipediaArtista.lerArtigo(rainha, "Queen"));
        String album = "{\"query\":{\"pages\":[{\"title\":\"Bad Omens (álbum)\",\"index\":1,\"extract\":\"É o álbum de estreia da banda.\"}]}}";
        assertNull(WikipediaArtista.lerArtigo(album, "Bad Omens"));
        assertNull(WikipediaArtista.lerArtigo("{\"batchcomplete\":true}", "Queen"));
    }

    @Test
    public void artigo_banda_comParentesesNoTitulo() throws JSONException {
        String genesis = "{\"query\":{\"pages\":[{\"title\":\"Genesis (banda)\",\"index\":1,\"description\":\"banda inglesa\",\"extract\":\"Genesis é uma banda.\"}]}}";
        assertEquals("Genesis (banda)", WikipediaArtista.lerArtigo(genesis, "Genesis").titulo);
    }

    @Test
    public void fotos_aPrincipalEAsQueCitamOArtista_soJpeg() throws JSONException {
        String lista = "{\"items\":["
                + item("Ficheiro:Foto_de_divulgacao.jpg", true, "//thumb.wikimedia.org/a/500px-a.jpg?utm_source=pt")
                + "," + item("Ficheiro:Queen_1984_011.jpg", false, "//thumb.wikimedia.org/b/500px-b.jpg")
                + "," + item("Ficheiro:Roy_Thomas_Baker.jpg", false, "//thumb.wikimedia.org/c/500px-c.jpg")
                + "," + item("Ficheiro:Queen_logo.svg", false, "//thumb.wikimedia.org/d/500px-d.png")
                + "]}";
        assertEquals(Arrays.asList("https://thumb.wikimedia.org/a/500px-a.jpg", "https://thumb.wikimedia.org/b/500px-b.jpg"),
                WikipediaArtista.lerFotos(lista, "Queen"));
    }

    private static String item(String titulo, boolean principal, String endereco) {
        return "{\"title\":\"" + titulo + "\",\"type\":\"image\",\"leadImage\":" + principal
                + ",\"srcset\":[{\"src\":\"" + endereco + "\",\"scale\":\"1x\"}]}";
    }
}
