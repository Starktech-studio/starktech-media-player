// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.dados;

import android.content.Context;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import br.com.starktechstudio.mediaplayer.dados.Modelo.Campo;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Capitulo;
import br.com.starktechstudio.mediaplayer.dados.Modelo.DadosTecnicos;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Episodio;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Faixa;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Origem;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Pasta;
import br.com.starktechstudio.mediaplayer.dados.Modelo.PessoaElenco;
import br.com.starktechstudio.mediaplayer.dados.Modelo.ResultadoTmdb;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Temporada;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Tipo;
import br.com.starktechstudio.mediaplayer.dados.Modelo.TipoFaixa;
import br.com.starktechstudio.mediaplayer.dados.Modelo.TipoPasta;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Titulo;

/**
 * Dados simulados da F1: os exemplos das 13 telas de docs/paginas/telas-f1/ (filmes,
 * capítulos, cores e números do desenho). Os textos entre colchetes do desenho
 * ("[Nome da série]", "[N] vídeos"…) viraram exemplos fictícios (D8).
 * Estado das ações em {@link EstadoSimulado}. Não usa ContentResolver, VideoStore,
 * ScraperStore nem loaders do Nova. Sai na F3 (e não pode ir para distribuição).
 */
public final class RepositorioSimulado implements RepositorioBiblioteca {

    // Ids dos exemplos
    public static final String TEARS_OF_STEEL = "tears_of_steel";
    public static final String SINTEL = "sintel";
    public static final String BIG_BUCK_BUNNY = "big_buck_bunny";
    public static final String COSMOS_LAUNDROMAT = "cosmos_laundromat";
    public static final String ELEPHANTS_DREAM = "elephants_dream";
    public static final String SPRING = "spring";
    public static final String SPRITE_FRIGHT = "sprite_fright";
    public static final String CHARGE = "charge";
    public static final String HORIZONTE_AZUL = "serie_horizonte_azul";
    public static final String MARE_ALTA = "serie_mare_alta";
    public static final String CODIGO_AURORA = "serie_codigo_aurora";

    private static final String SINOPSE_BLENDER = "Curta-metragem de animação aberto, feito pela Blender.";
    private static final String SINOPSE_SHOW = "Show gravado ao vivo.";
    private static final String SINOPSE_SERIE = "Série de exemplo, organizada a partir da pasta.";

    /** Miniaturas dos capítulos e episódios, na ordem do desenho. */
    private static final int[] CORES_MINIATURA = {
            cor("#172554"), cor("#083344"), cor("#1e1b4b"), cor("#1e293b")
    };

    /** Os 8 capítulos do desenho (Tears of Steel, 12:14). */
    private static final String[] NOMES_CAPITULOS = {
            "Abertura", "A ponte", "O laboratório", "A memória", "O ataque", "A torre", "O reencontro", "Créditos"
    };
    private static final int[] INICIOS_CAPITULOS = {0, 80, 185, 290, 390, 490, 585, 680};

    private static RepositorioSimulado instancia;

    private final EstadoSimulado estado;
    private final Map<String, Titulo> titulos = new LinkedHashMap<>();
    private final Map<String, List<Temporada>> temporadas = new LinkedHashMap<>();
    private final Map<String, Episodio> episodios = new LinkedHashMap<>();
    private final Map<String, List<Capitulo>> capitulos = new LinkedHashMap<>();
    private final List<Pasta> pastas = new ArrayList<>();
    private final List<String> naoIdentificados = new ArrayList<>();

    public static synchronized RepositorioSimulado instancia(Context contexto) {
        if (instancia == null) instancia = new RepositorioSimulado(new EstadoSimulado(contexto));
        return instancia;
    }

    private RepositorioSimulado(EstadoSimulado estado) {
        this.estado = estado;
        montarFilmes();
        montarSeries();
        montarShows();
        montarPastas();
        montarNaoIdentificados();
    }

    // =====================================================================
    // Montagem dos exemplos
    // =====================================================================

