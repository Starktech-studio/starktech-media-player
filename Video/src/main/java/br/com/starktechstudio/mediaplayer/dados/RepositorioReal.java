// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.dados;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.archos.mediacenter.video.R;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.text.Collator;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import br.com.starktechstudio.mediaplayer.comum.Preferencias;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Campo;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Capitulo;
import br.com.starktechstudio.mediaplayer.dados.Modelo.DadosTecnicos;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Episodio;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Faixa;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Origem;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Pasta;
import br.com.starktechstudio.mediaplayer.dados.Modelo.PreviaTmdb;
import br.com.starktechstudio.mediaplayer.dados.Modelo.PessoaElenco;
import br.com.starktechstudio.mediaplayer.dados.Modelo.ResultadoTmdb;
import br.com.starktechstudio.mediaplayer.dados.Modelo.SecaoShows;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Temporada;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Tipo;
import br.com.starktechstudio.mediaplayer.dados.Modelo.TipoFaixa;
import br.com.starktechstudio.mediaplayer.dados.Modelo.TipoPasta;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Titulo;
import br.com.starktechstudio.mediaplayer.motor.Acervo;
import br.com.starktechstudio.mediaplayer.motor.ArquivosDoAparelho;
import br.com.starktechstudio.mediaplayer.motor.BuscaArtista;
import br.com.starktechstudio.mediaplayer.motor.BuscaTmdb;
import br.com.starktechstudio.mediaplayer.motor.Capitulos;
import br.com.starktechstudio.mediaplayer.motor.Classificador;
import br.com.starktechstudio.mediaplayer.motor.Edicoes;
import br.com.starktechstudio.mediaplayer.motor.Faixas;
import br.com.starktechstudio.mediaplayer.motor.ImagemDoAparelho;
import br.com.starktechstudio.mediaplayer.motor.Pastas;
import br.com.starktechstudio.mediaplayer.motor.Varredura;
import br.com.starktechstudio.mediaplayer.motor.WikipediaArtista;

/**
 * Biblioteca real (F3): as pastas escolhidas em Pastas ({@link Pastas}), varridas e classificadas
 * pelo tipo da pasta ({@link Varredura}, {@link Classificador}), com os dados técnicos lidos pelo
 * motor, os dados do TMDB ({@link BuscaTmdb}), a edição à mão ({@link Edicoes}) e o estado
 * (favorito, assistido, posição), tudo no banco do app ({@link Acervo}).
 *
 * <p>As telas leem um retrato na memória, montado fora da tela a cada mudança; a edição e o estado
 * entram na hora da leitura. Varredura e TMDB rodam numa thread só, em ordem (lista das pastas, TMDB,
 * leitura dos arquivos, quadros), ao abrir o app, em "Atualizar biblioteca", ao mudar uma pasta e ao
 * ligar um HD; HD desligado esconde os títulos dele.
 */
public final class RepositorioReal implements RepositorioBiblioteca {

    /** Bloco de cor de quem está sem imagem (as cores do desenho), pelo id. */
    private static final int[] CORES = {
            cor("#172554"), cor("#083344"), cor("#1e1b4b"), cor("#1e293b"), cor("#134e4a"),
            cor("#2e1065"), cor("#0c4a6e"), cor("#3b0764"), cor("#451a03"), cor("#4c0519")
    };
    /** Erro de rede no TMDB: tenta de novo depois deste tempo. */
    private static final long NOVA_TENTATIVA_MS = 10 * 60 * 1000L;
    /** Pasta veio sem vídeos com o HD ligado (erro de leitura): varre de novo, até 3 vezes seguidas. */
    private static final long NOVA_VARREDURA_MS = 30 * 1000L;
    private static final int VARREDURAS_DE_NOVO = 3;
    /** Voltar ao app varre de novo se o HD mudou ou se a última varredura foi há mais que isto. */
    private static final long CONFERIR_MS = 60 * 1000L;
    private static final int DESTAQUES = 5;
    private static final String LOG = "StarkTechVarredura";
    private static final int RECENTES = 15;
    private static final long AVISO_MS = 300;
    /** Idiomas de legenda no resumo do Detalhe. */
    private static final int LEGENDAS_NO_RESUMO = 3;
    /** Filmes da mesma coleção para virar uma capa só (D5). */
    private static final int FILMES_NA_COLETANEA = 2;

    private static RepositorioReal instancia;

    private final Context app;
    private final Acervo acervo;
    private final Pastas pastas;
    private final Edicoes edicoes;
    private final Map<String, Acervo.Estado> estados;
    private final ExecutorService trabalho = Executors.newSingleThreadExecutor(r -> new Thread(r, "StarkTechBiblioteca"));
    /** Leitura de um vídeo aberto na tela, sem esperar a fila da varredura. */
    private final ExecutorService agora = Executors.newSingleThreadExecutor(r -> new Thread(r, "StarkTechLerAgora"));
    private final Set<String> lendoAgora = ConcurrentHashMap.newKeySet();
    private final Set<String> quadrosPedidos = ConcurrentHashMap.newKeySet();
    private final Set<String> artigosPedidos = ConcurrentHashMap.newKeySet();
    /** Quantos saltos seguir nas pastas juntadas (A em B, B em C); o limite também corta volta sem fim. */
    private static final int MAXIMO_JUNTADOS = 8;
    /** Pastas juntadas no Editar: chave do artista → nome do artista que vale. */
    private volatile Map<String, String> apelidos = Collections.emptyMap();
    /** Capa de artista, um pedido a cada 2,1 s: à parte, sem segurar a leitura e os quadros; uma busca por vez. */
    private final ExecutorService filaDeArtistas = Executors.newSingleThreadExecutor(r -> new Thread(r, "StarkTechArtistas"));
    /** Busca de artista esperando a vez: pedido repetido não empilha. */
    private final AtomicBoolean artistasNaFila = new AtomicBoolean();
    /** A próxima busca de artista também tenta quem não achou ou perdeu a imagem. */
    private final AtomicBoolean artistasTudo = new AtomicBoolean();
    /** Só um monta o retrato por vez: as filas correm juntas, e um mais velho cobriria o novo. */
    private final Object montagem = new Object();
    private final Handler tela = new Handler(Looper.getMainLooper());
    private final List<Runnable> ouvintes = new CopyOnWriteArrayList<>();
    /** Resultados da última "Buscar no TMDB", para o Selecionar. */
    private final Map<String, BuscaTmdb.Resultado> resultados = new ConcurrentHashMap<>();
    /** Dados do resultado escolhido no Editar (por título), ainda não salvos: o Salvar usa sem buscar de novo. */
    private final Map<String, Acervo.Tmdb> previas = new ConcurrentHashMap<>();
    private volatile Retrato retrato;
    private boolean emAndamento;
    private volatile boolean repetir;
    /** Atualizar biblioteca: tenta de novo também o que o TMDB não achou ou veio sem capa. */
    private volatile boolean buscarTudo;
    /** Atualizar biblioteca ou HD ligado: mostra o resultado mesmo sem nada novo. */
    private volatile boolean mostrarResultado;
    private volatile String resultado;
    /** Só na thread de {@link #trabalho}. */
    private int varredurasDeNovo;
    private int atualizadosNaRodada;
    private volatile long ultimaVarredura;
    /** Andamento da atualização: passo (0 TMDB, 1 leitura, 2 quadros) e quanto dele já foi. */
    private volatile int passo;
    private volatile int feitosNoPasso;
    private volatile int totalDoPasso;
    private volatile int ultimoAvisado = -1;
    private final List<Runnable> ouvintesDoAndamento = new CopyOnWriteArrayList<>();
    /** O TMDB não respondeu ao completar uma coleção: os outros ficam para a próxima rodada. Só na thread de {@link #trabalho}. */
    private boolean colecaoSemResposta;

    public static synchronized RepositorioReal instancia(Context contexto) {
        if (instancia == null) {
            instancia = new RepositorioReal(contexto.getApplicationContext());
            // "Atualizar ao abrir" desligado: abre com o que já está no banco
            if (Preferencias.atualizarAoAbrir(contexto)) {
                instancia.indexarPastas();
                instancia.atualizarBiblioteca();
            }
        }
        return instancia;
    }

    private RepositorioReal(Context app) {
        this.app = app;
        acervo = Acervo.de(app);
        pastas = Pastas.de(app);
        edicoes = Edicoes.de(app);
        apelidos = edicoes.doCampo(Edicoes.Campo.ARTISTA);
        estados = new ConcurrentHashMap<>(acervo.estados());
        retrato = montar();
        ouvirHd();
    }

    // =====================================================================
    // Retrato
    // =====================================================================

    /** O que as telas leem: títulos, episódios e dados técnicos, sem estado e sem edição. */
    private static final class Retrato {
        final Map<String, Titulo> titulos = new LinkedHashMap<>();
        final Map<String, Episodio> episodios = new HashMap<>();
        final Map<String, List<Temporada>> temporadas = new HashMap<>();
        final Map<String, Acervo.Video> videos = new HashMap<>();
        final Map<String, Acervo.Tmdb> tmdb = new HashMap<>();
        final Map<String, Acervo.Artista> artistas = new HashMap<>();
        final Map<String, Pastas.Pasta> pastas = new LinkedHashMap<>();
        /** Pastas com o HD ligado quando o retrato foi montado. */
        final Set<String> montadas = new HashSet<>();
        /** Contagem por pasta: {vídeos, séries, episódios}, com o HD ligado ou não. */
        final Map<String, int[]> contagem = new HashMap<>();
        final List<String> naoIdentificados = new ArrayList<>();
    }

    private Set<String> pastasMontadas() {
        Set<String> montadas = new HashSet<>();
        for (Pastas.Pasta p : pastas.lista()) {
            if (new File(p.caminho).isDirectory()) montadas.add(p.caminho);
        }
        return montadas;
    }

    private Retrato montar() {
        Retrato r = new Retrato();
        for (Pastas.Pasta p : pastas.lista()) r.pastas.put(p.caminho, p);
        r.tmdb.putAll(acervo.tmdb());
        r.artistas.putAll(acervo.artistas());
        r.montadas.addAll(pastasMontadas());

        Map<String, List<Acervo.Video>> porSerie = new LinkedHashMap<>();
        Map<String, Set<String>> seriesPorPasta = new HashMap<>();
        for (Acervo.Video v : acervo.videos()) {
            int[] conta = r.contagem.get(v.pasta);
            if (conta == null) {
                conta = new int[3];
                r.contagem.put(v.pasta, conta);
            }
            if (v.tipo == Classificador.Tipo.EPISODIO) {
                conta[2]++;
                Set<String> series = seriesPorPasta.get(v.pasta);
                if (series == null) {
                    series = new HashSet<>();
                    seriesPorPasta.put(v.pasta, series);
                }
                if (series.add(Acervo.chaveSerie(v.serie))) conta[1]++;
            } else {
                conta[0]++;
            }
            // HD desligado: os títulos dele somem até ligar de novo
            if (!r.montadas.contains(v.pasta)) continue;
            r.videos.put(v.caminho, v);
            switch (v.tipo) {
                case FILME:
                case DESENHO:
                case SHOW:
                case STANDUP:
                    r.titulos.put(v.caminho, tituloDoVideo(r, v));
                    break;
                case EPISODIO: {
                    String chave = Acervo.chaveSerie(v.serie);
                    List<Acervo.Video> lista = porSerie.get(chave);
                    if (lista == null) {
                        lista = new ArrayList<>();
                        porSerie.put(chave, lista);
                    }
                    lista.add(v);
                    break;
                }
                default:
                    r.titulos.put(v.caminho, tituloDoVideo(r, v));
                    r.naoIdentificados.add(v.caminho);
                    break;
            }
        }
        for (Map.Entry<String, List<Acervo.Video>> serie : porSerie.entrySet()) {
            montarSerie(r, serie.getKey(), serie.getValue());
        }
        return r;
    }

