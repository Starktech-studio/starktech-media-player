// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import com.archos.mediacenter.video.R;

/**
 * Fundo animado das telas StarkTech (TV e tablet), atrás de todo o conteúdo: 3 manchas de luz
 * desfocadas (azul #2563eb e ciano #06b6d4, 10 a 18 % de opacidade) que andam devagar pela tela,
 * cada uma no seu ritmo (20 a 40 s por volta), mudando um pouco de tamanho.
 * Leve: cada mancha é um gradiente radial criado uma vez (o desfoque já vem pronto no gradiente);
 * por quadro só muda a matriz de posição e escala, sem alocar nada. Redesenha a ~30 quadros por
 * segundo (o movimento é lento) e para fora da tela. Parado com "remover animações" ligado ou com
 * {@link #definirParado(boolean)} (player).
 */
public class FundoAnimado extends View {

    /** ~30 quadros por segundo: o movimento é lento, 60 não muda nada na tela. */
    private static final long QUADRO_MS = 33;

    /**
     * Mancha: cor, opacidade, raio (fração da diagonal), centro e amplitude do passeio (fração da
     * tela), duração da volta (s), fase e variação de tamanho.
     */
    private static final class Mancha {
        final int corRes;
        final float opacidade;
        final float raio;
        final float cx, cy, ax, ay;
        final float voltaSegundos;
        final float fase;
        final float variacao;
        final Paint pincel = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Matrix matriz = new Matrix();

        Mancha(int corRes, float opacidade, float raio, float cx, float cy, float ax, float ay,
               float voltaSegundos, float fase, float variacao) {
            this.corRes = corRes;
            this.opacidade = opacidade;
            this.raio = raio;
            this.cx = cx;
            this.cy = cy;
            this.ax = ax;
            this.ay = ay;
            this.voltaSegundos = voltaSegundos;
            this.fase = fase;
            this.variacao = variacao;
        }
    }

    private final Mancha[] manchas = {
            // Azul no alto à direita (como o brilho do desenho), volta de 31 s
            new Mancha(R.color.st_azul_botao, 0.16f, 0.42f, 0.78f, 0.18f, 0.16f, 0.14f, 31f, 0.0f, 0.10f),
            // Ciano embaixo à esquerda, volta de 38 s
            new Mancha(R.color.st_ciano, 0.10f, 0.36f, 0.22f, 0.82f, 0.18f, 0.12f, 38f, 2.1f, 0.12f),
            // Azul no meio, mais fraca, volta de 24 s
            new Mancha(R.color.st_azul_botao, 0.12f, 0.30f, 0.50f, 0.50f, 0.26f, 0.22f, 24f, 4.2f, 0.08f),
    };

    private final long inicio = SystemClock.uptimeMillis();
    private boolean parado;
    private boolean rodando;
    private int largura;
    private int altura;

    private final Runnable proximoQuadro = new Runnable() {
        @Override
        public void run() {
            if (!rodando) return;
            invalidate();
            postOnAnimationDelayed(this, QUADRO_MS);
        }
    };

    public FundoAnimado(Context contexto) {
        this(contexto, null);
    }

    public FundoAnimado(Context contexto, @Nullable AttributeSet atributos) {
        super(contexto, atributos);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setFocusable(false);
    }

    /** Player: fundo parado. */
    public void definirParado(boolean parado) {
        this.parado = parado;
        atualizarRodando();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int antigoW, int antigoH) {
        super.onSizeChanged(w, h, antigoW, antigoH);
        largura = w;
        altura = h;
        float diagonal = (float) Math.hypot(w, h);
        for (Mancha m : manchas) {
            int cor = ContextCompat.getColor(getContext(), m.corRes);
            int centro = ColorUtils.setAlphaComponent(cor, Math.round(255 * m.opacidade));
            int meio = ColorUtils.setAlphaComponent(cor, Math.round(255 * m.opacidade * 0.45f));
            // Gradiente de raio 1 em (0,0): a matriz põe no lugar e no tamanho a cada quadro
            m.pincel.setShader(new RadialGradient(0f, 0f, 1f,
                    new int[]{centro, meio, 0x00000000}, new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
            m.matriz.reset();
            m.pincel.getShader().setLocalMatrix(m.matriz);
        }
        if (diagonal > 0) invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (largura == 0 || altura == 0) return;
        float diagonal = (float) Math.hypot(largura, altura);
        float t = parado || !Animacoes.ligadas(getContext()) ? 0f
                : (SystemClock.uptimeMillis() - inicio) / 1000f;
        for (Mancha m : manchas) {
            double angulo = 2 * Math.PI * t / m.voltaSegundos + m.fase;
            // Passeio em laço (Lissajous 1:2) e tamanho que respira com outro ritmo
            float x = largura * (m.cx + m.ax * (float) Math.sin(angulo));
            float y = altura * (m.cy + m.ay * (float) Math.sin(2 * angulo + m.fase));
            float raio = diagonal * m.raio * (1f + m.variacao * (float) Math.sin(angulo * 0.7 + 1.3));
            m.matriz.setScale(raio, raio);
            m.matriz.postTranslate(x, y);
            m.pincel.getShader().setLocalMatrix(m.matriz);
            canvas.drawCircle(x, y, raio, m.pincel);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        atualizarRodando();
    }

    @Override
    protected void onDetachedFromWindow() {
        rodando = false;
        removeCallbacks(proximoQuadro);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibilidade) {
        super.onWindowVisibilityChanged(visibilidade);
        atualizarRodando();
    }

    /** Só redesenha com a janela visível, animações ligadas e fora do player. */
    private void atualizarRodando() {
        boolean deve = isAttachedToWindow() && getWindowVisibility() == VISIBLE && !parado
                && Animacoes.ligadas(getContext());
        if (deve == rodando) return;
        rodando = deve;
        removeCallbacks(proximoQuadro);
        if (rodando) postOnAnimation(proximoQuadro);
    }
}
