// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.archos.filecorelibrary.samba.NetworkCredentialsDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Servidor em casa (2.0): um registro por IP e compartilhamento, com o usuário e a senha cifrada pelo
 * {@link CofreDeSenhas}, na tabela {@code servidor} do {@link BancoStarkTech}.
 *
 * <p>Caminho na biblioteca (D7 = B): {@code servidor:<id>/<caminho dentro do compartilhamento>}, traduzido
 * para {@code smb://IP/compartilhamento/…} só ao abrir ({@link #endereco}). Trocar o IP, o usuário ou o
 * compartilhamento não perde estado nem edição.
 *
 * <p>Credencial: entregue ao Nova só na memória ({@code addCredential}), ao abrir o app e ao cadastrar,
 * pela chave {@code smb://IP}. Nunca pelo {@code saveCredential} do Nova (chave fixa no código público), nunca
 * com usuário e senha dentro do endereço (o Nova põe o endereço no log).
 */
public final class Servidores {

    /** Começo do caminho de um arquivo do servidor. */
    public static final String PREFIXO = "servidor:";

    /** Um servidor cadastrado. */
    public static final class Servidor {
        public final long id;
        public final String ip;
        public final String compartilhamento;
        /** Vazio = sem usuário (convidado). */
        public final String usuario;
        final byte[] senhaCifrada;

        Servidor(long id, String ip, String compartilhamento, String usuario, byte[] senhaCifrada) {
            this.id = id;
            this.ip = ip;
            this.compartilhamento = compartilhamento;
            this.usuario = usuario == null ? "" : usuario;
            this.senhaCifrada = senhaCifrada;
        }

        /** Caminho da raiz do compartilhamento na biblioteca: {@code servidor:<id>}. */
        public String raiz() {
            return PREFIXO + id;
        }

        public boolean temSenha() {
            return senhaCifrada != null;
        }
    }

    /** Onde a credencial vai (o banco de credenciais do Nova, só na memória); nos testes, uma lista. */
    interface Credenciais {
        void entregar(String chave, String usuario, String senha);
    }

    private static Servidores instancia;

    private final BancoStarkTech banco;
    private final CofreDeSenhas cofre;
    private final Credenciais credenciais;
    private Map<Long, Servidor> cache;
    /** Servidores cuja senha guardada não abriu nesta execução (backup noutro aparelho): pedem a senha de novo. */
    private final java.util.Set<Long> semSenha = new java.util.HashSet<>();

    public static synchronized Servidores de(Context contexto) {
        if (instancia == null) {
            instancia = new Servidores(BancoStarkTech.de(contexto), CofreDeSenhas.doAparelho(),
                    (chave, usuario, senha) -> NetworkCredentialsDatabase.getInstance().addCredential(
                            new NetworkCredentialsDatabase.Credential(usuario, senha, chave, "", true)));
        }
        return instancia;
    }

    Servidores(BancoStarkTech banco, CofreDeSenhas cofre, Credenciais credenciais) {
        this.banco = banco;
        this.cofre = cofre;
        this.credenciais = credenciais;
    }

    // --- caminhos ---

    /** true = caminho de um arquivo ou pasta do servidor ({@code servidor:<id>…}). */
    public static boolean doServidor(String caminho) {
        return caminho != null && caminho.startsWith(PREFIXO);
    }

    /** Id do servidor no caminho; -1 se o caminho não é do servidor. */
    public static long idDo(String caminho) {
        if (!doServidor(caminho)) return -1;
        int fim = caminho.indexOf('/', PREFIXO.length());
        String numero = fim < 0 ? caminho.substring(PREFIXO.length()) : caminho.substring(PREFIXO.length(), fim);
        try {
            return Long.parseLong(numero);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** O caminho dentro do compartilhamento, sem barra no começo; vazio na raiz. */
    public static String relativo(String caminho) {
        if (!doServidor(caminho)) return "";
        int fim = caminho.indexOf('/', PREFIXO.length());
        return fim < 0 ? "" : caminho.substring(fim + 1);
    }

    /** Caminho na biblioteca de um item do servidor. */
    public static String caminho(long id, String relativo) {
        String limpo = relativo == null ? "" : relativo;
        while (limpo.startsWith("/")) limpo = limpo.substring(1);
        while (limpo.endsWith("/")) limpo = limpo.substring(0, limpo.length() - 1);
        return limpo.isEmpty() ? PREFIXO + id : PREFIXO + id + "/" + limpo;
    }

    /**
     * Endereço {@code smb://IP/compartilhamento/…} de um caminho, como o Nova escreve: o espaço fica, só
     * {@code #} e {@code ?} são escapados; pasta termina em barra. Sem usuário nem senha dentro.
     */
    static String endereco(String ip, String compartilhamento, String relativo, boolean pasta) {
        StringBuilder sb = new StringBuilder("smb://").append(ip).append('/');
        if (compartilhamento != null && !compartilhamento.isEmpty()) {
            sb.append(escapar(compartilhamento)).append('/');
            if (relativo != null && !relativo.isEmpty()) {
                sb.append(escapar(relativo));
                if (pasta) sb.append('/');
            }
        }
        return sb.toString();
    }

    /** Endereço smb de um caminho do servidor cadastrado; nulo se o servidor não existe mais. */
    public synchronized String endereco(String caminho, boolean pasta) {
        Servidor s = servidor(idDo(caminho));
        return s == null ? null : endereco(s.ip, s.compartilhamento, relativo(caminho), pasta);
    }

    /** Endereço da lista de compartilhamentos de um IP. */
    public static String enderecoDoIp(String ip) {
        return "smb://" + ip + "/";
    }

    /** Endereço de uma pasta de um compartilhamento ainda não cadastrado (navegador do Adicionar servidor). */
    public static String enderecoDaPasta(String ip, String compartilhamento, String relativo) {
        return endereco(ip, compartilhamento, relativo, true);
    }

    private static String escapar(String trecho) {
        return trecho.replace("#", "%23").replace("?", "%3F"); // como o FileUtils.escapeUriPathDelimiters do Nova
    }

    /** IPv4 (D6: o IP é fixo): quatro números de 0 a 255, só com os algarismos 0 a 9. */
    public static boolean ipValido(String ip) {
        if (ip == null) return false;
        String[] partes = ip.split("\\.", -1);
        if (partes.length != 4) return false;
        for (String parte : partes) {
            if (parte.isEmpty() || parte.length() > 3) return false;
            for (int i = 0; i < parte.length(); i++) {
                char c = parte.charAt(i);
                if (c < '0' || c > '9') return false;
            }
            if (Integer.parseInt(parte) > 255) return false;
        }
        return true;
    }

    /** Chave da credencial no Nova: vale para todos os compartilhamentos do IP. */
    static String chaveCredencial(String ip) {
        return "smb://" + ip;
    }

    // --- cadastro ---

    /** Servidores cadastrados, na ordem em que entraram. */
    public synchronized List<Servidor> lista() {
        return Collections.unmodifiableList(new ArrayList<>(carregar().values()));
    }

    public synchronized Servidor servidor(long id) {
        return carregar().get(id);
    }

    /** O servidor do caminho; nulo se não é do servidor ou ele foi removido. */
    public synchronized Servidor doCaminho(String caminho) {
        return servidor(idDo(caminho));
    }

    /** Já cadastrado com o mesmo IP e compartilhamento: devolve esse, com o usuário e a senha novos. */
    public synchronized Servidor adicionar(String ip, String compartilhamento, String usuario, String senha) {
        for (Servidor s : carregar().values()) {
            if (s.ip.equals(ip) && s.compartilhamento.equals(compartilhamento)) {
                return editar(s.id, ip, compartilhamento, usuario, senha);
            }
        }
        ContentValues linha = new ContentValues();
        linha.put("ip", ip);
        linha.put("compartilhamento", compartilhamento);
        linha.put("usuario", usuario == null ? "" : usuario);
        byte[] cifrada = cofre.cifrar(senha);
        if (cifrada == null) linha.putNull("senha");
        else linha.put("senha", cifrada);
        linha.put("criado", System.currentTimeMillis());
        long id = banco.getWritableDatabase().insert(BancoStarkTech.SERVIDOR, null, linha);
        if (id < 0) return null;
        Servidor novo = new Servidor(id, ip, compartilhamento, usuario, cifrada);
        carregar().put(id, novo);
        semSenha.remove(id);
        credenciais.entregar(chaveCredencial(ip), novo.usuario, senha == null ? "" : senha);
        return novo;
    }

    /** {@code senha} nula = mantém a guardada. */
    public synchronized Servidor editar(long id, String ip, String compartilhamento, String usuario, String senha) {
        Servidor atual = servidor(id);
        if (atual == null) return null;
        byte[] cifrada = senha == null ? atual.senhaCifrada : cofre.cifrar(senha);
        ContentValues linha = new ContentValues();
        linha.put("ip", ip);
        linha.put("compartilhamento", compartilhamento);
        linha.put("usuario", usuario == null ? "" : usuario);
        if (cifrada == null) linha.putNull("senha");
        else linha.put("senha", cifrada);
        banco.getWritableDatabase().update(BancoStarkTech.SERVIDOR, linha, "id = ?", new String[]{String.valueOf(id)});
        Servidor novo = new Servidor(id, ip, compartilhamento, usuario, cifrada);
        carregar().put(id, novo);
        if (!atual.ip.equals(ip)) credenciais.entregar(chaveCredencial(atual.ip), "", "");
        if (senha != null) semSenha.remove(id);
        entregar(novo, senha);
        return novo;
    }

    /** Apaga o servidor e a senha; as pastas dele saem pela {@link Pastas} (quem chama). */
    public synchronized void remover(long id) {
        Servidor atual = carregar().remove(id);
        if (atual == null) return;
        banco.getWritableDatabase().delete(BancoStarkTech.SERVIDOR, "id = ?", new String[]{String.valueOf(id)});
        semSenha.remove(id);
        credenciais.entregar(chaveCredencial(atual.ip), "", "");
        // outro compartilhamento no mesmo IP continua valendo
        for (Servidor s : carregar().values()) if (s.ip.equals(atual.ip)) entregar(s, null);
    }

    /** Ao abrir o app: a senha de cada servidor vai para a memória do Nova. */
    public synchronized void entregarTodas() {
        for (Servidor s : carregar().values()) entregar(s, null);
    }

    /**
     * Credencial do teste do Entrar e do navegador do Adicionar servidor, antes do cadastro: só na memória do
     * Nova, pela chave do IP.
     */
    public synchronized void credencialDoTeste(String ip, String usuario, String senha) {
        credenciais.entregar(chaveCredencial(ip), usuario == null ? "" : usuario, senha == null ? "" : senha);
    }

    /**
     * Editar servidor sem digitar a senha: a guardada vai para a memória do Nova pela chave do IP digitado, com o
     * usuário digitado, para o teste do Salvar. false = a guardada não abre (backup noutro aparelho): pedir a senha.
     */
    public synchronized boolean credencialGuardadaDoTeste(long id, String ip, String usuario) {
        Servidor s = servidor(id);
        if (s == null) return false;
        String aberta = "";
        if (s.senhaCifrada != null) {
            aberta = cofre.decifrar(s.senhaCifrada);
            if (aberta == null) {
                semSenha.add(id);
                return false;
            }
        }
        credenciais.entregar(chaveCredencial(ip), usuario == null ? "" : usuario, aberta);
        return true;
    }

    /** true = a senha guardada não abriu (noutro aparelho, chave apagada): o servidor pede a senha de novo. */
    public synchronized boolean precisaDaSenha(long id) {
        return semSenha.contains(id);
    }

    /** Entrega a credencial; {@code senha} nula = a guardada, decifrada só aqui. */
    private void entregar(Servidor s, String senha) {
        String aberta = senha;
        if (aberta == null && s.senhaCifrada != null) {
            aberta = cofre.decifrar(s.senhaCifrada);
            if (aberta == null) semSenha.add(s.id);
            else semSenha.remove(s.id);
        }
        credenciais.entregar(chaveCredencial(s.ip), s.usuario, aberta == null ? "" : aberta);
    }

    private Map<Long, Servidor> carregar() {
        if (cache != null) return cache;
        cache = new LinkedHashMap<>();
        try (Cursor c = banco.getReadableDatabase().query(BancoStarkTech.SERVIDOR,
                new String[]{"id", "ip", "compartilhamento", "usuario", "senha"}, null, null, null, null, "id")) {
            while (c.moveToNext()) {
                cache.put(c.getLong(0), new Servidor(c.getLong(0), c.getString(1), c.getString(2), c.getString(3),
                        c.isNull(4) ? null : c.getBlob(4)));
            }
        }
        return cache;
    }
}
