// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.dados;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Modelo de dados das telas novas. Java puro, sem nada do banco do Nova.
 * Na F1 os dados vêm do {@link RepositorioSimulado}; na F3, de uma implementação
 * de {@link RepositorioBiblioteca} ligada aos loaders do Nova.
 * Cores (capa, fundo, miniatura) são ARGB: na F1 as imagens são blocos de cor.
 */
public final class Modelo {

    private Modelo() {}

    /** Tipo do conteúdo: vem do tipo da pasta (INTERFACE.md §4). */
    public enum Tipo { FILME, SERIE, SHOW }

    /** Tipo escolhido para uma pasta; NENHUM = "escolha o tipo". */
    public enum TipoPasta { FILMES, SERIES, SHOWS, IGNORAR, NENHUM }

    /** Origem de uma pasta. */
    public enum Origem { HD_EXTERNO, INTERNO }

    /** Faixa de áudio ou de legenda. */
    public enum TipoFaixa { AUDIO, LEGENDA }

    /** Campos da tela Editar; cada um pode estar "editado à mão". */
    public enum Campo { TITULO, TITULO_ORIGINAL, TIPO, ANO, DURACAO, GENEROS, SINOPSE }

    /** Filme, série ou show da biblioteca, já com o estado simulado aplicado. */
    public static final class Titulo {
        public String id;
        public Tipo tipo;
        public String titulo;
        public String tituloOriginal;
        public String ano;
        /** Duração em minutos (série: duração média do episódio). */
        public int duracaoMin;
        /** Duração exata em segundos, para o player e o progresso. */
        public int duracaoSegundos;
        /** Gênero principal (selo do destaque). */
        public String genero;
        /** Gêneros do campo Editar. */
        public String generos;
        /** Qualidade curta do destaque ("4K HDR", "1080p"). */
        public String qualidade;
        public String sinopse;
        public int corCapa;
        public int corFundo;
        /** Capa / fundo escolhidos no aparelho (referência da {@code motor.Edicoes}); nulo = a cor. */
        public String capaImagem;
        public String fundoImagem;
        /** Cor da miniatura 16:9 (Continuar assistindo, Favoritos). */
        public int corCartao;
        public String arquivo;
        public boolean naoIdentificado;

        // Estado (EstadoSimulado)
        public boolean favorito;
        public boolean assistido;
        public int posicaoSegundos;
        /** Resultado do TMDB escolhido em "Buscar no TMDB" (vazio = o original). */
        public String idResultadoTmdb;
        public Set<Campo> editadosAMao = EnumSet.noneOf(Campo.class);

        // Série
        public int numeroTemporadas;
        public int numeroEpisodios;

        public Titulo copia() {
            Titulo t = new Titulo();
            t.id = id;
            t.tipo = tipo;
            t.titulo = titulo;
            t.tituloOriginal = tituloOriginal;
            t.ano = ano;
            t.duracaoMin = duracaoMin;
            t.duracaoSegundos = duracaoSegundos;
            t.genero = genero;
            t.generos = generos;
            t.qualidade = qualidade;
            t.sinopse = sinopse;
            t.corCapa = corCapa;
            t.corFundo = corFundo;
            t.capaImagem = capaImagem;
            t.fundoImagem = fundoImagem;
            t.corCartao = corCartao;
            t.arquivo = arquivo;
            t.naoIdentificado = naoIdentificado;
            t.favorito = favorito;
            t.assistido = assistido;
            t.posicaoSegundos = posicaoSegundos;
            t.idResultadoTmdb = idResultadoTmdb;
            t.editadosAMao = editadosAMao.isEmpty() ? EnumSet.noneOf(Campo.class) : EnumSet.copyOf(editadosAMao);
            t.numeroTemporadas = numeroTemporadas;
            t.numeroEpisodios = numeroEpisodios;
            return t;
        }

        /** Progresso de 0 a 100, arredondado para cima (04:12 de 12:14 = 35%, como no desenho). */
        public int progresso() {
            if (duracaoSegundos <= 0 || posicaoSegundos <= 0) return 0;
            return (int) Math.min(100, Math.ceil(posicaoSegundos * 100.0 / duracaoSegundos));
        }

        /** Valor do campo como aparece na tela Editar. */
        public String valor(Campo campo) {
            switch (campo) {
                case TITULO: return titulo;
                case TITULO_ORIGINAL: return tituloOriginal;
                case TIPO: return tipo == null ? "" : tipo.name();
                case ANO: return ano;
                case DURACAO: return duracaoMin + " min";
                case GENEROS: return generos;
                case SINOPSE: return sinopse;
                default: return "";
            }
        }

        /** Aplica o valor de um campo da tela Editar (TIPO pelo nome do enum; DURACAO pelo número inicial). */
        public void aplicar(Campo campo, String valor) {
            if (valor == null) return;
            switch (campo) {
                case TITULO: titulo = valor; break;
                case TITULO_ORIGINAL: tituloOriginal = valor; break;
                case TIPO:
                    try {
                        tipo = Tipo.valueOf(valor);
                    } catch (IllegalArgumentException e) {
                        // valor inválido: mantém o tipo
                    }
                    break;
                case ANO: ano = valor; break;
                case DURACAO: {
                    String numero = valor.replaceAll("^\\D*(\\d+).*$", "$1");
                    try {
                        duracaoMin = Integer.parseInt(numero);
                    } catch (NumberFormatException e) {
                        // sem número: mantém a duração
                    }
                    break;
                }
                case GENEROS: generos = valor; break;
                case SINOPSE: sinopse = valor; break;
                default: break;
            }
        }