    private void montarFilmes() {
        Titulo t = filme(TEARS_OF_STEEL, "Tears of Steel", "2012", 734, "Ficção científica",
                "Ficção científica, Ação", "4K HDR",
                "Em Amsterdã, um grupo de cientistas tenta salvar a cidade de um ataque de robôs revivendo uma lembrança do passado.",
                "#172554", "#172554", "tears_of_steel_4k.mkv");
        t.posicaoSegundos = 252; // 04:12 · 35% · Cap. 3 de 8 · Faltam 8 min
        capitulos.put(t.id, capitulosDoDesenho());

        t = filme(SINTEL, "Sintel", "2010", 900, "Animação", "Animação, Fantasia", "1080p",
                "Uma jovem atravessa terras perigosas em busca do filhote de dragão que ela salvou.",
                "#134e4a", "#134e4a", "sintel_1080p.mkv");
        t.posicaoSegundos = 558; // 62% · Cap. 4 de 8 · Faltam 6 min
        capitulos.put(t.id, capitulosGenericos(new int[]{0, 100, 230, 480, 600, 700, 790, 850}));

        t = filme(BIG_BUCK_BUNNY, "Big Buck Bunny", "2008", 600, "Animação", "Animação, Comédia", "1080p",
                SINOPSE_BLENDER, "#022c22", "#022c22", "big_buck_bunny_1080p.mp4");
        t.posicaoSegundos = 120; // 20% · Faltam 8 min, sem capítulos
        t.assistido = true;

        t = filme(COSMOS_LAUNDROMAT, "Cosmos Laundromat", "2015", 720, "Animação", "Animação, Fantasia", "1080p",
                "Numa ilha deserta, uma ovelha cansada da vida encontra um vendedor misterioso que lhe oferece outras vidas.",
                "#2e1065", "#2e1065", "cosmos_laundromat_1080p.mkv");
        t.posicaoSegundos = 576; // 80% · Faltam 2 min, sem capítulos

        t = filme(ELEPHANTS_DREAM, "Elephants Dream", "2006", 660, "Animação", "Animação, Ficção científica", "1080p",
                SINOPSE_BLENDER, "#451a03", "#451a03", "elephants_dream_1080p.mkv");
        t.favorito = true;
        t.assistido = true;
        capitulos.put(t.id, capitulosProporcionais(t.duracaoSegundos));

        t = filme(SPRING, "Spring", "2019", 480, "Animação", "Animação, Fantasia", "1080p",
                "Uma jovem pastora e seu cão enfrentam espíritos antigos para que o ciclo da primavera continue.",
                "#083344", "#083344", "spring_1080p.mkv");
        t.favorito = true;
        capitulos.put(t.id, capitulosProporcionais(t.duracaoSegundos));

        t = filme(SPRITE_FRIGHT, "Sprite Fright", "2021", 600, "Animação", "Animação, Comédia", "1080p",
                SINOPSE_BLENDER, "#4c0519", "#4c0519", "sprite_fright_1080p.mkv");
        t.favorito = true;
        capitulos.put(t.id, capitulosProporcionais(t.duracaoSegundos));

        t = filme(CHARGE, "Charge", "2022", 240, "Animação", "Animação, Ação", "1080p",
                SINOPSE_BLENDER, "#1e1b4b", "#1e1b4b", "charge_1080p.mkv");
        t.favorito = true;
        capitulos.put(t.id, capitulosProporcionais(t.duracaoSegundos));
    }

    private void montarSeries() {
        // Série principal do desenho (tv-serie): 3 temporadas, episódios 1 e 2 assistidos, 3 em andamento
        String[][] nomes = {
                {"O começo", "A travessia", "Sinal perdido", "A tempestade", "Porto seguro", "Mar aberto", "O farol", "Volta para casa"},
                {"Novos ventos", "A ilha", "Correnteza", "Noite sem lua", "O mapa", "Contra a maré", "A promessa", "Horizonte"},
                {"Recomeço", "Ventos do sul", "A carta", "Neblina", "O resgate", "Âncora", "Última onda", "Céu aberto"}
        };
        Titulo serie = serie(HORIZONTE_AZUL, "Horizonte Azul", "2023", "Drama", "Drama, Aventura", "#1e1b4b", nomes, 42);
        Episodio e = episodios.get(idEpisodio(serie.id, 1, 1));
        e.assistido = true;
        e = episodios.get(idEpisodio(serie.id, 1, 2));
        e.assistido = true;
        e = episodios.get(idEpisodio(serie.id, 1, 3));
        e.posicaoSegundos = 1100; // Em andamento · "Continuar · Episódio 3"

        serie(MARE_ALTA, "Maré Alta", "2021", "Suspense", "Suspense, Drama", "#0c4a6e", nomesGenericos(2, 6), 38);
        serie(CODIGO_AURORA, "Código Aurora", "2024", "Ficção científica", "Ficção científica", "#3b0764", nomesGenericos(1, 8), 45);
    }

