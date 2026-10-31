// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.dados;

import android.content.Context;
import android.content.SharedPreferences;

import br.com.starktechstudio.mediaplayer.dados.Modelo.TipoPasta;

/**
 * Estado das ações da F1 (favorito, assistido, posição, resultado do TMDB, tipo da pasta, HD),
 * num arquivo próprio: st_simulado. A edição à mão (campos, capa e fundo) está no banco do app
 * desde a F2 ({@code motor.Edicoes}).
 * Não toca nas preferências padrão nem no banco do Nova. Sai na F3.
 */
final class EstadoSimulado {

    static final String ARQUIVO = "st_simulado";

    private static final String HD_CONECTADO = "hd_conectado";

    private final SharedPreferences prefs;

    EstadoSimulado(Context contexto) {
        prefs = contexto.getApplicationContext().getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE);
    }

    // ---- Título / episódio ----

    boolean favorito(String id, boolean padrao) {
        return prefs.getBoolean("favorito_" + id, padrao);
    }

    void definirFavorito(String id, boolean valor) {
        prefs.edit().putBoolean("favorito_" + id, valor).apply();
    }

    boolean assistido(String id, boolean padrao) {
        return prefs.getBoolean("assistido_" + id, padrao);
    }

    void definirAssistido(String id, boolean valor) {
        prefs.edit().putBoolean("assistido_" + id, valor).apply();
    }

    int posicao(String id, int padrao) {
        return prefs.getInt("posicao_" + id, padrao);
    }

    void definirPosicao(String id, int segundos) {
        prefs.edit().putInt("posicao_" + id, segundos).apply();
    }

    // ---- Resultado do TMDB (a edição à mão fica na motor.Edicoes, desde a F2) ----

    String resultadoTmdb(String id) {
        return prefs.getString("tmdb_" + id, null);
    }

    void definirResultadoTmdb(String id, String idResultado) {
        prefs.edit().putString("tmdb_" + id, idResultado).apply();
    }

    /** Restaurar original: apaga o resultado do TMDB escolhido (a edição sai na motor.Edicoes). */
    void restaurarOriginal(String id) {
        prefs.edit().remove("tmdb_" + id).apply();
    }

    // ---- Pastas e HD ----

    TipoPasta tipoPasta(String idPasta, TipoPasta padrao) {
        String valor = prefs.getString("tipo_pasta_" + idPasta, null);
        if (valor == null) return padrao;
        try {
            return TipoPasta.valueOf(valor);
        } catch (IllegalArgumentException e) {
            return padrao;
        }
    }

    void definirTipoPasta(String idPasta, TipoPasta tipo) {
        prefs.edit().putString("tipo_pasta_" + idPasta, tipo.name()).apply();
    }

    boolean hdConectado() {
        return prefs.getBoolean(HD_CONECTADO, true);
    }

    void definirHdConectado(boolean conectado) {
        prefs.edit().putBoolean(HD_CONECTADO, conectado).apply();
    }
}
