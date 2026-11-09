// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.archos.mediacenter.video.R;

import java.util.ArrayList;
import java.util.List;

import br.com.starktechstudio.mediaplayer.dados.Modelo.Capitulo;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Tipo;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Titulo;
import br.com.starktechstudio.mediaplayer.dados.RepositorioBiblioteca;

/**
 * Cartões 16:9 (Main.dc.html, tablet-biblioteca.dc.html): fundo do TMDB, quadro do vídeo ou a capa ({@link Titulo#imagemCartao}).
 * Continuar assistindo: barra de progresso e "Cap. 4 de 8 · Faltam 6 min" (tablet: "Cap. 4 de 8";
 * sem capítulos: "Faltam 2 min"); o clique retoma a reprodução. Favoritos: coração no canto e
 * "ano · duração"; o clique abre o título. TV: borda de foco na imagem, sem aumento.
 */
public class CartaoAdapter extends RecyclerView.Adapter<CartaoAdapter.Cartao> {

    public enum Modo { CONTINUAR, FAVORITOS }

    private final Activity atividade;
    private final RepositorioBiblioteca repositorio;
    private final boolean tv;
    private final Modo modo;
    private final List<Titulo> titulos = new ArrayList<>();

    public CartaoAdapter(Activity atividade, RepositorioBiblioteca repositorio, boolean tv, Modo modo) {
        this.atividade = atividade;
        this.repositorio = repositorio;
        this.tv = tv;
        this.modo = modo;
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
    public Cartao onCreateViewHolder(@NonNull ViewGroup pai, int tipo) {
        View item = LayoutInflater.from(pai.getContext()).inflate(
                tv ? R.layout.st_tv_item_cartao : R.layout.st_tablet_item_cartao, pai, false);
        return new Cartao(item);
    }

    @Override
    public void onBindViewHolder(@NonNull Cartao cartao, int posicao) {
        cartao.vincular(titulos.get(posicao));
    }

    /** Linha de apoio de Continuar assistindo. */
    private String linhaContinuar(Context contexto, Titulo t) {
        List<Capitulo> capitulos = repositorio.capitulos(t.id);
        int faltam = t.faltamMin();
        if (capitulos.isEmpty()) return contexto.getString(R.string.st_faltam, faltam);
        int atual = capituloAtual(capitulos, t.posicaoSegundos);
        return tv
                ? contexto.getString(R.string.st_cap_faltam, atual, capitulos.size(), faltam)
                : contexto.getString(R.string.st_cap_de, atual, capitulos.size());
    }

    /** Número do capítulo em que a posição está (o último que já começou). */
    public static int capituloAtual(List<Capitulo> capitulos, int posicaoSegundos) {
        int atual = capitulos.isEmpty() ? 0 : capitulos.get(0).numero;
        for (Capitulo c : capitulos) {
            if (c.inicioSegundos <= posicaoSegundos) atual = c.numero;
        }
        return atual;
    }

    public final class Cartao extends RecyclerView.ViewHolder {

        private final View imagem;
        private final View favorito;
        private final ProgressBar progresso;
        private final TextView titulo;
        private final TextView sub;
        private Titulo atual;

        Cartao(View item) {
            super(item);
            imagem = item.findViewById(R.id.st_cartao_imagem);
            favorito = item.findViewById(R.id.st_cartao_favorito);
            progresso = item.findViewById(R.id.st_cartao_progresso);
            titulo = item.findViewById(R.id.st_cartao_titulo);
            sub = item.findViewById(R.id.st_cartao_sub);
            if (!tv) imagem.setClipToOutline(true);
            item.setOnClickListener(v -> {
                if (atual == null) return;
                if (modo == Modo.CONTINUAR) Navegacao.abrirReproducao(atividade, atual.id, Navegacao.DE_ONDE_PAROU);
                else Navegacao.abrirTitulo(atividade, atual.id, atual.tipo == Tipo.SERIE);
            });
        }

        void vincular(Titulo t) {
            atual = t;
            Context contexto = itemView.getContext();
            float canto = contexto.getResources().getDimension(tv ? R.dimen.st_tv_cartao_canto : R.dimen.st_tablet_cartao_canto);
            imagem.setBackground(CapaAdapter.blocoDeCor(contexto, t.corCartao, canto, tv));
            Imagens.noFundo(imagem, t.imagemCartao(), Imagens.CARTAO, canto);
            titulo.setText(t.titulo);
            if (modo == Modo.CONTINUAR) {
                progresso.setVisibility(View.VISIBLE);
                progresso.setProgress(t.progresso());
                favorito.setVisibility(View.GONE);
                sub.setText(linhaContinuar(contexto, t));
            } else {
                progresso.setVisibility(View.GONE);
                favorito.setVisibility(View.VISIBLE);
                sub.setText(contexto.getString(R.string.st_ano_duracao, t.ano, t.duracaoMin));
            }
            itemView.setContentDescription(t.titulo + ", " + sub.getText());
        }
    }
}
