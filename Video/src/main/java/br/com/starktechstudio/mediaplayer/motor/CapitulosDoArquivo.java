// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Capítulos lidos direto do arquivo, em Java (F3): a reserva quando o motor não traz capítulos (o
 * motor do Nova não carregou no aparelho e o Android ficou no lugar, ou o leitor dele falhou).
 * MKV: o elemento Chapters (achado pelo SeekHead, inclusive um 2º apontado pelo 1º, ou antes do
 * primeiro Cluster), como o MKVToolNix
 * grava — início, fim e o primeiro nome de cada capítulo da primeira edição. MP4: os capítulos
 * Nero ({@code chpl}) e a trilha de capítulos do QuickTime ({@code tref/chap}, trilha de texto).
 * Só lê os pedaços do arquivo que precisa; arquivo estragado ou de outro formato: nenhum capítulo.
 */
public final class CapitulosDoArquivo {

    /** Limite de cada pedaço lido (índice do MP4, capítulos do MKV). */
    private static final long LIMITE = 64L * 1024 * 1024;
    /** Quantos SeekHead ler num arquivo (arquivo estragado pode apontar um para o outro sem fim). */
    private static final int MAXIMO_SEEK_HEADS = 8;

    private CapitulosDoArquivo() {}

    public static Capitulos ler(String caminho) {
        File arquivo = new File(caminho);
        String nome = arquivo.getName().toLowerCase(Locale.ROOT);
        try (RandomAccessFile f = new RandomAccessFile(arquivo, "r")) {
            if (nome.endsWith(".mkv") || nome.endsWith(".mka") || nome.endsWith(".webm")) return mkv(f);
            if (nome.endsWith(".mp4") || nome.endsWith(".m4v") || nome.endsWith(".mov")) return mp4(f);
        } catch (IOException | RuntimeException e) {
            // sem capítulos
        }
        return Capitulos.nenhum();
    }

    // =====================================================================
    // MKV (EBML)
    // =====================================================================

    private static final int EBML = 0x1A45DFA3;
    private static final int SEGMENT = 0x18538067;
    private static final int SEEK_HEAD = 0x114D9B74;
    private static final int SEEK = 0x4DBB;
    private static final int SEEK_ID = 0x53AB;
    private static final int SEEK_POSITION = 0x53AC;
    private static final int CHAPTERS = 0x1043A770;
    private static final int CLUSTER = 0x1F43B675;
    private static final int EDITION_ENTRY = 0x45B9;
    private static final int CHAPTER_ATOM = 0xB6;
    private static final int CHAPTER_TIME_START = 0x91;
    private static final int CHAPTER_TIME_END = 0x92;
    private static final int CHAPTER_FLAG_HIDDEN = 0x98;
    private static final int CHAPTER_DISPLAY = 0x80;
    private static final int CHAP_STRING = 0x85;

    /** Cabeçalho de um elemento: id, tamanho (-1 = desconhecido) e onde começam os dados. */
    private static final class Elemento {
        int id;
        long tamanho;
        long dados;
    }

    private static Capitulos mkv(RandomAccessFile f) throws IOException {
        Elemento e = elemento(f, 0);
        if (e == null || e.id != EBML) return Capitulos.nenhum();
        Elemento segmento = elemento(f, e.dados + e.tamanho);
        if (segmento == null || segmento.id != SEGMENT) return Capitulos.nenhum();
        long inicio = segmento.dados;
        long fim = segmento.tamanho < 0 ? f.length() : Math.min(f.length(), inicio + segmento.tamanho);

        // Primeiro os elementos de cima até o primeiro Cluster; o SeekHead diz onde está o Chapters
        // quando ele foi gravado depois (no fim do arquivo)
        long posicaoCapitulos = -1;
        long pos = inicio;
        Set<Long> seekHeadsLidos = new HashSet<>();
        for (int i = 0; i < 64 && pos < fim; i++) {
            Elemento filho = elemento(f, pos);
            if (filho == null || filho.id == CLUSTER) break;
            if (filho.id == CHAPTERS) {
                posicaoCapitulos = pos;
                break;
            }
            if (filho.id == SEEK_HEAD && filho.tamanho > 0 && filho.tamanho < LIMITE) {
                long achado = procurarNoSeekHead(f, filho, inicio, seekHeadsLidos);
                if (achado >= 0) {
                    posicaoCapitulos = achado;
                    break;
                }
            }
            if (filho.tamanho < 0) break;
            pos = filho.dados + filho.tamanho;
        }
        if (posicaoCapitulos < 0) return Capitulos.nenhum();
        Elemento capitulos = elemento(f, posicaoCapitulos);
        if (capitulos == null || capitulos.id != CHAPTERS || capitulos.tamanho <= 0 || capitulos.tamanho > LIMITE) {
            return Capitulos.nenhum();
        }
        byte[] dados = new byte[(int) capitulos.tamanho];
        f.seek(capitulos.dados);
        f.readFully(dados);
        return lerCapitulosMkv(dados);
    }

