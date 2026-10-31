// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.tv;

import android.os.Bundle;
import android.view.FocusFinder;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.archos.mediacenter.video.R;

import br.com.starktechstudio.mediaplayer.comum.Marca;
import br.com.starktechstudio.mediaplayer.comum.Navegacao;
import br.com.starktechstudio.mediaplayer.comum.Navegacao.Aba;

/**
 * Tela principal da TV (Main.dc.html): barra do topo com a marca, as pílulas
 * Início, Filmes, Séries, Shows e Pastas e os botões Buscar e Configurações; abaixo,
 * o conteúdo da aba. ← / → anda entre as pílulas (a pílula em foco vira a aba ativa),
 * ↓ desce ao conteúdo, ↑ no topo do conteúdo volta à pílula ativa; Voltar no conteúdo
 * também volta à pílula ativa.
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
    private int abaAtiva = -1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.st_tv_principal);

        barraTopo = findViewById(R.id.st_barra_topo);
        conteudo = findViewById(R.id.st_conteudo);
        Marca.aplicar(findViewById(R.id.st_marca));

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

        int inicial = savedInstanceState == null ? 0 : savedInstanceState.getInt(ESTADO_ABA, 0);
        // Fragmento restaurado pelo sistema: só marca a pílula
        boolean restaurado = savedInstanceState != null
                && getSupportFragmentManager().findFragmentById(R.id.st_conteudo) != null;
        if (restaurado) marcarPilula(inicial);
        else selecionarAba(inicial);
        pilulas[inicial].requestFocus();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ESTADO_ABA, Math.max(abaAtiva, 0));
    }

    private void selecionarAba(int indice) {
        if (indice == abaAtiva) return;
        marcarPilula(indice);
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.st_conteudo, Navegacao.conteudoDaAba(ABAS[indice]))
                .commitNow();
    }

    /** Pílula ativa: fundo branco (selecionada) e Inter 800; demais Inter 700. */
    private void marcarPilula(int indice) {
        abaAtiva = indice;
        for (int i = 0; i < pilulas.length; i++) {
            boolean ativa = i == indice;
            pilulas[i].setSelected(ativa);
            pilulas[i].setTypeface(ResourcesCompat.getFont(this,
                    ativa ? R.font.st_inter_extrabold : R.font.st_inter_bold));
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent evento) {
        if (evento.getAction() == KeyEvent.ACTION_DOWN) {
            View foco = getCurrentFocus();
            int tecla = evento.getKeyCode();
            if (foco != null && dentroDe(foco, barraTopo) && tecla == KeyEvent.KEYCODE_DPAD_DOWN) {
                // ↓ do topo: primeiro item do conteúdo (se a aba tiver algum)
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
