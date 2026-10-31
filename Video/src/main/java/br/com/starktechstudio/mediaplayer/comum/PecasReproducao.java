// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import androidx.core.content.ContextCompat;

import com.archos.mediacenter.video.R;

import java.util.ArrayList;
import java.util.List;

import br.com.starktechstudio.mediaplayer.dados.Modelo.Capitulo;

/** Textos e marcas iguais nos players da TV e do tablet (tv-player.dc.html, tablet-player.dc.html). */
public final class PecasReproducao {

    private PecasReproducao() {}

    public static String tempo(int segundos) {
        return PecasDetalhe.tempo(segundos);
    }

    /** Começo de cada capítulo em fração da duração (marcas da barra). */
    public static List<Float> marcas(ReproducaoSimulada r) {
        List<Float> lista = new ArrayList<>();
        for (Capitulo c : r.capitulos) lista.add(c.inicioSegundos / (float) r.duracao);
        return lista;
    }

    /** "Capítulo 3 de 8 · O laboratório"; episódio sem capítulos: "Temporada 1 · Episódio 3"; senão vazio. */
    public static String linhaCapitulo(Context contexto, ReproducaoSimulada r) {
        Capitulo atual = r.capituloAtual();
        if (atual != null) {
            return contexto.getString(R.string.st_capitulo_de, atual.numero, r.capitulos.size(), atual.nome);
        }
        return r.linhaEpisodio;
    }

    /** "04 · A memória" (aviso de troca de capítulo). */
    public static String nomeCapitulo(Context contexto, Capitulo c) {
        return contexto.getString(R.string.st_capitulo_curto, c.numero, c.nome);
    }

    /** "Próximo: 04 · A memória em 00:38", com o capítulo em branco; vazio no último ou sem capítulos. */
    public static CharSequence textoProximo(Context contexto, ReproducaoSimulada r) {
        Capitulo proximo = r.proximoCapitulo();
        if (proximo == null) return "";
        SpannableStringBuilder texto = new SpannableStringBuilder(contexto.getString(R.string.st_proximo));
        int inicio = texto.length();
        texto.append(nomeCapitulo(contexto, proximo));
        texto.setSpan(new ForegroundColorSpan(ContextCompat.getColor(contexto, R.color.st_texto)),
                inicio, texto.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        texto.append(contexto.getString(R.string.st_proximo_em, tempo(proximo.inicioSegundos - r.posicao())));
        return texto;
    }
}
