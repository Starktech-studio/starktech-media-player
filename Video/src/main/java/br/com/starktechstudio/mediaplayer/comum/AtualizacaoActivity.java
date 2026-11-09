// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import com.archos.mediacenter.video.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import br.com.starktechstudio.mediaplayer.motor.Atualizador;

/**
 * Versão nova publicada (D11; vale na TV e no tablet, D12), numa janela própria, montada no código com as medidas
 * do desenho da TV ({@link Medidas}): no alto, o ícone do app, "Atualização disponível", "Versão X" e os selos da
 * versão instalada e da nova; embaixo de um traço, as novidades, uma por linha com ✓; o aviso de que instala por
 * cima sem perder os dados; o andamento (texto e barra) quando há trabalho; e os botões Agora não e Baixar e
 * instalar. Sem a liberação de fonte, leva à tela do Android e continua na volta; sem essa tela (TV pode não ter),
 * avisa e fica aqui. Baixa com o andamento na tela e entrega ao Android, que pede a confirmação. Falha só avisa:
 * nada é instalado e o app segue normal. O foco da TV fica sempre no botão principal. Tudo rola se não couber.
 */
public class AtualizacaoActivity extends AppCompatActivity {

    public static final String EXTRA_NOME = "br.com.starktechstudio.mediaplayer.extra.VERSAO_NOME";
    public static final String EXTRA_CODIGO = "br.com.starktechstudio.mediaplayer.extra.VERSAO_CODIGO";
    public static final String EXTRA_NOVIDADES = "br.com.starktechstudio.mediaplayer.extra.VERSAO_NOVIDADES";
    public static final String EXTRA_APK = "br.com.starktechstudio.mediaplayer.extra.VERSAO_APK";

    private static final String ESTADO_AGUARDANDO_FONTE = "aguardandoFonte";
    /** Novidades mostradas (o resto é cortado, como diz o build.md). */
    private static final int MAX_NOVIDADES = 8;
    /** Largura da janela, em px do desenho da TV (TV 520 dp, tablet 728 dp); nunca passa da tela. */
    private static final int LARGURA = 1040;
    private static final int RECUO = 56;

    /** Download e cópia para o instalador, um de cada vez e fora da thread da tela. */
    private final ExecutorService trabalho = Executors.newSingleThreadExecutor();
    private final Atualizador.AoFalharInstalacao aoFalhar = this::mostrarInicio;
    /** Do trabalho em andamento; marcado, o que ele ainda devolver é ignorado. */
    private AtomicBoolean cancelamento = new AtomicBoolean();
    private Atualizador.Versao versao;
    private boolean tv;
    private LinearLayout botoes;
    private View blocoEstado;
    private TextView estado;
    private ProgressBar barra;
    /** Foi à tela de liberação e ainda não voltou. */
    private boolean aguardandoFonte;
    /** O download terminou com o app em segundo plano: a confirmação do Android só abre com o app na frente. */
    private boolean instalarAoVoltar;
    private boolean naFrente;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        tv = Aparelho.ehTv(this);
        setTheme(tv ? R.style.Theme_StarkTech_Menu_Tv : R.style.Theme_StarkTech_Menu);
        super.onCreate(savedInstanceState);
        String nome = getIntent().getStringExtra(EXTRA_NOME);
        String apk = getIntent().getStringExtra(EXTRA_APK);
        long codigo = getIntent().getLongExtra(EXTRA_CODIGO, 0);
        if (nome == null || apk == null || codigo <= 0) {
            finish();
            return;
        }
        List<String> novidades = getIntent().getStringArrayListExtra(EXTRA_NOVIDADES);
        versao = new Atualizador.Versao(nome, codigo, novidades == null ? new ArrayList<>() : novidades, apk);
        aguardandoFonte = savedInstanceState != null && savedInstanceState.getBoolean(ESTADO_AGUARDANDO_FONTE);

        LinearLayout caixa = new LinearLayout(this);
        caixa.setOrientation(LinearLayout.VERTICAL);
        caixa.setBackgroundResource(R.drawable.st_fundo_menu);
        int recuo = px(RECUO);
        caixa.setPadding(recuo, recuo, recuo, recuo);
        caixa.setContentDescription(getString(R.string.st_versao_nova_titulo, versao.nome));
        caixa.addView(cabecalho());
        caixa.addView(traco(), margens(ViewGroup.LayoutParams.MATCH_PARENT, px(2), 40));
        caixa.addView(Textos.rotulo(this, getString(R.string.st_versao_novidades_rotulo), 20));
        caixa.addView(listaDeNovidades(), margens(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 22));
        caixa.addView(rodape(), margens(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 40));
        caixa.addView(andamento(), margens(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 32));
        botoes = new LinearLayout(this);
        botoes.setOrientation(LinearLayout.HORIZONTAL);
        botoes.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        caixa.addView(botoes, margens(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 40));

