// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import br.com.starktechstudio.mediaplayer.comum.Navegacao.Aba;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Origem;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Pasta;
import br.com.starktechstudio.mediaplayer.dados.Modelo.TipoPasta;
import br.com.starktechstudio.mediaplayer.dados.RepositorioBiblioteca;

/**
 * Pastas, aberta em Configurações &gt; Biblioteca (tv-pastas.dc.html; tablet-pastas.dc.html, a mesma tela compacta): Atualizar
 * biblioteca e Adicionar pasta; pastas por origem (HD externo "conectado" / "desconectado" e
 * armazenamento interno), cada uma com "reconhecida pelo nome" ou "escolha o tipo", o caminho e a
 * contagem, e o tipo Filmes, Séries, TV Shows, Desenhos, Shows, Stand-up, os de abas ligadas em Configurações &gt; Navegação,
 * ou Ignorar (marcado em verde / laranja e gravado no Salvar, que varre de novo), e Remover;
 * "Não identificados" com Revisar; painel "Como organizar". Montada no código, medidas do desenho
 * da TV ({@link Medidas}).
 */
public class PastasFragment extends Fragment {

    private static final TipoPasta[] TIPOS = {TipoPasta.FILMES, TipoPasta.SERIES, TipoPasta.TVSHOWS, TipoPasta.DESENHOS,
            TipoPasta.SHOWS, TipoPasta.STANDUP, TipoPasta.IGNORAR};
    private static final int[] NOMES_TIPOS = {R.string.st_tipo_filmes, R.string.st_tipo_series, R.string.st_tipo_tvshows,
            R.string.st_tipo_desenhos, R.string.st_tipo_shows, R.string.st_tipo_standup, R.string.st_tipo_ignorar};

    private RepositorioBiblioteca repositorio;
    private boolean tv;
    private LinearLayout lista;
    /** Tipos marcados e ainda não salvos (id da pasta → tipo); só valem depois de Salvar. */
    private final Map<String, TipoPasta> pendentes = new LinkedHashMap<>();
    private Button salvar;
    private Button adicionar;
    private IndicadorAtualizacao indicador;
    /** A próxima linha montada é a primeira da lista (TV: ↑ dela vai para Adicionar pasta). */
    private boolean primeiraLinha;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context contexto = requireContext();
        repositorio = RepositorioBiblioteca.atual(contexto);
        tv = Aparelho.ehTv(contexto);

