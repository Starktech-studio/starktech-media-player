// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.tablet;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioManager;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

/**
 * Gestos do player do tablet (INTERFACE.md §3), sem dica na tela: um toque mostra ou esconde os
 * controles; dois toques na lateral esquerda / direita = −10 s / +10 s; arrastar para os lados
 * avança ou volta (a largura da tela vale 2 min); arrastar para cima / baixo muda o volume do
 * aparelho. O sentido do arrasto é decidido no começo e não muda até soltar.
 */
final class GestosReproducao {

    /** O que os gestos pedem ao player. */
    interface Acoes {
        void alternarControles();

        void pular(int segundos);

        /** Arrastar para os lados: {@code deslocamento} em segundos desde o começo do arrasto. */
        void arrastar(int deslocamento, boolean soltou);

        /** Volume de 0 a 100, depois de mudar. */
        void volume(int porcento);
    }

    private static final int TELA_EM_SEGUNDOS = 120;

    private enum Sentido { NENHUM, LADO, ALTURA }

    private final View area;
    private final Acoes acoes;
    private final AudioManager audio;
    private final GestureDetector detector;
    private Sentido sentido = Sentido.NENHUM;
    private float inicioX;
    private float inicioY;
    private int volumeInicial;

    @SuppressLint("ClickableViewAccessibility")
    GestosReproducao(View area, Acoes acoes) {
        this.area = area;
        this.acoes = acoes;
        Context contexto = area.getContext();
        audio = (AudioManager) contexto.getSystemService(Context.AUDIO_SERVICE);
        detector = new GestureDetector(contexto, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(@NonNull MotionEvent e) {
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
                acoes.alternarControles();
                return true;
            }

            @Override
            public boolean onDoubleTap(@NonNull MotionEvent e) {
                float terco = area.getWidth() / 3f;
                if (e.getX() < terco) acoes.pular(-10);
                else if (e.getX() > 2 * terco) acoes.pular(10);
                return true;
            }
        });
        area.setOnTouchListener((v, e) -> tocar(e));
    }

    private boolean tocar(MotionEvent e) {
        detector.onTouchEvent(e);
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                sentido = Sentido.NENHUM;
                inicioX = e.getX();
                inicioY = e.getY();
                volumeInicial = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
                return true;
            case MotionEvent.ACTION_MOVE: {
                float dx = e.getX() - inicioX;
                float dy = e.getY() - inicioY;
                if (sentido == Sentido.NENHUM) {
                    float limite = 24 * area.getResources().getDisplayMetrics().density;
                    if (Math.abs(dx) > limite && Math.abs(dx) > Math.abs(dy)) sentido = Sentido.LADO;
                    else if (Math.abs(dy) > limite) sentido = Sentido.ALTURA;
                }
                if (sentido == Sentido.LADO) {
                    acoes.arrastar(Math.round(dx / Math.max(1, area.getWidth()) * TELA_EM_SEGUNDOS), false);
                } else if (sentido == Sentido.ALTURA) {
                    mudarVolume(dy);
                }
                return true;
            }
            case MotionEvent.ACTION_UP:
                if (sentido == Sentido.LADO) {
                    float dx = e.getX() - inicioX;
                    acoes.arrastar(Math.round(dx / Math.max(1, area.getWidth()) * TELA_EM_SEGUNDOS), true);
                }
                sentido = Sentido.NENHUM;
                return true;
            default:
                return true;
        }
    }

    /** Arrastar a altura inteira da tela para cima vai do volume atual ao máximo. */
    private void mudarVolume(float dy) {
        int maximo = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int passos = Math.round(-dy / Math.max(1, area.getHeight()) * maximo);
        int novo = Math.max(0, Math.min(maximo, volumeInicial + passos));
        if (novo != audio.getStreamVolume(AudioManager.STREAM_MUSIC)) {
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, novo, 0);
        }
        acoes.volume(maximo == 0 ? 0 : Math.round(novo * 100f / maximo));
    }
}
