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

        public long duracaoMs() {
            long total = 0;
            for (Musica m : musicas) total += m.duracaoMs;
            return total;
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
    }
}
