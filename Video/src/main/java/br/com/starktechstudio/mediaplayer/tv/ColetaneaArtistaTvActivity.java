// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.tv;

import android.view.View;

import com.archos.mediacenter.video.R;

import br.com.starktechstudio.mediaplayer.comum.ColetaneaArtistaActivity;
import br.com.starktechstudio.mediaplayer.comum.Navegacao;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Titulo;

/**
 * Coletânea e artista na TV: a tela comum com o topo da Série. A seção do título fica marcada
 * ({@link TopoSecaoTv}): Filmes na coletânea, Shows no artista. Em linha reta: pílulas, Editar (e Ver
 * mais) e a primeira linha de capas.
 */
public class ColetaneaArtistaTvActivity extends ColetaneaArtistaActivity {

    @Override
    protected void ligarTopo(Titulo titulo) {
        TopoSecaoTv topo = new TopoSecaoTv(this);
        topo.marcar(Navegacao.abaDoTipo(titulo.tipo), R.id.st_coletanea_editar);
        findViewById(R.id.st_botao_configuracoes).setOnClickListener(v -> Navegacao.abrirConfiguracoes(this));
        for (int acao : new int[]{R.id.st_coletanea_editar, R.id.st_coletanea_ver_mais}) {
            View botao = findViewById(acao);
            botao.setNextFocusUpId(topo.idAtiva());
            botao.setNextFocusDownId(R.id.st_coletanea_primeira);
        }
    }

    @Override
    protected int idAcimaDasCapas() {
        return R.id.st_coletanea_editar;
    }
}
