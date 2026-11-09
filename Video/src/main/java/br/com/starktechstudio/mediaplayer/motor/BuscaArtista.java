// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import com.archos.mediacenter.video.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Capa de artista (aba Shows): TheAudioDB (foto, fundo e país), com o Deezer de reserva (só a foto).
 * A TheAudioDB pede chave, que entra no APK na hora do build, vinda da linha
 * {@code starktech.theaudiodb.chave=} do {@code ~/.gradle/gradle.properties} do WSL — nunca do
 * repositório; sem ela, só o Deezer, que responde sem chave. As imagens são baixadas uma vez, para
 * {@code files/artistas/}. Só aceita o artista de nome igual ({@link #mesmoNome}) e, entre os homônimos do
 * Deezer, o de mais fãs; se ainda vier errado, fica a troca da capa no Editar ({@link #fotosDeezer}) e a
 * busca pelo nome ({@link #artistasDeezer}). O texto vem da {@link WikipediaArtista}. Sem resposta, fica o
 * bloco de cor. Tudo aqui usa a rede: só fora da thread da tela.
 */
public final class BuscaArtista {

    public static final String THEAUDIODB = "theaudiodb";
    public static final String DEEZER = "deezer";

    private static final String LOG = "StarkTechVarredura";
    private static final String ENDERECO_THEAUDIODB = "https://www.theaudiodb.com/api/v1/json/%s/search.php?s=%s";
    /** 25: o artista de verdade costuma vir depois de vários homônimos pequenos ("Queen" vem em 6º). */
    private static final String ENDERECO_DEEZER = "https://api.deezer.com/search/artist?limit=25&q=%s";
    /** Identifica o app nos pedidos: a Wikipédia recusa quem não se identifica. */
    public static final String IDENTIFICACAO = "StarkTechMediaPlayer/1.0 (https://starktechstudio.com.br)";
    /** Homônimo com menos que esta parte dos fãs do maior não é o artista. */
    private static final int HOMONIMO_DIVISOR = 10;
    private static final int MAXIMO_RESULTADOS = 12;
    /** Limite da TheAudioDB na chave gratuita: 30 pedidos por minuto (o mesmo passo vale para o Deezer). */
    private static final long INTERVALO_MS = 2100;
    private static final long LIMITE_RESPOSTA = 2L * 1024 * 1024;

    /** O que uma fonte trouxe: endereços das imagens e o país. */
    static final class Achado {
        String foto;
        String fundo;
        String pais;
    }

    /** Artista da busca do Deezer, para escolher pelo nome e pela foto. */
    public static final class Encontrado {
        public final String nome;
        public final String foto;
        public final long fas;

        Encontrado(String nome, String foto, long fas) {
            this.nome = nome;
            this.foto = foto;
            this.fas = fas;
        }
    }

    private final File pasta;
    private final String chave;
    private long ultimoPedido;

    public BuscaArtista(Context contexto) {
        pasta = new File(contexto.getFilesDir(), "artistas");
        chave = contexto.getString(R.string.st_chave_theaudiodb).trim();
    }

    /**
     * Linha do artista: {@link Acervo#ACHADO} com a imagem já baixada, {@link Acervo#NAO_ACHADO} se as
     * fontes responderam sem ele, {@link Acervo#ERRO} se alguma não respondeu e nada veio.
     */
    public Acervo.Artista buscar(String chaveArtista, String nome) {
        Acervo.Artista linha = new Acervo.Artista();
        linha.chave = chaveArtista;
        linha.nome = nome;
        linha.buscado = System.currentTimeMillis();
        boolean falhou = false;

        Achado principal = null;
        if (!chave.isEmpty()) {
            try {
                principal = lerTheAudioDb(pedir(String.format(Locale.ROOT, ENDERECO_THEAUDIODB, chave, codificar(nome))), nome);
            } catch (IOException | JSONException e) {
                semResposta(THEAUDIODB, e);
                falhou = true;
            }
        }
        if (principal != null) {
            linha.pais = principal.pais;
            if (principal.fundo != null) {
                linha.fundo = baixar(principal.fundo);
                if (linha.fundo == null) falhou = true;
            }
            if (principal.foto != null) {
                // A menor da TheAudioDB (500 px) já serve para a capa; sem ela, a original
                linha.foto = baixar(principal.foto + "/medium");
                if (linha.foto == null) linha.foto = baixar(principal.foto);
                if (linha.foto != null) linha.fonte = THEAUDIODB;
                else falhou = true;
            }
        }
        if (linha.foto == null) {
            try {
                Achado reserva = lerDeezer(pedir(String.format(Locale.ROOT, ENDERECO_DEEZER, codificar(nome))), nome);
                if (reserva != null && reserva.foto != null) {
                    linha.foto = baixar(reserva.foto);
                    if (linha.foto != null) linha.fonte = DEEZER;
                    else falhou = true;
                }
            } catch (IOException | JSONException e) {
                semResposta(DEEZER, e);
                falhou = true;
            }
        }
        linha.situacao = linha.foto != null || linha.fundo != null ? Acervo.ACHADO : falhou ? Acervo.ERRO : Acervo.NAO_ACHADO;
        lerTextoDoArtista(linha, nome);
        return linha;
    }

    /** Texto do artista: sem ele a capa continua valendo, então falha aqui não conta como falha da busca. */
    private static void lerTextoDoArtista(Acervo.Artista linha, String nome) {
        try {
            WikipediaArtista.Artigo artigo = new WikipediaArtista().artigo(nome);
            if (artigo != null) {
                linha.descricao = artigo.texto;
                linha.resumo = artigo.resumo;
            }
        } catch (IOException | JSONException e) {
            semResposta("wikipedia", e);
        }
    }

    /** Fotos do Deezer para escolher no Editar ({@link #lerFotosDeezer}); nula = sem resposta. */
    public List<String> fotosDeezer(String nome) {
        try {
            return lerFotosDeezer(pedir(String.format(Locale.ROOT, ENDERECO_DEEZER, codificar(nome))), nome);
        } catch (IOException | JSONException e) {
            semResposta(DEEZER, e);
            return null;
        }
    }

    /** Busca livre por nome, para "Buscar artista" no Editar; vazia sem resposta. */
    public List<Encontrado> artistasDeezer(String consulta) {
        try {
            return lerArtistasDeezer(pedir(String.format(Locale.ROOT, ENDERECO_DEEZER, codificar(consulta))));
        } catch (IOException | JSONException e) {
            semResposta(DEEZER, e);
            return new ArrayList<>();
        }
    }

    /** O motivo sem o endereço: nele vai a chave. */
    private static void semResposta(String fonte, Exception e) {
        Log.w(LOG, "artista: sem resposta de " + fonte + " (" + e.getClass().getSimpleName() + ")");
    }

    // ---- Respostas ----

    /** {@code {"artists": [{...}]}} ou {@code {"artists": null}}; nulo se o artista não veio. */
    static Achado lerTheAudioDb(String json, String nome) throws JSONException {
        JSONArray lista = new JSONObject(json).optJSONArray("artists");
        if (lista == null) return null;
        for (int i = 0; i < lista.length(); i++) {
            JSONObject artista = lista.getJSONObject(i);
            if (!mesmoNome(nome, texto(artista, "strArtist")) && !mesmoNome(nome, texto(artista, "strArtistAlternate"))) continue;
            Achado achado = new Achado();
            achado.foto = endereco(texto(artista, "strArtistThumb"));
            achado.fundo = endereco(texto(artista, "strArtistFanart"));
            if (achado.fundo == null) achado.fundo = endereco(texto(artista, "strArtistWideThumb"));
            achado.pais = texto(artista, "strCountry");
            return achado;
        }
        return null;
    }

    /**
     * {@code {"data": [{...}], "total": n}}; nulo se o artista não veio. O Deezer responde erro (cota,
     * por exemplo) com HTTP 200 e {@code {"error": {...}}}: vale como sem resposta.
     */
    static Achado lerDeezer(String json, String nome) throws JSONException, IOException {
        JSONArray lista = dadosDeezer(json);
        if (lista == null) return null;
        // Vários artistas têm o mesmo nome, e o maior nem sempre vem primeiro: fica o de mais fãs
        JSONObject melhor = null;
        for (int i = 0; i < lista.length(); i++) {
            JSONObject artista = lista.getJSONObject(i);
            if (!mesmoNome(nome, texto(artista, "name"))) continue;
            if (melhor == null || artista.optLong("nb_fan") > melhor.optLong("nb_fan")) melhor = artista;
        }
        if (melhor == null) return null;
        Achado achado = new Achado();
        achado.foto = fotoDe(melhor);
        return achado;
    }

    /**
     * Fotos para escolher no Editar: só de nome igual e, entre os homônimos, os que têm público perto do
     * maior; o resto da busca e os homônimos pequenos não têm nada a ver com a banda.
     */
    static List<String> lerFotosDeezer(String json, String nome) throws JSONException, IOException {
        List<JSONObject> iguais = new ArrayList<>();
        long maior = 0;
        JSONArray lista = dadosDeezer(json);
        for (int i = 0; lista != null && i < lista.length(); i++) {
            JSONObject artista = lista.getJSONObject(i);
            if (!mesmoNome(nome, texto(artista, "name"))) continue;
            iguais.add(artista);
            maior = Math.max(maior, artista.optLong("nb_fan"));
        }
        Collections.sort(iguais, (a, b) -> Long.compare(b.optLong("nb_fan"), a.optLong("nb_fan")));
        List<String> fotos = new ArrayList<>();
        for (JSONObject artista : iguais) {
            String foto = fotoDe(artista);
            if (foto != null && !fotos.contains(foto) && artista.optLong("nb_fan") * HOMONIMO_DIVISOR >= maior) fotos.add(foto);
        }
        return fotos;
    }

    /** Os da busca, do mais conhecido ao menos, sem repetir foto e sem quem não tem foto. */
    static List<Encontrado> lerArtistasDeezer(String json) throws JSONException, IOException {
        List<Encontrado> achados = new ArrayList<>();
        List<String> fotos = new ArrayList<>();
        JSONArray lista = dadosDeezer(json);
        for (int i = 0; lista != null && i < lista.length(); i++) {
            JSONObject artista = lista.getJSONObject(i);
            String nome = texto(artista, "name");
            String foto = fotoDe(artista);
            if (nome == null || foto == null || fotos.contains(foto)) continue;
            fotos.add(foto);
            achados.add(new Encontrado(nome, foto, artista.optLong("nb_fan")));
        }
        Collections.sort(achados, (a, b) -> Long.compare(b.fas, a.fas));
        return achados.size() > MAXIMO_RESULTADOS ? new ArrayList<>(achados.subList(0, MAXIMO_RESULTADOS)) : achados;
    }

    private static JSONArray dadosDeezer(String json) throws JSONException, IOException {
        JSONObject raiz = new JSONObject(json);
        if (raiz.has("error")) throw new IOException("erro do Deezer");
        return raiz.optJSONArray("data");
    }

    private static String fotoDe(JSONObject artista) {
        String foto = fotoDeezer(texto(artista, "picture_big"));
        return foto != null ? foto : fotoDeezer(texto(artista, "picture_xl"));
    }

    /** Artista sem foto no Deezer vem com o endereço de uma imagem padrão, sem o código ("/artist//"). */
    private static String fotoDeezer(String endereco) {
        return endereco == null || endereco.contains("/artist//") ? null : endereco(endereco);
    }

    /** Só https. */
    private static String endereco(String texto) {
        return texto != null && texto.startsWith("https://") ? texto : null;
    }

    /** Valor de texto; JSON nulo e vazio viram nulo. */
    private static String texto(JSONObject objeto, String campo) {
        if (objeto.isNull(campo)) return null;
        String valor = objeto.optString(campo, "").trim();
        return valor.isEmpty() ? null : valor;
    }

    /**
     * Mesmo artista pelo nome, com grafias diferentes: sem acento, maiúscula, pontuação e "The" no começo;
     * "&" vale "and" ("AC/DC" = "ACDC", "The Beatles" = "Beatles", "Simon & Garfunkel" = "Simon and Garfunkel").
     */
    static boolean mesmoNome(String a, String b) {
        String x = soLetras(a);
        return !x.isEmpty() && x.equals(soLetras(b));
    }

    static String soLetras(String nome) {
        return Nomes.normalizar(nome).replace("&", " and ").replaceAll("^the\\s+", "").replaceAll("[^\\p{L}\\p{N}]", "");
    }

    // ---- Rede ----

    static String codificar(String nome) throws IOException {
        return URLEncoder.encode(nome, "UTF-8").replace("+", "%20");
    }

    private void esperarVez() throws IOException {
        long falta = ultimoPedido + INTERVALO_MS - SystemClock.elapsedRealtime();
        if (ultimoPedido > 0 && falta > 0) {
            try {
                Thread.sleep(falta);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("interrompido");
            }
        }
        ultimoPedido = SystemClock.elapsedRealtime();
    }

    private String pedir(String endereco) throws IOException {
        esperarVez();
        return lerTexto(endereco);
    }

    static String lerTexto(String endereco) throws IOException {
        HttpURLConnection conexao = null;
        try {
            conexao = (HttpURLConnection) new URL(endereco).openConnection();
            conexao.setConnectTimeout(15_000);
            conexao.setReadTimeout(20_000);
            conexao.setRequestProperty("User-Agent", IDENTIFICACAO);
            // Inclui o 429 (limite de pedidos): sem resposta agora, tenta de novo mais tarde
            if (conexao.getResponseCode() != HttpURLConnection.HTTP_OK) throw new IOException("HTTP " + conexao.getResponseCode());
            try (InputStream entrada = conexao.getInputStream()) {
                ByteArrayOutputStream memoria = new ByteArrayOutputStream();
                if (ImagemDoAparelho.copiar(entrada, memoria, LIMITE_RESPOSTA) < 0) throw new IOException("resposta grande demais");
                return memoria.toString("UTF-8");
            }
        } catch (RuntimeException e) {
            throw new IOException("resposta estranha", e);
        } finally {
            if (conexao != null) conexao.disconnect();
        }
    }

    /** Baixa a imagem para {@code files/artistas/}; devolve o caminho ou nulo. */
    private String baixar(String endereco) {
        HttpURLConnection conexao = null;
        try {
            conexao = (HttpURLConnection) new URL(endereco).openConnection();
            conexao.setConnectTimeout(15_000);
            conexao.setReadTimeout(30_000);
            if (conexao.getResponseCode() != HttpURLConnection.HTTP_OK) return null;
            try (InputStream entrada = conexao.getInputStream()) {
                ImagemDoAparelho.Resultado imagem = ImagemDoAparelho.importar(pasta, entrada, conexao.getContentLength());
                return imagem.ok() ? Edicoes.arquivoDe(imagem.referencia) : null;
            }
        } catch (IOException | RuntimeException e) {
            return null;
        } finally {
            if (conexao != null) conexao.disconnect();
        }
    }
}
