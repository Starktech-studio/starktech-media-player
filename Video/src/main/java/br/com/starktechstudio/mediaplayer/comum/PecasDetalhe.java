// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.TypefaceSpan;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.ColorUtils;

import com.archos.mediacenter.video.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import br.com.starktechstudio.mediaplayer.dados.Modelo.Capitulo;
import br.com.starktechstudio.mediaplayer.dados.Modelo.DadosTecnicos;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Episodio;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Temporada;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Tipo;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Titulo;

/**
 * Partes iguais do Detalhe e da Série, na TV e no tablet (tv-detalhe, tablet-detalhe, tv-serie):
 * chips, dados técnicos, tempo, tipo, capítulo atual, próximo episódio e aviso da pasta.
 * As medidas mudam pelo aparelho.
 */
public final class PecasDetalhe {

    private PecasDetalhe() {}

    /** "Filme", "Série", "Desenho", "Show" ou "Stand-up" (rótulo em caixa alta acima do título). */
    @StringRes
    public static int rotuloTipo(Tipo tipo) {
        if (tipo == Tipo.SERIE) return R.string.st_tipo_serie;
        if (tipo == Tipo.DESENHO) return R.string.st_tipo_desenho;
        if (tipo == Tipo.SHOW) return R.string.st_tipo_show;
        if (tipo == Tipo.STANDUP) return R.string.st_tipo_standup;
        return R.string.st_tipo_filme;
    }

    /** "Ver todos" (azul): no Detalhe, abre o painel com todas as faixas de áudio e legenda. */
    private static TextView verTodos(Context contexto, float tamanho, boolean tv, Runnable acao) {
        TextView link = new TextView(contexto);
        link.setText(R.string.st_ver_todas_faixas);
        link.setTypeface(ResourcesCompat.getFont(contexto, R.font.st_inter_extrabold));
        link.setIncludeFontPadding(false);
        link.setTextSize(TypedValue.COMPLEX_UNIT_PX, tamanho);
        link.setTextColor(ContextCompat.getColor(contexto, R.color.st_azul));
        int folga = Math.round(tamanho / 3);
        link.setPadding(folga, folga, folga, folga);
        link.setFocusable(true);
        link.setClickable(true);
        if (tv) link.setForeground(ContextCompat.getDrawable(contexto, R.drawable.st_foco_tv));
        link.setOnClickListener(v -> acao.run());
        return link;
    }

    /** 04:12, ou 1:04:12 a partir de uma hora. */
    public static String tempo(int segundos) {
        int s = Math.max(0, segundos);
        int horas = s / 3600;
        int minutos = (s % 3600) / 60;
        int resto = s % 60;
        return horas > 0
                ? String.format(Locale.ROOT, "%d:%02d:%02d", horas, minutos, resto)
                : String.format(Locale.ROOT, "%02d:%02d", minutos, resto);
    }

    /** Número do capítulo onde o vídeo parou; 0 se não está em andamento ou não tem capítulos. */
    public static int capituloAtual(List<Capitulo> capitulos, Titulo titulo) {
        if (!titulo.emAndamento()) return 0;
        int atual = 0;
        for (Capitulo c : capitulos) {
            if (c.inicioSegundos <= titulo.posicaoSegundos) atual = c.numero;
        }
        return atual;
    }

    /** Próximo episódio da série: o em andamento; senão o primeiro não assistido; senão o primeiro. */
    public static Episodio proximoEpisodio(List<Temporada> temporadas) {
        Episodio primeiroNaoAssistido = null;
        Episodio primeiro = null;
        for (Temporada temporada : temporadas) {
            for (Episodio e : temporada.episodios) {
                if (e.emAndamento()) return e;
                if (primeiro == null) primeiro = e;
                if (primeiroNaoAssistido == null && !e.assistido) primeiroNaoAssistido = e;
            }
        }
        return primeiroNaoAssistido != null ? primeiroNaoAssistido : primeiro;
    }

    /** "Continuar · Episódio 3" (em andamento) ou "Assistir · Episódio 4"; fora da 1ª temporada, "T2 E3". */
    public static String textoProximo(Context contexto, Episodio e) {
        boolean continuar = e.emAndamento();
        if (e.temporada <= 1) {
            return contexto.getString(continuar ? R.string.st_continuar_episodio : R.string.st_assistir_episodio, e.numero);
        }
        return contexto.getString(continuar ? R.string.st_continuar_temporada_episodio : R.string.st_assistir_temporada_episodio,
                e.temporada, e.numero);
    }