    private Titulo tituloDoVideo(Retrato r, Acervo.Video v) {
        Acervo.Tmdb dados = achado(r.tmdb.get(v.caminho));
        Titulo t = new Titulo();
        t.id = v.caminho;
        t.caminho = v.caminho;
        t.naoIdentificado = v.tipo == Classificador.Tipo.NAO_IDENTIFICADO;
        t.tipo = v.tipo == Classificador.Tipo.FILME ? Tipo.FILME : v.tipo == Classificador.Tipo.SHOW ? Tipo.SHOW
                : v.tipo == Classificador.Tipo.STANDUP ? Tipo.STANDUP
                : v.tipo == Classificador.Tipo.DESENHO ? Tipo.DESENHO : null;
        String nomeArquivo = semExtensao(new File(v.caminho).getName());
        String doArquivo = v.titulo == null ? nomeArquivo
                : v.artista != null ? v.artista + " - " + v.titulo : v.titulo;
        t.titulo = dados != null && temTexto(dados.titulo) ? dados.titulo : doArquivo;
        t.tituloOriginal = dados != null && dados.tituloOriginal != null ? dados.tituloOriginal : doArquivo;
        t.ano = dados != null && dados.ano != null ? dados.ano : v.ano == null ? "" : String.valueOf(v.ano);
        t.duracaoSegundos = (int) (v.duracaoMs / 1000);
        if (t.duracaoSegundos <= 0 && dados != null) t.duracaoSegundos = dados.duracaoMin * 60;
        t.duracaoMin = Math.max(t.duracaoSegundos > 0 ? 1 : 0, Math.round(t.duracaoSegundos / 60f));
        t.generos = dados != null && dados.generos != null ? dados.generos : "";
        t.genero = primeiro(t.generos);
        if (t.genero.isEmpty() && v.tipo == Classificador.Tipo.SHOW) t.genero = "Show";
        if (t.genero.isEmpty() && v.tipo == Classificador.Tipo.STANDUP) t.genero = "Stand-up";
        if (t.genero.isEmpty() && v.tipo == Classificador.Tipo.DESENHO) t.genero = "Animação";
        t.qualidade = qualidade(v);
        t.sinopse = dados != null && dados.sinopse != null ? dados.sinopse : "";
        int cor = corDe(t.id);
        t.corCapa = cor;
        t.corFundo = cor;
        t.corCartao = cor;
        if (dados != null) {
            t.capaTmdb = existe(dados.capa);
            t.fundoTmdb = existe(dados.fundo);
            t.idResultadoTmdb = dados.escolhido ? idResultado(dados) : null;
            if (dados.colecaoId > 0) {
                t.colecaoNome = dados.colecaoNome;
                t.colecaoCapa = existe(dados.colecaoCapa);
                t.colecaoFundo = existe(dados.colecaoFundo);
            }
        }
        t.quadro = existe(v.miniatura);
        t.arquivo = exibir(r, v.pasta, v.caminho);
        t.adicionado = v.adicionado;
        t.modificado = v.modificado;
        t.chaveNome = Acervo.chaveNome(v);
        t.artista = v.artista;
        t.grupo = v.grupo;
        return t;
    }

    private void montarSerie(Retrato r, String chave, List<Acervo.Video> episodiosDaSerie) {
        Acervo.Tmdb dados = achado(r.tmdb.get(chave));
        Acervo.Video primeiro = episodiosDaSerie.get(0);
        Map<Integer, List<Episodio>> porTemporada = new TreeMap<>();
        long adicionado = 0;
        long modificado = 0;
        long somaDuracao = 0;
        for (Acervo.Video v : episodiosDaSerie) {
            Acervo.Tmdb doEpisodio = achado(r.tmdb.get(v.caminho));
            Episodio e = new Episodio();
            e.id = v.caminho;
            e.caminho = v.caminho;
            e.idSerie = chave;
            e.temporada = v.temporada;
            e.numero = v.episodio;
            e.nome = doEpisodio != null && temTexto(doEpisodio.nomeEpisodio) ? doEpisodio.nomeEpisodio
                    : v.titulo != null ? v.titulo : "Episódio " + v.episodio;
            e.arquivo = new File(v.caminho).getName();
            e.duracaoSegundos = (int) (v.duracaoMs / 1000);
            if (e.duracaoSegundos <= 0 && doEpisodio != null) e.duracaoSegundos = doEpisodio.duracaoMin * 60;
            e.duracaoMin = Math.max(e.duracaoSegundos > 0 ? 1 : 0, Math.round(e.duracaoSegundos / 60f));
            e.corMiniatura = CORES[(e.numero + CORES.length - 1) % 4];
            String foto = doEpisodio == null ? null : existe(doEpisodio.miniatura);
            e.miniatura = foto != null ? foto : existe(v.miniatura);
            r.episodios.put(e.id, e);
            List<Episodio> lista = porTemporada.get(e.temporada);
            if (lista == null) {
                lista = new ArrayList<>();
                porTemporada.put(e.temporada, lista);
            }
            lista.add(e);
            adicionado = Math.max(adicionado, v.adicionado);
            modificado = Math.max(modificado, v.modificado);
            somaDuracao += e.duracaoSegundos;
        }
        List<Temporada> temporadas = new ArrayList<>();
        int total = 0;
        for (Map.Entry<Integer, List<Episodio>> t : porTemporada.entrySet()) {
            List<Episodio> lista = t.getValue();
            Collections.sort(lista, (a, b) -> Integer.compare(a.numero, b.numero));
            temporadas.add(new Temporada(t.getKey(), lista));
            total += lista.size();
        }
        r.temporadas.put(chave, temporadas);

        Titulo t = new Titulo();
        t.id = chave;
        t.tipo = Tipo.SERIE;
        t.titulo = dados != null && temTexto(dados.titulo) ? dados.titulo : primeiro.serie;
        t.tituloOriginal = dados != null && dados.tituloOriginal != null ? dados.tituloOriginal : primeiro.serie;
        t.ano = dados != null && dados.ano != null ? dados.ano : primeiro.ano == null ? "" : String.valueOf(primeiro.ano);
        t.duracaoSegundos = total == 0 ? 0 : (int) (somaDuracao / total);
        t.duracaoMin = Math.max(t.duracaoSegundos > 0 ? 1 : 0, Math.round(t.duracaoSegundos / 60f));
        t.generos = dados != null && dados.generos != null ? dados.generos : "";
        t.genero = primeiro(t.generos);
        t.qualidade = qualidade(primeiro);
        t.sinopse = dados != null && dados.sinopse != null ? dados.sinopse : "";
        int cor = corDe(chave);
        t.corCapa = cor;
        t.corFundo = cor;
        t.corCartao = cor;
        if (dados != null) {
            t.capaTmdb = existe(dados.capa);
            t.fundoTmdb = existe(dados.fundo);
            t.idResultadoTmdb = dados.escolhido ? idResultado(dados) : null;
        }
        t.quadro = existe(primeiro.miniatura);
        t.arquivo = exibir(r, primeiro.pasta, primeiro.pasta) + " / " + primeiro.serie;
        t.numeroTemporadas = temporadas.size();
        t.numeroEpisodios = total;
        t.adicionado = adicionado;
        t.modificado = modificado;
        r.titulos.put(chave, t);
        for (Temporada temporada : temporadas) {
            for (Episodio e : temporada.episodios) e.nomeSerie = t.titulo;
        }
    }

    // =====================================================================
    // Estado e edição (na hora da leitura)
    // =====================================================================

    private Acervo.Estado estado(String id) {
        Acervo.Estado e = estados.get(id);
        return e == null ? new Acervo.Estado() : e;
    }

    private Titulo comEstado(Retrato r, Titulo base) {
        return comEstado(r, base, true);
    }

    /** {@code comEdicao} false: sem a edição à mão (prévia do Restaurar original no Editar). */
    private Titulo comEstado(Retrato r, Titulo base, boolean comEdicao) {
        Titulo t = base.copia();
        Acervo.Estado e = estado(t.id);
        t.favorito = e.favorito;
        if (t.tipo == Tipo.SERIE) {
            List<Temporada> temporadas = r.temporadas.get(t.id);
            boolean todos = temporadas != null && !temporadas.isEmpty();
            if (temporadas != null) {
                for (Temporada temporada : temporadas) {
                    for (Episodio ep : temporada.episodios) todos &= estado(ep.id).assistido;
                }
            }
            t.assistido = todos;
        } else {
            t.assistido = e.assistido;
            t.posicaoSegundos = e.posicao;
        }
        if (!comEdicao) {
            completar(t, e);
            return t;
        }
        // Edição à mão por cima do original / do TMDB: o que foi editado sempre vale, a deste vídeo
        // ou, sem ela, a herdada de outro de mesmo nome (D13)
        Map<Edicoes.Campo, String> edicao = edicoes.valendo(t.id, t.chaveNome);
        for (Campo campo : Campo.values()) {
            String valor = edicao.get(Edicoes.Campo.valueOf(campo.name()));
            if (valor != null) {
                t.aplicar(campo, valor);
                t.editadosAMao.add(campo);
            }
        }
        if (t.tipo == Tipo.SHOW && !t.ehArtista() && t.artista != null) t.artista = artistaFinal(t.artista);
        if (t.naoIdentificado && t.editadosAMao.contains(Campo.TIPO) && t.tipo != null) t.naoIdentificado = false;
        if (t.editadosAMao.contains(Campo.GENEROS)) t.genero = primeiro(t.generos);
        String capa = edicao.get(Edicoes.Campo.CAPA);
        if (capa != null) {
            t.capaImagem = capa;
            if (capa.startsWith(Edicoes.PREFIXO_COR)) t.corCapa = corDe(capa, t.corCapa);
        }
        String fundo = edicao.get(Edicoes.Campo.FUNDO);
        if (fundo != null) {
            t.fundoImagem = fundo;
            if (fundo.startsWith(Edicoes.PREFIXO_COR)) {
                t.corFundo = corDe(fundo, t.corFundo);
                t.corCartao = t.corFundo;
            }
        }
        String colecao = edicao.get(Edicoes.Campo.COLECAO);
        if (colecao != null) t.colecaoNome = colecao.isEmpty() ? null : colecao;
        completar(t, e);
        return t;
    }

    /** O que depende do tipo e do assistido já finais: a nota (D6) e a coletânea, só em filme. */
    private static void completar(Titulo t, Acervo.Estado e) {
        t.nota = t.podeDarNota() ? e.nota : null;
        t.idColetanea = t.tipo == Tipo.FILME && !t.ehColetanea() && temTexto(t.colecaoNome)
                ? Acervo.chaveColetanea(t.colecaoNome) : null;
    }

    private Episodio comEstado(Episodio base) {
        Episodio e = base.copia();
        Acervo.Estado estado = estado(e.id);
        e.assistido = estado.assistido;
        e.posicaoSegundos = estado.posicao;
        return e;
    }

    private List<Titulo> todos(Retrato r) {
        List<Titulo> lista = new ArrayList<>();
        for (Titulo base : r.titulos.values()) {
            Titulo t = comEstado(r, base);
            if (!t.naoIdentificado && t.tipo != null) lista.add(t);
        }
        return lista;
    }

