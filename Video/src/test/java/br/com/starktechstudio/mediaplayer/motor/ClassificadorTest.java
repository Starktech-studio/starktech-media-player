// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/** Filmes, séries (D4: temporada e episódio), shows, não identificados, varredura e montagem das séries. */
public class ClassificadorTest {

    @Rule
    public TemporaryFolder temporaria = new TemporaryFolder();

    private static final Pastas.Pasta FILMES = new Pastas.Pasta("/hd/Filmes", Pastas.Origem.HD_EXTERNO, Pastas.Tipo.FILMES, false);
    private static final Pastas.Pasta SERIES = new Pastas.Pasta("/hd/Séries", Pastas.Origem.HD_EXTERNO, Pastas.Tipo.SERIES, false);
    private static final Pastas.Pasta SHOWS = new Pastas.Pasta("/hd/Shows", Pastas.Origem.HD_EXTERNO, Pastas.Tipo.SHOWS, false);
    private static final Pastas.Pasta SEM_TIPO = new Pastas.Pasta("/hd/Downloads", Pastas.Origem.HD_EXTERNO, null, false);
    private static final Pastas.Pasta IGNORAR = new Pastas.Pasta("/hd/Backup", Pastas.Origem.HD_EXTERNO, Pastas.Tipo.IGNORAR, true);

    private static Classificador.Item serie(String relativo) {
        return Classificador.classificar("/hd/Séries/" + relativo, SERIES);
    }

    private static void episodio(Classificador.Item item, String serie, int temporada, int episodio) {
        assertEquals(Classificador.Tipo.EPISODIO, item.tipo);
        assertEquals(serie, item.serie);
        assertEquals(temporada, item.temporada);
        assertEquals(episodio, item.episodio);
    }

    @Test
    public void pastaTvShows_classificaComoSerie() {
        Pastas.Pasta tvShows = new Pastas.Pasta("/hd/TV Shows", Pastas.Origem.HD_EXTERNO, Pastas.Tipo.TVSHOWS, false);
        episodio(Classificador.classificar("/hd/TV Shows/Programa/Temporada 2/03.mkv", tvShows), "Programa", 2, 3);
    }

    @Test
    public void filme_nomeEAno() {
        Classificador.Item item = Classificador.classificar("/hd/Filmes/Nome do filme (2012).mkv", FILMES);
        assertEquals(Classificador.Tipo.FILME, item.tipo);
        assertEquals("Nome do filme", item.titulo);
        assertEquals(Integer.valueOf(2012), item.ano);
    }

    @Test
    public void filme_variacoes() {
        Classificador.Item solto = Classificador.classificar("/hd/Filmes/Tears.of.Steel.2012.1080p.mkv", FILMES);
        assertEquals("Tears of Steel", solto.titulo);
        assertEquals(Integer.valueOf(2012), solto.ano);

        Classificador.Item naPasta = Classificador.classificar("/hd/Filmes/Sintel (2010)/sintel_4k.mkv", FILMES);
        assertEquals("Sintel", naPasta.titulo);
        assertEquals(Integer.valueOf(2010), naPasta.ano);

        Classificador.Item numero = Classificador.classificar("/hd/Filmes/Blade Runner 2049 (2017).mkv", FILMES);
        assertEquals("Blade Runner 2049", numero.titulo);
        assertEquals(Integer.valueOf(2017), numero.ano);

        Classificador.Item semAno = Classificador.classificar("/hd/Filmes/Spring.mp4", FILMES);
        assertEquals("Spring", semAno.titulo);
        assertNull(semAno.ano);
    }

    @Test
    public void serie_padraoDoInterface() {
        episodio(serie("Nome da série/Temporada 1/01.mkv"), "Nome da série", 1, 1);
    }

    @Test
    public void serie_pastasDeTemporada() {
        episodio(serie("Maré Alta/Temp 2/03.mkv"), "Maré Alta", 2, 3);
        episodio(serie("Maré Alta/Season 2/04.mkv"), "Maré Alta", 2, 4);
        episodio(serie("Maré Alta/T02/05.mkv"), "Maré Alta", 2, 5);
        episodio(serie("Maré Alta/S02/06.mkv"), "Maré Alta", 2, 6);
        episodio(serie("Maré Alta/TEMPORADA 02/07.mkv"), "Maré Alta", 2, 7);
    }

    @Test
    public void serie_codigosNoArquivo() {
        episodio(serie("Horizonte Azul/Temporada 1/Horizonte.Azul.S01E02.mkv"), "Horizonte Azul", 1, 2);
        episodio(serie("Horizonte Azul/Temporada 3/3x04.mkv"), "Horizonte Azul", 3, 4);
        episodio(serie("Horizonte Azul/Temporada 2/E05.mkv"), "Horizonte Azul", 2, 5);
        // S01E01 e 1x01 trazem a temporada: valem sem a pasta dela
        episodio(serie("Código Aurora/s01e08 - O fim.mkv"), "Código Aurora", 1, 8);
        assertEquals("O fim", serie("Código Aurora/s01e08 - O fim.mkv").titulo);
        episodio(serie("Código Aurora/1x07.mkv"), "Código Aurora", 1, 7);
    }

    @Test
    public void serie_diretoNaPastaSeries_nomeAntesDoCodigo() {
        Classificador.Item item = serie("Maré.Alta.2021.S02E01.1080p.mkv");
        episodio(item, "Maré Alta", 2, 1);
        assertEquals(Integer.valueOf(2021), item.ano);
    }

