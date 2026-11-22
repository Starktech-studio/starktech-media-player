// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.tablet;

import android.content.res.Resources;
import android.os.Bundle;
import android.text.Layout;
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
import br.com.starktechstudio.mediaplayer.comum.Imagens;
import br.com.starktechstudio.mediaplayer.comum.Navegacao;
import br.com.starktechstudio.mediaplayer.comum.PecasDetalhe;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Episodio;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Temporada;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Tipo;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Titulo;
import br.com.starktechstudio.mediaplayer.dados.RepositorioBiblioteca;

/**
 * Série no tablet (sem desenho; D12): o Detalhe do tablet com o conteúdo da Série da TV —
 * Biblioteca (voltar), Favoritar e Editar (só o ícone, no canto), capa, rótulo ("Série" ou "Desenho"),
 * título, chips (temporadas, episódios, qualidade), a descrição da série (até 3 linhas e "Ver mais",
 * como no Detalhe; sem descrição, nada no lugar) e o botão do próximo episódio; as temporadas em abas
 * e os episódios em 2 colunas, com Assistido, Em andamento ou novo.
 */
public class SerieTabletActivity extends AppCompatActivity {

    private RepositorioBiblioteca repositorio;
    private String id;
    private Titulo serie;
    private List<Temporada> temporadas = new ArrayList<>();
    private int temporadaAtiva = -1;

    private View capa;
    private TextView rotulo;
    private TextView nome;
    private Button favoritar;
    private LinearLayout chips;
    private TextView sinopse;
    private TextView verMais;
    private Button principal;
    private LinearLayout abasTemporada;
    private LinearLayout episodios;
    /** Cartões da temporada mostrada: os quadros que chegam com a tela aberta entram neles (2.0.1). */
    private final List<CartaoEpisodio> mostrados = new ArrayList<>();
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
        rotulo = findViewById(R.id.st_serie_rotulo);
        nome = findViewById(R.id.st_serie_titulo);
        favoritar = findViewById(R.id.st_serie_favoritar);
        chips = findViewById(R.id.st_serie_chips);
        sinopse = findViewById(R.id.st_serie_sinopse);
        verMais = findViewById(R.id.st_serie_ver_mais);
        principal = findViewById(R.id.st_serie_principal);
        abasTemporada = findViewById(R.id.st_serie_temporadas);
        episodios = findViewById(R.id.st_serie_episodios);
        Button voltar = findViewById(R.id.st_serie_voltar);
        View editar = findViewById(R.id.st_serie_editar);

