// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.archos.mediacenter.video.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import br.com.starktechstudio.mediaplayer.dados.ModeloMusicas.Musica;
import br.com.starktechstudio.mediaplayer.dados.ModeloMusicas.Playlist;
import br.com.starktechstudio.mediaplayer.dados.RepositorioMusicas;
import br.com.starktechstudio.mediaplayer.musica.Tocador;

/**
 * Painéis de Músicas (1.5), no padrão do Menu da capa ({@link OpcaoMenu}): o menu da música (tocar a
 * seguir, pôr na fila, adicionar à playlist, ir para o álbum ou o artista; numa playlist, subir, descer e
 * tirar), a escolha da playlist (com Nova playlist), a confirmação de apagar uma playlist e a fila do Tocando
 * agora. OK ou toque escolhe e fecha.
 */
public class MenuMusicaActivity extends AppCompatActivity {

    private static final String EXTRA_MODO = "br.com.starktechstudio.mediaplayer.extra.MODO_MUSICA";
    private static final String EXTRA_MUSICAS = "br.com.starktechstudio.mediaplayer.extra.MUSICAS";
    private static final String EXTRA_PLAYLIST = "br.com.starktechstudio.mediaplayer.extra.PLAYLIST";
    /** Na playlist: a posição na lista inteira e o índice entre as que aparecem. */
    private static final String EXTRA_POSICAO = "br.com.starktechstudio.mediaplayer.extra.POSICAO";
    private static final String EXTRA_INDICE = "br.com.starktechstudio.mediaplayer.extra.INDICE";

    private static final int MODO_MUSICA = 0;
    private static final int MODO_PLAYLIST = 1;
    private static final int MODO_APAGAR = 2;
    private static final int MODO_FILA = 3;

    /** Menu da música; {@code idPlaylist} -1 = fora de uma playlist. */
    public static void abrirMusica(Activity origem, String idMusica, long idPlaylist, int indice) {
        Intent intent = new Intent(origem, MenuMusicaActivity.class)
                .putExtra(EXTRA_MODO, MODO_MUSICA)
                .putStringArrayListExtra(EXTRA_MUSICAS, new ArrayList<>(Collections.singletonList(idMusica)))
                .putExtra(EXTRA_PLAYLIST, idPlaylist)
                .putExtra(EXTRA_INDICE, indice);
        origem.startActivity(intent);
    }

    /** Escolher a playlist que recebe as músicas. */
    public static void abrirEscolherPlaylist(Activity origem, List<String> musicas) {
        origem.startActivity(new Intent(origem, MenuMusicaActivity.class)
                .putExtra(EXTRA_MODO, MODO_PLAYLIST)
                .putStringArrayListExtra(EXTRA_MUSICAS, new ArrayList<>(musicas)));
    }

    public static void abrirApagar(Activity origem, long idPlaylist) {
        origem.startActivity(new Intent(origem, MenuMusicaActivity.class)
                .putExtra(EXTRA_MODO, MODO_APAGAR)
                .putExtra(EXTRA_PLAYLIST, idPlaylist));
    }

    public static void abrirFila(Activity origem) {
        origem.startActivity(new Intent(origem, MenuMusicaActivity.class).putExtra(EXTRA_MODO, MODO_FILA));
    }

