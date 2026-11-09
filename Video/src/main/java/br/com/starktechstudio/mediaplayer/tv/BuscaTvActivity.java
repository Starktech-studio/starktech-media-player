// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.tv;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.leanback.widget.VerticalGridView;

import com.archos.mediacenter.video.R;

import java.util.Collections;
import java.util.List;

import br.com.starktechstudio.mediaplayer.comum.CapaAdapter;
import br.com.starktechstudio.mediaplayer.comum.Icones;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Titulo;
import br.com.starktechstudio.mediaplayer.dados.RepositorioBiblioteca;

/**
 * Buscar da TV (lupa do topo; D11 c, sem desenho): campo e grade de capas no padrão das abas
 * Filmes, Séries e Shows; o resultado muda a cada letra (filmes, séries e shows juntos). OK numa
 * capa abre o título; segurar OK abre o Menu da capa. ↓ do campo vai à grade. Busca na biblioteca.
 */
public class BuscaTvActivity extends AppCompatActivity {

    private RepositorioBiblioteca repositorio;
    private CapaAdapter adapter;
    private EditText campo;
    private TextView vazio;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.st_tv_busca);
        repositorio = RepositorioBiblioteca.atual(this);
        campo = findViewById(R.id.st_busca_campo);
        vazio = findViewById(R.id.st_busca_vazio);
        VerticalGridView grade = findViewById(R.id.st_busca_grade);
        findViewById(R.id.st_busca_voltar).setOnClickListener(v -> finish());
        Icones.noTexto(campo, R.drawable.st_ic_buscar, getResources().getDimensionPixelSize(R.dimen.st_tv_botao_icone),
                ContextCompat.getColor(this, R.color.st_texto_apagado));

        // Grade de capas como em ListaFragment: 98 dp, 14 dp entre colunas
        int capa = getResources().getDimensionPixelSize(R.dimen.st_tv_capa_largura);
        int espaco = getResources().getDimensionPixelSize(R.dimen.st_tv_capa_espaco);
        int largura = getResources().getDisplayMetrics().widthPixels - grade.getPaddingStart() - grade.getPaddingEnd();
        grade.setNumColumns(Math.max(1, (largura + espaco) / (capa + espaco)));
        grade.setColumnWidth(capa);
        grade.setHorizontalSpacing(espaco);
        grade.setVerticalSpacing(getResources().getDimensionPixelSize(R.dimen.st_tv_lista_espaco_vertical));
        adapter = new CapaAdapter(this, true);
        grade.setAdapter(adapter);
        campo.setNextFocusDownId(R.id.st_busca_grade);

        campo.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                buscar();
            }
        });
        campo.setOnEditorActionListener((v, acao, evento) -> {
            if (acao != EditorInfo.IME_ACTION_SEARCH) return false;
            if (adapter.getItemCount() > 0) grade.requestFocus();
            return true;
        });
        campo.requestFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Volta do título: o estado (assistido, título) pode ter mudado
        buscar();
    }

    private void buscar() {
        String consulta = campo.getText().toString().trim();
        List<Titulo> resultados = consulta.isEmpty() ? Collections.<Titulo>emptyList() : repositorio.buscar(consulta);
        adapter.atualizar(resultados);
        if (consulta.isEmpty()) {
            vazio.setText(R.string.st_busca_dica);
            vazio.setVisibility(View.VISIBLE);
        } else if (resultados.isEmpty()) {
            vazio.setText(getString(R.string.st_busca_nada, consulta));
            vazio.setVisibility(View.VISIBLE);
        } else {
            vazio.setVisibility(View.GONE);
        }
    }
}
