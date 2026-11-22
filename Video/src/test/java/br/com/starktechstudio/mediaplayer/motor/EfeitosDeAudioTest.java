// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Equalizador e volume extra (2.1): o ganho em milibéis, o passo do nível, os nomes e a ordem dos estilos. */
public class EfeitosDeAudioTest {

    // ---- Volume extra: % em milibéis ----

    @Test
    public void ganhoDeCadaPassoEmMilibeis() {
        int[] esperado = {0, 83, 158, 228, 292, 352, 408, 461, 511, 558, 602};
        for (int i = 0; i < esperado.length; i++) {
            int nivel = 100 + i * 10;
            assertEquals(nivel + " %", esperado[i], EfeitosDeAudio.ganhoEmMilibeis(nivel));
        }
    }

    @Test
    public void ganhoSegueAFormula() {
        for (int nivel = 100; nivel <= 200; nivel++) {
            int formula = (int) Math.round(20 * Math.log10(nivel / 100.0) * 100);
            assertEquals(nivel + " %", formula, EfeitosDeAudio.ganhoEmMilibeis(nivel));
        }
    }

    @Test
    public void ganhoForaDosLimites() {
        assertEquals(0, EfeitosDeAudio.ganhoEmMilibeis(99));
        assertEquals(0, EfeitosDeAudio.ganhoEmMilibeis(0));
        assertEquals(0, EfeitosDeAudio.ganhoEmMilibeis(-50));
        assertEquals(602, EfeitosDeAudio.ganhoEmMilibeis(201));
        assertEquals(602, EfeitosDeAudio.ganhoEmMilibeis(1000));
    }

    @Test
    public void ganhoNuncaDiminuiComONivel() {
        int antes = -1;
        for (int nivel = 100; nivel <= 200; nivel += 10) {
            int ganho = EfeitosDeAudio.ganhoEmMilibeis(nivel);
            assertTrue(nivel + " %", ganho > antes);
            antes = ganho;
        }
    }

    // ---- Volume extra: 100 a 200, de 10 em 10 ----

    @Test
    public void constantesDoPuxador() {
        assertEquals(100, EfeitosDeAudio.NIVEL_MINIMO);
        assertEquals(200, EfeitosDeAudio.NIVEL_MAXIMO);
        assertEquals(10, EfeitosDeAudio.NIVEL_PASSO);
        assertEquals(10, EfeitosDeAudio.POSICOES);
        assertEquals(150, EfeitosDeAudio.limitarNivel(EfeitosDeAudio.NIVEL_PADRAO));
    }

    @Test
    public void limitarNivelPrendeNaFaixa() {
        assertEquals(100, EfeitosDeAudio.limitarNivel(0));
        assertEquals(100, EfeitosDeAudio.limitarNivel(-10));
        assertEquals(100, EfeitosDeAudio.limitarNivel(99));
        assertEquals(200, EfeitosDeAudio.limitarNivel(201));
        assertEquals(200, EfeitosDeAudio.limitarNivel(Integer.MAX_VALUE));
        assertEquals(100, EfeitosDeAudio.limitarNivel(Integer.MIN_VALUE));
    }

    @Test
    public void limitarNivelVaiAoPassoMaisPerto() {
        assertEquals(150, EfeitosDeAudio.limitarNivel(150));
        assertEquals(150, EfeitosDeAudio.limitarNivel(154));
        assertEquals(160, EfeitosDeAudio.limitarNivel(155));
        assertEquals(110, EfeitosDeAudio.limitarNivel(106));
        assertEquals(100, EfeitosDeAudio.limitarNivel(104));
        assertEquals(200, EfeitosDeAudio.limitarNivel(196));
        for (int nivel = 100; nivel <= 200; nivel += 10) assertEquals(nivel, EfeitosDeAudio.limitarNivel(nivel));
    }

    @Test
    public void passoSobeEDesceDe10EmAte200() {
        assertEquals(160, EfeitosDeAudio.passo(150, 1));
        assertEquals(140, EfeitosDeAudio.passo(150, -1));
        // Qualquer direção positiva ou negativa é um passo só
        assertEquals(160, EfeitosDeAudio.passo(150, 5));
        assertEquals(140, EfeitosDeAudio.passo(150, -3));
        assertEquals(150, EfeitosDeAudio.passo(150, 0));
        // Para nas pontas
        assertEquals(200, EfeitosDeAudio.passo(200, 1));
        assertEquals(100, EfeitosDeAudio.passo(100, -1));
        // Nível fora do passo: ajusta antes de andar
        assertEquals(170, EfeitosDeAudio.passo(155, 1));
        assertEquals(100, EfeitosDeAudio.passo(30, -1));
    }

