// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import com.archos.mediacenter.video.R;

/**
 * Sobre (D17, sem desenho; no padrão das outras telas): ícone, nome e versão do app, descrição
 * (DADOS §2) e site; Créditos ("Baseado no Nova Video Player", Apache-2.0); avisos de terceiros, um
 * por biblioteca (docs/arquitetura/licencas.md §2 e §6), os primeiros e "Ver todos"; logo e aviso
 * do TMDB; direitos. A logo StarkTech não entra aqui (D5: só no rodapé da TV e na barra do tablet).
 * Os textos completos das licenças entram na F4.
 */
public class SobreActivity extends AppCompatActivity {

    /** Biblioteca e licença (licencas.md §2 e §6; jcifs-ng ainda a confirmar). */
    private static final String[][] TERCEIROS = {
            {"Nova Video Player", "Apache-2.0"},
            {"FFmpeg", "LGPL 2.1 ou posterior"},
            {"dav1d", "BSD-2-Clause"},
            {"libass", "ISC"},
            {"FreeType", "FreeType License (FTL) · Portions of this software are copyright © The FreeType Project (https://freetype.org). All rights reserved."},
            {"Inter", "SIL Open Font License 1.1 · © 2016 The Inter Project Authors"},
            {"Lucide", "ISC"},
            {"Opus", "BSD-3-Clause"},
            {"libmysofa", "BSD-3-Clause"},
            {"OpenSSL", "Apache-2.0"},
            {"FriBidi", "LGPL-2.1"},
            {"HarfBuzz", "Old MIT"},
            {"fontconfig", "Licença do fontconfig"},
            {"libunibreak", "zlib"},
            {"libxml2", "MIT"},
            {"libpng", "PNG Reference Library License v2"},
            {"zlib", "zlib"},
            {"AudioCompress", "LGPL-2.1"},
            {"libnativehelper (AOSP)", "Apache-2.0"},
            {"libtorrent", "BSD-3-Clause"},
            {"Boost", "Boost Software License 1.0"},
            {"jcifs-ng", "LGPL-2.1"},
    };
    private static final int MOSTRADOS = 7;

    private boolean tv;
    private LinearLayout avisos;
    private boolean todos;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(Aparelho.tema(this));
        super.onCreate(savedInstanceState);
        tv = Aparelho.ehTv(this);

