// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.archos.mediacenter.video.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import br.com.starktechstudio.mediaplayer.dados.Modelo.Capitulo;

/**
 * Capítulos (F1: lista simulada; F2: lidos do arquivo pelo motor), em quatro desenhos:
 * Detalhe da TV (fileira de miniaturas 16:9, foco na miniatura), Detalhe do tablet (linhas, o
 * capítulo onde o vídeo parou em azul), lista do player da TV (o atual com "Agora", os vistos com
 * ✓) e faixa do player do tablet (o atual com contorno azul). OK ou toque leva ao capítulo (D22).
 */
public class CapituloAdapter extends RecyclerView.Adapter<CapituloAdapter.Item> {

    /** Capítulo escolhido (OK na TV, toque no tablet). */
    public interface AoEscolher {
        void escolher(Capitulo capitulo);
    }

    @LayoutRes
    private final int layout;
    private final boolean tv;
    private final AoEscolher aoEscolher;
    private final List<Capitulo> capitulos = new ArrayList<>();
    private int atual;

    /** Detalhe: fileira da TV ou linhas do tablet. */
    public CapituloAdapter(boolean tv, AoEscolher aoEscolher) {
        this(tv ? R.layout.st_tv_item_capitulo : R.layout.st_tablet_item_capitulo, tv, aoEscolher);
    }

    /** Outro desenho do item (player): st_tv_item_capitulo_reproducao, st_tablet_item_capitulo_faixa. */
    public CapituloAdapter(@LayoutRes int layout, boolean tv, AoEscolher aoEscolher) {
        this.layout = layout;
        this.tv = tv;
        this.aoEscolher = aoEscolher;
        setHasStableIds(true);
    }

    /** {@code numeroAtual}: capítulo onde o vídeo está ou parou (0 = nenhum). */
    public void atualizar(List<Capitulo> novos, int numeroAtual) {
        capitulos.clear();
        capitulos.addAll(novos);
        atual = numeroAtual;
        notifyDataSetChanged();
    }

    /** Só o capítulo atual mudou (player): refaz os itens sem trocar a lista. */
    public void definirAtual(int numeroAtual) {
        if (numeroAtual == atual) return;
        atual = numeroAtual;
        notifyItemRangeChanged(0, capitulos.size());
    }

    @Override
    public int getItemCount() {
        return capitulos.size();
    }

    @Override
    public long getItemId(int posicao) {
        return capitulos.get(posicao).numero;
    }

    @NonNull
    @Override
    public Item onCreateViewHolder(@NonNull ViewGroup pai, int tipo) {
        return Item.criar(pai, layout, tv, aoEscolher);
    }

    @Override
    public void onBindViewHolder(@NonNull Item item, int posicao) {
        item.vincular(capitulos.get(posicao), atual);
    }

    /** Um capítulo; também usado fora do adapter (grade do tablet, dentro da página que rola). */
    public static final class Item extends RecyclerView.ViewHolder {

        private final boolean tv;
        private final View miniatura;
        private final TextView numero;
        private final TextView nome;
        private final TextView tempo;
        /** Só na lista do player da TV. */
        private final View visto;
        private final View agora;
        private Capitulo capitulo;

        private Item(View item, boolean tv, AoEscolher aoEscolher) {
            super(item);
            this.tv = tv;
            miniatura = item.findViewById(R.id.st_capitulo_miniatura);
            numero = item.findViewById(R.id.st_capitulo_numero);
            nome = item.findViewById(R.id.st_capitulo_nome);
            tempo = item.findViewById(R.id.st_capitulo_tempo);
            visto = item.findViewById(R.id.st_capitulo_visto);
            agora = item.findViewById(R.id.st_capitulo_agora);
            item.setOnClickListener(v -> {
                if (capitulo != null && aoEscolher != null) aoEscolher.escolher(capitulo);
            });
        }

        public static Item criar(ViewGroup pai, boolean tv, AoEscolher aoEscolher) {
            return criar(pai, tv ? R.layout.st_tv_item_capitulo : R.layout.st_tablet_item_capitulo, tv, aoEscolher);
        }

        public static Item criar(ViewGroup pai, @LayoutRes int layout, boolean tv, AoEscolher aoEscolher) {
            View item = LayoutInflater.from(pai.getContext()).inflate(layout, pai, false);
            // Linha do Detalhe do tablet: o realce do toque fica nos cantos do fundo. A faixa do
            // player não recorta: o fundo dela é só o realce, sem contorno, e sumiria com o item
            if (layout == R.layout.st_tablet_item_capitulo) item.setClipToOutline(true);
            return new Item(item, tv, aoEscolher);
        }

        /** {@code atual}: número do capítulo onde o vídeo está ou parou (0 = nenhum). */
        public void vincular(Capitulo c, int atual) {
            capitulo = c;
            Context contexto = itemView.getContext();
            float canto = contexto.getResources().getDimension(tv ? R.dimen.st_tv_cartao_canto : R.dimen.st_tablet_capitulo_canto);
            miniatura.setBackground(CapaAdapter.blocoDeCor(contexto, c.corMiniatura, canto, tv));
            numero.setText(String.format(Locale.ROOT, "%02d", c.numero));
            nome.setText(c.nome);
            String inicio = PecasDetalhe.tempo(c.inicioSegundos);
            tempo.setText(inicio);
            boolean ehAtual = c.numero == atual;
            itemView.setSelected(ehAtual);
            if (visto != null) visto.setVisibility(atual > 0 && c.numero < atual ? View.VISIBLE : View.GONE);
            if (agora != null) agora.setVisibility(ehAtual ? View.VISIBLE : View.GONE);
            itemView.setContentDescription(contexto.getString(R.string.st_capitulo_descricao, c.numero, c.nome, inicio));
        }
    }
}
