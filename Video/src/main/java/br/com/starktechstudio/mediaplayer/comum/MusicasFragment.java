// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.leanback.widget.VerticalGridView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.archos.mediacenter.video.R;

import java.util.ArrayList;
import java.util.List;

import br.com.starktechstudio.mediaplayer.dados.ModeloMusicas.Album;
import br.com.starktechstudio.mediaplayer.dados.ModeloMusicas.Artista;
import br.com.starktechstudio.mediaplayer.dados.ModeloMusicas.Playlist;
import br.com.starktechstudio.mediaplayer.dados.RepositorioBiblioteca;
import br.com.starktechstudio.mediaplayer.dados.RepositorioMusicas;
import br.com.starktechstudio.mediaplayer.motor.ArquivosDoAparelho;
import br.com.starktechstudio.mediaplayer.musica.Tocador;

/**
 * Aba Músicas (1.5): sub-abas Artistas, Álbuns, Playlists e Vídeos (1.5.1), cada uma numa grade de capas quadradas (a
 * foto do artista, redonda), a barra "Tocando agora" quando há música na fila e o aviso quando falta a
 * pasta de Músicas, a permissão ou as músicas. Na TV, a sub-aba em foco é a mostrada. OK ou toque abre;
 * segurar toca (o álbum, todas do artista ou a playlist). Desligada de fábrica: aparece no menu depois de
 * ligada em Configurações &gt; Navegação ou ao adicionar uma pasta de Músicas.
 */
public class MusicasFragment extends Fragment {

    private static final String ESTADO_SUBABA = "subaba";
    private static final int ARTISTAS = 0;
    private static final int ALBUNS = 1;
    private static final int PLAYLISTS = 2;
    /** 1.5.1: álbuns em vídeo e os vídeos soltos de cada artista. */
    private static final int VIDEOS = 3;
    private static final int[] NOMES = {R.string.st_musicas_artistas, R.string.st_musicas_albuns, R.string.st_musicas_playlists,
            R.string.st_musicas_videos};
    private static final int COLUNAS_TV = 6;

