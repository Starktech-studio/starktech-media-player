// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.app.Activity;
import android.content.pm.PackageManager;

import br.com.starktechstudio.mediaplayer.dados.RepositorioBiblioteca;
import br.com.starktechstudio.mediaplayer.motor.ArquivosDoAparelho;

/**
 * Permissão para ler os vídeos do aparelho (F3): pedida ao abrir o app e ao adicionar uma pasta.
 * Dada, a biblioteca é varrida na hora; negada, as telas mostram o aviso no lugar dos títulos.
 */
public final class PermissaoVideos {

    public static final int PEDIDO = 7301;

    private PermissaoVideos() {}

    public static boolean tem(Activity tela) {
        return ArquivosDoAparelho.podeLerVideos(tela);
    }

    /** Pede se ainda não tem; devolve true se já tinha. */
    public static boolean pedirSeFaltar(Activity tela) {
        if (tem(tela)) return true;
        tela.requestPermissions(new String[]{ArquivosDoAparelho.permissaoDeVideos()}, PEDIDO);
        return false;
    }

    /** Ao abrir o app: a de vídeos e, com ela dada, o acesso a todos os arquivos (legenda em arquivo). */
    public static void pedirAoAbrirOApp(Activity tela) {
        if (pedirSeFaltar(tela)) AcessoTodosArquivos.pedirUmaVez(tela);
    }

    /** Para o {@code onRequestPermissionsResult} da tela; devolve true se a permissão foi dada. */
    public static boolean aoResponder(Activity tela, int pedido, int[] respostas) {
        if (pedido != PEDIDO) return false;
        boolean deu = respostas.length > 0 && respostas[0] == PackageManager.PERMISSION_GRANTED;
        if (deu) {
            RepositorioBiblioteca.atual(tela).atualizarBiblioteca();
            AcessoTodosArquivos.pedirUmaVez(tela);
        }
        return deu;
    }
}
