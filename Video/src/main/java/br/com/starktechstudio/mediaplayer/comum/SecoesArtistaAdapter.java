// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.archos.mediacenter.video.R;

import java.util.ArrayList;
import java.util.List;

import br.com.starktechstudio.mediaplayer.dados.ModeloMusicas.Musica;

/**
 * Página do artista de Músicas (1.5.1): seções numa lista só, como as seções da aba Shows
 * ({@link SecoesShowsAdapter}). Rótulo da seção ("ÁLBUNS", "VÍDEOS"), fileiras de capas quadradas
 * ({@link CapaMusicaAdapter.Capa}, com {@code colunas} por fileira) e linhas de vídeo (número, título e
 * duração; OK ou toque abre o player de vídeo). Na TV, cada item é uma linha inteira da grade de uma coluna.
 */
public class SecoesArtistaAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface AoEscolherVideo {
        void abrir(Musica video);
    }

    private static final int ROTULO = 0;
    private static final int FILEIRA = 1;
    private static final int VIDEO = 2;

    private final boolean tv;
    private final AoEscolherVideo aoEscolher;
    private final List<Object> itens = new ArrayList<>();
    private final List<String> rotulos = new ArrayList<>();
    private final List<List<CapaMusicaAdapter.Capa>> secoesDeCapas = new ArrayList<>();
    private final List<List<Musica>> secoesDeVideos = new ArrayList<>();
    private int colunas = 6;
    private int lado;
    private int espaco;

    public SecoesArtistaAdapter(boolean tv, AoEscolherVideo aoEscolher) {
        this.tv = tv;
        this.aoEscolher = aoEscolher;
    }

    /** Colunas por fileira, o lado da capa e o espaço entre as capas, em px. */
    public void definirGrade(int colunas, int lado, int espaco) {
        if (colunas == this.colunas && lado == this.lado && espaco == this.espaco) return;
        this.colunas = Math.max(1, colunas);
        this.lado = lado;
        this.espaco = espaco;
        montar();
    }

    /** Seções na ordem: cada uma com o rótulo e capas ou vídeos (a outra lista nula). */
    public void limpar() {
        rotulos.clear();
        secoesDeCapas.clear();
        secoesDeVideos.clear();
    }

    public void secaoDeCapas(String rotulo, List<CapaMusicaAdapter.Capa> capas) {
        if (capas.isEmpty()) return;
        rotulos.add(rotulo);
        secoesDeCapas.add(capas);
        secoesDeVideos.add(null);
    }

    public void secaoDeVideos(String rotulo, List<Musica> videos) {
        if (videos.isEmpty()) return;
        rotulos.add(rotulo);
        secoesDeCapas.add(null);
        secoesDeVideos.add(videos);
    }

    /** Monta as linhas das seções definidas e avisa a lista. */
    public void montar() {
        itens.clear();
        for (int s = 0; s < rotulos.size(); s++) {
            itens.add(rotulos.get(s));
            List<CapaMusicaAdapter.Capa> capas = secoesDeCapas.get(s);
            if (capas != null) {
                for (int i = 0; i < capas.size(); i += colunas) {
                    itens.add(new ArrayList<>(capas.subList(i, Math.min(capas.size(), i + colunas))));
                }
            } else {
                itens.addAll(secoesDeVideos.get(s));
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    @Override
    public int getItemViewType(int posicao) {
        Object item = itens.get(posicao);
        return item instanceof String ? ROTULO : item instanceof Musica ? VIDEO : FILEIRA;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup pai, int tipo) {
        Context contexto = pai.getContext();
        if (tipo == ROTULO) {
            TextView rotulo = Textos.rotulo(contexto, "", 15);
            rotulo.setPadding(0, Medidas.px(contexto, 30), 0, Medidas.px(contexto, 14));
            rotulo.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return new RecyclerView.ViewHolder(rotulo) {
            };
        }
        if (tipo == VIDEO) return new LinhaVideo(contexto, tv);
        LinearLayout fileira = new LinearLayout(contexto);
        fileira.setOrientation(LinearLayout.HORIZONTAL);
        fileira.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        fileira.setPadding(0, 0, 0, espaco);
        return new RecyclerView.ViewHolder(fileira) {
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int posicao) {
        Object item = itens.get(posicao);
        if (item instanceof String) {
            ((TextView) holder.itemView).setText((String) item);
            return;
        }
        if (item instanceof Musica) {
            ((LinhaVideo) holder).vincular((Musica) item);
            return;
        }
        LinearLayout fileira = (LinearLayout) holder.itemView;
        fileira.removeAllViews();
        fileira.setPadding(0, 0, 0, espaco);
        Context contexto = fileira.getContext();
        for (CapaMusicaAdapter.Capa capa : (List<CapaMusicaAdapter.Capa>) item) {
            CapaMusicaAdapter.Item cartao = new CapaMusicaAdapter.Item(contexto, tv);
            cartao.vincular(capa, lado);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(lado, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (fileira.getChildCount() > 0) lp.setMarginStart(espaco);
            fileira.addView(cartao.itemView, lp);
        }
    }

    /** Linha de um vídeo: número, título, formato e duração. */
    final class LinhaVideo extends RecyclerView.ViewHolder {
        private final TextView numero;
        private final TextView titulo;
        private final TextView duracao;
        private Musica atual;

        LinhaVideo(Context contexto, boolean tv) {
            super(new LinearLayout(contexto));
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
            numero = Textos.novo(contexto, "", R.font.st_inter_bold, 20, R.color.st_texto_apagado);
            numero.setGravity(Gravity.CENTER);
            caixa.addView(numero, new LinearLayout.LayoutParams(Medidas.px(contexto, 56), ViewGroup.LayoutParams.WRAP_CONTENT));
            titulo = PecasMusica.linha(contexto, "", R.font.st_inter_bold, 22, R.color.st_texto);
            LinearLayout.LayoutParams lpTitulo = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lpTitulo.setMarginStart(Medidas.px(contexto, 18));
            caixa.addView(titulo, lpTitulo);
            duracao = Textos.novo(contexto, "", R.font.st_inter_semibold, 18, R.color.st_texto_apagado);
            LinearLayout.LayoutParams lpDuracao = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lpDuracao.setMarginStart(Medidas.px(contexto, 18));
            caixa.addView(duracao, lpDuracao);
            caixa.setOnClickListener(v -> {
                if (atual != null) aoEscolher.abrir(atual);
            });
        }

        void vincular(Musica v) {
            atual = v;
            numero.setText(v.faixa > 0 ? String.valueOf(v.faixa) : "▶");
            titulo.setText(v.titulo);
            duracao.setText(v.duracaoMs > 0 ? PecasMusica.tempo(v.duracaoMs) : "");
            itemView.setContentDescription(v.titulo);
            itemView.setVisibility(View.VISIBLE);
        }
    }
}
