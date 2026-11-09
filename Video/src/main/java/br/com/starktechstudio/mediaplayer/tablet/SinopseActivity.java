// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.tablet;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.archos.mediacenter.video.R;

import br.com.starktechstudio.mediaplayer.comum.Aparelho;

/**
 * "Ver mais" do texto, no Detalhe do tablet e na página do artista: o texto inteiro numa janela
 * flutuante, para os botões não descerem na página. Fecha em Fechar ou tocando fora. Na TV, as setas
 * rolam o texto e Voltar fecha.
 */
public class SinopseActivity extends AppCompatActivity {

    public static final String EXTRA_TITULO = "titulo";
    public static final String EXTRA_SINOPSE = "sinopse";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        boolean tv = Aparelho.ehTv(this);
        if (tv) setTheme(R.style.Theme_StarkTech_Menu_Tv);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.st_tablet_sinopse);
        setFinishOnTouchOutside(true);

        View caixa = findViewById(R.id.st_sinopse_caixa);
        ViewGroup.LayoutParams lp = caixa.getLayoutParams();
        lp.width = Math.min(getResources().getDimensionPixelSize(R.dimen.st_tablet_detalhe_sinopse_largura),
                getResources().getDisplayMetrics().widthPixels * 9 / 10);
        caixa.setLayoutParams(lp);

        ((TextView) findViewById(R.id.st_sinopse_titulo)).setText(getIntent().getStringExtra(EXTRA_TITULO));
        ((TextView) findViewById(R.id.st_sinopse_texto)).setText(getIntent().getStringExtra(EXTRA_SINOPSE));
        findViewById(R.id.st_sinopse_fechar).setOnClickListener(v -> finish());
        if (tv) {
            View rolagem = findViewById(R.id.st_sinopse_rolagem);
            rolagem.setFocusable(true);
            rolagem.requestFocus();
        }
    }
}
