// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.os.Bundle;
import android.text.Layout;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.leanback.widget.VerticalGridView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.archos.mediacenter.video.R;

import java.util.ArrayList;
import java.util.List;

import br.com.starktechstudio.mediaplayer.dados.ModeloMusicas.Album;
import br.com.starktechstudio.mediaplayer.dados.ModeloMusicas.Artista;
import br.com.starktechstudio.mediaplayer.dados.ModeloMusicas.Musica;
import br.com.starktechstudio.mediaplayer.dados.ModeloMusicas.Playlist;
import br.com.starktechstudio.mediaplayer.dados.RepositorioBiblioteca;
import br.com.starktechstudio.mediaplayer.dados.RepositorioMusicas;
import br.com.starktechstudio.mediaplayer.musica.Tocador;

/**
 * Álbum, artista, playlist e os vídeos de um artista (1.5 e 1.5.1), no padrão do Detalhe: Voltar, capa (a
 * foto redonda do artista), nome, a linha de dados, os selos ("ÁLBUM · MP3", "MÚSICAS · MP3", "VÍDEOS ·
 * 1080p"), a descrição da Wikipédia com "Ver mais" e os botões; embaixo, as faixas (álbum, playlist, vídeos)
 * ou as seções do artista (Álbuns, de áudio e em vídeo, e Vídeos). Álbum: Tocar, Aleatório, Adicionar à
 * playlist. Álbum em vídeo e vídeos do artista: Assistir, que abre o player de vídeo e segue a sequência.
 * Artista: Tocar tudo e Aleatório. Playlist: Tocar, Aleatório, Renomear e Apagar. OK ou toque numa faixa toca
 * a partir dela (vídeo: abre o player); segurar uma música abre o menu dela. A barra "Tocando agora" fica
 * embaixo.
 */
public class ColecaoMusicaActivity extends AppCompatActivity {

    static final String EXTRA_TIPO = "br.com.starktechstudio.mediaplayer.extra.TIPO_COLECAO";
    static final String EXTRA_PLAYLIST = "br.com.starktechstudio.mediaplayer.extra.ID_PLAYLIST";
    static final int ALBUM = 0;
    static final int ARTISTA = 1;
    static final int PLAYLIST = 2;
    /** 1.5.1: os vídeos soltos do artista (o id é o da coleção, {@link RepositorioMusicas#album}). */
    static final int VIDEOS = 3;
    /** Álbuns do artista por linha na TV. */
    private static final int COLUNAS_TV = 6;

    private boolean tv;
    private int tipo;
    private String id;
    private long idPlaylist;
    private RepositorioMusicas repositorio;
    private LinearLayout cabecalho;
    private RecyclerView lista;
    private TextView aviso;
    private LinhasMusicaAdapter faixas;
    private SecoesArtistaAdapter secoes;
    private BarraTocando barra;
    /** O que a tela mostra agora, para tocar (faixas do álbum, da playlist ou do artista; os vídeos). */
    private List<Musica> musicas = new ArrayList<>();
    private boolean focoInicial = true;
    @Nullable
    private String assinaturaDoTopo;
    @Nullable
    private String assinaturaDasSecoes;
    private final Runnable aoMudar = this::mostrar;
    private final Runnable aoTocar = this::marcarTocando;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(Aparelho.tema(this));
        super.onCreate(savedInstanceState);
        tv = Aparelho.ehTv(this);
        tipo = getIntent().getIntExtra(EXTRA_TIPO, ALBUM);
        id = getIntent().getStringExtra(Navegacao.EXTRA_ID);
        idPlaylist = getIntent().getLongExtra(EXTRA_PLAYLIST, -1);
        repositorio = RepositorioMusicas.de(this);

