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
import java.util.Map;

/**
 * Edição à mão dos dados de um título (F2, etapa 2), guardada no {@link BancoStarkTech}: título,
 * título original, tipo, ano, duração, gêneros, sinopse, capa e fundo. Campo salvo = "editado à
 * mão"; Restaurar original apaga a edição do título (e as imagens do aparelho que só ela usava).
 * Ao juntar com os dados do TMDB ({@link #juntar}), o que foi editado sempre vale: a busca no
 * TMDB nunca sobrescreve um campo editado.
 *
 * <p>A chave do título é a do arquivo na biblioteca (F3: caminho do filme / show ou da pasta da
 * série; na F2, o id dos dados simulados). Capa e fundo guardam uma referência: {@code cor:#RRGGBB}
 * (opções simuladas da F1), {@code arquivo:<caminho>} (imagem do aparelho, copiada pela
 * {@link ImagemDoAparelho}) ou, na F3, a imagem escolhida do TMDB.
 */
public final class Edicoes {

    public enum Campo { TITULO, TITULO_ORIGINAL, TIPO, ANO, DURACAO, GENEROS, SINOPSE, CAPA, FUNDO }

    public static final String PREFIXO_COR = "cor:";
    public static final String PREFIXO_ARQUIVO = "arquivo:";

    private static Edicoes instancia;

    private final BancoStarkTech banco;
    /** Pasta das imagens copiadas do aparelho: só estas são apagadas ao restaurar. */
    private final File pastaImagens;
    /** Tabela inteira na memória (é pequena): as listas aplicam a edição de cada título. */
    private Map<String, Map<Campo, String>> cache;

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

    public synchronized boolean editado(String chave, Campo campo) {
        Map<Campo, String> valores = carregar().get(chave);
        return valores != null && valores.containsKey(campo);
    }

    /**
     * Grava os campos dados como editados à mão (os outros ficam como estão); valor nulo tira a
     * edição daquele campo. Imagem do aparelho trocada por outra: a antiga é apagada.
     */
    public synchronized void salvar(String chave, Map<Campo, String> novos) {
        if (chave == null || novos == null || novos.isEmpty()) return;
        Map<Campo, String> atuais = carregar().get(chave);
        if (atuais == null) atuais = new EnumMap<>(Campo.class);
        SQLiteDatabase escrita = banco.getWritableDatabase();
        long agora = System.currentTimeMillis();
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
                    escrita.insertWithOnConflict(BancoStarkTech.EDICAO, null, linha, SQLiteDatabase.CONFLICT_REPLACE);
                }
            }
            escrita.setTransactionSuccessful();
        } finally {
            escrita.endTransaction();
        }
        Map<Campo, String> antes = new EnumMap<>(atuais);
        for (Map.Entry<Campo, String> item : novos.entrySet()) {
            if (item.getValue() == null) atuais.remove(item.getKey());
            else atuais.put(item.getKey(), item.getValue());
        }
        if (atuais.isEmpty()) cache.remove(chave);
        else cache.put(chave, atuais);
        for (Campo imagem : new Campo[]{Campo.CAPA, Campo.FUNDO}) {
            String velha = antes.get(imagem);
            if (velha != null && !velha.equals(atuais.get(imagem))) apagarSeSemUso(velha);
        }
    }

    public void salvar(String chave, Campo campo, String valor) {
        Map<Campo, String> um = new EnumMap<>(Campo.class);
        um.put(campo, valor);
        salvar(chave, um);
    }

    /** Restaurar original: apaga toda a edição do título e as imagens do aparelho que só ele usava. */
    public synchronized void restaurarOriginal(String chave) {
        if (chave == null) return;
        Map<Campo, String> antes = carregar().remove(chave);
        banco.getWritableDatabase().delete(BancoStarkTech.EDICAO, BancoStarkTech.CHAVE + " = ?", new String[]{chave});
        if (antes == null) return;
        for (Campo imagem : new Campo[]{Campo.CAPA, Campo.FUNDO}) {
            String velha = antes.get(imagem);
            if (velha != null) apagarSeSemUso(velha);
        }
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
        try (Cursor linhas = banco.getReadableDatabase().query(BancoStarkTech.EDICAO,
                new String[]{BancoStarkTech.CHAVE, BancoStarkTech.CAMPO, BancoStarkTech.VALOR},
                null, null, null, null, null)) {
            while (linhas.moveToNext()) {
                Campo campo;
                try {
                    campo = Campo.valueOf(linhas.getString(1));
                } catch (IllegalArgumentException e) {
                    continue; // campo de uma versão futura: ignora
                }
                Map<Campo, String> valores = cache.get(linhas.getString(0));
                if (valores == null) {
                    valores = new EnumMap<>(Campo.class);
                    cache.put(linhas.getString(0), valores);
                }
                valores.put(campo, linhas.getString(2));
            }
        }
        return cache;
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
