// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Locale;

/**
 * Armazenamentos e pastas de verdade do aparelho, para os navegadores do app (F2): o
 * armazenamento interno e cada HD / pen drive montado, as subpastas e, quando pedido, só as
 * imagens (JPG, PNG, WebP pela extensão; o formato de verdade é conferido na cópia).
 */
public final class ArquivosDoAparelho {

    /** Um armazenamento: a pasta raiz e se é removível (HD externo, pen drive). */
    public static final class Raiz {
        public final File pasta;
        public final boolean removivel;

        Raiz(File pasta, boolean removivel) {
            this.pasta = pasta;
            this.removivel = removivel;
        }
    }

    private static final String[] EXTENSOES_IMAGEM = {".jpg", ".jpeg", ".png", ".webp"};

    private ArquivosDoAparelho() {}

    /** Permissão para ler os vídeos do aparelho (Android 13+: só vídeos; antes: o armazenamento). */
    public static String permissaoDeVideos() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_VIDEO : Manifest.permission.READ_EXTERNAL_STORAGE;
    }

    public static boolean podeLerVideos(Context contexto) {
        return ContextCompat.checkSelfPermission(contexto, permissaoDeVideos()) == PackageManager.PERMISSION_GRANTED;
    }

    /** Permissão para ler as músicas do aparelho (1.5, pastas de Músicas). */
    public static String permissaoDeMusicas() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_AUDIO : Manifest.permission.READ_EXTERNAL_STORAGE;
    }

    /** Com o acesso a todos os arquivos, a pasta inteira é legível sem a permissão de músicas. */
    public static boolean podeLerMusicas(Context contexto) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && android.os.Environment.isExternalStorageManager()) return true;
        return ContextCompat.checkSelfPermission(contexto, permissaoDeMusicas()) == PackageManager.PERMISSION_GRANTED;
    }

    /** Permissão para ler as imagens do aparelho (Editar: capa e fundo do aparelho). */
    public static String permissaoDeImagens() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;
    }

    /** Raiz do armazenamento em que o caminho está; nula se não está em nenhum montado. */
    public static Raiz raizDe(Context contexto, String caminho) {
        Raiz melhor = null;
        for (Raiz r : raizes(contexto)) {
            String raiz = r.pasta.getAbsolutePath();
            if ((caminho.equals(raiz) || caminho.startsWith(raiz + "/"))
                    && (melhor == null || raiz.length() > melhor.pasta.getAbsolutePath().length())) {
                melhor = r;
            }
        }
        return melhor;
    }

    /**
     * Interno primeiro, depois os removíveis montados: os volumes do {@link StorageManager} (pen
     * drive e HD USB entram aqui — a pasta do app em cada volume, {@code getExternalFilesDirs},
     * deixa os USB de fora) e, de reserva, os cartões pela pasta do app.
     */
    public static List<Raiz> raizes(Context contexto) {
        List<Raiz> lista = new ArrayList<>();
        Set<String> vistas = new HashSet<>();
        File primaria = Environment.getExternalStorageDirectory();
        if (primaria != null && primaria.isDirectory()) {
            lista.add(new Raiz(primaria, Environment.isExternalStorageRemovable()));
            vistas.add(primaria.getAbsolutePath());
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            StorageManager volumes = contexto.getSystemService(StorageManager.class);
            if (volumes != null) {
                for (StorageVolume volume : volumes.getStorageVolumes()) {
                    if (volume.isPrimary()) continue;
                    String estado = volume.getState();
                    if (!Environment.MEDIA_MOUNTED.equals(estado) && !Environment.MEDIA_MOUNTED_READ_ONLY.equals(estado)) continue;
                    File pasta = pastaDo(volume);
                    if (pasta != null && pasta.isDirectory() && vistas.add(pasta.getAbsolutePath())) {
                        lista.add(new Raiz(pasta, true));
                    }
                }
            }
        }
        for (File doApp : contexto.getExternalFilesDirs(null)) {
            if (doApp == null) continue;
            String raiz = raizDe(doApp.getAbsolutePath());
            if (raiz == null) continue;
            File pasta = new File(raiz);
            if (pasta.isDirectory() && vistas.add(pasta.getAbsolutePath())) lista.add(new Raiz(pasta, true));
        }
        return lista;
    }

    /** Pasta do volume: Android 11+ pela API; antes, pelo caminho do volume ("/storage/1A2B-3C4D"). */
    @RequiresApi(Build.VERSION_CODES.N)
    private static File pastaDo(StorageVolume volume) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) return volume.getDirectory();
        try {
            Object caminho = StorageVolume.class.getMethod("getPath").invoke(volume);
            if (caminho instanceof String) return new File((String) caminho);
        } catch (ReflectiveOperationException | RuntimeException ignorado) {
            // sem o método: tenta pela identificação do volume
        }
        String id = volume.getUuid();
        return id == null ? null : new File("/storage/" + id);
    }

    /** "HD / Filmes / 2026": a origem e o caminho a partir da raiz do armazenamento (sem armazenamento: o nome). */
    public static String exibir(Context contexto, String caminho) {
        Raiz raiz = raizDe(contexto, caminho);
        if (raiz == null) return new File(caminho).getName();
        StringBuilder texto = new StringBuilder(raiz.removivel ? "HD" : "Interno");
        String base = raiz.pasta.getAbsolutePath();
        String resto = caminho.length() > base.length() ? caminho.substring(base.length()) : "";
        for (String parte : resto.split("/")) {
            if (!parte.isEmpty()) texto.append(" / ").append(parte);
        }
        return texto.toString();
    }

    /** Pasta com o nome dado ("Imagens") na raiz de um armazenamento, HD primeiro; nula se não há. */
    public static File pastaNaRaiz(Context contexto, String nome) {
        String alvo = Nomes.normalizar(nome);
        for (boolean removiveis : new boolean[]{true, false}) {
            for (Raiz r : raizes(contexto)) {
                if (r.removivel != removiveis) continue;
                File[] itens = r.pasta.listFiles();
                if (itens == null) continue;
                for (File item : itens) {
                    if (item.isDirectory() && Nomes.normalizar(item.getName()).equals(alvo)) return item;
                }
            }
        }
        return null;
    }

    /** "/storage/1A2B-3C4D/Android/data/…/files" → "/storage/1A2B-3C4D". */
    static String raizDe(String pastaDoApp) {
        int corte = pastaDoApp.indexOf("/Android/data/");
        return corte > 0 ? pastaDoApp.substring(0, corte) : null;
    }

    /** Subpastas (sem as ocultas) e, se {@code soImagens}, as imagens; cada grupo em ordem alfabética. */
    public static List<File> listar(File pasta, boolean soImagens) {
        return listar(pasta, nome -> soImagens && ehImagem(nome));
    }

    /** Subpastas (sem as ocultas) e as legendas ({@link LegendaExterna#ehLegenda}), em ordem alfabética. */
    public static List<File> listarLegendas(File pasta) {
        return listar(pasta, LegendaExterna::ehLegenda);
    }

    private interface Filtro {
        boolean aceita(String nome);
    }

    private static List<File> listar(File pasta, Filtro arquivosAceitos) {
        File[] itens = pasta.listFiles();
        if (itens == null) return Collections.emptyList();
        List<File> pastas = new ArrayList<>();
        List<File> arquivos = new ArrayList<>();
        for (File item : itens) {
            if (item.getName().startsWith(".")) continue;
            if (item.isDirectory()) pastas.add(item);
            else if (arquivosAceitos.aceita(item.getName())) arquivos.add(item);
        }
        Collator ordem = Collator.getInstance(Locale.forLanguageTag("pt-BR"));
        ordem.setStrength(Collator.PRIMARY);
        Collections.sort(pastas, (a, b) -> ordem.compare(a.getName(), b.getName()));
        Collections.sort(arquivos, (a, b) -> ordem.compare(a.getName(), b.getName()));
        List<File> lista = new ArrayList<>(pastas);
        lista.addAll(arquivos);
        return lista;
    }

    static boolean ehImagem(String nome) {
        String minusculo = nome.toLowerCase(Locale.ROOT);
        for (String extensao : EXTENSOES_IMAGEM) {
            if (minusculo.endsWith(extensao)) return true;
        }
        return false;
    }
}