    private void montarShows() {
        show("show_orquestra_verao", "Orquestra Sinfônica - Concerto de Verão", "2019", 90, "#422006", "Orquestra Sinfônica - Concerto de Verão (2019).mkv");
        show("show_banda_aurora", "Banda Aurora - Ao Vivo no Parque", "2021", 75, "#14532d", "Banda Aurora - Ao Vivo no Parque (2021).mkv");
        show("show_trio_alvorada", "Trio Alvorada - Acústico", "2018", 60, "#7c2d12", "Trio Alvorada - Acústico (2018).mkv");
        show("show_coral_natal", "Coral da Cidade - Natal", "2022", 55, "#1e3a8a", "Coral da Cidade - Natal (2022).mkv");
    }

    private void montarPastas() {
        pastas.add(pasta("hd_filmes", "Filmes", Origem.HD_EXTERNO, "HD / Filmes", true, 6, 0, 0, TipoPasta.FILMES));
        pastas.add(pasta("hd_series", "Séries", Origem.HD_EXTERNO, "HD / Séries", true, 0, 3, 44, TipoPasta.SERIES));
        pastas.add(pasta("hd_shows", "Shows", Origem.HD_EXTERNO, "HD / Shows", true, 4, 0, 0, TipoPasta.SHOWS));
        pastas.add(pasta("hd_downloads", "Downloads", Origem.HD_EXTERNO, "HD / Downloads", false, 5, 0, 0, TipoPasta.NENHUM));
        pastas.add(pasta("interno_movies", "Movies", Origem.INTERNO, "Interno / Movies", false, 2, 0, 0, TipoPasta.FILMES));
    }

    private void montarNaoIdentificados() {
        naoIdentificado("ni_1", "VID_20240312_201544.mp4", 185);
        naoIdentificado("ni_2", "aniversario_vovo.mkv", 1260);
        naoIdentificado("ni_3", "palestra_parte1.mp4", 2700);
        naoIdentificado("ni_4", "clipe_final.webm", 210);
        naoIdentificado("ni_5", "S01 ep 2 sem nome.mkv", 2400);
    }

    private Titulo filme(String id, String nome, String ano, int segundos, String genero, String generos,
                         String qualidade, String sinopse, String capa, String cartao, String arquivo) {
        Titulo t = new Titulo();
        t.id = id;
        t.tipo = Tipo.FILME;
        t.titulo = nome;
        t.tituloOriginal = nome;
        t.ano = ano;
        t.duracaoSegundos = segundos;
        t.duracaoMin = Math.round(segundos / 60f);
        t.genero = genero;
        t.generos = generos;
        t.qualidade = qualidade;
        t.sinopse = sinopse;
        t.corCapa = cor(capa);
        t.corFundo = cor(capa);
        t.corCartao = cor(cartao);
        t.arquivo = arquivo;
        titulos.put(id, t);
        return t;
    }

