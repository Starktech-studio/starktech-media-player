// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.tv;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.res.Resources;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.archos.mediacenter.video.R;

import java.util.ArrayList;
import java.util.List;

import br.com.starktechstudio.mediaplayer.comum.Animacoes;
import br.com.starktechstudio.mediaplayer.comum.CapaAdapter;
import br.com.starktechstudio.mediaplayer.comum.Icones;
import br.com.starktechstudio.mediaplayer.comum.Imagens;
import br.com.starktechstudio.mediaplayer.comum.Navegacao;
import br.com.starktechstudio.mediaplayer.comum.PecasDetalhe;
import br.com.starktechstudio.mediaplayer.comum.Preferencias;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Episodio;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Tipo;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Titulo;
import br.com.starktechstudio.mediaplayer.dados.RepositorioBiblioteca;

/**
 * Destaque "Novidades na biblioteca" do Início da TV: os slides de
 * {@link RepositorioBiblioteca#destaques()}, trocados no tempo de Configurações (10 s de fábrica,
 * pode estar desligada) enquanto o foco está fora dele.
 * Só há o botão Assistir, que abre a reprodução; segurar OK nele abre o Menu da capa.
 * ←/→ em Assistir trocam o item em laço (fade e deslize de 250 ms; sem animação com "remover
 * animações") e os pontos só indicam, sem foco. O botão fica parado na troca: só a capa e os
 * textos do item se movem.
 * Expansão (D2): o foco em Assistir expande o item (borda de foco, área da imagem maior, com o
 * fundo no lugar da capa); o primeiro ↓ recolhe e o segundo desce à fileira. A prévia em vídeo
 * ({@link PreviaVideoTv}) se liga por {@link #definirExpansao}.
 */
final class DestaqueTv {

    /**
     * Quem reage ao item expandido (a prévia em vídeo).
     * {@link #aoExpandir} chega quando o foco entra em Assistir, ao voltar de outra tela com o
     * foco nele e a cada item novo com o destaque expandido (o novo substitui o anterior, sem
     * {@link #aoRecolher} no meio). {@link #aoRecolher} chega no primeiro ↓, quando o foco sai e
     * em onPause (abrir o player passa por aqui): é a hora de soltar tudo.
     * {@link #aoAbrirPlayer} chega no Assistir: solta tudo e só então chama {@code abrir}, porque o
     * player pode precisar do mesmo decodificador da TV.
     */
    interface Expansao {
        void aoExpandir(Titulo titulo);

        void aoRecolher();

        void aoAbrirPlayer(Runnable abrir);
    }

    private static final long TRANSICAO_MS = 250;

    private final Activity atividade;
    private final RepositorioBiblioteca repositorio;
    private final Handler relogio = new Handler(Looper.getMainLooper());

    private final View item;
    private final View capa;
    private final TextView titulo;
    private final TextView ano;
    private final TextView duracao;
    private final TextView genero;
    private final TextView qualidade;
    private final TextView sinopse;
    private final TextView assistir;
    private final LinearLayout pontos;
    private final List<ImageView> listaPontos = new ArrayList<>();
    /** O que se move na troca de item. */
    private final View[] partesDoItem;
    private final GradientDrawable bordaExpandido;

    private final int pontoAltura;
    private final int pontoLargura;
    private final int pontoAtivoLargura;
    private final int pontoMargem;
    private final float deslize;
    private final int itemAltura;
    private final int itemAlturaExpandido;
    private final int capaLargura;
    private final int capaLarguraExpandida;
    private final int capaAltura;
    private final int capaAlturaExpandida;

    /** Tamanho do item: 0 recolhido, 1 expandido. */
    private float tamanho;
    @Nullable
    private ValueAnimator animadorTamanho;

    private List<Titulo> slides = new ArrayList<>();
    private int indice;
    private boolean focoDentro;
    private boolean ativo;
    private boolean expandido;
    /** Item que a {@link Expansao} recebeu por último. */
    @Nullable
    private String idExpandido;
    @Nullable
    private Expansao expansao;

    private final Runnable trocar = new Runnable() {
        @Override
        public void run() {
            if (slides.size() > 1) mostrar((indice + 1) % slides.size(), 1);
            agendar();
        }
    };

