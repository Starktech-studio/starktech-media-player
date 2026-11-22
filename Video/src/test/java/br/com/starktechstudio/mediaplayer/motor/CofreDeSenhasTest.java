// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

/**
 * Cofre da senha (2.0, D5 = B) com uma chave AES em memória no lugar do Keystore: a senha volta igual, não fica
 * em texto aberto e, sem a chave ou com outra, o cofre devolve nulo (o servidor pede a senha de novo).
 */
public class CofreDeSenhasTest {

    private static SecretKey novaChave() throws Exception {
        KeyGenerator gerador = KeyGenerator.getInstance("AES");
        gerador.init(256);
        return gerador.generateKey();
    }

    @Test
    public void cifraEDecifra() throws Exception {
        SecretKey chave = novaChave();
        CofreDeSenhas cofre = new CofreDeSenhas(criar -> chave);
        byte[] guardada = cofre.cifrar("senha-de-teste ç");
        assertNotNull(guardada);
        assertFalse("não fica em texto aberto",
                new String(guardada, StandardCharsets.ISO_8859_1).contains("senha-de-teste"));
        assertEquals("senha-de-teste ç", cofre.decifrar(guardada));
    }

    @Test
    public void semChave_pedeASenhaDeNovo() throws Exception {
        SecretKey chave = novaChave();
        byte[] guardada = new CofreDeSenhas(criar -> chave).cifrar("senha");
        // Backup levado a outro aparelho: a chave do Keystore não veio junto
        CofreDeSenhas semChave = new CofreDeSenhas(criar -> null);
        assertNull(semChave.decifrar(guardada));
        assertNull("sem chave, nada é guardado", semChave.cifrar("senha"));
    }

    @Test
    public void outraChave_ouDadoEstragado_devolveNulo() throws Exception {
        byte[] guardada = new CofreDeSenhas(criar -> novaChave()).cifrar("senha");
        SecretKey outra = novaChave();
        assertNull(new CofreDeSenhas(criar -> outra).decifrar(guardada));

        SecretKey chave = novaChave();
        CofreDeSenhas cofre = new CofreDeSenhas(criar -> chave);
        byte[] estragada = cofre.cifrar("senha");
        estragada[estragada.length - 1] ^= 1;
        assertNull("a marca do GCM pega a mudança", cofre.decifrar(estragada));
        assertNull(cofre.decifrar(new byte[]{12}));
        assertNull(cofre.decifrar(null));
    }

    @Test
    public void chaveQueFalha_naoQuebra() {
        CofreDeSenhas cofre = new CofreDeSenhas(criar -> {
            throw new IllegalStateException("keystore indisponível");
        });
        assertNull(cofre.cifrar("senha"));
        assertNull(cofre.decifrar(new byte[]{12, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13}));
    }

    @Test
    public void senhaVazia_naoGuarda() throws Exception {
        SecretKey chave = novaChave();
        CofreDeSenhas cofre = new CofreDeSenhas(criar -> chave);
        assertNull("convidado: sem senha", cofre.cifrar(""));
        assertNull(cofre.cifrar(null));
    }
}
