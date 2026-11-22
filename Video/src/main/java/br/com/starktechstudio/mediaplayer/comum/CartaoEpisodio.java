// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.archos.mediacenter.video.R;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import br.com.starktechstudio.mediaplayer.dados.Modelo.Episodio;
import br.com.starktechstudio.mediaplayer.dados.RepositorioBiblioteca;

/**
 * Cartão de um episódio na Série (tv-serie.dc.html; tablet nas mesmas peças, D12): miniatura com
 * a prévia (2.0.1: a imagem do episódio do TMDB ou o quadro do vídeo; sem ela, o número), "EPISÓDIO N",
 * nome (TMDB ou, sem ele, o do arquivo sem a extensão), a duração ("42 min"), barra de progresso e o selo
 * Assistido (verde) ou Em andamento (azul); episódio novo fica sem selo. OK ou toque abre o player
 * no episódio, de onde parou. Fica numa grade dentro da página que rola ({@link Grade}).
 */
public final class CartaoEpisodio {

    /** Episódio escolhido (OK na TV, toque no tablet). */
    public interface AoEscolher {
        void escolher(Episodio episodio);
    }

    public final View itemView;
    private final boolean tv;
    private final View miniatura;
    private final TextView numero;
    private final TextView rotulo;
    private final TextView nome;
    /** A linha de baixo: só a duração (2.0.1; antes, "Arquivo 01.mkv · 42 min"). */
    private final TextView duracao;
    private final ProgressBar progresso;
    private final TextView selo;
    private Episodio episodio;

    private CartaoEpisodio(View item, boolean tv, AoEscolher aoEscolher) {
        itemView = item;
        this.tv = tv;
        miniatura = item.findViewById(R.id.st_episodio_miniatura);
        numero = item.findViewById(R.id.st_episodio_numero);
        rotulo = item.findViewById(R.id.st_episodio_rotulo);
        nome = item.findViewById(R.id.st_episodio_nome);
        duracao = item.findViewById(R.id.st_episodio_arquivo);
        progresso = item.findViewById(R.id.st_episodio_progresso);
        selo = item.findViewById(R.id.st_episodio_selo);
        item.setOnClickListener(v -> {
            if (episodio != null && aoEscolher != null) aoEscolher.escolher(episodio);
        });
        if (!tv) item.setClipToOutline(true);
    }

    public static CartaoEpisodio criar(ViewGroup pai, boolean tv, AoEscolher aoEscolher) {
        View item = LayoutInflater.from(pai.getContext()).inflate(
                tv ? R.layout.st_tv_item_episodio : R.layout.st_tablet_item_episodio, pai, false);
        return new CartaoEpisodio(item, tv, aoEscolher);
    }

    /** O episódio do cartão (nulo antes do primeiro {@link #vincular}). */
    public Episodio episodio() {
        return episodio;
    }

    /**
     * Quadros que chegaram com a Série aberta ({@link RepositorioBiblioteca#lerQuadrosDaSerie}): refaz só os
     * cartões cuja imagem mudou, na mesma view, sem refazer a grade nem tirar o foco.
     */
    public static void atualizarImagens(List<CartaoEpisodio> cartoes, RepositorioBiblioteca repositorio) {
        for (CartaoEpisodio cartao : cartoes) {
            if (cartao.episodio == null) continue;
            Episodio novo = repositorio.episodio(cartao.episodio.id);
            if (novo != null && !Objects.equals(novo.miniatura, cartao.episodio.miniatura)) cartao.vincular(novo);
        }
    }

    public void vincular(Episodio e) {
        episodio = e;
        Context contexto = itemView.getContext();
        Resources res = contexto.getResources();
        float canto = res.getDimension(tv ? R.dimen.st_tv_episodio_miniatura_canto : R.dimen.st_tablet_episodio_miniatura_canto);
        miniatura.setBackground(CapaAdapter.blocoDeCor(contexto, e.corMiniatura, canto, tv));
        // Prévia (2.0.1): com a imagem, o número some; sem ela (ou até ela chegar), fica o número sobre a cor
        Imagens.noFundo(miniatura, e.miniatura, Imagens.CARTAO, canto, numero);
        numero.setText(String.format(Locale.ROOT, "%02d", e.numero));
        rotulo.setText(contexto.getString(R.string.st_episodio_n, e.numero));
        nome.setText(e.nome);
        // Só o tempo; duração ainda não lida: a linha some
        duracao.setText(contexto.getString(R.string.st_duracao_min, e.duracaoMin));
        duracao.setVisibility(e.duracaoMin > 0 ? View.VISIBLE : View.GONE);

        boolean andamento = e.emAndamento();
        progresso.setVisibility(andamento ? View.VISIBLE : View.GONE);
        if (andamento) {
            progresso.setProgress((int) Math.min(100, Math.ceil(e.posicaoSegundos * 100.0 / e.duracaoSegundos)));
        }

        int icone = res.getDimensionPixelSize(tv ? R.dimen.st_tv_episodio_selo_icone : R.dimen.st_tablet_episodio_selo_icone);
        if (e.assistido) {
            selo.setVisibility(View.VISIBLE);
            selo.setText(R.string.st_assistido);
            selo.setTextColor(ContextCompat.getColor(contexto, R.color.st_selo_hd_texto));
            selo.setBackgroundResource(R.drawable.st_selo_episodio_assistido);
            Icones.noTexto(selo, R.drawable.st_ic_check, icone);
        } else if (andamento) {
            selo.setVisibility(View.VISIBLE);
            selo.setText(R.string.st_em_andamento);
            selo.setTextColor(ContextCompat.getColor(contexto, R.color.st_texto));
            selo.setBackgroundResource(R.drawable.st_selo_destaque);
            selo.setCompoundDrawablesRelative(null, null, null, null);
        } else {
            selo.setVisibility(View.GONE);
        }
        itemView.setContentDescription(contexto.getString(R.string.st_episodio_descricao, e.numero, e.nome));
    }
}
