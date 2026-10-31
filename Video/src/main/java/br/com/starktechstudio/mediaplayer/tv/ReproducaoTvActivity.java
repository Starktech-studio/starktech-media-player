// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.tv;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.leanback.widget.VerticalGridView;

import com.archos.mediacenter.video.R;

import br.com.starktechstudio.mediaplayer.comum.Animacoes;
import br.com.starktechstudio.mediaplayer.comum.BarraCapitulosView;
import br.com.starktechstudio.mediaplayer.comum.CapituloAdapter;
import br.com.starktechstudio.mediaplayer.comum.Icones;
import br.com.starktechstudio.mediaplayer.comum.Navegacao;
import br.com.starktechstudio.mediaplayer.comum.PecasReproducao;
import br.com.starktechstudio.mediaplayer.comum.ReproducaoSimulada;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Capitulo;
import br.com.starktechstudio.mediaplayer.dados.Modelo.TipoFaixa;

/**
 * Player da TV (tv-player.dc.html), com a reprodução simulada da F1: topo com o título e o
 * capítulo, barra com as marcas dos capítulos, "Próximo capítulo em…", botões Cap. anterior,
 * −10 s, Pausar, +10 s e Próx. capítulo (os de capítulo somem em arquivo sem capítulos), Áudio,
 * Legendas e Capítulos (lista à direita; OK leva ao capítulo).
 * D13: os controles somem após 5 s; com eles ocultos, ← / → = −10 s / +10 s e qualquer outra tecla
 * mostra os controles; próxima / anterior do controle remoto = capítulo. Voltar fecha a lista de
 * capítulos ou sai do player (a posição fica salva).
 */
public class ReproducaoTvActivity extends AppCompatActivity {

    private static final long OCULTAR_MS = 5000;
    private static final long FADE_MS = 150;

    private ReproducaoSimulada reproducao;
    private View topo;
    private View painel;
    private View controles;
    private TextView sub;
    private TextView tempo;
    private TextView duracao;
    private TextView proximo;
    private TextView rotuloPausar;
    private TextView aviso;
    private ImageButton pausar;
    private Button botaoCapitulos;
    private BarraCapitulosView barra;
    private VerticalGridView lista;
    private CapituloAdapter adapter;
    private OnBackPressedCallback fecharPainel;

    private final Handler relogio = new Handler(Looper.getMainLooper());
    private final Runnable ocultar = this::ocultarControles;
    private boolean visiveis = true;
    private boolean tocandoMostrado;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        reproducao = ReproducaoSimulada.abrir(this, getIntent().getStringExtra(Navegacao.EXTRA_ID),
                getIntent().getIntExtra(Navegacao.EXTRA_CAPITULO, Navegacao.DE_ONDE_PAROU),
                getString(R.string.st_temporada_episodio));
        if (reproducao == null) {
            finish();
            return;
        }
        setContentView(R.layout.st_tv_reproducao);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        topo = findViewById(R.id.st_rep_topo);
        painel = findViewById(R.id.st_rep_painel);
        controles = findViewById(R.id.st_rep_controles);
        sub = findViewById(R.id.st_rep_sub);
        tempo = findViewById(R.id.st_rep_tempo);
        duracao = findViewById(R.id.st_rep_duracao);
        proximo = findViewById(R.id.st_rep_proximo);
        rotuloPausar = findViewById(R.id.st_rep_rotulo_pausar);
        aviso = findViewById(R.id.st_rep_aviso);
        pausar = findViewById(R.id.st_rep_pausar);
        botaoCapitulos = findViewById(R.id.st_rep_capitulos);
        barra = findViewById(R.id.st_rep_barra);
        lista = findViewById(R.id.st_rep_lista_capitulos);
        Button audio = findViewById(R.id.st_rep_audio);
        Button legendas = findViewById(R.id.st_rep_legendas);

        ((TextView) findViewById(R.id.st_rep_titulo)).setText(reproducao.titulo);
        duracao.setText(PecasReproducao.tempo(reproducao.duracao));
        barra.definirTv(true);
        barra.definirMarcas(PecasReproducao.marcas(reproducao));
        int icone = getResources().getDimensionPixelSize(R.dimen.st_tv_rep_lado_botao_icone);
        Icones.noTexto(audio, R.drawable.st_ic_audio, icone);
        Icones.noTexto(legendas, R.drawable.st_ic_legendas, icone);
        Icones.noTexto(botaoCapitulos, R.drawable.st_ic_capitulos, icone);

