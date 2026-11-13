// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.dados;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import java.io.File;
import java.text.Collator;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import br.com.starktechstudio.mediaplayer.dados.ModeloMusicas.Album;
import br.com.starktechstudio.mediaplayer.dados.ModeloMusicas.Artista;
import br.com.starktechstudio.mediaplayer.dados.ModeloMusicas.Musica;
import br.com.starktechstudio.mediaplayer.dados.ModeloMusicas.Playlist;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Titulo;
import br.com.starktechstudio.mediaplayer.motor.Acervo;
import br.com.starktechstudio.mediaplayer.motor.AcervoMusicas;
import br.com.starktechstudio.mediaplayer.motor.CapasMusica;
import br.com.starktechstudio.mediaplayer.motor.Discografia;
import br.com.starktechstudio.mediaplayer.motor.Edicoes;
import br.com.starktechstudio.mediaplayer.motor.Pastas;

/**
 * Músicas para as telas (1.5): artistas, álbuns e faixas, montados das músicas lidas pela
 * {@link br.com.starktechstudio.mediaplayer.motor.VarreduraMusicas}, e as playlists. Fotos de artista:
 * as mesmas dos Shows (tabela {@code artista}). As telas ouvem as mudanças ({@link #ouvir}) e também as da
 * biblioteca, que avisam quando uma foto chega.
 *
 * <p>1.5.1 (plano 006): os vídeos da pasta de Músicas (os títulos {@code CLIPE} da biblioteca de vídeo) entram
 * como álbuns em vídeo (pela subpasta) e como os vídeos soltos de cada artista; o artista conta músicas e
 * vídeos. O texto da Wikipédia dos álbuns (banco v8) e o do artista vão para as telas. Músicas e vídeos do HD
 * desligado ficam de fora até ligar.
 */
public final class RepositorioMusicas {

    /** Nomes de coletânea na tag de artista do álbum: entram como "Vários artistas". */
    private static final Set<String> VARIOS = new HashSet<>();

    static {
        for (String nome : new String[]{"various artists", "various", "varios artistas", "va", "v.a.", "diversos",
                "artistas variados", "coletanea"}) VARIOS.add(nome);
    }

    private static RepositorioMusicas instancia;

    private final Context app;
    private final AcervoMusicas acervo;
    private final Handler tela = new Handler(Looper.getMainLooper());
    private final List<Runnable> ouvintes = new CopyOnWriteArrayList<>();
    /** Playlists gravadas fora da tela. */
    private final ExecutorService gravacao = Executors.newSingleThreadExecutor(r -> new Thread(r, "StarkTechPlaylists"));
    private volatile Retrato retrato;
    /** As músicas do banco (lidas de novo só em {@link #recarregar}) e o texto dos álbuns. */
    private volatile List<AcervoMusicas.Musica> audio;
    private volatile Map<String, AcervoMusicas.TextoAlbum> textos;

    /** O que as telas leem, montado de uma vez. */
    private static final class Retrato {
        /** A versão da biblioteca de vídeo de que vieram os vídeos ({@link RepositorioReal#versao}). */
        int versaoVideos;
        final Map<String, Musica> musicas = new HashMap<>();
        /** 1.5.1: álbuns em vídeo e os vídeos soltos de cada artista. */
        final List<Album> colecoesDeVideo = new ArrayList<>();
        final Map<String, Album> soltosDoArtista = new HashMap<>();
        final Map<String, Integer> videosDoArtista = new HashMap<>();
        final Map<String, Set<String>> formatosDoArtista = new HashMap<>();
        final Map<String, String> qualidadeDoArtista = new HashMap<>();
        final List<Album> albuns = new ArrayList<>();
        final Map<String, Album> albunsPorId = new HashMap<>();
        /** Artista → álbuns dele (o do álbum ou de alguma faixa) e as faixas dele. */
        final Map<String, List<Album>> albunsDoArtista = new LinkedHashMap<>();
        final Map<String, List<Musica>> musicasDoArtista = new LinkedHashMap<>();
        final Map<String, String> nomeDoArtista = new HashMap<>();
        final Map<String, Integer> porPasta = new HashMap<>();
        final List<AcervoMusicas.Playlist> playlists = new ArrayList<>();
    }

    public static synchronized RepositorioMusicas de(Context contexto) {
        if (instancia == null) instancia = new RepositorioMusicas(contexto.getApplicationContext());
        return instancia;
    }

    private RepositorioMusicas(Context app) {
        this.app = app;
        this.acervo = AcervoMusicas.de(app);
    }