    @Test
    public void serie_anoNoNomeDaPasta() {
        Classificador.Item item = serie("Maré Alta (2021)/Temporada 1/01.mkv");
        episodio(item, "Maré Alta", 1, 1);
        assertEquals(Integer.valueOf(2021), item.ano);
    }

    @Test
    public void serie_foraDoPadrao() {
        assertEquals(Classificador.Motivo.SEM_TEMPORADA, serie("Maré Alta/01.mkv").motivo);
        assertEquals(Classificador.Motivo.SEM_TEMPORADA, serie("Maré Alta/Extras/02.mkv").motivo);
        assertEquals(Classificador.Motivo.SEM_EPISODIO, serie("Maré Alta/Temporada 1/making of.mkv").motivo);
        assertEquals(Classificador.Motivo.FORA_DO_PADRAO, serie("episodio sem numero.mkv").motivo);
        assertEquals(Classificador.Motivo.FUNDO_DEMAIS, serie("Maré Alta/Temporada 1/Disco 1/01.mkv").motivo);
        assertEquals(Classificador.Motivo.SEM_EPISODIO, serie("Maré Alta/Temporada 1/S01 ep 2 sem nome.mkv").motivo);
    }

    @Test
    public void show_artistaTituloEAno() {
        Classificador.Item item = Classificador.classificar("/hd/Shows/Artista - Show (2019).mkv", SHOWS);
        assertEquals(Classificador.Tipo.SHOW, item.tipo);
        assertEquals("Artista", item.artista);
        assertEquals("Show", item.titulo);
        assertEquals(Integer.valueOf(2019), item.ano);

        Classificador.Item semArtista = Classificador.classificar("/hd/Shows/Ao vivo no parque.mkv", SHOWS);
        assertNull(semArtista.artista);
        assertEquals("Ao vivo no parque", semArtista.titulo);
    }

    @Test
    public void pastaSemTipo_naoIdentificado_eIgnorarFicaDeFora() {
        assertEquals(Classificador.Motivo.PASTA_SEM_TIPO,
                Classificador.classificar("/hd/Downloads/VID_20240312_201544.mp4", SEM_TIPO).motivo);
        assertNull(Classificador.classificar("/hd/Backup/filme.mkv", IGNORAR));
        assertNull("não é vídeo", Classificador.classificar("/hd/Filmes/capa.jpg", FILMES));
    }

    @Test
    public void temporadaDaPasta() {
        assertEquals(4, Classificador.temporadaDaPasta("Temporada 4"));
        assertEquals(4, Classificador.temporadaDaPasta("temp 4"));
        assertEquals(4, Classificador.temporadaDaPasta("Season.4"));
        assertEquals(4, Classificador.temporadaDaPasta("t04"));
        assertEquals(4, Classificador.temporadaDaPasta("S04"));
        assertEquals(-1, Classificador.temporadaDaPasta("Extras"));
    }

    @Test
    public void varrer_montarSeriesENaoIdentificados() throws IOException {
        File hd = temporaria.newFolder("hd");
        criar(hd, "Séries/Maré Alta/Temporada 2/02.mkv");
        criar(hd, "Séries/Maré Alta/Temporada 1/02.mkv");
        criar(hd, "Séries/Maré Alta/Temporada 1/01.mkv");
        criar(hd, "Séries/maré alta/Temporada 1/legenda.srt");
        criar(hd, "Séries/Horizonte Azul/S01E01.mkv");
        criar(hd, "Séries/Horizonte Azul/sem numero.mkv");
        criar(hd, "Séries/.oculta/S01E01.mkv");
        criar(hd, "Séries/Shows dentro/Artista - Show (2019).mkv");
        criar(hd, "Backup/qualquer.mkv");
        List<Pastas.Pasta> pastas = Arrays.asList(
                new Pastas.Pasta(new File(hd, "Séries").getAbsolutePath(), Pastas.Origem.HD_EXTERNO, Pastas.Tipo.SERIES, false),
                new Pastas.Pasta(new File(hd, "Séries/Shows dentro").getAbsolutePath(), Pastas.Origem.HD_EXTERNO, Pastas.Tipo.SHOWS, true),
                new Pastas.Pasta(new File(hd, "Backup").getAbsolutePath(), Pastas.Origem.HD_EXTERNO, Pastas.Tipo.IGNORAR, true));

        List<Classificador.Item> itens = Classificador.varrer(pastas);
        assertEquals("4 episódios + 1 não identificado + 1 show", 6, itens.size());

        List<Classificador.Serie> series = Classificador.montarSeries(itens);
        assertEquals(2, series.size());
        assertEquals("Horizonte Azul", series.get(0).nome);
        Classificador.Serie mare = series.get(1);
        assertEquals("Maré Alta", mare.nome);
        assertEquals(3, mare.episodios());
        assertEquals(Arrays.asList(1, 2), Arrays.asList(mare.temporadas.keySet().toArray(new Integer[0])));
        assertEquals(1, mare.temporadas.get(1).get(0).episodio);
        assertEquals(2, mare.temporadas.get(1).get(1).episodio);

        List<Classificador.Item> fora = Classificador.naoIdentificados(itens);
        assertEquals(1, fora.size());
        assertTrue(fora.get(0).caminho.endsWith("sem numero.mkv"));

        long shows = itens.stream().filter(i -> i.tipo == Classificador.Tipo.SHOW).count();
        assertEquals("a subpasta cadastrada fica com o tipo dela", 1, shows);
    }

    private static void criar(File raiz, String relativo) throws IOException {
        File arquivo = new File(raiz, relativo);
        assertTrue(arquivo.getParentFile().isDirectory() || arquivo.getParentFile().mkdirs());
        assertTrue(arquivo.createNewFile());
    }
}
