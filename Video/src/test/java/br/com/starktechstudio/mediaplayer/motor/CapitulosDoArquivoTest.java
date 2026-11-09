// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Reserva em Java, no MKV: acha o Chapters depois dos Clusters, pelo 1º ou pelo 2º SeekHead. */
public class CapitulosDoArquivoTest {

    private static final int EBML = 0x1A45DFA3;
    private static final int SEGMENT = 0x18538067;
    private static final int SEEK_HEAD = 0x114D9B74;
    private static final int SEEK = 0x4DBB;
    private static final int SEEK_ID = 0x53AB;
    private static final int SEEK_POSITION = 0x53AC;
    private static final int CLUSTER = 0x1F43B675;
    private static final int CHAPTERS = 0x1043A770;
    private static final int EDITION_ENTRY = 0x45B9;
    private static final int CHAPTER_ATOM = 0xB6;
    private static final int CHAPTER_TIME_START = 0x91;
    private static final int CHAPTER_DISPLAY = 0x80;
    private static final int CHAP_STRING = 0x85;

    @Rule
    public TemporaryFolder pasta = new TemporaryFolder();

    /** Elemento com o tamanho sempre em 8 bytes: o cabeçalho não muda com o valor, as posições fecham. */
    private static byte[] elemento(int id, byte[]... corpos) {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        int bytesId = (id >>> 24) != 0 ? 4 : (id >>> 16) != 0 ? 3 : (id >>> 8) != 0 ? 2 : 1;
        for (int i = bytesId - 1; i >= 0; i--) saida.write((id >>> (8 * i)) & 0xFF);
        int total = 0;
        for (byte[] corpo : corpos) total += corpo.length;
        saida.write(0x01);
        for (int i = 6; i >= 0; i--) saida.write((int) (((long) total >>> (8 * i)) & 0xFF));
        for (byte[] corpo : corpos) saida.write(corpo, 0, corpo.length);
        return saida.toByteArray();
    }

    private static byte[] seekHead(int alvo, long posicao) {
        return elemento(SEEK_HEAD, elemento(SEEK,
                elemento(SEEK_ID, ByteBuffer.allocate(4).putInt(alvo).array()),
                elemento(SEEK_POSITION, ByteBuffer.allocate(8).putLong(posicao).array())));
    }

    private static byte[] capitulo(long inicioMs, String nome) {
        return elemento(CHAPTER_ATOM,
                elemento(CHAPTER_TIME_START, ByteBuffer.allocate(8).putLong(inicioMs * 1_000_000L).array()),
                elemento(CHAPTER_DISPLAY, elemento(CHAP_STRING, nome.getBytes(StandardCharsets.UTF_8))));
    }

    private static byte[] capitulos() {
        return elemento(CHAPTERS, elemento(EDITION_ENTRY, capitulo(0, "Um"), capitulo(60_000, "Dois")));
    }

    private Capitulos ler(byte[]... dentroDoSegmento) throws IOException {
        byte[] arquivo = junto(elemento(EBML, new byte[4]), elemento(SEGMENT, dentroDoSegmento));
        File mkv = pasta.newFile("video.mkv");
        Files.write(mkv.toPath(), arquivo);
        return CapitulosDoArquivo.ler(mkv.getPath());
    }

    private static byte[] junto(byte[]... partes) {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        for (byte[] parte : partes) saida.write(parte, 0, parte.length);
        return saida.toByteArray();
    }

    private static void confereDois(Capitulos lidos) {
        assertEquals(2, lidos.quantidade());
        assertEquals("Um", lidos.lista().get(0).nome);
        assertEquals("Dois", lidos.lista().get(1).nome);
        assertEquals(60_000, lidos.lista().get(1).inicioMs);
    }

    @Test
    public void capitulosAntesDoPrimeiroCluster() throws IOException {
        confereDois(ler(capitulos(), elemento(CLUSTER, new byte[32])));
    }

    @Test
    public void primeiroSeekHeadApontaOsCapitulos() throws IOException {
        byte[] cluster = elemento(CLUSTER, new byte[32]);
        long posicao = seekHead(CHAPTERS, 0).length + cluster.length;
        confereDois(ler(seekHead(CHAPTERS, posicao), cluster, capitulos()));
    }

    @Test
    public void segundoSeekHeadDepoisDosClustersApontaOsCapitulos() throws IOException {
        byte[] cluster = elemento(CLUSTER, new byte[32]);
        long tamanho = seekHead(CHAPTERS, 0).length;
        long posicaoDoSegundo = tamanho + cluster.length;
        long posicaoDosCapitulos = posicaoDoSegundo + tamanho;
        confereDois(ler(seekHead(SEEK_HEAD, posicaoDoSegundo), cluster,
                seekHead(CHAPTERS, posicaoDosCapitulos), capitulos()));
    }

    @Test
    public void seekHeadsApontandoUmParaOOutroNaoPrendem() throws IOException {
        byte[] cluster = elemento(CLUSTER, new byte[32]);
        long tamanho = seekHead(CHAPTERS, 0).length;
        assertTrue(ler(seekHead(SEEK_HEAD, tamanho + cluster.length), cluster, seekHead(SEEK_HEAD, 0)).vazio());
    }

    @Test
    public void seekHeadApontandoParaOutroElementoNaoAcha() throws IOException {
        byte[] cluster = elemento(CLUSTER, new byte[32]);
        long tamanho = seekHead(CHAPTERS, 0).length;
        assertTrue(ler(seekHead(SEEK_HEAD, tamanho), cluster).vazio());
    }
}
