// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.tv;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import com.archos.mediacenter.video.R;

import br.com.starktechstudio.mediaplayer.comum.Navegacao;
import br.com.starktechstudio.mediaplayer.comum.Navegacao.Aba;
import br.com.starktechstudio.mediaplayer.comum.Preferencias;

/**
 * Pílulas do topo das telas abertas sobre a principal na TV (Detalhe, Série): a seção do título
 * fica marcada; as pílulas recebem foco (← / → entre elas, → na última vai a Editar) e OK volta à
 * tela principal naquela aba. Com o foco nas pílulas, só a pílula em foco fica branca.
 * O ↑ do conteúdo leva à pílula da seção ({@link #idAtiva()}); o ↓ das pílulas, ao botão principal.
 */
final class TopoSecaoTv {

    private static final Aba[] ABAS = {Aba.INICIO, Aba.FILMES, Aba.SERIES, Aba.TVSHOWS, Aba.DESENHOS, Aba.SHOWS, Aba.STANDUP};
    private static final int[] PILULAS = {
            R.id.st_pilula_inicio, R.id.st_pilula_filmes, R.id.st_pilula_series, R.id.st_pilula_tvshows,
            R.id.st_pilula_desenhos, R.id.st_pilula_shows, R.id.st_pilula_standup
    };

    private final Activity atividade;
    private final TextView[] pilulas = new TextView[ABAS.length];
    private Aba ativa = Aba.INICIO;

    TopoSecaoTv(Activity atividade) {
        this.atividade = atividade;
        for (int i = 0; i < PILULAS.length; i++) {
            final Aba aba = ABAS[i];
            pilulas[i] = atividade.findViewById(PILULAS[i]);
            pilulas[i].setOnClickListener(v -> Navegacao.abrirAba(atividade, aba));
            pilulas[i].setOnFocusChangeListener((v, temFoco) -> atualizar());
        }
    }

    /**
     * Marca a seção; ↓ em qualquer pílula vai a {@code idDescida}. Só as abas ligadas em Configurações &gt;
     * Navegação aparecem: chamar ao voltar à tela, porque Configurações abre daqui.
     */
    void marcar(Aba aba, int idDescida) {
        ativa = aba;
        for (int i = 0; i < PILULAS.length; i++) {
            pilulas[i].setNextFocusDownId(idDescida);
            pilulas[i].setVisibility(Preferencias.abaLigada(atividade, ABAS[i]) ? View.VISIBLE : View.GONE);
        }
        atualizar();
    }

    /** Id da pílula da seção marcada (alvo do ↑ do conteúdo); com a aba dela desligada, Início. */
    int idAtiva() {
        for (int i = 0; i < ABAS.length; i++) {
            if (ABAS[i] == ativa && pilulas[i].getVisibility() == View.VISIBLE) return PILULAS[i];
        }
        return PILULAS[0];
    }

    /** Seção marcada em branco, Inter 800; com o foco nas pílulas, a branca é só a em foco. */
    private void atualizar() {
        boolean focoNasPilulas = false;
        for (TextView pilula : pilulas) focoNasPilulas |= pilula.isFocused();
        for (int i = 0; i < pilulas.length; i++) {
            boolean marcada = ABAS[i] == ativa;
            pilulas[i].setSelected(marcada && !focoNasPilulas);
            pilulas[i].setTypeface(ResourcesCompat.getFont(atividade,
                    marcada || pilulas[i].isFocused() ? R.font.st_inter_extrabold : R.font.st_inter_bold));
        }
    }
}
