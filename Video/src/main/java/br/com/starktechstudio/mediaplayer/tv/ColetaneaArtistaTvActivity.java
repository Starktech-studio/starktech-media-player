// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.tv;

import com.archos.mediacenter.video.R;

import br.com.starktechstudio.mediaplayer.comum.ColetaneaArtistaActivity;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Titulo;

/**
 * Coletânea e artista na TV: a tela comum com o topo da Série, só com a marca e o relógio (2.0, D1:
 * nada com foco; a saída é o Voltar). Em linha reta: a fileira de ações (Ver mais, quando aparece, e
 * Editar só com o ícone, no fim) e a primeira linha de capas; o ↑ das capas vai ao Editar, que está
 * sempre lá, e o ↑ das ações fica parado.
 */
public class ColetaneaArtistaTvActivity extends ColetaneaArtistaActivity {

    @Override
    protected void ligarTopo(Titulo titulo) {
        for (int acao : new int[]{R.id.st_coletanea_editar, R.id.st_coletanea_ver_mais}) {
            findViewById(acao).setNextFocusDownId(R.id.st_coletanea_primeira);
            findViewById(acao).setNextFocusUpId(acao);
        }
    }

    @Override
    protected int idAcimaDasCapas() {
        return R.id.st_coletanea_editar;
    }
}
