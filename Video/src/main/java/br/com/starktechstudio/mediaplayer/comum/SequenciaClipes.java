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
 * Ordem dos vídeos de música no player (1.6, plano 007): a da sequência (álbum em vídeo ou vídeos do
 * artista) ou, com o aleatório guardado ligado ou o botão Aleatório, sorteada com o vídeo aberto na frente.
 * A ordem sorteada fica enquanto se anda por ela (próximo e anterior voltam pelo mesmo caminho); abrir um
 * vídeo pela lista ({@link #soltar}) volta à regra da preferência.
 */
public final class SequenciaClipes {

    private static String chave;
    private static List<String> ordem = new ArrayList<>();
    private static boolean sorteada;
    /** Botão Aleatório: sorteada mesmo com a preferência desligada. */
    private static boolean forcada;
    private static Random sorteio = new Random();

    private SequenciaClipes() {}

    /** Abrir um vídeo pela lista ou por Assistir: a ordem segue a preferência. */
    public static synchronized void soltar() {
        forcada = false;
        chave = null;
    }

    /** Botão Aleatório: sorteia a sequência inteira e devolve o primeiro a tocar. */
    public static synchronized String sortear(List<String> ids) {
        ordem = new ArrayList<>(ids);
        Collections.shuffle(ordem, sorteio);
        chave = chaveDe(ids);
        sorteada = true;
        forcada = true;
        return ordem.get(0);
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

    private static List<String> ordem(Context contexto, RepositorioBiblioteca repositorio, String id) {
        List<String> base = repositorio.sequenciaDoClipe(id);
        if (base.isEmpty()) return base;
        return montar(base, id, forcada || Preferencias.aleatorioMusica(contexto));
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
    }
}