        FrameLayout raiz = new FrameLayout(this);
        raiz.setBackgroundResource(R.color.st_fundo);
        raiz.addView(new FundoAnimado(this), new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        LinearLayout pagina = new LinearLayout(this);
        pagina.setOrientation(LinearLayout.VERTICAL);
        int lateral = getResources().getDimensionPixelSize(tv ? R.dimen.st_tv_margem_lateral : R.dimen.st_tablet_margem_h);
        pagina.setPadding(lateral, Medidas.px(this, 36), lateral, Medidas.px(this, 28));
        if (!tv) BordasSistema.aplicar(pagina, true, true, true, true);

        int rotulo = tipo == ARTISTA ? R.string.st_musicas_artistas : tipo == PLAYLIST ? R.string.st_musicas_playlists
                : tipo == VIDEOS ? R.string.st_musicas_videos : R.string.st_musicas_albuns;
        pagina.addView(PecasMusica.topo(this, getString(rotulo)));
        cabecalho = new LinearLayout(this);
        cabecalho.setOrientation(LinearLayout.HORIZONTAL);
        cabecalho.setGravity(Gravity.CENTER_VERTICAL);
        pagina.addView(cabecalho, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        aviso = Textos.novo(this, "", R.font.st_inter_semibold, 20, R.color.st_texto_secundario);
        aviso.setVisibility(View.GONE);
        pagina.addView(aviso, PecasMusica.comTopo(Medidas.px(this, 28)));

        lista = tv ? new VerticalGridView(this) : new RecyclerView(this);
        lista.setClipToPadding(false);
        lista.setVerticalScrollBarEnabled(false);
        int folga = Medidas.px(this, 12);
        lista.setPadding(folga, folga, folga, folga);
        LinearLayout.LayoutParams lpLista = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        lpLista.topMargin = Medidas.px(this, 18);
        pagina.addView(lista, lpLista);
        if (tv) {
            VerticalGridView grade = (VerticalGridView) lista;
            grade.setNumColumns(1);
            grade.setVerticalSpacing(Medidas.px(this, 4));
        } else {
            lista.setLayoutManager(new LinearLayoutManager(this));
        }
        if (tipo == ARTISTA) montarSecoes();
        else montarFaixas();

        barra = new BarraTocando(this);
        LinearLayout.LayoutParams lpBarra = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpBarra.topMargin = Medidas.px(this, 16);
        pagina.addView(barra.view, lpBarra);

        raiz.addView(pagina, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(raiz);
    }

    private void montarFaixas() {
        faixas = new LinhasMusicaAdapter(tipo == ALBUM || tipo == VIDEOS, tipo == PLAYLIST, this::tocarDaLista, posicao -> {
            Musica m = faixas.musica(posicao);
            // Vídeo não tem o menu da música (fila e playlist são do áudio)
            if (!m.video) MenuMusicaActivity.abrirMusica(this, m.id, tipo == PLAYLIST ? idPlaylist : -1, posicao);
        });
        lista.setAdapter(faixas);
    }

    /** Música: toca a partir dela; vídeo (1.5.1): abre o player de vídeo nele, que segue a sequência. */
    private void tocarDaLista(int posicao) {
        Musica m = musicas.get(posicao);
        if (m.video) Navegacao.abrirReproducao(this, m.id, Navegacao.DE_ONDE_PAROU);
        else Tocador.de(this).tocar(musicas, posicao, false);
    }

    /** Artista (1.5.1): seções Álbuns e Vídeos; as colunas saem da largura, como na grade da aba. */
    private void montarSecoes() {
        secoes = new SecoesArtistaAdapter(tv, v -> Navegacao.abrirReproducao(this, v.id, Navegacao.DE_ONDE_PAROU));
        int espaco = getResources().getDimensionPixelSize(tv ? R.dimen.st_tv_capa_espaco : R.dimen.st_tablet_capa_espaco);
        if (tv) {
            int largura = getResources().getDisplayMetrics().widthPixels
                    - 2 * getResources().getDimensionPixelSize(R.dimen.st_tv_margem_lateral) - lista.getPaddingStart() - lista.getPaddingEnd();
            secoes.definirGrade(COLUNAS_TV, (largura - espaco * (COLUNAS_TV - 1)) / COLUNAS_TV, espaco);
        } else {
            int capa = getResources().getDimensionPixelSize(R.dimen.st_tablet_capa_largura);
            lista.addOnLayoutChangeListener((v, esq, topo, dir, base, esqAntes, topoAntes, dirAntes, baseAntes) -> {
                if (dir - esq == dirAntes - esqAntes) return;
                int util = dir - esq - v.getPaddingStart() - v.getPaddingEnd();
                int colunas = LinhasDeCapas.colunas(util, capa, espaco);
                v.post(() -> secoes.definirGrade(colunas, (util - espaco * (colunas - 1)) / colunas, espaco));
            });
        }
        lista.setAdapter(secoes);
    }

    @Override
    protected void onResume() {
        super.onResume();
        repositorio.ouvir(aoMudar);
        RepositorioBiblioteca.atual(this).ouvir(aoMudar);
        Tocador.de(this).ouvir(aoTocar);
        barra.ligar();
        mostrar();
    }

    @Override
    protected void onPause() {
        repositorio.deixarDeOuvir(aoMudar);
        RepositorioBiblioteca.atual(this).deixarDeOuvir(aoMudar);
        Tocador.de(this).deixarDeOuvir(aoTocar);
        barra.desligar();
        super.onPause();
    }

    private void mostrar() {
        switch (tipo) {
            case ARTISTA:
                mostrarArtista();
                break;
            case PLAYLIST:
                mostrarPlaylist();
                break;
            case VIDEOS:
                mostrarVideos();
                break;
            default:
                mostrarAlbum();
                break;
        }
        marcarTocando();
    }

    private void marcarTocando() {
        if (faixas == null) return;
        Musica atual = Tocador.de(this).atual();
        faixas.definirTocando(atual == null ? null : atual.id, -1);
    }

    private void mostrarAlbum() {
        Album a = id == null ? null : repositorio.album(id);
        if (a == null) {
            finish();
            return;
        }
        musicas = new ArrayList<>(a.musicas);
        String linha = juntar(PecasMusica.artistaDoAlbum(this, a), a.ano == null ? null : String.valueOf(a.ano),
                getResources().getQuantityString(a.video ? R.plurals.st_n_videos : R.plurals.st_n_musicas, a.musicas.size(), a.musicas.size()),
                a.duracaoMs() > 0 ? PecasMusica.duracaoLonga(a.duracaoMs()) : null);
        List<Button> botoes = new ArrayList<>();
        if (a.video) {
            botoes.add(botaoAssistir());
        } else {
            botoes.add(botaoTocar(R.string.st_musicas_tocar, false));
            botoes.add(botaoTocar(R.string.st_musicas_aleatorio, true));
            Button playlist = PecasMusica.botao(this, R.string.st_musicas_adicionar_playlist, R.drawable.st_ic_playlist_nova, false);
            playlist.setOnClickListener(v -> MenuMusicaActivity.abrirEscolherPlaylist(this, ids(musicas)));
            botoes.add(playlist);
        }
        if (a.idArtista != null && repositorio.artista(a.idArtista) != null) {
            Button artista = PecasMusica.botao(this, R.string.st_musicas_ir_artista, R.drawable.st_ic_shows, false);
            artista.setOnClickListener(v -> TelasMusica.abrirArtista(this, a.idArtista));
            botoes.add(artista);
        }
        List<String> selos = new ArrayList<>();
        selos.add(getString(a.video ? R.string.st_musicas_selo_album_video : R.string.st_musicas_selo_album));
        if (a.formato != null) selos.add(a.formato);
        cabecalho(PecasMusica.album(this, a.nome), linha, selos, texto(a.resumo, a.descricao), a.capa, a.cor,
                a.video ? R.drawable.st_ic_assistir : R.drawable.st_ic_disco, false, botoes);
        faixas.atualizar(musicas);
        aviso.setVisibility(View.GONE);
    }

    /** Os vídeos do artista sem álbum (1.5.1); o id é o da coleção ("videos:" e o artista). */
    private void mostrarVideos() {
        Album a = id == null ? null : repositorio.album(id);
        Artista dono = a == null || a.idArtista == null ? null : repositorio.artista(a.idArtista);
        if (a == null) {
            finish();
            return;
        }
        musicas = new ArrayList<>(a.musicas);
        String nomeArtista = PecasMusica.artista(this, a.artista);
        String linha = juntar(getResources().getQuantityString(R.plurals.st_n_videos, a.musicas.size(), a.musicas.size()),
                a.duracaoMs() > 0 ? PecasMusica.duracaoLonga(a.duracaoMs()) : null);
        List<Button> botoes = new ArrayList<>();
        botoes.add(botaoAssistir());
        if (dono != null) {
            Button artista = PecasMusica.botao(this, R.string.st_musicas_ir_artista, R.drawable.st_ic_shows, false);
            artista.setOnClickListener(v -> TelasMusica.abrirArtista(this, dono.id));
            botoes.add(artista);
        }
        List<String> selos = new ArrayList<>();
        selos.add(getString(R.string.st_musicas_selo_videos));
        if (a.formato != null) selos.add(a.formato);
        String imagem = dono != null && dono.foto != null ? dono.foto : a.capa;
        cabecalho(getString(R.string.st_musicas_videos_de, nomeArtista), linha, selos, null, imagem,
                dono != null ? dono.cor : a.cor, R.drawable.st_ic_shows, dono != null && dono.foto != null, botoes);
        faixas.atualizar(musicas);
        aviso.setVisibility(View.GONE);
    }

    private void mostrarArtista() {
        Artista a = id == null ? null : repositorio.artista(id);
        if (a == null) {
            finish();
            return;
        }
        musicas = repositorio.musicasDoArtista(a.id);
        String linha = juntar(a.albuns > 0 ? getResources().getQuantityString(R.plurals.st_n_albuns, a.albuns, a.albuns) : null,
                a.musicas > 0 ? getResources().getQuantityString(R.plurals.st_n_musicas, a.musicas, a.musicas) : null,
                a.videos > 0 ? getResources().getQuantityString(R.plurals.st_n_videos, a.videos, a.videos) : null);
        List<Button> botoes = new ArrayList<>();
        if (!musicas.isEmpty()) {
            botoes.add(botaoTocar(R.string.st_musicas_tocar_tudo, false));
            botoes.add(botaoTocar(R.string.st_musicas_aleatorio, true));
        }
        Album soltos = repositorio.videosSoltos(a.id);
        if (musicas.isEmpty() && soltos != null) {
            Button assistir = PecasMusica.botao(this, R.string.st_musicas_assistir, R.drawable.st_ic_assistir, true);
            assistir.setOnClickListener(v -> Navegacao.abrirReproducao(this, soltos.musicas.get(0).id, Navegacao.DE_ONDE_PAROU));
            botoes.add(assistir);
        }
        List<String> selos = new ArrayList<>();
        if (a.musicas > 0) selos.add(juntar(getString(R.string.st_musicas_selo_musicas), a.formatoMusicas));
        if (a.videos > 0) selos.add(juntar(getString(R.string.st_musicas_selo_videos), a.formatoVideos));
        cabecalho(PecasMusica.artista(this, a.nome), linha, selos, texto(a.resumo, a.descricao), a.foto, a.cor,
                R.drawable.st_ic_shows, true, botoes);

        List<CapaMusicaAdapter.Capa> capas = new ArrayList<>();
        for (Album album : repositorio.albunsDoArtista(a.id)) {
            String sub = album.video ? juntar(getString(R.string.st_musicas_album_em_video), album.ano == null ? null : String.valueOf(album.ano))
                    : album.ano == null ? getResources().getQuantityString(R.plurals.st_n_musicas, album.musicas.size(), album.musicas.size())
                    : String.valueOf(album.ano);
            capas.add(new CapaMusicaAdapter.Capa(album.id, PecasMusica.album(this, album.nome), sub,
                    album.capa, album.cor, album.video ? R.drawable.st_ic_assistir : R.drawable.st_ic_disco, false,
                    () -> TelasMusica.abrirAlbum(this, album.id),
                    album.video ? () -> Navegacao.abrirReproducao(this, album.musicas.get(0).id, Navegacao.DE_ONDE_PAROU)
                            : () -> Tocador.de(this).tocar(album.musicas, 0, false)));
        }
        List<Musica> videos = soltos == null ? new ArrayList<>() : soltos.musicas;
        // Só refaz as seções se mudaram (a biblioteca avisa muito durante a atualização)
        StringBuilder assinatura = new StringBuilder();
        for (CapaMusicaAdapter.Capa c : capas) assinatura.append(c.id).append('|').append(c.linha).append('|').append(c.imagem).append(';');
        for (Musica v : videos) assinatura.append(v.id).append('|').append(v.titulo).append(';');
        if (!assinatura.toString().equals(assinaturaDasSecoes)) {
            assinaturaDasSecoes = assinatura.toString();
            secoes.limpar();
            secoes.secaoDeCapas(getString(R.string.st_musicas_albuns), capas);
            secoes.secaoDeVideos(getString(R.string.st_musicas_videos), videos);
            secoes.montar();
        }
        aviso.setVisibility(View.GONE);
    }

    private void mostrarPlaylist() {
        Playlist p = repositorio.playlist(idPlaylist);
        if (p == null) {
            finish();
            return;
        }
        musicas = new ArrayList<>(p.musicas);
        long total = 0;
        for (Musica m : p.musicas) total += m.duracaoMs;
        int fora = p.total - p.musicas.size();
        String linha = juntar(getResources().getQuantityString(R.plurals.st_n_musicas, p.musicas.size(), p.musicas.size()),
                total > 0 ? PecasMusica.duracaoLonga(total) : null,
                fora > 0 ? getResources().getQuantityString(R.plurals.st_musicas_fora, fora, fora) : null);
        List<Button> botoes = new ArrayList<>();
        if (!musicas.isEmpty()) {
            botoes.add(botaoTocar(R.string.st_musicas_tocar, false));
            botoes.add(botaoTocar(R.string.st_musicas_aleatorio, true));
        }
        Button renomear = PecasMusica.botao(this, R.string.st_musicas_renomear, R.drawable.st_ic_editar, false);
        renomear.setOnClickListener(v -> NomePlaylistActivity.abrirRenomear(this, p.id, p.nome));
        botoes.add(renomear);
        Button apagar = PecasMusica.botao(this, R.string.st_musicas_apagar, R.drawable.st_ic_lixeira, false);
        apagar.setOnClickListener(v -> MenuMusicaActivity.abrirApagar(this, p.id));
        botoes.add(apagar);
        List<String> selos = new ArrayList<>();
        selos.add(getString(R.string.st_musicas_selo_playlist));
        cabecalho(p.nome, linha, selos, null, p.capa, p.cor, R.drawable.st_ic_fila, false, botoes);
        faixas.atualizar(musicas);
        aviso.setText(R.string.st_musicas_playlist_vazia);
        aviso.setVisibility(musicas.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private Button botaoTocar(int texto, boolean aleatorio) {
        Button botao = PecasMusica.botao(this, texto, aleatorio ? R.drawable.st_ic_aleatorio : R.drawable.st_ic_assistir, !aleatorio);
        botao.setOnClickListener(v -> {
            if (musicas.isEmpty()) return;
            Tocador.de(this).tocar(musicas, aleatorio ? (int) (Math.random() * musicas.size()) : 0, aleatorio);
        });
        return botao;
    }

    /** Vídeos (1.5.1): do primeiro, no player de vídeo; ele segue a sequência sozinho. */
    private Button botaoAssistir() {
        Button botao = PecasMusica.botao(this, R.string.st_musicas_assistir, R.drawable.st_ic_assistir, true);
        botao.setOnClickListener(v -> {
            if (!musicas.isEmpty()) Navegacao.abrirReproducao(this, musicas.get(0).id, Navegacao.DE_ONDE_PAROU);
        });
        return botao;
    }

    /** O resumo ("Banda brasileira de sertanejo") e o texto da Wikipédia, juntos; nulo se não tem. */
    @Nullable
    private static String texto(@Nullable String resumo, @Nullable String descricao) {
        boolean temResumo = resumo != null && !resumo.isEmpty();
        boolean temDescricao = descricao != null && !descricao.isEmpty();
        if (!temResumo && !temDescricao) return null;
        if (!temDescricao) return resumo;
        return temResumo ? resumo + ". " + descricao : descricao;
    }

    /**
     * Capa (a foto redonda do artista), nome, linha de dados, os selos, a descrição (3 linhas e "Ver mais")
     * e os botões, com o foco no primeiro (TV).
     */
    private void cabecalho(String nome, String linha, List<String> selos, @Nullable String descricao, @Nullable String imagem,
                           int cor, int icone, boolean redonda, List<Button> botoes) {
        // A biblioteca avisa muitas vezes durante a atualização: só refaz o topo se mudou
        StringBuilder assinatura = new StringBuilder(nome).append('|').append(linha).append('|').append(imagem)
                .append('|').append(selos).append('|').append(descricao);
        for (Button b : botoes) assinatura.append('|').append(b.getText());
        if (assinatura.toString().equals(assinaturaDoTopo)) return;
        assinaturaDoTopo = assinatura.toString();
        View focado = getCurrentFocus();
        boolean focoNoTopo = focado != null && dentroDoTopo(focado);
        cabecalho.removeAllViews();
        // Celular deitado: a capa não toma a altura da lista
        int lado = Math.min(Medidas.px(this, tv ? 300 : 340), getResources().getDisplayMetrics().heightPixels / 4);
        cabecalho.addView(PecasMusica.capa(this, lado, cor, imagem, icone, redonda), new LinearLayout.LayoutParams(lado, lado));
        LinearLayout textos = new LinearLayout(this);
        textos.setOrientation(LinearLayout.VERTICAL);
        TextView titulo = Textos.novo(this, nome, R.font.st_inter_black, tv ? 52 : 46, R.color.st_texto);
        titulo.setLetterSpacing(-0.02f);
        titulo.setMaxLines(2);
        titulo.setEllipsize(TextUtils.TruncateAt.END);
        textos.addView(titulo);
        textos.addView(PecasMusica.linha(this, linha, R.font.st_inter_semibold, 21, R.color.st_texto_secundario),
                PecasMusica.comTopo(Medidas.px(this, 12)));
        if (!selos.isEmpty()) {
            LinearLayout chips = new LinearLayout(this);
            chips.setOrientation(LinearLayout.HORIZONTAL);
            List<Boolean> azuis = new ArrayList<>();
            for (int i = 0; i < selos.size(); i++) azuis.add(i == 0);
            PecasDetalhe.chips(chips, selos, azuis, tv);
            textos.addView(chips, PecasMusica.comTopo(Medidas.px(this, 14)));
        }
        Button verMais = null;
        if (descricao != null) {
            TextView texto = Textos.novo(this, descricao, R.font.st_inter_medium, 19, R.color.st_texto_secundario);
            texto.setMaxLines(3);
            texto.setEllipsize(TextUtils.TruncateAt.END);
            texto.setLineSpacing(0f, 1.2f);
            textos.addView(texto, PecasMusica.comTopo(Medidas.px(this, 16)));
            Button mais = PecasMusica.botao(this, R.string.st_ver_mais, 0, false);
            mais.setVisibility(View.GONE);
            mais.setOnClickListener(v -> Navegacao.abrirSinopse(this, nome, descricao));
            textos.addView(mais, PecasMusica.comTopo(Medidas.px(this, 10)));
            verMais = mais;
            // As linhas só existem depois de medir: "Ver mais" só se o texto não coube
            texto.post(() -> {
                Layout linhas = texto.getLayout();
                int ultima = linhas == null ? -1 : linhas.getLineCount() - 1;
                boolean cortou = ultima >= 0 && (linhas.getEllipsisCount(ultima) > 0 || linhas.getLineEnd(ultima) < texto.getText().length());
                mais.setVisibility(cortou ? View.VISIBLE : View.GONE);
            });
        }
        LinearLayout fila = new LinearLayout(this);
        fila.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < botoes.size(); i++) {
            Button b = botoes.get(i);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i > 0) lp.setMarginStart(Medidas.px(this, 14));
            fila.addView(b, lp);
        }
        textos.addView(fila, PecasMusica.comTopo(Medidas.px(this, verMais != null ? 20 : 26)));
        LinearLayout.LayoutParams lpTextos = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lpTextos.setMarginStart(Medidas.px(this, 40));
        cabecalho.addView(textos, lpTextos);
        LinearLayout.LayoutParams lpCabecalho = (LinearLayout.LayoutParams) cabecalho.getLayoutParams();
        lpCabecalho.topMargin = Medidas.px(this, 24);
        cabecalho.setLayoutParams(lpCabecalho);
        // TV: o foco fica no primeiro botão ao abrir e quando o topo é refeito com o foco nele
        if (tv && !botoes.isEmpty() && (focoInicial || focoNoTopo)) {
            focoInicial = false;
            Button primeiro = botoes.get(0);
            primeiro.post(primeiro::requestFocus);
        }
    }

    private boolean dentroDoTopo(View view) {
        View atual = view;
        while (atual != null) {
            if (atual == cabecalho) return true;
            atual = atual.getParent() instanceof View ? (View) atual.getParent() : null;
        }
        return false;
    }

    private static List<String> ids(List<Musica> musicas) {
        List<String> ids = new ArrayList<>();
        for (Musica m : musicas) ids.add(m.id);
        return ids;
    }

    private static String juntar(String... partes) {
        StringBuilder texto = new StringBuilder();
        for (String p : partes) {
            if (p == null || p.isEmpty()) continue;
            if (texto.length() > 0) texto.append(" · ");
            texto.append(p);
        }
        return texto.toString();
    }
}
