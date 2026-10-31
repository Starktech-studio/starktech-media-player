// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Raiz de cada armazenamento, filtro de imagens e ordem da lista do navegador. */
public class ArquivosDoAparelhoTest {

    @Rule
    public TemporaryFolder temporaria = new TemporaryFolder();

    @Test
    public void raizDe_pelaPastaDoAppNoVolume() {
        assertEquals("/storage/1A2B-3C4D",
                ArquivosDoAparelho.raizDe("/storage/1A2B-3C4D/Android/data/br.com.starktechstudio.mediaplayer/files"));
        assertNull(ArquivosDoAparelho.raizDe("/data/user/0/br.com.starktechstudio.mediaplayer/files"));
    }

    @Test
    public void ehImagem_pelaExtensao() {
        assertTrue(ArquivosDoAparelho.ehImagem("capa.JPG"));
        assertTrue(ArquivosDoAparelho.ehImagem("fundo.jpeg"));
        assertTrue(ArquivosDoAparelho.ehImagem("poster.webp"));
        assertTrue(ArquivosDoAparelho.ehImagem("logo.png"));
        assertFalse(ArquivosDoAparelho.ehImagem("filme.mkv"));
        assertFalse(ArquivosDoAparelho.ehImagem("animacao.gif"));
    }

    @Test
    public void listar_pastasPrimeiroDepoisImagensEmOrdemSemOcultos() throws IOException {
        File raiz = temporaria.newFolder("hd");
        assertTrue(new File(raiz, "Séries").mkdir());
        assertTrue(new File(raiz, "Filmes").mkdir());
        assertTrue(new File(raiz, ".oculta").mkdir());
        assertTrue(new File(raiz, "zeta.png").createNewFile());
        assertTrue(new File(raiz, "Água.jpg").createNewFile());
        assertTrue(new File(raiz, "video.mkv").createNewFile());

        List<String> nomes = new ArrayList<>();
        for (File item : ArquivosDoAparelho.listar(raiz, true)) nomes.add(item.getName());
        assertEquals(List.of("Filmes", "Séries", "Água.jpg", "zeta.png"), nomes);

        List<String> soPastas = new ArrayList<>();
        for (File item : ArquivosDoAparelho.listar(raiz, false)) soPastas.add(item.getName());
        assertEquals(List.of("Filmes", "Séries"), soPastas);
    }
}
