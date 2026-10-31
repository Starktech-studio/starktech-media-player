// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.tv;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.widget.ImageViewCompat;

import com.archos.mediacenter.video.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import br.com.starktechstudio.mediaplayer.comum.CapaAdapter;
import br.com.starktechstudio.mediaplayer.comum.CartaoEpisodio;
import br.com.starktechstudio.mediaplayer.comum.Grade;
import br.com.starktechstudio.mediaplayer.comum.Icones;
import br.com.starktechstudio.mediaplayer.comum.Navegacao;
import br.com.starktechstudio.mediaplayer.comum.Navegacao.Aba;
import br.com.starktechstudio.mediaplayer.comum.PecasDetalhe;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Episodio;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Temporada;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Titulo;
import br.com.starktechstudio.mediaplayer.dados.RepositorioBiblioteca;

/**
 * Série na TV (tv-serie.dc.html): fundo da série atrás (bloco de cor na F1, véu e degradê); no
 * topo, Séries marcada ({@link TopoSecaoTv}) e Editar só com o ícone; capa, título, chips (temporadas, episódios, qualidade), aviso
 * da pasta, botão do próximo episódio ("Continuar · Episódio 3") e Favoritar; as temporadas em
 * pílulas (a pílula em foco vira a temporada mostrada, como no topo da tela principal) e os
 * episódios em 2 colunas, com o estado de cada um: Assistido, Em andamento ou novo.
 */
public class SerieTvActivity extends AppCompatActivity {

    private RepositorioBiblioteca repositorio;
    private String id;
    private Titulo serie;
    private List<Temporada> temporadas = new ArrayList<>();
    private int temporadaAtiva = -1;
    private boolean primeiraVez = true;

    private View fundo;
    private View capa;
    private TextView nome;
    private LinearLayout chips;
    private TextView aviso;
    private Button principal;
    private ImageButton favoritar;
    private LinearLayout pilulasTemporada;
    private LinearLayout episodios;
    private Episodio proximo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repositorio = RepositorioBiblioteca.atual(this);
        id = getIntent().getStringExtra(Navegacao.EXTRA_ID);
        if (repositorio.titulo(id) == null) {
            finish();
            return;
        }
        setContentView(R.layout.st_tv_serie);

        fundo = findViewById(R.id.st_serie_fundo);
        capa = findViewById(R.id.st_serie_capa);
        nome = findViewById(R.id.st_serie_titulo);
        chips = findViewById(R.id.st_serie_chips);
        aviso = findViewById(R.id.st_serie_aviso);
        principal = findViewById(R.id.st_serie_principal);
        favoritar = findViewById(R.id.st_serie_favoritar);
        pilulasTemporada = findViewById(R.id.st_serie_temporadas);
        episodios = findViewById(R.id.st_serie_episodios);
        View editar = findViewById(R.id.st_serie_editar);

        // Topo: Séries marcada; ↑ dos botões vai a ela (não a Editar), ↓ das pílulas ao botão principal
        TopoSecaoTv topo = new TopoSecaoTv(this);
        topo.marcar(Aba.SERIES, R.id.st_serie_principal);
        principal.setNextFocusUpId(topo.idAtiva());
        favoritar.setNextFocusUpId(topo.idAtiva());

