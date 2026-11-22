// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import android.content.Context;
import android.net.Uri;

import com.archos.filecorelibrary.FileEditorFactory;
import com.archos.filecorelibrary.MetaFile2;
import com.archos.filecorelibrary.RawListerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Camada dos arquivos da biblioteca (2.0): a mesma lista, leitura e endereço para a pasta do aparelho (HD
 * externo, memória interna) e a do servidor em casa ({@code servidor:<id>/…}, {@link Servidores}).
 *
 * <p>No servidor, nome, tamanho e data vêm da listagem da pasta (sem um pedido por arquivo). Servidor fora de
 * alcance ({@link EstadoServidor}) não lista: devolve nulo na hora, sem esperar o tempo de conexão. A tela
 * nunca chama a rede; quem lista é a varredura, fora da tela.
 */
public final class ArquivosDaBiblioteca {

    /** Um item de uma pasta. */
    public static final class Entrada {
        /** Caminho na biblioteca (o do aparelho, ou {@code servidor:<id>/…}). */
        public final String caminho;
        public final String nome;
        public final boolean pasta;
        public final long tamanho;
        public final long modificado;

        public Entrada(String caminho, String nome, boolean pasta, long tamanho, long modificado) {
            this.caminho = caminho;
            this.nome = nome;
            this.pasta = pasta;
            this.tamanho = tamanho;
            this.modificado = modificado;
        }
    }

    /** Usuário ou senha recusados pelo servidor. */
    public static final class SenhaRecusada extends IOException {
        public SenhaRecusada() {
            super("usuário ou senha recusados");
        }
    }

    /** Acesso à rede (o Nova); falso nos testes. */
    interface Rede {
        /** Itens da pasta pelo endereço smb terminado em barra, com o caminho nulo. */
        List<Entrada> listar(String endereco) throws IOException;

        InputStream abrir(String endereco, long de) throws IOException;
    }

    /** Tradução do caminho {@code servidor:<id>/…} para o endereço smb ({@link Servidores#endereco(String, boolean)}). */
    interface Enderecos {
        /** Nulo = o servidor não existe mais. */
        String endereco(String caminho, boolean pasta);
    }

    /** Servidor ao alcance agora, pelo estado guardado ({@link EstadoServidor}), sem rede. */
    interface Alcance {
        boolean conectado(long idServidor);
    }

    private static ArquivosDaBiblioteca instancia;

    private final Enderecos enderecos;
    private final Rede rede;
    private final Alcance alcance;

    public static synchronized ArquivosDaBiblioteca de(Context contexto) {
        if (instancia == null) {
            Context app = contexto.getApplicationContext();
            Servidores servidores = Servidores.de(app);
            EstadoServidor estado = EstadoServidor.de(app);
            instancia = new ArquivosDaBiblioteca(servidores::endereco, new RedeDoNova(app), estado::conectado);
        }
        return instancia;
    }

    /** Só o aparelho, sem servidor (testes e quem ainda não tem contexto). */
    static ArquivosDaBiblioteca soLocal() {
        return new ArquivosDaBiblioteca((caminho, pasta) -> null, null, id -> false);
    }

    ArquivosDaBiblioteca(Enderecos enderecos, Rede rede, Alcance alcance) {
        this.enderecos = enderecos;
        this.rede = rede;
        this.alcance = alcance;
    }

    /** true = caminho do aparelho (não do servidor). */
    public static boolean local(String caminho) {
        return !Servidores.doServidor(caminho);
    }

    /** Nome do arquivo ou da pasta no fim do caminho (vale para os dois tipos de caminho). */
    public static String nome(String caminho) {
        int barra = caminho.lastIndexOf('/');
        return barra < 0 ? caminho : caminho.substring(barra + 1);
    }

    /** Pasta acima do caminho; nula na raiz. */
    public static String acima(String caminho) {
        int barra = caminho.lastIndexOf('/');
        if (barra < 0) return null;
        return barra == 0 ? "/" : caminho.substring(0, barra);
    }

    /** true = o servidor do caminho está ao alcance agora (estado guardado); do aparelho, sempre true. */
    public boolean servidorAoAlcance(String caminho) {
        return local(caminho) || alcance.conectado(Servidores.idDo(caminho));
    }

    /**
     * Itens da pasta, sem os escondidos (nome começando com ponto), em ordem de nome. Nula = não deu para
     * listar: pasta que não existe, sem permissão, servidor fora de alcance ou com erro.
     */
    public List<Entrada> listar(String pasta) {
        try {
            return listarOuFalhar(pasta);
        } catch (IOException e) {
            return null;
        }
    }

    /** Como {@link #listar}, com o motivo da falha ({@link SenhaRecusada} = usuário ou senha). */
    public List<Entrada> listarOuFalhar(String pasta) throws IOException {
        List<Entrada> lista = new ArrayList<>();
        if (local(pasta)) {
            File[] itens = new File(pasta).listFiles();
            if (itens == null) throw new IOException("pasta não abriu");
            for (File f : itens) {
                if (f.getName().startsWith(".")) continue;
                boolean ehPasta = f.isDirectory();
                lista.add(new Entrada(f.getAbsolutePath(), f.getName(), ehPasta, ehPasta ? 0 : f.length(), f.lastModified()));
            }
        } else {
            if (rede == null || !alcance.conectado(Servidores.idDo(pasta))) throw new IOException("servidor fora de alcance");
            String endereco = enderecos.endereco(pasta, true);
            if (endereco == null) throw new IOException("servidor removido");
            for (Entrada e : rede.listar(endereco)) {
                String nome = semBarraNoFim(e.nome);
                if (nome.isEmpty() || nome.startsWith(".")) continue;
                lista.add(new Entrada(pasta + "/" + nome, nome, e.pasta, e.pasta ? 0 : e.tamanho, e.modificado));
            }
        }
        Collections.sort(lista, (a, b) -> a.nome.compareTo(b.nome));
        return lista;
    }