        public boolean emAndamento() {
            return posicaoSegundos > 0 && posicaoSegundos < duracaoSegundos;
        }

        /** Minutos que faltam, arredondados. */
        public int faltamMin() {
            return Math.max(0, Math.round((duracaoSegundos - posicaoSegundos) / 60f));
        }
    }

    /** Capítulo de um vídeo (F2: lido do arquivo pelo motor). */
    public static final class Capitulo {
        public final int numero;
        public final String nome;
        public final int inicioSegundos;
        public final int corMiniatura;

        public Capitulo(int numero, String nome, int inicioSegundos, int corMiniatura) {
            this.numero = numero;
            this.nome = nome;
            this.inicioSegundos = inicioSegundos;
            this.corMiniatura = corMiniatura;
        }
    }

    /** Temporada de uma série, montada pela pasta "Temporada N". */
    public static final class Temporada {
        public final int numero;
        public final List<Episodio> episodios;

        public Temporada(int numero, List<Episodio> episodios) {
            this.numero = numero;
            this.episodios = Collections.unmodifiableList(new ArrayList<>(episodios));
        }
    }

    /** Episódio: número pelo arquivo (01.mkv), nome do TMDB. */
    public static final class Episodio {
        public String id;
        public int temporada;
        public int numero;
        public String nome;
        public String arquivo;
        public int duracaoMin;
        public int duracaoSegundos;
        public int corMiniatura;

        // Estado (EstadoSimulado)
        public boolean assistido;
        public int posicaoSegundos;

        public Episodio copia() {
            Episodio e = new Episodio();
            e.id = id;
            e.temporada = temporada;
            e.numero = numero;
            e.nome = nome;
            e.arquivo = arquivo;
            e.duracaoMin = duracaoMin;
            e.duracaoSegundos = duracaoSegundos;
            e.corMiniatura = corMiniatura;
            e.assistido = assistido;
            e.posicaoSegundos = posicaoSegundos;
            return e;
        }

        public boolean emAndamento() {
            return !assistido && posicaoSegundos > 0 && posicaoSegundos < duracaoSegundos;
        }
    }

    /** Pasta da biblioteca, com o tipo escolhido (EstadoSimulado). */
    public static final class Pasta {
        public String id;
        public String nome;
        public Origem origem;
        /** Caminho mostrado na tela ("HD / Filmes"). */
        public String caminho;
        /** Nome igual a Filmes, Séries ou Shows: tipo reconhecido sozinho. */
        public boolean reconhecidaPeloNome;
        public int videos;
        public int series;
        public int episodios;

        // Estado (EstadoSimulado)
        public TipoPasta tipo;

        public Pasta copia() {
            Pasta p = new Pasta();
            p.id = id;
            p.nome = nome;
            p.origem = origem;
            p.caminho = caminho;
            p.reconhecidaPeloNome = reconhecidaPeloNome;
            p.videos = videos;
            p.series = series;
            p.episodios = episodios;
            p.tipo = tipo;
            return p;
        }
    }

    /** Resultado de "Buscar no TMDB". */
    public static final class ResultadoTmdb {
        public final String id;
        public final String titulo;
        public final String ano;
        public final Tipo tipo;
        public final String sinopse;
        public final int corCapa;
        public final int corFundo;
        /** Resultado ligado ao título hoje ("Atual"). */
        public boolean atual;

        public ResultadoTmdb(String id, String titulo, String ano, Tipo tipo, String sinopse, int corCapa, int corFundo) {
            this.id = id;
            this.titulo = titulo;
            this.ano = ano;
            this.tipo = tipo;
            this.sinopse = sinopse;
            this.corCapa = corCapa;
            this.corFundo = corFundo;
        }
    }

    /** Faixa de áudio ou de legenda do arquivo. */
    public static final class Faixa {
        public final String id;
        public final TipoFaixa tipo;
        public final String nome;
        public boolean atual;

        public Faixa(String id, TipoFaixa tipo, String nome, boolean atual) {
            this.id = id;
            this.tipo = tipo;
            this.nome = nome;
            this.atual = atual;
        }
    }

    /** Pessoa do elenco (aba Elenco do tablet). */
    public static final class PessoaElenco {
        public final String nome;
        public final String personagem;

        public PessoaElenco(String nome, String personagem) {
            this.nome = nome;
            this.personagem = personagem;
        }
    }

    /** Dados técnicos do arquivo (Detalhe). */
    public static final class DadosTecnicos {
        public final String video;
        public final String audio;
        public final String legendas;
        public final String arquivo;
        public final String capitulos;
        public final String origem;
        /** Chips do Detalhe da TV ("4K UHD", "HDR10", "Dolby Digital 5.1", "Legendas PT-BR"). */
        public final List<String> chips;
        /** Chips do Detalhe do tablet ("4K UHD", "HDR10", "5.1"). */
        public final List<String> chipsCurtos;
        /** Quantos chips do começo das duas listas são do vídeo (azuis no desenho: "4K UHD", "HDR10"). */
        public final int chipsVideo;

        public DadosTecnicos(String video, String audio, String legendas, String arquivo, String capitulos,
                             String origem, List<String> chips, List<String> chipsCurtos, int chipsVideo) {
            this.video = video;
            this.audio = audio;
            this.legendas = legendas;
            this.arquivo = arquivo;
            this.capitulos = capitulos;
            this.origem = origem;
            this.chips = Collections.unmodifiableList(new ArrayList<>(chips));
            this.chipsCurtos = Collections.unmodifiableList(new ArrayList<>(chipsCurtos));
            this.chipsVideo = chipsVideo;
        }
    }
}
