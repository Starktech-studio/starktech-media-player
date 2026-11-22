// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servidor ligado ou não (2.0, etapa 4) com a porta falsa: testa cada IP uma vez por rodada, guarda o estado (as
 * telas leem sem rede) e avisa só quando muda.
 */
public class EstadoServidorTest {

    private final Map<String, Boolean> abre = new HashMap<>();
    private final List<String> testados = new ArrayList<>();
    private final List<String> avisos = new ArrayList<>();
    private final List<Servidores.Servidor> cadastro = new ArrayList<>();

    private EstadoServidor novo() {
        EstadoServidor estado = new EstadoServidor(() -> cadastro, (ip, porta, limite) -> {
            testados.add(ip + ":" + porta + ":" + limite);
            Boolean sim = abre.get(ip);
            return sim != null && sim;
        }, Runnable::run, null);
        estado.ouvir((id, conectado) -> avisos.add(id + "=" + conectado));
        return estado;
    }

    @Test
    public void nuncaTestado_contaComoFora() {
        cadastro.add(new Servidores.Servidor(1, "192.168.0.10", "midia", "", null));
        EstadoServidor estado = novo();
        assertFalse(estado.conectado(1));
        assertFalse(estado.algumConectado());
        assertTrue("ler o estado não usa a rede", testados.isEmpty());
    }

    @Test
    public void conferir_portaSmbEmAteDoisSegundos_umaVezPorIp() {
        cadastro.addAll(Arrays.asList(new Servidores.Servidor(1, "192.168.0.10", "midia", "", null),
                new Servidores.Servidor(2, "192.168.0.10", "musicas", "", null),
                new Servidores.Servidor(3, "192.168.0.20", "fotos", "", null)));
        abre.put("192.168.0.10", true);
        EstadoServidor estado = novo();
        assertEquals("um fora", 1, estado.conferirAgora());
        assertEquals(Arrays.asList("192.168.0.10:445:2000", "192.168.0.20:445:2000"), testados);
        assertTrue(estado.conectado(1));
        assertTrue(estado.conectado(2));
        assertFalse(estado.conectado(3));
        assertTrue(estado.algumConectado());
        assertEquals("avisa os que entraram ligados", Arrays.asList("1=true", "2=true"), avisos);
    }

    @Test
    public void avisaSoQuandoMuda() {
        cadastro.add(new Servidores.Servidor(1, "192.168.0.10", "midia", "", null));
        abre.put("192.168.0.10", true);
        EstadoServidor estado = novo();
        estado.conferirAgora();
        estado.conferirAgora();
        assertEquals(Arrays.asList("1=true"), avisos);

        abre.put("192.168.0.10", false); // a rede caiu
        assertEquals(1, estado.conferirAgora());
        assertFalse(estado.conectado(1));
        abre.put("192.168.0.10", true); // voltou
        estado.conferirAgora();
        assertEquals(Arrays.asList("1=true", "1=false", "1=true"), avisos);
    }

    @Test
    public void esquecer_saiDoEstado() {
        cadastro.add(new Servidores.Servidor(1, "192.168.0.10", "midia", "", null));
        abre.put("192.168.0.10", true);
        EstadoServidor estado = novo();
        estado.conferirAgora();
        estado.esquecer(1);
        cadastro.clear();
        assertFalse(estado.conectado(1));
        assertFalse(estado.algumConectado());
    }
}