    private boolean tv;
    private final TextView[] subabas = new TextView[NOMES.length];
    private int ativa = ALBUNS;
    private TextView aviso;
    private Button permitir;
    private Button tudoAleatorio;
    private RecyclerView grade;
    private CapaMusicaAdapter capas;
    private GridLayoutManager gerenteTablet;
    private BarraTocando barra;
    private IndicadorAtualizacao indicador;
    private boolean pediuPermissao;
    private final Runnable aoMudar = this::mostrar;
    private ActivityResultLauncher<String> pedirPermissao;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pedirPermissao = registerForActivityResult(new ActivityResultContracts.RequestPermission(), deu -> {
            if (deu) RepositorioBiblioteca.atual(requireContext()).atualizarBiblioteca();
            mostrar();
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Context contexto = requireContext();
        tv = Aparelho.ehTv(contexto);
        if (savedInstanceState != null) ativa = savedInstanceState.getInt(ESTADO_SUBABA, ALBUNS);

        LinearLayout pagina = new LinearLayout(contexto);
        pagina.setOrientation(LinearLayout.VERTICAL);
        int lateral = getResources().getDimensionPixelSize(tv ? R.dimen.st_tv_margem_lateral : R.dimen.st_tablet_margem_h);
        int topo = tv ? Medidas.px(contexto, 44) : getResources().getDimensionPixelSize(R.dimen.st_tablet_margem_v);
        pagina.setPadding(lateral, topo, lateral, 0);
        if (!tv) BordasSistema.aplicar(pagina, false, false, false, true);

        // Título e, do outro lado, a situação da atualização da biblioteca
        LinearLayout linhaTitulo = new LinearLayout(contexto);
        linhaTitulo.setOrientation(LinearLayout.HORIZONTAL);
        linhaTitulo.setGravity(Gravity.CENTER_VERTICAL);
        TextView titulo = Textos.novo(contexto, getString(R.string.st_aba_musicas), R.font.st_inter_black,
                tv ? Medidas.TITULO_PAGINA_TV : Medidas.TITULO_PAGINA_TABLET, R.color.st_texto);
        titulo.setLetterSpacing(-0.02f);
        linhaTitulo.addView(titulo, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        indicador = new IndicadorAtualizacao(contexto, RepositorioBiblioteca.atual(contexto));
        linhaTitulo.addView(indicador.view);
        pagina.addView(linhaTitulo, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        pagina.addView(Textos.novo(contexto, getString(R.string.st_musicas_dica), R.font.st_inter_semibold, 20, R.color.st_texto_apagado),
                PecasMusica.comTopo(Medidas.px(contexto, 6)));

        barra = new BarraTocando(requireActivity());
        LinearLayout.LayoutParams lpBarra = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpBarra.topMargin = Medidas.px(contexto, 22);
        pagina.addView(barra.view, lpBarra);

        // Sub-abas Artistas, Álbuns e Playlists, como as pílulas de temporada da Série
        LinearLayout linha = new LinearLayout(contexto);
        linha.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < NOMES.length; i++) {
            final int indice = i;
            TextView subaba = Textos.novo(contexto, getString(NOMES[i]), R.font.st_inter_extrabold, 22, R.color.st_texto);
            subaba.setTextColor(ContextCompat.getColorStateList(contexto, R.color.st_cor_pilula));
            subaba.setBackgroundResource(R.drawable.st_pilula_temporada);
            subaba.setGravity(Gravity.CENTER);
            subaba.setPadding(Medidas.px(contexto, 30), 0, Medidas.px(contexto, 30), 0);
            subaba.setFocusable(true);
            subaba.setClickable(true);
            subaba.setOnClickListener(v -> trocar(indice));
            if (tv) subaba.setOnFocusChangeListener((v, temFoco) -> {
                if (temFoco) trocar(indice);
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, Medidas.px(contexto, 56));
            if (i > 0) lp.setMarginStart(Medidas.px(contexto, 12));
            linha.addView(subaba, lp);
            subabas[i] = subaba;
        }
        // Tocar tudo aleatório (1.6, D1a): a coleção inteira de áudio, sorteada
        tudoAleatorio = PecasMusica.botao(contexto, R.string.st_musicas_tocar_tudo_aleatorio, R.drawable.st_ic_aleatorio, false);
        tudoAleatorio.setOnClickListener(v -> {
            List<br.com.starktechstudio.mediaplayer.dados.ModeloMusicas.Musica> todas = RepositorioMusicas.de(contexto).todasAsMusicas();
            if (!todas.isEmpty()) Tocador.de(contexto).tocar(todas, (int) (Math.random() * todas.size()), true);
        });
        LinearLayout.LayoutParams lpTudo = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpTudo.setMarginStart(Medidas.px(contexto, 28));
        linha.setGravity(Gravity.CENTER_VERTICAL);
        linha.addView(tudoAleatorio, lpTudo);
        pagina.addView(linha, PecasMusica.comTopo(Medidas.px(contexto, 26)));

        aviso = Textos.novo(contexto, "", R.font.st_inter_semibold, 20, R.color.st_texto_secundario);
        aviso.setLineSpacing(0f, 1.2f);
        aviso.setBackgroundResource(R.drawable.st_fundo_cartao);
        int folga = Medidas.px(contexto, 22);
        aviso.setPadding(folga, Medidas.px(contexto, 16), folga, Medidas.px(contexto, 16));
        LinearLayout.LayoutParams lpAviso = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpAviso.topMargin = Medidas.px(contexto, 24);
        pagina.addView(aviso, lpAviso);
        permitir = PecasMusica.botao(contexto, R.string.st_musicas_permitir, R.drawable.st_ic_check, true);
        permitir.setOnClickListener(v -> pedirPermissao.launch(ArquivosDoAparelho.permissaoDeMusicas()));
        pagina.addView(permitir, PecasMusica.comTopo(Medidas.px(contexto, 16)));

        capas = new CapaMusicaAdapter(tv);
        grade = tv ? new VerticalGridView(contexto) : new RecyclerView(contexto);
        grade.setClipToPadding(false);
        grade.setVerticalScrollBarEnabled(false);
        int borda = Medidas.px(contexto, 12);
        grade.setPadding(borda, borda, borda, Medidas.px(contexto, 44));
        LinearLayout.LayoutParams lpGrade = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        lpGrade.topMargin = Medidas.px(contexto, 18);
        pagina.addView(grade, lpGrade);
        if (tv) configurarTv((VerticalGridView) grade, lateral);
        else configurarTablet();
        grade.setAdapter(capas);
        return pagina;
    }

    private void configurarTv(VerticalGridView gradeTv, int lateral) {
        int espaco = getResources().getDimensionPixelSize(R.dimen.st_tv_capa_espaco);
        int largura = getResources().getDisplayMetrics().widthPixels - 2 * lateral - gradeTv.getPaddingStart() - gradeTv.getPaddingEnd();
        int lado = (largura - espaco * (COLUNAS_TV - 1)) / COLUNAS_TV;
        capas.definirLado(lado);
        gradeTv.setNumColumns(COLUNAS_TV);
        gradeTv.setColumnWidth(lado);
        gradeTv.setHorizontalSpacing(espaco);
        gradeTv.setVerticalSpacing(getResources().getDimensionPixelSize(R.dimen.st_tv_lista_espaco_vertical));
    }

    private void configurarTablet() {
        int capa = getResources().getDimensionPixelSize(R.dimen.st_tablet_capa_largura);
        int espaco = getResources().getDimensionPixelSize(R.dimen.st_tablet_capa_espaco);
        gerenteTablet = new GridLayoutManager(requireContext(), 4);
        grade.setLayoutManager(gerenteTablet);
        grade.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect saida, @NonNull View item, @NonNull RecyclerView pai, @NonNull RecyclerView.State estado) {
                saida.set(0, 0, 0, espaco);
            }
        });
        grade.addOnLayoutChangeListener((v, esq, topo, dir, base, esqAntes, topoAntes, dirAntes, baseAntes) -> {
            if (dir - esq == dirAntes - esqAntes) return;
            int util = dir - esq - v.getPaddingStart() - v.getPaddingEnd();
            int colunas = LinhasDeCapas.colunas(util, capa, espaco);
            v.post(() -> {
                gerenteTablet.setSpanCount(colunas);
                capas.definirLado(util / colunas - espaco);
            });
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        RepositorioMusicas.de(requireContext()).ouvir(aoMudar);
        RepositorioBiblioteca.atual(requireContext()).ouvir(aoMudar);
        indicador.ligar();
        barra.ligar();
        mostrar();
        // Pasta de Músicas sem a permissão: pede uma vez ao abrir a aba
        RepositorioMusicas musicas = RepositorioMusicas.de(requireContext());
        if (!pediuPermissao && musicas.temPastaDeMusicas() && !ArquivosDoAparelho.podeLerMusicas(requireContext())) {
            pediuPermissao = true;
            pedirPermissao.launch(ArquivosDoAparelho.permissaoDeMusicas());
        }
    }

    @Override
    public void onPause() {
        RepositorioMusicas.de(requireContext()).deixarDeOuvir(aoMudar);
        RepositorioBiblioteca.atual(requireContext()).deixarDeOuvir(aoMudar);
        indicador.desligar();
        barra.desligar();
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ESTADO_SUBABA, ativa);
    }

    private void trocar(int indice) {
        if (indice == ativa && capas.getItemCount() > 0) return;
        ativa = indice;
        mostrar();
        grade.scrollToPosition(0);
    }

    private void mostrar() {
        if (getView() == null) return;
        Context contexto = requireContext();
        for (int i = 0; i < subabas.length; i++) subabas[i].setSelected(i == ativa);
        RepositorioMusicas repositorio = RepositorioMusicas.de(contexto);
        boolean temPasta = repositorio.temPastaDeMusicas();
        boolean podeLer = ArquivosDoAparelho.podeLerMusicas(contexto);
        List<CapaMusicaAdapter.Capa> lista = new ArrayList<>();
        String textoAviso = null;
        if (!temPasta && repositorio.vazio()) {
            textoAviso = getString(R.string.st_musicas_sem_pasta);
        } else if (!podeLer) {
            textoAviso = getString(R.string.st_musicas_sem_permissao);
        } else if (ativa == PLAYLISTS) {
            lista = playlists(repositorio);
            if (lista.size() == 1) textoAviso = getString(R.string.st_musicas_sem_playlists);
        } else if (repositorio.vazio()) {
            textoAviso = getString(R.string.st_musicas_vazia);
        } else if (ativa == VIDEOS) {
            lista = videos(repositorio);
            if (lista.isEmpty()) textoAviso = getString(R.string.st_musicas_sem_videos);
        } else if (ativa == ARTISTAS) {
            lista = artistas(repositorio);
        } else {
            lista = albuns(repositorio);
        }
        aviso.setText(textoAviso);
        aviso.setVisibility(textoAviso == null ? View.GONE : View.VISIBLE);
        permitir.setVisibility(temPasta && !podeLer ? View.VISIBLE : View.GONE);
        tudoAleatorio.setVisibility(podeLer && !repositorio.todasAsMusicas().isEmpty() ? View.VISIBLE : View.GONE);
        capas.atualizar(lista);
    }

    private List<CapaMusicaAdapter.Capa> artistas(RepositorioMusicas repositorio) {
        List<CapaMusicaAdapter.Capa> lista = new ArrayList<>();
        for (Artista a : repositorio.artistas()) {
            String linha = getResources().getQuantityString(R.plurals.st_n_albuns, a.albuns, a.albuns) + " · "
                    + getResources().getQuantityString(R.plurals.st_n_musicas, a.musicas, a.musicas);
            lista.add(new CapaMusicaAdapter.Capa(a.id, PecasMusica.artista(requireContext(), a.nome), linha, a.foto, a.cor,
                    R.drawable.st_ic_shows, true,
                    () -> TelasMusica.abrirArtista(requireActivity(), a.id),
                    () -> Tocador.de(requireContext()).tocar(repositorio.musicasDoArtista(a.id), 0, false)));
        }
        return lista;
    }

    private List<CapaMusicaAdapter.Capa> albuns(RepositorioMusicas repositorio) {
        List<CapaMusicaAdapter.Capa> lista = new ArrayList<>();
        for (Album a : repositorio.albuns()) {
            String linha = PecasMusica.artistaDoAlbum(requireContext(), a) + (a.ano == null ? "" : " · " + a.ano);
            lista.add(new CapaMusicaAdapter.Capa(a.id, PecasMusica.album(requireContext(), a.nome), linha, a.capa, a.cor,
                    R.drawable.st_ic_disco, false,
                    () -> TelasMusica.abrirAlbum(requireActivity(), a.id),
                    () -> Tocador.de(requireContext()).tocar(a.musicas, 0, false)));
        }
        return lista;
    }

    /**
     * Sub-aba Vídeos (1.5.1): uma capa por álbum em vídeo e uma "Vídeos de" por artista com vídeos soltos;
     * OK abre a lista, segurar assiste do primeiro (o player segue a sequência).
     */
    private List<CapaMusicaAdapter.Capa> videos(RepositorioMusicas repositorio) {
        List<CapaMusicaAdapter.Capa> lista = new ArrayList<>();
        for (Album a : repositorio.colecoesDeVideo()) {
            String artista = PecasMusica.artista(requireContext(), a.artista);
            String nome = a.soltos ? getString(R.string.st_musicas_videos_de, artista) : PecasMusica.album(requireContext(), a.nome);
            String linha = a.soltos ? getResources().getQuantityString(R.plurals.st_n_videos, a.musicas.size(), a.musicas.size())
                    : artista + (a.ano == null ? "" : " · " + a.ano);
            String primeiro = a.musicas.get(0).id;
            lista.add(new CapaMusicaAdapter.Capa(a.id, nome, linha, a.capa, a.cor, R.drawable.st_ic_assistir, false,
                    () -> {
                        if (a.soltos) TelasMusica.abrirVideos(requireActivity(), a.id);
                        else TelasMusica.abrirAlbum(requireActivity(), a.id);
                    },
                    () -> {
                        SequenciaClipes.soltar();
                        Navegacao.abrirReproducao(requireActivity(), primeiro, Navegacao.DE_ONDE_PAROU);
                    }));
        }
        return lista;
    }

    /** "Nova playlist" primeiro e as playlists em ordem alfabética. */
    private List<CapaMusicaAdapter.Capa> playlists(RepositorioMusicas repositorio) {
        List<CapaMusicaAdapter.Capa> lista = new ArrayList<>();
        lista.add(new CapaMusicaAdapter.Capa("playlist:nova", getString(R.string.st_musicas_nova_playlist), "",
                null, ContextCompat.getColor(requireContext(), R.color.st_cartao), R.drawable.st_ic_mais, false,
                () -> NomePlaylistActivity.abrirNova(requireActivity(), new ArrayList<>()), null));
        for (Playlist p : repositorio.playlists()) {
            lista.add(new CapaMusicaAdapter.Capa("playlist:" + p.id, p.nome,
                    getResources().getQuantityString(R.plurals.st_n_musicas, p.musicas.size(), p.musicas.size()),
                    p.capa, p.cor, R.drawable.st_ic_fila, false,
                    () -> TelasMusica.abrirPlaylist(requireActivity(), p.id),
                    p.musicas.isEmpty() ? null : () -> Tocador.de(requireContext()).tocar(p.musicas, 0, false)));
        }
        return lista;
    }
}