        // Tela pequena (celular deitado): a janela rola, com os botões no fim
        ScrollView rolagem = new ScrollView(this);
        rolagem.setFocusable(false);
        rolagem.setVerticalScrollBarEnabled(false);
        rolagem.setOverScrollMode(View.OVER_SCROLL_NEVER);
        rolagem.addView(caixa, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        int largura = Math.min(px(LARGURA), getResources().getDisplayMetrics().widthPixels - px(80));
        setContentView(rolagem, new ViewGroup.LayoutParams(largura, ViewGroup.LayoutParams.WRAP_CONTENT));

        Atualizador.ouvirInstalacao(aoFalhar);
        mostrarInicio(0);
    }

    // ---- As partes da janela ----

    /** Ícone do app; "Atualização disponível" em amarelo, "Versão X" e os selos Instalada / Nova. */
    private View cabecalho() {
        LinearLayout linha = new LinearLayout(this);
        linha.setOrientation(LinearLayout.HORIZONTAL);
        linha.setGravity(Gravity.CENTER_VERTICAL);
        ImageView icone = new ImageView(this);
        icone.setImageResource(R.drawable.st_icone_app);
        icone.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        linha.addView(icone, new LinearLayout.LayoutParams(px(112), px(112)));

        LinearLayout textos = new LinearLayout(this);
        textos.setOrientation(LinearLayout.VERTICAL);
        TextView rotulo = Textos.rotulo(this, getString(R.string.st_versao_rotulo), 20);
        rotulo.setTextColor(ContextCompat.getColor(this, R.color.st_amarelo_marca));
        textos.addView(rotulo);
        TextView titulo = Textos.novo(this, getString(R.string.st_versao_titulo, versao.nome), R.font.st_inter_black, 50, R.color.st_texto);
        titulo.setLetterSpacing(-0.02f);
        titulo.setSingleLine(true);
        titulo.setEllipsize(TextUtils.TruncateAt.END);
        textos.addView(titulo, margens(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 8));

        LinearLayout selos = new LinearLayout(this);
        selos.setOrientation(LinearLayout.HORIZONTAL);
        selos.setGravity(Gravity.CENTER_VERTICAL);
        String instalada = versaoInstalada();
        if (!instalada.isEmpty()) {
            selos.addView(selo(getString(R.string.st_versao_selo_instalada, instalada), R.drawable.st_selo_versao, R.color.st_texto_secundario));
            TextView seta = Textos.novo(this, "→", R.font.st_inter_bold, 22, R.color.st_texto_apagado);
            seta.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            LinearLayout.LayoutParams lpSeta = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lpSeta.setMarginStart(px(14));
            lpSeta.setMarginEnd(px(14));
            selos.addView(seta, lpSeta);
        }
        selos.addView(selo(getString(R.string.st_versao_selo_nova, versao.nome), R.drawable.st_selo_versao_nova, R.color.st_azul));
        textos.addView(selos, margens(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 16));

        LinearLayout.LayoutParams lpTextos = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lpTextos.setMarginStart(px(32));
        linha.addView(textos, lpTextos);
        return linha;
    }

    private TextView selo(String texto, @DrawableRes int fundo, @ColorRes int cor) {
        TextView selo = Textos.novo(this, texto, R.font.st_inter_extrabold, 20, cor);
        selo.setSingleLine(true);
        selo.setBackgroundResource(fundo);
        selo.setPadding(px(18), px(7), px(18), px(7));
        return selo;
    }

    private View traco() {
        View traco = new View(this);
        traco.setBackgroundResource(R.color.st_borda);
        traco.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return traco;
    }

    /** Uma novidade por linha, com o ✓ azul; sem novidades, o aviso. */
    private View listaDeNovidades() {
        LinearLayout lista = new LinearLayout(this);
        lista.setOrientation(LinearLayout.VERTICAL);
        if (versao.novidades.isEmpty()) {
            lista.addView(Textos.novo(this, getString(R.string.st_versao_sem_novidades), R.font.st_inter_semibold, 26, R.color.st_texto_apagado));
            return lista;
        }
        int quantas = Math.min(MAX_NOVIDADES, versao.novidades.size());
        for (int i = 0; i < quantas; i++) {
            LinearLayout linha = new LinearLayout(this);
            linha.setOrientation(LinearLayout.HORIZONTAL);
            linha.setGravity(Gravity.TOP);
            ImageView marca = new ImageView(this);
            marca.setImageResource(R.drawable.st_ic_check);
            ImageViewCompat.setImageTintList(marca, ContextCompat.getColorStateList(this, R.color.st_azul));
            marca.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            LinearLayout.LayoutParams lpMarca = new LinearLayout.LayoutParams(px(28), px(28));
            lpMarca.topMargin = px(2);
            linha.addView(marca, lpMarca);
            TextView texto = Textos.novo(this, versao.novidades.get(i), R.font.st_inter_semibold, 27, R.color.st_texto_secundario);
            texto.setLineSpacing(0f, 1.2f);
            LinearLayout.LayoutParams lpTexto = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lpTexto.setMarginStart(px(20));
            linha.addView(texto, lpTexto);
            lista.addView(linha, margens(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, i == 0 ? 0 : 18));
        }
        return lista;
    }

