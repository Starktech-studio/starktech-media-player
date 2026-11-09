// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.archos.mediacenter.video.R;

import java.io.File;
import java.util.List;

import br.com.starktechstudio.mediaplayer.dados.Modelo.Faixa;
import br.com.starktechstudio.mediaplayer.dados.Modelo.TipoFaixa;
import br.com.starktechstudio.mediaplayer.dados.RepositorioBiblioteca;

/**
 * Áudio e legendas (D13): painel lateral próprio, à direita, com a lista de faixas e a atual
 * marcada (✓). Do player abre só Áudio ou só Legendas; do Detalhe, os dois. OK ou toque escolhe e
 * fecha. F3: com o vídeo tocando, as faixas e a escolha são as do player ({@link Reproducao});
 * no Detalhe, a escolha só fica guardada, e o player a usa ao abrir o título. Nos dois casos ela
 * vale também nas próximas vezes ({@link RepositorioBiblioteca#guardarFaixa}). Com o vídeo tocando,
 * Legendas termina em "Carregar legenda" ({@link NavegadorLegendasActivity}).
 */
public class FaixasActivity extends AppCompatActivity {

    /** Só uma lista (nome do enum {@link TipoFaixa}); sem o extra, Áudio e Legendas. */
    public static final String EXTRA_TIPO = "br.com.starktechstudio.mediaplayer.extra.TIPO_FAIXA";

    private boolean tv;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        tv = Aparelho.ehTv(this);
        setTheme(tv ? R.style.Theme_StarkTech_Menu_Tv : R.style.Theme_StarkTech_Menu);
        super.onCreate(savedInstanceState);
        String id = getIntent().getStringExtra(Navegacao.EXTRA_ID);
        String tipoExtra = getIntent().getStringExtra(EXTRA_TIPO);
        TipoFaixa so = tipoExtra == null ? null : TipoFaixa.valueOf(tipoExtra);
        setContentView(R.layout.st_menu_capa);
        setFinishOnTouchOutside(true);

        // Painel à direita, centrado na altura
        getWindow().setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        WindowManager.LayoutParams janela = getWindow().getAttributes();
        janela.x = getResources().getDimensionPixelSize(tv ? R.dimen.st_tv_rep_painel_margem : R.dimen.st_tablet_rep_topo_padding_h);
        getWindow().setAttributes(janela);

        Resources res = getResources();
        int padding = res.getDimensionPixelSize(tv ? R.dimen.st_tv_menu_padding : R.dimen.st_tablet_menu_padding);
        LinearLayout caixa = findViewById(R.id.st_menu_caixa);
        ViewGroup.LayoutParams lpCaixa = caixa.getLayoutParams();
        lpCaixa.width = res.getDimensionPixelSize(tv ? R.dimen.st_tv_menu_largura : R.dimen.st_tablet_menu_largura);
        caixa.setLayoutParams(lpCaixa);
        caixa.setPadding(padding, padding, padding, padding);
        caixa.setContentDescription(getString(R.string.st_audio_legendas));
        int recuo = padding * 2 / 3;

        TextView titulo = findViewById(R.id.st_menu_titulo);
        titulo.setText(so == TipoFaixa.AUDIO ? R.string.st_audio : so == TipoFaixa.LEGENDA ? R.string.st_legendas
                : R.string.st_audio_legendas);
        titulo.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(tv ? R.dimen.st_tv_texto_menu_titulo : R.dimen.st_tablet_texto_menu_titulo));
        titulo.setPadding(recuo, padding / 3, recuo, recuo);
        findViewById(R.id.st_menu_dica).setVisibility(View.GONE);

        LinearLayout opcoes = findViewById(R.id.st_menu_opcoes);
        RepositorioBiblioteca repositorio = RepositorioBiblioteca.atual(this);
        TextView primeira = null;
        for (TipoFaixa tipo : TipoFaixa.values()) {
            if (so != null && tipo != so) continue;
            if (so == null) secao(opcoes, tipo == TipoFaixa.AUDIO ? R.string.st_audio : R.string.st_legendas, recuo);
            Reproducao tocando = Reproducao.naTela(id);
            List<Faixa> faixas = tocando != null ? tocando.faixas(tipo) : repositorio.faixas(id, tipo);
            String guardada = tocando != null ? null : repositorio.faixaEscolhida(id, null, tipo);
            // Legenda em arquivo fica guardada pelo nome, que a lista do Detalhe não tem
            String escolhida = guardada != null && guardada.startsWith("arquivo:") ? null : guardada;
            for (Faixa f : faixas) {
                boolean atual = escolhida != null ? f.id.equals(escolhida) : f.atual;
                TextView item = opcao(opcoes, f.nome, atual, () -> {
                    if (tocando != null) tocando.escolherFaixa(f);
                    else repositorio.guardarFaixa(id, null, tipo, f.id);
                });
                if (primeira == null || atual && so != null) primeira = item;
            }
            if (tocando != null && tipo == TipoFaixa.LEGENDA) {
                OpcaoMenu.adicionar(opcoes, R.drawable.st_ic_legendas, getString(R.string.st_carregar_legenda), () -> {
                    startActivity(new Intent(this, NavegadorLegendasActivity.class)
                            .putExtra(Navegacao.EXTRA_ID, id)
                            .putExtra(NavegadorArquivosActivity.EXTRA_PASTA, new File(tocando.caminho).getParent()));
                    finish();
                });
            }
        }
        if (tv && primeira != null) primeira.requestFocus();
    }

    /** Título da lista (com Áudio e Legendas juntos). */
    private void secao(LinearLayout opcoes, @StringRes int texto, int recuo) {
        TextView secao = new TextView(this);
        secao.setText(texto);
        secao.setTypeface(ResourcesCompat.getFont(this, R.font.st_inter_black));
        secao.setAllCaps(true);
        secao.setLetterSpacing(0.15f);
        secao.setTextColor(ContextCompat.getColor(this, R.color.st_texto_apagado));
        secao.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(
                tv ? R.dimen.st_tv_texto_dados_rotulo : R.dimen.st_tablet_texto_dados_rotulo));
        secao.setPadding(recuo, opcoes.getChildCount() > 0 ? recuo : 0, recuo, recuo / 2);
        opcoes.addView(secao);
    }

    /** Uma faixa ({@link OpcaoMenu}): ✓ na atual, espaço vazio nas outras (para alinhar). Escolhe e fecha. */
    private TextView opcao(LinearLayout opcoes, String texto, boolean atual, Runnable escolher) {
        TextView item = OpcaoMenu.adicionar(opcoes, atual ? R.drawable.st_ic_check : 0, texto, () -> {
            escolher.run();
            finish();
        });
        item.setSelected(atual);
        return item;
    }
}
