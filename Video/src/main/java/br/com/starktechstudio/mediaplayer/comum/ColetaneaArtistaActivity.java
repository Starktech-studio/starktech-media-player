// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.content.res.Resources;
import android.os.Bundle;
import android.text.Layout;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.archos.mediacenter.video.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import br.com.starktechstudio.mediaplayer.dados.Modelo.Titulo;
import br.com.starktechstudio.mediaplayer.dados.RepositorioBiblioteca;

/**
 * Coletânea (D5) e artista (D7), no padrão do Detalhe, com o layout de cada aparelho: a capa e, ao
 * lado, o nome, os chips, o texto do artista (Wikipédia) e Editar; embaixo, com título próprio, as
 * capas dos filmes ou dos shows, que abrem o Detalhe como as de qualquer lista. Editar troca a capa, o
 * fundo e o nome, e no artista busca outro para juntar os shows. O que muda na TV (pílulas do topo e o
 * foco ligado nelas) fica em {@code tv.ColetaneaArtistaTvActivity}.
 */
public class ColetaneaArtistaActivity extends AppCompatActivity {

    /** Capas por linha na TV, como nas abas. */
    private static final int COLUNAS_TV = 6;

    private RepositorioBiblioteca repositorio;
    private String id;
    private boolean tv;
    private boolean primeiraVez = true;
    private int larguraMontada;

    /** Só na TV: o tablet não desenha o fundo atrás, como na Série. */
    @Nullable
    private View fundo;
    private View capa;
    private TextView rotulo;
    private TextView nome;
    private LinearLayout chips;
    private LinearLayout capas;
    private TextView sinopse;
    private TextView fonte;
    private TextView secao;
    private Button verMais;
    private String sinopseMostrada;
    private String capaMostrada;
    private final List<View> itens = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tv = Aparelho.ehTv(this);
        repositorio = RepositorioBiblioteca.atual(this);
        id = getIntent().getStringExtra(Navegacao.EXTRA_ID);
        Titulo titulo = repositorio.titulo(id);
        if (titulo == null) {
            finish();
            return;
        }
        setContentView(tv ? R.layout.st_tv_coletanea_artista : R.layout.st_tablet_coletanea_artista);

        fundo = findViewById(R.id.st_coletanea_fundo);
        capa = findViewById(R.id.st_coletanea_capa);
        rotulo = findViewById(R.id.st_coletanea_rotulo);
        nome = findViewById(R.id.st_coletanea_titulo);
        chips = findViewById(R.id.st_coletanea_chips);
        capas = findViewById(R.id.st_coletanea_capas);
        sinopse = findViewById(R.id.st_coletanea_sinopse);
        fonte = findViewById(R.id.st_coletanea_fonte);
        secao = findViewById(R.id.st_coletanea_secao);
        verMais = findViewById(R.id.st_coletanea_ver_mais);
        Button editar = findViewById(R.id.st_coletanea_editar);
        editar.setOnClickListener(v -> Navegacao.abrirEditar(this, id));
        Icones.noTexto(editar, R.drawable.st_ic_editar,
                getResources().getDimensionPixelSize(tv ? R.dimen.st_tv_botao_icone : R.dimen.st_tablet_detalhe_topo_botao_icone));
        verMais.setOnClickListener(v -> {
            Titulo atual = repositorio.titulo(id);
            if (atual != null) Navegacao.abrirSinopse(this, atual.titulo, atual.sinopse);
        });

