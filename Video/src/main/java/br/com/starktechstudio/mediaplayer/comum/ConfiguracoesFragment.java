// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
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

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.archos.mediacenter.video.R;

import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import br.com.starktechstudio.mediaplayer.comum.Navegacao.Aba;
import br.com.starktechstudio.mediaplayer.dados.RepositorioBiblioteca;
import br.com.starktechstudio.mediaplayer.motor.ArquivosDoAparelho;
import br.com.starktechstudio.mediaplayer.motor.Atualizador;

/**
 * Configurações (D16, sem desenho; no padrão de Pastas), salvas no aparelho ({@link Preferencias}).
 * Lista curta de seções (App, Biblioteca, Início, só na TV, Legendas, Player, Séries e Shows), e cada
 * uma abre a própria subtela; seções e opções em ordem alfabética. Voltar retorna da subtela à lista,
 * com o foco da TV na seção de onde veio; no tablet há também o botão Voltar. Opção de dois valores:
 * OK ou toque troca na hora; com mais valores, abre um seletor em janela. O que só existe na TV não
 * aparece no tablet (D12). TV: tela aberta pelo botão do topo; tablet: aba Ajustes (título
 * "Configurações").
 */
public class ConfiguracoesFragment extends Fragment {

    private static final String ESTADO_SECAO_ABERTA = "secaoAberta";

    /** Monta uma opção na subtela da seção. */
    private interface Montar {
        void em(LinearLayout corpo);
    }

    /** Valor escolhido no seletor, pelo índice. */
    private interface Escolha {
        void em(int indice);
    }

    private static final class Opcao {
        final String nome;
        final Montar montar;

        Opcao(String nome, Montar montar) {
            this.nome = nome;
            this.montar = montar;
        }
    }

    /** Uma seção: a linha na lista de seções e a subtela, que fica montada mesmo escondida. */
    private static final class Secao {
        @DrawableRes
        final int icone;
        @StringRes
        final int nome;
        @StringRes
        final int dica;
        final List<Opcao> opcoes = new ArrayList<>();
        View linha;
        LinearLayout corpo;

        Secao(@DrawableRes int icone, @StringRes int nome, @StringRes int dica) {
            this.icone = icone;
            this.nome = nome;
            this.dica = dica;
        }
    }

    private boolean tv;
    private View pastaImagens;
    private TextView valorPastaImagens;
    private final List<Secao> secoes = new ArrayList<>();
    /** Índice em {@link #secoes}; -1 = a lista de seções. */
    private int secaoAberta = -1;
    private boolean verificando;
    private TextView versaoPublicada;
    private TextView avisoDeVersao;
    private Button botaoDeVersao;
    /** Versão mais nova que a instalada, já achada: o botão passa a ser Baixar e instalar. */
    @Nullable
    private Atualizador.Versao versaoParaInstalar;
    private TextView titulo;
    private TextView dica;
    @Nullable
    private Button voltar;
    private LinearLayout listaDeSecoes;
    private ViewGroup rolagem;
    @Nullable
    private Dialog seletor;

    /** Voltar na subtela retorna à lista; na lista, segue o caminho normal da tela. */
    private final OnBackPressedCallback voltarParaSecoes = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            abrir(-1);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context contexto = requireContext();
        tv = Aparelho.ehTv(contexto);
        if (savedInstanceState != null) secaoAberta = savedInstanceState.getInt(ESTADO_SECAO_ABERTA, -1);
        LinearLayout pagina = new LinearLayout(contexto);
        pagina.setOrientation(LinearLayout.VERTICAL);
        int lateral = getResources().getDimensionPixelSize(tv ? R.dimen.st_tv_margem_lateral : R.dimen.st_tablet_margem_h);
        int topo = tv ? Medidas.px(contexto, 44) : getResources().getDimensionPixelSize(R.dimen.st_tablet_margem_v);
        pagina.setPadding(lateral, topo, lateral, Medidas.px(contexto, 44));

