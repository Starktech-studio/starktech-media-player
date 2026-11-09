// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.content.Context;
import android.view.Gravity;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.archos.mediacenter.video.R;

import br.com.starktechstudio.mediaplayer.dados.RepositorioBiblioteca;

/**
 * Indicador no alto das páginas (Filmes, Séries, Desenhos, Shows, Stand-up, Pastas), do lado oposto ao título:
 * durante a atualização da biblioteca, o percentual ("42 %") com o ícone de atualizar; em dia, o
 * ícone verde de ✓ e o resultado da última ("12 atualizados · 2 não identificados"). Liga no
 * onResume e desliga no onPause da página.
 */
public final class IndicadorAtualizacao {

    private final RepositorioBiblioteca repositorio;
    public final TextView view;
    private final int icone;
    private final Runnable mostrar = this::mostrar;

    public IndicadorAtualizacao(Context contexto, RepositorioBiblioteca repositorio) {
        this.repositorio = repositorio;
        view = Textos.novo(contexto, "", R.font.st_inter_extrabold, 22, R.color.st_texto_secundario);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setSingleLine(true);
        view.setCompoundDrawablePadding(Medidas.px(contexto, 10));
        icone = Medidas.px(contexto, 30);
    }

    public void ligar() {
        repositorio.ouvirAndamento(mostrar);
        repositorio.ouvir(mostrar);
        mostrar();
    }

    public void desligar() {
        repositorio.deixarDeOuvirAndamento(mostrar);
        repositorio.deixarDeOuvir(mostrar);
    }

    private void mostrar() {
        Context contexto = view.getContext();
        int progresso = repositorio.progresso();
        if (progresso >= 0) {
            view.setText(contexto.getString(R.string.st_atualizacao_percentual, progresso));
            Icones.noTexto(view, R.drawable.st_ic_atualizar, icone, ContextCompat.getColor(contexto, R.color.st_texto_apagado));
            view.setContentDescription(contexto.getString(R.string.st_atualizacao_andando, progresso));
        } else {
            String resultado = repositorio.resultadoDaAtualizacao();
            view.setText(resultado == null ? "" : resultado);
            Icones.noTexto(view, R.drawable.st_ic_check, icone, ContextCompat.getColor(contexto, R.color.st_assistido));
            view.setContentDescription(contexto.getString(R.string.st_atualizacao_em_dia));
        }
    }
}