    // =====================================================================
    // RepositorioBiblioteca — leitura
    // =====================================================================

    @Override
    public void ouvir(Runnable ouvinte) {
        if (!ouvintes.contains(ouvinte)) ouvintes.add(ouvinte);
    }

    @Override
    public void deixarDeOuvir(Runnable ouvinte) {
        ouvintes.remove(ouvinte);
    }

    @Override
    public synchronized boolean atualizando() {
        return emAndamento;
    }

    /** TMDB 0–40 %, leitura dos vídeos 40–90 %, quadros 90–100 %. */
    @Override
    public int progresso() {
        if (!atualizando()) return -1;
        int[] inicio = {0, 40, 90};
        int[] peso = {40, 50, 10};
        int total = totalDoPasso;
        int dentro = total <= 0 ? 0 : Math.min(100, feitosNoPasso * 100 / total);
        return Math.min(99, inicio[passo] + peso[passo] * dentro / 100);
    }

    private void andar(int novoPasso, int feitos, int total) {
        passo = novoPasso;
        feitosNoPasso = feitos;
        totalDoPasso = total;
        int agora = progresso();
        if (agora != ultimoAvisado) {
            ultimoAvisado = agora;
            tela.post(avisarAndamento);
        }
    }

    /** Só o indicador de atualização relê (as telas não piscam a cada por cento). */
    private final Runnable avisarAndamento = () -> {
        for (Runnable ouvinte : ouvintesDoAndamento) ouvinte.run();
    };

    @Override
    public void ouvirAndamento(Runnable ouvinte) {
        if (!ouvintesDoAndamento.contains(ouvinte)) ouvintesDoAndamento.add(ouvinte);
    }

    @Override
    public void deixarDeOuvirAndamento(Runnable ouvinte) {
        ouvintesDoAndamento.remove(ouvinte);
    }

    @Override
    public List<Titulo> destaques() {
        // Os mais recentes, sem exigir capa nem sinopse: a exigência escondia os shows
        List<Titulo> lista = recentes(todos(retrato));
        return new ArrayList<>(lista.subList(0, Math.min(DESTAQUES, lista.size())));
    }

    @Override
    public List<Titulo> continuarAssistindo() {
        Retrato r = retrato;
        List<Titulo> lista = new ArrayList<>();
        for (Titulo t : todos(r)) {
            if (t.tipo != Tipo.SERIE && t.emAndamento()) lista.add(t);
        }
        for (Episodio base : r.episodios.values()) {
            Episodio e = comEstado(base);
            if (!e.emAndamento()) continue;
            Titulo serie = r.titulos.get(e.idSerie);
            if (serie == null) continue;
            lista.add(cartaoDoEpisodio(r, comEstado(r, serie), e));
        }
        Collections.sort(lista, (a, b) -> Long.compare(estado(b.id).visto, estado(a.id).visto));
        for (Titulo t : lista) {
            if (t.fundoImagem == null && t.imagemFundo() == null && t.quadro == null) pedirQuadro(t.id);
        }
        return lista;
    }

    /**
     * Cartão de Continuar assistindo sem fundo e sem quadro: o quadro vem na hora, sem esperar a vez na
     * rodada, e quem já falhou tenta de novo. Uma vez por abertura do app; com o HD fora, quando ele voltar.
     */
    private void pedirQuadro(String id) {
        if (!quadrosPedidos.add(id)) return;
        agora.execute(() -> {
            try {
                if (!new File(id).isFile()) quadrosPedidos.remove(id);
                else if (Varredura.quadroAgora(app, id)) reconstruirENotificar();
            } catch (RuntimeException e) {
                // fica a capa ou o bloco de cor
            }
        });
    }

    /** Episódio em andamento como cartão de Continuar assistindo: "Série · T1 E3", imagem do episódio. */
    private static Titulo cartaoDoEpisodio(Retrato r, Titulo serie, Episodio e) {
        Titulo t = serie.copia();
        t.id = e.id;
        t.caminho = e.caminho;
        t.titulo = String.format(Locale.ROOT, "%s · T%d E%d", serie.titulo, e.temporada, e.numero);
        t.duracaoSegundos = e.duracaoSegundos;
        t.duracaoMin = e.duracaoMin;
        t.posicaoSegundos = e.posicaoSegundos;
        t.assistido = e.assistido;
        if (e.miniatura != null) {
            t.fundoImagem = Modelo.ARQUIVO + e.miniatura;
            t.quadro = e.miniatura;
        }
        return t;
    }

    @Override
    public List<Titulo> favoritos() {
        List<Titulo> lista = new ArrayList<>();
        for (Titulo t : todos(retrato)) if (t.favorito) lista.add(t);
        Collections.sort(lista, (a, b) -> Long.compare(estado(b.id).favoritado, estado(a.id).favoritado));
        return lista;
    }

    @Override
    public List<Titulo> filmesAdicionadosRecentemente() {
        List<Titulo> lista = new ArrayList<>();
        // Todos: a fileira da TV mostra 6 e carrega mais 6 quando o foco chega ao último
        for (Titulo t : recentes(todos(retrato))) {
            if (t.tipo == Tipo.FILME) lista.add(t);
        }
        return lista;
    }

    @Override
    public List<Titulo> adicionadosRecentemente() {
        List<Titulo> lista = recentes(todos(retrato));
        return new ArrayList<>(lista.subList(0, Math.min(RECENTES, lista.size())));
    }

    static List<Titulo> recentes(List<Titulo> lista) {
        List<Titulo> ordem = new ArrayList<>(lista);
        Collator alfabetica = collator();
        Collections.sort(ordem, (a, b) -> {
            int c = Long.compare(b.adicionado, a.adicionado);
            if (c != 0) return c;
            // A varredura dá o mesmo "adicionado" a todos os que achou: desempata a data do arquivo
            c = Long.compare(b.modificado, a.modificado);
            return c != 0 ? c : alfabetica.compare(a.titulo, b.titulo);
        });
        return ordem;
    }

    @Override
    public List<Titulo> titulos(Tipo tipo) {
        List<Titulo> lista = new ArrayList<>();
        for (Titulo t : todos(retrato)) if (t.tipo == tipo) lista.add(t);
        Collator ordem = collator();
        Collections.sort(lista, (a, b) -> ordem.compare(a.titulo, b.titulo));
        return lista;
    }

    @Override
    public List<Titulo> buscar(String consulta) {
        List<Titulo> lista = new ArrayList<>();
        String alvo = normalizar(consulta);
        if (alvo.isEmpty()) return lista;
        for (Titulo t : todos(retrato)) {
            if (normalizar(t.titulo).contains(alvo) || normalizar(t.tituloOriginal).contains(alvo)) lista.add(t);
        }
        Collator ordem = collator();
        Collections.sort(lista, (a, b) -> ordem.compare(a.titulo, b.titulo));
        return lista;
    }

    @Override
    public Titulo titulo(String id) {
        Retrato r = retrato;
        if (Acervo.ehColetanea(id) || Acervo.ehArtista(id)) return capaMontada(r, id, true);
        Titulo t = id == null ? null : r.titulos.get(id);
        return t == null ? null : comEstado(r, t);
    }

    @Override
    public Titulo tituloOriginal(String id) {
        Retrato r = retrato;
        if (Acervo.ehColetanea(id) || Acervo.ehArtista(id)) return capaMontada(r, id, false);
        Titulo t = id == null ? null : r.titulos.get(id);
        return t == null ? null : comEstado(r, t, false);
    }

    @Override
    public boolean vazia() {
        return todos(retrato).isEmpty();
    }

    // =====================================================================
    // Coletânea (D5), shows por artista e por grupo (D7): montados na hora da leitura, com a edição
    // =====================================================================

    private List<Titulo> doTipo(Retrato r, Tipo tipo) {
        List<Titulo> lista = new ArrayList<>();
        for (Titulo t : todos(r)) if (t.tipo == tipo) lista.add(t);
        return lista;
    }

    /** Capa de coletânea ou de artista; nula se não existe (coletânea com menos de 2, artista sem show). */
    private Titulo capaMontada(Retrato r, String id, boolean comEdicao) {
        if (Acervo.ehColetanea(id)) {
            List<Titulo> filmes = porColetanea(doTipo(r, Tipo.FILME)).get(id);
            return filmes == null || filmes.size() < FILMES_NA_COLETANEA ? null : capaDeColetanea(r, id, filmes, comEdicao);
        }
        List<Titulo> shows = showsDe(r, id);
        return shows.isEmpty() ? null : capaDeArtista(r, id, shows, comEdicao);
    }

    /** Sem imagem e sem dados: o bloco de cor, com o nome. */
    private static Titulo capaVazia(String id, Tipo tipo, String nome) {
        Titulo t = new Titulo();
        t.id = id;
        t.tipo = tipo;
        t.titulo = nome;
        t.tituloOriginal = nome;
        t.ano = "";
        t.generos = "";
        t.genero = "";
        t.qualidade = "";
        t.sinopse = "";
        t.arquivo = "";
        int cor = corDe(id);
        t.corCapa = cor;
        t.corFundo = cor;
        t.corCartao = cor;
        return t;
    }

    /** Os filmes de cada coletânea, pelo id dela, por ano. */
    static Map<String, List<Titulo>> porColetanea(List<Titulo> filmes) {
        Map<String, List<Titulo>> grupos = new LinkedHashMap<>();
        for (Titulo t : filmes) {
            if (t.idColetanea == null) continue;
            List<Titulo> grupo = grupos.get(t.idColetanea);
            if (grupo == null) {
                grupo = new ArrayList<>();
                grupos.put(t.idColetanea, grupo);
            }
            grupo.add(t);
        }
        Collator alfabetica = collator();
        for (List<Titulo> grupo : grupos.values()) {
            Collections.sort(grupo, (a, b) -> {
                int c = Integer.compare(anoDe(a), anoDe(b));
                return c != 0 ? c : alfabetica.compare(a.titulo, b.titulo);
            });
        }
        return grupos;
    }

    private static int anoDe(Titulo t) {
        Integer ano = numero(t.ano);
        return ano == null ? Integer.MAX_VALUE : ano;
    }

    /** A capa e o fundo da coleção; sem eles, os do primeiro filme. */
    private Titulo capaDeColetanea(Retrato r, String id, List<Titulo> filmes, boolean comEdicao) {
        Titulo primeiro = filmes.get(0);
        Titulo t = capaVazia(id, Tipo.FILME, primeiro.colecaoNome);
        t.numeroFilmes = filmes.size();
        t.todosAssistidos = true;
        for (Titulo filme : filmes) {
            if (t.capaTmdb == null) t.capaTmdb = filme.colecaoCapa;
            if (t.fundoTmdb == null) t.fundoTmdb = filme.colecaoFundo;
            t.adicionado = Math.max(t.adicionado, filme.adicionado);
            t.modificado = Math.max(t.modificado, filme.modificado);
            t.todosAssistidos &= filme.assistido;
        }
        if (t.capaTmdb == null) t.capaTmdb = primeiro.imagemCapa();
        if (t.fundoTmdb == null) t.fundoTmdb = primeiro.imagemFundo();
        return comEstado(r, t, comEdicao);
    }

