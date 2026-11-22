// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.musica;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Downloads (2.0, D10 e D11): a pasta {@code Music/StarkTech Media Player} espelha o caminho de dentro da pasta de
 * Músicas do servidor, e a cópia só vale com o mesmo tamanho (sem arquivo de controle).
 */
public class DownloadsTest {

    @Rule
    public TemporaryFolder temporaria = new TemporaryFolder();

    private static final String PASTA = "/storage/emulated/0/Music/StarkTech Media Player";

    @Test
    public void destino_espelhaOCaminhoDaPastaDeMusicas() {
        assertEquals(PASTA + "/Queen/A Night at the Opera/01 - Death on Two Legs.mp3",
                Downloads.destino(PASTA, "servidor:1/Musicas/Queen/A Night at the Opera/01 - Death on Two Legs.mp3",
                        "servidor:1/Musicas"));
    }

    @Test
    public void destino_foraDaPastaOuSemEla_soONome() {
        assertEquals(PASTA + "/01.mp3", Downloads.destino(PASTA, "servidor:1/Outra/Queen/01.mp3", "servidor:1/Musicas"));
        assertEquals(PASTA + "/01.mp3", Downloads.destino(PASTA, "servidor:1/Musicas/01.mp3", null));
        assertEquals("pasta de nome parecido não conta", PASTA + "/01.mp3",
                Downloads.destino(PASTA, "servidor:1/Musicas 2/01.mp3", "servidor:1/Musicas"));
    }

    @Test
    public void confere_peloTamanho() throws IOException {
        File copia = new File(temporaria.getRoot(), "01.mp3");
        try (FileOutputStream saida = new FileOutputStream(copia)) {
            saida.write(new byte[1234]);
        }
        assertTrue("mesmo tamanho do servidor: reconhecida", Downloads.confere(copia, 1234));
        assertFalse("cópia pela metade não vale", Downloads.confere(copia, 5000));
        assertFalse("sem tamanho no servidor não dá para conferir", Downloads.confere(copia, 0));
        assertFalse("sumiu", Downloads.confere(new File(temporaria.getRoot(), "02.mp3"), 1234));
        assertFalse("pasta não é cópia", Downloads.confere(temporaria.getRoot(), temporaria.getRoot().length()));
    }
}
