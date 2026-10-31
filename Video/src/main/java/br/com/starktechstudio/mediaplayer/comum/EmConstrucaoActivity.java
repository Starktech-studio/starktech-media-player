// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.archos.mediacenter.video.R;

/** Tela provisória para as rotas de {@link Navegacao} que ainda não têm tela. Voltar fecha. */
public class EmConstrucaoActivity extends AppCompatActivity {

    public static final String EXTRA_NOME = "br.com.starktechstudio.mediaplayer.extra.NOME";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(Aparelho.tema(this));
        super.onCreate(savedInstanceState);
        FrameLayout conteudo = new FrameLayout(this);
        conteudo.setId(R.id.st_conteudo);
        conteudo.setBackgroundResource(R.color.st_fundo);
        BordasSistema.aplicar(conteudo, true, true, true, true);
        setContentView(conteudo);

        if (savedInstanceState == null) {
            int nome = getIntent().getIntExtra(EXTRA_NOME, 0);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.st_conteudo, EmConstrucaoFragment.novo(nome))
                    .commitNow();
        }
        if (Aparelho.ehTv(this)) conteudo.post(() -> conteudo.requestFocus(android.view.View.FOCUS_DOWN));
    }
}
