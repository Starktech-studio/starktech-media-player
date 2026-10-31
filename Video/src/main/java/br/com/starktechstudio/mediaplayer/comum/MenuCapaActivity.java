// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.content.res.Resources;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import com.archos.mediacenter.video.R;

import br.com.starktechstudio.mediaplayer.dados.Modelo.Episodio;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Tipo;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Titulo;
import br.com.starktechstudio.mediaplayer.dados.RepositorioBiblioteca;

/**
 * Menu da capa (tv-menu-capa.dc.html): segurar OK na capa (TV) ou tocar e segurar (tablet, D12).
 * Assistir, Marcar como assistido, Adicionar aos favoritos e Editar; os textos alternam (D22:
 * "Marcar como não assistido", "Remover dos favoritos"). Trocar capa e Trocar fundo do desenho
 * saíram (Gustavo, 31/10/2026): ficam só em Editar.
 * É uma janela flutuante sobre a tela de origem: ao fechar, a tela de origem volta ao onResume
 * e relê o estado simulado (selo Assistido, fileira de Favoritos) sem precisar de aviso.
 */
public class MenuCapaActivity extends AppCompatActivity {

    private RepositorioBiblioteca repositorio;
    private Titulo titulo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        boolean tv = Aparelho.ehTv(this);
        setTheme(tv ? R.style.Theme_StarkTech_Menu_Tv : R.style.Theme_StarkTech_Menu);
        super.onCreate(savedInstanceState);
        repositorio = RepositorioBiblioteca.atual(this);
        titulo = repositorio.titulo(getIntent().getStringExtra(Navegacao.EXTRA_ID));
        if (titulo == null) {
            finish();
            return;
        }
        setContentView(R.layout.st_menu_capa);
        setFinishOnTouchOutside(true);

        Resources res = getResources();
        int padding = res.getDimensionPixelSize(tv ? R.dimen.st_tv_menu_padding : R.dimen.st_tablet_menu_padding);
        View caixa = findViewById(R.id.st_menu_caixa);
        ViewGroup.LayoutParams lpCaixa = caixa.getLayoutParams();
        lpCaixa.width = res.getDimensionPixelSize(tv ? R.dimen.st_tv_menu_largura : R.dimen.st_tablet_menu_largura);
        caixa.setLayoutParams(lpCaixa);
        caixa.setPadding(padding, padding, padding, padding);

        // Cabeçalho: padding 6 px 12 px 12 px do desenho (metade na TV)
        int recuo = padding * 2 / 3;
        TextView nome = findViewById(R.id.st_menu_titulo);
        nome.setText(titulo.titulo);
        nome.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(tv ? R.dimen.st_tv_texto_menu_titulo : R.dimen.st_tablet_texto_menu_titulo));
        nome.setPadding(recuo, padding / 3, recuo, 0);
        TextView dica = findViewById(R.id.st_menu_dica);
        dica.setText(tv ? R.string.st_menu_dica_tv : R.string.st_menu_dica_tablet);
        dica.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(tv ? R.dimen.st_tv_texto_menu_dica : R.dimen.st_tablet_texto_menu_dica));
        dica.setPadding(recuo, 0, recuo, recuo);

        LinearLayout opcoes = findViewById(R.id.st_menu_opcoes);
        View primeira = opcao(opcoes, R.drawable.st_ic_assistir, R.string.st_assistir, this::assistir);
        opcao(opcoes, R.drawable.st_ic_check,
                titulo.assistido ? R.string.st_marcar_nao_assistido : R.string.st_marcar_assistido,
                () -> repositorio.definirAssistido(titulo.id, !titulo.assistido));
        opcao(opcoes, R.drawable.st_ic_favorito,
                titulo.favorito ? R.string.st_remover_favoritos : R.string.st_adicionar_favoritos,
                () -> repositorio.definirFavorito(titulo.id, !titulo.favorito));
        opcao(opcoes, R.drawable.st_ic_editar, R.string.st_editar, () -> Navegacao.abrirEditar(this, titulo.id));
        if (tv) primeira.requestFocus();
    }

    /** Assistir: filme ou show de onde parou; série no próximo episódio. */
    private void assistir() {
        if (titulo.tipo == Tipo.SERIE) {
            Episodio proximo = PecasDetalhe.proximoEpisodio(repositorio.temporadas(titulo.id));
            if (proximo != null) Navegacao.abrirReproducao(this, proximo.id, Navegacao.DE_ONDE_PAROU);
        } else {
            Navegacao.abrirReproducao(this, titulo.id, Navegacao.DE_ONDE_PAROU);
        }
    }

    /** Opção do menu ({@link OpcaoMenu}); faz a ação e fecha o menu. */
    private View opcao(LinearLayout opcoes, @DrawableRes int icone, @StringRes int texto, Runnable acao) {
        return OpcaoMenu.adicionar(opcoes, icone, getString(texto), () -> {
            acao.run();
            finish();
        });
    }
}
