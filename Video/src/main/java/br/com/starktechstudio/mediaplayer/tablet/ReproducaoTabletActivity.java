// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.tablet;

import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.TextAppearanceSpan;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
 * Player do tablet (tablet-player.dc.html), com a reprodução simulada da F1, em tela cheia: topo
 * com Voltar, título grande e o capítulo; embaixo, barra (tocar ou arrastar muda a posição),
 * "Próximo capítulo em…", Cap. anterior, Pausar, Próx. capítulo (os de capítulo somem sem
 * capítulos; −10 s / +10 s só com dois toques nas laterais), Áudio, Legendas e Capítulos (abre a faixa de capítulos, que arrasta para
 * o lado, sem "Ver todos"). Um toque mostra ou esconde os controles; à vista, somem 5 s depois do
 * último toque (botões, barra e faixa de capítulos contam) e ficam enquanto Áudio / Legendas estão
 * abertos. Gestos sem dica ({@link GestosReproducao}).
 */
public class ReproducaoTabletActivity extends AppCompatActivity implements GestosReproducao.Acoes {

    private static final long OCULTAR_MS = 5000;
    private static final long FADE_MS = 150;

    private ReproducaoSimulada reproducao;
    private View topo;
    private View controles;
    private View faixa;
    private TextView sub;
    private TextView tempo;
    private TextView duracao;
    private TextView proximo;
    private TextView aviso;
    private TextView botaoCapitulos;
    private ImageButton pausar;
    private BarraCapitulosView barra;
    private CapituloAdapter adapter;

    private final Handler relogio = new Handler(Looper.getMainLooper());
    private final Runnable ocultar = this::ocultarControles;
    private boolean visiveis = true;
    private boolean tocandoMostrado = true;
    private int inicioArrasto = -1;

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
        setContentView(R.layout.st_tablet_reproducao);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // Tela cheia: as barras do sistema voltam com um arrasto da borda
        WindowInsetsControllerCompat barras = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        barras.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        barras.hide(WindowInsetsCompat.Type.systemBars());

        topo = findViewById(R.id.st_rep_topo);
        controles = findViewById(R.id.st_rep_controles);
        faixa = findViewById(R.id.st_rep_faixa);
        sub = findViewById(R.id.st_rep_sub);
        tempo = findViewById(R.id.st_rep_tempo);
        duracao = findViewById(R.id.st_rep_duracao);
        proximo = findViewById(R.id.st_rep_proximo);
        aviso = findViewById(R.id.st_rep_aviso);
        botaoCapitulos = findViewById(R.id.st_rep_capitulos);
        pausar = findViewById(R.id.st_rep_pausar);
        barra = findViewById(R.id.st_rep_barra);
        TextView capAnterior = findViewById(R.id.st_rep_cap_anterior);
        TextView capProximo = findViewById(R.id.st_rep_cap_proximo);
        TextView audio = findViewById(R.id.st_rep_audio);
        TextView legendas = findViewById(R.id.st_rep_legendas);

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
        Icones.acimaDoTexto(botaoCapitulos, R.drawable.st_ic_capitulos, icone, branco);

        // Sem capítulos: só Pausar no centro (INTERFACE.md §4)
        boolean comCapitulos = reproducao.temCapitulos();
        capAnterior.setVisibility(comCapitulos ? View.VISIBLE : View.GONE);
        capProximo.setVisibility(comCapitulos ? View.VISIBLE : View.GONE);
        botaoCapitulos.setVisibility(comCapitulos ? View.VISIBLE : View.GONE);

        findViewById(R.id.st_rep_voltar).setOnClickListener(v -> finish());
        capAnterior.setOnClickListener(v -> comando(() -> reproducao.irParaCapituloAnterior()));
        pausar.setOnClickListener(v -> comando(reproducao::pausarOuContinuar));
        capProximo.setOnClickListener(v -> comando(() -> reproducao.irParaProximoCapitulo()));
        audio.setOnClickListener(v -> Navegacao.abrirFaixas(this, reproducao.id, TipoFaixa.AUDIO));
        legendas.setOnClickListener(v -> Navegacao.abrirFaixas(this, reproducao.id, TipoFaixa.LEGENDA));
        botaoCapitulos.setOnClickListener(v -> comando(this::alternarFaixa));

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

        new GestosReproducao(findViewById(R.id.st_rep_video), this);
        reproducao.definirOuvinte(this::atualizar);
        atualizar();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (reproducao != null) reproducao.iniciar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // A contagem começa com a tela já aberta (e de novo na volta de Áudio / Legendas)
        if (visiveis) agendarOcultar();
    }

    @Override
    protected void onPause() {
        relogio.removeCallbacks(ocultar);
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (reproducao != null) reproducao.parar();
        super.onStop();
    }

    /** Com os controles à vista, qualquer toque recomeça os 5 s (o toque que esconde cancela). */
    @Override
    public boolean dispatchTouchEvent(MotionEvent evento) {
        if (visiveis) agendarOcultar();
        return super.dispatchTouchEvent(evento);
    }

    private void atualizar() {
        tempo.setText(PecasReproducao.tempo(reproducao.posicao()));
        barra.definirFracao(reproducao.posicao() / (float) reproducao.duracao);
        sub.setText(PecasReproducao.linhaCapitulo(this, reproducao));
        sub.setVisibility(sub.getText().length() == 0 ? View.GONE : View.VISIBLE);
        proximo.setText(PecasReproducao.textoProximo(this, reproducao));
        boolean tocando = reproducao.estaTocando();
        if (tocando != tocandoMostrado) {
            tocandoMostrado = tocando;
            pausar.setImageResource(tocando ? R.drawable.st_ic_pausar : R.drawable.st_ic_assistir);
            pausar.setContentDescription(getString(tocando ? R.string.st_pausar : R.string.st_continuar));
        }
        adapter.definirAtual(numeroAtual());
    }

    private int numeroAtual() {
        Capitulo atual = reproducao.capituloAtual();
        return atual == null ? 0 : atual.numero;
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
        reproducao.pular(segundos);
        mostrarAviso(getString(segundos < 0 ? R.string.st_menos_10 : R.string.st_mais_10));
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
        relogio.postDelayed(ocultar, OCULTAR_MS);
    }

    private void mostrarControles() {
        visiveis = true;
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
