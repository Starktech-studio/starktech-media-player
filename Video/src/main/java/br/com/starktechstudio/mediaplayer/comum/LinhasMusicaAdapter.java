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
 * partir dela; segurar abre o menu da música. 1.6: álbum com mais de um disco ganha "DISCO 1", "DISCO 2" antes
 * das faixas de cada um; as posições dos avisos são sempre as da lista de músicas, sem os títulos de disco.
 */
public class LinhasMusicaAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int MUSICA = 0;
    private static final int DISCO = 1;

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
    /** Cada linha da lista: o índice da música ou, num título de disco, -(número do disco). */
    private final List<Integer> linhas = new ArrayList<>();
    private boolean comDiscos;

    public LinhasMusicaAdapter(boolean numeroDaFaixa, boolean comAlbum, AoEscolher tocar, AoEscolher segurar) {
        this.numeroDaFaixa = numeroDaFaixa;
        this.comAlbum = comAlbum;
        this.tocar = tocar;
        this.segurar = segurar;
    }

    /** Só refaz se a lista mudou (as telas recebem muitos avisos durante a atualização da biblioteca). */
    public void atualizar(List<Musica> novas) {
        atualizar(novas, false);
    }

    /** {@code separarDiscos}: no álbum (1.6, D3), os títulos de disco quando há mais de um. */
    public void atualizar(List<Musica> novas, boolean separarDiscos) {
        if (iguais(musicas, novas) && separarDiscos == comDiscos) return;
        musicas.clear();
        musicas.addAll(novas);
        comDiscos = separarDiscos;
        linhas.clear();
        boolean variosDiscos = false;
        for (Musica m : musicas) if (m.disco > 1) variosDiscos = true;
        int discoAtual = -1;
        for (int i = 0; i < musicas.size(); i++) {
            int disco = Math.max(1, musicas.get(i).disco);
            if (separarDiscos && variosDiscos && disco != discoAtual) {
                linhas.add(-disco);
                discoAtual = disco;
            }
            linhas.add(i);
        }
        notifyDataSetChanged();
    }

    /** O índice da música na linha {@code posicao}; -1 num título de disco. */
    private int indiceDaMusica(int posicao) {
        int i = posicao < 0 || posicao >= linhas.size() ? -1 : linhas.get(posicao);
        return i < 0 ? -1 : i;
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
        return linhas.size();
    }

    @Override
    public int getItemViewType(int posicao) {
        return linhas.get(posicao) < 0 ? DISCO : MUSICA;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup pai, int tipo) {
        if (tipo == DISCO) {
            TextView rotulo = Textos.rotulo(pai.getContext(), "", 15);
            rotulo.setPadding(Medidas.px(pai.getContext(), 22), Medidas.px(pai.getContext(), 26), 0, Medidas.px(pai.getContext(), 10));
            rotulo.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return new RecyclerView.ViewHolder(rotulo) {
            };
        }
        return new Linha(pai.getContext());
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int posicao) {
        int i = linhas.get(posicao);
        if (i < 0) {
            TextView rotulo = (TextView) holder.itemView;
            rotulo.setText(rotulo.getContext().getString(R.string.st_musicas_disco, -i));
            return;
        }
        Musica m = musicas.get(i);
        boolean agora = posicaoTocando >= 0 ? i == posicaoTocando : m.id.equals(tocando);
        ((Linha) holder).vincular(m, numeroDaFaixa ? m.faixa : i + 1, agora, comAlbum);
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
                int i = indiceDaMusica(getBindingAdapterPosition());
                if (i >= 0) tocar.escolher(i);
            });
            caixa.setOnLongClickListener(v -> {
                int i = indiceDaMusica(getBindingAdapterPosition());
                if (i < 0) return false;
                segurar.escolher(i);
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
