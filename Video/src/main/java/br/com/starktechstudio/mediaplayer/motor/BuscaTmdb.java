// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import com.archos.mediascraper.BaseTags;
import com.archos.mediascraper.EpisodeTags;
import com.archos.mediascraper.MovieTags;
import com.archos.mediascraper.ScrapeDetailResult;
import com.archos.mediascraper.ScrapeSearchResult;
import com.archos.mediascraper.ScrapeStatus;
import com.archos.mediascraper.Scraper;
import com.archos.mediascraper.ScraperImage;
import com.archos.mediascraper.SearchResult;
import com.archos.mediascraper.ShowTags;
import com.archos.mediascraper.VideoTags;
import com.archos.mediascraper.preprocess.SearchInfo;
import com.archos.mediascraper.preprocess.SearchPreprocessor;
import com.archos.mediascraper.xml.MovieScraper3;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Busca no TMDB (F3) pelo buscador do Nova ({@link Scraper}, com a chave do app —
 * {@link ChaveTmdb}): filme ou show pelo título e o ano, episódio pela série, temporada e número,
 * e a pesquisa livre de "Buscar no TMDB". Devolve as linhas do {@link Acervo} com título, sinopse,
 * gêneros, duração, elenco, coleção e as imagens já baixadas (capa e fundo; o resto fica como opção
 * do Editar). Tudo aqui usa a rede: só fora da thread da tela.
 */
public final class BuscaTmdb {

    /** Opções de capa e de fundo guardadas para o Editar. */
    private static final int OPCOES = 6;
    private static final int ELENCO = 15;
    private static final String IMAGEM_PEQUENA = "https://image.tmdb.org/t/p/w185";

    /** Um resultado de "Buscar no TMDB". */
    public static final class Resultado {
        /** "filme:123" ou "serie:456". */
        public final String id;
        public final String titulo;
        public final String ano;
        public final boolean serie;
        /** Endereço da capa pequena; nulo sem capa. */
        public final String capa;
        final SearchResult origem;

        Resultado(SearchResult origem) {
            this.origem = origem;
            serie = origem.isTvShow();
            id = (serie ? "serie:" : "filme:") + origem.getId();
            titulo = origem.getTitle();
            ano = origem.getYear() == null ? "" : origem.getYear();
            String caminho = origem.getPosterPath();
            capa = caminho == null || caminho.isEmpty() ? null
                    : caminho.startsWith("http") ? caminho : IMAGEM_PEQUENA + caminho;
        }
    }

    private final Context contexto;
    private final Scraper buscador;

    public BuscaTmdb(Context contexto) {
        this.contexto = contexto.getApplicationContext();
        this.buscador = new Scraper(this.contexto);
    }

    public boolean temChave() {
        return ChaveTmdb.temChave(contexto);
    }

    // ---- Busca automática ----

    /** Filme ou show pelo título e o ano (o arquivo só dá o caminho para o buscador). */
    public Acervo.Tmdb filme(String chave, String arquivo, String titulo, Integer ano) {
        Acervo.Tmdb linha = nova(chave);
        String texto = ano == null ? titulo : titulo + " (" + ano + ")";
        ScrapeDetailResult r = automatico(arquivo, texto);
        if (r.status != ScrapeStatus.OKAY || !(r.tag instanceof MovieTags)) {
            linha.situacao = situacaoDe(r.status);
            return linha;
        }
        preencherFilme(linha, (MovieTags) r.tag);
        return linha;
    }

    /**
     * Filme achado antes da versão 3 do banco: consulta a coleção uma vez, pelo id já guardado, sem
     * mudar o resto da linha (nem a escolha feita em "Buscar no TMDB"). Devolve false se o TMDB não
     * respondeu (a linha fica como estava, para tentar de novo); id que o TMDB não conhece fica sem coleção.
     */
    public boolean completarColecao(Acervo.Tmdb linha, String arquivo) {
        SearchResult pedido = new SearchResult(SearchResult.movie, linha.titulo, (int) linha.idTmdb);
        pedido.setFile(Uri.fromFile(new File(arquivo)));
        pedido.setScraper(new MovieScraper3(contexto));
        ScrapeDetailResult r = detalhes(pedido, new Bundle());
        if (r.status == ScrapeStatus.NOT_FOUND) {
            linha.colecaoId = Acervo.SEM_COLECAO;
            return true;
        }
        if (r.status != ScrapeStatus.OKAY || !(r.tag instanceof MovieTags)) return false;
        preencherColecao(linha, (MovieTags) r.tag);
        return true;
    }

