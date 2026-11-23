// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.audiofx.Equalizer;
import android.media.audiofx.LoudnessEnhancer;
import android.util.Log;

import androidx.annotation.Nullable;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import br.com.starktechstudio.mediaplayer.comum.Preferencias;

/**
 * Equalizador e volume extra (2.1, plano 011), os mesmos para o player de vídeo ({@code comum/Reproducao}) e o de
 * música ({@code musica/Tocador}): um por player, preso à sessão de áudio dele ({@link #usarSessao}), lendo
 * {@link Preferencias} e ouvindo as mudanças, que valem na hora, sem reabrir o vídeo nem a música.
 *
 * <ul>
 *   <li>Equalizador: {@link Equalizer} com os estilos que o aparelho oferece ({@code getPresetName}), com os nomes
 *   conhecidos em pt-BR ({@link #nomeDoEstilo}) e em ordem alfabética ({@link #emOrdem}).</li>
 *   <li>Volume extra: {@link LoudnessEnhancer} (API 19), de 100 % a 200 %, de 10 em 10; o ganho em milibéis é
 *   {@code 15·log2(%/100) × 100} ({@link #ganhoEmMilibeis}: 200 % = 1500 mB, +15 dB; desde a 2.1.1, mais forte que
 *   os +6 dB da 2.1.0, que mudavam pouco o volume percebido). Picos: o próprio LoudnessEnhancer
 *   passa o som aumentado por um compressor com limitador (o do efeito do Android), que segura o que passaria do
 *   máximo; o app não soma outro.</li>
 * </ul>
 *
 * <p>Sessão nova (outro AudioTrack do motor do Nova, troca de faixa de áudio ou de formato): os efeitos da antiga são
 * soltos e refeitos na nova. Áudio direto para o receptor (o AudioTrack não é PCM: Dolby e DTS sem decodificar, pelo
 * HDMI), os efeitos não se aplicam e ficam soltos ({@link Situacao#AUDIO_DIRETO}). Aparelho sem o efeito ou que
 * recusa criá-lo: a opção fica sem efeito, com o aviso nas telas ({@link #temEqualizador}, {@link #temVolumeExtra},
 * {@link #equalizadorFalhou}, {@link #volumeExtraFalhou}), sem fechar o app. Tudo na thread da tela.
 */
public final class EfeitosDeAudio {

    /** Volume extra, em %: 100 = o volume normal (sem ganho). */
    public static final int NIVEL_MINIMO = 100;
    public static final int NIVEL_MAXIMO = 200;
    public static final int NIVEL_PASSO = 10;
    /** O nível ao ligar pela primeira vez. */
    public static final int NIVEL_PADRAO = 150;
    /** Posições do puxador: 0 = 100 %, {@code POSICOES} = 200 %. */
    public static final int POSICOES = (NIVEL_MAXIMO - NIVEL_MINIMO) / NIVEL_PASSO;

    /** Codificações PCM do AudioTrack (AudioFormat.ENCODING_*): DEFAULT, PCM_16BIT, PCM_8BIT, PCM_FLOAT, PCM_24BIT_PACKED e PCM_32BIT. */
    private static final int[] CODIFICACOES_PCM = {1, 2, 3, 4, 21, 22};

    /** Nomes dos estilos de fábrica do Android em pt-BR; o que não está aqui fica com o nome que o aparelho dá. */
    private static final String[][] NOMES_DOS_ESTILOS = {
            {"classical", "Clássica"},
            {"dance", "Dance"},
            {"flat", "Plano"},
            {"folk", "Folk"},
            {"heavy metal", "Heavy metal"},
            {"hip hop", "Hip hop"},
            {"jazz", "Jazz"},
            {"normal", "Normal"},
            {"pop", "Pop"},
            {"rock", "Rock"},
    };

    private static final String LOG = "StarkTechEfeitos";

    /** Um estilo do equalizador: a posição e o nome no aparelho, e o nome na tela. */
    public static final class Estilo {
        public final short indice;
        public final String original;
        public final String nome;

        public Estilo(short indice, String original) {
            this.indice = indice;
            this.original = original;
            this.nome = nomeDoEstilo(original);
        }
    }

