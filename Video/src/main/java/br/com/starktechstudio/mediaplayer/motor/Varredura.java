// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.SystemClock;
import android.util.Log;

import com.archos.medialib.IMediaMetadataRetriever;
import com.archos.medialib.MediaFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Varredura das pastas da biblioteca (F3): {@link Classificador#varrer} acha e classifica os vídeos;
 * os novos entram no {@link Acervo}, os que sumiram de verdade saem (HD desligado ou com erro de
 * leitura não apaga nada) e o motor lê os dados técnicos de cada novo — duração,
 * resolução, faixas de áudio e legenda, capítulos — numa abertura só do arquivo. O quadro do vídeo
 * (miniatura) é outro passo, depois do TMDB e só para quem ficou sem imagem ({@link #miniaturas}):
 * é a parte lenta (num HD USB com arquivos de vários GB, segundos por vídeo) e atrasava os capítulos.
 */
public final class Varredura {

    /** Registro no log (adb logcat -s StarkTechVarredura): o que foi lido de cada vídeo e quanto levou. */
    private static final String LOG = "StarkTechVarredura";

    /** Avisado a cada passo que muda a biblioteca (a tela relê). */
    public interface Aviso {
        void aoMudar();
    }

    /** Andamento de um passo longo: {@code feitos} de {@code total} vídeos. */
    public interface Andamento {
        void em(int feitos, int total);
    }

    /** Ponto do vídeo para a miniatura (fração da duração). */
    private static final double PONTO_MINIATURA = 0.15;
    private static final int LARGURA_MINIATURA = 480;
    /**
     * Versão da leitura: vídeo lido com uma versão anterior é lido de novo. 1 lia os metadados em
     * bloco, que o leitor guarda como texto (erro de tipo: sem capítulos e sem duração); 2 lia tudo
     * pelo texto do leitor; 3 = a 2 com a reserva dos capítulos direto do arquivo
     * ({@link CapitulosDoArquivo}), para quando o motor do app não traz capítulos no aparelho; 4 = só
     * fecha se a leitura respondeu. Até a 3, erro ou tempo estourado ficavam marcados como lidos e
     * vazios: quem ficou sem capítulos ou sem duração é lido de novo, o resto fica ({@link #faltaLer});
     * 5 = o HDR, que o leitor não trazia: da 4, só é lido de novo o formato que pode ter HDR.
     */
    public static final int LEITURA = 5;
    /**
     * Em {@code lido}: nunca fechou, mas o motor já tentou abrir (não respondeu ou nem terminou). Segue
     * faltando ler ({@link #faltaLer}), só que não é mais novo: entra no tempo da rodada.
     */
    static final int TENTOU = -1;
    /** Quantos vídeos lidos entre um aviso e outro. */
    private static final int AVISO_A_CADA = 8;
    private static final long LIMITE_POR_VIDEO_MS = 60 * 1000L;
    /** Tempo por rodada para reler quem já passou por uma leitura, inclusive quem falhou; quem sobrar fica para a próxima. */
    private static final long RELEITURA_POR_RODADA_MS = 5 * 60 * 1000L;
    /** Leituras presas (motor ou HD sem responder) com que a fila para: cada uma é uma linha parada. */
    private static final int MAXIMO_PRESAS = 3;
    /** Depois disso, a leitura presa deixa de barrar a fila e o Detalhe; a linha dela, se acordar, ainda grava o que achou. */
    static final long PRAZO_PRESA_MS = 5 * 60 * 1000L;

    /** Vídeos sendo lidos, com a hora do início, inclusive os que passaram do limite: não abre outra leitura em cima. */
    private static final ConcurrentHashMap<String, Long> lendo = new ConcurrentHashMap<>();

    private Varredura() {}

    /**
     * Procura os vídeos nas pastas; devolve true se vale tentar de novo: uma pasta com vídeos na
     * biblioteca veio sem nenhum, com o armazenamento dela montado (erro de leitura no HD).
     */
    public static boolean listar(Context contexto, Aviso aviso) {
        return atualizarLista(contexto, Acervo.de(contexto), Pastas.de(contexto).lista(), aviso);
    }

    /**
     * Classificação: grava os novos e os que mudaram e apaga os que saíram. Vídeo não achado só sai
     * se a pasta saiu da biblioteca (ou virou Ignorar) ou se o arquivo sumiu de verdade
     * ({@link #sumiu}): com erro de leitura no HD a lista vem vazia, sem erro nenhum, e apagar pela
     * lista levava a biblioteca inteira. Pasta com vídeos na biblioteca que vem sem nenhum não perde
     * nada (some de vez só ao tirar a pasta ou quando ela voltar a ter vídeos). Devolve true se vale
     * tentar de novo.
     */
    private static boolean atualizarLista(Context contexto, Acervo acervo, List<Pastas.Pasta> pastas, Aviso aviso) {
        Map<String, Acervo.Video> antes = new HashMap<>();
        for (Acervo.Video v : acervo.videos()) antes.put(v.caminho, v);

        // Por pasta: {achados, mantidos, apagados}, para o registro no log
        Map<String, int[]> contas = new HashMap<>();
        Set<String> cadastradas = new HashSet<>();
        for (Pastas.Pasta p : pastas) {
            cadastradas.add(p.caminho);
            conta(contas, p.caminho);
        }

        long agora = System.currentTimeMillis();
        List<Acervo.Video> gravar = new ArrayList<>();
        Set<String> achados = new HashSet<>();
        for (Classificador.Item item : Classificador.varrer(pastas)) {
            achados.add(item.caminho);
            File arquivo = new File(item.caminho);
            Pastas.Pasta pasta = pastaDe(pastas, item.caminho);
            conta(contas, pasta == null ? "" : pasta.caminho)[0]++;
            Acervo.Video velho = antes.get(item.caminho);
            // Listado, mas sem os dados do arquivo (erro de leitura no HD): o que já tem fica como está
            if (velho != null && !arquivo.exists()) continue;
            Acervo.Video v = new Acervo.Video();
            v.caminho = item.caminho;
            v.pasta = pasta == null ? "" : pasta.caminho;
            v.tipo = item.tipo;
            v.titulo = item.titulo;
            v.ano = item.ano;
            v.serie = item.serie;
            v.temporada = item.temporada;
            v.episodio = item.episodio;
            v.artista = item.artista;
            v.grupo = item.grupo;
            v.motivo = item.motivo;
            v.tamanho = arquivo.length();
            v.modificado = arquivo.lastModified();
            boolean mesmoArquivo = velho != null && velho.tamanho == v.tamanho && velho.modificado == v.modificado;
            v.adicionado = velho != null ? velho.adicionado : agora;
            if (mesmoArquivo) copiarDadosTecnicos(velho, v);
            if (velho == null || !igual(velho, v)) gravar.add(v);
        }

        List<String> remover = new ArrayList<>();
        Set<String> semNenhum = new HashSet<>();
        for (Acervo.Video v : antes.values()) {
            if (achados.contains(v.caminho)) continue;
            Pastas.Pasta dona = pastaDe(pastas, v.caminho);
            boolean saiu = !cadastradas.contains(v.pasta) || dona == null || dona.tipo == Pastas.Tipo.IGNORAR;
            boolean pastaSemNenhum = conta(contas, v.pasta)[0] == 0;
            if (saiu || (!pastaSemNenhum && sumiu(v))) {
                remover.add(v.caminho);
                conta(contas, v.pasta)[2]++;
            } else {
                conta(contas, v.pasta)[1]++;
                if (pastaSemNenhum) semNenhum.add(v.pasta);
            }
        }
        // HD desligado não conta: quando ligar, o aviso do sistema varre de novo
        boolean tentarDeNovo = false;
        for (String pasta : semNenhum) {
            if (ArquivosDoAparelho.raizDe(contexto, pasta) != null) tentarDeNovo = true;
        }
        for (Map.Entry<String, int[]> item : contas.entrySet()) {
            int[] c = item.getValue();
            Log.i(LOG, item.getKey() + ": " + c[0] + " achados, " + c[1] + " mantidos, " + c[2] + " apagados");
        }
        if (!gravar.isEmpty() || !remover.isEmpty()) {
            acervo.gravarVideos(gravar, remover);
            for (String caminho : remover) apagarMiniatura(antes.get(caminho));
            aviso.aoMudar();
        }
        return tentarDeNovo;
    }

    private static int[] conta(Map<String, int[]> contas, String pasta) {
        int[] conta = contas.get(pasta);
        if (conta == null) {
            conta = new int[3];
            contas.put(pasta, conta);
        }
        return conta;
    }

    /**
     * O arquivo sumiu de verdade: não existe e a pasta mais próxima acima dele que existe (até a
     * cadastrada: subpasta renomeada também conta) abre e lista algo. Com erro de leitura no HD, essa
     * lista vem nula ou vazia.
     */
    private static boolean sumiu(Acervo.Video v) {
        File arquivo = new File(v.caminho);
        if (arquivo.exists()) return false;
        File raiz = new File(v.pasta);
        File acima = arquivo.getParentFile();
        while (acima != null && !acima.exists() && !acima.equals(raiz)) acima = acima.getParentFile();
        String[] itens = acima == null ? null : acima.list();
        return itens != null && itens.length > 0;
    }

    private static Pastas.Pasta pastaDe(List<Pastas.Pasta> pastas, String caminho) {
        Pastas.Pasta melhor = null;
        for (Pastas.Pasta p : pastas) {
            if (Pastas.dentro(caminho, p.caminho) && (melhor == null || p.caminho.length() > melhor.caminho.length())) melhor = p;
        }
        return melhor;
    }

    private static void copiarDadosTecnicos(Acervo.Video de, Acervo.Video para) {
        para.lido = de.lido;
        para.duracaoMs = de.duracaoMs;
        para.largura = de.largura;
        para.altura = de.altura;
        para.infoVideo = de.infoVideo;
        para.audios = de.audios;
        para.legendas = de.legendas;
        para.capitulos = de.capitulos;
        para.miniatura = de.miniatura;
    }

    /** Mesma classificação e mesmos dados técnicos: não precisa gravar. */
    private static boolean igual(Acervo.Video a, Acervo.Video b) {
        return a.tipo == b.tipo && a.motivo == b.motivo && a.lido == b.lido
                && a.tamanho == b.tamanho && a.modificado == b.modificado
                && iguais(a.pasta, b.pasta) && iguais(a.titulo, b.titulo) && iguais(a.ano, b.ano)
                && iguais(a.serie, b.serie) && a.temporada == b.temporada && a.episodio == b.episodio
                && iguais(a.artista, b.artista) && iguais(a.grupo, b.grupo);
    }

    private static boolean iguais(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    // ---- Dados técnicos ----

    /**
     * Fila da leitura: primeiro os nunca tentados, sem limite; depois, em ordem sorteada (os que falham
     * não ocupam sempre os mesmos lugares) e até o tempo da rodada, quem já passou por uma leitura,
     * inclusive quem falhou ({@link #TENTOU}). Roda fora da tela; pedido novo recomeça a rodada
     * ({@link Andamento}).
     */
    public static void lerDadosTecnicos(Context contexto, Aviso aviso, Andamento andamento) {
        Acervo acervo = Acervo.de(contexto);
        List<Acervo.Video> faltam = new ArrayList<>();
        List<Acervo.Video> reler = new ArrayList<>();
        for (Acervo.Video v : semLeitura(acervo)) {
            if (!new File(v.caminho).isFile()) continue;
            if (jaTentou(v)) reler.add(v);
            else faltam.add(v);
        }
        Collections.shuffle(reler);
        faltam.addAll(reler);
        long fimDaReleitura = -1;
        int lidos = 0;
        for (Acervo.Video v : faltam) {
            int presas = leiturasAbertas(lendo, SystemClock.elapsedRealtime());
            if (presas >= MAXIMO_PRESAS) {
                Log.w(LOG, "leitura parada nesta rodada: " + presas + " presas, faltam " + (faltam.size() - lidos));
                break;
            }
            if (jaTentou(v)) {
                long agora = SystemClock.elapsedRealtime();
                if (fimDaReleitura < 0) {
                    fimDaReleitura = agora + RELEITURA_POR_RODADA_MS;
                } else if (agora >= fimDaReleitura) {
                    Log.i(LOG, "releitura: tempo da rodada acabou, faltam " + (faltam.size() - lidos));
                    break;
                }
            }
            // Passou do limite: a reserva pode já estar gravada, e a tela não espera os próximos
            if (!lerComLimite(contexto, acervo, v.caminho, aviso, false)) aviso.aoMudar();
            andamento.em(++lidos, faltam.size());
            if (lidos % AVISO_A_CADA == 0) aviso.aoMudar();
        }
        if (lidos % AVISO_A_CADA != 0) aviso.aoMudar();
    }

    /** Já passou por uma leitura, respondendo ou não: só não é mais novo. */
    static boolean jaTentou(Acervo.Video v) {
        return v.lido != 0;
    }

    /** Vídeo nunca tentado passa a {@link #TENTOU}; devolve true se mudou. */
    static boolean marcarTentativa(Acervo.Video v) {
        if (jaTentou(v)) return false;
        v.lido = TENTOU;
        return true;
    }

    /** Quantas leituras contam ({@code abertas}: vídeo e início), depois de tirar as que passaram do prazo. */
    static int leiturasAbertas(Map<String, Long> abertas, long agora) {
        Iterator<Long> inicios = abertas.values().iterator();
        while (inicios.hasNext()) {
            if (agora - inicios.next() >= PRAZO_PRESA_MS) inicios.remove();
        }
        return abertas.size();
    }

    /**
     * Quem precisa ser lido. De quem veio de leitura anterior só isso: quem já tem capítulos e duração
     * é fechado aqui, sem abrir o arquivo.
     */
    private static List<Acervo.Video> semLeitura(Acervo acervo) {
        List<Acervo.Video> faltam = new ArrayList<>();
        List<Acervo.Video> completos = new ArrayList<>();
        synchronized (acervo) {
            for (Acervo.Video v : acervo.videos()) {
                if (faltaLer(v)) {
                    faltam.add(v);
                } else if (v.lido < LEITURA) {
                    v.lido = LEITURA;
                    completos.add(v);
                }
            }
            if (!completos.isEmpty()) acervo.gravarVideos(completos, Collections.<String>emptyList());
        }
        return faltam;
    }

    /**
     * Falta ler este vídeo: nunca foi lido; ou veio de uma leitura anterior sem capítulos ou sem
     * duração. Com a leitura atual fechada, nada mais (arquivo sem capítulos fica sem capítulos).
     */
    public static boolean faltaLer(Acervo.Video v) {
        if (v.lido >= LEITURA) return false;
        if (v.lido == 4) return podeTerHdr(v.infoVideo);
        return v.lido <= 0 || v.duracaoMs <= 0 || v.capitulos.vazio();
    }

    /** H.264 e os formatos antigos não têm HDR: quem veio da leitura 4 com eles não abre de novo. */
    private static boolean podeTerHdr(String infoVideo) {
        return infoVideo != null && (infoVideo.contains("H.265") || infoVideo.contains("AV1") || infoVideo.contains("VP9"));
    }

    /**
     * Arquivo que trava o motor (ou HD que parou de responder) não segura a biblioteca: a leitura
     * roda à parte e, passado o limite, segue para o próximo vídeo. Devolve true se acabou no limite.
     */
    private static boolean comLimite(Runnable leitura, String caminho) {
        Thread linha = new Thread(leitura, "StarkTechLerVideo");
        linha.setDaemon(true);
        linha.start();
        try {
            linha.join(LIMITE_POR_VIDEO_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (!linha.isAlive()) return true;
        Log.w(LOG, "leitura travada, seguindo: " + caminho);
        return false;
    }

    /**
     * Um vídeo aberto na tela (Detalhe) que a varredura ainda não leu, ou cuja leitura falhou: lê
     * agora, fora da fila. Devolve true se acabou no limite (a tela relê a biblioteca); depois do
     * limite, a leitura ainda grava o que achar e chama {@code aviso}, como ao achar a reserva.
     */
    public static boolean lerAgora(Context contexto, String caminho, Aviso aviso) {
        Acervo acervo = Acervo.de(contexto);
        Acervo.Video v = acervo.video(caminho);
        if (v == null || !faltaLer(v) || leiturasAbertas(lendo, SystemClock.elapsedRealtime()) >= MAXIMO_PRESAS
                || !new File(v.caminho).isFile()) return false;
        return lerComLimite(contexto, acervo, caminho, aviso, true);
    }

    /**
     * Quem grava é a própria linha da leitura: tempo estourado não grava nada (vazio, muito menos) e
     * o resultado que chegar depois não se perde; {@code aviso} leva a novidade à tela nesse caso.
     */
    private static boolean lerComLimite(Context contexto, Acervo acervo, String caminho, Aviso aviso, boolean avisarReserva) {
        AtomicBoolean passouDoLimite = new AtomicBoolean();
        boolean noLimite = comLimite(() -> {
            try {
                lerEGravar(contexto, acervo, caminho, avisarReserva ? aviso : null);
                if (passouDoLimite.get()) aviso.aoMudar();
            } catch (RuntimeException e) {
                // Linha à parte: um erro aqui derrubaria o app
                Log.w(LOG, "não deu para gravar a leitura de " + caminho, e);
            }
        }, caminho);
        if (!noLimite) passouDoLimite.set(true);
        return noLimite;
    }

    /**
     * Primeiro a reserva em Java (rápida, gravada na hora: se o motor travar depois, os capítulos já
     * ficaram); com ela, quem só precisava dos capítulos nem abre no motor. {@code aoAcharReserva}:
     * avisado ao gravar a reserva (nulo = sem aviso).
     */
    private static void lerEGravar(Context contexto, Acervo acervo, String caminho, Aviso aoAcharReserva) {
        Long inicio = SystemClock.elapsedRealtime();
        if (lendo.putIfAbsent(caminho, inicio) != null) return;
        try {
            Acervo.Video v = acervo.video(caminho);
            if (v == null || !faltaLer(v)) return;
            if (v.capitulos.vazio()) {
                Capitulos reserva = CapitulosDoArquivo.ler(caminho);
                if (!reserva.vazio()) {
                    Log.i(LOG, new File(caminho).getName() + ": " + reserva.quantidade() + " capítulos lidos do arquivo (Java)");
                    Acervo.Video achada = new Acervo.Video();
                    achada.capitulos = reserva;
                    boolean aindaFalta = gravarLeitura(acervo, caminho, achada);
                    if (aoAcharReserva != null) aoAcharReserva.aoMudar();
                    if (!aindaFalta) return;
                }
            }
            marcarTentativa(acervo, caminho);
            gravarLeitura(acervo, caminho, lerNoMotor(contexto, caminho));
        } finally {
            // Com o início: passado o prazo, outra leitura deste vídeo pode já estar aberta, e fica
            lendo.remove(caminho, inicio);
        }
    }

    /** Antes de abrir no motor: se ele não responder ou travar, o vídeo já não volta como novo na próxima rodada. */
    private static void marcarTentativa(Acervo acervo, String caminho) {
        synchronized (acervo) {
            Acervo.Video atual = acervo.video(caminho);
            if (atual == null || !marcarTentativa(atual)) return;
            acervo.gravarVideos(Collections.singletonList(atual), Collections.<String>emptyList());
        }
    }

    /**
     * Grava sobre a linha de agora, não a do começo da leitura (o player pode ter gravado capítulos
     * nesse meio-tempo). Devolve true se o vídeo ainda precisa de leitura.
     */
    private static boolean gravarLeitura(Acervo acervo, String caminho, Acervo.Video lida) {
        synchronized (acervo) {
            Acervo.Video atual = acervo.video(caminho);
            if (atual == null) return false;
            aplicar(lida, atual);
            acervo.gravarVideos(Collections.singletonList(atual), Collections.<String>emptyList());
            return faltaLer(atual);
        }
    }

    /**
     * O que a leitura trouxe entra; o que não trouxe fica como estava (leitura falha nunca apaga). O
     * vídeo fecha se a leitura respondeu ou se, com o que ela trouxe, nada mais falta.
     */
    static void aplicar(Acervo.Video lida, Acervo.Video v) {
        if (!lida.capitulos.vazio()) v.capitulos = lida.capitulos;
        if (lida.duracaoMs > 0) v.duracaoMs = lida.duracaoMs;
        if (lida.largura > 0) v.largura = lida.largura;
        if (lida.altura > 0) v.altura = lida.altura;
        if (lida.infoVideo != null && !lida.infoVideo.isEmpty()) v.infoVideo = lida.infoVideo;
        if (!lida.audios.isEmpty()) v.audios = lida.audios;
        if (!lida.legendas.isEmpty()) v.legendas = lida.legendas;
        if (lida.lido >= LEITURA || !faltaLer(v)) v.lido = LEITURA;
    }

    /** O motor abriu e disse alguma coisa do arquivo (abrir sem erro e sem dado nenhum não conta). */
    static boolean respondeu(Acervo.Video lida) {
        return lida.duracaoMs > 0 || lida.largura > 0 || !lida.capitulos.vazio() || !lida.audios.isEmpty();
    }

    /**
     * Lê pelo motor numa abertura só (leitor de dados, sem tocar o vídeo): capítulos, duração,
     * tamanho da imagem, formato e as faixas de áudio e legenda, tudo pelo texto do leitor (é como
     * ele entrega; a conferência da F2 lê os capítulos assim). Devolve o que achou; só vem com
     * {@code lido} preenchido se o motor respondeu. Falhou, o vídeo fica para a próxima rodada e o
     * motivo vai para o log (o player lê os capítulos, a duração e as faixas dele ao abrir).
     */
    private static Acervo.Video lerNoMotor(Context contexto, String caminho) {
        Acervo.Video v = new Acervo.Video();
        String nome = new File(caminho).getName();
        long comeco = SystemClock.elapsedRealtime();
        IMediaMetadataRetriever leitor = null;
        try {
            leitor = MediaFactory.createMetadataRetriever(contexto);
            leitor.setDataSource(caminho);
            v.capitulos = Capitulos.doLeitor(leitor);
            v.duracaoMs = numero(leitor.extractMetadata(IMediaMetadataRetriever.METADATA_KEY_DURATION));
            v.largura = (int) numero(leitor.extractMetadata(IMediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
            v.altura = (int) numero(leitor.extractMetadata(IMediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
            v.infoVideo = Faixas.video(leitor.extractMetadata(IMediaMetadataRetriever.METADATA_KEY_VIDEO_TRACK
                    + IMediaMetadataRetriever.METADATA_KEY_VIDEO_TRACK_FORMAT),
                    (int) numero(leitor.extractMetadata(IMediaMetadataRetriever.METADATA_KEY_VIDEO_TRACK
                            + IMediaMetadataRetriever.METADATA_KEY_VIDEO_TRACK_COLOR_TRC)));
            List<String> audios = new ArrayList<>();
            int nAudios = (int) numero(leitor.extractMetadata(IMediaMetadataRetriever.METADATA_KEY_NB_AUDIO_TRACK));
            for (int i = 0; i < nAudios; i++) {
                int base = IMediaMetadataRetriever.METADATA_KEY_AUDIO_TRACK + i * IMediaMetadataRetriever.METADATA_KEY_AUDIO_TRACK_MAX;
                audios.add(Faixas.audio(leitor.extractMetadata(base + IMediaMetadataRetriever.METADATA_KEY_AUDIO_TRACK_NAME),
                        leitor.extractMetadata(base + IMediaMetadataRetriever.METADATA_KEY_AUDIO_TRACK_FORMAT),
                        leitor.extractMetadata(base + IMediaMetadataRetriever.METADATA_KEY_AUDIO_TRACK_CHANNELS),
                        leitor.extractMetadata(base + IMediaMetadataRetriever.METADATA_KEY_AUDIO_TRACK_LANGUAGE), i));
            }
            v.audios = audios;
            List<String> legendas = new ArrayList<>();
            int nLegendas = (int) numero(leitor.extractMetadata(IMediaMetadataRetriever.METADATA_KEY_NB_SUBTITLE_TRACK));
            for (int i = 0; i < nLegendas; i++) {
                int base = IMediaMetadataRetriever.METADATA_KEY_SUBTITLE_TRACK + i * IMediaMetadataRetriever.METADATA_KEY_SUBTITLE_TRACK_MAX;
                String arquivo = leitor.extractMetadata(base + IMediaMetadataRetriever.METADATA_KEY_SUBTITLE_TRACK_PATH);
                String nomeDoMotor = leitor.extractMetadata(base + IMediaMetadataRetriever.METADATA_KEY_SUBTITLE_TRACK_NAME);
                if (arquivo == null || arquivo.isEmpty()) {
                    legendas.add(Faixas.legenda(nomeDoMotor,
                            leitor.extractMetadata(base + IMediaMetadataRetriever.METADATA_KEY_SUBTITLE_TRACK_LANGUAGE), false, i));
                } else if (!LegendaExterna.deOutroVideo(nome, arquivo)) {
                    legendas.add(Faixas.legendaEmArquivo(nome, arquivo, nomeDoMotor, i));
                }
            }
            v.legendas = legendas;
            Log.i(LOG, nome + ": " + v.duracaoMs + " ms, " + v.capitulos.quantidade()
                    + " capítulos, " + nAudios + " áudios, " + nLegendas + " legendas ("
                    + leitor.getClass().getSimpleName() + ", " + (SystemClock.elapsedRealtime() - comeco) + " ms)");
            if (respondeu(v)) v.lido = LEITURA;
            else Log.w(LOG, "não deu para ler " + caminho + ": o motor abriu e não trouxe dados; fica para a próxima rodada");
        } catch (Exception e) {
            Log.w(LOG, "não deu para ler " + caminho + ": erro no motor; fica para a próxima rodada", e);
        } finally {
            try {
                if (leitor != null) leitor.release();
            } catch (Exception ignorado) {
                // nada a fazer
            }
        }
        return v;
    }

    /**
     * Quadro do vídeo (miniatura) para quem ficou sem imagem do TMDB ({@code comImagem}: caminhos que
     * já têm fundo ou imagem do episódio). Tentou e não deu: fica marcado ({@code ""}) para não
     * tentar a cada atualização.
     */
    public static void miniaturas(Context contexto, Set<String> comImagem, Aviso aviso, Andamento andamento) {
        File pasta = pastaMiniaturas(contexto);
        if (pasta == null) return;
        Acervo acervo = Acervo.de(contexto);
        List<Acervo.Video> faltam = new ArrayList<>();
        for (Acervo.Video v : acervo.videos()) {
            if (v.miniatura != null || v.duracaoMs <= 0 || comImagem.contains(v.caminho) || !new File(v.caminho).isFile()) continue;
            faltam.add(v);
        }
        int feitas = 0;
        for (Acervo.Video v : faltam) {
            andamento.em(feitas, faltam.size());
            v.miniatura = "";
            comLimite(() -> gravarQuadro(contexto, v, pasta), v.caminho);
            guardarMiniatura(acervo, v);
            if (++feitas % AVISO_A_CADA == 0) aviso.aoMudar();
        }
        if (feitas % AVISO_A_CADA != 0) aviso.aoMudar();
    }

    /**
     * Quadro de um vídeo agora, fora da fila: o passo das miniaturas é o último da rodada, e o cartão
     * de Continuar assistindo não espera por ele. Tenta de novo quem já falhou. True se gravou.
     */
    public static boolean quadroAgora(Context contexto, String caminho) {
        File pasta = pastaMiniaturas(contexto);
        Acervo acervo = Acervo.de(contexto);
        Acervo.Video v = acervo.video(caminho);
        if (pasta == null || v == null || v.duracaoMs <= 0 || !new File(caminho).isFile()) return false;
        if (v.miniatura != null && new File(v.miniatura).isFile()) return false;
        v.miniatura = "";
        comLimite(() -> gravarQuadro(contexto, v, pasta), caminho);
        guardarMiniatura(acervo, v);
        return !v.miniatura.isEmpty();
    }

    /** Sobre a linha de agora: a de {@code v} pode ser anterior aos capítulos que chegaram depois. */
    private static void guardarMiniatura(Acervo acervo, Acervo.Video v) {
        synchronized (acervo) {
            Acervo.Video atual = acervo.video(v.caminho);
            if (atual != null) {
                atual.miniatura = v.miniatura;
                acervo.gravarVideos(Collections.singletonList(atual), Collections.<String>emptyList());
            }
        }
    }

    private static void gravarQuadro(Context contexto, Acervo.Video v, File pasta) {
        long momentoUs = (long) (v.duracaoMs * PONTO_MINIATURA) * 1000L;
        Bitmap quadro = quadroDoMotor(contexto, v.caminho, momentoUs);
        if (quadro == null) quadro = quadroDoAndroid(v.caminho, momentoUs);
        if (quadro == null) return;
        String gravada = gravarMiniatura(quadro, pasta, v.caminho);
        if (gravada != null) v.miniatura = gravada;
    }

    private static Bitmap quadroDoMotor(Context contexto, String caminho, long momentoUs) {
        IMediaMetadataRetriever leitor = MediaFactory.createMetadataRetriever(contexto);
        try {
            leitor.setDataSource(caminho);
            return leitor.getFrameAtTime(momentoUs);
        } catch (Exception e) {
            Log.w(LOG, "sem quadro do motor em " + caminho, e);
            return null;
        } finally {
            try {
                leitor.release();
            } catch (Exception ignorado) {
                // nada a fazer
            }
        }
    }

    /** Reserva: o leitor do próprio Android, quando o motor não entrega o quadro. */
    private static Bitmap quadroDoAndroid(String caminho, long momentoUs) {
        MediaMetadataRetriever leitor = new MediaMetadataRetriever();
        try {
            leitor.setDataSource(caminho);
            return leitor.getFrameAtTime(momentoUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
        } catch (RuntimeException e) {
            Log.w(LOG, "sem quadro do Android em " + caminho, e);
            return null;
        } finally {
            try {
                leitor.release();
            } catch (Exception ignorado) {
                // nada a fazer
            }
        }
    }

    /** Número do texto do leitor; 0 se não veio ou não é número. */
    private static long numero(String texto) {
        if (texto == null) return 0;
        try {
            return Math.max(0, Long.parseLong(texto.trim()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    static File pastaMiniaturas(Context contexto) {
        File pasta = new File(contexto.getFilesDir(), "miniaturas");
        if (!pasta.isDirectory() && !pasta.mkdirs()) return null;
        return pasta;
    }

    private static String gravarMiniatura(Bitmap quadro, File pasta, String caminho) {
        Bitmap menor = quadro;
        if (quadro.getWidth() > LARGURA_MINIATURA) {
            int altura = Math.max(1, quadro.getHeight() * LARGURA_MINIATURA / quadro.getWidth());
            menor = Bitmap.createScaledBitmap(quadro, LARGURA_MINIATURA, altura, true);
        }
        File destino = new File(pasta, String.format(Locale.ROOT, "%08x_%d.jpg", caminho.hashCode(), caminho.length()));
        try (OutputStream saida = new FileOutputStream(destino)) {
            menor.compress(Bitmap.CompressFormat.JPEG, 82, saida);
            return destino.getAbsolutePath();
        } catch (IOException e) {
            return null;
        } finally {
            if (menor != quadro) menor.recycle();
            quadro.recycle();
        }
    }

    private static void apagarMiniatura(Acervo.Video v) {
        if (v == null || v.miniatura == null || v.miniatura.isEmpty()) return;
        //noinspection ResultOfMethodCallIgnored
        new File(v.miniatura).delete();
    }
}
