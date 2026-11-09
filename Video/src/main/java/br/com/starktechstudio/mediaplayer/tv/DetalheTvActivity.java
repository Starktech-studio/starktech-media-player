// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.tv;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.leanback.widget.HorizontalGridView;

import com.archos.mediacenter.video.R;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import br.com.starktechstudio.mediaplayer.comum.CapaAdapter;
import br.com.starktechstudio.mediaplayer.comum.CapituloAdapter;
import br.com.starktechstudio.mediaplayer.comum.EstrelasNota;
import br.com.starktechstudio.mediaplayer.comum.Icones;
import br.com.starktechstudio.mediaplayer.comum.Imagens;
import br.com.starktechstudio.mediaplayer.comum.Navegacao;
import br.com.starktechstudio.mediaplayer.comum.PecasDetalhe;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Capitulo;
import br.com.starktechstudio.mediaplayer.dados.Modelo.DadosTecnicos;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Titulo;
import br.com.starktechstudio.mediaplayer.dados.RepositorioBiblioteca;

/**
 * Detalhe do filme ou do show na TV (tv-detalhe.dc.html): fundo do título atrás (bloco de cor
 * na F1, véu 72 % e degradê); no topo, as seções com a do título marcada ({@link TopoSecaoTv}:
 * ↑ do conteúdo vai a ela, OK leva à aba) e Configurações;
 * capa, tipo e origem dos dados, chips, sinopse, progresso com "Marcar como assistido", botões
 * Continuar, Do início, Capítulos, Áudio e legendas, Editar e Favoritar (os dois só com o ícone),
 * dados técnicos e a fileira de
 * capítulos. D22: título sem progresso mostra "Assistir" no lugar de Continuar e Do início; os
 * textos alternam ("Marcar como não assistido", "Remover dos favoritos"); "Capítulos" leva o
 * foco ao capítulo onde o vídeo parou; OK num capítulo abre o player nele. Arquivo sem
 * capítulos: sem o botão e sem a fileira. Filme assistido: as estrelas da nota ao lado de "Marcar
 * como não assistido" ({@link EstrelasNota}; mudar a nota não relê a tela nem tira o foco).
 */
public class DetalheTvActivity extends AppCompatActivity {

    private RepositorioBiblioteca repositorio;
    private TopoSecaoTv topo;
    private String id;
    private Titulo titulo;
    private List<Capitulo> capitulos;
    private boolean primeiraVez = true;

    private View fundo;
    private View capa;
    private TextView tipo;
    private TextView origemDados;
    private TextView nome;
    private LinearLayout chips;
    private TextView sinopse;
    private ProgressBar progresso;
    private TextView parou;
    private Button marcar;
    private EstrelasNota estrelas;
    private Button principal;
    private Button doInicio;
    private Button botaoCapitulos;
    private ImageButton favoritar;
    private LinearLayout dados;
    private View secaoCapitulos;
    private TextView capitulosSub;
    private HorizontalGridView fileiraCapitulos;
    private CapituloAdapter adapterCapitulos;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repositorio = RepositorioBiblioteca.atual(this);
        id = getIntent().getStringExtra(Navegacao.EXTRA_ID);
        if (repositorio.titulo(id) == null) {
            finish();
            return;
        }
        setContentView(R.layout.st_tv_detalhe);

        fundo = findViewById(R.id.st_detalhe_fundo);
        capa = findViewById(R.id.st_detalhe_capa);
        tipo = findViewById(R.id.st_detalhe_tipo);
        origemDados = findViewById(R.id.st_detalhe_origem_dados);
        nome = findViewById(R.id.st_detalhe_titulo);
        chips = findViewById(R.id.st_detalhe_chips);
        sinopse = findViewById(R.id.st_detalhe_sinopse);
        progresso = findViewById(R.id.st_detalhe_progresso);
        parou = findViewById(R.id.st_detalhe_parou);
        marcar = findViewById(R.id.st_detalhe_marcar);
        estrelas = findViewById(R.id.st_detalhe_nota);
        principal = findViewById(R.id.st_detalhe_principal);
        doInicio = findViewById(R.id.st_detalhe_do_inicio);
        botaoCapitulos = findViewById(R.id.st_detalhe_capitulos);
        favoritar = findViewById(R.id.st_detalhe_favoritar);
        dados = findViewById(R.id.st_detalhe_dados);
        secaoCapitulos = findViewById(R.id.st_secao_capitulos);
        capitulosSub = findViewById(R.id.st_capitulos_sub);
        fileiraCapitulos = findViewById(R.id.st_fileira_capitulos);
        View editar = findViewById(R.id.st_detalhe_editar);
        Button faixas = findViewById(R.id.st_detalhe_faixas);
        topo = new TopoSecaoTv(this);

        Resources res = getResources();
        Icones.noTexto(origemDados, R.drawable.st_ic_check, res.getDimensionPixelSize(R.dimen.st_tv_detalhe_selo_icone));
        Icones.noTexto(marcar, R.drawable.st_ic_check, res.getDimensionPixelSize(R.dimen.st_tv_detalhe_marcar_icone));
        int icone = res.getDimensionPixelSize(R.dimen.st_tv_botao_icone);
        Icones.noTexto(doInicio, R.drawable.st_ic_do_inicio, icone);
        Icones.noTexto(botaoCapitulos, R.drawable.st_ic_capitulos, icone);
        Icones.noTexto(faixas, R.drawable.st_ic_legendas, icone);

