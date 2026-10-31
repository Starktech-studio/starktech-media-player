// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Application;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Nomes reconhecidos (D4), tipo escolhido, ordem e a pasta de cada arquivo. */
@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, manifest = Config.NONE, sdk = 34)
public class PastasTest {

    private BancoStarkTech banco;
    private Pastas pastas;

    @Before
    public void preparar() {
        banco = new BancoStarkTech(ApplicationProvider.getApplicationContext(), null);
        pastas = new Pastas(banco);
    }

    @Test
    public void reconhecer_semMaiusculaNemAcento() {
        assertEquals(Pastas.Tipo.FILMES, Pastas.reconhecer("Filmes"));
        assertEquals(Pastas.Tipo.FILMES, Pastas.reconhecer("filme"));
        assertEquals(Pastas.Tipo.FILMES, Pastas.reconhecer("MOVIES"));
        assertEquals(Pastas.Tipo.FILMES, Pastas.reconhecer("Films"));
        assertEquals(Pastas.Tipo.SERIES, Pastas.reconhecer("Séries"));
        assertEquals(Pastas.Tipo.SERIES, Pastas.reconhecer("serie"));
        assertEquals(Pastas.Tipo.SERIES, Pastas.reconhecer("Series"));
        assertEquals(Pastas.Tipo.SERIES, Pastas.reconhecer("TV  Shows"));
        assertEquals(Pastas.Tipo.SHOWS, Pastas.reconhecer("Shows"));
        assertEquals(Pastas.Tipo.SHOWS, Pastas.reconhecer("show"));
        assertEquals(Pastas.Tipo.SHOWS, Pastas.reconhecer("Concerts"));
        assertEquals(Pastas.Tipo.SHOWS, Pastas.reconhecer("Concertos"));
        assertNull(Pastas.reconhecer("Downloads"));
        assertNull(Pastas.reconhecer("Filmes 2026"));
    }

    @Test
    public void adicionar_comTipoPeloNomeOuEmBranco() {
        Pastas.Pasta filmes = pastas.adicionar("/storage/HD/Filmes/", Pastas.Origem.HD_EXTERNO);
        assertEquals("/storage/HD/Filmes", filmes.caminho);
        assertEquals(Pastas.Tipo.FILMES, filmes.tipo);
        assertFalse(filmes.escolhido);
        Pastas.Pasta downloads = pastas.adicionar("/storage/HD/Downloads", Pastas.Origem.HD_EXTERNO);
        assertNull("escolha o tipo", downloads.tipo);
    }

    @Test
    public void definirTipo_ficaEscolhidoEMantemAOrdem() {
        pastas.adicionar("/storage/HD/Downloads", Pastas.Origem.HD_EXTERNO);
        pastas.adicionar("/storage/emulated/0/Movies", Pastas.Origem.INTERNO);
        pastas.definirTipo("/storage/HD/Downloads", Pastas.Tipo.IGNORAR);

        Pastas outra = new Pastas(banco); // lê de novo do banco
        assertEquals(2, outra.lista().size());
        assertEquals("/storage/HD/Downloads", outra.lista().get(0).caminho);
        assertEquals(Pastas.Tipo.IGNORAR, outra.lista().get(0).tipo);
        assertTrue(outra.lista().get(0).escolhido);
        assertEquals(Pastas.Origem.INTERNO, outra.lista().get(1).origem);
    }

    @Test
    public void adicionarDeNovo_naoTrocaOTipoEscolhido() {
        pastas.adicionar("/storage/HD/Filmes", Pastas.Origem.HD_EXTERNO);
        pastas.definirTipo("/storage/HD/Filmes", Pastas.Tipo.SHOWS);
        assertEquals(Pastas.Tipo.SHOWS, pastas.adicionar("/storage/HD/Filmes", Pastas.Origem.HD_EXTERNO).tipo);
    }

    @Test
    public void pastaDoArquivo_aMaisProximaAcima() {
        pastas.adicionar("/storage/HD/Videos", Pastas.Origem.HD_EXTERNO);
        pastas.adicionar("/storage/HD/Videos/Séries", Pastas.Origem.HD_EXTERNO);
        assertEquals("/storage/HD/Videos/Séries",
                pastas.pastaDoArquivo("/storage/HD/Videos/Séries/Maré Alta/Temporada 1/01.mkv").caminho);
        assertEquals("/storage/HD/Videos", pastas.pastaDoArquivo("/storage/HD/Videos/outro.mkv").caminho);
        assertNull("nome parecido não conta", pastas.pastaDoArquivo("/storage/HD/Videos2/x.mkv"));
    }

    @Test
    public void remover() {
        pastas.adicionar("/storage/HD/Shows", Pastas.Origem.HD_EXTERNO);
        pastas.remover("/storage/HD/Shows");
        assertTrue(new Pastas(banco).lista().isEmpty());
    }
}
