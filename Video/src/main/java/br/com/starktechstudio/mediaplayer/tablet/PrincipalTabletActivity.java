// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.tablet;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import com.archos.mediacenter.video.R;

import br.com.starktechstudio.mediaplayer.comum.BordasSistema;
import br.com.starktechstudio.mediaplayer.comum.Navegacao;
import br.com.starktechstudio.mediaplayer.comum.Navegacao.Aba;
import br.com.starktechstudio.mediaplayer.comum.AvisoDeVersao;
import br.com.starktechstudio.mediaplayer.comum.PermissaoVideos;
import br.com.starktechstudio.mediaplayer.comum.TelaCarregamento;
import br.com.starktechstudio.mediaplayer.dados.RepositorioBiblioteca;

/**
 * Tela principal do tablet (tablet-biblioteca.dc.html): barra lateral com logo e os itens
 * Início, Filmes, Séries, Desenhos, Shows, Stand-up, Pastas e Ajustes (ativo com fundo azul 18 %; a barra
 * rola se não couber); na aba
 * Início, a marca e o selo do HD (verde "HD externo" / vermelho "Sem HD ✕"). Sem busca
 * e sem filtros. O selo segue o HD de verdade (ligado ou desligado com o app aberto).
 */
public class PrincipalTabletActivity extends AppCompatActivity {

    private static final String ESTADO_ABA = "aba";

    private static final Aba[] ABAS = Aba.values();

    private final View[] itens = new View[ABAS.length];
    private View topoInicio;
    private View seloHd;
    private ImageView seloHdIcone;
    private TextView seloHdTexto;
    private View seloHdFechar;
    private RepositorioBiblioteca repositorio;
    private int abaAtiva = -1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.st_tablet_principal);
        repositorio = RepositorioBiblioteca.atual(this);

        topoInicio = findViewById(R.id.st_topo_inicio);
        seloHd = findViewById(R.id.st_selo_hd);
        seloHdIcone = findViewById(R.id.st_selo_hd_icone);
        seloHdTexto = findViewById(R.id.st_selo_hd_texto);
        seloHdFechar = findViewById(R.id.st_selo_hd_fechar);

        ViewGroup barra = findViewById(R.id.st_barra_lateral);
        // Fundo da barra lateral até as bordas de cima e de baixo; logo e conteúdo fora das barras do sistema.
        // Embaixo, cada aba soma a barra de navegação à própria rolagem: o conteúdo passa por trás dela
        BordasSistema.aplicar(findViewById(R.id.st_corpo), true, false, true, false);
        BordasSistema.aplicar(barra, false, true, false, true);
        BordasSistema.aplicar(findViewById(R.id.st_coluna_conteudo), false, true, false, false);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < ABAS.length; i++) {
            final int indice = i;
            View item = inflater.inflate(R.layout.st_tablet_item_barra, barra, false);
            ((ImageView) item.findViewById(R.id.st_item_barra_icone)).setImageResource(ABAS[i].icone);
            ((TextView) item.findViewById(R.id.st_item_barra_texto)).setText(ABAS[i].nome);
            item.setOnClickListener(v -> selecionarAba(indice));
            barra.addView(item);
            itens[i] = item;
        }

        Aba pedida = Navegacao.abaDoIntent(getIntent());
        int inicial = savedInstanceState != null ? savedInstanceState.getInt(ESTADO_ABA, 0)
                : pedida == null ? 0 : pedida.ordinal();
        boolean restaurado = savedInstanceState != null
                && getSupportFragmentManager().findFragmentById(R.id.st_conteudo) != null;
        if (restaurado) marcarItem(inicial);
        else selecionarAba(inicial);
        // Ao abrir o app: tela de carregamento por cima até a biblioteca ficar pronta
        if (savedInstanceState == null) {
            TelaCarregamento.mostrar(this);
            PermissaoVideos.pedirAoAbrirOApp(this);
            AvisoDeVersao.procurarAoAbrir(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int pedido, @NonNull String[] permissoes, @NonNull int[] respostas) {
        super.onRequestPermissionsResult(pedido, permissoes, respostas);
        PermissaoVideos.aoResponder(this, pedido, respostas);
    }

    /** Pastas da biblioteca (em Ajustes) e outras telas que pedem uma aba. */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        Aba aba = Navegacao.abaDoIntent(intent);
        if (aba != null) selecionarAba(aba.ordinal());
    }

    @Override
    protected void onResume() {
        super.onResume();
        atualizarSeloHd();
        // O HD pode ter saído ou voltado com o app fora da tela
        repositorio.conferirBiblioteca();
        // HD ligado ou desligado com o app aberto
        repositorio.ouvir(aoMudarBiblioteca);
    }

    @Override
    protected void onPause() {
        repositorio.deixarDeOuvir(aoMudarBiblioteca);
        super.onPause();
    }

    private final Runnable aoMudarBiblioteca = this::atualizarSeloHd;

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ESTADO_ABA, Math.max(abaAtiva, 0));
    }

    private void selecionarAba(int indice) {
        if (indice == abaAtiva) return;
        marcarItem(indice);
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.st_conteudo, Navegacao.conteudoDaAba(this, ABAS[indice]))
                .commitNow();
    }

    private void marcarItem(int indice) {
        abaAtiva = indice;
        for (int i = 0; i < itens.length; i++) itens[i].setSelected(i == indice);
        // Marca e selo do HD só no Início (nas outras abas o desenho começa pelo título)
        topoInicio.setVisibility(ABAS[indice] == Aba.INICIO ? View.VISIBLE : View.GONE);
    }

    private void atualizarSeloHd() {
        boolean conectado = repositorio.hdConectado();
        int cor = ContextCompat.getColor(this, conectado ? R.color.st_selo_hd_texto : R.color.st_selo_sem_hd_texto);
        seloHd.setBackgroundResource(conectado ? R.drawable.st_selo_hd : R.drawable.st_selo_sem_hd);
        ImageViewCompat.setImageTintList(seloHdIcone, ColorStateList.valueOf(cor));
        seloHdTexto.setText(conectado ? R.string.st_hd_conectado : R.string.st_hd_desconectado);
        seloHdTexto.setTextColor(cor);
        seloHdFechar.setVisibility(conectado ? View.GONE : View.VISIBLE);
    }
}
