// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import com.archos.mediacenter.video.R;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Texto completo de uma licença (1.1.1), aberto pelo aviso da biblioteca no Sobre: o nome da biblioteca, a
 * licença e o texto do arquivo em {@code res/raw} ({@link #EXTRA_TEXTO}). Na TV, a rolagem recebe o foco e as
 * setas ↑ ↓ descem o texto; Voltar fecha.
 */
public class LicencaActivity extends AppCompatActivity {

    public static final String EXTRA_NOME = "br.com.starktechstudio.mediaplayer.extra.LICENCA_NOME";
    public static final String EXTRA_LICENCA = "br.com.starktechstudio.mediaplayer.extra.LICENCA_TIPO";
    /** O id do recurso raw com o texto. */
    public static final String EXTRA_TEXTO = "br.com.starktechstudio.mediaplayer.extra.LICENCA_TEXTO";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(Aparelho.tema(this));
        super.onCreate(savedInstanceState);
        boolean tv = Aparelho.ehTv(this);
        int texto = getIntent().getIntExtra(EXTRA_TEXTO, 0);
        String nome = getIntent().getStringExtra(EXTRA_NOME);
        if (texto == 0 || nome == null) {
            finish();
            return;
        }

        FrameLayout raiz = new FrameLayout(this);
        raiz.setBackgroundResource(R.color.st_fundo);
        raiz.addView(new FundoAnimado(this), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        LinearLayout pagina = new LinearLayout(this);
        pagina.setOrientation(LinearLayout.VERTICAL);
        int lateral = getResources().getDimensionPixelSize(tv ? R.dimen.st_tv_margem_lateral : R.dimen.st_tablet_margem_h);
        pagina.setPadding(lateral, Medidas.px(this, 44), lateral, Medidas.px(this, 32));
        if (!tv) BordasSistema.aplicar(pagina, true, true, true, true);

        // Topo: Voltar, o nome da biblioteca e a licença
        LinearLayout topo = new LinearLayout(this);
        topo.setOrientation(LinearLayout.HORIZONTAL);
        topo.setGravity(Gravity.CENTER_VERTICAL);
        ImageButton voltar = new ImageButton(this);
        voltar.setImageResource(R.drawable.st_ic_voltar);
        ImageViewCompat.setImageTintList(voltar, ContextCompat.getColorStateList(this, R.color.st_texto));
        voltar.setBackgroundResource(R.drawable.st_botao_secundario);
        voltar.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int lado = getResources().getDimensionPixelSize(tv ? R.dimen.st_tv_botao_icone_altura : R.dimen.st_tablet_detalhe_topo_botao_altura);
        voltar.setPadding(lado / 4, lado / 4, lado / 4, lado / 4);
        voltar.setContentDescription(getString(R.string.st_voltar));
        voltar.setOnClickListener(v -> finish());
        // TV: Voltar do controle fecha; o foco fica no texto, que rola
        voltar.setFocusable(!tv);
        topo.addView(voltar, new LinearLayout.LayoutParams(lado, lado));
        LinearLayout titulos = new LinearLayout(this);
        titulos.setOrientation(LinearLayout.VERTICAL);
        TextView titulo = Textos.novo(this, nome, R.font.st_inter_black, tv ? 44 : 43, R.color.st_texto);
        titulo.setLetterSpacing(-0.02f);
        titulos.addView(titulo);
        String licenca = getIntent().getStringExtra(EXTRA_LICENCA);
        if (licenca != null) {
            TextView tipo = Textos.novo(this, licenca, R.font.st_inter_semibold, 20, R.color.st_texto_apagado);
            LinearLayout.LayoutParams lpTipo = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lpTipo.topMargin = Medidas.px(this, 6);
            titulos.addView(tipo, lpTipo);
        }
        LinearLayout.LayoutParams lpTitulos = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lpTitulos.setMarginStart(Medidas.px(this, 24));
        topo.addView(titulos, lpTitulos);
        pagina.addView(topo);

        // O texto, num cartão que rola
        TextView corpo = Textos.novo(this, ler(texto), R.font.st_inter_medium, 19, R.color.st_texto_secundario);
        corpo.setLineSpacing(0f, 1.25f);
        corpo.setTextIsSelectable(false);
        ScrollView rolagem = new ScrollView(this);
        rolagem.setBackgroundResource(R.drawable.st_fundo_cartao);
        int folga = Medidas.px(this, 32);
        rolagem.setPadding(folga, folga, folga, folga);
        rolagem.setClipToPadding(false);
        rolagem.setVerticalScrollBarEnabled(!tv);
        rolagem.setOverScrollMode(View.OVER_SCROLL_NEVER);
        rolagem.addView(corpo, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        if (tv) {
            rolagem.setFocusable(true);
            rolagem.setForeground(ContextCompat.getDrawable(this, R.drawable.st_foco_tv));
        }
        LinearLayout.LayoutParams lpRolagem = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        lpRolagem.topMargin = Medidas.px(this, 28);
        pagina.addView(rolagem, lpRolagem);

        raiz.addView(pagina, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(raiz);
        if (tv) rolagem.post(rolagem::requestFocus);
    }

    private String ler(int recurso) {
        try (InputStream entrada = getResources().openRawResource(recurso)) {
            ByteArrayOutputStream saida = new ByteArrayOutputStream();
            byte[] bloco = new byte[8 * 1024];
            int lidos;
            while ((lidos = entrada.read(bloco)) > 0) saida.write(bloco, 0, lidos);
            return saida.toString(StandardCharsets.UTF_8.name());
        } catch (IOException | RuntimeException e) {
            return "";
        }
    }
}