    /**
     * Aviso da pasta da série: ícone de pasta em ciano e "Organizada a partir da pasta
     * Séries / Nome / Temporada 1 / 01.mkv, 02.mkv… — …", com o caminho em monoespaçada branca.
     */
    public static void avisoPasta(TextView aviso, Titulo serie, boolean tv) {
        Context contexto = aviso.getContext();
        String caminho = contexto.getString(R.string.st_aviso_pasta_caminho, serie.arquivo);
        SpannableStringBuilder texto = new SpannableStringBuilder(contexto.getString(R.string.st_aviso_pasta_inicio));
        int inicio = texto.length();
        texto.append(caminho);
        texto.setSpan(new TypefaceSpan("monospace"), inicio, texto.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        texto.setSpan(new ForegroundColorSpan(ContextCompat.getColor(contexto, R.color.st_texto)),
                inicio, texto.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        texto.append(contexto.getString(R.string.st_aviso_pasta_fim));
        aviso.setText(texto);
        Icones.noTexto(aviso, R.drawable.st_ic_pastas,
                contexto.getResources().getDimensionPixelSize(tv ? R.dimen.st_tv_aviso_icone : R.dimen.st_tablet_aviso_icone),
                ContextCompat.getColor(contexto, R.color.st_ciano));
    }

    /**
     * Fundo do título no Detalhe e na Série da TV: a cor do fundo (bloco na F1) com o véu 72 % já
     * misturado; o último quarto some no transparente, sobre o fundo animado, sem emenda.
     */
    public static GradientDrawable fundoDoTitulo(Context contexto, int cor) {
        int composta = ColorUtils.compositeColors(ContextCompat.getColor(contexto, R.color.st_veu_72), cor);
        int transparente = composta & 0x00FFFFFF;
        return new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{composta, composta, composta, composta, transparente});
    }

    /** "MKV", "MP4"… pela extensão do arquivo (subtítulo da fileira de capítulos). */
    public static String extensao(String arquivo) {
        if (arquivo == null) return "";
        int ponto = arquivo.lastIndexOf('.');
        return ponto < 0 ? "" : arquivo.substring(ponto + 1).toUpperCase(Locale.ROOT);
    }

    /**
     * Chips do Detalhe: ano, duração e gênero (branco 8 %) e os técnicos; os
     * {@code chipsVideo} primeiros técnicos são do vídeo e ficam em azul, como no desenho.
     */
    public static void chips(LinearLayout alvo, Titulo titulo, List<String> tecnicos, int chipsVideo, boolean tv) {
        Context contexto = alvo.getContext();
        List<String> textos = new ArrayList<>();
        List<Boolean> azuis = new ArrayList<>();
        for (String simples : new String[]{titulo.ano, contexto.getString(R.string.st_duracao_min, titulo.duracaoMin), titulo.genero}) {
            if (simples == null || simples.isEmpty()) continue;
            textos.add(simples);
            azuis.add(false);
        }
        for (int i = 0; i < tecnicos.size(); i++) {
            textos.add(tecnicos.get(i));
            azuis.add(i < chipsVideo);
        }
        chips(alvo, textos, azuis, tv);
    }

    /** Chips com os textos dados; {@code azuis} marca os que ficam em azul (mesmo tamanho da lista). */
    public static void chips(LinearLayout alvo, List<String> textos, List<Boolean> azuis, boolean tv) {
        alvo.removeAllViews();
        Context contexto = alvo.getContext();
        Resources res = contexto.getResources();
        int ph = res.getDimensionPixelSize(tv ? R.dimen.st_tv_selo_padding_h : R.dimen.st_tablet_detalhe_chip_padding_h);
        int pv = res.getDimensionPixelSize(tv ? R.dimen.st_tv_selo_padding_v : R.dimen.st_tablet_detalhe_chip_padding_v);
        int espaco = res.getDimensionPixelSize(tv ? R.dimen.st_tv_detalhe_chip_espaco : R.dimen.st_tablet_detalhe_chip_espaco);
        float tamanho = res.getDimension(tv ? R.dimen.st_tv_texto_selo : R.dimen.st_tablet_texto_detalhe_chip);
        Typeface fonte = ResourcesCompat.getFont(contexto, R.font.st_inter_bold);
        for (int i = 0; i < textos.size(); i++) {
            boolean azul = azuis.get(i);
            TextView chip = new TextView(contexto);
            chip.setText(textos.get(i));
            chip.setTypeface(fonte);
            chip.setIncludeFontPadding(false);
            chip.setSingleLine(true);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_PX, tamanho);
            chip.setTextColor(ContextCompat.getColor(contexto, azul ? R.color.st_texto : R.color.st_texto_secundario));
            chip.setBackgroundResource(azul ? R.drawable.st_selo_destaque : R.drawable.st_selo);
            chip.setPadding(ph, pv, ph, pv);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i > 0) lp.setMarginStart(espaco);
            alvo.addView(chip, lp);
        }
    }

    /** Dados técnicos em grade de 3 colunas: Vídeo, Áudio, Legendas, Arquivo, Capítulos, Origem. */
    public static void dadosTecnicos(LinearLayout alvo, DadosTecnicos dados, boolean tv) {
        dadosTecnicos(alvo, dados, tv, null);
    }

    /**
     * Com {@code verTodas}: arquivo com mais faixas do que o resumo mostra ganha "Ver todos" embaixo
     * das legendas (abre o painel Áudio e legendas). Cada valor fica em até 2 linhas.
     */
    public static void dadosTecnicos(LinearLayout alvo, DadosTecnicos dados, boolean tv, Runnable verTodas) {
        Context contexto = alvo.getContext();
        Resources res = contexto.getResources();
        int[] rotulos = {R.string.st_dt_video, R.string.st_dt_audio, R.string.st_dt_legendas,
                R.string.st_dt_arquivo, R.string.st_dt_capitulos, R.string.st_dt_origem};
        String[] valores = {dados.video, dados.audio, dados.legendas, dados.arquivo, dados.capitulos, dados.origem};
        float tamanhoRotulo = res.getDimension(tv ? R.dimen.st_tv_texto_dados_rotulo : R.dimen.st_tablet_texto_dados_rotulo);
        float tamanhoValor = res.getDimension(tv ? R.dimen.st_tv_texto_dados_valor : R.dimen.st_tablet_texto_dados_valor);
        int espacoRotulo = res.getDimensionPixelSize(tv ? R.dimen.st_tv_detalhe_dados_rotulo_espaco : R.dimen.st_tablet_dados_rotulo_espaco);
        Typeface black = ResourcesCompat.getFont(contexto, R.font.st_inter_black);
        Typeface bold = ResourcesCompat.getFont(contexto, R.font.st_inter_bold);

        List<View> celulas = new ArrayList<>();
        for (int i = 0; i < rotulos.length; i++) {
            LinearLayout celula = new LinearLayout(contexto);
            celula.setOrientation(LinearLayout.VERTICAL);

            TextView rotulo = new TextView(contexto);
            rotulo.setText(rotulos[i]);
            rotulo.setTypeface(black);
            rotulo.setIncludeFontPadding(false);
            rotulo.setAllCaps(true);
            rotulo.setLetterSpacing(0.15f);
            rotulo.setTextSize(TypedValue.COMPLEX_UNIT_PX, tamanhoRotulo);
            rotulo.setTextColor(ContextCompat.getColor(contexto, R.color.st_texto_apagado));
            celula.addView(rotulo);

            TextView valor = new TextView(contexto);
            valor.setText(valores[i]);
            valor.setTypeface(bold);
            valor.setIncludeFontPadding(false);
            valor.setTextSize(TypedValue.COMPLEX_UNIT_PX, tamanhoValor);
            valor.setTextColor(ContextCompat.getColor(contexto, R.color.st_texto_secundario));
            valor.setMaxLines(2);
            valor.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = espacoRotulo;
            celula.addView(valor, lp);
            if (rotulos[i] == R.string.st_dt_legendas && dados.faixasDemais && verTodas != null) {
                celula.addView(verTodos(contexto, tamanhoValor, tv, verTodas), lp);
            }
            celulas.add(celula);
        }
        Grade.preencher(alvo, celulas, 3,
                res.getDimensionPixelSize(tv ? R.dimen.st_tv_detalhe_dados_espaco_h : R.dimen.st_tablet_dados_espaco_h),
                res.getDimensionPixelSize(tv ? R.dimen.st_tv_detalhe_dados_espaco_v : R.dimen.st_tablet_dados_espaco_v));
    }
}