    /** O que os efeitos fazem no que está tocando. */
    public enum Situacao {
        /** Valem (os ligados). */
        PRONTO,
        /** Áudio direto para o receptor: o som sai sem passar pelo aparelho, e os efeitos não chegam a ele. */
        AUDIO_DIRETO,
        /** O player não deu a sessão de áudio (ainda abrindo, vídeo sem som ou o motor do Nova no Android 6). */
        SEM_SESSAO
    }

    // ---- O aparelho (uma vez por abertura do app) ----

    private static boolean conhecido;
    private static boolean temEqualizador;
    private static boolean temVolumeExtra;
    private static List<Estilo> estilosDoAparelho = Collections.emptyList();

    /** O aparelho tem equalizador com estilos. */
    public static synchronized boolean temEqualizador(Context contexto) {
        conhecer(contexto);
        return temEqualizador;
    }

    public static synchronized boolean temVolumeExtra(Context contexto) {
        conhecer(contexto);
        return temVolumeExtra;
    }

    /** Os estilos do aparelho, em ordem alfabética do nome na tela; vazia sem equalizador. */
    public static synchronized List<Estilo> estilos(Context contexto) {
        conhecer(contexto);
        return estilosDoAparelho;
    }

    /** O estilo que vale: o escolhido ou, sem escolha (ou outro aparelho), o de fábrica ({@link #estiloEscolhido}). */
    @Nullable
    public static Estilo estiloAtual(Context contexto) {
        return estiloEscolhido(estilos(contexto), Preferencias.estiloEqualizador(contexto));
    }

    /**
     * Pergunta ao aparelho numa sessão de áudio só para isso, sem som: cria um equalizador e lê os estilos, cria um
     * volume extra, e solta os dois. O que não cria fica sem suporte.
     */
    private static void conhecer(Context contexto) {
        if (conhecido) return;
        conhecido = true;
        int sessao = AudioManager.ERROR;
        try {
            AudioManager audio = (AudioManager) contexto.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
            if (audio != null) sessao = audio.generateAudioSessionId();
        } catch (RuntimeException e) {
            Log.w(LOG, "sem sessão para conhecer os efeitos: " + e.getClass().getSimpleName());
        }
        if (sessao <= 0) return;

        Equalizer sonda = null;
        List<Estilo> lidos = new ArrayList<>();
        try {
            sonda = new Equalizer(0, sessao);
            short total = sonda.getNumberOfPresets();
            for (short i = 0; i < total; i++) lidos.add(new Estilo(i, sonda.getPresetName(i)));
        } catch (RuntimeException e) {
            Log.w(LOG, "sem equalizador: " + e.getClass().getSimpleName());
            lidos.clear();
        } finally {
            soltar(sonda);
        }
        estilosDoAparelho = Collections.unmodifiableList(emOrdem(lidos));
        temEqualizador = !estilosDoAparelho.isEmpty();

        LoudnessEnhancer volume = null;
        try {
            volume = new LoudnessEnhancer(sessao);
            temVolumeExtra = true;
        } catch (RuntimeException e) {
            Log.w(LOG, "sem volume extra: " + e.getClass().getSimpleName());
        } finally {
            soltar(volume);
        }
    }

    // ---- Regras (sem Android) ----

    /**
     * % do volume em ganho do LoudnessEnhancer, em milibéis: {@code 15·log2(%/100)} dB × 100, arredondado (110 % = 206,
     * 150 % = 877, 200 % = 1500, +15 dB). Curva de volume percebido: cada passo de 10 % soa mais alto que o anterior de
     * forma clara (a 2.1.0 usava 20·log10, só +6 dB em 200 %). 100 % ou menos, 0. Fora de 100–200, vale o limite.
     */
    public static int ganhoEmMilibeis(int porcento) {
        int nivel = Math.min(NIVEL_MAXIMO, porcento);
        if (nivel <= NIVEL_MINIMO) return 0;
        return (int) Math.round(1500.0 * Math.log(nivel / 100.0) / Math.log(2.0));
    }

    /** Dentro de 100–200 e no passo de 10 mais perto (155 → 160; 154 → 150). */
    public static int limitarNivel(int porcento) {
        int dentro = Math.max(NIVEL_MINIMO, Math.min(NIVEL_MAXIMO, porcento));
        return NIVEL_MINIMO + Math.round((dentro - NIVEL_MINIMO) / (float) NIVEL_PASSO) * NIVEL_PASSO;
    }

