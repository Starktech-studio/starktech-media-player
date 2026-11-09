// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.archos.mediacenter.video.R;

/**
 * Pastas da biblioteca (1.1), aberta em Configurações &gt; Biblioteca, na TV e no tablet:
 * {@link PastasFragment} sobre o fundo animado. Voltar retorna às Configurações.
 */
public class PastasActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(Aparelho.tema(this));
        super.onCreate(savedInstanceState);
        FrameLayout raiz = new FrameLayout(this);
        raiz.setBackgroundResource(R.color.st_fundo);
        raiz.addView(new FundoAnimado(this), new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        FrameLayout conteudo = new FrameLayout(this);
        conteudo.setId(R.id.st_conteudo);
        BordasSistema.aplicar(conteudo, true, true, true, false);
        raiz.addView(conteudo, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(raiz);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.st_conteudo, new PastasFragment())
                    .commitNow();
        }
    }
}
