// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.util.UUID;

/**
 * Capa ou fundo escolhidos no aparelho (F2, etapa 2; D3 e D7): aceita JPG, PNG e WebP pelos
 * primeiros bytes do arquivo (não pela extensão), até 15 MB, em qualquer proporção (a tela
 * recorta em 2:3 ou 16:9). A imagem é copiada para a pasta do app ({@link #pasta}), as dimensões
 * são lidas sem abrir a imagem inteira, e a referência {@code arquivo:<caminho>} vai para a
 * {@link Edicoes}. Tablet: arquivo vindo do seletor do sistema ({@link #doConteudo}); TV: do
 * navegador de pastas do app ({@link #doArquivo}).
 */
public final class ImagemDoAparelho {

    /** D7: até 15 MB. */
    public static final long LIMITE_BYTES = 15L * 1024 * 1024;

    public enum Formato {
        JPG("jpg"), PNG("png"), WEBP("webp");

        public final String extensao;

        Formato(String extensao) {
            this.extensao = extensao;
        }
    }

    public enum Erro { FORMATO, GRANDE_DEMAIS, ILEGIVEL }

    /** Imagem copiada ({@link #referencia} para a edição) ou o motivo de não ter copiado. */
    public static final class Resultado {
        public final String referencia;
        public final Formato formato;
        public final int largura;
        public final int altura;
        public final Erro erro;

        private Resultado(String referencia, Formato formato, int largura, int altura, Erro erro) {
            this.referencia = referencia;
            this.formato = formato;
            this.largura = largura;
            this.altura = altura;
            this.erro = erro;
        }

        static Resultado falha(Erro erro) {
            return new Resultado(null, null, 0, 0, erro);
        }

        public boolean ok() {
            return erro == null;
        }
    }

    private static final int CABECALHO = 12;

    private ImagemDoAparelho() {}

    /** Pasta das imagens copiadas, dentro da área privada do app. */
    public static File pasta(Context contexto) {
        return new File(contexto.getFilesDir(), "imagens");
    }

    /** Tablet: imagem escolhida no seletor do sistema. */
    public static Resultado doConteudo(Context contexto, Uri endereco) {
        long tamanho = -1;
        try (Cursor dados = contexto.getContentResolver().query(endereco, new String[]{OpenableColumns.SIZE}, null, null, null)) {
            if (dados != null && dados.moveToFirst() && !dados.isNull(0)) tamanho = dados.getLong(0);
        } catch (RuntimeException ignorado) {
            // sem o tamanho: a cópia confere o limite enquanto lê
        }
        try (InputStream entrada = contexto.getContentResolver().openInputStream(endereco)) {
            if (entrada == null) return Resultado.falha(Erro.ILEGIVEL);
            return importar(pasta(contexto), entrada, tamanho);
        } catch (IOException | RuntimeException e) {
            return Resultado.falha(Erro.ILEGIVEL);
        }
    }

    /** TV: imagem escolhida no navegador de pastas do app. */
    public static Resultado doArquivo(Context contexto, File arquivo) {
        try (InputStream entrada = new FileInputStream(arquivo)) {
            return importar(pasta(contexto), entrada, arquivo.length());
        } catch (IOException | RuntimeException e) {
            return Resultado.falha(Erro.ILEGIVEL);
        }
    }