        // Tablet: sem tecla Voltar à vista, a subtela tem o botão
        if (!tv) {
            voltar = new Button(contexto, null, 0, R.style.StarkTech_Botao_Tablet_Secundario);
            voltar.setText(R.string.st_voltar);
            voltar.setSingleLine(true);
            Icones.noTexto(voltar, R.drawable.st_ic_voltar, getResources().getDimensionPixelSize(R.dimen.st_tablet_botao_icone));
            voltar.setOnClickListener(v -> abrir(-1));
            LinearLayout.LayoutParams lpVoltar = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    getResources().getDimensionPixelSize(R.dimen.st_tablet_botao_altura));
            lpVoltar.bottomMargin = Medidas.px(contexto, 22);
            pagina.addView(voltar, lpVoltar);
        }

        titulo = Textos.novo(contexto, getString(R.string.st_configuracoes), R.font.st_inter_black,
                tv ? Medidas.TITULO_PAGINA_TV : Medidas.TITULO_PAGINA_TABLET, R.color.st_texto);
        titulo.setLetterSpacing(-0.02f);
        pagina.addView(titulo);
        dica = Textos.novo(contexto, getString(R.string.st_configuracoes_dica), R.font.st_inter_semibold, 20, R.color.st_texto_apagado);
        adicionar(pagina, dica, Medidas.px(contexto, 6));

        // Largura de leitura confortável: até 1100 px do desenho
        LinearLayout conteudo = new LinearLayout(contexto);
        conteudo.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpConteudo = new LinearLayout.LayoutParams(Math.min(
                getResources().getDisplayMetrics().widthPixels, Medidas.px(contexto, 1100)), ViewGroup.LayoutParams.WRAP_CONTENT);
        lpConteudo.topMargin = Medidas.px(contexto, 32);
        pagina.addView(conteudo, lpConteudo);

        listaDeSecoes = new LinearLayout(contexto);
        listaDeSecoes.setOrientation(LinearLayout.VERTICAL);
        conteudo.addView(listaDeSecoes, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        montarSecoes(conteudo);
        if (secaoAberta >= secoes.size()) secaoAberta = -1;

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

        mostrarAberta();
        if (tv) {
            View primeira = secaoAberta >= 0 ? secoes.get(secaoAberta).corpo : secoes.get(0).linha;
            primeira.post(() -> primeira.requestFocus(View.FOCUS_DOWN));
        }
        return rolagem;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), voltarParaSecoes);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ESTADO_SECAO_ABERTA, secaoAberta);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Volta da escolha da pasta de imagens (ou o HD com a pasta "Imagens" foi ligado)
        Context contexto = requireContext();
        String pasta = Preferencias.pastaImagens(contexto);
        valorPastaImagens.setText(pasta == null ? getString(R.string.st_pasta_imagens_nenhuma)
                : ArquivosDoAparelho.exibir(contexto, pasta));
        pastaImagens.setContentDescription(getString(R.string.st_cfg_pasta_imagens) + ", " + valorPastaImagens.getText());
    }

    @Override
    public void onDestroyView() {
        if (seletor != null) seletor.dismiss();
        super.onDestroyView();
    }

    // ---- Seções e subtelas ----

    private void montarSecoes(LinearLayout conteudo) {
        Context contexto = conteudo.getContext();
        secoes.clear();
        definirApp();
        definirBiblioteca();
        if (tv) definirInicio();
        definirLegendas();
        definirPlayer();
        definirSeries();
        definirShows();

        Collator ordem = Collator.getInstance(Locale.forLanguageTag("pt-BR"));
        Collections.sort(secoes, (a, b) -> ordem.compare(getString(a.nome), getString(b.nome)));
        for (int i = 0; i < secoes.size(); i++) {
            Secao secao = secoes.get(i);
            final int indice = i;
            LinearLayout linha = linha(listaDeSecoes, secao.icone, secao.nome, secao.dica);
            adicionarSeta(linha);
            linha.setOnClickListener(v -> abrir(indice));
            secao.linha = linha;

            secao.corpo = new LinearLayout(contexto);
            secao.corpo.setOrientation(LinearLayout.VERTICAL);
            Collections.sort(secao.opcoes, (a, b) -> ordem.compare(a.nome, b.nome));
            for (Opcao opcao : secao.opcoes) opcao.montar.em(secao.corpo);
            conteudo.addView(secao.corpo, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }

    /**
     * Vai à subtela da seção (ou à lista, com -1). O foco da TV muda antes de esconder o que saiu: sem
     * foco, ele cairia no primeiro item da janela.
     */
    private void abrir(int indice) {
        int antes = secaoAberta;
        secaoAberta = indice;
        if (indice < 0) listaDeSecoes.setVisibility(View.VISIBLE);
        else secoes.get(indice).corpo.setVisibility(View.VISIBLE);
        rolagem.scrollTo(0, 0);
        if (tv) {
            View alvo = indice >= 0 ? secoes.get(indice).corpo : secoes.get(Math.max(antes, 0)).linha;
            alvo.requestFocus(View.FOCUS_DOWN);
        }
        mostrarAberta();
    }

    /** Só a lista ou só a subtela aberta à vista, com o título e a dica de cada uma. */
    private void mostrarAberta() {
        boolean naLista = secaoAberta < 0;
        listaDeSecoes.setVisibility(naLista ? View.VISIBLE : View.GONE);
        for (int i = 0; i < secoes.size(); i++) {
            secoes.get(i).corpo.setVisibility(i == secaoAberta ? View.VISIBLE : View.GONE);
        }
        Secao aberta = naLista ? null : secoes.get(secaoAberta);
        titulo.setText(aberta == null ? R.string.st_configuracoes : aberta.nome);
        dica.setText(aberta == null ? R.string.st_configuracoes_dica : aberta.dica);
        if (voltar != null) voltar.setVisibility(naLista ? View.GONE : View.VISIBLE);
        voltarParaSecoes.setEnabled(!naLista);
    }

    private Secao secao(@DrawableRes int icone, @StringRes int nome, @StringRes int dica) {
        Secao secao = new Secao(icone, nome, dica);
        secoes.add(secao);
        return secao;
    }

    private void opcao(Secao secao, @StringRes int nome, Montar montar) {
        secao.opcoes.add(new Opcao(getString(nome), montar));
    }

    private void opcaoAcao(Secao secao, @DrawableRes int icone, @StringRes int nome, @StringRes int dica, Runnable acao) {
        opcao(secao, nome, corpo -> acao(corpo, icone, nome, dica, acao));
    }

    private void opcaoValor(Secao secao, @DrawableRes int icone, @StringRes int nome, @StringRes int dica, Preferencias.Item item) {
        opcao(secao, nome, corpo -> valor(corpo, icone, nome, dica, item));
    }

    private void definirApp() {
        Secao secao = secao(R.drawable.st_ic_info, R.string.st_cfg_grupo_app, R.string.st_cfg_grupo_app_dica);
        opcaoAcao(secao, R.drawable.st_ic_ajuda, R.string.st_cfg_uso, R.string.st_cfg_uso_dica,
                () -> Navegacao.abrirInformacoesUso(requireActivity()));
        opcaoAcao(secao, R.drawable.st_ic_info, R.string.st_cfg_sobre, R.string.st_cfg_sobre_dica,
                () -> Navegacao.abrirSobre(requireActivity()));
        opcaoValor(secao, R.drawable.st_ic_atualizar, R.string.st_cfg_atualizar_app, R.string.st_cfg_atualizar_app_dica,
                Preferencias.Item.ATUALIZAR_APP);
        opcao(secao, R.string.st_cfg_versao, this::versao);
    }

    private void definirBiblioteca() {
        Context contexto = requireContext();
        Secao secao = secao(R.drawable.st_ic_pastas, R.string.st_cfg_grupo_biblioteca, R.string.st_cfg_grupo_biblioteca_dica);
        opcaoAcao(secao, R.drawable.st_ic_pastas, R.string.st_cfg_pastas, R.string.st_cfg_pastas_dica,
                () -> Navegacao.abrirAba(requireActivity(), Aba.PASTAS));
        opcaoAcao(secao, R.drawable.st_ic_atualizar, R.string.st_atualizar_biblioteca, R.string.st_cfg_atualizar_dica, () -> {
            RepositorioBiblioteca.atual(contexto).atualizarAgora();
            Toast.makeText(contexto, R.string.st_atualizando_biblioteca, Toast.LENGTH_SHORT).show();
        });
        opcaoValor(secao, R.drawable.st_ic_atualizar, R.string.st_cfg_atualizar_ao_abrir, R.string.st_cfg_atualizar_ao_abrir_dica,
                Preferencias.Item.ATUALIZAR_AO_ABRIR);
        opcao(secao, R.string.st_cfg_pasta_imagens, this::pastaDeImagens);
    }

    private void pastaDeImagens(LinearLayout corpo) {
        Context contexto = corpo.getContext();
        pastaImagens = acao(corpo, R.drawable.st_ic_imagem, R.string.st_cfg_pasta_imagens, R.string.st_cfg_pasta_imagens_dica,
                () -> Navegacao.abrirPastaImagens(requireActivity()));
        valorPastaImagens = Textos.novo(contexto, "", R.font.st_inter_extrabold, 18, R.color.st_texto);
        valorPastaImagens.setSingleLine(true);
        valorPastaImagens.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        valorPastaImagens.setMaxWidth(Medidas.px(contexto, 360));
        LinearLayout.LayoutParams lpPasta = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpPasta.setMarginStart(Medidas.px(contexto, 22));
        ((LinearLayout) pastaImagens).addView(valorPastaImagens, lpPasta);
    }

    /** Só na TV: o Início do tablet não tem Novidades. */
    private void definirInicio() {
        Secao secao = secao(R.drawable.st_ic_inicio, R.string.st_cfg_grupo_inicio, R.string.st_cfg_grupo_inicio_dica);
        opcaoValor(secao, R.drawable.st_ic_assistir, R.string.st_cfg_previa_video, R.string.st_cfg_previa_video_dica,
                Preferencias.Item.PREVIA_VIDEO);
        opcaoValor(secao, R.drawable.st_ic_atualizar, R.string.st_cfg_troca_novidades, R.string.st_cfg_troca_novidades_dica,
                Preferencias.Item.TROCA_NOVIDADES);
    }

    private void definirLegendas() {
        Secao secao = secao(R.drawable.st_ic_legendas, R.string.st_cfg_grupo_legendas, R.string.st_cfg_grupo_legendas_dica);
        int icone = R.drawable.st_ic_legendas;
        opcaoValor(secao, icone, R.string.st_cfg_legenda, R.string.st_cfg_legenda_dica, Preferencias.Item.LEGENDA);
        opcaoValor(secao, icone, R.string.st_cfg_tamanho_legenda, R.string.st_cfg_tamanho_legenda_dica, Preferencias.Item.TAMANHO_LEGENDA);
        opcaoValor(secao, icone, R.string.st_cfg_fonte_legenda, R.string.st_cfg_fonte_legenda_dica, Preferencias.Item.FONTE_LEGENDA);
        opcaoValor(secao, icone, R.string.st_cfg_negrito_legenda, R.string.st_cfg_negrito_legenda_dica, Preferencias.Item.NEGRITO_LEGENDA);
        opcaoValor(secao, icone, R.string.st_cfg_cor_legenda, R.string.st_cfg_cor_legenda_dica, Preferencias.Item.COR_LEGENDA);
        opcaoValor(secao, icone, R.string.st_cfg_contorno_legenda, R.string.st_cfg_contorno_legenda_dica, Preferencias.Item.CONTORNO_LEGENDA);
        opcaoValor(secao, icone, R.string.st_cfg_sombra_legenda, R.string.st_cfg_sombra_legenda_dica, Preferencias.Item.SOMBRA_LEGENDA);
        opcaoValor(secao, icone, R.string.st_cfg_fundo_legenda, R.string.st_cfg_fundo_legenda_dica, Preferencias.Item.FUNDO_LEGENDA);
        opcaoValor(secao, icone, R.string.st_cfg_posicao_legenda, R.string.st_cfg_posicao_legenda_dica, Preferencias.Item.POSICAO_LEGENDA);
    }

    private void definirPlayer() {
        Secao secao = secao(R.drawable.st_ic_assistir, R.string.st_cfg_grupo_player, R.string.st_cfg_grupo_player_dica);
        opcaoValor(secao, R.drawable.st_ic_audio, R.string.st_cfg_audio, R.string.st_cfg_audio_dica, Preferencias.Item.AUDIO);
        opcaoValor(secao, R.drawable.st_ic_do_inicio, R.string.st_cfg_retomar, R.string.st_cfg_retomar_dica, Preferencias.Item.RETOMAR);
        opcao(secao, R.string.st_cfg_zoom, this::zoom);
        opcaoValor(secao, R.drawable.st_ic_cap_proximo, tv ? R.string.st_cfg_pulo_tv : R.string.st_cfg_pulo_tablet,
                tv ? R.string.st_cfg_pulo_tv_dica : R.string.st_cfg_pulo_tablet_dica, Preferencias.Item.PULO);
        opcaoValor(secao, R.drawable.st_ic_pausar, R.string.st_cfg_tempo_controles, R.string.st_cfg_tempo_controles_dica,
                Preferencias.Item.TEMPO_CONTROLES);
        if (tv) {
            opcaoValor(secao, R.drawable.st_ic_ajustes, R.string.st_cfg_taxa_tv, R.string.st_cfg_taxa_tv_dica, Preferencias.Item.TAXA_DA_TV);
        }
        opcaoValor(secao, R.drawable.st_ic_ajustes, R.string.st_cfg_decodificacao, R.string.st_cfg_decodificacao_dica,
                Preferencias.Item.DECODIFICACAO);
        if (tv) {
            opcaoValor(secao, R.drawable.st_ic_audio, R.string.st_cfg_audio_direto, R.string.st_cfg_audio_direto_dica,
                    Preferencias.Item.AUDIO_DIRETO);
        }
        opcaoValor(secao, R.drawable.st_ic_imagem, R.string.st_cfg_dolby_vision, R.string.st_cfg_dolby_vision_dica,
                Preferencias.Item.DOLBY_VISION);
    }

    private void definirSeries() {
        Secao secao = secao(R.drawable.st_ic_series, R.string.st_cfg_grupo_series, R.string.st_cfg_grupo_series_dica);
        opcaoValor(secao, R.drawable.st_ic_series, R.string.st_cfg_proximo_episodio, R.string.st_cfg_proximo_episodio_dica,
                Preferencias.Item.PROXIMO_EPISODIO);
    }

    private void definirShows() {
        Secao secao = secao(R.drawable.st_ic_shows, R.string.st_cfg_grupo_shows, R.string.st_cfg_grupo_shows_dica);
        opcaoValor(secao, R.drawable.st_ic_shows, R.string.st_cfg_shows_por_grupo, R.string.st_cfg_shows_por_grupo_dica,
                Preferencias.Item.SHOWS_POR_GRUPO);
    }

    // ---- Versão ----

    /**
     * Versão instalada e versão disponível no GitHub, com o aviso do que isso quer dizer (D11). O botão é
     * quem recebe o foco da TV: Verificar ou, com versão nova, Baixar e instalar. Com "Atualizar
     * automaticamente" ligado, já procura ao montar a tela.
     */
    private void versao(LinearLayout lista) {
        Context contexto = lista.getContext();
        String numero = versaoInstalada(contexto);
        LinearLayout instalada = linha(lista, R.drawable.st_ic_info, R.string.st_cfg_versao_instalada, R.string.st_cfg_versao_dica);
        adicionarValor(instalada, numero);
        instalada.setContentDescription(getString(R.string.st_cfg_versao_instalada) + ", " + numero);
        instalada.setFocusable(false);
        instalada.setClickable(false);

        LinearLayout disponivel = linha(lista, R.drawable.st_ic_atualizar, R.string.st_cfg_versao_disponivel, R.string.st_cfg_versao_disponivel_dica);
        disponivel.setFocusable(false);
        disponivel.setClickable(false);
        versaoPublicada = adicionarValor(disponivel, getString(R.string.st_versao_sem_valor));
        // A dica é o segundo texto da linha ({@link #linha})
        avisoDeVersao = (TextView) ((LinearLayout) disponivel.getChildAt(1)).getChildAt(1);

        botaoDeVersao = new Button(contexto, null, 0, tv ? R.style.StarkTech_Botao_Tv_Secundario : R.style.StarkTech_Botao_Tablet_Secundario);
        botaoDeVersao.setText(R.string.st_versao_verificar);
        botaoDeVersao.setSingleLine(true);
        botaoDeVersao.setContentDescription(getString(R.string.st_versao_verificar_descricao, numero));
        Icones.noTexto(botaoDeVersao, R.drawable.st_ic_atualizar,
                getResources().getDimensionPixelSize(tv ? R.dimen.st_tv_botao_icone : R.dimen.st_tablet_botao_icone));
        botaoDeVersao.setOnClickListener(v -> {
            if (versaoParaInstalar != null) Navegacao.abrirAtualizacao(requireActivity(), versaoParaInstalar);
            else verificar();
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                getResources().getDimensionPixelSize(tv ? R.dimen.st_tv_botao_altura : R.dimen.st_tablet_botao_altura));
        lp.setMarginStart(Medidas.px(contexto, 22));
        disponivel.addView(botaoDeVersao, lp);

        if (Preferencias.procurarVersaoSozinho(contexto)) verificar();
    }

    /** Pergunta ao GitHub, fora da thread da tela; sem resposta só avisa, e o uso segue normal. */
    private void verificar() {
        if (verificando) return;
        verificando = true;
        botaoDeVersao.setText(R.string.st_versao_verificando);
        Context aplicativo = requireContext().getApplicationContext();
        Button botao = botaoDeVersao;
        new Thread(() -> {
            Atualizador.Versao publicada = null;
            try {
                publicada = Atualizador.versaoPublicada();
            } catch (IOException e) {
                // sem resposta: fica nula
            }
            Atualizador.Versao resultado = publicada;
            boolean nova = publicada != null && Atualizador.ehMaisNova(aplicativo, publicada);
            botao.post(() -> aoVerificar(resultado, nova));
        }).start();
    }

    private void aoVerificar(@Nullable Atualizador.Versao publicada, boolean nova) {
        verificando = false;
        if (!isAdded()) return;
        versaoParaInstalar = nova ? publicada : null;
        versaoPublicada.setText(publicada == null ? getString(R.string.st_versao_sem_valor) : publicada.nome);
        avisoDeVersao.setText(publicada == null ? getString(R.string.st_versao_sem_resposta)
                : nova ? getString(R.string.st_versao_nova_titulo, publicada.nome) : getString(R.string.st_versao_atualizado));
        avisoDeVersao.setTextColor(ContextCompat.getColor(requireContext(), nova ? R.color.st_azul : R.color.st_texto_apagado));
        botaoDeVersao.setText(nova ? R.string.st_versao_baixar_instalar : R.string.st_versao_verificar);
    }

    private static String versaoInstalada(Context contexto) {
        try {
            String nome = contexto.getPackageManager().getPackageInfo(contexto.getPackageName(), 0).versionName;
            return nome == null ? "" : nome;
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }

    // ---- As linhas ----

    /** Linha que abre outra tela ou faz uma ação. */
    private View acao(LinearLayout lista, @DrawableRes int icone, @StringRes int nome, @StringRes int dica, Runnable acao) {
        LinearLayout linha = linha(lista, icone, nome, dica);
        linha.setOnClickListener(v -> acao.run());
        return linha;
    }

    /**
     * Linha com o valor atual à direita. Dois valores: OK / toque troca. Mais que isso: abre o seletor.
     * A escolha já vale no vídeo aberto.
     */
    private void valor(LinearLayout lista, @DrawableRes int icone, @StringRes int nome, @StringRes int dica, Preferencias.Item item) {
        Context contexto = lista.getContext();
        LinearLayout linha = linha(lista, icone, nome, dica);
        TextView valor = adicionarValor(linha, getString(Preferencias.valor(contexto, item)));
        linha.setContentDescription(getString(nome) + ", " + valor.getText());
        if (item.valores.length <= 2) {
            linha.setOnClickListener(v -> {
                valor.setText(Preferencias.proximo(contexto, item));
                linha.setContentDescription(getString(nome) + ", " + valor.getText());
                Reproducao.aoMudarConfiguracao();
            });
            return;
        }
        String[] nomes = new String[item.valores.length];
        for (int i = 0; i < nomes.length; i++) nomes[i] = getString(item.valores[i]);
        adicionarSeta(linha);
        linha.setOnClickListener(v -> abrirSeletor(getString(nome), nomes, Preferencias.indice(contexto, item), escolhido -> {
            Preferencias.escolher(contexto, item, escolhido);
            valor.setText(nomes[escolhido]);
            linha.setContentDescription(getString(nome) + ", " + valor.getText());
            Reproducao.aoMudarConfiguracao();
        }));
    }

    /** Zoom padrão: o mesmo número do botão Zoom do player, com os mesmos nomes. */
    private void zoom(LinearLayout lista) {
        Context contexto = lista.getContext();
        String[] nomes = getResources().getStringArray(R.array.st_zoom_nomes);
        LinearLayout linha = linha(lista, R.drawable.st_ic_zoom, R.string.st_cfg_zoom, R.string.st_cfg_zoom_dica);
        TextView valor = adicionarValor(linha, nomes[Reproducao.zoomValido(Preferencias.zoomPlayer(contexto))]);
        adicionarSeta(linha);
        linha.setOnClickListener(v -> abrirSeletor(getString(R.string.st_cfg_zoom), nomes,
                Reproducao.zoomValido(Preferencias.zoomPlayer(contexto)), escolhido -> {
                    Preferencias.definirZoomPlayer(contexto, escolhido);
                    valor.setText(nomes[escolhido]);
                    linha.setContentDescription(getString(R.string.st_cfg_zoom) + ", " + valor.getText());
                    Reproducao.aoMudarConfiguracao();
                }));
        linha.setContentDescription(getString(R.string.st_cfg_zoom) + ", " + valor.getText());
    }

    /**
     * Seletor em janela, no padrão do Menu da capa: o valor atual com ✓ e, na TV, com o foco. Escolher
     * fecha a janela; Voltar ou tocar fora fecha sem mudar nada.
     */
    private void abrirSeletor(String nome, String[] nomes, int atual, Escolha escolha) {
        Resources res = getResources();
        Dialog janela = new Dialog(requireContext(), tv ? R.style.Theme_StarkTech_Menu_Tv : R.style.Theme_StarkTech_Menu);
        janela.setContentView(R.layout.st_menu_capa);
        janela.setCanceledOnTouchOutside(true);

        int padding = res.getDimensionPixelSize(tv ? R.dimen.st_tv_menu_padding : R.dimen.st_tablet_menu_padding);
        View caixa = janela.findViewById(R.id.st_menu_caixa);
        ViewGroup.LayoutParams lpCaixa = caixa.getLayoutParams();
        lpCaixa.width = res.getDimensionPixelSize(tv ? R.dimen.st_tv_menu_largura : R.dimen.st_tablet_menu_largura);
        caixa.setLayoutParams(lpCaixa);
        caixa.setPadding(padding, padding, padding, padding);

        int recuo = padding * 2 / 3;
        TextView cabecalho = janela.findViewById(R.id.st_menu_titulo);
        cabecalho.setText(nome);
        cabecalho.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(tv ? R.dimen.st_tv_texto_menu_titulo : R.dimen.st_tablet_texto_menu_titulo));
        cabecalho.setPadding(recuo, padding / 3, recuo, recuo);
        janela.findViewById(R.id.st_menu_dica).setVisibility(View.GONE);

        // A lista rola se não couber (celular deitado)
        LinearLayout opcoes = janela.findViewById(R.id.st_menu_opcoes);
        ViewGroup pai = (ViewGroup) opcoes.getParent();
        pai.removeView(opcoes);
        ScrollView rolar = new ScrollView(janela.getContext());
        rolar.setFocusable(false);
        rolar.setVerticalScrollBarEnabled(false);
        rolar.setOverScrollMode(View.OVER_SCROLL_NEVER);
        rolar.addView(opcoes, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        pai.addView(rolar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        View marcada = null;
        for (int i = 0; i < nomes.length; i++) {
            final int indice = i;
            View item = OpcaoMenu.adicionar(opcoes, i == atual ? R.drawable.st_ic_check : 0, nomes[i], () -> {
                janela.dismiss();
                escolha.em(indice);
            });
            if (i == atual) marcada = item;
        }
        janela.setOnDismissListener(d -> {
            if (seletor == janela) seletor = null;
        });
        seletor = janela;
        janela.show();
        if (tv && marcada != null) marcada.requestFocus();
    }

    /** Seta à direita: a linha abre outra coisa (subtela ou seletor). */
    private static void adicionarSeta(LinearLayout linha) {
        Context contexto = linha.getContext();
        ImageView seta = new ImageView(contexto);
        seta.setImageResource(R.drawable.st_ic_voltar);
        seta.setRotation(180f);
        ImageViewCompat.setImageTintList(seta, ContextCompat.getColorStateList(contexto, R.color.st_texto_apagado));
        seta.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Medidas.px(contexto, 28), Medidas.px(contexto, 28));
        lp.setMarginStart(Medidas.px(contexto, 22));
        linha.addView(seta, lp);
    }

    private static TextView adicionarValor(LinearLayout linha, String texto) {
        Context contexto = linha.getContext();
        TextView valor = Textos.novo(contexto, texto, R.font.st_inter_extrabold, 18, R.color.st_texto);
        valor.setSingleLine(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMarginStart(Medidas.px(contexto, 22));
        linha.addView(valor, lp);
        return valor;
    }

    /** Cartão: ícone azul, nome (Inter 800) e a dica embaixo; na TV, em foco, fundo claro e borda branca. */
    private LinearLayout linha(LinearLayout lista, @DrawableRes int icone, @StringRes int nome, @StringRes int dica) {
        Context contexto = lista.getContext();
        LinearLayout linha = new LinearLayout(contexto);
        linha.setOrientation(LinearLayout.HORIZONTAL);
        linha.setGravity(Gravity.CENTER_VERTICAL);
        linha.setBackgroundResource(tv ? R.drawable.st_linha_configuracao : R.drawable.st_fundo_cartao);
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
        TextView nomeDaLinha = Textos.novo(contexto, getString(nome), R.font.st_inter_extrabold, 23, R.color.st_texto);
        nomeDaLinha.setSingleLine(true);
        nomeDaLinha.setEllipsize(TextUtils.TruncateAt.END);
        textos.addView(nomeDaLinha);
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