    @Override
    public List<Titulo> filmesEColetaneas() {
        Retrato r = retrato;
        List<Titulo> filmes = doTipo(r, Tipo.FILME);
        Map<String, List<Titulo>> grupos = porColetanea(filmes);
        List<Titulo> lista = new ArrayList<>();
        for (Titulo t : filmes) {
            List<Titulo> grupo = t.idColetanea == null ? null : grupos.get(t.idColetanea);
            if (grupo == null || grupo.size() < FILMES_NA_COLETANEA) lista.add(t);
        }
        for (Map.Entry<String, List<Titulo>> grupo : grupos.entrySet()) {
            if (grupo.getValue().size() >= FILMES_NA_COLETANEA) lista.add(capaDeColetanea(r, grupo.getKey(), grupo.getValue(), true));
        }
        Collator ordem = collator();
        Collections.sort(lista, (a, b) -> ordem.compare(a.titulo, b.titulo));
        return lista;
    }

    @Override
    public List<Titulo> filmesDaColetanea(String idColetanea) {
        List<Titulo> filmes = idColetanea == null ? null : porColetanea(doTipo(retrato, Tipo.FILME)).get(idColetanea);
        return filmes == null || filmes.size() < FILMES_NA_COLETANEA ? Collections.<Titulo>emptyList() : filmes;
    }

    @Override
    public List<String> nomesDeColetanea() {
        List<String> nomes = new ArrayList<>();
        for (List<Titulo> grupo : porColetanea(doTipo(retrato, Tipo.FILME)).values()) nomes.add(grupo.get(0).colecaoNome);
        Collections.sort(nomes, collator());
        return nomes;
    }

    @Override
    public void definirColetanea(String idFilme, String nome) {
        if (idFilme == null) return;
        edicoes.salvar(idFilme, nomeComum(idFilme), Edicoes.Campo.COLECAO, nome == null ? null : nome.trim());
        notificar();
    }

    /** Os shows do artista ({@code id} de {@link Acervo#chaveArtista}). */
    private List<Titulo> showsDe(Retrato r, String id) {
        List<Titulo> lista = new ArrayList<>();
        for (Titulo t : doTipo(r, Tipo.SHOW)) {
            if (id.equals(Acervo.chaveArtista(t.artista))) lista.add(t);
        }
        return lista;
    }

    /** A foto da API; sem ela, o bloco de cor. A contagem é a de {@code shows}. */
    private Titulo capaDeArtista(Retrato r, String id, List<Titulo> shows, boolean comEdicao) {
        String nome = shows.get(0).artista;
        Titulo t = capaVazia(id, Tipo.SHOW, nome != null ? nome : app.getString(R.string.st_sem_artista));
        t.artista = nome;
        t.numeroShows = shows.size();
        t.todosAssistidos = true;
        for (Titulo show : shows) t.todosAssistidos &= show.assistido;
        Acervo.Artista dados = r.artistas.get(id);
        if (dados != null) {
            t.capaTmdb = existe(dados.foto);
            t.fundoTmdb = existe(dados.fundo);
            if (dados.descricao != null) t.sinopse = dados.descricao;
            if (dados.resumo != null) t.genero = dados.resumo;
        }
        return comEstado(r, t, comEdicao);
    }

    /** Uma capa por artista de {@code shows}, em ordem alfabética, com "Sem artista" por último. */
    private List<Titulo> artistasDe(Retrato r, List<Titulo> shows) {
        Map<String, List<Titulo>> porArtista = new LinkedHashMap<>();
        for (Titulo t : shows) {
            String id = Acervo.chaveArtista(t.artista);
            List<Titulo> dele = porArtista.get(id);
            if (dele == null) {
                dele = new ArrayList<>();
                porArtista.put(id, dele);
            }
            dele.add(t);
        }
        List<Titulo> lista = new ArrayList<>();
        for (Map.Entry<String, List<Titulo>> artista : porArtista.entrySet()) {
            lista.add(capaDeArtista(r, artista.getKey(), artista.getValue(), true));
        }
        String semArtista = Acervo.chaveArtista(null);
        Collator ordem = collator();
        Collections.sort(lista, (a, b) -> {
            boolean semA = semArtista.equals(a.id);
            if (semA != semArtista.equals(b.id)) return semA ? 1 : -1;
            return ordem.compare(a.titulo, b.titulo);
        });
        return lista;
    }

    @Override
    public List<Titulo> artistas() {
        Retrato r = retrato;
        return artistasDe(r, doTipo(r, Tipo.SHOW));
    }

    @Override
    public List<Titulo> showsDoArtista(String idArtista) {
        if (idArtista == null) return Collections.emptyList();
        List<Titulo> lista = showsDe(retrato, idArtista);
        for (Titulo t : lista) {
            // Na tela do artista, o nome dele no título só repete
            String prefixo = t.artista + " - ";
            if (t.artista != null && !t.editadosAMao.contains(Campo.TITULO) && t.titulo.startsWith(prefixo)) {
                t.titulo = t.titulo.substring(prefixo.length());
            }
        }
        Collator ordem = collator();
        Collections.sort(lista, (a, b) -> ordem.compare(a.titulo, b.titulo));
        return lista;
    }

    @Override
    public List<SecaoShows> gruposDeShows() {
        Retrato r = retrato;
        // Sem diferenciar maiúscula e acento, como as outras listas; o nome é o do primeiro show achado
        Map<String, List<Titulo>> porGrupo = new TreeMap<>(collator());
        List<Titulo> semGrupo = new ArrayList<>();
        for (Titulo t : doTipo(r, Tipo.SHOW)) {
            if (!temTexto(t.grupo)) {
                semGrupo.add(t);
                continue;
            }
            List<Titulo> grupo = porGrupo.get(t.grupo);
            if (grupo == null) {
                grupo = new ArrayList<>();
                porGrupo.put(t.grupo, grupo);
            }
            grupo.add(t);
        }
        List<SecaoShows> secoes = new ArrayList<>();
        for (Map.Entry<String, List<Titulo>> grupo : porGrupo.entrySet()) {
            secoes.add(new SecaoShows(grupo.getKey(), false, artistasDe(r, grupo.getValue())));
        }
        if (!semGrupo.isEmpty()) secoes.add(new SecaoShows(app.getString(R.string.st_sem_grupo), true, artistasDe(r, semGrupo)));
        return secoes;
    }

    @Override
    public List<Capitulo> capitulos(String id) {
        Acervo.Video v = id == null ? null : retrato.videos.get(id);
        return v == null ? Collections.<Capitulo>emptyList() : paraTela(v.capitulos);
    }

    /** Capítulos do motor como a tela usa: sem nome no arquivo, "Capítulo N". */
    public static List<Capitulo> paraTela(Capitulos capitulos) {
        if (capitulos == null || capitulos.vazio()) return Collections.emptyList();
        List<Capitulo> lista = new ArrayList<>();
        for (Capitulos.Capitulo c : capitulos.lista()) {
            String nome = c.nome.isEmpty() ? "Capítulo " + c.numero : c.nome;
            lista.add(new Capitulo(c.numero, nome, (int) (c.inicioMs / 1000), CORES[(c.numero - 1) % 4]));
        }
        return Collections.unmodifiableList(lista);
    }

    @Override
    public List<Temporada> temporadas(String idSerie) {
        List<Temporada> base = idSerie == null ? null : retrato.temporadas.get(idSerie);
        if (base == null) return Collections.emptyList();
        List<Temporada> lista = new ArrayList<>();
        for (Temporada temporada : base) {
            List<Episodio> eps = new ArrayList<>();
            for (Episodio e : temporada.episodios) eps.add(comEstado(e));
            lista.add(new Temporada(temporada.numero, eps));
        }
        return lista;
    }

    @Override
    public Episodio episodio(String idEpisodio) {
        Episodio e = idEpisodio == null ? null : retrato.episodios.get(idEpisodio);
        return e == null ? null : comEstado(e);
    }

    @Override
    public DadosTecnicos dadosTecnicos(String idTitulo) {
        Retrato r = retrato;
        Acervo.Video v = idTitulo == null ? null : r.videos.get(idTitulo);
        if (v == null) return null;
        String resolucao = v.largura > 0 && v.altura > 0 ? v.largura + "×" + v.altura : "";
        String video = juntar(" · ", resolucao, v.infoVideo);
        // Resumo curto (arquivo com muitas faixas empurrava os capítulos): 1 áudio e até 3 idiomas de
        // legenda, com "+N"; o resto em "Ver todos" (painel Áudio e legendas)
        String audio = v.audios.isEmpty() ? "—" : v.audios.get(0)
                + (v.audios.size() > 1 ? "  +" + (v.audios.size() - 1) : "");
        List<String> idiomas = new ArrayList<>();
        for (String legenda : v.legendas) {
            String idioma = legenda.replaceAll(" · .*$", "");
            if (!idiomas.contains(idioma)) idiomas.add(idioma);
        }
        String legendas = idiomas.isEmpty() ? "Sem legendas"
                : juntarLista(idiomas.subList(0, Math.min(LEGENDAS_NO_RESUMO, idiomas.size())))
                + (idiomas.size() > LEGENDAS_NO_RESUMO ? "  +" + (idiomas.size() - LEGENDAS_NO_RESUMO) : "");
        String capitulos = v.capitulos.vazio() ? "Sem capítulos" : v.capitulos.quantidade() + (temNome(v.capitulos) ? ", com nome" : "");
        Pastas.Pasta pasta = r.pastas.get(v.pasta);
        String origem = pasta != null && pasta.origem == Pastas.Origem.HD_EXTERNO ? "HD externo (USB)" : "Armazenamento interno";

        List<String> chips = new ArrayList<>();
        List<String> curtos = new ArrayList<>();
        int chipsVideo = 0;
        String qualidade = Faixas.qualidade(v.largura, v.altura);
        if (!qualidade.isEmpty()) {
            String nome = "4K".equals(qualidade) ? "4K UHD" : qualidade;
            chips.add(nome);
            curtos.add(nome);
            chipsVideo++;
        }
        String hdr = hdr(v.infoVideo);
        if (!hdr.isEmpty()) {
            chips.add(hdr);
            curtos.add(hdr);
            chipsVideo++;
        }
        if (!v.audios.isEmpty()) {
            String detalhe = v.audios.get(0).contains(" · ") ? v.audios.get(0).replaceAll("^.* · ", "") : "";
            if (!detalhe.isEmpty()) chips.add(detalhe);
            String canais = detalhe.replaceAll("^.* ", "");
            if (canais.matches("5\\.1|7\\.1|Estéreo|Mono")) curtos.add(canais);
        }
        for (String legenda : v.legendas) {
            if (legenda.startsWith("Português")) {
                chips.add("Legendas PT-BR");
                break;
            }
        }
        DadosTecnicos dados = new DadosTecnicos(video.isEmpty() ? "—" : video, audio, legendas, new File(v.caminho).getName(),
                capitulos, origem, chips, curtos, chipsVideo);
        dados.faixasDemais = v.audios.size() > 1 || idiomas.size() > LEGENDAS_NO_RESUMO;
        return dados;
    }

    @Override
    public List<PessoaElenco> elenco(String idTitulo) {
        Retrato r = retrato;
        if (idTitulo == null) return Collections.emptyList();
        String chave = idTitulo;
        Episodio e = r.episodios.get(idTitulo);
        if (e != null) chave = e.idSerie;
        Acervo.Tmdb dados = achado(r.tmdb.get(chave));
        if (dados == null) return Collections.emptyList();
        List<PessoaElenco> lista = new ArrayList<>();
        for (String[] pessoa : dados.elenco) {
            if (pessoa.length == 0 || pessoa[0].isEmpty()) continue;
            lista.add(new PessoaElenco(pessoa[0], pessoa.length > 1 ? pessoa[1] : ""));
        }
        return lista;
    }

