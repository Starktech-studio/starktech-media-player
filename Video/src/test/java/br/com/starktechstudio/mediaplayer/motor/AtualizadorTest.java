// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Application;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Collections;

/** Leitura do version.json e comparação de versões (sem rede). */
@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, manifest = Config.NONE, sdk = 34)
public class AtualizadorTest {

    private static final String PACOTE = "br.com.starktechstudio.mediaplayer";
    private static final String APK = "https://endereco-do-site/arquivo.apk";

    private static String json(String versao, String codigo, String novidades, String apk) {
        return "{\"versao\":" + versao + ",\"versionCode\":" + codigo + ",\"novidades\":" + novidades + ",\"apk\":" + apk + "}";
    }

    @Test
    public void lerVersao_formatoDoPlano() throws JSONException {
        Atualizador.Versao versao = Atualizador.lerVersao(
                json("\"0.2.0\"", "2", "[\"Novidade 1\",\"Novidade 2\"]", "\"" + APK + "\""));
        assertEquals("0.2.0", versao.nome);
        assertEquals(2L, versao.codigo);
        assertEquals(Arrays.asList("Novidade 1", "Novidade 2"), versao.novidades);
        assertEquals(APK, versao.apk);
    }

    @Test
    public void lerVersao_novidadesVaziasSaem() throws JSONException {
        Atualizador.Versao versao = Atualizador.lerVersao(
                json("\"0.2.0\"", "2", "[\"  Uma  \",\"\",null]", "\"" + APK + "\""));
        assertEquals(Collections.singletonList("Uma"), versao.novidades);
    }

    @Test
    public void lerVersao_semNovidades_listaVazia() throws JSONException {
        Atualizador.Versao versao = Atualizador.lerVersao("{\"versao\":\"0.2.0\",\"versionCode\":2,\"apk\":\"" + APK + "\"}");
        assertTrue(versao.novidades.isEmpty());
    }

    @Test(expected = JSONException.class)
    public void lerVersao_paginaDeErroNoLugarDoJson() throws JSONException {
        Atualizador.lerVersao("<!doctype html><html></html>");
    }

    @Test(expected = JSONException.class)
    public void lerVersao_semVersao() throws JSONException {
        Atualizador.lerVersao("{\"versionCode\":2,\"apk\":\"" + APK + "\"}");
    }

    @Test(expected = JSONException.class)
    public void lerVersao_versaoNula() throws JSONException {
        Atualizador.lerVersao(json("null", "2", "[]", "\"" + APK + "\""));
    }

    @Test(expected = JSONException.class)
    public void lerVersao_semVersionCode() throws JSONException {
        Atualizador.lerVersao("{\"versao\":\"0.2.0\",\"apk\":\"" + APK + "\"}");
    }

    @Test(expected = JSONException.class)
    public void lerVersao_versionCodeZero() throws JSONException {
        Atualizador.lerVersao(json("\"0.2.0\"", "0", "[]", "\"" + APK + "\""));
    }

    @Test(expected = JSONException.class)
    public void lerVersao_apkSemHttps() throws JSONException {
        Atualizador.lerVersao(json("\"0.2.0\"", "2", "[]", "\"http://endereco-do-site/arquivo.apk\""));
    }

    @Test
    public void temVersaoNova_soComCodigoMaior() {
        assertTrue(Atualizador.temVersaoNova(1, 2));
        assertFalse("mesma versão", Atualizador.temVersaoNova(2, 2));
        assertFalse("site mais velho que o app", Atualizador.temVersaoNova(3, 2));
    }

    @Test
    public void apkAceito_doAppPrometidoEMaisNovo() {
        assertTrue(Atualizador.apkAceito(PACOTE, 2, PACOTE, 1, 2));
    }

    @Test
    public void apkAceito_recusaOutroPacote() {
        assertFalse(Atualizador.apkAceito("com.outro.app", 2, PACOTE, 1, 2));
        assertFalse("não é APK", Atualizador.apkAceito(null, 2, PACOTE, 1, 2));
    }

    @Test
    public void apkAceito_recusaCodigoDiferenteDoSite() {
        assertFalse(Atualizador.apkAceito(PACOTE, 3, PACOTE, 1, 2));
    }

    @Test
    public void apkAceito_recusaVersaoNaoMaisNova() {
        assertFalse(Atualizador.apkAceito(PACOTE, 2, PACOTE, 2, 2));
    }

    @Test
    public void percentual_doDownload() {
        assertEquals(0, Atualizador.percentual(0, 200));
        assertEquals(50, Atualizador.percentual(100, 200));
        assertEquals(100, Atualizador.percentual(200, 200));
        assertEquals("nunca passa de 100", 100, Atualizador.percentual(300, 200));
        assertEquals("sem tamanho informado", -1, Atualizador.percentual(100, -1));
    }

    @Test
    public void percentual_arquivoGrandeNaoEstoura() {
        int tamanho = 1_500_000_000;
        assertEquals(50, Atualizador.percentual(750_000_000L, tamanho));
    }
}