    private Titulo serie(String id, String nome, String ano, String genero, String generos, String capa,
                         String[][] nomesEpisodios, int minutosEpisodio) {
        Titulo t = new Titulo();
        t.id = id;
        t.tipo = Tipo.SERIE;
        t.titulo = nome;
        t.tituloOriginal = nome;
        t.ano = ano;
        t.duracaoMin = minutosEpisodio;
        t.duracaoSegundos = minutosEpisodio * 60;
        t.genero = genero;
        t.generos = generos;
        t.qualidade = "1080p";
        t.sinopse = SINOPSE_SERIE;
        t.corCapa = cor(capa);
        t.corFundo = cor(capa);
        t.corCartao = cor(capa);
        t.arquivo = "Séries / " + nome;
        t.numeroTemporadas = nomesEpisodios.length;

        List<Temporada> lista = new ArrayList<>();
        int total = 0;
        for (int s = 0; s < nomesEpisodios.length; s++) {
            List<Episodio> eps = new ArrayList<>();
            for (int n = 0; n < nomesEpisodios[s].length; n++) {
                Episodio e = new Episodio();
                e.temporada = s + 1;
                e.numero = n + 1;
                e.id = idEpisodio(id, e.temporada, e.numero);
                e.nome = nomesEpisodios[s][n];
                e.arquivo = String.format(Locale.ROOT, "%02d.mkv", e.numero);
                e.duracaoMin = minutosEpisodio;
                e.duracaoSegundos = minutosEpisodio * 60;
                e.corMiniatura = CORES_MINIATURA[n % CORES_MINIATURA.length];
                episodios.put(e.id, e);
                eps.add(e);
                total++;
            }
            lista.add(new Temporada(s + 1, eps));
        }
        t.numeroEpisodios = total;
        temporadas.put(id, lista);
        titulos.put(id, t);
        return t;
    }

    private void show(String id, String nome, String ano, int minutos, String capa, String arquivo) {
        Titulo t = new Titulo();
        t.id = id;
        t.tipo = Tipo.SHOW;
        t.titulo = nome;
        t.tituloOriginal = nome;
        t.ano = ano;
        t.duracaoMin = minutos;
        t.duracaoSegundos = minutos * 60;
        t.genero = "Show";
        t.generos = "Música";
        t.qualidade = "1080p";
        t.sinopse = SINOPSE_SHOW;
        t.corCapa = cor(capa);
        t.corFundo = cor(capa);
        t.corCartao = cor(capa);
        t.arquivo = arquivo;
        titulos.put(id, t);
    }

    private static Pasta pasta(String id, String nome, Origem origem, String caminho, boolean reconhecida,
                               int videos, int series, int episodios, TipoPasta tipo) {
        Pasta p = new Pasta();
        p.id = id;
        p.nome = nome;
        p.origem = origem;
        p.caminho = caminho;
        p.reconhecidaPeloNome = reconhecida;
        p.videos = videos;
        p.series = series;
        p.episodios = episodios;
        p.tipo = tipo;
        return p;
    }

    private void naoIdentificado(String id, String arquivo, int segundos) {
        Titulo t = new Titulo();
        t.id = id;
        t.tipo = null;
        String nome = arquivo.replaceAll("\\.[^.]+$", "");
        t.titulo = nome;
        t.tituloOriginal = nome;
        t.ano = "";
        t.duracaoSegundos = segundos;
        t.duracaoMin = Math.max(1, Math.round(segundos / 60f));
        t.genero = "";
        t.generos = "";
        t.qualidade = "";
        t.sinopse = "";
        t.corCapa = cor("#1e293b");
        t.corFundo = cor("#1e293b");
        t.corCartao = cor("#1e293b");
        t.arquivo = "HD / Downloads / " + arquivo;
        t.naoIdentificado = true;
        titulos.put(id, t);
        naoIdentificados.add(id);
    }

    private static List<Capitulo> capitulosDoDesenho() {
        List<Capitulo> lista = new ArrayList<>();
        for (int i = 0; i < NOMES_CAPITULOS.length; i++) {
            lista.add(new Capitulo(i + 1, NOMES_CAPITULOS[i], INICIOS_CAPITULOS[i], CORES_MINIATURA[i % CORES_MINIATURA.length]));
        }
        return Collections.unmodifiableList(lista);
    }

