// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.archos.medialib.IMediaMetadataRetriever;
import com.archos.medialib.MediaFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Quadro do começo de cada capítulo (2.0.1): a prévia dos cartões de capítulo de filmes, desenhos e shows, no
 * Detalhe e no player (TV e tablet), no lugar do número. Feito fora da tela, numa fila só e com a prioridade baixa,
 * um vídeo por vez e com o arquivo aberto uma vez para todos os capítulos dele — o motor do app, como o quadro da
 * miniatura ({@link Varredura}); no arquivo do aparelho, a reserva do Android. Cada quadro pronto avisa a tela na
 * hora. Guardado no cache do app (pasta {@code capitulos}, JPG de até 480 px), pelo caminho do vídeo e pelo começo
 * do capítulo: "Limpar o cache" apaga, e o Android também pode apagar quando faltar espaço. O ponto do quadro é um
 * pouco depois do começo ({@link #momentoMs}), para fugir da tela preta da troca de capítulo. Vídeo que não deu
 * nenhum quadro não é tentado de novo até o app abrir outra vez.
 * <p>
 * 2.1.2: quadro estragado (faixas verdes de vídeo de TV gravado, cujo ponto de busca não traz a imagem inteira) não
 * fica: tenta a reserva do Android e mais dois pontos à frente ({@link #momentosMs}); sem nenhum bom, fica o número.
 * Os quadros da 2.0.1 a 2.1.1 (pasta {@code capitulos}, alguns estragados) são apagados e feitos de novo.
 */
public final class QuadrosDeCapitulo {

    private static final String LOG = "StarkTechQuadros";
    private static final String PASTA = "capitulos2";
    /** Pasta da 2.0.1 a 2.1.1: apagada na primeira vez (podia ter quadros estragados). */
    private static final String PASTA_ANTIGA = "capitulos";
    /** Pontos a mais, depois do primeiro, quando o quadro sai estragado (ms). */
    static final long[] OUTROS_PONTOS_MS = {3000, 8000};
    /** Linha verde: pelo menos esta fração dos pontos dela. Quadro estragado: pelo menos esta fração das linhas. */
    static final float PONTOS_VERDES_NA_LINHA = 0.6f;
    static final float LINHAS_VERDES_NO_QUADRO = 0.05f;
    private static final int LARGURA = 480;
    /** Quanto depois do começo do capítulo fica o quadro, no máximo (capítulo curto: um quarto dele). */
    static final long DEPOIS_DO_INICIO_MS = 5000;
    /** Arquivo que trava o motor não segura a fila: passado o limite, segue para o próximo pedido. */
    private static final long LIMITE_POR_VIDEO_MS = 90 * 1000L;

    /** Um quadro novo (na thread da tela). */
    public interface Aviso {
        void chegou(int inicioSegundos, String arquivo);
    }

    /** Pedido de uma tela: {@link #cancelar} ao sair dela (o quadro em andamento termina e fica guardado). */
    public static final class Pedido {
        private volatile boolean cancelado;

        public void cancelar() {
            cancelado = true;
        }
    }

    private static final ExecutorService FILA = Executors.newSingleThreadExecutor(r -> {
        Thread linha = new Thread(r, "StarkTechQuadrosCapitulo");
        linha.setPriority(Thread.MIN_PRIORITY);
        return linha;
    });
    /** A thread da tela, criada no primeiro aviso (o teste da JVM carrega a classe sem o Android). */
    private static volatile Handler tela;
    /** Vídeos que não deram nenhum quadro nesta abertura do app (formato que o motor não lê, arquivo estragado). */
    private static final Set<String> SEM_QUADRO = ConcurrentHashMap.newKeySet();

    private QuadrosDeCapitulo() {}

    /**
     * Onde fica o quadro do capítulo que começa em {@code inicioSegundos}. Pode ainda não existir: a tela carrega a
     * imagem fora dela e, sem o arquivo, fica o número (nada é lido do disco aqui).
     */
    public static String arquivoDo(Context contexto, String caminho, int inicioSegundos) {
        return new File(pasta(contexto), nome(caminho, inicioSegundos)).getAbsolutePath();
    }

    /** Nome do arquivo do quadro: o vídeo (código e tamanho do caminho) e o começo do capítulo, em segundos. */
    static String nome(String caminho, int inicioSegundos) {
        return String.format(Locale.ROOT, "%08x_%d_%d.jpg", caminho.hashCode(), caminho.length(), Math.max(0, inicioSegundos));
    }

    /**
     * O ponto do quadro, em ms: {@link #DEPOIS_DO_INICIO_MS} depois do começo, sem passar de um quarto do capítulo.
     * {@code fimSegundos}: o começo do seguinte ou, no último, o fim do vídeo; 0 ou antes do começo = não se sabe.
     */
    static long momentoMs(int inicioSegundos, int fimSegundos) {
        int inicio = Math.max(0, inicioSegundos);
        long depois = DEPOIS_DO_INICIO_MS;
        if (fimSegundos > inicio) depois = Math.min(depois, (fimSegundos - inicio) * 1000L / 4);
        return inicio * 1000L + depois;
    }

    /**
     * Os pontos a tentar, em ordem (2.1.2): {@link #momentoMs} e, se o quadro sair estragado, {@link #OUTROS_PONTOS_MS}
     * depois dele, sem passar do fim do capítulo (quando se sabe) e sem repetir.
     */
    static long[] momentosMs(int inicioSegundos, int fimSegundos) {
        long primeiro = momentoMs(inicioSegundos, fimSegundos);
        long limite = fimSegundos > Math.max(0, inicioSegundos) ? fimSegundos * 1000L - 500 : Long.MAX_VALUE;
        long[] pontos = new long[1 + OUTROS_PONTOS_MS.length];
        int n = 0;
        pontos[n++] = primeiro;
        for (long mais : OUTROS_PONTOS_MS) {
            long ponto = primeiro + mais;
            if (ponto <= limite && ponto > pontos[n - 1]) pontos[n++] = ponto;
        }
        long[] saida = new long[n];
        System.arraycopy(pontos, 0, saida, 0, n);
        return saida;
    }

    /**
     * Quadro estragado (2.1.2): faixas de verde puro, o que o decodificador deixa onde faltou a imagem de referência.
     * {@code pontos}: ARGB, {@code largura} × {@code altura} (uma amostra do quadro basta). Linha verde: pelo menos
     * {@link #PONTOS_VERDES_NA_LINHA} dos pontos; estragado: pelo menos {@link #LINHAS_VERDES_NO_QUADRO} das linhas.
     */
    static boolean comFaixasVerdes(int[] pontos, int largura, int altura) {
        if (pontos == null || largura <= 0 || altura <= 0 || pontos.length < largura * altura) return false;
        int linhasVerdes = 0;
        for (int y = 0; y < altura; y++) {
            int verdes = 0;
            for (int x = 0; x < largura; x++) {
                if (verde(pontos[y * largura + x])) verdes++;
            }
            if (verdes >= largura * PONTOS_VERDES_NA_LINHA) linhasVerdes++;
        }
        return linhasVerdes >= Math.max(1, altura * LINHAS_VERDES_NO_QUADRO);
    }

    /** Verde de quadro estragado: o verde alto, o vermelho e o azul quase zero. */
    private static boolean verde(int argb) {
        int r = (argb >> 16) & 0xff;
        int g = (argb >> 8) & 0xff;
        int b = argb & 0xff;
        return g >= 96 && r <= 48 && b <= 48 && g > 2 * Math.max(r, b);
    }

    /** {@link #comFaixasVerdes} numa amostra do bitmap (até 64 × 64 pontos). */
    private static boolean estragado(Bitmap quadro) {
        int largura = Math.min(64, quadro.getWidth());
        int altura = Math.min(64, quadro.getHeight());
        if (largura <= 0 || altura <= 0) return true;
        Bitmap amostra = Bitmap.createScaledBitmap(quadro, largura, altura, false);
        int[] pontos = new int[largura * altura];
        amostra.getPixels(pontos, 0, largura, 0, 0, largura, altura);
        if (amostra != quadro) amostra.recycle();
        return comFaixasVerdes(pontos, largura, altura);
    }

    /**
     * Pede os quadros que faltam dos capítulos do vídeo {@code caminho} ({@code inicios}: o começo de cada um, em
     * ordem; {@code duracaoSegundos} 0 = não se sabe). {@code aviso} roda na tela a cada quadro novo, até o pedido
     * ser cancelado. Os que já existem não são feitos de novo.
     */
    public static Pedido pedir(Context contexto, String caminho, int[] inicios, int duracaoSegundos, Aviso aviso) {
        Pedido pedido = new Pedido();
        if (caminho == null || inicios == null || inicios.length == 0 || SEM_QUADRO.contains(caminho)) return pedido;
        Context app = contexto.getApplicationContext();
        int[] comecos = inicios.clone();
        FILA.execute(() -> {
            if (pedido.cancelado) return;
            Thread linha = new Thread(() -> {
                try {
                    fazer(app, caminho, comecos, duracaoSegundos, pedido, aviso);
                } catch (RuntimeException e) {
                    Log.w(LOG, "sem quadros dos capítulos de " + caminho, e);
                }
            }, "StarkTechQuadroCapitulo");
            linha.setDaemon(true);
            linha.setPriority(Thread.MIN_PRIORITY);
            linha.start();
            try {
                linha.join(LIMITE_POR_VIDEO_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (linha.isAlive()) Log.w(LOG, "quadros travados, seguindo: " + caminho);
        });
        return pedido;
    }

    private static void fazer(Context app, String caminho, int[] inicios, int duracao, Pedido pedido, Aviso aviso) {
        apagarPastaAntiga(app);
        File pasta = pasta(app);
        if (!pasta.isDirectory() && !pasta.mkdirs()) return;
        List<Integer> faltam = new ArrayList<>();
        for (int i = 0; i < inicios.length; i++) {
            if (!new File(pasta, nome(caminho, inicios[i])).isFile()) faltam.add(i);
        }
        if (faltam.isEmpty()) return;
        ArquivosDaBiblioteca arquivos = ArquivosDaBiblioteca.de(app);
        // HD fora ou servidor longe: tenta de novo na próxima vez que a tela pedir
        String endereco = arquivos.disponivel(caminho) ? arquivos.endereco(caminho) : null;
        if (endereco == null) return;
        Leitor leitor = new Leitor(app, endereco, ArquivosDaBiblioteca.local(caminho));
        int feitos = 0;
        try {
            for (int i : faltam) {
                if (pedido.cancelado) return;
                int fim = i + 1 < inicios.length ? inicios[i + 1] : duracao;
                Bitmap quadro = null;
                for (long ponto : momentosMs(inicios[i], fim)) {
                    if (pedido.cancelado) return;
                    quadro = leitor.quadro(ponto * 1000L);
                    if (quadro != null) break;
                }
                if (quadro == null) continue;
                File destino = new File(pasta, nome(caminho, inicios[i]));
                if (!gravar(quadro, destino)) continue;
                feitos++;
                final int inicio = inicios[i];
                final String gravado = destino.getAbsolutePath();
                tela().post(() -> {
                    if (!pedido.cancelado) aviso.chegou(inicio, gravado);
                });
            }
            if (feitos == 0) SEM_QUADRO.add(caminho);
        } finally {
            leitor.fechar();
        }
    }

    private static Handler tela() {
        Handler h = tela;
        if (h == null) {
            synchronized (QuadrosDeCapitulo.class) {
                if (tela == null) tela = new Handler(Looper.getMainLooper());
                h = tela;
            }
        }
        return h;
    }

    private static File pasta(Context contexto) {
        return new File(contexto.getCacheDir(), PASTA);
    }

    private static volatile boolean pastaAntigaApagada;

    /** Só os JPG do cache da 2.0.1 a 2.1.1 (refeitos na pasta nova, sem os estragados). */
    private static void apagarPastaAntiga(Context contexto) {
        if (pastaAntigaApagada) return;
        pastaAntigaApagada = true;
        File antiga = new File(contexto.getCacheDir(), PASTA_ANTIGA);
        File[] arquivos = antiga.listFiles();
        if (arquivos != null) {
            for (File arquivo : arquivos) {
                //noinspection ResultOfMethodCallIgnored
                arquivo.delete();
            }
        }
        //noinspection ResultOfMethodCallIgnored
        antiga.delete();
    }

    /** JPG de até {@link #LARGURA} px, por arquivo temporário (a tela nunca lê um quadro pela metade). */
    private static boolean gravar(Bitmap quadro, File destino) {
        Bitmap menor = quadro;
        if (quadro.getWidth() > LARGURA) {
            int altura = Math.max(1, quadro.getHeight() * LARGURA / quadro.getWidth());
            menor = Bitmap.createScaledBitmap(quadro, LARGURA, altura, true);
        }
        File temporario = new File(destino.getPath() + ".tmp");
        try (OutputStream saida = new FileOutputStream(temporario)) {
            if (!menor.compress(Bitmap.CompressFormat.JPEG, 82, saida)) return false;
        } catch (IOException e) {
            return false;
        } finally {
            if (menor != quadro) menor.recycle();
            quadro.recycle();
        }
        if (temporario.renameTo(destino)) return true;
        //noinspection ResultOfMethodCallIgnored
        temporario.delete();
        return false;
    }

    /** O motor do app e, no arquivo do aparelho, a reserva do Android, cada um aberto uma vez só. */
    private static final class Leitor {
        private final Context contexto;
        private final String endereco;
        private final boolean local;
        private IMediaMetadataRetriever motor;
        private boolean motorAberto;
        private boolean motorFalhou;
        private MediaMetadataRetriever android;
        private boolean androidAberto;
        private boolean androidFalhou;

        Leitor(Context contexto, String endereco, boolean local) {
            this.contexto = contexto;
            this.endereco = endereco;
            this.local = local;
        }

        /** Um quadro bom ou nulo: o estragado ({@link #estragado}) é descartado e a reserva tenta o mesmo ponto. */
        Bitmap quadro(long momentoUs) {
            Bitmap quadro = bom(doMotor(momentoUs));
            // A reserva do Android lê só o arquivo do aparelho; no servidor, só o motor
            if (quadro == null && local) quadro = bom(doAndroid(momentoUs));
            return quadro;
        }

        @Nullable
        private static Bitmap bom(@Nullable Bitmap quadro) {
            if (quadro == null || !estragado(quadro)) return quadro;
            quadro.recycle();
            return null;
        }

        private Bitmap doMotor(long momentoUs) {
            if (motorFalhou) return null;
            try {
                if (!motorAberto) {
                    if (motor == null) motor = MediaFactory.createMetadataRetriever(contexto);
                    motor.setDataSource(endereco);
                    motorAberto = true;
                }
                return motor.getFrameAtTime(momentoUs);
            } catch (Exception e) {
                Log.w(LOG, "sem quadro do motor em " + endereco, e);
                // Não abriu: os outros capítulos vão direto à reserva; um quadro que falhou não fecha o arquivo
                if (!motorAberto) motorFalhou = true;
                return null;
            }
        }

        private Bitmap doAndroid(long momentoUs) {
            if (androidFalhou) return null;
            try {
                if (!androidAberto) {
                    if (android == null) android = new MediaMetadataRetriever();
                    android.setDataSource(endereco);
                    androidAberto = true;
                }
                return android.getFrameAtTime(momentoUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            } catch (RuntimeException e) {
                Log.w(LOG, "sem quadro do Android em " + endereco, e);
                if (!androidAberto) androidFalhou = true;
                return null;
            }
        }

        void fechar() {
            try {
                if (motor != null) motor.release();
            } catch (Exception ignorado) {
                // nada a fazer
            }
            try {
                if (android != null) android.release();
            } catch (Exception ignorado) {
                // nada a fazer
            }
        }
    }
}
