// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.archos.mediacenter.video.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import br.com.starktechstudio.mediaplayer.motor.Atualizador;

/**
 * Versão nova do site (D11; vale na TV e no tablet, D12), no padrão do Menu da capa: o número, as novidades
 * e Baixar e instalar. Sem a liberação de fonte, leva à tela do Android e continua na volta; sem essa tela
 * (TV pode não ter), avisa e fica aqui. Baixa com o andamento na tela e entrega ao Android, que pede a
 * confirmação. Falha só avisa: nada é instalado e o app segue normal. O foco da TV fica sempre na primeira opção.
 */
public class AtualizacaoActivity extends AppCompatActivity {

    public static final String EXTRA_NOME = "br.com.starktechstudio.mediaplayer.extra.VERSAO_NOME";
    public static final String EXTRA_CODIGO = "br.com.starktechstudio.mediaplayer.extra.VERSAO_CODIGO";
    public static final String EXTRA_NOVIDADES = "br.com.starktechstudio.mediaplayer.extra.VERSAO_NOVIDADES";
    public static final String EXTRA_APK = "br.com.starktechstudio.mediaplayer.extra.VERSAO_APK";

    private static final String ESTADO_AGUARDANDO_FONTE = "aguardandoFonte";
    /** A janela é flutuante e sem rolagem: novidade demais não pode empurrar as opções para fora da tela. */
    private static final int MAX_LINHAS_NOVIDADES = 8;

    /** Download e cópia para o instalador, um de cada vez e fora da thread da tela. */
    private final ExecutorService trabalho = Executors.newSingleThreadExecutor();
    private final Atualizador.AoFalharInstalacao aoFalhar = this::mostrarInicio;
    /** Do trabalho em andamento; marcado, o que ele ainda devolver é ignorado. */
    private AtomicBoolean cancelamento = new AtomicBoolean();
    private Atualizador.Versao versao;
    private boolean tv;
    private LinearLayout opcoes;
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
        setContentView(R.layout.st_menu_capa);

        Resources res = getResources();
        int padding = res.getDimensionPixelSize(tv ? R.dimen.st_tv_menu_padding : R.dimen.st_tablet_menu_padding);
        LinearLayout caixa = findViewById(R.id.st_menu_caixa);
        ViewGroup.LayoutParams lpCaixa = caixa.getLayoutParams();
        lpCaixa.width = res.getDimensionPixelSize(tv ? R.dimen.st_tv_menu_largura : R.dimen.st_tablet_menu_largura);
        caixa.setLayoutParams(lpCaixa);
        caixa.setPadding(padding, padding, padding, padding);

        int recuo = padding * 2 / 3;
        float tamanhoDica = res.getDimension(tv ? R.dimen.st_tv_texto_menu_dica : R.dimen.st_tablet_texto_menu_dica);
        TextView titulo = findViewById(R.id.st_menu_titulo);
        titulo.setText(getString(R.string.st_versao_nova_titulo, versao.nome));
        titulo.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(tv ? R.dimen.st_tv_texto_menu_titulo : R.dimen.st_tablet_texto_menu_titulo));
        titulo.setPadding(recuo, padding / 3, recuo, 0);
        caixa.setContentDescription(titulo.getText());
        TextView dica = findViewById(R.id.st_menu_dica);
        dica.setText(textoDasNovidades());
        dica.setSingleLine(false);
        dica.setMaxLines(MAX_LINHAS_NOVIDADES);
        dica.setEllipsize(TextUtils.TruncateAt.END);
        dica.setTextSize(TypedValue.COMPLEX_UNIT_PX, tamanhoDica);
        dica.setPadding(recuo, 0, recuo, recuo);

        opcoes = findViewById(R.id.st_menu_opcoes);
        int posicao = caixa.indexOfChild(opcoes);
        estado = Textos.novo(this, "", R.font.st_inter_semibold, 17, R.color.st_texto);
        estado.setTextSize(TypedValue.COMPLEX_UNIT_PX, tamanhoDica);
        estado.setPadding(recuo, 0, recuo, recuo);
        estado.setVisibility(View.GONE);
        caixa.addView(estado, posicao, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        barra = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        barra.setMax(100);
        barra.setProgressTintList(ContextCompat.getColorStateList(this, R.color.st_azul));
        barra.setIndeterminateTintList(ContextCompat.getColorStateList(this, R.color.st_azul));
        barra.setVisibility(View.GONE);
        LinearLayout.LayoutParams lpBarra = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpBarra.setMarginStart(recuo);
        lpBarra.setMarginEnd(recuo);
        lpBarra.bottomMargin = recuo;
        caixa.addView(barra, posicao + 1, lpBarra);

        Atualizador.ouvirInstalacao(aoFalhar);
        mostrarInicio(0);
    }

    private CharSequence textoDasNovidades() {
        if (versao.novidades.isEmpty()) return getString(R.string.st_versao_sem_novidades);
        StringBuilder texto = new StringBuilder();
        for (String novidade : versao.novidades) {
            if (texto.length() > 0) texto.append('\n');
            texto.append(getString(R.string.st_versao_novidade, novidade));
        }
        return texto;
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

    /** Em repouso, com um aviso ou sem ({@code aviso} 0): Baixar e instalar, com o foco da TV, e Fechar. */
    private void mostrarInicio(@StringRes int aviso) {
        mostrarEstado(aviso == 0 ? null : getString(aviso));
        fimDoTrabalho();
        opcoes.removeAllViews();
        View baixar = OpcaoMenu.adicionar(opcoes, R.drawable.st_ic_atualizar, getString(R.string.st_versao_baixar_instalar), this::baixarEInstalar);
        OpcaoMenu.adicionar(opcoes, R.drawable.st_ic_fechar, getString(R.string.st_fechar), this::finish);
        if (tv) baixar.requestFocus();
    }

    /** Trabalho em andamento: o texto, a barra e uma opção só; toque fora não fecha. */
    private void mostrarTrabalho(@StringRes int texto, @DrawableRes int icone, @StringRes int opcao, Runnable acao) {
        mostrarEstado(getString(texto));
        barra.setIndeterminate(true);
        barra.setVisibility(View.VISIBLE);
        setFinishOnTouchOutside(false);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        umaOpcao(icone, opcao, acao);
    }

    private void fimDoTrabalho() {
        barra.setVisibility(View.GONE);
        setFinishOnTouchOutside(true);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void umaOpcao(@DrawableRes int icone, @StringRes int nome, Runnable acao) {
        opcoes.removeAllViews();
        View opcao = OpcaoMenu.adicionar(opcoes, icone, getString(nome), acao);
        if (tv) opcao.requestFocus();
    }

    private void mostrarEstado(@Nullable CharSequence texto) {
        estado.setText(texto);
        estado.setVisibility(texto == null ? View.GONE : View.VISIBLE);
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
        mostrarEstado(getString(R.string.st_versao_confirmar));
        fimDoTrabalho();
        umaOpcao(R.drawable.st_ic_fechar, R.string.st_fechar, this::finish);
    }
}
