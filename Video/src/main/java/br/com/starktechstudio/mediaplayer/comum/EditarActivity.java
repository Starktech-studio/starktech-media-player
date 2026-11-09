// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.Layout;
import android.text.TextWatcher;
import android.text.method.ArrowKeyMovementMethod;
import android.text.method.KeyListener;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.ColorRes;
import androidx.annotation.DimenRes;
import androidx.annotation.FontRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.ImageViewCompat;

import com.archos.mediacenter.video.R;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import br.com.starktechstudio.mediaplayer.dados.Modelo.Campo;
import br.com.starktechstudio.mediaplayer.dados.Modelo.PreviaTmdb;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Tipo;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Titulo;
import br.com.starktechstudio.mediaplayer.dados.RepositorioBiblioteca;
import br.com.starktechstudio.mediaplayer.motor.Acervo;
import br.com.starktechstudio.mediaplayer.motor.Edicoes;
import br.com.starktechstudio.mediaplayer.motor.ImagemDoAparelho;

/**
 * Editar (tv-editar.dc.html, tablet-editar.dc.html), igual nos dois aparelhos com o layout de
 * cada um: capa e fundo escolhidos entre as opções do TMDB, a escolhida em destaque e as outras
 * embaixo, ou uma imagem do aparelho ("Do aparelho" no tablet, "Do HD" na TV: o
 * {@link NavegadorImagensActivity} já na pasta de imagens das Configurações; JPG, PNG ou WebP até
 * 15 MB, copiada pela {@link ImagemDoAparelho}); campos Título, Título original, Tipo, Ano, Duração, Gêneros e Sinopse;
 * Buscar no TMDB, Restaurar original e Salvar. Salvar marca os campos mudados como "editados à mão"
 * (o Detalhe passa a mostrar "corrigidos à mão"). O resultado escolhido em Buscar no TMDB preenche
 * a tela na hora (prévia, sem gravar: Salvar grava, fechar descarta); os campos mexidos nesta tela ou
 * já editados à mão ficam como estão. Na TV, o foco só realça a caixa de texto: OK entra, seleciona
 * o texto e abre o teclado. Filme: Coletânea (D5), para mover a outra, tirar dela ou voltar ao TMDB.
 * Capa de coletânea e de artista: só o nome, a capa e o fundo, sem Buscar no TMDB. Capa de artista:
 * as fotos do Deezer e da Wikipédia entram nas opções da capa, e "Buscar artista" troca o artista: com
 * um que já está na biblioteca, os shows dos dois ficam juntos ("Queen + Adam Lambert" em "Queen").
 */
public class EditarActivity extends AppCompatActivity {

    /** TV, caixa só em foco: nenhuma tecla digita e o tipo nulo deixa o teclado do sistema de fora. */
    private static final KeyListener SEM_TECLAS = new KeyListener() {
        @Override
        public int getInputType() {
            return InputType.TYPE_NULL;
        }

        @Override
        public boolean onKeyDown(View view, Editable text, int keyCode, KeyEvent event) {
            return false;
        }

        @Override
        public boolean onKeyUp(View view, Editable text, int keyCode, KeyEvent event) {
            return false;
        }

        @Override
        public boolean onKeyOther(View view, Editable text, KeyEvent event) {
            return false;
        }

        @Override
        public void clearMetaKeyState(View view, Editable content, int states) {}
    };

