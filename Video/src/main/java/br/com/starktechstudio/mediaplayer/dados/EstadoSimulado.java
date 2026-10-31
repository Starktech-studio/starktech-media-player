// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.dados;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;

import br.com.starktechstudio.mediaplayer.dados.Modelo.Campo;
import br.com.starktechstudio.mediaplayer.dados.Modelo.TipoPasta;

/**
 * Estado das ações da F1 (favorito, assistido, posição, edição à mão, capa, fundo,
 * resultado do TMDB, tipo da pasta, HD), num arquivo próprio: st_simulado.
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

    // ---- Edição à mão ----

    /** Valor editado à mão, ou null se o campo não foi editado. */
    String campoEditado(String id, Campo campo) {
        return prefs.getString(chaveCampo(id, campo), null);
    }

    void definirCampoEditado(String id, Campo campo, String valor) {
        prefs.edit().putString(chaveCampo(id, campo), valor).apply();
    }

    String capa(String id) {
        return prefs.getString("capa_" + id, null);
    }

    void definirCapa(String id, int cor) {
        prefs.edit().putString("capa_" + id, Integer.toString(cor)).apply();
    }

    String fundo(String id) {
        return prefs.getString("fundo_" + id, null);
    }

    void definirFundo(String id, int cor) {
        prefs.edit().putString("fundo_" + id, Integer.toString(cor)).apply();
    }

    String resultadoTmdb(String id) {
        return prefs.getString("tmdb_" + id, null);
    }

    void definirResultadoTmdb(String id, String idResultado) {
        prefs.edit().putString("tmdb_" + id, idResultado).apply();
    }

    /** Escolha do TMDB troca capa e fundo: apaga as escolhas anteriores. */
    void limparCapaFundo(String id) {
        prefs.edit().remove("capa_" + id).remove("fundo_" + id).apply();
    }

    /** Restaurar original: apaga edição, capa, fundo e resultado do TMDB do título. */
    void restaurarOriginal(String id) {
        SharedPreferences.Editor editor = prefs.edit();
        for (Campo campo : Campo.values()) editor.remove(chaveCampo(id, campo));
        editor.remove("capa_" + id).remove("fundo_" + id).remove("tmdb_" + id).apply();
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

    private static String chaveCampo(String id, Campo campo) {
        return "editado_" + id + "_" + campo.name().toLowerCase(Locale.ROOT);
    }
}
