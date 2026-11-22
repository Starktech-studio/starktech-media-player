// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.archos.mediacenter.video.R;

import br.com.starktechstudio.mediaplayer.motor.EfeitosDeAudio;

/**
 * Puxador do volume extra (2.1): o trilho de 100 % a 200 %, de 10 em 10 ({@link EfeitosDeAudio#POSICOES} passos), com o
 * valor em % ao lado ("150%"). Tablet: tocar ou arrastar no trilho. TV: o foco é da linha que o contém
 * ({@link #ligarTeclas}, com o realce dela); ← e → mudam 10 % e o trilho só mostra. Cores: o cheio em azul, o trilho
 * apagado e o botão azul com borda branca, legíveis no cartão escuro e no branco do item em foco do painel de Áudio.
 * Cada mudança vai para {@link AoMudar} na hora (a tela grava; os efeitos ouvem a preferência).
 */
public final class PuxadorNivel {

    public interface AoMudar {
        void em(int nivel);
    }

    /** Trilho e valor, numa linha. */
    public final LinearLayout view;
    private final SeekBar trilho;
    private final TextView valor;
    private final AoMudar aoMudar;
    private int nivel;

    /**
     * {@code noPainel}: dentro de um item do painel do player (o valor segue a cor do item: escuro com o foco); senão,
     * numa linha de Configurações (valor branco, como os outros valores).
     */
    public PuxadorNivel(Context contexto, int nivel, boolean noPainel, AoMudar aoMudar) {
        this.aoMudar = aoMudar;
        this.nivel = EfeitosDeAudio.limitarNivel(nivel);
        boolean tv = Aparelho.ehTv(contexto);

        view = new LinearLayout(contexto);
        view.setOrientation(LinearLayout.HORIZONTAL);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setDuplicateParentStateEnabled(noPainel);

        int lado = Medidas.px(contexto, 26);
        int espessura = Medidas.px(contexto, 7);
        trilho = new SeekBar(contexto);
        trilho.setMax(EfeitosDeAudio.POSICOES);
        trilho.setProgress(EfeitosDeAudio.posicaoDoNivel(this.nivel));
        trilho.setProgressDrawable(desenhoDoTrilho(contexto, espessura));
        trilho.setThumb(botao(contexto, lado));
        trilho.setSplitTrack(false);
        trilho.setBackground(null);
        trilho.setPadding(lado / 2 + 1, 0, lado / 2 + 1, 0);
        trilho.setThumbOffset(lado / 2);
        if (tv) {
            // Na TV quem tem o foco é a linha; o trilho só mostra
            trilho.setFocusable(false);
            trilho.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
        trilho.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar barra, int posicao, boolean doUsuario) {
                if (doUsuario) mudar(EfeitosDeAudio.nivelDaPosicao(posicao));
            }

            @Override
            public void onStartTrackingTouch(SeekBar barra) {
                // nada: cada passo já vale
            }

            @Override
            public void onStopTrackingTouch(SeekBar barra) {
                // nada: cada passo já vale
            }
        });
        view.addView(trilho, new LinearLayout.LayoutParams(0, lado + Medidas.px(contexto, 12), 1f));

        valor = Textos.novo(contexto, "", noPainel ? R.font.st_inter_black : R.font.st_inter_extrabold, noPainel ? 21 : 18,
                R.color.st_texto);
        if (noPainel) {
            ColorStateList cores = ContextCompat.getColorStateList(contexto, R.color.st_cor_item_menu);
            valor.setTextColor(cores);
            valor.setDuplicateParentStateEnabled(true);
        }
        valor.setSingleLine(true);
        valor.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        // Largura de "200%", para o trilho não mudar de tamanho entre 100 e 200
        valor.setMinWidth((int) Math.ceil(valor.getPaint().measureText(contexto.getString(R.string.st_porcento, EfeitosDeAudio.NIVEL_MAXIMO))));
        LinearLayout.LayoutParams lpValor = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpValor.setMarginStart(Medidas.px(contexto, 14));
        view.addView(valor, lpValor);
        mostrar();
    }

    public int nivel() {
        return nivel;
    }

    /** Troca o nível mostrado, sem avisar (o valor mudou noutro lugar). */
    public void definir(int novo) {
        nivel = EfeitosDeAudio.limitarNivel(novo);
        trilho.setProgress(EfeitosDeAudio.posicaoDoNivel(nivel));
        mostrar();
    }

    /** Desligado (volume extra desligado): apagado e sem toque. */
    public void habilitar(boolean ligado) {
        trilho.setEnabled(ligado);
        view.setAlpha(ligado ? 1f : 0.4f);
    }

    /**
     * TV: ← e → na linha {@code dona} (a que tem o foco) mudam 10 % e ficam com ela, também nas pontas e no soltar da
     * tecla, para o foco não sair pelos lados. Linha desligada não tem foco.
     */
    public void ligarTeclas(View dona) {
        dona.setOnKeyListener((v, tecla, evento) -> {
            if (tecla != KeyEvent.KEYCODE_DPAD_LEFT && tecla != KeyEvent.KEYCODE_DPAD_RIGHT) return false;
            if (evento.getAction() == KeyEvent.ACTION_DOWN && trilho.isEnabled()) {
                int novo = EfeitosDeAudio.passo(nivel, tecla == KeyEvent.KEYCODE_DPAD_RIGHT ? 1 : -1);
                if (novo != nivel) {
                    trilho.setProgress(EfeitosDeAudio.posicaoDoNivel(novo));
                    mudar(novo);
                }
            }
            return true;
        });
    }

    private void mudar(int novo) {
        if (novo == nivel) return;
        nivel = novo;
        mostrar();
        aoMudar.em(nivel);
    }

    private void mostrar() {
        Context contexto = valor.getContext();
        valor.setText(contexto.getString(R.string.st_porcento, nivel));
        trilho.setContentDescription(contexto.getString(R.string.st_rep_nivel_volume_extra, nivel));
    }

    /** Trilho apagado e o cheio azul, com a espessura dada, centrados na altura do puxador. */
    private static Drawable desenhoDoTrilho(Context contexto, int espessura) {
        GradientDrawable fundo = new GradientDrawable();
        fundo.setColor(ContextCompat.getColor(contexto, R.color.st_apagado_40));
        fundo.setCornerRadius(espessura / 2f);
        GradientDrawable cheio = new GradientDrawable();
        cheio.setColor(ContextCompat.getColor(contexto, R.color.st_azul_botao));
        cheio.setCornerRadius(espessura / 2f);
        LayerDrawable camadas = new LayerDrawable(new Drawable[]{fundo, new ClipDrawable(cheio, Gravity.START, ClipDrawable.HORIZONTAL)});
        camadas.setId(0, android.R.id.background);
        camadas.setId(1, android.R.id.progress);
        for (int i = 0; i < 2; i++) {
            camadas.setLayerGravity(i, Gravity.CENTER_VERTICAL | Gravity.FILL_HORIZONTAL);
            camadas.setLayerHeight(i, espessura);
        }
        return camadas;
    }

    /** Botão redondo azul com borda branca. */
    private static Drawable botao(Context contexto, int lado) {
        GradientDrawable botao = new GradientDrawable();
        botao.setShape(GradientDrawable.OVAL);
        botao.setColor(ContextCompat.getColor(contexto, R.color.st_azul_botao));
        botao.setStroke(Math.max(2, lado / 8), ContextCompat.getColor(contexto, R.color.st_texto));
        botao.setSize(lado, lado);
        return botao;
    }
}