        editar.setOnClickListener(v -> Navegacao.abrirEditar(this, id));
        findViewById(R.id.st_botao_configuracoes).setOnClickListener(v -> Navegacao.abrirConfiguracoes(this));
        // Sempre de onde parou: "Do início" é o outro botão (sem posição guardada, dá no mesmo)
        principal.setOnClickListener(v -> Navegacao.abrirReproducao(this, id, Navegacao.DE_ONDE_PAROU));
        doInicio.setOnClickListener(v -> Navegacao.abrirReproducao(this, id, Navegacao.DO_INICIO));
        botaoCapitulos.setOnClickListener(v -> focarCapituloAtual());
        faixas.setOnClickListener(v -> Navegacao.abrirFaixas(this, id));
        marcar.setOnClickListener(v -> {
            repositorio.definirAssistido(id, !titulo.assistido);
            carregar();
        });
        favoritar.setOnClickListener(v -> {
            repositorio.definirFavorito(id, !titulo.favorito);
            carregar();
        });

        // ↑ dos botões (Editar agora fica ao lado do Favoritar) vai a "Marcar como assistido"
        int[] botoes = {R.id.st_detalhe_principal, R.id.st_detalhe_do_inicio, R.id.st_detalhe_capitulos,
                R.id.st_detalhe_faixas, R.id.st_detalhe_editar, R.id.st_detalhe_favoritar};
        for (int botao : botoes) findViewById(botao).setNextFocusUpId(R.id.st_detalhe_marcar);

        estrelas.preparar(true, this::aoEscolherNota);
        // Nas pontas da nota, ← e → saem para os vizinhos; ↓ vai ao botão principal (↑: em carregar)
        estrelas.setNextFocusLeftId(R.id.st_detalhe_marcar);
        estrelas.setNextFocusRightId(R.id.st_detalhe_favoritar);
        estrelas.setNextFocusDownId(R.id.st_detalhe_principal);

