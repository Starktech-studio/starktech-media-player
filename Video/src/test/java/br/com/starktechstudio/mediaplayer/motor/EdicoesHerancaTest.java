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

/** Edições por nome em comum (D13): quem herda, o que vale mais e como Restaurar original corta. */
@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, manifest = Config.NONE, sdk = 34)
public class EdicoesHerancaTest {

    private static final String NOME = "filme:nome|2012";

    @Rule
    public TemporaryFolder temporaria = new TemporaryFolder();

    private File pastaImagens;
    private BancoStarkTech banco;
    private Edicoes edicoes;

    @Before
    public void preparar() throws IOException {
        pastaImagens = temporaria.newFolder("imagens");
        banco = new BancoStarkTech(ApplicationProvider.getApplicationContext(), null);
        edicoes = new Edicoes(banco, pastaImagens);
    }

    private static Map<Edicoes.Campo, String> um(Edicoes.Campo campo, String valor) {
        Map<Edicoes.Campo, String> mapa = new EnumMap<>(Edicoes.Campo.class);
        mapa.put(campo, valor);
        return mapa;
    }

    @Test
    public void outroVideoDeMesmoNome_herda() {
        edicoes.salvar("/hd/a.mkv", NOME, um(Edicoes.Campo.TITULO, "Meu título"));
        assertEquals("Meu título", edicoes.valendo("/hd/b.mkv", NOME).get(Edicoes.Campo.TITULO));
    }

    @Test
    public void capaEFundo_tambemHerdam() {
        edicoes.salvar("/hd/a.mkv", NOME, um(Edicoes.Campo.CAPA, Edicoes.PREFIXO_COR + "#111111"));
        edicoes.salvar("/hd/a.mkv", NOME, um(Edicoes.Campo.FUNDO, Edicoes.PREFIXO_COR + "#222222"));
        Map<Edicoes.Campo, String> herdada = edicoes.valendo("/hd/b.mkv", NOME);
        assertEquals(Edicoes.PREFIXO_COR + "#111111", herdada.get(Edicoes.Campo.CAPA));
        assertEquals(Edicoes.PREFIXO_COR + "#222222", herdada.get(Edicoes.Campo.FUNDO));
    }

    @Test
    public void outroNomeOuSemNome_naoHerda() {
        edicoes.salvar("/hd/a.mkv", NOME, um(Edicoes.Campo.TITULO, "Meu título"));
        assertTrue(edicoes.valendo("/hd/b.mkv", "filme:outro|2012").isEmpty());
        assertTrue(edicoes.valendo("/hd/b.mkv", null).isEmpty());
    }

    @Test
    public void edicaoSemNome_naoPassaParaOutros() {
        edicoes.salvar("serie:nome", um(Edicoes.Campo.TITULO, "Só dela"));
        assertTrue(edicoes.valendo("/hd/b.mkv", NOME).isEmpty());
        assertEquals("Só dela", edicoes.valendo("serie:nome", null).get(Edicoes.Campo.TITULO));
    }

    @Test
    public void aDoProprioVideo_valeMaisQueAHerdada() {
        edicoes.salvar("/hd/b.mkv", NOME, um(Edicoes.Campo.TITULO, "Do próprio"), 1000);
        edicoes.salvar("/hd/a.mkv", NOME, um(Edicoes.Campo.TITULO, "De outro, mais novo"), 2000);
        assertEquals("Do próprio", edicoes.valendo("/hd/b.mkv", NOME).get(Edicoes.Campo.TITULO));
        assertEquals("De outro, mais novo", edicoes.valendo("/hd/a.mkv", NOME).get(Edicoes.Campo.TITULO));
    }

    @Test
    public void entreHerdadas_aMaisRecenteDeCadaCampo() {
        Map<Edicoes.Campo, String> primeira = um(Edicoes.Campo.TITULO, "Antigo");
        primeira.put(Edicoes.Campo.SINOPSE, "Só aqui");
        edicoes.salvar("/hd/a.mkv", NOME, primeira, 1000);
        edicoes.salvar("/hd/b.mkv", NOME, um(Edicoes.Campo.TITULO, "Recente"), 2000);
        edicoes.salvar("/hd/c.mkv", NOME, um(Edicoes.Campo.TITULO, "Mais antigo"), 500);
        Map<Edicoes.Campo, String> herdada = edicoes.valendo("/hd/novo.mkv", NOME);
        assertEquals("Recente", herdada.get(Edicoes.Campo.TITULO));
        assertEquals("Só aqui", herdada.get(Edicoes.Campo.SINOPSE));
    }

