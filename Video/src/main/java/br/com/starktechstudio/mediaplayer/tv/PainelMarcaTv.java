// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.tv;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.widget.ImageViewCompat;

import com.archos.mediacenter.video.R;

import br.com.starktechstudio.mediaplayer.comum.Animacoes;
import br.com.starktechstudio.mediaplayer.comum.Medidas;
import br.com.starktechstudio.mediaplayer.comum.Textos;

/**
 * Painel da marca à direita das Configurações da TV (ajustes de 21/11/2026), na área que a lista deixava vazia: o logo
 * StarkTech (o do rodapé do Início) sobre um brilho azul que pulsa devagar, "StarkTech Studio®", "StarkTech Media
 * Player · versão X" (lida do pacote) e, num cartão, a versão instalada em destaque com o que mudou nela. Nada aqui
 * recebe foco. Com "remover animações", o brilho fica parado. Só na TV: o tablet segue sem o painel.
 *
 * <p>As novidades são as do array {@code st_cfg_novidades}, que valem só para a versão {@code st_cfg_novidades_versao};
 * outra versão instalada mostra o aviso de que não informou as novidades (nada inventado).
 */
public final class PainelMarcaTv {

    /** Meia volta do pulso do brilho (vai e volta: 6 s por ciclo), lento como o fundo animado. */
    static final long PULSO_MS = 3000;

    public final View view;
    private final View brilho;
    @Nullable
    private ValueAnimator pulso;

    public PainelMarcaTv(Context contexto, String versaoInstalada) {
        LinearLayout painel = new LinearLayout(contexto);
        painel.setOrientation(LinearLayout.VERTICAL);
        painel.setGravity(Gravity.CENTER);
        painel.setFocusable(false);

        // Logo sobre o brilho: um degradê radial azul, do centro para o transparente
        FrameLayout marca = new FrameLayout(contexto);
        marca.setClipChildren(false);
        int ladoBrilho = Medidas.px(contexto, 320);
        // Logo atual da StarkTech (INSTITUCIONAL\MARCA\DIGITAL, vertical para fundo escuro, 640 × 437), com o nome
        int larguraLogo = Medidas.px(contexto, 280);
        int alturaLogo = Math.round(larguraLogo * 437f / 640f);
        brilho = new View(contexto);
        int azul = ContextCompat.getColor(contexto, R.color.st_azul_botao);
        GradientDrawable degrade = new GradientDrawable();
        degrade.setShape(GradientDrawable.OVAL);
        degrade.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        degrade.setGradientRadius(ladoBrilho / 2f);
        degrade.setColors(new int[]{ColorUtils.setAlphaComponent(azul, 120), ColorUtils.setAlphaComponent(azul, 40),
                ColorUtils.setAlphaComponent(azul, 0)});
        brilho.setBackground(degrade);
        brilho.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        brilho.setAlpha(0.75f);
        marca.addView(brilho, new FrameLayout.LayoutParams(ladoBrilho, ladoBrilho, Gravity.CENTER));
        ImageView logo = new ImageView(contexto);
        logo.setImageResource(R.drawable.st_marca_vertical);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setContentDescription(contexto.getString(R.string.st_logo));
        marca.addView(logo, new FrameLayout.LayoutParams(larguraLogo, alturaLogo, Gravity.CENTER));
        painel.addView(marca, new LinearLayout.LayoutParams(ladoBrilho, ladoBrilho));

        // O nome da empresa já vem na logo; embaixo, "StarkTech Media Player · versão X", como no rodapé do Início
        TextView app = Textos.novo(contexto, contexto.getString(R.string.st_rodape_versao, versaoInstalada),
                R.font.st_inter_semibold, 19, R.color.st_texto_apagado);
        app.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams lpApp = centrado();
        lpApp.topMargin = -Medidas.px(contexto, 40);
        painel.addView(app, lpApp);

        LinearLayout.LayoutParams lpCartao = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpCartao.topMargin = Medidas.px(contexto, 36);
        painel.addView(cartaoDaVersao(contexto, versaoInstalada), lpCartao);
        view = painel;
    }

