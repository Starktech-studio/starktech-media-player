// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Toast;

import com.archos.mediacenter.video.R;

/**
 * Acesso a todos os arquivos: do Android 11 em diante, sem ele o sistema só mostra ao app os vídeos e
 * as imagens, e a legenda em arquivo (.srt, .ass) fica invisível, para o app e para o motor. Só se
 * libera numa tela do sistema, que algumas TVs não têm: ali, só por adb
 * ({@code docs/operacional/build.md} §3).
 */
public final class AcessoTodosArquivos {

    private AcessoTodosArquivos() {}

    /** Até o Android 10, a permissão de ler o armazenamento já mostra tudo. */
    public static boolean tem() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager();
    }

    /**
     * Abre a tela do sistema; {@code pedido} volta no {@code onActivityResult} da tela (-1 = sem
     * resposta). False se o aparelho não tem essa tela.
     */
    public static boolean abrirTela(Activity tela, int pedido) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false;
        Intent[] telas = {
                new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + tela.getPackageName())),
                new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
        };
        for (Intent intent : telas) {
            try {
                tela.startActivityForResult(intent, pedido);
                return true;
            } catch (ActivityNotFoundException | SecurityException e) {
                // tenta a próxima
            }
        }
        return false;
    }

    /** Ao abrir o app, uma vez só; depois, "Carregar legenda" pede de novo. */
    static void pedirUmaVez(Activity tela) {
        if (tem() || !Preferencias.primeiroPedidoDeAcesso(tela)) return;
        if (abrirTela(tela, -1)) Toast.makeText(tela, R.string.st_pedir_acesso_arquivos, Toast.LENGTH_LONG).show();
    }
}
