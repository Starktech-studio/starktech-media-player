// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.SuperscriptSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.archos.mediacenter.video.R;

import br.com.starktechstudio.mediaplayer.dados.RepositorioBiblioteca;

/**
 * Tela de carregamento no padrão StarkTech, ao abrir o app (TV e tablet): fundo #020617 com o
 * fundo animado, o ícone do app no centro, "StarkTech" branco e "Media Player" amarelo #E8AA30
 * (como no banner), "Carregando biblioteca…" e, no rodapé, "StarkTech Studio®" e a versão.
 * Fica por cima da tela principal enquanto a biblioteca carrega (fora da tela); some quando ela
 * fica pronta — ao menos 0,7 s, para não piscar, e no máximo 2 s, contados do primeiro quadro em
 * que a tela aparece. Com "remover animações",
 * aparece e some sem animação. Enquanto está na tela, segura os toques; as teclas da TV a tela
 * principal segura por {@link #aberta()}.
 */
public final class TelaCarregamento {

    static final long MINIMO_MS = 700;
    static final long MAXIMO_MS = 2000;
    private static final long FADE_MS = 250;

    private final Handler relogio = new Handler(Looper.getMainLooper());
    private final boolean animar;
    /** Ícone, marca e status (entram juntos). */
    private LinearLayout centro;
    private final FrameLayout camada;
    private boolean aberta = true;

    private TelaCarregamento(Activity tela) {
        animar = Animacoes.ligadas(tela);
        camada = montar(tela);
    }

    /** Põe a tela por cima do conteúdo de {@code tela} e começa a carregar a biblioteca. */
    public static TelaCarregamento mostrar(Activity tela) {
        TelaCarregamento carregamento = new TelaCarregamento(tela);
        ViewGroup raiz = tela.findViewById(android.R.id.content);
        raiz.addView(carregamento.camada, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        carregamento.carregar(tela.getApplicationContext());
        return carregamento;
    }

    public boolean aberta() {
        return aberta;
    }

    /**
     * A abertura a frio leva segundos até o primeiro quadro (no emulador, 4 a 6 s): a contagem do
     * mínimo e do máximo e a entrada começam quando a tela aparece de fato, não no onCreate.
     */
    private void carregar(Context app) {
        camada.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                camada.getViewTreeObserver().removeOnPreDrawListener(this);
                comecar(app);
                return true;
            }
        });
    }

    private void comecar(Context app) {
        if (animar) {
            centro.setAlpha(0f);
            centro.setTranslationY(Medidas.px(camada.getContext(), 16));
            centro.animate().alpha(1f).translationY(0f).setDuration(300).start();
        }
        long inicio = SystemClock.uptimeMillis();
        relogio.postDelayed(this::esconder, MAXIMO_MS);
        // F1/F2: os dados simulados (e o banco do app) ficam prontos aqui; na F3, a biblioteca real
        new Thread(() -> {
            RepositorioBiblioteca.atual(app);
            long falta = Math.max(0, MINIMO_MS - (SystemClock.uptimeMillis() - inicio));
            relogio.postDelayed(this::esconder, falta);
        }, "StarkTechCarregamento").start();
    }

    private void esconder() {
        if (!aberta) return;
        aberta = false;
        relogio.removeCallbacksAndMessages(null);
        if (!animar) {
            remover();
            return;
        }
        camada.animate().alpha(0f).setDuration(FADE_MS).withEndAction(this::remover).start();
    }

    private void remover() {
        ViewGroup pai = (ViewGroup) camada.getParent();
        if (pai != null) pai.removeView(camada);
    }

    private FrameLayout montar(Activity tela) {
        FrameLayout raiz = new FrameLayout(tela);
        raiz.setBackgroundResource(R.color.st_fundo);
        raiz.setClickable(true); // os toques não passam para a tela de baixo
        raiz.addView(new FundoAnimado(tela), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout centro = new LinearLayout(tela);
        centro.setOrientation(LinearLayout.VERTICAL);
        centro.setGravity(Gravity.CENTER_HORIZONTAL);
        ImageView icone = new ImageView(tela);
        icone.setImageResource(R.drawable.st_icone_app);
        icone.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        int lado = Medidas.px(tela, 200);
        centro.addView(icone, new LinearLayout.LayoutParams(lado, lado));

        TextView marca = Textos.novo(tela, tela.getString(R.string.st_marca), R.font.st_inter_bold, 60, R.color.st_texto);
        centro.addView(marca, comTopo(Medidas.px(tela, 24)));
        TextView produto = Textos.novo(tela, tela.getString(R.string.st_marca_media_player), R.font.st_inter_extrabold, 36,
                R.color.st_amarelo_marca);
        centro.addView(produto, comTopo(Medidas.px(tela, 4)));
        TextView status = Textos.novo(tela, tela.getString(R.string.st_carregando_biblioteca), R.font.st_inter_semibold, 22,
                R.color.st_texto_apagado);
        centro.addView(status, comTopo(Medidas.px(tela, 40)));
        raiz.addView(centro, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        TextView rodape = Textos.novo(tela, rodape(tela), R.font.st_inter_semibold, 18, R.color.st_texto_apagado);
        rodape.setGravity(Gravity.CENTER);
        int margem = Medidas.px(tela, 40);
        rodape.setPadding(margem, 0, margem, margem);
        FrameLayout.LayoutParams lpRodape = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
        raiz.addView(rodape, lpRodape);
        // Tablet: o rodapé fica acima da barra de gestos (o app desenha por baixo das barras)
        BordasSistema.aplicar(rodape, false, false, false, true);
        // Até o primeiro quadro o centro fica escondido; a entrada roda em comecar()
        if (animar) centro.setAlpha(0f);
        this.centro = centro;
        return raiz;
    }

    /** "StarkTech Studio® · versão 0.1.0", com o ® pequeno e alto. */
    private static CharSequence rodape(Context contexto) {
        SpannableStringBuilder empresa = new SpannableStringBuilder(contexto.getString(R.string.st_rodape_empresa));
        int inicio = empresa.length();
        empresa.append(contexto.getString(R.string.st_rodape_registrada));
        empresa.setSpan(new SuperscriptSpan(), inicio, empresa.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        empresa.setSpan(new RelativeSizeSpan(0.6f), inicio, empresa.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        String versao = "";
        try {
            String nome = contexto.getPackageManager().getPackageInfo(contexto.getPackageName(), 0).versionName;
            if (nome != null) versao = nome;
        } catch (PackageManager.NameNotFoundException ignorado) {
            // sem a versão, o rodapé fica só com a empresa
        }
        String modelo = contexto.getString(R.string.st_carregamento_rodape);
        int corte = modelo.indexOf("%1$s");
        SpannableStringBuilder texto = new SpannableStringBuilder(modelo.substring(0, corte));
        texto.append(empresa);
        texto.append(modelo.substring(corte + 4).replace("%2$s", versao));
        return texto;
    }

    private static LinearLayout.LayoutParams comTopo(int topo) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = topo;
        return lp;
    }
}