    @Override
    public List<Faixa> faixas(String id, TipoFaixa tipo) {
        Acervo.Video v = id == null ? null : retrato.videos.get(id);
        List<Faixa> lista = new ArrayList<>();
        if (v == null) return lista;
        if (tipo == TipoFaixa.AUDIO) {
            for (int i = 0; i < v.audios.size(); i++) lista.add(new Faixa("a" + i, tipo, v.audios.get(i), i == 0));
        } else {
            lista.add(new Faixa("l-1", tipo, "Desligadas", true));
            for (int i = 0; i < v.legendas.size(); i++) lista.add(new Faixa("l" + i, tipo, v.legendas.get(i), false));
        }
        return lista;
    }

    @Override
    public String faixaEscolhida(String id, String idSerie, TipoFaixa tipo) {
        String doVideo = faixaDe(estado(id), tipo);
        return doVideo != null || idSerie == null ? doVideo : faixaDe(estado(idSerie), tipo);
    }

    private static String faixaDe(Acervo.Estado e, TipoFaixa tipo) {
        return tipo == TipoFaixa.AUDIO ? e.faixaAudio : e.faixaLegenda;
    }

    @Override
    public void guardarFaixa(String id, String idSerie, TipoFaixa tipo, String faixa) {
        gravarFaixa(id, tipo, faixa);
        if (idSerie != null) gravarFaixa(idSerie, tipo, faixa);
    }

    /** Sem avisar as telas: nada do que elas mostram muda. */
    private void gravarFaixa(String chave, TipoFaixa tipo, String faixa) {
        Acervo.Estado e = estado(chave).copia();
        if (tipo == TipoFaixa.AUDIO) e.faixaAudio = faixa;
        else e.faixaLegenda = faixa;
        estados.put(chave, e);
        acervo.gravarEstado(chave, e);
    }

    @Override
    public void lerDadosDoArquivo(String id) {
        Acervo.Video v = id == null ? null : retrato.videos.get(id);
        if (v == null || !Varredura.faltaLer(v) || !lendoAgora.add(id)) return;
        agora.execute(() -> {
            try {
                if (Varredura.lerAgora(app, id, this::reconstruirENotificar)) reconstruirENotificar();
            } catch (RuntimeException e) {
                // a varredura tenta de novo na fila
            } finally {
                lendoAgora.remove(id);
            }
        });
    }

    @Override
    public void guardarCapitulos(String id, Capitulos capitulos) {
        if (id == null || capitulos == null || capitulos.vazio()) return;
        agora.execute(() -> {
            Acervo.Video v = acervo.video(id);
            if (v == null || !v.capitulos.vazio()) return;
            v.capitulos = capitulos;
            acervo.gravarVideos(Collections.singletonList(v), Collections.<String>emptyList());
            reconstruirENotificar();
        });
    }

    @Override
    public void guardarDuracao(String id, long duracaoMs) {
        if (id == null || duracaoMs <= 0) return;
        // O comum: a varredura já leu; nem entra na fila
        Acervo.Video lido = retrato.videos.get(id);
        if (lido != null && lido.duracaoMs > 0) return;
        agora.execute(() -> {
            try {
                synchronized (acervo) {
                    Acervo.Video v = acervo.video(id);
                    if (v == null || v.duracaoMs > 0) return;
                    v.duracaoMs = duracaoMs;
                    acervo.gravarVideos(Collections.singletonList(v), Collections.<String>emptyList());
                }
                reconstruirENotificar();
            } catch (RuntimeException e) {
                // o player acha a duração de novo na próxima vez que abrir
            }
        });
    }

    // =====================================================================
    // Editar e TMDB
    // =====================================================================

    @Override
    public List<String> opcoesCapa(String idTitulo) {
        return opcoes(idTitulo, true);
    }

    @Override
    public List<String> opcoesFundo(String idTitulo) {
        return opcoes(idTitulo, false);
    }

    @Override
    public List<String> fotosDoArtista(String idArtista) {
        Titulo artista = titulo(idArtista);
        // "Sem artista" não é ninguém para buscar
        if (artista == null || !artista.ehArtista() || idArtista.equals(Acervo.chaveArtista(null))) return Collections.emptyList();
        List<String> doDeezer = new BuscaArtista(app).fotosDeezer(artista.titulo);
        List<String> daWikipedia = new WikipediaArtista().fotos(artista.titulo);
        if (doDeezer == null && daWikipedia.isEmpty()) return null;
        List<String> referencias = new ArrayList<>();
        if (doDeezer != null) for (String foto : doDeezer) referencias.add("url:" + foto);
        for (String foto : daWikipedia) referencias.add("url:" + foto);
        return referencias;
    }

    @Override
    public List<ResultadoTmdb> buscarArtista(String idArtista, String consulta) {
        List<ResultadoTmdb> lista = new ArrayList<>();
        if (normalizar(consulta).isEmpty()) return lista;
        String alvo = normalizar(consulta);
        String semArtista = Acervo.chaveArtista(null);
        for (Titulo t : artistas()) {
            if (t.id.equals(idArtista) || t.id.equals(semArtista) || !normalizar(t.titulo).contains(alvo)) continue;
            String capa = t.imagemCapa();
            lista.add(new ResultadoTmdb(t.artista, t.titulo, "", Tipo.SHOW,
                    app.getResources().getQuantityString(R.plurals.st_artista_na_biblioteca, t.numeroShows, t.numeroShows),
                    t.corCapa, capa == null ? null : Uri.fromFile(new File(capa)).toString()));
        }
        NumberFormat numero = NumberFormat.getIntegerInstance(Locale.forLanguageTag("pt-BR"));
        for (BuscaArtista.Encontrado achado : new BuscaArtista(app).artistasDeezer(consulta.trim())) {
            lista.add(new ResultadoTmdb(achado.nome, achado.nome, "", Tipo.SHOW,
                    app.getString(R.string.st_artista_do_deezer, numero.format(achado.fas)),
                    corDe(Acervo.chaveArtista(achado.nome)), achado.foto));
        }
        return lista;
    }

    @Override
    public void definirArtista(String idArtista, String nome, String foto) {
        if (!Acervo.ehArtista(idArtista) || !temTexto(nome)) return;
        String limpo = nome.trim();
        String destino = Acervo.chaveArtista(limpo);
        if (foto != null) escolherImagem(destino, Edicoes.Campo.CAPA, "url:" + foto);
        if (destino.equals(idArtista)) return;
        // O outro já tinha sido juntado a este: agora quem manda é o outro, sem dar a volta
        if (idArtista.equals(Acervo.chaveArtista(artistaFinal(limpo)))) edicoes.salvar(destino, Edicoes.Campo.ARTISTA, null);
        edicoes.salvar(idArtista, Edicoes.Campo.ARTISTA, limpo);
        apelidos = edicoes.doCampo(Edicoes.Campo.ARTISTA);
        notificar();
        pedirArtistas(false);
    }

    /** O nome que vale para o artista, depois das pastas juntadas no Editar; {@code nome} se não há nenhuma. */
    private String artistaFinal(String nome) {
        String atual = nome;
        for (int i = 0; i < MAXIMO_JUNTADOS; i++) {
            String proximo = apelidos.get(Acervo.chaveArtista(atual));
            if (proximo == null) return atual;
            atual = proximo;
        }
        return atual;
    }

    /** Restaurar original do artista: os shows que foram juntados a ele voltam para as pastas deles. */
    private void desfazerJuntados(String idArtista) {
        for (Map.Entry<String, String> apelido : apelidos.entrySet()) {
            if (idArtista.equals(Acervo.chaveArtista(artistaFinal(apelido.getValue())))) {
                edicoes.salvar(apelido.getKey(), Edicoes.Campo.ARTISTA, null);
            }
        }
        apelidos = edicoes.doCampo(Edicoes.Campo.ARTISTA);
    }

    @Override
    public void lerDadosDoArtista(String idArtista) {
        Titulo artista = titulo(idArtista);
        if (artista == null || !artista.ehArtista() || temTexto(artista.sinopse) || idArtista.equals(Acervo.chaveArtista(null))) return;
        if (!artigosPedidos.add(idArtista)) return;
        final String nome = artista.titulo;
        filaDeArtistas.execute(() -> {
            try {
                WikipediaArtista.Artigo artigo = new WikipediaArtista().artigo(nome);
                if (artigo == null) return;
                Acervo.Artista linha = acervo.artistas().get(idArtista);
                if (linha == null) {
                    linha = new Acervo.Artista();
                    linha.chave = idArtista;
                    linha.nome = nome;
                }
                linha.descricao = artigo.texto;
                linha.resumo = artigo.resumo;
                acervo.gravarArtista(linha);
                reconstruirENotificar();
            } catch (IOException | JSONException | RuntimeException e) {
                // sem texto: a página fica só com a capa e os shows
                artigosPedidos.remove(idArtista);
            }
        });
    }

    /** A atual primeiro; depois as outras do TMDB; sem TMDB, três cores. */
    private List<String> opcoes(String idTitulo, boolean capa) {
        Titulo t = titulo(idTitulo);
        if (t == null) return Collections.emptyList();
        return opcoes(idTitulo, capa ? t.referenciaCapa() : t.referenciaFundo(), achado(retrato.tmdb.get(idTitulo)), capa);
    }

    private List<String> opcoes(String idTitulo, String atual, Acervo.Tmdb dados, boolean capa) {
        List<String> lista = new ArrayList<>();
        lista.add(atual);
        if (dados != null) {
            List<String> doTmdb = capa ? dados.capas : dados.fundos;
            // A primeira do TMDB é a que já foi baixada (capa / fundo do título)
            for (int i = 1; i < doTmdb.size(); i++) lista.add("url:" + doTmdb.get(i));
        }
        if (lista.size() == 1) {
            int base = corDe(idTitulo);
            for (int i = 1; i <= 3; i++) {
                String cor = referenciaCor(CORES[(indice(base) + i * 3) % CORES.length]);
                if (!lista.contains(cor)) lista.add(cor);
            }
        }
        return lista;
    }

    @Override
    public boolean temChaveTmdb() {
        return new BuscaTmdb(app).temChave();
    }

    @Override
    public List<ResultadoTmdb> buscarTmdb(String idTitulo, String consulta) {
        Titulo t = titulo(idTitulo);
        if (t == null) return Collections.emptyList();
        boolean serie = t.tipo == Tipo.SERIE;
        Acervo.Tmdb atual = achado(retrato.tmdb.get(idTitulo));
        List<ResultadoTmdb> lista = new ArrayList<>();
        for (BuscaTmdb.Resultado achado : new BuscaTmdb(app).pesquisar(consulta, serie)) {
            resultados.put(achado.id, achado);
            Tipo tipo = serie ? Tipo.SERIE
                    : t.tipo == Tipo.SHOW || t.tipo == Tipo.STANDUP || t.tipo == Tipo.DESENHO ? t.tipo : Tipo.FILME;
            ResultadoTmdb r = new ResultadoTmdb(achado.id, achado.titulo, achado.ano, tipo, "", corDe(achado.id), achado.capa);
            r.atual = atual != null && achado.id.equals(idResultado(atual));
            lista.add(r);
        }
        return lista;
    }

