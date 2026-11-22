// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Capa do álbum (1.5, sem internet): a gravada na música ou a imagem da pasta dele ("cover", "folder",
 * "front", "album" ou a única imagem que houver). Vira uma cópia reduzida em {@code files/musicas/capas/},
 * uma por pasta e álbum: a do arquivo pode estar no HD, que sai, e a da pasta pode não ser legível sem o
 * acesso a todos os arquivos na próxima vez.
 *
 * <p>2.0: no servidor em casa, a imagem da pasta vale antes da gravada na música (a da pasta já veio na
 * listagem; a gravada pede ler mais da música pela rede).
 */
public final class CapasMusica {

    /** Lado maior da cópia, em px. */
    static final int LADO = 600;
    /** Imagem de pasta do servidor maior que isto não é baixada para virar capa. */
    private static final long IMAGEM_MAXIMA = 20L * 1024 * 1024;
    /** Nomes preferidos da imagem na pasta, sem extensão. */
    private static final String[] NOMES = {"cover", "folder", "front", "album", "capa", "albumart"};

    private final File pasta;

    public CapasMusica(Context contexto) {
        this(new File(new File(contexto.getFilesDir(), "musicas"), "capas"));
    }

    CapasMusica(File pasta) {
        this.pasta = pasta;
    }

    /** Onde fica a capa do álbum desta pasta (existindo ou não). */
    File arquivoDa(String pastaDoAlbum, @Nullable String album) {
        String chave = pastaDoAlbum + "|" + Nomes.normalizar(album);
        return new File(pasta, UUID.nameUUIDFromBytes(chave.getBytes(StandardCharsets.UTF_8)) + ".jpg");
    }

    /**
     * Capa do álbum: a cópia já feita; senão a gravada na música ({@code embutida}) ou a imagem da
     * pasta. Nula se não há nenhuma.
     */
    @Nullable
    public String capa(File musica, @Nullable String album, @Nullable byte[] embutida) {
        File pastaDoAlbum = musica.getParentFile();
        if (pastaDoAlbum == null) return null;
        File destino = arquivoDa(pastaDoAlbum.getAbsolutePath(), album);
        if (destino.isFile() && destino.length() > 0) return destino.getAbsolutePath();
        Bitmap imagem = embutida == null ? null : reduzir(embutida);
        if (imagem == null) {
            File daPasta = imagemDaPasta(pastaDoAlbum);
            if (daPasta != null) imagem = reduzir(daPasta);
        }
        if (imagem == null) return null;
        try {
            gravar(imagem, destino);
            return destino.getAbsolutePath();
        } catch (IOException e) {
            return null;
        } finally {
            imagem.recycle();
        }
    }

    /** Capa já feita do álbum desta pasta (do aparelho ou do servidor); nula se ainda não há. */
    @Nullable
    public String jaFeita(String pastaDoAlbum, @Nullable String album) {
        File destino = arquivoDa(pastaDoAlbum, album);
        return destino.isFile() && destino.length() > 0 ? destino.getAbsolutePath() : null;
    }

    /**
     * Capa do álbum de uma música do servidor (2.0): a cópia já feita; senão a imagem da pasta ({@code daPasta},
     * da listagem), lida pela rede; senão a gravada na música. Nula se não há nenhuma.
     */
    @Nullable
    public String capaDoServidor(ArquivosDaBiblioteca arquivos, String musica, @Nullable String album,
                                 @Nullable ArquivosDaBiblioteca.Entrada daPasta, @Nullable byte[] embutida) {
        String pastaDoAlbum = ArquivosDaBiblioteca.acima(musica);
        if (pastaDoAlbum == null) return null;
        String feita = jaFeita(pastaDoAlbum, album);
        if (feita != null) return feita;
        Bitmap imagem = null;
        if (daPasta != null && daPasta.tamanho > 0 && daPasta.tamanho <= IMAGEM_MAXIMA) {
            byte[] dados = baixar(arquivos, daPasta.caminho, daPasta.tamanho);
            if (dados != null) imagem = reduzir(dados);
        }
        if (imagem == null && embutida != null) imagem = reduzir(embutida);
        if (imagem == null) return null;
        File destino = arquivoDa(pastaDoAlbum, album);
        try {
            gravar(imagem, destino);
            return destino.getAbsolutePath();
        } catch (IOException e) {
            return null;
        } finally {
            imagem.recycle();
        }
    }