    /** 8 capítulos com o início dado: "Abertura", "Capítulo 2"…, "Créditos". */
    private static List<Capitulo> capitulosGenericos(int[] inicios) {
        List<Capitulo> lista = new ArrayList<>();
        for (int i = 0; i < inicios.length; i++) {
            String nome = i == 0 ? "Abertura" : i == inicios.length - 1 ? "Créditos" : "Capítulo " + (i + 1);
            lista.add(new Capitulo(i + 1, nome, inicios[i], CORES_MINIATURA[i % CORES_MINIATURA.length]));
        }
        return Collections.unmodifiableList(lista);
    }

    private static List<Capitulo> capitulosProporcionais(int duracaoSegundos) {
        int[] inicios = new int[8];
        for (int i = 0; i < inicios.length; i++) inicios[i] = i * duracaoSegundos / inicios.length;
        return capitulosGenericos(inicios);
    }

    private static String[][] nomesGenericos(int temporadas, int porTemporada) {
        String[][] nomes = new String[temporadas][porTemporada];
        for (int s = 0; s < temporadas; s++) {
            for (int n = 0; n < porTemporada; n++) nomes[s][n] = "Episódio " + (n + 1);
        }
        return nomes;
    }

    private static String idEpisodio(String idSerie, int temporada, int numero) {
        return idSerie + "_t" + temporada + "_e" + numero;
    }

    /** "#rrggbb" para ARGB opaco, sem android.graphics.Color (Java puro). */
    static int cor(String hex) {
        return (int) (0xFF000000L | Long.parseLong(hex.substring(1), 16));
    }

    // =====================================================================
    // Estado
    // =====================================================================

    private Titulo comEstado(Titulo base) {
        Titulo t = base.copia();
        t.favorito = estado.favorito(t.id, base.favorito);
        t.assistido = estado.assistido(t.id, base.assistido);
        t.posicaoSegundos = estado.posicao(t.id, base.posicaoSegundos);

        String idTmdb = estado.resultadoTmdb(t.id);
        if (idTmdb != null) {
            for (ResultadoTmdb r : resultadosTmdb(base)) {
                if (r.id.equals(idTmdb)) {
                    t.idResultadoTmdb = r.id;
                    t.titulo = r.titulo;
                    t.sinopse = r.sinopse;
                    t.corCapa = r.corCapa;
                    t.corFundo = r.corFundo;
                    break;
                }
            }
        }
        String capa = estado.capa(t.id);
        if (capa != null) t.corCapa = Integer.parseInt(capa);
        String fundo = estado.fundo(t.id);
        if (fundo != null) t.corFundo = Integer.parseInt(fundo);

        for (Campo campo : Campo.values()) {
            String valor = estado.campoEditado(t.id, campo);
            if (valor != null) {
                t.aplicar(campo, valor);
                t.editadosAMao.add(campo);
            }
        }
        return t;
    }

    private Episodio comEstado(Episodio base) {
        Episodio e = base.copia();
        e.assistido = estado.assistido(e.id, base.assistido);
        e.posicaoSegundos = estado.posicao(e.id, base.posicaoSegundos);
        return e;
    }

    private List<Titulo> lista(String... ids) {
        List<Titulo> lista = new ArrayList<>();
        for (String id : ids) {
            Titulo t = titulos.get(id);
            if (t != null) lista.add(comEstado(t));
        }
        return lista;
    }

    // =====================================================================
    // RepositorioBiblioteca
    // =====================================================================

    @Override
    public List<Titulo> destaques() {
        return lista(TEARS_OF_STEEL, SPRING, COSMOS_LAUNDROMAT, SINTEL);
    }

    @Override
    public List<Titulo> continuarAssistindo() {
        List<Titulo> lista = new ArrayList<>();
        for (Titulo t : lista(SINTEL, TEARS_OF_STEEL, COSMOS_LAUNDROMAT, BIG_BUCK_BUNNY,
                ELEPHANTS_DREAM, SPRING, SPRITE_FRIGHT, CHARGE)) {
            if (t.emAndamento()) lista.add(t);
        }
        return lista;
    }