    /** Cartão: "VERSÃO INSTALADA", "Versão 2.0.0" grande e, embaixo de um traço, as novidades com o ✓ azul. */
    private static View cartaoDaVersao(Context contexto, String versaoInstalada) {
        LinearLayout cartao = new LinearLayout(contexto);
        cartao.setOrientation(LinearLayout.VERTICAL);
        cartao.setBackgroundResource(R.drawable.st_fundo_cartao);
        int folga = Medidas.px(contexto, 32);
        cartao.setPadding(folga, Medidas.px(contexto, 28), folga, Medidas.px(contexto, 30));
        cartao.addView(Textos.rotulo(contexto, contexto.getString(R.string.st_cfg_versao_instalada), 15));
        TextView numero = Textos.novo(contexto, contexto.getString(R.string.st_versao_titulo, versaoInstalada),
                R.font.st_inter_black, 46, R.color.st_texto);
        numero.setLetterSpacing(-0.02f);
        cartao.addView(numero, comTopo(Medidas.px(contexto, 10)));

        View traco = new View(contexto);
        traco.setBackgroundResource(R.color.st_borda);
        traco.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams lpTraco = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, Medidas.px(contexto, 2)));
        lpTraco.topMargin = Medidas.px(contexto, 22);
        cartao.addView(traco, lpTraco);

        cartao.addView(Textos.rotulo(contexto, contexto.getString(R.string.st_versao_novidades_rotulo), 15), comTopo(Medidas.px(contexto, 22)));
        String[] novidades = novidadesDa(contexto, versaoInstalada);
        if (novidades.length == 0) {
            TextView aviso = Textos.novo(contexto, contexto.getString(R.string.st_versao_sem_novidades), R.font.st_inter_semibold, 20,
                    R.color.st_texto_apagado);
            cartao.addView(aviso, comTopo(Medidas.px(contexto, 14)));
            return cartao;
        }
        for (int i = 0; i < novidades.length; i++) {
            LinearLayout linha = new LinearLayout(contexto);
            linha.setOrientation(LinearLayout.HORIZONTAL);
            linha.setGravity(Gravity.TOP);
            ImageView marca = new ImageView(contexto);
            marca.setImageResource(R.drawable.st_ic_check);
            ImageViewCompat.setImageTintList(marca, ContextCompat.getColorStateList(contexto, R.color.st_azul));
            marca.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            LinearLayout.LayoutParams lpMarca = new LinearLayout.LayoutParams(Medidas.px(contexto, 22), Medidas.px(contexto, 22));
            lpMarca.topMargin = Medidas.px(contexto, 1);
            linha.addView(marca, lpMarca);
            TextView texto = Textos.novo(contexto, novidades[i], R.font.st_inter_semibold, 20, R.color.st_texto_secundario);
            texto.setLineSpacing(0f, 1.15f);
            LinearLayout.LayoutParams lpTexto = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lpTexto.setMarginStart(Medidas.px(contexto, 14));
            linha.addView(texto, lpTexto);
            cartao.addView(linha, comTopo(Medidas.px(contexto, i == 0 ? 16 : 12)));
        }
        return cartao;
    }

    /**
     * As novidades da versão instalada: as do array só se a versão bate com a delas (sem o sufixo de data do build
     * de teste, "2.0.0-20261122"); senão, nenhuma.
     */
    private static String[] novidadesDa(Context contexto, String versaoInstalada) {
        String numero = versaoInstalada == null ? "" : versaoInstalada;
        int traco = numero.indexOf('-');
        if (traco >= 0) numero = numero.substring(0, traco);
        if (!numero.equals(contexto.getString(R.string.st_cfg_novidades_versao))) return new String[0];
        return contexto.getResources().getStringArray(R.array.st_cfg_novidades);
    }

    /** Liga o pulso do brilho (onResume); com "remover animações", fica parado. */
    public void ligar() {
        desligar();
        if (!Animacoes.ligadas(brilho.getContext())) {
            brilho.setAlpha(0.75f);
            brilho.setScaleX(1f);
            brilho.setScaleY(1f);
            return;
        }
        ObjectAnimator animador = ObjectAnimator.ofPropertyValuesHolder(brilho,
                PropertyValuesHolder.ofFloat(View.ALPHA, 0.45f, 1f),
                PropertyValuesHolder.ofFloat(View.SCALE_X, 0.9f, 1.08f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.9f, 1.08f));
        animador.setDuration(PULSO_MS);
        animador.setRepeatCount(ValueAnimator.INFINITE);
        animador.setRepeatMode(ValueAnimator.REVERSE);
        animador.setInterpolator(new AccelerateDecelerateInterpolator());
        animador.start();
        pulso = animador;
    }

    /** Para o pulso (onPause): fora da tela, nada anda. */
    public void desligar() {
        if (pulso != null) {
            pulso.cancel();
            pulso = null;
        }
    }

    private static LinearLayout.LayoutParams centrado() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        return lp;
    }

    private static LinearLayout.LayoutParams comTopo(int topo) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = topo;
        return lp;
    }
}