    @Nullable
    private static byte[] baixar(ArquivosDaBiblioteca arquivos, String caminho, long tamanho) {
        try (InputStream entrada = arquivos.abrir(caminho, 0)) {
            ByteArrayOutputStream saida = new ByteArrayOutputStream((int) Math.min(tamanho, Integer.MAX_VALUE));
            byte[] bloco = new byte[64 * 1024];
            int lidos;
            while ((lidos = entrada.read(bloco)) > 0) {
                saida.write(bloco, 0, lidos);
                if (saida.size() > IMAGEM_MAXIMA) return null;
            }
            return saida.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    /** A imagem da pasta entre os itens da listagem do servidor (mesma escolha de {@link #imagemDaPasta}). */
    @Nullable
    public static ArquivosDaBiblioteca.Entrada imagemEntre(List<ArquivosDaBiblioteca.Entrada> itens) {
        List<ArquivosDaBiblioteca.Entrada> imagens = new ArrayList<>();
        List<String> nomes = new ArrayList<>();
        for (ArquivosDaBiblioteca.Entrada e : itens) {
            if (!e.pasta && !e.nome.startsWith(".") && Nomes.ehImagem(e.nome)) {
                imagens.add(e);
                nomes.add(e.nome);
            }
        }
        int escolhida = preferida(nomes);
        return escolhida < 0 ? null : imagens.get(escolhida);
    }

    /** Índice da imagem de nome preferido ou, sem ela, da única que houver; -1 se nenhuma serve. */
    static int preferida(List<String> nomes) {
        for (String preferido : NOMES) {
            for (int i = 0; i < nomes.size(); i++) {
                if (Nomes.semExtensao(nomes.get(i)).toLowerCase(Locale.ROOT).equals(preferido)) return i;
            }
        }
        return nomes.size() == 1 ? 0 : -1;
    }

    /** Apaga as cópias que nenhum álbum usa mais. */
    public void limpar(Set<String> emUso) {
        File[] arquivos = pasta.listFiles();
        if (arquivos == null) return;
        for (File f : arquivos) {
            if (!emUso.contains(f.getAbsolutePath())) {
                //noinspection ResultOfMethodCallIgnored
                f.delete();
            }
        }
    }

    /** A imagem da pasta: a de nome preferido ou, sem ela, a única imagem que houver. */
    @Nullable
    public static File imagemDaPasta(File pastaDoAlbum) {
        File[] itens = pastaDoAlbum.listFiles();
        if (itens == null) return null;
        List<File> imagens = new ArrayList<>();
        for (File f : itens) {
            if (f.isFile() && !f.getName().startsWith(".") && Nomes.ehImagem(f.getName())) imagens.add(f);
        }
        for (String preferido : NOMES) {
            for (File f : imagens) {
                if (Nomes.semExtensao(f.getName()).toLowerCase(Locale.ROOT).equals(preferido)) return f;
            }
        }
        return imagens.size() == 1 ? imagens.get(0) : null;
    }

    @Nullable
    private static Bitmap reduzir(byte[] dados) {
        BitmapFactory.Options medidas = new BitmapFactory.Options();
        medidas.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(dados, 0, dados.length, medidas);
        if (medidas.outWidth <= 0 || medidas.outHeight <= 0) return null;
        BitmapFactory.Options opcoes = new BitmapFactory.Options();
        opcoes.inSampleSize = amostra(medidas.outWidth, medidas.outHeight);
        return escalar(BitmapFactory.decodeByteArray(dados, 0, dados.length, opcoes));
    }

    @Nullable
    private static Bitmap reduzir(File arquivo) {
        BitmapFactory.Options medidas = new BitmapFactory.Options();
        medidas.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(arquivo.getAbsolutePath(), medidas);
        if (medidas.outWidth <= 0 || medidas.outHeight <= 0) return null;
        BitmapFactory.Options opcoes = new BitmapFactory.Options();
        opcoes.inSampleSize = amostra(medidas.outWidth, medidas.outHeight);
        return escalar(BitmapFactory.decodeFile(arquivo.getAbsolutePath(), opcoes));
    }

    /** Potência de 2 que deixa o lado maior ainda acima de {@link #LADO}. */
    static int amostra(int largura, int altura) {
        int amostra = 1;
        while (Math.max(largura, altura) / (amostra * 2) >= LADO) amostra *= 2;
        return amostra;
    }

    @Nullable
    private static Bitmap escalar(@Nullable Bitmap imagem) {
        if (imagem == null) return null;
        int maior = Math.max(imagem.getWidth(), imagem.getHeight());
        if (maior <= LADO) return imagem;
        float escala = LADO / (float) maior;
        Bitmap menor = Bitmap.createScaledBitmap(imagem, Math.max(1, Math.round(imagem.getWidth() * escala)),
                Math.max(1, Math.round(imagem.getHeight() * escala)), true);
        if (menor != imagem) imagem.recycle();
        return menor;
    }

    private static void gravar(Bitmap imagem, File destino) throws IOException {
        File pai = destino.getParentFile();
        if (pai != null && !pai.isDirectory() && !pai.mkdirs()) throw new IOException("sem pasta " + pai);
        File temporario = new File(destino.getPath() + ".tmp");
        try (OutputStream saida = new FileOutputStream(temporario)) {
            if (!imagem.compress(Bitmap.CompressFormat.JPEG, 88, saida)) throw new IOException("não gravou " + destino);
        }
        if (!temporario.renameTo(destino)) {
            //noinspection ResultOfMethodCallIgnored
            temporario.delete();
            throw new IOException("não renomeou " + destino);
        }
    }
}