    // ---- Avisos ----

    /** {@code ouvinte} roda na thread da tela quando as músicas ou as playlists mudam. */
    public void ouvir(Runnable ouvinte) {
        if (!ouvintes.contains(ouvinte)) ouvintes.add(ouvinte);
    }

    public void deixarDeOuvir(Runnable ouvinte) {
        ouvintes.remove(ouvinte);
    }

    private final Runnable avisar = () -> {
        for (Runnable ouvinte : ouvintes) ouvinte.run();
    };

    private void notificar() {
        tela.removeCallbacks(avisar);
        tela.postDelayed(avisar, 150);
    }

    /** Lê de novo do banco (depois da varredura ou de mudar uma playlist) e avisa as telas. */
    public void recarregar() {
        audio = acervo.musicas();
        textos = acervo.textosDeAlbum();
        synchronized (this) {
            retrato = montar();
        }
        notificar();
    }

    /** O texto de um álbum chegou da Wikipédia (1.5.1). */
    public void recarregarTextos() {
        textos = acervo.textosDeAlbum();
        synchronized (this) {
            retrato = montar();
        }
        notificar();
    }

    /** Refaz quando a biblioteca de vídeo mudou (vídeos de música novos, quadros, HD ligado ou desligado). */
    private Retrato atual() {
        Retrato r = retrato;
        int versao = RepositorioReal.instancia(app).versao();
        if (r == null || r.versaoVideos != versao) {
            synchronized (this) {
                if (retrato == null || retrato.versaoVideos != versao) retrato = montar();
                r = retrato;
            }
        }
        return r;
    }

    private Retrato montar() {
        Retrato r = new Retrato();
        RepositorioReal real = RepositorioReal.instancia(app);
        r.versaoVideos = real.versao();
        if (audio == null) audio = acervo.musicas();
        if (textos == null) textos = acervo.textosDeAlbum();
        Map<String, AcervoMusicas.TextoAlbum> txt = textos;
        // HD desligado: as músicas dele somem até ligar (como os vídeos)
        Map<String, Boolean> montada = new HashMap<>();
        List<AcervoMusicas.Musica> todas = new ArrayList<>();
        for (AcervoMusicas.Musica m : audio) {
            Boolean ok = montada.get(m.pasta);
            if (ok == null) {
                ok = new File(m.pasta).isDirectory();
                montada.put(m.pasta, ok);
            }
            if (ok) todas.add(m);
        }
        Collator ordem = collator();
        for (AcervoMusicas.Musica m : todas) {
            Integer n = r.porPasta.get(m.pasta);
            r.porPasta.put(m.pasta, n == null ? 1 : n + 1);
        }
        for (Discografia.Album d : Discografia.albuns(todas)) {
            Album a = new Album();
            a.id = d.chave;
            a.nome = d.nome;
            a.variosArtistas = d.variosArtistas || (d.artista != null && VARIOS.contains(normalizar(d.artista)));
            a.artista = a.variosArtistas ? null : d.artista;
            a.idArtista = a.artista == null ? null : Acervo.chaveArtista(a.artista);
            a.ano = d.ano;
            a.capa = existe(d.capa);
            // 1.6: capa trocada à mão vale mais que a da música ou da pasta
            String editada = capaEditada(a.id);
            if (editada != null) a.capa = editada;
            a.cor = RepositorioReal.corDe(a.id);
            a.formato = formatos(d.musicas);
            aplicarTexto(a, txt);
            for (AcervoMusicas.Musica m : d.musicas) {
                Musica musica = musica(m, a);
                a.musicas.add(musica);
                r.musicas.put(musica.id, musica);
                // Faixa de outro artista (coletânea, participação): também conta para ele
                String nomeArtista = m.artista != null ? m.artista : a.artista;
                if (nomeArtista != null && !VARIOS.contains(normalizar(nomeArtista))) {
                    String chave = Acervo.chaveArtista(nomeArtista);
                    adicionar(r.musicasDoArtista, chave, musica);
                    if (!r.nomeDoArtista.containsKey(chave)) r.nomeDoArtista.put(chave, nomeArtista);
                    Set<String> deles = r.formatosDoArtista.get(chave);
                    if (deles == null) {
                        deles = new java.util.TreeSet<>();
                        r.formatosDoArtista.put(chave, deles);
                    }
                    String formato = formatoDoArquivo(m.caminho);
                    if (formato != null) deles.add(formato);
                    List<Album> dele = r.albunsDoArtista.get(chave);
                    if (dele == null || !dele.contains(a)) adicionar(r.albunsDoArtista, chave, a);
                }
            }
            r.albuns.add(a);
            r.albunsPorId.put(a.id, a);
        }
        // Álbuns: artista, ano e nome; sem nome, por último
        Collections.sort(r.albuns, (x, y) -> {
            int artista = comparar(ordem, x.artista, y.artista);
            if (artista != 0) return artista;
            if (x.nome == null || y.nome == null) return x.nome == null ? (y.nome == null ? 0 : 1) : -1;
            int ano = Integer.compare(x.ano == null ? 0 : x.ano, y.ano == null ? 0 : y.ano);
            return ano != 0 ? ano : ordem.compare(x.nome, y.nome);
        });
        montarVideos(r, real, txt, ordem);
        for (List<Album> dele : r.albunsDoArtista.values()) {
            Collections.sort(dele, (x, y) -> {
                int ano = Integer.compare(x.ano == null ? 0 : x.ano, y.ano == null ? 0 : y.ano);
                if (ano != 0) return ano;
                int nome = comparar(ordem, x.nome, y.nome);
                return nome != 0 ? nome : Boolean.compare(x.video, y.video);
            });
        }
        r.playlists.addAll(acervo.playlists());
        return r;
    }

