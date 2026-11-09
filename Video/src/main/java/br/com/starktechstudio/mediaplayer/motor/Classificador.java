// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import java.io.File;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Classifica cada vídeo pela pasta e pelo nome (F2, etapa 3; D4):
 * <ul>
 * <li>Filmes: "Nome do filme (2012).mkv" (o ano também vale solto, como em
 * "Nome.do.Filme.2012.1080p.mkv", ou no nome da pasta do filme); sem ano, o título é o nome.</li>
 * <li>Séries: "Nome da série / Temporada N / 01.mkv". Temporada pela pasta ("Temporada N",
 * "Temp N", "Season N", "T01", "S01"); episódio pelo arquivo: só o número ("01.mkv") ou
 * "S01E01", "1x01" e "E01" no nome. "S01E01" e "1x01" trazem a temporada junto, então valem
 * também sem a pasta da temporada (e, direto na pasta Séries, o nome da série vem antes deles).</li>
 * <li>Shows: "Artista - Show (2019).mkv" (ano opcional; sem " - ", só o título). A primeira subpasta
 * abaixo da pasta Shows é o grupo (D7: "Shows/Nacionais/…"); direto em Shows, sem grupo.</li>
 * <li>Desenhos (F3): como Filmes, um arquivo só ("Nome do desenho (2019).mkv").</li>
 * <li>Stand-up (F3): como Filmes, um arquivo só ("Nome do especial (2021).mkv").</li>
 * </ul>
 * Pasta Ignorar e arquivos que não são vídeo ficam de fora; pasta sem tipo e nomes fora do padrão
 * vão para os não identificados, com o motivo. Sem diferenciar maiúscula, minúscula e acento.
 */
public final class Classificador {

    public enum Tipo { FILME, EPISODIO, DESENHO, SHOW, STANDUP, NAO_IDENTIFICADO }

    public enum Motivo {
        /** A pasta ainda não tem tipo ("escolha o tipo"). */
        PASTA_SEM_TIPO,
        /** O nome não segue o padrão do tipo da pasta. */
        FORA_DO_PADRAO,
        /** Episódio sem temporada (nem pela pasta nem pelo nome). */
        SEM_TEMPORADA,
        /** Na pasta da série, mas sem número de episódio. */
        SEM_EPISODIO,
        /** Pastas demais entre a série e o arquivo. */
        FUNDO_DEMAIS
    }

    /** Um vídeo classificado. Episódio: {@link #serie}, {@link #temporada}, {@link #episodio}. */
    public static final class Item {
        public final Tipo tipo;
        public final String caminho;
        /** Filme e show: o título; episódio: o nome depois do "S01E01", se houver (senão nulo). */
        public final String titulo;
        /** Ano do filme, do show ou da série; nulo se o nome não traz. */
        public final Integer ano;
        public final String serie;
        public final int temporada;
        public final int episodio;
        public final String artista;
        /** Show: a subpasta de Shows (D7); nulo = sem grupo. */
        public final String grupo;
        public final Motivo motivo;

        private Item(Tipo tipo, String caminho, String titulo, Integer ano, String serie, int temporada,
                     int episodio, String artista, String grupo, Motivo motivo) {
            this.tipo = tipo;
            this.caminho = caminho;
            this.titulo = titulo;
            this.ano = ano;
            this.serie = serie;
            this.temporada = temporada;
            this.episodio = episodio;
            this.artista = artista;
            this.grupo = grupo;
            this.motivo = motivo;
        }

        static Item filme(String caminho, String titulo, Integer ano) {
            return new Item(Tipo.FILME, caminho, titulo, ano, null, 0, 0, null, null, null);
        }

        /** Stand-up (F3): o mesmo do filme — um arquivo só, título e ano. */
        static Item standup(Item comoFilme) {
            if (comoFilme.tipo != Tipo.FILME) return comoFilme;
            return new Item(Tipo.STANDUP, comoFilme.caminho, comoFilme.titulo, comoFilme.ano, null, 0, 0, null, null, null);
        }