    /** Um passo para cima ({@code direcao} positiva) ou para baixo, parando nas pontas. */
    public static int passo(int nivel, int direcao) {
        return limitarNivel(limitarNivel(nivel) + Integer.signum(direcao) * NIVEL_PASSO);
    }

    /** Posição do puxador (0 a {@link #POSICOES}) em %. */
    public static int nivelDaPosicao(int posicao) {
        return limitarNivel(NIVEL_MINIMO + posicao * NIVEL_PASSO);
    }

    public static int posicaoDoNivel(int nivel) {
        return (limitarNivel(nivel) - NIVEL_MINIMO) / NIVEL_PASSO;
    }

    /** Nome do estilo na tela: os de fábrica do Android em pt-BR ("Flat" → "Plano"); os outros, como o aparelho dá. */
    public static String nomeDoEstilo(@Nullable String original) {
        if (original == null) return "";
        String limpo = original.trim();
        String chave = limpo.toLowerCase(Locale.ROOT).replace('-', ' ').replace('_', ' ').replaceAll("\\s+", " ");
        for (String[] par : NOMES_DOS_ESTILOS) {
            if (par[0].equals(chave)) return par[1];
        }
        return limpo;
    }

    /** Sem nome vazio nem repetido (o primeiro fica), em ordem alfabética do nome na tela (pt-BR). */
    public static List<Estilo> emOrdem(List<Estilo> estilos) {
        List<Estilo> lista = new ArrayList<>();
        List<String> vistos = new ArrayList<>();
        for (Estilo e : estilos) {
            if (e == null || e.original == null || e.nome.isEmpty() || vistos.contains(e.original)) continue;
            vistos.add(e.original);
            lista.add(e);
        }
        Collator ordem = Collator.getInstance(Locale.forLanguageTag("pt-BR"));
        Collections.sort(lista, (a, b) -> ordem.compare(a.nome, b.nome));
        return lista;
    }

    /**
     * O escolhido ({@code guardado}, o nome no aparelho); sem ele, ou se este aparelho não tem: "Normal" e, sem
     * "Normal", o primeiro do aparelho. Nulo sem estilos.
     */
    @Nullable
    public static Estilo estiloEscolhido(List<Estilo> estilos, @Nullable String guardado) {
        Estilo normal = null;
        Estilo primeiro = null;
        for (Estilo e : estilos) {
            if (guardado != null && guardado.equals(e.original)) return e;
            if (normal == null && "normal".equalsIgnoreCase(e.original.trim())) normal = e;
            if (primeiro == null || e.indice < primeiro.indice) primeiro = e;
        }
        return normal != null ? normal : primeiro;
    }

    /** O AudioTrack não é PCM (AC3, DTS, IEC 61937…): o áudio vai direto para o receptor. 0 = não se sabe: não é. */
    public static boolean ehAudioDireto(int codificacao) {
        if (codificacao <= 0) return false;
        for (int pcm : CODIFICACOES_PCM) {
            if (pcm == codificacao) return false;
        }
        return true;
    }

    // ---- Num player ----

    private final Context app;
    private final SharedPreferences.OnSharedPreferenceChangeListener aoMudarPreferencia = (preferencias, chave) -> {
        if (Preferencias.ehDosEfeitos(chave)) aplicar();
    };
    /** 0 = nenhuma. */
    private int sessao;
    private boolean audioDireto;
    @Nullable
    private Equalizer equalizador;
    @Nullable
    private LoudnessEnhancer volume;
    /** Não criou (ou recusou o comando) nesta sessão: não tenta de novo até a sessão mudar. */
    private boolean equalizadorFalhou;
    private boolean volumeFalhou;
    private boolean solto;

    /** Um por player; {@link #soltar} quando o player fecha. */
    public EfeitosDeAudio(Context contexto) {
        app = contexto.getApplicationContext();
        Preferencias.ouvir(app, aoMudarPreferencia);
    }

    /**
     * A sessão de áudio do player ({@code nova} 0 ou menos = nenhuma) e se o som dele vai direto para o receptor. Sessão
     * diferente da de antes: solta os efeitos dela e cria na nova, com as preferências de agora.
     */
    public void usarSessao(int nova, boolean direto) {
        if (solto) return;
        int valida = Math.max(0, nova);
        if (valida != sessao) {
            soltarEfeitos();
            sessao = valida;
            equalizadorFalhou = false;
            volumeFalhou = false;
        }
        audioDireto = direto;
        aplicar();
    }

