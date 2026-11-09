// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/** Nome em comum (D13): a chave vem da classificação, nunca do nome do arquivo; e o grupo dos shows (D7). */
public class ChaveNomeTest {

    private static final Pastas.Pasta FILMES = new Pastas.Pasta("/hd/Filmes", Pastas.Origem.HD_EXTERNO, Pastas.Tipo.FILMES, false);
    private static final Pastas.Pasta DESENHOS = new Pastas.Pasta("/hd/Desenhos", Pastas.Origem.HD_EXTERNO, Pastas.Tipo.DESENHOS, false);
    private static final Pastas.Pasta SERIES = new Pastas.Pasta("/hd/Séries", Pastas.Origem.HD_EXTERNO, Pastas.Tipo.SERIES, false);
    private static final Pastas.Pasta SHOWS = new Pastas.Pasta("/hd/Shows", Pastas.Origem.HD_EXTERNO, Pastas.Tipo.SHOWS, false);
    private static final Pastas.Pasta SEM_TIPO = new Pastas.Pasta("/hd/Downloads", Pastas.Origem.HD_EXTERNO, null, false);

    private static String chave(String caminho, Pastas.Pasta pasta) {
        return Classificador.chaveNome(Classificador.classificar(caminho, pasta));
    }

    @Test
    public void filme_tituloEAno_naoONomeDoArquivo() {
        String limpo = chave("/hd/Filmes/Nome do Filme (2012).mkv", FILMES);
        assertEquals(limpo, chave("/hd/Filmes/Nome.do.Filme.2012.1080p.mkv", FILMES));
        assertEquals(limpo, chave("/hd/Filmes/Nome do Filme (2012)/qualquer.nome.mp4", FILMES));
    }

    @Test
    public void filme_outroAnoOuSemAno_eOutroNome() {
        String com2012 = chave("/hd/Filmes/Nome do Filme (2012).mkv", FILMES);
        assertNotEquals(com2012, chave("/hd/Filmes/Nome do Filme (2020).mkv", FILMES));
        assertNotEquals(com2012, chave("/hd/Filmes/Nome do Filme.mkv", FILMES));
    }

    @Test
    public void semDiferenciarAcentoEMaiuscula() {
        assertEquals(chave("/hd/Filmes/Ação Final (2010).mkv", FILMES), chave("/hd/Filmes/ACAO FINAL [2010].mkv", FILMES));
    }

    @Test
    public void filmeEDesenhoDeMesmoNome_naoSeMisturam() {
        assertNotEquals(chave("/hd/Filmes/Nome (2012).mkv", FILMES), chave("/hd/Desenhos/Nome (2012).mkv", DESENHOS));
    }

    @Test
    public void episodio_serieMaisTemporadaEEpisodio() {
        String pelaPasta = chave("/hd/Séries/Nome da série/Temporada 1/02.mkv", SERIES);
        assertEquals("episodio:nome da serie|S01E02", pelaPasta);
        assertEquals(pelaPasta, chave("/hd/Séries/Nome da Serie/S01E02 - Piloto.mkv", SERIES));
        assertNotEquals(pelaPasta, chave("/hd/Séries/Nome da série/Temporada 1/03.mkv", SERIES));
        assertNotEquals(pelaPasta, chave("/hd/Séries/Nome da série/Temporada 2/02.mkv", SERIES));
    }

    @Test
    public void show_artistaETitulo() {
        String comAno = chave("/hd/Shows/Queen - Live (1986).mkv", SHOWS);
        assertEquals("show:queen|live", comAno);
        assertEquals(comAno, chave("/hd/Shows/Nacionais/QUEEN - Live.mkv", SHOWS));
        assertNotEquals(comAno, chave("/hd/Shows/Outro - Live.mkv", SHOWS));
        assertEquals("show:|live", chave("/hd/Shows/Live.mkv", SHOWS));
    }

    @Test
    public void naoIdentificado_naoTemNome() {
        assertNull(chave("/hd/Downloads/Nome (2012).mkv", SEM_TIPO));
        assertNull(Classificador.chaveNome(Classificador.Tipo.FILME, "", 2012, null, 0, 0, null));
    }

    @Test
    public void grupo_primeiraSubpastaDeShows() {
        assertEquals("Nacionais", Classificador.classificar("/hd/Shows/Nacionais/Artista - Show.mkv", SHOWS).grupo);
        assertEquals("Internacionais", Classificador.classificar("/hd/Shows/Internacionais/Rock/Artista - Show.mkv", SHOWS).grupo);
        assertNull(Classificador.classificar("/hd/Shows/Artista - Show.mkv", SHOWS).grupo);
        assertEquals("Artista", Classificador.classificar("/hd/Shows/Nacionais/Artista - Show.mkv", SHOWS).artista);
        assertNull(Classificador.classificar("/hd/Filmes/Nacionais/Nome (2012).mkv", FILMES).grupo);
    }
}
