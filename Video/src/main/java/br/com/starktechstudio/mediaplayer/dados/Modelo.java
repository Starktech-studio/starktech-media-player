// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.dados;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Modelo de dados das telas novas. Java puro, sem nada do banco do Nova.
 * Na F3 os dados vêm do {@link RepositorioReal} (pastas, motor e TMDB).
 * Cores (capa, fundo, miniatura) são ARGB: o bloco de cor fica só quando falta a imagem
 * (capa e fundo do TMDB, imagem do aparelho ou quadro do vídeo).
 */
public final class Modelo {

    /** Prefixo da imagem escolhida no Editar (igual ao da {@code motor.Edicoes}). */
    static final String ARQUIVO = "arquivo:";
    /** Prefixos do id da capa de coletânea e de artista (iguais aos do {@code motor.Acervo}). */
    private static final String COLETANEA = "coletanea:";
    private static final String ARTISTA = "artista:";

    /** Maior nota; a menor é 0 (D6). */
    public static final int NOTA_MAXIMA = 5;

    private Modelo() {}

    /** Nota inteira de 0 a 5 (D6); sem nota é nulo, à parte. */
    public static boolean notaValida(int nota) {
        return nota >= 0 && nota <= NOTA_MAXIMA;
    }

    /** Tipo do conteúdo: vem do tipo da pasta (INTERFACE.md §4). */
    public enum Tipo { FILME, SERIE, DESENHO, SHOW, STANDUP }

    /** Tipo escolhido para uma pasta; NENHUM = "escolha o tipo". */
    public enum TipoPasta { FILMES, SERIES, TVSHOWS, DESENHOS, SHOWS, STANDUP, MUSICAS, IGNORAR, NENHUM }

    /** Origem de uma pasta. */
    public enum Origem { HD_EXTERNO, INTERNO }

    /** Faixa de áudio ou de legenda. */
    public enum TipoFaixa { AUDIO, LEGENDA }

    /** Campos da tela Editar; cada um pode estar "editado à mão". */
    public enum Campo { TITULO, TITULO_ORIGINAL, TIPO, ANO, DURACAO, GENEROS, SINOPSE }

    /** Seção da aba Shows com "por grupo" ligado (D7): a subpasta de Shows e os artistas dela. */
    public static final class SecaoShows {
        /** Nome da subpasta; sem grupo, o texto "Sem grupo". */
        public final String nome;
        /** Shows fora das subpastas (vem por último). */
        public final boolean semGrupo;
        /** Capas de artista ({@link Titulo#ehArtista()}), com {@link Titulo#numeroShows} desta seção. */
        public final List<Titulo> artistas;

        public SecaoShows(String nome, boolean semGrupo, List<Titulo> artistas) {
            this.nome = nome;
            this.semGrupo = semGrupo;
            this.artistas = Collections.unmodifiableList(new ArrayList<>(artistas));
        }
    }

    /** Títulos do {@link Tipo#SERIE} de uma aba: TV Shows ou Séries, pelo tipo da pasta. */
    public static List<Titulo> daAbaDeSeries(List<Titulo> titulos, boolean tvShows) {
        List<Titulo> lista = new ArrayList<>();
        for (Titulo t : titulos) if (t.tvShow == tvShows) lista.add(t);
        return lista;
    }

    /** Filtro "Só não assistidos" das listas. */
    public static List<Titulo> naoAssistidos(List<Titulo> titulos) {
        List<Titulo> lista = new ArrayList<>();
        for (Titulo t : titulos) if (!t.tudoAssistido()) lista.add(t);
        return lista;
    }

    /** O mesmo filtro nas seções da aba Shows; seção sem nada a assistir sai. */
    public static List<SecaoShows> secoesNaoAssistidas(List<SecaoShows> secoes) {
        List<SecaoShows> lista = new ArrayList<>();
        for (SecaoShows s : secoes) {
            List<Titulo> artistas = naoAssistidos(s.artistas);
            if (!artistas.isEmpty()) lista.add(new SecaoShows(s.nome, s.semGrupo, artistas));
        }
        return lista;
    }

    /** Filme, série ou show da biblioteca, já com o estado (favorito, assistido, posição) aplicado. */
    /** Prévia do resultado escolhido em "Buscar no TMDB": como o título fica, e as opções de capa e fundo. */
    public static final class PreviaTmdb {
        public final Titulo titulo;
        public final List<String> capas;
        public final List<String> fundos;