    /**
     * Episódio pela série (nome e ano), temporada e número. {@code [0]} é a linha do episódio,
     * {@code [1]} a da série (nula se não achou a série ou sem {@code comSerie}: as imagens da
     * série só são baixadas uma vez).
     */
    public Acervo.Tmdb[] episodio(String chaveEpisodio, String chaveSerie, String arquivo, String serie, Integer ano,
                                  int temporada, int numero, boolean comSerie) {
        Acervo.Tmdb ep = nova(chaveEpisodio);
        ep.filme = false;
        String nome = ano == null ? serie : serie + " " + ano;
        String texto = String.format(Locale.ROOT, "%s S%02dE%02d", nome, temporada, numero);
        ScrapeDetailResult r = automatico(arquivo, texto);
        if (r.status != ScrapeStatus.OKAY || !(r.tag instanceof EpisodeTags)) {
            ep.situacao = situacaoDe(r.status);
            return new Acervo.Tmdb[]{ep, null};
        }
        EpisodeTags tags = (EpisodeTags) r.tag;
        ep.situacao = Acervo.ACHADO;
        ep.idTmdb = tags.getOnlineId();
        ep.nomeEpisodio = tags.getTitle();
        ep.sinopse = tags.getPlot();
        ep.duracaoMin = (int) tags.getRuntime(TimeUnit.MINUTES);
        ep.miniatura = baixar(tags.getEpisodePicture());
        Acervo.Tmdb linhaSerie = null;
        ShowTags show = tags.getShowTags();
        if (comSerie && show != null) {
            linhaSerie = nova(chaveSerie);
            preencherSerie(linhaSerie, show);
        }
        return new Acervo.Tmdb[]{ep, linhaSerie};
    }

    // ---- Buscar no TMDB ----

    /** Pesquisa livre; {@code serie}: só séries. Lista vazia se nada achou (ou sem rede). */
    public List<Resultado> pesquisar(String consulta, boolean serie) {
        String texto = consulta == null ? "" : consulta.trim();
        if (texto.isEmpty()) return Collections.emptyList();
        // Como no Nova: " S1E1" faz o buscador procurar só séries
        ScrapeSearchResult r = pesquisa(serie ? texto + " S1E1" : texto);
        List<Resultado> lista = new ArrayList<>();
        if (r.status != ScrapeStatus.OKAY) return lista;
        for (SearchResult item : r.results) {
            if (item.isTvShow() == serie) lista.add(new Resultado(item));
            if (lista.size() >= 12) break;
        }
        return lista;
    }

    /** Dados completos do resultado escolhido, com {@code chave} (filme / show ou a série). */
    public Acervo.Tmdb escolher(Resultado escolhido, String chave) {
        Acervo.Tmdb linha = nova(chave);
        Bundle pedido = new Bundle();
        pedido.putBoolean(Scraper.ITEM_REQUEST_REFRESH_SHOW_METADATA, true);
        if (escolhido.serie) {
            pedido.putInt(Scraper.ITEM_REQUEST_SEASON, 1);
            pedido.putInt(Scraper.ITEM_REQUEST_EPISODE, 1);
        } else {
            pedido.putBoolean(Scraper.ITEM_REQUEST_BASIC_VIDEO, true);
        }
        ScrapeDetailResult r = detalhes(escolhido.origem, pedido);
        if (r.status != ScrapeStatus.OKAY || r.tag == null) {
            linha.situacao = situacaoDe(r.status);
            return linha;
        }
        if (r.tag instanceof MovieTags) {
            preencherFilme(linha, (MovieTags) r.tag);
        } else {
            ShowTags show = r.tag instanceof EpisodeTags ? ((EpisodeTags) r.tag).getShowTags()
                    : r.tag instanceof ShowTags ? (ShowTags) r.tag : null;
            if (show == null) {
                linha.situacao = Acervo.NAO_ACHADO;
                return linha;
            }
            preencherSerie(linha, show);
        }
        if (linha.titulo == null || linha.titulo.isEmpty()) linha.titulo = escolhido.titulo;
        linha.escolhido = true;
        return linha;
    }

    // ---- Montagem ----

    private Acervo.Tmdb nova(String chave) {
        Acervo.Tmdb linha = new Acervo.Tmdb();
        linha.chave = chave;
        linha.buscado = System.currentTimeMillis();
        return linha;
    }

    private void preencherFilme(Acervo.Tmdb linha, MovieTags tags) {
        linha.situacao = Acervo.ACHADO;
        linha.filme = true;
        linha.idTmdb = tags.getOnlineId();
        linha.titulo = tags.getTitle();
        linha.tituloOriginal = tags.getOriginalTitle();
        linha.ano = tags.getYear() > 0 ? String.valueOf(tags.getYear()) : null;
        linha.sinopse = tags.getPlot();
        linha.generos = generos(tags);
        linha.duracaoMin = (int) tags.getRuntime(TimeUnit.MINUTES);
        linha.elenco = elenco(tags);
        imagens(linha, tags);
        preencherColecao(linha, tags);
    }

    /**
     * Coleção do filme (D5): o id e o nome vêm sempre com os dados do filme; as imagens, o buscador só
     * baixa se o banco do Nova ainda não conhece a coleção (sem elas, a coletânea usa a capa de um filme).
     */
    private static void preencherColecao(Acervo.Tmdb linha, MovieTags tags) {
        String nome = tags.getCollectionName();
        if (tags.getCollectionId() <= 0 || nome == null || nome.trim().isEmpty()) {
            linha.colecaoId = Acervo.SEM_COLECAO;
            return;
        }
        linha.colecaoId = tags.getCollectionId();
        linha.colecaoNome = nome.trim();
        linha.colecaoCapa = arquivoBaixado(tags.getCollectionPosterLargeFile());
        linha.colecaoFundo = arquivoBaixado(tags.getCollectionBackdropLargeFile());
    }

