// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Filmes, séries (D4: temporada e episódio), shows, não identificados, varredura e montagem das séries;
 * desenhos com episódios e coletânea de desenhos (1.8.0, D7 a D9 do plano 009); nome do episódio sem nome
 * próprio (2.0.1).
 */
public class ClassificadorTest {

    @Rule
    public TemporaryFolder temporaria = new TemporaryFolder();

    private static final Pastas.Pasta FILMES = new Pastas.Pasta("/hd/Filmes", Pastas.Origem.HD_EXTERNO, Pastas.Tipo.FILMES, false);
    private static final Pastas.Pasta SERIES = new Pastas.Pasta("/hd/Séries", Pastas.Origem.HD_EXTERNO, Pastas.Tipo.SERIES, false);
    private static final Pastas.Pasta SHOWS = new Pastas.Pasta("/hd/Shows", Pastas.Origem.HD_EXTERNO, Pastas.Tipo.SHOWS, false);
    private static final Pastas.Pasta DESENHOS = new Pastas.Pasta("/hd/Desenhos", Pastas.Origem.HD_EXTERNO, Pastas.Tipo.DESENHOS, false);
    private static final Pastas.Pasta SEM_TIPO = new Pastas.Pasta("/hd/Downloads", Pastas.Origem.HD_EXTERNO, null, false);
    private static final Pastas.Pasta IGNORAR = new Pastas.Pasta("/hd/Backup", Pastas.Origem.HD_EXTERNO, Pastas.Tipo.IGNORAR, true);

    private static Classificador.Item serie(String relativo) {
        return Classificador.classificar("/hd/Séries/" + relativo, SERIES);
    }