    DestaqueTv(Activity atividade, View raiz, RepositorioBiblioteca repositorio) {
        this.atividade = atividade;
        this.repositorio = repositorio;

        item = raiz;
        capa = raiz.findViewById(R.id.st_destaque_capa);
        titulo = raiz.findViewById(R.id.st_destaque_titulo);
        ano = raiz.findViewById(R.id.st_destaque_ano);
        duracao = raiz.findViewById(R.id.st_destaque_duracao);
        genero = raiz.findViewById(R.id.st_destaque_genero);
        qualidade = raiz.findViewById(R.id.st_destaque_qualidade);
        sinopse = raiz.findViewById(R.id.st_destaque_sinopse);
        pontos = raiz.findViewById(R.id.st_destaque_pontos);
        assistir = raiz.findViewById(R.id.st_destaque_assistir);
        partesDoItem = new View[]{capa, raiz.findViewById(R.id.st_destaque_textos)};

        Resources res = atividade.getResources();
        int folga = res.getDimensionPixelSize(R.dimen.st_tv_ponto_folga);
        pontoAltura = res.getDimensionPixelSize(R.dimen.st_tv_ponto) + 2 * folga;
        pontoLargura = pontoAltura;
        pontoAtivoLargura = res.getDimensionPixelSize(R.dimen.st_tv_ponto_ativo) + 2 * folga;
        // 10 px entre os pontos no desenho: a folga em volta de cada um entra como margem negativa
        pontoMargem = res.getDimensionPixelSize(R.dimen.st_tv_ponto) - 2 * folga;
        deslize = res.getDimension(R.dimen.st_tv_novidades_deslize);
        itemAltura = res.getDimensionPixelSize(R.dimen.st_tv_destaque_altura);
        itemAlturaExpandido = res.getDimensionPixelSize(R.dimen.st_tv_destaque_altura_expandido);
        capaLargura = res.getDimensionPixelSize(R.dimen.st_tv_destaque_capa_largura);
        capaLarguraExpandida = res.getDimensionPixelSize(R.dimen.st_tv_destaque_capa_largura_expandida);
        capaAltura = res.getDimensionPixelSize(R.dimen.st_tv_destaque_capa_altura);
        capaAlturaExpandida = res.getDimensionPixelSize(R.dimen.st_tv_destaque_capa_altura_expandida);

        bordaExpandido = new GradientDrawable();
        bordaExpandido.setShape(GradientDrawable.RECTANGLE);
        bordaExpandido.setCornerRadius(res.getDimension(R.dimen.st_tv_destaque_canto));
        bordaExpandido.setStroke(res.getDimensionPixelSize(R.dimen.st_tv_foco_borda),
                ContextCompat.getColor(atividade, R.color.st_texto));

        Icones.noTexto(assistir, R.drawable.st_ic_assistir, res.getDimensionPixelSize(R.dimen.st_tv_botao_icone));

        assistir.setOnClickListener(v -> {
            Titulo t = atual();
            if (t == null) return;
            // Série: o próximo episódio a ver
            if (t.tipo == Tipo.SERIE) {
                Episodio proximo = PecasDetalhe.proximoEpisodio(repositorio.temporadas(t.id));
                if (proximo != null) abrirReproducao(proximo.id);
                return;
            }
            abrirReproducao(t.id);
        });
        // Segurar OK: Menu da capa (favoritar, marcar como assistido, editar)
        assistir.setOnLongClickListener(v -> {
            Titulo t = atual();
            if (t == null) return false;
            Navegacao.abrirMenuCapa(atividade, t.id);
            return true;
        });
        assistir.setOnKeyListener((v, tecla, evento) -> aoTeclar(tecla, evento));
    }

    /** Relê os slides (o estado ou a biblioteca podem ter mudado) e mostra o atual. */
    void recarregar() {
        slides = repositorio.destaques();
        if (indice >= slides.size()) indice = 0;
        criarPontos();
        mostrar(indice, 0);
        // A lista pode ter mudado com o item expandido: a expansão acompanha o item da tela
        if (expandido) {
            if (atual() == null) recolher();
            else expandir();
        }
    }