    @Test
    public void passosDaPontaAPonta() {
        int nivel = 100;
        for (int i = 0; i < EfeitosDeAudio.POSICOES; i++) nivel = EfeitosDeAudio.passo(nivel, 1);
        assertEquals(200, nivel);
        nivel = EfeitosDeAudio.passo(nivel, 1);
        assertEquals(200, nivel);
        for (int i = 0; i < EfeitosDeAudio.POSICOES; i++) nivel = EfeitosDeAudio.passo(nivel, -1);
        assertEquals(100, nivel);
    }

    @Test
    public void posicaoENivelIdaEVolta() {
        for (int posicao = 0; posicao <= EfeitosDeAudio.POSICOES; posicao++) {
            int nivel = EfeitosDeAudio.nivelDaPosicao(posicao);
            assertEquals(100 + posicao * 10, nivel);
            assertEquals(posicao, EfeitosDeAudio.posicaoDoNivel(nivel));
        }
        assertEquals(100, EfeitosDeAudio.nivelDaPosicao(-1));
        assertEquals(200, EfeitosDeAudio.nivelDaPosicao(EfeitosDeAudio.POSICOES + 3));
        assertEquals(0, EfeitosDeAudio.posicaoDoNivel(50));
        assertEquals(EfeitosDeAudio.POSICOES, EfeitosDeAudio.posicaoDoNivel(500));
        assertEquals(6, EfeitosDeAudio.posicaoDoNivel(155));
    }

    // ---- Equalizador: nomes dos estilos ----

    @Test
    public void nomesDeFabricaEmPortugues() {
        assertEquals("Normal", EfeitosDeAudio.nomeDoEstilo("Normal"));
        assertEquals("Clássica", EfeitosDeAudio.nomeDoEstilo("Classical"));
        assertEquals("Dance", EfeitosDeAudio.nomeDoEstilo("Dance"));
        assertEquals("Plano", EfeitosDeAudio.nomeDoEstilo("Flat"));
        assertEquals("Folk", EfeitosDeAudio.nomeDoEstilo("Folk"));
        assertEquals("Heavy metal", EfeitosDeAudio.nomeDoEstilo("Heavy Metal"));
        assertEquals("Hip hop", EfeitosDeAudio.nomeDoEstilo("Hip Hop"));
        assertEquals("Jazz", EfeitosDeAudio.nomeDoEstilo("Jazz"));
        assertEquals("Pop", EfeitosDeAudio.nomeDoEstilo("Pop"));
        assertEquals("Rock", EfeitosDeAudio.nomeDoEstilo("Rock"));
    }

    @Test
    public void nomesComOutraEscrita() {
        assertEquals("Clássica", EfeitosDeAudio.nomeDoEstilo("CLASSICAL"));
        assertEquals("Plano", EfeitosDeAudio.nomeDoEstilo("  flat "));
        assertEquals("Hip hop", EfeitosDeAudio.nomeDoEstilo("hip-hop"));
        assertEquals("Hip hop", EfeitosDeAudio.nomeDoEstilo("Hip_Hop"));
        assertEquals("Heavy metal", EfeitosDeAudio.nomeDoEstilo("Heavy  Metal"));
    }

    @Test
    public void nomeDesconhecidoFicaComoOAparelhoDa() {
        assertEquals("Bass Boost", EfeitosDeAudio.nomeDoEstilo("Bass Boost"));
        assertEquals("Cinema", EfeitosDeAudio.nomeDoEstilo(" Cinema "));
        assertEquals("Rock clássico", EfeitosDeAudio.nomeDoEstilo("Rock clássico"));
        assertEquals("", EfeitosDeAudio.nomeDoEstilo(""));
        assertEquals("", EfeitosDeAudio.nomeDoEstilo(null));
    }

    private static List<EfeitosDeAudio.Estilo> estilosDeFabrica() {
        String[] nomes = {"Normal", "Classical", "Dance", "Flat", "Folk", "Heavy Metal", "Hip Hop", "Jazz", "Pop", "Rock"};
        List<EfeitosDeAudio.Estilo> estilos = new ArrayList<>();
        for (short i = 0; i < nomes.length; i++) estilos.add(new EfeitosDeAudio.Estilo(i, nomes[i]));
        return estilos;
    }