        /** Desenho (F3): o mesmo do filme — um arquivo só, título e ano. */
        static Item desenho(Item comoFilme) {
            if (comoFilme.tipo != Tipo.FILME) return comoFilme;
            return new Item(Tipo.DESENHO, comoFilme.caminho, comoFilme.titulo, comoFilme.ano, null, 0, 0, null, null, null);
        }

        static Item show(String caminho, String artista, String titulo, Integer ano, String grupo) {
            return new Item(Tipo.SHOW, caminho, titulo, ano, null, 0, 0, artista, grupo, null);
        }

        static Item episodio(String caminho, String serie, Integer ano, int temporada, int episodio, String titulo) {
            return new Item(Tipo.EPISODIO, caminho, titulo, ano, serie, temporada, episodio, null, null, null);
        }

        static Item naoIdentificado(String caminho, Motivo motivo) {
            return new Item(Tipo.NAO_IDENTIFICADO, caminho, null, null, null, 0, 0, null, null, motivo);
        }
    }

    /** Uma série montada: temporadas em ordem, episódios em ordem dentro de cada uma. */
    public static final class Serie {
        public final String nome;
        public final Integer ano;
        /** Número da temporada → episódios. */
        public final Map<Integer, List<Item>> temporadas;

        Serie(String nome, Integer ano, Map<Integer, List<Item>> temporadas) {
            this.nome = nome;
            this.ano = ano;
            this.temporadas = temporadas;
        }

        public int episodios() {
            int total = 0;
            for (List<Item> lista : temporadas.values()) total += lista.size();
            return total;
        }
    }

    private static final String ANO = "((?:19|20)\\d{2})";
    /** "Nome (2012)" ou "Nome [2012]". */
    private static final Pattern ANO_ENTRE_PARENTESES = Pattern.compile("^(.*?)\\s*[(\\[]" + ANO + "[)\\]].*$");
    /** "Nome 2012 1080p": o último ano solto, com título antes dele. */
    private static final Pattern ANO_SOLTO = Pattern.compile("^(.+)\\s" + ANO + "(?:\\s.*)?$");
    private static final Pattern S_E = Pattern.compile("s(\\d{1,2})[\\s._-]*e(\\d{1,3})(?!\\d)");
    private static final Pattern N_X_N = Pattern.compile("(?<!\\d)(\\d{1,2})x(\\d{1,3})(?!\\d)");
    private static final Pattern SO_E = Pattern.compile("(?:^|[\\s._-])e(\\d{1,3})(?!\\d)");
    private static final Pattern SO_NUMERO = Pattern.compile("^(\\d{1,3})$");
    private static final Pattern TEMPORADA = Pattern.compile("^(?:temporada|temp|season)\\s*(\\d{1,2})$");
    private static final Pattern TEMPORADA_CURTA = Pattern.compile("^[ts](\\d{1,2})$");

    /** Pastas abaixo de uma cadastrada que a varredura percorre (evita laço de link). */
    private static final int PROFUNDIDADE_MAXIMA = 8;

    private Classificador() {}

    /** Classifica o vídeo dentro da pasta; nulo se fica de fora (pasta Ignorar ou não é vídeo). */
    public static Item classificar(String caminhoArquivo, Pastas.Pasta pasta) {
        String nomeArquivo = new File(caminhoArquivo).getName();
        if (!Nomes.ehVideo(nomeArquivo) || pasta.tipo == Pastas.Tipo.IGNORAR) return null;
        if (pasta.tipo == null) return Item.naoIdentificado(caminhoArquivo, Motivo.PASTA_SEM_TIPO);
        String relativo = caminhoArquivo.substring(pasta.caminho.length() + 1);
        String[] partes = relativo.split("/");
        switch (pasta.tipo) {
            case FILMES: return filme(caminhoArquivo, partes);
            case DESENHOS: return Item.desenho(filme(caminhoArquivo, partes));
            case STANDUP: return Item.standup(filme(caminhoArquivo, partes));
            case SHOWS: return show(caminhoArquivo, partes);
            default: return episodio(caminhoArquivo, partes);
        }
    }

