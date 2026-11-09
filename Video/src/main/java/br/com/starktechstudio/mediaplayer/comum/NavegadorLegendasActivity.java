// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.archos.mediacenter.video.R;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import br.com.starktechstudio.mediaplayer.motor.ArquivosDoAparelho;
import br.com.starktechstudio.mediaplayer.motor.LegendasNoIndice;

/**
 * "Carregar legenda", no player: só as legendas (.srt, .ass, .ssa, .vtt), a partir da pasta do vídeo
 * ({@link #EXTRA_PASTA}). OK numa legenda entrega o arquivo à {@link Reproducao} na tela
 * ({@link Navegacao#EXTRA_ID}). Sem o acesso a todos os arquivos o Android esconde as legendas da pasta: pede o
 * acesso antes; recusado, ou no aparelho sem a tela desse acesso, as legendas vêm do índice de mídia
 * ({@link LegendasNoIndice}), que o Android mostra a quem lê vídeos.
 */
public class NavegadorLegendasActivity extends NavegadorArquivosActivity {

    private static final int PEDIDO_ACESSO = 1;

    @Override
    protected void aoAbrir() {
        if (AcessoTodosArquivos.tem()) {
            mostrarInicio();
        } else if (!AcessoTodosArquivos.abrirTela(this, PEDIDO_ACESSO)) {
            mostrarPeloIndice();
        }
    }

    @Override
    protected void onActivityResult(int pedido, int resultado, @Nullable Intent dados) {
        super.onActivityResult(pedido, resultado, dados);
        if (pedido != PEDIDO_ACESSO) return;
        if (AcessoTodosArquivos.tem()) mostrarInicio();
        else mostrarPeloIndice();
    }

    private void mostrarPeloIndice() {
        Toast.makeText(this, R.string.st_legenda_pelo_indice, Toast.LENGTH_LONG).show();
        mostrarInicio();
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

    /** As subpastas e as legendas da pasta; sem o acesso, as legendas que só o índice mostra entram também. */
    @Override
    protected List<File> listar(File pasta) {
        List<File> lista = ArquivosDoAparelho.listarLegendas(pasta);
        if (!LegendasNoIndice.precisa()) return lista;
        List<File> pastas = new ArrayList<>();
        List<File> legendas = new ArrayList<>();
        Set<String> vistas = new HashSet<>();
        for (File item : lista) {
            if (item.isDirectory()) pastas.add(item);
            else if (vistas.add(item.getAbsolutePath())) legendas.add(item);
        }
        for (String caminho : LegendasNoIndice.naPasta(this, pasta).keySet()) {
            if (vistas.add(caminho)) legendas.add(new File(caminho));
        }
        Collator ordem = Collator.getInstance(Locale.forLanguageTag("pt-BR"));
        ordem.setStrength(Collator.PRIMARY);
        Collections.sort(legendas, (a, b) -> ordem.compare(a.getName(), b.getName()));
        pastas.addAll(legendas);
        return pastas;
    }

    @Override
    protected void aoEscolher(File arquivo) {
        Reproducao tocando = Reproducao.naTela(getIntent().getStringExtra(Navegacao.EXTRA_ID));
        if (tocando != null) tocando.carregarLegenda(arquivo);
        finish();
    }
}