        public PreviaTmdb(Titulo titulo, List<String> capas, List<String> fundos) {
            this.titulo = titulo;
            this.capas = capas;
            this.fundos = fundos;
        }
    }

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
        /** Capa / fundo escolhidos no Editar (referência da {@code motor.Edicoes}); nulo = o do TMDB. */
        public String capaImagem;
        public String fundoImagem;
        /** Capa e fundo baixados do TMDB (caminho do arquivo); nulo = sem imagem. */
        public String capaTmdb;
        public String fundoTmdb;
        /** Quadro do vídeo (caminho), para o cartão 16:9 sem fundo do TMDB. */
        public String quadro;
        /** Cor da miniatura 16:9 (Continuar assistindo, Favoritos). */
        public int corCartao;
        /** Arquivo como aparece na tela ("HD / Filmes / Nome.mkv"); série: a pasta. */
        public String arquivo;
        /** Caminho de verdade do arquivo (player); nulo na série. */
        public String caminho;
        public boolean naoIdentificado;
        /** Quando entrou na biblioteca (ms). */
        public long adicionado;
        /** Data do arquivo (ms): desempata quem entrou na mesma varredura. */
        public long modificado;
        /** Nome em comum (D13): vídeos com a mesma chave herdam a edição à mão; nulo = não herda. */
        public String chaveNome;

        // Estado
        public boolean favorito;
        public boolean assistido;
        public int posicaoSegundos;
        /** Nota de 0 a 5, só em filme assistido (D6); nula = sem nota, diferente de 0. */
        public Integer nota;
        /** Resultado do TMDB escolhido em "Buscar no TMDB" (vazio = o original). */
        public String idResultadoTmdb;
        public Set<Campo> editadosAMao = EnumSet.noneOf(Campo.class);

        // Série
        public int numeroTemporadas;
        public int numeroEpisodios;
        /** Série de pasta TV Shows: igual a uma série em tudo, só que na aba TV Shows. */
        public boolean tvShow;

        // Coletânea (D5)
        /** Filme: a coletânea em que está (do TMDB ou ajustada à mão); nulo = fora de coletânea. */
        public String colecaoNome;
        /** Filme: id da coletânea (mesmo sozinho nela, sem capa própria); nulo = fora de coletânea. */
        public String idColetanea;
        /** Capa e fundo da coleção vindos do TMDB (caminho do arquivo). */
        public String colecaoCapa;
        public String colecaoFundo;
        /** Capa de coletânea: quantos filmes tem. */
        public int numeroFilmes;

        // Show
        /** Show: o artista; nulo = "Sem artista". Capa de artista: o nome está em {@link #titulo}. */
        public String artista;
        /** Show: a subpasta de Shows (D7); nulo = sem grupo. */
        public String grupo;
        /** Capa de artista: quantos shows tem. */
        public int numeroShows;
        /** Capa de coletânea ou de artista: todos os filmes ou shows dela já assistidos. */
        public boolean todosAssistidos;

        /** Coletânea e artista: todos os títulos dela; o resto: ele mesmo (série: todos os episódios). */
        public boolean tudoAssistido() {
            return ehColetanea() || ehArtista() ? todosAssistidos : assistido;
        }

        /** Capa de coletânea na aba Filmes: abre a lista de filmes, não o Detalhe. */
        public boolean ehColetanea() {
            return id != null && id.startsWith(COLETANEA);
        }

        /** Capa de artista na aba Shows: abre os shows dele, não o Detalhe. */
        public boolean ehArtista() {
            return id != null && id.startsWith(ARTISTA);
        }

        /** Selo "4K" da capa. */
        public boolean eh4k() {
            return qualidade != null && qualidade.startsWith("4K");
        }

        /** Selo "1080p" da capa (1.1.1: a capa só mostra o tamanho, 4K ou 1080p; HDR saiu). */
        public boolean eh1080p() {
            return qualidade != null && qualidade.startsWith("1080p");
        }

