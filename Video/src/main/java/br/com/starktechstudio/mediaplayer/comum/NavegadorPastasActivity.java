// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.archos.mediacenter.video.R;

import java.io.File;
import java.util.List;

import br.com.starktechstudio.mediaplayer.dados.Modelo.Origem;
import br.com.starktechstudio.mediaplayer.dados.RepositorioBiblioteca;
import br.com.starktechstudio.mediaplayer.motor.ArquivosDoAparelho;

/**
 * Adicionar pasta (D15): navegador de pastas próprio, por controle e por toque (F3: as pastas de
 * verdade). Primeiro os armazenamentos (HD externo, armazenamento interno); dentro de cada um,
 * "Adicionar esta pasta", "Voltar uma pasta" e as subpastas (OK ou toque entra). Adicionada, a
 * biblioteca é varrida e a tela fecha. Sem a permissão de ler os vídeos, pede antes.
 * Com {@link #EXTRA_PASTA_IMAGENS} (Configurações): escolhe a pasta de imagens do Editar
 * ("Usar esta pasta"), pedindo a permissão de ler imagens.
 */
public class NavegadorPastasActivity extends AppCompatActivity {

    /** true = escolher a pasta de imagens do Editar, em vez de adicionar uma pasta de vídeos. */
    public static final String EXTRA_PASTA_IMAGENS = "pasta_imagens";
    private static final int PEDIDO_IMAGENS = 7302;