    private RepositorioBiblioteca repositorio;
    private String id;
    private boolean tv;
    private final Map<Campo, EditText> campos = new EnumMap<>(Campo.class);
    /** Campos mexidos nesta tela e ainda não salvos. */
    private final Set<Campo> mexidos = EnumSet.noneOf(Campo.class);
    private final List<TextView> segmentos = new ArrayList<>();
    private Tipo tipo;
    /**
     * Capa e fundo escolhidos: referência da Edicoes — {@code arquivo:} (TMDB já baixada ou imagem
     * do aparelho), {@code url:} (outra opção do TMDB, baixada ao salvar) ou {@code cor:}.
     */
    private String capa;
    private String fundo;
    private String capaSalva;
    private String fundoSalvo;
    /** Capa / fundo trocados nesta tela: a volta de outra tela (seletor, TMDB) não desfaz. */
    private boolean capaMexida;
    private boolean fundoMexido;
    private boolean salvou;
    private boolean salvando;
    /**
     * Mudanças que só valem no Salvar: o resultado escolhido em "Buscar no TMDB" (id da busca e a
     * prévia, quando chega da rede) e o Restaurar original (a tela mostra a prévia, sem a edição à mão).
     */
    private String resultadoTmdb;
    private String resultadoTmdbTexto;
    @Nullable
    private PreviaTmdb previa;
    private boolean restaurar;
    /** Capa de coletânea ou de artista, não um título. */
    private boolean capaAgrupada;
    private boolean ehArtista;
    /** Artista escolhido em "Buscar artista": o nome dele, o que aparece no aviso e a foto; só vale no Salvar. */
    @Nullable
    private String artistaEscolhido;
    private String artistaEscolhidoTexto;
    @Nullable
    private String fotoDoArtistaEscolhido;
    /** Coletânea do filme: a caixa e a linha dela; nulas na capa de coletânea e de artista. */
    @Nullable
    private EditText caixaColetanea;
    @Nullable
    private View linhaColetanea;
    /** A coletânea mudou nesta tela; só vale no Salvar. */
    private boolean coletaneaMexida;
    /** O que o Salvar grava é "voltar ao TMDB", não o nome da caixa. */
    private boolean coletaneaDoTmdb;
    private ActivityResultLauncher<Intent> escolherColetanea;
    private TextView avisoPendente;
    private ActivityResultLauncher<Intent> buscaTmdb;
    /** Imagens copiadas nesta tela: as que não forem salvas são apagadas ao sair. */
    private final List<String> importadas = new ArrayList<>();
    private final ExecutorService copia = Executors.newSingleThreadExecutor();
    private ActivityResultLauncher<Intent> navegadorDoApp;
    /** O pedido de imagem em andamento é da capa (senão, do fundo). */
    private boolean pedidoDaCapa;
    /** Opções em fileira (para o foco da TV entre capa e fundo). */
    private final List<View> opcoesCapa = new ArrayList<>();
    private final List<View> opcoesFundo = new ArrayList<>();
    private int porFileiraCapa = 1;
    private int porFileiraFundo = 1;
    /** Capa de artista: as fotos do Deezer, depois que chegam ({@link #buscarFotosDoArtista}). */
    private List<String> fotosDoArtista = new ArrayList<>();
    private boolean carregando;
    /** TV: a caixa de texto aberta para digitar (sem nenhuma, o foco só realça e as setas andam). */
    @Nullable
    private EditText editando;
    /** O teclado da caixa aberta já apareceu: quando sumir, a edição acabou. */
    private boolean tecladoVisto;
    /** TV, caixa aberta sem teclado na tela: Voltar fecha a caixa, não a tela (fechar descarta). */
    private final OnBackPressedCallback voltarDaCaixa = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            if (editando != null) sairDaCaixa(editando);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(Aparelho.tema(this));
        super.onCreate(savedInstanceState);
        tv = Aparelho.ehTv(this);
        // Buscar no TMDB devolve o resultado escolhido: a tela preenche na hora; o banco só muda no Salvar
        buscaTmdb = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), volta -> {
            Intent dados = volta.getData();
            if (volta.getResultCode() != RESULT_OK || dados == null) return;
            if (ehArtista) {
                artistaEscolhido = dados.getStringExtra(BuscaTmdbActivity.EXTRA_RESULTADO);
                artistaEscolhidoTexto = dados.getStringExtra(BuscaTmdbActivity.EXTRA_RESULTADO_TITULO);
                fotoDoArtistaEscolhido = dados.getStringExtra(BuscaTmdbActivity.EXTRA_RESULTADO_FOTO);
                restaurar = false;
                mostrarPendente();
                return;
            }
            buscarPrevia(dados.getStringExtra(BuscaTmdbActivity.EXTRA_RESULTADO),
                    dados.getStringExtra(BuscaTmdbActivity.EXTRA_RESULTADO_TITULO));
        });
        // Tablet e TV pelo navegador do app, aberto na pasta de imagens (o seletor do sistema
        // travava com as imagens do HD no tablet)
        navegadorDoApp = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), volta -> {
            Intent dados = volta.getData();
            String caminho = dados == null ? null : dados.getStringExtra(NavegadorImagensActivity.EXTRA_CAMINHO);
            if (volta.getResultCode() == RESULT_OK && caminho != null) {
                importar(() -> ImagemDoAparelho.doArquivo(this, new File(caminho)));
            }
        });
        // A coletânea escolhida na janela vai para a caixa; o banco só muda no Salvar
        escolherColetanea = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), volta -> {
            Intent dados = volta.getData();
            String nome = dados == null ? null : dados.getStringExtra(EscolherColetaneaActivity.EXTRA_NOME);
            if (volta.getResultCode() == RESULT_OK && nome != null) mudarColetanea(nome, false);
        });
        repositorio = RepositorioBiblioteca.atual(this);
        id = getIntent().getStringExtra(Navegacao.EXTRA_ID);
        Titulo inicial = repositorio.titulo(id);
        if (inicial == null) {
            finish();
            return;
        }
        capaAgrupada = inicial.ehColetanea() || inicial.ehArtista();
        ehArtista = inicial.ehArtista();
        setContentView(tv ? R.layout.st_tv_editar : R.layout.st_tablet_editar);
        if (!tv) BordasSistema.aplicar(findViewById(R.id.st_editar_corpo), true, true, true, true);

        findViewById(R.id.st_editar_voltar).setOnClickListener(v -> finish());
        View buscarTmdb = findViewById(R.id.st_editar_buscar_tmdb);
        buscarTmdb.setOnClickListener(v ->
                buscaTmdb.launch(new Intent(this, BuscaTmdbActivity.class).putExtra(Navegacao.EXTRA_ID, id)));
        // Coletânea não tem resultado do TMDB para escolher; o artista busca outro artista, menos "Sem artista"
        boolean buscaArtista = ehArtista && !Acervo.chaveArtista(null).equals(id);
        if (capaAgrupada && !buscaArtista) buscarTmdb.setVisibility(View.GONE);
        if (buscaArtista) ((TextView) buscarTmdb).setText(R.string.st_buscar_artista);
        // Restaurar original: prévia na tela; o banco só muda no Salvar
        findViewById(R.id.st_editar_restaurar).setOnClickListener(v -> {
            restaurar = true;
            artistaEscolhido = null;
            fotoDoArtistaEscolhido = null;
            resultadoTmdb = null;
            previa = null;
            mexidos.clear();
            capaMexida = false;
            fundoMexido = false;
            coletaneaMexida = false;
            coletaneaDoTmdb = false;
            carregar();
            mostrarPendente();
        });
        findViewById(R.id.st_editar_salvar).setOnClickListener(v -> salvar());
        View cancelar = findViewById(R.id.st_editar_cancelar);
        if (cancelar != null) cancelar.setOnClickListener(v -> finish());

        montarCampos();
        if (tv) {
            getOnBackPressedDispatcher().addCallback(this, voltarDaCaixa);
            vigiarTeclado();
        }
        // Aviso do que vai mudar no Salvar, embaixo do título da tela
        TextView nomeDoTitulo = findViewById(R.id.st_editar_nome);
        avisoPendente = Textos.novo(this, "", R.font.st_inter_bold, tv ? 17 : 19, R.color.st_aviso_claro);
        avisoPendente.setVisibility(View.GONE);
        ((ViewGroup) nomeDoTitulo.getParent()).addView(avisoPendente);
        if (inicial.ehArtista()) buscarFotosDoArtista();
    }

    private void mostrarPendente() {
        String texto = restaurar ? getString(R.string.st_restaurar_pendente) : "";
        if (resultadoTmdb != null) {
            String tmdb = resultadoTmdbTexto;
            texto = texto.isEmpty() ? tmdb : texto + "\n" + tmdb;
        }
        if (artistaEscolhido != null) texto = getString(R.string.st_artista_pendente, artistaEscolhidoTexto);
        avisoPendente.setText(texto);
        avisoPendente.setVisibility(texto.isEmpty() ? View.GONE : View.VISIBLE);
    }

    /** Os dados completos do resultado vêm da rede, fora da tela; sem rede, avisa e fica como estava. */
    private void buscarPrevia(String resultado, String nome) {
        resultadoTmdb = resultado;
        previa = null;
        resultadoTmdbTexto = getString(R.string.st_tmdb_buscando_dados, nome);
        mostrarPendente();
        copia.execute(() -> {
            PreviaTmdb chegou;
            try {
                chegou = repositorio.previaResultadoTmdb(id, resultado);
            } catch (RuntimeException e) {
                chegou = null;
            }
            final PreviaTmdb pronta = chegou;
            runOnUiThread(() -> aoChegarPrevia(resultado, pronta));
        });
    }

    private void aoChegarPrevia(String resultado, @Nullable PreviaTmdb chegou) {
        if (isFinishing() || !resultado.equals(resultadoTmdb)) return;
        if (chegou == null) {
            resultadoTmdb = null;
            mostrarPendente();
            Toast.makeText(this, R.string.st_tmdb_sem_rede, Toast.LENGTH_LONG).show();
            return;
        }
        previa = chegou;
        restaurar = false;
        capaMexida = false;
        fundoMexido = false;
        // Sem ano: só o nome (nada de parênteses vazios)
        String ano = chegou.titulo.ano;
        resultadoTmdbTexto = getString(R.string.st_tmdb_pendente,
                ano == null || ano.isEmpty() ? chegou.titulo.titulo : chegou.titulo.titulo + " (" + ano + ")");
        carregar();
        mostrarPendente();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Primeira vez e volta de outra tela (a prévia do TMDB, se houver, continua valendo)
        carregar();
    }

    private List<String> opcoes(boolean ehCapa) {
        if (previa != null) return ehCapa ? previa.capas : previa.fundos;
        if (!ehCapa) return repositorio.opcoesFundo(id);
        List<String> capas = new ArrayList<>(repositorio.opcoesCapa(id));
        // A atual fica primeiro, e as cores, depois das fotos
        capas.addAll(Math.min(1, capas.size()), fotosDoArtista);
        return capas;
    }

    /**
     * Capa de artista: as fotos do Deezer entram nas opções quando chegam, sem segurar a tela. O banco
     * só muda no Salvar, que baixa a escolhida (como as opções do TMDB).
     */
    private void buscarFotosDoArtista() {
        new Thread(() -> {
            List<String> chegou;
            try {
                chegou = repositorio.fotosDoArtista(id);
            } catch (RuntimeException e) {
                chegou = null;
            }
            final List<String> fotos = chegou;
            runOnUiThread(() -> aoChegarFotos(fotos));
        }, "StarkTechFotosArtista").start();
    }

    private void aoChegarFotos(@Nullable List<String> fotos) {
        if (isFinishing() || isDestroyed()) return;
        if (fotos == null || fotos.isEmpty()) {
            Toast.makeText(this, fotos == null ? R.string.st_fotos_sem_rede : R.string.st_fotos_nenhuma, Toast.LENGTH_LONG).show();
            return;
        }
        fotosDoArtista = fotos;
        // TV: com o foco numa opção da capa, ele continua na mesma posição
        montarOpcoes(findViewById(R.id.st_editar_capas), opcoes(true), true, opcoesCapa.indexOf(getCurrentFocus()));
    }

    private void carregar() {
        Titulo salvo = repositorio.titulo(id);
        Titulo t = previa != null ? previa.titulo : restaurar ? repositorio.tituloOriginal(id) : salvo;
        if (t == null || salvo == null) {
            finish();
            return;
        }
        ((TextView) findViewById(R.id.st_editar_nome)).setText(t.titulo);
        carregando = true;
        for (Map.Entry<Campo, EditText> item : campos.entrySet()) {
            if (!mexidos.contains(item.getKey())) item.getValue().setText(t.valor(item.getKey()));
        }
        if (caixaColetanea != null && !coletaneaMexida) caixaColetanea.setText(t.colecaoNome == null ? "" : t.colecaoNome);
        carregando = false;
        if (!mexidos.contains(Campo.TIPO)) marcarTipo(t.tipo == null ? Tipo.FILME : t.tipo);
        // "Atual" é o que está salvo; a prévia aparece como "nova"
        capaSalva = salvo.referenciaCapa();
        fundoSalvo = salvo.referenciaFundo();
        if (!capaMexida) capa = t.referenciaCapa();
        if (!fundoMexido) fundo = t.referenciaFundo();
        montarOpcoes(findViewById(R.id.st_editar_capas), opcoes(true), true, -1);
        montarOpcoes(findViewById(R.id.st_editar_fundos), opcoes(false), false, -1);
    }

    /**
     * Salvar: tudo o que mudou nesta tela vai para a biblioteca de uma vez — Restaurar original, o
     * resultado do TMDB, os campos e a capa / o fundo. Com Restaurar ou TMDB pendente, só os campos
     * mexidos aqui ficam "editados à mão" (os outros vêm do TMDB).
     */
    private void salvar() {
        if (salvando) return;
        boolean pendente = restaurar || resultadoTmdb != null;
        Map<Campo, String> valores = new EnumMap<>(Campo.class);
        for (Map.Entry<Campo, EditText> item : campos.entrySet()) {
            if (!pendente || mexidos.contains(item.getKey())) valores.put(item.getKey(), item.getValue().getText().toString());
        }
        if (!pendente || mexidos.contains(Campo.TIPO)) valores.put(Campo.TIPO, tipo.name());
        // Coletânea só vale em filme: "" tira dela; null volta ao que o TMDB diz
        final boolean gravarColetanea = caixaColetanea != null && coletaneaMexida && tipo == Tipo.FILME;
        final String coletanea = gravarColetanea && !coletaneaDoTmdb ? caixaColetanea.getText().toString() : null;
        final String artista = artistaEscolhido;
        final String fotoDoArtista = fotoDoArtistaEscolhido;
        if (!pendente && !capaMexida && !fundoMexido && artista == null) {
            repositorio.salvarEdicao(id, valores);
            if (gravarColetanea) repositorio.definirColetanea(id, coletanea);
            salvou = true;
            finish();
            return;
        }
        // TMDB e opção do TMDB ainda não baixada (url:) usam a rede: fora da tela, antes de fechar
        salvando = true;
        Toast.makeText(this, R.string.st_salvando, Toast.LENGTH_SHORT).show();
        final boolean restaurarAgora = restaurar;
        final String tmdb = resultadoTmdb;
        final String novaCapa = capaMexida ? capa : null;
        final String novoFundo = fundoMexido ? fundo : null;
        new Thread(() -> {
            if (restaurarAgora) repositorio.restaurarOriginal(id);
            boolean trocou = tmdb == null || repositorio.selecionarResultadoTmdb(id, tmdb);
            repositorio.salvarEdicao(id, valores);
            if (gravarColetanea) repositorio.definirColetanea(id, coletanea);
            boolean baixou = true;
            if (novaCapa != null) baixou = repositorio.escolherCapa(id, novaCapa);
            if (novoFundo != null) baixou &= repositorio.escolherFundo(id, novoFundo);
            // Por último: depois disso, este artista pode nem existir mais (juntou-se ao outro)
            if (artista != null) repositorio.definirArtista(id, artista, fotoDoArtista);
            final boolean imagens = baixou;
            runOnUiThread(() -> {
                if (!trocou) Toast.makeText(this, R.string.st_tmdb_sem_rede, Toast.LENGTH_LONG).show();
                else if (!imagens) Toast.makeText(this, R.string.st_imagem_nao_baixou, Toast.LENGTH_LONG).show();
                salvou = true;
                finish();
            });
        }, "StarkTechSalvar").start();
    }

    @Override
    protected void onDestroy() {
        copia.shutdownNow();
        // Imagens copiadas e não salvas (ou trocadas por outra antes de salvar) saem da pasta do app
        for (String referencia : importadas) {
            boolean ficou = salvou && (referencia.equals(capa) || referencia.equals(fundo));
            String caminho = Edicoes.arquivoDe(referencia);
            if (!ficou && caminho != null) {
                //noinspection ResultOfMethodCallIgnored
                new File(caminho).delete();
            }
        }
        super.onDestroy();
    }

    // ---- Imagem do aparelho (F2, D3 e D7) ----

    private void escolherDoAparelho(boolean ehCapa) {
        pedidoDaCapa = ehCapa;
        Intent navegador = new Intent(this, NavegadorImagensActivity.class);
        String pasta = Preferencias.pastaImagens(this);
        if (pasta != null) navegador.putExtra(NavegadorImagensActivity.EXTRA_PASTA, pasta);
        navegadorDoApp.launch(navegador);
    }

    /** Copia a imagem fora da tela (até 15 MB) e mostra no destaque; problema vira aviso. */
    private void importar(Callable<ImagemDoAparelho.Resultado> tarefa) {
        final boolean ehCapa = pedidoDaCapa;
        copia.execute(() -> {
            ImagemDoAparelho.Resultado resultado;
            try {
                resultado = tarefa.call();
            } catch (Exception e) {
                resultado = null;
            }
            final ImagemDoAparelho.Resultado fim = resultado;
            runOnUiThread(() -> aoImportar(fim, ehCapa));
        });
    }

    private void aoImportar(@Nullable ImagemDoAparelho.Resultado resultado, boolean ehCapa) {
        boolean copiou = resultado != null && resultado.ok();
        if (copiou && isDestroyed()) {
            // a tela fechou durante a cópia: a imagem não vai ser usada
            //noinspection ResultOfMethodCallIgnored
            new File(Edicoes.arquivoDe(resultado.referencia)).delete();
            return;
        }
        if (copiou) importadas.add(resultado.referencia);
        if (isFinishing()) return;
        if (resultado == null || !resultado.ok()) {
            ImagemDoAparelho.Erro erro = resultado == null || resultado.ok() ? ImagemDoAparelho.Erro.ILEGIVEL : resultado.erro;
            Toast.makeText(this, erro == ImagemDoAparelho.Erro.FORMATO ? R.string.st_imagem_formato
                    : erro == ImagemDoAparelho.Erro.GRANDE_DEMAIS ? R.string.st_imagem_grande
                    : R.string.st_imagem_ilegivel, Toast.LENGTH_LONG).show();
            return;
        }
        if (ehCapa) {
            capa = resultado.referencia;
            capaMexida = true;
            montarOpcoes(findViewById(R.id.st_editar_capas), opcoes(true), true, -1);
        } else {
            fundo = resultado.referencia;
            fundoMexido = true;
            montarOpcoes(findViewById(R.id.st_editar_fundos), opcoes(false), false, -1);
        }
    }

    // ---- Campos (2 colunas, como no desenho) ----

    private void montarCampos() {
        LinearLayout alvo = findViewById(R.id.st_editar_campos);
        Resources res = getResources();
        int espacoH = res.getDimensionPixelSize(tv ? R.dimen.st_tv_campo_espaco_h : R.dimen.st_tablet_campo_espaco_h);
        int espacoV = res.getDimensionPixelSize(tv ? R.dimen.st_tv_campo_espaco_v : R.dimen.st_tablet_campo_espaco_v);

        if (capaAgrupada) {
            linha(alvo, 0, espacoH, campo(Campo.TITULO, R.string.st_campo_nome, false));
            return;
        }
        linha(alvo, 0, espacoH, campo(Campo.TITULO, R.string.st_campo_titulo, false), campo(Campo.TITULO_ORIGINAL, R.string.st_campo_titulo_original, false));
        // Tipo na linha inteira: com 5 opções, "Stand-up" quebrava em duas linhas na metade da largura
        linha(alvo, espacoV, espacoH, tipoSegmentado());
        linha(alvo, espacoV, espacoH, campo(Campo.ANO, R.string.st_campo_ano, false), campo(Campo.DURACAO, R.string.st_campo_duracao, false));
        linha(alvo, espacoV, espacoH, campo(Campo.GENEROS, R.string.st_campo_generos, false));
        View coletanea = campoColetanea();
        linha(alvo, espacoV, espacoH, coletanea);
        linhaColetanea = (View) coletanea.getParent();
        linha(alvo, espacoV, espacoH, campo(Campo.SINOPSE, R.string.st_campo_sinopse, true));
    }

    private void linha(LinearLayout alvo, int topo, int espaco, View... celulas) {
        LinearLayout linha = new LinearLayout(this);
        linha.setOrientation(LinearLayout.HORIZONTAL);
        linha.setBaselineAligned(false);
        for (int i = 0; i < celulas.length; i++) adicionarComPeso(linha, celulas[i], i > 0 ? espaco : 0);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = topo;
        alvo.addView(linha, lp);
    }

    private static void adicionarComPeso(LinearLayout linha, View celula, int inicio) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMarginStart(inicio);
        linha.addView(celula, lp);
    }

    /** Rótulo em caixa alta + campo (#0f172a, borda #1e293b, azul em foco). */
    private View campo(Campo qual, @StringRes int rotulo, boolean sinopse) {
        LinearLayout celula = new LinearLayout(this);
        celula.setOrientation(LinearLayout.VERTICAL);
        celula.addView(rotulo(rotulo));

        EditText texto = caixaDeTexto(qual == Campo.ANO, sinopse);
        texto.addTextChangedListener(aoDigitar(() -> mexidos.add(qual)));
        campos.put(qual, texto);
        celula.addView(texto, lpCaixa(sinopse));
        return celula;
    }

    private EditText caixaDeTexto(boolean numero, boolean sinopse) {
        Resources res = getResources();
        EditText texto = new EditText(this);
        texto.setBackgroundResource(R.drawable.st_campo_texto);
        texto.setTypeface(ResourcesCompat.getFont(this, sinopse ? R.font.st_inter_medium : R.font.st_inter_semibold));
        texto.setTextColor(ContextCompat.getColor(this, R.color.st_texto));
        texto.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(tv ? R.dimen.st_tv_texto_campo : R.dimen.st_tablet_texto_campo));
        int padding = res.getDimensionPixelSize(tv ? R.dimen.st_tv_campo_padding : R.dimen.st_tablet_campo_padding);
        if (sinopse) {
            texto.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            texto.setMinLines(4);
            texto.setMaxLines(6);
            texto.setGravity(Gravity.TOP | Gravity.START);
            texto.setLineSpacing(0f, 1.24f);
            texto.setPadding(padding, padding * 3 / 4, padding, padding * 3 / 4);
        } else {
            texto.setInputType(numero ? InputType.TYPE_CLASS_NUMBER
                    : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            texto.setSingleLine(true);
            texto.setGravity(Gravity.CENTER_VERTICAL);
            texto.setPadding(padding, 0, padding, 0);
        }
        if (tv) {
            texto.setForeground(ContextCompat.getDrawable(this, R.drawable.st_foco_tv));
            ligarCaixaTv(texto);
        }
        return texto;
    }

    /** {@code acao} roda quando quem usa a tela muda o texto, não quando a tela preenche a caixa. */
    private TextWatcher aoDigitar(Runnable acao) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!carregando) acao.run();
            }
        };
    }

    private LinearLayout.LayoutParams lpCaixa(boolean sinopse) {
        Resources res = getResources();
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, sinopse
                ? ViewGroup.LayoutParams.WRAP_CONTENT
                : res.getDimensionPixelSize(tv ? R.dimen.st_tv_campo_altura : R.dimen.st_tablet_campo_altura));
        lp.topMargin = res.getDimensionPixelSize(tv ? R.dimen.st_tv_detalhe_dados_rotulo_espaco : R.dimen.st_tablet_dados_rotulo_espaco);
        return lp;
    }

    // ---- Coletânea do filme (D5) ----

    /**
     * A caixa (nome novo ou o escolhido; vazia = fora de coletânea) e, embaixo, Escolher (as coletâneas
     * da biblioteca, se houver), Tirar e Voltar ao TMDB. Como o resto da tela, só vale no Salvar.
     */
    private View campoColetanea() {
        LinearLayout celula = new LinearLayout(this);
        celula.setOrientation(LinearLayout.VERTICAL);
        celula.addView(rotulo(R.string.st_campo_coletanea));

        caixaColetanea = caixaDeTexto(false, false);
        caixaColetanea.setHint(R.string.st_coletanea_fora);
        caixaColetanea.addTextChangedListener(aoDigitar(() -> {
            coletaneaMexida = true;
            coletaneaDoTmdb = false;
        }));
        celula.addView(caixaColetanea, lpCaixa(false));

        Resources res = getResources();
        int folga = res.getDimensionPixelSize(tv ? R.dimen.st_tv_detalhe_dados_rotulo_espaco : R.dimen.st_tablet_dados_rotulo_espaco);
        LinearLayout botoes = new LinearLayout(this);
        botoes.setOrientation(LinearLayout.HORIZONTAL);
        if (!repositorio.nomesDeColetanea().isEmpty()) {
            botaoColetanea(botoes, R.string.st_coletanea_escolher, folga, () -> escolherColetanea.launch(
                    new Intent(this, EscolherColetaneaActivity.class).putExtra(EscolherColetaneaActivity.EXTRA_ATUAL,
                            caixaColetanea.getText().toString().trim())));
        }
        botaoColetanea(botoes, R.string.st_coletanea_tirar, folga, () -> mudarColetanea("", false));
        botaoColetanea(botoes, R.string.st_coletanea_tmdb, folga, () -> {
            Titulo original = repositorio.tituloOriginal(id);
            mudarColetanea(original == null || original.colecaoNome == null ? "" : original.colecaoNome, true);
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = folga;
        celula.addView(botoes, lp);
        return celula;
    }

    private void botaoColetanea(LinearLayout botoes, @StringRes int nome, int folga, Runnable acao) {
        Resources res = getResources();
        TextView botao = new TextView(this);
        botao.setText(nome);
        botao.setSingleLine(true);
        botao.setTypeface(ResourcesCompat.getFont(this, R.font.st_inter_extrabold));
        botao.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(tv ? R.dimen.st_tv_texto_botao : R.dimen.st_tablet_texto_botao));
        botao.setTextColor(ContextCompat.getColor(this, R.color.st_texto));
        botao.setBackgroundResource(R.drawable.st_botao_secundario);
        botao.setGravity(Gravity.CENTER);
        botao.setFocusable(true);
        botao.setClickable(true);
        if (tv) botao.setForeground(ContextCompat.getDrawable(this, R.drawable.st_foco_tv));
        botao.setOnClickListener(v -> acao.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                res.getDimensionPixelSize(tv ? R.dimen.st_tv_segmento_altura : R.dimen.st_tablet_segmento_altura), 1f);
        if (botoes.getChildCount() > 0) lp.setMarginStart(folga);
        botoes.addView(botao, lp);
    }

    /** {@code doTmdb}: o Salvar volta ao TMDB em vez de gravar o nome, que aqui é só a prévia. */
    private void mudarColetanea(String nome, boolean doTmdb) {
        if (caixaColetanea == null) return;
        carregando = true;
        caixaColetanea.setText(nome);
        carregando = false;
        coletaneaMexida = true;
        coletaneaDoTmdb = doTmdb;
    }

    // ---- Caixa de texto na TV ----

    /**
     * O foco só realça: sem teclado, sem cursor e com as setas andando entre as caixas. OK entra,
     * seleciona o texto e abre o teclado. Sai ao fechar o teclado (Voltar, Concluir), ao perder o
     * foco ou com ↑ / ↓ (na Sinopse, depois da primeira / última linha).
     */
    private void ligarCaixaTv(EditText caixa) {
        final int entrada = caixa.getInputType();
        // Sem tela cheia: a caixa e o texto selecionado ficam à vista, acima do teclado
        caixa.setImeOptions(EditorInfo.IME_ACTION_DONE | EditorInfo.IME_FLAG_NO_FULLSCREEN);
        modoFoco(caixa);
        caixa.setOnClickListener(v -> {
            if (editando == caixa) abrirTeclado(caixa);
            else entrarNaCaixa(caixa, entrada);
        });
        caixa.setOnFocusChangeListener((v, temFoco) -> {
            if (!temFoco) sairDaCaixa(caixa);
        });
        caixa.setOnEditorActionListener((v, acao, evento) -> {
            if (acao != EditorInfo.IME_ACTION_DONE) return false;
            sairDaCaixa(caixa);
            return true;
        });
        // Não consome: já fora da caixa, a mesma tecla anda o foco
        caixa.setOnKeyListener((v, tecla, evento) -> {
            boolean vertical = tecla == KeyEvent.KEYCODE_DPAD_UP || tecla == KeyEvent.KEYCODE_DPAD_DOWN;
            if (editando == caixa && vertical && evento.getAction() == KeyEvent.ACTION_DOWN
                    && !temLinha(caixa, tecla == KeyEvent.KEYCODE_DPAD_UP)) {
                sairDaCaixa(caixa);
            }
            return false;
        });
    }

    /** Sem o movimento do cursor, as setas andam o foco. O SEM_TECLAS mantém a caixa focável. */
    private static void modoFoco(EditText caixa) {
        caixa.setKeyListener(SEM_TECLAS);
        caixa.setMovementMethod(null);
        caixa.setCursorVisible(false);
        caixa.setLongClickable(false);
    }

    private void entrarNaCaixa(EditText caixa, int entrada) {
        editando = caixa;
        tecladoVisto = false;
        voltarDaCaixa.setEnabled(true);
        caixa.setMovementMethod(ArrowKeyMovementMethod.getInstance());
        caixa.setCursorVisible(true);
        caixa.selectAll();
        caixa.setInputType(entrada);
        abrirTeclado(caixa);
    }

    private void abrirTeclado(EditText caixa) {
        getSystemService(InputMethodManager.class).showSoftInput(caixa, 0);
    }

    private void sairDaCaixa(EditText caixa) {
        if (editando != caixa) return;
        editando = null;
        voltarDaCaixa.setEnabled(false);
        getSystemService(InputMethodManager.class).hideSoftInputFromWindow(caixa.getWindowToken(), 0);
        modoFoco(caixa);
        caixa.scrollTo(0, 0);
    }

    private static boolean temLinha(EditText caixa, boolean paraCima) {
        Layout layout = caixa.getLayout();
        if (layout == null) return false;
        int linha = layout.getLineForOffset(caixa.getSelectionEnd());
        return paraCima ? linha > 0 : linha < layout.getLineCount() - 1;
    }

    /** O Voltar com o teclado aberto fica com o teclado e não chega à tela: o fim vem do layout. */
    private void vigiarTeclado() {
        View raiz = findViewById(android.R.id.content);
        raiz.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if (editando == null) return;
            WindowInsetsCompat janela = ViewCompat.getRootWindowInsets(raiz);
            boolean aberto = janela != null && janela.isVisible(WindowInsetsCompat.Type.ime());
            if (aberto) tecladoVisto = true;
            else if (tecladoVisto) sairDaCaixa(editando);
        });
    }

    /** Tipo: Filme / Série / Desenho / Show / Stand-up, numa linha só, a escolhida em branco. */
    private View tipoSegmentado() {
        Resources res = getResources();
        LinearLayout celula = new LinearLayout(this);
        celula.setOrientation(LinearLayout.VERTICAL);
        celula.addView(rotulo(R.string.st_campo_tipo));

        LinearLayout grupo = new LinearLayout(this);
        grupo.setOrientation(LinearLayout.HORIZONTAL);
        grupo.setBackgroundResource(R.drawable.st_campo_texto);
        int folga = res.getDimensionPixelSize(tv ? R.dimen.st_tv_detalhe_dados_rotulo_espaco : R.dimen.st_tablet_dados_rotulo_espaco);
        grupo.setPadding(folga, folga, folga, folga);
        int altura = res.getDimensionPixelSize(tv ? R.dimen.st_tv_segmento_altura : R.dimen.st_tablet_segmento_altura);
        Tipo[] tipos = {Tipo.FILME, Tipo.SERIE, Tipo.DESENHO, Tipo.SHOW, Tipo.STANDUP};
        for (int i = 0; i < tipos.length; i++) {
            final Tipo opcao = tipos[i];
            TextView segmento = new TextView(this);
            segmento.setText(PecasDetalhe.rotuloTipo(opcao));
            segmento.setSingleLine(true);
            segmento.setTypeface(ResourcesCompat.getFont(this, R.font.st_inter_extrabold));
            segmento.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(tv ? R.dimen.st_tv_texto_botao : R.dimen.st_tablet_texto_botao));
            segmento.setTextColor(ContextCompat.getColorStateList(this, R.color.st_cor_segmento));
            segmento.setBackgroundResource(R.drawable.st_segmento);
            segmento.setGravity(Gravity.CENTER);
            segmento.setFocusable(true);
            segmento.setClickable(true);
            segmento.setTag(opcao);
            if (tv) segmento.setForeground(ContextCompat.getDrawable(this, R.drawable.st_foco_tv));
            segmento.setOnClickListener(v -> {
                mexidos.add(Campo.TIPO);
                marcarTipo(opcao);
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, altura, 1f);
            if (i > 0) lp.setMarginStart(folga);
            grupo.addView(segmento, lp);
            segmentos.add(segmento);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = folga;
        celula.addView(grupo, lp);
        return celula;
    }

    private void marcarTipo(Tipo novo) {
        tipo = novo;
        for (TextView segmento : segmentos) segmento.setSelected(segmento.getTag() == novo);
        // Coletânea só existe em filme
        if (linhaColetanea != null) linhaColetanea.setVisibility(novo == Tipo.FILME ? View.VISIBLE : View.GONE);
    }

    private TextView rotulo(@StringRes int texto) {
        TextView rotulo = new TextView(this);
        rotulo.setText(texto);
        rotulo.setTypeface(ResourcesCompat.getFont(this, R.font.st_inter_black));
        rotulo.setIncludeFontPadding(false);
        rotulo.setAllCaps(true);
        rotulo.setLetterSpacing(0.15f);
        rotulo.setTextColor(ContextCompat.getColor(this, R.color.st_texto_apagado));
        rotulo.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(
                tv ? R.dimen.st_tv_texto_dados_rotulo : R.dimen.st_tablet_texto_dados_rotulo));
        return rotulo;
    }

    // ---- Capa e fundo ----

    /**
     * Capa ou fundo: a escolhida em destaque (maior, anel azul e ✓), com "atual" / "nova" e as
     * regras de formato ao lado; embaixo, as outras opções do TMDB (sem TMDB, cores) e "Do
     * aparelho" (tablet) / "Do HD" (TV, D14), em fileiras que cabem na coluna. Tocar numa opção
     * a traz para o destaque; {@code focar} (TV) devolve o foco à mesma posição da fileira.
     */
    private void montarOpcoes(LinearLayout alvo, List<String> referencias, boolean ehCapa, int focar) {
        alvo.removeAllViews();
        Resources res = getResources();
        int espaco = px(R.dimen.st_tv_editar_opcao_espaco, R.dimen.st_tablet_editar_opcao_espaco);
        String escolhida = ehCapa ? capa : fundo;
        boolean mudou = !Objects.equals(escolhida, ehCapa ? capaSalva : fundoSalvo);
        int larguraPrevia = ehCapa ? px(R.dimen.st_tv_editar_previa_capa_largura, R.dimen.st_tablet_editar_previa_capa_largura)
                : px(R.dimen.st_tv_editar_previa_fundo_largura, R.dimen.st_tablet_editar_previa_fundo_largura);
        int alturaPrevia = ehCapa ? px(R.dimen.st_tv_editar_previa_capa_altura, R.dimen.st_tablet_editar_previa_capa_altura)
                : px(R.dimen.st_tv_editar_previa_fundo_altura, R.dimen.st_tablet_editar_previa_fundo_altura);

        LinearLayout destaque = new LinearLayout(this);
        destaque.setOrientation(LinearLayout.HORIZONTAL);
        destaque.setGravity(Gravity.CENTER_VERTICAL);
        FrameLayout previa = bloco(escolhida, ehCapa, true);
        destaque.addView(previa, new LinearLayout.LayoutParams(larguraPrevia, alturaPrevia));
        LinearLayout textos = new LinearLayout(this);
        textos.setOrientation(LinearLayout.VERTICAL);
        TextView nome = texto(ehCapa ? (mudou ? R.string.st_capa_nova : R.string.st_capa_atual)
                : (mudou ? R.string.st_fundo_novo : R.string.st_fundo_atual),
                R.font.st_inter_extrabold, R.color.st_texto, res.getDimension(tv ? R.dimen.st_tv_texto_campo : R.dimen.st_tablet_texto_campo));
        textos.addView(nome);
        previa.setContentDescription(nome.getText());
        TextView regra = texto(ehCapa ? R.string.st_regra_capa : R.string.st_regra_fundo, R.font.st_inter_semibold,
                R.color.st_texto_apagado, res.getDimension(tv ? R.dimen.st_tv_texto_editar_dica : R.dimen.st_tablet_texto_editar_dica));
        LinearLayout.LayoutParams lpRegra = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpRegra.topMargin = espaco / 2;
        textos.addView(regra, lpRegra);
        LinearLayout.LayoutParams lpTextos = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lpTextos.setMarginStart(espaco + espaco / 2);
        destaque.addView(textos, lpTextos);
        alvo.addView(destaque);

        TextView outras = texto(R.string.st_outras_opcoes, R.font.st_inter_semibold, R.color.st_texto_apagado,
                res.getDimension(tv ? R.dimen.st_tv_texto_editar_dica : R.dimen.st_tablet_texto_editar_dica));
        LinearLayout.LayoutParams lpOutras = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpOutras.topMargin = espaco + espaco / 2;
        alvo.addView(outras, lpOutras);

        List<View> itens = new ArrayList<>();
        int numero = 0;
        for (String referencia : referencias) {
            if (referencia.equals(escolhida)) continue;
            final int posicao = itens.size();
            FrameLayout opcao = bloco(referencia, ehCapa, false);
            opcao.setFocusable(true);
            opcao.setClickable(true);
            opcao.setContentDescription(getString(R.string.st_opcao_imagem, ++numero));
            opcao.setOnClickListener(v -> {
                if (ehCapa) {
                    capa = referencia;
                    capaMexida = true;
                } else {
                    fundo = referencia;
                    fundoMexido = true;
                }
                montarOpcoes(alvo, referencias, ehCapa, posicao);
            });
            itens.add(opcao);
        }
        itens.add(doAparelho(ehCapa));
        int porFileira = fileiras(alvo, itens,
                ehCapa ? px(R.dimen.st_tv_editar_capa_largura, R.dimen.st_tablet_editar_capa_largura)
                        : px(R.dimen.st_tv_editar_fundo_largura, R.dimen.st_tablet_editar_fundo_largura),
                ehCapa ? px(R.dimen.st_tv_editar_capa_altura, R.dimen.st_tablet_editar_capa_altura)
                        : px(R.dimen.st_tv_editar_fundo_altura, R.dimen.st_tablet_editar_fundo_altura),
                espaco);
        List<View> opcoes = ehCapa ? opcoesCapa : opcoesFundo;
        opcoes.clear();
        opcoes.addAll(itens);
        if (ehCapa) porFileiraCapa = porFileira;
        else porFileiraFundo = porFileira;
        ligarFocoTv();
        if (tv && focar >= 0) itens.get(Math.min(focar, itens.size() - 1)).requestFocus();
    }

    /**
     * Opção de capa ou fundo: a imagem (TMDB, aparelho) sobre o bloco de cor, ou só a cor, com o
     * ícone de imagem enquanto não carrega; a escolhida com anel azul e ✓.
     */
    private FrameLayout bloco(String referencia, boolean ehCapa, boolean escolhida) {
        int selo = px(R.dimen.st_tv_editar_selo, R.dimen.st_tablet_editar_selo);
        float canto = getResources().getDimension(tv ? R.dimen.st_tv_botao_canto : R.dimen.st_tablet_botao_canto);
        FrameLayout opcao = new FrameLayout(this);
        opcao.setBackground(CapaAdapter.blocoDeCor(this, corDe(referencia), canto, tv));
        opcao.setForeground(ContextCompat.getDrawable(this, R.drawable.st_opcao_imagem));
        opcao.setSelected(escolhida);

        ImageView imagem = new ImageView(this);
        imagem.setImageResource(R.drawable.st_ic_imagem);
        ImageViewCompat.setImageTintList(imagem, ContextCompat.getColorStateList(this, R.color.st_branco_35));
        FrameLayout.LayoutParams lpImagem = new FrameLayout.LayoutParams(selo, selo,
                ehCapa ? Gravity.TOP | Gravity.END : Gravity.CENTER);
        lpImagem.setMargins(selo / 3, selo / 3, selo / 3, selo / 3);
        opcao.addView(imagem, lpImagem);
        Imagens.daReferencia(opcao, referencia, ehCapa ? Imagens.CAPA : Imagens.CARTAO, canto, imagem);

        if (escolhida) {
            ImageView marca = new ImageView(this);
            marca.setImageResource(R.drawable.st_ic_check);
            marca.setBackgroundResource(R.drawable.st_selo_escolhida);
            ImageViewCompat.setImageTintList(marca, ContextCompat.getColorStateList(this, R.color.st_texto));
            marca.setPadding(selo / 5, selo / 5, selo / 5, selo / 5);
            FrameLayout.LayoutParams lpMarca = new FrameLayout.LayoutParams(selo, selo, Gravity.TOP | Gravity.START);
            lpMarca.setMargins(selo / 3, selo / 3, 0, 0);
            opcao.addView(marca, lpMarca);
        }
        return opcao;
    }

    /** Cor da referência {@code cor:}; imagem: o cinza dos cartões (por baixo, enquanto carrega). */
    private int corDe(String referencia) {
        if (referencia != null && referencia.startsWith(Edicoes.PREFIXO_COR)) {
            try {
                return (int) (0xFF000000L | Long.parseLong(referencia.substring(Edicoes.PREFIXO_COR.length() + 1), 16));
            } catch (RuntimeException e) {
                // cor estragada: o cinza
            }
        }
        return ContextCompat.getColor(this, R.color.st_cartao);
    }

    /** "Do aparelho" (tablet) / "Do HD" (TV) no fim das opções, ícone e nome centrados juntos (D3). */
    private View doAparelho(boolean ehCapa) {
        int selo = px(R.dimen.st_tv_editar_selo, R.dimen.st_tablet_editar_selo);
        LinearLayout botao = new LinearLayout(this);
        botao.setOrientation(LinearLayout.VERTICAL);
        botao.setGravity(Gravity.CENTER);
        botao.setBackgroundResource(R.drawable.st_botao_secundario);
        botao.setFocusable(true);
        botao.setClickable(true);
        if (tv) botao.setForeground(ContextCompat.getDrawable(this, R.drawable.st_foco_tv));
        botao.setOnClickListener(v -> escolherDoAparelho(ehCapa));

        ImageView icone = new ImageView(this);
        icone.setImageResource(R.drawable.st_ic_pastas);
        ImageViewCompat.setImageTintList(icone, ContextCompat.getColorStateList(this, R.color.st_texto));
        botao.addView(icone, new LinearLayout.LayoutParams(selo, selo));
        TextView nome = texto(tv ? R.string.st_do_hd : R.string.st_do_aparelho, R.font.st_inter_extrabold, R.color.st_texto_secundario,
                getResources().getDimension(tv ? R.dimen.st_tv_texto_editar_dica : R.dimen.st_tablet_texto_editar_dica));
        nome.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = px(R.dimen.st_tv_editar_opcao_espaco, R.dimen.st_tablet_editar_opcao_espaco) / 2;
        botao.addView(nome, lp);
        botao.setContentDescription(nome.getText());
        return botao;
    }

    /**
     * TV: com a escolhida em destaque, as fileiras de opções ficam mais baixas que os campos ao
     * lado e o foco pularia para eles; ↑ / ↓ andam Voltar → opções da capa → opções do fundo.
     */
    private void ligarFocoTv() {
        if (!tv || opcoesCapa.isEmpty() || opcoesFundo.isEmpty()) return;
        View voltar = findViewById(R.id.st_editar_voltar);
        voltar.setNextFocusDownId(idDe(opcoesCapa.get(0)));
        int inicioUltima = (opcoesCapa.size() - 1) / porFileiraCapa * porFileiraCapa;
        for (int i = 0; i < opcoesCapa.size(); i++) {
            View opcao = opcoesCapa.get(i);
            if (i < porFileiraCapa) opcao.setNextFocusUpId(voltar.getId());
            if (i >= inicioUltima) opcao.setNextFocusDownId(idDe(opcoesFundo.get(Math.min(i - inicioUltima, opcoesFundo.size() - 1))));
        }
        for (int i = 0; i < Math.min(porFileiraFundo, opcoesFundo.size()); i++) {
            opcoesFundo.get(i).setNextFocusUpId(idDe(opcoesCapa.get(Math.min(inicioUltima + i, opcoesCapa.size() - 1))));
        }
    }

    private static int idDe(View view) {
        if (view.getId() == View.NO_ID) view.setId(View.generateViewId());
        return view.getId();
    }

    /**
     * Opções no tamanho fixo, quantas couberem por fileira na coluna (nada passa para baixo dos
     * campos). Devolve quantas couberam por fileira.
     */
    private int fileiras(LinearLayout alvo, List<View> itens, int largura, int altura, int espaco) {
        int coluna = px(R.dimen.st_tv_editar_coluna, R.dimen.st_tablet_editar_coluna);
        int porFileira = Math.max(1, (coluna + espaco) / (largura + espaco));
        LinearLayout fileira = null;
        for (int i = 0; i < itens.size(); i++) {
            if (i % porFileira == 0) {
                fileira = new LinearLayout(this);
                fileira.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.topMargin = espaco;
                alvo.addView(fileira, lp);
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(largura, altura);
            if (i % porFileira > 0) lp.setMarginStart(espaco);
            fileira.addView(itens.get(i), lp);
        }
        return porFileira;
    }

    private TextView texto(@StringRes int texto, @FontRes int fonte, @ColorRes int cor, float tamanho) {
        TextView view = new TextView(this);
        view.setText(texto);
        view.setTypeface(ResourcesCompat.getFont(this, fonte));
        view.setTextColor(ContextCompat.getColor(this, cor));
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, tamanho);
        return view;
    }

    private int px(@DimenRes int naTv, @DimenRes int noTablet) {
        return getResources().getDimensionPixelSize(tv ? naTv : noTablet);
    }
}
