// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import com.archos.mediacenter.video.R;

import java.util.Locale;

import br.com.starktechstudio.mediaplayer.dados.ModeloMusicas.Album;
import br.com.starktechstudio.mediaplayer.dados.ModeloMusicas.Musica;

/**
 * Peças das telas de Músicas (1.5), no padrão das outras telas montadas no código: botões da TV e do
 * tablet, capa quadrada (bloco de cor com o ícone até a imagem chegar), topo com Voltar e os nomes que
 * faltam na tag ("Artista desconhecido", "Sem álbum", "Vários artistas").
 */
public final class PecasMusica {

    private PecasMusica() {}

    /** Botão com texto e ícone; primário em azul. */
    public static Button botao(Context contexto, @StringRes int texto, @DrawableRes int icone, boolean primario) {
        boolean tv = Aparelho.ehTv(contexto);
        int estilo = tv ? (primario ? R.style.StarkTech_Botao_Tv_Primario : R.style.StarkTech_Botao_Tv_Secundario)
                : (primario ? R.style.StarkTech_Botao_Tablet_Primario : R.style.StarkTech_Botao_Tablet_Secundario);
        Button botao = new Button(contexto, null, 0, estilo);
        botao.setText(texto);
        if (icone != 0) {
            int lado = contexto.getResources().getDimensionPixelSize(tv ? R.dimen.st_tv_botao_icone : R.dimen.st_tablet_botao_icone);
            Icones.noTexto(botao, icone, lado);
        }
        return botao;
    }

    /** Botão só com o ícone, quadrado (Voltar, Aleatório, Repetir). */
    public static ImageButton botaoIcone(Context contexto, @DrawableRes int icone, @StringRes int descricao) {
        boolean tv = Aparelho.ehTv(contexto);
        ImageButton botao = new ImageButton(contexto);
        botao.setImageResource(icone);
        ImageViewCompat.setImageTintList(botao, ContextCompat.getColorStateList(contexto, R.color.st_texto));
        botao.setBackgroundResource(R.drawable.st_botao_secundario);
        botao.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int lado = ladoBotaoIcone(contexto);
        botao.setPadding(lado / 4, lado / 4, lado / 4, lado / 4);
        botao.setContentDescription(contexto.getString(descricao));
        if (tv) botao.setForeground(ContextCompat.getDrawable(contexto, R.drawable.st_foco_tv));
        return botao;
    }

    public static int ladoBotaoIcone(Context contexto) {
        return contexto.getResources().getDimensionPixelSize(Aparelho.ehTv(contexto)
                ? R.dimen.st_tv_botao_icone_altura : R.dimen.st_tablet_detalhe_topo_botao_altura);
    }

    /** Topo das telas: Voltar e o rótulo da seção ("ÁLBUM", "ARTISTA", "PLAYLIST"). */
    public static LinearLayout topo(Activity tela, CharSequence rotulo) {
        LinearLayout topo = new LinearLayout(tela);
        topo.setOrientation(LinearLayout.HORIZONTAL);
        topo.setGravity(Gravity.CENTER_VERTICAL);
        ImageButton voltar = botaoIcone(tela, R.drawable.st_ic_voltar, R.string.st_voltar);
        voltar.setOnClickListener(v -> tela.finish());
        int lado = ladoBotaoIcone(tela);
        topo.addView(voltar, new LinearLayout.LayoutParams(lado, lado));
        TextView nome = Textos.rotulo(tela, rotulo, 16);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMarginStart(Medidas.px(tela, 24));
        topo.addView(nome, lp);
        return topo;
    }

    /**
     * Capa quadrada de {@code lado} px: bloco de cor com o ícone no meio; a imagem, quando chega, fica por
     * cima ({@link Imagens}). {@code redonda}: a foto do artista.
     */
    public static FrameLayout capa(Context contexto, int lado, int cor, @Nullable String arquivo, @DrawableRes int icone, boolean redonda) {
        boolean tv = Aparelho.ehTv(contexto);
        FrameLayout capa = new FrameLayout(contexto);
        float canto = redonda ? lado / 2f : contexto.getResources().getDimension(tv ? R.dimen.st_tv_capa_canto : R.dimen.st_tablet_capa_canto);
        capa.setBackground(CapaAdapter.blocoDeCor(contexto, cor, canto, tv));
        if (redonda) capa.setClipToOutline(true);
        ImageView reserva = new ImageView(contexto);
        reserva.setId(R.id.st_imagem_reserva);
        reserva.setImageResource(icone);
        ImageViewCompat.setImageTintList(reserva, ContextCompat.getColorStateList(contexto, R.color.st_branco_30));
        reserva.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        int icoLado = Math.max(1, lado * 3 / 10);
        capa.addView(reserva, new FrameLayout.LayoutParams(icoLado, icoLado, Gravity.CENTER));
        capa.setLayoutParams(new ViewGroup.LayoutParams(lado, lado));
        capa.setContentDescription(contexto.getString(R.string.st_musicas_capa));
        Imagens.noFundo(capa, arquivo, Math.min(Imagens.CARTAO, Math.max(Imagens.CAPA, lado)), canto);
        return capa;
    }

    /** Texto de uma linha, com "…" no fim. */
    public static TextView linha(Context contexto, CharSequence texto, int fonte, float pxHtml, int cor) {
        TextView t = Textos.novo(contexto, texto, fonte, pxHtml, cor);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        return t;
    }

    /** "3:45" ou "1:02:03". */
    public static String tempo(long ms) {
        long segundos = Math.max(0, ms / 1000);
        long horas = segundos / 3600;
        long minutos = (segundos % 3600) / 60;
        long resto = segundos % 60;
        return horas > 0 ? String.format(Locale.ROOT, "%d:%02d:%02d", horas, minutos, resto)
                : String.format(Locale.ROOT, "%d:%02d", minutos, resto);
    }

    /** "1 h 12 min" ou "42 min" (duração do álbum ou da playlist). */
    public static String duracaoLonga(long ms) {
        long minutos = Math.max(1, Math.round(ms / 60000.0));
        return minutos >= 60 ? String.format(Locale.ROOT, "%d h %d min", minutos / 60, minutos % 60)
                : String.format(Locale.ROOT, "%d min", minutos);
    }

    public static String artista(Context contexto, @Nullable String nome) {
        return nome == null ? contexto.getString(R.string.st_musicas_artista_desconhecido) : nome;
    }

    public static String artistaDoAlbum(Context contexto, Album a) {
        return a.variosArtistas ? contexto.getString(R.string.st_musicas_varios_artistas) : artista(contexto, a.artista);
    }

    public static String album(Context contexto, @Nullable String nome) {
        return nome == null ? contexto.getString(R.string.st_musicas_sem_album) : nome;
    }

    /** "Artista · Álbum" da linha de baixo da música. */
    public static String artistaEAlbum(Context contexto, Musica m) {
        return artista(contexto, m.artista) + " · " + album(contexto, m.album);
    }

    /** Altura de uma linha de música. */
    public static int alturaLinha(Context contexto) {
        return Medidas.px(contexto, Aparelho.ehTv(contexto) ? 92 : 100);
    }

    /** Margem do alto da página. */
    public static LinearLayout.LayoutParams comTopo(int topo) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = topo;
        return lp;
    }
}
