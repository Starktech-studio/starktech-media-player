// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.debug;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.archos.medialib.IMediaMetadataRetriever;
import com.archos.medialib.MediaFactory;

import br.com.starktechstudio.mediaplayer.motor.Capitulos;

/**
 * Só no APK debug (F2, etapa 5, D8): lê os capítulos de um arquivo pelo motor (leitor de dados,
 * sem tocar o vídeo) e escreve no log, para conferir no emulador enquanto as telas são simuladas.
 * <pre>
 * adb shell am broadcast -n br.com.starktechstudio.mediaplayer/br.com.starktechstudio.mediaplayer.debug.CapitulosReceiver \
 *     --es caminho /sdcard/Android/data/br.com.starktechstudio.mediaplayer/files/teste_capitulos.mkv
 * adb logcat -s StarkTechCapitulos
 * </pre>
 */
public final class CapitulosReceiver extends BroadcastReceiver {

    private static final String TAG = "StarkTechCapitulos";

    @Override
    public void onReceive(Context context, Intent intent) {
        String caminho = intent.getStringExtra("caminho");
        if (caminho == null) {
            Log.w(TAG, "falta o extra \"caminho\"");
            return;
        }
        PendingResult pendente = goAsync();
        Context app = context.getApplicationContext();
        new Thread(() -> {
            IMediaMetadataRetriever leitor = MediaFactory.createMetadataRetriever(app);
            try {
                leitor.setDataSource(caminho);
                Capitulos capitulos = Capitulos.doLeitor(leitor);
                Log.i(TAG, "leitor " + leitor.getClass().getSimpleName() + ": " + capitulos.quantidade() + " capítulos em " + caminho);
                for (Capitulos.Capitulo c : capitulos.lista()) {
                    Log.i(TAG, c.numero + " | \"" + c.nome + "\" | " + c.inicioMs + " ms → " + c.fimMs + " ms");
                }
                long meio = 15_000;
                Log.i(TAG, "em 15 s: capítulo " + (capitulos.naPosicao(meio) == null ? "-" : capitulos.naPosicao(meio).numero)
                        + ", anterior → " + capitulos.anterior(meio) + " ms, próximo → " + capitulos.proximo(meio) + " ms");
            } catch (Exception e) {
                Log.e(TAG, "não deu para ler " + caminho, e);
            } finally {
                try {
                    leitor.release();
                } catch (Exception ignorado) {
                    // só conferência
                }
                pendente.finish();
            }
        }, "StarkTechCapitulos").start();
    }
}
