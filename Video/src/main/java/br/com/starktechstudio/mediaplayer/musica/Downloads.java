// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.musica;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import br.com.starktechstudio.mediaplayer.comum.Preferencias;
import br.com.starktechstudio.mediaplayer.motor.AcervoMusicas;
import br.com.starktechstudio.mediaplayer.motor.ArquivosDaBiblioteca;
import br.com.starktechstudio.mediaplayer.motor.ArquivosDoAparelho;
import br.com.starktechstudio.mediaplayer.motor.BancoStarkTech;
import br.com.starktechstudio.mediaplayer.motor.Pastas;

/**
 * Músicas do servidor em casa baixadas para o aparelho (2.0, D10 a D13), na tabela {@code baixada} do banco:
 * caminho no servidor (o id da música, que segue o mesmo: as playlists continuam valendo), caminho local,
 * tamanho e situação (na fila ou baixada).
 *
 * <p>Destino (D11): {@code Music/StarkTech Media Player} no armazenamento escolhido em Configurações › Músicas
 * (memória interna de fábrica, ou o cartão ou pendrive), espelhando o caminho de dentro da pasta de Músicas
 * (Artista/Álbum/faixa). Reconhece de volta pelo caminho e pelo tamanho, sem arquivo de controle: ao abrir o
 * app, ao cadastrar o servidor e ao trocar o destino. Quem copia é o {@link ServicoDownload}.
 */
public final class Downloads {

    public static final int NA_FILA = 0;
    public static final int BAIXADA = 1;

    /** Uma linha da tabela. */
    static final class Registro {
        final String caminho;
        final String local;
        final long tamanho;
        final int situacao;

        Registro(String caminho, String local, long tamanho, int situacao) {
            this.caminho = caminho;
            this.local = local;
            this.tamanho = tamanho;
            this.situacao = situacao;
        }
    }

    private static Downloads instancia;

    private final Context app;
    private final Handler tela = new Handler(Looper.getMainLooper());
    private final List<Runnable> ouvintes = new CopyOnWriteArrayList<>();
    private final ExecutorService fundo = Executors.newSingleThreadExecutor(r -> new Thread(r, "StarkTechDownloads"));
    /** Pelo caminho no servidor, na ordem em que entraram. Só com o lock deste objeto. */
    private Map<String, Registro> registros;
    /** Andamento da fila desde que ficou vazia pela última vez: "Baixando 3 de 12". */
    private int feitasNaRodada;
    private int totalNaRodada;

    public static synchronized Downloads de(Context contexto) {
        if (instancia == null) instancia = new Downloads(contexto.getApplicationContext());
        return instancia;
    }

    private Downloads(Context app) {
        this.app = app;
    }

    // ---- Caminhos ----

    /**
     * Espelho do caminho (D10): a música {@code servidor:1/Musicas/Queen/Opera/01.mp3} da pasta de Músicas
     * {@code servidor:1/Musicas} vai para {@code <pasta de downloads>/Queen/Opera/01.mp3}. Fora da pasta, só o nome.
     */
    static String destino(String pastaDownloads, String caminhoMusica, @Nullable String pastaDeMusicas) {
        String relativo = pastaDeMusicas != null && Pastas.dentro(caminhoMusica, pastaDeMusicas)
                ? caminhoMusica.substring(pastaDeMusicas.length() + 1) : ArquivosDaBiblioteca.nome(caminhoMusica);
        return pastaDownloads + "/" + relativo;
    }

    /** A cópia vale: existe e tem o mesmo tamanho da música no servidor (D10, sem arquivo de controle). */
    static boolean confere(File local, long tamanho) {
        return tamanho > 0 && local.isFile() && local.length() == tamanho;
    }

    /** Raiz do armazenamento escolhido, se ainda montado; senão a memória interna. */
    public File raiz() {
        String escolhida = Preferencias.destinoDownloads(app);
        if (escolhida != null) {
            for (ArquivosDoAparelho.Raiz r : ArquivosDoAparelho.raizes(app)) {
                if (r.pasta.getAbsolutePath().equals(escolhida)) return r.pasta;
            }
        }
        return Environment.getExternalStorageDirectory();
    }

    /** {@code Music/StarkTech Media Player} na raiz escolhida (D11). */
    public File pasta() {
        return new File(raiz(), Pastas.PASTA_DOWNLOADS);
    }

    // ---- Leitura (memória) ----

    /** true = a música do servidor tem cópia no aparelho. */
    public synchronized boolean baixada(String caminho) {
        Registro r = carregar().get(caminho);
        return r != null && r.situacao == BAIXADA;
    }

    /** A cópia no aparelho, sem conferir o disco; nula se não baixada. */
    @Nullable
    public synchronized String local(String caminho) {
        Registro r = carregar().get(caminho);
        return r != null && r.situacao == BAIXADA ? r.local : null;
    }

    public synchronized boolean naFila(String caminho) {
        Registro r = carregar().get(caminho);
        return r != null && r.situacao == NA_FILA;
    }

