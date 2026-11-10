// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.tablet;

import android.app.ActivityManager;
import android.app.PictureInPictureParams;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.TextAppearanceSpan;
import android.util.Rational;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.archos.mediacenter.video.R;

import br.com.starktechstudio.mediaplayer.comum.Animacoes;
import br.com.starktechstudio.mediaplayer.comum.BarraCapitulosView;
import br.com.starktechstudio.mediaplayer.comum.BordasSistema;
import br.com.starktechstudio.mediaplayer.comum.CapituloAdapter;
import br.com.starktechstudio.mediaplayer.comum.Icones;
import br.com.starktechstudio.mediaplayer.comum.Navegacao;
import br.com.starktechstudio.mediaplayer.comum.PecasReproducao;
import br.com.starktechstudio.mediaplayer.comum.Preferencias;
import br.com.starktechstudio.mediaplayer.comum.Reproducao;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Capitulo;
import br.com.starktechstudio.mediaplayer.dados.Modelo.TipoFaixa;

/**
 * Player do tablet (tablet-player.dc.html), tocando o arquivo pelo motor do Nova ({@link Reproducao}): topo
 * com Voltar, título grande e o capítulo; embaixo, barra (tocar ou arrastar muda a posição),
 * Capítulos (abre a faixa de capítulos, que arrasta para o lado, sem "Ver todos"), Áudio e Legendas
 * à esquerda; Cap. anterior, Pausar, Próx. capítulo no centro (os de capítulo somem sem capítulos;
 * filme e desenho ganham marcas de 10 em 10 minutos, que usam os mesmos botões;
 * o pulo de Configurações, 10 s de fábrica, só com dois toques nas laterais); "Próximo capítulo em…"
 * e Expandir à direita.
 * Um toque mostra ou esconde os controles; à vista, somem no tempo de Configurações (5 s de fábrica)
 * depois de começar a tocar ou do último toque (botões, barra e faixa de capítulos contam) e ficam
 * enquanto Áudio / Legendas estão abertos.
 * Tela cheia junto com os controles: some com eles a barra do sistema, volta com eles; Expandir
 * entra na hora. Saindo do player com o vídeo tocando (Home, outro app), o vídeo segue na miniatura
 * (picture-in-picture), com o botão de expandir do sistema no centro; Voltar fecha o player, como
 * sempre. Botões de mídia de fora do app em {@link BotoesDeMidia}; gestos sem dica em
 * {@link GestosReproducao}.
 */
public class ReproducaoTabletActivity extends AppCompatActivity implements GestosReproducao.Acoes {

    private static final String ESTADO_PASSOU_PELA_MINIATURA = "passouPelaMiniatura";
    private static final long FADE_MS = 150;
    /** Expandir com a tela já cheia. */
    private static final float EXPANDIR_APAGADO = 0.4f;
    /** Limites do Android para a miniatura (largura ÷ altura), um pouco para dentro. */
    private static final float MINIATURA_MENOR = 0.42f;
    private static final float MINIATURA_MAIOR = 2.38f;

    /** O player aberto: na miniatura, abrir outro vídeo fecha este (um vídeo de cada vez). */
    @Nullable
    private static ReproducaoTabletActivity aberta;

    private Reproducao reproducao;
    private BotoesDeMidia botoes;
    private WindowInsetsControllerCompat barras;
    private View topo;
    private View controles;
    private View faixa;
    private TextView sub;
    private TextView tempo;
    private TextView duracao;
    private TextView proximo;
    private TextView aviso;
    private TextView botaoCapitulos;
    private TextView ajustarLegenda;
    private TextView expandir;
    private ImageButton pausar;
    private BarraCapitulosView barra;
    private CapituloAdapter adapter;

    private final Handler relogio = new Handler(Looper.getMainLooper());
    private final Runnable ocultar = this::ocultarControles;
    private boolean visiveis = true;
    private boolean tocandoMostrado = true;
    private int inicioArrasto = -1;
    /** O vídeo já começou: a partir daí conta o tempo para os controles sumirem. */
    private boolean comecou;
    private boolean naMiniatura;
    private boolean passouPelaMiniatura;
    private boolean abrindoFaixas;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        passouPelaMiniatura = savedInstanceState != null && savedInstanceState.getBoolean(ESTADO_PASSOU_PELA_MINIATURA);
        // O que ficou na miniatura fecha e grava a posição antes de abrir o novo
        if (aberta != null) aberta.fecharMiniatura();
        reproducao = Reproducao.abrir(this, getIntent().getStringExtra(Navegacao.EXTRA_ID),
                getIntent().getIntExtra(Navegacao.EXTRA_CAPITULO, Navegacao.DE_ONDE_PAROU),
                getString(R.string.st_temporada_episodio));
        if (reproducao == null) {
            naoAbriu();
            return;
        }
        aberta = this;
        setContentView(R.layout.st_tablet_reproducao);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        reproducao.ligar(this, findViewById(R.id.st_rep_superficie), this::naoAbriu);
        // Desenha sob as barras do sistema: o vídeo não muda de tamanho quando elas somem ou voltam.
        // Em tela cheia, voltam com um arrasto da borda; a de status fica sempre escondida no tablet
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        barras = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        barras.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        barras.hide(WindowInsetsCompat.Type.statusBars());