        FrameLayout raiz = new FrameLayout(this);
        raiz.setBackgroundResource(R.color.st_fundo);
        raiz.addView(new FundoAnimado(this), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        LinearLayout pagina = new LinearLayout(this);
        pagina.setOrientation(LinearLayout.VERTICAL);
        int lateral = getResources().getDimensionPixelSize(tv ? R.dimen.st_tv_margem_lateral : R.dimen.st_tablet_margem_h);
        pagina.setPadding(lateral, Medidas.px(this, 44), lateral, Medidas.px(this, 44));
        if (!tv) BordasSistema.aplicar(pagina, true, true, true, true);

        pagina.addView(topo());
        pagina.addView(aplicativo(), comTopo(Medidas.px(this, 32)));

        pagina.addView(Textos.rotulo(this, getString(R.string.st_creditos), 14), comTopo(Medidas.px(this, 36)));
        pagina.addView(Textos.novo(this, getString(R.string.st_creditos_nova), R.font.st_inter_semibold, 19, R.color.st_texto_secundario),
                comTopo(Medidas.px(this, 10)));

        pagina.addView(Textos.rotulo(this, getString(R.string.st_avisos_terceiros), 14), comTopo(Medidas.px(this, 36)));
        avisos = new LinearLayout(this);
        avisos.setOrientation(LinearLayout.VERTICAL);
        pagina.addView(avisos, comTopo(Medidas.px(this, 10)));
        mostrarAvisos();

        pagina.addView(tmdb(), comTopo(Medidas.px(this, 36)));
        pagina.addView(Textos.novo(this, getString(R.string.st_direitos), R.font.st_inter_semibold, 16, R.color.st_texto_apagado),
                comTopo(Medidas.px(this, 36)));

        ScrollView rolagem = new ScrollView(this);
        rolagem.setFocusable(false);
        rolagem.setVerticalScrollBarEnabled(false);
        rolagem.setOverScrollMode(View.OVER_SCROLL_NEVER);
        rolagem.addView(pagina, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        raiz.addView(rolagem, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(raiz);
    }

    private View topo() {
        LinearLayout topo = new LinearLayout(this);
        topo.setOrientation(LinearLayout.HORIZONTAL);
        topo.setGravity(Gravity.CENTER_VERTICAL);
        ImageButton voltar = new ImageButton(this);
        voltar.setImageResource(R.drawable.st_ic_voltar);
        ImageViewCompat.setImageTintList(voltar, ContextCompat.getColorStateList(this, R.color.st_texto));
        voltar.setBackgroundResource(R.drawable.st_botao_secundario);
        voltar.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int lado = getResources().getDimensionPixelSize(tv ? R.dimen.st_tv_botao_icone_altura : R.dimen.st_tablet_detalhe_topo_botao_altura);
        voltar.setPadding(lado / 4, lado / 4, lado / 4, lado / 4);
        voltar.setContentDescription(getString(R.string.st_voltar));
        if (tv) voltar.setForeground(ContextCompat.getDrawable(this, R.drawable.st_foco_tv));
        voltar.setOnClickListener(v -> finish());
        topo.addView(voltar, new LinearLayout.LayoutParams(lado, lado));
        TextView titulo = Textos.novo(this, getString(R.string.st_cfg_sobre), R.font.st_inter_black, tv ? 48 : 43, R.color.st_texto);
        titulo.setLetterSpacing(-0.02f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMarginStart(Medidas.px(this, 24));
        topo.addView(titulo, lp);
        if (tv) voltar.post(voltar::requestFocus);
        return topo;
    }

    /** Ícone do app, nome, versão (do pacote), descrição e site. */
    private View aplicativo() {
        LinearLayout bloco = new LinearLayout(this);
        bloco.setOrientation(LinearLayout.HORIZONTAL);
        bloco.setGravity(Gravity.CENTER_VERTICAL);
        ImageView icone = new ImageView(this);
        icone.setImageResource(R.mipmap.st_ic_launcher);
        int lado = Medidas.px(this, 128);
        bloco.addView(icone, new LinearLayout.LayoutParams(lado, lado));

        LinearLayout textos = new LinearLayout(this);
        textos.setOrientation(LinearLayout.VERTICAL);
        TextView nome = Textos.novo(this, getString(R.string.nova), R.font.st_inter_black, 34, R.color.st_texto);
        nome.setLetterSpacing(-0.02f);
        textos.addView(nome);
        textos.addView(Textos.novo(this, getString(R.string.st_versao, versao()), R.font.st_inter_bold, 18, R.color.st_ciano),
                comTopo(Medidas.px(this, 6)));
        TextView descricao = Textos.novo(this, getString(R.string.st_sobre_descricao), R.font.st_inter_medium, 19, R.color.st_texto_secundario);
        descricao.setLineSpacing(0f, 1.24f);
        descricao.setMaxWidth(Medidas.px(this, 1100));
        textos.addView(descricao, comTopo(Medidas.px(this, 12)));
        textos.addView(Textos.novo(this, getString(R.string.st_rodape_site), R.font.st_inter_bold, 18, R.color.st_azul),
                comTopo(Medidas.px(this, 10)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMarginStart(Medidas.px(this, 32));
        bloco.addView(textos, lp);
        return bloco;
    }

    /** Um aviso por biblioteca; "Ver todos" mostra a lista inteira. */
    private void mostrarAvisos() {
        avisos.removeAllViews();
        int quantos = todos ? TERCEIROS.length : Math.min(MOSTRADOS, TERCEIROS.length);
        for (int i = 0; i < quantos; i++) {
            LinearLayout linha = new LinearLayout(this);
            linha.setOrientation(LinearLayout.VERTICAL);
            if (tv) {
                // Na TV cada aviso recebe o foco: o controle desce a página por eles
                linha.setFocusable(true);
                linha.setForeground(ContextCompat.getDrawable(this, R.drawable.st_foco_tv));
                int folga = Medidas.px(this, 8);
                linha.setPadding(folga, folga, folga, folga);
            }
            linha.addView(Textos.novo(this, TERCEIROS[i][0], R.font.st_inter_extrabold, 18, R.color.st_texto));
            linha.addView(Textos.novo(this, TERCEIROS[i][1], R.font.st_inter_semibold, 16, R.color.st_texto_apagado),
                    comTopo(Medidas.px(this, 4)));
            avisos.addView(linha, comTopo(i == 0 ? 0 : Medidas.px(this, tv ? 6 : 14)));
        }
        if (!todos && TERCEIROS.length > MOSTRADOS) {
            int estilo = tv ? R.style.StarkTech_Botao_Tv_Secundario : R.style.StarkTech_Botao_Tablet_Secundario;
            Button verTodos = new Button(this, null, 0, estilo);
            verTodos.setText(R.string.st_ver_todos);
            verTodos.setOnClickListener(v -> {
                todos = true;
                mostrarAvisos();
                if (tv) avisos.getChildAt(MOSTRADOS).requestFocus();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    getResources().getDimensionPixelSize(tv ? R.dimen.st_tv_botao_altura : R.dimen.st_tablet_botao_altura));
            lp.topMargin = Medidas.px(this, 18);
            avisos.addView(verTodos, lp);
        }
    }

    /** Logo do TMDB (o do Nova; conferir na F4 se é o aprovado pelo TMDB) e o aviso exigido. */
    private View tmdb() {
        LinearLayout bloco = new LinearLayout(this);
        bloco.setOrientation(LinearLayout.VERTICAL);
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.tmdb_banner);
        logo.setAdjustViewBounds(true);
        logo.setContentDescription(getString(R.string.st_logo_tmdb));
        bloco.addView(logo, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, Medidas.px(this, 48)));
        TextView aviso = Textos.novo(this, getString(R.string.st_aviso_tmdb), R.font.st_inter_semibold, 17, R.color.st_texto_secundario);
        bloco.addView(aviso, comTopo(Medidas.px(this, 12)));
        return bloco;
    }

    private String versao() {
        try {
            String nome = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            return nome == null ? "" : nome;
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }

    private static LinearLayout.LayoutParams comTopo(int topo) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = topo;
        return lp;
    }
}
