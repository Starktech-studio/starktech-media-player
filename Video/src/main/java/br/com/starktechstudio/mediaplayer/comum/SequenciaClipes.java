// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.content.Context;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

import br.com.starktechstudio.mediaplayer.dados.RepositorioBiblioteca;

/**
 * Ordem dos vídeos de música no player (1.6 e 1.7): a da sequência (álbum em vídeo ou vídeos do artista) ou,
 * com o aleatório guardado ligado ou o botão Aleatório, sorteada com o vídeo aberto na frente. A ordem fica
 * enquanto se anda por ela (próximo e anterior voltam pelo mesmo caminho). Uma lista dada pela tela
 * ({@link #usar}: todos os vídeos do artista) vale até abrir outro vídeo pela lista ({@link #soltar}). O botão
 * Aleatório do player ({@link #alternar}) liga ou desliga na hora, a partir do vídeo da tela.
 */
public final class SequenciaClipes {

    private static String chave;
    private static List<String> ordem = new ArrayList<>();
    /** A lista na ordem natural (da sequência ou a dada pela tela). */
    private static List<String> original = new ArrayList<>();
    private static boolean sorteada;
    /** Lista dada pela tela: vale mesmo que não seja a sequência do vídeo. */
    private static boolean forcada;
    private static Random sorteio = new Random();

    private SequenciaClipes() {}

    /** Abrir um vídeo pela lista: a ordem volta a ser a da sequência dele, pela preferência. */
    public static synchronized void soltar() {
        forcada = false;
        chave = null;
    }

    /** A tela dá a lista (todos os vídeos do artista, 1.7): na ordem ou sorteada; devolve o primeiro a tocar. */
    public static synchronized String usar(List<String> ids, boolean sortear) {
        original = new ArrayList<>(ids);
        ordem = new ArrayList<>(ids);
        if (sortear) Collections.shuffle(ordem, sorteio);
        chave = chaveDe(ids);
        sorteada = sortear;
        forcada = true;
        return ordem.get(0);
    }

    /** Botão Aleatório (1.6): sorteia a lista inteira e devolve o primeiro. */
    public static synchronized String sortear(List<String> ids) {
        return usar(ids, true);
    }

    @Nullable
    public static synchronized String seguinte(Context contexto, RepositorioBiblioteca repositorio, String id) {
        List<String> lista = ordem(contexto, repositorio, id);
        int i = lista.indexOf(id);
        return i >= 0 && i + 1 < lista.size() ? lista.get(i + 1) : null;
    }

    @Nullable
    public static synchronized String anterior(Context contexto, RepositorioBiblioteca repositorio, String id) {
        List<String> lista = ordem(contexto, repositorio, id);
        int i = lista.indexOf(id);
        return i > 0 ? lista.get(i - 1) : null;
    }

    /** O aleatório está valendo para este vídeo (o do botão do player, em azul). */
    public static synchronized boolean sorteadaAgora(Context contexto, RepositorioBiblioteca repositorio, String id) {
        ordem(contexto, repositorio, id);
        return sorteada;
    }

    /**
     * Botão Aleatório do player (1.7): liga ou desliga o aleatório guardado e refaz a ordem na hora, com o vídeo
     * da tela na frente (ligado) ou no lugar dele (desligado). Devolve o novo estado.
     */
    public static synchronized boolean alternar(Context contexto, RepositorioBiblioteca repositorio, String id) {
        boolean novo = !sorteadaAgora(contexto, repositorio, id);
        Preferencias.definirAleatorioMusica(contexto, novo);
        List<String> base = forcada && original.contains(id) ? original : repositorio.sequenciaDoClipe(id);
        if (base.isEmpty()) return novo;
        boolean manter = forcada && original.contains(id);
        chave = null;
        montar(base, id, novo);
        forcada = manter;
        if (manter) original = new ArrayList<>(base);
        return novo;
    }

    private static List<String> ordem(Context contexto, RepositorioBiblioteca repositorio, String id) {
        if (forcada && ordem.contains(id)) return ordem;
        List<String> base = repositorio.sequenciaDoClipe(id);
        if (base.isEmpty()) return base;
        forcada = false;
        return montar(base, id, Preferencias.aleatorioMusica(contexto));
    }

    /** A ordem que vale: a guardada, se é da mesma sequência, tem o vídeo e o mesmo modo; senão, uma nova. */
    static List<String> montar(List<String> base, String id, boolean querSorteada) {
        String nova = chaveDe(base);
        if (nova.equals(chave) && ordem.contains(id) && ordem.size() == base.size() && sorteada == querSorteada) return ordem;
        ordem = new ArrayList<>(base);
        if (querSorteada) {
            ordem.remove(id);
            Collections.shuffle(ordem, sorteio);
            ordem.add(0, id);
        }
        original = new ArrayList<>(base);
        chave = nova;
        sorteada = querSorteada;
        return ordem;
    }

    /** Mesma sequência, qualquer ordem. */
    private static String chaveDe(List<String> ids) {
        return new TreeSet<>(ids).toString();
    }

    /** Testes: sorteio repetível. */
    static synchronized void definirSorteio(Random novo) {
        sorteio = novo;
        soltar();
        ordem = new ArrayList<>();
        original = new ArrayList<>();
    }
}