        editar.setOnClickListener(v -> Navegacao.abrirEditar(this, id));
        findViewById(R.id.st_botao_configuracoes).setOnClickListener(v -> Navegacao.abrirConfiguracoes(this));
        principal.setOnClickListener(v -> {
            if (proximo != null) Navegacao.abrirReproducao(this, proximo.id, Navegacao.DE_ONDE_PAROU);
        });
        favoritar.setOnClickListener(v -> {
            repositorio.definirFavorito(id, !serie.favorito);
            carregar();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Volta do player ou de Editar: o estado simulado (assistido, posição) pode ter mudado
        carregar();
        if (primeiraVez) {
            primeiraVez = false;
            principal.requestFocus();
        }
    }

    private void carregar() {
        serie = repositorio.titulo(id);
        if (serie == null) {
            finish();
            return;
        }
        temporadas = repositorio.temporadas(id);
        proximo = PecasDetalhe.proximoEpisodio(temporadas);

        fundo.setBackground(PecasDetalhe.fundoDoTitulo(this, serie.corFundo));
        capa.setBackground(CapaAdapter.blocoDeCor(this, serie.corCapa,
                getResources().getDimension(R.dimen.st_tv_serie_capa_canto), true));
        nome.setText(serie.titulo);
        List<String> textos = new ArrayList<>(Arrays.asList(
                getResources().getQuantityString(R.plurals.st_n_temporadas, serie.numeroTemporadas, serie.numeroTemporadas),
                getResources().getQuantityString(R.plurals.st_n_episodios, serie.numeroEpisodios, serie.numeroEpisodios)));
        if (serie.qualidade != null && !serie.qualidade.isEmpty()) textos.add(serie.qualidade);
        List<Boolean> azuis = new ArrayList<>();
        for (int i = 0; i < textos.size(); i++) azuis.add(false);
        PecasDetalhe.chips(chips, textos, azuis, true);
        PecasDetalhe.avisoPasta(aviso, serie, true);

        principal.setVisibility(proximo == null ? View.GONE : View.VISIBLE);
        if (proximo != null) {
            principal.setText(PecasDetalhe.textoProximo(this, proximo));
            Icones.noTexto(principal, R.drawable.st_ic_assistir, getResources().getDimensionPixelSize(R.dimen.st_tv_botao_icone));
        }
        favoritar.setContentDescription(getString(serie.favorito ? R.string.st_remover_favoritos : R.string.st_favoritar));
        ImageViewCompat.setImageTintList(favoritar, ColorStateList.valueOf(
                ContextCompat.getColor(this, serie.favorito ? R.color.st_favorito : R.color.st_texto)));

        // A grade é refeita: o episódio em foco (volta do player) recebe o foco de novo
        int focado = indiceDoCartaoEmFoco();
        criarPilulas();
        int inicial = temporadaAtiva;
        if (inicial < 0 || inicial >= temporadas.size()) inicial = indiceDaTemporada(proximo);
        temporadaAtiva = -1;
        mostrarTemporada(inicial);
        List<View> cartoes = cartoes();
        if (focado >= 0 && focado < cartoes.size()) cartoes.get(focado).requestFocus();
    }

    /** Cartões da grade, na ordem (as linhas da {@link Grade} têm os cartões e espaços vazios). */
    private List<View> cartoes() {
        List<View> lista = new ArrayList<>();
        for (int i = 0; i < episodios.getChildCount(); i++) {
            View linha = episodios.getChildAt(i);
            if (!(linha instanceof LinearLayout)) continue;
            LinearLayout grupo = (LinearLayout) linha;
            for (int j = 0; j < grupo.getChildCount(); j++) {
                View filho = grupo.getChildAt(j);
                if (filho.isFocusable()) lista.add(filho);
            }
        }
        return lista;
    }

    private int indiceDoCartaoEmFoco() {
        View foco = getCurrentFocus();
        return foco == null ? -1 : cartoes().indexOf(foco);
    }

    private int indiceDaTemporada(Episodio e) {
        if (e == null) return 0;
        for (int i = 0; i < temporadas.size(); i++) {
            if (temporadas.get(i).numero == e.temporada) return i;
        }
        return 0;
    }

    /** Pílulas "Temporada N" (56 px, Inter 800); a em foco mostra a temporada. */
    private void criarPilulas() {
        if (pilulasTemporada.getChildCount() == temporadas.size()) return;
        pilulasTemporada.removeAllViews();
        Resources res = getResources();
        int altura = res.getDimensionPixelSize(R.dimen.st_tv_temporada_altura);
        int padding = res.getDimensionPixelSize(R.dimen.st_tv_temporada_padding);
        int espaco = res.getDimensionPixelSize(R.dimen.st_tv_temporada_espaco);
        for (int i = 0; i < temporadas.size(); i++) {
            final int indice = i;
            TextView pilula = new TextView(this);
            pilula.setText(getString(R.string.st_temporada_n, temporadas.get(i).numero));
            pilula.setTypeface(ResourcesCompat.getFont(this, R.font.st_inter_extrabold));
            pilula.setIncludeFontPadding(false);
            pilula.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(R.dimen.st_tv_texto_temporada));
            pilula.setTextColor(ContextCompat.getColorStateList(this, R.color.st_cor_pilula));
            pilula.setBackgroundResource(R.drawable.st_pilula_temporada);
            pilula.setGravity(Gravity.CENTER);
            pilula.setPadding(padding, 0, padding, 0);
            pilula.setFocusable(true);
            pilula.setOnFocusChangeListener((v, temFoco) -> {
                if (temFoco) mostrarTemporada(indice);
            });
            pilula.setOnClickListener(v -> mostrarTemporada(indice));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, altura);
            if (i > 0) lp.setMarginStart(espaco);
            pilulasTemporada.addView(pilula, lp);
        }
    }

    private void mostrarTemporada(int indice) {
        if (temporadas.isEmpty() || indice == temporadaAtiva) return;
        temporadaAtiva = indice;
        for (int i = 0; i < pilulasTemporada.getChildCount(); i++) {
            pilulasTemporada.getChildAt(i).setSelected(i == indice);
        }
        List<View> cartoes = new ArrayList<>();
        for (Episodio e : temporadas.get(indice).episodios) {
            CartaoEpisodio cartao = CartaoEpisodio.criar(episodios, true,
                    escolhido -> Navegacao.abrirReproducao(this, escolhido.id, Navegacao.DE_ONDE_PAROU));
            cartao.vincular(e);
            cartoes.add(cartao.itemView);
        }
        Resources res = getResources();
        Grade.preencher(episodios, cartoes, 2,
                res.getDimensionPixelSize(R.dimen.st_tv_episodios_espaco_h),
                res.getDimensionPixelSize(R.dimen.st_tv_episodios_espaco_v));
    }
}
