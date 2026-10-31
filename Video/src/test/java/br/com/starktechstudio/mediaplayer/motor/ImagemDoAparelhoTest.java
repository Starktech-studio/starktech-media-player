// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/** Formato pelos primeiros bytes (não pela extensão), limite de 15 MB (D7) e cópia. */
public class ImagemDoAparelhoTest {

    @Rule
    public TemporaryFolder temporaria = new TemporaryFolder();

    private static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F', 0, 1};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D};
    private static final byte[] WEBP = {'R', 'I', 'F', 'F', 0x24, 0, 0, 0, 'W', 'E', 'B', 'P'};
    private static final byte[] GIF = {'G', 'I', 'F', '8', '9', 'a', 0, 0, 0, 0, 0, 0};

    @Test
    public void formato_pelosBytes() {
        assertEquals(ImagemDoAparelho.Formato.JPG, ImagemDoAparelho.formato(JPG, JPG.length));
        assertEquals(ImagemDoAparelho.Formato.PNG, ImagemDoAparelho.formato(PNG, PNG.length));
        assertEquals(ImagemDoAparelho.Formato.WEBP, ImagemDoAparelho.formato(WEBP, WEBP.length));
        assertNull("GIF não é aceito", ImagemDoAparelho.formato(GIF, GIF.length));
        assertNull("arquivo curto demais", ImagemDoAparelho.formato(PNG, 4));
        byte[] riffSemWebp = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'A', 'V', 'E'};
        assertNull("RIFF de áudio não é WebP", ImagemDoAparelho.formato(riffSemWebp, riffSemWebp.length));
    }

    @Test
    public void limite_15MB() {
        assertFalse(ImagemDoAparelho.excede(ImagemDoAparelho.LIMITE_BYTES));
        assertTrue(ImagemDoAparelho.excede(ImagemDoAparelho.LIMITE_BYTES + 1));
        assertFalse("tamanho desconhecido (-1) é conferido na cópia", ImagemDoAparelho.excede(-1));
    }

    @Test
    public void copiar_paraNoLimite() throws IOException {
        byte[] dados = new byte[1000];
        assertEquals(1000, ImagemDoAparelho.copiar(new ByteArrayInputStream(dados), new ByteArrayOutputStream(), 1000));
        assertEquals(-1, ImagemDoAparelho.copiar(new ByteArrayInputStream(dados), new ByteArrayOutputStream(), 999));
    }

    @Test
    public void importar_recusaFormatoErradoSemCopiar() throws IOException {
        File destino = temporaria.newFolder("imagens");
        ImagemDoAparelho.Resultado resultado = ImagemDoAparelho.importar(destino, new ByteArrayInputStream(GIF), GIF.length);
        assertEquals(ImagemDoAparelho.Erro.FORMATO, resultado.erro);
        assertEquals(0, destino.list().length);
    }

    @Test
    public void importar_recusaTamanhoInformadoAcimaDoLimite() throws IOException {
        File destino = temporaria.newFolder("imagens");
        ImagemDoAparelho.Resultado resultado = ImagemDoAparelho.importar(destino, new ByteArrayInputStream(JPG),
                ImagemDoAparelho.LIMITE_BYTES + 1);
        assertEquals(ImagemDoAparelho.Erro.GRANDE_DEMAIS, resultado.erro);
    }

    @Test
    public void importar_semTamanhoInformado_paraNaCopiaEApagaOPedaco() throws IOException {
        File destino = temporaria.newFolder("imagens");
        // PNG "infinito": cabeçalho certo e bytes até passar de 15 MB
        InputStream grande = new InputStream() {
            private long lidos;

            @Override
            public int read() {
                if (lidos > ImagemDoAparelho.LIMITE_BYTES + 10) return -1;
                return lidos < PNG.length ? PNG[(int) lidos++] & 0xFF : (int) (lidos++ & 0x7F);
            }
        };
        ImagemDoAparelho.Resultado resultado = ImagemDoAparelho.importar(destino, grande, -1);
        assertEquals(ImagemDoAparelho.Erro.GRANDE_DEMAIS, resultado.erro);
        assertEquals("o pedaço copiado sai da pasta", 0, destino.list().length);
    }
}
