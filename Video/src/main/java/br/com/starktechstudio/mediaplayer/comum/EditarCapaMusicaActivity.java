// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.archos.mediacenter.video.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import br.com.starktechstudio.mediaplayer.dados.RepositorioBiblioteca;
import br.com.starktechstudio.mediaplayer.dados.RepositorioMusicas;
import br.com.starktechstudio.mediaplayer.motor.Edicoes;
import br.com.starktechstudio.mediaplayer.motor.ImagemDoAparelho;

/**
 * Trocar capa em Músicas (1.6, D2 do plano 007): no artista, as fotos do fanart.tv e da Wikipédia (as mesmas
 * opções do Editar do show); no artista, no álbum e na playlist, "Do aparelho" (o navegador de imagens do app,
 * na pasta de imagens de Configurações) e "Restaurar" (volta à capa automática). A escolha vale na hora e fica
 * nas edições, pela chave: a foto do artista é a mesma nos Shows.
 */
public class EditarCapaMusicaActivity extends AppCompatActivity {

    private static final String EXTRA_CHAVE = "br.com.starktechstudio.mediaplayer.extra.CHAVE_CAPA";
    private static final String EXTRA_NOME = "br.com.starktechstudio.mediaplayer.extra.NOME_CAPA";
    private static final String EXTRA_ARTISTA = "br.com.starktechstudio.mediaplayer.extra.ARTISTA_CAPA";

    /** {@code nomeArtista}: só no artista (busca as fotos dele); nulo no álbum e na playlist. */
    public static void abrir(Activity origem, String chave, String nome, @Nullable String nomeArtista) {
        origem.startActivity(new Intent(origem, EditarCapaMusicaActivity.class)
                .putExtra(EXTRA_CHAVE, chave)
                .putExtra(EXTRA_NOME, nome)
                .putExtra(EXTRA_ARTISTA, nomeArtista));
    }

