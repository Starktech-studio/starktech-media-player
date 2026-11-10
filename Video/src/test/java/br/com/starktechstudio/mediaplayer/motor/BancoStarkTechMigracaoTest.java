// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.IOException;

/** Banco v3 sobre um banco v2 (só em arquivo de teste): não perde posição nem edição, e os dados novos entram. */
@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, manifest = Config.NONE, sdk = 34)
public class BancoStarkTechMigracaoTest {

    private static final String BANCO = "migracao_teste.db";
    private static final String FILME = "/hd/Filmes/Nome (2012).mkv";

    @Rule
    public TemporaryFolder temporaria = new TemporaryFolder();

    private BancoStarkTech bancoMigrado() {
        Context contexto = ApplicationProvider.getApplicationContext();
        contexto.deleteDatabase(BANCO);
        File arquivo = contexto.getDatabasePath(BANCO);
        //noinspection ResultOfMethodCallIgnored
        arquivo.getParentFile().mkdirs();
        try (SQLiteDatabase antigo = SQLiteDatabase.openOrCreateDatabase(arquivo, null)) {
            BancoStarkTech.criarAteVersao2(antigo);
            ContentValues video = new ContentValues();
            video.put("caminho", FILME);
            video.put("pasta", "/hd/Filmes");
            video.put("tipo", Classificador.Tipo.FILME.name());
            video.put("titulo", "Nome");
            video.put("ano", 2012);
            video.put("adicionado", 1000L);
            antigo.insertOrThrow(BancoStarkTech.VIDEO, null, video);

            ContentValues estado = new ContentValues();
            estado.put("chave", FILME);
            estado.put("assistido", 1);
            estado.put("posicao", 1234);
            antigo.insertOrThrow(BancoStarkTech.ESTADO, null, estado);

            ContentValues tmdb = new ContentValues();
            tmdb.put("chave", FILME);
            tmdb.put("situacao", Acervo.ACHADO);
            tmdb.put("id_tmdb", 99L);
            tmdb.put("buscado", 1000L);
            antigo.insertOrThrow(BancoStarkTech.TMDB, null, tmdb);

            ContentValues edicao = new ContentValues();
            edicao.put(BancoStarkTech.CHAVE, FILME);
            edicao.put(BancoStarkTech.CAMPO, Edicoes.Campo.TITULO.name());
            edicao.put(BancoStarkTech.VALOR, "Meu título");
            edicao.put(BancoStarkTech.ALTERADO, 2000L);
            antigo.insertOrThrow(BancoStarkTech.EDICAO, null, edicao);

            antigo.setVersion(2);
        }
        return new BancoStarkTech(contexto, BANCO);
    }

    @Test
    public void naoPerdePosicaoNemEdicao() throws IOException {
        BancoStarkTech banco = bancoMigrado();
        Acervo.Estado estado = new Acervo(banco).estados().get(FILME);
        assertNotNull(estado);
        assertEquals(1234, estado.posicao);
        assertTrue(estado.assistido);
        Edicoes edicoes = new Edicoes(banco, temporaria.newFolder("imagens"));
        assertEquals("Meu título", edicoes.ler(FILME).get(Edicoes.Campo.TITULO));
    }

    @Test
    public void faixasEscolhidas_nulasDepoisDaMigracaoEGuardadasAoGravar() {
        Acervo acervo = new Acervo(bancoMigrado());
        Acervo.Estado estado = acervo.estados().get(FILME);
        assertNull(estado.faixaAudio);
        assertNull(estado.faixaLegenda);

        estado.faixaAudio = "a1";
        estado.faixaLegenda = "arquivo:Nome.por.srt";
        acervo.gravarEstado(FILME, estado);

        Acervo.Estado lido = acervo.estados().get(FILME);
        assertEquals("a1", lido.faixaAudio);
        assertEquals("arquivo:Nome.por.srt", lido.faixaLegenda);
        assertEquals("a cópia leva as faixas", "a1", lido.copia().faixaAudio);
        assertEquals(1234, lido.posicao);
    }

    @Test
    public void colunasNovas_comOValorDeQuemVeioDaV2() {
        Acervo acervo = new Acervo(bancoMigrado());
        assertNull("sem nota, diferente de 0", acervo.estados().get(FILME).nota);
        Acervo.Tmdb tmdb = acervo.tmdb().get(FILME);
        assertEquals("consulta a coleção uma vez", Acervo.COLECAO_NAO_CONSULTADA, tmdb.colecaoId);
        assertEquals(99L, tmdb.idTmdb);
        assertNull(acervo.video(FILME).grupo);
        assertTrue(acervo.artistas().isEmpty());
    }