    @Override
    public List<Titulo> favoritos() {
        List<Titulo> lista = new ArrayList<>();
        for (Titulo t : lista(SPRING, SPRITE_FRIGHT, ELEPHANTS_DREAM, CHARGE)) {
            if (t.favorito) lista.add(t);
        }
        // Favoritados depois (fora da ordem do desenho) entram no fim
        for (Titulo base : titulos.values()) {
            if (base.naoIdentificado) continue;
            Titulo t = comEstado(base);
            if (t.favorito && !contem(lista, t.id)) lista.add(t);
        }
        return lista;
    }

    @Override
    public List<Titulo> filmesAdicionadosRecentemente() {
        return lista(TEARS_OF_STEEL, SINTEL, BIG_BUCK_BUNNY, COSMOS_LAUNDROMAT,
                ELEPHANTS_DREAM, SPRING, SPRITE_FRIGHT, CHARGE);
    }

    @Override
    public List<Titulo> adicionadosRecentemente() {
        return lista(TEARS_OF_STEEL, SINTEL, BIG_BUCK_BUNNY, COSMOS_LAUNDROMAT, ELEPHANTS_DREAM,
                HORIZONTE_AZUL, SPRING, "show_banda_aurora", SPRITE_FRIGHT, MARE_ALTA,
                CHARGE, "show_coral_natal", CODIGO_AURORA, "show_orquestra_verao", "show_trio_alvorada");
    }

    @Override
    public List<Titulo> titulos(Tipo tipo) {
        List<Titulo> lista = new ArrayList<>();
        for (Titulo base : titulos.values()) {
            if (base.naoIdentificado) continue;
            Titulo t = comEstado(base);
            if (t.tipo == tipo) lista.add(t);
        }
        return lista;
    }

    @Override
    public List<Titulo> buscar(String consulta) {
        List<Titulo> lista = new ArrayList<>();
        String alvo = normalizar(consulta);
        if (alvo.isEmpty()) return lista;
        for (Titulo base : titulos.values()) {
            if (base.naoIdentificado) continue;
            Titulo t = comEstado(base);
            if (normalizar(t.titulo).contains(alvo) || normalizar(t.tituloOriginal).contains(alvo)) lista.add(t);
        }
        return lista;
    }

    @Override
    public Titulo titulo(String id) {
        Titulo t = id == null ? null : titulos.get(id);
        return t == null ? null : comEstado(t);
    }

    @Override
    public List<Capitulo> capitulos(String idTitulo) {
        List<Capitulo> lista = capitulos.get(idTitulo);
        return lista == null ? Collections.<Capitulo>emptyList() : lista;
    }

    @Override
    public List<Temporada> temporadas(String idSerie) {
        List<Temporada> base = temporadas.get(idSerie);
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
        Episodio e = idEpisodio == null ? null : episodios.get(idEpisodio);
        return e == null ? null : comEstado(e);
    }

    @Override
    public DadosTecnicos dadosTecnicos(String idTitulo) {
        Titulo t = titulos.get(idTitulo);
        if (t == null) return null;
        if (TEARS_OF_STEEL.equals(idTitulo)) {
            return new DadosTecnicos("3840×2160 · H.265 · HDR10", "Inglês · Dolby Digital 5.1",
                    "Português (BR) · Inglês", t.arquivo, "8, com nome", "HD externo (USB)",
                    Arrays.asList("4K UHD", "HDR10", "Dolby Digital 5.1", "Legendas PT-BR"),
                    Arrays.asList("4K UHD", "HDR10", "5.1"), 2);
        }
        List<Capitulo> caps = capitulos(idTitulo);
        String textoCapitulos = caps.isEmpty() ? "Sem capítulos" : caps.size() + ", com nome";
        String origem = SPRING.equals(idTitulo) || CHARGE.equals(idTitulo) ? "Armazenamento interno" : "HD externo (USB)";
        return new DadosTecnicos("1920×1080 · H.264", "Inglês · Estéreo", "Português (BR)", t.arquivo,
                textoCapitulos, origem,
                Arrays.asList("1080p", "Estéreo", "Legendas PT-BR"),
                Arrays.asList("1080p", "Estéreo"), 1);
    }

