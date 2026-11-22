// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Servidor em casa (2.0, D7): o caminho {@code servidor:<id>/…} na biblioteca, o endereço smb dele (sem usuário nem
 * senha dentro), a chave da credencial do Nova e o IP do Adicionar servidor (D6).
 */
public class ServidoresTest {

    @Test
    public void caminho_idERelativo() {
        String caminho = Servidores.caminho(3, "Filmes/Nome (2012).mkv");
        assertEquals("servidor:3/Filmes/Nome (2012).mkv", caminho);
        assertTrue(Servidores.doServidor(caminho));
        assertEquals(3, Servidores.idDo(caminho));
        assertEquals("Filmes/Nome (2012).mkv", Servidores.relativo(caminho));
    }

    @Test
    public void caminho_semBarrasSobrandoENaRaiz() {
        assertEquals("servidor:1/Musicas", Servidores.caminho(1, "/Musicas/"));
        assertEquals("servidor:1", Servidores.caminho(1, ""));
        assertEquals("servidor:1", Servidores.caminho(1, null));
        assertEquals(1, Servidores.idDo("servidor:1"));
        assertEquals("", Servidores.relativo("servidor:1"));
    }

    @Test
    public void caminhoDoAparelho_naoEDoServidor() {
        assertFalse(Servidores.doServidor("/storage/1234-ABCD/Filmes/a.mkv"));
        assertFalse(Servidores.doServidor(null));
        assertEquals(-1, Servidores.idDo("/storage/emulated/0/Music/a.mp3"));
        assertEquals("", Servidores.relativo("/storage/emulated/0/Music/a.mp3"));
        assertEquals(-1, Servidores.idDo("servidor:x/Filmes"));
        assertTrue(ArquivosDaBiblioteca.local("/storage/emulated/0/Music/a.mp3"));
        assertFalse(ArquivosDaBiblioteca.local("servidor:2/Musicas/a.mp3"));
    }

    @Test
    public void endereco_comoONovaEscreve() {
        assertEquals("smb://192.168.0.10/midia/Filmes/Nome (2012).mkv",
                Servidores.endereco("192.168.0.10", "midia", "Filmes/Nome (2012).mkv", false));
        assertEquals("pasta termina em barra", "smb://192.168.0.10/midia/Séries/",
                Servidores.endereco("192.168.0.10", "midia", "Séries", true));
        assertEquals("só # e ? escapados", "smb://192.168.0.10/midia/Músicas/Faixa %231 %3F.mp3",
                Servidores.endereco("192.168.0.10", "midia", "Músicas/Faixa #1 ?.mp3", false));
        assertEquals("raiz do compartilhamento", "smb://192.168.0.10/midia/", Servidores.enderecoDaPasta("192.168.0.10", "midia", ""));
        assertEquals("lista de compartilhamentos", "smb://192.168.0.10/", Servidores.enderecoDoIp("192.168.0.10"));
    }

    @Test
    public void endereco_nuncaLevaUsuarioNemSenha() {
        String endereco = Servidores.endereco("10.0.0.5", "midia", "Filmes/a.mkv", false);
        assertFalse(endereco.contains("@"));
        assertEquals("a credencial vale para o IP inteiro", "smb://10.0.0.5", Servidores.chaveCredencial("10.0.0.5"));
    }

    @Test
    public void ipValido_soIpv4() {
        assertTrue(Servidores.ipValido("192.168.0.10"));
        assertTrue(Servidores.ipValido("10.0.0.1"));
        assertTrue(Servidores.ipValido("255.255.255.255"));
        assertFalse(Servidores.ipValido("192.168.0"));
        assertFalse(Servidores.ipValido("192.168.0.256"));
        assertFalse(Servidores.ipValido("192.168..10"));
        assertFalse(Servidores.ipValido("192.168.0.10."));
        assertFalse(Servidores.ipValido("servidor"));
        assertFalse(Servidores.ipValido("1920.168.0.1"));
        assertFalse(Servidores.ipValido(" 192.168.0.10"));
        assertFalse("algarismos de outra escrita ficam de fora", Servidores.ipValido("١٩٢.168.0.1"));
        assertFalse(Servidores.ipValido(""));
        assertFalse(Servidores.ipValido(null));
    }
}