        topo = findViewById(R.id.st_rep_topo);
        controles = findViewById(R.id.st_rep_controles);
        BordasSistema.aplicar(topo, true, true, true, false);
        BordasSistema.aplicar(controles, true, false, true, true);
        faixa = findViewById(R.id.st_rep_faixa);
        sub = findViewById(R.id.st_rep_sub);
        tempo = findViewById(R.id.st_rep_tempo);
        duracao = findViewById(R.id.st_rep_duracao);
        proximo = findViewById(R.id.st_rep_proximo);
        aviso = findViewById(R.id.st_rep_aviso);
        botaoCapitulos = findViewById(R.id.st_rep_capitulos);
        expandir = findViewById(R.id.st_rep_expandir);
        pausar = findViewById(R.id.st_rep_pausar);
        barra = findViewById(R.id.st_rep_barra);
        TextView capAnterior = findViewById(R.id.st_rep_cap_anterior);
        TextView capProximo = findViewById(R.id.st_rep_cap_proximo);
        TextView audio = findViewById(R.id.st_rep_audio);
        TextView legendas = findViewById(R.id.st_rep_legendas);
        ajustarLegenda = findViewById(R.id.st_rep_ajustar_legenda);

        ((TextView) findViewById(R.id.st_rep_titulo)).setText(reproducao.titulo);
        duracao.setText(PecasReproducao.tempo(reproducao.duracao));
        barra.definirTv(false);
        barra.definirMarcas(PecasReproducao.marcas(reproducao));
        barra.definirAoArrastar((fracao, soltou) -> {
            reproducao.irPara(Math.round(fracao * reproducao.duracao));
            agendarOcultar();
        });

        int icone = getResources().getDimensionPixelSize(R.dimen.st_tablet_rep_botao_icone);
        int branco = ContextCompat.getColor(this, R.color.st_texto);
        Icones.acimaDoTexto(capAnterior, R.drawable.st_ic_cap_anterior, icone, branco);
        Icones.acimaDoTexto(capProximo, R.drawable.st_ic_cap_proximo, icone, branco);
        Icones.acimaDoTexto(audio, R.drawable.st_ic_audio, icone, branco);
        Icones.acimaDoTexto(legendas, R.drawable.st_ic_legendas, icone, branco);
        Icones.acimaDoTexto(ajustarLegenda, R.drawable.st_ic_ajustar, icone, branco);
        Icones.acimaDoTexto(botaoCapitulos, R.drawable.st_ic_capitulos, icone, branco);
        Icones.acimaDoTexto(expandir, R.drawable.st_ic_expandir, icone, branco);

        // Sem capítulos: só Pausar no centro (INTERFACE.md §4)
        boolean comCapitulos = reproducao.temCapitulos();
        capAnterior.setVisibility(comCapitulos ? View.VISIBLE : View.GONE);
        capProximo.setVisibility(comCapitulos ? View.VISIBLE : View.GONE);
        botaoCapitulos.setVisibility(comCapitulos ? View.VISIBLE : View.GONE);

        findViewById(R.id.st_rep_voltar).setOnClickListener(v -> finish());
        capAnterior.setOnClickListener(v -> comando(() -> reproducao.irParaCapituloAnterior()));
        pausar.setOnClickListener(v -> comando(reproducao::pausarOuContinuar));
        capProximo.setOnClickListener(v -> comando(() -> reproducao.irParaProximoCapitulo()));
        audio.setOnClickListener(v -> abrirFaixas(TipoFaixa.AUDIO));
        legendas.setOnClickListener(v -> abrirFaixas(TipoFaixa.LEGENDA));
        ajustarLegenda.setOnClickListener(v -> abrirAjusteLegenda());
        botaoCapitulos.setOnClickListener(v -> comando(this::alternarFaixa));
        expandir.setOnClickListener(v -> comando(this::entrarTelaCheia));

