// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import com.archos.mediacenter.video.R;

import br.com.starktechstudio.mediaplayer.dados.ModeloMusicas.Musica;
import br.com.starktechstudio.mediaplayer.dados.RepositorioMusicas;
import br.com.starktechstudio.mediaplayer.musica.Fila;
import br.com.starktechstudio.mediaplayer.musica.Tocador;

/**
 * Tocando agora (1.5): capa grande, nome, artista e álbum, a barra com o tempo e os botões Aleatório,
 * Anterior, Tocar ou pausar, Próxima e Repetir; embaixo, Fila e Ir para o álbum. TV: a barra recebe o foco
 * e ← / → andam 10 s; tablet: tocar ou arrastar na barra. Abre também pela notificação.
 */
public class TocandoAgoraActivity extends AppCompatActivity {

    private static final int PASSO_MS = 10_000;

    private boolean tv;
    private Tocador tocador;
    private FrameLayout lugarDaCapa;
    private TextView titulo;
    private TextView artista;
    private TextView album;
    private TextView tempo;
    private TextView duracao;
    private BarraCapitulosView barra;
    private ImageButton tocarPausar;
    private ImageButton aleatorio;
    private ImageButton repetir;
    private Button irParaAlbum;
    private String capaMostrada;
    /** Tablet: arrastando na barra, a posição da tela é a do dedo. */
    private float fracaoArrastada = -1;
    private final Runnable aoMudar = this::atualizar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(Aparelho.tema(this));
        super.onCreate(savedInstanceState);
        tv = Aparelho.ehTv(this);
        tocador = Tocador.de(this);

