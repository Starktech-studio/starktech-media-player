// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.musica;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;

import com.archos.mediacenter.video.R;

import br.com.starktechstudio.mediaplayer.comum.TocandoAgoraActivity;
import br.com.starktechstudio.mediaplayer.dados.ModeloMusicas.Musica;

/**
 * Música fora da tela (1.5, D1): serviço em primeiro plano enquanto toca, com a notificação de mídia (capa,
 * nome, artista; anterior, tocar ou pausar, próxima). Pausada, a notificação fica, mas pode ser fechada, e
 * fechar para a música. Quem toca é o {@link Tocador}; o serviço só segura o app vivo e mostra a notificação.
 */
public class ServicoMusica extends Service {

    private static final String LOG = "StarkTechMusica";
    private static final String CANAL = "st_musica";
    private static final int NOTIFICACAO = 7401;
    private static final String ACAO_TOCAR_PAUSAR = "br.com.starktechstudio.mediaplayer.musica.TOCAR_PAUSAR";
    private static final String ACAO_PROXIMA = "br.com.starktechstudio.mediaplayer.musica.PROXIMA";
    private static final String ACAO_ANTERIOR = "br.com.starktechstudio.mediaplayer.musica.ANTERIOR";
    private static final String ACAO_PARAR = "br.com.starktechstudio.mediaplayer.musica.PARAR";

    @Nullable
    private static ServicoMusica ativo;
    private boolean naFrente;

    /** Começa (ou só atualiza) ao tocar; chamado da tela, com o app à vista. */
    static void iniciar(Context contexto) {
        if (ativo != null) {
            ativo.mostrar();
            return;
        }
        try {
            ContextCompat.startForegroundService(contexto, new Intent(contexto, ServicoMusica.class));
        } catch (RuntimeException e) {
            // Android não deixou (app fora da tela): toca assim mesmo, sem a notificação
            Log.w(LOG, "não deu para abrir o serviço da música", e);
        }
    }

    /** Estado novo no {@link Tocador}: a notificação acompanha. */
    static void atualizar() {
        if (ativo != null) ativo.mostrar();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ativo = this;
        criarCanal();
        // Até 5 s depois do startForegroundService: já na frente, com o que estiver tocando
        irParaFrente(notificacao());
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        String acao = intent == null ? null : intent.getAction();
        Tocador tocador = Tocador.de(this);
        if (ACAO_TOCAR_PAUSAR.equals(acao)) tocador.pausarOuContinuar();
        else if (ACAO_PROXIMA.equals(acao)) tocador.proxima();
        else if (ACAO_ANTERIOR.equals(acao)) tocador.anterior();
        else if (ACAO_PARAR.equals(acao)) tocador.parar();
        mostrar();
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // App fechado pela lista de recentes: parada, a música não fica sozinha na notificação
        if (!Tocador.de(this).querTocar()) Tocador.de(this).parar();
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        if (ativo == this) ativo = null;
        super.onDestroy();
    }

    /** Tocando: na frente, sem poder fechar. Pausada: a notificação fica e pode ser fechada. Nada na fila: some. */
    private void mostrar() {
        Tocador tocador = Tocador.de(this);
        if (tocador.atual() == null) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
            naFrente = false;
            stopSelf();
            return;
        }
        Notification aviso = notificacao();
        if (tocador.querTocar()) {
            irParaFrente(aviso);
        } else {
            if (naFrente) {
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH);
                naFrente = false;
            }
            // Sem a permissão de notificação (Android 13+): só não aparece
            boolean pode = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
            if (pode) {
                try {
                    NotificationManagerCompat.from(this).notify(NOTIFICACAO, aviso);
                } catch (SecurityException e) {
                    // permissão tirada no meio do caminho
                }
            }
        }
    }

    private void irParaFrente(Notification aviso) {
        try {
            ServiceCompat.startForeground(this, NOTIFICACAO, aviso,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK : 0);
            naFrente = true;
        } catch (RuntimeException e) {
            Log.w(LOG, "não deu para pôr a música na frente", e);
        }
    }

    private Notification notificacao() {
        Tocador tocador = Tocador.de(this);
        Musica m = tocador.atual();
        boolean tocando = tocador.querTocar();
        Intent abrir = new Intent(this, TocandoAgoraActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        NotificationCompat.Builder aviso = new NotificationCompat.Builder(this, CANAL)
                .setSmallIcon(R.drawable.nova_notification)
                .setContentTitle(m == null ? getString(R.string.st_aba_musicas) : m.titulo)
                .setContentText(m == null || m.artista == null ? getString(R.string.st_musicas_artista_desconhecido) : m.artista)
                .setSubText(m == null ? null : m.album)
                .setLargeIcon(tocador.capa())
                .setContentIntent(PendingIntent.getActivity(this, 0, abrir,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE))
                .setDeleteIntent(acao(ACAO_PARAR, 4))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setOngoing(tocando)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .addAction(botao(R.drawable.st_ic_cap_anterior, R.string.st_musicas_anterior, ACAO_ANTERIOR, 1))
                .addAction(tocando ? botao(R.drawable.st_ic_pausar, R.string.st_pausar, ACAO_TOCAR_PAUSAR, 2)
                        : botao(R.drawable.st_ic_assistir, R.string.st_musicas_tocar, ACAO_TOCAR_PAUSAR, 2))
                .addAction(botao(R.drawable.st_ic_cap_proximo, R.string.st_musicas_proxima, ACAO_PROXIMA, 3))
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(tocador.token())
                        .setShowActionsInCompactView(0, 1, 2));
        return aviso.build();
    }

    private NotificationCompat.Action botao(@DrawableRes int icone, @StringRes int nome, String acao, int pedido) {
        return new NotificationCompat.Action(icone, getString(nome), acao(acao, pedido));
    }

    private PendingIntent acao(String acao, int pedido) {
        Intent intent = new Intent(this, ServicoMusica.class).setAction(acao);
        return PendingIntent.getService(this, pedido, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private void criarCanal() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationChannel canal = new NotificationChannel(CANAL, getString(R.string.st_musicas_canal), NotificationManager.IMPORTANCE_LOW);
        canal.setShowBadge(false);
        NotificationManager gerente = getSystemService(NotificationManager.class);
        if (gerente != null) gerente.createNotificationChannel(canal);
    }
}
