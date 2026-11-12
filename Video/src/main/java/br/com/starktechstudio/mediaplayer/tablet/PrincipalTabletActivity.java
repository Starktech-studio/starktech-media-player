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
import br.com.starktechstudio.mediaplayer.comum.Preferencias;
import br.com.starktechstudio.mediaplayer.comum.TelaCarregamento;
import br.com.starktechstudio.mediaplayer.dados.RepositorioBiblioteca;

/**
 * Tela principal do tablet (tablet-biblioteca.dc.html): barra lateral com logo e os itens
 * Início (em amarelo), as abas ligadas em Configurações &gt; Navegação e Ajustes (ativo com fundo azul 18 %; a
 * barra rola se não couber); na aba
 * Início, a marca e o selo do HD (verde "HD externo" / vermelho "Sem HD ✕"); em todas, o relógio. Sem busca
 * e sem filtros. O selo segue o HD de verdade (ligado ou desligado com o app aberto).
 */
public class PrincipalTabletActivity extends AppCompatActivity implements Navegacao.TelaComAbas {

    private static final String ESTADO_ABA = "aba";

    /** Início, as abas em ordem alfabética e Ajustes (1.5.1, {@link Navegacao#emOrdem}); montada no onCreate. */
    private Aba[] abas = Aba.values();

    private final View[] itens = new View[Aba.values().length];
    private View topoInicio;
    private View topoMarca;
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
        topoMarca = findViewById(R.id.st_topo_marca);
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
        // 1.5.1: Início primeiro (em destaque), as outras em ordem alfabética e Ajustes no fim
        abas = Navegacao.emOrdem(this, Aba.values());
        for (int i = 0; i < abas.length; i++) {
            final int indice = i;
            View item = inflater.inflate(R.layout.st_tablet_item_barra, barra, false);
            ImageView icone = item.findViewById(R.id.st_item_barra_icone);
            TextView texto = item.findViewById(R.id.st_item_barra_texto);
            icone.setImageResource(abas[i].icone);
            texto.setText(abas[i].nome);
            // Início em destaque: ícone e texto em amarelo, parado ou ativo
            if (abas[i] == Aba.INICIO) {
                int amarelo = ContextCompat.getColor(this, R.color.st_amarelo_marca);
                ImageViewCompat.setImageTintList(icone, ColorStateList.valueOf(amarelo));
                texto.setTextColor(amarelo);
                item.setBackgroundResource(R.drawable.st_item_barra_inicio);
            }
            item.setOnClickListener(v -> selecionarAba(indice));
            barra.addView(item);
            itens[i] = item;
        }
        mostrarAbasLigadas();

        Aba pedida = Navegacao.abaDoIntent(getIntent());
        int inicial = savedInstanceState != null ? savedInstanceState.getInt(ESTADO_ABA, 0)
                : pedida == null ? 0 : indiceDe(pedida);
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

    /** Telas que pedem uma aba. */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        Aba aba = Navegacao.abaDoIntent(intent);
        if (aba != null) selecionarAba(indiceDe(aba));
    }

    @Override
    protected void onResume() {
        super.onResume();
        mostrarAbasLigadas();
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

    @Override
    public void aoMudarAbas() {
        mostrarAbasLigadas();
    }

    /** Posição da aba na barra ({@link #abas}); 0 (Início) se não estiver. */
    private int indiceDe(Aba aba) {
        for (int i = 0; i < abas.length; i++) if (abas[i] == aba) return i;
        return 0;
    }

    /** Só as abas ligadas em Configurações &gt; Navegação; a aba aberta que foi desligada volta ao Início. */
    private void mostrarAbasLigadas() {
        for (int i = 0; i < abas.length; i++) {
            itens[i].setVisibility(Preferencias.abaLigada(this, abas[i]) ? View.VISIBLE : View.GONE);
        }
        if (abaAtiva >= 0 && itens[abaAtiva].getVisibility() != View.VISIBLE) selecionarAba(indiceDe(Aba.INICIO));
    }

    private void selecionarAba(int indice) {
        if (indice == abaAtiva) return;
        marcarItem(indice);
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.st_conteudo, Navegacao.conteudoDaAba(this, abas[indice]))
                .commitNow();
    }

    private void marcarItem(int indice) {
        abaAtiva = indice;
        for (int i = 0; i < itens.length; i++) itens[i].setSelected(i == indice);
        // Marca e selo do HD só no Início (nas outras abas o desenho começa pelo título); o relógio fica em todas
        boolean inicio = abas[indice] == Aba.INICIO;
        topoMarca.setVisibility(inicio ? View.VISIBLE : View.GONE);
        seloHd.setVisibility(inicio ? View.VISIBLE : View.GONE);
        topoInicio.setPaddingRelative(topoInicio.getPaddingStart(),
                getResources().getDimensionPixelSize(inicio ? R.dimen.st_tablet_margem_v : R.dimen.st_tablet_relogio_topo),
                topoInicio.getPaddingEnd(), topoInicio.getPaddingBottom());
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