    // ---- Filmes ----

    private static Item filme(String caminho, String[] partes) {
        String base = Nomes.limpar(Nomes.semExtensao(partes[partes.length - 1]));
        TituloAno doArquivo = tituloAno(base);
        // Filme na própria pasta ("Nome (2012)/filme.mkv"): o ano pode estar só no nome da pasta
        if (doArquivo.ano == null && partes.length >= 2) {
            TituloAno daPasta = tituloAno(Nomes.limpar(partes[partes.length - 2]));
            if (daPasta.ano != null && !daPasta.titulo.isEmpty()) return Item.filme(caminho, daPasta.titulo, daPasta.ano);
        }
        if (doArquivo.titulo.isEmpty()) return Item.naoIdentificado(caminho, Motivo.FORA_DO_PADRAO);
        return Item.filme(caminho, doArquivo.titulo, doArquivo.ano);
    }

    // ---- Shows ----

    private static Item show(String caminho, String[] partes) {
        TituloAno nome = tituloAno(Nomes.limpar(Nomes.semExtensao(partes[partes.length - 1])));
        if (nome.titulo.isEmpty()) return Item.naoIdentificado(caminho, Motivo.FORA_DO_PADRAO);
        String grupo = partes.length >= 2 ? partes[0].trim() : null;
        int traco = nome.titulo.indexOf(" - ");
        if (traco > 0) {
            String artista = nome.titulo.substring(0, traco).trim();
            String titulo = nome.titulo.substring(traco + 3).trim();
            if (!artista.isEmpty() && !titulo.isEmpty()) return Item.show(caminho, artista, titulo, nome.ano, grupo);
        }
        return Item.show(caminho, null, nome.titulo, nome.ano, grupo);
    }

    // ---- Nome em comum (D13) ----

    public static String chaveNome(Item item) {
        return chaveNome(item.tipo, item.titulo, item.ano, item.serie, item.temporada, item.episodio, item.artista);
    }

    /**
     * Chave do nome em comum: vídeos com a mesma chave herdam a edição à mão uns dos outros. Vem da
     * classificação, nunca do nome do arquivo (filme: título + ano; episódio: série + SxxEyy; show:
     * artista + título; desenho e stand-up: como filme, cada um no seu tipo), sem diferenciar
     * maiúscula, minúscula e acento. Nula: não identificado ou sem nome (não herda).
     */
    public static String chaveNome(Tipo tipo, String titulo, Integer ano, String serie, int temporada, int episodio, String artista) {
        if (tipo == null) return null;
        switch (tipo) {
            case FILME:
            case DESENHO:
            case STANDUP: {
                String nome = Nomes.normalizar(titulo);
                if (nome.isEmpty()) return null;
                return tipo.name().toLowerCase(Locale.ROOT) + ":" + nome + "|" + (ano == null ? "" : ano);
            }
            case EPISODIO: {
                String nome = Nomes.normalizar(serie);
                if (nome.isEmpty()) return null;
                return String.format(Locale.ROOT, "episodio:%s|S%02dE%02d", nome, temporada, episodio);
            }
            case SHOW: {
                String nome = Nomes.normalizar(titulo);
                if (nome.isEmpty()) return null;
                return "show:" + Nomes.normalizar(artista) + "|" + nome;
            }
            default:
                return null;
        }
    }

    // ---- Séries ----

