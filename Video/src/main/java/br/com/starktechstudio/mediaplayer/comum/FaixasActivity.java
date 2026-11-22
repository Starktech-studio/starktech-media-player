// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.archos.mediacenter.video.R;

import java.io.File;
import java.util.List;

import br.com.starktechstudio.mediaplayer.dados.Modelo.Faixa;
import br.com.starktechstudio.mediaplayer.dados.Modelo.TipoFaixa;
import br.com.starktechstudio.mediaplayer.dados.RepositorioBiblioteca;
import br.com.starktechstudio.mediaplayer.motor.EfeitosDeAudio;

/**
 * Áudio e legendas (D13): painel lateral próprio, à direita, com a lista de faixas e a atual
 * marcada (✓). Do player abre só Áudio ou só Legendas; do Detalhe, os dois. OK ou toque escolhe e
 * fecha. F3: com o vídeo tocando, as faixas e a escolha são as do player ({@link Reproducao});
 * no Detalhe, a escolha só fica guardada, e o player a usa ao abrir o título. Nos dois casos ela
 * vale também nas próximas vezes ({@link RepositorioBiblioteca#guardarFaixa}). Com o vídeo tocando,
 * Legendas termina em "Carregar legenda" ({@link NavegadorLegendasActivity}).
 *
 * <p>2.1: Áudio aberto do player termina em Efeitos: Equalizador (Ligado / Desligado e, ligado, os estilos do aparelho,
 * com ✓ no atual) e Volume extra (Ligado / Desligado e o puxador de 100 % a 200 %). Valem na hora, com o vídeo atrás,
 * e ficam guardados (as mesmas preferências de Configurações › Reprodução); escolher um efeito não fecha o painel. Sem
 * o efeito no aparelho, no áudio direto para o receptor ou com o que não ligou, o aviso curto embaixo de Efeitos. A
 * lista rola se não couber.
 */
public class FaixasActivity extends AppCompatActivity {

    /** Só uma lista (nome do enum {@link TipoFaixa}); sem o extra, Áudio e Legendas. */
    public static final String EXTRA_TIPO = "br.com.starktechstudio.mediaplayer.extra.TIPO_FAIXA";

    private boolean tv;
    private int recuo;
    // Efeitos (2.1): só no Áudio aberto do player
    @Nullable
    private Reproducao tocandoAgora;
    private TextView avisoDosEfeitos;
    private TextView valorEqualizador;
    private LinearLayout estilos;
    private TextView valorVolume;
    private View linhaDoNivel;
    private PuxadorNivel puxador;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        tv = Aparelho.ehTv(this);
        setTheme(tv ? R.style.Theme_StarkTech_Menu_Tv : R.style.Theme_StarkTech_Menu);
        super.onCreate(savedInstanceState);
        String id = getIntent().getStringExtra(Navegacao.EXTRA_ID);
        String tipoExtra = getIntent().getStringExtra(EXTRA_TIPO);
        TipoFaixa so = tipoExtra == null ? null : TipoFaixa.valueOf(tipoExtra);
        setContentView(R.layout.st_menu_capa);
        setFinishOnTouchOutside(true);

        // Painel à direita, centrado na altura
        getWindow().setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        WindowManager.LayoutParams janela = getWindow().getAttributes();
        janela.x = getResources().getDimensionPixelSize(tv ? R.dimen.st_tv_rep_painel_margem : R.dimen.st_tablet_rep_topo_padding_h);
        getWindow().setAttributes(janela);

        Resources res = getResources();
        int padding = res.getDimensionPixelSize(tv ? R.dimen.st_tv_menu_padding : R.dimen.st_tablet_menu_padding);
        LinearLayout caixa = findViewById(R.id.st_menu_caixa);
        ViewGroup.LayoutParams lpCaixa = caixa.getLayoutParams();
        lpCaixa.width = res.getDimensionPixelSize(tv ? R.dimen.st_tv_menu_largura : R.dimen.st_tablet_menu_largura);
        caixa.setLayoutParams(lpCaixa);
        caixa.setPadding(padding, padding, padding, padding);
        caixa.setContentDescription(getString(R.string.st_audio_legendas));
        recuo = padding * 2 / 3;