    private boolean tv;
    private String chave;
    private final ExecutorService trabalho = Executors.newSingleThreadExecutor(r -> new Thread(r, "StarkTechCapaMusica"));
    private LinearLayout opcoes;
    private TextView situacao;
    private boolean ocupado;
    private ActivityResultLauncher<Intent> navegador;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(Aparelho.tema(this));
        super.onCreate(savedInstanceState);
        tv = Aparelho.ehTv(this);
        chave = getIntent().getStringExtra(EXTRA_CHAVE);
        String nome = getIntent().getStringExtra(EXTRA_NOME);
        String artista = getIntent().getStringExtra(EXTRA_ARTISTA);
        if (chave == null) {
            finish();
            return;
        }
        navegador = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), volta -> {
            Intent dados = volta.getData();
            String caminho = dados == null ? null : dados.getStringExtra(NavegadorImagensActivity.EXTRA_CAMINHO);
            if (volta.getResultCode() == RESULT_OK && caminho != null) doAparelho(new File(caminho));
        });

        FrameLayout raiz = new FrameLayout(this);
        raiz.setBackgroundResource(R.color.st_fundo);
        raiz.addView(new FundoAnimado(this), new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        LinearLayout pagina = new LinearLayout(this);
        pagina.setOrientation(LinearLayout.VERTICAL);
        int lateral = getResources().getDimensionPixelSize(tv ? R.dimen.st_tv_margem_lateral : R.dimen.st_tablet_margem_h);
        pagina.setPadding(lateral, Medidas.px(this, 36), lateral, Medidas.px(this, 44));
        if (!tv) BordasSistema.aplicar(pagina, true, true, true, true);
        pagina.addView(PecasMusica.topo(this, getString(artista != null ? R.string.st_musicas_trocar_foto : R.string.st_musicas_trocar_capa)));

        TextView titulo = Textos.novo(this, nome == null ? "" : nome, R.font.st_inter_black, tv ? 52 : 46, R.color.st_texto);
        titulo.setLetterSpacing(-0.02f);
        pagina.addView(titulo, PecasMusica.comTopo(Medidas.px(this, 24)));
        TextView dica = Textos.novo(this, getString(R.string.st_musicas_capa_dica), R.font.st_inter_semibold, 19, R.color.st_texto_secundario);
        pagina.addView(dica, PecasMusica.comTopo(Medidas.px(this, 10)));

        LinearLayout botoes = new LinearLayout(this);
        botoes.setOrientation(LinearLayout.HORIZONTAL);
        Button aparelho = PecasMusica.botao(this, R.string.st_do_aparelho, R.drawable.st_ic_imagem, true);
        aparelho.setOnClickListener(v -> {
            if (ocupado) return;
            Intent intent = new Intent(this, NavegadorImagensActivity.class);
            String pasta = Preferencias.pastaImagens(this);
            if (pasta != null) intent.putExtra(NavegadorImagensActivity.EXTRA_PASTA, pasta);
            navegador.launch(intent);
        });
        botoes.addView(aparelho);
        Button restaurar = PecasMusica.botao(this, R.string.st_musicas_restaurar, R.drawable.st_ic_do_inicio, false);
        restaurar.setOnClickListener(v -> restaurar());
        LinearLayout.LayoutParams lpRestaurar = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpRestaurar.setMarginStart(Medidas.px(this, 14));
        botoes.addView(restaurar, lpRestaurar);
        pagina.addView(botoes, PecasMusica.comTopo(Medidas.px(this, 26)));

        if (artista != null) {
            pagina.addView(Textos.rotulo(this, getString(R.string.st_musicas_opcoes_foto), 15), PecasMusica.comTopo(Medidas.px(this, 36)));
            situacao = Textos.novo(this, getString(R.string.st_musicas_buscando_fotos), R.font.st_inter_semibold, 19, R.color.st_texto_apagado);
            pagina.addView(situacao, PecasMusica.comTopo(Medidas.px(this, 12)));
            opcoes = new LinearLayout(this);
            opcoes.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams lpOpcoes = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lpOpcoes.topMargin = Medidas.px(this, 18);
            pagina.addView(opcoes, lpOpcoes);
            buscarFotos(artista);
        }

        ScrollView rolagem = new ScrollView(this);
        rolagem.setFocusable(false);
        rolagem.setVerticalScrollBarEnabled(false);
        rolagem.setOverScrollMode(View.OVER_SCROLL_NEVER);
        rolagem.addView(pagina, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        raiz.addView(rolagem, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(raiz);
        if (tv) aparelho.post(aparelho::requestFocus);
    }

    @Override
    protected void onDestroy() {
        trabalho.shutdownNow();
        super.onDestroy();
    }

    /** As fotos do artista (fanart.tv e Wikipédia), fora da tela; cada uma vira uma opção. */
    private void buscarFotos(String artista) {
        RepositorioBiblioteca repositorio = RepositorioBiblioteca.atual(this);
        trabalho.execute(() -> {
            List<String> fotos;
            try {
                fotos = repositorio.fotosDoArtistaPeloNome(artista);
            } catch (RuntimeException e) {
                fotos = new ArrayList<>();
            }
            final List<String> achadas = fotos;
            runOnUiThread(() -> mostrarFotos(achadas));
        });
    }

    private void mostrarFotos(List<String> fotos) {
        if (isFinishing() || opcoes == null) return;
        situacao.setText(R.string.st_musicas_sem_fotos);
        situacao.setVisibility(fotos.isEmpty() ? View.VISIBLE : View.GONE);
        int colunas = tv ? 6 : Math.max(2, Math.min(6, getResources().getDisplayMetrics().widthPixels / Medidas.px(this, 300)));
        int espaco = getResources().getDimensionPixelSize(tv ? R.dimen.st_tv_capa_espaco : R.dimen.st_tablet_capa_espaco);
        int largura = getResources().getDisplayMetrics().widthPixels
                - 2 * getResources().getDimensionPixelSize(tv ? R.dimen.st_tv_margem_lateral : R.dimen.st_tablet_margem_h);
        int lado = (largura - espaco * (colunas - 1)) / colunas;
        float canto = getResources().getDimension(tv ? R.dimen.st_tv_capa_canto : R.dimen.st_tablet_capa_canto);
        List<View> itens = new ArrayList<>();
        for (String foto : fotos) {
            FrameLayout opcao = PecasMusica.capa(this, lado, ContextCompat.getColor(this, R.color.st_cartao), null, R.drawable.st_ic_imagem, false);
            // A grade estica a largura; a altura fica a do lado, quadrada
            opcao.setMinimumHeight(lado);
            Imagens.daReferencia(opcao, foto, Imagens.CAPA, canto);
            opcao.setFocusable(true);
            opcao.setClickable(true);
            if (tv) FocoTv.aplicarComEscala(opcao, null);
            opcao.setOnClickListener(v -> escolher(foto));
            itens.add(opcao);
        }
        Grade.preencher(opcoes, itens, colunas, espaco, espaco);
    }

    /** Uma das fotos: baixa, guarda e fecha. */
    private void escolher(String referencia) {
        if (ocupado) return;
        ocupado = true;
        RepositorioBiblioteca repositorio = RepositorioBiblioteca.atual(this);
        trabalho.execute(() -> {
            boolean ok = repositorio.escolherCapa(chave, referencia);
            runOnUiThread(() -> terminar(ok ? R.string.st_musicas_capa_trocada : R.string.st_musicas_capa_nao_baixou, ok));
        });
    }

    /** Imagem do aparelho: copia (até 15 MB), guarda e fecha; problema vira aviso. */
    private void doAparelho(File arquivo) {
        if (ocupado) return;
        ocupado = true;
        RepositorioBiblioteca repositorio = RepositorioBiblioteca.atual(this);
        trabalho.execute(() -> {
            ImagemDoAparelho.Resultado resultado;
            try {
                resultado = ImagemDoAparelho.doArquivo(this, arquivo);
            } catch (RuntimeException e) {
                resultado = null;
            }
            boolean ok = resultado != null && resultado.ok() && repositorio.escolherCapa(chave, resultado.referencia);
            int aviso = ok ? R.string.st_musicas_capa_trocada
                    : resultado != null && resultado.erro == ImagemDoAparelho.Erro.FORMATO ? R.string.st_imagem_formato
                    : resultado != null && resultado.erro == ImagemDoAparelho.Erro.GRANDE_DEMAIS ? R.string.st_imagem_grande
                    : R.string.st_imagem_ilegivel;
            runOnUiThread(() -> terminar(aviso, ok));
        });
    }

    /** Volta à capa automática (a da música, da pasta ou a foto buscada). */
    private void restaurar() {
        if (ocupado) return;
        Edicoes.de(this).salvar(chave, Edicoes.Campo.CAPA, null);
        terminar(R.string.st_musicas_capa_restaurada, true);
    }

    private void terminar(int aviso, boolean fechar) {
        ocupado = false;
        if (isFinishing() || isDestroyed()) return;
        Toast.makeText(this, aviso, Toast.LENGTH_SHORT).show();
        if (!fechar) return;
        RepositorioMusicas.de(this).refazer();
        finish();
    }
}
