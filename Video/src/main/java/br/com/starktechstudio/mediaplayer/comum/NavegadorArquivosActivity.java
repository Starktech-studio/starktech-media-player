// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.content.res.Resources;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import com.archos.mediacenter.video.R;

import java.io.File;
import java.util.List;

import br.com.starktechstudio.mediaplayer.motor.ArquivosDoAparelho;

/**
 * Navegador de arquivos do app, em janela flutuante, na TV e no tablet (o seletor do sistema travava
 * com o HD no tablet e pode não existir na TV): os armazenamentos de verdade (interno, HD / pen drive),
 * as pastas e os arquivos do tipo pedido. Abre direto em {@link #EXTRA_PASTA}; "Voltar uma pasta" sobe
 * dali. Cada tipo diz o que lista, os textos e o que fazer com o arquivo escolhido.
 */
public abstract class NavegadorArquivosActivity extends AppCompatActivity {

    /** Pasta em que o navegador abre; sem ela ou sem o HD, os armazenamentos. */
    public static final String EXTRA_PASTA = "pasta";

    private boolean tv;
    private LinearLayout opcoes;
    private TextView titulo;
    private TextView caminho;

    /** Confere a permissão do tipo e chama {@link #mostrarInicio}. */
    protected abstract void aoAbrir();

    @StringRes
    protected abstract int textoTitulo();

    @StringRes
    protected abstract int textoDica();

    @StringRes
    protected abstract int textoVazio();

    @DrawableRes
    protected abstract int iconeDoArquivo();

    protected abstract List<File> listar(File pasta);

    protected abstract void aoEscolher(File arquivo);

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

        aoAbrir();
    }

    /** A pasta pedida, se está à mão; senão, a lista de armazenamentos. */
    protected void mostrarInicio() {
        String inicial = getIntent().getStringExtra(EXTRA_PASTA);
        File pasta = inicial == null ? null : new File(inicial);
        ArquivosDoAparelho.Raiz raiz = pasta == null ? null : ArquivosDoAparelho.raizDe(this, pasta.getAbsolutePath());
        if (pasta != null && pasta.isDirectory() && raiz != null) {
            mostrarPasta(pasta, raiz.pasta, getString(raiz.removivel ? R.string.st_hd_externo_usb : R.string.st_armazenamento_interno));
        } else {
            mostrarArmazenamentos();
        }
    }

    private void mostrarArmazenamentos() {
        titulo.setText(textoTitulo());
        caminho.setText(textoDica());
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
        List<File> itens = listar(pasta);
        for (File item : itens) {
            if (item.isDirectory()) {
                opcao(R.drawable.st_ic_pastas, item.getName(), () -> mostrarPasta(item, raiz, nomeRaiz));
            } else {
                opcao(iconeDoArquivo(), item.getName(), () -> aoEscolher(item));
            }
        }
        if (itens.isEmpty()) {
            TextView vazio = Textos.novo(this, getString(textoVazio()), R.font.st_inter_semibold,
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