    /** Liga, troca ou solta cada efeito conforme as preferências (também ao mudarem, pelo ouvinte). */
    public void aplicar() {
        if (solto) return;
        aplicarEqualizador();
        aplicarVolume();
    }

    public Situacao situacao() {
        if (sessao <= 0) return Situacao.SEM_SESSAO;
        return audioDireto ? Situacao.AUDIO_DIRETO : Situacao.PRONTO;
    }

    /** Ligado, mas o aparelho não criou ou recusou nesta sessão. */
    public boolean equalizadorFalhou() {
        return equalizadorFalhou;
    }

    public boolean volumeExtraFalhou() {
        return volumeFalhou;
    }

    /** O player fechou: solta os efeitos e para de ouvir as preferências. */
    public void soltar() {
        if (solto) return;
        solto = true;
        Preferencias.deixarDeOuvir(app, aoMudarPreferencia);
        soltarEfeitos();
        sessao = 0;
    }

    private void aplicarEqualizador() {
        boolean querer = sessao > 0 && !audioDireto && Preferencias.equalizadorLigado(app) && temEqualizador(app);
        if (!querer) {
            soltarEqualizador();
            return;
        }
        if (equalizador == null) {
            if (equalizadorFalhou) return;
            try {
                equalizador = new Equalizer(0, sessao);
            } catch (RuntimeException e) {
                Log.w(LOG, "equalizador não criou na sessão " + sessao + ": " + e.getClass().getSimpleName());
                equalizadorFalhou = true;
                return;
            }
        }
        try {
            Estilo estilo = estiloAtual(app);
            short indice = estilo == null ? -1 : indiceNoEqualizador(equalizador, estilo);
            if (indice >= 0) equalizador.usePreset(indice);
            equalizador.setEnabled(true);
        } catch (RuntimeException e) {
            Log.w(LOG, "equalizador recusou: " + e.getClass().getSimpleName());
            equalizadorFalhou = true;
            soltarEqualizador();
        }
    }

    /** A posição do estilo neste equalizador, pelo nome (a do aparelho na sonda, se o nome não bater). */
    private static short indiceNoEqualizador(Equalizer eq, Estilo estilo) {
        short total = eq.getNumberOfPresets();
        for (short i = 0; i < total; i++) {
            if (estilo.original.equals(eq.getPresetName(i))) return i;
        }
        return estilo.indice < total ? estilo.indice : -1;
    }

    private void aplicarVolume() {
        int nivel = Preferencias.nivelVolumeExtra(app);
        boolean querer = sessao > 0 && !audioDireto && Preferencias.volumeExtraLigado(app) && nivel > NIVEL_MINIMO
                && temVolumeExtra(app);
        if (!querer) {
            soltarVolume();
            return;
        }
        if (volume == null) {
            if (volumeFalhou) return;
            try {
                volume = new LoudnessEnhancer(sessao);
            } catch (RuntimeException e) {
                Log.w(LOG, "volume extra não criou na sessão " + sessao + ": " + e.getClass().getSimpleName());
                volumeFalhou = true;
                return;
            }
        }
        try {
            volume.setTargetGain(ganhoEmMilibeis(nivel));
            volume.setEnabled(true);
        } catch (RuntimeException e) {
            Log.w(LOG, "volume extra recusou: " + e.getClass().getSimpleName());
            volumeFalhou = true;
            soltarVolume();
        }
    }

    private void soltarEfeitos() {
        soltarEqualizador();
        soltarVolume();
    }

    private void soltarEqualizador() {
        soltar(equalizador);
        equalizador = null;
    }

    private void soltarVolume() {
        soltar(volume);
        volume = null;
    }

    private static void soltar(@Nullable android.media.audiofx.AudioEffect efeito) {
        if (efeito == null) return;
        try {
            efeito.setEnabled(false);
        } catch (RuntimeException ignorado) {
            // já sem controle
        }
        try {
            efeito.release();
        } catch (RuntimeException ignorado) {
            // já solto
        }
    }
}
