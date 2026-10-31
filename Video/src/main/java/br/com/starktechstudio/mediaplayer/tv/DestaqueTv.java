// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.tv;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.archos.mediacenter.video.R;

import java.util.ArrayList;
import java.util.List;

import br.com.starktechstudio.mediaplayer.comum.CapaAdapter;
import br.com.starktechstudio.mediaplayer.comum.FocoTv;
import br.com.starktechstudio.mediaplayer.comum.Icones;
import br.com.starktechstudio.mediaplayer.comum.Navegacao;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Tipo;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Titulo;
import br.com.starktechstudio.mediaplayer.dados.RepositorioBiblioteca;

/**
 * Destaque "Novidades na biblioteca" do Início da TV (Main.dc.html): os slides de
 * {@link RepositorioBiblioteca#destaques()}, trocados a cada 10 s. D10: os pontos recebem
 * o foco (→ depois de Favoritar) e OK num ponto mostra aquele slide; a troca automática
 * pausa enquanto o foco está no destaque e recomeça a contar quando ele sai.
 * Assistir abre a reprodução, Detalhes abre o título e Favoritar alterna o favorito
 * (o texto vira "Remover dos favoritos", D22).
 */
final class DestaqueTv {

    static final long TROCA_MS = 10_000;

    private final Activity atividade;
    private final RepositorioBiblioteca repositorio;
    private final Runnable aoMudarFavorito;
    private final Handler relogio = new Handler(Looper.getMainLooper());

    private final View slide;
    private final View capa;
    private final TextView titulo;
    private final TextView ano;
    private final TextView duracao;
    private final TextView genero;
    private final TextView qualidade;
    private final TextView sinopse;
    private final TextView favoritar;
    private final TextView[] botoes;
    private final LinearLayout pontos;
    private final List<ImageView> listaPontos = new ArrayList<>();

    private final int pontoAltura;
    private final int pontoLargura;
    private final int pontoAtivoLargura;
    private final int pontoMargem;

    private List<Titulo> slides = new ArrayList<>();
    private int indice;
    private boolean focoDentro;
    private boolean ativo;
    private int idDescida = View.NO_ID;

    private final Runnable trocar = new Runnable() {
        @Override
        public void run() {
            if (slides.size() > 1) mostrar((indice + 1) % slides.size(), true);
            agendar();
        }
    };

    DestaqueTv(Activity atividade, View raiz, RepositorioBiblioteca repositorio, Runnable aoMudarFavorito) {
        this.atividade = atividade;
        this.repositorio = repositorio;
        this.aoMudarFavorito = aoMudarFavorito;

        slide = raiz.findViewById(R.id.st_destaque_slide);
        capa = raiz.findViewById(R.id.st_destaque_capa);
        titulo = raiz.findViewById(R.id.st_destaque_titulo);
        ano = raiz.findViewById(R.id.st_destaque_ano);
        duracao = raiz.findViewById(R.id.st_destaque_duracao);
        genero = raiz.findViewById(R.id.st_destaque_genero);
        qualidade = raiz.findViewById(R.id.st_destaque_qualidade);
        sinopse = raiz.findViewById(R.id.st_destaque_sinopse);
        favoritar = raiz.findViewById(R.id.st_destaque_favoritar);
        pontos = raiz.findViewById(R.id.st_destaque_pontos);
        TextView assistir = raiz.findViewById(R.id.st_destaque_assistir);
        TextView detalhes = raiz.findViewById(R.id.st_destaque_detalhes);
        botoes = new TextView[]{assistir, detalhes, favoritar};

        Resources res = atividade.getResources();
        int folga = res.getDimensionPixelSize(R.dimen.st_tv_ponto_folga);
        pontoAltura = res.getDimensionPixelSize(R.dimen.st_tv_ponto) + 2 * folga;
        pontoLargura = pontoAltura;
        pontoAtivoLargura = res.getDimensionPixelSize(R.dimen.st_tv_ponto_ativo) + 2 * folga;
        // 10 px entre os pontos no desenho: a folga do anel de foco entra como margem negativa
        pontoMargem = res.getDimensionPixelSize(R.dimen.st_tv_ponto) - 2 * folga;

        int icone = res.getDimensionPixelSize(R.dimen.st_tv_botao_icone);
        Icones.noTexto(assistir, R.drawable.st_ic_assistir, icone);
        Icones.noTexto(detalhes, R.drawable.st_ic_detalhes, icone);
        Icones.noTexto(favoritar, R.drawable.st_ic_favorito, icone);

        assistir.setOnClickListener(v -> {
            Titulo t = atual();
            if (t != null) Navegacao.abrirReproducao(atividade, t.id, Navegacao.DE_ONDE_PAROU);
        });
        detalhes.setOnClickListener(v -> {
            Titulo t = atual();
            if (t != null) Navegacao.abrirTitulo(atividade, t.id, t.tipo == Tipo.SERIE);
        });
        favoritar.setOnClickListener(v -> {
            Titulo t = atual();
            if (t == null) return;
            repositorio.definirFavorito(t.id, !t.favorito);
            slides = repositorio.destaques();
            mostrar(indice, false);
            if (aoMudarFavorito != null) aoMudarFavorito.run();
        });
    }

