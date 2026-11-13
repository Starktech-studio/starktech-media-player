// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.archos.mediacenter.utils.MediaUtils;
import com.archos.mediacenter.video.R;
import com.archos.mediacenter.video.player.Player;
import com.archos.mediacenter.video.player.PlayerController;
import com.archos.mediacenter.video.player.SubtitleEngine;
import com.archos.mediacenter.video.player.SubtitleManager;
import com.archos.mediacenter.video.player.SurfaceController;
import com.archos.mediacenter.video.utils.VideoMetadata;
import com.archos.medialib.IMediaMetadataRetriever;
import com.archos.medialib.MediaFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import br.com.starktechstudio.mediaplayer.dados.Modelo.Capitulo;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Episodio;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Faixa;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Temporada;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Tipo;
import br.com.starktechstudio.mediaplayer.dados.Modelo.TipoFaixa;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Titulo;
import br.com.starktechstudio.mediaplayer.dados.RepositorioBiblioteca;
import br.com.starktechstudio.mediaplayer.dados.RepositorioReal;
import br.com.starktechstudio.mediaplayer.motor.Capitulos;
import br.com.starktechstudio.mediaplayer.motor.CapitulosDoArquivo;
import br.com.starktechstudio.mediaplayer.motor.Faixas;
import br.com.starktechstudio.mediaplayer.motor.LegendaExterna;
import br.com.starktechstudio.mediaplayer.motor.LegendasNoIndice;
import br.com.starktechstudio.mediaplayer.musica.Tocador;

/**
 * Reprodução de verdade (F3): o motor do Nova ({@link Player}, com o {@link SurfaceController} e as
 * legendas do motor) por baixo dos controles da F1. Abre o arquivo do título ou do episódio de onde
 * parou (ou no capítulo pedido), aplica as preferências de áudio e legenda, a forma da legenda
 * ({@link #aplicarEstiloDaLegenda}) e a escolha feita no Detalhe; pausar, pulo e capítulo anterior /
 * próximo (só entre capítulos, nunca para outro título — INTERFACE.md §4). Ao sair de vista, a
 * posição vai para a biblioteca, sem perder a que já estava lá ({@link #trocaPosicao}); no fim do
 * vídeo, o título fica assistido e, com "Próximo episódio automático" ligado, o episódio dá lugar ao
 * seguinte ({@link #definirAoPassarEpisodio}). O zoom guardado vale ao abrir, na TV e no tablet
 * ({@link #ZOOM_OPCOES}).
 */
public final class Reproducao implements Player.Listener {

    /** Avisado a cada meio segundo e a cada comando. */
    public interface Ouvinte {
        void aoMudar();
    }

    /** Fim de um episódio com "Próximo episódio automático" ligado: a tela abre o seguinte. */
    public interface AoPassarEpisodio {
        void passar(String idEpisodio);
    }

    /** Barra da TV: o passo ao apertar a seta. O pulo com os controles ocultos é o de {@link Preferencias#puloSegundos}. */
    public static final int PULO_SEGUNDOS = 10;
    /** Barra da TV: com a seta segurada, um passo na tela a cada tanto (o motor só busca ao soltar). */
    public static final long BARRA_INTERVALO_MS = 200;
    /** Barra da TV: seta segurada há tanto tempo, o passo sobe para 30 s e para 60 s. */
    private static final long BARRA_30_SEGUNDOS_MS = 2000;
    private static final long BARRA_60_SEGUNDOS_MS = 5000;
    /**
     * Opções de zoom. O número guardado em {@link Preferencias#zoomPlayer} é o formato de
     * {@link SurfaceController.VideoFormat}, de ORIGINAL a FORCE239, na ordem de {@code R.array.st_zoom_nomes}.
     * AUTO fica de fora: só existe em uma das listas do motor.
     */
    public static final int ZOOM_OPCOES = SurfaceController.VideoFormat.FORCE239 + 1;
    private static final long TIQUE_MS = 500;
    /** Quanto "Carregar legenda" espera o motor ler o arquivo. */
    private static final long ESPERA_LEGENDA_MS = 10_000;
    /** Quanto a abertura espera as legendas do índice (sem o acesso a todos os arquivos). */
    private static final long ESPERA_INDICE_MS = 1_500;
    /** Legenda em arquivo guardada pelo nome dele ({@link #codigoDaLegenda}). */
    private static final String LEGENDA_POR_NOME = "arquivo:";
    private static final String LOG = "StarkTechReproducao";
    // Forma da legenda (D8), na ordem dos valores de Preferencias.Item
    /** Tamanho, em pontos do motor; a média é a de fábrica ({@code player_pref_subtitle_size_default}). */
    static final float[] LEGENDA_TAMANHO_PT = {40f, 47f, 55f, 66f, 80f};
    private static final float LEGENDA_TAMANHO_MEDIO_PT = 55f;
    /** Famílias de {@code /system/fonts}, pelo fontconfig; aparelho sem a fonte: o libass usa a padrão. */
    static final String[] LEGENDA_FONTE = {"Roboto Medium", "Noto Serif", "Droid Sans Mono"};
    static final int[] LEGENDA_COR = {0xFFFFFFFF, 0xFFFFEB3B, 0xFF4DD0E1, 0xFF9CCC65};
    /** Contorno e sombra, em pixels numa tela de 720 de altura; médio e leve são os de fábrica. */
    static final float[] LEGENDA_CONTORNO = {0f, 1f, 2f, 4f};
    static final float[] LEGENDA_SOMBRA = {0f, 2f, 4f};
    static final int[] LEGENDA_FUNDO = {SubtitleManager.BG_MODE_FLOATING, SubtitleManager.BG_MODE_BOXED_LINE,
            SubtitleManager.BG_MODE_BOXED_BLOCK};
    /** Distância da base da imagem, em % da altura dela. */
    static final int[] LEGENDA_POSICAO_PORCENTO = {0, 6, 12};
    /** A tantos segundos do fim, o vídeo já conta como assistido. */
    private static final int FIM_SEGUNDOS = 5;
    /** Sem ida manual: quanto a sessão precisa andar para a posição guardada dar lugar a uma menor. */
    private static final int ANDOU_SEGUNDOS = 60;
    /** Legenda rápida (1.5): passo do atraso e o máximo para cada lado. */
    public static final int ATRASO_PASSO_MS = 100;
    static final int ATRASO_MAXIMO_MS = 600_000;

