// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.Collections;
import java.util.List;

import br.com.starktechstudio.mediaplayer.dados.Modelo.Capitulo;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Episodio;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Titulo;
import br.com.starktechstudio.mediaplayer.dados.RepositorioBiblioteca;

/**
 * Reprodução simulada da F1 (o player do Nova entra na F3): um relógio de 1 s com a posição, a
 * duração e os capítulos do título ou do episódio. Pausar, ±10 s e capítulo anterior / próximo;
 * os botões de capítulo andam só entre capítulos, nunca para outro título (INTERFACE.md §4).
 * Ao parar, a posição vai para o estado simulado; no fim do vídeo, o título fica assistido.
 */
public final class ReproducaoSimulada {

    /** Avisado a cada segundo e a cada comando. */
    public interface Ouvinte {
        void aoMudar();
    }

    /** Pulo de −10 s / +10 s. */
    public static final int PULO_SEGUNDOS = 10;

    private final RepositorioBiblioteca repositorio;
    public final String id;
    public final String titulo;
    /** Episódio: "Temporada 1 · Episódio 3" (sem capítulos); filme ou show: vazio. */
    public final String linhaEpisodio;
    public final int duracao;
    public final List<Capitulo> capitulos;

    private final Handler relogio = new Handler(Looper.getMainLooper());
    private int posicao;
    private boolean tocando = true;
    private boolean rodando;
    private Ouvinte ouvinte;

    private final Runnable tique = new Runnable() {
        @Override
        public void run() {
            if (!rodando) return;
            if (tocando) {
                posicao = Math.min(duracao, posicao + 1);
                if (posicao >= duracao) tocando = false;
                avisar();
            }
            relogio.postDelayed(this, 1000);
        }
    };

    private ReproducaoSimulada(RepositorioBiblioteca repositorio, String id, String titulo, String linhaEpisodio,
                               int duracao, int posicao, List<Capitulo> capitulos) {
        this.repositorio = repositorio;
        this.id = id;
        this.titulo = titulo;
        this.linhaEpisodio = linhaEpisodio;
        this.duracao = Math.max(1, duracao);
        this.posicao = posicao;
        this.capitulos = capitulos;
    }

    /**
     * Abre o título ou o episódio {@code id}; {@code capitulo}: número (1…), {@link Navegacao#DE_ONDE_PAROU}
     * ou {@link Navegacao#DO_INICIO}. Null se o id não existir.
     */
    public static ReproducaoSimulada abrir(Context contexto, String id, int capitulo, String formatoEpisodio) {
        RepositorioBiblioteca repositorio = RepositorioBiblioteca.atual(contexto);
        Titulo t = repositorio.titulo(id);
        String nome;
        String linha = "";
        int duracao;
        int parou;
        List<Capitulo> capitulos;
        if (t != null) {
            nome = t.titulo;
            duracao = t.duracaoSegundos;
            parou = t.posicaoSegundos;
            capitulos = repositorio.capitulos(id);
        } else {
            Episodio e = repositorio.episodio(id);
            if (e == null) return null;
            nome = e.nome;
            linha = String.format(formatoEpisodio, e.temporada, e.numero);
            duracao = e.duracaoSegundos;
            parou = e.posicaoSegundos;
            capitulos = Collections.emptyList();
        }
        int inicio;
        if (capitulo == Navegacao.DO_INICIO) inicio = 0;
        else if (capitulo >= 1 && capitulo <= capitulos.size()) inicio = capitulos.get(capitulo - 1).inicioSegundos;
        else inicio = parou >= duracao ? 0 : parou;
        return new ReproducaoSimulada(repositorio, id, nome, linha, duracao, inicio, capitulos);
    }

    public void definirOuvinte(Ouvinte ouvinte) {
        this.ouvinte = ouvinte;
    }

    /** Liga o relógio (tela visível). */
    public void iniciar() {
        if (rodando) return;
        rodando = true;
        relogio.postDelayed(tique, 1000);
    }

    /** Desliga o relógio e grava a posição (tela saiu de vista). */
    public void parar() {
        rodando = false;
        relogio.removeCallbacks(tique);
        salvar();
    }

    private void salvar() {
        if (posicao >= duracao - 5) {
            repositorio.definirAssistido(id, true);
            repositorio.definirPosicao(id, 0);
        } else {
            repositorio.definirPosicao(id, posicao);
        }
    }

    public int posicao() {
        return posicao;
    }

    public boolean estaTocando() {
        return tocando;
    }

    public boolean temCapitulos() {
        return !capitulos.isEmpty();
    }

    public int progresso() {
        return (int) Math.min(100, Math.ceil(posicao * 100.0 / duracao));
    }

    public void pausarOuContinuar() {
        if (!tocando && posicao >= duracao) posicao = 0;
        tocando = !tocando;
        avisar();
    }

    /** ±segundos, dentro do vídeo. */
    public void pular(int segundos) {
        irPara(posicao + segundos);
    }

    public void irPara(int segundos) {
        posicao = Math.max(0, Math.min(duracao, segundos));
        avisar();
    }

    /** Capítulo em que a posição está; null sem capítulos. */
    public Capitulo capituloAtual() {
        Capitulo atual = null;
        for (Capitulo c : capitulos) {
            if (c.inicioSegundos <= posicao) atual = c;
        }
        return atual;
    }

    /** Próximo capítulo depois da posição; null no último ou sem capítulos. */
    public Capitulo proximoCapitulo() {
        for (Capitulo c : capitulos) {
            if (c.inicioSegundos > posicao) return c;
        }
        return null;
    }

    /** Capítulo anterior: começo do capítulo de antes (no primeiro, volta ao começo dele). */
    public Capitulo irParaCapituloAnterior() {
        Capitulo atual = capituloAtual();
        if (atual == null) return null;
        Capitulo alvo = atual.numero > 1 ? capitulos.get(atual.numero - 2) : atual;
        irPara(alvo.inicioSegundos);
        return alvo;
    }

    /** Próximo capítulo; no último não faz nada (nunca passa para outro título). */
    public Capitulo irParaProximoCapitulo() {
        Capitulo proximo = proximoCapitulo();
        if (proximo != null) irPara(proximo.inicioSegundos);
        return proximo;
    }

    public void irParaCapitulo(Capitulo capitulo) {
        irPara(capitulo.inicioSegundos);
    }

    private void avisar() {
        if (ouvinte != null) ouvinte.aoMudar();
    }
}
