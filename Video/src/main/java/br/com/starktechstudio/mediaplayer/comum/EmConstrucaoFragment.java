// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import com.archos.mediacenter.video.R;

/**
 * Conteúdo provisório de aba ou tela que entra numa fase seguinte da F1.
 * Na TV o painel recebe foco (↓ desce até ele; ↑ volta ao menu).
 */
public class EmConstrucaoFragment extends Fragment {

    private static final String ARG_NOME = "nome";

    public static EmConstrucaoFragment novo(@StringRes int nome) {
        EmConstrucaoFragment fragmento = new EmConstrucaoFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_NOME, nome);
        fragmento.setArguments(args);
        return fragmento;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View raiz = inflater.inflate(R.layout.st_em_construcao, container, false);
        View painel = raiz.findViewById(R.id.st_em_construcao_painel);
        TextView titulo = raiz.findViewById(R.id.st_em_construcao_titulo);
        TextView texto = raiz.findViewById(R.id.st_em_construcao_texto);

        Bundle args = getArguments();
        int nome = args == null ? 0 : args.getInt(ARG_NOME);
        if (nome != 0) titulo.setText(nome);

        if (Aparelho.ehTv(requireContext())) {
            // TV: px do HTML ÷ 2 (título de tela 48 px, texto 20 px, painel 36 px de respiro)
            raiz.setPadding(dp(40), dp(20), dp(40), dp(22));
            titulo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
            texto.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            painel.setPadding(dp(18), dp(18), dp(18), dp(18));
            FocoTv.aplicar(painel);
        } else {
            titulo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
            texto.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        }
        return raiz;
    }

    private int dp(int valor) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valor,
                getResources().getDisplayMetrics()));
    }
}