    /** Quantas esperam na fila. */
    public synchronized int naFila() {
        int n = 0;
        for (Registro r : carregar().values()) if (r.situacao == NA_FILA) n++;
        return n;
    }

    /** As baixadas: caminho no servidor → caminho local. */
    public synchronized Map<String, String> baixadas() {
        Map<String, String> mapa = new HashMap<>();
        for (Registro r : carregar().values()) if (r.situacao == BAIXADA) mapa.put(r.caminho, r.local);
        return mapa;
    }

    /** Espaço das baixadas, em bytes. */
    public synchronized long espacoUsado() {
        long total = 0;
        for (Registro r : carregar().values()) if (r.situacao == BAIXADA) total += r.tamanho;
        return total;
    }

    /** {feitas, total} da fila desde que ficou vazia ("Baixando 3 de 12"); {0, 0} sem fila. */
    public synchronized int[] andamento() {
        return naFila() == 0 ? new int[]{0, 0} : new int[]{Math.min(feitasNaRodada + 1, totalNaRodada), totalNaRodada};
    }

    // ---- Avisos ----

    /** Avisado na thread da tela quando a fila ou as baixadas mudam. */
    public void ouvir(Runnable ouvinte) {
        if (!ouvintes.contains(ouvinte)) ouvintes.add(ouvinte);
    }

    public void deixarDeOuvir(Runnable ouvinte) {
        ouvintes.remove(ouvinte);
    }

    private void notificar() {
        tela.post(() -> {
            for (Runnable o : ouvintes) o.run();
        });
    }

    // ---- Ações ----

    /** Põe na fila só as do servidor que ainda não estão baixadas nem na fila; começa o serviço. Devolve quantas entraram. */
    public int baixar(Collection<String> caminhos) {
        int novas = 0;
        synchronized (this) {
            long agora = System.currentTimeMillis();
            for (String caminho : caminhos) {
                if (caminho == null || ArquivosDaBiblioteca.local(caminho) || carregar().containsKey(caminho)) continue;
                Registro r = new Registro(caminho, "", 0, NA_FILA);
                gravar(r, agora);
                novas++;
            }
            if (novas > 0) {
                if (feitasNaRodada >= totalNaRodada) {
                    feitasNaRodada = 0;
                    totalNaRodada = 0;
                }
                totalNaRodada += novas;
            }
        }
        if (novas > 0) {
            ServicoDownload.iniciar(app);
            notificar();
        }
        return novas;
    }

    /** Fila que parou (Android 15+: tempo do {@code dataSync}; app fechado) continua com o app à frente. */
    public void retomar() {
        if (naFila() > 0) ServicoDownload.iniciar(app);
    }

    /** Cancelar: a fila esvazia (a música em cópia para no próximo bloco); as baixadas ficam. */
    public void cancelar() {
        synchronized (this) {
            List<String> tirar = new ArrayList<>();
            for (Registro r : carregar().values()) if (r.situacao == NA_FILA) tirar.add(r.caminho);
            for (String caminho : tirar) apagarLinha(caminho);
            feitasNaRodada = 0;
            totalNaRodada = 0;
        }
        notificar();
    }

    /** Apaga as cópias e as linhas (fora da tela); as músicas seguem no servidor. */
    public void remover(Collection<String> caminhos) {
        List<String> lista = new ArrayList<>(caminhos);
        fundo.execute(() -> {
            synchronized (this) {
                for (String caminho : lista) {
                    Registro r = carregar().get(caminho);
                    if (r == null || r.situacao != BAIXADA) continue;
                    apagarArquivo(r.local);
                    apagarLinha(caminho);
                }
            }
            notificar();
        });
    }

    /** Remover todas (Configurações › Músicas, com confirmação). */
    public void removerTodas() {
        List<String> todas;
        synchronized (this) {
            todas = new ArrayList<>(baixadas().keySet());
        }
        remover(todas);
    }

    /** Trocou o destino: as que já estão lá voltam a valer, as do destino antigo deixam de contar. */
    public void destinoMudou() {
        reconhecer();
    }

