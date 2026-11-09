// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.tv;

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

    private TopoSecaoTv topo;
    private Navegacao.Aba secao;

    @Override
    protected void ligarTopo(Titulo titulo) {
        topo = new TopoSecaoTv(this);
        secao = Navegacao.abaDoTipo(titulo.tipo);
        findViewById(R.id.st_botao_configuracoes).setOnClickListener(v -> Navegacao.abrirConfiguracoes(this));
        for (int acao : new int[]{R.id.st_coletanea_editar, R.id.st_coletanea_ver_mais}) {
            findViewById(acao).setNextFocusDownId(R.id.st_coletanea_primeira);
        }
    }

    /** Volta de Configurações: as abas ligadas podem ter mudado. */
    @Override
    protected void onResume() {
        super.onResume();
        if (topo == null) return;
        topo.marcar(secao, R.id.st_coletanea_editar);
        for (int acao : new int[]{R.id.st_coletanea_editar, R.id.st_coletanea_ver_mais}) {
            findViewById(acao).setNextFocusUpId(topo.idAtiva());
        }
    }

    @Override
    protected int idAcimaDasCapas() {
        return R.id.st_coletanea_editar;
    }
}
