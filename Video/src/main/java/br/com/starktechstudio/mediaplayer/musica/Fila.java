// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.musica;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import br.com.starktechstudio.mediaplayer.dados.ModeloMusicas.Musica;

/**
 * Fila do player de música (1.5): as músicas na ordem em que vão tocar, a atual, o aleatório (sorteia a
 * ordem com a atual na frente; desligar volta à ordem original a partir dela) e o repetir (nenhuma, todas ou
 * a atual). "Tocar a seguir" entra logo depois da atual; "Pôr na fila", no fim.
 */
public final class Fila {

    public enum Repetir { NAO, TODAS, UMA }

    /** Na ordem original (a do álbum ou da playlist, mais o que entrou depois). */
    private final List<Musica> original = new ArrayList<>();
    /** Índices de {@link #original}, na ordem de tocar. */
    private final List<Integer> ordem = new ArrayList<>();
    /** Em {@link #ordem}; -1 = fila vazia. */
    private int posicao = -1;
    private boolean aleatorio;
    private Repetir repetir = Repetir.NAO;
    private final Random sorteio;

    public Fila() {
        this(new Random());
    }

    Fila(Random sorteio) {
        this.sorteio = sorteio;
    }

    /** Troca a fila: começa em {@code inicio} (na lista dada); com aleatório, a ordem é sorteada a partir dela. */
    public void definir(List<Musica> musicas, int inicio, boolean comAleatorio) {
        original.clear();
        original.addAll(musicas);
        ordem.clear();
        for (int i = 0; i < original.size(); i++) ordem.add(i);
        aleatorio = comAleatorio;
        if (original.isEmpty()) {
            posicao = -1;
            return;
        }
        int comeco = Math.max(0, Math.min(original.size() - 1, inicio));
        posicao = comeco;
        if (aleatorio) embaralharComAtualNaFrente();
    }

    public boolean vazia() {
        return posicao < 0;
    }

    @Nullable
    public Musica atual() {
        return posicao < 0 ? null : original.get(ordem.get(posicao));
    }

    /** Posição da atual em {@link #naOrdem()}. */
    public int posicao() {
        return posicao;
    }

    /** As músicas na ordem de tocar. */
    public List<Musica> naOrdem() {
        List<Musica> lista = new ArrayList<>();
        for (int i : ordem) lista.add(original.get(i));
        return lista;
    }

    public boolean aleatorio() {
        return aleatorio;
    }

    public Repetir repetir() {
        return repetir;
    }

    /**
     * A seguinte. {@code pedida} = botão Próxima (com "repetir a atual", vai para a outra); no fim da
     * música, repete a atual. No fim da fila: com "repetir todas", volta ao começo; senão, nula.
     */
    @Nullable
    public Musica proxima(boolean pedida) {
        if (posicao < 0) return null;
        if (!pedida && repetir == Repetir.UMA) return atual();
        if (posicao + 1 < ordem.size()) {
            posicao++;
            return atual();
        }
        if (repetir == Repetir.NAO) return null;
        posicao = 0;
        return atual();
    }

    /** A anterior; na primeira, com "repetir todas", a última; senão, fica na primeira. */
    @Nullable
    public Musica anterior() {
        if (posicao < 0) return null;
        if (posicao > 0) posicao--;
        else if (repetir == Repetir.TODAS) posicao = ordem.size() - 1;
        return atual();
    }

    /** Pula para a música da {@code indice} de {@link #naOrdem()}. */
    @Nullable
    public Musica irPara(int indice) {
        if (indice < 0 || indice >= ordem.size()) return atual();
        posicao = indice;
        return atual();
    }

    public void alternarAleatorio() {
        if (posicao < 0) {
            aleatorio = !aleatorio;
            return;
        }
        int atualOriginal = ordem.get(posicao);
        aleatorio = !aleatorio;
        if (aleatorio) {
            embaralharComAtualNaFrente();
        } else {
            ordem.clear();
            for (int i = 0; i < original.size(); i++) ordem.add(i);
            posicao = atualOriginal;
        }
    }

    public Repetir trocarRepetir() {
        repetir = repetir == Repetir.NAO ? Repetir.TODAS : repetir == Repetir.TODAS ? Repetir.UMA : Repetir.NAO;
        return repetir;
    }

    /** Logo depois da atual, na ordem dada. Fila vazia: vira a fila. */
    public void tocarASeguir(List<Musica> musicas) {
        if (posicao < 0) {
            definir(musicas, 0, false);
            return;
        }
        int onde = posicao + 1;
        for (Musica m : musicas) {
            original.add(m);
            ordem.add(onde++, original.size() - 1);
        }
    }

    /** No fim. Fila vazia: vira a fila. */
    public void porNoFim(List<Musica> musicas) {
        if (posicao < 0) {
            definir(musicas, 0, false);
            return;
        }
        for (Musica m : musicas) {
            original.add(m);
            ordem.add(original.size() - 1);
        }
    }

    public void limpar() {
        original.clear();
        ordem.clear();
        posicao = -1;
    }

    private void embaralharComAtualNaFrente() {
        int atualOriginal = ordem.get(posicao);
        List<Integer> resto = new ArrayList<>();
        for (int i = 0; i < original.size(); i++) if (i != atualOriginal) resto.add(i);
        Collections.shuffle(resto, sorteio);
        ordem.clear();
        ordem.add(atualOriginal);
        ordem.addAll(resto);
        posicao = 0;
    }
}
