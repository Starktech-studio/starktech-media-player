// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.archos.mediacenter.video.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import br.com.starktechstudio.mediaplayer.dados.RepositorioMusicas;

/**
 * Nome da playlist (1.5): criar (com as músicas escolhidas, se houver) ou renomear. Caixa de texto e
 * Salvar / Cancelar, no padrão do Menu da capa. Na TV, o teclado abre sozinho. Nova sem nome: "Minha
 * playlist".
 */
public class NomePlaylistActivity extends AppCompatActivity {

    private static final String EXTRA_MUSICAS = "br.com.starktechstudio.mediaplayer.extra.MUSICAS_NOVA";
    private static final String EXTRA_PLAYLIST = "br.com.starktechstudio.mediaplayer.extra.RENOMEAR";
    private static final String EXTRA_NOME = "br.com.starktechstudio.mediaplayer.extra.NOME_ATUAL";

    /** Nova playlist; com músicas, já entra com elas e abre a playlist. */
    public static void abrirNova(Activity origem, List<String> musicas) {
        origem.startActivity(new Intent(origem, NomePlaylistActivity.class)
                .putStringArrayListExtra(EXTRA_MUSICAS, new ArrayList<>(musicas)));
    }

    public static void abrirRenomear(Activity origem, long id, String nome) {
        origem.startActivity(new Intent(origem, NomePlaylistActivity.class)
                .putExtra(EXTRA_PLAYLIST, id)
                .putExtra(EXTRA_NOME, nome));
    }

    private EditText nome;
    private long id;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        boolean tv = Aparelho.ehTv(this);
        setTheme(tv ? R.style.Theme_StarkTech_Menu_Tv : R.style.Theme_StarkTech_Menu);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.st_menu_capa);
        setFinishOnTouchOutside(true);
        id = getIntent().getLongExtra(EXTRA_PLAYLIST, -1);

        Resources res = getResources();
        int padding = res.getDimensionPixelSize(tv ? R.dimen.st_tv_menu_padding : R.dimen.st_tablet_menu_padding);
        LinearLayout caixa = findViewById(R.id.st_menu_caixa);
        ViewGroup.LayoutParams lpCaixa = caixa.getLayoutParams();
        lpCaixa.width = res.getDimensionPixelSize(tv ? R.dimen.st_tv_menu_largura : R.dimen.st_tablet_menu_largura) * 3 / 2;
        caixa.setLayoutParams(lpCaixa);
        caixa.setPadding(padding, padding, padding, padding);
        int recuo = padding * 2 / 3;

        TextView titulo = findViewById(R.id.st_menu_titulo);
        titulo.setText(id >= 0 ? R.string.st_musicas_renomear : R.string.st_musicas_nova_playlist);
        titulo.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(tv ? R.dimen.st_tv_texto_menu_titulo : R.dimen.st_tablet_texto_menu_titulo));
        titulo.setPadding(recuo, padding / 3, recuo, recuo / 2);
        TextView dica = findViewById(R.id.st_menu_dica);
        dica.setText(R.string.st_musicas_nome_playlist);
        dica.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(tv ? R.dimen.st_tv_texto_menu_dica : R.dimen.st_tablet_texto_menu_dica));
        dica.setPadding(recuo, 0, recuo, recuo);

        LinearLayout opcoes = findViewById(R.id.st_menu_opcoes);
        nome = new EditText(this);
        nome.setBackgroundResource(R.drawable.st_campo_texto);
        nome.setTypeface(ResourcesCompat.getFont(this, R.font.st_inter_semibold));
        nome.setTextColor(ContextCompat.getColor(this, R.color.st_texto));
        nome.setHintTextColor(ContextCompat.getColor(this, R.color.st_texto_apagado));
        nome.setHint(R.string.st_musicas_nome_padrao);
        nome.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(tv ? R.dimen.st_tv_texto_campo : R.dimen.st_tablet_texto_campo));
        int campo = res.getDimensionPixelSize(tv ? R.dimen.st_tv_campo_padding : R.dimen.st_tablet_campo_padding);
        nome.setPadding(campo, 0, campo, 0);
        nome.setSingleLine(true);
        nome.setGravity(Gravity.CENTER_VERTICAL);
        nome.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        nome.setImeOptions(EditorInfo.IME_ACTION_DONE);
        if (tv) nome.setForeground(ContextCompat.getDrawable(this, R.drawable.st_foco_tv));
        String atual = getIntent().getStringExtra(EXTRA_NOME);
        if (atual != null) nome.setText(atual);
        nome.setSelectAllOnFocus(true);
        nome.setOnEditorActionListener((v, acao, evento) -> {
            boolean enter = evento != null && evento.getKeyCode() == KeyEvent.KEYCODE_ENTER && evento.getAction() == KeyEvent.ACTION_DOWN;
            if (acao == EditorInfo.IME_ACTION_DONE || enter) {
                salvar();
                return true;
            }
            return false;
        });
        LinearLayout.LayoutParams lpNome = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                res.getDimensionPixelSize(tv ? R.dimen.st_tv_campo_altura : R.dimen.st_tablet_campo_altura));
        lpNome.bottomMargin = recuo;
        opcoes.addView(nome, lpNome);
        OpcaoMenu.adicionar(opcoes, R.drawable.st_ic_check, getString(R.string.st_salvar), this::salvar);
        OpcaoMenu.adicionar(opcoes, R.drawable.st_ic_fechar, getString(R.string.st_cancelar), this::finish);

        nome.requestFocus();
        nome.post(() -> {
            InputMethodManager teclado = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (teclado != null) teclado.showSoftInput(nome, InputMethodManager.SHOW_IMPLICIT);
        });
    }

    private void salvar() {
        String texto = nome.getText().toString().trim();
        if (texto.isEmpty()) texto = getString(R.string.st_musicas_nome_padrao);
        RepositorioMusicas repositorio = RepositorioMusicas.de(this);
        if (id >= 0) {
            repositorio.renomearPlaylist(id, texto);
            finish();
            return;
        }
        List<String> musicas = getIntent().getStringArrayListExtra(EXTRA_MUSICAS);
        if (musicas == null) musicas = Collections.emptyList();
        final String nomeFinal = texto;
        final boolean comMusicas = !musicas.isEmpty();
        Context app = getApplicationContext();
        repositorio.criarPlaylist(texto, musicas, novo -> {
            if (comMusicas) Toast.makeText(app, app.getString(R.string.st_musicas_na_playlist_feito, nomeFinal), Toast.LENGTH_SHORT).show();
        });
        finish();
    }
}
