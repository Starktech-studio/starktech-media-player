// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import android.content.Context;

import androidx.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Limpar o cache (1.5, D3): só o temporário, as pastas de cache do app (a interna e a externa): imagens
 * baixadas (o app baixa de novo), cópias de legenda e o APK de atualização. Não toca em
 * {@code files/} (miniaturas, fotos de artista, capas de música, imagens escolhidas) nem no banco.
 */
public final class Cache {

    private Cache() {}

    /** As pastas de cache do app que existem. */
    static List<File> pastas(Context contexto) {
        List<File> pastas = new ArrayList<>();
        adicionar(pastas, contexto.getCacheDir());
        File[] externas = contexto.getExternalCacheDirs();
        if (externas != null) for (File f : externas) adicionar(pastas, f);
        return pastas;
    }

    private static void adicionar(List<File> pastas, @Nullable File pasta) {
        if (pasta != null && pasta.isDirectory()) pastas.add(pasta);
    }

    /** Tamanho do cache, em bytes. */
    public static long tamanho(Context contexto) {
        long total = 0;
        for (File pasta : pastas(contexto)) total += tamanhoDe(pasta);
        return total;
    }

    /** Apaga o que está dentro das pastas de cache (elas ficam) e devolve quanto liberou, em bytes. */
    public static long limpar(Context contexto) {
        long liberado = 0;
        for (File pasta : pastas(contexto)) liberado += apagarConteudo(pasta);
        return liberado;
    }

    static long tamanhoDe(File arquivo) {
        if (arquivo.isFile()) return arquivo.length();
        File[] itens = arquivo.listFiles();
        if (itens == null) return 0;
        long total = 0;
        for (File item : itens) total += tamanhoDe(item);
        return total;
    }

    /** Apaga o conteúdo de {@code pasta}, sem seguir link para fora dela; devolve os bytes apagados. */
    static long apagarConteudo(File pasta) {
        File[] itens = pasta.listFiles();
        if (itens == null) return 0;
        long liberado = 0;
        for (File item : itens) liberado += apagar(item);
        return liberado;
    }

    private static long apagar(File item) {
        long liberado = 0;
        if (item.isDirectory() && !ehLink(item)) liberado += apagarConteudo(item);
        long tamanho = item.isFile() ? item.length() : 0;
        if (item.delete()) liberado += tamanho;
        return liberado;
    }

    private static boolean ehLink(File item) {
        try {
            File pai = item.getParentFile();
            File canonico = pai == null ? item : new File(pai.getCanonicalFile(), item.getName());
            return !canonico.getCanonicalFile().equals(canonico.getAbsoluteFile());
        } catch (java.io.IOException e) {
            return true;
        }
    }
}