    /** "Instala por cima… sem perder os seus dados", com o ícone de informação. */
    private View rodape() {
        LinearLayout linha = new LinearLayout(this);
        linha.setOrientation(LinearLayout.HORIZONTAL);
        linha.setGravity(Gravity.CENTER_VERTICAL);
        linha.setBackgroundResource(R.drawable.st_fundo_cartao);
        linha.setPadding(px(24), px(18), px(24), px(18));
        ImageView info = new ImageView(this);
        info.setImageResource(R.drawable.st_ic_info);
        ImageViewCompat.setImageTintList(info, ContextCompat.getColorStateList(this, R.color.st_texto_apagado));
        info.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        linha.addView(info, new LinearLayout.LayoutParams(px(26), px(26)));
        TextView texto = Textos.novo(this, getString(R.string.st_versao_rodape), R.font.st_inter_semibold, 21, R.color.st_texto_apagado);
        texto.setLineSpacing(0f, 1.15f);
        LinearLayout.LayoutParams lpTexto = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lpTexto.setMarginStart(px(16));
        linha.addView(texto, lpTexto);
        return linha;
    }

    /** O que está acontecendo (ou o aviso de falha) e a barra; escondido em repouso. */
    private View andamento() {
        LinearLayout bloco = new LinearLayout(this);
        bloco.setOrientation(LinearLayout.VERTICAL);
        estado = Textos.novo(this, "", R.font.st_inter_bold, 24, R.color.st_texto);
        estado.setLineSpacing(0f, 1.15f);
        bloco.addView(estado);
        barra = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        barra.setMax(100);
        barra.setProgressDrawable(ContextCompat.getDrawable(this, R.drawable.st_barra_atualizacao));
        barra.setIndeterminateTintList(ContextCompat.getColorStateList(this, R.color.st_azul));
        barra.setVisibility(View.GONE);
        bloco.addView(barra, margens(ViewGroup.LayoutParams.MATCH_PARENT, px(12), 16));
        bloco.setVisibility(View.GONE);
        blocoEstado = bloco;
        return bloco;
    }

    private Button botao(@StringRes int texto, @DrawableRes int icone, boolean principal, Runnable acao) {
        int estilo = tv ? (principal ? R.style.StarkTech_Botao_Tv_Primario : R.style.StarkTech_Botao_Tv_Secundario)
                : (principal ? R.style.StarkTech_Botao_Tablet_Primario : R.style.StarkTech_Botao_Tablet_Secundario);
        Button botao = new Button(this, null, 0, estilo);
        botao.setText(texto);
        botao.setSingleLine(true);
        Icones.noTexto(botao, icone, getResources().getDimensionPixelSize(tv ? R.dimen.st_tv_botao_icone : R.dimen.st_tablet_botao_icone));
        botao.setOnClickListener(v -> acao.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                getResources().getDimensionPixelSize(tv ? R.dimen.st_tv_botao_altura : R.dimen.st_tablet_botao_altura));
        if (botoes.getChildCount() > 0) lp.setMarginStart(px(18));
        botoes.addView(botao, lp);
        return botao;
    }

    private String versaoInstalada() {
        try {
            String nome = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            return nome == null ? "" : nome;
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }

    private int px(float pxHtml) {
        return Medidas.px(this, pxHtml);
    }

    private LinearLayout.LayoutParams margens(int largura, int altura, float topoHtml) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(largura, altura);
        lp.topMargin = px(topoHtml);
        return lp;
    }

    @Override
    protected void onResume() {
        super.onResume();
        naFrente = true;
        if (aguardandoFonte) {
            aguardandoFonte = false;
            if (Atualizador.fonteLiberada(this)) baixar();
            else mostrarInicio(R.string.st_versao_fonte_nao_liberada);
        } else if (instalarAoVoltar) {
            instalarAoVoltar = false;
            instalar();
        }
    }

    @Override
    protected void onPause() {
        naFrente = false;
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Liberar a fonte pode encerrar o app em alguns aparelhos: na volta, continua daqui
        outState.putBoolean(ESTADO_AGUARDANDO_FONTE, aguardandoFonte);
    }

    @Override
    protected void onDestroy() {
        cancelamento.set(true);
        trabalho.shutdown();
        Atualizador.deixarDeOuvirInstalacao(aoFalhar);
        if (isFinishing()) Atualizador.apagarApk(this);
        super.onDestroy();
    }

