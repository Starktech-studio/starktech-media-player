// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import androidx.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Álbuns montados das músicas (1.5). Com o artista do álbum na tag, o álbum é o par artista do álbum e
 * nome; sem ele, as músicas da mesma pasta com o mesmo nome de álbum: com artistas diferentes, é uma
 * coletânea ("Vários artistas"). As faixas ficam em ordem de disco, faixa e título.
 */
public final class Discografia {

    public static final class Album {
        public final String chave;
        /** Nulo = sem nome de álbum (músicas soltas da pasta). */
        @Nullable
        public final String nome;
        /** Nulo = artista desconhecido ou {@link #variosArtistas}. */
        @Nullable
        public String artista;
        public boolean variosArtistas;
        @Nullable
        public Integer ano;
        @Nullable
        public String capa;
        public final List<AcervoMusicas.Musica> musicas = new ArrayList<>();

        Album(String chave, @Nullable String nome) {
            this.chave = chave;
            this.nome = nome;
        }
    }

    /** Ordem das faixas no álbum. */
    static final Comparator<AcervoMusicas.Musica> ORDEM_DAS_FAIXAS = (a, b) -> {
        if (a.disco != b.disco) return Integer.compare(a.disco, b.disco);
        if (a.faixa != b.faixa) return Integer.compare(a.faixa == 0 ? Integer.MAX_VALUE : a.faixa, b.faixa == 0 ? Integer.MAX_VALUE : b.faixa);
        String ta = a.titulo == null ? "" : Nomes.normalizar(a.titulo);
        String tb = b.titulo == null ? "" : Nomes.normalizar(b.titulo);
        int titulo = ta.compareTo(tb);
        return titulo != 0 ? titulo : a.caminho.compareTo(b.caminho);
    };

    private Discografia() {}

    /** Os álbuns, na ordem em que aparecem; cada um com as faixas em ordem. */
    public static List<Album> albuns(List<AcervoMusicas.Musica> musicas) {
        Map<String, Album> porChave = new LinkedHashMap<>();
        for (AcervoMusicas.Musica m : musicas) {
            String chave = chave(m);
            Album album = porChave.get(chave);
            if (album == null) {
                album = new Album(chave, m.album);
                porChave.put(chave, album);
            }
            album.musicas.add(m);
        }
        List<Album> lista = new ArrayList<>(porChave.values());
        for (Album a : lista) {
            Collections.sort(a.musicas, ORDEM_DAS_FAIXAS);
            completar(a);
        }
        return lista;
    }

    /** "album:" + artista do álbum e nome; sem artista do álbum, a pasta do arquivo e o nome. */
    static String chave(AcervoMusicas.Musica m) {
        String album = Nomes.normalizar(m.album);
        if (m.artistaAlbum != null) return "album:" + Nomes.normalizar(m.artistaAlbum) + "|" + album;
        File pai = new File(m.caminho).getParentFile();
        return "album:" + (pai == null ? "" : pai.getAbsolutePath()) + "|" + album;
    }

    /** Artista (o do álbum, o único das faixas ou "Vários artistas"), o ano mais comum e a primeira capa. */
    private static void completar(Album a) {
        String artistaAlbum = null;
        String unico = null;
        boolean varios = false;
        Map<Integer, Integer> anos = new LinkedHashMap<>();
        for (AcervoMusicas.Musica m : a.musicas) {
            if (artistaAlbum == null && m.artistaAlbum != null) artistaAlbum = m.artistaAlbum;
            if (m.artista != null) {
                if (unico == null) unico = m.artista;
                else if (!Nomes.normalizar(unico).equals(Nomes.normalizar(m.artista))) varios = true;
            }
            if (m.ano != null) {
                Integer n = anos.get(m.ano);
                anos.put(m.ano, n == null ? 1 : n + 1);
            }
            if (a.capa == null && m.capa != null) a.capa = m.capa;
        }
        if (artistaAlbum != null) {
            a.artista = artistaAlbum;
        } else if (varios) {
            a.variosArtistas = true;
        } else {
            a.artista = unico;
        }
        int maior = 0;
        for (Map.Entry<Integer, Integer> ano : anos.entrySet()) {
            if (ano.getValue() > maior) {
                maior = ano.getValue();
                a.ano = ano.getKey();
            }
        }
    }

    /** O artista conta como dele: é o do álbum ou o da faixa. */
    public static boolean ehDoArtista(AcervoMusicas.Musica m, Album album, String chaveArtista) {
        if (album.artista != null && Acervo.chaveArtista(album.artista).equals(chaveArtista)) return true;
        return m.artista != null && Acervo.chaveArtista(m.artista).equals(chaveArtista);
    }
}