    @Test
    public void lerEEditado_soODoProprio() {
        edicoes.salvar("/hd/a.mkv", NOME, um(Edicoes.Campo.TITULO, "Meu título"));
        assertTrue(edicoes.ler("/hd/b.mkv").isEmpty());
        assertFalse(edicoes.editado("/hd/b.mkv", Edicoes.Campo.TITULO));
    }

    @Test
    public void restaurarNoQueHerdou_cortaSoDele() {
        edicoes.salvar("/hd/a.mkv", NOME, um(Edicoes.Campo.TITULO, "Meu título"));
        edicoes.restaurarOriginal("/hd/b.mkv", NOME);
        assertTrue(edicoes.valendo("/hd/b.mkv", NOME).isEmpty());
        assertTrue(edicoes.ler("/hd/b.mkv").isEmpty());
        assertEquals("Meu título", edicoes.valendo("/hd/c.mkv", NOME).get(Edicoes.Campo.TITULO));
        assertEquals("Meu título", edicoes.valendo("/hd/a.mkv", NOME).get(Edicoes.Campo.TITULO));
    }

    @Test
    public void corteVale_paraEdicaoFeitaDepoisEmOutroVideo() {
        edicoes.restaurarOriginal("/hd/b.mkv", NOME);
        edicoes.salvar("/hd/a.mkv", NOME, um(Edicoes.Campo.TITULO, "Depois"));
        assertNull(edicoes.valendo("/hd/b.mkv", NOME).get(Edicoes.Campo.TITULO));
    }

    @Test
    public void editarDepoisDoCorte_valeSoAPropria() {
        edicoes.salvar("/hd/a.mkv", NOME, um(Edicoes.Campo.SINOPSE, "De outro"));
        edicoes.restaurarOriginal("/hd/b.mkv", NOME);
        edicoes.salvar("/hd/b.mkv", NOME, um(Edicoes.Campo.TITULO, "Meu"));
        Map<Edicoes.Campo, String> valendo = edicoes.valendo("/hd/b.mkv", NOME);
        assertEquals("Meu", valendo.get(Edicoes.Campo.TITULO));
        assertNull(valendo.get(Edicoes.Campo.SINOPSE));
    }

    @Test
    public void restaurarNaOrigem_osQueHerdavamVoltamAoOriginal() {
        edicoes.salvar("/hd/a.mkv", NOME, um(Edicoes.Campo.TITULO, "Meu título"));
        edicoes.restaurarOriginal("/hd/a.mkv", NOME);
        assertTrue(edicoes.valendo("/hd/b.mkv", NOME).isEmpty());
    }

    @Test
    public void imagemHerdada_ficaEnquantoOutroAUsa() throws IOException {
        File imagem = new File(pastaImagens, "capa.jpg");
        assertTrue(imagem.createNewFile());
        String referencia = Edicoes.PREFIXO_ARQUIVO + imagem.getAbsolutePath();
        edicoes.salvar("/hd/a.mkv", NOME, um(Edicoes.Campo.CAPA, referencia));
        // Restaurar quem só herdou não apaga a imagem de quem editou
        edicoes.restaurarOriginal("/hd/b.mkv", NOME);
        assertTrue(imagem.exists());
        assertEquals(referencia, edicoes.valendo("/hd/c.mkv", NOME).get(Edicoes.Campo.CAPA));
    }

    @Test
    public void gravadoNoBanco_herancaECorteVoltamNaOutraInstancia() {
        edicoes.salvar("/hd/a.mkv", NOME, um(Edicoes.Campo.TITULO, "Meu título"));
        edicoes.restaurarOriginal("/hd/b.mkv", NOME);
        Edicoes outra = new Edicoes(banco, pastaImagens);
        assertEquals("Meu título", outra.valendo("/hd/c.mkv", NOME).get(Edicoes.Campo.TITULO));
        assertTrue(outra.valendo("/hd/b.mkv", NOME).isEmpty());
        assertTrue("o corte não aparece como edição", outra.ler("/hd/b.mkv").isEmpty());
    }
}
