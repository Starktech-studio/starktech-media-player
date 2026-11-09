// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Edição à mão dos dados de um título (F2, etapa 2), guardada no {@link BancoStarkTech}: título,
 * título original, tipo, ano, duração, gêneros, sinopse, capa, fundo e coletânea. Campo salvo =
 * "editado à mão"; Restaurar original apaga a edição do título (e as imagens do aparelho que só ela
 * usava). Ao juntar com os dados do TMDB ({@link #juntar}), o que foi editado sempre vale: a busca no
 * TMDB nunca sobrescreve um campo editado.
 *
 * <p>A chave do título é a do {@link Acervo}: o caminho do filme / show, ou {@link Acervo#chaveSerie}
 * na série. Capa e fundo guardam uma referência: {@code cor:#RRGGBB} (a cor escolhida no Editar) ou
 * {@code arquivo:<caminho>} (imagem do aparelho ou opção do TMDB, copiadas para a pasta do app pela
 * {@link ImagemDoAparelho}).
 *
 * <p>Nome em comum (D13): quem edita guarda junto o {@link Classificador#chaveNome}, e a edição vale
 * também para os outros vídeos de mesmo nome, mesmo depois que o arquivo editado sair da biblioteca
 * ({@link #valendo}). A do próprio vídeo vale mais que a herdada; entre herdadas, a mais recente de
 * cada campo. Restaurar original corta a herança só daquele vídeo.
 */
public final class Edicoes {

    public enum Campo { TITULO, TITULO_ORIGINAL, TIPO, ANO, DURACAO, GENEROS, SINOPSE, CAPA, FUNDO, COLECAO, ARTISTA }

    public static final String PREFIXO_COR = "cor:";
    public static final String PREFIXO_ARQUIVO = "arquivo:";

    /** Linha que marca o vídeo que não herda mais: fica fora de {@link Campo} e de {@link #ler}. */
    private static final String SEM_HERANCA = "SEM_HERANCA";

    private static Edicoes instancia;

    private final BancoStarkTech banco;
    /** Pasta das imagens copiadas do aparelho: só estas são apagadas ao restaurar. */
    private final File pastaImagens;
    /** Tabela inteira na memória (é pequena): as listas aplicam a edição de cada título. */
    private Map<String, Map<Campo, String>> cache;
    /** Quando cada campo foi editado, por título. */
    private Map<String, Map<Campo, Long>> alterados;
    /** Nome em comum → títulos editados com ele. */
    private Map<String, Set<String>> porNome;
    /** Títulos que não herdam mais. */
    private Set<String> semHeranca;

    public static synchronized Edicoes de(Context contexto) {
        if (instancia == null) {
            instancia = new Edicoes(BancoStarkTech.de(contexto), ImagemDoAparelho.pasta(contexto));
        }
        return instancia;
    }

    Edicoes(BancoStarkTech banco, File pastaImagens) {
        this.banco = banco;
        this.pastaImagens = pastaImagens;
    }

    /** Campos editados do título (vazio se nenhum). */
    public synchronized Map<Campo, String> ler(String chave) {
        Map<Campo, String> valores = carregar().get(chave);
        return valores == null ? Collections.emptyMap() : Collections.unmodifiableMap(new EnumMap<>(valores));
    }

    /**
     * O que vale para o vídeo (D13): as edições herdadas de outros vídeos de mesmo {@code nome} (de
     * cada campo, a mais recente), com as dele por cima. {@code nome} nulo ou herança cortada: só as dele.
     */
    public synchronized Map<Campo, String> valendo(String chave, String nome) {
        Map<String, Map<Campo, String>> todas = carregar();
        Map<Campo, String> resultado = new EnumMap<>(Campo.class);
        Set<String> mesmoNome = nome == null || semHeranca.contains(chave) ? null : porNome.get(nome);
        if (mesmoNome != null) {
            Map<Campo, Long> maisRecente = new EnumMap<>(Campo.class);
            for (String outra : mesmoNome) {
                Map<Campo, String> valores = todas.get(outra);
                if (valores == null || outra.equals(chave)) continue;
                Map<Campo, Long> datas = alterados.get(outra);
                for (Map.Entry<Campo, String> item : valores.entrySet()) {
                    Long data = datas == null ? null : datas.get(item.getKey());
                    long esta = data == null ? 0 : data;
                    Long melhor = maisRecente.get(item.getKey());
                    if (melhor == null || esta > melhor) {
                        maisRecente.put(item.getKey(), esta);
                        resultado.put(item.getKey(), item.getValue());
                    }
                }
            }
        }
        Map<Campo, String> proprias = todas.get(chave);
        if (proprias != null) resultado.putAll(proprias);
        return Collections.unmodifiableMap(resultado);
    }

    /** Todos os títulos que têm o campo: chave → valor. */
    public synchronized Map<String, String> doCampo(Campo campo) {
        Map<String, String> valores = new java.util.HashMap<>();
        for (Map.Entry<String, Map<Campo, String>> titulo : carregar().entrySet()) {
            String valor = titulo.getValue().get(campo);
            if (valor != null) valores.put(titulo.getKey(), valor);
        }
        return valores;
    }

    public synchronized boolean editado(String chave, Campo campo) {
        Map<Campo, String> valores = carregar().get(chave);
        return valores != null && valores.containsKey(campo);
    }

    public void salvar(String chave, Map<Campo, String> novos) {
        salvar(chave, null, novos);
    }

    /**
     * Grava os campos dados como editados à mão (os outros ficam como estão); valor nulo tira a
     * edição daquele campo. Imagem do aparelho trocada por outra: a antiga é apagada. {@code nome}: o
     * nome em comum do vídeo ({@link Classificador#chaveNome}); nulo = não passa para outros vídeos.
     */
    public void salvar(String chave, String nome, Map<Campo, String> novos) {
        salvar(chave, nome, novos, System.currentTimeMillis());
    }

    synchronized void salvar(String chave, String nome, Map<Campo, String> novos, long agora) {
        if (chave == null || novos == null || novos.isEmpty()) return;
        Map<Campo, String> atuais = carregar().get(chave);
        if (atuais == null) atuais = new EnumMap<>(Campo.class);
        Map<Campo, Long> datas = alterados.get(chave);
        if (datas == null) {
            datas = new EnumMap<>(Campo.class);
            alterados.put(chave, datas);
        }
        SQLiteDatabase escrita = banco.getWritableDatabase();
        escrita.beginTransaction();
        try {
            for (Map.Entry<Campo, String> item : novos.entrySet()) {
                Campo campo = item.getKey();
                String valor = item.getValue();
                String[] onde = {chave, campo.name()};
                if (valor == null) {
                    escrita.delete(BancoStarkTech.EDICAO, BancoStarkTech.CHAVE + " = ? AND " + BancoStarkTech.CAMPO + " = ?", onde);
                } else {
                    ContentValues linha = new ContentValues();
                    linha.put(BancoStarkTech.CHAVE, chave);
                    linha.put(BancoStarkTech.CAMPO, campo.name());
                    linha.put(BancoStarkTech.VALOR, valor);
                    linha.put(BancoStarkTech.ALTERADO, agora);
                    linha.put(BancoStarkTech.NOME_COMUM, nome);
                    escrita.insertWithOnConflict(BancoStarkTech.EDICAO, null, linha, SQLiteDatabase.CONFLICT_REPLACE);
                }
            }
            escrita.setTransactionSuccessful();
        } finally {
            escrita.endTransaction();
        }
        Map<Campo, String> antes = new EnumMap<>(atuais);
        for (Map.Entry<Campo, String> item : novos.entrySet()) {
            if (item.getValue() == null) {
                atuais.remove(item.getKey());
                datas.remove(item.getKey());
            } else {
                atuais.put(item.getKey(), item.getValue());
                datas.put(item.getKey(), agora);
            }
        }
        if (atuais.isEmpty()) cache.remove(chave);
        else cache.put(chave, atuais);
        if (nome != null) guardarNome(nome, chave);
        for (Campo imagem : new Campo[]{Campo.CAPA, Campo.FUNDO}) {
            String velha = antes.get(imagem);
            if (velha != null && !velha.equals(atuais.get(imagem))) apagarSeSemUso(velha);
        }
    }

    public void salvar(String chave, Campo campo, String valor) {
        salvar(chave, null, campo, valor);
    }

    public void salvar(String chave, String nome, Campo campo, String valor) {
        Map<Campo, String> um = new EnumMap<>(Campo.class);
        um.put(campo, valor);
        salvar(chave, nome, um);
    }

    /** Restaurar original: apaga toda a edição do título e as imagens do aparelho que só ele usava. */
    public synchronized void restaurarOriginal(String chave) {
        if (chave == null) return;
        Map<Campo, String> antes = carregar().remove(chave);
        alterados.remove(chave);
        semHeranca.remove(chave);
        banco.getWritableDatabase().delete(BancoStarkTech.EDICAO, BancoStarkTech.CHAVE + " = ?", new String[]{chave});
        if (antes == null) return;
        for (Campo imagem : new Campo[]{Campo.CAPA, Campo.FUNDO}) {
            String velha = antes.get(imagem);
            if (velha != null) apagarSeSemUso(velha);
        }
    }

    /**
     * Restaurar original de um vídeo com nome em comum: além de apagar a edição dele, corta a herança
     * só dele (os outros de mesmo nome seguem como estão; os que herdavam dele deixam de ter o que herdar).
     */
    public synchronized void restaurarOriginal(String chave, String nome) {
        restaurarOriginal(chave);
        if (nome != null) cortarHeranca(chave);
    }

    /** O vídeo deixa de herdar edições de outros (a dele continua valendo). */
    public synchronized void cortarHeranca(String chave) {
        if (chave == null) return;
        carregar();
        if (!semHeranca.add(chave)) return;
        ContentValues linha = new ContentValues();
        linha.put(BancoStarkTech.CHAVE, chave);
        linha.put(BancoStarkTech.CAMPO, SEM_HERANCA);
        linha.put(BancoStarkTech.VALOR, "1");
        linha.put(BancoStarkTech.ALTERADO, System.currentTimeMillis());
        banco.getWritableDatabase().insertWithOnConflict(BancoStarkTech.EDICAO, null, linha, SQLiteDatabase.CONFLICT_REPLACE);
    }

    /**
     * Dados do título com a edição por cima: os campos editados substituem os de {@code base}
     * (original ou resultado do TMDB). É a regra "o TMDB não sobrescreve o que foi editado".
     */
    public static Map<Campo, String> juntar(Map<Campo, String> base, Map<Campo, String> edicao) {
        Map<Campo, String> resultado = new EnumMap<>(Campo.class);
        if (base != null) resultado.putAll(base);
        if (edicao != null) resultado.putAll(edicao);
        return resultado;
    }

    /** Caminho da imagem do aparelho guardada na referência; nulo se não for uma. */
    public static String arquivoDe(String referencia) {
        return referencia != null && referencia.startsWith(PREFIXO_ARQUIVO)
                ? referencia.substring(PREFIXO_ARQUIVO.length()) : null;
    }

    private Map<String, Map<Campo, String>> carregar() {
        if (cache != null) return cache;
        cache = new HashMap<>();
        alterados = new HashMap<>();
        porNome = new HashMap<>();
        semHeranca = new HashSet<>();
        try (Cursor linhas = banco.getReadableDatabase().query(BancoStarkTech.EDICAO,
                new String[]{BancoStarkTech.CHAVE, BancoStarkTech.CAMPO, BancoStarkTech.VALOR, BancoStarkTech.ALTERADO, BancoStarkTech.NOME_COMUM},
                null, null, null, null, null)) {
            while (linhas.moveToNext()) {
                String chave = linhas.getString(0);
                if (SEM_HERANCA.equals(linhas.getString(1))) {
                    semHeranca.add(chave);
                    continue;
                }
                Campo campo;
                try {
                    campo = Campo.valueOf(linhas.getString(1));
                } catch (IllegalArgumentException e) {
                    continue; // campo de uma versão futura: ignora
                }
                Map<Campo, String> valores = cache.get(chave);
                if (valores == null) {
                    valores = new EnumMap<>(Campo.class);
                    cache.put(chave, valores);
                    alterados.put(chave, new EnumMap<>(Campo.class));
                }
                valores.put(campo, linhas.getString(2));
                alterados.get(chave).put(campo, linhas.getLong(3));
                if (!linhas.isNull(4)) guardarNome(linhas.getString(4), chave);
            }
        }
        return cache;
    }

    private void guardarNome(String nome, String chave) {
        Set<String> chaves = porNome.get(nome);
        if (chaves == null) {
            chaves = new HashSet<>();
            porNome.put(nome, chaves);
        }
        chaves.add(chave);
    }

    /** Apaga a imagem copiada do aparelho se nenhum outro título a usa (só dentro da pasta do app). */
    private void apagarSeSemUso(String referencia) {
        String caminho = arquivoDe(referencia);
        if (caminho == null || pastaImagens == null) return;
        File arquivo = new File(caminho);
        File pasta = arquivo.getParentFile();
        if (pasta == null || !pasta.getAbsolutePath().equals(pastaImagens.getAbsolutePath())) return;
        for (Map<Campo, String> valores : carregar().values()) {
            if (valores.containsValue(referencia)) return;
        }
        //noinspection ResultOfMethodCallIgnored
        arquivo.delete();
    }
}