    private static Item episodio(String caminho, String[] partes) {
        if (partes.length > 3) return Item.naoIdentificado(caminho, Motivo.FUNDO_DEMAIS);
        String limpo = Nomes.limpar(Nomes.semExtensao(partes[partes.length - 1]));
        // Os códigos são só letras sem acento e números: buscar no texto em minúsculas mantém as
        // posições iguais às do nome limpo (de onde saem o nome da série e o do episódio)
        String minusculo = limpo.toLowerCase(Locale.ROOT);

        int temporada = -1;
        int episodio = -1;
        int fimDoCodigo = -1;
        Matcher codigo = S_E.matcher(minusculo);
        if (!codigo.find()) {
            codigo = N_X_N.matcher(minusculo);
            if (!codigo.find()) codigo = null;
        }
        if (codigo != null) {
            temporada = Integer.parseInt(codigo.group(1));
            episodio = Integer.parseInt(codigo.group(2));
            fimDoCodigo = codigo.end();
        }

        if (partes.length == 1) {
            // Direto na pasta Séries: só com "S01E01" / "1x01", e o nome da série vem antes do código
            if (codigo == null) return Item.naoIdentificado(caminho, Motivo.FORA_DO_PADRAO);
            TituloAno serie = tituloAno(aparar(limpo.substring(0, codigo.start())));
            if (serie.titulo.isEmpty()) return Item.naoIdentificado(caminho, Motivo.FORA_DO_PADRAO);
            return Item.episodio(caminho, serie.titulo, serie.ano, temporada, episodio, tituloDoEpisodio(limpo, fimDoCodigo));
        }

        TituloAno serie = tituloAno(Nomes.limpar(partes[0]));
        if (serie.titulo.isEmpty()) return Item.naoIdentificado(caminho, Motivo.FORA_DO_PADRAO);
        if (codigo == null) {
            Matcher so = SO_NUMERO.matcher(minusculo);
            if (so.matches()) {
                episodio = Integer.parseInt(so.group(1));
            } else {
                Matcher e = SO_E.matcher(minusculo);
                if (e.find()) {
                    episodio = Integer.parseInt(e.group(1));
                    fimDoCodigo = e.end();
                }
            }
            if (episodio < 0) return Item.naoIdentificado(caminho, Motivo.SEM_EPISODIO);
            temporada = partes.length == 3 ? temporadaDaPasta(partes[1]) : -1;
            if (temporada < 0) return Item.naoIdentificado(caminho, Motivo.SEM_TEMPORADA);
        }
        return Item.episodio(caminho, serie.titulo, serie.ano, temporada, episodio, tituloDoEpisodio(limpo, fimDoCodigo));
    }

    /** "Temporada 2", "Temp 2", "Season 2", "T02", "S02" → 2; -1 se não é pasta de temporada. */
    static int temporadaDaPasta(String nome) {
        String normalizado = Nomes.normalizar(Nomes.limpar(nome));
        Matcher longa = TEMPORADA.matcher(normalizado);
        if (longa.matches()) return Integer.parseInt(longa.group(1));
        Matcher curta = TEMPORADA_CURTA.matcher(normalizado);
        return curta.matches() ? Integer.parseInt(curta.group(1)) : -1;
    }

    /** O que vem depois do código no nome limpo ("S01E01 - Piloto" → "Piloto"); nulo se nada. */
    private static String tituloDoEpisodio(String limpo, int fimDoCodigo) {
        if (fimDoCodigo < 0 || fimDoCodigo >= limpo.length()) return null;
        String resto = limpo.substring(fimDoCodigo).replaceAll("^[\\s._-]+", "").trim();
        return resto.isEmpty() ? null : resto;
    }

    // ---- Título e ano ----

    private static final class TituloAno {
        final String titulo;
        final Integer ano;

        TituloAno(String titulo, Integer ano) {
            this.titulo = titulo;
            this.ano = ano;
        }
    }

    static TituloAno tituloAno(String nome) {
        Matcher entre = ANO_ENTRE_PARENTESES.matcher(nome);
        if (entre.matches()) return new TituloAno(aparar(entre.group(1)), Integer.valueOf(entre.group(2)));
        Matcher solto = ANO_SOLTO.matcher(nome);
        if (solto.matches() && !aparar(solto.group(1)).isEmpty()) {
            return new TituloAno(aparar(solto.group(1)), Integer.valueOf(solto.group(2)));
        }
        return new TituloAno(aparar(nome), null);
    }

