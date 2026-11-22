// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Camada dos arquivos (2.0, etapa 4): a pasta do aparelho pelo {@code File} e a do servidor em casa por uma rede
 * falsa (no app, a do Nova). Servidor fora de alcance não chama a rede.
 */
public class ArquivosDaBibliotecaTest {

    @Rule
    public TemporaryFolder temporaria = new TemporaryFolder();

    /** Rede falsa: a listagem de cada endereço smb e quantas vezes foi chamada. */
    static final class RedeFalsa implements ArquivosDaBiblioteca.Rede {
        final Map<String, List<ArquivosDaBiblioteca.Entrada>> pastas = new HashMap<>();
        final Map<String, byte[]> arquivos = new HashMap<>();
        final List<String> pedidos = new ArrayList<>();
        IOException erro;

        void pasta(String endereco, ArquivosDaBiblioteca.Entrada... itens) {
            pastas.put(endereco, Arrays.asList(itens));
        }

        @Override
        public List<ArquivosDaBiblioteca.Entrada> listar(String endereco) throws IOException {
            pedidos.add(endereco);
            if (erro != null) throw erro;
            List<ArquivosDaBiblioteca.Entrada> lista = pastas.get(endereco);
            if (lista == null) throw new IOException("pasta não abriu");
            return lista;
        }

        @Override
        public InputStream abrir(String endereco, long de) throws IOException {
            pedidos.add(endereco);
            byte[] dados = arquivos.get(endereco);
            if (dados == null) throw new IOException("arquivo não abriu");
            ByteArrayInputStream entrada = new ByteArrayInputStream(dados);
            //noinspection ResultOfMethodCallIgnored
            entrada.skip(de);
            return entrada;
        }
    }

    static final String IP = "192.168.0.10";
    static final String COMPARTILHAMENTO = "midia";

    /** Só o servidor 1 existe; o endereço como o {@link Servidores} escreve. */
    static String endereco(String caminho, boolean pasta) {
        if (Servidores.idDo(caminho) != 1) return null;
        return Servidores.endereco(IP, COMPARTILHAMENTO, Servidores.relativo(caminho), pasta);
    }

    static ArquivosDaBiblioteca.Entrada arquivo(String nome, long tamanho) {
        return new ArquivosDaBiblioteca.Entrada(null, nome, false, tamanho, 1000L);
    }

    static ArquivosDaBiblioteca.Entrada pasta(String nome) {
        return new ArquivosDaBiblioteca.Entrada(null, nome, true, 0, 1000L);
    }

    private final RedeFalsa rede = new RedeFalsa();
    private boolean conectado = true;
    private final ArquivosDaBiblioteca camada = new ArquivosDaBiblioteca(ArquivosDaBibliotecaTest::endereco, rede, id -> conectado);

    @Test
    public void listar_doServidor_semEscondidosEmOrdemComOCaminhoDaBiblioteca() {
        rede.pasta("smb://192.168.0.10/midia/Filmes/",
                arquivo("Zeta (2020).mkv", 30), pasta("Coleção/"), arquivo(".escondido.mkv", 1), arquivo("Alfa (2001).mp4", 20));
        List<ArquivosDaBiblioteca.Entrada> itens = camada.listar("servidor:1/Filmes");
        assertEquals(3, itens.size());
        assertEquals("Alfa (2001).mp4", itens.get(0).nome);
        assertEquals("servidor:1/Filmes/Alfa (2001).mp4", itens.get(0).caminho);
        assertEquals(20, itens.get(0).tamanho);
        assertEquals("a barra do fim sai do nome da pasta", "Coleção", itens.get(1).nome);
        assertTrue(itens.get(1).pasta);
        assertEquals("servidor:1/Filmes/Coleção", itens.get(1).caminho);
        assertEquals(0, itens.get(1).tamanho);
        assertEquals("servidor:1/Filmes/Zeta (2020).mkv", itens.get(2).caminho);
    }

