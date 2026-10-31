// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.archos.medialib.IMediaMetadataRetriever;
import com.archos.medialib.IMediaPlayer;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/** Leitura dos capítulos pelas chaves do motor e navegação (ir para, anterior com a D1, próximo). */
public class CapitulosTest {

    private static final int NB = IMediaPlayer.METADATA_KEY_NB_CHAPTER;

    /** Metadados falsos, como o motor publica: número de capítulos e, por capítulo, nome, início e fim. */
    private static final class Dados implements Capitulos.Fonte {
        final Map<Integer, Object> valores = new HashMap<>();

        Dados capitulo(int indice, String nome, long inicio, long fim) {
            int base = IMediaPlayer.METADATA_KEY_CHAPTER + indice * IMediaPlayer.METADATA_KEY_CHAPTER_MAX;
            if (nome != null) valores.put(base + IMediaPlayer.METADATA_KEY_CHAPTER_TITLE, nome);
            valores.put(base + IMediaPlayer.METADATA_KEY_CHAPTER_START, inicio);
            if (fim >= 0) valores.put(base + IMediaPlayer.METADATA_KEY_CHAPTER_END, fim);
            valores.put(NB, (long) Math.max(indice + 1, ((Long) valores.getOrDefault(NB, 0L)).intValue()));
            return this;
        }

        @Override
        public boolean tem(int chave) {
            return valores.containsKey(chave);
        }

        @Override
        public String texto(int chave) {
            return (String) valores.get(chave);
        }

        @Override
        public long numero(int chave) {
            return (Long) valores.get(chave);
        }
    }

    /** Três capítulos: 0–60 s, 60–150 s e 150–300 s. */
    private static Capitulos tres() {
        return Capitulos.ler(new Dados()
                .capitulo(0, "Abertura", 0, 60_000)
                .capitulo(1, "A ponte", 60_000, 150_000)
                .capitulo(2, "Final", 150_000, 300_000));
    }

    @Test
    public void semCapitulosNoArquivo_listaVaziaENenhumDestino() {
        Capitulos capitulos = Capitulos.ler(new Dados());
        assertTrue(capitulos.vazio());
        assertEquals(-1, capitulos.anterior(10_000));
        assertEquals(-1, capitulos.proximo(10_000));
        assertEquals(-1, capitulos.irPara(1));
        assertNull(capitulos.naPosicao(10_000));
    }

    @Test
    public void leitura_ordenaNumeraECompletaFimENome() {
        Capitulos capitulos = Capitulos.ler(new Dados()
                .capitulo(0, "Dois", 90_000, -1)
                .capitulo(1, null, 0, 90_000)
                .capitulo(2, "  Três  ", 200_000, 260_000));
        assertEquals(3, capitulos.quantidade());
        Capitulos.Capitulo primeiro = capitulos.lista().get(0);
        assertEquals(1, primeiro.numero);
        assertEquals("", primeiro.nome);
        assertEquals(0, primeiro.inicioMs);
        Capitulos.Capitulo segundo = capitulos.lista().get(1);
        assertEquals(2, segundo.numero);
        assertEquals("Dois", segundo.nome);
        assertEquals("fim que faltava = início do seguinte", 200_000, segundo.fimMs);
        assertEquals("Três", capitulos.lista().get(2).nome);
    }

    @Test
    public void irPara_inicioDoCapitulo() {
        Capitulos capitulos = tres();
        assertEquals(60_000, capitulos.irPara(2));
        assertEquals(-1, capitulos.irPara(0));
        assertEquals(-1, capitulos.irPara(4));
    }

    @Test
    public void anterior_seguePeloTempoNoCapitulo() {
        Capitulos capitulos = tres();
        assertEquals("até 3 s no capítulo: vai ao de antes", 0, capitulos.anterior(62_000));
        assertEquals("depois de 3 s: volta ao início deste", 60_000, capitulos.anterior(70_000));
        assertEquals("no primeiro: o início dele", 0, capitulos.anterior(1_000));
    }

    @Test
    public void anterior_antesDoPrimeiroCapitulo_vaiAoInicio() {
        Capitulos capitulos = Capitulos.ler(new Dados().capitulo(0, "Um", 5_000, 50_000));
        assertNull(capitulos.naPosicao(0));
        assertEquals(0, capitulos.anterior(0));
    }

    @Test
    public void proximo_inicioDoSeguinteENadaNoUltimo() {
        Capitulos capitulos = tres();
        assertEquals(60_000, capitulos.proximo(10_000));
        assertEquals(-1, capitulos.proximo(200_000));
    }

    @Test
    public void proximo_depoisDeIrParaUmCapituloQueCaiuUmPoucoAntes() {
        Capitulos capitulos = tres();
        // ir para o 2º caiu no quadro-chave 1 s antes do início: "próximo" leva ao 3º, não ao 2º de novo
        assertEquals(2, capitulos.naPosicao(59_000).numero);
        assertEquals(150_000, capitulos.proximo(59_000));
    }

    @Test
    public void leitor_valoresComoTexto() {
        IMediaMetadataRetriever leitor = mock(IMediaMetadataRetriever.class);
        Map<Integer, String> textos = new HashMap<>();
        int base = IMediaMetadataRetriever.METADATA_KEY_CHAPTER;
        int passo = IMediaMetadataRetriever.METADATA_KEY_CHAPTER_MAX;
        textos.put(IMediaMetadataRetriever.METADATA_KEY_NB_CHAPTER, "2");
        textos.put(base + IMediaMetadataRetriever.METADATA_KEY_CHAPTER_TITLE, "Abertura");
        textos.put(base + IMediaMetadataRetriever.METADATA_KEY_CHAPTER_START, "0");
        textos.put(base + IMediaMetadataRetriever.METADATA_KEY_CHAPTER_END, "80000");
        textos.put(base + passo + IMediaMetadataRetriever.METADATA_KEY_CHAPTER_TITLE, "");
        textos.put(base + passo + IMediaMetadataRetriever.METADATA_KEY_CHAPTER_START, "80000");
        textos.put(base + passo + IMediaMetadataRetriever.METADATA_KEY_CHAPTER_END, "120000");
        when(leitor.extractMetadata(anyInt())).thenAnswer(chamada -> textos.get((Integer) chamada.getArgument(0)));

        Capitulos capitulos = Capitulos.doLeitor(leitor);
        assertEquals(2, capitulos.quantidade());
        assertEquals("Abertura", capitulos.lista().get(0).nome);
        assertEquals(80_000, capitulos.lista().get(1).inicioMs);
        assertEquals(120_000, capitulos.lista().get(1).fimMs);
    }
}
