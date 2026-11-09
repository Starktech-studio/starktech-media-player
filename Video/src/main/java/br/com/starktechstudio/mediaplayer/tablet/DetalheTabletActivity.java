// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.tablet;

import android.content.res.Resources;
import android.os.Bundle;
import android.text.Layout;
import android.util.TypedValue;
import android.view.LayoutInflater;
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
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import br.com.starktechstudio.mediaplayer.comum.BordasSistema;
import br.com.starktechstudio.mediaplayer.comum.CapaAdapter;
import br.com.starktechstudio.mediaplayer.comum.CapituloAdapter;
import br.com.starktechstudio.mediaplayer.comum.EstrelasNota;
import br.com.starktechstudio.mediaplayer.comum.Grade;
import br.com.starktechstudio.mediaplayer.comum.Icones;
import br.com.starktechstudio.mediaplayer.comum.Imagens;
import br.com.starktechstudio.mediaplayer.comum.Navegacao;
import br.com.starktechstudio.mediaplayer.comum.PecasDetalhe;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Capitulo;
import br.com.starktechstudio.mediaplayer.dados.Modelo.DadosTecnicos;
import br.com.starktechstudio.mediaplayer.dados.Modelo.PessoaElenco;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Titulo;
import br.com.starktechstudio.mediaplayer.dados.RepositorioBiblioteca;

/**
 * Detalhe do filme ou do show no tablet (tablet-detalhe.dc.html): Biblioteca (voltar), as estrelas
 * da nota (só em filme assistido, {@link EstrelasNota}) e Editar no topo; capa, tipo, título, chips, sinopse (até 3 linhas; o resto em "Ver mais", numa janela,
 * para os botões não descerem) e os botões Assistir (do início), Continuar
 * (de onde parou, só com o vídeo em andamento) e Áudio e legendas; abas Capítulos (o capítulo
 * onde o vídeo parou em azul), Dados técnicos e Elenco (D12). Favoritar e Marcar como assistido
 * ficam no menu da capa (tocar e segurar a capa, aqui ou nas listas). Arquivo sem capítulos: sem
 * a aba Capítulos. A página inteira rola: puxar leva os capítulos para cima, e dá para ver todos.
 */
public class DetalheTabletActivity extends AppCompatActivity {

    private enum Aba { CAPITULOS, DADOS, ELENCO }

    private RepositorioBiblioteca repositorio;
    private String id;
    private Titulo titulo;
    private List<Capitulo> capitulos = Collections.emptyList();
    private Aba abaAtiva;

    private View capa;
    private TextView tipo;
    private TextView nome;
    private EstrelasNota estrelas;
    private LinearLayout chips;
    private TextView sinopse;
    private TextView verMais;
    private Button continuar;
    private TextView abaCapitulos;
    private TextView abaDados;
    private TextView abaElenco;
    private LinearLayout conteudoAba;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repositorio = RepositorioBiblioteca.atual(this);
        id = getIntent().getStringExtra(Navegacao.EXTRA_ID);
        if (repositorio.titulo(id) == null) {
            finish();
            return;
        }
        setContentView(R.layout.st_tablet_detalhe);
        BordasSistema.aplicar(findViewById(R.id.st_detalhe_corpo), true, true, true, true);

        capa = findViewById(R.id.st_detalhe_capa);
        tipo = findViewById(R.id.st_detalhe_tipo);
        nome = findViewById(R.id.st_detalhe_titulo);
        estrelas = findViewById(R.id.st_detalhe_nota);
        chips = findViewById(R.id.st_detalhe_chips);
        sinopse = findViewById(R.id.st_detalhe_sinopse);
        verMais = findViewById(R.id.st_detalhe_ver_mais);
        continuar = findViewById(R.id.st_detalhe_continuar);
        abaCapitulos = findViewById(R.id.st_aba_capitulos);
        abaDados = findViewById(R.id.st_aba_dados);
        abaElenco = findViewById(R.id.st_aba_elenco);
        conteudoAba = findViewById(R.id.st_detalhe_aba_conteudo);
        Button voltar = findViewById(R.id.st_detalhe_voltar);
        Button editar = findViewById(R.id.st_detalhe_editar);
        Button assistir = findViewById(R.id.st_detalhe_assistir);
        Button faixas = findViewById(R.id.st_detalhe_faixas);

