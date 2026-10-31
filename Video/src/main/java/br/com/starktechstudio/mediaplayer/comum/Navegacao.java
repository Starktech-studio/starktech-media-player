// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import com.archos.mediacenter.video.R;

import br.com.starktechstudio.mediaplayer.tablet.PrincipalTabletActivity;
import br.com.starktechstudio.mediaplayer.tv.PrincipalTvActivity;

/**
 * Rotas das telas novas. Toda abertura de tela passa por aqui; tela que ainda não
 * existe abre {@link EmConstrucaoActivity}. As fases D a I trocam cada rota pela tela real.
 */
public final class Navegacao {

    /** Abas das telas principais. TV: pílulas do topo (sem Ajustes); tablet: barra lateral. */
    public enum Aba {
        INICIO(R.string.st_aba_inicio, R.drawable.st_ic_inicio),
        FILMES(R.string.st_aba_filmes, R.drawable.st_ic_filmes),
        SERIES(R.string.st_aba_series, R.drawable.st_ic_series),
        SHOWS(R.string.st_aba_shows, R.drawable.st_ic_shows),
        PASTAS(R.string.st_aba_pastas, R.drawable.st_ic_pastas),
        AJUSTES(R.string.st_aba_ajustes, R.drawable.st_ic_ajustes);

        @StringRes
        public final int nome;
        /** Ícone da barra lateral do tablet. */
        @DrawableRes
        public final int icone;

        Aba(@StringRes int nome, @DrawableRes int icone) {
            this.nome = nome;
            this.icone = icone;
        }
    }

    private Navegacao() {}

    /** Tela principal do aparelho (EntryActivity). */
    public static Class<? extends Activity> telaPrincipal(Activity origem) {
        return Aparelho.ehTv(origem) ? PrincipalTvActivity.class : PrincipalTabletActivity.class;
    }

    /** Conteúdo de cada aba. F1 fase C: todas em construção. */
    public static Fragment conteudoDaAba(Aba aba) {
        return EmConstrucaoFragment.novo(aba.nome);
    }

    // ---- Telas (fases D a I) ----

    /** Busca da TV (D11). */
    public static void abrirBusca(Activity origem) {
        emConstrucao(origem, R.string.st_tela_busca);
    }

    /** Configurações (D16): botão do topo da TV. No tablet, aba Ajustes. */
    public static void abrirConfiguracoes(Activity origem) {
        emConstrucao(origem, R.string.st_tela_configuracoes);
    }

    /** Detalhe do filme ou do show; série abre a tela da série. */
    public static void abrirTitulo(Activity origem, String idTitulo, boolean ehSerie) {
        emConstrucao(origem, ehSerie ? R.string.st_tela_serie : R.string.st_tela_detalhe);
    }

    /** Player; capítulo 0 = de onde parou. */
    public static void abrirReproducao(Activity origem, String idTitulo, int capitulo) {
        emConstrucao(origem, R.string.st_tela_reproducao);
    }

    public static void abrirEditar(Activity origem, String idTitulo) {
        emConstrucao(origem, R.string.st_tela_editar);
    }

    public static void abrirNaoIdentificados(Activity origem) {
        emConstrucao(origem, R.string.st_tela_nao_identificados);
    }

    public static void abrirSobre(Activity origem) {
        emConstrucao(origem, R.string.st_tela_sobre);
    }

    private static void emConstrucao(Activity origem, @StringRes int nomeTela) {
        Intent intent = new Intent(origem, EmConstrucaoActivity.class);
        intent.putExtra(EmConstrucaoActivity.EXTRA_NOME, nomeTela);
        origem.startActivity(intent);
    }
}
