// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.archos.mediacenter.video.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.WeakHashMap;

import br.com.starktechstudio.mediaplayer.dados.Modelo.Capitulo;
import br.com.starktechstudio.mediaplayer.motor.QuadrosDeCapitulo;

/**
 * Capítulos (lidos do arquivo pelo motor), em quatro desenhos:
 * Detalhe da TV (fileira de miniaturas 16:9, foco na miniatura), Detalhe do tablet (linhas, o
 * capítulo onde o vídeo parou em azul), lista do player da TV (o atual com "Agora", os vistos com
 * ✓) e faixa do player do tablet (o atual com contorno azul). OK ou toque leva ao capítulo (D22).
 * Prévia (2.0.1): o quadro do começo do capítulo no lugar do número ({@link QuadrosDeCapitulo}); o
 * Detalhe faz os que faltam, fora da tela, e o player só mostra os já feitos. O quadro que chega entra
 * direto no item dele, sem refazer a lista (o foco da TV não sai do lugar).
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
    /** Vídeo dos capítulos (2.0.1); nulo = só o número. */
    @Nullable
    private String video;
    private int duracaoVideo;
    private boolean gerarQuadros;
    @Nullable
    private Context contexto;
    @Nullable
    private QuadrosDeCapitulo.Pedido pedido;
    /** Itens criados (também os que a lista guarda fora da tela sem vincular de novo): o quadro novo vai a eles. */
    private final Set<Item> itens = Collections.newSetFromMap(new WeakHashMap<>());

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

    /**
     * Vídeo dos capítulos (2.0.1): a prévia de cada um é o quadro do começo dele. {@code gerar}: faz os que faltam,
     * fora da tela (Detalhe); falso, só mostra os já feitos (player: fazer ali disputaria o motor com o vídeo).
     * Chamar antes de {@link #atualizar}; {@link #pararQuadros} ao sair da tela.
     */
    public void definirVideo(Context contexto, @Nullable String caminho, int duracaoSegundos, boolean gerar) {
        this.contexto = contexto.getApplicationContext();
        video = caminho;
        duracaoVideo = duracaoSegundos;
        gerarQuadros = gerar;
    }

    /** {@code numeroAtual}: capítulo onde o vídeo está ou parou (0 = nenhum). */
    public void atualizar(List<Capitulo> novos, int numeroAtual) {
        capitulos.clear();
        capitulos.addAll(novos);
        atual = numeroAtual;
        notifyDataSetChanged();
        pararQuadros();
        if (gerarQuadros && contexto != null) pedido = pedirQuadros(contexto, video, capitulos, duracaoVideo, itens);
    }

    /** Saída da tela (onPause): o quadro em andamento termina e fica guardado; os outros, na volta. */
    public void pararQuadros() {
        if (pedido != null) pedido.cancelar();
        pedido = null;
    }

    /**
     * Pede os quadros que faltam dos capítulos do vídeo; cada um que chega entra no item dele, entre {@code itens}
     * (o adapter ou a grade do Detalhe do tablet). Nulo sem vídeo ou sem capítulos.
     */
    @Nullable
    public static QuadrosDeCapitulo.Pedido pedirQuadros(Context contexto, @Nullable String video, List<Capitulo> capitulos,
                                                        int duracaoSegundos, Collection<Item> itens) {
        if (video == null || capitulos.isEmpty()) return null;
        int[] inicios = new int[capitulos.size()];
        for (int i = 0; i < inicios.length; i++) inicios[i] = capitulos.get(i).inicioSegundos;
        return QuadrosDeCapitulo.pedir(contexto, video, inicios, duracaoSegundos, (inicio, arquivo) -> {
            for (Item item : new ArrayList<>(itens)) item.mostrarQuadro(video, inicio, arquivo);
        });
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
        Item item = Item.criar(pai, layout, tv, aoEscolher);
        itens.add(item);
        return item;
    }

    @Override
    public void onBindViewHolder(@NonNull Item item, int posicao) {
        item.vincular(capitulos.get(posicao), atual, video);
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
        /** Vídeo do capítulo vinculado (prévia, 2.0.1); nulo = só o número. */
        @Nullable
        private String video;

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

        /** {@code atual}: número do capítulo onde o vídeo está ou parou (0 = nenhum). Sem prévia: só o número. */
        public void vincular(Capitulo c, int atual) {
            vincular(c, atual, null);
        }

        /**
         * {@code video}: o caminho do vídeo dos capítulos (2.0.1): a prévia é o quadro do começo do capítulo, se
         * já existe ({@link QuadrosDeCapitulo}); com ela, o número some; sem ela, fica o número sobre a cor.
         */
        public void vincular(Capitulo c, int atual, @Nullable String video) {
            capitulo = c;
            this.video = video;
            Context contexto = itemView.getContext();
            float canto = canto();
            miniatura.setBackground(CapaAdapter.blocoDeCor(contexto, c.corMiniatura, canto, tv));
            Imagens.noFundo(miniatura, video == null ? null : QuadrosDeCapitulo.arquivoDo(contexto, video, c.inicioSegundos),
                    Imagens.CARTAO, canto, numero);
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

        /** O quadro do capítulo ficou pronto (2.0.1): entra no lugar do número, se o item ainda é desse capítulo. */
        public void mostrarQuadro(String doVideo, int inicioSegundos, String arquivo) {
            if (capitulo == null || capitulo.inicioSegundos != inicioSegundos || !doVideo.equals(video)) return;
            Imagens.noFundo(miniatura, arquivo, Imagens.CARTAO, canto(), numero);
        }

        private float canto() {
            return miniatura.getResources().getDimension(tv ? R.dimen.st_tv_cartao_canto : R.dimen.st_tablet_capitulo_canto);
        }
    }
}
