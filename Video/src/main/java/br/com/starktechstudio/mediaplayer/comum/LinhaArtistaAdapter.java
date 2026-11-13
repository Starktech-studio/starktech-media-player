// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.archos.mediacenter.video.R;

import java.util.ArrayList;
import java.util.List;

import br.com.starktechstudio.mediaplayer.dados.ModeloMusicas.Artista;
import br.com.starktechstudio.mediaplayer.dados.RepositorioMusicas;
import br.com.starktechstudio.mediaplayer.musica.Tocador;

/**
 * Sub-aba Artistas de Músicas em linhas (1.7, D1 do plano 008): foto redonda grande, nome em destaque, as
 * contagens ("3 álbuns · 42 músicas · 12 vídeos", só as que existem) e os formatos. OK ou toque abre a página
 * do artista; segurar ouve todas as músicas dele.
 */
public class LinhaArtistaAdapter extends RecyclerView.Adapter<LinhaArtistaAdapter.Linha> {

    private final Activity tela;
    private final boolean tv;
    private final List<Artista> artistas = new ArrayList<>();

    public LinhaArtistaAdapter(Activity tela, boolean tv) {
        this.tela = tela;
        this.tv = tv;
        setHasStableIds(true);
    }

    /** Só refaz se mudou (a biblioteca avisa muito durante a atualização). */
    public void atualizar(List<Artista> novos) {
        if (iguais(artistas, novos)) return;
        artistas.clear();
        artistas.addAll(novos);
        notifyDataSetChanged();
    }

    private static boolean iguais(List<Artista> a, List<Artista> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            Artista x = a.get(i);
            Artista y = b.get(i);
            if (!x.id.equals(y.id) || x.albuns != y.albuns || x.musicas != y.musicas || x.videos != y.videos
                    || !igual(x.foto, y.foto) || !igual(x.nome, y.nome)) return false;
        }
        return true;
    }

    private static boolean igual(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    @Override
    public int getItemCount() {
        return artistas.size();
    }

    @Override
    public long getItemId(int posicao) {
        return artistas.get(posicao).id.hashCode();
    }

    @NonNull
    @Override
    public Linha onCreateViewHolder(@NonNull ViewGroup pai, int tipo) {
        return new Linha(pai.getContext());
    }

    @Override
    public void onBindViewHolder(@NonNull Linha linha, int posicao) {
        linha.vincular(artistas.get(posicao));
    }

    /** "3 álbuns · 42 músicas · 12 vídeos", só as que existem. */
    static String contagens(Context contexto, Artista a) {
        List<String> partes = new ArrayList<>();
        if (a.albuns > 0) partes.add(contexto.getResources().getQuantityString(R.plurals.st_n_albuns, a.albuns, a.albuns));
        if (a.musicas > 0) partes.add(contexto.getResources().getQuantityString(R.plurals.st_n_musicas, a.musicas, a.musicas));
        if (a.videos > 0) partes.add(contexto.getResources().getQuantityString(R.plurals.st_n_videos, a.videos, a.videos));
        return android.text.TextUtils.join(" · ", partes);
    }

    final class Linha extends RecyclerView.ViewHolder {
        private final FrameLayout lugarDaFoto;
        private final TextView nome;
        private final TextView contagem;
        private final TextView formatos;
        private final int lado;
        private Artista atual;

        Linha(Context contexto) {
            super(new LinearLayout(contexto));
            LinearLayout caixa = (LinearLayout) itemView;
            caixa.setOrientation(LinearLayout.HORIZONTAL);
            caixa.setGravity(Gravity.CENTER_VERTICAL);
            caixa.setBackgroundResource(R.drawable.st_linha_musica);
            caixa.setFocusable(true);
            caixa.setClickable(true);
            int folga = Medidas.px(contexto, 18);
            caixa.setPadding(folga, folga, folga * 2, folga);
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = Medidas.px(contexto, 10);
            caixa.setLayoutParams(lp);
            if (tv) caixa.setForeground(ContextCompat.getDrawable(contexto, R.drawable.st_foco_tv));
            lado = Medidas.px(contexto, 140);
            lugarDaFoto = new FrameLayout(contexto);
            caixa.addView(lugarDaFoto, new LinearLayout.LayoutParams(lado, lado));
            LinearLayout textos = new LinearLayout(contexto);
            textos.setOrientation(LinearLayout.VERTICAL);
            nome = PecasMusica.linha(contexto, "", R.font.st_inter_black, 32, R.color.st_texto);
            textos.addView(nome);
            contagem = PecasMusica.linha(contexto, "", R.font.st_inter_bold, 21, R.color.st_texto_secundario);
            textos.addView(contagem, PecasMusica.comTopo(Medidas.px(contexto, 10)));
            formatos = PecasMusica.linha(contexto, "", R.font.st_inter_semibold, 17, R.color.st_ciano);
            textos.addView(formatos, PecasMusica.comTopo(Medidas.px(contexto, 8)));
            LinearLayout.LayoutParams lpTextos = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lpTextos.setMarginStart(Medidas.px(contexto, 28));
            caixa.addView(textos, lpTextos);
            caixa.setOnClickListener(v -> {
                if (atual != null) TelasMusica.abrirArtista(tela, atual.id);
            });
            caixa.setOnLongClickListener(v -> {
                if (atual == null || atual.musicas == 0) return false;
                Tocador.de(tela).tocar(RepositorioMusicas.de(tela).musicasDoArtista(atual.id), 0, false);
                return true;
            });
        }

        void vincular(Artista a) {
            atual = a;
            Context contexto = itemView.getContext();
            lugarDaFoto.removeAllViews();
            lugarDaFoto.addView(PecasMusica.capa(contexto, lado, a.cor, a.foto, R.drawable.st_ic_shows, true));
            nome.setText(PecasMusica.artista(contexto, a.nome));
            contagem.setText(contagens(contexto, a));
            List<String> f = new ArrayList<>();
            if (a.formatoMusicas != null) f.add(contexto.getString(R.string.st_musicas_selo_musicas) + " " + a.formatoMusicas);
            if (a.formatoVideos != null) f.add(contexto.getString(R.string.st_musicas_selo_videos) + " " + a.formatoVideos);
            formatos.setText(android.text.TextUtils.join(" · ", f));
            formatos.setVisibility(f.isEmpty() ? android.view.View.GONE : android.view.View.VISIBLE);
            itemView.setContentDescription(nome.getText() + ", " + contagem.getText());
        }
    }
}
