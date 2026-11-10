// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import com.archos.mediacenter.video.R;

import java.util.ArrayList;

import br.com.starktechstudio.mediaplayer.dados.Modelo.Tipo;
import br.com.starktechstudio.mediaplayer.dados.Modelo.TipoFaixa;
import br.com.starktechstudio.mediaplayer.dados.Modelo.TipoPasta;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Titulo;
import br.com.starktechstudio.mediaplayer.motor.Atualizador;
import br.com.starktechstudio.mediaplayer.tablet.DetalheTabletActivity;
import br.com.starktechstudio.mediaplayer.tablet.InicioTabletFragment;
import br.com.starktechstudio.mediaplayer.tablet.PrincipalTabletActivity;
import br.com.starktechstudio.mediaplayer.tablet.ReproducaoTabletActivity;
import br.com.starktechstudio.mediaplayer.tablet.SerieTabletActivity;
import br.com.starktechstudio.mediaplayer.tablet.SinopseActivity;
import br.com.starktechstudio.mediaplayer.tv.BuscaTvActivity;
import br.com.starktechstudio.mediaplayer.tv.ColetaneaArtistaTvActivity;
import br.com.starktechstudio.mediaplayer.tv.DetalheTvActivity;
import br.com.starktechstudio.mediaplayer.tv.InicioTvFragment;
import br.com.starktechstudio.mediaplayer.tv.PrincipalTvActivity;
import br.com.starktechstudio.mediaplayer.tv.ReproducaoTvActivity;
import br.com.starktechstudio.mediaplayer.tv.SerieTvActivity;

/**
 * Rotas das telas novas. Toda abertura de tela passa por aqui; tela que ainda não
 * existe abre {@link EmConstrucaoActivity}. As fases D a I trocam cada rota pela tela real.
 */
public final class Navegacao {

    /** Id do título (ou episódio) aberto pela tela. */
    public static final String EXTRA_ID = "br.com.starktechstudio.mediaplayer.extra.ID";
    /** Aba que a tela principal deve mostrar (nome do enum {@link Aba}). */
    public static final String EXTRA_ABA = "br.com.starktechstudio.mediaplayer.extra.ABA";
    /** Player: capítulo de começo (1…), {@link #DE_ONDE_PAROU} ou {@link #DO_INICIO}. */
    public static final String EXTRA_CAPITULO = "br.com.starktechstudio.mediaplayer.extra.CAPITULO";

    /** Player: começa de onde parou. */
    public static final int DE_ONDE_PAROU = 0;
    /** Player: começa do início do arquivo. */
    public static final int DO_INICIO = -1;

    /** Abas das telas principais. TV: pílulas do topo (sem Ajustes); tablet: barra lateral. */
    public enum Aba {
        INICIO(R.string.st_aba_inicio, R.drawable.st_ic_inicio, null),
        FILMES(R.string.st_aba_filmes, R.drawable.st_ic_filmes, Preferencias.Item.NAV_FILMES),
        SERIES(R.string.st_aba_series, R.drawable.st_ic_series, Preferencias.Item.NAV_SERIES),
        TVSHOWS(R.string.st_aba_tvshows, R.drawable.st_ic_tvshows, Preferencias.Item.NAV_TVSHOWS),
        DESENHOS(R.string.st_aba_desenhos, R.drawable.st_ic_desenhos, Preferencias.Item.NAV_DESENHOS),
        SHOWS(R.string.st_aba_shows, R.drawable.st_ic_shows, Preferencias.Item.NAV_SHOWS),
        STANDUP(R.string.st_aba_standup, R.drawable.st_ic_standup, Preferencias.Item.NAV_STANDUP),
        /** 1.1.1: só a navegação (Artistas, Álbuns, Playlists); o motor é da 1.5. */
        MUSICAS(R.string.st_aba_musicas, R.drawable.st_ic_musicas, Preferencias.Item.NAV_MUSICAS),
        AJUSTES(R.string.st_aba_ajustes, R.drawable.st_ic_ajustes, null);

        @StringRes
        public final int nome;
        /** Ícone da barra lateral do tablet. */
        @DrawableRes
        public final int icone;
        /** A escolha em Configurações &gt; Navegação; null = sempre no nav. */
        @Nullable
        public final Preferencias.Item itemDoNav;

        Aba(@StringRes int nome, @DrawableRes int icone, @Nullable Preferencias.Item itemDoNav) {
            this.nome = nome;
            this.icone = icone;
            this.itemDoNav = itemDoNav;
        }
    }

    /** Tela com o nav à vista enquanto Configurações está aberta (tablet): a escolha das abas vale na hora. */
    public interface TelaComAbas {
        void aoMudarAbas();
    }

    private Navegacao() {}