        int iconeTopo = getResources().getDimensionPixelSize(R.dimen.st_tablet_detalhe_topo_botao_icone);
        Icones.noTexto(voltar, R.drawable.st_ic_voltar, iconeTopo);
        voltar.setOnClickListener(v -> finish());
        editar.setOnClickListener(v -> Navegacao.abrirEditar(this, id));
        verMais.setOnClickListener(v -> Navegacao.abrirSinopse(this, serie.titulo, serie.sinopse));
        favoritar.setOnClickListener(v -> {
            repositorio.definirFavorito(id, !serie.favorito);
            Titulo nova = repositorio.titulo(id);
            if (nova == null) return;
            serie = nova;
            mostrarFavorito();
        });
        // Tocar e segurar a capa: Menu da capa (D12)
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
        // Volta do player ou de Editar: o estado (assistido, posição) pode ter mudado
        carregar();
        repositorio.ouvir(aoMudarBiblioteca);
        // Episódio sem imagem: o quadro do vídeo vem agora, fora da tela (2.0.1)
        if (!isFinishing()) repositorio.lerQuadrosDaSerie(id);
    }

    @Override
    protected void onPause() {
        repositorio.deixarDeOuvir(aoMudarBiblioteca);
        super.onPause();
    }

    /** Quadro novo com a tela aberta: só a imagem dos cartões muda, sem refazer a grade. */
    private final Runnable aoMudarBiblioteca = () -> CartaoEpisodio.atualizarImagens(mostrados, repositorio);

    private void carregar() {
        serie = repositorio.titulo(id);
        if (serie == null) {
            finish();
            return;
        }
        if (serie.ehColetanea()) {
            // Coletânea de desenhos que voltou a "Como filmes" no Editar (ajuste de 22/11/2026): a tela dela no lugar
            Navegacao.abrirCapa(this, serie);
            finish();
            return;
        }
        temporadas = repositorio.temporadas(id);
        proximo = PecasDetalhe.proximoEpisodio(temporadas);

        float canto = getResources().getDimension(R.dimen.st_tablet_detalhe_capa_canto);
        capa.setBackground(CapaAdapter.blocoDeCor(this, serie.corCapa, canto, false));
        Imagens.noFundo(capa, serie.imagemCapa(), Imagens.CAPA, canto);
        rotulo.setText(PecasDetalhe.rotuloTipo(serie.desenho ? Tipo.DESENHO : Tipo.SERIE));
        nome.setText(serie.titulo);
        mostrarFavorito();
        List<String> textos = new ArrayList<>(Arrays.asList(
                getResources().getQuantityString(R.plurals.st_n_temporadas, serie.numeroTemporadas, serie.numeroTemporadas),
                getResources().getQuantityString(R.plurals.st_n_episodios, serie.numeroEpisodios, serie.numeroEpisodios)));
        if (serie.qualidade != null && !serie.qualidade.isEmpty()) textos.add(serie.qualidade);
        List<Boolean> azuis = new ArrayList<>();
        for (int i = 0; i < textos.size(); i++) azuis.add(false);
        PecasDetalhe.chips(chips, textos, azuis, false);
        PecasDetalhe.origem(chips, serie.origem, false);
        sinopse.setText(serie.sinopse);
        sinopse.setVisibility(serie.sinopse == null || serie.sinopse.isEmpty() ? View.GONE : View.VISIBLE);
        verMais.setVisibility(View.GONE);
        sinopse.post(this::mostrarVerMais);

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

    /** "Ver mais" só quando a descrição não coube nas 3 linhas (como no Detalhe). */
    private void mostrarVerMais() {
        Layout linhas = sinopse.getVisibility() == View.VISIBLE ? sinopse.getLayout() : null;
        int ultima = linhas == null ? -1 : linhas.getLineCount() - 1;
        boolean cortou = ultima >= 0 && (linhas.getEllipsisCount(ultima) > 0 || linhas.getLineEnd(ultima) < sinopse.getText().length());
        verMais.setVisibility(cortou ? View.VISIBLE : View.GONE);
    }

    /** Favorita vai para a fileira Favoritos do Início. */
    private void mostrarFavorito() {
        favoritar.setText(serie.favorito ? R.string.st_favorito : R.string.st_favoritar);
        favoritar.setContentDescription(getString(serie.favorito ? R.string.st_remover_favoritos : R.string.st_favoritar));
        Icones.noTexto(favoritar, R.drawable.st_ic_favorito, getResources().getDimensionPixelSize(R.dimen.st_tablet_detalhe_topo_botao_icone),
                ContextCompat.getColor(this, serie.favorito ? R.color.st_favorito : R.color.st_texto));
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
        mostrados.clear();
        for (Episodio e : temporadas.get(indice).episodios) {
            CartaoEpisodio cartao = CartaoEpisodio.criar(episodios, false,
                    escolhido -> Navegacao.abrirReproducao(this, escolhido.id, Navegacao.DE_ONDE_PAROU));
            cartao.vincular(e);
            mostrados.add(cartao);
            cartoes.add(cartao.itemView);
        }
        Resources res = getResources();
        Grade.preencher(episodios, cartoes, 2,
                res.getDimensionPixelSize(R.dimen.st_tablet_lista_espaco_h),
                res.getDimensionPixelSize(R.dimen.st_tablet_lista_espaco_v));
    }
}