    @Test
    public void edicaoAntiga_passaAValerPeloNomeEmComum() throws IOException {
        Edicoes edicoes = new Edicoes(bancoMigrado(), temporaria.newFolder("imagens"));
        String nome = Classificador.chaveNome(Classificador.Tipo.FILME, "Nome", 2012, null, 0, 0, null);
        assertEquals("Meu título", edicoes.valendo("/hd/Filmes/Outro arquivo.mkv", nome).get(Edicoes.Campo.TITULO));
    }

    @Test
    public void bancoNovo_gravaELeNotaColecaoEGrupo() {
        Acervo acervo = new Acervo(new BancoStarkTech(ApplicationProvider.getApplicationContext(), null));

        Acervo.Estado semNota = new Acervo.Estado();
        Acervo.Estado zero = new Acervo.Estado();
        zero.nota = 0;
        acervo.gravarEstado("a", semNota);
        acervo.gravarEstado("b", zero);
        assertNull(acervo.estados().get("a").nota);
        assertEquals(Integer.valueOf(0), acervo.estados().get("b").nota);
        assertEquals(Integer.valueOf(0), zero.copia().nota);

        Acervo.Tmdb tmdb = new Acervo.Tmdb();
        tmdb.chave = "c";
        tmdb.situacao = Acervo.ACHADO;
        tmdb.colecaoId = 10;
        tmdb.colecaoNome = "Coletânea";
        tmdb.colecaoCapa = "/capa.jpg";
        acervo.gravarTmdb(tmdb);
        Acervo.Tmdb lida = acervo.tmdb().get("c");
        assertEquals(10, lida.colecaoId);
        assertEquals("Coletânea", lida.colecaoNome);
        assertEquals("/capa.jpg", lida.colecaoCapa);
        assertNull(lida.colecaoFundo);

        Acervo.Artista artista = new Acervo.Artista();
        artista.chave = Acervo.chaveArtista("Queen");
        artista.nome = "Queen";
        artista.situacao = Acervo.ACHADO;
        artista.fonte = BuscaArtista.DEEZER;
        artista.foto = "/foto.jpg";
        acervo.gravarArtista(artista);
        Acervo.Artista lido = acervo.artistas().get(Acervo.chaveArtista("QUEEN"));
        assertEquals("/foto.jpg", lido.foto);
        assertEquals(BuscaArtista.DEEZER, lido.fonte);
    }

    @Test
    public void versao7_musicasPlaylistsEAtrasoDaLegenda() {
        BancoStarkTech banco = bancoMigrado();
        Acervo acervo = new Acervo(banco);
        Acervo.Estado estado = acervo.estados().get(FILME);
        assertEquals("sem atraso depois da migração", 0, estado.atrasoLegenda);
        assertEquals(1234, estado.posicao);
        estado.atrasoLegenda = -1500;
        acervo.gravarEstado(FILME, estado);
        assertEquals(-1500, acervo.estados().get(FILME).atrasoLegenda);
        assertEquals("a cópia leva o atraso", -1500, acervo.estados().get(FILME).copia().atrasoLegenda);

        AcervoMusicas musicas = new AcervoMusicas(banco);
        assertTrue(musicas.musicas().isEmpty());
        long id = musicas.criarPlaylist("Nova", java.util.Collections.singletonList("/hd/Musicas/a.mp3"));
        assertEquals(1, musicas.playlist(id).musicas.size());
    }

    @Test
    public void versao6_quemVeioDoDeezerOuDaTheAudioDbVoltaAFila() {
        BancoStarkTech banco = new BancoStarkTech(ApplicationProvider.getApplicationContext(), null);
        Acervo acervo = new Acervo(banco);
        String[][] artistas = {{"Queen", BuscaArtista.DEEZER}, {"Coldplay", BuscaArtista.THEAUDIODB}, {"Muse", BuscaArtista.FANART}};
        for (String[] a : artistas) {
            Acervo.Artista artista = new Acervo.Artista();
            artista.chave = Acervo.chaveArtista(a[0]);
            artista.nome = a[0];
            artista.situacao = Acervo.ACHADO;
            artista.fonte = a[1];
            artista.foto = "/" + a[0] + ".jpg";
            acervo.gravarArtista(artista);
        }
        BancoStarkTech.criarVersao6(banco.getWritableDatabase());
        assertEquals(Acervo.PENDENTE, acervo.artistas().get(Acervo.chaveArtista("Queen")).situacao);
        assertEquals(Acervo.PENDENTE, acervo.artistas().get(Acervo.chaveArtista("Coldplay")).situacao);
        assertEquals(Acervo.ACHADO, acervo.artistas().get(Acervo.chaveArtista("Muse")).situacao);
        assertEquals("a foto antiga fica até achar outra", "/Queen.jpg", acervo.artistas().get(Acervo.chaveArtista("Queen")).foto);
    }
}
