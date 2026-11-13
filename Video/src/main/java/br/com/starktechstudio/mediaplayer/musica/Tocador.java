// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.musica;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.KeyEvent;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.media.AudioAttributesCompat;
import androidx.media.AudioFocusRequestCompat;
import androidx.media.AudioManagerCompat;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import br.com.starktechstudio.mediaplayer.comum.Preferencias;
import br.com.starktechstudio.mediaplayer.dados.ModeloMusicas.Musica;

/**
 * Player de música (1.5): um só no app, com o {@link MediaPlayer} do Android (os formatos das pastas de
 * Músicas), a {@link Fila}, o foco de áudio (outro app tocando pausa ou abaixa), a pausa ao tirar o fone e a
 * sessão de mídia, que leva os botões do fone, do controle e da notificação. Tocando, o
 * {@link ServicoMusica} mantém o app vivo fora da tela, com a notificação. Tudo na thread da tela.
 */
public final class Tocador {

    private static final String LOG = "StarkTechMusica";
    /** "Anterior" depois disso recomeça a música em vez de voltar uma. */
    private static final int RECOMECA_MS = 3000;
    private static final float VOLUME_BAIXO = 0.2f;
    /** Lado da capa na sessão e na notificação. */
    private static final int LADO_CAPA = 320;

    @Nullable
    private static Tocador instancia;

    private final Context app;
    private final Handler tela = new Handler(Looper.getMainLooper());
    private final Fila fila = new Fila();
    private final List<Runnable> ouvintes = new CopyOnWriteArrayList<>();
    private final AudioManager audio;
    private final MediaSessionCompat sessao;
    private MediaPlayer player;
    private boolean preparado;
    private boolean querTocando;
    private boolean perdeuFocoAgora;
    private boolean temFoco;
    @Nullable
    private AudioFocusRequestCompat pedidoDeFoco;
    private int errosSeguidos;
    private int posicaoPedidaMs;
    @Nullable
    private Bitmap capa;
    @Nullable
    private String capaDe;
    private boolean ouvindoFone;

    /** A cada meio segundo, com a música tocando: as telas atualizam o tempo. */
    private final Runnable tique = new Runnable() {
        @Override
        public void run() {
            avisar();
            if (tocando()) tela.postDelayed(this, 500);
        }
    };