    /** ↓ em Assistir, já recolhido, vai à fileira {@code idFileira} (a primeira visível). */
    void definirDescida(int idFileira) {
        assistir.setNextFocusDownId(idFileira);
    }

    /** Liga quem reage ao item expandido. */
    void definirExpansao(@Nullable Expansao ouvinte) {
        expansao = ouvinte;
    }

    void retomar() {
        boolean voltando = !ativo;
        ativo = true;
        agendar();
        // Só na volta de outra tela: a biblioteca mudar não desfaz o recolher do ↓
        if (voltando) expandir();
    }

    void pausar() {
        ativo = false;
        relogio.removeCallbacks(trocar);
        recolher();
        if (animadorTamanho != null) animadorTamanho.end();
    }

    /** Foco entrou ou saiu do destaque: expande ou recolhe; pausa ou recomeça a contagem da troca. */
    void definirFocoDentro(boolean dentro) {
        if (dentro == focoDentro) return;
        focoDentro = dentro;
        agendar();
        if (dentro) expandir();
        else recolher();
    }

    private void agendar() {
        relogio.removeCallbacks(trocar);
        long troca = Preferencias.trocaNovidadesMs(atividade);
        if (troca > 0 && ativo && !focoDentro && slides.size() > 1) relogio.postDelayed(trocar, troca);
    }

    private Titulo atual() {
        return indice < slides.size() ? slides.get(indice) : null;
    }

    /** O player abre depois de soltar a prévia. */
    private void abrirReproducao(String id) {
        Runnable abrir = () -> Navegacao.abrirReproducao(atividade, id, Navegacao.DE_ONDE_PAROU);
        if (expansao != null) expansao.aoAbrirPlayer(abrir);
        else abrir.run();
    }

    /**
     * Teclas em Assistir. ← e → sempre ficam aqui: a busca de foco cairia numa fileira de baixo.
     * ↓ expandido recolhe; recolhido segue para a rolagem, que desce à fileira de
     * {@link #definirDescida}. Tecla segurada não repete: um toque longo em ↓ não passa do
     * recolher à fileira.
     */
    private boolean aoTeclar(int tecla, KeyEvent evento) {
        boolean seta = tecla == KeyEvent.KEYCODE_DPAD_LEFT || tecla == KeyEvent.KEYCODE_DPAD_RIGHT;
        if (!seta && tecla != KeyEvent.KEYCODE_DPAD_DOWN) return false;
        if (evento.getAction() != KeyEvent.ACTION_DOWN) return false;
        if (evento.getRepeatCount() > 0) return true;
        if (seta) {
            passar(tecla == KeyEvent.KEYCODE_DPAD_RIGHT ? 1 : -1);
            return true;
        }
        if (expandido) {
            recolher();
            return true;
        }
        return false;
    }

    /** Item seguinte (1) ou anterior (-1), em laço; quem passa o item volta a vê-lo expandido. */
    private void passar(int sentido) {
        if (slides.size() < 2) return;
        mostrar((indice + sentido + slides.size()) % slides.size(), sentido);
        expandir();
    }

    private void expandir() {
        Titulo t = atual();
        if (!ativo || !focoDentro || t == null) return;
        if (expandido && t.id.equals(idExpandido)) return;
        // Item novo com o destaque já expandido: mostrar() já trouxe o fundo dele
        if (!expandido) {
            expandido = true;
            item.setForeground(bordaExpandido);
            mostrarImagem(t);
            animarTamanho(1f);
        }
        idExpandido = t.id;
        if (expansao != null) expansao.aoExpandir(t);
    }

    private void recolher() {
        if (!expandido) return;
        expandido = false;
        idExpandido = null;
        if (expansao != null) expansao.aoRecolher();
        item.setForeground(null);
        Titulo t = atual();
        if (t != null) mostrarImagem(t);
        animarTamanho(0f);
    }

