// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Varredura das pastas de Músicas (1.5): acha as músicas, lê as tags e a capa das novas e das que
 * mudaram ({@link TagsMusica}, {@link CapasMusica}) e tira as que sumiram de verdade. Como na varredura de
 * vídeo, HD desligado ou com erro de leitura não apaga nada: a música só sai se a pasta saiu da biblioteca
 * (ou deixou de ser de Músicas) ou se o arquivo sumiu com a pasta dele legível.
 */
public final class VarreduraMusicas {

    private static final String LOG = "StarkTechMusicas";
    /** Músicas lidas entre um aviso e outro (a tela relê). */
    private static final int AVISO_A_CADA = 40;
    private static final int PROFUNDIDADE_MAXIMA = 8;

    private VarreduraMusicas() {}

    /** Devolve true se vale tentar de novo: uma pasta com músicas veio sem nenhuma, com o armazenamento montado. */
    public static boolean atualizar(Context contexto, Varredura.Aviso aviso) {
        List<Pastas.Pasta> pastas = Pastas.de(contexto).lista();
        AcervoMusicas acervo = AcervoMusicas.de(contexto);
        Map<String, AcervoMusicas.Musica> antes = new HashMap<>();
        for (AcervoMusicas.Musica m : acervo.musicas()) antes.put(m.caminho, m);

        Set<String> cadastradas = new HashSet<>();
        for (Pastas.Pasta p : pastas) cadastradas.add(p.caminho);
        Map<String, Integer> achadosPorPasta = new HashMap<>();
        Set<String> achados = new HashSet<>();
        List<AcervoMusicas.Musica> gravar = new ArrayList<>();
        CapasMusica capas = new CapasMusica(contexto);
        long agora = System.currentTimeMillis();
        boolean mudou = false;

        for (Pastas.Pasta pasta : pastas) {
            if (pasta.tipo != Pastas.Tipo.MUSICAS) continue;
            List<File> arquivos = new ArrayList<>();
            listar(new File(pasta.caminho), cadastradas, new HashSet<>(), 0, arquivos);
            achadosPorPasta.put(pasta.caminho, arquivos.size());
            for (File arquivo : arquivos) {
                String caminho = arquivo.getAbsolutePath();
                achados.add(caminho);
                AcervoMusicas.Musica velha = antes.get(caminho);
                // Listado, mas sem os dados do arquivo (erro de leitura no HD): o que já tem fica
                if (velha != null && !arquivo.exists()) continue;
                long tamanho = arquivo.length();
                long modificado = arquivo.lastModified();
                if (velha != null && velha.tamanho == tamanho && velha.modificado == modificado
                        && velha.lido == AcervoMusicas.LEITURA && pasta.caminho.equals(velha.pasta)) continue;
                AcervoMusicas.Musica nova = ler(arquivo, pasta.caminho, capas);
                nova.tamanho = tamanho;
                nova.modificado = modificado;
                nova.adicionado = velha != null ? velha.adicionado : agora;
                gravar.add(nova);
                if (gravar.size() >= AVISO_A_CADA) {
                    acervo.gravar(gravar, Collections.emptyList());
                    gravar.clear();
                    mudou = true;
                    aviso.aoMudar();
                }
            }
        }

        List<String> remover = new ArrayList<>();
        Set<String> semNenhuma = new HashSet<>();
        for (AcervoMusicas.Musica m : antes.values()) {
            if (achados.contains(m.caminho)) continue;
            Pastas.Pasta dona = pastaDe(pastas, m.caminho);
            boolean saiu = !cadastradas.contains(m.pasta) || dona == null || dona.tipo != Pastas.Tipo.MUSICAS;
            Integer achadas = achadosPorPasta.get(m.pasta);
            boolean pastaSemNenhuma = achadas == null || achadas == 0;
            if (saiu || (!pastaSemNenhuma && sumiu(m))) remover.add(m.caminho);
            else if (pastaSemNenhuma) semNenhuma.add(m.pasta);
        }
        if (!gravar.isEmpty() || !remover.isEmpty()) {
            acervo.gravar(gravar, remover);
            mudou = true;
        }
        Log.i(LOG, achados.size() + " achadas, " + remover.size() + " apagadas");
        if (mudou) {
            // Capas que nenhum álbum usa mais (as do HD desligado ficam: as músicas dele continuam no banco)
            Set<String> emUso = new HashSet<>();
            for (AcervoMusicas.Musica m : acervo.musicas()) if (m.capa != null) emUso.add(m.capa);
            capas.limpar(emUso);
            aviso.aoMudar();
        }
        boolean tentarDeNovo = false;
        for (String pasta : semNenhuma) {
            if (ArquivosDoAparelho.raizDe(contexto, pasta) != null) tentarDeNovo = true;
        }
        return tentarDeNovo;
    }

