// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

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
 */
public final class QuadrosDeCapitulo {

    private static final String LOG = "StarkTechQuadros";
    private static final String PASTA = "capitulos";
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
    private static final Handler TELA = new Handler(Looper.getMainLooper());
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
                Bitmap quadro = leitor.quadro(momentoMs(inicios[i], fim) * 1000L);
                if (quadro == null) continue;
                File destino = new File(pasta, nome(caminho, inicios[i]));
                if (!gravar(quadro, destino)) continue;
                feitos++;
                final int inicio = inicios[i];
                final String gravado = destino.getAbsolutePath();
                TELA.post(() -> {
                    if (!pedido.cancelado) aviso.chegou(inicio, gravado);
                });
            }
            if (feitos == 0) SEM_QUADRO.add(caminho);
        } finally {
            leitor.fechar();
        }
    }

    private static File pasta(Context contexto) {
        return new File(contexto.getCacheDir(), PASTA);
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

        Bitmap quadro(long momentoUs) {
            Bitmap quadro = doMotor(momentoUs);
            // A reserva do Android lê só o arquivo do aparelho; no servidor, só o motor
            if (quadro == null && local) quadro = doAndroid(momentoUs);
            return quadro;
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