    /**
     * D10: confere cada música do servidor contra o espelho no destino de agora, pelo tamanho. A que está lá
     * vira baixada; a baixada cuja cópia sumiu ou mudou de tamanho deixa de ser. Fora da tela.
     */
    public void reconhecer() {
        fundo.execute(() -> {
            boolean mudou = false;
            String pastaDownloads = pasta().getAbsolutePath();
            List<Pastas.Pasta> deMusicas = Pastas.de(app).deMusicas();
            List<AcervoMusicas.Musica> musicas = AcervoMusicas.de(app).musicas();
            synchronized (this) {
                long agora = System.currentTimeMillis();
                Map<String, AcervoMusicas.Musica> doServidor = new HashMap<>();
                for (AcervoMusicas.Musica m : musicas) if (!ArquivosDaBiblioteca.local(m.caminho)) doServidor.put(m.caminho, m);
                // Baixadas que não valem mais (cópia apagada à mão, música trocada no servidor, outro destino)
                for (Registro r : new ArrayList<>(carregar().values())) {
                    if (r.situacao != BAIXADA) continue;
                    AcervoMusicas.Musica m = doServidor.get(r.caminho);
                    long tamanho = m != null && m.tamanho > 0 ? m.tamanho : r.tamanho;
                    if (!confere(new File(r.local), tamanho) || !r.local.startsWith(pastaDownloads + "/")) {
                        apagarLinha(r.caminho);
                        mudou = true;
                    }
                }
                // As que estão no destino com o mesmo tamanho
                for (AcervoMusicas.Musica m : doServidor.values()) {
                    if (carregar().containsKey(m.caminho)) continue;
                    String local = destino(pastaDownloads, m.caminho, pastaDe(deMusicas, m));
                    if (confere(new File(local), m.tamanho)) {
                        gravar(new Registro(m.caminho, local, m.tamanho, BAIXADA), agora);
                        mudou = true;
                    }
                }
            }
            if (mudou) notificar();
        });
    }

    @Nullable
    private static String pastaDe(List<Pastas.Pasta> deMusicas, AcervoMusicas.Musica m) {
        if (m.pasta != null) return m.pasta;
        for (Pastas.Pasta p : deMusicas) if (Pastas.dentro(m.caminho, p.caminho)) return p.caminho;
        return null;
    }

    // ---- Fila (ServicoDownload) ----

    /** A próxima da fila; nula se vazia. */
    @Nullable
    synchronized String proxima() {
        for (Registro r : carregar().values()) if (r.situacao == NA_FILA) return r.caminho;
        return null;
    }

    /** Onde a música vai ficar, pelo espelho do caminho. */
    String destinoDe(String caminho, @Nullable String pastaDeMusicas) {
        return destino(pasta().getAbsolutePath(), caminho, pastaDeMusicas);
    }

    /** Cópia pronta (o {@code .parte} já trocado pelo nome final). */
    void concluir(String caminho, String local, long tamanho) {
        synchronized (this) {
            // Cancelada no meio da cópia: a cópia já está inteira e vale, só não conta no andamento
            boolean daFila = carregar().containsKey(caminho);
            gravar(new Registro(caminho, local, tamanho, BAIXADA), System.currentTimeMillis());
            if (daFila) feitasNaRodada++;
            if (naFila() == 0) {
                feitasNaRodada = 0;
                totalNaRodada = 0;
            }
        }
        notificar();
    }

    /** Não deu (sem espaço, servidor fora, erro): sai da fila; a próxima tentativa é um Baixar novo. */
    void falhou(String caminho) {
        synchronized (this) {
            Registro r = carregar().get(caminho);
            if (r != null && r.situacao == NA_FILA) {
                apagarLinha(caminho);
                feitasNaRodada++;
            }
            if (naFila() == 0) {
                feitasNaRodada = 0;
                totalNaRodada = 0;
            }
        }
        notificar();
    }

    // ---- Banco ----

    private SQLiteDatabase escrita() {
        return BancoStarkTech.de(app).getWritableDatabase();
    }

    private Map<String, Registro> carregar() {
        if (registros != null) return registros;
        registros = new LinkedHashMap<>();
        try (Cursor c = BancoStarkTech.de(app).getReadableDatabase().query(BancoStarkTech.BAIXADA,
                new String[]{"caminho", "local", "tamanho", "situacao"}, null, null, null, null, "rowid")) {
            while (c.moveToNext()) {
                registros.put(c.getString(0), new Registro(c.getString(0), c.getString(1), c.getLong(2), c.getInt(3)));
            }
        }
        return registros;
    }

    private void gravar(Registro r, long agora) {
        ContentValues linha = new ContentValues();
        linha.put("caminho", r.caminho);
        linha.put("local", r.local);
        linha.put("tamanho", r.tamanho);
        linha.put("situacao", r.situacao);
        linha.put("alterado", agora);
        escrita().insertWithOnConflict(BancoStarkTech.BAIXADA, null, linha, SQLiteDatabase.CONFLICT_REPLACE);
        carregar().put(r.caminho, r);
    }

    private void apagarLinha(String caminho) {
        escrita().delete(BancoStarkTech.BAIXADA, "caminho = ?", new String[]{caminho});
        carregar().remove(caminho);
    }

    /** Apaga a cópia e as pastas que ficaram vazias, até a pasta de downloads. */
    private void apagarArquivo(String local) {
        if (local == null || local.isEmpty()) return;
        File arquivo = new File(local);
        //noinspection ResultOfMethodCallIgnored
        arquivo.delete();
        File base = pasta();
        File acima = arquivo.getParentFile();
        while (acima != null && !acima.equals(base) && acima.getAbsolutePath().startsWith(base.getAbsolutePath() + "/")) {
            String[] resto = acima.list();
            if (resto == null || resto.length > 0 || !acima.delete()) break;
            acima = acima.getParentFile();
        }
    }
}