    /** Reprodução na tela (o painel de faixas manda a escolha para ela). */
    @Nullable
    private static Reproducao naTela;

    private final RepositorioBiblioteca repositorio;
    private final Context contexto;
    public final String id;
    /** Episódio: a série dele; filme ou show: null. */
    @Nullable
    public final String idSerie;
    public final String titulo;
    /** Episódio: "Temporada 1 · Episódio 3"; filme ou show: vazio. */
    public final String linhaEpisodio;
    /** Caminho do arquivo. */
    public final String caminho;
    /** Duração em segundos (da biblioteca; o player confirma ao abrir). */
    public int duracao;
    /**
     * Da biblioteca; vídeo ainda não lido pela varredura: lidos ao abrir ({@link #definirAoMudarCapitulos}).
     * Filme sem nenhum: as marcas de 10 em 10 minutos, só aqui no player ({@link #usarMarcas}).
     */
    public List<Capitulo> capitulos;
    /** Filme e desenho: sem capítulos, ganham as marcas. Episódio, show e stand-up, não. */
    private final boolean usaMarcas;

    private final Handler relogio = new Handler(Looper.getMainLooper());
    private Player player;
    private SurfaceController superficie;
    private Ouvinte ouvinte;
    private Runnable aoFalhar;
    private Runnable aoMudarCapitulos;
    @Nullable
    private AoPassarEpisodio aoPassarEpisodio;
    /** Altura da imagem na tela: a posição da legenda é relativa a ela. */
    private int alturaVideo;
    private boolean pronto;
    private boolean rodando;
    private boolean querTocando = true;
    private boolean terminou;
    private boolean preferenciasAplicadas;
    /** Já procurou os capítulos que faltavam na biblioteca (uma vez por abertura). */
    private boolean procurouCapitulos;
    /** Posição pedida antes de o player ficar pronto (segundos). */
    private int posicaoPedida;
    /** Onde esta sessão começou (segundos). */
    private final int inicioDaSessao;
    /** A posição que está na biblioteca (segundos). */
    private int posicaoGuardada;
    /** Pulo, capítulo, barra ou recomeço ({@link #recomecaDoZero}) nesta sessão: a posição nova vale, mesmo menor. */
    private boolean idaManual;
    /** Duração dada pelo motor ao abrir (segundos); 0 = não deu ({@link #duracao} é só estimativa). */
    private int duracaoDoMotor;
    @Nullable
    private VideoMetadata faixasDoArquivo;
    private int audioAtual = -1;
    private int legendaAtual = -1;
    /** "Carregar legenda": o arquivo à espera de o motor ler; {@link #legendasDeAntes}: as que já estavam na lista. */
    @Nullable
    private File legendaPedida;
    private Set<String> legendasDeAntes = new HashSet<>();
    /** Atraso da legenda deste título (ms; positivo = mais tarde), guardado nele. */
    private int atrasoLegendaMs;
    /** Vídeo de música (1.5.1). */
    private boolean clipe;

    /** O motor não virou faixa (arquivo que ele não entende): avisa e apaga a cópia. */
    private final Runnable desistirDaLegenda = new Runnable() {
        @Override
        public void run() {
            File pedida = legendaPedida;
            if (pedida == null) return;
            legendaPedida = null;
            if (pedida.getPath().startsWith(MediaUtils.getSubsDir(contexto).getPath())) pedida.delete();
            Toast.makeText(contexto, R.string.st_legenda_nao_carregada, Toast.LENGTH_LONG).show();
        }
    };

    private final Runnable tique = new Runnable() {
        @Override
        public void run() {
            if (!rodando) return;
            avisar();
            relogio.postDelayed(this, TIQUE_MS);
        }
    };

    private Reproducao(Context contexto, RepositorioBiblioteca repositorio, String id, @Nullable String idSerie, String titulo,
                       String linhaEpisodio, String caminho, int duracao, int inicio, int guardada, List<Capitulo> capitulos,
                       boolean usaMarcas) {
        this.contexto = contexto.getApplicationContext();
        this.repositorio = repositorio;
        this.id = id;
        this.idSerie = idSerie;
        this.titulo = titulo;
        this.linhaEpisodio = linhaEpisodio;
        this.caminho = caminho;
        this.duracao = Math.max(1, duracao);
        this.posicaoPedida = inicio;
        this.inicioDaSessao = inicio;
        this.posicaoGuardada = guardada;
        this.capitulos = capitulos;
        this.usaMarcas = usaMarcas;
    }

