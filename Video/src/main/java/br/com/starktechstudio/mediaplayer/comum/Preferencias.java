// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.preference.PreferenceManager;

import com.archos.mediacenter.video.R;
import com.archos.mediacenter.video.utils.VideoPreferencesCommon;

import java.io.File;

import br.com.starktechstudio.mediaplayer.motor.ArquivosDoAparelho;

/**
 * Configurações (D16), salvas no aparelho. As da StarkTech ficam em arquivo próprio
 * ("st_preferencias"), sem tocar nas preferências do Nova; as que o motor do Nova já lê são gravadas
 * nas chaves dele, no formato dele ({@link Item#doMotor}). Cada uma tem uma lista curta de valores:
 * com dois, OK / toque troca; com mais, Configurações abre um seletor. Quem usa: {@link Reproducao} (áudio, legenda, retomar, próximo episódio),
 * os players, o Início da TV e o repositório.
 */
public final class Preferencias {

    /** Configuração: chave, valores (textos) e o valor de fábrica (índice). */
    public enum Item {
        AUDIO("audio", 0, R.string.st_valor_original, R.string.st_valor_portugues, R.string.st_valor_ingles),
        LEGENDA("legenda", 0, R.string.st_valor_portugues, R.string.st_valor_ingles, R.string.st_valor_desligada),
        // Forma da legenda (D8): o que cada valor faz está nas tabelas LEGENDA_* de Reproducao, na mesma ordem
        TAMANHO_LEGENDA("tamanho_legenda_5", 2, R.string.st_valor_muito_pequena, R.string.st_valor_pequena,
                R.string.st_valor_media, R.string.st_valor_grande, R.string.st_valor_muito_grande),
        FONTE_LEGENDA("fonte_legenda", 0, R.string.st_valor_padrao, R.string.st_valor_com_serifa, R.string.st_valor_monoespacada),
        NEGRITO_LEGENDA("negrito_legenda", 0, R.string.st_valor_desligado, R.string.st_valor_ligado),
        COR_LEGENDA("cor_legenda", 0, R.string.st_valor_branca, R.string.st_valor_amarela, R.string.st_valor_ciano, R.string.st_valor_verde),
        CONTORNO_LEGENDA("contorno_legenda", 2, R.string.st_valor_nenhum, R.string.st_valor_fino, R.string.st_valor_medio, R.string.st_valor_grosso),
        SOMBRA_LEGENDA("sombra_legenda", 1, R.string.st_valor_nenhuma, R.string.st_valor_leve, R.string.st_valor_forte),
        FUNDO_LEGENDA("fundo_legenda", 0, R.string.st_valor_sem_fundo, R.string.st_valor_por_linha, R.string.st_valor_em_bloco),
        POSICAO_LEGENDA("posicao_legenda", 0, R.string.st_valor_embaixo, R.string.st_valor_pouco_acima, R.string.st_valor_mais_acima),
        RETOMAR("retomar", 0, R.string.st_valor_ligado, R.string.st_valor_desligado),
        /** Pulo das setas da TV e dos dois toques do tablet: {@link Preferencias#puloSegundos}. */
        PULO("pulo", 1, R.string.st_valor_5_s, R.string.st_valor_10_s, R.string.st_valor_30_s),
        /** Quanto os controles do player ficam na tela: {@link Preferencias#tempoControlesMs}. */
        TEMPO_CONTROLES("tempo_controles", 1, R.string.st_valor_3_s, R.string.st_valor_5_s, R.string.st_valor_10_s),
        /** Ao terminar um episódio, abre o seguinte; desligado de fábrica. */
        PROXIMO_EPISODIO("proximo_episodio", 0, R.string.st_valor_desligado, R.string.st_valor_ligado),
        /** Só na TV. */
        PREVIA_VIDEO("previa_video", 0, R.string.st_valor_ligada, R.string.st_valor_desligada),
        /** Só na TV: {@link Preferencias#trocaNovidadesMs}. */
        TROCA_NOVIDADES("troca_novidades", 0, R.string.st_valor_10_s, R.string.st_valor_30_s, R.string.st_valor_desligada),
        /** Aba Shows em seções por subpasta (D7); desligado de fábrica. */
        SHOWS_POR_GRUPO("shows_por_grupo", 1, R.string.st_valor_ligado, R.string.st_valor_desligado),
        /** Varre as pastas e busca no TMDB ao abrir o app e ao voltar a ele. */
        ATUALIZAR_AO_ABRIR("atualizar_ao_abrir", 0, R.string.st_valor_ligado, R.string.st_valor_desligado),
        /** Procura versão nova do app ao abrir, e em Configurações ({@link Preferencias#procurarVersaoSozinho}). */
        ATUALIZAR_APP("atualizar_app", 0, R.string.st_valor_ligado, R.string.st_valor_desligado),
        // Abas do nav ({@link Preferencias#abaLigada}): 0 = aparece. TV Shows e Stand-up, só se ligar
        NAV_FILMES("nav_filmes", 0, R.string.st_valor_ligado, R.string.st_valor_desligado),
        NAV_SERIES("nav_series", 0, R.string.st_valor_ligado, R.string.st_valor_desligado),
        NAV_TVSHOWS("nav_tvshows", 1, R.string.st_valor_ligado, R.string.st_valor_desligado),
        NAV_DESENHOS("nav_desenhos", 0, R.string.st_valor_ligado, R.string.st_valor_desligado),
        NAV_SHOWS("nav_shows", 0, R.string.st_valor_ligado, R.string.st_valor_desligado),
        NAV_STANDUP("nav_standup", 1, R.string.st_valor_ligado, R.string.st_valor_desligado),
        /** Músicas (1.1.1, sem motor): desligada de fábrica. */
        NAV_MUSICAS("nav_musicas", 1, R.string.st_valor_ligado, R.string.st_valor_desligado),

