// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.tablet;

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

import com.archos.mediacenter.video.BuildConfig;
import com.archos.mediacenter.video.R;

import br.com.starktechstudio.mediaplayer.comum.BordasSistema;
import br.com.starktechstudio.mediaplayer.comum.Marca;
import br.com.starktechstudio.mediaplayer.comum.Navegacao;
import br.com.starktechstudio.mediaplayer.comum.Navegacao.Aba;
import br.com.starktechstudio.mediaplayer.dados.RepositorioBiblioteca;

/**
 * Tela principal do tablet (tablet-biblioteca.dc.html): barra lateral com logo e os itens
 * Início, Filmes, Séries, Shows, Pastas e Ajustes (ativo com fundo azul 18 %); na aba
 * Início, a marca e o selo do HD (verde "HD externo" / vermelho "Sem HD ✕"). Sem busca
 * e sem filtros. No APK debug, toque longo no selo alterna o HD (para teste).
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

        Marca.aplicar(findViewById(R.id.st_marca));
        topoInicio = findViewById(R.id.st_topo_inicio);
        seloHd = findViewById(R.id.st_selo_hd);
        seloHdIcone = findViewById(R.id.st_selo_hd_icone);
        seloHdTexto = findViewById(R.id.st_selo_hd_texto);
        seloHdFechar = findViewById(R.id.st_selo_hd_fechar);

        ViewGroup barra = findViewById(R.id.st_barra_lateral);
        // Fundo da barra lateral até as bordas de cima e de baixo; logo e conteúdo fora das barras do sistema
        BordasSistema.aplicar(findViewById(R.id.st_corpo), true, false, true, false);
        BordasSistema.aplicar(barra, false, true, false, true);
        BordasSistema.aplicar(findViewById(R.id.st_coluna_conteudo), false, true, false, true);
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

        if (BuildConfig.DEBUG) {
            seloHd.setOnLongClickListener(v -> {
                repositorio.definirHdConectado(!repositorio.hdConectado());
                atualizarSeloHd();
                return true;
            });
        }

        int inicial = savedInstanceState == null ? 0 : savedInstanceState.getInt(ESTADO_ABA, 0);
        boolean restaurado = savedInstanceState != null
                && getSupportFragmentManager().findFragmentById(R.id.st_conteudo) != null;
        if (restaurado) marcarItem(inicial);
        else selecionarAba(inicial);
    }

    @Override
    protected void onResume() {
        super.onResume();
        atualizarSeloHd();
    }

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
                .replace(R.id.st_conteudo, Navegacao.conteudoDaAba(ABAS[indice]))
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
