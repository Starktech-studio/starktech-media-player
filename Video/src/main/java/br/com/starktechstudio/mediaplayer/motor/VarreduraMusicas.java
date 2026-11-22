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
 *
 * <p>2.0: a lista vem pela camada dos arquivos ({@link ArquivosDaBiblioteca}), do aparelho e do servidor em casa.
 * No servidor, tamanho e data vêm da listagem, as tags são lidas por trechos ({@link TagsMusica#lerDoServidor}) e
 * a capa pela imagem da pasta antes da gravada; "sumiu" pela listagem da rodada (servidor fora não apaga nada).
 * A pasta de downloads de Músicas ({@link Pastas#PASTA_DOWNLOADS}) fica fora: as baixadas são do servidor.
 */
public final class VarreduraMusicas {

    private static final String LOG = "StarkTechMusicas";
    /** Músicas lidas entre um aviso e outro (a tela relê). */
    private static final int AVISO_A_CADA = 40;
    private static final int PROFUNDIDADE_MAXIMA = 8;

    private VarreduraMusicas() {}

    /** O que a listagem de uma pasta de Músicas achou. */
    static final class Lista {
        final List<ArquivosDaBiblioteca.Entrada> musicas = new ArrayList<>();
        /** Imagem da pasta de cada álbum (servidor), pela pasta. */
        final Map<String, ArquivosDaBiblioteca.Entrada> imagens = new HashMap<>();
        final Set<String> naoAbriram = new HashSet<>();

        boolean dentroDeQuemNaoAbriu(String caminho) {
            for (String pasta : naoAbriram) if (caminho.equals(pasta) || Pastas.dentro(caminho, pasta)) return true;
            return false;
        }
    }

    /** Devolve true se vale tentar de novo: uma pasta com músicas veio sem nenhuma, com o armazenamento montado. */
    public static boolean atualizar(Context contexto, Varredura.Aviso aviso) {
        return atualizar(contexto, aviso, true);
    }

    /** {@code comServidor} false: as pastas do servidor ficam como estão nesta rodada (D8). */
    public static boolean atualizar(Context contexto, Varredura.Aviso aviso, boolean comServidor) {
        ArquivosDaBiblioteca camada = ArquivosDaBiblioteca.de(contexto);
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

        Lista rodada = new Lista();
        for (Pastas.Pasta pasta : pastas) {
            if (pasta.tipo != Pastas.Tipo.MUSICAS) continue;
            Lista lista = new Lista();
            if (!comServidor && !ArquivosDaBiblioteca.local(pasta.caminho)) lista.naoAbriram.add(pasta.caminho);
            else listar(camada, pasta.caminho, cadastradas, new HashSet<>(), 0, lista);
            rodada.naoAbriram.addAll(lista.naoAbriram);
            achadosPorPasta.put(pasta.caminho, lista.musicas.size());
            for (ArquivosDaBiblioteca.Entrada entrada : lista.musicas) {
                String caminho = entrada.caminho;
                boolean local = ArquivosDaBiblioteca.local(caminho);
                File arquivo = local ? new File(caminho) : null;
                achados.add(caminho);
                AcervoMusicas.Musica velha = antes.get(caminho);
                // Listado, mas sem os dados do arquivo (erro de leitura no HD): o que já tem fica
                if (velha != null && local && !arquivo.exists()) continue;
                long tamanho = local ? arquivo.length() : entrada.tamanho;
                long modificado = local ? arquivo.lastModified() : entrada.modificado;
                if (velha != null && velha.tamanho == tamanho && velha.modificado == modificado
                        && velha.lido == AcervoMusicas.LEITURA && pasta.caminho.equals(velha.pasta)) continue;
                AcervoMusicas.Musica nova = local ? ler(arquivo, pasta.caminho, capas)
                        : lerDoServidor(camada, entrada, pasta.caminho, capas, lista.imagens.get(ArquivosDaBiblioteca.acima(caminho)));
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
            if (saiu || (!pastaSemNenhuma && sumiu(m, rodada))) remover.add(m.caminho);
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

    /** Música do servidor: tags por trechos; capa pela imagem da pasta e, sem ela, pela gravada na música. */
    private static AcervoMusicas.Musica lerDoServidor(ArquivosDaBiblioteca camada, ArquivosDaBiblioteca.Entrada entrada,
                                                      String pasta, CapasMusica capas, ArquivosDaBiblioteca.Entrada imagem) {
        String relativo = entrada.caminho.substring(pasta.length() + 1);
        TagsMusica.Tags tags = TagsMusica.lerDoServidor(camada, entrada.caminho, entrada.tamanho, relativo, imagem == null);
        AcervoMusicas.Musica m = deTags(entrada.caminho, pasta, tags);
        try {
            m.capa = capas.capaDoServidor(camada, entrada.caminho, tags.album, imagem, tags.capa);
        } catch (RuntimeException | OutOfMemoryError e) {
            Log.w(LOG, "capa: não deu para ler " + entrada.nome);
        }
        return m;
    }

    private static AcervoMusicas.Musica deTags(String caminho, String pasta, TagsMusica.Tags tags) {
        AcervoMusicas.Musica m = new AcervoMusicas.Musica();
        m.caminho = caminho;
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
        return m;
    }

    /**
     * As músicas da pasta e das subpastas pela camada dos arquivos (menos as cadastradas à parte, as escondidas e
     * a pasta de downloads); pasta que não abriu vai para {@link Lista#naoAbriram}.
     */
    static void listar(ArquivosDaBiblioteca camada, String dir, Set<String> cadastradas, Set<String> vistas,
                       int profundidade, Lista saida) {
        if (profundidade > PROFUNDIDADE_MAXIMA) return;
        if (ArquivosDaBiblioteca.local(dir)) {
            if (Pastas.deDownloads(dir)) return;
            try {
                if (!vistas.add(new File(dir).getCanonicalPath())) return; // link que volta para trás
            } catch (IOException e) {
                saida.naoAbriram.add(dir);
                return;
            }
        }
        List<ArquivosDaBiblioteca.Entrada> itens = camada.listar(dir);
        if (itens == null) {
            saida.naoAbriram.add(dir);
            return;
        }
        if (!ArquivosDaBiblioteca.local(dir)) {
            ArquivosDaBiblioteca.Entrada imagem = CapasMusica.imagemEntre(itens);
            if (imagem != null) saida.imagens.put(dir, imagem);
        }
        for (ArquivosDaBiblioteca.Entrada item : itens) {
            if (item.pasta) {
                if (!cadastradas.contains(item.caminho)) listar(camada, item.caminho, cadastradas, vistas, profundidade + 1, saida);
            } else if (Nomes.ehAudio(item.nome)) {
                saida.musicas.add(item);
            }
        }
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

    /** Do servidor (2.0): não veio na listagem e nenhuma pasta acima dela deixou de abrir nesta rodada. */
    static boolean sumiu(AcervoMusicas.Musica m, Lista rodada) {
        if (!ArquivosDaBiblioteca.local(m.caminho)) return !rodada.dentroDeQuemNaoAbriu(m.caminho);
        return sumiu(m);
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