        FrameLayout raiz = new FrameLayout(this);
        raiz.setBackgroundResource(R.color.st_fundo);
        raiz.addView(new FundoAnimado(this), new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        LinearLayout pagina = new LinearLayout(this);
        pagina.setOrientation(LinearLayout.VERTICAL);
        int lateral = getResources().getDimensionPixelSize(tv ? R.dimen.st_tv_margem_lateral : R.dimen.st_tablet_margem_h);
        pagina.setPadding(lateral, Medidas.px(this, 36), lateral, Medidas.px(this, 36));
        if (!tv) BordasSistema.aplicar(pagina, true, true, true, true);
        pagina.addView(PecasMusica.topo(this, getString(R.string.st_musicas_tocando_agora)));

        LinearLayout corpo = new LinearLayout(this);
        corpo.setOrientation(LinearLayout.HORIZONTAL);
        corpo.setGravity(Gravity.CENTER_VERTICAL);
        lugarDaCapa = new FrameLayout(this);
        // Celular deitado: a capa cabe na altura da tela
        int lado = Math.min(Medidas.px(this, tv ? 640 : 560), getResources().getDisplayMetrics().heightPixels - Medidas.px(this, 260));
        lado = Math.max(lado, Medidas.px(this, 240));
        corpo.addView(lugarDaCapa, new LinearLayout.LayoutParams(lado, lado));

        LinearLayout lado2 = new LinearLayout(this);
        lado2.setOrientation(LinearLayout.VERTICAL);
        titulo = Textos.novo(this, "", R.font.st_inter_black, tv ? 60 : 52, R.color.st_texto);
        titulo.setLetterSpacing(-0.02f);
        titulo.setMaxLines(2);
        titulo.setEllipsize(android.text.TextUtils.TruncateAt.END);
        lado2.addView(titulo);
        artista = PecasMusica.linha(this, "", R.font.st_inter_bold, 28, R.color.st_texto_secundario);
        lado2.addView(artista, PecasMusica.comTopo(Medidas.px(this, 14)));
        album = PecasMusica.linha(this, "", R.font.st_inter_semibold, 22, R.color.st_texto_apagado);
        lado2.addView(album, PecasMusica.comTopo(Medidas.px(this, 8)));

        barra = new BarraCapitulosView(this);
        barra.definirTv(tv);
        if (tv) {
            barra.setFocusable(true);
            barra.setForeground(ContextCompat.getDrawable(this, R.drawable.st_foco_tv));
            barra.setOnKeyListener((v, tecla, evento) -> {
                if (evento.getAction() != KeyEvent.ACTION_DOWN) return false;
                if (tecla == KeyEvent.KEYCODE_DPAD_LEFT) {
                    tocador.irPara(tocador.posicaoMs() - PASSO_MS);
                    return true;
                }
                if (tecla == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    tocador.irPara(tocador.posicaoMs() + PASSO_MS);
                    return true;
                }
                return false;
            });
        } else {
            barra.definirAoArrastar((fracao, soltou) -> {
                fracaoArrastada = soltou ? -1 : fracao;
                if (soltou) tocador.irPara(Math.round(fracao * tocador.duracaoMs()));
                atualizar();
            });
        }
        LinearLayout.LayoutParams lpBarra = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Medidas.px(this, 44));
        lpBarra.topMargin = Medidas.px(this, 44);
        lado2.addView(barra, lpBarra);
        LinearLayout tempos = new LinearLayout(this);
        tempos.setOrientation(LinearLayout.HORIZONTAL);
        tempo = Textos.novo(this, "0:00", R.font.st_inter_bold, 20, R.color.st_texto_secundario);
        tempos.addView(tempo, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        duracao = Textos.novo(this, "0:00", R.font.st_inter_bold, 20, R.color.st_texto_apagado);
        tempos.addView(duracao);
        lado2.addView(tempos, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout botoes = new LinearLayout(this);
        botoes.setOrientation(LinearLayout.HORIZONTAL);
        botoes.setGravity(Gravity.CENTER_VERTICAL);
        int ladoBotao = PecasMusica.ladoBotaoIcone(this) * 3 / 2;
        aleatorio = PecasMusica.botaoIcone(this, R.drawable.st_ic_aleatorio, R.string.st_musicas_aleatorio);
        aleatorio.setOnClickListener(v -> tocador.alternarAleatorio());
        botoes.addView(aleatorio, new LinearLayout.LayoutParams(ladoBotao, ladoBotao));
        ImageButton anterior = PecasMusica.botaoIcone(this, R.drawable.st_ic_cap_anterior, R.string.st_musicas_anterior);
        anterior.setOnClickListener(v -> tocador.anterior());
        botoes.addView(anterior, lpBotao(ladoBotao));
        tocarPausar = PecasMusica.botaoIcone(this, R.drawable.st_ic_pausar, R.string.st_pausar);
        tocarPausar.setBackgroundResource(R.drawable.st_botao_primario);
        tocarPausar.setOnClickListener(v -> tocador.pausarOuContinuar());
        int ladoGrande = ladoBotao * 4 / 3;
        botoes.addView(tocarPausar, lpBotao(ladoGrande));
        ImageButton proxima = PecasMusica.botaoIcone(this, R.drawable.st_ic_cap_proximo, R.string.st_musicas_proxima);
        proxima.setOnClickListener(v -> tocador.proxima());
        botoes.addView(proxima, lpBotao(ladoBotao));
        repetir = PecasMusica.botaoIcone(this, R.drawable.st_ic_repetir, R.string.st_musicas_repetir);
        repetir.setOnClickListener(v -> tocador.trocarRepetir());
        botoes.addView(repetir, lpBotao(ladoBotao));
        lado2.addView(botoes, PecasMusica.comTopo(Medidas.px(this, 36)));

        LinearLayout mais = new LinearLayout(this);
        mais.setOrientation(LinearLayout.HORIZONTAL);
        Button fila = PecasMusica.botao(this, R.string.st_musicas_fila, R.drawable.st_ic_fila, false);
        fila.setOnClickListener(v -> MenuMusicaActivity.abrirFila(this));
        mais.addView(fila);
        irParaAlbum = PecasMusica.botao(this, R.string.st_musicas_ir_album, R.drawable.st_ic_disco, false);
        irParaAlbum.setOnClickListener(v -> {
            Musica m = tocador.atual();
            if (m != null && RepositorioMusicas.de(this).album(m.idAlbum) != null) TelasMusica.abrirAlbum(this, m.idAlbum);
        });
        LinearLayout.LayoutParams lpAlbum = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpAlbum.setMarginStart(Medidas.px(this, 14));
        mais.addView(irParaAlbum, lpAlbum);
        lado2.addView(mais, PecasMusica.comTopo(Medidas.px(this, 28)));

        LinearLayout.LayoutParams lpLado = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lpLado.setMarginStart(Medidas.px(this, 64));
        corpo.addView(lado2, lpLado);
        LinearLayout.LayoutParams lpCorpo = tv ? new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
                : new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpCorpo.topMargin = Medidas.px(this, 24);
        pagina.addView(corpo, lpCorpo);

        if (tv) {
            raiz.addView(pagina, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        } else {
            // Tablet e celular: rola se não couber
            androidx.core.widget.NestedScrollView rolagem = new androidx.core.widget.NestedScrollView(this);
            rolagem.setVerticalScrollBarEnabled(false);
            rolagem.setOverScrollMode(View.OVER_SCROLL_NEVER);
            rolagem.addView(pagina, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            raiz.addView(rolagem, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        setContentView(raiz);
        if (tv) tocarPausar.post(tocarPausar::requestFocus);
    }

    private LinearLayout.LayoutParams lpBotao(int lado) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(lado, lado);
        lp.setMarginStart(Medidas.px(this, 18));
        return lp;
    }

    @Override
    protected void onResume() {
        super.onResume();
        tocador.ouvir(aoMudar);
        atualizar();
    }

    @Override
    protected void onPause() {
        tocador.deixarDeOuvir(aoMudar);
        super.onPause();
    }

    private void atualizar() {
        Musica m = tocador.atual();
        if (m == null) {
            finish();
            return;
        }
        titulo.setText(m.titulo);
        artista.setText(PecasMusica.artista(this, m.artista));
        album.setText(PecasMusica.album(this, m.album));
        String capa = m.capa == null ? "" : m.capa;
        if (!capa.equals(capaMostrada) || lugarDaCapa.getChildCount() == 0) {
            capaMostrada = capa;
            lugarDaCapa.removeAllViews();
            int lado = lugarDaCapa.getLayoutParams().width;
            lugarDaCapa.addView(PecasMusica.capa(this, lado, m.cor, m.capa, R.drawable.st_ic_musicas, false));
        }
        int total = tocador.duracaoMs();
        int agora = fracaoArrastada >= 0 ? Math.round(fracaoArrastada * total) : tocador.posicaoMs();
        tempo.setText(PecasMusica.tempo(agora));
        duracao.setText(PecasMusica.tempo(total));
        barra.definirFracao(total > 0 ? agora / (float) total : 0f);

        boolean tocando = tocador.querTocar();
        tocarPausar.setImageResource(tocando ? R.drawable.st_ic_pausar : R.drawable.st_ic_assistir);
        tocarPausar.setContentDescription(getString(tocando ? R.string.st_pausar : R.string.st_musicas_tocar));
        ligado(aleatorio, tocador.aleatorio());
        aleatorio.setContentDescription(getString(tocador.aleatorio() ? R.string.st_musicas_aleatorio_ligado : R.string.st_musicas_aleatorio_desligado));
        Fila.Repetir modo = tocador.repetir();
        repetir.setImageResource(modo == Fila.Repetir.UMA ? R.drawable.st_ic_repetir_uma : R.drawable.st_ic_repetir);
        ligado(repetir, modo != Fila.Repetir.NAO);
        repetir.setContentDescription(getString(modo == Fila.Repetir.UMA ? R.string.st_musicas_repetir_uma
                : modo == Fila.Repetir.TODAS ? R.string.st_musicas_repetir_todas : R.string.st_musicas_repetir_nao));
        irParaAlbum.setVisibility(RepositorioMusicas.de(this).album(m.idAlbum) != null ? View.VISIBLE : View.GONE);
    }

    /** Aleatório e Repetir ligados: ícone em azul. */
    private void ligado(ImageButton botao, boolean ligado) {
        ImageViewCompat.setImageTintList(botao, ContextCompat.getColorStateList(this, ligado ? R.color.st_azul : R.color.st_texto));
    }
}
