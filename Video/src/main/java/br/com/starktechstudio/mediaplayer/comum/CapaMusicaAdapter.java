// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.archos.mediacenter.video.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Grade de capas quadradas de Músicas (1.5): artistas (foto redonda), álbuns e playlists, com o nome e a
 * linha de baixo embaixo da capa. TV: foco com a borda na capa e aumento de 6 % ({@link FocoTv}); tablet:
 * toque. OK ou toque abre; segurar faz a ação do item (tocar o álbum, o artista ou a playlist).
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
            if (!x.id.equals(y.id) || !x.nome.equals(y.nome) || !x.linha.equals(y.linha)
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
            caixa.setContentDescription(c.nome + ", " + c.linha);
        }
    }
}