    private static List<String> nomes(List<EfeitosDeAudio.Estilo> estilos) {
        List<String> nomes = new ArrayList<>();
        for (EfeitosDeAudio.Estilo e : estilos) nomes.add(e.nome);
        return nomes;
    }

    @Test
    public void estilosEmOrdemAlfabetica() {
        List<EfeitosDeAudio.Estilo> ordem = EfeitosDeAudio.emOrdem(estilosDeFabrica());
        assertEquals(Arrays.asList("Clássica", "Dance", "Folk", "Heavy metal", "Hip hop", "Jazz", "Normal", "Plano", "Pop", "Rock"),
                nomes(ordem));
    }

    @Test
    public void estilosSemRepetidoNemVazio() {
        List<EfeitosDeAudio.Estilo> estilos = new ArrayList<>();
        estilos.add(new EfeitosDeAudio.Estilo((short) 0, "Rock"));
        estilos.add(new EfeitosDeAudio.Estilo((short) 1, "  "));
        estilos.add(new EfeitosDeAudio.Estilo((short) 2, "Rock"));
        estilos.add(null);
        estilos.add(new EfeitosDeAudio.Estilo((short) 3, "Bass Boost"));
        List<EfeitosDeAudio.Estilo> ordem = EfeitosDeAudio.emOrdem(estilos);
        assertEquals(Arrays.asList("Bass Boost", "Rock"), nomes(ordem));
        // O primeiro repetido fica
        assertEquals(0, ordem.get(1).indice);
        assertTrue(EfeitosDeAudio.emOrdem(Collections.emptyList()).isEmpty());
    }

    @Test
    public void estiloEscolhidoGuardadoNormalOuPrimeiro() {
        List<EfeitosDeAudio.Estilo> estilos = EfeitosDeAudio.emOrdem(estilosDeFabrica());
        assertEquals("Rock", EfeitosDeAudio.estiloEscolhido(estilos, "Rock").original);
        // Guarda o nome do aparelho, não o da tela
        assertEquals("Flat", EfeitosDeAudio.estiloEscolhido(estilos, "Flat").original);
        // Sem escolha, ou escolha que este aparelho não tem: Normal
        assertEquals("Normal", EfeitosDeAudio.estiloEscolhido(estilos, null).original);
        assertEquals("Normal", EfeitosDeAudio.estiloEscolhido(estilos, "Plano").original);
        assertEquals("Normal", EfeitosDeAudio.estiloEscolhido(estilos, "Cinema").original);

        // Sem Normal: o primeiro do aparelho (menor índice), não o primeiro da ordem alfabética
        List<EfeitosDeAudio.Estilo> semNormal = new ArrayList<>();
        semNormal.add(new EfeitosDeAudio.Estilo((short) 4, "Bass Boost"));
        semNormal.add(new EfeitosDeAudio.Estilo((short) 2, "Rock"));
        semNormal.add(new EfeitosDeAudio.Estilo((short) 7, "Cinema"));
        EfeitosDeAudio.Estilo escolhido = EfeitosDeAudio.estiloEscolhido(EfeitosDeAudio.emOrdem(semNormal), null);
        assertSame(semNormal.get(1), escolhido);

        assertNull(EfeitosDeAudio.estiloEscolhido(Collections.emptyList(), "Rock"));
    }

    // ---- Áudio direto para o receptor ----

    @Test
    public void audioDiretoQuandoNaoEhPcm() {
        // AudioFormat.ENCODING_*: PCM (DEFAULT 1, 16 bits 2, 8 bits 3, float 4, 24 bits 21, 32 bits 22) não é direto
        for (int pcm : new int[]{1, 2, 3, 4, 21, 22}) assertFalse(String.valueOf(pcm), EfeitosDeAudio.ehAudioDireto(pcm));
        // AC3 5, E-AC3 6, DTS 7, DTS HD 8, IEC 61937 13, Dolby TrueHD 14
        for (int direto : new int[]{5, 6, 7, 8, 13, 14}) assertTrue(String.valueOf(direto), EfeitosDeAudio.ehAudioDireto(direto));
        // Sem AudioTrack (0) ou inválido: não se sabe, não é
        assertFalse(EfeitosDeAudio.ehAudioDireto(0));
        assertFalse(EfeitosDeAudio.ehAudioDireto(-1));
    }
}