        TextView titulo = findViewById(R.id.st_menu_titulo);
        titulo.setText(so == TipoFaixa.AUDIO ? R.string.st_audio : so == TipoFaixa.LEGENDA ? R.string.st_legendas
                : R.string.st_audio_legendas);
        titulo.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(tv ? R.dimen.st_tv_texto_menu_titulo : R.dimen.st_tablet_texto_menu_titulo));
        titulo.setPadding(recuo, padding / 3, recuo, recuo);
        findViewById(R.id.st_menu_dica).setVisibility(View.GONE);

        LinearLayout opcoes = findViewById(R.id.st_menu_opcoes);
        RepositorioBiblioteca repositorio = RepositorioBiblioteca.atual(this);
        TextView primeira = null;
        for (TipoFaixa tipo : TipoFaixa.values()) {
            if (so != null && tipo != so) continue;
            if (so == null) secao(opcoes, tipo == TipoFaixa.AUDIO ? R.string.st_audio : R.string.st_legendas, recuo);
            Reproducao tocando = Reproducao.naTela(id);
            List<Faixa> faixas = tocando != null ? tocando.faixas(tipo) : repositorio.faixas(id, tipo);
            String guardada = tocando != null ? null : repositorio.faixaEscolhida(id, null, tipo);
            // Legenda em arquivo fica guardada pelo nome, que a lista do Detalhe não tem
            String escolhida = guardada != null && guardada.startsWith("arquivo:") ? null : guardada;
            for (Faixa f : faixas) {
                boolean atual = escolhida != null ? f.id.equals(escolhida) : f.atual;
                TextView item = opcao(opcoes, f.nome, atual, () -> {
                    if (tocando != null) tocando.escolherFaixa(f);
                    else repositorio.guardarFaixa(id, null, tipo, f.id);
                });
                if (primeira == null || atual && so != null) primeira = item;
            }
            if (tocando != null && tipo == TipoFaixa.LEGENDA) {
                OpcaoMenu.adicionar(opcoes, R.drawable.st_ic_legendas, getString(R.string.st_carregar_legenda), () -> {
                    startActivity(new Intent(this, NavegadorLegendasActivity.class)
                            .putExtra(Navegacao.EXTRA_ID, id)
                            .putExtra(NavegadorArquivosActivity.EXTRA_PASTA, new File(tocando.caminho).getParent()));
                    finish();
                });
            }
        }
        Reproducao tocando = Reproducao.naTela(id);
        if (tocando != null && so == TipoFaixa.AUDIO) {
            tocandoAgora = tocando;
            rolarSeNaoCouber(opcoes);
            efeitos(opcoes);
        }
        if (tv && primeira != null) primeira.requestFocus();
    }

    // ---- Efeitos (2.1) ----

    /** A lista rola se não couber (Efeitos com os estilos ligados), como o seletor de Configurações. */
    private void rolarSeNaoCouber(LinearLayout opcoes) {
        ViewGroup pai = (ViewGroup) opcoes.getParent();
        pai.removeView(opcoes);
        ScrollView rolar = new ScrollView(this);
        rolar.setFocusable(false);
        rolar.setVerticalScrollBarEnabled(false);
        rolar.setOverScrollMode(View.OVER_SCROLL_NEVER);
        rolar.addView(opcoes, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        pai.addView(rolar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
    }

    private void efeitos(LinearLayout opcoes) {
        secao(opcoes, R.string.st_rep_efeitos, recuo);
        avisoDosEfeitos = Textos.novo(this, "", R.font.st_inter_semibold, 17, R.color.st_aviso_claro);
        avisoDosEfeitos.setPadding(recuo, 0, recuo, recuo / 2);
        opcoes.addView(avisoDosEfeitos);

        boolean temEqualizador = EfeitosDeAudio.temEqualizador(this);
        valorEqualizador = OpcaoMenu.adicionarComValor(opcoes, R.drawable.st_ic_equalizador, getString(R.string.st_cfg_equalizador),
                "", () -> {
                    if (!EfeitosDeAudio.temEqualizador(this)) return;
                    Preferencias.definirEqualizador(this, !Preferencias.equalizadorLigado(this));
                    mostrarEfeitos();
                });
        estilos = new LinearLayout(this);
        estilos.setOrientation(LinearLayout.VERTICAL);
        opcoes.addView(estilos, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        if (!temEqualizador) desligarLinha((View) valorEqualizador.getParent());

        boolean temVolume = EfeitosDeAudio.temVolumeExtra(this);
        valorVolume = OpcaoMenu.adicionarComValor(opcoes, R.drawable.st_ic_volume_extra, getString(R.string.st_cfg_volume_extra),
                "", () -> {
                    if (!EfeitosDeAudio.temVolumeExtra(this)) return;
                    Preferencias.definirVolumeExtra(this, !Preferencias.volumeExtraLigado(this));
                    mostrarEfeitos();
                });
        if (!temVolume) desligarLinha((View) valorVolume.getParent());
        linhaDoNivel = linhaDoPuxador(opcoes);
        if (!temVolume) linhaDoNivel.setVisibility(View.GONE);
        mostrarEfeitos();
    }

    /** Aparelho sem o efeito: a linha fica apagada e sem ação (o aviso diz por quê). */
    private static void desligarLinha(View linha) {
        linha.setAlpha(0.4f);
        linha.setFocusable(false);
        linha.setClickable(false);
    }

    /** O puxador num item do painel: na TV, o item tem o foco e ← → mudam 10 %; no tablet, arrastar. */
    private View linhaDoPuxador(LinearLayout opcoes) {
        Resources res = getResources();
        LinearLayout linha = new LinearLayout(this);
        linha.setOrientation(LinearLayout.HORIZONTAL);
        linha.setGravity(Gravity.CENTER_VERTICAL);
        linha.setBackgroundResource(R.drawable.st_item_menu);
        int padding = res.getDimensionPixelSize(tv ? R.dimen.st_tv_menu_item_padding : R.dimen.st_tablet_menu_item_padding);
        linha.setPadding(padding, 0, padding, 0);
        puxador = new PuxadorNivel(this, Preferencias.nivelVolumeExtra(this), true, nivel -> {
            Preferencias.definirNivelVolumeExtra(this, nivel);
            linha.setContentDescription(getString(R.string.st_rep_nivel_volume_extra, nivel));
            mostrarAviso();
        });
        linha.addView(puxador.view, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        linha.setContentDescription(getString(R.string.st_rep_nivel_volume_extra, puxador.nivel()));
        if (tv) {
            linha.setFocusable(true);
            puxador.ligarTeclas(linha);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                res.getDimensionPixelSize(tv ? R.dimen.st_tv_menu_item_altura : R.dimen.st_tablet_menu_item_altura));
        lp.topMargin = res.getDimensionPixelSize(tv ? R.dimen.st_tv_menu_item_espaco : R.dimen.st_tablet_menu_item_espaco);
        opcoes.addView(linha, lp);
        return linha;
    }

    /** Valores, estilos, puxador e aviso conforme as preferências e o vídeo (depois de cada escolha). */
    private void mostrarEfeitos() {
        boolean equalizador = Preferencias.equalizadorLigado(this) && EfeitosDeAudio.temEqualizador(this);
        valorEqualizador.setText(equalizador ? R.string.st_valor_ligado : R.string.st_valor_desligado);
        ((View) valorEqualizador.getParent()).setContentDescription(getString(R.string.st_cfg_equalizador) + ", " + valorEqualizador.getText());
        mostrarEstilos(equalizador);

        boolean volume = Preferencias.volumeExtraLigado(this) && EfeitosDeAudio.temVolumeExtra(this);
        valorVolume.setText(volume ? R.string.st_valor_ligado : R.string.st_valor_desligado);
        ((View) valorVolume.getParent()).setContentDescription(getString(R.string.st_cfg_volume_extra) + ", " + valorVolume.getText());
        puxador.habilitar(volume);
        if (tv) linhaDoNivel.setFocusable(volume);
        mostrarAviso();
    }

    /**
     * Ligado: os estilos do aparelho em ordem alfabética, com ✓ no atual; escolher vale na hora, fica guardado e deixa
     * o foco no escolhido. Desligado: nenhum.
     */
    private void mostrarEstilos(boolean ligado) {
        View foco = estilos.findFocus();
        String focado = foco == null ? null : (String) foco.getTag();
        // Refazer com o foco dentro o perderia: passa pela linha do Equalizador e volta ao estilo no fim
        if (foco != null) ((View) valorEqualizador.getParent()).requestFocus();
        estilos.removeAllViews();
        if (!ligado) return;
        EfeitosDeAudio.Estilo atual = EfeitosDeAudio.estiloAtual(this);
        for (EfeitosDeAudio.Estilo estilo : EfeitosDeAudio.estilos(this)) {
            boolean marcado = atual != null && atual.original.equals(estilo.original);
            TextView item = OpcaoMenu.adicionar(estilos, marcado ? R.drawable.st_ic_check : 0, estilo.nome, () -> {
                Preferencias.definirEstiloEqualizador(this, estilo.original);
                mostrarEstilos(true);
                mostrarAviso();
            });
            item.setTag(estilo.original);
            item.setSelected(marcado);
            // Recuados sob o Equalizador: são dele
            item.setPaddingRelative(item.getPaddingStart() + recuo, 0, item.getPaddingEnd(), 0);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) item.getLayoutParams();
            lp.topMargin = getResources().getDimensionPixelSize(tv ? R.dimen.st_tv_menu_item_espaco : R.dimen.st_tablet_menu_item_espaco);
            item.setLayoutParams(lp);
        }
        if (focado != null) {
            View alvo = estilos.findViewWithTag(focado);
            if (alvo != null) alvo.requestFocus();
        }
    }

    /**
     * Um aviso curto, o mais importante: aparelho sem os efeitos; áudio direto para o receptor; vídeo sem sessão de
     * áudio (já aberto e com som); o que não ligou neste vídeo. Sem nada a dizer, some.
     */
    private void mostrarAviso() {
        Reproducao r = tocandoAgora;
        String texto = null;
        boolean temEqualizador = EfeitosDeAudio.temEqualizador(this);
        boolean temVolume = EfeitosDeAudio.temVolumeExtra(this);
        if (!temEqualizador && !temVolume) {
            texto = getString(R.string.st_equalizador_sem_suporte) + ". " + getString(R.string.st_volume_extra_sem_suporte);
        } else if (r != null) {
            EfeitosDeAudio.Situacao situacao = r.situacaoDosEfeitos();
            boolean algumLigado = Preferencias.equalizadorLigado(this) && temEqualizador
                    || Preferencias.volumeExtraLigado(this) && temVolume;
            if (situacao == EfeitosDeAudio.Situacao.AUDIO_DIRETO) texto = getString(R.string.st_efeitos_audio_direto);
            else if (situacao == EfeitosDeAudio.Situacao.SEM_SESSAO && r.temAudio() && algumLigado) {
                texto = getString(R.string.st_efeitos_sem_sessao);
            } else if (Preferencias.equalizadorLigado(this) && r.equalizadorFalhou()) {
                texto = getString(R.string.st_equalizador_falhou);
            } else if (Preferencias.volumeExtraLigado(this) && r.volumeExtraFalhou()) {
                texto = getString(R.string.st_volume_extra_falhou);
            }
        }
        if (texto == null && !temEqualizador) texto = getString(R.string.st_equalizador_sem_suporte);
        if (texto == null && !temVolume) texto = getString(R.string.st_volume_extra_sem_suporte);
        avisoDosEfeitos.setText(texto);
        avisoDosEfeitos.setVisibility(texto == null ? View.GONE : View.VISIBLE);
    }

    /** Título da lista (com Áudio e Legendas juntos). */
    private void secao(LinearLayout opcoes, @StringRes int texto, int recuo) {
        TextView secao = new TextView(this);
        secao.setText(texto);
        secao.setTypeface(ResourcesCompat.getFont(this, R.font.st_inter_black));
        secao.setAllCaps(true);
        secao.setLetterSpacing(0.15f);
        secao.setTextColor(ContextCompat.getColor(this, R.color.st_texto_apagado));
        secao.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(
                tv ? R.dimen.st_tv_texto_dados_rotulo : R.dimen.st_tablet_texto_dados_rotulo));
        secao.setPadding(recuo, opcoes.getChildCount() > 0 ? recuo : 0, recuo, recuo / 2);
        opcoes.addView(secao);
    }

    /** Uma faixa ({@link OpcaoMenu}): ✓ na atual, espaço vazio nas outras (para alinhar). Escolhe e fecha. */
    private TextView opcao(LinearLayout opcoes, String texto, boolean atual, Runnable escolher) {
        TextView item = OpcaoMenu.adicionar(opcoes, atual ? R.drawable.st_ic_check : 0, texto, () -> {
            escolher.run();
            finish();
        });
        item.setSelected(atual);
        return item;
    }
}
