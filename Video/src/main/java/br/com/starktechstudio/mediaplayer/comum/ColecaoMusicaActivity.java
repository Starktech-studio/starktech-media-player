// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.os.Bundle;
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
import androidx.recyclerview.widget.GridLayoutManager;
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
 * Álbum, artista e playlist (1.5), no padrão do Detalhe: Voltar, capa (a foto redonda do artista), nome, a
 * linha de dados e os botões; embaixo, as faixas (álbum, playlist) ou os álbuns (artista). Álbum: Tocar,
 * Aleatório e Adicionar à playlist. Artista: Tocar tudo e Aleatório. Playlist: Tocar, Aleatório, Renomear e
 * Apagar. OK ou toque numa faixa toca a partir dela; segurar abre o menu da música. A barra "Tocando agora"
 * fica embaixo.
 */
public class ColecaoMusicaActivity extends AppCompatActivity {

    static final String EXTRA_TIPO = "br.com.starktechstudio.mediaplayer.extra.TIPO_COLECAO";
    static final String EXTRA_PLAYLIST = "br.com.starktechstudio.mediaplayer.extra.ID_PLAYLIST";
    static final int ALBUM = 0;
    static final int ARTISTA = 1;
    static final int PLAYLIST = 2;
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
    private CapaMusicaAdapter albuns;
    private BarraTocando barra;
    /** O que a tela mostra agora, para tocar (faixas do álbum, da playlist ou do artista). */
    private List<Musica> musicas = new ArrayList<>();
    private boolean focoInicial = true;
    @Nullable
    private String assinaturaDoTopo;
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

        int rotulo = tipo == ARTISTA ? R.string.st_musicas_artistas : tipo == PLAYLIST ? R.string.st_musicas_playlists : R.string.st_musicas_albuns;
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
        if (tipo == ARTISTA) montarGrade();
        else montarFaixas();

        barra = new BarraTocando(this);
        LinearLayout.LayoutParams lpBarra = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpBarra.topMargin = Medidas.px(this, 16);
        pagina.addView(barra.view, lpBarra);

        raiz.addView(pagina, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(raiz);
    }

    private void montarFaixas() {
        faixas = new LinhasMusicaAdapter(tipo == ALBUM, tipo == PLAYLIST,
                posicao -> Tocador.de(this).tocar(musicas, posicao, false),
                posicao -> MenuMusicaActivity.abrirMusica(this, faixas.musica(posicao).id, tipo == PLAYLIST ? idPlaylist : -1, posicao));
        if (tv) {
            VerticalGridView grade = (VerticalGridView) lista;
            grade.setNumColumns(1);
            grade.setVerticalSpacing(Medidas.px(this, 4));
        } else {
            lista.setLayoutManager(new LinearLayoutManager(this));
        }
        lista.setAdapter(faixas);
    }

