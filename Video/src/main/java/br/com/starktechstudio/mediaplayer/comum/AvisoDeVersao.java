// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;

import java.io.IOException;

import br.com.starktechstudio.mediaplayer.motor.Atualizador;

/**
 * Ao abrir o app, com "Atualizar automaticamente" ligado em Configurações, procura versão nova sozinho, fora
 * da tela; achou, abre a janela de atualização, uma vez por versão (depois, fica o botão das Configurações).
 * Sem internet ou com outra tela por cima, não avisa nada e tenta na próxima abertura. Instalar continua
 * pedindo a confirmação do Android.
 */
public final class AvisoDeVersao {

    private AvisoDeVersao() {}

    public static void procurarAoAbrir(AppCompatActivity tela) {
        Context app = tela.getApplicationContext();
        if (!Preferencias.procurarVersaoSozinho(app)) return;
        new Thread(() -> {
            Atualizador.Versao nova;
            try {
                nova = Atualizador.versaoNova(app);
            } catch (IOException | RuntimeException e) {
                return;
            }
            if (nova == null || nova.codigo <= Preferencias.versaoJaAvisada(app)) return;
            final Atualizador.Versao achada = nova;
            tela.runOnUiThread(() -> {
                if (tela.isFinishing() || !tela.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) return;
                Preferencias.definirVersaoAvisada(app, achada.codigo);
                Navegacao.abrirAtualizacao(tela, achada);
            });
        }, "StarkTechVersao").start();
    }
}