    private static Classificador.Item desenho(String relativo) {
        return Classificador.classificar("/hd/Desenhos/" + relativo, DESENHOS);
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
    public void desenho_noPadraoDeSeries_viraEpisodio() {
        episodio(desenho("Turma da Praia/Temporada 2/03.mkv"), "Turma da Praia", 2, 3);
        episodio(desenho("Turma da Praia/Temp 1/Turma.da.Praia.S01E04.mkv"), "Turma da Praia", 1, 4);
        episodio(desenho("Turma da Praia/3x05.mkv"), "Turma da Praia", 3, 5);
        episodio(desenho("Turma da Praia/Temporada 1/E06.mkv"), "Turma da Praia", 1, 6);
        // Direto na pasta Desenhos: só com o código, e o nome antes dele
        episodio(desenho("Turma da Praia S02E07.mkv"), "Turma da Praia", 2, 7);
    }

    @Test
    public void desenho_semPastaDeTemporada_valeComoTemporada1() {
        episodio(desenho("Turma da Praia/01.mkv"), "Turma da Praia", 1, 1);
        episodio(desenho("Turma da Praia/E02.mkv"), "Turma da Praia", 1, 2);
    }

    @Test
    public void desenho_parteN_viraEpisodio() {
        episodio(desenho("A Grande Viagem/A Grande Viagem - Parte 1.mkv"), "A Grande Viagem", 1, 1);
        episodio(desenho("A Grande Viagem/A.Grande.Viagem.Part.2.mkv"), "A Grande Viagem", 1, 2);
        episodio(desenho("A Grande Viagem/Temporada 2/parte_03.mkv"), "A Grande Viagem", 2, 3);
        // "Parte N" é só dos desenhos: em Séries continua sem episódio
        assertEquals(Classificador.Motivo.SEM_EPISODIO, serie("Maré Alta/Temporada 1/Parte 2.mkv").motivo);
    }

    @Test
    public void desenho_foraDoPadrao_continuaFilme() {
        Classificador.Item solto = desenho("Nome do desenho (2019).mkv");
        assertEquals(Classificador.Tipo.DESENHO, solto.tipo);
        assertEquals("Nome do desenho", solto.titulo);
        assertEquals(Integer.valueOf(2019), solto.ano);
        // Pasta de filmes (D8): "Disney / Nome (1999).mkv" não junta nada
        Classificador.Item naPasta = desenho("Disney/Viagem ao Mar 2 (1999).mkv");
        assertEquals(Classificador.Tipo.DESENHO, naPasta.tipo);
        assertEquals("Viagem ao Mar 2", naPasta.titulo);
        assertEquals(Classificador.Tipo.DESENHO, desenho("Disney/Pixar/Turma/01.mkv").tipo);
        assertEquals("pasta do meio que não é de temporada", Classificador.Tipo.DESENHO, desenho("Turma da Praia/Extras/02.mkv").tipo);
        assertEquals("direto na pasta, sem código", Classificador.Tipo.DESENHO, desenho("Parte 2.mkv").tipo);
        assertEquals(Classificador.Tipo.DESENHO, desenho("Turma da Praia/Temporada 1/making of.mkv").tipo);
    }

    @Test
    public void desenhoQueViraEpisodio_perdeALinhaDoTmdb() {
        Acervo.Video filme = new Acervo.Video();
        filme.tipo = Classificador.Tipo.DESENHO;
        Acervo.Video episodio = new Acervo.Video();
        episodio.tipo = Classificador.Tipo.EPISODIO;
        Acervo.Video outroFilme = new Acervo.Video();
        outroFilme.tipo = Classificador.Tipo.FILME;
        assertTrue("desenho → episódio", Varredura.mudouDeEpisodio(filme, episodio));
        assertTrue("episódio → desenho", Varredura.mudouDeEpisodio(episodio, filme));
        assertFalse("filme ↔ desenho: a busca é a mesma, a linha fica", Varredura.mudouDeEpisodio(outroFilme, filme));
        assertFalse("o mesmo tipo", Varredura.mudouDeEpisodio(episodio, episodio));
        assertFalse("vídeo novo", Varredura.mudouDeEpisodio(null, episodio));
    }

    @Test
    public void coletaneaDeDesenho_chavePorTipo() {
        String deFilme = Acervo.chaveColetanea("Coleção Brinquedos", false);
        String deDesenho = Acervo.chaveColetanea("Coleção Brinquedos", true);
        assertEquals("a de filmes não muda (a edição guardada vale)", Acervo.chaveColetanea("Coleção Brinquedos"), deFilme);
        assertNotEquals("filme e desenho da mesma coleção não se misturam", deFilme, deDesenho);
        assertTrue(Acervo.ehColetanea(deDesenho));
        assertTrue(Acervo.ehColetaneaDeDesenho(deDesenho));
        assertFalse(Acervo.ehColetaneaDeDesenho(deFilme));
        assertFalse("coleção de filmes que começa por \"Desenho\"",
                Acervo.ehColetaneaDeDesenho(Acervo.chaveColetanea("Desenho: a coleção", false)));
        assertEquals("sem diferenciar maiúscula e acento", deDesenho, Acervo.chaveColetanea("coleção brinquedos", true));
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

    @Test
    public void coletaneaComoSerie_numeroDoNomeDoArquivo() {
        assertEquals("número na frente", 3, Classificador.numeroNoNome("03 - A flauta mágica.mkv").numero);
        assertEquals("A flauta mágica", Classificador.numeroNoNome("03 - A flauta mágica.mkv").nome);
        assertEquals("S01E02", 2, Classificador.numeroNoNome("Os Smurfs S01E02.mkv").numero);
        assertEquals("1x05", 5, Classificador.numeroNoNome("Os Smurfs 1x05 - O ovo.mp4").numero);
        assertEquals("O ovo", Classificador.numeroNoNome("Os Smurfs 1x05 - O ovo.mp4").nome);
        assertEquals("E07", 7, Classificador.numeroNoNome("Os.Smurfs.E07.mkv").numero);
        assertEquals("Ep 2", 2, Classificador.numeroNoNome("Os Smurfs Ep 2.mkv").numero);
        assertEquals("Episódio 12", 12, Classificador.numeroNoNome("Os Smurfs - Episódio 12.mkv").numero);
        assertEquals("Parte 4", 4, Classificador.numeroNoNome("Os Smurfs Parte 4.avi").numero);
        assertEquals("sem número", -1, Classificador.numeroNoNome("Os Smurfs e a Flauta Mágica.mkv").numero);
        assertEquals("o ano não é número de episódio", -1, Classificador.numeroNoNome("1983 - Os Smurfs.mkv").numero);
        assertEquals("o número no meio do título também não", -1, Classificador.numeroNoNome("Os Smurfs 2 (2013).mkv").numero);
    }

    @Test
    public void coletaneaComoSerie_semNumeroPelaOrdemAlfabetica() {
        // Na ordem em que chegam (por ano, como na coletânea), numerados pela ordem alfabética dos arquivos
        List<String> arquivos = Arrays.asList("Smurfs - O Pântano.mkv", "Os Smurfs e a Flauta Mágica.mkv",
                "Smurfs - A Aldeia Perdida.mkv", "os smurfs - natal.mkv");
        assertArrayEquals(new int[]{4, 1, 3, 2}, Classificador.numerarEpisodios(arquivos));
        // Acento e maiúscula não mudam a ordem
        assertArrayEquals(new int[]{2, 1}, Classificador.numerarEpisodios(Arrays.asList("Érica.mkv", "abelha.mkv")));
        assertArrayEquals(new int[0], Classificador.numerarEpisodios(Arrays.<String>asList()));
    }

    @Test
    public void coletaneaComoSerie_comNumeroOsSemNumeroDepois() {
        // O número do nome vale; os sem número vêm depois do maior, em ordem alfabética
        List<String> arquivos = Arrays.asList("02 - O ovo.mkv", "Zebra.mkv", "01 - A flauta.mkv", "Abelha.mkv");
        assertArrayEquals(new int[]{2, 4, 1, 3}, Classificador.numerarEpisodios(arquivos));
    }

    @Test
    public void nomeDoEpisodio_semNomeProprioONomeDoArquivo() {
        // 2.0.1: sem nome próprio, o nome do arquivo sem a extensão (não o da série repetido)
        assertEquals("As Aventuras das Babás Smurf",
                Classificador.nomeDoEpisodio(null, "As Aventuras das Babás Smurf.mp4"));
        assertEquals("vazio conta como sem nome", "As Aventuras das Babás Smurf",
                Classificador.nomeDoEpisodio("  ", "As Aventuras das Babás Smurf.mp4"));
        // Limpo como os outros nomes tirados do arquivo: ponto e sublinhado viram espaço quando não há espaço
        assertEquals("A Flauta Magica", Classificador.nomeDoEpisodio(null, "A.Flauta_Magica.mkv"));
        assertEquals("só o número", "01", Classificador.nomeDoEpisodio(null, "01.mkv"));
        assertEquals("sem extensão", "Natal", Classificador.nomeDoEpisodio(null, "Natal"));
        assertEquals("", Classificador.nomeDoEpisodio(null, null));
    }

    @Test
    public void nomeDoEpisodio_oProprioVale() {
        // Do TMDB, editado à mão ou o que vem depois do número no nome do arquivo
        assertEquals("O Ovo de Ouro", Classificador.nomeDoEpisodio("O Ovo de Ouro", "Os Smurfs S01E02.mkv"));
        assertEquals("sem espaço sobrando", "A flauta", Classificador.nomeDoEpisodio(" A flauta ", "01 - A flauta.mkv"));
        assertEquals("A flauta", Classificador.nomeDoEpisodio(Classificador.numeroNoNome("01 - A flauta.mkv").nome,
                "01 - A flauta.mkv"));
    }

    private static void criar(File raiz, String relativo) throws IOException {
        File arquivo = new File(raiz, relativo);
        assertTrue(arquivo.getParentFile().isDirectory() || arquivo.getParentFile().mkdirs());
        assertTrue(arquivo.createNewFile());
    }
}
