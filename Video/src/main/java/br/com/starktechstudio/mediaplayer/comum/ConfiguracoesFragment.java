// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.archos.mediacenter.video.R;

import br.com.starktechstudio.mediaplayer.comum.Navegacao.Aba;
import br.com.starktechstudio.mediaplayer.dados.RepositorioBiblioteca;

/**
 * Configurações (D16, sem desenho; no padrão de Pastas): tela curta com Pastas da biblioteca,
 * Atualizar biblioteca, Áudio preferido, Legenda preferida, Tamanho da legenda, Retomar de onde
 * parou, Informações de uso e Sobre. OK ou toque numa configuração passa ao próximo valor ({@link Preferencias},
 * simulado na F1). TV: tela aberta pelo botão do topo; tablet: aba Ajustes (título "Configurações").
 */
public class ConfiguracoesFragment extends Fragment {

    private boolean tv;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context contexto = requireContext();
        tv = Aparelho.ehTv(contexto);
        LinearLayout pagina = new LinearLayout(contexto);
        pagina.setOrientation(LinearLayout.VERTICAL);
        int lateral = getResources().getDimensionPixelSize(tv ? R.dimen.st_tv_margem_lateral : R.dimen.st_tablet_margem_h);
        int topo = tv ? Medidas.px(contexto, 44) : getResources().getDimensionPixelSize(R.dimen.st_tablet_margem_v);
        pagina.setPadding(lateral, topo, lateral, Medidas.px(contexto, 44));

        TextView titulo = Textos.novo(contexto, getString(R.string.st_configuracoes), R.font.st_inter_black, tv ? 48 : 43, R.color.st_texto);
        titulo.setLetterSpacing(-0.02f);
        pagina.addView(titulo);
        TextView dica = Textos.novo(contexto, getString(R.string.st_configuracoes_dica), R.font.st_inter_semibold, 20, R.color.st_texto_apagado);
        adicionar(pagina, dica, Medidas.px(contexto, 6));

        // Largura de leitura confortável: até 1100 px do desenho
        LinearLayout lista = new LinearLayout(contexto);
        lista.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpLista = new LinearLayout.LayoutParams(Math.min(
                getResources().getDisplayMetrics().widthPixels, Medidas.px(contexto, 1100)), ViewGroup.LayoutParams.WRAP_CONTENT);
        lpLista.topMargin = Medidas.px(contexto, 32);
        pagina.addView(lista, lpLista);

        View primeira = acao(lista, R.drawable.st_ic_pastas, R.string.st_cfg_pastas, R.string.st_cfg_pastas_dica,
                () -> Navegacao.abrirAba(requireActivity(), Aba.PASTAS));
        acao(lista, R.drawable.st_ic_atualizar, R.string.st_atualizar_biblioteca, R.string.st_cfg_atualizar_dica, () -> {
            RepositorioBiblioteca.atual(contexto).atualizarBiblioteca();
            Toast.makeText(contexto, R.string.st_biblioteca_atualizada, Toast.LENGTH_SHORT).show();
        });
        valor(lista, R.drawable.st_ic_audio, R.string.st_cfg_audio, R.string.st_cfg_audio_dica, Preferencias.Item.AUDIO);
        valor(lista, R.drawable.st_ic_legendas, R.string.st_cfg_legenda, R.string.st_cfg_legenda_dica, Preferencias.Item.LEGENDA);
        valor(lista, R.drawable.st_ic_legendas, R.string.st_cfg_tamanho_legenda, R.string.st_cfg_tamanho_legenda_dica, Preferencias.Item.TAMANHO_LEGENDA);
        valor(lista, R.drawable.st_ic_do_inicio, R.string.st_cfg_retomar, R.string.st_cfg_retomar_dica, Preferencias.Item.RETOMAR);
        acao(lista, R.drawable.st_ic_ajuda, R.string.st_cfg_uso, R.string.st_cfg_uso_dica,
                () -> Navegacao.abrirInformacoesUso(requireActivity()));
        acao(lista, R.drawable.st_ic_info, R.string.st_cfg_sobre, R.string.st_cfg_sobre_dica,
                () -> Navegacao.abrirSobre(requireActivity()));
        if (tv && getActivity() instanceof ConfiguracoesActivity) primeira.post(primeira::requestFocus);