    /** A aba do tipo de pasta; Ignorar e "sem tipo": null. */
    @Nullable
    public static Aba abaDaPasta(TipoPasta tipo) {
        switch (tipo) {
            case FILMES: return Aba.FILMES;
            case SERIES: return Aba.SERIES;
            case TVSHOWS: return Aba.TVSHOWS;
            case DESENHOS: return Aba.DESENHOS;
            case SHOWS: return Aba.SHOWS;
            case STANDUP: return Aba.STANDUP;
            case MUSICAS: return Aba.MUSICAS;
            default: return null;
        }
    }

    /** Tela principal do aparelho (EntryActivity). */
    public static Class<? extends Activity> telaPrincipal(Activity origem) {
        return Aparelho.ehTv(origem) ? PrincipalTvActivity.class : PrincipalTabletActivity.class;
    }

    /**
     * Volta à tela principal na aba dada (OK numa pílula do topo do Detalhe ou da Série da TV).
     * A principal é singleTask: as telas abertas sobre ela fecham.
     */
    public static void abrirAba(Activity origem, Aba aba) {
        Intent intent = new Intent(origem, telaPrincipal(origem));
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(EXTRA_ABA, aba.name());
        origem.startActivity(intent);
    }

    /** Aba pedida no intent ({@link #EXTRA_ABA}); null se não houver. */
    public static Aba abaDoIntent(Intent intent) {
        String nome = intent == null ? null : intent.getStringExtra(EXTRA_ABA);
        if (nome == null) return null;
        try {
            return Aba.valueOf(nome);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Aba onde o tipo de título aparece (seção marcada no topo do Detalhe da TV). */
    public static Aba abaDoTipo(Tipo tipo) {
        if (tipo == Tipo.SERIE) return Aba.SERIES;
        if (tipo == Tipo.DESENHO) return Aba.DESENHOS;
        if (tipo == Tipo.SHOW) return Aba.SHOWS;
        if (tipo == Tipo.STANDUP) return Aba.STANDUP;
        return Aba.FILMES;
    }

    /** Conteúdo de cada aba: Início, Filmes, Séries, TV Shows, Desenhos e Stand-up (F3), Shows, Músicas (1.1.1, sem motor) e Ajustes (fase I). */
    public static Fragment conteudoDaAba(Context contexto, Aba aba) {
        switch (aba) {
            case INICIO:
                return Aparelho.ehTv(contexto) ? new InicioTvFragment() : new InicioTabletFragment();
            case FILMES:
                return ListaFragment.novo(Tipo.FILME);
            case SERIES:
                return ListaFragment.novo(Tipo.SERIE);
            case TVSHOWS:
                return ListaFragment.novoTvShows();
            case DESENHOS:
                return ListaFragment.novo(Tipo.DESENHO);
            case SHOWS:
                return ListaFragment.novo(Tipo.SHOW);
            case STANDUP:
                return ListaFragment.novo(Tipo.STANDUP);
            case MUSICAS:
                return new MusicasFragment();
            case AJUSTES:
                return new ConfiguracoesFragment();
            default:
                return EmConstrucaoFragment.novo(aba.nome);
        }
    }

    // ---- Telas (fases D a I) ----

    /** Busca da TV (lupa do topo; D11 c). */
    public static void abrirBusca(Activity origem) {
        origem.startActivity(new Intent(origem, BuscaTvActivity.class));
    }

    /** Configurações (D16): botão do topo da TV. No tablet, aba Ajustes. */
    public static void abrirConfiguracoes(Activity origem) {
        if (Aparelho.ehTv(origem)) origem.startActivity(new Intent(origem, ConfiguracoesActivity.class));
        else abrirAba(origem, Aba.AJUSTES);
    }

    /** Detalhe do filme ou do show; série abre a tela da série (fase E). */
    public static void abrirTitulo(Activity origem, String idTitulo, boolean ehSerie) {
        boolean tv = Aparelho.ehTv(origem);
        Class<? extends Activity> tela = ehSerie
                ? (tv ? SerieTvActivity.class : SerieTabletActivity.class)
                : (tv ? DetalheTvActivity.class : DetalheTabletActivity.class);
        origem.startActivity(new Intent(origem, tela).putExtra(EXTRA_ID, idTitulo));
    }

    /** Capa de uma lista: coletânea e artista abrem a lista deles (D5, D7); o resto, como {@link #abrirTitulo}. */
    public static void abrirCapa(Activity origem, Titulo titulo) {
        if (titulo.ehColetanea() || titulo.ehArtista()) {
            Class<? extends Activity> tela = Aparelho.ehTv(origem) ? ColetaneaArtistaTvActivity.class : ColetaneaArtistaActivity.class;
            origem.startActivity(new Intent(origem, tela).putExtra(EXTRA_ID, titulo.id));
        } else {
            abrirTitulo(origem, titulo.id, titulo.tipo == Tipo.SERIE);
        }
    }

    /**
     * Player (fase F) do título ou do episódio; {@code capitulo}: número do capítulo (1…),
     * {@link #DE_ONDE_PAROU} ou {@link #DO_INICIO}.
     */
    public static void abrirReproducao(Activity origem, String idTitulo, int capitulo) {
        Class<? extends Activity> tela = Aparelho.ehTv(origem) ? ReproducaoTvActivity.class : ReproducaoTabletActivity.class;
        origem.startActivity(new Intent(origem, tela).putExtra(EXTRA_ID, idTitulo).putExtra(EXTRA_CAPITULO, capitulo));
    }

    /** Menu da capa: segurar OK (TV) ou tocar e segurar (tablet). */
    public static void abrirMenuCapa(Activity origem, String idTitulo) {
        origem.startActivity(new Intent(origem, MenuCapaActivity.class).putExtra(EXTRA_ID, idTitulo));
    }

    /** Áudio e legendas do título, as duas listas (botão do Detalhe; D13). */
    public static void abrirFaixas(Activity origem, String idTitulo) {
        origem.startActivity(new Intent(origem, FaixasActivity.class).putExtra(EXTRA_ID, idTitulo));
    }

    /** Só Áudio ou só Legendas (botões do player; D13). */
    public static void abrirFaixas(Activity origem, String idTitulo, TipoFaixa tipo) {
        origem.startActivity(new Intent(origem, FaixasActivity.class).putExtra(EXTRA_ID, idTitulo)
                .putExtra(FaixasActivity.EXTRA_TIPO, tipo.name()));
    }

    /** Legenda rápida (1.5): atraso e tamanho, sobre o player (botão Ajustar). */
    public static void abrirAjusteLegenda(Activity origem, String idTitulo) {
        origem.startActivity(new Intent(origem, AjusteLegendaActivity.class).putExtra(EXTRA_ID, idTitulo));
    }

    /** Sinopse inteira ("Ver mais" do Detalhe do tablet). */
    public static void abrirSinopse(Activity origem, String titulo, String sinopse) {
        origem.startActivity(new Intent(origem, SinopseActivity.class)
                .putExtra(SinopseActivity.EXTRA_TITULO, titulo)
                .putExtra(SinopseActivity.EXTRA_SINOPSE, sinopse));
    }

    /** Editar (fase G): do Detalhe, da Série e do Menu da capa. */
    public static void abrirEditar(Activity origem, String idTitulo) {
        origem.startActivity(new Intent(origem, EditarActivity.class).putExtra(EXTRA_ID, idTitulo));
    }

    /** Pastas da biblioteca (1.1): em Configurações &gt; Biblioteca, fora do nav. */
    public static void abrirPastas(Activity origem) {
        origem.startActivity(new Intent(origem, PastasActivity.class));
    }

    /** Não identificados (Revisar, em Pastas; D15). */
    public static void abrirNaoIdentificados(Activity origem) {
        origem.startActivity(new Intent(origem, NaoIdentificadosActivity.class));
    }

    /** Adicionar pasta (D15): navegador de pastas próprio. */
    public static void abrirAdicionarPasta(Activity origem) {
        origem.startActivity(new Intent(origem, NavegadorPastasActivity.class));
    }

    /** Remover uma pasta da biblioteca (Pastas), com confirmação; {@code exibido}: "HD / Filmes". */
    public static void abrirRemoverPasta(Activity origem, String idPasta, String exibido) {
        origem.startActivity(new Intent(origem, RemoverPastaActivity.class)
                .putExtra(EXTRA_ID, idPasta)
                .putExtra(RemoverPastaActivity.EXTRA_CAMINHO_EXIBIDO, exibido));
    }

    /** Pasta de imagens do Editar (Configurações): o mesmo navegador de pastas, com "Usar esta pasta". */
    public static void abrirPastaImagens(Activity origem) {
        origem.startActivity(new Intent(origem, NavegadorPastasActivity.class)
                .putExtra(NavegadorPastasActivity.EXTRA_PASTA_IMAGENS, true));
    }

    /** Sobre (D17), em Configurações. */
    public static void abrirSobre(Activity origem) {
        origem.startActivity(new Intent(origem, SobreActivity.class));
    }

    public static void abrirInformacoesUso(Activity origem) {
        origem.startActivity(new Intent(origem, InformacoesUsoActivity.class));
    }

    /** Versão nova do site (Configurações, linha Versão): número, novidades e Baixar e instalar. */
    public static void abrirAtualizacao(Activity origem, Atualizador.Versao versao) {
        origem.startActivity(new Intent(origem, AtualizacaoActivity.class)
                .putExtra(AtualizacaoActivity.EXTRA_NOME, versao.nome)
                .putExtra(AtualizacaoActivity.EXTRA_CODIGO, versao.codigo)
                .putStringArrayListExtra(AtualizacaoActivity.EXTRA_NOVIDADES, new ArrayList<>(versao.novidades))
                .putExtra(AtualizacaoActivity.EXTRA_APK, versao.apk));
    }
}
