// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.archos.mediacenter.video.R;

import java.io.File;
import java.util.List;

import br.com.starktechstudio.mediaplayer.motor.ArquivosDoAparelho;

/**
 * Escolher imagem na TV (D3, "Do HD" do Editar): a TV costuma não ter o seletor do sistema, então
 * o app mostra os armazenamentos de verdade (interno, HD / pen drive), as pastas e só as imagens
 * (JPG, PNG, WebP). OK numa imagem devolve o caminho em {@link #EXTRA_CAMINHO}. Pede a permissão
 * de ler imagens se ainda não tiver.
 */
public class NavegadorImagensActivity extends AppCompatActivity {

    public static final String EXTRA_CAMINHO = "caminho";

    private static final int PEDIDO_PERMISSAO = 1;

    private boolean tv;
    private LinearLayout opcoes;
    private TextView titulo;
    private TextView caminho;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        tv = Aparelho.ehTv(this);
        setTheme(tv ? R.style.Theme_StarkTech_Menu_Tv : R.style.Theme_StarkTech_Menu);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.st_navegador_imagens);
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

        String permissao = permissaoDeImagens();
        if (ContextCompat.checkSelfPermission(this, permissao) == PackageManager.PERMISSION_GRANTED) {
            mostrarArmazenamentos();
        } else {
            requestPermissions(new String[]{permissao}, PEDIDO_PERMISSAO);
        }
    }

    @Override
    public void onRequestPermissionsResult(int pedido, @NonNull String[] permissoes, @NonNull int[] respostas) {
        super.onRequestPermissionsResult(pedido, permissoes, respostas);
        if (pedido != PEDIDO_PERMISSAO) return;
        if (respostas.length > 0 && respostas[0] == PackageManager.PERMISSION_GRANTED) {
            mostrarArmazenamentos();
        } else {
            Toast.makeText(this, R.string.st_imagem_sem_permissao, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private static String permissaoDeImagens() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;
    }

    private void mostrarArmazenamentos() {
        titulo.setText(R.string.st_escolher_imagem);
        caminho.setText(R.string.st_escolher_imagem_dica);
        opcoes.removeAllViews();
        View primeira = null;
        for (ArquivosDoAparelho.Raiz raiz : ArquivosDoAparelho.raizes(this)) {
            String nome = getString(raiz.removivel ? R.string.st_hd_externo_usb : R.string.st_armazenamento_interno);
            View opcao = opcao(raiz.removivel ? R.drawable.st_ic_hd : R.drawable.st_ic_pastas, nome,
                    () -> mostrarPasta(raiz.pasta, raiz.pasta, nome));
            if (primeira == null) primeira = opcao;
        }
        if (tv && primeira != null) primeira.requestFocus();
    }

    /** Conteúdo de {@code pasta}; {@code raiz} e {@code nomeRaiz} montam o caminho mostrado. */
    private void mostrarPasta(File pasta, File raiz, String nomeRaiz) {
        boolean naRaiz = pasta.equals(raiz);
        titulo.setText(naRaiz ? nomeRaiz : pasta.getName());
        String relativo = pasta.getAbsolutePath().substring(raiz.getAbsolutePath().length()).replace("/", " / ");
        caminho.setText(nomeRaiz + relativo);
        opcoes.removeAllViews();
        View subir = opcao(R.drawable.st_ic_voltar, getString(R.string.st_subir), () -> {
            if (naRaiz) mostrarArmazenamentos();
            else mostrarPasta(pasta.getParentFile(), raiz, nomeRaiz);
        });
        List<File> itens = ArquivosDoAparelho.listar(pasta, true);
        for (File item : itens) {
            if (item.isDirectory()) {
                opcao(R.drawable.st_ic_pastas, item.getName(), () -> mostrarPasta(item, raiz, nomeRaiz));
            } else {
                opcao(R.drawable.st_ic_imagem, item.getName(), () -> {
                    setResult(RESULT_OK, new Intent().putExtra(EXTRA_CAMINHO, item.getAbsolutePath()));
                    finish();
                });
            }
        }
        if (itens.isEmpty()) {
            TextView vazio = Textos.novo(this, getString(R.string.st_nenhuma_imagem), R.font.st_inter_semibold,
                    tv ? 18 : 20, R.color.st_texto_apagado);
            int folga = caminho.getPaddingStart();
            vazio.setPadding(folga, folga, folga, folga);
            opcoes.addView(vazio);
        }
        if (tv && opcoes.getChildCount() > 1 && !itens.isEmpty()) opcoes.getChildAt(1).requestFocus();
        else if (tv) subir.requestFocus();
    }

    private View opcao(@DrawableRes int icone, String texto, Runnable acao) {
        return OpcaoMenu.adicionar(opcoes, icone, texto, acao);
    }
}
