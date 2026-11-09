// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import com.archos.medialib.IMediaMetadataRetriever;
import com.archos.medialib.IMediaPlayer;
import com.archos.medialib.MediaMetadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Capítulos lidos do arquivo pelo motor (F2, etapa 1): nome, início e fim em ms, na ordem do
 * vídeo. Vêm dos metadados do player ({@link IMediaPlayer#getMediaMetadata}) ou do leitor de
 * dados, sem tocar o vídeo ({@link IMediaMetadataRetriever#extractMetadata}); os dois usam as
 * mesmas chaves ({@code METADATA_KEY_CHAPTER*}). Navegação: ir para um capítulo, anterior (D1) e
 * próximo. Sem capítulos no arquivo a lista fica vazia; para o filme, o player pode usar
 * {@link #aCadaDezMinutos}, que nunca vai para a biblioteca nem para o Detalhe.
 */
public final class Capitulos {

    /** D1: até 3 s depois do início do capítulo, "anterior" vai ao de antes; depois, volta ao início deste. */
    public static final long TOLERANCIA_ANTERIOR_MS = 3000;
    /**
     * Ir para um capítulo cai no quadro-chave mais próximo, às vezes um pouco antes do início:
     * até 1,5 s antes ainda conta como dentro dele (senão "próximo" repetiria o mesmo capítulo).
     */
    static final long PRECISAO_MS = 1500;
    /** Distância entre as marcas do player em filme sem capítulos. */
    static final long INTERVALO_MARCAS_MS = 10 * 60_000L;
    /** Marca a menos disso do fim não entra: seria um capítulo que acaba logo em seguida. */
    static final long FOLGA_FIM_MS = 60_000L;

    /** Um capítulo do arquivo; {@code numero} começa em 1. */
    public static final class Capitulo {
        public final int numero;
        /** Vazio quando o arquivo não dá nome ao capítulo. */
        public final String nome;
        public final long inicioMs;
        public final long fimMs;

        Capitulo(int numero, String nome, long inicioMs, long fimMs) {
            this.numero = numero;
            this.nome = nome;
            this.inicioMs = inicioMs;
            this.fimMs = fimMs;
        }
    }

    /** Um valor dos metadados, por chave (player ou leitor de dados). */
    interface Fonte {
        boolean tem(int chave);
        String texto(int chave);
        long numero(int chave);
    }

    private static final Capitulos VAZIO = new Capitulos(Collections.emptyList());

    private final List<Capitulo> lista;

    private Capitulos(List<Capitulo> lista) {
        this.lista = lista;
    }

    /** Lista sem capítulos (arquivo sem capítulos ou player sem metadados). */
    public static Capitulos nenhum() {
        return VAZIO;
    }

    /** Do player: metadados de {@link IMediaPlayer#getMediaMetadata}. */
    public static Capitulos doPlayer(MediaMetadata dados) {
        if (dados == null) return VAZIO;
        return ler(new Fonte() {
            @Override
            public boolean tem(int chave) {
                return dados.has(chave);
            }

            @Override
            public String texto(int chave) {
                return dados.getString(chave);
            }

            @Override
            public long numero(int chave) {
                return dados.getInt(chave);
            }
        });
    }

    /** Do leitor de dados (sem tocar o vídeo): os valores vêm como texto. */
    public static Capitulos doLeitor(IMediaMetadataRetriever leitor) {
        if (leitor == null) return VAZIO;
        return ler(new Fonte() {
            @Override
            public boolean tem(int chave) {
                return leitor.extractMetadata(chave) != null;
            }

            @Override
            public String texto(int chave) {
                return leitor.extractMetadata(chave);
            }

            @Override
            public long numero(int chave) {
                try {
                    return Long.parseLong(leitor.extractMetadata(chave).trim());
                } catch (RuntimeException e) {
                    return -1;
                }
            }
        });
    }

    /** Lê os capítulos pelas chaves do motor; para no primeiro sem início. */
    static Capitulos ler(Fonte fonte) {
        if (!fonte.tem(IMediaPlayer.METADATA_KEY_NB_CHAPTER)) return VAZIO;
        int quantos = (int) fonte.numero(IMediaPlayer.METADATA_KEY_NB_CHAPTER);
        List<Capitulo> lidos = new ArrayList<>();
        for (int i = 0; i < quantos; i++) {
            int base = IMediaPlayer.METADATA_KEY_CHAPTER + i * IMediaPlayer.METADATA_KEY_CHAPTER_MAX;
            int chaveInicio = base + IMediaPlayer.METADATA_KEY_CHAPTER_START;
            if (!fonte.tem(chaveInicio)) break;
            long inicio = fonte.numero(chaveInicio);
            if (inicio < 0) break;
            int chaveFim = base + IMediaPlayer.METADATA_KEY_CHAPTER_END;
            long fim = fonte.tem(chaveFim) ? fonte.numero(chaveFim) : -1;
            int chaveNome = base + IMediaPlayer.METADATA_KEY_CHAPTER_TITLE;
            String nome = fonte.tem(chaveNome) ? fonte.texto(chaveNome) : null;
            lidos.add(new Capitulo(0, nome == null ? "" : nome.trim(), inicio, fim));
        }
        return de(lidos);
    }

    /** Ordena pelo início, numera de 1 em diante e completa o fim que faltar com o início do seguinte. */
    static Capitulos de(List<Capitulo> lidos) {
        if (lidos.isEmpty()) return VAZIO;
        List<Capitulo> ordem = new ArrayList<>(lidos);
        Collections.sort(ordem, (a, b) -> Long.compare(a.inicioMs, b.inicioMs));
        List<Capitulo> lista = new ArrayList<>(ordem.size());
        for (int i = 0; i < ordem.size(); i++) {
            Capitulo c = ordem.get(i);
            long fim = c.fimMs;
            if (fim <= c.inicioMs && i + 1 < ordem.size()) fim = ordem.get(i + 1).inicioMs;
            lista.add(new Capitulo(i + 1, c.nome, c.inicioMs, Math.max(fim, c.inicioMs)));
        }
        return new Capitulos(Collections.unmodifiableList(lista));
    }

    /**
     * Marcas de 10 em 10 minutos para o filme sem capítulos: "Início", "10 min", "1 h 10 min"…, a
     * partir do começo do vídeo. Vídeo curto demais para ter uma segunda marca: nenhuma.
     */
    public static Capitulos aCadaDezMinutos(long duracaoMs) {
        if (duracaoMs <= INTERVALO_MARCAS_MS + FOLGA_FIM_MS) return VAZIO;
        List<Capitulo> marcas = new ArrayList<>();
        for (long inicio = 0; inicio + FOLGA_FIM_MS < duracaoMs; inicio += INTERVALO_MARCAS_MS) {
            marcas.add(new Capitulo(0, nomeDaMarca(inicio), inicio, Math.min(inicio + INTERVALO_MARCAS_MS, duracaoMs)));
        }
        return de(marcas);
    }

    private static String nomeDaMarca(long inicioMs) {
        long minutos = inicioMs / 60_000;
        if (minutos == 0) return "Início";
        long horas = minutos / 60;
        long resto = minutos % 60;
        if (horas == 0) return resto + " min";
        return resto == 0 ? horas + " h" : horas + " h " + resto + " min";
    }

    /** Para guardar no banco (F3): uma linha por capítulo, "início\tfim\tnome" em ms. */
    public String paraTexto() {
        StringBuilder texto = new StringBuilder();
        for (Capitulo c : lista) {
            if (texto.length() > 0) texto.append('\n');
            texto.append(c.inicioMs).append('\t').append(c.fimMs).append('\t')
                    .append(c.nome.replace('\n', ' ').replace('\t', ' '));
        }
        return texto.toString();
    }

    /** O contrário de {@link #paraTexto()}; linha que não dá para ler fica de fora. */
    public static Capitulos deTexto(String texto) {
        if (texto == null || texto.isEmpty()) return VAZIO;
        List<Capitulo> lidos = new ArrayList<>();
        for (String linha : texto.split("\n")) {
            String[] partes = linha.split("\t", 3);
            if (partes.length < 2) continue;
            try {
                lidos.add(new Capitulo(0, partes.length > 2 ? partes[2] : "",
                        Long.parseLong(partes[0]), Long.parseLong(partes[1])));
            } catch (NumberFormatException e) {
                // linha estragada: ignora
            }
        }
        return de(lidos);
    }

    public boolean vazio() {
        return lista.isEmpty();
    }

    public int quantidade() {
        return lista.size();
    }

    public List<Capitulo> lista() {
        return lista;
    }

    /** Capítulo em que a posição está; {@code null} antes do primeiro ou sem capítulos. */
    public Capitulo naPosicao(long posicaoMs) {
        Capitulo atual = null;
        for (Capitulo c : lista) {
            if (c.inicioMs <= posicaoMs + PRECISAO_MS) atual = c;
            else break;
        }
        return atual;
    }

    /** Início do capítulo {@code numero} (1 em diante); -1 se não existe. */
    public long irPara(int numero) {
        return numero >= 1 && numero <= lista.size() ? lista.get(numero - 1).inicioMs : -1;
    }

    /**
     * Destino de "anterior" (D1): o início do capítulo atual se já passou de 3 s dele; senão, o
     * início do capítulo de antes (no primeiro, o início dele; antes do primeiro, 0). -1 sem capítulos.
     */
    public long anterior(long posicaoMs) {
        if (lista.isEmpty()) return -1;
        Capitulo atual = naPosicao(posicaoMs);
        if (atual == null) return 0;
        if (atual.numero == 1 || posicaoMs - atual.inicioMs > TOLERANCIA_ANTERIOR_MS) return atual.inicioMs;
        return lista.get(atual.numero - 2).inicioMs;
    }

    /** Destino de "próximo": o início do capítulo seguinte; -1 no último ou sem capítulos. */
    public long proximo(long posicaoMs) {
        for (Capitulo c : lista) {
            if (c.inicioMs > posicaoMs + PRECISAO_MS) return c.inicioMs;
        }
        return -1;
    }
}