    @Test
    public void foraDeAlcance_naoChamaARede() {
        conectado = false;
        rede.pasta("smb://192.168.0.10/midia/Filmes/", arquivo("a.mkv", 1));
        assertNull(camada.listar("servidor:1/Filmes"));
        assertFalse(camada.disponivel("servidor:1/Filmes/a.mkv"));
        assertFalse(camada.servidorAoAlcance("servidor:1/Filmes/a.mkv"));
        try {
            camada.abrir("servidor:1/Filmes/a.mkv", 0);
            fail("fora de alcance não abre");
        } catch (IOException esperado) {
            // ok
        }
        assertTrue("nenhum pedido à rede", rede.pedidos.isEmpty());
    }

    @Test
    public void servidorRemovido_semEndereco() {
        assertNull(camada.listar("servidor:9/Filmes"));
        assertNull(camada.endereco("servidor:9/Filmes/a.mkv"));
        assertFalse(camada.disponivel("servidor:9/Filmes/a.mkv"));
    }

    @Test
    public void erroDeSenha_chegaComoSenhaRecusada() {
        rede.erro = new ArquivosDaBiblioteca.SenhaRecusada();
        try {
            camada.listarOuFalhar("servidor:1/Filmes");
            fail("senha recusada");
        } catch (ArquivosDaBiblioteca.SenhaRecusada esperado) {
            // ok
        } catch (IOException outro) {
            fail("devia ser a de senha");
        }
        assertNull("listar só devolve nula", camada.listar("servidor:1/Filmes"));
        assertTrue(ArquivosDaBiblioteca.deSenha(new IOException("falhou", new ArquivosDaBiblioteca.SenhaRecusada())));
        assertFalse(ArquivosDaBiblioteca.deSenha(new IOException("tempo esgotado")));
    }

    @Test
    public void abrir_doServidorAPartirDeUmPonto() throws IOException {
        rede.arquivos.put("smb://192.168.0.10/midia/Musicas/a.mp3", new byte[]{1, 2, 3, 4, 5});
        try (InputStream entrada = camada.abrir("servidor:1/Musicas/a.mp3", 2)) {
            assertEquals(3, entrada.read());
        }
    }

    @Test
    public void endereco_doAparelhoEDoServidor() {
        assertEquals("/storage/emulated/0/Music/a.mp3", camada.endereco("/storage/emulated/0/Music/a.mp3"));
        assertEquals("smb://192.168.0.10/midia/Musicas/a.mp3", camada.endereco("servidor:1/Musicas/a.mp3"));
        assertEquals("smb://192.168.0.10/midia/Musicas/", camada.enderecoDaPasta("servidor:1/Musicas"));
        assertTrue("o do aparelho não depende do servidor", camada.servidorAoAlcance("/storage/emulated/0/Music/a.mp3"));
    }

    @Test
    public void listar_doAparelho_semEscondidos() throws IOException {
        File pasta = temporaria.newFolder("Filmes");
        try (FileOutputStream saida = new FileOutputStream(new File(pasta, "b.mkv"))) {
            saida.write(new byte[7]);
        }
        //noinspection ResultOfMethodCallIgnored
        new File(pasta, ".oculto").createNewFile();
        //noinspection ResultOfMethodCallIgnored
        new File(pasta, "Sub").mkdir();
        List<ArquivosDaBiblioteca.Entrada> itens = camada.listar(pasta.getAbsolutePath());
        assertEquals(2, itens.size());
        assertEquals("Sub", itens.get(0).nome);
        assertTrue(itens.get(0).pasta);
        assertEquals("b.mkv", itens.get(1).nome);
        assertEquals(7, itens.get(1).tamanho);
        assertNull("pasta que não existe", camada.listar(new File(pasta, "nao-existe").getAbsolutePath()));
        assertTrue("o aparelho não usa a rede", rede.pedidos.isEmpty());
    }

    @Test
    public void nomeEAcima_valemParaOsDois() {
        assertEquals("a.mp3", ArquivosDaBiblioteca.nome("servidor:1/Musicas/a.mp3"));
        assertEquals("servidor:1/Musicas", ArquivosDaBiblioteca.acima("servidor:1/Musicas/a.mp3"));
        assertEquals("/hd", ArquivosDaBiblioteca.acima("/hd/a.mp3"));
    }
}