    @Override
    public List<PessoaElenco> elenco(String idTitulo) {
        Titulo t = titulos.get(idTitulo);
        if (t == null || t.naoIdentificado) return Collections.emptyList();
        if (t.tipo == Tipo.SHOW) {
            return Collections.singletonList(new PessoaElenco(t.titulo.replaceAll(" - .*$", ""), "Artista"));
        }
        return Arrays.asList(
                new PessoaElenco("Marina Alves", "Cientista"),
                new PessoaElenco("Rafael Costa", "Soldado"),
                new PessoaElenco("Lúcia Prado", "Capitã"),
                new PessoaElenco("Paulo Mendes", "Engenheiro"));
    }

    @Override
    public List<Faixa> faixas(String idTitulo, TipoFaixa tipo) {
        boolean tears = TEARS_OF_STEEL.equals(idTitulo);
        if (tipo == TipoFaixa.AUDIO) {
            return tears
                    ? Arrays.asList(new Faixa("a1", tipo, "Inglês · Dolby Digital 5.1", true),
                                    new Faixa("a2", tipo, "Inglês · Estéreo", false))
                    : Collections.singletonList(new Faixa("a1", tipo, "Inglês · Estéreo", true));
        }
        return tears
                ? Arrays.asList(new Faixa("l0", tipo, "Desligadas", false),
                                new Faixa("l1", tipo, "Português (BR)", true),
                                new Faixa("l2", tipo, "Inglês", false))
                : Arrays.asList(new Faixa("l0", tipo, "Desligadas", true),
                                new Faixa("l1", tipo, "Português (BR)", false));
    }

    @Override
    public List<Integer> opcoesCapa(String idTitulo) {
        Titulo t = titulos.get(idTitulo);
        if (t == null) return Collections.emptyList();
        // tv-editar: a capa atual + 3 opções do TMDB
        return semRepetir(4, t.corCapa, cor("#1e1b4b"), cor("#1e293b"), cor("#083344"), cor("#172554"));
    }

    @Override
    public List<Integer> opcoesFundo(String idTitulo) {
        Titulo t = titulos.get(idTitulo);
        if (t == null) return Collections.emptyList();
        // tv-editar: o fundo atual + 2 opções do TMDB
        return semRepetir(3, t.corFundo, cor("#1e293b"), cor("#1e1b4b"), cor("#172554"));
    }

    private static List<Integer> semRepetir(int quantidade, int primeira, int... candidatas) {
        List<Integer> lista = new ArrayList<>();
        lista.add(primeira);
        for (int c : candidatas) {
            if (lista.size() >= quantidade) break;
            if (!lista.contains(c)) lista.add(c);
        }
        return lista;
    }

    @Override
    public List<ResultadoTmdb> buscarTmdb(String idTitulo, String consulta) {
        Titulo base = titulos.get(idTitulo);
        if (base == null) return Collections.emptyList();
        // Simulado: os resultados não dependem do texto buscado
        List<ResultadoTmdb> lista = resultadosTmdb(base);
        String escolhido = estado.resultadoTmdb(idTitulo);
        for (ResultadoTmdb r : lista) {
            r.atual = escolhido == null ? r.id.equals(idTitulo) : r.id.equals(escolhido);
        }
        return lista;
    }

    /** tv-busca-tmdb: o título atual + 3 resultados fictícios (D8). */
    private List<ResultadoTmdb> resultadosTmdb(Titulo base) {
        Tipo tipo = base.tipo == null ? Tipo.FILME : base.tipo;
        List<ResultadoTmdb> lista = new ArrayList<>();
        lista.add(new ResultadoTmdb(base.id, base.titulo, base.ano, tipo, base.sinopse, base.corCapa, base.corFundo));
        if (TEARS_OF_STEEL.equals(base.id)) {
            lista.add(new ResultadoTmdb("tmdb_steel_tears", "Steel Tears", "2008", Tipo.FILME,
                    "Resultado de exemplo do TMDB.", cor("#1e293b"), cor("#1e293b")));
            lista.add(new ResultadoTmdb("tmdb_tears_machine", "Tears of the Machine", "2016", Tipo.FILME,
                    "Resultado de exemplo do TMDB.", cor("#1e1b4b"), cor("#1e1b4b")));
            lista.add(new ResultadoTmdb("tmdb_city_steel", "City of Steel", "2019", Tipo.FILME,
                    "Resultado de exemplo do TMDB.", cor("#083344"), cor("#083344")));
        } else {
            String nome = base.titulo;
            int[] cores = {cor("#1e293b"), cor("#1e1b4b"), cor("#083344")};
            for (int i = 0; i < cores.length; i++) {
                lista.add(new ResultadoTmdb(base.id + "_tmdb_" + (i + 1), nome + " " + (i + 2), "20" + (15 + i), tipo,
                        "Resultado de exemplo do TMDB.", cores[i], cores[i]));
            }
        }
        return lista;
    }