        // Do motor do Nova: Player.java, MediaFactory.java e CustomApplication.applyAudioOutputToNative
        /** Só na TV. "1": escolhe o modo de tela pelo fps do vídeo. */
        TAXA_DA_TV(VideoPreferencesCommon.KEY_ACTIVATE_REFRESHRATE_SWITCH, new Object[]{"0", "1"},
                R.string.st_valor_desligado, R.string.st_valor_ligado),
        DECODIFICACAO(VideoPreferencesCommon.KEY_FORCE_SW, new Object[]{false, true},
                R.string.st_valor_automatica, R.string.st_valor_software),
        /** Só na TV. "2": o encapsulamento do sistema (o "1" é marcado como experimental no Nova). */
        AUDIO_DIRETO("force_audio_passthrough_multiple", new Object[]{"0", "2"},
                R.string.st_valor_desligado, R.string.st_valor_ligado),
        DOLBY_VISION(VideoPreferencesCommon.KEY_DOLBY_VISION_MODE, new Object[]{"auto", "off"},
                R.string.st_valor_automatico, R.string.st_valor_desligado);

        final String chave;
        final int padrao;
        /** Do Nova: o valor gravado para cada texto (String ou Boolean), o primeiro é o de fábrica. Da StarkTech: null. */
        @Nullable
        final Object[] doMotor;
        @StringRes
        final int[] valores;

        Item(String chave, int padrao, @StringRes int... valores) {
            this.chave = chave;
            this.padrao = padrao;
            this.doMotor = null;
            this.valores = valores;
        }

        Item(String chave, Object[] doMotor, @StringRes int... valores) {
            this.chave = chave;
            this.padrao = 0;
            this.doMotor = doMotor;
            this.valores = valores;
        }
    }

    private static final String ARQUIVO = "st_preferencias";

