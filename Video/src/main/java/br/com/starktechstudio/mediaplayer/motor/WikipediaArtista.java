// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Texto e fotos do artista pela Wikipédia, sem chave: o artigo de nome igual e que fala de música
 * (senão, "Queen" podia virar a rainha ou o álbum). O texto só em português, a língua do app; as fotos,
 * do artigo em português e do em inglês, e só as que citam o artista no nome do arquivo, além da
 * principal do artigo. Texto em CC BY-SA e fotos do Wikimedia Commons: créditos em Sobre. Tudo aqui
 * usa a rede: só fora da thread da tela.
 */
public final class WikipediaArtista {

    /** O que o artigo traz. */
    public static final class Artigo {
        public final String titulo;
        /** "Banda britânica de rock"; nulo se o artigo não tem. */
        public final String resumo;
        public final String texto;

        Artigo(String titulo, String resumo, String texto) {
            this.titulo = titulo;
            this.resumo = resumo;
            this.texto = texto;
        }
    }

    private static final String ENDERECO_BUSCA = "https://%s.wikipedia.org/w/api.php?action=query&format=json&formatversion=2"
            + "&generator=search&gsrlimit=8&gsrsearch=%s&prop=extracts%%7Cdescription&exintro=1&explaintext=1";
    private static final String ENDERECO_FOTOS = "https://%s.wikipedia.org/api/rest_v1/page/media-list/%s";
    private static final int MAXIMO_FOTOS = 12;
    /** O artigo fala de música: pela descrição ou pelo começo do texto. */
    private static final Pattern DE_MUSICA = Pattern.compile("(?iu)\\b(banda|cantor|cantora|m[uú]sico|m[uú]sica|grupo musical"
            + "|rapper|compositor|compositora|dj|dupla|duo|trio|quarteto|orquestra|vocalista|guitarrista|pianista|baterista"
            + "|band|singer|musician|songwriter|musical group|composer|orchestra)\\b");
    /** Artigo que tem o nome do artista, mas é de outra coisa: "Bad Omens (álbum)", "Queen (desambiguação)". */
    private static final Pattern OUTRA_COISA = Pattern.compile("(?iu)\\((.*(desambigua|disambiguation|[aá]lbum|album|filme|film"
            + "|can[cç][aã]o|song|single|livro|book|s[eé]rie).*)\\)");
    private static final int COMECO_DO_TEXTO = 300;

    /** O artigo do artista em português; nulo se não há. */
    public Artigo artigo(String nome) throws IOException, JSONException {
        return artigo("pt", nome);
    }

    private static Artigo artigo(String idioma, String nome) throws IOException, JSONException {
        return lerArtigo(BuscaArtista.lerTexto(String.format(Locale.ROOT, ENDERECO_BUSCA, idioma, BuscaArtista.codificar(nome))), nome);
    }

    /** Endereços das fotos, sem repetir; o que não responder fica de fora. */
    public List<String> fotos(String nome) {
        List<String> fotos = new ArrayList<>();
        for (String idioma : new String[]{"pt", "en"}) {
            try {
                Artigo artigo = artigo(idioma, nome);
                if (artigo == null) continue;
                String pagina = BuscaArtista.codificar(artigo.titulo.replace(' ', '_'));
                for (String foto : lerFotos(BuscaArtista.lerTexto(String.format(Locale.ROOT, ENDERECO_FOTOS, idioma, pagina)), nome)) {
                    if (!fotos.contains(foto) && fotos.size() < MAXIMO_FOTOS) fotos.add(foto);
                }
            } catch (IOException | JSONException e) {
                // fica com o que já veio
            }
        }
        return fotos;
    }

    // ---- Respostas ----

    /** O primeiro resultado da busca com o nome do artista e que fala de música. */
    static Artigo lerArtigo(String json, String nome) throws JSONException {
        JSONObject consulta = new JSONObject(json).optJSONObject("query");
        JSONArray paginas = consulta == null ? null : consulta.optJSONArray("pages");
        if (paginas == null) return null;
        List<JSONObject> ordem = new ArrayList<>();
        for (int i = 0; i < paginas.length(); i++) ordem.add(paginas.getJSONObject(i));
        Collections.sort(ordem, (a, b) -> Integer.compare(a.optInt("index"), b.optInt("index")));
        for (JSONObject pagina : ordem) {
            String titulo = pagina.optString("title", "");
            if (OUTRA_COISA.matcher(titulo).find()) continue;
            if (!BuscaArtista.mesmoNome(nome, titulo.replaceAll("\\s*\\(.*\\)\\s*$", ""))) continue;
            String resumo = pagina.isNull("description") ? "" : pagina.optString("description", "").trim();
            String texto = pagina.isNull("extract") ? "" : pagina.optString("extract", "").trim();
            String comeco = texto.substring(0, Math.min(COMECO_DO_TEXTO, texto.length()));
            if (texto.isEmpty() || resumo.toLowerCase(Locale.ROOT).contains("desambigua")) continue;
            if (!DE_MUSICA.matcher(resumo).find() && !DE_MUSICA.matcher(comeco).find()) continue;
            return new Artigo(titulo, resumo.isEmpty() ? null : maiuscula(resumo), texto);
        }
        return null;
    }

    /** A foto principal do artigo e as que citam o artista no nome do arquivo; só JPEG (o resto costuma ser logo). */
    static List<String> lerFotos(String json, String nome) throws JSONException {
        List<String> fotos = new ArrayList<>();
        JSONArray itens = new JSONObject(json).optJSONArray("items");
        String artista = BuscaArtista.soLetras(nome);
        if (itens == null || artista.isEmpty()) return fotos;
        for (int i = 0; i < itens.length(); i++) {
            JSONObject item = itens.getJSONObject(i);
            String titulo = item.optString("title", "");
            String minusculo = titulo.toLowerCase(Locale.ROOT);
            if (!"image".equals(item.optString("type")) || !(minusculo.endsWith(".jpg") || minusculo.endsWith(".jpeg"))) continue;
            if (!item.optBoolean("leadImage") && !BuscaArtista.soLetras(titulo).contains(artista)) continue;
            JSONArray tamanhos = item.optJSONArray("srcset");
            String endereco = tamanhos == null || tamanhos.length() == 0 ? "" : tamanhos.getJSONObject(0).optString("src", "");
            if (endereco.startsWith("//")) endereco = "https:" + endereco;
            int parametros = endereco.indexOf('?');
            if (parametros > 0) endereco = endereco.substring(0, parametros);
            if (endereco.startsWith("https://") && !fotos.contains(endereco)) fotos.add(endereco);
        }
        return fotos;
    }

    private static String maiuscula(String texto) {
        return texto.substring(0, 1).toUpperCase(Locale.forLanguageTag("pt-BR")) + texto.substring(1);
    }
}