    private static String aparar(String texto) {
        return texto.replaceAll("^[\\s._-]+|[\\s._-]+$", "").trim();
    }

    // ---- Varredura, séries e não identificados ----

    /**
     * Percorre as pastas cadastradas (menos as Ignorar) e classifica cada vídeo. Uma subpasta
     * que também está cadastrada fica com o tipo dela (é percorrida só uma vez, por ela mesma).
     */
    public static List<Item> varrer(List<Pastas.Pasta> pastas) {
        Set<String> cadastradas = new HashSet<>();
        for (Pastas.Pasta p : pastas) cadastradas.add(p.caminho);
        List<Item> itens = new ArrayList<>();
        for (Pastas.Pasta pasta : pastas) {
            if (pasta.tipo == Pastas.Tipo.IGNORAR) continue;
            percorrer(new File(pasta.caminho), pasta, cadastradas, new HashSet<>(), 0, itens);
        }
        return itens;
    }

    private static void percorrer(File dir, Pastas.Pasta pasta, Set<String> cadastradas, Set<String> vistas,
                                  int profundidade, List<Item> saida) {
        if (profundidade > PROFUNDIDADE_MAXIMA) return;
        try {
            if (!vistas.add(dir.getCanonicalPath())) return; // link que volta para trás
        } catch (IOException e) {
            return;
        }
        File[] itens = dir.listFiles();
        if (itens == null) return;
        List<File> ordem = new ArrayList<>(Arrays.asList(itens));
        Collections.sort(ordem, (a, b) -> a.getName().compareTo(b.getName()));
        for (File item : ordem) {
            if (item.getName().startsWith(".")) continue;
            if (item.isDirectory()) {
                if (!cadastradas.contains(item.getAbsolutePath())) {
                    percorrer(item, pasta, cadastradas, vistas, profundidade + 1, saida);
                }
            } else {
                Item classificado = classificar(item.getAbsolutePath(), pasta);
                if (classificado != null) saida.add(classificado);
            }
        }
    }

    /** Episódios agrupados por série (sem diferenciar maiúscula e acento), em ordem alfabética. */
    public static List<Serie> montarSeries(List<Item> itens) {
        Map<String, List<Item>> porSerie = new LinkedHashMap<>();
        Map<String, Item> primeiro = new LinkedHashMap<>();
        for (Item item : itens) {
            if (item.tipo != Tipo.EPISODIO) continue;
            String chave = Nomes.normalizar(item.serie);
            List<Item> lista = porSerie.get(chave);
            if (lista == null) {
                lista = new ArrayList<>();
                porSerie.put(chave, lista);
                primeiro.put(chave, item);
            }
            lista.add(item);
        }
        List<Serie> series = new ArrayList<>();
        for (Map.Entry<String, List<Item>> grupo : porSerie.entrySet()) {
            Map<Integer, List<Item>> temporadas = new TreeMap<>();
            Integer ano = null;
            for (Item episodio : grupo.getValue()) {
                if (ano == null) ano = episodio.ano;
                List<Item> daTemporada = temporadas.get(episodio.temporada);
                if (daTemporada == null) {
                    daTemporada = new ArrayList<>();
                    temporadas.put(episodio.temporada, daTemporada);
                }
                daTemporada.add(episodio);
            }
            for (List<Item> lista : temporadas.values()) {
                Collections.sort(lista, (a, b) -> Integer.compare(a.episodio, b.episodio));
            }
            series.add(new Serie(primeiro.get(grupo.getKey()).serie, ano, temporadas));
        }
        Collator ordem = Collator.getInstance(Locale.forLanguageTag("pt-BR"));
        ordem.setStrength(Collator.PRIMARY);
        Collections.sort(series, (a, b) -> ordem.compare(a.nome, b.nome));
        return series;
    }

    public static List<Item> naoIdentificados(List<Item> itens) {
        List<Item> lista = new ArrayList<>();
        for (Item item : itens) {
            if (item.tipo == Tipo.NAO_IDENTIFICADO) lista.add(item);
        }
        return lista;
    }
}
