// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Application;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Músicas (1.5): pasta de Músicas, nome do arquivo e das pastas, álbuns, coletânea e playlists. */
@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, manifest = Config.NONE, sdk = 34)
public class MusicasTest {

    @Rule
    public TemporaryFolder temporaria = new TemporaryFolder();

    // ---- Pasta e formatos ----

    @Test
    public void pastaDeMusicas_reconhecidaPeloNome() {
        assertEquals(Pastas.Tipo.MUSICAS, Pastas.reconhecer("Músicas"));
        assertEquals(Pastas.Tipo.MUSICAS, Pastas.reconhecer("musica"));
        assertEquals(Pastas.Tipo.MUSICAS, Pastas.reconhecer("Music"));
    }

    @Test
    public void formatos_deAudio() {
        for (String nome : new String[]{"a.mp3", "b.M4A", "c.flac", "d.ogg", "e.opus", "f.wav", "g.aac", "h.oga"}) {
            assertTrue(nome, Nomes.ehAudio(nome));
        }
        assertFalse(Nomes.ehAudio("capa.jpg"));
        assertFalse(Nomes.ehAudio("filme.mkv"));
        assertTrue(Nomes.ehImagem("cover.JPG"));
    }

    @Test
    public void pastaDeMusicas_ficaForaDaVarreduraDeVideo() throws IOException {
        File musicas = temporaria.newFolder("hd", "Musicas");
        assertTrue(new File(musicas, "clipe.mp4").createNewFile());
        Pastas.Pasta pasta = new Pastas.Pasta(musicas.getAbsolutePath(), Pastas.Origem.HD_EXTERNO, Pastas.Tipo.MUSICAS, false);
        assertTrue(Classificador.varrer(Collections.singletonList(pasta)).isEmpty());
        assertNull(Classificador.classificar(new File(musicas, "clipe.mp4").getAbsolutePath(), pasta));
    }

    // ---- Nome do arquivo e das pastas ----

    private static TagsMusica.Tags doNome(String relativo) {
        TagsMusica.Tags tags = new TagsMusica.Tags();
        TagsMusica.completar(tags, relativo);
        return tags;
    }

    @Test
    public void semTag_artistaEAlbumPelasPastas() {
        TagsMusica.Tags t = doNome("Queen/A Night at the Opera (1975)/11 - Bohemian Rhapsody.mp3");
        assertEquals("Queen", t.artista);
        assertEquals("A Night at the Opera", t.album);
        assertEquals(Integer.valueOf(1975), t.ano);
        assertEquals(11, t.faixa);
        assertEquals("Bohemian Rhapsody", t.titulo);
    }

    @Test
    public void semTag_artistaEAlbumNumaPastaSo() {
        TagsMusica.Tags t = doNome("Legião Urbana - Dois/03. Tempo Perdido.flac");
        assertEquals("Legião Urbana", t.artista);
        assertEquals("Dois", t.album);
        assertEquals(3, t.faixa);
        assertEquals("Tempo Perdido", t.titulo);
    }

    @Test
    public void semTag_soltaNaPasta() {
        TagsMusica.Tags t = doNome("Coldplay - Yellow.mp3");
        assertEquals("Coldplay", t.artista);
        assertEquals("Yellow", t.titulo);
        assertNull(t.album);
        assertEquals(0, t.faixa);
    }

    @Test
    public void semTag_discoEFaixaEAnoNaFrenteDoAlbum() {
        TagsMusica.Tags t = doNome("Pink Floyd/1979 - The Wall/2-05 Comfortably Numb.mp3");
        assertEquals("The Wall", t.album);
        assertEquals(Integer.valueOf(1979), t.ano);
        assertEquals(2, t.disco);
        assertEquals(5, t.faixa);
        assertEquals("Comfortably Numb", t.titulo);
    }

    @Test
    public void tagVaiNaFrenteDoNome() {
        TagsMusica.Tags t = new TagsMusica.Tags();
        t.titulo = "Título da tag";
        t.artista = "Artista da tag";
        t.faixa = 7;
        TagsMusica.completar(t, "Outro/Álbum/01 - Nome do arquivo.mp3");
        assertEquals("Título da tag", t.titulo);
        assertEquals("Artista da tag", t.artista);
        assertEquals(7, t.faixa);
        assertEquals("Álbum", t.album);
    }

    @Test
    public void numeroEAno_dasTags() {
        assertEquals(3, TagsMusica.numero("3/12"));
        assertEquals(3, TagsMusica.numero("03"));
        assertEquals(0, TagsMusica.numero(null));
        assertEquals(0, TagsMusica.numero("sem"));
        assertEquals(Integer.valueOf(2019), TagsMusica.ano("2019-05-01"));
        assertEquals(Integer.valueOf(2019), TagsMusica.ano("20190501T000000.000Z"));
        assertNull(TagsMusica.ano(""));
        assertNull(TagsMusica.limpo("   "));
    }

    // ---- Álbuns ----

    private static AcervoMusicas.Musica musica(String caminho, String titulo, String artista, String artistaAlbum, String album,
                                                int faixa, Integer ano) {
        AcervoMusicas.Musica m = new AcervoMusicas.Musica();
        m.caminho = caminho;
        m.pasta = "/hd/Musicas";
        m.titulo = titulo;
        m.artista = artista;
        m.artistaAlbum = artistaAlbum;
        m.album = album;
        m.faixa = faixa;
        m.ano = ano;
        return m;
    }