    @Override
    public PreviaTmdb previaResultadoTmdb(String idTitulo, String idResultado) {
        BuscaTmdb.Resultado escolhido = resultados.get(idResultado);
        Titulo atual = titulo(idTitulo);
        if (escolhido == null || atual == null) return null;
        Acervo.Tmdb linha = new BuscaTmdb(app).escolher(escolhido, idTitulo);
        if (linha.situacao != Acervo.ACHADO) return null;
        previas.put(idTitulo, linha);
        // O que a tela vai mostrar depois do Salvar: os dados do resultado, com o que foi editado à
        // mão por cima; a capa e o fundo são os do resultado (os de antes eram de outro título).
        // Editado neste vídeo: a herança de outro de mesmo nome cai ao escolher um resultado
        Titulo t = atual.copia();
        if (t.naoIdentificado) {
            t.naoIdentificado = false;
            t.tipo = Tipo.FILME;
        }
        Set<Campo> aMao = editadosNele(idTitulo);
        if (!aMao.contains(Campo.TITULO) && temTexto(linha.titulo)) t.titulo = linha.titulo;
        if (!aMao.contains(Campo.TITULO_ORIGINAL) && linha.tituloOriginal != null) t.tituloOriginal = linha.tituloOriginal;
        if (!aMao.contains(Campo.ANO) && linha.ano != null) t.ano = linha.ano;
        if (!aMao.contains(Campo.GENEROS)) t.generos = linha.generos == null ? "" : linha.generos;
        if (!aMao.contains(Campo.SINOPSE)) t.sinopse = linha.sinopse == null ? "" : linha.sinopse;
        // Duração: a do arquivo, quando já lida; senão, a do TMDB
        if (!aMao.contains(Campo.DURACAO) && atual.duracaoSegundos <= 0) t.duracaoMin = linha.duracaoMin;
        t.capaImagem = null;
        t.fundoImagem = null;
        t.capaTmdb = existe(linha.capa);
        t.fundoTmdb = existe(linha.fundo);
        return new PreviaTmdb(t, opcoes(idTitulo, t.referenciaCapa(), linha, true), opcoes(idTitulo, t.referenciaFundo(), linha, false));
    }

    @Override
    public boolean selecionarResultadoTmdb(String idTitulo, String idResultado) {
        BuscaTmdb.Resultado escolhido = resultados.get(idResultado);
        Titulo atual = titulo(idTitulo);
        if (escolhido == null || atual == null) return false;
        Acervo.Tmdb linha = previas.remove(idTitulo);
        if (linha == null || !idResultado.equals(idResultado(linha))) linha = new BuscaTmdb(app).escolher(escolhido, idTitulo);
        if (linha.situacao != Acervo.ACHADO) return false;
        acervo.gravarTmdb(linha);
        // Capa e fundo escolhidos antes eram de outro título: valem os do resultado
        Map<Edicoes.Campo, String> semImagem = new EnumMap<>(Edicoes.Campo.class);
        semImagem.put(Edicoes.Campo.CAPA, null);
        semImagem.put(Edicoes.Campo.FUNDO, null);
        edicoes.salvar(idTitulo, semImagem);
        // A escolha é deste vídeo: a herança de outro de mesmo nome não passa por cima dela
        if (nomeComum(idTitulo) != null) edicoes.cortarHeranca(idTitulo);
        // Não identificado escolhido no TMDB: vira filme (é um arquivo só) e sai de Não identificados
        if (atual.naoIdentificado) edicoes.salvar(idTitulo, Edicoes.Campo.TIPO, Tipo.FILME.name());
        // Série: os episódios buscam de novo, agora pela série escolhida
        if (Acervo.ehSerie(idTitulo)) apagarEpisodiosDoTmdb(idTitulo);
        reconstruir();
        atualizarBiblioteca();
        return true;
    }

    @Override
    public void salvarEdicao(String idTitulo, Map<Campo, String> valores) {
        Titulo atual = titulo(idTitulo);
        if (atual == null || valores == null) return;
        Map<Edicoes.Campo, String> mudados = new EnumMap<>(Edicoes.Campo.class);
        for (Map.Entry<Campo, String> item : valores.entrySet()) {
            String novo = item.getValue() == null ? "" : item.getValue().trim();
            String antes = atual.valor(item.getKey());
            if (!novo.equals(antes == null ? "" : antes)) mudados.put(Edicoes.Campo.valueOf(item.getKey().name()), novo);
        }
        if (mudados.isEmpty()) return;
        edicoes.salvar(idTitulo, nomeComum(idTitulo), mudados);
        // Sem dados do TMDB (ou não identificado): busca de novo com o título e o ano editados
        boolean busca = mudados.containsKey(Edicoes.Campo.TITULO) || mudados.containsKey(Edicoes.Campo.ANO)
                || mudados.containsKey(Edicoes.Campo.TIPO);
        if (busca && achado(retrato.tmdb.get(idTitulo)) == null) {
            acervo.apagarTmdb(idTitulo);
            if (Acervo.ehSerie(idTitulo)) apagarEpisodiosDoTmdb(idTitulo);
            atualizarBiblioteca();
        }
        notificar();
    }

    @Override
    public void restaurarOriginal(String idTitulo) {
        if (Acervo.ehArtista(idTitulo)) desfazerJuntados(idTitulo);
        // Vídeo com nome em comum: corta também a herança, só dele (D13)
        edicoes.restaurarOriginal(idTitulo, nomeComum(idTitulo));
        apelidos = edicoes.doCampo(Edicoes.Campo.ARTISTA);
        Acervo.Tmdb dados = retrato.tmdb.get(idTitulo);
        if (dados != null && dados.escolhido) {
            acervo.apagarTmdb(idTitulo);
            if (Acervo.ehSerie(idTitulo)) apagarEpisodiosDoTmdb(idTitulo);
            reconstruir();
            atualizarBiblioteca();
        } else {
            notificar();
        }
    }

    @Override
    public boolean escolherCapa(String idTitulo, String referencia) {
        return escolherImagem(idTitulo, Edicoes.Campo.CAPA, referencia);
    }

    @Override
    public boolean escolherFundo(String idTitulo, String referencia) {
        return escolherImagem(idTitulo, Edicoes.Campo.FUNDO, referencia);
    }

    private boolean escolherImagem(String idTitulo, Edicoes.Campo campo, String referencia) {
        if (referencia == null) return false;
        String gravar = referencia;
        if (referencia.startsWith("url:")) {
            ImagemDoAparelho.Resultado baixada = ImagemDoAparelho.doEndereco(app, referencia.substring(4));
            if (!baixada.ok()) return false;
            gravar = baixada.referencia;
        }
        edicoes.salvar(idTitulo, nomeComum(idTitulo), campo, gravar);
        notificar();
        return true;
    }

    /** Nome em comum do vídeo (D13); nulo em série, coletânea, artista e não identificado. */
    private String nomeComum(String id) {
        Acervo.Video v = id == null ? null : retrato.videos.get(id);
        return v == null ? null : Acervo.chaveNome(v);
    }

    /** Os campos editados neste título, sem os herdados. */
    private Set<Campo> editadosNele(String id) {
        Map<Edicoes.Campo, String> proprias = edicoes.ler(id);
        Set<Campo> campos = EnumSet.noneOf(Campo.class);
        for (Campo campo : Campo.values()) {
            if (proprias.containsKey(Edicoes.Campo.valueOf(campo.name()))) campos.add(campo);
        }
        return campos;
    }

    private void apagarEpisodiosDoTmdb(String idSerie) {
        List<Temporada> temporadas = retrato.temporadas.get(idSerie);
        if (temporadas == null) return;
        for (Temporada temporada : temporadas) {
            for (Episodio e : temporada.episodios) acervo.apagarTmdb(e.id);
        }
    }

    // =====================================================================
    // Pastas
    // =====================================================================

    @Override
    public List<Pasta> pastas() {
        Retrato r = retrato;
        List<Pasta> lista = new ArrayList<>();
        for (Pastas.Pasta p : pastas.lista()) {
            Pasta m = new Pasta();
            m.id = p.caminho;
            m.nome = p.nome();
            m.origem = p.origem == Pastas.Origem.HD_EXTERNO ? Origem.HD_EXTERNO : Origem.INTERNO;
            m.caminho = exibirPasta(p);
            m.tipo = tipoPasta(p.tipo);
            m.reconhecidaPeloNome = p.tipo != null && !p.escolhido;
            int[] conta = r.contagem.get(p.caminho);
            if (conta != null) {
                m.videos = conta[0];
                m.series = conta[1];
                m.episodios = conta[2];
            }
            lista.add(m);
        }
        return lista;
    }

    @Override
    public List<Titulo> naoIdentificados() {
        Retrato r = retrato;
        List<Titulo> lista = new ArrayList<>();
        for (String id : r.naoIdentificados) {
            Titulo t = comEstado(r, r.titulos.get(id));
            if (t.naoIdentificado) lista.add(t);
        }
        return lista;
    }

    @Override
    public boolean hdConectado() {
        for (ArquivosDoAparelho.Raiz raiz : ArquivosDoAparelho.raizes(app)) {
            if (raiz.removivel) return true;
        }
        return false;
    }

    @Override
    public boolean podeLerVideos() {
        return ArquivosDoAparelho.podeLerVideos(app);
    }

    @Override
    public void definirTipoPasta(String idPasta, TipoPasta tipo) {
        Pastas.Tipo novo = tipo == TipoPasta.FILMES ? Pastas.Tipo.FILMES : tipo == TipoPasta.SERIES ? Pastas.Tipo.SERIES
                : tipo == TipoPasta.DESENHOS ? Pastas.Tipo.DESENHOS
                : tipo == TipoPasta.SHOWS ? Pastas.Tipo.SHOWS : tipo == TipoPasta.STANDUP ? Pastas.Tipo.STANDUP
                : tipo == TipoPasta.IGNORAR ? Pastas.Tipo.IGNORAR : null;
        pastas.definirTipo(idPasta, novo);
        atualizarBiblioteca();
    }

    @Override
    public void adicionarPasta(Origem origem, String caminho) {
        pastas.adicionar(caminho, origem == Origem.HD_EXTERNO ? Pastas.Origem.HD_EXTERNO : Pastas.Origem.INTERNO);
        atualizarBiblioteca();
    }

    /** A varredura tira os vídeos da pasta que saiu (mesmo com o HD desligado). */
    @Override
    public void removerPasta(String idPasta) {
        pastas.remover(idPasta);
        atualizarBiblioteca();
    }

    private static TipoPasta tipoPasta(Pastas.Tipo tipo) {
        if (tipo == null) return TipoPasta.NENHUM;
        switch (tipo) {
            case FILMES: return TipoPasta.FILMES;
            case SERIES: return TipoPasta.SERIES;
            case DESENHOS: return TipoPasta.DESENHOS;
            case SHOWS: return TipoPasta.SHOWS;
            case STANDUP: return TipoPasta.STANDUP;
            default: return TipoPasta.IGNORAR;
        }
    }

    // =====================================================================
    // Estado
    // =====================================================================

    @Override
    public void definirFavorito(String idTitulo, boolean favorito) {
        Acervo.Estado e = estado(idTitulo).copia();
        e.favorito = favorito;
        e.favoritado = favorito ? System.currentTimeMillis() : 0;
        gravarEstado(idTitulo, e);
    }

    @Override
    public void definirAssistido(String id, boolean assistido) {
        if (Acervo.ehSerie(id)) {
            List<Temporada> temporadas = retrato.temporadas.get(id);
            if (temporadas == null) return;
            for (Temporada temporada : temporadas) {
                for (Episodio ep : temporada.episodios) marcarAssistido(ep.id, assistido);
            }
        } else {
            marcarAssistido(id, assistido);
        }
        notificar();
    }

