// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import com.archos.mediacenter.video.R;

import java.util.Locale;

/**
 * Legenda rápida (1.5): painel à esquerda do player, aberto pelo botão Ajustar. Atraso: − adianta e +
 * atrasa, de 0,1 em 0,1 s (segurar OK repete), e Zerar; fica guardado no título. Tamanho: − e +, os cinco
 * níveis de Configurações, que valem para todos os vídeos. Tudo vale na hora, com o vídeo atrás.
 */
public class AjusteLegendaActivity extends AppCompatActivity {

    private boolean tv;
    private Reproducao reproducao;
    private TextView atraso;
    private TextView atrasoDica;
    private TextView tamanho;
    private ImageButton menor;
    private ImageButton maior;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        tv = Aparelho.ehTv(this);
        setTheme(tv ? R.style.Theme_StarkTech_Menu_Tv : R.style.Theme_StarkTech_Menu);
        super.onCreate(savedInstanceState);
        reproducao = Reproducao.naTela(getIntent().getStringExtra(Navegacao.EXTRA_ID));
        if (reproducao == null) {
            finish();
            return;
        }
        setContentView(R.layout.st_menu_capa);
        setFinishOnTouchOutside(true);
        // Painel à esquerda, centrado na altura; o vídeo fica à vista atrás (sem escurecer)
        getWindow().setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        WindowManager.LayoutParams janela = getWindow().getAttributes();
        janela.x = getResources().getDimensionPixelSize(tv ? R.dimen.st_tv_rep_painel_margem : R.dimen.st_tablet_rep_topo_padding_h);
        janela.dimAmount = 0.15f;
        getWindow().setAttributes(janela);

        Resources res = getResources();
        int padding = res.getDimensionPixelSize(tv ? R.dimen.st_tv_menu_padding : R.dimen.st_tablet_menu_padding);
        LinearLayout caixa = findViewById(R.id.st_menu_caixa);
        ViewGroup.LayoutParams lpCaixa = caixa.getLayoutParams();
        lpCaixa.width = res.getDimensionPixelSize(tv ? R.dimen.st_tv_menu_largura : R.dimen.st_tablet_menu_largura);
        caixa.setLayoutParams(lpCaixa);
        caixa.setPadding(padding, padding, padding, padding);
        int recuo = padding * 2 / 3;

