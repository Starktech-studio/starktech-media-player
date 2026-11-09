// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.archos.mediacenter.video.R;

import java.util.ArrayList;
import java.util.List;

import br.com.starktechstudio.mediaplayer.dados.Modelo.SecaoShows;

/**
 * Aba Shows com "por grupo" ligado (D7): cada item é uma linha de capas de artista; a primeira de
 * cada seção traz o nome dela em cima. O nome fica junto das capas para todo item ter foco: a grade
 * da TV não sabe descer a um item sem foco, o que travaria o ↓ das pílulas numa seção.
 */
public class SecoesShowsAdapter extends RecyclerView.Adapter<SecoesShowsAdapter.Bloco> {

    private final Activity atividade;
    private final boolean tv;
    private final List<SecaoShows> secoes = new ArrayList<>();
    private final List<LinhasDeCapas.Linha> linhas = new ArrayList<>();
    private int colunas = 1;
    private int espaco;
    /** Tamanho da capa quando a grade estica as colunas (TV); 0 = o do layout. */
    private int largura;
    private int altura;

    public SecoesShowsAdapter(Activity atividade, boolean tv) {
        this.atividade = atividade;
        this.tv = tv;
        setHasStableIds(true);
    }

    public void atualizar(List<SecaoShows> novas) {
        secoes.clear();
        secoes.addAll(novas);
        remontar();
    }

    /** {@code colunas} capas por linha, {@code espaco} px entre elas; refaz as linhas se mudou. */
    public void definirGrade(int colunas, int espaco) {
        if (colunas == this.colunas && espaco == this.espaco) return;
        this.colunas = colunas;
        this.espaco = espaco;
        remontar();
    }

    /** Capas de {@code largura} x {@code altura} px no lugar do tamanho do layout. */
    public void definirTamanho(int largura, int altura) {
        this.largura = largura;
        this.altura = altura;
    }

    private void remontar() {
        linhas.clear();
        linhas.addAll(LinhasDeCapas.dasSecoes(secoes, colunas));
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return linhas.size();
    }

    @Override
    public long getItemId(int posicao) {
        return linhas.get(posicao).chave.hashCode();
    }

    @NonNull
    @Override
    public Bloco onCreateViewHolder(@NonNull ViewGroup pai, int tipo) {
        return new Bloco(atividade, pai.getContext(), tv);
    }

    @Override
    public void onBindViewHolder(@NonNull Bloco bloco, int posicao) {
        bloco.vincular(linhas.get(posicao), largura, altura, espaco);
    }

    /** Nome da seção (só na primeira linha dela) e as capas. */
    public static final class Bloco extends RecyclerView.ViewHolder {

        private final Activity atividade;
        private final boolean tv;
        private final TextView secao;
        private final LinearLayout fileira;
        private final List<CapaAdapter.Capa> capas = new ArrayList<>();

        Bloco(Activity atividade, Context contexto, boolean tv) {
            super(new LinearLayout(contexto));
            this.atividade = atividade;
            this.tv = tv;
            Resources res = contexto.getResources();
            LinearLayout raiz = (LinearLayout) itemView;
            raiz.setOrientation(LinearLayout.VERTICAL);
            raiz.setClipChildren(false);
            raiz.setClipToPadding(false);
            raiz.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            secao = new TextView(contexto);
            secao.setTypeface(ResourcesCompat.getFont(contexto, R.font.st_inter_black));
            secao.setIncludeFontPadding(false);
            secao.setSingleLine(true);
            secao.setTextColor(ContextCompat.getColor(contexto, R.color.st_texto));
            secao.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(
                    tv ? R.dimen.st_tv_texto_titulo_secao : R.dimen.st_tablet_texto_titulo_secao));
            secao.setPadding(0, 0, 0, tv ? res.getDimensionPixelSize(R.dimen.st_tv_espaco_titulo_secao)
                    : res.getDimensionPixelSize(R.dimen.st_tablet_capa_espaco));
            raiz.addView(secao);

            fileira = new LinearLayout(contexto);
            fileira.setOrientation(LinearLayout.HORIZONTAL);
            fileira.setClipChildren(false);
            fileira.setClipToPadding(false);
            raiz.addView(fileira, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        void vincular(LinhasDeCapas.Linha linha, int largura, int altura, int espaco) {
            secao.setVisibility(linha.secao == null ? View.GONE : View.VISIBLE);
            secao.setText(linha.secao);
            while (capas.size() < linha.capas.size()) {
                CapaAdapter.Capa nova = CapaAdapter.Capa.criar(atividade, fileira, tv);
                fileira.addView(nova.itemView);
                capas.add(nova);
            }
            for (int i = 0; i < capas.size(); i++) {
                View item = capas.get(i).itemView;
                if (i >= linha.capas.size()) {
                    item.setVisibility(View.GONE);
                    continue;
                }
                item.setVisibility(View.VISIBLE);
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) item.getLayoutParams();
                if (largura > 0 && altura > 0) {
                    lp.width = largura;
                    lp.height = altura;
                }
                lp.setMarginStart(i > 0 ? espaco : 0);
                item.setLayoutParams(lp);
                capas.get(i).vincular(linha.capas.get(i));
            }
        }
    }
}
