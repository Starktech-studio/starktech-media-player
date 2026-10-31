// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Application;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

/** Edição à mão: salvar, ler, tirar um campo, restaurar original, imagens do aparelho e a regra do TMDB. */
@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, manifest = Config.NONE, sdk = 34)
public class EdicoesTest {

    @Rule
    public TemporaryFolder temporaria = new TemporaryFolder();

    private File pastaImagens;
    private Edicoes edicoes;

    @Before
    public void preparar() throws IOException {
        pastaImagens = temporaria.newFolder("imagens");
        // Banco só na memória: cada teste começa vazio
        edicoes = new Edicoes(new BancoStarkTech(ApplicationProvider.getApplicationContext(), null), pastaImagens);
    }

    private static Map<Edicoes.Campo, String> campos(Object... pares) {
        Map<Edicoes.Campo, String> mapa = new EnumMap<>(Edicoes.Campo.class);
        for (int i = 0; i < pares.length; i += 2) mapa.put((Edicoes.Campo) pares[i], (String) pares[i + 1]);
        return mapa;
    }

    @Test
    public void salvarELer_soOsCamposEditados() {
        edicoes.salvar("filme", campos(Edicoes.Campo.TITULO, "Meu título", Edicoes.Campo.ANO, "2012"));
        Map<Edicoes.Campo, String> lida = edicoes.ler("filme");
        assertEquals(2, lida.size());
        assertEquals("Meu título", lida.get(Edicoes.Campo.TITULO));
        assertTrue(edicoes.editado("filme", Edicoes.Campo.ANO));
        assertFalse(edicoes.editado("filme", Edicoes.Campo.SINOPSE));
        assertTrue(edicoes.ler("outro").isEmpty());
    }

    @Test
    public void salvarDeNovo_mantemOsOutrosETrocaOValor() {
        edicoes.salvar("filme", campos(Edicoes.Campo.TITULO, "Um", Edicoes.Campo.GENEROS, "Drama"));
        edicoes.salvar("filme", campos(Edicoes.Campo.TITULO, "Dois"));
        Map<Edicoes.Campo, String> lida = edicoes.ler("filme");
        assertEquals("Dois", lida.get(Edicoes.Campo.TITULO));
        assertEquals("Drama", lida.get(Edicoes.Campo.GENEROS));
    }

    @Test
    public void valorNulo_tiraAEdicaoDoCampo() {
        edicoes.salvar("filme", campos(Edicoes.Campo.TITULO, "Um", Edicoes.Campo.ANO, "2012"));
        edicoes.salvar("filme", Edicoes.Campo.ANO, null);
        assertFalse(edicoes.editado("filme", Edicoes.Campo.ANO));
        assertTrue(edicoes.editado("filme", Edicoes.Campo.TITULO));
    }

    @Test
    public void gravaNoBanco_outraInstanciaLeOMesmo() {
        BancoStarkTech banco = new BancoStarkTech(ApplicationProvider.getApplicationContext(), null);
        new Edicoes(banco, pastaImagens).salvar("serie", Edicoes.Campo.SINOPSE, "Nova sinopse");
        assertEquals("Nova sinopse", new Edicoes(banco, pastaImagens).ler("serie").get(Edicoes.Campo.SINOPSE));
    }

    @Test
    public void restaurarOriginal_apagaTudoEAImagemDoAparelho() throws IOException {
        File imagem = new File(pastaImagens, "capa.jpg");
        assertTrue(imagem.createNewFile());
        edicoes.salvar("filme", campos(Edicoes.Campo.TITULO, "Um", Edicoes.Campo.CAPA, Edicoes.PREFIXO_ARQUIVO + imagem.getAbsolutePath()));
        edicoes.restaurarOriginal("filme");
        assertTrue(edicoes.ler("filme").isEmpty());
        assertFalse("a imagem copiada sai junto", imagem.exists());
    }

    @Test
    public void trocarAImagem_apagaAAntiga() throws IOException {
        File antiga = new File(pastaImagens, "antiga.png");
        assertTrue(antiga.createNewFile());
        edicoes.salvar("filme", Edicoes.Campo.FUNDO, Edicoes.PREFIXO_ARQUIVO + antiga.getAbsolutePath());
        edicoes.salvar("filme", Edicoes.Campo.FUNDO, Edicoes.PREFIXO_COR + "#1E293B");
        assertFalse(antiga.exists());
    }

    @Test
    public void imagemUsadaPorOutroTitulo_naoEApagada() throws IOException {
        File imagem = new File(pastaImagens, "compartilhada.webp");
        assertTrue(imagem.createNewFile());
        String referencia = Edicoes.PREFIXO_ARQUIVO + imagem.getAbsolutePath();
        edicoes.salvar("a", Edicoes.Campo.CAPA, referencia);
        edicoes.salvar("b", Edicoes.Campo.CAPA, referencia);
        edicoes.restaurarOriginal("a");
        assertTrue(imagem.exists());
    }

    @Test
    public void imagemForaDaPastaDoApp_nuncaEApagada() throws IOException {
        File deFora = temporaria.newFile("do_usuario.jpg");
        edicoes.salvar("filme", Edicoes.Campo.CAPA, Edicoes.PREFIXO_ARQUIVO + deFora.getAbsolutePath());
        edicoes.restaurarOriginal("filme");
        assertTrue(deFora.exists());
    }

    @Test
    public void juntar_oEditadoSempreVale() {
        Map<Edicoes.Campo, String> doTmdb = campos(Edicoes.Campo.TITULO, "Título do TMDB",
                Edicoes.Campo.SINOPSE, "Sinopse do TMDB", Edicoes.Campo.ANO, "2010");
        Map<Edicoes.Campo, String> edicao = campos(Edicoes.Campo.TITULO, "Título meu");
        Map<Edicoes.Campo, String> tela = Edicoes.juntar(doTmdb, edicao);
        assertEquals("Título meu", tela.get(Edicoes.Campo.TITULO));
        assertEquals("Sinopse do TMDB", tela.get(Edicoes.Campo.SINOPSE));
        assertEquals("2010", tela.get(Edicoes.Campo.ANO));
    }

    @Test
    public void arquivoDe_soDaReferenciaDeArquivo() {
        assertEquals("/x/y.jpg", Edicoes.arquivoDe(Edicoes.PREFIXO_ARQUIVO + "/x/y.jpg"));
        assertNull(Edicoes.arquivoDe(Edicoes.PREFIXO_COR + "#000000"));
        assertNull(Edicoes.arquivoDe(null));
    }
}
