// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.widget.ImageViewCompat;

import com.archos.mediacenter.video.R;

import br.com.starktechstudio.mediaplayer.dados.Modelo;

/**
 * Nota do filme assistido no Detalhe (D6, D12): 5 estrelas, de 0 a 5; sem nota é outra coisa.
 * TV: a fileira é um item de foco só; ← / → andam pela escala "sem nota, 0, 1... 5" com o foco
 * parado; nas pontas a tecla passa adiante e o foco sai; ↑ / ↓ sempre saem.
 * Tablet: tocar numa estrela dá a nota dela, "0" dá 0 estrelas e "Tirar nota" tira a nota.
 * Não grava: avisa quem usa, que grava e devolve o que ficou em {@link #mostrar}.
 */
public class EstrelasNota extends LinearLayout {

    /** Nota escolhida; null tira a nota. */
    public interface AoEscolher {
        void aoEscolher(@Nullable Integer nota);
    }

    private final ImageView[] estrelas = new ImageView[Modelo.NOTA_MAXIMA];
    private boolean tv;
    private AoEscolher aoEscolher;
    private Integer nota;
    private TextView zero;
    private TextView rotulo;

    public EstrelasNota(Context contexto) {
        super(contexto);
    }

    public EstrelasNota(Context contexto, @Nullable AttributeSet atributos) {
        super(contexto, atributos);
    }

    /** Monta a fileira (uma vez, ao abrir a tela). */
    public void preparar(boolean tv, AoEscolher aoEscolher) {
        this.tv = tv;
        this.aoEscolher = aoEscolher;
        Resources res = getResources();
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        float tamanhoTexto = res.getDimension(tv ? R.dimen.st_tv_texto_detalhe_marcar : R.dimen.st_nota_tablet_texto);
        int celula = res.getDimensionPixelSize(tv ? R.dimen.st_nota_tv_estrela : R.dimen.st_nota_tablet_celula);

        if (tv) {
            FocoTv.aplicar(this);
            setBackgroundResource(R.drawable.st_botao_contorno);
            int padding = res.getDimensionPixelSize(R.dimen.st_tv_detalhe_marcar_padding);
            setPaddingRelative(padding, 0, padding, 0);
            setOnKeyListener((v, tecla, evento) -> aoTeclar(tecla, evento));
        } else {
            zero = texto(tamanhoTexto);
            zero.setText(R.string.st_nota_zero);
            zero.setContentDescription(textoDar(0));
            zero.setOnClickListener(v -> escolher(0));
            addView(zero, new LayoutParams(celula, LayoutParams.MATCH_PARENT));
        }

        int espaco = res.getDimensionPixelSize(R.dimen.st_nota_tv_espaco);
        for (int i = 0; i < estrelas.length; i++) {
            ImageView estrela = new ImageView(getContext());
            estrela.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            LayoutParams lp = new LayoutParams(celula, LayoutParams.MATCH_PARENT);
            if (tv) {
                estrela.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
                if (i > 0) lp.setMarginStart(espaco);
            } else {
                final int valor = i + 1;
                estrela.setContentDescription(textoDar(valor));
                estrela.setOnClickListener(v -> escolher(valor));
            }
            estrelas[i] = estrela;
            addView(estrela, lp);
        }

        rotulo = texto(tamanhoTexto);
        LayoutParams lpRotulo = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        if (tv) {
            rotulo.setTextColor(ContextCompat.getColor(getContext(), R.color.st_texto_secundario));
            lpRotulo.setMarginStart(res.getDimensionPixelSize(R.dimen.st_nota_tv_rotulo_margem));
        } else {
            // Largura fixa: as estrelas não andam sob o dedo quando o texto troca
            rotulo.setMinWidth(res.getDimensionPixelSize(R.dimen.st_nota_tablet_rotulo_largura));
            rotulo.setOnClickListener(v -> escolher(null));
        }
        addView(rotulo, lpRotulo);
        mostrar(null);
    }

    /** Mostra a nota sem refazer a fileira: na TV, o foco continua nela. */
    public void mostrar(@Nullable Integer nota) {
        this.nota = nota;
        Context contexto = getContext();
        int cheias = nota == null ? 0 : nota;
        int corCheia = ContextCompat.getColor(contexto, R.color.st_nota_cheia);
        int corVazia = ContextCompat.getColor(contexto, R.color.st_texto_apagado);
        for (int i = 0; i < estrelas.length; i++) {
            boolean cheia = i < cheias;
            estrelas[i].setImageResource(cheia ? R.drawable.st_ic_estrela_cheia : R.drawable.st_ic_estrela);
            ImageViewCompat.setImageTintList(estrelas[i], ColorStateList.valueOf(cheia ? corCheia : corVazia));
        }
        if (tv) {
            String texto = nota == null ? contexto.getString(R.string.st_nota_sem)
                    : contexto.getString(R.string.st_nota_de, nota, Modelo.NOTA_MAXIMA);
            rotulo.setText(texto);
            setContentDescription(contexto.getString(R.string.st_nota_descricao_tv, texto));
        } else {
            zero.setTextColor(nota != null && cheias == 0 ? corCheia : corVazia);
            rotulo.setText(nota == null ? R.string.st_nota_sem : R.string.st_nota_tirar);
            rotulo.setTextColor(ContextCompat.getColor(contexto, nota == null ? R.color.st_texto_apagado : R.color.st_azul));
            rotulo.setClickable(nota != null);
        }
    }

    /** ← / → mudam a nota; o que não consome segue para a navegação normal do foco. */
    private boolean aoTeclar(int tecla, KeyEvent evento) {
        if (tecla != KeyEvent.KEYCODE_DPAD_LEFT && tecla != KeyEvent.KEYCODE_DPAD_RIGHT) return false;
        if (evento.getAction() != KeyEvent.ACTION_DOWN) return false;
        // Seta segurada: um passo por aperto, sem atravessar as notas nem sair da fileira sem querer
        if (evento.getRepeatCount() > 0) return true;
        int passo = tecla == KeyEvent.KEYCODE_DPAD_LEFT ? -1 : 1;
        if (naPonta(nota, passo)) return false;
        escolher(vizinha(nota, passo));
        return true;
    }

    private void escolher(@Nullable Integer escolhida) {
        if (aoEscolher != null) aoEscolher.aoEscolher(escolhida);
    }

    /** A seta passaria do fim da escala: sem nota à esquerda, 5 à direita. */
    static boolean naPonta(@Nullable Integer nota, int passo) {
        return nota == null ? passo < 0 : nota + passo > Modelo.NOTA_MAXIMA;
    }

    /** Próxima nota na escala "sem nota, 0, 1... 5" (fora das pontas: {@link #naPonta}). */
    @Nullable
    static Integer vizinha(@Nullable Integer nota, int passo) {
        if (nota == null) return 0;
        int nova = nota + passo;
        return nova < 0 ? null : Integer.valueOf(nova);
    }

    private String textoDar(int valor) {
        return getContext().getString(R.string.st_nota_dar, valor, Modelo.NOTA_MAXIMA);
    }

    private TextView texto(float tamanho) {
        TextView texto = new TextView(getContext());
        texto.setTypeface(ResourcesCompat.getFont(getContext(), R.font.st_inter_extrabold));
        texto.setIncludeFontPadding(false);
        texto.setSingleLine(true);
        texto.setGravity(Gravity.CENTER);
        texto.setTextSize(TypedValue.COMPLEX_UNIT_PX, tamanho);
        return texto;
    }
}
