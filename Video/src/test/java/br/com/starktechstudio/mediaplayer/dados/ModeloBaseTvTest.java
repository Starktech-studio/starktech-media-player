// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.dados;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Application;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Nota (D6), ordem de Novidades e formação das coletâneas (D5). */
@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, manifest = Config.NONE, sdk = 34)
public class ModeloBaseTvTest {

    private static Modelo.Titulo filme(String titulo, boolean assistido) {
        Modelo.Titulo t = new Modelo.Titulo();
        t.id = "/hd/" + titulo + ".mkv";
        t.tipo = Modelo.Tipo.FILME;
        t.titulo = titulo;
        t.ano = "";
        t.assistido = assistido;
        return t;
    }

    private static List<String> titulos(List<Modelo.Titulo> lista) {
        List<String> nomes = new ArrayList<>();
        for (Modelo.Titulo t : lista) nomes.add(t.titulo);
        return nomes;
    }

    @Test
    public void nota_soEmFilmeAssistido() {
        assertTrue(filme("a", true).podeDarNota());
        assertFalse("sem assistido", filme("a", false).podeDarNota());
        for (Modelo.Tipo tipo : new Modelo.Tipo[]{Modelo.Tipo.SERIE, Modelo.Tipo.DESENHO, Modelo.Tipo.SHOW, Modelo.Tipo.STANDUP}) {
            Modelo.Titulo outro = filme("a", true);
            outro.tipo = tipo;
            assertFalse(tipo.name(), outro.podeDarNota());
        }
        Modelo.Titulo capa = filme("a", true);
        capa.id = "coletanea:a";
        assertFalse("capa de coletânea", capa.podeDarNota());
    }

    @Test
    public void nota_deZeroACincoESemNotaEOutraCoisa() {
        assertTrue(Modelo.notaValida(0));
        assertTrue(Modelo.notaValida(5));
        assertFalse(Modelo.notaValida(-1));
        assertFalse(Modelo.notaValida(6));
        Modelo.Titulo semNota = filme("a", true);
        assertNull(semNota.nota);
        semNota.nota = 0;
        assertEquals(Integer.valueOf(0), semNota.copia().nota);
    }

    @Test
    public void capasMontadas_pelosPrefixos() {
        Modelo.Titulo t = new Modelo.Titulo();
        t.id = "coletanea:x";
        assertTrue(t.ehColetanea());
        assertFalse(t.ehArtista());
        t.id = "artista:x";
        assertTrue(t.ehArtista());
        assertFalse(t.ehColetanea());
        t.id = "/hd/artista:x.mkv";
        assertFalse(t.ehArtista());
    }

    @Test
    public void novidades_adicionadoPrimeiroDepoisADataDoArquivo() {
        Modelo.Titulo a = filme("A", false);
        a.adicionado = 100;
        a.modificado = 1;
        Modelo.Titulo b = filme("B", false);
        b.adicionado = 100;
        b.modificado = 3;
        Modelo.Titulo c = filme("C", false);
        c.adicionado = 100;
        c.modificado = 2;
        Modelo.Titulo novoComDataAntiga = filme("D", false);
        novoComDataAntiga.adicionado = 200;
        novoComDataAntiga.modificado = 0;
        assertEquals(Arrays.asList("D", "B", "C", "A"), titulos(RepositorioReal.recentes(Arrays.asList(a, b, c, novoComDataAntiga))));
    }

    @Test
    public void novidades_mesmaDataVaiEmOrdemAlfabetica() {
        Modelo.Titulo b = filme("Bravo", false);
        Modelo.Titulo a = filme("Água", false);
        assertEquals(Arrays.asList("Água", "Bravo"), titulos(RepositorioReal.recentes(Arrays.asList(b, a))));
    }

    @Test
    public void coletanea_agrupaPeloIdEOrdenaPorAno() {
        Modelo.Titulo segundo = filme("Segundo", false);
        segundo.idColetanea = "coletanea:saga";
        segundo.ano = "2005";
        Modelo.Titulo primeiro = filme("Primeiro", false);
        primeiro.idColetanea = "coletanea:saga";
        primeiro.ano = "2001";
        Modelo.Titulo semAno = filme("Sem ano", false);
        semAno.idColetanea = "coletanea:saga";
        Modelo.Titulo solto = filme("Solto", false);
        Map<String, List<Modelo.Titulo>> grupos = RepositorioReal.porColetanea(Arrays.asList(segundo, solto, primeiro, semAno));
        assertEquals(1, grupos.size());
        assertEquals(Arrays.asList("Primeiro", "Segundo", "Sem ano"), titulos(grupos.get("coletanea:saga")));
    }
}