    /**
     * Onde está o Chapters segundo este SeekHead; se ele não disser, segundo os outros que ele aponta
     * (o 1º pode trazer só o caminho de um 2º, gravado depois dos Clusters). -1 se nenhum disser.
     */
    private static long procurarNoSeekHead(RandomAccessFile f, Elemento seekHead, long inicioSegmento, Set<Long> lidos) throws IOException {
        if (lidos.size() >= MAXIMO_SEEK_HEADS || !lidos.add(seekHead.dados)) return -1;
        byte[] dados = new byte[(int) seekHead.tamanho];
        f.seek(seekHead.dados);
        f.readFully(dados);
        List<Long> outros = new ArrayList<>();
        int p = 0;
        while (p < dados.length) {
            long[] seek = cabecalho(dados, p);
            if (seek == null) break;
            int corpo = (int) seek[2];
            int fimSeek = (int) Math.min(dados.length, corpo + seek[1]);
            if (seek[0] == SEEK) {
                long alvo = -1;
                long posicao = -1;
                int q = corpo;
                while (q < fimSeek) {
                    long[] campo = cabecalho(dados, q);
                    if (campo == null) break;
                    int d = (int) campo[2];
                    int t = (int) campo[1];
                    if (campo[0] == SEEK_ID) alvo = numero(dados, d, t);
                    else if (campo[0] == SEEK_POSITION) posicao = numero(dados, d, t);
                    q = d + t;
                }
                if (alvo == CHAPTERS && posicao >= 0) return inicioSegmento + posicao;
                if (alvo == SEEK_HEAD && posicao >= 0) outros.add(inicioSegmento + posicao);
            }
            p = fimSeek;
        }
        for (long posicao : outros) {
            Elemento outro = elemento(f, posicao);
            if (outro == null || outro.id != SEEK_HEAD || outro.tamanho <= 0 || outro.tamanho >= LIMITE) continue;
            long achado = procurarNoSeekHead(f, outro, inicioSegmento, lidos);
            if (achado >= 0) return achado;
        }
        return -1;
    }

    /** Capítulos da primeira edição com capítulos (os escondidos ficam de fora). */
    private static Capitulos lerCapitulosMkv(byte[] dados) {
        int p = 0;
        while (p < dados.length) {
            long[] edicao = cabecalho(dados, p);
            if (edicao == null) break;
            int corpo = (int) edicao[2];
            int fimEdicao = (int) Math.min(dados.length, corpo + edicao[1]);
            if (edicao[0] == EDITION_ENTRY) {
                List<Capitulos.Capitulo> lista = new ArrayList<>();
                int q = corpo;
                while (q < fimEdicao) {
                    long[] atomo = cabecalho(dados, q);
                    if (atomo == null) break;
                    int corpoAtomo = (int) atomo[2];
                    int fimAtomo = (int) Math.min(fimEdicao, corpoAtomo + atomo[1]);
                    if (atomo[0] == CHAPTER_ATOM) {
                        Capitulos.Capitulo c = atomoMkv(dados, corpoAtomo, fimAtomo);
                        if (c != null) lista.add(c);
                    }
                    q = fimAtomo;
                }
                if (!lista.isEmpty()) return Capitulos.de(lista);
            }
            p = fimEdicao;
        }
        return Capitulos.nenhum();
    }

