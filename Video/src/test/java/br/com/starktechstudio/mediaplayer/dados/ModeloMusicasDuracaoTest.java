// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.dados;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import br.com.starktechstudio.mediaplayer.dados.ModeloMusicas.Musica;

/**
 * Selo do tempo total no álbum, na playlist e no álbum em vídeo (ajuste de 21/11/2026): a soma de tudo o que a
 * coleção tem, músicas e vídeos; a duração que não se sabe conta 0. Até 59 min, "N min"; depois, "H h M min".
 */
public class ModeloMusicasDuracaoTest {

    private static final long MINUTO = 60_000;

    private static Musica item(long duracaoMs, boolean video) {
        Musica m = new Musica();
        m.id = "i" + duracaoMs;
        m.duracaoMs = duracaoMs;
        m.video = video;
        return m;
    }

    @Test
    public void soma_musicasEVideos_desconhecidaContaZero() {
        List<Musica> videos = new ArrayList<>();
        for (int i = 0; i < 5; i++) videos.add(item(3 * MINUTO, true));
        assertEquals("5 vídeos de 3 min", 15 * MINUTO, ModeloMusicas.duracaoTotalMs(videos));
        assertEquals("15 min", ModeloMusicas.textoDaDuracao(ModeloMusicas.duracaoTotalMs(videos)));

        List<Musica> misturado = new ArrayList<>(videos);
        misturado.add(item(60 * MINUTO, false));
        misturado.add(item(0, false));
        misturado.add(item(-1, true));
        assertEquals("sem duração e duração inválida contam 0", 75 * MINUTO, ModeloMusicas.duracaoTotalMs(misturado));
        assertEquals(0, ModeloMusicas.duracaoTotalMs(Collections.<Musica>emptyList()));
    }

    @Test
    public void texto_minutosEHoras() {
        assertEquals("1 h 15 min", ModeloMusicas.textoDaDuracao(75 * MINUTO));
        assertEquals("hora cheia", "2 h", ModeloMusicas.textoDaDuracao(120 * MINUTO));
        assertEquals("59 min", ModeloMusicas.textoDaDuracao(59 * MINUTO));
        assertEquals("60 min vira hora", "1 h", ModeloMusicas.textoDaDuracao(60 * MINUTO));
        assertEquals("arredonda", "4 min", ModeloMusicas.textoDaDuracao(3 * MINUTO + 40_000));
        assertEquals("pouco tempo: 1 min no mínimo", "1 min", ModeloMusicas.textoDaDuracao(10_000));
        assertEquals("nada: 0 min (o selo some)", "0 min", ModeloMusicas.textoDaDuracao(0));
    }

    @Test
    public void album_somaAsFaixas() {
        ModeloMusicas.Album album = new ModeloMusicas.Album();
        album.musicas.add(item(4 * MINUTO, false));
        album.musicas.add(item(0, false));
        album.musicas.add(item(5 * MINUTO, false));
        assertEquals(9 * MINUTO, album.duracaoMs());
    }
}