    /**
     * Abre o título ou o episódio {@code id}; {@code capitulo}: número (1…), {@link Navegacao#DE_ONDE_PAROU}
     * ou {@link Navegacao#DO_INICIO}. Null se o id não existir ou não tiver arquivo.
     */
    public static Reproducao abrir(Context contexto, String id, int capitulo, String formatoEpisodio) {
        RepositorioBiblioteca repositorio = RepositorioBiblioteca.atual(contexto);
        Titulo t = repositorio.titulo(id);
        String nome;
        String idSerie = null;
        String linha = "";
        String caminho;
        int duracao;
        int parou;
        if (t != null) {
            nome = t.titulo;
            caminho = t.caminho;
            duracao = t.duracaoSegundos;
            parou = t.posicaoSegundos;
        } else {
            Episodio e = repositorio.episodio(id);
            if (e == null) return null;
            nome = e.nomeSerie != null ? e.nomeSerie + " · " + e.nome : e.nome;
            idSerie = e.idSerie;
            linha = String.format(formatoEpisodio, e.temporada, e.numero);
            caminho = e.caminho;
            duracao = e.duracaoSegundos;
            parou = e.assistido ? 0 : e.posicaoSegundos;
        }
        if (caminho == null) return null;
        List<Capitulo> capitulos = repositorio.capitulos(id);
        int inicio;
        boolean retomar = Preferencias.indice(contexto, Preferencias.Item.RETOMAR) == 0;
        if (capitulo == Navegacao.DO_INICIO) inicio = 0;
        else if (capitulo >= 1 && capitulo <= capitulos.size()) inicio = capitulos.get(capitulo - 1).inicioSegundos;
        // A duração daqui pode ser a do TMDB ou nenhuma: quem confere o fim do vídeo é o onPrepared
        else inicio = retomar ? parou : 0;
        boolean usaMarcas = t != null && (t.tipo == Tipo.FILME || t.tipo == Tipo.DESENHO);
        // Vídeo de música (1.5.1): o artista embaixo do título e, no fim, o seguinte da sequência
        boolean clipe = t != null && t.tipo == Tipo.CLIPE;
        if (clipe && t.artista != null) linha = t.artista;
        Reproducao r = new Reproducao(contexto, repositorio, id, idSerie, nome, linha, caminho, duracao, inicio, parou, capitulos, usaMarcas);
        r.clipe = clipe;
        return r;
    }

    /** Vídeo da pasta de Músicas (1.5.1): Voltar volta para Músicas e o fim abre o seguinte. */
    public boolean ehClipe() {
        return clipe;
    }

    /** Vídeo de música (1.6): o seguinte e o anterior da sequência (na ordem sorteada, com o aleatório); nulo nas pontas. */
    @Nullable
    public String clipeSeguinte() {
        return clipe ? SequenciaClipes.seguinte(contexto, repositorio, id) : null;
    }

    @Nullable
    public String clipeAnterior() {
        return clipe ? SequenciaClipes.anterior(contexto, repositorio, id) : null;
    }

    /** O nome de outro título (o próximo vídeo, para "Próximo: …"). */
    @Nullable
    public String nomeDe(String outro) {
        Titulo t = repositorio.titulo(outro);
        return t == null ? null : t.titulo;
    }

    /** A reprodução na tela, se for do título {@code id}. */
    @Nullable
    public static Reproducao naTela(String id) {
        Reproducao r = naTela;
        return r != null && r.id.equals(id) ? r : null;
    }

    /**
     * Liga o motor às superfícies de {@code video} (as views {@code surface_view}, {@code gl_surface_view}
     * e {@code gl_subtitle_view} do Nova). {@code aoFalhar}: o arquivo não abriu (HD desligado,
     * arquivo estragado).
     */
    public void ligar(Activity tela, View video, Runnable aoFalhar) {
        this.aoFalhar = aoFalhar;
        superficie = new SurfaceController(video);
        superficie.setScreenSize(tela.getResources().getDisplayMetrics().widthPixels,
                tela.getResources().getDisplayMetrics().heightPixels);
        // Sem margem de recorte de câmera aqui: "Preencher" (STRETCH_XY) corta as bordas, em vez de
        // esticar a imagem e mostrar o aviso de recorte do Nova
        superficie.mFullScreenWithCutout = true;
        aplicarZoom(zoomValido(Preferencias.zoomPlayer(contexto)));
        alturaVideo = tela.getResources().getDisplayMetrics().heightPixels;
        video.addOnLayoutChangeListener((v, esq, topo, dir, base, esqAntes, topoAntes, dirAntes, baseAntes) -> {
            if (dir - esq > 0 && base - topo > 0 && (dir - esq != dirAntes - esqAntes || base - topo != baseAntes - topoAntes)) {
                superficie.setScreenSize(dir - esq, base - topo);
                alturaVideo = base - topo;
                aplicarPosicaoDaLegenda();
            }
        });
        // Música tocando (1.5) para quando o vídeo abre
        Tocador.pausarSeTocando();
        player = new Player(tela, tela.getWindow(), superficie, false);
        player.setListener(this);
        // Antes de abrir: a primeira legenda já sai na forma escolhida, com o atraso guardado do título
        aplicarEstiloDaLegenda();
        atrasoLegendaMs = repositorio.atrasoLegenda(id);
        if (atrasoLegendaMs != 0) player.setSubtitleDelay(atrasoLegendaMs);
        trazerLegendasDoIndice();
        player.setVideoURI(Uri.fromFile(new File(caminho)), null);
        // Depois de abrir: setVideoURI apaga a posição pedida antes (começava sempre do zero)
        if (posicaoPedida > 0) player.seekTo(posicaoPedida * 1000);
        naTela = this;
    }