        TextView titulo = findViewById(R.id.st_menu_titulo);
        titulo.setText(R.string.st_legenda_ajustar_titulo);
        titulo.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(tv ? R.dimen.st_tv_texto_menu_titulo : R.dimen.st_tablet_texto_menu_titulo));
        titulo.setPadding(recuo, padding / 3, recuo, recuo / 2);
        TextView dica = findViewById(R.id.st_menu_dica);
        dica.setText(R.string.st_legenda_ajustar_dica);
        dica.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(tv ? R.dimen.st_tv_texto_menu_dica : R.dimen.st_tablet_texto_menu_dica));
        dica.setPadding(recuo, 0, recuo, recuo);

        LinearLayout opcoes = findViewById(R.id.st_menu_opcoes);
        secao(opcoes, R.string.st_legenda_atraso, recuo);
        atraso = valor();
        ImageButton adiantar = botao(R.drawable.st_ic_menos, R.string.st_legenda_adiantar,
                () -> reproducao.ajustarAtrasoLegenda(-Reproducao.ATRASO_PASSO_MS));
        ImageButton atrasar = botao(R.drawable.st_ic_mais, R.string.st_legenda_atrasar,
                () -> reproducao.ajustarAtrasoLegenda(Reproducao.ATRASO_PASSO_MS));
        opcoes.addView(linha(adiantar, atraso, atrasar));
        atrasoDica = Textos.novo(this, "", R.font.st_inter_semibold, 16, R.color.st_texto_apagado);
        atrasoDica.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams lpDica = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpDica.topMargin = recuo / 2;
        opcoes.addView(atrasoDica, lpDica);
        OpcaoMenu.adicionar(opcoes, R.drawable.st_ic_do_inicio, getString(R.string.st_legenda_zerar), () -> {
            reproducao.definirAtrasoLegenda(0);
            mostrar();
        });

        secao(opcoes, R.string.st_legenda_tamanho, recuo);
        tamanho = valor();
        menor = botao(R.drawable.st_ic_menos, R.string.st_legenda_menor, () -> reproducao.ajustarTamanhoLegenda(-1));
        maior = botao(R.drawable.st_ic_mais, R.string.st_legenda_maior, () -> reproducao.ajustarTamanhoLegenda(1));
        opcoes.addView(linha(menor, tamanho, maior));

        mostrar();
        if (tv) atrasar.requestFocus();
    }

    private void mostrar() {
        int ms = reproducao.atrasoLegendaMs();
        String segundos = String.format(Locale.forLanguageTag("pt-BR"), "%.1f s", Math.abs(ms) / 1000f);
        atraso.setText(ms == 0 ? getString(R.string.st_legenda_sem_atraso) : (ms > 0 ? "+" : "−") + segundos);
        atrasoDica.setText(ms == 0 ? "" : getString(ms > 0 ? R.string.st_legenda_mais_tarde : R.string.st_legenda_mais_cedo, segundos));
        atrasoDica.setVisibility(ms == 0 ? View.GONE : View.VISIBLE);
        int nivel = Preferencias.indice(this, Preferencias.Item.TAMANHO_LEGENDA);
        tamanho.setText(Preferencias.valor(this, Preferencias.Item.TAMANHO_LEGENDA));
        menor.setEnabled(nivel > 0);
        menor.setAlpha(nivel > 0 ? 1f : 0.35f);
        int ultimo = Reproducao.LEGENDA_TAMANHO_PT.length - 1;
        maior.setEnabled(nivel < ultimo);
        maior.setAlpha(nivel < ultimo ? 1f : 0.35f);
    }

    private void secao(LinearLayout opcoes, @StringRes int texto, int recuo) {
        TextView secao = Textos.rotulo(this, getString(texto), 14);
        secao.setPadding(recuo, opcoes.getChildCount() > 0 ? recuo : 0, recuo, recuo / 2);
        opcoes.addView(secao);
    }

    private TextView valor() {
        TextView t = Textos.novo(this, "", R.font.st_inter_black, 30, R.color.st_texto);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    /** − valor + numa linha. */
    private LinearLayout linha(View menos, TextView meio, View mais) {
        LinearLayout linha = new LinearLayout(this);
        linha.setOrientation(LinearLayout.HORIZONTAL);
        linha.setGravity(Gravity.CENTER_VERTICAL);
        int lado = getResources().getDimensionPixelSize(tv ? R.dimen.st_tv_menu_item_altura : R.dimen.st_tablet_menu_item_altura);
        linha.addView(menos, new LinearLayout.LayoutParams(lado, lado));
        linha.addView(meio, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        linha.addView(mais, new LinearLayout.LayoutParams(lado, lado));
        return linha;
    }

    /** Botão quadrado do painel: em foco, fundo branco e ícone escuro (como as opções); segurar OK repete. */
    private ImageButton botao(@DrawableRes int icone, @StringRes int descricao, Runnable acao) {
        ImageButton b = new ImageButton(this);
        b.setImageResource(icone);
        b.setBackgroundResource(R.drawable.st_item_menu);
        b.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        ColorStateList cores = ContextCompat.getColorStateList(this, R.color.st_cor_item_menu);
        ImageViewCompat.setImageTintList(b, cores);
        b.setContentDescription(getString(descricao));
        b.setFocusable(true);
        b.setOnClickListener(v -> {
            acao.run();
            mostrar();
        });
        b.setOnKeyListener((v, tecla, evento) -> {
            boolean ok = tecla == KeyEvent.KEYCODE_DPAD_CENTER || tecla == KeyEvent.KEYCODE_ENTER;
            if (ok && evento.getAction() == KeyEvent.ACTION_DOWN && evento.getRepeatCount() > 0 && v.isEnabled()) {
                acao.run();
                mostrar();
                return true;
            }
            return false;
        });
        return b;
    }
}
