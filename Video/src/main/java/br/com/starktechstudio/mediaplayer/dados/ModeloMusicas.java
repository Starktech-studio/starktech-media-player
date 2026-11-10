// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.dados;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * O que as telas de Músicas mostram (1.5), montado pelo {@link RepositorioMusicas}. Texto nulo = não se
 * sabe: a tela mostra "Artista desconhecido", "Sem álbum" ou "Vários artistas".
 */
public final class ModeloMusicas {

    private ModeloMusicas() {}

    /** Uma música; {@link #id} é o caminho do arquivo. */
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
