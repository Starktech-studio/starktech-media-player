// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Quem precisa de leitura, quando a leitura fecha e o que ela não pode apagar. */
public class VarreduraLeituraTest {

    private static final Capitulos DOIS = Capitulos.deTexto("0\t60000\tUm\n60000\t120000\tDois");

    private static Acervo.Video video(int lido, long duracaoMs, Capitulos capitulos) {
        Acervo.Video v = new Acervo.Video();
        v.lido = lido;
        v.duracaoMs = duracaoMs;
        v.capitulos = capitulos;
        return v;
    }

    @Test
    public void nuncaLidoSempreFaltaLer() {
        assertTrue(Varredura.faltaLer(video(0, 1000, DOIS)));
    }

    @Test
    public void leituraAnteriorCompletaNaoRele() {
        assertFalse(Varredura.faltaLer(video(3, 1000, DOIS)));
    }

    @Test
    public void leituraAnteriorSemCapitulosOuSemDuracaoRele() {
        assertTrue(Varredura.faltaLer(video(3, 1000, Capitulos.nenhum())));
        assertTrue(Varredura.faltaLer(video(3, 0, DOIS)));
        assertTrue(Varredura.faltaLer(video(1, 0, Capitulos.nenhum())));
    }

    @Test
    public void daLeitura4_soReleQuemPodeTerHdr() {
        Acervo.Video h265 = video(4, 1000, DOIS);
        h265.infoVideo = "H.265";
        Acervo.Video h264SemCapitulos = video(4, 1000, Capitulos.nenhum());
        h264SemCapitulos.infoVideo = "H.264";
        assertTrue(Varredura.faltaLer(h265));
        assertFalse(Varredura.faltaLer(h264SemCapitulos));
    }

    @Test
    public void leituraAtualFechadaNaoRele() {
        assertFalse(Varredura.faltaLer(video(Varredura.LEITURA, 0, Capitulos.nenhum())));
    }

    @Test
    public void respondeuPrecisaDeAlgumDado() {
        assertFalse(Varredura.respondeu(new Acervo.Video()));
        assertTrue(Varredura.respondeu(video(0, 1000, Capitulos.nenhum())));
        assertTrue(Varredura.respondeu(video(0, 0, DOIS)));
        Acervo.Video soAudio = new Acervo.Video();
        soAudio.audios = Collections.singletonList("Português");
        assertTrue(Varredura.respondeu(soAudio));
    }

    @Test
    public void leituraFalhaNaoApagaNemFecha() {
        Acervo.Video v = video(3, 1000, DOIS);
        v.largura = 1920;
        v.audios = Collections.singletonList("Português");
        v.duracaoMs = 0;
        Varredura.aplicar(new Acervo.Video(), v);
        assertEquals(1920, v.largura);
        assertEquals(1, v.audios.size());
        assertEquals(2, v.capitulos.quantidade());
        assertEquals(3, v.lido);
    }

    @Test
    public void reservaFechaQuemSoPrecisavaDosCapitulos() {
        Acervo.Video v = video(3, 1000, Capitulos.nenhum());
        Acervo.Video reserva = new Acervo.Video();
        reserva.capitulos = DOIS;
        Varredura.aplicar(reserva, v);
        assertEquals(2, v.capitulos.quantidade());
        assertEquals(Varredura.LEITURA, v.lido);
    }

    @Test
    public void reservaNaoFechaVideoNuncaLido() {
        Acervo.Video v = video(0, 0, Capitulos.nenhum());
        Acervo.Video reserva = new Acervo.Video();
        reserva.capitulos = DOIS;
        Varredura.aplicar(reserva, v);
        assertEquals(2, v.capitulos.quantidade());
        assertEquals(0, v.lido);
    }

    @Test
    public void tentativaSoMarcaQuemNuncaFoiTentado() {
        Acervo.Video novo = video(0, 0, Capitulos.nenhum());
        assertFalse(Varredura.jaTentou(novo));
        assertTrue(Varredura.marcarTentativa(novo));
        assertEquals(Varredura.TENTOU, novo.lido);
        assertTrue(Varredura.jaTentou(novo));
        assertFalse(Varredura.marcarTentativa(novo));

        Acervo.Video deOutraLeitura = video(3, 0, DOIS);
        assertTrue(Varredura.jaTentou(deOutraLeitura));
        assertFalse(Varredura.marcarTentativa(deOutraLeitura));
        assertEquals(3, deOutraLeitura.lido);
    }

    @Test
    public void quemFalhouSegueFaltandoLerAteOMotorResponder() {
        Acervo.Video v = video(Varredura.TENTOU, 0, Capitulos.nenhum());
        assertTrue(Varredura.faltaLer(v));
        Varredura.aplicar(new Acervo.Video(), v);
        assertEquals(Varredura.TENTOU, v.lido);
        assertTrue(Varredura.faltaLer(v));
        Varredura.aplicar(video(Varredura.LEITURA, 5000, Capitulos.nenhum()), v);
        assertEquals(Varredura.LEITURA, v.lido);
        assertFalse(Varredura.faltaLer(v));
    }

    @Test
    public void leituraPresaDeixaDeContarDepoisDoPrazo() {
        Map<String, Long> abertas = new HashMap<>();
        abertas.put("presa", 1000L);
        abertas.put("recente", 1000L + Varredura.PRAZO_PRESA_MS);
        assertEquals(2, Varredura.leiturasAbertas(abertas, 999L + Varredura.PRAZO_PRESA_MS));
        assertEquals(1, Varredura.leiturasAbertas(abertas, 1000L + Varredura.PRAZO_PRESA_MS));
        assertFalse(abertas.containsKey("presa"));
        assertTrue(abertas.containsKey("recente"));
    }

    @Test
    public void motorQueRespondeuFechaSemTrocarOsCapitulosPorNada() {
        Acervo.Video v = video(0, 0, DOIS);
        Acervo.Video lida = video(Varredura.LEITURA, 5000, Capitulos.nenhum());
        Varredura.aplicar(lida, v);
        assertEquals(5000, v.duracaoMs);
        assertEquals(2, v.capitulos.quantidade());
        assertEquals(Varredura.LEITURA, v.lido);
    }
}