    /** Relê os slides (estado simulado pode ter mudado) e mostra o atual. */
    void recarregar() {
        slides = repositorio.destaques();
        if (indice >= slides.size()) indice = 0;
        criarPontos();
        mostrar(indice, false);
    }

    /**
     * ↓ nos botões e nos pontos vai à fileira {@code idFileira} (a primeira visível). Sem isso,
     * ↓ em Favoritar cairia nos pontos, que ficam embaixo à direita (D10: pontos pela →).
     */
    void definirDescida(int idFileira) {
        idDescida = idFileira;
        for (TextView botao : botoes) botao.setNextFocusDownId(idFileira);
        for (ImageView ponto : listaPontos) ponto.setNextFocusDownId(idFileira);
    }

    void retomar() {
        ativo = true;
        agendar();
    }

    void pausar() {
        ativo = false;
        relogio.removeCallbacks(trocar);
    }

    /** Foco entrou ou saiu do destaque: pausa ou recomeça a contagem de 10 s. */
    void definirFocoDentro(boolean dentro) {
        if (dentro == focoDentro) return;
        focoDentro = dentro;
        agendar();
    }

    private void agendar() {
        relogio.removeCallbacks(trocar);
        if (ativo && !focoDentro && slides.size() > 1) relogio.postDelayed(trocar, TROCA_MS);
    }

    private Titulo atual() {
        return indice < slides.size() ? slides.get(indice) : null;
    }

    private void mostrar(int novo, boolean animar) {
        if (slides.isEmpty()) return;
        indice = novo;
        Titulo t = slides.get(indice);
        float canto = atividade.getResources().getDimension(R.dimen.st_tv_capa_canto);
        capa.setBackground(CapaAdapter.blocoDeCor(atividade, t.corCapa, canto, true));
        titulo.setText(t.titulo);
        ano.setText(t.ano);
        duracao.setText(atividade.getString(R.string.st_duracao_min, t.duracaoMin));
        genero.setText(t.genero);
        qualidade.setText(t.qualidade);
        sinopse.setText(t.sinopse);
        favoritar.setText(t.favorito ? R.string.st_remover_favoritos : R.string.st_favoritar);
        for (int i = 0; i < listaPontos.size(); i++) {
            ImageView ponto = listaPontos.get(i);
            boolean ativoPonto = i == indice;
            ponto.setSelected(ativoPonto);
            ViewGroup.LayoutParams lp = ponto.getLayoutParams();
            int largura = ativoPonto ? pontoAtivoLargura : pontoLargura;
            if (lp.width != largura) {
                lp.width = largura;
                ponto.setLayoutParams(lp);
            }
        }
        if (animar) {
            slide.setAlpha(0f);
            slide.animate().alpha(1f).setDuration(300).start();
        }
    }

    /** Um ponto por slide; o anel de foco de 2 dp cabe na folga em volta do ponto. */
    private void criarPontos() {
        if (listaPontos.size() == slides.size()) return;
        pontos.removeAllViews();
        listaPontos.clear();
        int folga = atividade.getResources().getDimensionPixelSize(R.dimen.st_tv_ponto_folga);
        for (int i = 0; i < slides.size(); i++) {
            final int alvo = i;
            ImageView ponto = new ImageView(atividade);
            ponto.setImageResource(R.drawable.st_ponto);
            ponto.setScaleType(ImageView.ScaleType.FIT_XY);
            ponto.setPadding(folga, folga, folga, folga);
            ponto.setContentDescription(atividade.getString(R.string.st_ir_para, slides.get(i).titulo));
            ponto.setId(View.generateViewId());
            FocoTv.aplicar(ponto);
            ponto.setOnClickListener(v -> mostrar(alvo, true));
            ponto.setNextFocusDownId(idDescida);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(pontoLargura, pontoAltura);
            if (i > 0) lp.setMarginStart(pontoMargem);
            pontos.addView(ponto, lp);
            listaPontos.add(ponto);
        }
        // → em Favoritar vai ao primeiro ponto; ← no primeiro ponto volta a Favoritar
        if (!listaPontos.isEmpty()) {
            favoritar.setNextFocusRightId(listaPontos.get(0).getId());
            listaPontos.get(0).setNextFocusLeftId(favoritar.getId());
        }
    }
}
