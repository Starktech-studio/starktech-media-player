// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.tv;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.FocusFinder;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.archos.mediacenter.video.R;

import br.com.starktechstudio.mediaplayer.comum.Animacoes;
import br.com.starktechstudio.mediaplayer.comum.Navegacao;
import br.com.starktechstudio.mediaplayer.comum.Navegacao.Aba;

/**
 * Tela principal da TV (Main.dc.html): barra do topo com a marca, as pílulas
 * Início, Filmes, Séries, Shows e Pastas e os botões Buscar e Configurações; abaixo,
 * o conteúdo da aba. ← / → anda entre as pílulas (a pílula em foco vira a aba ativa),
 * ↓ desce ao conteúdo, ↑ no topo do conteúdo volta à pílula ativa; Voltar no conteúdo
 * também volta à pílula ativa. Na troca de aba, a pílula branca estica até a aba nova e encolhe
 * nela (260 ms); o conteúdo atual sai para o outro lado e o novo, montado quando o menu para,
 * entra deslizando (sem animação com "remover animações").
 */
public class PrincipalTvActivity extends AppCompatActivity {

    private static final String ESTADO_ABA = "aba";

    private static final Aba[] ABAS = {Aba.INICIO, Aba.FILMES, Aba.SERIES, Aba.SHOWS, Aba.PASTAS};
    private static final int[] PILULAS = {
            R.id.st_pilula_inicio, R.id.st_pilula_filmes, R.id.st_pilula_series,
            R.id.st_pilula_shows, R.id.st_pilula_pastas
    };

