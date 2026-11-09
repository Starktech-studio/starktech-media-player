// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.TypedValue;
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
import androidx.annotation.RawRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import com.archos.mediacenter.video.R;
import com.archos.medialib.LibAvos;

import java.util.Arrays;
import java.util.List;

/**
 * Sobre (D17, sem desenho; no padrão das outras telas): ícone, nome e versão do app, descrição
 * (DADOS §2) e site; Créditos ("Baseado no Nova Video Player", Apache-2.0); avisos de terceiros, um
 * por biblioteca (docs/arquitetura/licencas.md §2, §6 e §9), os primeiros e "Ver todos", e OK ou toque num
 * aviso abre o texto completo da licença ({@link LicencaActivity}, 1.1.1); onde está o código-fonte; logo
 * e aviso do TMDB; fonte dos dados de artista; direitos. A logo StarkTech não entra aqui (D5: só no rodapé
 * da TV e na barra do tablet).
 */
public class SobreActivity extends AppCompatActivity {

    /** Uma biblioteca (ou um grupo com a mesma licença), a licença e o texto completo em res/raw. */
    private static final class Aviso {
        final String nome;
        final String licenca;
        @RawRes
        final int texto;

        Aviso(String nome, String licenca, @RawRes int texto) {
            this.nome = nome;
            this.licenca = licenca;
            this.texto = texto;
        }
    }

    private static final int MOSTRADOS = 7;

    /** Nativos, recursos e bibliotecas Java do APK (licencas.md §2, §6 e §9). */
    private List<Aviso> terceiros() {
        return Arrays.asList(
                new Aviso("Nova Video Player", "Apache-2.0", R.raw.st_lic_apache_2_0),
                new Aviso("FFmpeg", "LGPL 2.1 ou posterior", R.raw.st_lic_lgpl_2_1),
                new Aviso("dav1d", "BSD-2-Clause", R.raw.st_lic_dav1d),
                new Aviso("libass", "ISC", R.raw.st_lic_libass),
                new Aviso("FreeType", "FreeType License (FTL) · Portions of this software are copyright © The FreeType Project (https://freetype.org). All rights reserved.", R.raw.st_lic_freetype),
                new Aviso("Inter", "SIL Open Font License 1.1 · © 2016 The Inter Project Authors", R.raw.st_licenca_inter),
                new Aviso("Lucide", "ISC", R.raw.st_lic_lucide),
                new Aviso("Opus", "BSD-3-Clause", R.raw.st_lic_opus),
                new Aviso("libmysofa", "BSD-3-Clause", R.raw.st_lic_libmysofa),
                new Aviso("OpenSSL", "Apache-2.0", R.raw.st_lic_apache_2_0),
                new Aviso("FriBidi", "LGPL-2.1", R.raw.st_lic_lgpl_2_1),
                new Aviso("HarfBuzz", "Old MIT", R.raw.st_lic_harfbuzz),
                new Aviso("fontconfig", "Licença do fontconfig", R.raw.st_lic_fontconfig),
                new Aviso("libunibreak", "zlib", R.raw.st_lic_libunibreak),
                new Aviso("libxml2", "MIT", R.raw.st_lic_libxml2),
                new Aviso("libpng", "PNG Reference Library License v2", R.raw.st_lic_libpng),
                new Aviso("zlib", "zlib", R.raw.st_lic_zlib),
                new Aviso("AudioCompress", "LGPL-2.1", R.raw.st_lic_lgpl_2_1),
                new Aviso("libnativehelper (AOSP)", "Apache-2.0 · NOTICE", R.raw.st_lic_libnativehelper),
                new Aviso("libtorrent", "BSD-3-Clause", R.raw.st_lic_libtorrent),
                new Aviso("Boost", "Boost Software License 1.0", R.raw.st_lic_boost),
                new Aviso("jcifs-ng", "LGPL-2.1", R.raw.st_lic_lgpl_2_1),
                new Aviso("JSch, JZlib e jBCrypt", "BSD revisada e ISC", R.raw.st_lic_jsch),
                new Aviso("jUPnP", "CDDL-1.0", R.raw.st_lic_jupnp),
                new Aviso("Java Servlet API", "CDDL 1.1 ou GPLv2 com a exceção de classpath", R.raw.st_lic_servlet),
                new Aviso("Bouncy Castle", "Licença do Bouncy Castle (tipo MIT)", R.raw.st_lic_bouncycastle),
                new Aviso("SLF4J", "MIT", R.raw.st_lic_slf4j),
                new Aviso("ThreeTen Backport", "BSD-3-Clause", R.raw.st_lic_threetenbp),
                new Aviso("Jakarta ORO", "Apache-1.1", R.raw.st_lic_oro),
                new Aviso("Sentry", "MIT", R.raw.st_lic_sentry),
                new Aviso("MBassador", "MIT", R.raw.st_lic_mbassador),
                new Aviso("Epitaph", "MIT", R.raw.st_lic_epitaph),
                new Aviso(getString(R.string.st_bibliotecas_java), "Apache-2.0", R.raw.st_lic_apache_2_0));
    }