        ViewGroup rolagem;
        if (tv) {
            ScrollView sv = new ScrollView(contexto);
            sv.setFocusable(false);
            rolagem = sv;
        } else {
            NestedScrollView sv = new NestedScrollView(contexto);
            sv.setClipToPadding(false);
            BordasSistema.aplicar(sv, false, false, false, true);
            rolagem = sv;
        }
        rolagem.setVerticalScrollBarEnabled(false);
        rolagem.setOverScrollMode(View.OVER_SCROLL_NEVER);
        rolagem.addView(pagina, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return rolagem;
    }

    /** Linha que abre outra tela ou faz uma ação. */
    private View acao(LinearLayout lista, @DrawableRes int icone, @StringRes int nome, @StringRes int dica, Runnable acao) {
        LinearLayout linha = linha(lista, icone, nome, dica);
        linha.setOnClickListener(v -> acao.run());
        return linha;
    }

    /** Linha com o valor atual à direita; OK / toque passa ao próximo. */
    private void valor(LinearLayout lista, @DrawableRes int icone, @StringRes int nome, @StringRes int dica, Preferencias.Item item) {
        Context contexto = lista.getContext();
        LinearLayout linha = linha(lista, icone, nome, dica);
        TextView valor = Textos.novo(contexto, getString(Preferencias.valor(contexto, item)), R.font.st_inter_extrabold, 18, R.color.st_texto);
        valor.setSingleLine(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMarginStart(Medidas.px(contexto, 22));
        linha.addView(valor, lp);
        linha.setOnClickListener(v -> {
            valor.setText(Preferencias.proximo(contexto, item));
            linha.setContentDescription(getString(nome) + ", " + valor.getText());
        });
        linha.setContentDescription(getString(nome) + ", " + valor.getText());
    }

    /** Cartão: ícone azul, nome (Inter 800) e a dica embaixo. */
    private LinearLayout linha(LinearLayout lista, @DrawableRes int icone, @StringRes int nome, @StringRes int dica) {
        Context contexto = lista.getContext();
        LinearLayout linha = new LinearLayout(contexto);
        linha.setOrientation(LinearLayout.HORIZONTAL);
        linha.setGravity(Gravity.CENTER_VERTICAL);
        linha.setBackgroundResource(R.drawable.st_fundo_cartao);
        linha.setPadding(Medidas.px(contexto, 22), Medidas.px(contexto, 18), Medidas.px(contexto, 22), Medidas.px(contexto, 18));
        linha.setFocusable(true);
        linha.setClickable(true);
        if (tv) {
            linha.setForeground(ContextCompat.getDrawable(contexto, R.drawable.st_foco_tv));
        } else {
            TypedValue valor = new TypedValue();
            contexto.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, valor, true);
            linha.setForeground(ContextCompat.getDrawable(contexto, valor.resourceId));
        }

        ImageView imagem = new ImageView(contexto);
        imagem.setImageResource(icone);
        ImageViewCompat.setImageTintList(imagem, ContextCompat.getColorStateList(contexto, R.color.st_azul));
        linha.addView(imagem, new LinearLayout.LayoutParams(Medidas.px(contexto, 28), Medidas.px(contexto, 28)));

        LinearLayout textos = new LinearLayout(contexto);
        textos.setOrientation(LinearLayout.VERTICAL);
        TextView titulo = Textos.novo(contexto, getString(nome), R.font.st_inter_extrabold, 23, R.color.st_texto);
        titulo.setSingleLine(true);
        titulo.setEllipsize(TextUtils.TruncateAt.END);
        textos.addView(titulo);
        adicionar(textos, Textos.novo(contexto, getString(dica), R.font.st_inter_semibold, 17, R.color.st_texto_apagado),
                Medidas.px(contexto, 4));
        LinearLayout.LayoutParams lpTextos = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lpTextos.setMarginStart(Medidas.px(contexto, 22));
        linha.addView(textos, lpTextos);
        linha.setContentDescription(getString(nome));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if (lista.getChildCount() > 0) lp.topMargin = Medidas.px(contexto, 12);
        lista.addView(linha, lp);
        return linha;
    }

    private static void adicionar(LinearLayout pai, View filho, int topo) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = topo;
        pai.addView(filho, lp);
    }
}
