// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import com.archos.mediacenter.video.R;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Target;

import java.io.File;

/**
 * Imagens de verdade nas telas (F3): capa e fundo do TMDB, imagem escolhida no Editar, quadro do
 * vídeo. A view já está com o bloco de cor (a reserva); quando a imagem chega (Picasso, fora da
 * tela), vira o fundo da view, recortada para preencher e com os cantos arredondados, e as
 * {@code reservas} (ícone de imagem, título sobre a cor) somem. Sem imagem, fica o bloco de cor.
 */
public final class Imagens {

    /** Largura máxima carregada de cada tipo (a imagem é reduzida antes de ir para a tela). */
    public static final int CAPA = 480;
    public static final int CARTAO = 720;
    public static final int FUNDO = 1600;

    private Imagens() {}

    /** Imagem do arquivo no fundo de {@code alvo}, recortada e com {@code canto}. */
    public static void noFundo(View alvo, @Nullable String arquivo, int largura, float canto, View... reservas) {
        carregar(alvo, arquivo == null ? null : Picasso.get().load(new File(arquivo)), largura, canto, false, reservas);
    }

    /** Imagem de um endereço (capa pequena do TMDB em "Buscar no TMDB"). */
    public static void doEndereco(View alvo, @Nullable String endereco, int largura, float canto, View... reservas) {
        carregar(alvo, endereco == null ? null : Picasso.get().load(endereco), largura, canto, false, reservas);
    }

    /** Fundo do Detalhe e da Série: a imagem com o véu do desenho, sumindo no fundo da página embaixo. */
    public static void fundoDaPagina(View alvo, @Nullable String arquivo) {
        carregar(alvo, arquivo == null ? null : Picasso.get().load(new File(arquivo)), FUNDO, 0, true);
    }

    /** Imagem do TMDB ou do aparelho da opção do Editar ({@code arquivo:} ou {@code url:}). */
    public static void daReferencia(View alvo, String referencia, int largura, float canto, View... reservas) {
        if (referencia != null && referencia.startsWith("url:")) {
            doEndereco(alvo, referencia.substring(4), largura, canto, reservas);
        } else if (referencia != null && referencia.startsWith("arquivo:")) {
            noFundo(alvo, referencia.substring("arquivo:".length()), largura, canto, reservas);
        } else {
            noFundo(alvo, null, largura, canto, reservas);
        }
    }

    private static void carregar(View alvo, @Nullable RequestCreator pedido, int largura, float canto, boolean veu,
                                 View... reservas) {
        Object anterior = alvo.getTag(R.id.st_tag_imagem);
        if (anterior instanceof Alvo) Picasso.get().cancelRequest((Alvo) anterior);
        alvo.setTag(R.id.st_tag_imagem, null);
        // O ícone de imagem do desenho dentro da view (capa ou fundo) também é reserva
        View[] todas = new View[reservas.length + 2];
        System.arraycopy(reservas, 0, todas, 0, reservas.length);
        todas[reservas.length] = alvo.findViewById(R.id.st_imagem_reserva);
        todas[reservas.length + 1] = alvo.findViewById(R.id.st_imagem_reserva_fundo);
        for (View reserva : todas) if (reserva != null) reserva.setVisibility(View.VISIBLE);
        if (pedido == null) return;
        Alvo novo = new Alvo(alvo, canto, veu, todas);
        alvo.setTag(R.id.st_tag_imagem, novo);
        pedido.resize(largura, 0).onlyScaleDown().into(novo);
    }

    /** Recebe a imagem do Picasso (fica na tag da view: o Picasso só guarda referência fraca). */
    private static final class Alvo implements Target {
        private final View view;
        private final float canto;
        private final boolean veu;
        private final View[] reservas;

        Alvo(View view, float canto, boolean veu, View[] reservas) {
            this.view = view;
            this.canto = canto;
            this.veu = veu;
            this.reservas = reservas;
        }

        @Override
        public void onBitmapLoaded(Bitmap imagem, Picasso.LoadedFrom origem) {
            if (view.getTag(R.id.st_tag_imagem) != this) return;
            Drawable recortada = new Recortada(imagem, canto);
            view.setBackground(veu ? comVeu(view.getContext(), recortada) : recortada);
            for (View reserva : reservas) if (reserva != null) reserva.setVisibility(View.GONE);
        }

        @Override
        public void onBitmapFailed(Exception erro, Drawable reserva) {
            // fica o bloco de cor
        }

        @Override
        public void onPrepareLoad(Drawable reserva) {
            // o bloco de cor já está na view
        }
    }

    /** Véu #020617 72 % por cima; nos 25 % de baixo, some no fundo da página. */
    private static Drawable comVeu(Context contexto, Drawable imagem) {
        int fundo = ContextCompat.getColor(contexto, R.color.st_fundo);
        int veu = ContextCompat.getColor(contexto, R.color.st_veu_72);
        int forte = ColorUtils.compositeColors(veu, ColorUtils.setAlphaComponent(fundo, 0x80));
        GradientDrawable gradiente = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{veu, veu, veu, forte, fundo});
        return new LayerDrawable(new Drawable[]{imagem, gradiente});
    }

    /** Desenha a imagem preenchendo a área (corta o que sobra, centrado) com os cantos arredondados. */
    private static final class Recortada extends Drawable {
        private final Bitmap imagem;
        private final Paint pinta = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final BitmapShader sombreador;
        private final Matrix matriz = new Matrix();
        private final RectF area = new RectF();
        private final float canto;

        Recortada(Bitmap imagem, float canto) {
            this.imagem = imagem;
            this.canto = canto;
            sombreador = new BitmapShader(imagem, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            pinta.setShader(sombreador);
        }

        @Override
        protected void onBoundsChange(@NonNull Rect limites) {
            super.onBoundsChange(limites);
            area.set(limites);
            float escala = Math.max(limites.width() / (float) imagem.getWidth(), limites.height() / (float) imagem.getHeight());
            float dx = limites.left + (limites.width() - imagem.getWidth() * escala) / 2f;
            float dy = limites.top + (limites.height() - imagem.getHeight() * escala) / 2f;
            matriz.setScale(escala, escala);
            matriz.postTranslate(dx, dy);
            sombreador.setLocalMatrix(matriz);
        }

        @Override
        public void draw(@NonNull Canvas tela) {
            if (canto > 0) tela.drawRoundRect(area, canto, canto, pinta);
            else tela.drawRect(area, pinta);
        }

        @Override
        public void setAlpha(int alfa) {
            pinta.setAlpha(alfa);
            invalidateSelf();
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter filtro) {
            pinta.setColorFilter(filtro);
            invalidateSelf();
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }

        @Override
        public int getIntrinsicWidth() {
            return -1;
        }

        @Override
        public int getIntrinsicHeight() {
            return -1;
        }
    }
}