    private void marcarAssistido(String id, boolean assistido) {
        Acervo.Estado e = estado(id).copia();
        e.assistido = assistido;
        if (assistido) e.posicao = 0;
        estados.put(id, e);
        acervo.gravarEstado(id, e);
    }

    @Override
    public void definirPosicao(String id, int segundos) {
        Acervo.Estado e = estado(id).copia();
        e.posicao = Math.max(0, segundos);
        e.visto = System.currentTimeMillis();
        gravarEstado(id, e);
    }

    @Override
    public boolean definirNota(String idFilme, Integer nota) {
        Titulo t = titulo(idFilme);
        if (t == null || !t.podeDarNota() || (nota != null && !Modelo.notaValida(nota))) return false;
        Acervo.Estado e = estado(idFilme).copia();
        e.nota = nota;
        gravarEstado(idFilme, e);
        return true;
    }

    private void gravarEstado(String id, Acervo.Estado e) {
        estados.put(id, e);
        acervo.gravarEstado(id, e);
        notificar();
    }

    // =====================================================================
    // Atualizar biblioteca (varredura + TMDB), fora da tela
    // =====================================================================

    @Override
    public void atualizarBiblioteca() {
        synchronized (this) {
            if (emAndamento) {
                repetir = true;
                return;
            }
            emAndamento = true;
        }
        notificar();
        trabalho.execute(() -> {
            boolean outraVez;
            boolean deNovo;
            do {
                deNovo = false;
                synchronized (this) {
                    repetir = false;
                }
                try {
                    boolean pode = ArquivosDoAparelho.podeLerVideos(app);
                    andar(0, 0, 0);
                    if (pode) deNovo = Varredura.listar(app, this::reconstruirENotificar);
                    // O TMDB vem antes de ler os arquivos: só precisa do nome, e a leitura num HD USB
                    // leva minutos (esperar por ela fazia o Atualizar parecer parado)
                    boolean tudo = buscarTudo;
                    buscarNoTmdb(tudo);
                    if (tudo) buscarTudo = false;
                    reconstruirENotificar();
                    fecharResultado();
                    pedirArtistas(tudo);
                    andar(1, 0, 0);
                    if (pode) Varredura.lerDadosTecnicos(app, this::reconstruirENotificar,
                            (feitos, total) -> passoLongo(1, feitos, total));
                    // Quadros por último (é o passo lento) e só para quem ficou sem imagem do TMDB
                    andar(2, 0, 0);
                    if (pode) Varredura.miniaturas(app, comImagemDoTmdb(), this::reconstruirENotificar,
                            (feitos, total) -> passoLongo(2, feitos, total));
                } catch (RuntimeException e) {
                    // um arquivo ou resposta estranha não derruba o app; a próxima atualização tenta de novo
                }
                synchronized (this) {
                    outraVez = repetir;
                    if (!outraVez) emAndamento = false;
                }
            } while (outraVez);
            reconstruirENotificar();
            ultimaVarredura = SystemClock.elapsedRealtime();
            if (!deNovo) {
                varredurasDeNovo = 0;
            } else if (varredurasDeNovo < VARREDURAS_DE_NOVO) {
                varredurasDeNovo++;
                tela.postDelayed(this::atualizarBiblioteca, NOVA_VARREDURA_MS);
            }
        });
    }

    /** Pedido novo com a leitura dos arquivos em andamento: a rodada recomeça, com o TMDB primeiro. */
    private static final class Recomecar extends RuntimeException {
    }

    private void passoLongo(int qual, int feitos, int total) {
        andar(qual, feitos, total);
        if (repetir) throw new Recomecar();
    }

    @Override
    public void atualizarAgora() {
        buscarTudo = true;
        mostrarResultado = true;
        indexarPastas();
        atualizarBiblioteca();
    }

    /**
     * O Android lista os arquivos do HD pelo índice dele: pede para ele olhar as pastas agora (arquivo
     * recém-copiado ainda não está lá) e procura de novo ao terminar cada uma.
     */
    private void indexarPastas() {
        agora.execute(() -> {
            Set<String> montadas = pastasMontadas();
            if (montadas.isEmpty()) return;
            try {
                MediaScannerConnection.scanFile(app, montadas.toArray(new String[0]), null,
                        (caminho, uri) -> atualizarBiblioteca());
            } catch (RuntimeException e) {
                // sem o índice novo, fica o que o Android já conhece
            }
        });
    }

    @Override
    public String resultadoDaAtualizacao() {
        return resultado;
    }

    /**
     * "12 atualizados · 2 não identificados", ao terminar o TMDB: se foi pedido (Atualizar, HD ligado)
     * ou se veio algo novo. Com uma tela da biblioteca aberta, aparece também como aviso.
     */
    private void fecharResultado() {
        if (!mostrarResultado && atualizadosNaRodada == 0) return;
        mostrarResultado = false;
        int naoIdentificados = 0;
        int semResposta = 0;
        Retrato r = retrato;
        for (Acervo.Video v : r.videos.values()) {
            Acervo.Tmdb t = r.tmdb.get(v.caminho);
            if (t != null && t.situacao == Acervo.ERRO) semResposta++;
            else if (achado(t) == null) naoIdentificados++;
        }
        Resources res = app.getResources();
        StringBuilder texto = new StringBuilder(quantos(res, R.plurals.st_n_atualizados, atualizadosNaRodada))
                .append(" · ").append(quantos(res, R.plurals.st_n_nao_identificados, naoIdentificados));
        if (semResposta > 0) texto.append(" · ").append(quantos(res, R.plurals.st_n_sem_resposta, semResposta));
        final String pronto = texto.toString();
        atualizadosNaRodada = 0;
        resultado = pronto;
        Log.i(LOG, "TMDB: " + pronto);
        tela.post(() -> {
            avisarAndamento.run();
            if (!ouvintes.isEmpty()) Toast.makeText(app, pronto, Toast.LENGTH_LONG).show();
        });
    }

    /** Em português, zero também usa a forma "one" do plural: aqui fica "0 atualizados". */
    private static String quantos(Resources res, int plural, int n) {
        return res.getQuantityString(plural, n == 0 ? 2 : n, n);
    }

    @Override
    public void conferirBiblioteca() {
        // Na fila da varredura: se houver uma em andamento, confere depois dela (e ela já conta)
        trabalho.execute(() -> {
            boolean hdMudou = !pastasMontadas().equals(retrato.montadas);
            if (!Preferencias.atualizarAoAbrir(app)) {
                // Sem procurar nada: só some ou volta o que já está no banco
                if (hdMudou) reconstruirENotificar();
                return;
            }
            if (hdMudou || SystemClock.elapsedRealtime() - ultimaVarredura >= CONFERIR_MS) {
                indexarPastas();
                atualizarBiblioteca();
            }
        });
    }

    /**
     * Busca no TMDB o que ainda não tem dados (ou deu erro de rede há mais de 10 min); {@code tudo}:
     * também o que não achou ou veio sem capa. Um vídeo com problema não segura os outros.
     */
    private void buscarNoTmdb(boolean tudo) {
        BuscaTmdb busca = new BuscaTmdb(app);
        if (!busca.temChave()) return;
        colecaoSemResposta = false;
        Map<String, Acervo.Tmdb> tmdb = acervo.tmdb();
        Set<String> seriesFeitas = new HashSet<>();
        int feitos = 0;
        List<Acervo.Video> videos = acervo.videos();
        int vistos = 0;
        for (Acervo.Video v : videos) {
            andar(0, vistos++, videos.size());
            if (!new File(v.pasta).isDirectory()) continue; // HD desligado: fica para quando ligar
            try {
                if (buscarDoVideo(busca, v, tmdb, seriesFeitas, tudo) && ++feitos % 5 == 0) reconstruirENotificar();
            } catch (RuntimeException e) {
                Log.w(LOG, "TMDB: não deu para buscar " + v.caminho, e);
            }
        }
    }

    /** Busca os dados de um vídeo; devolve true se foi ao TMDB. */
    private boolean buscarDoVideo(BuscaTmdb busca, Acervo.Video v, Map<String, Acervo.Tmdb> tmdb, Set<String> seriesFeitas, boolean tudo) {
        Map<Edicoes.Campo, String> edicao = edicoes.valendo(v.caminho, Acervo.chaveNome(v));
        Acervo.Tmdb antes = tmdb.get(v.caminho);
        switch (v.tipo) {
            case FILME:
            case DESENHO:
            case SHOW:
            case STANDUP: {
                if (!precisa(antes, tudo)) return v.tipo == Classificador.Tipo.FILME && completarColecao(busca, v, antes);
                String titulo = edicao.containsKey(Edicoes.Campo.TITULO) ? edicao.get(Edicoes.Campo.TITULO)
                        : v.artista != null ? v.artista + " " + v.titulo : v.titulo;
                Integer ano = edicao.containsKey(Edicoes.Campo.ANO) ? numero(edicao.get(Edicoes.Campo.ANO)) : v.ano;
                if (!temTexto(titulo)) return false;
                guardarDoTmdb(busca.filme(v.caminho, v.caminho, titulo, ano), antes);
                return true;
            }
            case EPISODIO: {
                String chaveSerie = Acervo.chaveSerie(v.serie);
                Acervo.Tmdb serie = tmdb.get(chaveSerie);
                boolean precisaSerie = precisa(serie, tudo) && !seriesFeitas.contains(chaveSerie);
                if (!precisa(antes, tudo) && !precisaSerie) return false;
                Map<Edicoes.Campo, String> daSerie = edicoes.ler(chaveSerie);
                boolean escolhida = serie != null && serie.escolhido && serie.situacao == Acervo.ACHADO;
                String nome = escolhida && serie.titulo != null ? serie.titulo
                        : daSerie.containsKey(Edicoes.Campo.TITULO) ? daSerie.get(Edicoes.Campo.TITULO) : v.serie;
                Integer ano = escolhida ? numero(serie.ano)
                        : daSerie.containsKey(Edicoes.Campo.ANO) ? numero(daSerie.get(Edicoes.Campo.ANO)) : v.ano;
                Acervo.Tmdb[] achados = busca.episodio(v.caminho, chaveSerie, v.caminho, nome, ano,
                        v.temporada, v.episodio, precisaSerie);
                guardarDoTmdb(achados[0], antes);
                if (precisaSerie) {
                    seriesFeitas.add(chaveSerie);
                    Acervo.Tmdb linhaSerie = achados[1];
                    if (linhaSerie == null && achados[0].situacao != Acervo.ERRO && achado(serie) == null) {
                        linhaSerie = new Acervo.Tmdb();
                        linhaSerie.chave = chaveSerie;
                        linhaSerie.situacao = Acervo.NAO_ACHADO;
                        linhaSerie.filme = false;
                        linhaSerie.buscado = System.currentTimeMillis();
                    }
                    if (linhaSerie != null) {
                        acervo.gravarTmdb(linhaSerie);
                        tmdb.put(chaveSerie, linhaSerie);
                    }
                }
                return true;
            }
            default: {
                // Não identificado com tipo e título editados à mão: busca como filme / show
                String tipo = edicao.get(Edicoes.Campo.TIPO);
                String titulo = edicao.get(Edicoes.Campo.TITULO);
                if (tipo == null || Tipo.SERIE.name().equals(tipo) || !temTexto(titulo)) return false;
                if (!precisa(antes, tudo)) return false;
                guardarDoTmdb(busca.filme(v.caminho, v.caminho, titulo, numero(edicao.get(Edicoes.Campo.ANO))), antes);
                return true;
            }
        }
    }