    /**
     * Vídeos da pasta de Músicas (1.5.1): pelo artista e pela subpasta. Com subpasta, um álbum em vídeo; sem,
     * os vídeos soltos do artista. Em cada um, a ordem do número do arquivo e, sem ele, do nome. Capa: a imagem
     * da pasta ou o quadro do primeiro vídeo.
     */
    private void montarVideos(Retrato r, RepositorioReal real, Map<String, AcervoMusicas.TextoAlbum> txt, Collator ordem) {
        Map<String, Album> porColecao = new LinkedHashMap<>();
        for (Titulo t : real.clipes()) {
            String nomeArtista = t.artista;
            String chaveArtista = nomeArtista == null ? null : Acervo.chaveArtista(nomeArtista);
            String id = t.grupo != null ? "albumvideo:" + normalizar(nomeArtista) + "|" + normalizar(t.grupo)
                    : "videos:" + (chaveArtista == null ? "" : chaveArtista);
            Album a = porColecao.get(id);
            if (a == null) {
                a = new Album();
                a.id = id;
                a.nome = t.grupo;
                a.artista = nomeArtista;
                a.idArtista = chaveArtista;
                a.video = true;
                a.soltos = t.grupo == null;
                a.ano = numero(t.ano);
                a.cor = RepositorioReal.corDe(id);
                porColecao.put(id, a);
            }
            Musica v = new Musica();
            v.id = t.id;
            v.titulo = t.titulo;
            v.artista = nomeArtista;
            v.album = t.grupo;
            v.idAlbum = id;
            v.faixa = real.numeroDoClipe(t.id);
            v.duracaoMs = t.duracaoSegundos * 1000L;
            v.capa = t.quadro;
            v.cor = a.cor;
            v.video = true;
            a.musicas.add(v);
        }
        for (Album a : porColecao.values()) {
            Collections.sort(a.musicas, (x, y) -> {
                if (x.faixa != y.faixa) return Integer.compare(x.faixa == 0 ? Integer.MAX_VALUE : x.faixa, y.faixa == 0 ? Integer.MAX_VALUE : y.faixa);
                int nome = normalizar(x.titulo).compareTo(normalizar(y.titulo));
                return nome != 0 ? nome : x.id.compareTo(y.id);
            });
            Musica primeiro = a.musicas.get(0);
            File pasta = new File(primeiro.id).getParentFile();
            File imagem = pasta == null || a.soltos ? null : CapasMusica.imagemDaPasta(pasta);
            a.capa = imagem != null && imagem.canRead() ? imagem.getAbsolutePath() : primeiro.capa;
            String editada = capaEditada(a.id);
            if (editada != null) a.capa = editada;
            a.formato = real.formatoDoClipe(primeiro.id);
            if (!a.soltos) aplicarTexto(a, txt);
            r.colecoesDeVideo.add(a);
            r.albunsPorId.put(a.id, a);
            if (a.idArtista == null) continue;
            if (!r.nomeDoArtista.containsKey(a.idArtista)) r.nomeDoArtista.put(a.idArtista, a.artista);
            Integer conta = r.videosDoArtista.get(a.idArtista);
            r.videosDoArtista.put(a.idArtista, (conta == null ? 0 : conta) + a.musicas.size());
            if (!r.qualidadeDoArtista.containsKey(a.idArtista) && a.formato != null && !a.formato.isEmpty()) {
                r.qualidadeDoArtista.put(a.idArtista, a.formato.split(" · ")[0]);
            }
            if (a.soltos) r.soltosDoArtista.put(a.idArtista, a);
            else adicionar(r.albunsDoArtista, a.idArtista, a);
        }
        // Artista, álbuns em vídeo por ano e nome, e os vídeos soltos depois dos álbuns dele
        Collections.sort(r.colecoesDeVideo, (x, y) -> {
            int artista = comparar(ordem, x.artista, y.artista);
            if (artista != 0) return artista;
            if (x.soltos != y.soltos) return x.soltos ? 1 : -1;
            int ano = Integer.compare(x.ano == null ? 0 : x.ano, y.ano == null ? 0 : y.ano);
            return ano != 0 ? ano : comparar(ordem, x.nome, y.nome);
        });
    }

