// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Servidor em casa ligado ou não (2.0): teste da porta 445 (SMB) em até 2 s, fora da tela, guardado. As telas
 * e a varredura leem só o estado guardado ({@link #conectado}), sem rede. Confere ao abrir o app, ao voltar a
 * ele ({@link #conferir}), quando a rede muda e, com algum servidor fora, a cada 1 min; avisa quem ouve quando
 * um servidor sai ou volta.
 */
public final class EstadoServidor {

    /** Avisado na thread da tela quando um servidor muda de estado. */
    public interface Ouvinte {
        void aoMudar(long idServidor, boolean conectado);
    }

    /** Teste da porta; nos testes, uma resposta pronta. */
    interface Porta {
        boolean abre(String ip, int porta, int limiteMs);
    }

    /** Servidores a testar (id e IP). */
    interface Cadastro {
        List<Servidores.Servidor> lista();
    }

    static final int PORTA_SMB = 445;
    static final int LIMITE_MS = 2000;
    static final long REPETIR_FORA_MS = 60 * 1000L;
    private static final String PREFS = "starktech_servidor";
    private static final String CHAVE = "conectado.";

    private static EstadoServidor instancia;

    private final Cadastro cadastro;
    private final Porta porta;
    private final Executor tela;
    private final SharedPreferences guardado;
    private final Map<Long, Boolean> estado = new ConcurrentHashMap<>();
    private final List<Ouvinte> ouvintes = new CopyOnWriteArrayList<>();
    private final ExecutorService fila = Executors.newSingleThreadExecutor(r -> new Thread(r, "StarkTechServidor"));
    private final AtomicBoolean naFila = new AtomicBoolean();
    private Handler repetir;
    private final Runnable repeticao = this::conferir;

    public static synchronized EstadoServidor de(Context contexto) {
        if (instancia == null) {
            Context app = contexto.getApplicationContext();
            Servidores servidores = Servidores.de(app);
            Handler principal = new Handler(Looper.getMainLooper());
            instancia = new EstadoServidor(servidores::lista, EstadoServidor::testarPorta, principal::post,
                    app.getSharedPreferences(PREFS, Context.MODE_PRIVATE));
            instancia.repetir = principal;
            instancia.ouvirRede(app);
        }
        return instancia;
    }

    EstadoServidor(Cadastro cadastro, Porta porta, Executor tela, SharedPreferences guardado) {
        this.cadastro = cadastro;
        this.porta = porta;
        this.tela = tela;
        this.guardado = guardado;
    }

    /** Estado guardado, sem rede; servidor nunca testado conta como fora. */
    public boolean conectado(long idServidor) {
        Boolean agora = estado.get(idServidor);
        if (agora != null) return agora;
        boolean ultimo = guardado != null && guardado.getBoolean(CHAVE + idServidor, false);
        estado.put(idServidor, ultimo);
        return ultimo;
    }

    /** true = há servidor cadastrado e algum está ao alcance. */
    public boolean algumConectado() {
        for (Servidores.Servidor s : cadastro.lista()) if (conectado(s.id)) return true;
        return false;
    }

    public void ouvir(Ouvinte ouvinte) {
        ouvintes.add(ouvinte);
    }

    public void parar(Ouvinte ouvinte) {
        ouvintes.remove(ouvinte);
    }

    /** Confere fora da tela; pedido repetido enquanto um espera não empilha. */
    public void conferir() {
        if (!naFila.compareAndSet(false, true)) return;
        fila.execute(() -> {
            naFila.set(false);
            conferirAgora();
        });
    }

    /** Confere agora, na thread de quem chama (a da varredura, fora da tela), antes de listar. */
    public void conferirEEsperar() {
        conferirAgora();
    }

    /** Servidor removido: some do estado. */
    public void esquecer(long idServidor) {
        estado.remove(idServidor);
        if (guardado != null) guardado.edit().remove(CHAVE + idServidor).apply();
    }

    /** Testa cada servidor agora (fora da tela) e avisa as mudanças; devolve quantos estão fora. */
    int conferirAgora() {
        List<Servidores.Servidor> servidores = new ArrayList<>(cadastro.lista());
        Map<String, Boolean> porIp = new java.util.HashMap<>();
        int fora = 0;
        for (Servidores.Servidor s : servidores) {
            Boolean abre = porIp.get(s.ip);
            if (abre == null) {
                abre = porta.abre(s.ip, PORTA_SMB, LIMITE_MS);
                porIp.put(s.ip, abre);
            }
            if (!abre) fora++;
            definir(s.id, abre);
        }
        agendar(fora > 0);
        return fora;
    }

    /** Grava o estado e avisa se mudou (um servidor novo, ao entrar ligado, também avisa). */
    void definir(long idServidor, boolean conectado) {
        Boolean antes = estado.put(idServidor, conectado);
        if (guardado != null) guardado.edit().putBoolean(CHAVE + idServidor, conectado).apply();
        boolean mudou = antes == null ? conectado : antes != conectado;
        if (!mudou) return;
        tela.execute(() -> {
            for (Ouvinte o : ouvintes) o.aoMudar(idServidor, conectado);
        });
    }

    /** Com algum fora: confere de novo daqui a 1 min. */
    private void agendar(boolean algumFora) {
        Handler h = repetir;
        if (h == null) return;
        h.removeCallbacks(repeticao);
        if (algumFora) h.postDelayed(repeticao, REPETIR_FORA_MS);
    }

    /** Rede que chega ou sai (Wi-Fi, cabo): confere de novo. Inclui rede sem internet (a de casa pode não ter). */
    private void ouvirRede(Context app) {
        try {
            ConnectivityManager conexoes = (ConnectivityManager) app.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (conexoes == null) return;
            NetworkRequest pedido = new NetworkRequest.Builder()
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();
            conexoes.registerNetworkCallback(pedido, new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network rede) {
                    conferir();
                }

                @Override
                public void onLost(Network rede) {
                    conferir();
                }
            });
        } catch (RuntimeException e) {
            // sem permissão ou sem o serviço: fica o teste ao abrir, ao voltar e a cada minuto
        }
    }

    /**
     * A porta 445 do IP abre em até 2 s (Entrar do Adicionar servidor: o "fora de alcance" vem logo, sem esperar o
     * tempo do SMB). Rede: chamar fora da tela.
     */
    public static boolean portaAbre(String ip) {
        return testarPorta(ip, PORTA_SMB, LIMITE_MS);
    }

    /** Abre e fecha a porta; sem resposta no limite = fora de alcance. */
    private static boolean testarPorta(String ip, int porta, int limiteMs) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, porta), limiteMs);
            return true;
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }
}