    /** Grava o que veio do TMDB; erro de rede ou "não achou" não apaga o que já tinha. */
    private void guardarDoTmdb(Acervo.Tmdb novo, Acervo.Tmdb antes) {
        if (novo.situacao == Acervo.ACHADO) atualizadosNaRodada++;
        else if (achado(antes) != null) return;
        acervo.gravarTmdb(novo);
    }

    /**
     * Filme achado antes da versão 3 do banco: consulta a coleção uma vez, sem buscar tudo de novo.
     * O TMDB não respondeu: os outros ficam para a próxima rodada. Devolve true se foi ao TMDB.
     */
    private boolean completarColecao(BuscaTmdb busca, Acervo.Video v, Acervo.Tmdb antes) {
        if (colecaoSemResposta || achado(antes) == null || antes.idTmdb <= 0
                || antes.colecaoId != Acervo.COLECAO_NAO_CONSULTADA) return false;
        if (busca.completarColecao(antes, v.caminho)) acervo.gravarTmdb(antes);
        else colecaoSemResposta = true;
        return true;
    }

    /**
     * Pede a busca de artista, na fila dela: a rodada não espera nem conta no andamento, e
     * {@link #buscarArtistas} avisa as telas quando as capas chegam.
     */
    private void pedirArtistas(boolean tudo) {
        if (tudo) artistasTudo.set(true);
        if (!artistasNaFila.compareAndSet(false, true)) return;
        filaDeArtistas.execute(() -> {
            artistasNaFila.set(false);
            buscarArtistas(artistasTudo.getAndSet(false));
        });
    }

    /**
     * Capa de artista, depois do TMDB ({@code tudo}: também quem não achou ou perdeu a imagem). A
     * primeira falta de resposta encerra a rodada: API fora do ar fica no bloco de cor e tenta depois.
     * Pedido novo não corta a busca: fica na fila e acha feito o que esta já buscou.
     */
    private void buscarArtistas(boolean tudo) {
        try {
            BuscaArtista busca = null;
            Map<String, Acervo.Artista> conhecidos = acervo.artistas();
            Set<String> vistos = new HashSet<>();
            int feitos = 0;
            for (Acervo.Video v : acervo.videos()) {
                if (v.tipo != Classificador.Tipo.SHOW || !temTexto(v.artista) || !new File(v.pasta).isDirectory()) continue;
                // Pasta juntada a outro artista no Editar: quem precisa de capa é ele
                String nome = artistaFinal(v.artista);
                String chave = Acervo.chaveArtista(nome);
                if (!vistos.add(chave)) continue;
                Acervo.Artista antes = conhecidos.get(chave);
                if (!precisaArtista(antes, tudo)) continue;
                if (busca == null) busca = new BuscaArtista(app);
                Acervo.Artista novo = busca.buscar(chave, nome);
                if (antes != null && novo.descricao == null) {
                    novo.descricao = antes.descricao;
                    novo.resumo = antes.resumo;
                }
                // Falha não apaga a imagem que já tinha
                if (novo.situacao == Acervo.ACHADO || antes == null || antes.situacao != Acervo.ACHADO) acervo.gravarArtista(novo);
                if (++feitos % 5 == 0) reconstruirENotificar();
                if (novo.situacao == Acervo.ERRO) break;
            }
            if (feitos % 5 != 0) reconstruirENotificar();
        } catch (RuntimeException e) {
            Log.w(LOG, "artista: não deu para buscar", e);
        }
    }

    private static boolean precisaArtista(Acervo.Artista a, boolean tudo) {
        if (a == null || a.situacao == Acervo.PENDENTE) return true;
        if (tudo && (a.situacao != Acervo.ACHADO || existe(a.foto) == null)) return true;
        return a.situacao == Acervo.ERRO && System.currentTimeMillis() - a.buscado > NOVA_TENTATIVA_MS;
    }

    /** Caminhos com fundo (filme, desenho, show, stand-up) ou imagem do episódio do TMDB: não precisam de quadro. */
    private Set<String> comImagemDoTmdb() {
        Set<String> caminhos = new HashSet<>();
        for (Acervo.Tmdb t : acervo.tmdb().values()) {
            if (t.situacao != Acervo.ACHADO || Acervo.ehSerie(t.chave)) continue;
            if (existe(t.fundo) != null || existe(t.miniatura) != null) caminhos.add(t.chave);
        }
        return caminhos;
    }

    private static boolean precisa(Acervo.Tmdb t, boolean tudo) {
        if (t == null || t.situacao == Acervo.PENDENTE) return true;
        if (tudo) {
            if (t.situacao != Acervo.ACHADO) return true;
            // Achou, mas a capa e o fundo não baixaram (filme, show ou a série; episódio não tem capa)
            boolean temCapa = t.filme || Acervo.ehSerie(t.chave);
            if (temCapa && !t.escolhido && existe(t.capa) == null && existe(t.fundo) == null) return true;
        }
        return t.situacao == Acervo.ERRO && System.currentTimeMillis() - t.buscado > NOVA_TENTATIVA_MS;
    }

    private void reconstruir() {
        synchronized (montagem) {
            retrato = montar();
        }
    }

    private void reconstruirENotificar() {
        reconstruir();
        notificar();
    }

    /** Avisos juntos em 300 ms (a varredura muda muito de uma vez; a tela relê uma vez só). */
    private void notificar() {
        tela.removeCallbacks(avisar);
        tela.postDelayed(avisar, AVISO_MS);
    }

    private final Runnable avisar = () -> {
        for (Runnable ouvinte : ouvintes) ouvinte.run();
    };

    /**
     * HD ligado: varre; desligado: some com os títulos dele. O Android lista os arquivos do HD pelo
     * índice dele: ao terminar de indexar, varre de novo para pegar o que acabou de entrar.
     */
    private void ouvirHd() {
        IntentFilter filtro = new IntentFilter();
        filtro.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filtro.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        filtro.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        filtro.addAction(Intent.ACTION_MEDIA_EJECT);
        filtro.addAction(Intent.ACTION_MEDIA_REMOVED);
        filtro.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
        filtro.addDataScheme("file");
        ContextCompat.registerReceiver(app, new BroadcastReceiver() {
            @Override
            public void onReceive(Context contexto, Intent intent) {
                String acao = intent.getAction();
                if (Intent.ACTION_MEDIA_MOUNTED.equals(acao)) {
                    mostrarResultado = true;
                    atualizarBiblioteca();
                } else if (Intent.ACTION_MEDIA_SCANNER_FINISHED.equals(acao)) {
                    atualizarBiblioteca();
                } else {
                    trabalho.execute(RepositorioReal.this::reconstruirENotificar);
                }
            }
        }, filtro, ContextCompat.RECEIVER_EXPORTED);
    }

    // =====================================================================
    // Apoio
    // =====================================================================

    /** "HD / Filmes / Nome.mkv": origem e o caminho a partir da raiz do armazenamento. */
    private String exibir(Retrato r, String pasta, String caminho) {
        Pastas.Pasta p = r.pastas.get(pasta);
        return exibir(p == null ? Pastas.Origem.INTERNO : p.origem, pasta, caminho);
    }

    private String exibir(Pastas.Origem origem, String pasta, String caminho) {
        String prefixo = origem == Pastas.Origem.HD_EXTERNO ? "HD" : "Interno";
        String base = pasta;
        ArquivosDoAparelho.Raiz raiz = ArquivosDoAparelho.raizDe(app, pasta);
        if (raiz != null) base = raiz.pasta.getAbsolutePath();
        else if (new File(pasta).getParent() != null) base = new File(pasta).getParent();
        String resto = caminho.length() > base.length() ? caminho.substring(base.length()) : "";
        StringBuilder texto = new StringBuilder(prefixo);
        for (String parte : resto.split("/")) {
            if (!parte.isEmpty()) texto.append(" / ").append(parte);
        }
        return texto.toString();
    }

    private String exibirPasta(Pastas.Pasta p) {
        return exibir(p.origem, p.caminho, p.caminho);
    }

    private static Acervo.Tmdb achado(Acervo.Tmdb t) {
        return t != null && t.situacao == Acervo.ACHADO ? t : null;
    }

    private static String idResultado(Acervo.Tmdb t) {
        return (t.filme ? "filme:" : "serie:") + t.idTmdb;
    }

    private static String qualidade(Acervo.Video v) {
        String q = Faixas.qualidade(v.largura, v.altura);
        String hdr = hdr(v.infoVideo);
        if (q.isEmpty()) return hdr;
        return hdr.isEmpty() ? q : q + " HDR";
    }

    private static String hdr(String infoVideo) {
        if (infoVideo == null) return "";
        if (infoVideo.contains("HDR10")) return "HDR10";
        if (infoVideo.contains("HLG")) return "HLG";
        return "";
    }

    private static boolean temNome(Capitulos capitulos) {
        for (Capitulos.Capitulo c : capitulos.lista()) if (!c.nome.isEmpty()) return true;
        return false;
    }

    private static String existe(String caminho) {
        return caminho != null && new File(caminho).isFile() ? caminho : null;
    }

    private static boolean temTexto(String texto) {
        return texto != null && !texto.trim().isEmpty();
    }

    private static String primeiro(String generos) {
        if (generos == null || generos.isEmpty()) return "";
        return generos.split("\\s*[,/|]\\s*")[0].trim();
    }

    private static Integer numero(String texto) {
        if (texto == null) return null;
        try {
            return Integer.valueOf(texto.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String semExtensao(String nome) {
        int ponto = nome.lastIndexOf('.');
        return ponto > 0 ? nome.substring(0, ponto) : nome;
    }

    private static String juntar(String separador, String... partes) {
        StringBuilder texto = new StringBuilder();
        for (String parte : partes) {
            if (parte == null || parte.isEmpty()) continue;
            if (texto.length() > 0) texto.append(separador);
            texto.append(parte);
        }
        return texto.toString();
    }

    private static String juntarLista(List<String> partes) {
        return juntar(" · ", partes.toArray(new String[0]));
    }

    private static int corDe(String id) {
        return CORES[Math.abs(id.hashCode() % CORES.length)];
    }

    private static int indice(int cor) {
        for (int i = 0; i < CORES.length; i++) if (CORES[i] == cor) return i;
        return 0;
    }

    /** "cor:#RRGGBB" → cor ({@code padrao} se não der para ler). */
    private static int corDe(String referencia, int padrao) {
        try {
            return cor(referencia.substring(Edicoes.PREFIXO_COR.length()));
        } catch (RuntimeException e) {
            return padrao;
        }
    }

    private static String referenciaCor(int cor) {
        return Edicoes.PREFIXO_COR + String.format(Locale.ROOT, "#%06X", cor & 0xFFFFFF);
    }

    /** "#rrggbb" para ARGB opaco. */
    static int cor(String hex) {
        return (int) (0xFF000000L | Long.parseLong(hex.substring(1), 16));
    }

    private static Collator collator() {
        Collator ordem = Collator.getInstance(Locale.forLanguageTag("pt-BR"));
        ordem.setStrength(Collator.PRIMARY);
        return ordem;
    }

    private static String normalizar(String texto) {
        if (texto == null) return "";
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.toLowerCase(Locale.ROOT).trim();
    }

}
