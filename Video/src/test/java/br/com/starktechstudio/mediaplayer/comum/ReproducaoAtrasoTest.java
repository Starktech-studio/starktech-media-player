// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Legenda rápida (1.5): o atraso fica dentro de ±10 min e anda de 0,1 em 0,1 s. */
public class ReproducaoAtrasoTest {

    @Test
    public void atraso_limitado() {
        assertEquals(0, Reproducao.limitarAtraso(0));
        assertEquals(-300, Reproducao.limitarAtraso(-300));
        assertEquals(Reproducao.ATRASO_MAXIMO_MS, Reproducao.limitarAtraso(Integer.MAX_VALUE / 2));
        assertEquals(-Reproducao.ATRASO_MAXIMO_MS, Reproducao.limitarAtraso(-Reproducao.ATRASO_MAXIMO_MS - 1));
        assertEquals(100, Reproducao.ATRASO_PASSO_MS);
    }
}