    private List<Aviso> avisosTerceiros;

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
        pagina.addView(Textos.novo(this, getString(R.string.st_avisos_dica), R.font.st_inter_semibold, 16, R.color.st_texto_apagado),
                comTopo(Medidas.px(this, 6)));
        avisosTerceiros = terceiros();
        avisos = new LinearLayout(this);
        avisos.setOrientation(LinearLayout.VERTICAL);
        pagina.addView(avisos, comTopo(Medidas.px(this, 10)));
        mostrarAvisos();
        TextView codigo = Textos.novo(this, getString(R.string.st_codigo_fonte), R.font.st_inter_semibold, 17, R.color.st_texto_secundario);
        codigo.setLineSpacing(0f, 1.2f);
        codigo.setMaxWidth(Medidas.px(this, 1400));
        pagina.addView(codigo, comTopo(Medidas.px(this, 24)));

        pagina.addView(tmdb(), comTopo(Medidas.px(this, 36)));
        pagina.addView(Textos.novo(this, getString(R.string.st_credito_artistas), R.font.st_inter_semibold, 17, R.color.st_texto_secundario),
                comTopo(Medidas.px(this, 18)));
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
        TextView titulo = Textos.novo(this, getString(R.string.st_cfg_sobre), R.font.st_inter_black,
                tv ? Medidas.TITULO_PAGINA_TV : Medidas.TITULO_PAGINA_TABLET, R.color.st_texto);
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
        // Motor de vídeo em uso: o do app (FFmpeg, capítulos, legendas) ou o do Android (o do app não carregou)
        LibAvos.init(getApplicationContext());
        textos.addView(Textos.novo(this, getString(LibAvos.isAvailable() ? R.string.st_motor_app : R.string.st_motor_android),
                R.font.st_inter_semibold, 16, LibAvos.isAvailable() ? R.color.st_texto_apagado : R.color.st_aviso_claro),
                comTopo(Medidas.px(this, 4)));
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

    /** Um aviso por biblioteca; "Ver todos" mostra a lista inteira. OK ou toque abre o texto da licença. */
    private void mostrarAvisos() {
        avisos.removeAllViews();
        int quantos = todos ? avisosTerceiros.size() : Math.min(MOSTRADOS, avisosTerceiros.size());
        int folga = Medidas.px(this, 8);
        for (int i = 0; i < quantos; i++) {
            final Aviso aviso = avisosTerceiros.get(i);
            LinearLayout linha = new LinearLayout(this);
            linha.setOrientation(LinearLayout.VERTICAL);
            linha.setPadding(folga, folga, folga, folga);
            linha.setFocusable(true);
            linha.setClickable(true);
            if (tv) {
                // Na TV cada aviso recebe o foco: o controle desce a página por eles
                linha.setForeground(ContextCompat.getDrawable(this, R.drawable.st_foco_tv));
            } else {
                TypedValue realce = new TypedValue();
                getTheme().resolveAttribute(android.R.attr.selectableItemBackground, realce, true);
                linha.setForeground(ContextCompat.getDrawable(this, realce.resourceId));
            }
            linha.setOnClickListener(v -> startActivity(new Intent(this, LicencaActivity.class)
                    .putExtra(LicencaActivity.EXTRA_NOME, aviso.nome)
                    .putExtra(LicencaActivity.EXTRA_LICENCA, aviso.licenca)
                    .putExtra(LicencaActivity.EXTRA_TEXTO, aviso.texto)));
            linha.addView(Textos.novo(this, aviso.nome, R.font.st_inter_extrabold, 18, R.color.st_texto));
            linha.addView(Textos.novo(this, aviso.licenca, R.font.st_inter_semibold, 16, R.color.st_texto_apagado),
                    comTopo(Medidas.px(this, 4)));
            avisos.addView(linha, comTopo(i == 0 ? 0 : Medidas.px(this, tv ? 6 : 6)));
        }
        if (!todos && avisosTerceiros.size() > MOSTRADOS) {
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

    /** Logo do TMDB (o "TMDB" com a pílula em degradê, um dos logos da página de marca do TMDB) e o aviso exigido. */
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
