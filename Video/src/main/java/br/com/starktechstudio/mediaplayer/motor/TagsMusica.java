// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import android.media.MediaDataSource;
import android.media.MediaMetadataRetriever;

import androidx.annotation.Nullable;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tags de uma música (1.5): título, artista, artista do álbum, álbum, faixa, disco, ano, gênero e
 * duração, lidos pelo Android ({@link MediaMetadataRetriever}), e a capa gravada no arquivo. O que a
 * tag não traz vem do nome: "Artista / Álbum / 01 - Faixa.mp3" dentro da pasta de Músicas; arquivo
 * solto, "Artista - Faixa.mp3".
 *
 * <p>2.0: a música do servidor em casa é lida por trechos ({@link MediaDataSource} sobre a leitura do Nova a
 * partir de um ponto), sem baixar o arquivo; a capa gravada só é pedida quando a pasta não tem imagem.
 */
public final class TagsMusica {

    /** O que foi lido; texto nulo = não se sabe. */
    public static final class Tags {
        public String titulo;
        public String artista;
        /** Do álbum ("Vários artistas" numa coletânea); nulo = o da faixa. */
        public String artistaAlbum;
        public String album;
        public int faixa;
        public int disco;
        public Integer ano;
        public String genero;
        public long duracaoMs;
        /** Capa gravada no arquivo (JPEG ou PNG); nula = não tem. Não vai para o banco. */
        @Nullable
        public byte[] capa;
    }

    /** "03 - Nome", "03. Nome", "03 Nome", "3-Nome": o número da faixa e o resto. */
    private static final Pattern FAIXA_NO_NOME = Pattern.compile("^(\\d{1,3})\\s*(?:[-._)]\\s*|\\s+)(.+)$");
    /** "1-03 Nome" (disco e faixa). */
    private static final Pattern DISCO_FAIXA_NO_NOME = Pattern.compile("^(\\d)-(\\d{2})\\s*(?:[-._]\\s*|\\s+)(.+)$");
    private static final Pattern ANO = Pattern.compile("((?:19|20)\\d{2})");
    private static final Pattern ANO_DEPOIS_DO_ALBUM = Pattern.compile("^(.*?)\\s*[(\\[]((?:19|20)\\d{2})[)\\]]\\s*$");
    private static final Pattern ANO_ANTES_DO_ALBUM = Pattern.compile("^[(\\[]?((?:19|20)\\d{2})[)\\]]?\\s*-\\s*(.+)$");
    private static final Pattern NUMERO = Pattern.compile("^\\s*(\\d{1,4})");

    private TagsMusica() {}

    /** Lê o arquivo; {@code relativo} é o caminho dele dentro da pasta de Músicas ("Artista/Álbum/01.mp3"). */
    public static Tags ler(File arquivo, String relativo) {
        Tags tags = new Tags();
        MediaMetadataRetriever leitor = new MediaMetadataRetriever();
        try {
            leitor.setDataSource(arquivo.getAbsolutePath());
            lerTags(leitor, tags, true);
        } catch (RuntimeException e) {
            // arquivo que o Android não abre: fica só o que vem do nome
        } finally {
            try {
                leitor.release();
            } catch (IOException | RuntimeException e) {
                // nada a fazer
            }
        }
        completar(tags, relativo);
        return tags;
    }

    /**
     * Música do servidor em casa (2.0), lida por trechos; {@code comCapa} false = sem a capa gravada (a pasta tem
     * imagem, que vale antes).
     */
    public static Tags lerDoServidor(ArquivosDaBiblioteca arquivos, String caminho, long tamanho, String relativo, boolean comCapa) {
        Tags tags = new Tags();
        MediaMetadataRetriever leitor = new MediaMetadataRetriever();
        FonteDoServidor fonte = new FonteDoServidor(arquivos, caminho, tamanho);
        try {
            leitor.setDataSource(fonte);
            lerTags(leitor, tags, comCapa);
        } catch (RuntimeException e) {
            // servidor saiu no meio ou arquivo que o Android não abre: fica só o que vem do nome
        } finally {
            try {
                leitor.release();
            } catch (IOException | RuntimeException e) {
                // nada a fazer
            }
            fonte.close();
        }
        completar(tags, relativo);
        return tags;
    }

