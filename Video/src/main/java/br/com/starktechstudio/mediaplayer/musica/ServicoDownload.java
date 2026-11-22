// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.musica;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.StatFs;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;

import com.archos.mediacenter.video.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import br.com.starktechstudio.mediaplayer.motor.AcervoMusicas;
import br.com.starktechstudio.mediaplayer.motor.ArquivosDaBiblioteca;

/**
 * Cópia das músicas do servidor em casa para o aparelho (2.0, D13): serviço próprio em primeiro plano, do tipo
 * {@code dataSync}, com a notificação ("Baixando 3 de 12", o progresso e Cancelar). Uma música por vez: confere o
 * espaço livre antes de cada uma, copia para {@code .parte} e só no fim troca pelo nome final (a cópia pela
 * metade nunca conta como baixada). Sem fila, para sozinho.
 *
 * <p>Android 15+: o {@code dataSync} tem 6 h a cada 24 h; ao passar, {@link #onTimeout} para a cópia e a fila
 * continua quando o app volta à frente ({@link Downloads#retomar}).
 */
public class ServicoDownload extends Service {

    private static final String LOG = "StarkTechDownloads";
    private static final String CANAL = "st_downloads";
    private static final int NOTIFICACAO = 7402;
    private static final String ACAO_CANCELAR = "br.com.starktechstudio.mediaplayer.musica.CANCELAR_DOWNLOADS";
    /** Folga além do tamanho da música, para o sistema não ficar sem espaço. */
    private static final long FOLGA = 50L * 1024 * 1024;
    private static final int BLOCO = 256 * 1024;
    /** A notificação muda no máximo a cada isto (o Android limita as atualizações). */
    private static final long AVISO_MS = 1000;

    private final ExecutorService copia = Executors.newSingleThreadExecutor(r -> new Thread(r, "StarkTechCopia"));
    private volatile boolean rodando;
    private volatile boolean parar;
    private long ultimoAviso;

