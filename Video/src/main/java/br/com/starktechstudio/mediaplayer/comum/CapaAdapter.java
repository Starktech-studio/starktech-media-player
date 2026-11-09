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

import br.com.starktechstudio.mediaplayer.dados.Modelo.Titulo;

/**
 * Capas 2:3 (Main.dc.html, tablet-biblioteca.dc.html, tv-menu-capa.dc.html): bloco de cor
 * no lugar da imagem quando falta a capa ({@link Imagens}), selo "Assistido", selos 4K ou 1080p e nota, título e ano. TV: foco com borda branca e
 * aumento de 6 % ({@link FocoTv}); tablet: toque. O clique abre o título por {@link Navegacao};
 * segurar OK ou tocar e segurar abre o Menu da capa. Capa de coletânea e de artista (D5, D7): sempre
 * com o nome e a contagem, o clique abre a lista dela e segurar vai ao Editar (o Menu da capa é de
 * quem tem Assistir e Favoritar).
 */
public class CapaAdapter extends RecyclerView.Adapter<CapaAdapter.Capa> {

    private final Activity atividade;
    private final boolean tv;
    private final List<Titulo> titulos = new ArrayList<>();
    /** Tamanho da capa quando a grade estica as colunas (abas da TV); 0 = o do layout. */
    private int largura;
    private int altura;

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

    /** Mais capas no fim (fileira que carrega aos poucos), sem mexer no foco das que já estão. */
    public void acrescentar(List<Titulo> novos) {
        int inicio = titulos.size();
        titulos.addAll(novos);
        notifyItemRangeInserted(inicio, novos.size());
    }

    @Override
    public int getItemCount() {
        return titulos.size();
    }

    @Override
    public long getItemId(int posicao) {
        return titulos.get(posicao).id.hashCode();
    }

    /** Capas de {@code largura} x {@code altura} px no lugar do tamanho do layout. */
    public void definirTamanho(int largura, int altura) {
        this.largura = largura;
        this.altura = altura;
    }

    @NonNull
    @Override
    public Capa onCreateViewHolder(@NonNull ViewGroup pai, int tipo) {
        Capa capa = Capa.criar(atividade, pai, tv);
        if (largura > 0 && altura > 0) {
            ViewGroup.LayoutParams lp = capa.itemView.getLayoutParams();
            lp.width = largura;
            lp.height = altura;
            capa.itemView.setLayoutParams(lp);
        }
        return capa;
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
        private final View selos;
        private final View selo4k;
        private final View selo1080p;
        private final View seloNota;
        private final TextView notaValor;
        private Titulo atual;

        private Capa(Activity atividade, View item, boolean tv) {
            super(item);
            this.atividade = atividade;
            this.tv = tv;
            assistido = item.findViewById(R.id.st_capa_assistido);
            titulo = item.findViewById(R.id.st_capa_titulo);
            ano = item.findViewById(R.id.st_capa_ano);
            selos = item.findViewById(R.id.st_capa_selos);
            selo4k = item.findViewById(R.id.st_capa_selo_4k);
            selo1080p = item.findViewById(R.id.st_capa_selo_1080p);
            seloNota = item.findViewById(R.id.st_capa_selo_nota);
            notaValor = item.findViewById(R.id.st_capa_nota_valor);
            item.setOnClickListener(v -> {
                if (atual != null) Navegacao.abrirCapa(atividade, atual);
            });
            // Segurar OK (TV) ou tocar e segurar (tablet): Menu da capa
            item.setOnLongClickListener(v -> {
                if (atual == null) return false;
                if (ehAgrupada(atual)) Navegacao.abrirEditar(atividade, atual.id);
                else Navegacao.abrirMenuCapa(atividade, atual.id);
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
            ano.setText(subtitulo(contexto, t));
            if (ehAgrupada(t)) {
                // Foto de artista e capa emprestada do primeiro filme não trazem o nome: ele e a
                // contagem ficam, com sombra para ler sobre a imagem
                sombra(contexto, true);
                titulo.setVisibility(View.VISIBLE);
                ano.setVisibility(View.VISIBLE);
                Imagens.noFundo(itemView, t.imagemCapa(), Imagens.CAPA, canto);
            } else {
                sombra(contexto, false);
                // Com a capa de verdade, o título e o ano sobre a cor saem (a capa já traz o nome)
                Imagens.noFundo(itemView, t.imagemCapa(), Imagens.CAPA, canto, titulo, ano);
            }
            assistido.setVisibility(t.assistido ? View.VISIBLE : View.GONE);
            mostrarSelos(t);
            itemView.setContentDescription(t.titulo);
        }

        /** Só com o que o título tem: coletânea e artista não têm tamanho de imagem nem nota. */
        private void mostrarSelos(Titulo t) {
            selo4k.setVisibility(t.eh4k() ? View.VISIBLE : View.GONE);
            selo1080p.setVisibility(t.eh1080p() ? View.VISIBLE : View.GONE);
            seloNota.setVisibility(t.nota != null ? View.VISIBLE : View.GONE);
            if (t.nota != null) notaValor.setText(String.valueOf(t.nota));
            selos.setVisibility(t.eh4k() || t.eh1080p() || t.nota != null ? View.VISIBLE : View.GONE);
        }

        private void sombra(Context contexto, boolean ligada) {
            float densidade = contexto.getResources().getDisplayMetrics().density;
            int cor = ligada ? ContextCompat.getColor(contexto, R.color.st_fundo) : 0;
            float raio = ligada ? 4 * densidade : 0;
            float descida = ligada ? densidade : 0;
            titulo.setShadowLayer(raio, 0, descida, cor);
            ano.setShadowLayer(raio, 0, descida, cor);
        }
    }

    private static boolean ehAgrupada(Titulo t) {
        return t.ehColetanea() || t.ehArtista();
    }

    /** Linha de baixo da capa: o ano; coletânea e artista, quantos títulos têm. */
    private static String subtitulo(Context contexto, Titulo t) {
        if (t.ehColetanea()) {
            return contexto.getResources().getQuantityString(R.plurals.st_n_filmes, t.numeroFilmes, t.numeroFilmes);
        }
        if (t.ehArtista()) {
            return contexto.getResources().getQuantityString(R.plurals.st_n_shows, t.numeroShows, t.numeroShows);
        }
        return t.ano;
    }

    /**
     * Bloco de cor com a borda branca 8 % do desenho (0,5 dp na TV = 1 px; 1 dp no tablet): fica
     * enquanto a imagem carrega e quando o título não tem imagem ({@link Imagens}).
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