    private static void lerTags(MediaMetadataRetriever leitor, Tags tags, boolean comCapa) {
        tags.titulo = limpo(leitor.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));
        tags.artista = limpo(leitor.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));
        tags.artistaAlbum = limpo(leitor.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST));
        tags.album = limpo(leitor.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM));
        tags.faixa = numero(leitor.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER));
        tags.disco = numero(leitor.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER));
        tags.ano = ano(leitor.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR));
        if (tags.ano == null) tags.ano = ano(leitor.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE));
        tags.genero = limpo(leitor.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE));
        tags.duracaoMs = numeroLongo(leitor.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
        if (comCapa) tags.capa = leitor.getEmbeddedPicture();
    }

    /**
     * O arquivo do servidor por trechos: mantém uma leitura aberta e só abre outra quando o leitor pula para
     * trás ou para longe (cada abertura é um pedido ao servidor).
     */
    static final class FonteDoServidor extends MediaDataSource {
        /** Pulo para a frente até aqui: lê e descarta em vez de abrir de novo. */
        private static final long PULO_SEM_REABRIR = 256 * 1024L;
        private static final int BUFFER = 64 * 1024;

        private final ArquivosDaBiblioteca arquivos;
        private final String caminho;
        private final long tamanho;
        private InputStream entrada;
        private long posicao;

        FonteDoServidor(ArquivosDaBiblioteca arquivos, String caminho, long tamanho) {
            this.arquivos = arquivos;
            this.caminho = caminho;
            this.tamanho = tamanho;
        }

        @Override
        public synchronized int readAt(long ponto, byte[] destino, int inicio, int quantos) throws IOException {
            if (quantos <= 0) return 0;
            if (tamanho > 0 && ponto >= tamanho) return -1;
            if (entrada == null || ponto < posicao || ponto - posicao > PULO_SEM_REABRIR) {
                fechar();
                entrada = new BufferedInputStream(arquivos.abrir(caminho, ponto), BUFFER);
                posicao = ponto;
            }
            while (posicao < ponto) {
                long pulados = entrada.skip(ponto - posicao);
                if (pulados <= 0) {
                    if (entrada.read() < 0) return -1;
                    pulados = 1;
                }
                posicao += pulados;
            }
            int lidos = entrada.read(destino, inicio, quantos);
            if (lidos > 0) posicao += lidos;
            return lidos;
        }

        @Override
        public long getSize() {
            return tamanho > 0 ? tamanho : -1;
        }

        @Override
        public synchronized void close() {
            fechar();
        }

        private void fechar() {
            if (entrada == null) return;
            try {
                entrada.close();
            } catch (IOException e) {
                // nada a fazer
            }
            entrada = null;
        }
    }

    /**
     * O que falta vem do nome: arquivo em "Artista/Álbum/", a pasta de cima é o artista e a de baixo, o
     * álbum ("Artista - Álbum" também vale numa pasta só); solto, "Artista - Faixa". Sem título, o nome do
     * arquivo sem o número da faixa.
     */
    static void completar(Tags tags, String relativo) {
        String[] partes = relativo.split("/");
        String arquivo = Nomes.semExtensao(partes[partes.length - 1]);
        String nome = Nomes.limpar(arquivo);
        int faixaDoNome = 0;
        int discoDoNome = 0;
        Matcher discoFaixa = DISCO_FAIXA_NO_NOME.matcher(nome);
        Matcher faixa = FAIXA_NO_NOME.matcher(nome);
        if (discoFaixa.matches()) {
            discoDoNome = Integer.parseInt(discoFaixa.group(1));
            faixaDoNome = Integer.parseInt(discoFaixa.group(2));
            nome = discoFaixa.group(3).trim();
        } else if (faixa.matches() && !ehAno(faixa.group(1))) {
            faixaDoNome = Integer.parseInt(faixa.group(1));
            nome = faixa.group(2).trim();
        }

        String artistaDaPasta = null;
        String albumDaPasta = null;
        if (partes.length >= 3) {
            artistaDaPasta = Nomes.limpar(partes[partes.length - 3]);
            albumDaPasta = Nomes.limpar(partes[partes.length - 2]);
        } else if (partes.length == 2) {
            String pasta = Nomes.limpar(partes[0]);
            int traco = pasta.indexOf(" - ");
            if (traco > 0) {
                artistaDaPasta = pasta.substring(0, traco).trim();
                albumDaPasta = pasta.substring(traco + 3).trim();
            } else {
                albumDaPasta = pasta;
            }
        }
        // Ano no nome do álbum: "Álbum (1975)" ou "1975 - Álbum"
        if (albumDaPasta != null) {
            Matcher depois = ANO_DEPOIS_DO_ALBUM.matcher(albumDaPasta);
            Matcher antes = ANO_ANTES_DO_ALBUM.matcher(albumDaPasta);
            if (depois.matches() && !depois.group(1).trim().isEmpty()) {
                if (tags.ano == null) tags.ano = Integer.valueOf(depois.group(2));
                albumDaPasta = depois.group(1).trim();
            } else if (antes.matches()) {
                if (tags.ano == null) tags.ano = Integer.valueOf(antes.group(1));
                albumDaPasta = antes.group(2).trim();
            }
        }

        // Solto na pasta de Músicas: "Artista - Faixa"
        String artistaDoNome = null;
        if (partes.length == 1 || artistaDaPasta == null) {
            int traco = nome.indexOf(" - ");
            if (traco > 0) {
                artistaDoNome = nome.substring(0, traco).trim();
                nome = nome.substring(traco + 3).trim();
            }
        }

        if (tags.titulo == null) tags.titulo = nome.isEmpty() ? arquivo : nome;
        if (tags.artista == null) tags.artista = artistaDoNome != null ? artistaDoNome : artistaDaPasta;
        if (tags.album == null) tags.album = albumDaPasta;
        if (tags.faixa == 0) tags.faixa = faixaDoNome;
        if (tags.disco == 0) tags.disco = discoDoNome;
    }

    /** Pastas dos vídeos soltos do artista (1.5.1): "Vídeos", "Clipes"; o vídeo dentro delas não tem álbum. */
    private static final String[] PASTAS_DE_VIDEOS = {"videos", "video", "clipes", "clipe", "clips", "clip",
            "videoclipes", "videoclipe", "video clipes", "video clipe", "music videos"};

    static boolean ehPastaDeVideos(String nome) {
        String normal = Nomes.normalizar(nome);
        for (String p : PASTAS_DE_VIDEOS) if (p.equals(normal)) return true;
        return false;
    }

    /**
     * Vídeo dentro da pasta de Músicas (1.5.1, D1 do plano 006), só pelo nome: o artista é a 1ª pasta;
     * a pasta de cima do arquivo, se não for a do artista nem "Vídeos", é o álbum em vídeo ("Ao Vivo (2019)",
     * com o ano); o número no começo do arquivo é a ordem. Solto na raiz: "Artista - Título". Dentro da pasta
     * do artista, "Artista - Título" perde o artista repetido.
     */
    static Tags clipe(String relativo) {
        String[] partes = relativo.split("/");
        String arquivo = Nomes.semExtensao(partes[partes.length - 1]);
        Tags t = new Tags();
        String nome = Nomes.limpar(arquivo);
        Matcher faixa = FAIXA_NO_NOME.matcher(nome);
        if (faixa.matches() && !ehAno(faixa.group(1))) {
            t.faixa = Integer.parseInt(faixa.group(1));
            nome = faixa.group(2).trim();
        }
        String artista = partes.length >= 2 ? Nomes.limpar(partes[0]) : null;
        String album = partes.length >= 3 ? Nomes.limpar(partes[partes.length - 2]) : null;
        if (album != null && ehPastaDeVideos(album)) album = null;
        if (album != null) {
            Matcher depois = ANO_DEPOIS_DO_ALBUM.matcher(album);
            Matcher antes = ANO_ANTES_DO_ALBUM.matcher(album);
            if (depois.matches() && !depois.group(1).trim().isEmpty()) {
                t.ano = Integer.valueOf(depois.group(2));
                album = depois.group(1).trim();
            } else if (antes.matches()) {
                t.ano = Integer.valueOf(antes.group(1));
                album = antes.group(2).trim();
            }
        }
        int traco = nome.indexOf(" - ");
        if (traco > 0) {
            String antes = nome.substring(0, traco).trim();
            String depois = nome.substring(traco + 3).trim();
            if (artista == null) {
                artista = antes;
                nome = depois;
            } else if (Nomes.normalizar(antes).equals(Nomes.normalizar(artista)) && !depois.isEmpty()) {
                nome = depois;
            }
        }
        t.artista = artista;
        t.album = album;
        t.titulo = nome.isEmpty() ? arquivo : nome;
        return t;
    }

    /** "3/12" → 3; "03" → 3; nulo ou sem número → 0. */
    static int numero(@Nullable String texto) {
        if (texto == null) return 0;
        Matcher m = NUMERO.matcher(texto);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    /** "2019", "2019-05-01", "20190501T000000.000Z" → 2019; nulo se não há ano. */
    @Nullable
    static Integer ano(@Nullable String texto) {
        if (texto == null) return null;
        Matcher m = ANO.matcher(texto);
        return m.find() ? Integer.valueOf(m.group(1)) : null;
    }

    private static long numeroLongo(@Nullable String texto) {
        if (texto == null) return 0;
        try {
            return Long.parseLong(texto.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static boolean ehAno(String numero) {
        return numero.length() == 4 && ANO.matcher(numero).matches();
    }

    /** Sem espaços nas pontas; vazio vira nulo. */
    @Nullable
    static String limpo(@Nullable String texto) {
        if (texto == null) return null;
        String t = texto.replace('\u0000', ' ').trim();
        return t.isEmpty() ? null : t;
    }
}
