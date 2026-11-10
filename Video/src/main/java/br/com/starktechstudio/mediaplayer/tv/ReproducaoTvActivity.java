// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.tv;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

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
import br.com.starktechstudio.mediaplayer.comum.Preferencias;
import br.com.starktechstudio.mediaplayer.comum.Reproducao;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Capitulo;
import br.com.starktechstudio.mediaplayer.dados.Modelo.TipoFaixa;

/**
 * Player da TV (tv-player.dc.html), tocando o arquivo pelo motor do Nova ({@link Reproducao}): topo com o título e o
 * capítulo, barra com as marcas dos capítulos, "Próximo capítulo em…", botões Cap. anterior,
 * Pausar e Próx. capítulo (os de capítulo somem em arquivo sem capítulos; filme e desenho ganham marcas
 * de 10 em 10 minutos, que usam os mesmos botões), Áudio,
 * Legendas, Zoom (troca e guarda) e Capítulos (lista à direita; OK leva ao capítulo).
 * Barra: ↑ dos botões dá o foco a ela; ← / → movem 10 s e, segurando, 30 s e 60 s: o ponto anda na
 * tela e o motor só busca ao soltar (busca a cada repetição travaria em HD USB).
 * D13: os controles somem no tempo de Configurações, 5 s de fábrica (nunca com a barra em uso); com eles
 * ocultos, ← / → pulam o que está em Configurações, 10 s de fábrica, e qualquer outra tecla mostra os
 * controles; próxima / anterior do controle remoto = capítulo.
 * Voltar fecha a lista de capítulos; depois, recolhe os controles; com eles ocultos, o 1º só avisa e o 2º,
 * em até 3 s, vai para o Detalhe ou, em episódio, para a Série (D4 do plano 004; a posição fica salva).
 */
public class ReproducaoTvActivity extends AppCompatActivity {

    private static final long FADE_MS = 150;
    private static final long AVISO_PARADO_MS = 700;
    private static final long AVISO_SOME_MS = 250;
    private static final long CONFIRMAR_VOLTAR_MS = 3000;
    /** Soltou a barra e o motor não saiu da posição antiga por tanto tempo: o ponto volta a seguir o motor. */
    private static final long ESPERA_BUSCA_MS = 3000;

    private Reproducao reproducao;
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
    private Button ajustarLegenda;
    private BarraCapitulosView barra;
    private VerticalGridView lista;
    private CapituloAdapter adapter;
    private OnBackPressedCallback fecharPainel;

    private final Handler relogio = new Handler(Looper.getMainLooper());
    private final Runnable ocultar = this::ocultarControles;
    private boolean visiveis = true;
    private boolean tocandoMostrado;
    /** Barra em uso: a posição escolhida com ← / → (segundos), ainda sem busca; -1 = fora de uso. */
    private int alvoNaBarra = -1;
    private long ultimoPassoNaBarra;
    /** Barra solta: enquanto busca, o motor repete a posição antiga, então o ponto fica neste alvo; -1 = nada. */
    private int alvoSolto = -1;
    private int posicaoAoSoltar;
    private long limiteAlvoSolto;
    /** Hora do 1º Voltar (o aviso); 0 = nenhum. */
    private long primeiroVoltar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        reproducao = Reproducao.abrir(this, getIntent().getStringExtra(Navegacao.EXTRA_ID),
                getIntent().getIntExtra(Navegacao.EXTRA_CAPITULO, Navegacao.DE_ONDE_PAROU),
                getString(R.string.st_temporada_episodio));
        if (reproducao == null) {
            naoAbriu();
            return;
        }
        setContentView(R.layout.st_tv_reproducao);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        reproducao.ligar(this, findViewById(R.id.st_rep_superficie), this::naoAbriu);

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
        Button zoom = findViewById(R.id.st_rep_zoom);
        ajustarLegenda = findViewById(R.id.st_rep_ajustar_legenda);

