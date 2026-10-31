// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.tablet;

import android.content.res.Resources;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.archos.mediacenter.video.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import br.com.starktechstudio.mediaplayer.comum.BordasSistema;
import br.com.starktechstudio.mediaplayer.comum.CapaAdapter;
import br.com.starktechstudio.mediaplayer.comum.CartaoEpisodio;
import br.com.starktechstudio.mediaplayer.comum.Grade;
import br.com.starktechstudio.mediaplayer.comum.Icones;
import br.com.starktechstudio.mediaplayer.comum.Navegacao;
import br.com.starktechstudio.mediaplayer.comum.PecasDetalhe;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Episodio;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Temporada;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Titulo;
import br.com.starktechstudio.mediaplayer.dados.RepositorioBiblioteca;

/**
 * Série no tablet (sem desenho; D12): o Detalhe do tablet com o conteúdo da Série da TV —
 * Biblioteca (voltar) e Editar, capa, título, chips (temporadas, episódios, qualidade), aviso da
 * pasta e o botão do próximo episódio; as temporadas em abas e os episódios em 2 colunas, com
 * Assistido, Em andamento ou novo. Favoritar fica no menu da capa (toque longo).
 */
public class SerieTabletActivity extends AppCompatActivity {

    private RepositorioBiblioteca repositorio;
    private String id;
    private Titulo serie;
    private List<Temporada> temporadas = new ArrayList<>();
    private int temporadaAtiva = -1;

    private View capa;
    private TextView nome;
    private LinearLayout chips;
    private TextView aviso;
    private Button principal;
    private LinearLayout abasTemporada;
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
        setContentView(R.layout.st_tablet_serie);
        BordasSistema.aplicar(findViewById(R.id.st_serie_corpo), true, true, true, true);

        capa = findViewById(R.id.st_serie_capa);
        nome = findViewById(R.id.st_serie_titulo);
        chips = findViewById(R.id.st_serie_chips);
        aviso = findViewById(R.id.st_serie_aviso);
        principal = findViewById(R.id.st_serie_principal);
        abasTemporada = findViewById(R.id.st_serie_temporadas);
        episodios = findViewById(R.id.st_serie_episodios);
        Button voltar = findViewById(R.id.st_serie_voltar);
        Button editar = findViewById(R.id.st_serie_editar);

        int iconeTopo = getResources().getDimensionPixelSize(R.dimen.st_tablet_detalhe_topo_botao_icone);
        Icones.noTexto(voltar, R.drawable.st_ic_voltar, iconeTopo);
        Icones.noTexto(editar, R.drawable.st_ic_editar, iconeTopo);
        voltar.setOnClickListener(v -> finish());
        editar.setOnClickListener(v -> Navegacao.abrirEditar(this, id));
        // Tocar e segurar a capa: Menu da capa (Favoritar fica nele, D12)
        capa.setOnLongClickListener(v -> {
            Navegacao.abrirMenuCapa(this, id);
            return true;
        });
        principal.setOnClickListener(v -> {
            if (proximo != null) Navegacao.abrirReproducao(this, proximo.id, Navegacao.DE_ONDE_PAROU);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Volta do player ou de Editar: o estado simulado (assistido, posição) pode ter mudado
        carregar();
    }

    private void carregar() {
        serie = repositorio.titulo(id);
        if (serie == null) {
            finish();
            return;
        }
        temporadas = repositorio.temporadas(id);
        proximo = PecasDetalhe.proximoEpisodio(temporadas);

        capa.setBackground(CapaAdapter.blocoDeCor(this, serie.corCapa,
                getResources().getDimension(R.dimen.st_tablet_detalhe_capa_canto), false));
        nome.setText(serie.titulo);
        List<String> textos = new ArrayList<>(Arrays.asList(
                getResources().getQuantityString(R.plurals.st_n_temporadas, serie.numeroTemporadas, serie.numeroTemporadas),
                getResources().getQuantityString(R.plurals.st_n_episodios, serie.numeroEpisodios, serie.numeroEpisodios)));
        if (serie.qualidade != null && !serie.qualidade.isEmpty()) textos.add(serie.qualidade);
        List<Boolean> azuis = new ArrayList<>();
        for (int i = 0; i < textos.size(); i++) azuis.add(false);
        PecasDetalhe.chips(chips, textos, azuis, false);
        PecasDetalhe.avisoPasta(aviso, serie, false);

        principal.setVisibility(proximo == null ? View.GONE : View.VISIBLE);
        if (proximo != null) {
            principal.setText(PecasDetalhe.textoProximo(this, proximo));
            Icones.noTexto(principal, R.drawable.st_ic_assistir,
                    getResources().getDimensionPixelSize(R.dimen.st_tablet_detalhe_botao_icone_primario));
        }

        criarAbas();
        int inicial = temporadaAtiva;
        if (inicial < 0 || inicial >= temporadas.size()) inicial = indiceDaTemporada(proximo);
        mostrarTemporada(inicial);
    }

    private int indiceDaTemporada(Episodio e) {
        if (e == null) return 0;
        for (int i = 0; i < temporadas.size(); i++) {
            if (temporadas.get(i).numero == e.temporada) return i;
        }
        return 0;
    }

    /** Abas "Temporada N" no padrão das abas do Detalhe (ativa com traço azul). */
    private void criarAbas() {
        if (abasTemporada.getChildCount() == temporadas.size()) return;
        abasTemporada.removeAllViews();
        Resources res = getResources();
        int altura = res.getDimensionPixelSize(R.dimen.st_tablet_aba_altura);
        int espaco = res.getDimensionPixelSize(R.dimen.st_tablet_aba_espaco);
        int padding = Math.round(4 * res.getDisplayMetrics().density);
        for (int i = 0; i < temporadas.size(); i++) {
            final int indice = i;
            TextView aba = new TextView(this);
            aba.setText(getString(R.string.st_temporada_n, temporadas.get(i).numero));
            aba.setTypeface(ResourcesCompat.getFont(this, R.font.st_inter_extrabold));
            aba.setIncludeFontPadding(false);
            aba.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(R.dimen.st_tablet_texto_aba));
            aba.setTextColor(ContextCompat.getColorStateList(this, R.color.st_cor_aba_tablet));
            aba.setBackgroundResource(R.drawable.st_aba_tablet);
            aba.setGravity(Gravity.CENTER);
            aba.setPadding(padding, 0, padding, 0);
            aba.setOnClickListener(v -> mostrarTemporada(indice));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, altura);
            lp.setMarginEnd(espaco);
            abasTemporada.addView(aba, lp);
        }
    }

    private void mostrarTemporada(int indice) {
        if (temporadas.isEmpty()) return;
        temporadaAtiva = indice;
        for (int i = 0; i < abasTemporada.getChildCount(); i++) {
            abasTemporada.getChildAt(i).setSelected(i == indice);
        }
        List<View> cartoes = new ArrayList<>();
        for (Episodio e : temporadas.get(indice).episodios) {
            CartaoEpisodio cartao = CartaoEpisodio.criar(episodios, false,
                    escolhido -> Navegacao.abrirReproducao(this, escolhido.id, Navegacao.DE_ONDE_PAROU));
            cartao.vincular(e);
            cartoes.add(cartao.itemView);
        }
        Resources res = getResources();
        Grade.preencher(episodios, cartoes, 2,
                res.getDimensionPixelSize(R.dimen.st_tablet_lista_espaco_h),
                res.getDimensionPixelSize(R.dimen.st_tablet_lista_espaco_v));
    }
}