    private boolean tv;
    private boolean modoImagens;
    private LinearLayout opcoes;
    private ScrollView rolagem;
    private TextView titulo;
    private TextView caminho;
    /** Armazenamento aberto e a pasta atual dentro dele (nulos na lista de armazenamentos). */
    private ArquivosDoAparelho.Raiz raiz;
    private File atual;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        tv = Aparelho.ehTv(this);
        modoImagens = getIntent().getBooleanExtra(EXTRA_PASTA_IMAGENS, false);
        setTheme(tv ? R.style.Theme_StarkTech_Menu_Tv : R.style.Theme_StarkTech_Menu);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.st_menu_capa);
        setFinishOnTouchOutside(true);

        Resources res = getResources();
        int padding = res.getDimensionPixelSize(tv ? R.dimen.st_tv_menu_padding : R.dimen.st_tablet_menu_padding);
        LinearLayout caixa = findViewById(R.id.st_menu_caixa);
        ViewGroup.LayoutParams lpCaixa = caixa.getLayoutParams();
        lpCaixa.width = res.getDimensionPixelSize(tv ? R.dimen.st_tv_menu_largura : R.dimen.st_tablet_menu_largura);
        caixa.setLayoutParams(lpCaixa);
        caixa.setPadding(padding, padding, padding, padding);
        int recuo = padding * 2 / 3;
        titulo = findViewById(R.id.st_menu_titulo);
        titulo.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(tv ? R.dimen.st_tv_texto_menu_titulo : R.dimen.st_tablet_texto_menu_titulo));
        titulo.setPadding(recuo, padding / 3, recuo, 0);
        caminho = findViewById(R.id.st_menu_dica);
        caminho.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(tv ? R.dimen.st_tv_texto_menu_dica : R.dimen.st_tablet_texto_menu_dica));
        caminho.setPadding(recuo, 0, recuo, recuo);

        // Muitas subpastas: a lista rola dentro da caixa (até 70 % da altura da tela)
        opcoes = findViewById(R.id.st_menu_opcoes);
        int posicao = caixa.indexOfChild(opcoes);
        caixa.removeView(opcoes);
        rolagem = new RolagemLimitada(this, Math.round(res.getDisplayMetrics().heightPixels * 0.7f));
        rolagem.setVerticalScrollBarEnabled(false);
        rolagem.setOverScrollMode(View.OVER_SCROLL_NEVER);
        rolagem.addView(opcoes, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        caixa.addView(rolagem, posicao, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        mostrarArmazenamentos();
        if (!modoImagens) {
            PermissaoVideos.pedirSeFaltar(this);
        } else if (ContextCompat.checkSelfPermission(this, ArquivosDoAparelho.permissaoDeImagens()) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{ArquivosDoAparelho.permissaoDeImagens()}, PEDIDO_IMAGENS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int pedido, @NonNull String[] permissoes, @NonNull int[] respostas) {
        super.onRequestPermissionsResult(pedido, permissoes, respostas);
        if (pedido == PEDIDO_IMAGENS) {
            if (respostas.length == 0 || respostas[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.st_imagem_sem_permissao, Toast.LENGTH_LONG).show();
            }
            return;
        }
        if (pedido != PermissaoVideos.PEDIDO) return;
        if (PermissaoVideos.aoResponder(this, pedido, respostas)) {
            if (atual == null) mostrarArmazenamentos();
            else mostrarPasta(atual);
        } else {
            Toast.makeText(this, R.string.st_videos_sem_permissao, Toast.LENGTH_LONG).show();
        }
    }

    private void mostrarArmazenamentos() {
        raiz = null;
        atual = null;
        titulo.setText(modoImagens ? R.string.st_cfg_pasta_imagens : R.string.st_escolher_pasta);
        caminho.setText(modoImagens ? R.string.st_cfg_pasta_imagens_dica : R.string.st_adicionar_pasta);
        opcoes.removeAllViews();
        View primeira = null;
        // HD externo (pen drive) primeiro, como no desenho; depois o interno
        List<ArquivosDoAparelho.Raiz> raizes = ArquivosDoAparelho.raizes(this);
        for (boolean removiveis : new boolean[]{true, false}) {
            for (ArquivosDoAparelho.Raiz r : raizes) {
                if (r.removivel != removiveis) continue;
                View item = opcao(r.removivel ? R.drawable.st_ic_hd : R.drawable.st_ic_pastas, nomeDaRaiz(r, raizes), () -> {
                    raiz = r;
                    mostrarPasta(r.pasta);
                });
                if (primeira == null) primeira = item;
            }
        }
        if (primeira == null) aviso(getString(R.string.st_sem_armazenamento));
        else if (tv) primeira.requestFocus();
        rolagem.scrollTo(0, 0);
    }

    /** "HD externo (USB)"; com mais de um, o nome do volume junto. */
    private String nomeDaRaiz(ArquivosDoAparelho.Raiz r, List<ArquivosDoAparelho.Raiz> todas) {
        if (!r.removivel) return getString(R.string.st_armazenamento_interno);
        int removiveis = 0;
        for (ArquivosDoAparelho.Raiz outra : todas) if (outra.removivel) removiveis++;
        String nome = getString(R.string.st_hd_externo_usb);
        return removiveis > 1 ? nome + " · " + r.pasta.getName() : nome;
    }

    private void mostrarPasta(File pasta) {
        atual = pasta;
        boolean naRaiz = pasta.equals(raiz.pasta);
        titulo.setText(naRaiz ? (raiz.removivel ? getString(R.string.st_hd_externo_usb) : getString(R.string.st_armazenamento_interno))
                : pasta.getName());
        String exibido = exibir(pasta);
        caminho.setText(exibido);
        opcoes.removeAllViews();
        View adicionar = opcao(R.drawable.st_ic_check,
                getString(modoImagens ? R.string.st_usar_esta_pasta : R.string.st_adicionar_esta_pasta), () -> {
            if (modoImagens) {
                Preferencias.definirPastaImagens(this, pasta.getAbsolutePath());
                Toast.makeText(this, getString(R.string.st_pasta_imagens_escolhida, exibido), Toast.LENGTH_SHORT).show();
            } else {
                RepositorioBiblioteca.atual(this).adicionarPasta(raiz.removivel ? Origem.HD_EXTERNO : Origem.INTERNO,
                        pasta.getAbsolutePath());
                Toast.makeText(this, getString(R.string.st_pasta_adicionada, exibido), Toast.LENGTH_SHORT).show();
            }
            finish();
        });
        opcao(R.drawable.st_ic_voltar, getString(R.string.st_subir), () -> {
            if (naRaiz) mostrarArmazenamentos();
            else mostrarPasta(pasta.getParentFile());
        });
        List<File> subpastas = ArquivosDoAparelho.listar(pasta, false);
        for (File sub : subpastas) opcao(R.drawable.st_ic_pastas, sub.getName(), () -> mostrarPasta(sub));
        if (subpastas.isEmpty()) aviso(getString(R.string.st_sem_subpastas));
        if (tv) adicionar.requestFocus();
        rolagem.scrollTo(0, 0);
    }

    /** "HD / Filmes / 2026": a origem e o caminho a partir da raiz do armazenamento. */
    private String exibir(File pasta) {
        StringBuilder texto = new StringBuilder(raiz.removivel ? "HD" : "Interno");
        String base = raiz.pasta.getAbsolutePath();
        String resto = pasta.getAbsolutePath().length() > base.length() ? pasta.getAbsolutePath().substring(base.length()) : "";
        for (String parte : resto.split("/")) {
            if (!parte.isEmpty()) texto.append(" / ").append(parte);
        }
        return texto.toString();
    }

    private View opcao(@DrawableRes int icone, String texto, Runnable acao) {
        return OpcaoMenu.adicionar(opcoes, icone, texto, acao);
    }

    /** Linha de aviso no lugar das opções (sem foco). */
    private void aviso(String texto) {
        TextView linha = new TextView(this);
        linha.setText(texto);
        linha.setTextColor(getColor(R.color.st_texto_apagado));
        linha.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(
                tv ? R.dimen.st_tv_texto_menu_dica : R.dimen.st_tablet_texto_menu_dica));
        int recuo = getResources().getDimensionPixelSize(tv ? R.dimen.st_tv_menu_padding : R.dimen.st_tablet_menu_padding) * 2 / 3;
        linha.setPadding(recuo, recuo / 2, recuo, recuo / 2);
        opcoes.addView(linha);
    }

    /** Rolagem que não passa de {@code maximo} de altura. */
    private static final class RolagemLimitada extends ScrollView {
        private final int maximo;

        RolagemLimitada(Context contexto, int maximo) {
            super(contexto);
            this.maximo = maximo;
        }

        @Override
        protected void onMeasure(int largura, int altura) {
            super.onMeasure(largura, MeasureSpec.makeMeasureSpec(maximo, MeasureSpec.AT_MOST));
        }
    }
}
