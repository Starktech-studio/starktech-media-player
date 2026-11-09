// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.tv;

import android.app.Activity;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewOutlineProvider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.archos.mediacenter.video.R;
import com.archos.medialib.AndroidMediaPlayer;
import com.archos.medialib.IMediaPlayer;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import br.com.starktechstudio.mediaplayer.comum.Animacoes;
import br.com.starktechstudio.mediaplayer.comum.PecasDetalhe;
import br.com.starktechstudio.mediaplayer.comum.Preferencias;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Episodio;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Tipo;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Titulo;
import br.com.starktechstudio.mediaplayer.dados.RepositorioBiblioteca;

/**
 * Prévia em vídeo de Novidades na TV (D3), ligada ao item expandido ({@link DestaqueTv.Expansao}):
 * depois de 1,5 s o vídeo toca por 15 s, sem som, sobre a imagem, e a imagem volta. Série: o próximo
 * episódio. Se falhar (erro do motor, HD fora, arquivo que não abre), fica a imagem, sem aviso.
 * O vídeo só aparece com o primeiro quadro pronto; sem animação com "remover animações".
 * Prévia e player podem disputar o decodificador da TV: toda saída de cena solta o player da prévia,
 * e o Assistir só abre o player depois disso ({@link #aoAbrirPlayer}).
 */
final class PreviaVideoTv implements DestaqueTv.Expansao, TextureView.SurfaceTextureListener {

    /** Tempos propostos no plano: a espera com o item parado e quanto o vídeo toca. */
    private static final long ESPERA_MS = 1_500;
    private static final long TOCA_MS = 15_000;
    /** Sem o primeiro quadro nesse tempo (HD acordando, arquivo pesado), fica a imagem. */
    private static final long ABERTURA_MS = 15_000;
    /** O Assistir espera o motor até aqui: HD travado não prende a abertura do player. */
    private static final long LIMITE_SOLTAR_MS = 1_000;
    private static final long TRANSICAO_MS = 250;
    /** O vídeo começa em 1/10 da duração, depois das vinhetas (duração desconhecida: do início). */
    private static final int INICIO_DIVISOR = 10;

    /**
     * Todas as chamadas ao player, em fila e fora da tela: abrir e soltar podem travar em HD USB, e
     * a fila garante um player por vez e a superfície livre para o próximo. Sem uso, a thread acaba.
     */
    private static final Executor MOTOR = new ThreadPoolExecutor(0, 1, 30, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(), tarefa -> new Thread(tarefa, "StarkTechPrevia"));

    private final Activity atividade;
    private final Context aplicacao;
    private final RepositorioBiblioteca repositorio;
    private final TextureView tela;
    private final Handler relogio = new Handler(Looper.getMainLooper());

    private final Runnable comecar = this::abrirVideo;
    private final Runnable desistir = this::soltar;
    private final Runnable terminar = this::voltarAImagem;
    private final Runnable seguir = this::abrirAposSoltar;

    /** Item contando os 1,5 s. */
    @Nullable
    private Titulo aguardando;
    @Nullable
    private Sessao atual;
    /** Player esperando a prévia soltar. */
    @Nullable
    private Runnable aposSoltar;