    private static Capitulos.Capitulo atomoMkv(byte[] dados, int inicio, int fim) {
        long comeco = -1;
        long termino = -1;
        boolean escondido = false;
        String nome = null;
        int p = inicio;
        while (p < fim) {
            long[] campo = cabecalho(dados, p);
            if (campo == null) break;
            int d = (int) campo[2];
            int t = (int) Math.min(campo[1], fim - d);
            if (campo[0] == CHAPTER_TIME_START) comeco = numero(dados, d, t);
            else if (campo[0] == CHAPTER_TIME_END) termino = numero(dados, d, t);
            else if (campo[0] == CHAPTER_FLAG_HIDDEN) escondido = numero(dados, d, t) != 0;
            else if (campo[0] == CHAPTER_DISPLAY && nome == null) {
                int q = d;
                while (q < d + t) {
                    long[] texto = cabecalho(dados, q);
                    if (texto == null) break;
                    int td = (int) texto[2];
                    int tt = (int) Math.min(texto[1], d + t - td);
                    if (texto[0] == CHAP_STRING) {
                        nome = new String(dados, td, tt, StandardCharsets.UTF_8).replace("\u0000", "").trim();
                        break;
                    }
                    q = td + tt;
                }
            }
            p = d + t;
        }
        if (comeco < 0 || escondido) return null;
        // Tempos do MKV em nanossegundos
        return new Capitulos.Capitulo(0, nome == null ? "" : nome, comeco / 1_000_000L,
                termino < 0 ? -1 : termino / 1_000_000L);
    }

    private static Elemento elemento(RandomAccessFile f, long pos) throws IOException {
        if (pos < 0 || pos + 2 > f.length()) return null;
        byte[] b = new byte[16];
        f.seek(pos);
        int lidos = f.read(b, 0, (int) Math.min(16, f.length() - pos));
        if (lidos < 2) return null;
        long[] c = cabecalho(b, 0);
        if (c == null) return null;
        Elemento e = new Elemento();
        e.id = (int) c[0];
        e.tamanho = c[1];
        e.dados = pos + c[2];
        return e;
    }

    /** {id, tamanho (-1 se desconhecido), início dos dados} do elemento em {@code p}; nulo se não dá. */
    private static long[] cabecalho(byte[] b, int p) {
        if (p >= b.length) return null;
        int n = comprimento(b[p] & 0xFF);
        if (n < 1 || n > 4 || p + n > b.length) return null;
        long id = 0;
        for (int i = 0; i < n; i++) id = (id << 8) | (b[p + i] & 0xFF);
        int q = p + n;
        if (q >= b.length) return null;
        int m = comprimento(b[q] & 0xFF);
        if (m < 1 || m > 8 || q + m > b.length) return null;
        long tamanho = (b[q] & 0xFF) & (0xFF >> m);
        boolean desconhecido = tamanho == (0xFF >> m);
        for (int i = 1; i < m; i++) {
            int v = b[q + i] & 0xFF;
            tamanho = (tamanho << 8) | v;
            if (v != 0xFF) desconhecido = false;
        }
        return new long[]{id, desconhecido ? -1 : tamanho, q + m};
    }

    private static int comprimento(int primeiro) {
        for (int i = 0; i < 8; i++) {
            if ((primeiro & (0x80 >> i)) != 0) return i + 1;
        }
        return 0;
    }

    private static long numero(byte[] b, int p, int n) {
        long v = 0;
        for (int i = 0; i < n && p + i < b.length; i++) v = (v << 8) | (b[p + i] & 0xFF);
        return v;
    }

    // =====================================================================
    // MP4
    // =====================================================================

    private static Capitulos mp4(RandomAccessFile f) throws IOException {
        long[] moov = caixaDeCima(f, "moov");
        if (moov == null || moov[1] > LIMITE) return Capitulos.nenhum();
        byte[] dados = new byte[(int) moov[1]];
        f.seek(moov[0]);
        f.readFully(dados);
        Capitulos nero = nero(dados);
        if (!nero.vazio()) return nero;
        return quickTime(f, dados);
    }

    /** Caixa de primeiro nível: {início dos dados, tamanho dos dados}. */
    private static long[] caixaDeCima(RandomAccessFile f, String tipo) throws IOException {
        long pos = 0;
        long total = f.length();
        byte[] cab = new byte[16];
        for (int i = 0; i < 128 && pos + 8 <= total; i++) {
            f.seek(pos);
            f.readFully(cab, 0, (int) Math.min(16, total - pos));
            long tamanho = u32(cab, 0);
            String nome = new String(cab, 4, 4, StandardCharsets.ISO_8859_1);
            int cabecalho = 8;
            if (tamanho == 1) {
                tamanho = u64(cab, 8);
                cabecalho = 16;
            } else if (tamanho == 0) {
                tamanho = total - pos;
            }
            if (tamanho < cabecalho) return null;
            if (nome.equals(tipo)) return new long[]{pos + cabecalho, tamanho - cabecalho};
            pos += tamanho;
        }
        return null;
    }

