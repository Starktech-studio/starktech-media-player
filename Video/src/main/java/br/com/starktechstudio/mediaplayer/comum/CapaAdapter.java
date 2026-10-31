// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.archos.mediacenter.video.R;

import java.util.ArrayList;
import java.util.List;

import br.com.starktechstudio.mediaplayer.dados.Modelo.Tipo;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Titulo;

/**
 * Capas 2:3 (Main.dc.html, tablet-biblioteca.dc.html, tv-menu-capa.dc.html): bloco de cor
 * no lugar da imagem (F1), selo "Assistido", título e ano. TV: foco com borda branca e
 * aumento de 6 % ({@link FocoTv}); tablet: toque. O clique abre o título por {@link Navegacao};
 * segurar OK ou tocar e segurar abre o Menu da capa.
 */
public class CapaAdapter extends RecyclerView.Adapter<CapaAdapter.Capa> {

    private final Activity atividade;
    private final boolean tv;
    private final List<Titulo> titulos = new ArrayList<>();

    public CapaAdapter(Activity atividade, boolean tv) {
        this.atividade = atividade;
        this.tv = tv;
        setHasStableIds(true);
    }

    public void atualizar(List<Titulo> novos) {
        titulos.clear();
        titulos.addAll(novos);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return titulos.size();
    }

    @Override
    public long getItemId(int posicao) {
        return titulos.get(posicao).id.hashCode();
    }

    @NonNull
    @Override
    public Capa onCreateViewHolder(@NonNull ViewGroup pai, int tipo) {
        return Capa.criar(atividade, pai, tv);
    }

    @Override
    public void onBindViewHolder(@NonNull Capa capa, int posicao) {
        capa.vincular(titulos.get(posicao));
    }

    /** Uma capa; também usada fora do adapter (páginas do Início do tablet). */
    public static final class Capa extends RecyclerView.ViewHolder {

        private final Activity atividade;
        private final boolean tv;
        private final View assistido;
        private final TextView titulo;
        private final TextView ano;
        private Titulo atual;

        private Capa(Activity atividade, View item, boolean tv) {
            super(item);
            this.atividade = atividade;
            this.tv = tv;
            assistido = item.findViewById(R.id.st_capa_assistido);
            titulo = item.findViewById(R.id.st_capa_titulo);
            ano = item.findViewById(R.id.st_capa_ano);
            item.setOnClickListener(v -> {
                if (atual != null) Navegacao.abrirTitulo(atividade, atual.id, atual.tipo == Tipo.SERIE);
            });
            // Segurar OK (TV) ou tocar e segurar (tablet): Menu da capa
            item.setOnLongClickListener(v -> {
                if (atual == null) return false;
                Navegacao.abrirMenuCapa(atividade, atual.id);
                return true;
            });
            if (tv) {
                FocoTv.aplicarComEscala(item, null);
            } else {
                // Efeito do toque recortado no canto arredondado da capa
                item.setClipToOutline(true);
            }
        }

        public static Capa criar(Activity atividade, ViewGroup pai, boolean tv) {
            View item = LayoutInflater.from(pai.getContext()).inflate(
                    tv ? R.layout.st_tv_item_capa : R.layout.st_tablet_item_capa, pai, false);
            return new Capa(atividade, item, tv);
        }

        public void vincular(Titulo t) {
            atual = t;
            Context contexto = itemView.getContext();
            float canto = contexto.getResources().getDimension(tv ? R.dimen.st_tv_capa_canto : R.dimen.st_tablet_capa_canto);
            itemView.setBackground(blocoDeCor(contexto, t.corCapa, canto, tv));
            titulo.setText(t.titulo);
            ano.setText(t.ano);
            assistido.setVisibility(t.assistido ? View.VISIBLE : View.GONE);
            itemView.setContentDescription(t.titulo);
        }
    }

    /**
     * Imagem provisória da F1: bloco de cor com a borda branca 8 % do desenho
     * (0,5 dp na TV = 1 px; 1 dp no tablet). Na F3 entram as imagens do TMDB.
     */
    public static GradientDrawable blocoDeCor(Context contexto, int cor, float canto, boolean tv) {
        GradientDrawable fundo = new GradientDrawable();
        fundo.setShape(GradientDrawable.RECTANGLE);
        fundo.setColor(cor);
        fundo.setCornerRadius(canto);
        float densidade = contexto.getResources().getDisplayMetrics().density;
        int borda = Math.max(1, Math.round((tv ? 0.5f : 1f) * densidade));
        fundo.setStroke(borda, ContextCompat.getColor(contexto, R.color.st_branco_8));
        return fundo;
    }
}