    /** Confere o formato e o tamanho, copia para {@code destino} e lê as dimensões. */
    static Resultado importar(File destino, InputStream entrada, long tamanhoInformado) throws IOException {
        if (excede(tamanhoInformado)) return Resultado.falha(Erro.GRANDE_DEMAIS);
        PushbackInputStream leitura = new PushbackInputStream(entrada, CABECALHO);
        byte[] cabecalho = new byte[CABECALHO];
        int lidos = lerAte(leitura, cabecalho);
        Formato formato = formato(cabecalho, lidos);
        if (formato == null) return Resultado.falha(Erro.FORMATO);
        leitura.unread(cabecalho, 0, lidos);

        if (!destino.isDirectory() && !destino.mkdirs()) return Resultado.falha(Erro.ILEGIVEL);
        File copia = new File(destino, UUID.randomUUID() + "." + formato.extensao);
        long copiados;
        try (OutputStream saida = new FileOutputStream(copia)) {
            copiados = copiar(leitura, saida, LIMITE_BYTES);
        }
        if (copiados < 0) {
            //noinspection ResultOfMethodCallIgnored
            copia.delete();
            return Resultado.falha(Erro.GRANDE_DEMAIS);
        }
        BitmapFactory.Options medidas = new BitmapFactory.Options();
        medidas.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(copia.getAbsolutePath(), medidas);
        if (medidas.outWidth <= 0 || medidas.outHeight <= 0) {
            //noinspection ResultOfMethodCallIgnored
            copia.delete();
            return Resultado.falha(Erro.ILEGIVEL);
        }
        return new Resultado(Edicoes.PREFIXO_ARQUIVO + copia.getAbsolutePath(), formato,
                medidas.outWidth, medidas.outHeight, null);
    }

    /** Formato pelos primeiros bytes: JPG FF D8 FF; PNG 89 "PNG" 0D 0A 1A 0A; WebP "RIFF" ???? "WEBP". */
    static Formato formato(byte[] cabecalho, int lidos) {
        if (lidos >= 3 && (cabecalho[0] & 0xFF) == 0xFF && (cabecalho[1] & 0xFF) == 0xD8 && (cabecalho[2] & 0xFF) == 0xFF) {
            return Formato.JPG;
        }
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        if (comeca(cabecalho, lidos, 0, png)) return Formato.PNG;
        if (comeca(cabecalho, lidos, 0, new byte[]{'R', 'I', 'F', 'F'})
                && comeca(cabecalho, lidos, 8, new byte[]{'W', 'E', 'B', 'P'})) {
            return Formato.WEBP;
        }
        return null;
    }

    static boolean excede(long tamanho) {
        return tamanho > LIMITE_BYTES;
    }

    /** Copia até {@code limite} bytes; -1 se o arquivo passar do limite. */
    static long copiar(InputStream entrada, OutputStream saida, long limite) throws IOException {
        byte[] bloco = new byte[64 * 1024];
        long total = 0;
        int n;
        while ((n = entrada.read(bloco)) != -1) {
            total += n;
            if (total > limite) return -1;
            saida.write(bloco, 0, n);
        }
        return total;
    }

    /**
     * Prévia para a tela, reduzida até caber em {@code largura} × {@code altura} (sem abrir a
     * imagem inteira na memória); nula se a referência não for uma imagem do aparelho legível.
     */
    public static Bitmap previa(String referencia, int largura, int altura) {
        String caminho = Edicoes.arquivoDe(referencia);
        if (caminho == null) return null;
        BitmapFactory.Options medidas = new BitmapFactory.Options();
        medidas.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(caminho, medidas);
        if (medidas.outWidth <= 0 || medidas.outHeight <= 0) return null;
        int reducao = 1;
        while (medidas.outWidth / (reducao * 2) >= largura && medidas.outHeight / (reducao * 2) >= altura) reducao *= 2;
        BitmapFactory.Options leitura = new BitmapFactory.Options();
        leitura.inSampleSize = reducao;
        return BitmapFactory.decodeFile(caminho, leitura);
    }

    private static int lerAte(InputStream entrada, byte[] destino) throws IOException {
        int total = 0;
        while (total < destino.length) {
            int n = entrada.read(destino, total, destino.length - total);
            if (n == -1) break;
            total += n;
        }
        return total;
    }

    private static boolean comeca(byte[] dados, int lidos, int inicio, byte[] esperado) {
        if (lidos < inicio + esperado.length) return false;
        for (int i = 0; i < esperado.length; i++) {
            if (dados[inicio + i] != esperado[i]) return false;
        }
        return true;
    }
}