    PreviaVideoTv(Activity atividade, View raiz, RepositorioBiblioteca repositorio) {
        this.atividade = atividade;
        this.repositorio = repositorio;
        aplicacao = atividade.getApplicationContext();
        tela = raiz.findViewById(R.id.st_destaque_video);
        tela.setSurfaceTextureListener(this);
        float canto = atividade.getResources().getDimension(R.dimen.st_tv_capa_canto);
        tela.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline contorno) {
                contorno.setRoundRect(0, 0, view.getWidth(), view.getHeight(), canto);
            }
        });
        tela.setClipToOutline(true);
    }

    @Override
    public void aoExpandir(Titulo titulo) {
        soltar();
        if (!previaLigada()) return;
        aguardando = titulo;
        relogio.postDelayed(comecar, ESPERA_MS);
    }

    @Override
    public void aoRecolher() {
        soltar();
    }

    @Override
    public void aoAbrirPlayer(Runnable abrir) {
        soltar();
        // Segundo OK durante a espera
        if (aposSoltar != null) return;
        aposSoltar = abrir;
        relogio.postDelayed(seguir, LIMITE_SOLTAR_MS);
        // A fila do motor só chega aqui depois de soltar o player
        MOTOR.execute(() -> relogio.post(seguir));
    }

    /** Solta tudo: a espera, o player e a superfície. Pode ser chamado sem nada tocando. */
    void soltar() {
        relogio.removeCallbacks(comecar);
        aguardando = null;
        liberarSessao();
        esconderTela();
    }

    /** Chave "Início: Prévia em vídeo" das Configurações. */
    private boolean previaLigada() {
        return Preferencias.previaLigada(aplicacao);
    }

    /**
     * O player da prévia. O do Android, e não o de {@code MediaFactory.createPlayer}: em
     * {@code AvosMediaPlayer} o {@code setVolume} é vazio (também na ponte JNI) e a prévia sairia
     * com som. Vídeo que o Android não abre fica na imagem.
     */
    private static IMediaPlayer criarPlayer(Context contexto) {
        return new AndroidMediaPlayer(contexto);
    }

    private void abrirVideo() {
        Titulo titulo = aguardando;
        aguardando = null;
        Sessao sessao = titulo == null ? null : sessaoDe(titulo);
        if (sessao == null) return;
        atual = sessao;
        tela.animate().cancel();
        tela.setAlpha(0f);
        tela.setVisibility(View.VISIBLE);
        relogio.postDelayed(desistir, ABERTURA_MS);
        // Na primeira vez a superfície só existe depois de desenhar: vem por onSurfaceTextureAvailable
        if (tela.isAvailable()) abrir(sessao, tela.getSurfaceTexture());
    }

    @Nullable
    private Sessao sessaoDe(Titulo titulo) {
        if (titulo.tipo == Tipo.SERIE) {
            Episodio proximo = PecasDetalhe.proximoEpisodio(repositorio.temporadas(titulo.id));
            if (proximo == null || proximo.caminho == null) return null;
            return new Sessao(proximo.caminho, proximo.duracaoSegundos);
        }
        return titulo.caminho == null ? null : new Sessao(titulo.caminho, titulo.duracaoSegundos);
    }

    private void abrir(Sessao sessao, SurfaceTexture textura) {
        sessao.superficie = new Surface(textura);
        MOTOR.execute(sessao::abrirNoMotor);
    }

    /** O tempo da prévia acabou (ou o vídeo): solta o motor e a imagem volta. */
    private void voltarAImagem() {
        liberarSessao();
        if (Animacoes.ligadas(atividade)) {
            tela.animate().alpha(0f).setDuration(TRANSICAO_MS)
                    .withEndAction(() -> tela.setVisibility(View.GONE)).start();
        } else {
            esconderTela();
        }
    }

    private void liberarSessao() {
        relogio.removeCallbacks(desistir);
        relogio.removeCallbacks(terminar);
        Sessao sessao = atual;
        if (sessao == null) return;
        atual = null;
        sessao.solta = true;
        MOTOR.execute(sessao::liberarNoMotor);
    }

    private void esconderTela() {
        tela.animate().cancel();
        tela.setAlpha(0f);
        tela.setVisibility(View.GONE);
    }

    private void abrirAposSoltar() {
        relogio.removeCallbacks(seguir);
        Runnable abrir = aposSoltar;
        aposSoltar = null;
        if (abrir != null) abrir.run();
    }

    /** O vídeo preenche a área e corta o que sobra, centrado, como a imagem embaixo dele. */
    private void enquadrar() {
        Sessao sessao = atual;
        int largura = tela.getWidth();
        int altura = tela.getHeight();
        if (sessao == null || sessao.videoLargura <= 0 || sessao.videoAltura <= 0 || largura == 0 || altura == 0) return;
        double proporcaoVideo = sessao.videoLargura * sessao.aspecto / sessao.videoAltura;
        double proporcaoTela = (double) largura / altura;
        float escalaX = proporcaoVideo > proporcaoTela ? (float) (proporcaoVideo / proporcaoTela) : 1f;
        float escalaY = proporcaoVideo > proporcaoTela ? 1f : (float) (proporcaoTela / proporcaoVideo);
        Matrix matriz = new Matrix();
        matriz.setScale(escalaX, escalaY, largura / 2f, altura / 2f);
        tela.setTransform(matriz);
    }

    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture textura, int largura, int altura) {
        if (atual != null && atual.superficie == null) abrir(atual, textura);
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture textura, int largura, int altura) {
        enquadrar();
    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture textura) {
        soltar();
        return true;
    }

    /** Primeiro quadro depois de dar o play: o vídeo aparece e os 15 s começam. */
    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture textura) {
        Sessao sessao = atual;
        if (sessao == null || !sessao.iniciada || sessao.visivel) return;
        sessao.visivel = true;
        relogio.removeCallbacks(desistir);
        relogio.postDelayed(terminar, TOCA_MS);
        if (Animacoes.ligadas(atividade)) tela.animate().alpha(1f).setDuration(TRANSICAO_MS).start();
        else tela.setAlpha(1f);
    }

    /**
     * Um vídeo da prévia, do abrir ao soltar. O player só é tocado na fila do motor; os avisos dele
     * chegam na thread da tela (o player é criado numa thread sem Looper).
     */
    private final class Sessao implements IMediaPlayer.OnPreparedListener, IMediaPlayer.OnCompletionListener,
            IMediaPlayer.OnErrorListener, IMediaPlayer.OnSeekCompleteListener,
            IMediaPlayer.OnVideoSizeChangedListener {

        private final String caminho;
        private final int inicioMs;
        @Nullable
        private IMediaPlayer player;
        private volatile boolean inicioAplicado;

        @Nullable
        Surface superficie;
        volatile boolean solta;
        boolean iniciada;
        boolean visivel;
        int videoLargura;
        int videoAltura;
        double aspecto = 1.0;

        Sessao(String caminho, int duracaoSegundos) {
            this.caminho = caminho;
            inicioMs = duracaoSegundos * 1000 / INICIO_DIVISOR;
        }

        void abrirNoMotor() {
            if (solta) return;
            try {
                IMediaPlayer novo = criarPlayer(aplicacao);
                player = novo;
                novo.setOnPreparedListener(this);
                novo.setOnCompletionListener(this);
                novo.setOnErrorListener(this);
                novo.setOnSeekCompleteListener(this);
                novo.setOnVideoSizeChangedListener(this);
                novo.setDataSource(aplicacao, Uri.fromFile(new File(caminho)));
                novo.setSurface(superficie);
                novo.setVolume(0f, 0f);
                inicioAplicado = inicioMs == 0 || novo.setStartTime(inicioMs);
                novo.prepareAsync();
            } catch (IOException | RuntimeException e) {
                relogio.post(this::falhar);
            }
        }

        /** Solta o player e depois a superfície. */
        void liberarNoMotor() {
            try {
                if (player != null) player.release();
            } catch (RuntimeException e) {
                // nada a fazer
            }
            player = null;
            if (superficie != null) superficie.release();
        }

        private void noMotor(Runnable comando) {
            MOTOR.execute(() -> {
                if (solta) return;
                try {
                    comando.run();
                } catch (RuntimeException e) {
                    relogio.post(this::falhar);
                }
            });
        }

        private void falhar() {
            if (this == atual) soltar();
        }

        private void tocar(IMediaPlayer mp) {
            iniciada = true;
            noMotor(mp::start);
        }

        @Override
        public void onPrepared(IMediaPlayer mp) {
            if (this != atual) return;
            if (inicioAplicado) tocar(mp);
            else noMotor(() -> mp.seekTo(inicioMs));
        }

        @Override
        public void onSeekComplete(IMediaPlayer mp) {
            if (this == atual && !iniciada) tocar(mp);
        }

        @Override
        public void onAllSeekComplete(IMediaPlayer mp) {
            // onSeekComplete já cuida
        }

        @Override
        public void onCompletion(IMediaPlayer mp) {
            if (this == atual) voltarAImagem();
        }

        @Override
        public boolean onError(IMediaPlayer mp, int errorCode, int errorQualCode, String msg) {
            falhar();
            return true;
        }

        @Override
        public void onVideoSizeChanged(IMediaPlayer mp, int width, int height) {
            if (this != atual) return;
            videoLargura = width;
            videoAltura = height;
            enquadrar();
        }

        @Override
        public void onVideoAspectChanged(IMediaPlayer mp, double aspect) {
            if (this != atual) return;
            aspecto = aspect;
            enquadrar();
        }
    }
}