    /** Lista pelo endereço smb (navegador do Adicionar servidor, antes do cadastro), sem conferir o alcance. */
    public List<Entrada> listarEndereco(String endereco) throws IOException {
        if (rede == null) throw new IOException("sem rede");
        List<Entrada> lista = new ArrayList<>();
        for (Entrada e : rede.listar(endereco)) {
            String nome = semBarraNoFim(e.nome);
            if (nome.isEmpty() || nome.startsWith(".")) continue;
            lista.add(new Entrada(null, nome, e.pasta, e.tamanho, e.modificado));
        }
        Collections.sort(lista, (a, b) -> a.nome.compareToIgnoreCase(b.nome));
        return lista;
    }

    /**
     * true = dá para abrir agora, sem esperar a rede: do aparelho, o arquivo existe; do servidor, o servidor
     * está ao alcance (o arquivo em si é conferido pela varredura).
     */
    public boolean disponivel(String caminho) {
        if (caminho == null) return false;
        if (local(caminho)) return new File(caminho).isFile();
        return alcance.conectado(Servidores.idDo(caminho)) && enderecos.endereco(caminho, false) != null;
    }

    /** Endereço para o motor e o player: do aparelho, o próprio caminho; do servidor, {@code smb://…} (nulo se saiu). */
    public String endereco(String caminho) {
        if (caminho == null) return null;
        return local(caminho) ? caminho : enderecos.endereco(caminho, false);
    }

    /** Endereço de uma pasta (o do servidor termina em barra). */
    public String enderecoDaPasta(String caminho) {
        if (caminho == null) return null;
        return local(caminho) ? caminho : enderecos.endereco(caminho, true);
    }

    /** Uri para o player: {@code file://} do aparelho ou {@code smb://} do servidor (pelo proxy do Nova). */
    public Uri uri(String caminho) {
        if (local(caminho)) return Uri.fromFile(new File(caminho));
        String endereco = endereco(caminho);
        return endereco == null ? null : Uri.parse(endereco);
    }

    /** Lê o arquivo a partir de {@code de} bytes. */
    public InputStream abrir(String caminho, long de) throws IOException {
        if (local(caminho)) {
            FileInputStream entrada = new FileInputStream(caminho);
            if (de > 0) {
                long pulados = 0;
                while (pulados < de) {
                    long n = entrada.skip(de - pulados);
                    if (n <= 0) break;
                    pulados += n;
                }
            }
            return entrada;
        }
        if (rede == null || !alcance.conectado(Servidores.idDo(caminho))) throw new IOException("servidor fora de alcance");
        String endereco = enderecos.endereco(caminho, false);
        if (endereco == null) throw new IOException("servidor removido");
        return rede.abrir(endereco, de);
    }

    private static String semBarraNoFim(String nome) {
        String limpo = nome == null ? "" : nome;
        while (limpo.endsWith("/")) limpo = limpo.substring(0, limpo.length() - 1);
        return limpo;
    }

    /** true = a falha foi de usuário ou senha (a do jcifs ou a do Nova), em qualquer ponto da causa. */
    static boolean deSenha(Throwable erro) {
        for (Throwable t = erro; t != null; t = t.getCause()) {
            if (t instanceof SenhaRecusada) return true;
            String nome = t.getClass().getSimpleName();
            if (nome.equals("SmbAuthException") || nome.equals("AuthenticationException")) return true;
            if (t.getCause() == t) break;
        }
        return false;
    }

    /** Rede pelo Nova: listagem pelo {@code RawListerFactory} e leitura pelo {@code FileEditorFactory}. */
    private static final class RedeDoNova implements Rede {
        private final Context contexto;

        RedeDoNova(Context contexto) {
            this.contexto = contexto;
        }

        @Override
        public List<Entrada> listar(String endereco) throws IOException {
            List<MetaFile2> itens;
            try {
                itens = RawListerFactory.getRawListerForUrl(Uri.parse(endereco)).getFileList();
            } catch (Exception e) {
                if (deSenha(e)) throw new SenhaRecusada();
                throw e instanceof IOException ? (IOException) e : new IOException("falha ao listar", e);
            }
            if (itens == null) throw new IOException("pasta não abriu");
            List<Entrada> lista = new ArrayList<>();
            for (MetaFile2 m : itens) {
                // compartilhamentos escondidos (IPC$, print$) ficam fora
                if (m.getName() == null || m.getName().endsWith("$") || m.getName().endsWith("$/")) continue;
                lista.add(new Entrada(null, m.getName(), m.isDirectory(), m.isDirectory() ? 0 : m.length(), m.lastModified()));
            }
            return lista;
        }

        @Override
        public InputStream abrir(String endereco, long de) throws IOException {
            try {
                InputStream entrada = FileEditorFactory.getFileEditorForUrl(Uri.parse(endereco), contexto).getInputStream(de);
                if (entrada == null) throw new IOException("arquivo não abriu");
                return entrada;
            } catch (IOException e) {
                if (deSenha(e)) throw new SenhaRecusada();
                throw e;
            } catch (Exception e) {
                if (deSenha(e)) throw new SenhaRecusada();
                throw new IOException("arquivo não abriu", e);
            }
        }
    }
}
