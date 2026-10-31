// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import android.content.Context;

/**
 * Chave da API do TMDB (F2, etapa 4): entra no APK na hora do build, vinda da linha
 * {@code starktech.tmdb.chave=} do {@code ~/.gradle/gradle.properties} do WSL — nunca do
 * repositório. Sem ela, a busca no TMDB avisa que está sem chave em vez de tentar (o TMDB
 * responderia "sem autorização").
 */
public final class ChaveTmdb {

    private ChaveTmdb() {}

    public static boolean temChave(Context contexto) {
        String chave = contexto.getString(com.archos.medialib.R.string.tmdb_api_key);
        return chave != null && !chave.trim().isEmpty();
    }
}