        /** Nota só em filme (D6) e só com assistido. */
        public boolean podeDarNota() {
            return tipo == Tipo.FILME && assistido && !ehColetanea();
        }

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
            t.capaTmdb = capaTmdb;
            t.fundoTmdb = fundoTmdb;
            t.quadro = quadro;
            t.corCartao = corCartao;
            t.arquivo = arquivo;
            t.caminho = caminho;
            t.naoIdentificado = naoIdentificado;
            t.adicionado = adicionado;
            t.modificado = modificado;
            t.chaveNome = chaveNome;
            t.favorito = favorito;
            t.assistido = assistido;
            t.posicaoSegundos = posicaoSegundos;
            t.nota = nota;
            t.idResultadoTmdb = idResultadoTmdb;
            t.editadosAMao = editadosAMao.isEmpty() ? EnumSet.noneOf(Campo.class) : EnumSet.copyOf(editadosAMao);
            t.numeroTemporadas = numeroTemporadas;
            t.numeroEpisodios = numeroEpisodios;
            t.tvShow = tvShow;
            t.colecaoNome = colecaoNome;
            t.idColetanea = idColetanea;
            t.colecaoCapa = colecaoCapa;
            t.colecaoFundo = colecaoFundo;
            t.numeroFilmes = numeroFilmes;
            t.artista = artista;
            t.grupo = grupo;
            t.numeroShows = numeroShows;
            t.todosAssistidos = todosAssistidos;
            return t;
        }

        /**
         * Imagem da capa: a escolhida no Editar ({@code arquivo:}; {@code cor:} = sem imagem), senão
         * a do TMDB. Caminho do arquivo; nulo = bloco de cor ({@link #corCapa}).
         */
        public String imagemCapa() {
            return imagem(capaImagem, capaTmdb);
        }

        /** Imagem do fundo (como a da capa). */
        public String imagemFundo() {
            return imagem(fundoImagem, fundoTmdb);
        }

        /** Imagem do cartão 16:9: o fundo; sem ele, o quadro do vídeo; sem nenhum, a capa. Fundo em cor escolhido no Editar: sem imagem. */
        public String imagemCartao() {
            String fundo = imagemFundo();
            if (fundo != null) return fundo;
            if (fundoImagem != null) return null;
            return quadro != null ? quadro : imagemCapa();
        }

        /** Referência da capa atual para o Editar ({@code arquivo:} ou {@code cor:}). */
        public String referenciaCapa() {
            return referencia(capaImagem, capaTmdb, corCapa);
        }

        public String referenciaFundo() {
            return referencia(fundoImagem, fundoTmdb, corFundo);
        }

        private static String imagem(String editada, String doTmdb) {
            if (editada != null) return editada.startsWith(ARQUIVO) ? editada.substring(ARQUIVO.length()) : null;
            return doTmdb;
        }

        private static String referencia(String editada, String doTmdb, int cor) {
            if (editada != null) return editada;
            if (doTmdb != null) return ARQUIVO + doTmdb;
            return String.format(Locale.ROOT, "cor:#%06X", cor & 0xFFFFFF);
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
        /** Nome do arquivo ("01.mkv"). */
        public String arquivo;
        /** Caminho de verdade (player). */
        public String caminho;
        /** Série do episódio: id e nome. */
        public String idSerie;
        public String nomeSerie;
        public int duracaoMin;
        public int duracaoSegundos;
        public int corMiniatura;
        /** Imagem do episódio (TMDB) ou quadro do vídeo; nulo = bloco de cor. */
        public String miniatura;

        // Estado
        public boolean assistido;
        public int posicaoSegundos;

        public Episodio copia() {
            Episodio e = new Episodio();
            e.id = id;
            e.temporada = temporada;
            e.numero = numero;
            e.nome = nome;
            e.arquivo = arquivo;
            e.caminho = caminho;
            e.idSerie = idSerie;
            e.nomeSerie = nomeSerie;
            e.duracaoMin = duracaoMin;
            e.duracaoSegundos = duracaoSegundos;
            e.corMiniatura = corMiniatura;
            e.miniatura = miniatura;
            e.assistido = assistido;
            e.posicaoSegundos = posicaoSegundos;
            return e;
        }

        public boolean emAndamento() {
            return !assistido && posicaoSegundos > 0 && posicaoSegundos < duracaoSegundos;
        }
    }

    /** Pasta da biblioteca, com o tipo escolhido. {@link #id}: o caminho de verdade. */
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
        /** Endereço da capa pequena do TMDB; nulo = bloco de cor. */
        public final String capaEndereco;
        /** Resultado ligado ao título hoje ("Atual"). */
        public boolean atual;

        public ResultadoTmdb(String id, String titulo, String ano, Tipo tipo, String sinopse, int corCapa, String capaEndereco) {
            this.id = id;
            this.titulo = titulo;
            this.ano = ano;
            this.tipo = tipo;
            this.sinopse = sinopse;
            this.corCapa = corCapa;
            this.capaEndereco = capaEndereco;
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
        /** Mais faixas do que o resumo mostra (áudio além do primeiro, legendas além de 3): "Ver todos". */
        public boolean faixasDemais;

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
