// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import java.text.Normalizer;
import java.util.Locale;

/** Comparação de nomes de pasta e arquivo (D4): sem diferenciar maiúscula, minúscula e acento. */
final class Nomes {

    /** Extensões de vídeo que entram na biblioteca. */
    private static final String[] VIDEO = {
            "mkv", "mp4", "m4v", "avi", "mov", "wmv", "webm", "ts", "m2ts", "mts", "mpg", "mpeg", "flv", "ogv", "3gp", "vob"
    };

    private Nomes() {}

    /** "Séries " → "series"; espaços repetidos viram um só. */
    static String normalizar(String nome) {
        if (nome == null) return "";
        String semAcento = Normalizer.normalize(nome, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    /** "Tears.of.Steel_2012" → "Tears of Steel 2012": ponto e sublinhado viram espaço quando o nome não tem espaço. */
    static String limpar(String nome) {
        String limpo = nome.trim();
        if (!limpo.contains(" ")) limpo = limpo.replace('.', ' ').replace('_', ' ');
        return limpo.replaceAll("\\s+", " ").trim();
    }

    static String semExtensao(String arquivo) {
        int ponto = arquivo.lastIndexOf('.');
        return ponto > 0 ? arquivo.substring(0, ponto) : arquivo;
    }

    static boolean ehVideo(String arquivo) {
        int ponto = arquivo.lastIndexOf('.');
        if (ponto < 0) return false;
        String extensao = arquivo.substring(ponto + 1).toLowerCase(Locale.ROOT);
        for (String v : VIDEO) {
            if (v.equals(extensao)) return true;
        }
        return false;
    }
}