        if (!tv) {
            BordasSistema.aplicar(findViewById(R.id.st_coletanea_corpo), true, true, true, true);
            Button voltar = findViewById(R.id.st_coletanea_voltar);
            Icones.noTexto(voltar, R.drawable.st_ic_voltar, getResources().getDimensionPixelSize(R.dimen.st_tablet_detalhe_topo_botao_icone));
            voltar.setOnClickListener(v -> finish());
        }
        ligarTopo(titulo);
        // Tablet: as colunas dependem da largura de verdade (barras do sistema, tela dividida)
        capas.addOnLayoutChangeListener((v, esq, topo, dir, base, esqAntes, topoAntes, dirAntes, baseAntes) -> {
            if (dir - esq > 0 && dir - esq != larguraMontada) v.post(this::recarregarCapas);
        });
    }

    /** Topo da TV (pílulas); o tablet só tem Voltar e Editar. */
    protected void ligarTopo(Titulo titulo) {}

    /** Id do alvo do ↑ da primeira linha de capas na TV. */
    protected int idAcimaDasCapas() {
        return View.NO_ID;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Volta do player, do Detalhe ou de Editar: assistido, capa e a própria lista podem ter mudado
        carregar();
        if (primeiraVez) {
            primeiraVez = false;
            if (tv && !itens.isEmpty()) itens.get(0).requestFocus();
        }
        repositorio.ouvir(aoMudarBiblioteca);
        repositorio.lerDadosDoArtista(id);
    }

    @Override
    protected void onPause() {
        repositorio.deixarDeOuvir(aoMudarBiblioteca);
        super.onPause();
    }

    /** O texto ou a capa chegaram com a tela aberta: só o bloco de cima muda, sem mexer no foco das capas. */
    private final Runnable aoMudarBiblioteca = () -> {
        Titulo novo = repositorio.titulo(id);
        if (novo != null && (!Objects.equals(novo.sinopse, sinopseMostrada) || !Objects.equals(novo.imagemCapa(), capaMostrada))) {
            mostrarTopoDaPagina(novo);
        }
    };

    private void carregar() {
        Titulo titulo = repositorio.titulo(id);
        if (titulo == null) {
            // A coletânea ficou com menos de 2 filmes, ou o artista sem shows (juntado a outro)
            finish();
            return;
        }
        mostrarTopoDaPagina(titulo);
        recarregarCapas();
    }

    private void mostrarTopoDaPagina(Titulo titulo) {
        sinopseMostrada = titulo.sinopse;
        capaMostrada = titulo.imagemCapa();
        if (fundo != null) {
            fundo.setBackground(PecasDetalhe.fundoDoTitulo(this, titulo.corFundo));
            // Artista quase nunca tem fundo próprio: a foto dele, com o véu, como na página de filmes
            String imagemDoFundo = titulo.imagemFundo();
            Imagens.fundoDaPagina(fundo, imagemDoFundo != null ? imagemDoFundo : titulo.imagemCapa());
        }
        float canto = getResources().getDimension(tv ? R.dimen.st_tv_serie_capa_canto : R.dimen.st_tablet_detalhe_capa_canto);
        capa.setBackground(CapaAdapter.blocoDeCor(this, titulo.corCapa, canto, tv));
        Imagens.noFundo(capa, titulo.imagemCapa(), Imagens.CAPA, canto);
        rotulo.setText(titulo.ehColetanea() ? R.string.st_rotulo_coletanea : R.string.st_rotulo_artista);
        nome.setText(titulo.titulo);
        String contagem = titulo.ehColetanea()
                ? getResources().getQuantityString(R.plurals.st_n_filmes, titulo.numeroFilmes, titulo.numeroFilmes)
                : getResources().getQuantityString(R.plurals.st_n_shows, titulo.numeroShows, titulo.numeroShows);
        List<String> textos = new ArrayList<>();
        List<Boolean> azuis = new ArrayList<>();
        textos.add(contagem);
        azuis.add(false);
        if (titulo.ehArtista() && titulo.genero != null && !titulo.genero.isEmpty()) {
            textos.add(titulo.genero);
            azuis.add(false);
        }
        PecasDetalhe.chips(chips, textos, azuis, tv);

        boolean temTexto = titulo.sinopse != null && !titulo.sinopse.isEmpty();
        sinopse.setText(titulo.sinopse);
        sinopse.setVisibility(temTexto ? View.VISIBLE : View.GONE);
        fonte.setVisibility(temTexto && titulo.ehArtista() ? View.VISIBLE : View.GONE);
        // As linhas só existem depois de medir
        sinopse.post(this::mostrarVerMais);
        secao.setText(titulo.ehArtista() ? getString(R.string.st_shows_de, titulo.titulo) : getString(R.string.st_filmes_da_coletanea));
    }

    /** "Ver mais" só quando o texto não coube. */
    private void mostrarVerMais() {
        Layout linhas = sinopse.getVisibility() == View.VISIBLE ? sinopse.getLayout() : null;
        int ultima = linhas == null ? -1 : linhas.getLineCount() - 1;
        boolean cortou = ultima >= 0 && (linhas.getEllipsisCount(ultima) > 0 || linhas.getLineEnd(ultima) < sinopse.getText().length());
        verMais.setVisibility(cortou ? View.VISIBLE : View.GONE);
    }

    /** Refaz as linhas; a capa em foco (volta do Detalhe) recebe o foco de novo. */
    private void recarregarCapas() {
        if (isFinishing()) return;
        View foco = getCurrentFocus();
        int focado = foco == null ? -1 : itens.indexOf(foco);

        Resources res = getResources();
        int margem = res.getDimensionPixelSize(tv ? R.dimen.st_tv_margem_lateral : R.dimen.st_tablet_margem_h);
        int largura = capas.getWidth() > 0 ? capas.getWidth() : res.getDisplayMetrics().widthPixels - 2 * margem;
        larguraMontada = largura;
        int colunas;
        int larguraCapa;
        int alturaCapa;
        int espaco;
        int espacoVertical;
        if (tv) {
            colunas = COLUNAS_TV;
            espaco = res.getDimensionPixelSize(R.dimen.st_tv_capa_espaco);
            espacoVertical = res.getDimensionPixelSize(R.dimen.st_tv_lista_espaco_vertical);
            larguraCapa = (largura - espaco * (colunas - 1)) / colunas;
            alturaCapa = larguraCapa * 3 / 2;
        } else {
            larguraCapa = res.getDimensionPixelSize(R.dimen.st_tablet_capa_largura);
            alturaCapa = res.getDimensionPixelSize(R.dimen.st_tablet_capa_altura);
            espacoVertical = res.getDimensionPixelSize(R.dimen.st_tablet_capa_espaco);
            colunas = LinhasDeCapas.colunas(largura, larguraCapa, espacoVertical);
            // Como na grade da aba: cada capa no começo da sua coluna
            espaco = largura / colunas - larguraCapa;
        }

        capas.removeAllViews();
        itens.clear();
        List<List<Titulo>> linhas = LinhasDeCapas.dividir(titulos(), colunas);
        for (int l = 0; l < linhas.size(); l++) {
            LinearLayout fileira = new LinearLayout(this);
            fileira.setOrientation(LinearLayout.HORIZONTAL);
            fileira.setClipChildren(false);
            fileira.setClipToPadding(false);
            List<Titulo> linha = linhas.get(l);
            for (int i = 0; i < linha.size(); i++) {
                CapaAdapter.Capa item = CapaAdapter.Capa.criar(this, fileira, tv);
                item.vincular(linha.get(i));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(larguraCapa, alturaCapa);
                if (i > 0) lp.setMarginStart(espaco);
                fileira.addView(item.itemView, lp);
                if (tv && l == 0) item.itemView.setNextFocusUpId(idAcimaDasCapas());
                itens.add(item.itemView);
            }
            LinearLayout.LayoutParams lpFileira = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            if (l > 0) lpFileira.topMargin = espacoVertical;
            capas.addView(fileira, lpFileira);
        }
        if (!itens.isEmpty()) itens.get(0).setId(R.id.st_coletanea_primeira);
        if (focado >= 0 && focado < itens.size()) itens.get(focado).requestFocus();
    }

    /** Os filmes da coletânea (por ano) ou os shows do artista (em ordem alfabética). */
    private List<Titulo> titulos() {
        Titulo titulo = repositorio.titulo(id);
        if (titulo != null && titulo.ehArtista()) return repositorio.showsDoArtista(id);
        return repositorio.filmesDaColetanea(id);
    }
}
