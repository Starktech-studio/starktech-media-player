// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.archos.mediacenter.video.R;

import java.util.ArrayList;
import java.util.List;

import br.com.starktechstudio.mediaplayer.dados.ModeloMusicas;

/**
 * Grade de capas quadradas de Músicas (1.5): artistas (foto redonda), álbuns e playlists, com o nome e a
 * linha de baixo embaixo da capa. TV: foco com a borda na capa e aumento de 6 % ({@link FocoTv}); tablet:
 * toque. OK ou toque abre; segurar faz a ação do item (tocar o álbum, o artista ou a playlist). 2.0 (D16): com
 * {@link Capa#origem}, um ícone pequeno no canto de cima da capa, na cor da origem, sem texto; sem servidor
 * cadastrado, quem monta a capa passa nula ({@link PecasDetalhe#origemAMostrar}) e nenhuma capa tem o ícone.
 */
public class CapaMusicaAdapter extends RecyclerView.Adapter<CapaMusicaAdapter.Item> {

    /** Uma capa da grade. */
    public static final class Capa {
        public final String id;
        public final String nome;
        public final String linha;
        @Nullable
        public final String imagem;
        public final int cor;
        @DrawableRes
        public final int icone;
        public final boolean redonda;
        public final Runnable abrir;
        @Nullable
        public final Runnable segurar;
        /** 2.0 (D16): servidor, baixada ou aparelho, no canto da capa; nula = sem ícone (Nova playlist). */
        @Nullable
        public ModeloMusicas.Origem origem;

        public Capa(String id, String nome, String linha, @Nullable String imagem, int cor, @DrawableRes int icone,
                    boolean redonda, Runnable abrir, @Nullable Runnable segurar) {
            this.id = id;
            this.nome = nome;
            this.linha = linha;
            this.imagem = imagem;
            this.cor = cor;
            this.icone = icone;
            this.redonda = redonda;
            this.abrir = abrir;
            this.segurar = segurar;
        }

        /** A origem do canto (D16). */
        public Capa comOrigem(@Nullable ModeloMusicas.Origem origem) {
            this.origem = origem;
            return this;
        }
    }

    private final boolean tv;
    private final List<Capa> capas = new ArrayList<>();
    private int lado;

    public CapaMusicaAdapter(boolean tv) {
        this.tv = tv;
        setHasStableIds(true);
    }

    /** Lado da capa em px (a grade calcula pela largura). */
    public void definirLado(int lado) {
        if (lado == this.lado) return;
        this.lado = lado;
        notifyDataSetChanged();
    }

    /** Só refaz se mudou (as telas recebem muitos avisos durante a atualização da biblioteca). */
    public void atualizar(List<Capa> novas) {
        if (iguais(capas, novas)) {
            // As ações podem ter mudado (lista nova do repositório): troca sem refazer a tela
            capas.clear();
            capas.addAll(novas);
            return;
        }
        capas.clear();
        capas.addAll(novas);
        notifyDataSetChanged();
    }

