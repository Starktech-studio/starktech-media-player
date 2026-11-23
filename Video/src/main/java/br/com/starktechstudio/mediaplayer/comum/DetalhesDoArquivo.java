// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.archos.mediacenter.video.R;
import com.archos.mediacenter.video.utils.VideoMetadata;
import com.archos.medialib.LibAvos;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import br.com.starktechstudio.mediaplayer.dados.Modelo.DadosDoArquivo;
import br.com.starktechstudio.mediaplayer.motor.Faixas;

/**
 * Janela Detalhes do player (2.1.2), na TV e no tablet: o arquivo em rótulo e valor, em ordem alfabética e em grade
 * de 3 colunas, no visual dos dados técnicos do Detalhe. O que a biblioteca sabe ({@link DadosDoArquivo}) e o que o
 * motor contou ao abrir (decodificação e quadros por segundo). Sem o dado, "—".
 */
public final class DetalhesDoArquivo {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final String SEM = "—";
    /** Faixas de áudio e idiomas de legenda mostrados; o resto vira "+N". */
    static final int FAIXAS_NA_JANELA = 3;

    private DetalhesDoArquivo() {}

    /** Rótulo e valor de cada linha, em ordem alfabética do rótulo. */
    public static List<String[]> linhas(Context contexto, Reproducao reproducao) {
        DadosDoArquivo dados = reproducao.dadosDoArquivo();
        VideoMetadata.VideoTrack faixa = reproducao.faixaDeVideo();
        long duracaoMs = dados != null && dados.duracaoMs > 0 ? dados.duracaoMs : reproducao.duracao * 1000L;
        List<String[]> linhas = new ArrayList<>();
        linhas.add(linha(contexto, R.string.st_da_arquivo, dados == null ? "" : dados.nome));
        linhas.add(linha(contexto, R.string.st_da_audio, dados == null ? "" : lista(dados.audios)));
        linhas.add(linha(contexto, R.string.st_da_capitulos, dados == null ? "" : capitulos(contexto, dados)));
        linhas.add(linha(contexto, R.string.st_da_decodificacao, decodificacao(contexto, faixa)));
        linhas.add(linha(contexto, R.string.st_da_duracao, duracaoMs > 0 ? PecasDetalhe.tempo((int) (duracaoMs / 1000)) : ""));
        linhas.add(linha(contexto, R.string.st_da_formato, dados == null ? "" : PecasDetalhe.extensao(dados.nome)));
        linhas.add(linha(contexto, R.string.st_da_legendas, dados == null ? ""
                : dados.legendas.isEmpty() ? contexto.getString(R.string.st_da_sem_legendas) : lista(dados.legendas)));
        linhas.add(linha(contexto, R.string.st_da_origem, dados == null ? "" : dados.origem));
        linhas.add(linha(contexto, R.string.st_da_pasta, dados == null ? "" : dados.pasta));
        linhas.add(linha(contexto, R.string.st_da_quadros, faixa == null ? "" : quadros(faixa.fpsRate, faixa.fpsScale, faixa.fps)));
        linhas.add(linha(contexto, R.string.st_da_resolucao, dados == null ? "" : resolucao(dados.largura, dados.altura)));
        linhas.add(linha(contexto, R.string.st_da_tamanho, dados == null ? "" : tamanho(dados.tamanho)));
        linhas.add(linha(contexto, R.string.st_da_taxa, dados == null ? "" : taxa(dados.tamanho, duracaoMs)));
        linhas.add(linha(contexto, R.string.st_da_video, dados == null ? "" : dados.video));
        return linhas;
    }

    /** Troca o conteúdo de {@code alvo} (vertical) pelas linhas, em 3 colunas. */
    public static void preencher(LinearLayout alvo, List<String[]> linhas, boolean tv) {
        Context contexto = alvo.getContext();
        Resources res = contexto.getResources();
        float tamanhoRotulo = res.getDimension(tv ? R.dimen.st_tv_texto_dados_rotulo : R.dimen.st_tablet_texto_dados_rotulo);
        float tamanhoValor = res.getDimension(tv ? R.dimen.st_tv_texto_dados_valor : R.dimen.st_tablet_texto_dados_valor);
        int espacoRotulo = res.getDimensionPixelSize(tv ? R.dimen.st_tv_detalhe_dados_rotulo_espaco : R.dimen.st_tablet_dados_rotulo_espaco);
        Typeface black = ResourcesCompat.getFont(contexto, R.font.st_inter_black);
        Typeface bold = ResourcesCompat.getFont(contexto, R.font.st_inter_bold);
        List<View> celulas = new ArrayList<>();
        for (String[] linha : linhas) {
            LinearLayout celula = new LinearLayout(contexto);
            celula.setOrientation(LinearLayout.VERTICAL);

            TextView rotulo = new TextView(contexto);
            rotulo.setText(linha[0]);
            rotulo.setTypeface(black);
            rotulo.setIncludeFontPadding(false);
            rotulo.setAllCaps(true);
            rotulo.setLetterSpacing(0.15f);
            rotulo.setTextSize(TypedValue.COMPLEX_UNIT_PX, tamanhoRotulo);
            rotulo.setTextColor(ContextCompat.getColor(contexto, R.color.st_texto_apagado));
            celula.addView(rotulo);

            TextView valor = new TextView(contexto);
            valor.setText(linha[1]);
            valor.setTypeface(bold);
            valor.setIncludeFontPadding(false);
            valor.setTextSize(TypedValue.COMPLEX_UNIT_PX, tamanhoValor);
            valor.setTextColor(ContextCompat.getColor(contexto, R.color.st_texto));
            valor.setMaxLines(3);
            valor.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = espacoRotulo;
            celula.addView(valor, lp);
            celulas.add(celula);
        }
        Grade.preencher(alvo, celulas, 3,
                res.getDimensionPixelSize(tv ? R.dimen.st_tv_detalhe_dados_espaco_h : R.dimen.st_tablet_dados_espaco_h),
                res.getDimensionPixelSize(tv ? R.dimen.st_tv_detalhe_dados_espaco_v : R.dimen.st_tablet_dados_espaco_v));
    }

