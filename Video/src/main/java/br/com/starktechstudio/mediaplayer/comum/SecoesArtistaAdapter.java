// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.archos.mediacenter.video.R;

import java.util.ArrayList;
import java.util.List;

import br.com.starktechstudio.mediaplayer.dados.ModeloMusicas.Musica;

/**
 * Página do artista de Músicas (1.5.1; cards na 1.7): seções numa lista só, como as seções da aba Shows
 * ({@link SecoesShowsAdapter}). Rótulo da seção ("ÁLBUNS", "VÍDEOS"), fileiras de capas quadradas
 * ({@link CapaMusicaAdapter.Capa}, com {@code colunas} por fileira) e fileiras de cards de vídeo (quadro 16:9,
 * número da ordem, duração no canto, a barrinha do que já foi visto e o título; OK ou toque abre o player).
 * Na TV, cada item é uma linha inteira da grade de uma coluna.
 */
public class SecoesArtistaAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface AoEscolherVideo {
        void abrir(Musica video);
    }

    private static final int ROTULO = 0;
    private static final int FILEIRA = 1;
    private static final int VIDEOS = 2;

    /** Uma fileira de cards de vídeo. */
    private static final class FileiraDeVideos {
        final List<Musica> videos;

        FileiraDeVideos(List<Musica> videos) {
            this.videos = videos;
        }
    }

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

    /** Colunas de capas por fileira, o lado da capa e o espaço entre elas, em px; os cards saem disso. */
    public void definirGrade(int colunas, int lado, int espaco) {
        if (colunas == this.colunas && lado == this.lado && espaco == this.espaco) return;
        this.colunas = Math.max(1, colunas);
        this.lado = lado;
        this.espaco = espaco;
        montar();
    }

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

    /** Cards por fileira: dois terços das colunas de capas (TV: 4), no mínimo 2. */
    private int colunasDeVideo() {
        return Math.max(2, colunas * 2 / 3);
    }

    private int larguraDoCard() {
        int largura = colunas * lado + (colunas - 1) * espaco;
        int n = colunasDeVideo();
        return (largura - (n - 1) * espaco) / n;
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
                List<Musica> videos = secoesDeVideos.get(s);
                int n = colunasDeVideo();
                for (int i = 0; i < videos.size(); i += n) {
                    itens.add(new FileiraDeVideos(new ArrayList<>(videos.subList(i, Math.min(videos.size(), i + n)))));
                }
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
        return item instanceof String ? ROTULO : item instanceof FileiraDeVideos ? VIDEOS : FILEIRA;
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
        LinearLayout fileira = new LinearLayout(contexto);
        fileira.setOrientation(LinearLayout.HORIZONTAL);
        fileira.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
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
        LinearLayout fileira = (LinearLayout) holder.itemView;
        fileira.removeAllViews();
        fileira.setPadding(0, 0, 0, espaco);
        Context contexto = fileira.getContext();
        if (item instanceof FileiraDeVideos) {
            int largura = larguraDoCard();
            for (Musica v : ((FileiraDeVideos) item).videos) {
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(largura, ViewGroup.LayoutParams.WRAP_CONTENT);
                if (fileira.getChildCount() > 0) lp.setMarginStart(espaco);
                fileira.addView(card(contexto, v, largura), lp);
            }
            return;
        }
        for (CapaMusicaAdapter.Capa capa : (List<CapaMusicaAdapter.Capa>) item) {
            CapaMusicaAdapter.Item cartao = new CapaMusicaAdapter.Item(contexto, tv);
            cartao.vincular(capa, lado);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(lado, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (fileira.getChildCount() > 0) lp.setMarginStart(espaco);
            fileira.addView(cartao.itemView, lp);
        }
    }

    /** Card de um vídeo: quadro 16:9 com o número e a duração, a barrinha do visto e o título em 2 linhas. */
    private View card(Context contexto, Musica v, int largura) {
        LinearLayout caixa = new LinearLayout(contexto);
        caixa.setOrientation(LinearLayout.VERTICAL);
        caixa.setFocusable(true);
        caixa.setClickable(true);
        caixa.setOnClickListener(x -> aoEscolher.abrir(v));
        int altura = largura * 9 / 16;
        float canto = contexto.getResources().getDimension(tv ? R.dimen.st_tv_capa_canto : R.dimen.st_tablet_capa_canto);
        FrameLayout quadro = new FrameLayout(contexto);
        quadro.setBackground(CapaAdapter.blocoDeCor(contexto, v.cor, canto, tv));
        quadro.setClipToOutline(true);
        quadro.setDuplicateParentStateEnabled(true);
        if (tv) quadro.setForeground(ContextCompat.getDrawable(contexto, R.drawable.st_foco_tv));
        View reserva = new android.widget.ImageView(contexto);
        reserva.setId(R.id.st_imagem_reserva);
        ((android.widget.ImageView) reserva).setImageResource(R.drawable.st_ic_assistir);
        androidx.core.widget.ImageViewCompat.setImageTintList((android.widget.ImageView) reserva,
                ContextCompat.getColorStateList(contexto, R.color.st_branco_30));
        int icone = altura / 3;
        quadro.addView(reserva, new FrameLayout.LayoutParams(icone, icone, Gravity.CENTER));
        Imagens.noFundo(quadro, v.capa, Imagens.CARTAO, canto);
        int margem = Medidas.px(contexto, 12);
        if (v.faixa > 0) {
            TextView numero = selo(contexto, String.valueOf(v.faixa));
            FrameLayout.LayoutParams lpNumero = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.START);
            lpNumero.setMargins(margem, margem, margem, margem);
            quadro.addView(numero, lpNumero);
        }
        if (v.duracaoMs > 0) {
            TextView duracao = selo(contexto, PecasMusica.tempo(v.duracaoMs));
            FrameLayout.LayoutParams lpDuracao = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.END);
            lpDuracao.setMargins(margem, margem, margem, margem + Medidas.px(contexto, 6));
            quadro.addView(duracao, lpDuracao);
        }
        // O que já foi visto: a barrinha azul embaixo (assistido = cheia)
        long total = v.duracaoMs / 1000;
        float fracao = v.assistido ? 1f : total > 0 ? Math.min(1f, v.posicaoSegundos / (float) total) : 0f;
        if (fracao > 0.01f) {
            View trilho = new View(contexto);
            trilho.setBackgroundColor(ContextCompat.getColor(contexto, R.color.st_branco_15));
            int alturaBarra = Math.max(2, Medidas.px(contexto, 6));
            quadro.addView(trilho, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, alturaBarra, Gravity.BOTTOM));
            View avanco = new View(contexto);
            avanco.setBackgroundColor(ContextCompat.getColor(contexto, R.color.st_azul));
            quadro.addView(avanco, new FrameLayout.LayoutParams(Math.round(largura * fracao), alturaBarra, Gravity.BOTTOM | Gravity.START));
        }
        caixa.addView(quadro, new LinearLayout.LayoutParams(largura, altura));
        TextView titulo = Textos.novo(contexto, v.titulo, R.font.st_inter_extrabold, 21, tv ? R.color.st_texto_secundario : R.color.st_texto);
        titulo.setMaxLines(2);
        titulo.setEllipsize(TextUtils.TruncateAt.END);
        caixa.addView(titulo, PecasMusica.comTopo(Medidas.px(contexto, 12)));
        caixa.setContentDescription(v.titulo);
        if (tv) {
            final float escala = ResourcesCompat.getFloat(contexto.getResources(), R.dimen.st_tv_foco_escala);
            caixa.setOnFocusChangeListener((x, temFoco) -> {
                float alvo = temFoco ? escala : 1f;
                x.animate().scaleX(alvo).scaleY(alvo).setDuration(150).start();
                x.setTranslationZ(temFoco ? 1f : 0f);
                titulo.setTextColor(ContextCompat.getColor(contexto, temFoco ? R.color.st_texto : R.color.st_texto_secundario));
            });
        }
        return caixa;
    }

    /** Selo escuro sobre o quadro ("3", "4:12"). */
    private static TextView selo(Context contexto, String texto) {
        TextView selo = Textos.novo(contexto, texto, R.font.st_inter_bold, 17, R.color.st_texto);
        GradientDrawable fundo = new GradientDrawable();
        fundo.setColor(ContextCompat.getColor(contexto, R.color.st_veu_72));
        fundo.setCornerRadius(Medidas.px(contexto, 8));
        selo.setBackground(fundo);
        int ph = Medidas.px(contexto, 10);
        int pv = Medidas.px(contexto, 4);
        selo.setPadding(ph, pv, ph, pv);
        return selo;
    }
}