    private void montarGrade() {
        albuns = new CapaMusicaAdapter(tv);
        int espaco = getResources().getDimensionPixelSize(tv ? R.dimen.st_tv_capa_espaco : R.dimen.st_tablet_capa_espaco);
        if (tv) {
            VerticalGridView grade = (VerticalGridView) lista;
            int largura = getResources().getDisplayMetrics().widthPixels
                    - 2 * getResources().getDimensionPixelSize(R.dimen.st_tv_margem_lateral) - lista.getPaddingStart() - lista.getPaddingEnd();
            int lado = (largura - espaco * (COLUNAS_TV - 1)) / COLUNAS_TV;
            albuns.definirLado(lado);
            grade.setNumColumns(COLUNAS_TV);
            grade.setColumnWidth(lado);
            grade.setHorizontalSpacing(espaco);
            grade.setVerticalSpacing(getResources().getDimensionPixelSize(R.dimen.st_tv_lista_espaco_vertical));
        } else {
            int capa = getResources().getDimensionPixelSize(R.dimen.st_tablet_capa_largura);
            GridLayoutManager gerente = new GridLayoutManager(this, 4);
            lista.setLayoutManager(gerente);
            lista.addItemDecoration(new RecyclerView.ItemDecoration() {
                @Override
                public void getItemOffsets(@androidx.annotation.NonNull android.graphics.Rect saida, @androidx.annotation.NonNull View item,
                                           @androidx.annotation.NonNull RecyclerView pai, @androidx.annotation.NonNull RecyclerView.State estado) {
                    saida.set(0, 0, 0, espaco);
                }
            });
            lista.addOnLayoutChangeListener((v, esq, topo, dir, base, esqAntes, topoAntes, dirAntes, baseAntes) -> {
                if (dir - esq == dirAntes - esqAntes) return;
                int util = dir - esq - v.getPaddingStart() - v.getPaddingEnd();
                int colunas = LinhasDeCapas.colunas(util, capa, espaco);
                v.post(() -> {
                    gerente.setSpanCount(colunas);
                    albuns.definirLado(util / colunas - espaco);
                });
            });
        }
        lista.setAdapter(albuns);
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
                getResources().getQuantityString(R.plurals.st_n_musicas, a.musicas.size(), a.musicas.size()),
                a.duracaoMs() > 0 ? PecasMusica.duracaoLonga(a.duracaoMs()) : null);
        List<Button> botoes = new ArrayList<>();
        botoes.add(botaoTocar(R.string.st_musicas_tocar, false));
        botoes.add(botaoTocar(R.string.st_musicas_aleatorio, true));
        Button playlist = PecasMusica.botao(this, R.string.st_musicas_adicionar_playlist, R.drawable.st_ic_playlist_nova, false);
        playlist.setOnClickListener(v -> MenuMusicaActivity.abrirEscolherPlaylist(this, ids(musicas)));
        botoes.add(playlist);
        if (a.idArtista != null && repositorio.artista(a.idArtista) != null) {
            Button artista = PecasMusica.botao(this, R.string.st_musicas_ir_artista, R.drawable.st_ic_shows, false);
            artista.setOnClickListener(v -> TelasMusica.abrirArtista(this, a.idArtista));
            botoes.add(artista);
        }
        cabecalho(PecasMusica.album(this, a.nome), linha, a.capa, a.cor, R.drawable.st_ic_disco, false, botoes);
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
        String linha = juntar(getResources().getQuantityString(R.plurals.st_n_albuns, a.albuns, a.albuns),
                getResources().getQuantityString(R.plurals.st_n_musicas, a.musicas, a.musicas));
        List<Button> botoes = new ArrayList<>();
        botoes.add(botaoTocar(R.string.st_musicas_tocar_tudo, false));
        botoes.add(botaoTocar(R.string.st_musicas_aleatorio, true));
        cabecalho(PecasMusica.artista(this, a.nome), linha, a.foto, a.cor, R.drawable.st_ic_shows, true, botoes);
        List<CapaMusicaAdapter.Capa> capas = new ArrayList<>();
        for (Album album : repositorio.albunsDoArtista(a.id)) {
            capas.add(new CapaMusicaAdapter.Capa(album.id, PecasMusica.album(this, album.nome),
                    album.ano == null ? getResources().getQuantityString(R.plurals.st_n_musicas, album.musicas.size(), album.musicas.size())
                            : String.valueOf(album.ano),
                    album.capa, album.cor, R.drawable.st_ic_disco, false,
                    () -> TelasMusica.abrirAlbum(this, album.id),
                    () -> Tocador.de(this).tocar(album.musicas, 0, false)));
        }
        albuns.atualizar(capas);
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
        cabecalho(p.nome, linha, p.capa, p.cor, R.drawable.st_ic_fila, false, botoes);
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

    /** Capa (a foto redonda do artista), nome, linha de dados e os botões, com o foco no primeiro (TV). */
    private void cabecalho(String nome, String linha, @Nullable String imagem, int cor, int icone, boolean redonda, List<Button> botoes) {
        // A biblioteca avisa muitas vezes durante a atualização: só refaz o topo se mudou
        StringBuilder assinatura = new StringBuilder(nome).append('|').append(linha).append('|').append(imagem);
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
        titulo.setEllipsize(android.text.TextUtils.TruncateAt.END);
        textos.addView(titulo);
        textos.addView(PecasMusica.linha(this, linha, R.font.st_inter_semibold, 21, R.color.st_texto_secundario),
                PecasMusica.comTopo(Medidas.px(this, 12)));
        LinearLayout fila = new LinearLayout(this);
        fila.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < botoes.size(); i++) {
            Button b = botoes.get(i);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i > 0) lp.setMarginStart(Medidas.px(this, 14));
            fila.addView(b, lp);
        }
        textos.addView(fila, PecasMusica.comTopo(Medidas.px(this, 26)));
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