    private static String arquivoBaixado(String caminho) {
        if (caminho == null) return null;
        File arquivo = new File(caminho);
        return arquivo.isFile() && arquivo.length() > 0 ? caminho : null;
    }

    private void preencherSerie(Acervo.Tmdb linha, ShowTags tags) {
        linha.situacao = Acervo.ACHADO;
        linha.filme = false;
        linha.idTmdb = tags.getOnlineId();
        linha.titulo = tags.getTitle();
        linha.tituloOriginal = tags.getOriginalTitle();
        if (tags.getPremiered() != null) {
            Calendar data = Calendar.getInstance();
            data.setTime(tags.getPremiered());
            linha.ano = String.valueOf(data.get(Calendar.YEAR));
        }
        linha.sinopse = tags.getPlot();
        linha.generos = generos(tags);
        linha.duracaoMin = (int) tags.getRuntime(TimeUnit.MINUTES);
        linha.elenco = elenco(tags);
        imagens(linha, tags);
    }

    private String generos(VideoTags tags) {
        String texto = tags.getGenresFormatted(contexto);
        return texto == null ? null : texto.trim();
    }

    private static List<String[]> elenco(BaseTags tags) {
        List<String[]> lista = new ArrayList<>();
        Map<String, String> atores = tags.getActors();
        if (atores == null) return lista;
        for (Map.Entry<String, String> ator : atores.entrySet()) {
            lista.add(new String[]{ator.getKey(), ator.getValue() == null ? "" : ator.getValue()});
            if (lista.size() >= ELENCO) break;
        }
        return lista;
    }

    /** Baixa a capa e o fundo principais; as outras ficam como endereço (opções do Editar). */
    private void imagens(Acervo.Tmdb linha, BaseTags tags) {
        linha.capa = baixar(tags.getDefaultPoster());
        linha.fundo = baixar(tags.getDefaultBackdrop());
        linha.capas = enderecos(tags.getPosters());
        linha.fundos = enderecos(tags.getBackdrops());
    }

    private static List<String> enderecos(List<ScraperImage> imagens) {
        List<String> lista = new ArrayList<>();
        if (imagens == null) return lista;
        for (ScraperImage imagem : imagens) {
            String endereco = imagem.getLargeUrl();
            if (endereco != null && !endereco.isEmpty() && !lista.contains(endereco)) lista.add(endereco);
            if (lista.size() >= OPCOES) break;
        }
        return lista;
    }

    /** Baixa a imagem (a grande; sem ela, a pequena); devolve o caminho ou nulo. */
    private String baixar(ScraperImage imagem) {
        if (imagem == null) return null;
        try {
            imagem.download(contexto);
        } catch (RuntimeException e) {
            return null;
        }
        File grande = imagem.getLargeFileF();
        if (grande != null && grande.isFile() && grande.length() > 0) return grande.getAbsolutePath();
        File pequena = imagem.getThumbFileF();
        if (pequena != null && pequena.isFile() && pequena.length() > 0) return pequena.getAbsolutePath();
        return null;
    }

    private static int situacaoDe(ScrapeStatus status) {
        return status == ScrapeStatus.NOT_FOUND || status == ScrapeStatus.OKAY ? Acervo.NAO_ACHADO : Acervo.ERRO;
    }

    // Erro inesperado do buscador (resposta estranha, nome que ele não entende) vira "erro": a
    // atualização segue com o próximo vídeo e tenta este de novo mais tarde

    private ScrapeDetailResult automatico(String arquivo, String texto) {
        try {
            return buscador.getAutoDetails(info(arquivo, texto));
        } catch (RuntimeException e) {
            return new ScrapeDetailResult(null, true, null, ScrapeStatus.ERROR, e);
        }
    }

    private ScrapeSearchResult pesquisa(String texto) {
        try {
            return buscador.getAllMatches(info(null, texto));
        } catch (RuntimeException e) {
            return new ScrapeSearchResult(null, true, ScrapeStatus.ERROR, e);
        }
    }

    private static ScrapeDetailResult detalhes(SearchResult resultado, Bundle pedido) {
        try {
            return Scraper.getDetails(resultado, pedido);
        } catch (RuntimeException e) {
            return new ScrapeDetailResult(null, true, null, ScrapeStatus.ERROR, e);
        }
    }

    /** O pedido ao buscador: o texto digitado (ou montado) vale mais que o nome do arquivo. */
    private static SearchInfo info(String arquivo, String texto) {
        Uri uri = arquivo != null ? Uri.fromFile(new File(arquivo)) : Uri.parse("/starktech.mkv");
        SearchInfo info = SearchPreprocessor.instance().parseFileBased(uri, Uri.parse("/" + texto.replace('/', ' ') + ".mkv"));
        info.setUserInput(texto);
        return info;
    }
}