        adapterCapitulos = new CapituloAdapter(true, c -> Navegacao.abrirReproducao(this, id, c.numero));
        fileiraCapitulos.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        fileiraCapitulos.setHorizontalSpacing(res.getDimensionPixelSize(R.dimen.st_tv_capitulo_espaco));
        fileiraCapitulos.setAdapter(adapterCapitulos);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Volta de outra tela (Editar, player): o estado pode ter mudado
        relerSemTirarFoco();
        repositorio.lerDadosDoArquivo(id);
        repositorio.ouvir(aoMudarBiblioteca);
        if (primeiraVez) {
            primeiraVez = false;
            principal.requestFocus();
        }
    }

    @Override
    protected void onPause() {
        repositorio.deixarDeOuvir(aoMudarBiblioteca);
        super.onPause();
    }

    /**
     * Vídeo que a varredura ainda não leu (ou cuja leitura falhou): o repositório lê na hora; quando
     * os capítulos ou a duração chegam, a tela relê. Posição e assistido (o player grava ao sair, com
     * esta tela já de volta): só a parte do progresso. Nos outros avisos da varredura, nada, para não
     * mexer no foco.
     */
    private final Runnable aoMudarBiblioteca = () -> {
        Titulo novo = repositorio.titulo(id);
        if (novo == null || titulo == null) return;
        if (novo.duracaoSegundos != titulo.duracaoSegundos || repositorio.capitulos(id).size() != capitulos.size()
                || !Objects.equals(novo.titulo, titulo.titulo) || !Objects.equals(novo.sinopse, titulo.sinopse)
                || !Objects.equals(novo.imagemCapa(), titulo.imagemCapa())) {
            relerSemTirarFoco();
        } else if (novo.posicaoSegundos != titulo.posicaoSegundos || novo.assistido != titulo.assistido) {
            titulo = novo;
            mostrarProgresso();
            adapterCapitulos.definirAtual(PecasDetalhe.capituloAtual(capitulos, titulo));
        } else if (!Objects.equals(novo.nota, titulo.nota)) {
            titulo = novo;
            mostrarNota();
        }
    };

    /**
     * Relê com a tela aberta e o foco onde estava (no controle da TV, perder o foco é perder o lugar).
     * Os dados técnicos são refeitos: o foco volta para eles. Quem sumiu passa o foco ao botão principal.
     */
    private void relerSemTirarFoco() {
        View antes = getCurrentFocus();
        boolean nosDados = dados.hasFocus();
        carregar();
        if (antes == null || (antes.isFocused() && antes.isShown())) return;
        if (antes.isShown()) antes.requestFocus();
        else if (!nosDados || !dados.requestFocus()) principal.requestFocus();
    }

    private void carregar() {
        titulo = repositorio.titulo(id);
        if (titulo == null) {
            finish();
            return;
        }
        capitulos = repositorio.capitulos(id);
        DadosTecnicos tecnicos = repositorio.dadosTecnicos(id);

        topo.marcar(Navegacao.abaDoTipo(titulo.tipo), R.id.st_detalhe_principal);
        marcar.setNextFocusUpId(topo.idAtiva());
        estrelas.setNextFocusUpId(topo.idAtiva());
        fundo.setBackground(PecasDetalhe.fundoDoTitulo(this, titulo.corFundo));
        Imagens.fundoDaPagina(fundo, titulo.imagemFundo());
        float canto = getResources().getDimension(R.dimen.st_tv_detalhe_capa_canto);
        capa.setBackground(CapaAdapter.blocoDeCor(this, titulo.corCapa, canto, true));
        Imagens.noFundo(capa, titulo.imagemCapa(), Imagens.CAPA, canto);
        tipo.setText(PecasDetalhe.rotuloTipo(titulo.tipo));
        origemDados.setVisibility(titulo.naoIdentificado ? View.GONE : View.VISIBLE);
        origemDados.setText(titulo.editadosAMao.isEmpty() ? R.string.st_dados_tmdb : R.string.st_dados_tmdb_corrigidos);
        nome.setText(titulo.titulo);
        PecasDetalhe.chips(chips, titulo, tecnicos == null ? Collections.<String>emptyList() : tecnicos.chips,
                tecnicos == null ? 0 : tecnicos.chipsVideo, true);
        sinopse.setText(titulo.sinopse);
        sinopse.setVisibility(titulo.sinopse == null || titulo.sinopse.isEmpty() ? View.GONE : View.VISIBLE);

        mostrarProgresso();
        botaoCapitulos.setVisibility(capitulos.isEmpty() ? View.GONE : View.VISIBLE);

        favoritar.setContentDescription(getString(titulo.favorito ? R.string.st_remover_favoritos : R.string.st_favoritar));
        ImageViewCompat.setImageTintList(favoritar, ColorStateList.valueOf(
                ContextCompat.getColor(this, titulo.favorito ? R.color.st_favorito : R.color.st_texto)));

        if (tecnicos != null) PecasDetalhe.dadosTecnicos(dados, tecnicos, true, () -> Navegacao.abrirFaixas(this, id));
        dados.setVisibility(tecnicos == null ? View.GONE : View.VISIBLE);

        secaoCapitulos.setVisibility(capitulos.isEmpty() ? View.GONE : View.VISIBLE);
        capitulosSub.setText(getString(R.string.st_capitulos_lidos, capitulos.size(), PecasDetalhe.extensao(titulo.arquivo)));
        adapterCapitulos.atualizar(capitulos, PecasDetalhe.capituloAtual(capitulos, titulo));
    }

    /** O que depende da posição e do assistido: não refaz nada que o foco possa estar usando. */
    private void mostrarProgresso() {
        // Progresso só com o vídeo em andamento; "Marcar como assistido" sempre
        boolean andamento = titulo.emAndamento();
        progresso.setVisibility(andamento ? View.VISIBLE : View.GONE);
        parou.setVisibility(andamento ? View.VISIBLE : View.GONE);
        if (andamento) {
            progresso.setProgress(titulo.progresso());
            parou.setText(getString(R.string.st_parou_em, PecasDetalhe.tempo(titulo.posicaoSegundos), titulo.progresso()));
        }
        ViewGroup.MarginLayoutParams lpMarcar = (ViewGroup.MarginLayoutParams) marcar.getLayoutParams();
        lpMarcar.setMarginStart(andamento ? getResources().getDimensionPixelSize(R.dimen.st_tv_detalhe_marcar_margem) : 0);
        marcar.setLayoutParams(lpMarcar);
        marcar.setText(titulo.assistido ? R.string.st_marcar_nao_assistido : R.string.st_marcar_assistido);

        // D22: sem progresso, "Assistir" no lugar de Continuar e Do início
        principal.setText(andamento ? R.string.st_continuar : R.string.st_assistir);
        Icones.noTexto(principal, R.drawable.st_ic_assistir, getResources().getDimensionPixelSize(R.dimen.st_tv_botao_icone));
        // "Do início" vai sumir com o foco nele: passa antes, para não cair no topo da tela
        if (!andamento && doInicio.hasFocus()) principal.requestFocus();
        doInicio.setVisibility(andamento ? View.VISIBLE : View.GONE);
        mostrarNota();
    }

    /** Estrelas só em filme assistido (D6). A fileira não é refeita: o foco fica nela. */
    private void mostrarNota() {
        boolean pode = titulo.podeDarNota();
        // Vai sumir com o foco nela: passa antes, para não cair no topo da tela
        if (!pode && estrelas.hasFocus()) marcar.requestFocus();
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

    /** "Capítulos": foco no capítulo onde o vídeo parou (ou no primeiro). */
    private void focarCapituloAtual() {
        if (capitulos.isEmpty()) return;
        int atual = PecasDetalhe.capituloAtual(capitulos, titulo);
        fileiraCapitulos.setSelectedPosition(Math.max(0, atual - 1));
        fileiraCapitulos.requestFocus();
    }
}