    private final BroadcastReceiver fone = new BroadcastReceiver() {
        @Override
        public void onReceive(Context contexto, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) pausar();
        }
    };

    private final AudioManager.OnAudioFocusChangeListener aoMudarFoco = mudanca -> {
        switch (mudanca) {
            case AudioManager.AUDIOFOCUS_GAIN:
                temFoco = true;
                if (player != null) player.setVolume(1f, 1f);
                if (perdeuFocoAgora) {
                    perdeuFocoAgora = false;
                    continuar();
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (player != null) player.setVolume(VOLUME_BAIXO, VOLUME_BAIXO);
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (tocando()) {
                    perdeuFocoAgora = true;
                    pausarSemSoltarFoco();
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
            default:
                temFoco = false;
                perdeuFocoAgora = false;
                pausar();
                break;
        }
    };

    public static synchronized Tocador de(Context contexto) {
        if (instancia == null) instancia = new Tocador(contexto.getApplicationContext());
        return instancia;
    }

    /** Pausa se já existe um tocando (abrir um vídeo); não cria o player à toa. */
    public static void pausarSeTocando() {
        Tocador t = instancia;
        if (t != null && t.tocando()) t.pausar();
    }

    /** O que já existe, sem criar; nulo = nunca tocou nada. */
    @Nullable
    public static Tocador existente() {
        return instancia;
    }

    private Tocador(Context app) {
        this.app = app;
        audio = (AudioManager) app.getSystemService(Context.AUDIO_SERVICE);
        sessao = new MediaSessionCompat(app, "StarkTechMusica");
        sessao.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                continuar();
            }

            @Override
            public void onPause() {
                pausar();
            }

            @Override
            public void onSkipToNext() {
                proxima();
            }

            @Override
            public void onSkipToPrevious() {
                anterior();
            }

            @Override
            public void onSeekTo(long pos) {
                irPara((int) pos);
            }

            @Override
            public void onStop() {
                parar();
            }

            @Override
            public boolean onMediaButtonEvent(Intent botao) {
                KeyEvent tecla = botao.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                if (tecla != null && tecla.getAction() == KeyEvent.ACTION_DOWN
                        && tecla.getKeyCode() == KeyEvent.KEYCODE_HEADSETHOOK) {
                    pausarOuContinuar();
                    return true;
                }
                return super.onMediaButtonEvent(botao);
            }
        });
    }

    // ---- Ouvintes ----

    /** Roda na thread da tela quando a música, a fila ou o estado mudam, e a cada meio segundo tocando. */
    public void ouvir(Runnable ouvinte) {
        if (!ouvintes.contains(ouvinte)) ouvintes.add(ouvinte);
    }

    public void deixarDeOuvir(Runnable ouvinte) {
        ouvintes.remove(ouvinte);
    }

    private void avisar() {
        for (Runnable o : ouvintes) o.run();
    }

    // ---- Estado ----

    @Nullable
    public Musica atual() {
        return fila.atual();
    }

    public boolean tocando() {
        return player != null && preparado && player.isPlaying();
    }

    /** Tocando ou a ponto de tocar (abrindo a música). */
    public boolean querTocar() {
        return querTocando;
    }

    public int posicaoMs() {
        if (player == null || !preparado) return posicaoPedidaMs;
        try {
            return player.getCurrentPosition();
        } catch (IllegalStateException e) {
            return 0;
        }
    }

    public int duracaoMs() {
        Musica m = atual();
        if (player != null && preparado) {
            try {
                int d = player.getDuration();
                if (d > 0) return d;
            } catch (IllegalStateException e) {
                // cai na da biblioteca
            }
        }
        return m == null ? 0 : (int) m.duracaoMs;
    }

    public boolean aleatorio() {
        return fila.aleatorio();
    }

    public Fila.Repetir repetir() {
        return fila.repetir();
    }

    public List<Musica> fila() {
        return fila.naOrdem();
    }

    public int posicaoNaFila() {
        return fila.posicao();
    }

    MediaSessionCompat.Token token() {
        return sessao.getSessionToken();
    }

    @Nullable
    Bitmap capa() {
        Musica m = atual();
        String arquivo = m == null ? null : m.capa;
        if (arquivo == null) return null;
        if (!arquivo.equals(capaDe)) {
            capaDe = arquivo;
            capa = reduzida(arquivo);
        }
        return capa;
    }

    // ---- Comandos ----

    /** Troca a fila e toca a partir de {@code inicio}; com o aleatório guardado ligado (1.6), sorteia o resto. */
    public void tocar(List<Musica> musicas, int inicio, boolean aleatorio) {
        if (musicas.isEmpty()) return;
        fila.definir(musicas, inicio, aleatorio || Preferencias.aleatorioMusica(app));
        errosSeguidos = 0;
        abrir(true);
    }

    public void pausarOuContinuar() {
        if (tocando() || (querTocando && !preparado)) pausar();
        else continuar();
    }

    public void continuar() {
        if (fila.vazia()) return;
        if (player == null) {
            abrir(true);
            return;
        }
        querTocando = true;
        if (!preparado) return;
        if (!pedirFoco()) return;
        player.start();
        ouvirFone(true);
        mudouEstado();
    }

    public void pausar() {
        pausarSemSoltarFoco();
        perdeuFocoAgora = false;
    }

    private void pausarSemSoltarFoco() {
        querTocando = false;
        if (player != null && preparado && player.isPlaying()) player.pause();
        ouvirFone(false);
        mudouEstado();
    }

    public void proxima() {
        Musica seguinte = fila.proxima(true);
        if (seguinte == null) return;
        errosSeguidos = 0;
        abrir(querTocando || tocando());
    }

    public void anterior() {
        if (posicaoMs() > RECOMECA_MS) {
            irPara(0);
            return;
        }
        fila.anterior();
        errosSeguidos = 0;
        abrir(querTocando || tocando());
    }

    public void irPara(int ms) {
        if (player == null || !preparado) {
            posicaoPedidaMs = Math.max(0, ms);
            return;
        }
        player.seekTo(Math.max(0, Math.min(ms, Math.max(0, duracaoMs() - 500))));
        mudouEstado();
    }

    /** Toca a {@code indice} da fila (na ordem de tocar). */
    public void tocarDaFila(int indice) {
        fila.irPara(indice);
        errosSeguidos = 0;
        abrir(true);
    }

    /** Liga ou desliga o aleatório da fila e guarda a escolha (1.6): vale para o que tocar depois. */
    public void alternarAleatorio() {
        fila.alternarAleatorio();
        Preferencias.definirAleatorioMusica(app, fila.aleatorio());
        mudouEstado();
    }

    public void trocarRepetir() {
        fila.trocarRepetir();
        mudouEstado();
    }

    public void tocarASeguir(List<Musica> musicas) {
        boolean vazia = fila.vazia();
        fila.tocarASeguir(musicas);
        if (vazia) abrir(true);
        else mudouEstado();
    }

    public void porNoFim(List<Musica> musicas) {
        boolean vazia = fila.vazia();
        fila.porNoFim(musicas);
        if (vazia) abrir(true);
        else mudouEstado();
    }

    /** Para e some com a notificação (fechar a notificação, "parar" da sessão). */
    public void parar() {
        querTocando = false;
        soltarPlayer();
        soltarFoco();
        ouvirFone(false);
        fila.limpar();
        sessao.setActive(false);
        mudouEstado();
    }

    // ---- Player ----

    private void abrir(boolean tocar) {
        Musica m = fila.atual();
        if (m == null) {
            parar();
            return;
        }
        querTocando = tocar;
        preparado = false;
        posicaoPedidaMs = 0;
        if (player == null) {
            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            player.setWakeMode(app, PowerManager.PARTIAL_WAKE_LOCK);
            player.setOnPreparedListener(p -> aoPreparar());
            player.setOnCompletionListener(p -> aoTerminar());
            player.setOnErrorListener((p, oque, extra) -> {
                Musica falhou = fila.atual();
                Log.w(LOG, "não tocou " + (falhou == null ? "" : falhou.id) + " (" + oque + ", " + extra + ")");
                aoFalhar();
                return true;
            });
        } else {
            player.reset();
        }
        try {
            player.setDataSource(m.id);
            player.prepareAsync();
        } catch (IOException | RuntimeException e) {
            Log.w(LOG, "não abriu " + m.id, e);
            tela.post(this::aoFalhar);
        }
        sessao.setActive(true);
        if (tocar) ServicoMusica.iniciar(app);
        mudouEstado();
    }

    private void aoPreparar() {
        preparado = true;
        errosSeguidos = 0;
        if (posicaoPedidaMs > 0) player.seekTo(posicaoPedidaMs);
        if (querTocando && pedirFoco()) {
            player.start();
            ouvirFone(true);
        } else {
            querTocando = false;
        }
        mudouEstado();
    }

    private void aoTerminar() {
        Musica seguinte = fila.proxima(false);
        if (seguinte == null) {
            // Fim da fila: fica na última, parada no começo
            soltarFoco();
            ouvirFone(false);
            abrir(false);
            return;
        }
        abrir(true);
    }

    /** Arquivo que não toca: vai para a seguinte, até dar a volta na fila. */
    private void aoFalhar() {
        preparado = false;
        errosSeguidos++;
        if (errosSeguidos >= Math.max(1, fila.naOrdem().size())) {
            querTocando = false;
            mudouEstado();
            return;
        }
        boolean tocar = querTocando;
        if (fila.proxima(true) == null) {
            querTocando = false;
            mudouEstado();
            return;
        }
        abrir(tocar);
    }

    private void soltarPlayer() {
        preparado = false;
        if (player != null) {
            try {
                player.release();
            } catch (RuntimeException e) {
                // já solto
            }
            player = null;
        }
    }

    // ---- Foco, fone e sessão ----

    private boolean pedirFoco() {
        if (temFoco) return true;
        if (pedidoDeFoco == null) {
            pedidoDeFoco = new AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(new AudioAttributesCompat.Builder()
                            .setUsage(AudioAttributesCompat.USAGE_MEDIA)
                            .setContentType(AudioAttributesCompat.CONTENT_TYPE_MUSIC)
                            .build())
                    .setOnAudioFocusChangeListener(aoMudarFoco, tela)
                    .setWillPauseWhenDucked(false)
                    .build();
        }
        temFoco = AudioManagerCompat.requestAudioFocus(audio, pedidoDeFoco) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        return temFoco;
    }

    private void soltarFoco() {
        if (pedidoDeFoco != null && temFoco) AudioManagerCompat.abandonAudioFocusRequest(audio, pedidoDeFoco);
        temFoco = false;
    }

    private void ouvirFone(boolean ligar) {
        if (ligar == ouvindoFone) return;
        ouvindoFone = ligar;
        if (ligar) {
            ContextCompat.registerReceiver(app, fone, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
                    ContextCompat.RECEIVER_NOT_EXPORTED);
        } else {
            try {
                app.unregisterReceiver(fone);
            } catch (IllegalArgumentException e) {
                // não estava registrado
            }
        }
    }

    private void mudouEstado() {
        atualizarSessao();
        ServicoMusica.atualizar();
        tela.removeCallbacks(tique);
        tela.post(tique);
    }

    private void atualizarSessao() {
        Musica m = atual();
        long acoes = PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_PLAY_PAUSE
                | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                | PlaybackStateCompat.ACTION_SEEK_TO | PlaybackStateCompat.ACTION_STOP;
        int estado = m == null ? PlaybackStateCompat.STATE_NONE
                : tocando() ? PlaybackStateCompat.STATE_PLAYING
                : querTocando ? PlaybackStateCompat.STATE_BUFFERING : PlaybackStateCompat.STATE_PAUSED;
        sessao.setPlaybackState(new PlaybackStateCompat.Builder()
                .setActions(acoes)
                .setState(estado, posicaoMs(), tocando() ? 1f : 0f)
                .build());
        if (m == null) return;
        MediaMetadataCompat.Builder dados = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, m.id)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, m.titulo)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, m.artista)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, m.album)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duracaoMs());
        Bitmap imagem = capa();
        if (imagem != null) dados.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, imagem);
        sessao.setMetadata(dados.build());
    }

    @Nullable
    private static Bitmap reduzida(String arquivo) {
        try {
            BitmapFactory.Options medidas = new BitmapFactory.Options();
            medidas.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(arquivo, medidas);
            if (medidas.outWidth <= 0) return null;
            BitmapFactory.Options opcoes = new BitmapFactory.Options();
            int amostra = 1;
            while (Math.max(medidas.outWidth, medidas.outHeight) / (amostra * 2) >= LADO_CAPA) amostra *= 2;
            opcoes.inSampleSize = amostra;
            return BitmapFactory.decodeFile(arquivo, opcoes);
        } catch (RuntimeException | OutOfMemoryError e) {
            return null;
        }
    }
}