    private static boolean iguais(List<Capa> a, List<Capa> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            Capa x = a.get(i);
            Capa y = b.get(i);
            if (!x.id.equals(y.id) || !x.nome.equals(y.nome) || !x.linha.equals(y.linha) || x.origem != y.origem
                    || (x.imagem == null ? y.imagem != null : !x.imagem.equals(y.imagem))) return false;
        }
        return true;
    }

    @Override
    public int getItemCount() {
        return capas.size();
    }

    @Override
    public long getItemId(int posicao) {
        return capas.get(posicao).id.hashCode();
    }

    @NonNull
    @Override
    public Item onCreateViewHolder(@NonNull ViewGroup pai, int tipo) {
        return new Item(pai.getContext(), tv);
    }

    @Override
    public void onBindViewHolder(@NonNull Item item, int posicao) {
        item.vincular(capas.get(posicao), lado);
    }

    static final class Item extends RecyclerView.ViewHolder {
        private final LinearLayout caixa;
        private final TextView nome;
        private final TextView linha;
        private FrameLayout capa;
        private Capa atual;

        Item(Context contexto, boolean tv) {
            super(new LinearLayout(contexto));
            caixa = (LinearLayout) itemView;
            caixa.setOrientation(LinearLayout.VERTICAL);
            caixa.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            caixa.setFocusable(true);
            caixa.setClickable(true);
            nome = PecasMusica.linha(contexto, "", R.font.st_inter_extrabold, 22, R.color.st_texto);
            linha = PecasMusica.linha(contexto, "", R.font.st_inter_semibold, 18, R.color.st_texto_apagado);
            caixa.setOnClickListener(v -> {
                if (atual != null) atual.abrir.run();
            });
            caixa.setOnLongClickListener(v -> {
                if (atual == null || atual.segurar == null) return false;
                atual.segurar.run();
                return true;
            });
            if (tv) {
                final float escala = androidx.core.content.res.ResourcesCompat.getFloat(contexto.getResources(), R.dimen.st_tv_foco_escala);
                caixa.setOnFocusChangeListener((v, temFoco) -> {
                    float alvo = temFoco ? escala : 1f;
                    v.animate().scaleX(alvo).scaleY(alvo).setDuration(150).start();
                    v.setTranslationZ(temFoco ? 1f : 0f);
                    nome.setTextColor(ContextCompat.getColor(v.getContext(), temFoco ? R.color.st_texto : R.color.st_texto_secundario));
                });
                nome.setTextColor(ContextCompat.getColor(contexto, R.color.st_texto_secundario));
            }
        }

        void vincular(Capa c, int lado) {
            atual = c;
            Context contexto = caixa.getContext();
            caixa.removeAllViews();
            int medida = lado > 0 ? lado : Medidas.px(contexto, 260);
            capa = PecasMusica.capa(contexto, medida, c.cor, c.imagem, c.icone, c.redonda);
            // A borda de foco da TV fica na capa, não no texto
            capa.setDuplicateParentStateEnabled(true);
            if (Aparelho.ehTv(contexto)) capa.setForeground(ContextCompat.getDrawable(contexto, R.drawable.st_foco_tv));
            if (c.origem != null) origemNoCanto(contexto, capa, c.origem, medida);
            caixa.addView(capa, new LinearLayout.LayoutParams(medida, medida));
            nome.setText(c.nome);
            linha.setText(c.linha);
            nome.setGravity(c.redonda ? Gravity.CENTER_HORIZONTAL : Gravity.START);
            linha.setGravity(c.redonda ? Gravity.CENTER_HORIZONTAL : Gravity.START);
            LinearLayout.LayoutParams lpNome = new LinearLayout.LayoutParams(medida, ViewGroup.LayoutParams.WRAP_CONTENT);
            lpNome.topMargin = Medidas.px(contexto, 14);
            caixa.addView(nome, lpNome);
            LinearLayout.LayoutParams lpLinha = new LinearLayout.LayoutParams(medida, ViewGroup.LayoutParams.WRAP_CONTENT);
            lpLinha.topMargin = Medidas.px(contexto, 6);
            caixa.addView(linha, lpLinha);
            caixa.setContentDescription(c.origem == null ? c.nome + ", " + c.linha
                    : c.nome + ", " + c.linha + ", " + contexto.getString(PecasDetalhe.nomeDaOrigem(c.origem)));
        }

        /** O ícone da origem (D16) no canto de cima, sobre o véu escuro dos selos de imagem; a capa redonda, mais para dentro. */
        private static void origemNoCanto(Context contexto, FrameLayout capa, ModeloMusicas.Origem origem, int medida) {
            ImageView icone = new ImageView(contexto);
            icone.setImageResource(PecasDetalhe.iconeDaOrigem(origem));
            ImageViewCompat.setImageTintList(icone, ContextCompat.getColorStateList(contexto, PecasDetalhe.corDaOrigem(origem)));
            icone.setBackgroundResource(R.drawable.st_selo_veu);
            int lado = Math.max(Medidas.px(contexto, 30), medida / 7);
            int folga = lado / 5;
            icone.setPadding(folga, folga, folga, folga);
            icone.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(lado, lado, Gravity.TOP | Gravity.END);
            int margem = capa.getClipToOutline() ? medida / 8 : Medidas.px(contexto, 10);
            lp.topMargin = margem;
            lp.setMarginEnd(margem);
            capa.addView(icone, lp);
        }
    }
}