        // Sem capítulos: só −10 s / +10 s (INTERFACE.md §4)
        boolean comCapitulos = reproducao.temCapitulos();
        findViewById(R.id.st_rep_grupo_cap_anterior).setVisibility(comCapitulos ? View.VISIBLE : View.GONE);
        findViewById(R.id.st_rep_grupo_cap_proximo).setVisibility(comCapitulos ? View.VISIBLE : View.GONE);
        botaoCapitulos.setVisibility(comCapitulos ? View.VISIBLE : View.GONE);

        findViewById(R.id.st_rep_cap_anterior).setOnClickListener(v -> capituloAnterior());
        findViewById(R.id.st_rep_menos_10).setOnClickListener(v -> reproducao.pular(-ReproducaoSimulada.PULO_SEGUNDOS));
        pausar.setOnClickListener(v -> reproducao.pausarOuContinuar());
        findViewById(R.id.st_rep_mais_10).setOnClickListener(v -> reproducao.pular(ReproducaoSimulada.PULO_SEGUNDOS));
        findViewById(R.id.st_rep_cap_proximo).setOnClickListener(v -> proximoCapitulo());
        audio.setOnClickListener(v -> Navegacao.abrirFaixas(this, reproducao.id, TipoFaixa.AUDIO));
        legendas.setOnClickListener(v -> Navegacao.abrirFaixas(this, reproducao.id, TipoFaixa.LEGENDA));
        botaoCapitulos.setOnClickListener(v -> {
            if (painel.getVisibility() == View.VISIBLE) fecharPainel();
            else abrirPainel();
        });

        adapter = new CapituloAdapter(R.layout.st_tv_item_capitulo_reproducao, true, c -> {
            reproducao.irParaCapitulo(c);
            fecharPainel();
            pausar.requestFocus();
        });
        lista.setAdapter(adapter);
        adapter.atualizar(reproducao.capitulos, numeroAtual());