    private static AcervoMusicas.Musica ler(File arquivo, String pasta, CapasMusica capas) {
        String relativo = arquivo.getAbsolutePath().substring(pasta.length() + 1);
        TagsMusica.Tags tags = TagsMusica.ler(arquivo, relativo);
        AcervoMusicas.Musica m = new AcervoMusicas.Musica();
        m.caminho = arquivo.getAbsolutePath();
        m.pasta = pasta;
        m.titulo = tags.titulo;
        m.artista = tags.artista;
        m.artistaAlbum = tags.artistaAlbum;
        m.album = tags.album;
        m.faixa = tags.faixa;
        m.disco = tags.disco;
        m.ano = tags.ano;
        m.genero = tags.genero;
        m.duracaoMs = tags.duracaoMs;
        m.lido = AcervoMusicas.LEITURA;
        try {
            m.capa = capas.capa(arquivo, tags.album, tags.capa);
        } catch (RuntimeException | OutOfMemoryError e) {
            Log.w(LOG, "capa: não deu para ler " + arquivo.getName());
        }
        return m;
    }

    /** As músicas da pasta e das subpastas (menos as cadastradas à parte e as escondidas). */
    private static void listar(File dir, Set<String> cadastradas, Set<String> vistas, int profundidade, List<File> saida) {
        if (profundidade > PROFUNDIDADE_MAXIMA) return;
        try {
            if (!vistas.add(dir.getCanonicalPath())) return; // link que volta para trás
        } catch (IOException e) {
            return;
        }
        File[] itens = dir.listFiles();
        if (itens == null) return;
        List<File> ordem = new ArrayList<>(Arrays.asList(itens));
        Collections.sort(ordem, (a, b) -> a.getName().compareTo(b.getName()));
        for (File item : ordem) {
            if (item.getName().startsWith(".")) continue;
            if (item.isDirectory()) {
                if (!cadastradas.contains(item.getAbsolutePath())) listar(item, cadastradas, vistas, profundidade + 1, saida);
            } else if (Nomes.ehAudio(item.getName())) {
                saida.add(item);
            }
        }
    }

    private static Pastas.Pasta pastaDe(List<Pastas.Pasta> pastas, String caminho) {
        Pastas.Pasta melhor = null;
        for (Pastas.Pasta p : pastas) {
            if (Pastas.dentro(caminho, p.caminho) && (melhor == null || p.caminho.length() > melhor.caminho.length())) melhor = p;
        }
        return melhor;
    }

    /** O arquivo sumiu de verdade: não existe e a pasta mais próxima acima dele que existe lista algo. */
    private static boolean sumiu(AcervoMusicas.Musica m) {
        File arquivo = new File(m.caminho);
        if (arquivo.exists()) return false;
        File raiz = new File(m.pasta);
        File acima = arquivo.getParentFile();
        while (acima != null && !acima.exists() && !acima.equals(raiz)) acima = acima.getParentFile();
        String[] itens = acima == null ? null : acima.list();
        return itens != null && itens.length > 0;
    }
}