    /** Filhas de uma caixa: lista de {tipo, início, tamanho dos dados}. */
    private static List<Object[]> filhas(byte[] b, int inicio, int fim) {
        List<Object[]> lista = new ArrayList<>();
        int p = inicio;
        while (p + 8 <= fim) {
            long tamanho = u32(b, p);
            String tipo = new String(b, p + 4, 4, StandardCharsets.ISO_8859_1);
            int cabecalho = 8;
            if (tamanho == 1) {
                if (p + 16 > fim) break;
                tamanho = u64(b, p + 8);
                cabecalho = 16;
            } else if (tamanho == 0) {
                tamanho = fim - p;
            }
            if (tamanho < cabecalho || p + tamanho > fim) break;
            lista.add(new Object[]{tipo, p + cabecalho, (int) tamanho - cabecalho});
            p += (int) tamanho;
        }
        return lista;
    }

    private static Object[] filha(byte[] b, int inicio, int fim, String tipo) {
        for (Object[] c : filhas(b, inicio, fim)) if (c[0].equals(tipo)) return c;
        return null;
    }

    /** Capítulos Nero: moov/udta/chpl (início em unidades de 100 ns). */
    private static Capitulos nero(byte[] moov) {
        Object[] udta = filha(moov, 0, moov.length, "udta");
        if (udta == null) return Capitulos.nenhum();
        int ui = (int) udta[1];
        Object[] chpl = filha(moov, ui, ui + (int) udta[2], "chpl");
        if (chpl == null) return Capitulos.nenhum();
        int p = (int) chpl[1];
        int fim = p + (int) chpl[2];
        int versao = moov[p] & 0xFF;
        p += 4 + (versao == 1 ? 4 : 0);
        if (p >= fim) return Capitulos.nenhum();
        int quantos = moov[p++] & 0xFF;
        List<Capitulos.Capitulo> lista = new ArrayList<>();
        for (int i = 0; i < quantos && p + 9 <= fim; i++) {
            long comeco = u64(moov, p);
            int n = moov[p + 8] & 0xFF;
            if (p + 9 + n > fim) break;
            String nome = new String(moov, p + 9, n, StandardCharsets.UTF_8).trim();
            lista.add(new Capitulos.Capitulo(0, nome, comeco / 10_000L, -1));
            p += 9 + n;
        }
        return Capitulos.de(lista);
    }

    /** Trilha de capítulos do QuickTime: a trilha apontada por tref/chap, amostras de texto. */
    private static Capitulos quickTime(RandomAccessFile f, byte[] moov) throws IOException {
        long idCapitulos = -1;
        List<Object[]> trilhas = new ArrayList<>();
        for (Object[] c : filhas(moov, 0, moov.length)) if (c[0].equals("trak")) trilhas.add(c);
        for (Object[] trak : trilhas) {
            int ti = (int) trak[1];
            Object[] tref = filha(moov, ti, ti + (int) trak[2], "tref");
            if (tref == null) continue;
            int ri = (int) tref[1];
            Object[] chap = filha(moov, ri, ri + (int) tref[2], "chap");
            if (chap != null && (int) chap[2] >= 4) {
                idCapitulos = u32(moov, (int) chap[1]);
                break;
            }
        }
        if (idCapitulos < 0) return Capitulos.nenhum();
        for (Object[] trak : trilhas) {
            int ti = (int) trak[1];
            int tf = ti + (int) trak[2];
            Object[] tkhd = filha(moov, ti, tf, "tkhd");
            if (tkhd == null) continue;
            int hi = (int) tkhd[1];
            int versao = moov[hi] & 0xFF;
            long id = u32(moov, hi + (versao == 1 ? 20 : 12));
            if (id != idCapitulos) continue;
            return textoDaTrilha(f, moov, ti, tf);
        }
        return Capitulos.nenhum();
    }