    // ---- Estados da tela ----

    /** Em repouso, com um aviso em amarelo ou sem ({@code aviso} 0): Agora não e Baixar e instalar, com o foco da TV. */
    private void mostrarInicio(@StringRes int aviso) {
        mostrarEstado(aviso == 0 ? null : getString(aviso), R.color.st_aviso_claro);
        fimDoTrabalho();
        botoes.removeAllViews();
        botao(R.string.st_versao_agora_nao, R.drawable.st_ic_fechar, false, this::finish);
        View baixar = botao(R.string.st_versao_baixar_instalar, R.drawable.st_ic_atualizar, true, this::baixarEInstalar);
        if (tv) baixar.requestFocus();
    }

    /** Trabalho em andamento: o texto, a barra e um botão só; toque fora não fecha. */
    private void mostrarTrabalho(@StringRes int texto, @DrawableRes int icone, @StringRes int opcao, Runnable acao) {
        mostrarEstado(getString(texto), R.color.st_texto);
        barra.setIndeterminate(true);
        barra.setVisibility(View.VISIBLE);
        setFinishOnTouchOutside(false);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        umBotao(icone, opcao, acao);
    }

    private void fimDoTrabalho() {
        barra.setVisibility(View.GONE);
        setFinishOnTouchOutside(true);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void umBotao(@DrawableRes int icone, @StringRes int nome, Runnable acao) {
        botoes.removeAllViews();
        View botao = botao(nome, icone, false, acao);
        if (tv) botao.requestFocus();
    }

    private void mostrarEstado(@Nullable CharSequence texto, @ColorRes int cor) {
        estado.setText(texto);
        estado.setTextColor(ContextCompat.getColor(this, cor));
        blocoEstado.setVisibility(texto == null ? View.GONE : View.VISIBLE);
    }

    private void mostrarPercentual(int percentual) {
        barra.setIndeterminate(percentual < 0);
        if (percentual >= 0) {
            barra.setProgress(percentual);
            estado.setText(getString(R.string.st_versao_baixando_percentual, percentual));
        }
    }

    // ---- O caminho até a confirmação do Android ----

    private void baixarEInstalar() {
        if (Atualizador.fonteLiberada(this)) {
            baixar();
            return;
        }
        aguardandoFonte = true;
        try {
            startActivity(Atualizador.telaDeLiberacao(this));
        } catch (ActivityNotFoundException | SecurityException e) {
            aguardandoFonte = false;
            mostrarInicio(R.string.st_versao_sem_tela_liberacao);
        }
    }

    private void baixar() {
        AtomicBoolean este = new AtomicBoolean();
        cancelamento = este;
        mostrarTrabalho(R.string.st_versao_baixando, R.drawable.st_ic_voltar, R.string.st_cancelar, this::cancelar);
        Context aplicativo = getApplicationContext();
        trabalho.execute(() -> {
            Atualizador.Download fim = Atualizador.baixar(aplicativo, versao, percentual -> runOnUiThread(() -> {
                if (!este.get()) mostrarPercentual(percentual);
            }), este);
            runOnUiThread(() -> {
                if (!este.get()) aoBaixar(fim);
            });
        });
    }

    private void cancelar() {
        cancelamento.set(true);
        mostrarInicio(0);
    }

    private void aoBaixar(Atualizador.Download fim) {
        switch (fim) {
            case PRONTO:
                if (naFrente) instalar();
                else instalarAoVoltar = true;
                break;
            case INVALIDO:
                mostrarInicio(R.string.st_versao_apk_invalido);
                break;
            case FALHOU:
                mostrarInicio(R.string.st_versao_falha_baixar);
                break;
            default:
                mostrarInicio(0);
                break;
        }
    }

    private void instalar() {
        AtomicBoolean este = cancelamento;
        mostrarTrabalho(R.string.st_versao_preparando, R.drawable.st_ic_fechar, R.string.st_fechar, this::finish);
        Context aplicativo = getApplicationContext();
        trabalho.execute(() -> {
            boolean entregue = Atualizador.instalar(aplicativo, este);
            runOnUiThread(() -> {
                if (!este.get()) aoEntregar(entregue);
            });
        });
    }

    /** Entregue, o Android assume; recusar ou falhar volta por {@link #aoFalhar}, com Baixar e instalar de novo. */
    private void aoEntregar(boolean entregue) {
        if (!entregue) {
            mostrarInicio(R.string.st_versao_falha_instalar);
            return;
        }
        mostrarEstado(getString(R.string.st_versao_confirmar), R.color.st_texto);
        fimDoTrabalho();
        umBotao(R.drawable.st_ic_fechar, R.string.st_fechar, this::finish);
    }
}
