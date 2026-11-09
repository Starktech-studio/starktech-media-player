// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.content.res.Resources;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.archos.mediacenter.video.R;

import br.com.starktechstudio.mediaplayer.dados.RepositorioBiblioteca;

/**
 * Confirmação de "Remover" em Pastas (F3), no padrão do Menu da capa: "Remover a pasta?", o caminho
 * e o aviso de que os arquivos ficam onde estão; Remover tira a pasta e os vídeos dela da
 * biblioteca, Cancelar só fecha. A TV abre com o foco em Cancelar.
 */
public class RemoverPastaActivity extends AppCompatActivity {

    public static final String EXTRA_CAMINHO_EXIBIDO = "caminho_exibido";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        boolean tv = Aparelho.ehTv(this);
        setTheme(tv ? R.style.Theme_StarkTech_Menu_Tv : R.style.Theme_StarkTech_Menu);
        super.onCreate(savedInstanceState);
        String id = getIntent().getStringExtra(Navegacao.EXTRA_ID);
        if (id == null) {
            finish();
            return;
        }
        String exibido = getIntent().getStringExtra(EXTRA_CAMINHO_EXIBIDO);
        setContentView(R.layout.st_menu_capa);
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
        titulo.setText(R.string.st_remover_pasta_titulo);
        titulo.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(tv ? R.dimen.st_tv_texto_menu_titulo : R.dimen.st_tablet_texto_menu_titulo));
        titulo.setPadding(recuo, padding / 3, recuo, 0);
        TextView dica = findViewById(R.id.st_menu_dica);
        dica.setText(getString(R.string.st_remover_pasta_dica, exibido == null ? id : exibido));
        dica.setSingleLine(false);
        dica.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(tv ? R.dimen.st_tv_texto_menu_dica : R.dimen.st_tablet_texto_menu_dica));
        dica.setPadding(recuo, 0, recuo, recuo);

        LinearLayout opcoes = findViewById(R.id.st_menu_opcoes);
        OpcaoMenu.adicionar(opcoes, R.drawable.st_ic_fechar, getString(R.string.st_remover), () -> {
            RepositorioBiblioteca.atual(this).removerPasta(id);
            Toast.makeText(this, getString(R.string.st_pasta_removida, exibido == null ? id : exibido), Toast.LENGTH_SHORT).show();
            finish();
        });
        View cancelar = OpcaoMenu.adicionar(opcoes, R.drawable.st_ic_voltar, getString(R.string.st_cancelar), this::finish);
        // Remover não pode ser o OK sem querer: a TV começa em Cancelar
        if (tv) cancelar.requestFocus();
    }
}