        // Faixa de capítulos: "Capítulos · arraste para o lado", 176 x 99, 16 dp entre eles
        SpannableStringBuilder titulo = new SpannableStringBuilder(getString(R.string.st_capitulos)).append(' ');
        int inicio = titulo.length();
        titulo.append(getString(R.string.st_arraste_para_o_lado));
        titulo.setSpan(new TextAppearanceSpan(this, R.style.StarkTech_Texto_Medio), inicio, titulo.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        titulo.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.st_texto_apagado)),
                inicio, titulo.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ((TextView) findViewById(R.id.st_rep_faixa_titulo)).setText(titulo);
        RecyclerView lista = findViewById(R.id.st_rep_lista_capitulos);
        lista.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        final int espaco = getResources().getDimensionPixelSize(R.dimen.st_tablet_rep_capitulo_espaco);
        lista.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect saida, @NonNull View item, @NonNull RecyclerView pai,
                                       @NonNull RecyclerView.State estado) {
                saida.set(pai.getChildAdapterPosition(item) > 0 ? espaco : 0, 0, 0, 0);
            }
        });
        adapter = new CapituloAdapter(R.layout.st_tablet_item_capitulo_faixa, false,
                c -> comando(() -> reproducao.irParaCapitulo(c)));
        lista.setAdapter(adapter);
        adapter.atualizar(reproducao.capitulos, numeroAtual());
        reproducao.definirAoMudarCapitulos(this::mostrarCapitulos);
        reproducao.definirAoPassarEpisodio(this::abrirEpisodio);

        new GestosReproducao(findViewById(R.id.st_rep_video), this);
        botoes = new BotoesDeMidia(this, reproducao.titulo, new BotoesDeMidia.Acoes() {
            @Override
            public void tocar() {
                if (!reproducao.estaTocando()) reproducao.pausarOuContinuar();
            }

            @Override
            public void pausar() {
                if (reproducao.estaTocando()) reproducao.pausarOuContinuar();
            }

            @Override
            public void capituloAnterior() {
                avisarCapitulo(reproducao.irParaCapituloAnterior());
            }

            @Override
            public void proximoCapitulo() {
                avisarCapitulo(reproducao.irParaProximoCapitulo());
            }
        });
        reproducao.definirOuvinte(this::atualizar);
        atualizar();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (reproducao != null) {
            reproducao.iniciar();
            botoes.ligar(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        abrindoFaixas = false;
        // Na volta de Áudio / Legendas, a contagem recomeça; a de abrir começa com o vídeo tocando
        if (!visiveis) entrarTelaCheia();
        else if (comecou) agendarOcultar();
    }

    @Override
    protected void onPause() {
        relogio.removeCallbacks(ocultar);
        if (reproducao != null) reproducao.gravarPosicao();
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (reproducao != null) {
            reproducao.parar();
            botoes.ligar(false);
        }
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(ESTADO_PASSOU_PELA_MINIATURA, passouPelaMiniatura);
    }

    @Override
    protected void onDestroy() {
        soltarMotor();
        if (aberta == this) aberta = null;
        super.onDestroy();
    }

    /**
     * Fim do episódio com "Próximo episódio automático" ligado: esta tela reabre com o seguinte
     * (recreate() solta o motor antes de abrir o novo; o intent trocado aqui é o que o onCreate novo
     * lê). Na miniatura, fica no fim do vídeo: a tela nova abriria com os controles na janelinha.
     */
    private void abrirEpisodio(String id) {
        if (naMiniatura || isFinishing()) return;
        setIntent(new Intent(getIntent()).putExtra(Navegacao.EXTRA_ID, id)
                .putExtra(Navegacao.EXTRA_CAPITULO, Navegacao.DE_ONDE_PAROU));
        recreate();
    }

    private void soltarMotor() {
        if (botoes != null) botoes.liberar();
        botoes = null;
        if (reproducao != null) reproducao.liberar();
        reproducao = null;
    }

    /** Depois da miniatura o player fica numa tarefa só dele: Voltar leva ao app, não à tela inicial. */
    @Override
    public void finish() {
        if (passouPelaMiniatura) trazerOAppParaFrente();
        super.finish();
    }

    private void trazerOAppParaFrente() {
        try {
            ActivityManager sistema = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            for (ActivityManager.AppTask tarefa : sistema.getAppTasks()) {
                if (tarefa.getTaskInfo().id != getTaskId()) {
                    tarefa.moveToFront();
                    return;
                }
            }
        } catch (RuntimeException e) {
            // a outra tarefa acabou de fechar: volta para onde o sistema mandar
        }
    }

    // ---- Miniatura (picture-in-picture) ----

    /** Home, outro app: com o vídeo tocando, ele segue na miniatura. Pausado, para como antes. */
    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (reproducao != null && reproducao.estaTocando() && !abrindoFaixas) entrarNaMiniatura();
    }

    private void entrarNaMiniatura() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O
                || !getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) return;
        float proporcao = Math.max(MINIATURA_MENOR, Math.min(MINIATURA_MAIOR, reproducao.proporcao()));
        try {
            enterPictureInPictureMode(new PictureInPictureParams.Builder()
                    .setAspectRatio(new Rational(Math.round(proporcao * 1000), 1000))
                    .build());
        } catch (RuntimeException e) {
            // miniatura desligada para o app nos ajustes do aparelho: o vídeo pausa ao sair, como antes
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean agora, @NonNull Configuration configuracao) {
        super.onPictureInPictureModeChanged(agora, configuracao);
        naMiniatura = agora;
        if (agora) {
            passouPelaMiniatura = true;
            relogio.removeCallbacks(ocultar);
            visiveis = false;
            for (View v : new View[]{topo, controles}) {
                v.animate().cancel();
                v.setAlpha(0f);
                v.setVisibility(View.INVISIBLE);
            }
            aviso.animate().cancel();
            aviso.setAlpha(0f);
        } else if (getLifecycle().getCurrentState() == Lifecycle.State.CREATED) {
            // Fechou a miniatura no X (já parou e gravou a posição)
            finishAndRemoveTask();
        } else {
            // Expandiu: volta ao player, com os controles à vista
            mostrarControles();
        }
    }

    /** Outro vídeo vai abrir: este sai da miniatura e grava a posição antes de ela ser lida. */
    private void fecharMiniatura() {
        if (reproducao != null) reproducao.parar();
        soltarMotor();
        finishAndRemoveTask();
    }

    /** Arquivo que não abre (HD desligado, arquivo estragado): avisa e volta. */
    private void naoAbriu() {
        Toast.makeText(this, R.string.st_video_nao_abriu, Toast.LENGTH_LONG).show();
        finish();
    }

    /** Com os controles à vista, qualquer toque recomeça a contagem (o toque que esconde cancela). */
    @Override
    public boolean dispatchTouchEvent(MotionEvent evento) {
        if (visiveis) agendarOcultar();
        return super.dispatchTouchEvent(evento);
    }

    private void atualizar() {
        tempo.setText(PecasReproducao.tempo(reproducao.posicao()));
        duracao.setText(PecasReproducao.tempo(reproducao.duracao));
        barra.definirFracao(reproducao.posicao() / (float) reproducao.duracao);
        sub.setText(PecasReproducao.linhaCapitulo(this, reproducao));
        sub.setVisibility(sub.getText().length() == 0 ? View.GONE : View.VISIBLE);
        proximo.setText(PecasReproducao.textoProximo(this, reproducao));
        // Legenda rápida (1.5): só quando o vídeo tem legenda
        int ajustar = reproducao.temLegenda() ? View.VISIBLE : View.GONE;
        if (ajustarLegenda.getVisibility() != ajustar) ajustarLegenda.setVisibility(ajustar);
        boolean tocando = reproducao.estaTocando();
        if (tocando != tocandoMostrado) {
            tocandoMostrado = tocando;
            pausar.setImageResource(tocando ? R.drawable.st_ic_pausar : R.drawable.st_ic_assistir);
            pausar.setContentDescription(getString(tocando ? R.string.st_pausar : R.string.st_continuar));
        }
        adapter.definirAtual(numeroAtual());
        botoes.atualizar(tocando, reproducao.posicao(), reproducao.temCapitulos());
        if (!comecou && reproducao.estaPronto()) {
            comecou = true;
            if (visiveis) agendarOcultar();
        }
    }

    /** Capítulos lidos depois de abrir (vídeo ainda não varrido): marcas, botões e faixa. */
    private void mostrarCapitulos() {
        barra.definirMarcas(PecasReproducao.marcas(reproducao));
        int visivel = reproducao.temCapitulos() ? View.VISIBLE : View.GONE;
        findViewById(R.id.st_rep_cap_anterior).setVisibility(visivel);
        findViewById(R.id.st_rep_cap_proximo).setVisibility(visivel);
        botaoCapitulos.setVisibility(visivel);
        adapter.atualizar(reproducao.capitulos, numeroAtual());
    }

    private int numeroAtual() {
        Capitulo atual = reproducao.capituloAtual();
        return atual == null ? 0 : atual.numero;
    }

    /** Capítulo trocado por um botão de fora do app, com os controles ocultos: mostra qual. */
    private void avisarCapitulo(@Nullable Capitulo capitulo) {
        if (capitulo != null && !visiveis && !naMiniatura) mostrarAviso(PecasReproducao.nomeCapitulo(this, capitulo));
    }

    /** Abrir outra tela do app também chama onUserLeaveHint: aqui não vai para a miniatura. */
    private void abrirFaixas(TipoFaixa tipo) {
        abrindoFaixas = true;
        Navegacao.abrirFaixas(this, reproducao.id, tipo);
    }

    /** Legenda rápida (1.5): como Áudio e Legendas, o painel por cima não manda o vídeo para a miniatura. */
    private void abrirAjusteLegenda() {
        abrindoFaixas = true;
        Navegacao.abrirAjusteLegenda(this, reproducao.id);
    }

    /** Botão dos controles: faz o comando e recomeça a contagem para sumir. */
    private void comando(Runnable acao) {
        acao.run();
        agendarOcultar();
    }

    private void alternarFaixa() {
        boolean abrir = faixa.getVisibility() != View.VISIBLE;
        faixa.setVisibility(abrir ? View.VISIBLE : View.GONE);
        botaoCapitulos.setActivated(abrir);
    }

    // ---- GestosReproducao.Acoes ----

    @Override
    public void alternarControles() {
        if (visiveis) ocultarControles();
        else mostrarControles();
    }

    @Override
    public void pular(int segundos) {
        // Dos gestos só vale o lado: quanto anda vem de Configurações
        int pulo = Preferencias.puloSegundos(this);
        reproducao.pular(segundos < 0 ? -pulo : pulo);
        mostrarAviso(getString(segundos < 0 ? R.string.st_pulo_menos : R.string.st_pulo_mais, pulo));
    }

    @Override
    public void arrastar(int deslocamento, boolean soltou) {
        if (inicioArrasto < 0) inicioArrasto = reproducao.posicao();
        int alvo = Math.max(0, Math.min(reproducao.duracao, inicioArrasto + deslocamento));
        mostrarAviso(PecasReproducao.tempo(alvo));
        if (soltou) {
            reproducao.irPara(alvo);
            inicioArrasto = -1;
        }
    }

    @Override
    public void volume(int porcento) {
        mostrarAviso(getString(R.string.st_volume, porcento));
    }

    // ---- Controles que somem ----

    private void agendarOcultar() {
        relogio.removeCallbacks(ocultar);
        relogio.postDelayed(ocultar, Preferencias.tempoControlesMs(this));
    }

    private void entrarTelaCheia() {
        barras.hide(WindowInsetsCompat.Type.systemBars());
        expandir.setEnabled(false);
        expandir.setAlpha(EXPANDIR_APAGADO);
    }

    private void sairDaTelaCheia() {
        barras.show(WindowInsetsCompat.Type.navigationBars());
        expandir.setEnabled(true);
        expandir.setAlpha(1f);
    }

    private void mostrarControles() {
        visiveis = true;
        sairDaTelaCheia();
        for (View v : new View[]{topo, controles}) {
            v.setVisibility(View.VISIBLE);
            v.animate().cancel();
            if (Animacoes.ligadas(this)) v.animate().alpha(1f).setDuration(FADE_MS).start();
            else v.setAlpha(1f);
        }
        agendarOcultar();
    }

    /** Some com fade e fica INVISIBLE: os toques passam para a área do vídeo (gestos). */
    private void ocultarControles() {
        visiveis = false;
        relogio.removeCallbacks(ocultar);
        entrarTelaCheia();
        for (View v : new View[]{topo, controles}) {
            v.animate().cancel();
            if (Animacoes.ligadas(this)) {
                v.animate().alpha(0f).setDuration(FADE_MS).withEndAction(() -> {
                    if (!visiveis) v.setVisibility(View.INVISIBLE);
                }).start();
            } else {
                v.setAlpha(0f);
                v.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void mostrarAviso(String texto) {
        aviso.setText(texto);
        aviso.animate().cancel();
        aviso.setAlpha(1f);
        aviso.animate().alpha(0f).setStartDelay(700).setDuration(Animacoes.ligadas(this) ? 250 : 0).start();
    }
}