    /** Expandido: o fundo (sem ele, o quadro do vídeo; sem nada, fica a capa). Recolhido: a capa. */
    private void mostrarImagem(Titulo t) {
        float canto = atividade.getResources().getDimension(R.dimen.st_tv_capa_canto);
        String larga = expandido ? t.imagemCartao() : null;
        String imagem = larga != null ? larga : t.imagemCapa();
        // Sem imagem, o bloco de cor: a imagem do outro estado não pode ficar na área
        if (imagem == null) capa.setBackground(CapaAdapter.blocoDeCor(atividade, t.corCapa, canto, true));
        Imagens.noFundo(capa, imagem, larga != null ? Imagens.FUNDO : Imagens.CAPA, canto);
    }

    /** Leva o item ao {@code alvo} (0 recolhido, 1 expandido); sem animação fora de cena ou com "remover animações". */
    private void animarTamanho(float alvo) {
        if (animadorTamanho != null) animadorTamanho.cancel();
        animadorTamanho = null;
        if (!ativo || !Animacoes.ligadas(atividade)) {
            definirTamanho(alvo);
            return;
        }
        ValueAnimator animador = ValueAnimator.ofFloat(tamanho, alvo);
        animador.setDuration(TRANSICAO_MS);
        animador.setInterpolator(new DecelerateInterpolator(1.6f));
        animador.addUpdateListener(a -> definirTamanho((float) a.getAnimatedValue()));
        animadorTamanho = animador;
        animador.start();
    }

    private void definirTamanho(float novo) {
        tamanho = novo;
        ViewGroup.LayoutParams lpItem = item.getLayoutParams();
        lpItem.height = entre(itemAltura, itemAlturaExpandido, novo);
        item.setLayoutParams(lpItem);
        ViewGroup.LayoutParams lpCapa = capa.getLayoutParams();
        lpCapa.width = entre(capaLargura, capaLarguraExpandida, novo);
        lpCapa.height = entre(capaAltura, capaAlturaExpandida, novo);
        capa.setLayoutParams(lpCapa);
    }

    private static int entre(int de, int para, float fracao) {
        return Math.round(de + (para - de) * fracao);
    }

    /** Mostra o slide {@code novo}; {@code sentido} é o lado de onde ele entra (1 ou -1; 0: sem animação). */
    private void mostrar(int novo, int sentido) {
        if (slides.isEmpty()) return;
        indice = novo;
        Titulo t = slides.get(indice);
        float canto = atividade.getResources().getDimension(R.dimen.st_tv_capa_canto);
        capa.setBackground(CapaAdapter.blocoDeCor(atividade, t.corCapa, canto, true));
        mostrarImagem(t);
        titulo.setText(t.titulo);
        ano.setText(t.ano);
        duracao.setText(atividade.getString(R.string.st_duracao_min, t.duracaoMin));
        genero.setText(t.genero);
        qualidade.setText(t.qualidade);
        sinopse.setText(t.sinopse);
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
        animarEntrada(sentido);
    }

    private void animarEntrada(int sentido) {
        boolean animar = sentido != 0 && Animacoes.ligadas(atividade);
        for (View parte : partesDoItem) {
            parte.animate().cancel();
            parte.setAlpha(animar ? 0f : 1f);
            parte.setTranslationX(animar ? sentido * deslize : 0f);
            if (animar) {
                parte.animate().alpha(1f).translationX(0f).setDuration(TRANSICAO_MS)
                        .setInterpolator(new DecelerateInterpolator(1.6f)).start();
            }
        }
    }

    /** Um ponto por slide: só indica o item atual, sem foco. */
    private void criarPontos() {
        if (listaPontos.size() == slides.size()) return;
        pontos.removeAllViews();
        listaPontos.clear();
        int folga = atividade.getResources().getDimensionPixelSize(R.dimen.st_tv_ponto_folga);
        for (int i = 0; i < slides.size(); i++) {
            ImageView ponto = new ImageView(atividade);
            ponto.setImageResource(R.drawable.st_ponto);
            ponto.setScaleType(ImageView.ScaleType.FIT_XY);
            ponto.setPadding(folga, folga, folga, folga);
            ponto.setFocusable(false);
            ponto.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(pontoLargura, pontoAltura);
            if (i > 0) lp.setMarginStart(pontoMargem);
            pontos.addView(ponto, lp);
            listaPontos.add(ponto);
        }
    }
}
