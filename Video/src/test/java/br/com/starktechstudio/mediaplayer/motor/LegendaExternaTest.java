// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Legenda em arquivo: o que é do vídeo, o nome da faixa e a cópia para a pasta de legendas. */
public class LegendaExternaTest {

    private static final String VIDEO = "Filme.2024.mkv";

    @Rule
    public TemporaryFolder temporaria = new TemporaryFolder();

    @Test
    public void ehLegenda_pelaExtensao() {
        assertTrue(LegendaExterna.ehLegenda("a.srt"));
        assertTrue(LegendaExterna.ehLegenda("a.ASS"));
        assertTrue(LegendaExterna.ehLegenda("a.ssa"));
        assertTrue(LegendaExterna.ehLegenda("a.vtt"));
        assertFalse(LegendaExterna.ehLegenda("a.mkv"));
        assertFalse(LegendaExterna.ehLegenda("srt"));
    }

    @Test
    public void doVideo_soComONomeInteiro() {
        assertTrue(LegendaExterna.doVideo(VIDEO, "/hd/Filme.2024.srt"));
        assertTrue(LegendaExterna.doVideo(VIDEO, "/hd/filme.2024.por.srt"));
        assertFalse(LegendaExterna.doVideo(VIDEO, "/hd/Filme.2024 Extras.srt"));
        assertFalse(LegendaExterna.doVideo(VIDEO, "/hd/Outro.srt"));
    }

    @Test
    public void deOutroVideo_comecaIgualMasContinua() {
        assertTrue(LegendaExterna.deOutroVideo("Episodio 1.mkv", "/hd/Episodio 10.srt"));
        assertFalse(LegendaExterna.deOutroVideo("Episodio 1.mkv", "/hd/Episodio 1.srt"));
        assertFalse(LegendaExterna.deOutroVideo("Episodio 1.mkv", "/hd/Episodio 1.por.srt"));
        // Arquivo temporário do motor: não dá para saber, fica na lista
        assertFalse(LegendaExterna.deOutroVideo("Episodio 1.mkv", "/data/cache/tmp123"));
    }

    @Test
    public void idioma_soODoNomeQueSobra() {
        assertEquals("por", LegendaExterna.idioma(VIDEO, "/hd/Filme.2024.por.srt"));
        assertEquals("pt-BR", LegendaExterna.idioma(VIDEO, "/hd/Filme.2024.pt-BR.srt"));
        assertEquals("", LegendaExterna.idioma(VIDEO, "/hd/Filme.2024.srt"));
        assertEquals("", LegendaExterna.idioma(VIDEO, "/hd/Filme.2024.Dublado.srt"));
        // O fim do nome do vídeo não é idioma
        assertEquals("", LegendaExterna.idioma("Os.Dez.mkv", "/hd/Os.Dez.srt"));
    }

    @Test
    public void rotulo_oQueOArquivoTemAMais() {
        assertEquals("Minha legenda", LegendaExterna.rotulo(VIDEO, "/hd/Filme.2024.Minha legenda.srt", "SRT"));
        assertEquals("SRT", LegendaExterna.rotulo(VIDEO, "/hd/Filme.2024.srt", "SRT"));
        assertEquals("portuguese", LegendaExterna.rotulo(VIDEO, "/data/cache/tmp123", "s_portuguese"));
    }

    @Test
    public void nomeDaCopia_extensaoMinusculaENomeCurto() {
        assertEquals("Filme.2024.Minha legenda.srt", LegendaExterna.nomeDaCopia(VIDEO, "Minha legenda.SRT"));
        StringBuilder longo = new StringBuilder();
        for (int i = 0; i < 300; i++) longo.append('a');
        assertEquals("Filme.2024.srt", LegendaExterna.nomeDaCopia(VIDEO, longo + ".srt"));
    }

    @Test
    public void paraOMotor_aoLadoDoVideoUsaOProprio() throws IOException {
        File pasta = temporaria.newFolder("filmes");
        File video = new File(pasta, VIDEO);
        File legenda = escrever(new File(pasta, "Filme.2024.por.srt"), "um");
        assertEquals(legenda, LegendaExterna.paraOMotor(video, legenda, temporaria.newFolder("cache")));
    }

    @Test
    public void paraOMotor_comOutroNomeCopiaParaAPastaDeLegendas() throws IOException {
        File pasta = temporaria.newFolder("filmes");
        File cache = temporaria.newFolder("cache");
        File video = new File(pasta, VIDEO);
        File legenda = escrever(new File(pasta, "Minha legenda.srt"), "dois");
        File pronta = LegendaExterna.paraOMotor(video, legenda, cache);
        assertEquals(new File(cache, "Filme.2024.Minha legenda.srt"), pronta);
        assertEquals("dois", new String(Files.readAllBytes(pronta.toPath()), StandardCharsets.UTF_8));
        assertTrue(LegendaExterna.doVideo(VIDEO, pronta.getPath()));
    }

    @Test
    public void paraOMotor_mesmoNomeEmOutraPastaTambemCopia() throws IOException {
        File cache = temporaria.newFolder("cache");
        File video = new File(temporaria.newFolder("filmes"), VIDEO);
        File legenda = escrever(new File(temporaria.newFolder("outra"), "Filme.2024.srt"), "tres");
        assertEquals(cache, LegendaExterna.paraOMotor(video, legenda, cache).getParentFile());
    }

    private static File escrever(File arquivo, String texto) throws IOException {
        Files.write(arquivo.toPath(), texto.getBytes(StandardCharsets.UTF_8));
        return arquivo;
    }
}