    @Test
    public void albuns_porArtistaENome_faixasEmOrdem() {
        List<AcervoMusicas.Musica> lista = Arrays.asList(
                musica("/hd/Musicas/Queen/Opera/02.mp3", "B", "Queen", null, "Opera", 2, 1975),
                musica("/hd/Musicas/Queen/Opera/01.mp3", "A", "Queen", null, "Opera", 1, 1975),
                musica("/hd/Musicas/Queen/Jazz/01.mp3", "C", "Queen", null, "Jazz", 1, 1978));
        List<Discografia.Album> albuns = Discografia.albuns(lista);
        assertEquals(2, albuns.size());
        Discografia.Album opera = albuns.get(0);
        assertEquals("Opera", opera.nome);
        assertEquals("Queen", opera.artista);
        assertEquals(Integer.valueOf(1975), opera.ano);
        assertEquals("A", opera.musicas.get(0).titulo);
        assertEquals("B", opera.musicas.get(1).titulo);
    }

    @Test
    public void coletanea_semArtistaDoAlbum_viraVariosArtistas() {
        List<AcervoMusicas.Musica> lista = Arrays.asList(
                musica("/hd/Musicas/Hits/01.mp3", "Um", "Artista A", null, "Hits 90", 1, null),
                musica("/hd/Musicas/Hits/02.mp3", "Dois", "Artista B", null, "Hits 90", 2, null));
        List<Discografia.Album> albuns = Discografia.albuns(lista);
        assertEquals(1, albuns.size());
        assertTrue(albuns.get(0).variosArtistas);
        assertNull(albuns.get(0).artista);
    }

    @Test
    public void artistaDoAlbum_juntaPastasDiferentes() {
        List<AcervoMusicas.Musica> lista = Arrays.asList(
                musica("/hd/Musicas/CD1/01.mp3", "Um", "Queen", "Queen", "Live", 1, null),
                musica("/hd/Musicas/CD2/01.mp3", "Dois", "Queen", "QUEEN", "live", 1, null));
        assertEquals(1, Discografia.albuns(lista).size());
    }

    // ---- Playlists ----

    @Test
    public void playlists_criarRenomearMoverTirarEApagar() {
        BancoStarkTech banco = new BancoStarkTech(ApplicationProvider.getApplicationContext(), null);
        AcervoMusicas acervo = new AcervoMusicas(banco);
        long id = acervo.criarPlaylist("Viagem", Arrays.asList("/a.mp3", "/b.mp3", "/c.mp3"));
        AcervoMusicas.Playlist p = acervo.playlist(id);
        assertEquals("Viagem", p.nome);
        assertEquals(Arrays.asList("/a.mp3", "/b.mp3", "/c.mp3"), p.musicas);

        acervo.renomearPlaylist(id, "Estrada");
        acervo.gravarMusicas(id, AcervoMusicas.mover(acervo.playlist(id).musicas, 2, 0));
        assertEquals("Estrada", acervo.playlist(id).nome);
        assertEquals(Arrays.asList("/c.mp3", "/a.mp3", "/b.mp3"), acervo.playlist(id).musicas);

        List<String> semA = new ArrayList<>(acervo.playlist(id).musicas);
        semA.remove(1);
        acervo.gravarMusicas(id, semA);
        assertEquals(Arrays.asList("/c.mp3", "/b.mp3"), acervo.playlist(id).musicas);

        acervo.apagarPlaylist(id);
        assertNull(acervo.playlist(id));
        assertTrue(acervo.playlists().isEmpty());
    }

    @Test
    public void musicas_gravarEApagar() {
        AcervoMusicas acervo = new AcervoMusicas(new BancoStarkTech(ApplicationProvider.getApplicationContext(), null));
        AcervoMusicas.Musica m = musica("/hd/Musicas/x.mp3", "X", "Y", null, "Z", 4, 2001);
        m.adicionado = 10;
        m.lido = AcervoMusicas.LEITURA;
        acervo.gravar(Collections.singletonList(m), Collections.emptyList());
        AcervoMusicas.Musica lida = acervo.musicas().get(0);
        assertEquals("X", lida.titulo);
        assertEquals(Integer.valueOf(2001), lida.ano);
        assertEquals(4, lida.faixa);
        acervo.gravar(Collections.emptyList(), Collections.singletonList("/hd/Musicas/x.mp3"));
        assertTrue(acervo.musicas().isEmpty());
    }

    @Test
    public void mover_foraDosLimites_naoMexe() {
        List<String> lista = Arrays.asList("a", "b");
        assertEquals(lista, AcervoMusicas.mover(lista, 0, 5));
        assertEquals(lista, AcervoMusicas.mover(lista, -1, 0));
    }

    // ---- Capa ----

    @Test
    public void capaDaPasta_nomePreferidoOuAUnica() throws IOException {
        File album = temporaria.newFolder("album");
        assertTrue(new File(album, "outra.png").createNewFile());
        assertEquals("outra.png", CapasMusica.imagemDaPasta(album).getName());
        assertTrue(new File(album, "Folder.jpg").createNewFile());
        assertEquals("Folder.jpg", CapasMusica.imagemDaPasta(album).getName());
        File duas = temporaria.newFolder("duas");
        assertTrue(new File(duas, "a.jpg").createNewFile());
        assertTrue(new File(duas, "b.jpg").createNewFile());
        assertNull("duas imagens sem nome preferido: nenhuma", CapasMusica.imagemDaPasta(duas));
        assertEquals(1, CapasMusica.amostra(500, 500));
        assertEquals(2, CapasMusica.amostra(1200, 1200));
    }
}
