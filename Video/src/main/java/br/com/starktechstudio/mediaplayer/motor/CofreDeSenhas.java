// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Cofre da senha do servidor em casa (2.0, D5 = B): AES-GCM com a chave no Keystore do Android, que não sai
 * do aparelho. A senha cifrada fica no {@code starktech.db} ({@link Servidores}); decifrada, vai só para a
 * memória do Nova ({@code addCredential}). Nunca em log, arquivo de texto ou endereço {@code smb://}.
 *
 * <p>Sem a chave (backup do Android levado a outro aparelho, chave apagada) ou com o texto cifrado por outra
 * chave, {@link #decifrar} devolve nulo e o servidor pede a senha de novo.
 *
 * <p>Formato guardado: 1 byte com o tamanho do vetor inicial, o vetor e o texto cifrado (com a marca do GCM).
 */
public final class CofreDeSenhas {

    /** De onde vem a chave; nos testes, uma chave em memória. */
    interface Chave {
        /** A chave do cofre; {@code criar} = gera se ainda não houver. Nula se não houver e não puder criar. */
        SecretKey obter(boolean criar) throws Exception;
    }

    private static final String TRANSFORMACAO = "AES/GCM/NoPadding";
    private static final int MARCA_BITS = 128;
    private static final String KEYSTORE = "AndroidKeyStore";
    private static final String APELIDO = "starktech-servidor";

    private static CofreDeSenhas instancia;

    private final Chave chave;

    public static synchronized CofreDeSenhas doAparelho() {
        if (instancia == null) instancia = new CofreDeSenhas(CofreDeSenhas::chaveDoKeystore);
        return instancia;
    }

    CofreDeSenhas(Chave chave) {
        this.chave = chave;
    }

    /** A senha cifrada; nula se a senha é vazia ou o cofre falhou (aí nada é guardado). */
    public byte[] cifrar(String senha) {
        if (senha == null || senha.isEmpty()) return null;
        try {
            SecretKey k = chave.obter(true);
            if (k == null) return null;
            Cipher cifra = Cipher.getInstance(TRANSFORMACAO);
            cifra.init(Cipher.ENCRYPT_MODE, k);
            byte[] vetor = cifra.getIV();
            byte[] cifrado = cifra.doFinal(senha.getBytes(StandardCharsets.UTF_8));
            if (vetor == null || vetor.length == 0 || vetor.length > 255) return null;
            byte[] saida = new byte[1 + vetor.length + cifrado.length];
            saida[0] = (byte) vetor.length;
            System.arraycopy(vetor, 0, saida, 1, vetor.length);
            System.arraycopy(cifrado, 0, saida, 1 + vetor.length, cifrado.length);
            return saida;
        } catch (Exception e) {
            return null; // sem detalhe no log: a mensagem do erro não leva a senha, mas não há o que registrar
        }
    }

    /** A senha; nula sem a chave, com o texto de outra chave ou com o dado estragado (aí pede a senha de novo). */
    public String decifrar(byte[] guardado) {
        if (guardado == null || guardado.length < 2) return null;
        int tamanhoVetor = guardado[0] & 0xFF;
        if (tamanhoVetor == 0 || guardado.length <= 1 + tamanhoVetor) return null;
        try {
            SecretKey k = chave.obter(false);
            if (k == null) return null;
            Cipher cifra = Cipher.getInstance(TRANSFORMACAO);
            cifra.init(Cipher.DECRYPT_MODE, k, new GCMParameterSpec(MARCA_BITS, Arrays.copyOfRange(guardado, 1, 1 + tamanhoVetor)));
            byte[] aberto = cifra.doFinal(guardado, 1 + tamanhoVetor, guardado.length - 1 - tamanhoVetor);
            return new String(aberto, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    /** Chave AES de 256 bits no Keystore do Android, só para cifrar e decifrar com GCM. */
    private static SecretKey chaveDoKeystore(boolean criar) throws Exception {
        KeyStore loja = KeyStore.getInstance(KEYSTORE);
        loja.load(null);
        KeyStore.Entry entrada = loja.getEntry(APELIDO, null);
        if (entrada instanceof KeyStore.SecretKeyEntry) return ((KeyStore.SecretKeyEntry) entrada).getSecretKey();
        if (!criar) return null;
        KeyGenerator gerador = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE);
        gerador.init(new KeyGenParameterSpec.Builder(APELIDO, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        return gerador.generateKey();
    }
}