        ((TextView) findViewById(R.id.st_rep_titulo)).setText(reproducao.titulo);
        duracao.setText(PecasReproducao.tempo(reproducao.duracao));
        barra.definirTv(true);
        barra.definirMarcas(PecasReproducao.marcas(reproducao));
        int icone = getResources().getDimensionPixelSize(R.dimen.st_tv_rep_lado_botao_icone);
        Icones.noTexto(audio, R.drawable.st_ic_audio, icone);
        Icones.noTexto(legendas, R.drawable.st_ic_legendas, icone);
        Icones.noTexto(zoom, R.drawable.st_ic_zoom, icone);
        Icones.noTexto(botaoCapitulos, R.drawable.st_ic_capitulos, icone);
        Icones.noTexto(ajustarLegenda, R.drawable.st_ic_ajustar, icone);
        ajustarLegenda.setOnClickListener(v -> Navegacao.abrirAjusteLegenda(this, reproducao.id));

        // Sem capítulos: só Pausar no centro (INTERFACE.md §4)
        boolean comCapitulos = reproducao.temCapitulos();
        findViewById(R.id.st_rep_grupo_cap_anterior).setVisibility(comCapitulos ? View.VISIBLE : View.GONE);
        findViewById(R.id.st_rep_grupo_cap_proximo).setVisibility(comCapitulos ? View.VISIBLE : View.GONE);
        botaoCapitulos.setVisibility(comCapitulos ? View.VISIBLE : View.GONE);

        findViewById(R.id.st_rep_cap_anterior).setOnClickListener(v -> capituloAnterior());
        pausar.setOnClickListener(v -> reproducao.pausarOuContinuar());
        findViewById(R.id.st_rep_cap_proximo).setOnClickListener(v -> proximoCapitulo());
        audio.setOnClickListener(v -> Navegacao.abrirFaixas(this, reproducao.id, TipoFaixa.AUDIO));
        legendas.setOnClickListener(v -> Navegacao.abrirFaixas(this, reproducao.id, TipoFaixa.LEGENDA));
        zoom.setOnClickListener(v -> trocarZoom());
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
        reproducao.definirAoMudarCapitulos(this::mostrarCapitulos);
        reproducao.definirAoPassarEpisodio(this::abrirEpisodio);

