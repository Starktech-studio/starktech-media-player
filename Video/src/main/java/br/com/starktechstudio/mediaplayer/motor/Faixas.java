// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import com.archos.mediacenter.video.utils.VideoMetadata;

import java.util.Locale;

/**
 * Textos das faixas do arquivo (F3), iguais no Detalhe e no player: vídeo ("H.265 · HDR10"),
 * áudio ("Inglês · Dolby Digital 5.1") e legenda ("Português"), com o idioma em português.
 */
public final class Faixas {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private Faixas() {}

    /** Formato do vídeo e HDR; vazio se não deu para ler. */
    public static String video(VideoMetadata.VideoTrack faixa) {
        return faixa == null ? "" : video(faixa.format, faixa.colorTrc);
    }

    /** {@code transferencia}: AVCOL_TRC do motor (16 = PQ / HDR10, 18 = HLG; 0 = não se sabe). */
    public static String video(String formatoDoArquivo, int transferencia) {
        String formato = formatoVideo(formatoDoArquivo);
        String hdr = transferencia == 16 ? "HDR10" : transferencia == 18 ? "HLG" : "";
        if (formato.isEmpty()) return hdr;
        return hdr.isEmpty() ? formato : formato + " · " + hdr;
    }

    public static String audio(VideoMetadata.AudioTrack faixa, int indice) {
        if (faixa == null) return "Faixa " + (indice + 1);
        return audio(faixa.name, faixa.format, faixa.channels, faixa.language, indice);
    }

    /** Pelos textos do motor (o leitor de dados entrega assim; o player, pelo {@link VideoMetadata}). */
    public static String audio(String nome, String formatoDoArquivo, String canaisDoArquivo, String idiomaDoArquivo, int indice) {
        String idioma = idioma(idiomaDoArquivo);
        String formato = formatoAudio(formatoDoArquivo);
        String canais = canais(canaisDoArquivo);
        StringBuilder detalhe = new StringBuilder(formato);
        if (!canais.isEmpty()) detalhe.append(detalhe.length() > 0 ? " " : "").append(canais);
        if (idioma.isEmpty()) idioma = limpo(nome);
        if (idioma.isEmpty()) idioma = "Faixa " + (indice + 1);
        return detalhe.length() == 0 ? idioma : idioma + " · " + detalhe;
    }

    public static String legenda(VideoMetadata.SubtitleTrack faixa, int indice) {
        if (faixa == null) return "Legenda " + (indice + 1);
        return legenda(faixa.name, faixa.language, faixa.isExternal, indice);
    }

    public static String legenda(String nome, String idiomaDoArquivo, boolean externa, int indice) {
        String idioma = idioma(idiomaDoArquivo);
        if (idioma.isEmpty()) idioma = limpo(nome);
        if (idioma.isEmpty()) idioma = "Legenda " + (indice + 1);
        return externa ? idioma + " · arquivo" : idioma;
    }

    /** Legenda em arquivo, pelo nome dele: "Filme.por.srt" → "Português · arquivo". */
    public static String legendaEmArquivo(String nomeDoVideo, String caminho, String nomeDoMotor, int indice) {
        return legenda(LegendaExterna.rotulo(nomeDoVideo, caminho, nomeDoMotor),
                LegendaExterna.idioma(nomeDoVideo, caminho), true, indice);
    }

    /** "por", "pt", "pt-BR" → "Português"; vazio se não for um idioma. */
    public static String idioma(String codigo) {
        String c = limpo(codigo);
        if (c.isEmpty() || c.equalsIgnoreCase("und")) return "";
        String nome = Locale.forLanguageTag(c.split("[-_]")[0]).getDisplayLanguage(PT_BR);
        if (nome == null || nome.isEmpty() || nome.equalsIgnoreCase(c)) return "";
        return nome.substring(0, 1).toUpperCase(PT_BR) + nome.substring(1);
    }

    /** O código é de português ("por", "pt", "pob")? */
    public static boolean ehPortugues(String codigo) {
        String c = limpo(codigo).toLowerCase(Locale.ROOT);
        return c.startsWith("pt") || c.startsWith("por") || c.equals("pob");
    }

    public static boolean ehIngles(String codigo) {
        String c = limpo(codigo).toLowerCase(Locale.ROOT);
        return c.startsWith("en");
    }

    /** "1920×1080" → "1080p"; 3840 → "4K". */
    public static String qualidade(int largura, int altura) {
        if (largura <= 0 || altura <= 0) return "";
        if (largura >= 3200 || altura >= 2000) return "4K";
        if (largura >= 1800 || altura >= 1000) return "1080p";
        if (largura >= 1200 || altura >= 700) return "720p";
        return "SD";
    }

    private static String formatoVideo(String formato) {
        String f = limpo(formato).toUpperCase(Locale.ROOT);
        if (f.isEmpty()) return "";
        if (f.contains("HEVC") || f.contains("265")) return "H.265";
        if (f.contains("264") || f.contains("AVC")) return "H.264";
        if (f.contains("AV1")) return "AV1";
        if (f.contains("VP9")) return "VP9";
        if (f.contains("VP8")) return "VP8";
        if (f.contains("MPEG2") || f.contains("MPEG-2")) return "MPEG-2";
        if (f.contains("MPEG4") || f.contains("MPEG-4") || f.contains("XVID") || f.contains("DIVX")) return "MPEG-4";
        return f;
    }

    private static String formatoAudio(String formato) {
        String f = limpo(formato).toUpperCase(Locale.ROOT).replace("-", "");
        if (f.isEmpty()) return "";
        if (f.contains("TRUEHD")) return "Dolby TrueHD";
        if (f.contains("EAC3")) return "Dolby Digital Plus";
        if (f.contains("AC3")) return "Dolby Digital";
        if (f.contains("DTS")) return "DTS";
        if (f.contains("AAC")) return "AAC";
        if (f.contains("FLAC")) return "FLAC";
        if (f.contains("OPUS")) return "Opus";
        if (f.contains("VORBIS")) return "Vorbis";
        if (f.contains("MP3") || f.contains("MPEG")) return "MP3";
        if (f.contains("PCM")) return "PCM";
        return limpo(formato);
    }

    /** "2" / "stereo" → "Estéreo"; "6" / "5.1" → "5.1". */
    private static String canais(String canais) {
        String c = limpo(canais).toLowerCase(Locale.ROOT);
        if (c.isEmpty()) return "";
        if (c.equals("1") || c.contains("mono")) return "Mono";
        if (c.equals("2") || c.contains("stereo") || c.equals("2.0")) return "Estéreo";
        if (c.equals("6") || c.contains("5.1")) return "5.1";
        if (c.equals("8") || c.contains("7.1")) return "7.1";
        return limpo(canais);
    }

    private static String limpo(String texto) {
        return texto == null ? "" : texto.trim();
    }
}