    private static final int[] PULO_SEGUNDOS = {5, 10, 30};
    private static final long[] TEMPO_CONTROLES_MS = {3_000, 5_000, 10_000};
    /** 0 = desligada. */
    private static final long[] TROCA_NOVIDADES_MS = {10_000, 30_000, 0};

    private Preferencias() {}

    private static SharedPreferences arquivo(Context contexto) {
        return contexto.getApplicationContext().getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE);
    }

    /** As preferências que o motor do Nova lê. */
    private static SharedPreferences doNova(Context contexto) {
        return PreferenceManager.getDefaultSharedPreferences(contexto.getApplicationContext());
    }

    /** Nome da pasta de imagens de fábrica (na raiz do HD ou do armazenamento interno). */
    public static final String PASTA_IMAGENS_PADRAO = "Imagens";
    private static final String CHAVE_PASTA_IMAGENS = "pasta_imagens";

    /**
     * Pasta de imagens do Editar (capas e fundos do aparelho): a escolhida em Configurações; sem
     * escolha, a pasta "Imagens" na raiz do HD (ou do interno). Nula se não há nenhuma.
     */
    @Nullable
    public static String pastaImagens(Context contexto) {
        String escolhida = arquivo(contexto).getString(CHAVE_PASTA_IMAGENS, null);
        if (escolhida != null) return escolhida;
        File padrao = ArquivosDoAparelho.pastaNaRaiz(contexto, PASTA_IMAGENS_PADRAO);
        return padrao == null ? null : padrao.getAbsolutePath();
    }

    /** true = a pasta foi escolhida em Configurações (senão, é a "Imagens" achada sozinha). */
    public static boolean pastaImagensEscolhida(Context contexto) {
        return arquivo(contexto).getString(CHAVE_PASTA_IMAGENS, null) != null;
    }

    public static void definirPastaImagens(Context contexto, String caminho) {
        arquivo(contexto).edit().putString(CHAVE_PASTA_IMAGENS, caminho).apply();
    }

    private static final String CHAVE_VERSAO_AVISADA = "versao_avisada";

    /** Última versão nova oferecida sozinha ao abrir o app ({@link AvisoDeVersao}). */
    static long versaoJaAvisada(Context contexto) {
        return arquivo(contexto).getLong(CHAVE_VERSAO_AVISADA, 0);
    }

    static void definirVersaoAvisada(Context contexto, long codigo) {
        arquivo(contexto).edit().putLong(CHAVE_VERSAO_AVISADA, codigo).apply();
    }

    private static final String CHAVE_SO_NAO_ASSISTIDOS = "so_nao_assistidos";

    /** Filtro das abas Filmes, Séries, Desenhos, Shows e Stand-up; vale para todas. */
    public static boolean soNaoAssistidos(Context contexto) {
        return arquivo(contexto).getBoolean(CHAVE_SO_NAO_ASSISTIDOS, false);
    }

    public static void definirSoNaoAssistidos(Context contexto, boolean ligado) {
        arquivo(contexto).edit().putBoolean(CHAVE_SO_NAO_ASSISTIDOS, ligado).apply();
    }

    private static final String CHAVE_PEDIU_ACESSO = "pediu_acesso_arquivos";

    /** True só na primeira chamada: o acesso a todos os arquivos é pedido sozinho uma vez. */
    static boolean primeiroPedidoDeAcesso(Context contexto) {
        if (arquivo(contexto).getBoolean(CHAVE_PEDIU_ACESSO, false)) return false;
        arquivo(contexto).edit().putBoolean(CHAVE_PEDIU_ACESSO, true).apply();
        return true;
    }

    private static final String CHAVE_ZOOM_PLAYER = "zoom_player";

    /** Zoom escolhido no player ou em Configurações (na ordem de {@code R.array.st_zoom_nomes}); 0 = o de fábrica. */
    public static int zoomPlayer(Context contexto) {
        return arquivo(contexto).getInt(CHAVE_ZOOM_PLAYER, 0);
    }

    public static void definirZoomPlayer(Context contexto, int zoom) {
        arquivo(contexto).edit().putInt(CHAVE_ZOOM_PLAYER, zoom).apply();
    }

    /** A aba aparece no nav? Início e Ajustes, sempre; as outras, pela escolha em Configurações &gt; Navegação. */
    public static boolean abaLigada(Context contexto, Navegacao.Aba aba) {
        return aba.itemDoNav == null || indice(contexto, aba.itemDoNav) == 0;
    }

    /** Aba Shows em seções por subpasta (D7); desligado de fábrica. */
    public static boolean showsPorGrupo(Context contexto) {
        return indice(contexto, Item.SHOWS_POR_GRUPO) == 0;
    }

    public static void definirShowsPorGrupo(Context contexto, boolean ligado) {
        arquivo(contexto).edit().putInt(Item.SHOWS_POR_GRUPO.chave, ligado ? 0 : 1).apply();
    }

    /** Pulo das setas com os controles ocultos, na TV, e dos dois toques, no tablet. */
    public static int puloSegundos(Context contexto) {
        return PULO_SEGUNDOS[indice(contexto, Item.PULO)];
    }

    public static long tempoControlesMs(Context contexto) {
        return TEMPO_CONTROLES_MS[indice(contexto, Item.TEMPO_CONTROLES)];
    }

    /** Troca automática de Novidades; 0 = desligada. */
    public static long trocaNovidadesMs(Context contexto) {
        return TROCA_NOVIDADES_MS[indice(contexto, Item.TROCA_NOVIDADES)];
    }

    public static boolean previaLigada(Context contexto) {
        return indice(contexto, Item.PREVIA_VIDEO) == 0;
    }

    public static boolean atualizarAoAbrir(Context contexto) {
        return indice(contexto, Item.ATUALIZAR_AO_ABRIR) == 0;
    }

    public static boolean procurarVersaoSozinho(Context contexto) {
        return indice(contexto, Item.ATUALIZAR_APP) == 0;
    }

    public static boolean proximoEpisodioAutomatico(Context contexto) {
        return indice(contexto, Item.PROXIMO_EPISODIO) == 1;
    }

    /** Índice do valor atual (na ordem de {@link Item}). */
    public static int indice(Context contexto, Item item) {
        return Math.max(0, Math.min(item.valores.length - 1, lido(contexto, item)));
    }

    /** Texto do valor atual. */
    @StringRes
    public static int valor(Context contexto, Item item) {
        return item.valores[indice(contexto, item)];
    }

    /** Passa ao próximo valor (volta ao primeiro depois do último), grava e devolve o texto dele. */
    @StringRes
    public static int proximo(Context contexto, Item item) {
        int i = (indice(contexto, item) + 1) % item.valores.length;
        gravar(contexto, item, i);
        return item.valores[i];
    }

    /** Grava o valor escolhido no seletor (índice na ordem de {@link Item}). */
    public static void escolher(Context contexto, Item item, int i) {
        gravar(contexto, item, Math.max(0, Math.min(item.valores.length - 1, i)));
    }

    private static int lido(Context contexto, Item item) {
        if (item.doMotor == null) return arquivo(contexto).getInt(item.chave, item.padrao);
        Object gravado = doNova(contexto).getAll().get(item.chave);
        for (int i = 0; i < item.doMotor.length; i++) {
            if (item.doMotor[i].equals(gravado)) return i;
        }
        return item.padrao;
    }

    private static void gravar(Context contexto, Item item, int i) {
        if (item.doMotor == null) {
            arquivo(contexto).edit().putInt(item.chave, i).apply();
            return;
        }
        Object gravado = item.doMotor[i];
        SharedPreferences.Editor editor = doNova(contexto).edit();
        if (gravado instanceof Boolean) editor.putBoolean(item.chave, (Boolean) gravado);
        else editor.putString(item.chave, (String) gravado);
        editor.apply();
    }
}
