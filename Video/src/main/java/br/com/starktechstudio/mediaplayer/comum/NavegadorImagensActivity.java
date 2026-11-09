// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.archos.mediacenter.video.R;

import java.io.File;
import java.util.List;

import br.com.starktechstudio.mediaplayer.motor.ArquivosDoAparelho;

/**
 * Escolher imagem ("Do HD" na TV, "Do aparelho" no tablet): só as imagens (JPG, PNG, WebP), a partir
 * da pasta de imagens das Configurações ({@link #EXTRA_PASTA}). OK numa imagem devolve o caminho em
 * {@link #EXTRA_CAMINHO}. Pede a permissão de ler imagens se ainda não tiver.
 */
public class NavegadorImagensActivity extends NavegadorArquivosActivity {

    public static final String EXTRA_CAMINHO = "caminho";

    private static final int PEDIDO_PERMISSAO = 1;

    @Override
    protected void aoAbrir() {
        String permissao = permissaoDeImagens();
        if (ContextCompat.checkSelfPermission(this, permissao) == PackageManager.PERMISSION_GRANTED) {
            mostrarInicio();
        } else {
            requestPermissions(new String[]{permissao}, PEDIDO_PERMISSAO);
        }
    }

    @Override
    public void onRequestPermissionsResult(int pedido, @NonNull String[] permissoes, @NonNull int[] respostas) {
        super.onRequestPermissionsResult(pedido, permissoes, respostas);
        if (pedido != PEDIDO_PERMISSAO) return;
        if (respostas.length > 0 && respostas[0] == PackageManager.PERMISSION_GRANTED) {
            mostrarInicio();
        } else {
            Toast.makeText(this, R.string.st_imagem_sem_permissao, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private static String permissaoDeImagens() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;
    }

    @Override
    protected int textoTitulo() {
        return R.string.st_escolher_imagem;
    }

    @Override
    protected int textoDica() {
        return R.string.st_escolher_imagem_dica;
    }

    @Override
    protected int textoVazio() {
        return R.string.st_nenhuma_imagem;
    }

    @Override
    protected int iconeDoArquivo() {
        return R.drawable.st_ic_imagem;
    }

    @Override
    protected List<File> listar(File pasta) {
        return ArquivosDoAparelho.listar(pasta, true);
    }

    @Override
    protected void aoEscolher(File arquivo) {
        setResult(RESULT_OK, new Intent().putExtra(EXTRA_CAMINHO, arquivo.getAbsolutePath()));
        finish();
    }
}
