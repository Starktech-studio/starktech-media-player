// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * Varredura pela camada dos arquivos (2.0, etapa 5) com a listagem falsa do servidor em casa: classifica como as
 * pastas do aparelho, guarda o tamanho e a data da listagem e, sem rede, o que é do servidor não some.
 */
public class VarreduraServidorTest {

    private static final Pastas.Pasta FILMES = new Pastas.Pasta("servidor:1/Filmes", Pastas.Origem.SERVIDOR, Pastas.Tipo.FILMES, false);
    private static final Pastas.Pasta SERIES = new Pastas.Pasta("servidor:1/Séries", Pastas.Origem.SERVIDOR, Pastas.Tipo.SERIES, false);

    private final ArquivosDaBibliotecaTest.RedeFalsa rede = new ArquivosDaBibliotecaTest.RedeFalsa();
    private boolean conectado = true;
    private final ArquivosDaBiblioteca camada = new ArquivosDaBiblioteca(ArquivosDaBibliotecaTest::endereco, rede, id -> conectado);

    private void servidorComFilmesESeries() {
        rede.pasta("smb://192.168.0.10/midia/Filmes/",
                ArquivosDaBibliotecaTest.arquivo("Nome do filme (2012).mkv", 4000),
                ArquivosDaBibliotecaTest.arquivo("capa.jpg", 10),
                ArquivosDaBibliotecaTest.pasta("Coletânea/"));
        rede.pasta("smb://192.168.0.10/midia/Filmes/Coletânea/",
                ArquivosDaBibliotecaTest.arquivo("Outro (1999).mp4", 3000));
        rede.pasta("smb://192.168.0.10/midia/Séries/",
                ArquivosDaBibliotecaTest.pasta("Programa"));
        rede.pasta("smb://192.168.0.10/midia/Séries/Programa/",
                ArquivosDaBibliotecaTest.pasta("Temporada 2"));
        rede.pasta("smb://192.168.0.10/midia/Séries/Programa/Temporada 2/",
                ArquivosDaBibliotecaTest.arquivo("Programa S02E03.mkv", 2000));
    }

    @Test
    public void varrer_servidor_classificaComoNoAparelho() {
        servidorComFilmesESeries();
        Classificador.Varrido varrido = Classificador.varrer(Arrays.asList(FILMES, SERIES), camada, Collections.emptySet());
        assertTrue(varrido.naoAbriram.isEmpty());
        assertEquals(3, varrido.itens.size());

        // Em ordem de nome: a subpasta Coletânea vem antes do arquivo
        Classificador.Item daColetanea = varrido.itens.get(0);
        assertEquals(Classificador.Tipo.FILME, daColetanea.tipo);
        assertEquals("servidor:1/Filmes/Coletânea/Outro (1999).mp4", daColetanea.caminho);
        Classificador.Item filme = varrido.itens.get(1);
        assertEquals("Nome do filme", filme.titulo);
        assertEquals(Integer.valueOf(2012), filme.ano);

        Classificador.Item episodio = varrido.itens.get(2);
        assertEquals(Classificador.Tipo.EPISODIO, episodio.tipo);
        assertEquals("Programa", episodio.serie);
        assertEquals(2, episodio.temporada);
        assertEquals(3, episodio.episodio);

        assertEquals("tamanho da listagem, sem pedido por arquivo", 4000,
                varrido.arquivos.get("servidor:1/Filmes/Nome do filme (2012).mkv").tamanho);
        assertFalse("a imagem não entra", varrido.arquivos.containsKey("servidor:1/Filmes/capa.jpg"));
    }

    @Test
    public void varrer_servidorFora_naoAbreENaoChamaARede() {
        servidorComFilmesESeries();
        conectado = false;
        Classificador.Varrido varrido = Classificador.varrer(Collections.singletonList(FILMES), camada, Collections.emptySet());
        assertTrue(varrido.itens.isEmpty());
        assertTrue(varrido.naoAbriram.contains("servidor:1/Filmes"));
        assertTrue(rede.pedidos.isEmpty());
    }

    @Test
    public void varrer_pastaPulada_contaComoNaoAberta() {
        servidorComFilmesESeries();
        Classificador.Varrido varrido = Classificador.varrer(Arrays.asList(FILMES, SERIES), camada,
                new HashSet<>(Collections.singletonList("servidor:1/Séries")));
        assertTrue(varrido.naoAbriram.contains("servidor:1/Séries"));
        assertEquals(2, varrido.itens.size());
        for (String pedido : rede.pedidos) assertFalse(pedido.contains("Séries"));
    }

    @Test
    public void subpastaQueNaoAbriu_soElaFica() {
        servidorComFilmesESeries();
        rede.pastas.remove("smb://192.168.0.10/midia/Filmes/Coletânea/");
        Classificador.Varrido varrido = Classificador.varrer(Collections.singletonList(FILMES), camada, Collections.emptySet());
        assertEquals(1, varrido.itens.size());
        assertTrue(varrido.dentroDeQuemNaoAbriu("servidor:1/Filmes/Coletânea/Outro (1999).mp4"));
        assertFalse(varrido.dentroDeQuemNaoAbriu("servidor:1/Filmes/Nome do filme (2012).mkv"));
    }

    @Test
    public void sumiu_semRede_oDoServidorFica() {
        conectado = false;
        Classificador.Varrido varrido = Classificador.varrer(Collections.singletonList(FILMES), camada, Collections.emptySet());
        Acervo.Video video = new Acervo.Video();
        video.caminho = "servidor:1/Filmes/Nome do filme (2012).mkv";
        video.pasta = "servidor:1/Filmes";
        assertFalse("servidor fora: não sumiu", Varredura.sumiu(video, varrido));
    }

    @Test
    public void sumiu_comRede_oQueSaiuDaListagemSome() {
        servidorComFilmesESeries();
        Classificador.Varrido varrido = Classificador.varrer(Collections.singletonList(FILMES), camada, Collections.emptySet());
        Acervo.Video video = new Acervo.Video();
        video.caminho = "servidor:1/Filmes/Apagado (2000).mkv";
        video.pasta = "servidor:1/Filmes";
        assertTrue(Varredura.sumiu(video, varrido));
    }

    @Test
    public void musicaDoServidor_semRedeNaoSome() {
        VarreduraMusicas.Lista rodada = new VarreduraMusicas.Lista();
        rodada.naoAbriram.add("servidor:1/Musicas");
        AcervoMusicas.Musica fora = new AcervoMusicas.Musica();
        fora.caminho = "servidor:1/Musicas/Queen/Opera/01.mp3";
        fora.pasta = "servidor:1/Musicas";
        assertFalse(VarreduraMusicas.sumiu(fora, rodada));

        VarreduraMusicas.Lista aberta = new VarreduraMusicas.Lista();
        assertTrue("servidor ao alcance e fora da listagem: sumiu", VarreduraMusicas.sumiu(fora, aberta));
    }

    @Test
    public void pastaDoServidor_dentroPeloCaminhoDaBiblioteca() {
        List<String> dentro = Arrays.asList("servidor:1/Filmes/a.mkv", "servidor:1/Filmes/Sub/b.mkv");
        for (String caminho : dentro) assertTrue(Pastas.dentro(caminho, "servidor:1/Filmes"));
        assertFalse(Pastas.dentro("servidor:10/Filmes/a.mkv", "servidor:1/Filmes"));
        assertFalse(Pastas.dentro("servidor:1/Filmes 2/a.mkv", "servidor:1/Filmes"));
    }
}