    private static void aplicarTexto(Album a, Map<String, AcervoMusicas.TextoAlbum> txt) {
        AcervoMusicas.TextoAlbum t = txt == null ? null : txt.get(a.id);
        if (t == null || t.situacao != Acervo.ACHADO) return;
        a.resumo = t.resumo;
        a.descricao = t.descricao;
    }

    /** "MP3" ou "FLAC · MP3": as extensões das faixas, sem repetir. */
    @Nullable
    private static String formatos(List<AcervoMusicas.Musica> musicas) {
        Set<String> deles = new java.util.TreeSet<>();
        for (AcervoMusicas.Musica m : musicas) {
            String f = formatoDoArquivo(m.caminho);
            if (f != null) deles.add(f);
        }
        return deles.isEmpty() ? null : android.text.TextUtils.join(" · ", deles);
    }

    @Nullable
    static String formatoDoArquivo(String caminho) {
        int ponto = caminho.lastIndexOf('.');
        if (ponto < 0 || ponto == caminho.length() - 1) return null;
        String extensao = caminho.substring(ponto + 1).toUpperCase(Locale.ROOT);
        return "OGA".equals(extensao) ? "OGG" : extensao;
    }

    @Nullable
    private static Integer numero(@Nullable String texto) {
        if (texto == null || texto.trim().isEmpty()) return null;
        try {
            return Integer.valueOf(texto.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Musica musica(AcervoMusicas.Musica m, Album a) {
        Musica musica = new Musica();
        musica.id = m.caminho;
        musica.titulo = m.titulo != null ? m.titulo : new File(m.caminho).getName();
        musica.artista = m.artista != null ? m.artista : a.artista;
        musica.album = a.nome;
        musica.idAlbum = a.id;
        musica.faixa = m.faixa;
        musica.disco = m.disco;
        musica.duracaoMs = m.duracaoMs;
        musica.capa = a.capa;
        musica.cor = a.cor;
        return musica;
    }

    private static <T> void adicionar(Map<String, List<T>> mapa, String chave, T item) {
        List<T> lista = mapa.get(chave);
        if (lista == null) {
            lista = new ArrayList<>();
            mapa.put(chave, lista);
        }
        lista.add(item);
    }

    // ---- Leitura ----

    public boolean temPastaDeMusicas() {
        return !Pastas.de(app).deMusicas().isEmpty();
    }

    /** Sem músicas e sem vídeos de música. */
    public boolean vazio() {
        Retrato r = atual();
        return r.musicas.isEmpty() && r.colecoesDeVideo.isEmpty();
    }

    public int musicasDaPasta(String pasta) {
        Integer n = atual().porPasta.get(pasta);
        return n == null ? 0 : n;
    }

    /** Em ordem alfabética, com a foto (a mesma dos Shows) e as contagens. */
    public List<Artista> artistas() {
        Retrato r = atual();
        Map<String, Acervo.Artista> fotos = Acervo.de(app).artistas();
        List<Artista> lista = new ArrayList<>();
        for (String chave : r.nomeDoArtista.keySet()) lista.add(artista(r, fotos, chave));
        Collator ordem = collator();
        Collections.sort(lista, (x, y) -> comparar(ordem, x.nome, y.nome));
        return lista;
    }

    @Nullable
    public Artista artista(String id) {
        Retrato r = atual();
        if (!r.nomeDoArtista.containsKey(id)) return null;
        return artista(r, Acervo.de(app).artistas(), id);
    }

    private Artista artista(Retrato r, Map<String, Acervo.Artista> fotos, String chave) {
        Artista a = new Artista();
        a.id = chave;
        a.nome = r.nomeDoArtista.get(chave);
        Acervo.Artista foto = fotos.get(chave);
        a.foto = foto == null ? null : existe(foto.foto);
        // 1.6: foto trocada à mão (a mesma edição do artista dos Shows)
        String editada = capaEditada(chave);
        if (editada != null) a.foto = editada;
        if (foto != null) {
            a.resumo = foto.resumo;
            a.descricao = foto.descricao;
        }
        a.cor = RepositorioReal.corDe(chave);
        List<Album> albuns = r.albunsDoArtista.get(chave);
        a.albuns = albuns == null ? 0 : albuns.size();
        List<Musica> musicas = r.musicasDoArtista.get(chave);
        a.musicas = musicas == null ? 0 : musicas.size();
        Integer videos = r.videosDoArtista.get(chave);
        a.videos = videos == null ? 0 : videos;
        Set<String> formatos = r.formatosDoArtista.get(chave);
        a.formatoMusicas = formatos == null || formatos.isEmpty() ? null : android.text.TextUtils.join(" · ", formatos);
        a.formatoVideos = r.qualidadeDoArtista.get(chave);
        return a;
    }

    /** Os vídeos do artista sem álbum (1.5.1); nulo se não tem. */
    @Nullable
    public Album videosSoltos(String idArtista) {
        return atual().soltosDoArtista.get(idArtista);
    }

    /** A sub-aba Vídeos (1.5.1): os álbuns em vídeo e os vídeos soltos de cada artista. */
    public List<Album> colecoesDeVideo() {
        return Collections.unmodifiableList(atual().colecoesDeVideo);
    }

    /** Álbuns (de áudio e em vídeo) com nome, para buscar o texto na Wikipédia: {chave, nome, artista ou nulo}. */
    public List<String[]> albunsParaTexto() {
        Retrato r = atual();
        List<String[]> lista = new ArrayList<>();
        for (Album a : r.albunsPorId.values()) {
            if (a.nome == null || a.soltos) continue;
            lista.add(new String[]{a.id, a.nome, a.artista});
        }
        return lista;
    }

    /** Nomes dos artistas que precisam de foto (a fila dos Shows busca também estes). */
    public List<String> artistasParaFoto() {
        return new ArrayList<>(atual().nomeDoArtista.values());
    }

    public List<Album> albuns() {
        return Collections.unmodifiableList(atual().albuns);
    }

    @Nullable
    public Album album(String id) {
        return atual().albunsPorId.get(id);
    }

    public List<Album> albunsDoArtista(String idArtista) {
        List<Album> lista = atual().albunsDoArtista.get(idArtista);
        return lista == null ? Collections.emptyList() : Collections.unmodifiableList(lista);
    }

    /** Todas as dele, álbum por álbum, na ordem dos álbuns. */
    public List<Musica> musicasDoArtista(String idArtista) {
        List<Musica> lista = new ArrayList<>();
        List<Musica> dele = atual().musicasDoArtista.get(idArtista);
        if (dele == null) return lista;
        Set<String> ids = new HashSet<>();
        for (Musica m : dele) ids.add(m.id);
        for (Album a : albunsDoArtista(idArtista)) {
            for (Musica m : a.musicas) if (ids.contains(m.id)) lista.add(m);
        }
        return lista;
    }

    @Nullable
    public Musica musica(String id) {
        return atual().musicas.get(id);
    }

    /** Todas as músicas de áudio da coleção ("Tocar tudo aleatório", 1.6). */
    public List<Musica> todasAsMusicas() {
        return new ArrayList<>(atual().musicas.values());
    }

    /** Refaz o que as telas leem (capa trocada à mão, 1.6) e avisa. */
    public void refazer() {
        synchronized (this) {
            retrato = montar();
        }
        notificar();
    }

    /** Capa trocada à mão (1.6): a imagem guardada nas edições, pela chave do álbum, do artista ou "playlist:<id>". */
    @Nullable
    private String capaEditada(String chave) {
        String referencia = Edicoes.de(app).ler(chave).get(Edicoes.Campo.CAPA);
        return referencia == null ? null : existe(Edicoes.arquivoDe(referencia));
    }

    /** As músicas pelos ids, na ordem; as que não estão na biblioteca agora ficam de fora. */
    public List<Musica> musicas(List<String> ids) {
        Retrato r = atual();
        List<Musica> lista = new ArrayList<>();
        for (String id : ids) {
            Musica m = r.musicas.get(id);
            if (m != null) lista.add(m);
        }
        return lista;
    }

    // ---- Playlists ----

    /** Em ordem alfabética. */
    public List<Playlist> playlists() {
        Retrato r = atual();
        List<Playlist> lista = new ArrayList<>();
        for (AcervoMusicas.Playlist p : r.playlists) lista.add(playlist(r, p));
        Collator ordem = collator();
        Collections.sort(lista, (x, y) -> ordem.compare(x.nome, y.nome));
        return lista;
    }

    @Nullable
    public Playlist playlist(long id) {
        Retrato r = atual();
        for (AcervoMusicas.Playlist p : r.playlists) if (p.id == id) return playlist(r, p);
        return null;
    }

    private Playlist playlist(Retrato r, AcervoMusicas.Playlist p) {
        Playlist lista = new Playlist();
        lista.id = p.id;
        lista.nome = p.nome;
        lista.total = p.musicas.size();
        lista.cor = RepositorioReal.corDe("playlist:" + p.id);
        for (int i = 0; i < p.musicas.size(); i++) {
            Musica m = r.musicas.get(p.musicas.get(i));
            if (m == null) continue;
            lista.musicas.add(m);
            lista.posicoes.add(i);
            if (lista.capa == null && m.capa != null) lista.capa = m.capa;
        }
        String editada = capaEditada("playlist:" + p.id);
        if (editada != null) lista.capa = editada;
        return lista;
    }

    /** Cria com as músicas dadas (pode ser nenhuma) e avisa as telas; {@code depois} recebe o id, na tela. */
    public void criarPlaylist(String nome, List<String> musicas, @Nullable IdCriado depois) {
        String limpo = nome.trim();
        gravacao.execute(() -> {
            long id = acervo.criarPlaylist(limpo, musicas);
            recarregar();
            if (depois != null) tela.post(() -> depois.aoCriar(id));
        });
    }

    public interface IdCriado {
        void aoCriar(long id);
    }

    public void renomearPlaylist(long id, String nome) {
        String limpo = nome.trim();
        gravacao.execute(() -> {
            acervo.renomearPlaylist(id, limpo);
            recarregar();
        });
    }

    public void apagarPlaylist(long id) {
        gravacao.execute(() -> {
            acervo.apagarPlaylist(id);
            recarregar();
        });
    }

    /** Põe as músicas no fim da playlist. */
    public void adicionarNaPlaylist(long id, List<String> musicas) {
        gravacao.execute(() -> {
            AcervoMusicas.Playlist p = acervo.playlist(id);
            if (p == null) return;
            List<String> nova = new ArrayList<>(p.musicas);
            nova.addAll(musicas);
            acervo.gravarMusicas(id, nova);
            recarregar();
        });
    }

    /** Tira a música da {@code posicao} da playlist inteira ({@link Playlist#posicoes}). */
    public void tirarDaPlaylist(long id, int posicao) {
        gravacao.execute(() -> {
            AcervoMusicas.Playlist p = acervo.playlist(id);
            if (p == null || posicao < 0 || posicao >= p.musicas.size()) return;
            List<String> nova = new ArrayList<>(p.musicas);
            nova.remove(posicao);
            acervo.gravarMusicas(id, nova);
            recarregar();
        });
    }

    /** Troca de lugar as músicas das posições {@code de} e {@code para} da playlist inteira. */
    public void moverNaPlaylist(long id, int de, int para) {
        gravacao.execute(() -> {
            AcervoMusicas.Playlist p = acervo.playlist(id);
            if (p == null) return;
            acervo.gravarMusicas(id, AcervoMusicas.mover(p.musicas, de, para));
            recarregar();
        });
    }

    // ---- Texto ----

    @Nullable
    private static String existe(@Nullable String arquivo) {
        return arquivo != null && new File(arquivo).isFile() ? arquivo : null;
    }

    /** Nulo (desconhecido) por último. */
    private static int comparar(Comparator<Object> ordem, @Nullable String a, @Nullable String b) {
        if (a == null || b == null) return a == null ? (b == null ? 0 : 1) : -1;
        return ordem.compare(a, b);
    }

    private static Collator collator() {
        Collator ordem = Collator.getInstance(Locale.forLanguageTag("pt-BR"));
        ordem.setStrength(Collator.PRIMARY);
        return ordem;
    }

    private static String normalizar(String texto) {
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }
}
