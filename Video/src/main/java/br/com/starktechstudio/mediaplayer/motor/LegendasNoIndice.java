// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Legenda em arquivo sem o acesso a todos os arquivos (1.1.1; TV que não tem a tela desse acesso, ou quem
 * não liberou). Do Android 11 em diante o app não lê o .srt pelo caminho, mas o índice de mídia do Android
 * mostra os arquivos de legenda (media_type = legenda) a quem tem a permissão de ler vídeos
 * (AccessChecker do MediaProvider). Daqui o app acha as legendas de uma pasta e copia a escolhida, ou as
 * do vídeo ao abrir, para a pasta de legendas do app, onde o motor lê. Com o acesso, nada disto roda.
 */
public final class LegendasNoIndice {

    private static final String LOG = "StarkTechLegendas";

    private LegendasNoIndice() {}

    /** Android 11 em diante, sem o acesso a todos os arquivos. */
    public static boolean precisa() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager();
    }

    /** As legendas que estão na {@code pasta} (sem as subpastas), pelo índice: caminho → endereço. Vazia antes do Android 11. */
    public static Map<String, Uri> naPasta(Context contexto, File pasta) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || pasta == null) return Collections.emptyMap();
        return consultar(contexto, pasta);
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private static Map<String, Uri> consultar(Context contexto, File pasta) {
        Map<String, Uri> achadas = new LinkedHashMap<>();
        String base = pasta.getAbsolutePath() + "/";
        String[] colunas = {MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.VOLUME_NAME};
        String selecao = MediaStore.Files.FileColumns.MEDIA_TYPE + " = " + MediaStore.Files.FileColumns.MEDIA_TYPE_SUBTITLE
                + " AND " + MediaStore.Files.FileColumns.DATA + " LIKE ? ESCAPE '\\'";
        try (Cursor linhas = contexto.getContentResolver().query(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL),
                colunas, selecao, new String[]{escapar(base) + "%"}, null)) {
            if (linhas == null) return achadas;
            while (linhas.moveToNext()) {
                String caminho = linhas.getString(1);
                // O LIKE não diferencia maiúscula: o caminho exato e só o que está direto na pasta
                if (caminho == null || !caminho.startsWith(base) || caminho.indexOf('/', base.length()) >= 0) continue;
                if (!LegendaExterna.ehLegenda(caminho)) continue;
                String volume = linhas.getString(2);
                achadas.put(caminho, MediaStore.Files.getContentUri(volume == null ? MediaStore.VOLUME_EXTERNAL : volume,
                        linhas.getLong(0)));
            }
        } catch (RuntimeException e) {
            Log.w(LOG, "índice: " + e.getClass().getSimpleName());
        }
        return achadas;
    }

    /** Abre a legenda: direto, se o app lê o arquivo; senão, pelo índice. */
    public static InputStream abrir(Context contexto, File legenda) throws IOException {
        if (legenda.canRead()) return new FileInputStream(legenda);
        Uri endereco = naPasta(contexto, legenda.getAbsoluteFile().getParentFile()).get(legenda.getAbsolutePath());
        if (endereco == null) throw new IOException("legenda fora do índice");
        InputStream entrada = contexto.getContentResolver().openInputStream(endereco);
        if (entrada == null) throw new IOException("índice sem o arquivo");
        return entrada;
    }

    /**
     * A legenda escolhida em "Carregar legenda": o que {@link LegendaExterna#paraOMotor} faz, e, quando o app não
     * lê o arquivo, a cópia vem pelo índice, com o nome que o motor procura.
     */
    public static File paraOMotor(Context contexto, File video, File legenda, File pastaDeLegendas) throws IOException {
        if (legenda.canRead()) return LegendaExterna.paraOMotor(video, legenda, pastaDeLegendas);
        File copia = new File(pastaDeLegendas, LegendaExterna.nomeNoMotor(video.getName(), legenda.getName()));
        try (InputStream de = abrir(contexto, legenda)) {
            copiar(de, copia);
        }
        return copia;
    }

    /**
     * Ao abrir o vídeo: as legendas da pasta com o nome dele ("Filme.srt", "Filme.por.srt"), que o motor não lê
     * sem o acesso, vão para a pasta de legendas do app, com o mesmo nome; as já trazidas ficam. Devolve
     * quantas copiou.
     */
    public static int trazerDoVideo(Context contexto, File video, File pastaDeLegendas) {
        if (!precisa()) return 0;
        int copiadas = 0;
        for (Map.Entry<String, Uri> item : naPasta(contexto, video.getAbsoluteFile().getParentFile()).entrySet()) {
            File original = new File(item.getKey());
            if (!LegendaExterna.doVideo(video.getName(), item.getKey()) || original.canRead()) continue;
            File copia = new File(pastaDeLegendas, original.getName());
            if (copia.isFile() && copia.length() > 0) continue;
            try (InputStream de = contexto.getContentResolver().openInputStream(item.getValue())) {
                if (de == null) continue;
                copiar(de, copia);
                copiadas++;
            } catch (IOException | RuntimeException e) {
                Log.w(LOG, "cópia: " + e.getClass().getSimpleName());
            }
        }
        return copiadas;
    }

    private static void copiar(InputStream de, File copia) throws IOException {
        File pasta = copia.getParentFile();
        if (pasta != null && !pasta.isDirectory() && !pasta.mkdirs()) throw new IOException("sem a pasta de legendas");
        try (OutputStream para = new FileOutputStream(copia)) {
            byte[] bloco = new byte[16 * 1024];
            int lidos;
            while ((lidos = de.read(bloco)) > 0) para.write(bloco, 0, lidos);
        } catch (IOException e) {
            //noinspection ResultOfMethodCallIgnored
            copia.delete();
            throw e;
        }
    }

    /** O caminho no LIKE, com % e _ como letras. */
    static String escapar(String caminho) {
        return caminho.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