    private boolean tv;
    private RepositorioMusicas repositorio;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        tv = Aparelho.ehTv(this);
        setTheme(tv ? R.style.Theme_StarkTech_Menu_Tv : R.style.Theme_StarkTech_Menu);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.st_menu_capa);
        setFinishOnTouchOutside(true);
        repositorio = RepositorioMusicas.de(this);

        Resources res = getResources();
        int padding = res.getDimensionPixelSize(tv ? R.dimen.st_tv_menu_padding : R.dimen.st_tablet_menu_padding);
        LinearLayout caixa = findViewById(R.id.st_menu_caixa);
        ViewGroup.LayoutParams lpCaixa = caixa.getLayoutParams();
        lpCaixa.width = res.getDimensionPixelSize(tv ? R.dimen.st_tv_menu_largura : R.dimen.st_tablet_menu_largura);
        caixa.setLayoutParams(lpCaixa);
        caixa.setPadding(padding, padding, padding, padding);
        int recuo = padding * 2 / 3;

        TextView titulo = findViewById(R.id.st_menu_titulo);
        titulo.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(tv ? R.dimen.st_tv_texto_menu_titulo : R.dimen.st_tablet_texto_menu_titulo));
        titulo.setPadding(recuo, padding / 3, recuo, recuo / 2);
        TextView dica = findViewById(R.id.st_menu_dica);
        dica.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(tv ? R.dimen.st_tv_texto_menu_dica : R.dimen.st_tablet_texto_menu_dica));
        dica.setPadding(recuo, 0, recuo, recuo);

        // Opções numa rolagem: a fila e as playlists podem passar da altura da tela
        LinearLayout opcoes = findViewById(R.id.st_menu_opcoes);
        caixa.removeView(opcoes);
        ScrollView rolagem = new ScrollView(this);
        rolagem.setFocusable(false);
        rolagem.setVerticalScrollBarEnabled(false);
        rolagem.setOverScrollMode(View.OVER_SCROLL_NEVER);
        rolagem.addView(opcoes, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        caixa.addView(rolagem, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView primeira;
        switch (getIntent().getIntExtra(EXTRA_MODO, MODO_MUSICA)) {
            case MODO_PLAYLIST:
                primeira = escolherPlaylist(titulo, dica, opcoes);
                break;
            case MODO_APAGAR:
                primeira = apagar(titulo, dica, opcoes);
                break;
            case MODO_FILA:
                primeira = fila(titulo, dica, opcoes);
                break;
            default:
                primeira = musica(titulo, dica, opcoes);
                break;
        }
        if (primeira == null) {
            finish();
            return;
        }
        if (tv) primeira.requestFocus();
    }

    private List<String> ids() {
        List<String> ids = getIntent().getStringArrayListExtra(EXTRA_MUSICAS);
        return ids == null ? Collections.emptyList() : ids;
    }

    @Nullable
    private TextView musica(TextView titulo, TextView dica, LinearLayout opcoes) {
        List<String> ids = ids();
        Musica m = ids.isEmpty() ? null : repositorio.musica(ids.get(0));
        if (m == null) return null;
        titulo.setText(m.titulo);
        dica.setText(PecasMusica.artistaEAlbum(this, m));
        Tocador tocador = Tocador.de(this);
        List<Musica> uma = Collections.singletonList(m);
        TextView primeira = OpcaoMenu.adicionar(opcoes, R.drawable.st_ic_tocar_seguir, getString(R.string.st_musicas_tocar_a_seguir), () -> {
            tocador.tocarASeguir(uma);
            avisar(getString(R.string.st_musicas_a_seguir_feito));
        });
        OpcaoMenu.adicionar(opcoes, R.drawable.st_ic_por_no_fim, getString(R.string.st_musicas_por_na_fila), () -> {
            tocador.porNoFim(uma);
            avisar(getString(R.string.st_musicas_na_fila_feito));
        });
        OpcaoMenu.adicionar(opcoes, R.drawable.st_ic_playlist_nova, getString(R.string.st_musicas_adicionar_playlist), () -> {
            abrirEscolherPlaylist(this, ids);
            finish();
        });

        long idPlaylist = getIntent().getLongExtra(EXTRA_PLAYLIST, -1);
        Playlist playlist = idPlaylist < 0 ? null : repositorio.playlist(idPlaylist);
        int indice = getIntent().getIntExtra(EXTRA_INDICE, -1);
        if (playlist != null && indice >= 0 && indice < playlist.posicoes.size()) {
            int posicao = playlist.posicoes.get(indice);
            if (indice > 0) {
                int acima = playlist.posicoes.get(indice - 1);
                OpcaoMenu.adicionar(opcoes, R.drawable.st_ic_subir, getString(R.string.st_musicas_subir), () -> {
                    repositorio.moverNaPlaylist(playlist.id, posicao, acima);
                    finish();
                });
            }
            if (indice < playlist.posicoes.size() - 1) {
                int abaixo = playlist.posicoes.get(indice + 1);
                OpcaoMenu.adicionar(opcoes, R.drawable.st_ic_descer, getString(R.string.st_musicas_descer), () -> {
                    repositorio.moverNaPlaylist(playlist.id, posicao, abaixo);
                    finish();
                });
            }
            OpcaoMenu.adicionar(opcoes, R.drawable.st_ic_lixeira, getString(R.string.st_musicas_tirar_playlist), () -> {
                repositorio.tirarDaPlaylist(playlist.id, posicao);
                finish();
            });
        }

        if (repositorio.album(m.idAlbum) != null) {
            OpcaoMenu.adicionar(opcoes, R.drawable.st_ic_disco, getString(R.string.st_musicas_ir_album), () -> {
                TelasMusica.abrirAlbum(this, m.idAlbum);
                finish();
            });
        }
        String idArtista = m.artista == null ? null : br.com.starktechstudio.mediaplayer.motor.Acervo.chaveArtista(m.artista);
        if (idArtista != null && repositorio.artista(idArtista) != null) {
            OpcaoMenu.adicionar(opcoes, R.drawable.st_ic_shows, getString(R.string.st_musicas_ir_artista), () -> {
                TelasMusica.abrirArtista(this, idArtista);
                finish();
            });
        }
        return primeira;
    }

    private TextView escolherPlaylist(TextView titulo, TextView dica, LinearLayout opcoes) {
        List<String> ids = ids();
        titulo.setText(R.string.st_musicas_adicionar_playlist);
        dica.setVisibility(View.GONE);
        TextView primeira = OpcaoMenu.adicionar(opcoes, R.drawable.st_ic_mais, getString(R.string.st_musicas_nova_playlist), () -> {
            NomePlaylistActivity.abrirNova(this, ids);
            finish();
        });
        for (Playlist p : repositorio.playlists()) {
            OpcaoMenu.adicionar(opcoes, R.drawable.st_ic_fila, p.nome, () -> {
                repositorio.adicionarNaPlaylist(p.id, ids);
                avisar(getString(R.string.st_musicas_na_playlist_feito, p.nome));
            });
        }
        return primeira;
    }

    @Nullable
    private TextView apagar(TextView titulo, TextView dica, LinearLayout opcoes) {
        Playlist p = repositorio.playlist(getIntent().getLongExtra(EXTRA_PLAYLIST, -1));
        if (p == null) return null;
        titulo.setText(getString(R.string.st_musicas_apagar_playlist, p.nome));
        dica.setText(R.string.st_musicas_apagar_aviso);
        TextView cancelar = OpcaoMenu.adicionar(opcoes, R.drawable.st_ic_fechar, getString(R.string.st_cancelar), this::finish);
        OpcaoMenu.adicionar(opcoes, R.drawable.st_ic_lixeira, getString(R.string.st_musicas_apagar), () -> {
            repositorio.apagarPlaylist(p.id);
            setResult(RESULT_OK);
            finish();
        });
        return cancelar;
    }

    @Nullable
    private TextView fila(TextView titulo, TextView dica, LinearLayout opcoes) {
        Tocador tocador = Tocador.de(this);
        List<Musica> fila = tocador.fila();
        if (fila.isEmpty()) return null;
        titulo.setText(R.string.st_musicas_fila);
        dica.setText(getResources().getQuantityString(R.plurals.st_n_musicas, fila.size(), fila.size()));
        int atual = tocador.posicaoNaFila();
        TextView foco = null;
        for (int i = 0; i < fila.size(); i++) {
            final int indice = i;
            Musica m = fila.get(i);
            TextView item = OpcaoMenu.adicionar(opcoes, i == atual ? R.drawable.st_ic_check : 0,
                    m.titulo + " · " + PecasMusica.artista(this, m.artista), () -> {
                        tocador.tocarDaFila(indice);
                        finish();
                    });
            item.setSelected(i == atual);
            if (foco == null || i == atual) foco = item;
        }
        return foco;
    }

    private void avisar(String texto) {
        Toast.makeText(this, texto, Toast.LENGTH_SHORT).show();
        finish();
    }
}
