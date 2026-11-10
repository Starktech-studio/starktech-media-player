// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/** Limpar o cache (1.5): conta e apaga o que está dentro da pasta, e ela fica. */
public class CacheTest {

    @Rule
    public TemporaryFolder temporaria = new TemporaryFolder();

    private static void escrever(File arquivo, int bytes) throws IOException {
        //noinspection ResultOfMethodCallIgnored
        arquivo.getParentFile().mkdirs();
        try (FileOutputStream saida = new FileOutputStream(arquivo)) {
            saida.write(new byte[bytes]);
        }
    }

    @Test
    public void contaEApaga_mantendoAPasta() throws IOException {
        File cache = temporaria.newFolder("cache");
        escrever(new File(cache, "picasso-cache/a.0"), 300);
        escrever(new File(cache, "subtitles/Filme.srt"), 200);
        escrever(new File(cache, "atualizacao/atualizacao.apk"), 500);
        assertEquals(1000, Cache.tamanhoDe(cache));
        assertEquals(1000, Cache.apagarConteudo(cache));
        assertTrue("a pasta de cache fica", cache.isDirectory());
        assertEquals(0, cache.list().length);
        assertEquals(0, Cache.tamanhoDe(cache));
    }
}