        fecharPainel = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                fecharPainel();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, fecharPainel);

        reproducao.definirOuvinte(this::atualizar);
        atualizar();
        pausar.requestFocus();
        agendarOcultar();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (reproducao != null) reproducao.iniciar();
    }

    @Override
    protected void onStop() {
        if (reproducao != null) reproducao.parar();
        relogio.removeCallbacks(ocultar);
        super.onStop();
    }

    private void atualizar() {
        tempo.setText(PecasReproducao.tempo(reproducao.posicao()));
        barra.definirFracao(reproducao.posicao() / (float) reproducao.duracao);
        sub.setText(PecasReproducao.linhaCapitulo(this, reproducao));
        sub.setVisibility(sub.getText().length() == 0 ? View.GONE : View.VISIBLE);
        proximo.setText(PecasReproducao.textoProximo(this, reproducao));
        boolean tocando = reproducao.estaTocando();
        if (tocando != tocandoMostrado || rotuloPausar.getText().length() == 0) {
            tocandoMostrado = tocando;
            pausar.setImageResource(tocando ? R.drawable.st_ic_pausar : R.drawable.st_ic_assistir);
            pausar.setContentDescription(getString(tocando ? R.string.st_pausar : R.string.st_continuar));
            rotuloPausar.setText(tocando ? R.string.st_pausar : R.string.st_continuar);
        }
        adapter.definirAtual(numeroAtual());
    }

    private int numeroAtual() {
        Capitulo atual = reproducao.capituloAtual();
        return atual == null ? 0 : atual.numero;
    }

    private void capituloAnterior() {
        Capitulo c = reproducao.irParaCapituloAnterior();
        if (c != null && !visiveis) mostrarAviso(PecasReproducao.nomeCapitulo(this, c));
    }

    private void proximoCapitulo() {
        Capitulo c = reproducao.irParaProximoCapitulo();
        if (c != null && !visiveis) mostrarAviso(PecasReproducao.nomeCapitulo(this, c));
    }

    private void abrirPainel() {
        painel.setVisibility(View.VISIBLE);
        botaoCapitulos.setBackgroundResource(R.drawable.st_botao_primario);
        fecharPainel.setEnabled(true);
        lista.setSelectedPosition(Math.max(0, numeroAtual() - 1));
        lista.requestFocus();
    }

    private void fecharPainel() {
        boolean focoNoPainel = painel.hasFocus();
        painel.setVisibility(View.GONE);
        botaoCapitulos.setBackgroundResource(R.drawable.st_botao_secundario);
        fecharPainel.setEnabled(false);
        if (focoNoPainel) botaoCapitulos.requestFocus();
    }

    // ---- Controles que somem (D13) ----

    @Override
    public boolean dispatchKeyEvent(KeyEvent evento) {
        if (evento.getAction() == KeyEvent.ACTION_DOWN) {
            int tecla = evento.getKeyCode();
            switch (tecla) {
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    boolean pausa = tecla == KeyEvent.KEYCODE_MEDIA_PAUSE;
                    boolean toca = tecla == KeyEvent.KEYCODE_MEDIA_PLAY;
                    if ((!pausa || reproducao.estaTocando()) && (!toca || !reproducao.estaTocando())) {
                        reproducao.pausarOuContinuar();
                    }
                    mostrarControles();
                    return true;
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    proximoCapitulo();
                    return true;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    capituloAnterior();
                    return true;
                case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                    reproducao.pular(ReproducaoSimulada.PULO_SEGUNDOS);
                    if (!visiveis) mostrarAviso(getString(R.string.st_mais_10));
                    return true;
                case KeyEvent.KEYCODE_MEDIA_REWIND:
                    reproducao.pular(-ReproducaoSimulada.PULO_SEGUNDOS);
                    if (!visiveis) mostrarAviso(getString(R.string.st_menos_10));
                    return true;
                default:
                    break;
            }
            if (!visiveis) {
                if (tecla == KeyEvent.KEYCODE_DPAD_LEFT) {
                    reproducao.pular(-ReproducaoSimulada.PULO_SEGUNDOS);
                    mostrarAviso(getString(R.string.st_menos_10));
                    return true;
                }
                if (tecla == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    reproducao.pular(ReproducaoSimulada.PULO_SEGUNDOS);
                    mostrarAviso(getString(R.string.st_mais_10));
                    return true;
                }
                if (teclaDeControle(tecla)) {
                    mostrarControles();
                    return true;
                }
            } else {
                agendarOcultar();
            }
        } else if (!visiveis && teclaDeControle(evento.getKeyCode())) {
            // O soltar da tecla que só mostrou os controles não aperta nada
            return true;
        }
        return super.dispatchKeyEvent(evento);
    }

    /** Setas e confirmar: com os controles ocultos, só os mostram. Voltar e volume seguem o caminho normal. */
    private static boolean teclaDeControle(int tecla) {
        switch (tecla) {
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
            case KeyEvent.KEYCODE_BUTTON_A:
            case KeyEvent.KEYCODE_SPACE:
                return true;
            default:
                return false;
        }
    }

    private void agendarOcultar() {
        relogio.removeCallbacks(ocultar);
        relogio.postDelayed(ocultar, OCULTAR_MS);
    }

    private void mostrarControles() {
        if (!visiveis) {
            visiveis = true;
            fade(1f);
            if (getCurrentFocus() == null) pausar.requestFocus();
        }
        agendarOcultar();
    }

    private void ocultarControles() {
        if (!visiveis) return;
        visiveis = false;
        fade(0f);
    }

    /** Topo, lista de capítulos e controles aparecem ou somem juntos; o foco fica onde estava. */
    private void fade(float alvo) {
        boolean animar = Animacoes.ligadas(this);
        for (View v : new View[]{topo, painel, controles}) {
            v.animate().cancel();
            if (animar) v.animate().alpha(alvo).setDuration(FADE_MS).start();
            else v.setAlpha(alvo);
        }
    }

    /** "−10 s", "+10 s" ou o capítulo novo no centro da tela, com os controles ocultos. */
    private void mostrarAviso(String texto) {
        aviso.setText(texto);
        aviso.animate().cancel();
        aviso.setAlpha(1f);
        aviso.animate().alpha(0f).setStartDelay(700).setDuration(Animacoes.ligadas(this) ? 250 : 0).start();
    }
}
