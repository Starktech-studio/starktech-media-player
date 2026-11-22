// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.dados;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.com.starktechstudio.mediaplayer.dados.ModeloMusicas.Musica;
import br.com.starktechstudio.mediaplayer.dados.ModeloMusicas.Origem;

/**
 * Origem de cada música e coleção (2.0, D16) e o filtro "Só baixadas" (D14): a baixada que também está no servidor
 * mostra Baixada; a coleção mostra a da maioria. Sem rede, as baixadas são as que ficam (D9). O ícone da origem só
 * aparece com algum servidor cadastrado (ajuste de 21/11/2026).
 */
public class ModeloMusicasOrigemTest {

    private static Musica musica(String id, Origem origem) {
        Musica m = new Musica();
        m.id = id;
        m.titulo = id;
        m.origem = origem;
        return m;
    }

    @Test
    public void origemDaMusica_aparelhoServidorOuBaixada() {
        Map<String, String> baixadas = new HashMap<>();
        baixadas.put("servidor:1/Musicas/a.mp3", "/storage/emulated/0/Music/StarkTech Media Player/a.mp3");
        assertEquals(Origem.APARELHO, RepositorioMusicas.origem("/storage/emulated/0/Music/b.mp3", baixadas));
        assertEquals(Origem.BAIXADA, RepositorioMusicas.origem("servidor:1/Musicas/a.mp3", baixadas));
        assertEquals(Origem.SERVIDOR, RepositorioMusicas.origem("servidor:1/Musicas/c.mp3", baixadas));
    }

    @Test
    public void colecao_aDaMaioria() {
        List<Musica> album = Arrays.asList(musica("1", Origem.SERVIDOR), musica("2", Origem.SERVIDOR), musica("3", Origem.BAIXADA));
        assertEquals(Origem.SERVIDOR, ModeloMusicas.daMaioria(album));
        assertEquals("empate: baixada", Origem.BAIXADA,
                ModeloMusicas.daMaioria(Arrays.asList(musica("1", Origem.SERVIDOR), musica("2", Origem.BAIXADA))));
        assertEquals("empate: servidor antes do aparelho", Origem.SERVIDOR,
                ModeloMusicas.daMaioria(Arrays.asList(musica("1", Origem.APARELHO), musica("2", Origem.SERVIDOR))));
        assertEquals("vazia: aparelho", Origem.APARELHO, ModeloMusicas.daMaioria(Collections.emptyList()));
        assertEquals("vídeos pelas contagens", Origem.APARELHO, ModeloMusicas.daMaioria(3, 0, 2));
        assertEquals(Origem.SERVIDOR, ModeloMusicas.daMaioria(0, 0, 1));
    }

    @Test
    public void soBaixadas_naMesmaOrdem() {
        List<Musica> playlist = new ArrayList<>(Arrays.asList(musica("c", Origem.BAIXADA), musica("a", Origem.SERVIDOR),
                musica("b", Origem.BAIXADA), musica("d", Origem.APARELHO)));
        assertTrue(ModeloMusicas.temBaixada(playlist));
        List<Musica> baixadas = ModeloMusicas.soBaixadas(playlist);
        assertEquals(2, baixadas.size());
        assertEquals("c", baixadas.get(0).id);
        assertEquals("b", baixadas.get(1).id);
        assertFalse(ModeloMusicas.temBaixada(Collections.singletonList(musica("a", Origem.SERVIDOR))));
        assertTrue(ModeloMusicas.soBaixadas(Collections.singletonList(musica("d", Origem.APARELHO))).isEmpty());
    }

    @Test
    public void iconeDaOrigem_soComServidorCadastrado() {
        for (Origem origem : Origem.values()) {
            assertNull("sem servidor: nenhuma capa nem detalhe mostra a origem", ModeloMusicas.origemAMostrar(origem, false));
            assertEquals("com servidor (ao alcance ou não): a origem de sempre", origem, ModeloMusicas.origemAMostrar(origem, true));
        }
        assertNull("sem origem: nada", ModeloMusicas.origemAMostrar(null, true));
    }
}
