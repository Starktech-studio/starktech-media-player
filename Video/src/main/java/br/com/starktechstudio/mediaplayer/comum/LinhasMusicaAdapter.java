// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.content.Context;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.archos.mediacenter.video.R;

import java.util.ArrayList;
import java.util.List;

import br.com.starktechstudio.mediaplayer.dados.ModeloMusicas.Musica;

/**
 * Lista de músicas (1.5: álbum, playlist e a fila do Tocando agora): número da faixa (ou a posição), nome,
 * artista e duração. A que está tocando fica em azul, com o ícone no lugar do número. OK ou toque toca a
 * partir dela; segurar abre o menu da música.
 */
public class LinhasMusicaAdapter extends RecyclerView.Adapter<LinhasMusicaAdapter.Linha> {

    public interface AoEscolher {
        void escolher(int posicao);
    }

    private final List<Musica> musicas = new ArrayList<>();
    private final AoEscolher tocar;
    private final AoEscolher segurar;
    /** true = o número é o da faixa no disco (álbum); false = a posição na lista (playlist, fila). */
    private final boolean numeroDaFaixa;
    /** true = a linha de baixo traz o álbum (playlist, fila); no álbum, só o artista. */
    private final boolean comAlbum;
    private String tocando;
    private int posicaoTocando = -1;

    public LinhasMusicaAdapter(boolean numeroDaFaixa, boolean comAlbum, AoEscolher tocar, AoEscolher segurar) {
        this.numeroDaFaixa = numeroDaFaixa;
        this.comAlbum = comAlbum;
        this.tocar = tocar;
        this.segurar = segurar;
    }

    /** Só refaz se a lista mudou (as telas recebem muitos avisos durante a atualização da biblioteca). */
    public void atualizar(List<Musica> novas) {
        if (iguais(musicas, novas)) return;
        musicas.clear();
        musicas.addAll(novas);
        notifyDataSetChanged();
    }

    private static boolean iguais(List<Musica> a, List<Musica> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            Musica x = a.get(i);
            Musica y = b.get(i);
            if (!x.id.equals(y.id) || !eq(x.titulo, y.titulo) || !eq(x.artista, y.artista) || !eq(x.capa, y.capa)) return false;
        }
        return true;
    }

    /** Marca a que está tocando: pela posição (fila, onde a mesma música pode estar duas vezes) ou pelo id. */
    public void definirTocando(String id, int posicao) {
        if (eq(id, tocando) && posicao == posicaoTocando) return;
        tocando = id;
        posicaoTocando = posicao;
        notifyDataSetChanged();
    }

    private static boolean eq(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    public Musica musica(int posicao) {
        return musicas.get(posicao);
    }

    @Override
    public int getItemCount() {
        return musicas.size();
    }

    @NonNull
    @Override
    public Linha onCreateViewHolder(@NonNull ViewGroup pai, int tipo) {
        return new Linha(pai.getContext());
    }

    @Override
    public void onBindViewHolder(@NonNull Linha linha, int posicao) {
        Musica m = musicas.get(posicao);
        boolean agora = posicaoTocando >= 0 ? posicao == posicaoTocando : m.id.equals(tocando);
        linha.vincular(m, numeroDaFaixa ? m.faixa : posicao + 1, agora, comAlbum);
    }

    final class Linha extends RecyclerView.ViewHolder {
        private final TextView numero;
        private final ImageView icone;
        private final TextView titulo;
        private final TextView sub;
        private final TextView duracao;

        Linha(Context contexto) {
            super(new LinearLayout(contexto));
            boolean tv = Aparelho.ehTv(contexto);
            LinearLayout caixa = (LinearLayout) itemView;
            caixa.setOrientation(LinearLayout.HORIZONTAL);
            caixa.setGravity(Gravity.CENTER_VERTICAL);
            caixa.setBackgroundResource(R.drawable.st_linha_musica);
            caixa.setFocusable(true);
            caixa.setClickable(true);
            int lateral = Medidas.px(contexto, 22);
            caixa.setPadding(lateral, 0, lateral, 0);
            caixa.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, PecasMusica.alturaLinha(contexto)));
            if (tv) caixa.setForeground(ContextCompat.getDrawable(contexto, R.drawable.st_foco_tv));

            int larguraNumero = Medidas.px(contexto, 56);
            numero = Textos.novo(contexto, "", R.font.st_inter_bold, 20, R.color.st_texto_apagado);
            numero.setGravity(Gravity.CENTER);
            caixa.addView(numero, new LinearLayout.LayoutParams(larguraNumero, ViewGroup.LayoutParams.WRAP_CONTENT));
            icone = new ImageView(contexto);
            icone.setImageResource(R.drawable.st_ic_musicas);
            ImageViewCompat.setImageTintList(icone, ContextCompat.getColorStateList(contexto, R.color.st_azul));
            int lado = Medidas.px(contexto, 28);
            LinearLayout.LayoutParams lpIcone = new LinearLayout.LayoutParams(lado, lado);
            lpIcone.setMarginStart((larguraNumero - lado) / 2);
            lpIcone.setMarginEnd((larguraNumero - lado) / 2);
            caixa.addView(icone, lpIcone);

            LinearLayout textos = new LinearLayout(contexto);
            textos.setOrientation(LinearLayout.VERTICAL);
            titulo = PecasMusica.linha(contexto, "", R.font.st_inter_bold, 22, R.color.st_texto);
            textos.addView(titulo);
            sub = PecasMusica.linha(contexto, "", R.font.st_inter_semibold, 17, R.color.st_texto_apagado);
            textos.addView(sub, PecasMusica.comTopo(Medidas.px(contexto, 6)));
            LinearLayout.LayoutParams lpTextos = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lpTextos.setMarginStart(Medidas.px(contexto, 18));
            caixa.addView(textos, lpTextos);

            duracao = Textos.novo(contexto, "", R.font.st_inter_semibold, 18, R.color.st_texto_apagado);
            LinearLayout.LayoutParams lpDuracao = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lpDuracao.setMarginStart(Medidas.px(contexto, 18));
            caixa.addView(duracao, lpDuracao);

            caixa.setOnClickListener(v -> {
                int p = getBindingAdapterPosition();
                if (p != RecyclerView.NO_POSITION) tocar.escolher(p);
            });
            caixa.setOnLongClickListener(v -> {
                int p = getBindingAdapterPosition();
                if (p == RecyclerView.NO_POSITION) return false;
                segurar.escolher(p);
                return true;
            });
        }

        void vincular(Musica m, int numeroMostrado, boolean agora, boolean comAlbum) {
            Context contexto = itemView.getContext();
            itemView.setSelected(agora);
            numero.setText(numeroMostrado > 0 ? String.valueOf(numeroMostrado) : "–");
            numero.setVisibility(agora ? android.view.View.GONE : android.view.View.VISIBLE);
            icone.setVisibility(agora ? android.view.View.VISIBLE : android.view.View.GONE);
            titulo.setText(m.titulo);
            titulo.setTextColor(ContextCompat.getColor(contexto, agora ? R.color.st_azul : R.color.st_texto));
            sub.setText(comAlbum ? PecasMusica.artistaEAlbum(contexto, m) : PecasMusica.artista(contexto, m.artista));
            duracao.setText(m.duracaoMs > 0 ? PecasMusica.tempo(m.duracaoMs) : "");
            itemView.setContentDescription(m.titulo + ", " + sub.getText());
        }
    }
}
