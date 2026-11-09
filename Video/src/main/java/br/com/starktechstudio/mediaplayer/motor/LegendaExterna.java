// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Legenda em arquivo separado (.srt, .ass, .ssa, .vtt). Quem acha e lê é o motor: ao abrir o vídeo,
 * procura na pasta dele e na pasta de legendas do app os arquivos cujo nome começa com o do vídeo.
 * Aqui fica o que o motor não faz: separar o que é mesmo do vídeo ("Filme 2.srt" também vem para
 * "Filme.mkv"), dar nome à faixa pelo arquivo e levar para a pasta de legendas o que foi escolhido à mão.
 */
public final class LegendaExterna {

    private static final String[] EXTENSOES = {".srt", ".ass", ".ssa", ".vtt"};
    /** "por", "pt", "pt-BR", "en_US". */
    private static final Pattern CODIGO_DE_IDIOMA = Pattern.compile("[A-Za-z]{2,3}([-_][A-Za-z0-9]{2,4})?");
    private static final int NOME_MAX_BYTES = 255;

    private LegendaExterna() {}

    public static boolean ehLegenda(String nome) {
        String minusculo = nome.toLowerCase(Locale.ROOT);
        for (String extensao : EXTENSOES) {
            if (minusculo.endsWith(extensao)) return true;
        }
        return false;
    }

    /** "Filme.srt" e "Filme.por.srt" são de "Filme.mkv". */
    public static boolean doVideo(String nomeDoVideo, String caminhoDaLegenda) {
        String base = semExtensao(nomeDoVideo) + ".";
        return nomeDe(caminhoDaLegenda).regionMatches(true, 0, base, 0, base.length());
    }

    /**
     * O nome começa com o do vídeo, mas continua: "Filme 2.srt" em "Filme.mkv", "Episódio 10.srt" em
     * "Episódio 1.mkv". O arquivo temporário do motor (legenda em UTF-16) não entra aqui.
     */
    public static boolean deOutroVideo(String nomeDoVideo, String caminhoDaLegenda) {
        String base = semExtensao(nomeDoVideo);
        return nomeDe(caminhoDaLegenda).regionMatches(true, 0, base, 0, base.length())
                && !doVideo(nomeDoVideo, caminhoDaLegenda);
    }

    /** Idioma no nome do arquivo: "Filme.por.srt" → "por"; "Filme.srt" e "Filme.Dublado.srt" → vazio. */
    public static String idioma(String nomeDoVideo, String caminhoDaLegenda) {
        String resto = resto(nomeDoVideo, caminhoDaLegenda);
        String ultimo = resto.substring(resto.lastIndexOf('.') + 1);
        return CODIGO_DE_IDIOMA.matcher(ultimo).matches() ? ultimo : "";
    }

    /** Nome da faixa: o que o arquivo tem a mais que o vídeo; sem isso, o do motor ("SRT"). */
    public static String rotulo(String nomeDoVideo, String caminhoDaLegenda, String nomeDoMotor) {
        String resto = resto(nomeDoVideo, caminhoDaLegenda);
        if (!resto.isEmpty()) return resto;
        // O motor troca o idioma por um nome interno ("s_portuguese")
        return nomeDoMotor != null && nomeDoMotor.startsWith("s_") ? nomeDoMotor.substring(2) : nomeDoMotor;
    }

    /**
     * O arquivo que o motor vai ler: o próprio, se já está ao lado do vídeo com o nome dele; senão,
     * uma cópia na pasta de legendas, com o nome que o motor procura. Copiar para lá também faz a
     * escolha valer da próxima vez que o vídeo abrir.
     */
    public static File paraOMotor(File video, File legenda, File pastaDeLegendas) throws IOException {
        if (doVideo(video.getName(), legenda.getPath()) && legenda.getAbsoluteFile().getParentFile()
                .equals(video.getAbsoluteFile().getParentFile())) {
            return legenda;
        }
        File copia = new File(pastaDeLegendas, nomeDaCopia(video.getName(), legenda.getName()));
        try (InputStream de = new FileInputStream(legenda); OutputStream para = new FileOutputStream(copia)) {
            byte[] bloco = new byte[16 * 1024];
            int lidos;
            while ((lidos = de.read(bloco)) > 0) para.write(bloco, 0, lidos);
        } catch (IOException e) {
            copia.delete();
            throw e;
        }
        return copia;
    }

    /** Nome na pasta de legendas do app: o mesmo, se já é do vídeo ("Filme.por.srt"); senão, {@link #nomeDaCopia}. */
    public static String nomeNoMotor(String nomeDoVideo, String nomeDaLegenda) {
        return doVideo(nomeDoVideo, nomeDaLegenda) ? nomeDaLegenda : nomeDaCopia(nomeDoVideo, nomeDaLegenda);
    }

    /** "Filme.mkv" + "Minha legenda.SRT" → "Filme.Minha legenda.srt"; nome longo demais: "Filme.srt". */
    static String nomeDaCopia(String nomeDoVideo, String nomeDaLegenda) {
        String base = semExtensao(nomeDoVideo);
        String extensao = nomeDaLegenda.substring(semExtensao(nomeDaLegenda).length()).toLowerCase(Locale.ROOT);
        String nome = base + "." + semExtensao(nomeDaLegenda) + extensao;
        return nome.getBytes(StandardCharsets.UTF_8).length <= NOME_MAX_BYTES ? nome : base + extensao;
    }

    /** O que vem entre o nome do vídeo e a extensão: "Filme.por.srt" → "por"; não é do vídeo: vazio. */
    private static String resto(String nomeDoVideo, String caminhoDaLegenda) {
        if (!doVideo(nomeDoVideo, caminhoDaLegenda)) return "";
        String semFim = semExtensao(nomeDe(caminhoDaLegenda));
        int inicio = semExtensao(nomeDoVideo).length() + 1;
        return semFim.length() > inicio ? semFim.substring(inicio) : "";
    }

    private static String nomeDe(String caminho) {
        return caminho.substring(caminho.lastIndexOf('/') + 1);
    }

    private static String semExtensao(String nome) {
        int ponto = nome.lastIndexOf('.');
        return ponto > 0 ? nome.substring(0, ponto) : nome;
    }
}