        Resources res = getResources();
        int iconeTopo = res.getDimensionPixelSize(R.dimen.st_tablet_detalhe_topo_botao_icone);
        Icones.noTexto(voltar, R.drawable.st_ic_voltar, iconeTopo);
        Icones.noTexto(editar, R.drawable.st_ic_editar, iconeTopo);
        Icones.noTexto(assistir, R.drawable.st_ic_assistir, res.getDimensionPixelSize(R.dimen.st_tablet_detalhe_botao_icone_primario));
        int icone = res.getDimensionPixelSize(R.dimen.st_tablet_detalhe_botao_icone);
        Icones.noTexto(continuar, R.drawable.st_ic_do_inicio, icone);
        Icones.noTexto(faixas, R.drawable.st_ic_legendas, icone);

        voltar.setOnClickListener(v -> finish());
        editar.setOnClickListener(v -> Navegacao.abrirEditar(this, id));
        // Tocar e segurar a capa: Menu da capa (Favoritar e Marcar como assistido ficam nele, D12)
        capa.setOnLongClickListener(v -> {
            Navegacao.abrirMenuCapa(this, id);
            return true;
        });
        assistir.setOnClickListener(v -> Navegacao.abrirReproducao(this, id, Navegacao.DO_INICIO));
        continuar.setOnClickListener(v -> Navegacao.abrirReproducao(this, id, Navegacao.DE_ONDE_PAROU));
        faixas.setOnClickListener(v -> Navegacao.abrirFaixas(this, id));
        verMais.setOnClickListener(v -> Navegacao.abrirSinopse(this, titulo.titulo, titulo.sinopse));
        abaCapitulos.setOnClickListener(v -> mostrarAba(Aba.CAPITULOS));
        abaDados.setOnClickListener(v -> mostrarAba(Aba.DADOS));
        abaElenco.setOnClickListener(v -> mostrarAba(Aba.ELENCO));
        estrelas.preparar(false, this::aoEscolherNota);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Volta de outra tela (Editar, player): o estado pode ter mudado
        carregar();
        repositorio.lerDadosDoArquivo(id);
        repositorio.ouvir(aoMudarBiblioteca);
    }

    @Override
    protected void onPause() {
        repositorio.deixarDeOuvir(aoMudarBiblioteca);
        super.onPause();
    }

    /**
     * Vídeo que a varredura ainda não leu (ou cuja leitura falhou): o repositório lê na hora; quando
     * os capítulos ou a duração chegam, a tela relê; também com a posição ou o assistido novos (o
     * player grava ao sair, com esta tela já de volta). Nos outros avisos da varredura, nada, para não
     * mexer no foco.
     */
    private final Runnable aoMudarBiblioteca = () -> {
        Titulo novo = repositorio.titulo(id);
        if (novo == null || titulo == null) return;
        if (novo.duracaoSegundos != titulo.duracaoSegundos || repositorio.capitulos(id).size() != capitulos.size()
                || novo.posicaoSegundos != titulo.posicaoSegundos || novo.assistido != titulo.assistido
                || !Objects.equals(novo.titulo, titulo.titulo) || !Objects.equals(novo.sinopse, titulo.sinopse)
                || !Objects.equals(novo.imagemCapa(), titulo.imagemCapa())) {
            carregar();
        } else if (!Objects.equals(novo.nota, titulo.nota)) {
            titulo = novo;
            mostrarNota();
        }
    };

    private void carregar() {
        titulo = repositorio.titulo(id);
        if (titulo == null) {
            finish();
            return;
        }
        capitulos = repositorio.capitulos(id);
        DadosTecnicos tecnicos = repositorio.dadosTecnicos(id);

        float canto = getResources().getDimension(R.dimen.st_tablet_detalhe_capa_canto);
        capa.setBackground(CapaAdapter.blocoDeCor(this, titulo.corCapa, canto, false));
        Imagens.noFundo(capa, titulo.imagemCapa(), Imagens.CAPA, canto);
        tipo.setText(PecasDetalhe.rotuloTipo(titulo.tipo));
        nome.setText(titulo.titulo);
        mostrarNota();
        PecasDetalhe.chips(chips, titulo, tecnicos == null ? Collections.<String>emptyList() : tecnicos.chipsCurtos,
                tecnicos == null ? 0 : tecnicos.chipsVideo, false);
        sinopse.setText(titulo.sinopse);
        sinopse.setVisibility(titulo.sinopse == null || titulo.sinopse.isEmpty() ? View.GONE : View.VISIBLE);
        // As linhas só existem depois de medir
        sinopse.post(this::mostrarVerMais);

        boolean andamento = titulo.emAndamento();
        continuar.setVisibility(andamento ? View.VISIBLE : View.GONE);
        if (andamento) continuar.setText(getString(R.string.st_continuar_em, PecasDetalhe.tempo(titulo.posicaoSegundos)));

        abaCapitulos.setVisibility(capitulos.isEmpty() ? View.GONE : View.VISIBLE);
        Aba aba = abaAtiva;
        if (aba == null || (aba == Aba.CAPITULOS && capitulos.isEmpty())) {
            aba = capitulos.isEmpty() ? Aba.DADOS : Aba.CAPITULOS;
        }
        mostrarAba(aba);
    }

    /** Estrelas só em filme assistido (D6). */
    private void mostrarNota() {
        boolean pode = titulo.podeDarNota();
        estrelas.setVisibility(pode ? View.VISIBLE : View.GONE);
        if (pode) estrelas.mostrar(titulo.nota);
    }

    /** Grava e mostra só a nota; se não gravou, volta ao que está guardado. */
    private void aoEscolherNota(Integer nota) {
        repositorio.definirNota(id, nota);
        Titulo novo = repositorio.titulo(id);
        if (novo == null) return;
        titulo = novo;
        mostrarNota();
    }

    /** "Ver mais" só quando o texto não coube nas 3 linhas. */
    private void mostrarVerMais() {
        Layout linhas = sinopse.getVisibility() == View.VISIBLE ? sinopse.getLayout() : null;
        int ultima = linhas == null ? -1 : linhas.getLineCount() - 1;
        boolean cortou = ultima >= 0 && (linhas.getEllipsisCount(ultima) > 0 || linhas.getLineEnd(ultima) < sinopse.getText().length());
        verMais.setVisibility(cortou ? View.VISIBLE : View.GONE);
    }

    private void mostrarAba(Aba aba) {
        abaAtiva = aba;
        abaCapitulos.setSelected(aba == Aba.CAPITULOS);
        abaDados.setSelected(aba == Aba.DADOS);
        abaElenco.setSelected(aba == Aba.ELENCO);
        Resources res = getResources();
        int espacoH = res.getDimensionPixelSize(R.dimen.st_tablet_lista_espaco_h);
        int espacoV = res.getDimensionPixelSize(R.dimen.st_tablet_lista_espaco_v);
        switch (aba) {
            case CAPITULOS: {
                int atual = PecasDetalhe.capituloAtual(capitulos, titulo);
                List<View> itens = new ArrayList<>();
                for (Capitulo c : capitulos) {
                    CapituloAdapter.Item item = CapituloAdapter.Item.criar(conteudoAba, false,
                            escolhido -> Navegacao.abrirReproducao(this, id, escolhido.numero));
                    item.vincular(c, atual);
                    itens.add(item.itemView);
                }
                Grade.preencher(conteudoAba, itens, 2, espacoH, espacoV);
                break;
            }
            case DADOS: {
                DadosTecnicos tecnicos = repositorio.dadosTecnicos(id);
                if (tecnicos != null) PecasDetalhe.dadosTecnicos(conteudoAba, tecnicos, false, () -> Navegacao.abrirFaixas(this, id));
                else conteudoAba.removeAllViews();
                break;
            }
            case ELENCO:
            default: {
                List<PessoaElenco> elenco = repositorio.elenco(id);
                if (elenco.isEmpty()) {
                    conteudoAba.removeAllViews();
                    conteudoAba.addView(textoVazio(R.string.st_sem_elenco));
                    break;
                }
                LayoutInflater inflater = LayoutInflater.from(this);
                List<View> itens = new ArrayList<>();
                for (PessoaElenco pessoa : elenco) {
                    View item = inflater.inflate(R.layout.st_tablet_item_elenco, conteudoAba, false);
                    ((TextView) item.findViewById(R.id.st_elenco_nome)).setText(pessoa.nome);
                    ((TextView) item.findViewById(R.id.st_elenco_personagem)).setText(pessoa.personagem);
                    itens.add(item);
                }
                Grade.preencher(conteudoAba, itens, 2, espacoH, espacoV);
                break;
            }
        }
    }

    private TextView textoVazio(int texto) {
        TextView vazio = new TextView(this);
        vazio.setText(texto);
        vazio.setTypeface(ResourcesCompat.getFont(this, R.font.st_inter_semibold));
        vazio.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.st_tablet_texto_dados_valor));
        vazio.setTextColor(ContextCompat.getColor(this, R.color.st_texto_apagado));
        return vazio;
    }
}