        LinearLayout pagina = new LinearLayout(contexto);
        pagina.setOrientation(LinearLayout.VERTICAL);
        int lateral = getResources().getDimensionPixelSize(tv ? R.dimen.st_tv_margem_lateral : R.dimen.st_tablet_margem_h);
        int topo = tv ? Medidas.px(contexto, 44) : getResources().getDimensionPixelSize(R.dimen.st_tablet_margem_v);
        pagina.setPadding(lateral, topo, lateral, Medidas.px(contexto, 44));
        // Tablet: sem tecla Voltar à vista, a tela tem o botão (como as subtelas de Configurações)
        if (!tv) {
            Button voltar = new Button(contexto, null, 0, R.style.StarkTech_Botao_Tablet_Secundario);
            voltar.setText(R.string.st_voltar);
            voltar.setSingleLine(true);
            Icones.noTexto(voltar, R.drawable.st_ic_voltar, getResources().getDimensionPixelSize(R.dimen.st_tablet_botao_icone));
            voltar.setOnClickListener(v -> requireActivity().finish());
            LinearLayout.LayoutParams lpVoltar = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, alturaBotao(contexto));
            lpVoltar.bottomMargin = Medidas.px(contexto, 22);
            pagina.addView(voltar, lpVoltar);
        }
        pagina.addView(cabecalho(contexto));

        LinearLayout corpo = new LinearLayout(contexto);
        corpo.setOrientation(LinearLayout.HORIZONTAL);
        corpo.setBaselineAligned(false);
        lista = new LinearLayout(contexto);
        lista.setOrientation(LinearLayout.VERTICAL);
        corpo.addView(lista, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout.LayoutParams lpPainel = new LinearLayout.LayoutParams(Medidas.px(contexto, 460), ViewGroup.LayoutParams.WRAP_CONTENT);
        lpPainel.setMarginStart(Medidas.px(contexto, 40));
        corpo.addView(comoOrganizar(contexto), lpPainel);
        LinearLayout.LayoutParams lpCorpo = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpCorpo.topMargin = Medidas.px(contexto, 32);
        pagina.addView(corpo, lpCorpo);

        // TV: rolagem padrão (segue o foco); tablet: passa por trás da barra de navegação
        ViewGroup rolagem;
        if (tv) {
            // O foco entra em Adicionar pasta, o primeiro botão da página; a lista desce dali
            ScrollView sv = new ScrollView(contexto) {
                @Override
                protected boolean onRequestFocusInDescendants(int direcao, Rect anterior) {
                    return adicionar.requestFocus() || super.onRequestFocusInDescendants(direcao, anterior);
                }
            };
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
        if (tv) adicionar.post(() -> adicionar.requestFocus());
        return rolagem;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Volta de Adicionar pasta ou de Não identificados; a varredura atualiza as contagens
        montarLista();
        repositorio.ouvir(aoMudarBiblioteca);
        indicador.ligar();
    }

    @Override
    public void onPause() {
        repositorio.deixarDeOuvir(aoMudarBiblioteca);
        indicador.desligar();
        super.onPause();
    }

    private final Runnable aoMudarBiblioteca = this::montarLista;

    /** "Pastas", a dica e os botões Atualizar biblioteca (só o ícone), Salvar (com tipo marcado) e Adicionar pasta. */
    private View cabecalho(Context contexto) {
        LinearLayout linha = new LinearLayout(contexto);
        linha.setOrientation(LinearLayout.HORIZONTAL);
        linha.setGravity(Gravity.BOTTOM);
        LinearLayout titulos = new LinearLayout(contexto);
        titulos.setOrientation(LinearLayout.VERTICAL);
        TextView titulo = Textos.novo(contexto, getString(R.string.st_aba_pastas), R.font.st_inter_black,
                tv ? Medidas.TITULO_PAGINA_TV : Medidas.TITULO_PAGINA_TABLET, R.color.st_texto);
        titulo.setLetterSpacing(-0.02f);
        titulos.addView(titulo);
        TextView dica = Textos.novo(contexto, getString(R.string.st_pastas_dica), R.font.st_inter_semibold, 20, R.color.st_texto_apagado);
        LinearLayout.LayoutParams lpDica = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpDica.topMargin = Medidas.px(contexto, 6);
        titulos.addView(dica, lpDica);
        linha.addView(titulos, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        // Andamento da atualização (ou ✓ verde), do lado oposto ao título
        indicador = new IndicadorAtualizacao(contexto, repositorio);
        LinearLayout.LayoutParams lpIndicador = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, alturaBotao(contexto));
        lpIndicador.setMarginEnd(Medidas.px(contexto, 18));
        linha.addView(indicador.view, lpIndicador);
        Button atualizar = botao(contexto, 0, R.drawable.st_ic_atualizar, false);
        atualizar.setContentDescription(getString(R.string.st_atualizar_biblioteca));
        // TV: ← de Atualizar fica nele (não cai na lista); ↓ dos botões vai para o tipo da primeira pasta
        atualizar.setId(View.generateViewId());
        atualizar.setNextFocusLeftId(atualizar.getId());
        atualizar.setNextFocusDownId(R.id.st_pastas_primeira);
        atualizar.setOnClickListener(v -> {
            if (!PermissaoVideos.pedirSeFaltar(requireActivity())) return;
            repositorio.atualizarAgora();
            Toast.makeText(contexto, R.string.st_atualizando_biblioteca, Toast.LENGTH_SHORT).show();
        });
        linha.addView(atualizar);
        // Salvar aparece com um tipo marcado e ainda não salvo
        salvar = botao(contexto, R.string.st_salvar, R.drawable.st_ic_check, true);
        salvar.setOnClickListener(v -> salvarTipos());
        salvar.setNextFocusDownId(R.id.st_pastas_primeira);
        salvar.setVisibility(View.GONE);
        LinearLayout.LayoutParams lpSalvar = (LinearLayout.LayoutParams) salvar.getLayoutParams();
        lpSalvar.setMarginStart(Medidas.px(contexto, 14));
        linha.addView(salvar, lpSalvar);
        adicionar = botao(contexto, R.string.st_adicionar_pasta, R.drawable.st_ic_pastas, true);
        adicionar.setId(R.id.st_pastas_adicionar);
        adicionar.setNextFocusDownId(R.id.st_pastas_primeira);
        adicionar.setOnClickListener(v -> Navegacao.abrirAdicionarPasta(requireActivity()));
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) adicionar.getLayoutParams();
        lp.setMarginStart(Medidas.px(contexto, 14));
        linha.addView(adicionar, lp);
        return linha;
    }

    /** Salvar: grava os tipos marcados e varre a biblioteca de novo (é aí que os vídeos mudam de aba). */
    private void salvarTipos() {
        if (pendentes.isEmpty()) return;
        for (Map.Entry<String, TipoPasta> item : pendentes.entrySet()) {
            repositorio.definirTipoPasta(item.getKey(), item.getValue());
        }
        pendentes.clear();
        // O Salvar some: o foco vai antes para Adicionar pasta (senão ficaria sem lugar certo)
        if (salvar.hasFocus()) adicionar.requestFocus();
        montarLista();
        Toast.makeText(requireContext(), R.string.st_tipos_salvos, Toast.LENGTH_SHORT).show();
    }

    private void mostrarSalvar() {
        boolean mostrar = !pendentes.isEmpty();
        if (!mostrar && salvar.hasFocus()) adicionar.requestFocus();
        salvar.setVisibility(mostrar ? View.VISIBLE : View.GONE);
    }

    private int alturaBotao(Context contexto) {
        return getResources().getDimensionPixelSize(tv ? R.dimen.st_tv_botao_altura : R.dimen.st_tablet_botao_altura);
    }

    /** Botão no padrão (64 px, Inter 800); sem texto, só o ícone. */
    private Button botao(Context contexto, @StringRes int texto, @DrawableRes int icone, boolean primario) {
        int estilo = tv ? (primario ? R.style.StarkTech_Botao_Tv_Primario : R.style.StarkTech_Botao_Tv_Secundario)
                : (primario ? R.style.StarkTech_Botao_Tablet_Primario : R.style.StarkTech_Botao_Tablet_Secundario);
        Button botao = new Button(contexto, null, 0, estilo);
        if (texto != 0) botao.setText(texto);
        int tamanho = getResources().getDimensionPixelSize(tv ? R.dimen.st_tv_botao_icone : R.dimen.st_tablet_botao_icone);
        Icones.noTexto(botao, icone, tamanho);
        if (texto == 0) botao.setCompoundDrawablePadding(0);
        botao.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, alturaBotao(contexto)));
        return botao;
    }

    // ---- Lista ----

    private void montarLista() {
        Context contexto = getContext();
        if (contexto == null || lista == null) return;
        View foco = lista.findFocus();
        Object marca = foco == null ? null : foco.getTag();
        // Refazer a lista com o foco nela perderia o foco: ele passa por Adicionar pasta e volta ao
        // mesmo lugar no fim
        if (foco != null && adicionar != null) adicionar.requestFocus();
        lista.removeAllViews();
        // Pasta que saiu (Remover) não tem mais tipo pendente
        List<Pasta> pastas = repositorio.pastas();
        Set<String> existentes = new HashSet<>();
        for (Pasta p : pastas) existentes.add(p.id);
        pendentes.keySet().retainAll(existentes);
        mostrarSalvar();

        if (!repositorio.podeLerVideos()) dica(contexto, getString(R.string.st_videos_sem_permissao));
        else if (pastas.isEmpty()) dica(contexto, getString(R.string.st_sem_pastas_dica));
        boolean hd = repositorio.hdConectado();
        primeiraLinha = true;
        grupo(contexto, Origem.HD_EXTERNO, pastas, hd, false);
        grupo(contexto, Origem.INTERNO, pastas, true, lista.getChildCount() > 0);
        naoIdentificados(contexto);

        if (marca != null) {
            View alvo = lista.findViewWithTag(marca);
            if (alvo != null) alvo.requestFocus();
        }
    }

    private void grupo(Context contexto, Origem origem, List<Pasta> pastas, boolean disponivel, boolean espacoAntes) {
        List<Pasta> doGrupo = new ArrayList<>();
        for (Pasta p : pastas) if (p.origem == origem) doGrupo.add(p);
        if (doGrupo.isEmpty()) return;

        LinearLayout cabeca = new LinearLayout(contexto);
        cabeca.setOrientation(LinearLayout.HORIZONTAL);
        cabeca.setGravity(Gravity.CENTER_VERTICAL);
        cabeca.addView(Textos.rotulo(contexto, getString(origem == Origem.HD_EXTERNO ? R.string.st_hd_externo_usb
                : R.string.st_armazenamento_interno), 14));
        if (origem == Origem.HD_EXTERNO) {
            TextView situacao = Textos.novo(contexto, getString(disponivel ? R.string.st_conectado : R.string.st_desconectado),
                    R.font.st_inter_extrabold, 16, disponivel ? R.color.st_selo_hd_texto : R.color.st_selo_sem_hd_texto);
            situacao.setCompoundDrawablePadding(Medidas.px(contexto, 6));
            Icones.noTexto(situacao, R.drawable.st_ic_hd, Medidas.px(contexto, 18));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginStart(Medidas.px(contexto, 14));
            cabeca.addView(situacao, lp);
        }
        LinearLayout.LayoutParams lpCabeca = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpCabeca.topMargin = espacoAntes ? Medidas.px(contexto, 24) : 0;
        lista.addView(cabeca, lpCabeca);

        for (Pasta p : doGrupo) {
            View linha = linha(contexto, p);
            // HD desligado (ou outro HD no lugar): a pasta fica apagada
            linha.setAlpha(disponivel && new File(p.id).isDirectory() ? 1f : 0.5f);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = Medidas.px(contexto, 12);
            lista.addView(linha, lp);
        }
    }

    /** Uma pasta: ícone, nome + selo, caminho e contagem, e o tipo (Filmes, Séries, Shows, Ignorar). */
    private View linha(Context contexto, Pasta p) {
        LinearLayout linha = new LinearLayout(contexto);
        linha.setOrientation(LinearLayout.HORIZONTAL);
        linha.setGravity(Gravity.CENTER_VERTICAL);
        linha.setBackgroundResource(R.drawable.st_fundo_cartao);
        linha.setPadding(Medidas.px(contexto, 22), Medidas.px(contexto, 16), Medidas.px(contexto, 22), Medidas.px(contexto, 16));

        ImageView icone = new ImageView(contexto);
        icone.setImageResource(R.drawable.st_ic_pastas);
        ImageViewCompat.setImageTintList(icone, ContextCompat.getColorStateList(contexto, R.color.st_azul));
        linha.addView(icone, new LinearLayout.LayoutParams(Medidas.px(contexto, 28), Medidas.px(contexto, 28)));

        LinearLayout textos = new LinearLayout(contexto);
        textos.setOrientation(LinearLayout.VERTICAL);
        LinearLayout nomeLinha = new LinearLayout(contexto);
        nomeLinha.setOrientation(LinearLayout.HORIZONTAL);
        nomeLinha.setGravity(Gravity.CENTER_VERTICAL);
        TextView nome = Textos.novo(contexto, p.nome, R.font.st_inter_extrabold, 23, R.color.st_texto);
        nome.setSingleLine(true);
        nome.setEllipsize(TextUtils.TruncateAt.END);
        nomeLinha.addView(nome);
        TextView selo = null;
        if (p.tipo == TipoPasta.NENHUM) {
            selo = Textos.novo(contexto, getString(R.string.st_escolha_tipo), R.font.st_inter_bold, 14, R.color.st_aviso_claro);
            selo.setBackgroundResource(R.drawable.st_selo_escolha_tipo);
        } else if (p.reconhecidaPeloNome) {
            selo = Textos.novo(contexto, getString(R.string.st_reconhecida_nome), R.font.st_inter_bold, 14, R.color.st_ciano_claro);
            selo.setBackgroundResource(R.drawable.st_selo_reconhecida);
            selo.setCompoundDrawablePadding(Medidas.px(contexto, 6));
            Icones.noTexto(selo, R.drawable.st_ic_check, Medidas.px(contexto, 14));
        }
        if (selo != null) {
            selo.setSingleLine(true);
            selo.setPadding(Medidas.px(contexto, 8), Medidas.px(contexto, 3), Medidas.px(contexto, 8), Medidas.px(contexto, 3));
            LinearLayout.LayoutParams lpSelo = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lpSelo.setMarginStart(Medidas.px(contexto, 10));
            nomeLinha.addView(selo, lpSelo);
        }
        textos.addView(nomeLinha);
        TextView caminho = Textos.novo(contexto, getString(R.string.st_pasta_linha, p.caminho, contagem(p)),
                R.font.st_inter_semibold, 17, R.color.st_texto_apagado);
        caminho.setSingleLine(true);
        caminho.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams lpCaminho = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpCaminho.topMargin = Medidas.px(contexto, 4);
        textos.addView(caminho, lpCaminho);
        LinearLayout.LayoutParams lpTextos = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lpTextos.setMarginStart(Medidas.px(contexto, 22));
        linha.addView(textos, lpTextos);

        LinearLayout.LayoutParams lpTipos = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpTipos.setMarginStart(Medidas.px(contexto, 22));
        linha.addView(tipos(contexto, p), lpTipos);
        return linha;
    }

    private String contagem(Pasta p) {
        if (p.tipo == TipoPasta.SERIES || p.tipo == TipoPasta.TVSHOWS || p.series > 0) {
            return getResources().getQuantityString(R.plurals.st_n_series, p.series, p.series) + " · "
                    + getResources().getQuantityString(R.plurals.st_n_episodios, p.episodios, p.episodios);
        }
        return getResources().getQuantityString(R.plurals.st_n_videos, p.videos, p.videos);
    }

    /**
     * Grupo Filmes / Séries / TV Shows / Desenhos / Shows / Stand-up / Ignorar (48 px, Inter 800): o escolhido em
     * verde (Ignorar em laranja), marcado na hora e gravado só no Salvar. Tipo de aba desligada em Navegação só
     * aparece se a pasta já é dele. No fim, Remover.
     */
    private View tipos(Context contexto, Pasta p) {
        LinearLayout grupo = new LinearLayout(contexto);
        grupo.setOrientation(LinearLayout.HORIZONTAL);
        grupo.setBackgroundResource(R.drawable.st_grupo_tipo_pasta);
        grupo.setContentDescription(getString(R.string.st_tipo_da_pasta));
        int folga = Medidas.px(contexto, 4);
        grupo.setPadding(folga, folga, folga, folga);
        // TV: primeira pasta ligada aos botões do alto (↑ em qualquer opção vai para Adicionar pasta)
        final boolean primeira = primeiraLinha;
        primeiraLinha = false;
        for (int i = 0; i < TIPOS.length; i++) {
            final TipoPasta tipo = TIPOS[i];
            Aba aba = Navegacao.abaDaPasta(tipo);
            if (aba != null && !Preferencias.abaLigada(contexto, aba) && tipoMarcado(p) != tipo) continue;
            TextView opcao = Textos.novo(contexto, getString(NOMES_TIPOS[i]), R.font.st_inter_extrabold, 17, R.color.st_texto_secundario);
            opcao.setTextColor(ContextCompat.getColorStateList(contexto, R.color.st_cor_segmento));
            opcao.setBackgroundResource(tipo == TipoPasta.IGNORAR ? R.drawable.st_segmento_laranja : R.drawable.st_segmento_verde);
            opcao.setGravity(Gravity.CENTER);
            opcao.setPadding(Medidas.px(contexto, 15), 0, Medidas.px(contexto, 15), 0);
            opcao.setSelected(tipoMarcado(p) == tipo);
            opcao.setFocusable(true);
            opcao.setClickable(true);
            opcao.setTag(p.id + ":" + tipo.name());
            if (primeira) {
                opcao.setNextFocusUpId(R.id.st_pastas_adicionar);
                if (tipoMarcado(p) == tipo || (grupo.getChildCount() == 0 && tipoMarcado(p) == TipoPasta.NENHUM)) opcao.setId(R.id.st_pastas_primeira);
            }
            if (tv) opcao.setForeground(ContextCompat.getDrawable(contexto, R.drawable.st_foco_tv));
            // Marca na hora (sem refazer a lista); vale só depois de Salvar
            opcao.setOnClickListener(v -> {
                if (tipo == p.tipo) pendentes.remove(p.id);
                else pendentes.put(p.id, tipo);
                for (int j = 0; j < grupo.getChildCount(); j++) {
                    View irmao = grupo.getChildAt(j);
                    Object marca = irmao.getTag();
                    if (marca instanceof String && !((String) marca).endsWith(":remover")) {
                        irmao.setSelected(marca.equals(p.id + ":" + tipo.name()));
                    }
                }
                mostrarSalvar();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, Medidas.px(contexto, 48));
            if (grupo.getChildCount() > 0) lp.setMarginStart(folga);
            grupo.addView(opcao, lp);
        }
        // Remover (em vermelho): tira a pasta da biblioteca, depois de confirmar
        TextView remover = Textos.novo(contexto, getString(R.string.st_remover), R.font.st_inter_extrabold, 17, R.color.st_selo_sem_hd_texto);
        remover.setBackgroundResource(R.drawable.st_segmento);
        remover.setGravity(Gravity.CENTER);
        remover.setPadding(Medidas.px(contexto, 15), 0, Medidas.px(contexto, 15), 0);
        remover.setFocusable(true);
        remover.setClickable(true);
        remover.setTag(p.id + ":remover");
        // TV: → de Remover fica nele (não sobe para os botões do alto)
        remover.setId(View.generateViewId());
        remover.setNextFocusRightId(remover.getId());
        if (primeira) remover.setNextFocusUpId(R.id.st_pastas_adicionar);
        remover.setContentDescription(getString(R.string.st_remover_pasta_descricao, p.nome));
        if (tv) remover.setForeground(ContextCompat.getDrawable(contexto, R.drawable.st_foco_tv));
        remover.setOnClickListener(v -> Navegacao.abrirRemoverPasta(requireActivity(), p.id, p.caminho));
        LinearLayout.LayoutParams lpRemover = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, Medidas.px(contexto, 48));
        lpRemover.setMarginStart(folga);
        grupo.addView(remover, lpRemover);
        return grupo;
    }

    /** Tipo marcado da pasta: o pendente (ainda não salvo) ou o gravado. */
    private TipoPasta tipoMarcado(Pasta p) {
        TipoPasta pendente = pendentes.get(p.id);
        return pendente != null ? pendente : p.tipo;
    }

    /** Aviso no alto da lista (sem permissão, sem pastas). */
    private void dica(Context contexto, String texto) {
        TextView aviso = Textos.novo(contexto, texto, R.font.st_inter_semibold, 19, R.color.st_texto_secundario);
        aviso.setLineSpacing(0f, 1.2f);
        aviso.setBackgroundResource(R.drawable.st_fundo_cartao);
        int padding = Medidas.px(contexto, 22);
        aviso.setPadding(padding, Medidas.px(contexto, 16), padding, Medidas.px(contexto, 16));
        lista.addView(aviso, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    /** "Não identificados · N vídeos sem tipo ou fora do padrão" com Revisar. */
    private void naoIdentificados(Context contexto) {
        int quantidade = repositorio.naoIdentificados().size();
        if (quantidade == 0) return;
        LinearLayout caixa = new LinearLayout(contexto);
        caixa.setOrientation(LinearLayout.HORIZONTAL);
        caixa.setGravity(Gravity.CENTER_VERTICAL);
        caixa.setBackgroundResource(R.drawable.st_fundo_nao_identificados);
        caixa.setPadding(Medidas.px(contexto, 22), Medidas.px(contexto, 16), Medidas.px(contexto, 22), Medidas.px(contexto, 16));

        ImageView icone = new ImageView(contexto);
        icone.setImageResource(R.drawable.st_ic_info);
        ImageViewCompat.setImageTintList(icone, ContextCompat.getColorStateList(contexto, R.color.st_aviso_claro));
        caixa.addView(icone, new LinearLayout.LayoutParams(Medidas.px(contexto, 28), Medidas.px(contexto, 28)));

        LinearLayout textos = new LinearLayout(contexto);
        textos.setOrientation(LinearLayout.VERTICAL);
        textos.addView(Textos.novo(contexto, getString(R.string.st_nao_identificados), R.font.st_inter_extrabold, 21, R.color.st_texto));
        TextView sub = Textos.novo(contexto, getResources().getQuantityString(R.plurals.st_n_sem_tipo, quantidade, quantidade),
                R.font.st_inter_semibold, 17, R.color.st_texto_apagado);
        LinearLayout.LayoutParams lpSub = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpSub.topMargin = Medidas.px(contexto, 4);
        textos.addView(sub, lpSub);
        LinearLayout.LayoutParams lpTextos = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lpTextos.setMarginStart(Medidas.px(contexto, 18));
        caixa.addView(textos, lpTextos);

        Button revisar = botao(contexto, R.string.st_revisar, R.drawable.st_ic_editar, false);
        revisar.setTag("revisar");
        revisar.setOnClickListener(v -> Navegacao.abrirNaoIdentificados(requireActivity()));
        caixa.addView(revisar);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Medidas.px(contexto, 14);
        lista.addView(caixa, lp);
    }

    /** Painel "Como organizar": o tipo da pasta manda o vídeo; exemplos de nome de arquivo. */
    private View comoOrganizar(Context contexto) {
        LinearLayout painel = new LinearLayout(contexto);
        painel.setOrientation(LinearLayout.VERTICAL);
        painel.setBackgroundResource(R.drawable.st_fundo_como_organizar);
        int padding = Medidas.px(contexto, 26);
        painel.setPadding(padding, padding, padding, padding);
        int espaco = Medidas.px(contexto, 18);

        TextView titulo = Textos.novo(contexto, getString(R.string.st_como_organizar), R.font.st_inter_black, 22, R.color.st_texto);
        titulo.setCompoundDrawablePadding(Medidas.px(contexto, 10));
        Icones.noTexto(titulo, R.drawable.st_ic_info, Medidas.px(contexto, 24), ContextCompat.getColor(contexto, R.color.st_azul));
        titulo.setGravity(Gravity.CENTER_VERTICAL);
        painel.addView(titulo);

        TextView texto = Textos.novo(contexto, getString(R.string.st_como_organizar_texto), R.font.st_inter_semibold, 17, R.color.st_texto_secundario);
        texto.setLineSpacing(0f, 1.24f);
        adicionar(painel, texto, espaco);
        exemplo(painel, Aba.FILMES, R.string.st_tipo_filmes, R.string.st_exemplo_filme, espaco);
        exemplo(painel, Aba.SERIES, R.string.st_tipo_series, R.string.st_exemplo_serie, espaco);
        exemplo(painel, Aba.TVSHOWS, R.string.st_tipo_tvshows, R.string.st_exemplo_tvshow, espaco);
        exemplo(painel, Aba.DESENHOS, R.string.st_tipo_desenhos, R.string.st_exemplo_desenho, espaco);
        exemplo(painel, Aba.SHOWS, R.string.st_tipo_shows, R.string.st_exemplo_show, espaco);
        exemplo(painel, Aba.STANDUP, R.string.st_tipo_standup, R.string.st_exemplo_standup, espaco);
        TextView fim = Textos.novo(contexto, getString(R.string.st_como_organizar_fim), R.font.st_inter_semibold, 16, R.color.st_texto_apagado);
        fim.setLineSpacing(0f, 1.24f);
        adicionar(painel, fim, espaco);
        return painel;
    }

    /** Exemplo de nome de arquivo do tipo, se a aba dele está ligada em Navegação. */
    private void exemplo(LinearLayout painel, Aba aba, @StringRes int tipo, @StringRes int nome, int espaco) {
        Context contexto = painel.getContext();
        if (!Preferencias.abaLigada(contexto, aba)) return;
        adicionar(painel, Textos.rotulo(contexto, getString(tipo), 14), espaco);
        TextView mono = Textos.novo(contexto, getString(nome), R.font.st_inter_medium, 17, R.color.st_texto_secundario);
        mono.setTypeface(Typeface.MONOSPACE);
        adicionar(painel, mono, Medidas.px(contexto, 4));
    }

    private static void adicionar(LinearLayout pai, View filho, int topo) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = topo;
        pai.addView(filho, lp);
    }
}
