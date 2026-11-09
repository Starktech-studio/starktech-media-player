// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.StringRes;

import com.archos.mediacenter.video.R;

import br.com.starktechstudio.mediaplayer.dados.RepositorioBiblioteca;

/**
 * Aviso no lugar dos títulos quando não há nada para mostrar (F3): sem permissão para ler os
 * vídeos, sem pastas, procurando os vídeos ou nenhum vídeo daquele tipo.
 */
public final class AvisoVazio {

    private AvisoVazio() {}

    /** {@code semVideos}: o texto da tela quando as pastas existem mas não há títulos. */
    public static String texto(Context contexto, RepositorioBiblioteca repositorio, @StringRes int semVideos) {
        if (!repositorio.podeLerVideos()) return contexto.getString(R.string.st_vazio_sem_permissao);
        if (repositorio.pastas().isEmpty()) return contexto.getString(R.string.st_vazio_sem_pastas);
        if (repositorio.atualizando()) return contexto.getString(R.string.st_vazio_procurando);
        return contexto.getString(semVideos);
    }

    /** Texto do aviso, centrado, escondido até ser preciso. */
    public static TextView criar(Context contexto) {
        TextView aviso = Textos.novo(contexto, "", R.font.st_inter_semibold, 24, R.color.st_texto_secundario);
        aviso.setGravity(Gravity.CENTER);
        aviso.setLineSpacing(0f, 1.25f);
        int lateral = Medidas.px(contexto, 120);
        aviso.setPadding(lateral, Medidas.px(contexto, 60), lateral, Medidas.px(contexto, 60));
        aviso.setVisibility(View.GONE);
        return aviso;
    }
}
