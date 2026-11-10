// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.archos.mediacenter.video.R;

import br.com.starktechstudio.mediaplayer.dados.ModeloMusicas.Musica;
import br.com.starktechstudio.mediaplayer.musica.Tocador;

/**
 * Barra "Tocando agora" (1.5), no alto da aba Músicas e embaixo das telas de álbum, artista e playlist:
 * capa pequena, nome, artista e os botões anterior, tocar ou pausar e próxima. OK ou toque na barra abre o
 * Tocando agora. Some quando não há nada na fila. Ligar no onResume e desligar no onPause da tela.
 */
public final class BarraTocando {

    public final LinearLayout view;
    private final Activity tela;
    private final FrameLayout lugarDaCapa;
    private final TextView titulo;
    private final TextView artista;
    private final ImageButton tocarPausar;
    private String capaMostrada;
    private boolean tocandoMostrado;
    private final Runnable aoMudar = this::atualizar;

    public BarraTocando(Activity tela) {
        this.tela = tela;
        boolean tv = Aparelho.ehTv(tela);
        view = new LinearLayout(tela);
        view.setOrientation(LinearLayout.HORIZONTAL);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setBackgroundResource(R.drawable.st_fundo_cartao);
        int folga = Medidas.px(tela, 16);
        view.setPadding(folga, folga, folga, folga);

        LinearLayout abrir = new LinearLayout(tela);
        abrir.setOrientation(LinearLayout.HORIZONTAL);
        abrir.setGravity(Gravity.CENTER_VERTICAL);
        abrir.setBackgroundResource(R.drawable.st_linha_musica);
        abrir.setFocusable(true);
        abrir.setClickable(true);
        abrir.setPadding(folga / 2, folga / 2, folga, folga / 2);
        if (tv) abrir.setForeground(ContextCompat.getDrawable(tela, R.drawable.st_foco_tv));
        abrir.setOnClickListener(v -> TelasMusica.abrirTocandoAgora(tela));
        abrir.setContentDescription(tela.getString(R.string.st_musicas_tocando_agora));
        lugarDaCapa = new FrameLayout(tela);
        int lado = Medidas.px(tela, 84);
        abrir.addView(lugarDaCapa, new LinearLayout.LayoutParams(lado, lado));
        LinearLayout textos = new LinearLayout(tela);
        textos.setOrientation(LinearLayout.VERTICAL);
        TextView rotulo = Textos.rotulo(tela, tela.getString(R.string.st_musicas_tocando_agora), 13);
        rotulo.setTextColor(ContextCompat.getColor(tela, R.color.st_ciano));
        textos.addView(rotulo);
        titulo = PecasMusica.linha(tela, "", R.font.st_inter_extrabold, 22, R.color.st_texto);
        textos.addView(titulo, PecasMusica.comTopo(Medidas.px(tela, 6)));
        artista = PecasMusica.linha(tela, "", R.font.st_inter_semibold, 17, R.color.st_texto_apagado);
        textos.addView(artista, PecasMusica.comTopo(Medidas.px(tela, 4)));
        LinearLayout.LayoutParams lpTextos = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lpTextos.setMarginStart(Medidas.px(tela, 18));
        abrir.addView(textos, lpTextos);
        view.addView(abrir, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        int ladoBotao = PecasMusica.ladoBotaoIcone(tela);
        ImageButton anterior = PecasMusica.botaoIcone(tela, R.drawable.st_ic_cap_anterior, R.string.st_musicas_anterior);
        anterior.setOnClickListener(v -> Tocador.de(tela).anterior());
        view.addView(anterior, botaoLp(ladoBotao, folga));
        tocarPausar = PecasMusica.botaoIcone(tela, R.drawable.st_ic_pausar, R.string.st_pausar);
        tocarPausar.setBackgroundResource(R.drawable.st_botao_primario);
        tocarPausar.setOnClickListener(v -> Tocador.de(tela).pausarOuContinuar());
        view.addView(tocarPausar, botaoLp(ladoBotao, folga / 2));
        ImageButton proxima = PecasMusica.botaoIcone(tela, R.drawable.st_ic_cap_proximo, R.string.st_musicas_proxima);
        proxima.setOnClickListener(v -> Tocador.de(tela).proxima());
        view.addView(proxima, botaoLp(ladoBotao, folga / 2));
        view.setVisibility(View.GONE);
    }

    private static LinearLayout.LayoutParams botaoLp(int lado, int antes) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(lado, lado);
        lp.setMarginStart(antes);
        return lp;
    }

    public void ligar() {
        Tocador.de(tela).ouvir(aoMudar);
        atualizar();
    }

    public void desligar() {
        Tocador.de(tela).deixarDeOuvir(aoMudar);
    }

    private void atualizar() {
        Tocador tocador = Tocador.de(tela);
        Musica m = tocador.atual();
        if (m == null) {
            view.setVisibility(View.GONE);
            return;
        }
        view.setVisibility(View.VISIBLE);
        titulo.setText(m.titulo);
        artista.setText(PecasMusica.artista(tela, m.artista));
        String capa = m.capa == null ? "" : m.capa;
        if (!capa.equals(capaMostrada) || lugarDaCapa.getChildCount() == 0) {
            capaMostrada = capa;
            lugarDaCapa.removeAllViews();
            lugarDaCapa.addView(PecasMusica.capa(tela, Medidas.px(tela, 84), m.cor, m.capa, R.drawable.st_ic_musicas, false));
        }
        boolean tocando = tocador.querTocar();
        if (tocando != tocandoMostrado || tocarPausar.getTag() == null) {
            tocandoMostrado = tocando;
            tocarPausar.setTag(Boolean.TRUE);
            tocarPausar.setImageResource(tocando ? R.drawable.st_ic_pausar : R.drawable.st_ic_assistir);
            tocarPausar.setContentDescription(tela.getString(tocando ? R.string.st_pausar : R.string.st_musicas_tocar));
        }
    }
}
