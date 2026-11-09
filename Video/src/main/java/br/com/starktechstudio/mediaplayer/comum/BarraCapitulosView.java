// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import com.archos.mediacenter.video.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Barra do player (tv-player.dc.html, tablet-player.dc.html): trilho branco 15 %, avanço azul,
 * marca branca 75 % no começo de cada capítulo e o ponto branco com anel azul 45 % na posição.
 * TV: recebe o foco pelo layout e a posição anda pelas teclas (← / → em ReproducaoTvActivity).
 * Tablet: tocar ou arrastar na barra muda a posição.
 */
public class BarraCapitulosView extends View {

    /** Posição escolhida no toque (tablet), em fração de 0 a 1. */
    public interface AoArrastar {
        void irPara(float fracao, boolean soltou);
    }

    private final Paint trilho = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint avanco = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint marca = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ponto = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint anel = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF retangulo = new RectF();
    private final List<Float> marcas = new ArrayList<>();

    private float alturaTrilho;
    private float larguraMarca;
    private float alturaMarca;
    private float raioPonto;
    private float larguraAnel;
    private float fracao;
    private AoArrastar aoArrastar;

    public BarraCapitulosView(Context contexto) {
        this(contexto, null);
    }

    public BarraCapitulosView(Context contexto, @Nullable AttributeSet atributos) {
        super(contexto, atributos);
        trilho.setColor(ContextCompat.getColor(contexto, R.color.st_branco_15));
        avanco.setColor(ContextCompat.getColor(contexto, R.color.st_azul));
        marca.setColor(ColorUtils.setAlphaComponent(ContextCompat.getColor(contexto, R.color.st_texto), 191));
        ponto.setColor(ContextCompat.getColor(contexto, R.color.st_texto));
        anel.setColor(ColorUtils.setAlphaComponent(ContextCompat.getColor(contexto, R.color.st_azul), 115));
        definirTv(Aparelho.ehTv(contexto));
    }

    /** Medidas do desenho: TV (px / 2) ou tablet (px = dp). */
    public void definirTv(boolean tv) {
        float d = getResources().getDisplayMetrics().density;
        alturaTrilho = (tv ? 5f : 8f) * d;
        larguraMarca = (tv ? 1.5f : 2f) * d;
        alturaMarca = (tv ? 9f : 14f) * d;
        raioPonto = (tv ? 6.5f : 12f) * d;
        larguraAnel = (tv ? 3f : 5f) * d;
        requestLayout();
        invalidate();
    }

    /** Começo de cada capítulo, em fração da duração (o primeiro, no zero, não leva marca). */
    public void definirMarcas(List<Float> fracoes) {
        marcas.clear();
        for (Float f : fracoes) {
            if (f > 0f) marcas.add(f);
        }
        invalidate();
    }

    public void definirFracao(float nova) {
        fracao = Math.max(0f, Math.min(1f, nova));
        invalidate();
    }

    public void definirAoArrastar(AoArrastar aoArrastar) {
        this.aoArrastar = aoArrastar;
    }

    @Override
    protected void onMeasure(int larguraSpec, int alturaSpec) {
        int altura = Math.round(2 * (raioPonto + larguraAnel));
        setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), larguraSpec),
                resolveSize(altura, alturaSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float margem = raioPonto + larguraAnel;
        float esquerda = margem;
        float direita = getWidth() - margem;
        float meio = getHeight() / 2f;
        float largura = Math.max(0f, direita - esquerda);

        retangulo.set(esquerda, meio - alturaTrilho / 2, direita, meio + alturaTrilho / 2);
        canvas.drawRoundRect(retangulo, alturaTrilho / 2, alturaTrilho / 2, trilho);
        retangulo.right = esquerda + largura * fracao;
        canvas.drawRoundRect(retangulo, alturaTrilho / 2, alturaTrilho / 2, avanco);

        for (Float f : marcas) {
            float x = esquerda + largura * f;
            retangulo.set(x - larguraMarca / 2, meio - alturaMarca / 2, x + larguraMarca / 2, meio + alturaMarca / 2);
            canvas.drawRoundRect(retangulo, larguraMarca, larguraMarca, marca);
        }

        float x = esquerda + largura * fracao;
        canvas.drawCircle(x, meio, raioPonto + larguraAnel, anel);
        canvas.drawCircle(x, meio, raioPonto, ponto);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent evento) {
        if (aoArrastar == null) return super.onTouchEvent(evento);
        float margem = raioPonto + larguraAnel;
        float largura = Math.max(1f, getWidth() - 2 * margem);
        float f = Math.max(0f, Math.min(1f, (evento.getX() - margem) / largura));
        switch (evento.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                // fall through
            case MotionEvent.ACTION_MOVE:
                definirFracao(f);
                aoArrastar.irPara(f, false);
                return true;
            case MotionEvent.ACTION_UP:
                definirFracao(f);
                aoArrastar.irPara(f, true);
                return true;
            case MotionEvent.ACTION_CANCEL:
                return true;
            default:
                return super.onTouchEvent(evento);
        }
    }
}
