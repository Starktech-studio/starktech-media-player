// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.StringRes;

import com.archos.mediacenter.video.R;

/**
 * Configurações da F1 (D16), em arquivo próprio ("st_preferencias"), sem tocar nas preferências do
 * Nova. Cada uma tem uma lista curta de valores que OK / toque percorre. F3: ligar às preferências
 * do player do Nova (áudio, legenda, retomar).
 */
public final class Preferencias {

    /** Configuração: chave, valores (textos) e o valor de fábrica (índice). */
    public enum Item {
        AUDIO("audio", 0, R.string.st_valor_original, R.string.st_valor_portugues, R.string.st_valor_ingles),
        LEGENDA("legenda", 0, R.string.st_valor_portugues, R.string.st_valor_ingles, R.string.st_valor_desligada),
        TAMANHO_LEGENDA("tamanho_legenda", 1, R.string.st_valor_pequena, R.string.st_valor_media, R.string.st_valor_grande),
        RETOMAR("retomar", 0, R.string.st_valor_ligado, R.string.st_valor_desligado);

        final String chave;
        final int padrao;
        @StringRes
        final int[] valores;

        Item(String chave, int padrao, @StringRes int... valores) {
            this.chave = chave;
            this.padrao = padrao;
            this.valores = valores;
        }
    }

    private static final String ARQUIVO = "st_preferencias";

    private Preferencias() {}

    private static SharedPreferences arquivo(Context contexto) {
        return contexto.getApplicationContext().getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE);
    }

    /** Texto do valor atual. */
    @StringRes
    public static int valor(Context contexto, Item item) {
        int i = arquivo(contexto).getInt(item.chave, item.padrao);
        return item.valores[Math.max(0, Math.min(item.valores.length - 1, i))];
    }

    /** Passa ao próximo valor (volta ao primeiro depois do último) e devolve o texto dele. */
    @StringRes
    public static int proximo(Context contexto, Item item) {
        int i = (arquivo(contexto).getInt(item.chave, item.padrao) + 1) % item.valores.length;
        arquivo(contexto).edit().putInt(item.chave, i).apply();
        return item.valores[i];
    }
}