    /**
     * Sem o acesso a todos os arquivos (TV sem a tela dele): as legendas do vídeo vêm do índice de mídia para a
     * pasta de legendas do app antes de abrir, e o motor as acha como sempre. Espera até
     * {@link #ESPERA_INDICE_MS}; se demorar mais, entram quando chegarem (procura de novo).
     */
    private void trazerLegendasDoIndice() {
        if (!LegendasNoIndice.precisa()) return;
        final File video = new File(caminho);
        final File pasta = MediaUtils.getSubsDir(contexto);
        // 0 = andando; 1 = chegou a tempo; 2 = a tela não esperou mais
        final AtomicInteger fase = new AtomicInteger();
        final CountDownLatch pronto = new CountDownLatch(1);
        new Thread(() -> {
            int copiadas = 0;
            try {
                copiadas = LegendasNoIndice.trazerDoVideo(contexto, video, pasta);
            } catch (RuntimeException e) {
                Log.w(LOG, "legendas do índice: " + e.getClass().getSimpleName());
            }
            pronto.countDown();
            if (!fase.compareAndSet(0, 1) && copiadas > 0) {
                relogio.post(() -> {
                    if (player != null) player.checkSubtitles();
                });
            }
        }, "StarkTechLegendasIndice").start();
        try {
            pronto.await(ESPERA_INDICE_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        fase.compareAndSet(0, 2);
    }

    /** Avisado quando os capítulos chegam depois de abrir (marcas, botões e lista). */
    public void definirAoMudarCapitulos(Runnable aoMudar) {
        this.aoMudarCapitulos = aoMudar;
    }

    /**
     * Vídeo sem capítulos na biblioteca (a varredura ainda não chegou nele, ou o leitor falhou): os
     * do player, ao abrir ({@link #onPrepared}); sem eles, o leitor do motor, fora da tela.
     */
    private void usarCapitulos(Capitulos achados) {
        if (player == null || achados == null || achados.vazio() || !capitulos.isEmpty()) return;
        capitulos = RepositorioReal.paraTela(achados);
        // Na biblioteca também: o Detalhe passa a mostrar da próxima vez
        repositorio.guardarCapitulos(id, achados);
        if (aoMudarCapitulos != null) aoMudarCapitulos.run();
        avisar();
    }

    /**
     * Filme sem capítulos em lugar nenhum: marcas de 10 em 10 minutos, com os mesmos botões e a mesma
     * lista dos capítulos. Valem só nesta abertura do player: a biblioteca e o Detalhe não ficam sabendo.
     */
    private void usarMarcas() {
        if (!usaMarcas || player == null || !capitulos.isEmpty() || duracaoDoMotor <= 0) return;
        Capitulos marcas = Capitulos.aCadaDezMinutos(duracaoDoMotor * 1000L);
        if (marcas.vazio()) return;
        capitulos = RepositorioReal.paraTela(marcas);
        if (aoMudarCapitulos != null) aoMudarCapitulos.run();
        avisar();
    }

    /** Capítulos pelo leitor do motor, fora da tela (quando o player não trouxe). */
    private void lerCapitulos() {
        final String arquivo = caminho;
        new Thread(() -> {
            Capitulos lidos = Capitulos.nenhum();
            IMediaMetadataRetriever leitor = MediaFactory.createMetadataRetriever(contexto);
            try {
                leitor.setDataSource(arquivo);
                lidos = Capitulos.doLeitor(leitor);
            } catch (Exception e) {
                // tenta direto do arquivo, abaixo
            } finally {
                try {
                    leitor.release();
                } catch (Exception ignorado) {
                    // nada a fazer
                }
            }
            // Reserva: direto do arquivo (MKV, MP4), sem o motor
            if (lidos.vazio()) lidos = CapitulosDoArquivo.ler(arquivo);
            if (lidos.vazio()) {
                relogio.post(this::usarMarcas);
                return;
            }
            final Capitulos achados = lidos;
            relogio.post(() -> usarCapitulos(achados));
        }, "StarkTechCapitulos").start();
    }

    public void definirOuvinte(Ouvinte ouvinte) {
        this.ouvinte = ouvinte;
    }

    /** Quem abre o episódio seguinte ({@link AoPassarEpisodio}). */
    public void definirAoPassarEpisodio(@Nullable AoPassarEpisodio aoPassar) {
        this.aoPassarEpisodio = aoPassar;
    }

    /** Mudou algo em Configurações com um vídeo aberto (a miniatura do tablet): legenda e zoom valem na hora. */
    public static void aoMudarConfiguracao() {
        Reproducao r = naTela;
        if (r == null) return;
        r.aplicarEstiloDaLegenda();
        r.aplicarZoom(zoomValido(Preferencias.zoomPlayer(r.contexto)));
    }

    /** Tela visível: toca (se não foi pausado) e liga o relógio. */
    public void iniciar() {
        if (rodando) return;
        rodando = true;
        if (pronto && querTocando && !terminou) player.start(PlayerController.STATE_NORMAL);
        relogio.post(tique);
    }

    /**
     * Tela perdeu a frente (Voltar, Áudio e Legendas, miniatura): só grava a posição, sem pausar. Ao
     * voltar, a tela de trás relê antes do {@link #parar} desta: precisa achar a posição já gravada.
     */
    public void gravarPosicao() {
        salvar();
    }

    /** Tela saiu de vista: pausa, desliga o relógio e grava a posição. */
    public void parar() {
        rodando = false;
        relogio.removeCallbacks(tique);
        if (player != null && pronto && player.isPlaying()) player.pause(PlayerController.STATE_OTHER);
        salvar();
    }

    /** Tela fechou: solta o motor. */
    public void liberar() {
        relogio.removeCallbacksAndMessages(null);
        if (naTela == this) naTela = null;
        if (player != null) {
            player.setListener(null);
            player.releasePlayer();
            if (Player.sPlayer == player) Player.sPlayer = null;
            player = null;
        }
    }

    /** Só com o player pronto: antes dele não há posição de verdade, e a guardada fica como está. */
    private void salvar() {
        if (player == null || !pronto) return;
        int pos = posicao();
        if (terminou || chegouAoFim(pos, duracaoDoMotor)) {
            repositorio.definirAssistido(id, true);
            repositorio.definirPosicao(id, 0);
            posicaoGuardada = 0;
        } else if (trocaPosicao(pos, posicaoGuardada, inicioDaSessao, idaManual)) {
            repositorio.definirPosicao(id, pos);
            posicaoGuardada = pos;
        }
    }

    /** {@code duracaoDoMotor} 0 = não se sabe: nunca é o fim. */
    static boolean chegouAoFim(int pos, int duracaoDoMotor) {
        return duracaoDoMotor > 0 && pos > 0 && pos >= duracaoDoMotor - FIM_SEGUNDOS;
    }

    /** Posição gravada com a duração errada, já no fim do vídeo: a sessão recomeça do zero, se ninguém pediu outro ponto. */
    static boolean recomecaDoZero(int pedida, int guardada, int duracaoDoMotor, boolean idaManual) {
        return !idaManual && pedida > 0 && pedida == guardada && chegouAoFim(pedida, duracaoDoMotor);
    }

    /**
     * A posição {@code pos} entra no lugar da {@code guardada}? Maior, sempre. Menor, só se a sessão
     * andou de verdade ou com ida manual: abrir do início e sair logo não apaga onde o vídeo parou.
     */
    static boolean trocaPosicao(int pos, int guardada, int inicioDaSessao, boolean idaManual) {
        if (pos == guardada) return false;
        return pos > guardada || idaManual || pos - inicioDaSessao >= ANDOU_SEGUNDOS;
    }

    public int posicao() {
        if (player == null || !pronto) return posicaoPedida;
        int atual = Math.max(0, player.getCurrentPosition() / 1000);
        return duracaoDoMotor > 0 ? Math.min(duracaoDoMotor, atual) : atual;
    }

    public boolean estaTocando() {
        return querTocando && !terminou;
    }

    /** O arquivo abriu no motor: o vídeo começa a tocar (ou já pode). */
    public boolean estaPronto() {
        return pronto;
    }

    /** Largura ÷ altura da imagem na tela; 16:9 enquanto o motor não souber. */
    public float proporcao() {
        if (player == null || player.getVideoWidth() <= 0 || player.getVideoHeight() <= 0) return 16f / 9f;
        return (float) (player.getVideoWidth() * player.getVideoAspect() / player.getVideoHeight());
    }

    public boolean temCapitulos() {
        return !capitulos.isEmpty();
    }

    public int progresso() {
        return (int) Math.min(100, Math.ceil(posicao() * 100.0 / duracao));
    }

    public void pausarOuContinuar() {
        if (terminou) {
            terminou = false;
            querTocando = true;
            irPara(0);
        } else {
            querTocando = !querTocando;
        }
        if (player != null && pronto) {
            if (querTocando) player.start(PlayerController.STATE_NORMAL);
            else player.pause(PlayerController.STATE_NORMAL);
        }
        avisar();
    }

    /** ±segundos, dentro do vídeo. */
    public void pular(int segundos) {
        irPara(posicao() + segundos);
    }

    public void irPara(int segundos) {
        int alvo = Math.max(0, duracaoDoMotor > 0 ? Math.min(duracaoDoMotor, segundos) : segundos);
        idaManual = true;
        posicaoPedida = alvo;
        if (alvo < duracao - 1) terminou = false;
        if (player != null) player.seekTo(alvo * 1000);
        avisar();
    }

    /** Passo da barra da TV, em segundos, com a seta segurada há {@code segurandoMs}: 10 s, depois 30 s e 60 s. */
    public static int passoDaBarra(long segurandoMs) {
        if (segurandoMs >= BARRA_60_SEGUNDOS_MS) return 60;
        if (segurandoMs >= BARRA_30_SEGUNDOS_MS) return 30;
        return PULO_SEGUNDOS;
    }

    /** Onde o ponto da barra da TV cai depois de um passo ({@code passo} negativo volta), dentro do vídeo. */
    public static int alvoDaBarra(int atual, int passo, int duracao) {
        return Math.max(0, Math.min(duracao, atual + passo));
    }

    /** Capítulo em que a posição está; null sem capítulos. */
    public Capitulo capituloAtual() {
        int pos = posicao();
        Capitulo atual = null;
        for (Capitulo c : capitulos) {
            if (c.inicioSegundos <= pos) atual = c;
        }
        return atual;
    }

    /** Próximo capítulo depois da posição; null no último ou sem capítulos. */
    public Capitulo proximoCapitulo() {
        int pos = posicao();
        for (Capitulo c : capitulos) {
            // Ir para um capítulo cai no quadro-chave de antes: 1 s de folga (senão repete o mesmo)
            if (c.inicioSegundos > pos + 1) return c;
        }
        return null;
    }

    /** Capítulo anterior: até 3 s do começo, o de antes; depois, o começo deste (D1). */
    public Capitulo irParaCapituloAnterior() {
        Capitulo atual = capituloAtual();
        if (atual == null) return null;
        Capitulo alvo = posicao() - atual.inicioSegundos <= 3 && atual.numero > 1 ? capitulos.get(atual.numero - 2) : atual;
        irPara(alvo.inicioSegundos);
        return alvo;
    }

    /** Próximo capítulo; no último não faz nada (nunca passa para outro título). */
    public Capitulo irParaProximoCapitulo() {
        Capitulo proximo = proximoCapitulo();
        if (proximo != null) irPara(proximo.inicioSegundos);
        return proximo;
    }

    public void irParaCapitulo(Capitulo capitulo) {
        irPara(capitulo.inicioSegundos);
    }

    // ---- Zoom ----

    /** Fora das opções (valor antigo ou estranho): Original. */
    static int zoomValido(int zoom) {
        return zoom >= 0 && zoom < ZOOM_OPCOES ? zoom : SurfaceController.VideoFormat.ORIGINAL;
    }

    static int proximoZoom(int zoom) {
        return (zoomValido(zoom) + 1) % ZOOM_OPCOES;
    }

    /** Próximo zoom (botão do player da TV): vale na hora e fica guardado. Devolve o novo. */
    public int trocarZoom() {
        int novo = proximoZoom(Preferencias.zoomPlayer(contexto));
        Preferencias.definirZoomPlayer(contexto, novo);
        aplicarZoom(novo);
        return novo;
    }

    /** Nas duas listas de formatos: qual delas vale depende do vídeo e da tela (SurfaceController.getVideoFormat). */
    private void aplicarZoom(int zoom) {
        if (superficie != null) superficie.setVideoFormat(zoom, zoom);
    }

    // ---- Faixas ----

    /** Faixas do arquivo lidas pelo player, com a atual marcada (legenda: "Desligadas" primeiro). */
    public List<Faixa> faixas(TipoFaixa tipo) {
        VideoMetadata meta = faixasDoArquivo;
        if (meta == null) return repositorio.faixas(id, tipo);
        List<Faixa> lista = new ArrayList<>();
        if (tipo == TipoFaixa.AUDIO) {
            for (int i = 0; i < meta.getAudioTrackNb(); i++) {
                lista.add(new Faixa("a" + i, tipo, Faixas.audio(meta.getAudioTrack(i), i), i == audioAtual));
            }
        } else {
            lista.add(new Faixa("l-1", tipo, "Desligadas", legendaAtual < 0));
            String video = new File(caminho).getName();
            for (int i = 0; i < meta.getSubtitleTrackNb(); i++) {
                VideoMetadata.SubtitleTrack faixa = meta.getSubtitleTrack(i);
                if (faixa != null && faixa.isExternal) {
                    if (LegendaExterna.deOutroVideo(video, faixa.path)) continue;
                    lista.add(new Faixa("l" + i, tipo, Faixas.legendaEmArquivo(video, faixa.path, faixa.name, i), i == legendaAtual));
                } else {
                    lista.add(new Faixa("l" + i, tipo, Faixas.legenda(faixa, i), i == legendaAtual));
                }
            }
        }
        return lista;
    }

    /**
     * "Carregar legenda": deixa o arquivo onde o motor procura, manda procurar de novo e liga a faixa
     * quando ela chegar ({@link #onSubtitleMetadataUpdated}).
     */
    public void carregarLegenda(File arquivo) {
        new Thread(() -> {
            File pronta = null;
            try {
                File pasta = MediaUtils.getSubsDir(contexto);
                // Sem o acesso a todos os arquivos, a cópia vem pelo índice de mídia (LegendasNoIndice)
                pronta = LegendasNoIndice.paraOMotor(contexto, new File(caminho), arquivo, pasta);
                MediaUtils.removeLastSubs(contexto);
            } catch (IOException | RuntimeException e) {
                Log.w(LOG, "não deu para preparar a legenda " + arquivo, e);
            }
            final File preparada = pronta;
            relogio.post(() -> aoPrepararLegenda(preparada));
        }, "StarkTechLegenda").start();
    }

    private void aoPrepararLegenda(@Nullable File pronta) {
        if (player == null) return;
        if (pronta == null) {
            Toast.makeText(contexto, R.string.st_legenda_nao_carregada, Toast.LENGTH_LONG).show();
            return;
        }
        legendaPedida = pronta;
        legendasDeAntes = new HashSet<>();
        VideoMetadata meta = faixasDoArquivo;
        for (int i = 0; meta != null && i < meta.getSubtitleTrackNb(); i++) {
            VideoMetadata.SubtitleTrack faixa = meta.getSubtitleTrack(i);
            if (faixa != null && faixa.isExternal) legendasDeAntes.add(faixa.path);
        }
        if (ligarLegendaPedida()) return;
        player.checkSubtitles();
        relogio.removeCallbacks(desistirDaLegenda);
        relogio.postDelayed(desistirDaLegenda, ESPERA_LEGENDA_MS);
    }

    /** Liga a faixa do arquivo pedido, se já chegou. Legenda em UTF-16 vem com outro caminho (um temporário): vale a faixa nova. */
    private boolean ligarLegendaPedida() {
        VideoMetadata meta = faixasDoArquivo;
        if (legendaPedida == null || meta == null || player == null) return false;
        int nova = -1;
        for (int i = 0; i < meta.getSubtitleTrackNb(); i++) {
            VideoMetadata.SubtitleTrack faixa = meta.getSubtitleTrack(i);
            if (faixa == null || !faixa.isExternal) continue;
            if (legendaPedida.getPath().equals(faixa.path)) {
                nova = i;
                break;
            }
            if (nova < 0 && !legendasDeAntes.contains(faixa.path)) nova = i;
        }
        if (nova < 0) return false;
        legendaPedida = null;
        relogio.removeCallbacks(desistirDaLegenda);
        if (player.setSubtitleTrack(nova)) {
            legendaAtual = nova;
            repositorio.guardarFaixa(id, idSerie, TipoFaixa.LEGENDA, codigoDaLegenda(nova));
        }
        avisar();
        return true;
    }

    /** Escolha no painel de faixas: vale na hora e fica guardada para a próxima vez. */
    public void escolherFaixa(Faixa faixa) {
        if (player == null) return;
        int indice = indiceDe(faixa.id);
        if (faixa.tipo == TipoFaixa.AUDIO) {
            if (indice >= 0 && player.setAudioTrack(indice)) {
                audioAtual = indice;
                repositorio.guardarFaixa(id, idSerie, TipoFaixa.AUDIO, faixa.id);
            }
        } else if (player.setSubtitleTrack(indice)) {
            legendaAtual = indice;
            repositorio.guardarFaixa(id, idSerie, TipoFaixa.LEGENDA, codigoDaLegenda(indice));
        }
        avisar();
    }

    /** Legenda em arquivo vai pelo nome (a posição na lista muda se entrar outro arquivo); as outras, pela posição. */
    private String codigoDaLegenda(int indice) {
        VideoMetadata meta = faixasDoArquivo;
        VideoMetadata.SubtitleTrack faixa = meta == null || indice < 0 || indice >= meta.getSubtitleTrackNb() ? null : meta.getSubtitleTrack(indice);
        if (faixa != null && faixa.isExternal && LegendaExterna.doVideo(new File(caminho).getName(), faixa.path)) {
            return LEGENDA_POR_NOME + new File(faixa.path).getName();
        }
        return "l" + indice;
    }

    /** A legenda guardada neste arquivo (-1 = desligada); nula = nada guardado, ou ela não existe mais aqui. */
    @Nullable
    private Integer legendaGuardada(VideoMetadata meta) {
        String codigo = repositorio.faixaEscolhida(id, idSerie, TipoFaixa.LEGENDA);
        if (codigo == null) return null;
        if (!codigo.startsWith(LEGENDA_POR_NOME)) {
            int indice = indiceDe(codigo);
            return indice < meta.getSubtitleTrackNb() ? Integer.valueOf(indice) : null;
        }
        String nome = codigo.substring(LEGENDA_POR_NOME.length());
        for (int i = 0; i < meta.getSubtitleTrackNb(); i++) {
            VideoMetadata.SubtitleTrack faixa = meta.getSubtitleTrack(i);
            if (faixa != null && faixa.isExternal && nome.equals(new File(faixa.path).getName())) return i;
        }
        return null;
    }

    private static int indiceDe(String idFaixa) {
        try {
            return Integer.parseInt(idFaixa.substring(1));
        } catch (RuntimeException e) {
            return -1;
        }
    }

    /**
     * Uma vez, com as faixas conhecidas: a escolha guardada vale (no Detalhe ou no player, deste vídeo
     * ou da série dele); sem ela, as preferências (áudio original / português / inglês; legenda
     * português / inglês / desligada).
     */
    private void aplicarPreferencias() {
        VideoMetadata meta = faixasDoArquivo;
        if (preferenciasAplicadas || meta == null || player == null) return;
        preferenciasAplicadas = true;

        String audio = repositorio.faixaEscolhida(id, idSerie, TipoFaixa.AUDIO);
        int audioGuardado = audio == null ? -1 : indiceDe(audio);
        int audioIdioma = Preferencias.indice(contexto, Preferencias.Item.AUDIO);
        int alvoAudio = audioGuardado >= 0 && audioGuardado < meta.getAudioTrackNb() ? audioGuardado
                : audioIdioma == 0 ? -1 : faixaDoIdioma(meta, true, audioIdioma == 1);
        if (alvoAudio >= 0 && alvoAudio != audioAtual && player.setAudioTrack(alvoAudio)) audioAtual = alvoAudio;

        Integer legendaGuardada = legendaGuardada(meta);
        int legendaIdioma = Preferencias.indice(contexto, Preferencias.Item.LEGENDA);
        int alvoLegenda = legendaGuardada != null ? legendaGuardada
                : legendaIdioma == 2 ? -1 : legendaAoAbrir(meta, legendaIdioma == 0);
        if (alvoLegenda != legendaAtual && player.setSubtitleTrack(alvoLegenda)) legendaAtual = alvoLegenda;
    }

    /**
     * Com legenda ligada nas preferências: o arquivo com o nome do vídeo vem primeiro, no idioma
     * preferido ou sem idioma no nome ("Filme.srt"); sem ele, a faixa do idioma dentro do vídeo.
     */
    private int legendaAoAbrir(VideoMetadata meta, boolean portugues) {
        String video = new File(caminho).getName();
        int semIdioma = -1;
        for (int i = 0; i < meta.getSubtitleTrackNb(); i++) {
            VideoMetadata.SubtitleTrack faixa = meta.getSubtitleTrack(i);
            if (faixa == null || !faixa.isExternal || LegendaExterna.deOutroVideo(video, faixa.path)) continue;
            String idioma = LegendaExterna.idioma(video, faixa.path);
            if (portugues ? Faixas.ehPortugues(idioma) : Faixas.ehIngles(idioma)) return i;
            if (idioma.isEmpty() && semIdioma < 0) semIdioma = i;
        }
        return semIdioma >= 0 ? semIdioma : faixaDoIdioma(meta, false, portugues);
    }

    // ---- Forma da legenda (D8) ----

    /**
     * Ao abrir e na hora ({@link #aoMudarConfiguracao}). O motor sempre impõe tudo à legenda de texto
     * (SRT). ASS tem estilo próprio (sinais, karaokê, posições): em SCALE_ONLY fica como o arquivo
     * trouxe e só segue o tamanho, pela escala. Legenda em imagem (PGS, VobSub) não muda.
     */
    private void aplicarEstiloDaLegenda() {
        SubtitleEngine legendas = player == null ? null : player.getSubtitleEngine();
        if (legendas == null) return;
        int tamanho = Preferencias.indice(contexto, Preferencias.Item.TAMANHO_LEGENDA);
        legendas.setOverrideMode(SubtitleManager.OVERRIDE_SCALE_ONLY);
        legendas.setFontSize(LEGENDA_TAMANHO_PT[tamanho]);
        legendas.setFontScale(escalaDaLegenda(tamanho));
        legendas.setFontFamily(LEGENDA_FONTE[Preferencias.indice(contexto, Preferencias.Item.FONTE_LEGENDA)]);
        legendas.setBold(Preferencias.indice(contexto, Preferencias.Item.NEGRITO_LEGENDA) == 1);
        legendas.setTextColor(LEGENDA_COR[Preferencias.indice(contexto, Preferencias.Item.COR_LEGENDA)]);
        legendas.setOutlineWidth(LEGENDA_CONTORNO[Preferencias.indice(contexto, Preferencias.Item.CONTORNO_LEGENDA)]);
        legendas.setShadowWidth(LEGENDA_SOMBRA[Preferencias.indice(contexto, Preferencias.Item.SOMBRA_LEGENDA)]);
        legendas.setBackgroundMode(LEGENDA_FUNDO[Preferencias.indice(contexto, Preferencias.Item.FUNDO_LEGENDA)]);
        aplicarPosicaoDaLegenda();
    }

    private void aplicarPosicaoDaLegenda() {
        SubtitleEngine legendas = player == null ? null : player.getSubtitleEngine();
        if (legendas == null) return;
        int posicao = Preferencias.indice(contexto, Preferencias.Item.POSICAO_LEGENDA);
        legendas.setVerticalOffset(distanciaDaBase(posicao, alturaVideo));
    }

    // ---- Legenda rápida (1.5): atraso por título e tamanho, no painel Ajustar ----

    /** O vídeo tem alguma legenda (dentro dele ou em arquivo): o botão Ajustar aparece. */
    public boolean temLegenda() {
        VideoMetadata meta = faixasDoArquivo;
        return meta != null && meta.getSubtitleTrackNb() > 0;
    }

    public int atrasoLegendaMs() {
        return atrasoLegendaMs;
    }

    /** Soma {@code passoMs} ao atraso (negativo adianta), dentro de ±{@link #ATRASO_MAXIMO_MS}; vale na hora e fica no título. */
    public void ajustarAtrasoLegenda(int passoMs) {
        definirAtrasoLegenda(atrasoLegendaMs + passoMs);
    }

    public void definirAtrasoLegenda(int ms) {
        atrasoLegendaMs = limitarAtraso(ms);
        if (player != null) player.setSubtitleDelay(atrasoLegendaMs);
        repositorio.guardarAtrasoLegenda(id, atrasoLegendaMs);
        avisar();
    }

    static int limitarAtraso(int ms) {
        return Math.max(-ATRASO_MAXIMO_MS, Math.min(ATRASO_MAXIMO_MS, ms));
    }

    /** Tamanho da legenda (o de Configurações, para todos os vídeos), um nível acima ou abaixo. */
    public void ajustarTamanhoLegenda(int passo) {
        int atual = Preferencias.indice(contexto, Preferencias.Item.TAMANHO_LEGENDA);
        Preferencias.escolher(contexto, Preferencias.Item.TAMANHO_LEGENDA, atual + passo);
        aplicarEstiloDaLegenda();
        avisar();
    }

    /** Escala do tamanho na legenda ASS: o mesmo aumento da legenda de texto. */
    static float escalaDaLegenda(int tamanho) {
        return LEGENDA_TAMANHO_PT[tamanho] / LEGENDA_TAMANHO_MEDIO_PT;
    }

    static int distanciaDaBase(int posicao, int alturaVideo) {
        return alturaVideo * LEGENDA_POSICAO_PORCENTO[posicao] / 100;
    }

    /** Primeira faixa (de áudio ou de legenda) em português ou em inglês; -1 se não há. */
    private static int faixaDoIdioma(VideoMetadata meta, boolean audio, boolean portugues) {
        int total = audio ? meta.getAudioTrackNb() : meta.getSubtitleTrackNb();
        for (int i = 0; i < total; i++) {
            String idioma = audio ? meta.getAudioTrack(i).language : meta.getSubtitleTrack(i).language;
            if (portugues ? Faixas.ehPortugues(idioma) : Faixas.ehIngles(idioma)) return i;
        }
        return -1;
    }

    private void avisar() {
        if (ouvinte != null) ouvinte.aoMudar();
    }

    // ---- Player.Listener ----

    @Override
    public void onPrepared() {
        pronto = true;
        int duracaoMs = player.getDuration();
        int segundos = duracaoMs / 1000;
        if (segundos > 0) {
            duracao = segundos;
            duracaoDoMotor = segundos;
            repositorio.guardarDuracao(id, duracaoMs);
            // Conta como ida manual: senão nada é gravado até passar do ponto antigo, e o próximo Continuar recomeça de novo
            if (recomecaDoZero(posicaoPedida, posicaoGuardada, segundos, idaManual)) {
                idaManual = true;
                posicaoPedida = 0;
                player.seekTo(0);
            }
        }
        if (capitulos.isEmpty() && !procurouCapitulos) {
            procurouCapitulos = true;
            Capitulos doPlayer;
            try {
                doPlayer = Capitulos.doPlayer(player.getMediaMetadata());
            } catch (RuntimeException e) {
                doPlayer = Capitulos.nenhum();
            }
            if (doPlayer.vazio()) lerCapitulos();
            else usarCapitulos(doPlayer);
        }
        aplicarPreferencias();
        // O atraso vale depois de aberto (antes, o motor só guarda para a próxima abertura)
        if (atrasoLegendaMs != 0) player.setSubtitleDelay(atrasoLegendaMs);
        if (rodando && querTocando && !terminou) player.start(PlayerController.STATE_NORMAL);
        avisar();
    }

    @Override
    public void onCompletion() {
        terminou = true;
        querTocando = false;
        salvar();
        avisar();
        String seguinte = episodioAutomatico();
        if (seguinte != null && aoPassarEpisodio != null) aoPassarEpisodio.passar(seguinte);
    }

    /**
     * O episódio que vem depois deste, com "Próximo episódio automático" ligado; o vídeo de música seguinte
     * (1.5.1), sempre (D3 do plano 006); senão, null.
     */
    @Nullable
    private String episodioAutomatico() {
        if (clipe) return SequenciaClipes.seguinte(contexto, repositorio, id);
        if (idSerie == null || !Preferencias.proximoEpisodioAutomatico(contexto)) return null;
        Episodio seguinte = episodioSeguinte(repositorio.temporadas(idSerie), id);
        return seguinte == null ? null : seguinte.id;
    }

    /** O que vem depois de {@code id}, de temporada em temporada, pulando quem está sem arquivo; null no último. */
    @Nullable
    static Episodio episodioSeguinte(List<Temporada> temporadas, String id) {
        boolean passou = false;
        for (Temporada temporada : temporadas) {
            for (Episodio e : temporada.episodios) {
                if (passou && e.caminho != null) return e;
                if (id.equals(e.id)) passou = true;
            }
        }
        return null;
    }

    @Override
    public boolean onError(int errorCode, int errorQualCode, String msg) {
        if (aoFalhar != null) aoFalhar.run();
        return true;
    }

    @Override
    public void onSeekStart(int pos) {
        avisar();
    }

    @Override
    public void onSeekComplete() {
        avisar();
    }

    @Override
    public void onAllSeekComplete() {
        avisar();
    }

    @Override
    public void onPlay(int state) {
        avisar();
    }

    @Override
    public void onPause(int state) {
        avisar();
    }

    @Override
    public void onOSDUpdate() {
        // os controles são os da StarkTech
    }

    @Override
    public void onVideoMetadataUpdated(VideoMetadata vMetadata) {
        faixasDoArquivo = vMetadata;
    }

    @Override
    public void onAudioMetadataUpdated(VideoMetadata vMetadata, int currentAudio) {
        faixasDoArquivo = vMetadata;
        audioAtual = currentAudio;
    }

    @Override
    public void onSubtitleMetadataUpdated(VideoMetadata vMetadata, int currentSubtitle) {
        faixasDoArquivo = vMetadata;
        legendaAtual = currentSubtitle;
        ligarLegendaPedida();
    }

    @Override
    public void onBufferingUpdate(int percent) {
        // arquivo local
    }
}
