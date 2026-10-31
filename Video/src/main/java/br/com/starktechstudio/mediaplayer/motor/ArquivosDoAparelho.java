// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

    /** Interno primeiro, depois os removíveis montados (pela pasta do app em cada volume). */
    public static List<Raiz> raizes(Context contexto) {
        List<Raiz> lista = new ArrayList<>();
        File primaria = Environment.getExternalStorageDirectory();
        if (primaria != null && primaria.isDirectory()) {
            lista.add(new Raiz(primaria, Environment.isExternalStorageRemovable()));
        }
        for (File doApp : contexto.getExternalFilesDirs(null)) {
            if (doApp == null) continue;
            String raiz = raizDe(doApp.getAbsolutePath());
            if (raiz == null || (primaria != null && raiz.equals(primaria.getAbsolutePath()))) continue;
            File pasta = new File(raiz);
            if (pasta.isDirectory()) lista.add(new Raiz(pasta, true));
        }
        return lista;
    }

    /** "/storage/1A2B-3C4D/Android/data/…/files" → "/storage/1A2B-3C4D". */
    static String raizDe(String pastaDoApp) {
        int corte = pastaDoApp.indexOf("/Android/data/");
        return corte > 0 ? pastaDoApp.substring(0, corte) : null;
    }

    /** Subpastas (sem as ocultas) e, se {@code soImagens}, as imagens; cada grupo em ordem alfabética. */
    public static List<File> listar(File pasta, boolean soImagens) {
        File[] itens = pasta.listFiles();
        if (itens == null) return Collections.emptyList();
        List<File> pastas = new ArrayList<>();
        List<File> imagens = new ArrayList<>();
        for (File item : itens) {
            if (item.getName().startsWith(".")) continue;
            if (item.isDirectory()) pastas.add(item);
            else if (soImagens && ehImagem(item.getName())) imagens.add(item);
        }
        Collator ordem = Collator.getInstance(Locale.forLanguageTag("pt-BR"));
        ordem.setStrength(Collator.PRIMARY);
        Collections.sort(pastas, (a, b) -> ordem.compare(a.getName(), b.getName()));
        Collections.sort(imagens, (a, b) -> ordem.compare(a.getName(), b.getName()));
        List<File> lista = new ArrayList<>(pastas);
        lista.addAll(imagens);
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
