// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.archos.mediacenter.video.R;

import br.com.starktechstudio.mediaplayer.dados.RepositorioBiblioteca;

/**
 * "Escolher" da coletânea no Editar (D5), no padrão do Menu da capa: as coletâneas da biblioteca,
 * a do filme com ✓. Devolve o nome ao Editar; nada muda na biblioteca antes do Salvar.
 */
public class EscolherColetaneaActivity extends AppCompatActivity {

    /** Coletânea que a tela do Editar está mostrando (recebe o ✓). */
    public static final String EXTRA_ATUAL = "coletanea_atual";
    /** Nome escolhido (resultado). */
    public static final String EXTRA_NOME = "coletanea_nome";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        boolean tv = Aparelho.ehTv(this);
        setTheme(tv ? R.style.Theme_StarkTech_Menu_Tv : R.style.Theme_StarkTech_Menu);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.st_escolher_coletanea);
        setFinishOnTouchOutside(true);

        Resources res = getResources();
        int padding = res.getDimensionPixelSize(tv ? R.dimen.st_tv_menu_padding : R.dimen.st_tablet_menu_padding);
        View caixa = findViewById(R.id.st_menu_caixa);
        ViewGroup.LayoutParams lpCaixa = caixa.getLayoutParams();
        lpCaixa.width = res.getDimensionPixelSize(tv ? R.dimen.st_tv_menu_largura : R.dimen.st_tablet_menu_largura);
        caixa.setLayoutParams(lpCaixa);
        caixa.setPadding(padding, padding, padding, padding);

        int recuo = padding * 2 / 3;
        TextView titulo = findViewById(R.id.st_menu_titulo);
        titulo.setText(R.string.st_coletanea_janela_titulo);
        titulo.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(tv ? R.dimen.st_tv_texto_menu_titulo : R.dimen.st_tablet_texto_menu_titulo));
        titulo.setPadding(recuo, padding / 3, recuo, 0);
        TextView dica = findViewById(R.id.st_menu_dica);
        dica.setText(R.string.st_coletanea_janela_dica);
        dica.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(tv ? R.dimen.st_tv_texto_menu_dica : R.dimen.st_tablet_texto_menu_dica));
        dica.setPadding(recuo, 0, recuo, recuo);

        String atual = getIntent().getStringExtra(EXTRA_ATUAL);
        LinearLayout opcoes = findViewById(R.id.st_menu_opcoes);
        View focar = null;
        for (String nome : RepositorioBiblioteca.atual(this).nomesDeColetanea()) {
            boolean marcada = nome.equals(atual);
            View opcao = OpcaoMenu.adicionar(opcoes, marcada ? R.drawable.st_ic_check : 0, nome, () -> {
                setResult(RESULT_OK, new Intent().putExtra(EXTRA_NOME, nome));
                finish();
            });
            if (focar == null || marcada) focar = opcao;
        }
        if (tv && focar != null) focar.requestFocus();
    }
}
