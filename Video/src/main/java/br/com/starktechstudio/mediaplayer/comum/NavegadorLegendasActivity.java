// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.archos.mediacenter.video.R;

import java.io.File;
import java.util.List;

import br.com.starktechstudio.mediaplayer.motor.ArquivosDoAparelho;

/**
 * "Carregar legenda", no player: só as legendas (.srt, .ass, .ssa, .vtt), a partir da pasta do vídeo
 * ({@link #EXTRA_PASTA}). OK numa legenda entrega o arquivo à {@link Reproducao} na tela
 * ({@link Navegacao#EXTRA_ID}). Sem o acesso a todos os arquivos o Android esconde as legendas: pede
 * antes de mostrar; aparelho sem a tela desse acesso avisa e fecha.
 */
public class NavegadorLegendasActivity extends NavegadorArquivosActivity {

    private static final int PEDIDO_ACESSO = 1;

    @Override
    protected void aoAbrir() {
        if (AcessoTodosArquivos.tem()) {
            mostrarInicio();
        } else if (!AcessoTodosArquivos.abrirTela(this, PEDIDO_ACESSO)) {
            avisarEFechar(R.string.st_legenda_sem_tela_de_acesso);
        }
    }

    @Override
    protected void onActivityResult(int pedido, int resultado, @Nullable Intent dados) {
        super.onActivityResult(pedido, resultado, dados);
        if (pedido != PEDIDO_ACESSO) return;
        if (AcessoTodosArquivos.tem()) mostrarInicio();
        else avisarEFechar(R.string.st_legenda_sem_acesso);
    }

    private void avisarEFechar(int texto) {
        Toast.makeText(this, texto, Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    protected int textoTitulo() {
        return R.string.st_escolher_legenda;
    }

    @Override
    protected int textoDica() {
        return R.string.st_escolher_legenda_dica;
    }

    @Override
    protected int textoVazio() {
        return R.string.st_nenhuma_legenda;
    }

    @Override
    protected int iconeDoArquivo() {
        return R.drawable.st_ic_legendas;
    }

    @Override
    protected List<File> listar(File pasta) {
        return ArquivosDoAparelho.listarLegendas(pasta);
    }

    @Override
    protected void aoEscolher(File arquivo) {
        Reproducao tocando = Reproducao.naTela(getIntent().getStringExtra(Navegacao.EXTRA_ID));
        if (tocando != null) tocando.carregarLegenda(arquivo);
        finish();
    }
}