    private static String[] linha(Context contexto, int rotulo, @Nullable String valor) {
        return new String[]{contexto.getString(rotulo), valor == null || valor.trim().isEmpty() ? SEM : valor};
    }

    /** Até {@link #FAIXAS_NA_JANELA}, uma por linha; as outras em "+N". */
    static String lista(List<String> itens) {
        if (itens == null || itens.isEmpty()) return "";
        StringBuilder texto = new StringBuilder();
        int mostrar = Math.min(FAIXAS_NA_JANELA, itens.size());
        for (int i = 0; i < mostrar; i++) {
            if (i > 0) texto.append('\n');
            texto.append(itens.get(i));
        }
        if (itens.size() > mostrar) texto.append("  +").append(itens.size() - mostrar);
        return texto.toString();
    }

    private static String capitulos(Context contexto, DadosDoArquivo dados) {
        if (dados.capitulos <= 0) return contexto.getString(R.string.st_da_sem_capitulos);
        return dados.capitulosComNome ? contexto.getString(R.string.st_da_capitulos_com_nome, dados.capitulos)
                : String.valueOf(dados.capitulos);
    }

    /** Pelo motor: no aparelho (hardware) ou no processador (software); antes de abrir, vazio. */
    private static String decodificacao(Context contexto, @Nullable VideoMetadata.VideoTrack faixa) {
        if (faixa == null) return "";
        switch (faixa.decoder) {
            case LibAvos.MP_DECODER_SW:
                return contexto.getString(R.string.st_da_decodificacao_software);
            case LibAvos.MP_DECODER_HW_OMX:
            case LibAvos.MP_DECODER_HW_OMXPLUS:
            case LibAvos.MP_DECODER_HW_OMXCODEC:
            case LibAvos.MP_DECODER_HW_MEDIACODEC:
                return contexto.getString(R.string.st_da_decodificacao_hardware);
            default:
                return "";
        }
    }

    /** "3840 × 2160 (4K)"; sem o tamanho, vazio. */
    static String resolucao(int largura, int altura) {
        if (largura <= 0 || altura <= 0) return "";
        String qualidade = Faixas.qualidade(largura, altura);
        String base = largura + " × " + altura;
        return qualidade.isEmpty() ? base : base + " (" + qualidade + ")";
    }

    /** "850 KB", "734,5 MB", "6,21 GB" (base 1024); 0 = vazio. */
    static String tamanho(long bytes) {
        if (bytes <= 0) return "";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format(PT_BR, "%.0f KB", Math.max(1, kb));
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format(PT_BR, "%.1f MB", mb);
        return String.format(PT_BR, "%.2f GB", mb / 1024.0);
    }

    /** Taxa média do arquivo (vídeo e áudio juntos): "14,2 Mb/s" ou "850 kb/s"; sem tamanho ou duração, vazio. */
    static String taxa(long bytes, long duracaoMs) {
        if (bytes <= 0 || duracaoMs <= 0) return "";
        double kbps = bytes * 8.0 / duracaoMs;
        if (kbps < 1000) return String.format(PT_BR, "%.0f kb/s", kbps);
        return String.format(PT_BR, "%.1f Mb/s", kbps / 1000.0);
    }

    /** "23,976", "25", "29,97"; pelo par do motor e, sem ele, pelo número inteiro; nada, vazio. */
    static String quadros(int rate, int scale, int fps) {
        double valor = rate > 0 && scale > 0 ? rate / (double) scale : fps;
        if (valor <= 0) return "";
        double arredondado = Math.round(valor * 1000) / 1000.0;
        String texto = String.format(PT_BR, "%.3f", arredondado);
        // sem zeros à direita: 25,000 → 25; 29,970 → 29,97
        texto = texto.replaceAll("0+$", "").replaceAll(",$", "");
        return texto;
    }
}