    private static Capitulos textoDaTrilha(RandomAccessFile f, byte[] m, int ti, int tf) throws IOException {
        Object[] mdia = filha(m, ti, tf, "mdia");
        if (mdia == null) return Capitulos.nenhum();
        int mi = (int) mdia[1];
        int mf = mi + (int) mdia[2];
        Object[] mdhd = filha(m, mi, mf, "mdhd");
        Object[] minf = filha(m, mi, mf, "minf");
        if (mdhd == null || minf == null) return Capitulos.nenhum();
        int di = (int) mdhd[1];
        long escala = (m[di] & 0xFF) == 1 ? u32(m, di + 20) : u32(m, di + 12);
        if (escala <= 0) return Capitulos.nenhum();
        Object[] stbl = filha(m, (int) minf[1], (int) minf[1] + (int) minf[2], "stbl");
        if (stbl == null) return Capitulos.nenhum();
        int si = (int) stbl[1];
        int sf = si + (int) stbl[2];
        Object[] stts = filha(m, si, sf, "stts");
        Object[] stsz = filha(m, si, sf, "stsz");
        Object[] stsc = filha(m, si, sf, "stsc");
        Object[] stco = filha(m, si, sf, "stco");
        Object[] co64 = filha(m, si, sf, "co64");
        if (stts == null || stsz == null || stsc == null || (stco == null && co64 == null)) return Capitulos.nenhum();

        // Tamanho de cada amostra
        int zi = (int) stsz[1];
        long tamanhoFixo = u32(m, zi + 4);
        int amostras = (int) Math.min(u32(m, zi + 8), 1000);
        long[] tamanhos = new long[amostras];
        for (int i = 0; i < amostras; i++) tamanhos[i] = tamanhoFixo != 0 ? tamanhoFixo : u32(m, zi + 12 + 4 * i);
        // Início de cada amostra (stts)
        long[] inicios = new long[amostras];
        int ei = (int) stts[1];
        long entradas = u32(m, ei + 4);
        long tempo = 0;
        int k = 0;
        for (int e = 0; e < entradas && k < amostras; e++) {
            long quantas = u32(m, ei + 8 + 8 * e);
            long duracao = u32(m, ei + 12 + 8 * e);
            for (long j = 0; j < quantas && k < amostras; j++) {
                inicios[k++] = tempo;
                tempo += duracao;
            }
        }
        // Posição de cada amostra (blocos: stco/co64 + stsc)
        boolean longo = co64 != null;
        int oi = (int) (longo ? co64 : stco)[1];
        int blocos = (int) u32(m, oi + 4);
        int ci = (int) stsc[1];
        long nStsc = u32(m, ci + 4);
        long[] posicoes = new long[amostras];
        int amostra = 0;
        for (int b = 0; b < blocos && amostra < amostras; b++) {
            long porBloco = 1;
            for (int e = 0; e < nStsc; e++) {
                long primeiro = u32(m, ci + 8 + 12 * e);
                if (primeiro <= b + 1) porBloco = u32(m, ci + 12 + 12 * e);
            }
            long pos = longo ? u64(m, oi + 8 + 8 * b) : u32(m, oi + 8 + 4 * b);
            for (long j = 0; j < porBloco && amostra < amostras; j++) {
                posicoes[amostra] = pos;
                pos += tamanhos[amostra];
                amostra++;
            }
        }
        List<Capitulos.Capitulo> lista = new ArrayList<>();
        for (int i = 0; i < amostra; i++) {
            String nome = "";
            if (tamanhos[i] >= 2 && tamanhos[i] < 4096) {
                byte[] texto = new byte[(int) tamanhos[i]];
                f.seek(posicoes[i]);
                f.readFully(texto);
                int n = ((texto[0] & 0xFF) << 8) | (texto[1] & 0xFF);
                if (n > 0 && 2 + n <= texto.length) nome = new String(texto, 2, n, StandardCharsets.UTF_8).trim();
            }
            lista.add(new Capitulos.Capitulo(0, nome, inicios[i] * 1000L / escala, -1));
        }
        return Capitulos.de(lista);
    }

    private static long u32(byte[] b, int p) {
        if (p + 4 > b.length) return 0;
        return ((long) (b[p] & 0xFF) << 24) | ((b[p + 1] & 0xFF) << 16) | ((b[p + 2] & 0xFF) << 8) | (b[p + 3] & 0xFF);
    }

    private static long u64(byte[] b, int p) {
        return (u32(b, p) << 32) | u32(b, p + 4);
    }
}
