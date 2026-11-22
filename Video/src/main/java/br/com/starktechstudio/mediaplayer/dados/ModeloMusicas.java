// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.dados;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * O que as telas de Músicas mostram (1.5), montado pelo {@link RepositorioMusicas}. Texto nulo = não se
 * sabe: a tela mostra "Artista desconhecido", "Sem álbum" ou "Vários artistas". 1.5.1: vídeos da pasta de
 * Músicas, nos álbuns em vídeo e nos vídeos soltos de cada artista; tocam no player de vídeo.
 */
public final class ModeloMusicas {

    private ModeloMusicas() {}

    /**
     * De onde toca (2.0, D16): o aparelho (HD externo ou memória interna), o servidor em casa ou a cópia baixada
     * dele. A baixada que o servidor também tem conta como baixada.
     */
    public enum Origem { APARELHO, BAIXADA, SERVIDOR }

    /** Origem da coleção: a da maioria das faixas (empate: baixada, servidor, aparelho); vazia = aparelho. */
    public static Origem daMaioria(List<Musica> musicas) {
        int baixadas = 0;
        int servidor = 0;
        int aparelho = 0;
        for (Musica m : musicas) {
            if (m.origem == Origem.BAIXADA) baixadas++;
            else if (m.origem == Origem.SERVIDOR) servidor++;
            else aparelho++;
        }
        return daMaioria(aparelho, baixadas, servidor);
    }

    /** A da maioria pelas contagens (empate: baixada, servidor, aparelho); tudo zero = aparelho. Vale para vídeos. */
    public static Origem daMaioria(int aparelho, int baixadas, int servidor) {
        if (baixadas > 0 && baixadas >= servidor && baixadas >= aparelho) return Origem.BAIXADA;
        if (servidor > 0 && servidor >= aparelho) return Origem.SERVIDOR;
        return Origem.APARELHO;
    }

    /**
     * A origem que a capa ou o detalhe mostra (D16, ajuste de 21/11/2026): só com algum servidor cadastrado; sem
     * ele, tudo é do aparelho e nada aparece (nula).
     */
    @Nullable
    public static Origem origemAMostrar(@Nullable Origem origem, boolean temServidor) {
        return temServidor ? origem : null;
    }

    /** true = alguma faixa tem cópia no aparelho (filtro "Só baixadas", D14). */
    public static boolean temBaixada(List<Musica> musicas) {
        for (Musica m : musicas) if (m.origem == Origem.BAIXADA) return true;
        return false;
    }

    /**
     * Tempo de tudo o que a coleção tem, músicas e vídeos (ajuste de 21/11/2026: selo do álbum e da playlist). A
     * duração que não se sabe (0) conta 0.
     */
    public static long duracaoTotalMs(List<Musica> itens) {
        long total = 0;
        for (Musica m : itens) total += Math.max(0, m.duracaoMs);
        return total;
    }

    /**
     * "15 min"; de 60 min em diante, "1 h 15 min" ou, na hora cheia, "2 h". Minutos arredondados, com 1 min no
     * mínimo quando há algum tempo; 0 ou menos, "0 min" (quem mostra esconde o selo).
     */
    public static String textoDaDuracao(long ms) {
        if (ms <= 0) return "0 min";
        long minutos = Math.max(1, Math.round(ms / 60000.0));
        if (minutos < 60) return minutos + " min";
        long resto = minutos % 60;
        return resto == 0 ? (minutos / 60) + " h" : (minutos / 60) + " h " + resto + " min";
    }

    /** Só as baixadas, na mesma ordem. */
    public static List<Musica> soBaixadas(List<Musica> musicas) {
        List<Musica> lista = new ArrayList<>();
        for (Musica m : musicas) if (m.origem == Origem.BAIXADA) lista.add(m);
        return lista;
    }

    /** Uma música ou um vídeo de música ({@link #video}); {@link #id} é o caminho do arquivo. */
    public static final class Musica {
        public String id;
        public String titulo;
        @Nullable
        public String artista;
        @Nullable
        public String album;
        public String idAlbum;
        public int faixa;
        public int disco;
        public long duracaoMs;
        @Nullable
        public String capa;
        public int cor;
        /** 1.5.1: vídeo, que abre no player de vídeo (o id é o do título na biblioteca de vídeo). */
        public boolean video;
        /** 1.7: vídeo já começado (segundos) e assistido, para a barrinha do card. */
        public int posicaoSegundos;
        public boolean assistido;
        /** 2.0 (D16): de onde toca. */
        public Origem origem = Origem.APARELHO;
    }

    public static final class Album {
        public String id;
        @Nullable
        public String nome;
        @Nullable
        public String artista;
        @Nullable
        public String idArtista;
        public boolean variosArtistas;
        @Nullable
        public Integer ano;
        @Nullable
        public String capa;
        public int cor;
        /** 1.5.1: álbum em vídeo; com {@link #soltos}, os vídeos do artista sem álbum. */
        public boolean video;
        public boolean soltos;
        /** "MP3", "FLAC · MP3" ou, em vídeo, "1080p · H.264"; nulo se não se sabe. */
        @Nullable
        public String formato;
        /** Da Wikipédia (1.5.1); nulos = sem artigo. */
        @Nullable
        public String resumo;
        @Nullable
        public String descricao;
        public final List<Musica> musicas = new ArrayList<>();
        /** 2.0 (D16): a origem da maioria das faixas. */
        public Origem origem = Origem.APARELHO;

        public long duracaoMs() {
            return duracaoTotalMs(musicas);
        }
    }

    public static final class Artista {
        public String id;
        @Nullable
        public String nome;
        @Nullable
        public String foto;
        public int cor;
        public int albuns;
        public int musicas;
        /** 1.5.1: vídeos dele (dos álbuns em vídeo e soltos). */
        public int videos;
        /** "MP3 · FLAC" das músicas e "1080p" dos vídeos (1.5.1); nulos = não tem. */
        @Nullable
        public String formatoMusicas;
        @Nullable
        public String formatoVideos;
        /** Da Wikipédia; nulos = sem artigo. */
        @Nullable
        public String resumo;
        @Nullable
        public String descricao;
    }

    public static final class Playlist {
        public long id;
        public String nome;
        /** As que estão na biblioteca agora, na ordem (as do HD desligado não aparecem). */
        public final List<Musica> musicas = new ArrayList<>();
        /** Posição de cada uma de {@link #musicas} na playlist inteira (para tirar, subir e descer). */
        public final List<Integer> posicoes = new ArrayList<>();
        /** Quantas a playlist tem, contando as que não estão na biblioteca agora. */
        public int total;
        @Nullable
        public String capa;
        public int cor;
        /** 2.0 (D16): a origem da maioria das faixas. */
        public Origem origem = Origem.APARELHO;
    }
}
