// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.motor;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.IntentCompat;
import androidx.core.content.pm.PackageInfoCompat;

import com.archos.mediacenter.video.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Atualização do app sem loja (D11 do plano 004): lê o {@code version.json} das versões publicadas no GitHub,
 * compara o {@code versionCode} com o do app, baixa o APK e o entrega ao Android por {@link PackageInstaller}, sem
 * {@code FileProvider}. Atualiza por cima e mantém os dados, desde que o APK tenha a mesma assinatura; com
 * outra, o Android recusa. O Android sempre pede a confirmação: nada aqui instala sem toque.
 * {@link #versaoNova}, {@link #baixar} e {@link #instalar} usam rede ou disco: só fora da thread da tela.
 */
public final class Atualizador {

    /**
     * Endereço do version.json: o único lugar do código com ele. É a última versão publicada no repositório do
     * app, que é público; enquanto não houver versão publicada, "não deu para verificar". Mudar o endereço só vale
     * para quem instalar depois: quem já tem o app continua no antigo.
     */
    private static final String ENDERECO_VERSION_JSON =
            "https://github.com/Starktech-studio/starktech-media-player/releases/latest/download/version.json";

    private static final String LOG = "StarkTechAtualizador";
    private static final long LIMITE_RESPOSTA = 256L * 1024;

    /** O que o {@code version.json} traz. */
    public static final class Versao {
        public final String nome;
        public final long codigo;
        public final List<String> novidades;
        public final String apk;

        public Versao(String nome, long codigo, List<String> novidades, String apk) {
            this.nome = nome;
            this.codigo = codigo;
            this.novidades = Collections.unmodifiableList(new ArrayList<>(novidades));
            this.apk = apk;
        }
    }

    /** Fim do download. */
    public enum Download { PRONTO, FALHOU, INVALIDO, CANCELADO }

    /** Andamento do download; chamado fora da thread da tela. */
    public interface Andamento {
        /** @param percentual de 0 a 100; -1 se o site não informar o tamanho */
        void aoBaixar(int percentual);
    }

    /** Falha depois da entrega ao Android, na thread da tela. */
    public interface AoFalharInstalacao {
        void aoFalhar(@StringRes int aviso);
    }

    private static AoFalharInstalacao ouvinte;

    private Atualizador() {}

    // ---- Lógica sem Android: testada em AtualizadorTest ----

    /**
     * {@code {"versao": "0.2.0", "versionCode": 2, "novidades": ["..."], "apk": "https://..."}}; o APK só por
     * https. Faltando dado (ou uma página de erro no lugar do JSON), {@link JSONException}.
     */
    static Versao lerVersao(String json) throws JSONException {
        JSONObject raiz = new JSONObject(json);
        String nome = obrigatorio(raiz, "versao");
        long codigo = raiz.getLong("versionCode");
        String apk = obrigatorio(raiz, "apk");
        if (codigo <= 0) throw new JSONException("versionCode inválido");
        if (!apk.startsWith("https://")) throw new JSONException("apk sem https");
        List<String> novidades = new ArrayList<>();
        JSONArray lista = raiz.optJSONArray("novidades");
        if (lista != null) {
            for (int i = 0; i < lista.length(); i++) {
                String novidade = lista.isNull(i) ? "" : lista.optString(i, "").trim();
                if (!novidade.isEmpty()) novidades.add(novidade);
            }
        }
        return new Versao(nome, codigo, novidades, apk);
    }

    private static String obrigatorio(JSONObject raiz, String campo) throws JSONException {
        String valor = raiz.isNull(campo) ? "" : raiz.getString(campo).trim();
        if (valor.isEmpty()) throw new JSONException("version.json sem " + campo);
        return valor;
    }

    static boolean temVersaoNova(long codigoInstalado, long codigoDoSite) {
        return codigoDoSite > codigoInstalado;
    }

    /** O APK baixado é do app, é o que o site prometeu e é mais novo que o instalado. */
    static boolean apkAceito(@Nullable String pacoteApk, long codigoApk, String pacoteApp, long codigoInstalado, long codigoDoSite) {
        return pacoteApp.equals(pacoteApk) && codigoApk == codigoDoSite && temVersaoNova(codigoInstalado, codigoApk);
    }

    /** De 0 a 100; -1 se o tamanho não for conhecido. */
    static int percentual(long baixado, int tamanho) {
        return tamanho > 0 ? (int) Math.min(100, baixado * 100 / tamanho) : -1;
    }

    // ---- Verificar ----

    /** A versão publicada, mais nova ou não que a instalada. Sem resposta: {@link IOException}. */
    public static Versao versaoPublicada() throws IOException {
        try {
            return lerVersao(pedir(ENDERECO_VERSION_JSON));
        } catch (JSONException e) {
            throw new IOException("version.json inválido", e);
        }
    }

    public static boolean ehMaisNova(Context contexto, Versao publicada) {
        return temVersaoNova(codigoInstalado(contexto), publicada.codigo);
    }

    /** A versão publicada, se for mais nova que a instalada; nula se o app está em dia. Sem resposta: {@link IOException}. */
    @Nullable
    public static Versao versaoNova(Context contexto) throws IOException {
        // Sobra de um download anterior (o app é trocado sem passar pelo fechamento do modal)
        apagarApk(contexto);
        Versao publicada = versaoPublicada();
        return ehMaisNova(contexto, publicada) ? publicada : null;
    }

    private static long codigoInstalado(Context contexto) {
        try {
            return PackageInfoCompat.getLongVersionCode(contexto.getPackageManager().getPackageInfo(contexto.getPackageName(), 0));
        } catch (PackageManager.NameNotFoundException e) {
            // Não acontece com o próprio app; se acontecer, não oferece versão nenhuma
            return Long.MAX_VALUE;
        }
    }

    private static String pedir(String endereco) throws IOException {
        HttpURLConnection conexao = null;
        try {
            conexao = (HttpURLConnection) new URL(endereco).openConnection();
            conexao.setConnectTimeout(15_000);
            conexao.setReadTimeout(20_000);
            conexao.setUseCaches(false);
            if (conexao.getResponseCode() != HttpURLConnection.HTTP_OK) throw new IOException("HTTP " + conexao.getResponseCode());
            try (InputStream entrada = conexao.getInputStream()) {
                ByteArrayOutputStream memoria = new ByteArrayOutputStream();
                if (ImagemDoAparelho.copiar(entrada, memoria, LIMITE_RESPOSTA) < 0) throw new IOException("resposta grande demais");
                return memoria.toString("UTF-8");
            }
        } catch (RuntimeException e) {
            throw new IOException("resposta estranha", e);
        } finally {
            if (conexao != null) conexao.disconnect();
        }
    }

    // ---- Liberar a fonte ----

    /** Android 8 em diante pede a liberação por app; antes disso, o próprio Android trata na confirmação. */
    public static boolean fonteLiberada(Context contexto) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O || contexto.getPackageManager().canRequestPackageInstalls();
    }

    /** Tela de liberação deste app; só chamada com {@link #fonteLiberada} falso. A TV pode não ter: tratar a falha ao abrir. */
    @SuppressLint("InlinedApi")
    public static Intent telaDeLiberacao(Context contexto) {
        return new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + contexto.getPackageName()));
    }

    // ---- Baixar ----

    private static File arquivoApk(Context contexto) {
        return new File(new File(contexto.getCacheDir(), "atualizacao"), "atualizacao.apk");
    }

    /** Baixa e confere o APK. O que não chega inteiro ou não passa na conferência não fica no aparelho. */
    public static Download baixar(Context contexto, Versao versao, Andamento andamento, AtomicBoolean cancelado) {
        if (cancelado.get()) return Download.CANCELADO;
        File apk = arquivoApk(contexto);
        // Recusou a confirmação do Android e tentou de novo: o arquivo já está aqui
        if (apkValido(contexto, apk, versao)) return Download.PRONTO;
        File parte = new File(apk.getPath() + ".parte");
        HttpURLConnection conexao = null;
        try {
            File pasta = apk.getParentFile();
            if (pasta == null || (!pasta.isDirectory() && !pasta.mkdirs())) throw new IOException("sem pasta");
            conexao = (HttpURLConnection) new URL(versao.apk).openConnection();
            conexao.setConnectTimeout(15_000);
            conexao.setReadTimeout(30_000);
            conexao.setUseCaches(false);
            // Sem gzip transparente: o site informa o tamanho e dá para mostrar o andamento
            conexao.setRequestProperty("Accept-Encoding", "identity");
            if (conexao.getResponseCode() != HttpURLConnection.HTTP_OK) throw new IOException("HTTP " + conexao.getResponseCode());
            int tamanho = conexao.getContentLength();
            long total = 0;
            int ultimo = Integer.MIN_VALUE;
            try (InputStream entrada = conexao.getInputStream(); OutputStream saida = new FileOutputStream(parte)) {
                byte[] bloco = new byte[64 * 1024];
                int n;
                while ((n = entrada.read(bloco)) != -1) {
                    if (cancelado.get()) return Download.CANCELADO;
                    saida.write(bloco, 0, n);
                    total += n;
                    int agora = percentual(total, tamanho);
                    if (agora != ultimo) {
                        ultimo = agora;
                        andamento.aoBaixar(agora);
                    }
                }
            }
            if (tamanho > 0 && total != tamanho) throw new IOException("download incompleto");
            if (!parte.renameTo(apk)) throw new IOException("sem o arquivo");
        } catch (IOException | RuntimeException e) {
            Log.w(LOG, "download: " + e.getClass().getSimpleName());
            return Download.FALHOU;
        } finally {
            parte.delete();
            if (conexao != null) conexao.disconnect();
        }
        if (apkValido(contexto, apk, versao)) return Download.PRONTO;
        apk.delete();
        return Download.INVALIDO;
    }

    private static boolean apkValido(Context contexto, File apk, Versao versao) {
        if (!apk.isFile()) return false;
        PackageInfo info = contexto.getPackageManager().getPackageArchiveInfo(apk.getPath(), 0);
        return info != null && apkAceito(info.packageName, PackageInfoCompat.getLongVersionCode(info),
                contexto.getPackageName(), codigoInstalado(contexto), versao.codigo);
    }

    /** Apaga o APK baixado: depois da entrega ao Android ele não faz mais falta. */
    public static void apagarApk(Context contexto) {
        arquivoApk(contexto).delete();
    }

    // ---- Instalar ----

    /**
     * Copia o APK baixado para uma sessão do {@link PackageInstaller} e a entrega ao Android, que pede a
     * confirmação ({@link RetornoInstalacao}); {@code false} se não deu para entregar.
     */
    public static boolean instalar(Context contexto, AtomicBoolean cancelado) {
        File apk = arquivoApk(contexto);
        PackageInstaller instalador = contexto.getPackageManager().getPackageInstaller();
        int sessaoId = -1;
        try {
            PackageInstaller.SessionParams parametros = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            parametros.setAppPackageName(contexto.getPackageName());
            parametros.setSize(apk.length());
            sessaoId = instalador.createSession(parametros);
            try (PackageInstaller.Session sessao = instalador.openSession(sessaoId)) {
                try (InputStream entrada = new FileInputStream(apk);
                     OutputStream saida = sessao.openWrite("base.apk", 0, apk.length())) {
                    ImagemDoAparelho.copiar(entrada, saida, Long.MAX_VALUE);
                    sessao.fsync(saida);
                }
                if (cancelado.get()) {
                    abandonar(instalador, sessaoId);
                    return false;
                }
                sessao.commit(retorno(contexto, sessaoId).getIntentSender());
            }
            return true;
        } catch (IOException | RuntimeException e) {
            Log.w(LOG, "instalar: " + e.getClass().getSimpleName());
            abandonar(instalador, sessaoId);
            return false;
        }
    }

    private static void abandonar(PackageInstaller instalador, int sessaoId) {
        if (sessaoId == -1) return;
        try {
            instalador.abandonSession(sessaoId);
        } catch (RuntimeException e) {
            Log.w(LOG, "abandonar: " + e.getClass().getSimpleName());
        }
    }

    private static PendingIntent retorno(Context contexto, int sessaoId) {
        // Mutável: é o Android quem acrescenta o resultado ao intent, que aponta só para o receptor abaixo
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) flags |= PendingIntent.FLAG_MUTABLE;
        return PendingIntent.getBroadcast(contexto, sessaoId, new Intent(contexto, RetornoInstalacao.class), flags);
    }

    /** Só um ouvinte: o modal aberto. Sem ele, o aviso de falha vira Toast. */
    public static void ouvirInstalacao(AoFalharInstalacao novo) {
        ouvinte = novo;
    }

    public static void deixarDeOuvirInstalacao(AoFalharInstalacao antigo) {
        if (ouvinte == antigo) ouvinte = null;
    }

    private static void avisar(Context contexto, @StringRes int aviso) {
        if (ouvinte != null) ouvinte.aoFalhar(aviso);
        else Toast.makeText(contexto, aviso, Toast.LENGTH_LONG).show();
    }

    @StringRes
    private static int avisoDoStatus(int status) {
        switch (status) {
            case PackageInstaller.STATUS_FAILURE_ABORTED:
                return R.string.st_versao_instalacao_cancelada;
            case PackageInstaller.STATUS_FAILURE_CONFLICT:
                return R.string.st_versao_assinatura;
            case PackageInstaller.STATUS_FAILURE_STORAGE:
                return R.string.st_versao_sem_espaco;
            case PackageInstaller.STATUS_FAILURE_INVALID:
                return R.string.st_versao_apk_invalido;
            default:
                return R.string.st_versao_falha_instalar;
        }
    }

    /** Resultado da sessão: abre a confirmação do Android ou avisa a falha. No sucesso, o Android troca o app. */
    public static final class RetornoInstalacao extends BroadcastReceiver {
        @Override
        public void onReceive(Context contexto, Intent intent) {
            int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
            if (status == PackageInstaller.STATUS_SUCCESS) return;
            if (status != PackageInstaller.STATUS_PENDING_USER_ACTION) {
                avisar(contexto, avisoDoStatus(status));
                return;
            }
            Intent confirmar = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent.class);
            if (confirmar == null) {
                avisar(contexto, R.string.st_versao_falha_instalar);
                return;
            }
            confirmar.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                contexto.startActivity(confirmar);
            } catch (ActivityNotFoundException | SecurityException e) {
                Log.w(LOG, "confirmar: " + e.getClass().getSimpleName());
                avisar(contexto, R.string.st_versao_falha_instalar);
            }
        }
    }
}
