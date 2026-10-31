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

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.archos.mediacenter.video.R;

import br.com.starktechstudio.mediaplayer.dados.Modelo.Origem;
import br.com.starktechstudio.mediaplayer.dados.RepositorioBiblioteca;

/**
 * Adicionar pasta (D15): navegador de pastas próprio, por controle e por toque. Primeiro os
 * armazenamentos (HD externo, armazenamento interno), depois as pastas de cada um; OK ou toque
 * numa pasta a adiciona e fecha. F1: pastas de exemplo e a pasta nova só na memória do app;
 * F2: os armazenamentos e as pastas de verdade (ExtStorageManager do Nova).
 */
public class NavegadorPastasActivity extends AppCompatActivity {

    /** Pastas de exemplo de cada armazenamento (F1). */
    private static final String[] PASTAS_HD = {"Filmes", "Séries", "Shows", "Downloads", "Documentários", "Filmes 2026"};
    private static final String[] PASTAS_INTERNO = {"Movies", "Download", "DCIM"};

    private boolean tv;
    private LinearLayout opcoes;
    private TextView titulo;
    private TextView caminho;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        tv = Aparelho.ehTv(this);
        setTheme(tv ? R.style.Theme_StarkTech_Menu_Tv : R.style.Theme_StarkTech_Menu);
        super.onCreate(savedInstanceState);
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
        titulo = findViewById(R.id.st_menu_titulo);
        titulo.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(tv ? R.dimen.st_tv_texto_menu_titulo : R.dimen.st_tablet_texto_menu_titulo));
        titulo.setPadding(recuo, padding / 3, recuo, 0);
        caminho = findViewById(R.id.st_menu_dica);
        caminho.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(tv ? R.dimen.st_tv_texto_menu_dica : R.dimen.st_tablet_texto_menu_dica));
        caminho.setPadding(recuo, 0, recuo, recuo);
        opcoes = findViewById(R.id.st_menu_opcoes);
        mostrarArmazenamentos();
    }

    private void mostrarArmazenamentos() {
        titulo.setText(R.string.st_escolher_pasta);
        caminho.setText(R.string.st_adicionar_pasta);
        opcoes.removeAllViews();
        boolean hd = RepositorioBiblioteca.atual(this).hdConectado();
        View primeira = null;
        if (hd) {
            primeira = opcao(R.drawable.st_ic_hd, getString(R.string.st_hd_externo_usb),
                    () -> mostrarPastas(Origem.HD_EXTERNO, getString(R.string.st_hd_externo_usb), "HD", PASTAS_HD));
        }
        View interno = opcao(R.drawable.st_ic_pastas, getString(R.string.st_armazenamento_interno),
                () -> mostrarPastas(Origem.INTERNO, getString(R.string.st_armazenamento_interno), "Interno", PASTAS_INTERNO));
        if (primeira == null) primeira = interno;
        if (tv) primeira.requestFocus();
    }

    private void mostrarPastas(Origem origem, String nomeOrigem, String raiz, String[] pastas) {
        titulo.setText(nomeOrigem);
        caminho.setText(raiz);
        opcoes.removeAllViews();
        View subir = opcao(R.drawable.st_ic_voltar, getString(R.string.st_subir), this::mostrarArmazenamentos);
        for (String nome : pastas) {
            String completo = raiz + " / " + nome;
            opcao(R.drawable.st_ic_pastas, nome, () -> {
                RepositorioBiblioteca.atual(this).adicionarPasta(origem, completo);
                Toast.makeText(this, getString(R.string.st_pasta_adicionada, completo), Toast.LENGTH_SHORT).show();
                finish();
            });
        }
        if (tv && opcoes.getChildCount() > 1) opcoes.getChildAt(1).requestFocus();
        else if (tv) subir.requestFocus();
    }

    private View opcao(@DrawableRes int icone, String texto, Runnable acao) {
        return OpcaoMenu.adicionar(opcoes, icone, texto, acao);
    }
}
