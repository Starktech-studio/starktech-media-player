// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/** Valores de fábrica, a volta da lista e as configurações gravadas nas chaves que o motor do Nova lê. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class PreferenciasTest {

    private Context contexto;
    private SharedPreferences doNova;

    @Before
    public void limpar() {
        contexto = RuntimeEnvironment.getApplication();
        contexto.getSharedPreferences("st_preferencias", Context.MODE_PRIVATE).edit().clear().commit();
        doNova = PreferenceManager.getDefaultSharedPreferences(contexto);
        doNova.edit().clear().commit();
    }

    @Test
    public void valoresDeFabrica() {
        assertEquals(10, Preferencias.puloSegundos(contexto));
        assertEquals(5_000, Preferencias.tempoControlesMs(contexto));
        assertEquals(10_000, Preferencias.trocaNovidadesMs(contexto));
        assertTrue(Preferencias.previaLigada(contexto));
        assertTrue(Preferencias.atualizarAoAbrir(contexto));
        assertTrue(Preferencias.procurarVersaoSozinho(contexto));
        assertFalse(Preferencias.proximoEpisodioAutomatico(contexto));
        assertFalse(Preferencias.showsPorGrupo(contexto));
        assertEquals(2, Preferencias.indice(contexto, Preferencias.Item.TAMANHO_LEGENDA));
    }

    @Test
    public void escolherGravaOValorDoSeletor() {
        Preferencias.Item item = Preferencias.Item.COR_LEGENDA;
        Preferencias.escolher(contexto, item, 2);
        assertEquals(2, Preferencias.indice(contexto, item));
        Preferencias.escolher(contexto, item, 99);
        assertEquals(item.valores.length - 1, Preferencias.indice(contexto, item));
    }

    @Test
    public void proximoPercorreEVoltaAoPrimeiro() {
        Preferencias.Item item = Preferencias.Item.TROCA_NOVIDADES;
        assertEquals(item.valores[1], Preferencias.proximo(contexto, item));
        assertEquals(30_000, Preferencias.trocaNovidadesMs(contexto));
        assertEquals(item.valores[2], Preferencias.proximo(contexto, item));
        assertEquals(0, Preferencias.trocaNovidadesMs(contexto));
        assertEquals(item.valores[0], Preferencias.proximo(contexto, item));
        assertEquals(10_000, Preferencias.trocaNovidadesMs(contexto));
    }

    @Test
    public void asDoNovaVaoParaAChaveDoMotorNoFormatoDele() {
        Preferencias.proximo(contexto, Preferencias.Item.TAXA_DA_TV);
        assertEquals("1", doNova.getString("enable_tv_refreshrate_switch_mode", null));
        Preferencias.proximo(contexto, Preferencias.Item.DECODIFICACAO);
        assertTrue(doNova.getBoolean("force_software_decoding", false));
        Preferencias.proximo(contexto, Preferencias.Item.AUDIO_DIRETO);
        assertEquals("2", doNova.getString("force_audio_passthrough_multiple", null));
        Preferencias.proximo(contexto, Preferencias.Item.DOLBY_VISION);
        assertEquals("off", doNova.getString("dolby_vision_mode", null));
    }

    @Test
    public void asDoNovaLeemOQueEstaNaChaveEVoltam() {
        assertEquals(0, Preferencias.indice(contexto, Preferencias.Item.DOLBY_VISION));
        doNova.edit().putString("dolby_vision_mode", "off").commit();
        assertEquals(1, Preferencias.indice(contexto, Preferencias.Item.DOLBY_VISION));
        Preferencias.proximo(contexto, Preferencias.Item.DOLBY_VISION);
        assertEquals("auto", doNova.getString("dolby_vision_mode", null));

        // Valor que esta lista não tem (ex.: "force", da tela do Nova): vale como o de fábrica
        doNova.edit().putString("dolby_vision_mode", "force").commit();
        assertEquals(0, Preferencias.indice(contexto, Preferencias.Item.DOLBY_VISION));
    }
}
