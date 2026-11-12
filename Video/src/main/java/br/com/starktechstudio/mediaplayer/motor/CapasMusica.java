// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
 */
public final class CapasMusica {

    /** Lado maior da cópia, em px. */
    static final int LADO = 600;
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