    private final TextView[] pilulas = new TextView[ABAS.length];
    private ViewGroup barraTopo;
    private ViewGroup conteudo;
    private View indicador;
    private ValueAnimator animacaoPilula;
    private final Handler relogio = new Handler(Looper.getMainLooper());
    /** Aba nova esperando o menu parar para ser montada. */
    private Runnable trocaPendente;
    private int corTexto;
    private int corTextoAtivo;
    private int abaAtiva = -1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.st_tv_principal);

        barraTopo = findViewById(R.id.st_barra_topo);
        conteudo = findViewById(R.id.st_conteudo);
        indicador = findViewById(R.id.st_pilula_indicador);
        corTexto = ContextCompat.getColor(this, R.color.st_texto_secundario);
        corTextoAtivo = ContextCompat.getColor(this, R.color.st_texto_escuro);

        for (int i = 0; i < PILULAS.length; i++) {
            final int indice = i;
            pilulas[i] = findViewById(PILULAS[i]);
            pilulas[i].setOnFocusChangeListener((v, temFoco) -> {
                if (temFoco) selecionarAba(indice);
            });
            pilulas[i].setOnClickListener(v -> selecionarAba(indice));
        }
        findViewById(R.id.st_botao_buscar).setOnClickListener(v -> Navegacao.abrirBusca(this));
        findViewById(R.id.st_botao_configuracoes).setOnClickListener(v -> Navegacao.abrirConfiguracoes(this));

        // Voltar com o foco no conteúdo: volta à pílula ativa; no topo, sai do app
        final OnBackPressedCallback voltarAoMenu = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                focarPilulaAtiva();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, voltarAoMenu);
        getWindow().getDecorView().getViewTreeObserver().addOnGlobalFocusChangeListener(
                (antes, depois) -> voltarAoMenu.setEnabled(depois != null && dentroDe(depois, conteudo)));

        int inicial = savedInstanceState == null ? indiceDaAba(Navegacao.abaDoIntent(getIntent()))
                : savedInstanceState.getInt(ESTADO_ABA, 0);
        // Fragmento restaurado pelo sistema: só marca a pílula
        boolean restaurado = savedInstanceState != null
                && getSupportFragmentManager().findFragmentById(R.id.st_conteudo) != null;
        if (restaurado) marcarPilula(inicial);
        else selecionarAba(inicial);
        pilulas[inicial].requestFocus();
    }

    /** OK numa pílula do Detalhe ou da Série: volta para cá, na aba pedida, com o foco nela. */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        Aba aba = Navegacao.abaDoIntent(intent);
        if (aba == null) return;
        int indice = indiceDaAba(aba);
        selecionarAba(indice);
        pilulas[indice].requestFocus();
    }

    private static int indiceDaAba(Aba aba) {
        for (int i = 0; i < ABAS.length; i++) {
            if (ABAS[i] == aba) return i;
        }
        return 0;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ESTADO_ABA, Math.max(abaAtiva, 0));
    }

    @Override
    protected void onPause() {
        // Troca ainda esperando: monta já (antes de o estado ser salvo)
        aplicarTrocaPendente();
        super.onPause();
    }

    private void selecionarAba(int indice) {
        if (indice == abaAtiva) return;
        int anterior = abaAtiva;
        marcarPilula(indice);
        if (trocaPendente != null) relogio.removeCallbacks(trocaPendente);
        trocaPendente = null;
        if (anterior < 0 || !Animacoes.ligadas(this)) {
            conteudo.animate().cancel();
            conteudo.setAlpha(1f);
            conteudo.setTranslationX(0f);
            trocarConteudo(indice);
            return;
        }
        // O conteúdo atual sai para o lado oposto ao do menu; o novo é montado quando o menu para
        // e entra do lado para onde ele andou
        final int sentido = indice > anterior ? 1 : -1;
        final float dp = getResources().getDisplayMetrics().density;
        conteudo.animate().cancel();
        conteudo.animate().alpha(0f).translationX(-sentido * 16 * dp).setDuration(Animacoes.CONTEUDO_SAI_MS)
                .setInterpolator(new AccelerateInterpolator()).start();
        trocaPendente = () -> {
            trocaPendente = null;
            trocarConteudo(indice);
            conteudo.animate().cancel();
            conteudo.setAlpha(0f);
            conteudo.setTranslationX(sentido * 28 * dp);
            conteudo.animate().alpha(1f).translationX(0f).setDuration(Animacoes.CONTEUDO_ENTRA_MS)
                    .setInterpolator(new DecelerateInterpolator(1.6f)).start();
        };
        relogio.postDelayed(trocaPendente, Animacoes.CONTEUDO_ESPERA_MS);
    }

    private void trocarConteudo(int indice) {
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.st_conteudo, Navegacao.conteudoDaAba(this, ABAS[indice]))
                .commitNow();
    }

    private void aplicarTrocaPendente() {
        if (trocaPendente == null) return;
        relogio.removeCallbacks(trocaPendente);
        trocaPendente.run();
    }

    /**
     * Pílula ativa: Inter 800 e texto escuro sobre a pílula branca, que vai da aba anterior até
     * ela esticando e encolhendo (a borda da frente chega antes da de trás), com as cores dos dois
     * textos passando junto; demais Inter 700. O indicador é medido depois do layout (a troca de
     * peso da fonte muda a largura).
     */
    private void marcarPilula(int indice) {
        final int anterior = abaAtiva;
        abaAtiva = indice;
        for (int i = 0; i < pilulas.length; i++) {
            boolean ativa = i == indice;
            pilulas[i].setSelected(ativa);
            pilulas[i].setTypeface(ResourcesCompat.getFont(this,
                    ativa ? R.font.st_inter_extrabold : R.font.st_inter_bold));
        }
        final View area = (View) indicador.getParent();
        area.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                area.getViewTreeObserver().removeOnPreDrawListener(this);
                moverIndicador(anterior, indice);
                return true;
            }
        });
        area.invalidate();
    }

    private void moverIndicador(int de, int para) {
        if (para != abaAtiva) return;
        TextView alvo = pilulas[para];
        final float xFim = alvo.getLeft();
        final int larguraFim = alvo.getWidth();
        final ViewGroup.LayoutParams lp = indicador.getLayoutParams();
        lp.height = alvo.getHeight();
        if (animacaoPilula != null) animacaoPilula.cancel();
        for (int i = 0; i < pilulas.length; i++) {
            if (i != de && i != para) pilulas[i].setTextColor(corTexto);
        }

        if (de < 0 || lp.width <= 0 || !Animacoes.ligadas(this)) {
            lp.width = larguraFim;
            indicador.setLayoutParams(lp);
            indicador.setTranslationX(xFim);
            alvo.setTextColor(corTextoAtivo);
            if (de >= 0 && de != para) pilulas[de].setTextColor(corTexto);
            return;
        }

        final float esquerdaIni = indicador.getTranslationX();
        final float direitaIni = esquerdaIni + lp.width;
        final float direitaFim = xFim + larguraFim;
        final boolean paraDireita = xFim > esquerdaIni;
        // Borda da frente rápida, borda de trás com atraso: a pílula estica no caminho e encolhe na aba
        final Interpolator frente = new DecelerateInterpolator(2.2f);
        final Interpolator tras = new AccelerateDecelerateInterpolator();
        final TextView saindo = de != para ? pilulas[de] : null;
        final int corSaindoIni = saindo == null ? corTexto : saindo.getCurrentTextColor();
        final int corAlvoIni = alvo.getCurrentTextColor();
        final ArgbEvaluator cores = new ArgbEvaluator();
        animacaoPilula = ValueAnimator.ofFloat(0f, 1f);
        animacaoPilula.setDuration(Animacoes.PILULA_MS);
        animacaoPilula.setInterpolator(new LinearInterpolator());
        animacaoPilula.addUpdateListener(a -> {
            float f = (float) a.getAnimatedValue();
            float fFrente = frente.getInterpolation(f);
            float fTras = tras.getInterpolation(f);
            float esquerda = esquerdaIni + (xFim - esquerdaIni) * (paraDireita ? fTras : fFrente);
            float direita = direitaIni + (direitaFim - direitaIni) * (paraDireita ? fFrente : fTras);
            indicador.setTranslationX(esquerda);
            lp.width = Math.max(1, Math.round(direita - esquerda));
            indicador.setLayoutParams(lp);
            alvo.setTextColor((int) cores.evaluate(fFrente, corAlvoIni, corTextoAtivo));
            if (saindo != null) saindo.setTextColor((int) cores.evaluate(fTras, corSaindoIni, corTexto));
        });
        animacaoPilula.start();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent evento) {
        if (evento.getAction() == KeyEvent.ACTION_DOWN) {
            View foco = getCurrentFocus();
            int tecla = evento.getKeyCode();
            if (foco != null && dentroDe(foco, barraTopo) && tecla == KeyEvent.KEYCODE_DPAD_DOWN) {
                // ↓ do topo: primeiro item do conteúdo (se a aba tiver algum), já montado
                aplicarTrocaPendente();
                conteudo.requestFocus(View.FOCUS_DOWN);
                return true;
            }
            if (foco != null && dentroDe(foco, conteudo)) {
                if (tecla == KeyEvent.KEYCODE_DPAD_UP) {
                    View proximo = FocusFinder.getInstance().findNextFocus(conteudo, foco, View.FOCUS_UP);
                    if (proximo == null) {
                        focarPilulaAtiva();
                        return true;
                    }
                }
            }
        }
        return super.dispatchKeyEvent(evento);
    }

    private void focarPilulaAtiva() {
        pilulas[Math.max(abaAtiva, 0)].requestFocus();
    }

    private static boolean dentroDe(View view, View grupo) {
        View atual = view;
        while (atual != null) {
            if (atual == grupo) return true;
            ViewParent pai = atual.getParent();
            atual = pai instanceof View ? (View) pai : null;
        }
        return false;
    }
}