    @Override
    public List<Pasta> pastas() {
        List<Pasta> lista = new ArrayList<>();
        for (Pasta base : pastas) {
            Pasta p = base.copia();
            p.tipo = estado.tipoPasta(p.id, base.tipo);
            lista.add(p);
        }
        return lista;
    }

    @Override
    public List<Titulo> naoIdentificados() {
        return lista(naoIdentificados.toArray(new String[0]));
    }

    @Override
    public boolean hdConectado() {
        return estado.hdConectado();
    }

    @Override
    public void definirFavorito(String idTitulo, boolean favorito) {
        estado.definirFavorito(idTitulo, favorito);
    }

    @Override
    public void definirAssistido(String id, boolean assistido) {
        estado.definirAssistido(id, assistido);
    }

    @Override
    public void definirPosicao(String id, int segundos) {
        estado.definirPosicao(id, Math.max(0, segundos));
    }

    @Override
    public void salvarEdicao(String idTitulo, Map<Campo, String> valores) {
        Titulo atual = titulo(idTitulo);
        if (atual == null || valores == null) return;
        for (Map.Entry<Campo, String> item : valores.entrySet()) {
            String novo = item.getValue() == null ? "" : item.getValue().trim();
            String antes = atual.valor(item.getKey());
            if (!novo.equals(antes == null ? "" : antes)) estado.definirCampoEditado(idTitulo, item.getKey(), novo);
        }
    }

    @Override
    public void restaurarOriginal(String idTitulo) {
        estado.restaurarOriginal(idTitulo);
    }

    @Override
    public void selecionarResultadoTmdb(String idTitulo, String idResultado) {
        estado.definirResultadoTmdb(idTitulo, idResultado);
        estado.limparCapaFundo(idTitulo);
    }

    @Override
    public void escolherCapa(String idTitulo, int cor) {
        estado.definirCapa(idTitulo, cor);
    }

    @Override
    public void escolherFundo(String idTitulo, int cor) {
        estado.definirFundo(idTitulo, cor);
    }

    @Override
    public void definirTipoPasta(String idPasta, TipoPasta tipo) {
        estado.definirTipoPasta(idPasta, tipo);
    }

    @Override
    public synchronized void adicionarPasta(Origem origem, String caminho) {
        for (Pasta p : pastas) {
            if (p.caminho.equals(caminho)) return;
        }
        String nome = caminho.replaceAll("^.*/\\s*", "");
        String chave = normalizar(nome);
        TipoPasta tipo = chave.equals("filmes") ? TipoPasta.FILMES
                : chave.equals("series") ? TipoPasta.SERIES
                : chave.equals("shows") ? TipoPasta.SHOWS : TipoPasta.NENHUM;
        pastas.add(pasta("adicionada_" + pastas.size(), nome, origem, caminho, tipo != TipoPasta.NENHUM, 0, 0, 0, tipo));
    }

    @Override
    public void definirHdConectado(boolean conectado) {
        estado.definirHdConectado(conectado);
    }

    @Override
    public void atualizarBiblioteca() {
        // F1: nada a fazer; a tela mostra o aviso. F3: NetworkAutoRefresh.forceRescan do Nova.
    }

    private static boolean contem(List<Titulo> lista, String id) {
        for (Titulo t : lista) if (t.id.equals(id)) return true;
        return false;
    }

    private static String normalizar(String texto) {
        if (texto == null) return "";
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.toLowerCase(Locale.ROOT).trim();
    }
}