        // O último registrado vem primeiro: com a lista aberta, Voltar só a fecha
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                voltar();
            }
        });
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
    protected void onPause() {
        if (reproducao != null) {
            soltarBarra();
            reproducao.gravarPosicao();
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (reproducao != null) reproducao.parar();
        relogio.removeCallbacks(ocultar);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (reproducao != null) reproducao.liberar();
        super.onDestroy();
    }

    /** Arquivo que não abre (HD desligado, arquivo estragado): avisa e volta. */
    private void naoAbriu() {
        Toast.makeText(this, R.string.st_video_nao_abriu, Toast.LENGTH_LONG).show();
        finish();
    }

    /**
     * Fim do episódio com "Próximo episódio automático" ligado: esta tela reabre com o seguinte.
     * recreate() solta o motor antes de abrir o novo (o decodificador da TV pode ser um só) e mantém
     * o que está atrás; o intent trocado aqui é o que o onCreate novo lê.
     */
    private void abrirEpisodio(String id) {
        if (isFinishing()) return;
        setIntent(new Intent(getIntent()).putExtra(Navegacao.EXTRA_ID, id)
                .putExtra(Navegacao.EXTRA_CAPITULO, Navegacao.DE_ONDE_PAROU));
        recreate();
    }

    private void atualizar() {
        int posicao = posicaoNaTela();
        tempo.setText(PecasReproducao.tempo(posicao));
        duracao.setText(PecasReproducao.tempo(reproducao.duracao));
        barra.definirFracao(posicao / (float) reproducao.duracao);
        sub.setText(PecasReproducao.linhaCapitulo(this, reproducao));
        sub.setVisibility(sub.getText().length() == 0 ? View.GONE : View.VISIBLE);
        proximo.setText(PecasReproducao.textoProximo(this, reproducao));
        // Legenda rápida (1.5): só quando o vídeo tem legenda
        int ajustar = reproducao.temLegenda() ? View.VISIBLE : View.GONE;
        if (ajustarLegenda.getVisibility() != ajustar) ajustarLegenda.setVisibility(ajustar);
        boolean tocando = reproducao.estaTocando();
        if (tocando != tocandoMostrado || rotuloPausar.getText().length() == 0) {
            tocandoMostrado = tocando;
            pausar.setImageResource(tocando ? R.drawable.st_ic_pausar : R.drawable.st_ic_assistir);
            pausar.setContentDescription(getString(tocando ? R.string.st_pausar : R.string.st_continuar));
            rotuloPausar.setText(tocando ? R.string.st_pausar : R.string.st_continuar);
        }
        adapter.definirAtual(numeroAtual());
    }

    /** Capítulos lidos depois de abrir (vídeo ainda não varrido): marcas, botões e lista. */
    private void mostrarCapitulos() {
        barra.definirMarcas(PecasReproducao.marcas(reproducao));
        int visivel = reproducao.temCapitulos() ? View.VISIBLE : View.GONE;
        findViewById(R.id.st_rep_grupo_cap_anterior).setVisibility(visivel);
        findViewById(R.id.st_rep_grupo_cap_proximo).setVisibility(visivel);
        botaoCapitulos.setVisibility(visivel);
        adapter.atualizar(reproducao.capitulos, numeroAtual());
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

    private void trocarZoom() {
        String nome = getResources().getStringArray(R.array.st_zoom_nomes)[reproducao.trocarZoom()];
        mostrarAviso(getString(R.string.st_zoom_aviso, nome));
    }

    private void abrirPainel() {
        painel.setVisibility(View.VISIBLE);
        botaoCapitulos.setBackgroundResource(R.drawable.st_botao_primario);
        // A barra agora fica no caminho: ↑ de Capítulos continua indo à lista
        botaoCapitulos.setNextFocusUpId(R.id.st_rep_lista_capitulos);
        fecharPainel.setEnabled(true);
        lista.setSelectedPosition(Math.max(0, numeroAtual() - 1));
        lista.requestFocus();
    }

    private void fecharPainel() {
        boolean focoNoPainel = painel.hasFocus();
        painel.setVisibility(View.GONE);
        botaoCapitulos.setBackgroundResource(R.drawable.st_botao_secundario);
        botaoCapitulos.setNextFocusUpId(View.NO_ID);
        fecharPainel.setEnabled(false);
        primeiroVoltar = 0;
        if (focoNoPainel) botaoCapitulos.requestFocus();
    }

    // ---- Voltar duas vezes (D4 do plano 004) ----

    private void voltar() {
        // Com a barra na tela, Voltar só recolhe; quem sai é o vídeo sem controles
        if (visiveis) {
            relogio.removeCallbacks(ocultar);
            ocultarControles();
            primeiroVoltar = 0;
            return;
        }
        long agora = SystemClock.uptimeMillis();
        if (primeiroVoltar > 0 && agora - primeiroVoltar <= CONFIRMAR_VOLTAR_MS) {
            sair();
            return;
        }
        primeiroVoltar = agora;
        mostrarAviso(getString(R.string.st_voltar_de_novo), CONFIRMAR_VOLTAR_MS - AVISO_SOME_MS);
    }

    /**
     * Para o Detalhe ou, em episódio, para a Série. Na TV, a tela que está atrás do player é sempre a
     * deste título (ou a principal e a busca, que não contam): já atrás, CLEAR_TOP com SINGLE_TOP só
     * fecha o player, sem outra cópia; não estando, ela abre e o player fecha.
     */
    private void sair() {
        boolean episodio = reproducao.idSerie != null;
        startActivity(new Intent(this, episodio ? SerieTvActivity.class : DetalheTvActivity.class)
                .putExtra(Navegacao.EXTRA_ID, episodio ? reproducao.idSerie : reproducao.id)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        finish();
    }

    // ---- Barra selecionável ----

    private static boolean setaDoLado(int tecla) {
        return tecla == KeyEvent.KEYCODE_DPAD_LEFT || tecla == KeyEvent.KEYCODE_DPAD_RIGHT;
    }

    /** Posição mostrada: a escolhida na barra, a que acabou de ser pedida ao motor ou a do motor. */
    private int posicaoNaTela() {
        if (alvoNaBarra >= 0) return alvoNaBarra;
        int doMotor = reproducao.posicao();
        if (alvoSolto < 0) return doMotor;
        if (Math.abs(doMotor - posicaoAoSoltar) <= 1 && SystemClock.uptimeMillis() < limiteAlvoSolto) return alvoSolto;
        alvoSolto = -1;
        return doMotor;
    }

    /** Um passo na tela, sem busca; segurando, no máximo um a cada {@link Reproducao#BARRA_INTERVALO_MS}. */
    private void moverNaBarra(int tecla, KeyEvent evento) {
        long agora = evento.getEventTime();
        if (evento.getRepeatCount() > 0 && agora - ultimoPassoNaBarra < Reproducao.BARRA_INTERVALO_MS) return;
        ultimoPassoNaBarra = agora;
        int passo = Reproducao.passoDaBarra(agora - evento.getDownTime());
        alvoNaBarra = Reproducao.alvoDaBarra(posicaoNaTela(), tecla == KeyEvent.KEYCODE_DPAD_RIGHT ? passo : -passo,
                reproducao.duracao);
        atualizar();
    }

    /** Soltou a seta: agora, sim, a busca; a contagem para ocultar os controles recomeça. */
    private void soltarBarra() {
        if (alvoNaBarra < 0) return;
        alvoSolto = alvoNaBarra;
        alvoNaBarra = -1;
        posicaoAoSoltar = reproducao.posicao();
        limiteAlvoSolto = SystemClock.uptimeMillis() + ESPERA_BUSCA_MS;
        reproducao.irPara(alvoSolto);
        agendarOcultar();
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
                    pular(1);
                    return true;
                case KeyEvent.KEYCODE_MEDIA_REWIND:
                    pular(-1);
                    return true;
                default:
                    break;
            }
            if (!visiveis) {
                if (tecla == KeyEvent.KEYCODE_DPAD_LEFT) {
                    pular(-1);
                    return true;
                }
                if (tecla == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    pular(1);
                    return true;
                }
                if (teclaDeControle(tecla)) {
                    mostrarControles();
                    return true;
                }
            } else {
                agendarOcultar();
                if (barra.isFocused() && setaDoLado(tecla)) {
                    moverNaBarra(tecla, evento);
                    return true;
                }
            }
        } else if (evento.getAction() == KeyEvent.ACTION_UP && alvoNaBarra >= 0 && setaDoLado(evento.getKeyCode())) {
            soltarBarra();
            return true;
        } else if (!visiveis && teclaDeControle(evento.getKeyCode())) {
            // O soltar da tecla que só mostrou os controles não aperta nada
            return true;
        }
        return super.dispatchKeyEvent(evento);
    }

    /** O pulo de Configurações: {@code sentido} 1 avança, -1 volta. O número aparece com os controles ocultos. */
    private void pular(int sentido) {
        int segundos = Preferencias.puloSegundos(this);
        reproducao.pular(sentido * segundos);
        if (!visiveis) mostrarAviso(getString(sentido > 0 ? R.string.st_pulo_mais : R.string.st_pulo_menos, segundos));
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
        relogio.postDelayed(ocultar, Preferencias.tempoControlesMs(this));
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
        if (alvoNaBarra >= 0) {
            agendarOcultar();
            return;
        }
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

    /** O pulo ("−10 s", "+10 s") ou o capítulo novo no centro da tela, com os controles ocultos; o zoom escolhido. */
    private void mostrarAviso(String texto) {
        mostrarAviso(texto, AVISO_PARADO_MS);
    }

    private void mostrarAviso(String texto, long paradoMs) {
        aviso.setText(texto);
        aviso.animate().cancel();
        aviso.setAlpha(1f);
        aviso.animate().alpha(0f).setStartDelay(paradoMs).setDuration(Animacoes.ligadas(this) ? AVISO_SOME_MS : 0).start();
    }
}