    /** Começa (ou segue) a fila; chamado com o app à vista. */
    static void iniciar(Context contexto) {
        try {
            ContextCompat.startForegroundService(contexto, new Intent(contexto, ServicoDownload.class));
        } catch (RuntimeException e) {
            // Android não deixou (app fora da tela): a fila espera o app voltar à frente
            Log.w(LOG, "não deu para abrir o serviço de downloads", e);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        criarCanal();
        irParaFrente(notificacao(null, 0));
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        if (intent != null && ACAO_CANCELAR.equals(intent.getAction())) {
            Downloads.de(this).cancelar();
            parar = true;
            terminar();
            return START_NOT_STICKY;
        }
        // Cada pedido de primeiro plano precisa da notificação na frente
        irParaFrente(notificacao(null, 0));
        if (!rodando) {
            rodando = true;
            parar = false;
            copia.execute(this::copiarFila);
        }
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /** Android 15+: o tempo do {@code dataSync} acabou. Para agora; a fila fica para quando o app voltar. */
    @Override
    public void onTimeout(int startId, int tipo) {
        parar = true;
        terminar();
    }

    @Override
    public void onDestroy() {
        parar = true;
        copia.shutdown();
        super.onDestroy();
    }

    private void copiarFila() {
        Downloads downloads = Downloads.de(this);
        Map<String, AcervoMusicas.Musica> musicas = new HashMap<>();
        for (AcervoMusicas.Musica m : AcervoMusicas.de(this).musicas()) musicas.put(m.caminho, m);
        try {
            String caminho;
            while (!parar && (caminho = downloads.proxima()) != null) {
                AcervoMusicas.Musica m = musicas.get(caminho);
                if (m == null) {
                    // entrou depois desta lista (varredura nova): lê de novo
                    for (AcervoMusicas.Musica n : AcervoMusicas.de(this).musicas()) musicas.put(n.caminho, n);
                    m = musicas.get(caminho);
                }
                if (m == null) {
                    downloads.falhou(caminho);
                    continue;
                }
                if (!copiar(downloads, m)) {
                    if (parar) break;
                }
            }
        } catch (RuntimeException e) {
            Log.w(LOG, "fila de downloads parou", e);
        } finally {
            rodando = false;
            terminar();
        }
    }

    /** Copia uma música; false se não deu (a fila segue com a próxima, menos sem espaço: aí para tudo). */
    private boolean copiar(Downloads downloads, AcervoMusicas.Musica m) {
        String local = downloads.destinoDe(m.caminho, m.pasta);
        File destino = new File(local);
        File parte = new File(local + ".parte");
        File pai = destino.getParentFile();
        if (pai == null || (!pai.isDirectory() && !pai.mkdirs())) {
            Log.w(LOG, "sem pasta para " + destino.getName());
            downloads.falhou(m.caminho);
            return false;
        }
        if (!temEspaco(pai, m.tamanho)) {
            Log.w(LOG, "sem espaço para " + destino.getName());
            downloads.falhou(m.caminho);
            downloads.cancelar();
            avisar(getString(R.string.st_musicas_sem_espaco));
            parar = true;
            return false;
        }
        int[] andamento = downloads.andamento();
        mostrar(m, 0, andamento);
        long copiados = 0;
        try (InputStream entrada = ArquivosDaBiblioteca.de(this).abrir(m.caminho, 0);
             OutputStream saida = new FileOutputStream(parte)) {
            byte[] bloco = new byte[BLOCO];
            int lidos;
            while ((lidos = entrada.read(bloco)) > 0) {
                // Cancelar (pela notificação, pela tela ou fila esvaziada) para no próximo bloco
                if (parar || !downloads.naFila(m.caminho)) {
                    saida.close();
                    apagar(parte);
                    return false;
                }
                saida.write(bloco, 0, lidos);
                copiados += lidos;
                mostrar(m, m.tamanho > 0 ? (int) (copiados * 100 / m.tamanho) : 0, andamento);
            }
        } catch (IOException | RuntimeException e) {
            Log.w(LOG, "não deu para baixar " + destino.getName() + ": " + e.getClass().getSimpleName());
            apagar(parte);
            downloads.falhou(m.caminho);
            return false;
        }
        if (m.tamanho > 0 && copiados != m.tamanho) {
            Log.w(LOG, "cópia incompleta de " + destino.getName());
            apagar(parte);
            downloads.falhou(m.caminho);
            return false;
        }
        if (destino.exists()) apagar(destino);
        if (!parte.renameTo(destino)) {
            apagar(parte);
            downloads.falhou(m.caminho);
            return false;
        }
        downloads.concluir(m.caminho, destino.getAbsolutePath(), copiados);
        return true;
    }

    private static boolean temEspaco(File pasta, long tamanho) {
        try {
            return new StatFs(pasta.getAbsolutePath()).getAvailableBytes() >= tamanho + FOLGA;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static void apagar(File arquivo) {
        //noinspection ResultOfMethodCallIgnored
        arquivo.delete();
    }

    private void terminar() {
        try {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
        } catch (RuntimeException e) {
            // já fora da frente
        }
        stopSelf();
    }

    private void mostrar(AcervoMusicas.Musica m, int porcento, int[] andamento) {
        long agora = android.os.SystemClock.elapsedRealtime();
        if (porcento > 0 && porcento < 100 && agora - ultimoAviso < AVISO_MS) return;
        ultimoAviso = agora;
        Notification aviso = notificacao(m.titulo != null ? m.titulo : ArquivosDaBiblioteca.nome(m.caminho), porcento, andamento);
        try {
            NotificationManagerCompat.from(this).notify(NOTIFICACAO, aviso);
        } catch (SecurityException e) {
            // sem a permissão de notificação: a cópia segue sem aparecer
        }
    }

    private void avisar(String texto) {
        android.os.Handler tela = new android.os.Handler(android.os.Looper.getMainLooper());
        tela.post(() -> android.widget.Toast.makeText(getApplicationContext(), texto, android.widget.Toast.LENGTH_LONG).show());
    }

    private void irParaFrente(Notification aviso) {
        try {
            ServiceCompat.startForeground(this, NOTIFICACAO, aviso,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC : 0);
        } catch (RuntimeException e) {
            Log.w(LOG, "não deu para pôr os downloads na frente", e);
        }
    }

    private Notification notificacao(@Nullable String nome, int porcento) {
        return notificacao(nome, porcento, Downloads.de(this).andamento());
    }

    private Notification notificacao(@Nullable String nome, int porcento, int[] andamento) {
        String titulo = andamento[1] > 0 ? getString(R.string.st_musicas_baixando, andamento[0], andamento[1])
                : getString(R.string.st_musicas_downloads_canal);
        Intent cancelar = new Intent(this, ServicoDownload.class).setAction(ACAO_CANCELAR);
        return new NotificationCompat.Builder(this, CANAL)
                .setSmallIcon(R.drawable.nova_notification)
                .setContentTitle(titulo)
                .setContentText(nome)
                .setProgress(100, porcento, nome == null)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .addAction(new NotificationCompat.Action(R.drawable.st_ic_fechar, getString(R.string.st_cancelar),
                        PendingIntent.getService(this, 1, cancelar, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE)))
                .build();
    }

    private void criarCanal() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationChannel canal = new NotificationChannel(CANAL, getString(R.string.st_musicas_downloads_canal), NotificationManager.IMPORTANCE_LOW);
        canal.setShowBadge(false);
        NotificationManager gerente = getSystemService(NotificationManager.class);
        if (gerente != null) gerente.createNotificationChannel(canal);
    }
}
