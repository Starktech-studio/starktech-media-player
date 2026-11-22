// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.comum;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.method.ArrowKeyMovementMethod;
import android.text.method.KeyListener;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.archos.mediacenter.video.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import br.com.starktechstudio.mediaplayer.dados.Modelo;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Origem;
import br.com.starktechstudio.mediaplayer.dados.RepositorioBiblioteca;
import br.com.starktechstudio.mediaplayer.motor.ArquivosDaBiblioteca;
import br.com.starktechstudio.mediaplayer.motor.EstadoServidor;
import br.com.starktechstudio.mediaplayer.motor.Servidores;

/**
 * Adicionar servidor (2.0, D6), em Pastas: IP fixo, usuário e senha (campo de senha) e Entrar, que testa e diz o
 * erro (fora de alcance, usuário ou senha); depois o compartilhamento e as pastas, no padrão do Adicionar pasta, com
 * "Adicionar esta pasta". Editar servidor ({@link #abrirEditar}): IP, compartilhamento, usuário e senha (vazia =
 * mantém a guardada), Salvar e Remover servidor (apaga a senha e as pastas, com confirmação).
 *
 * <p>A senha fica só nos campos e na memória do Nova durante o teste ({@link Servidores#credencialDoTeste}); gravada
 * só cifrada ({@link RepositorioBiblioteca#salvarServidor}); nunca em log, aviso ou endereço. Na TV, as caixas são
 * como as do Editar: o foco só realça, OK abre o teclado da TV.
 */
public class ServidorActivity extends AppCompatActivity {

    private static final String LOG = "StarkTechServidor";
    private static final String EXTRA_SERVIDOR = "br.com.starktechstudio.mediaplayer.extra.SERVIDOR";
    private static final String EXTRA_REMOVER = "br.com.starktechstudio.mediaplayer.extra.REMOVER_SERVIDOR";

    /** Resultado do teste do Entrar ou do Salvar. */
    private enum Falha { NENHUMA, NAO_RESPONDE, SENHA, SEM_SENHA_GUARDADA, NAO_ABRIU }

    /** TV, caixa só em foco: nenhuma tecla digita e o tipo nulo deixa o teclado do sistema de fora. */
    private static final KeyListener SEM_TECLAS = new KeyListener() {
        @Override
        public int getInputType() {
            return InputType.TYPE_NULL;
        }

        @Override
        public boolean onKeyDown(View view, Editable text, int keyCode, KeyEvent event) {
            return false;
        }

        @Override
        public boolean onKeyUp(View view, Editable text, int keyCode, KeyEvent event) {
            return false;
        }

        @Override
        public boolean onKeyOther(View view, Editable text, KeyEvent event) {
            return false;
        }

        @Override
        public void clearMetaKeyState(View view, Editable content, int states) {
        }
    };

    public static void abrirNovo(Activity origem) {
        origem.startActivity(new Intent(origem, ServidorActivity.class));
    }

    public static void abrirEditar(Activity origem, long id) {
        origem.startActivity(new Intent(origem, ServidorActivity.class).putExtra(EXTRA_SERVIDOR, id));
    }

    /** Remover servidor (Pastas): direto na confirmação; Cancelar fecha. */
    public static void abrirRemover(Activity origem, long id) {
        origem.startActivity(new Intent(origem, ServidorActivity.class).putExtra(EXTRA_SERVIDOR, id).putExtra(EXTRA_REMOVER, true));
    }

    private final ExecutorService rede = Executors.newSingleThreadExecutor(r -> new Thread(r, "StarkTechServidorTela"));
    private boolean tv;
    private RepositorioBiblioteca repositorio;
    @Nullable
    private Modelo.Servidor editando;
    /** Aberta por Remover servidor: Cancelar fecha a tela. */
    private boolean soRemover;
    private TextView titulo;
    private TextView dica;
    private LinearLayout opcoes;
    private NavegadorPastasActivity.RolagemLimitada rolagem;
    private EditText campoIp;
    private EditText campoCompartilhamento;
    private EditText campoUsuario;
    private EditText campoSenha;
    /** Caixa com o teclado (TV); nula = só foco. */
    @Nullable
    private EditText caixaAberta;
    /** Teste em andamento: os botões não repetem. */
    private boolean ocupado;
    // Depois do Entrar: o que valeu no teste (a senha só aqui, na memória desta tela)
    private String ip;
    private String usuario;
    private String senha;

    private final OnBackPressedCallback voltarDaCaixa = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            if (caixaAberta != null) sairDaCaixa(caixaAberta);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        tv = Aparelho.ehTv(this);
        setTheme(tv ? R.style.Theme_StarkTech_Menu_Tv : R.style.Theme_StarkTech_Menu);
        super.onCreate(savedInstanceState);
        repositorio = RepositorioBiblioteca.atual(this);
        long id = getIntent().getLongExtra(EXTRA_SERVIDOR, -1);
        if (id >= 0) {
            for (Modelo.Servidor s : repositorio.servidores()) if (s.id == id) editando = s;
            if (editando == null) {
                finish();
                return;
            }
        }
        setContentView(R.layout.st_menu_capa);
        setFinishOnTouchOutside(true);
        getOnBackPressedDispatcher().addCallback(this, voltarDaCaixa);

        Resources res = getResources();
        int padding = res.getDimensionPixelSize(tv ? R.dimen.st_tv_menu_padding : R.dimen.st_tablet_menu_padding);
        LinearLayout caixa = findViewById(R.id.st_menu_caixa);
        ViewGroup.LayoutParams lpCaixa = caixa.getLayoutParams();
        lpCaixa.width = res.getDimensionPixelSize(tv ? R.dimen.st_tv_menu_largura : R.dimen.st_tablet_menu_largura) * 3 / 2;
        caixa.setLayoutParams(lpCaixa);
        caixa.setPadding(padding, padding, padding, padding);
        int recuo = padding * 2 / 3;
        titulo = findViewById(R.id.st_menu_titulo);
        titulo.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(tv ? R.dimen.st_tv_texto_menu_titulo : R.dimen.st_tablet_texto_menu_titulo));
        titulo.setPadding(recuo, padding / 3, recuo, 0);
        dica = findViewById(R.id.st_menu_dica);
        dica.setSingleLine(false);
        dica.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(tv ? R.dimen.st_tv_texto_menu_dica : R.dimen.st_tablet_texto_menu_dica));
        dica.setPadding(recuo, 0, recuo, recuo);

        // Campos e muitas pastas: a lista rola dentro da caixa (até 80 % da altura da tela)
        opcoes = findViewById(R.id.st_menu_opcoes);
        int posicao = caixa.indexOfChild(opcoes);
        caixa.removeView(opcoes);
        rolagem = new NavegadorPastasActivity.RolagemLimitada(this, Math.round(res.getDisplayMetrics().heightPixels * 0.8f));
        rolagem.setVerticalScrollBarEnabled(false);
        rolagem.setOverScrollMode(View.OVER_SCROLL_NEVER);
        rolagem.addView(opcoes, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        caixa.addView(rolagem, posicao, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        campoIp = caixaDeTexto(R.string.st_servidor_ip_exemplo, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI, false);
        campoCompartilhamento = caixaDeTexto(0, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS, false);
        campoUsuario = caixaDeTexto(0, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS, false);
        campoSenha = caixaDeTexto(editando != null ? R.string.st_servidor_senha_guardada : 0,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD, true);
        if (editando != null) {
            campoIp.setText(editando.ip);
            campoCompartilhamento.setText(editando.compartilhamento);
            campoUsuario.setText(editando.usuario);
        }
        soRemover = editando != null && getIntent().getBooleanExtra(EXTRA_REMOVER, false);
        if (soRemover) mostrarRemover();
        else mostrarEntrada(editando != null && editando.precisaDaSenha ? getString(R.string.st_servidor_digite_a_senha) : null);
    }

    @Override
    protected void onDestroy() {
        rede.shutdownNow();
        senha = null;
        super.onDestroy();
    }

    // ---- Entrada: IP, usuário e senha ----

    private void mostrarEntrada(@Nullable String aviso) {
        titulo.setText(editando != null ? R.string.st_editar_servidor : R.string.st_adicionar_servidor);
        dica.setText(editando != null ? R.string.st_servidor_dica_editar : R.string.st_servidor_dica);
        limpar();
        if (aviso != null) aviso(aviso, R.color.st_aviso_claro);
        campo(R.string.st_servidor_ip, campoIp);
        if (editando != null) campo(R.string.st_servidor_compartilhamento, campoCompartilhamento);
        campo(R.string.st_servidor_usuario, campoUsuario);
        campo(R.string.st_servidor_senha, campoSenha);
        View principal = opcao(R.drawable.st_ic_check,
                getString(editando != null ? R.string.st_salvar : R.string.st_servidor_entrar), this::entrar);
        if (editando != null) opcao(R.drawable.st_ic_lixeira, getString(R.string.st_remover_servidor), this::mostrarRemover);
        opcao(R.drawable.st_ic_fechar, getString(R.string.st_cancelar), this::finish);
        if (tv) (campoIp.getText().length() == 0 ? campoIp : principal).requestFocus();
        else if (campoIp.getText().length() == 0) campoIp.requestFocus();
    }

    /** Confere o que foi digitado e testa fora da tela: porta, credencial e a lista do servidor. */
    private void entrar() {
        if (ocupado) return;
        if (caixaAberta != null) sairDaCaixa(caixaAberta);
        String digitado = campoIp.getText().toString().trim();
        if (digitado.isEmpty()) {
            mostrarEntrada(getString(R.string.st_servidor_sem_ip));
            return;
        }
        if (!Servidores.ipValido(digitado)) {
            mostrarEntrada(getString(R.string.st_servidor_ip_invalido));
            return;
        }
        String compartilhamento = campoCompartilhamento.getText().toString().trim();
        if (editando != null && compartilhamento.isEmpty()) {
            mostrarEntrada(getString(R.string.st_servidor_sem_compartilhamento));
            return;
        }
        ip = digitado;
        usuario = campoUsuario.getText().toString().trim();
        senha = campoSenha.getText().toString();
        final boolean manterSenha = editando != null && senha.isEmpty();
        final long id = editando == null ? -1 : editando.id;
        ocupado = true;
        dica.setText(R.string.st_servidor_entrando);
        Context app = getApplicationContext();
        final String ipTeste = ip;
        final String usuarioTeste = usuario;
        final String senhaTeste = senha;
        rede.execute(() -> {
            Falha falha;
            List<String> compartilhamentos = new ArrayList<>();
            try {
                if (!EstadoServidor.portaAbre(ipTeste)) {
                    falha = Falha.NAO_RESPONDE;
                } else {
                    Servidores servidores = Servidores.de(app);
                    boolean comCredencial = true;
                    if (manterSenha) comCredencial = servidores.credencialGuardadaDoTeste(id, ipTeste, usuarioTeste);
                    else servidores.credencialDoTeste(ipTeste, usuarioTeste, senhaTeste);
                    if (!comCredencial) {
                        falha = Falha.SEM_SENHA_GUARDADA;
                    } else {
                        ArquivosDaBiblioteca arquivos = ArquivosDaBiblioteca.de(app);
                        // Editar: o compartilhamento digitado abre; Adicionar: a lista dos compartilhamentos
                        String endereco = id >= 0 ? Servidores.enderecoDaPasta(ipTeste, compartilhamento, "")
                                : Servidores.enderecoDoIp(ipTeste);
                        for (ArquivosDaBiblioteca.Entrada e : arquivos.listarEndereco(endereco)) {
                            if (e.pasta) compartilhamentos.add(e.nome);
                        }
                        falha = Falha.NENHUMA;
                    }
                }
            } catch (ArquivosDaBiblioteca.SenhaRecusada e) {
                falha = Falha.SENHA;
            } catch (IOException | RuntimeException e) {
                Log.w(LOG, "teste do servidor: " + e.getClass().getSimpleName());
                falha = Falha.NAO_ABRIU;
            }
            final Falha resultado = falha;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                ocupado = false;
                if (resultado != Falha.NENHUMA) {
                    mostrarEntrada(getString(textoDa(resultado)));
                } else if (editando != null) {
                    salvarEdicao(compartilhamento, manterSenha);
                } else {
                    mostrarCompartilhamentos(compartilhamentos);
                }
            });
        });
    }

    @StringRes
    private static int textoDa(Falha falha) {
        switch (falha) {
            case NAO_RESPONDE: return R.string.st_servidor_nao_responde;
            case SENHA: return R.string.st_servidor_senha_recusada;
            case SEM_SENHA_GUARDADA: return R.string.st_servidor_digite_a_senha;
            default: return R.string.st_servidor_nao_abriu;
        }
    }

    private void salvarEdicao(String compartilhamento, boolean manterSenha) {
        Modelo.Servidor salvo = repositorio.salvarServidor(editando.id, ip, compartilhamento, usuario, manterSenha ? null : senha);
        Toast.makeText(this, salvo != null ? R.string.st_servidor_salvo : R.string.st_servidor_nao_salvou, Toast.LENGTH_SHORT).show();
        if (salvo != null) finish();
    }

    // ---- Compartilhamentos e pastas ----

    private void mostrarCompartilhamentos(List<String> compartilhamentos) {
        titulo.setText(R.string.st_servidor_escolha_compartilhamento);
        dica.setText(getString(R.string.st_servidor_em_casa) + " · " + ip);
        limpar();
        View voltar = opcao(R.drawable.st_ic_voltar, getString(R.string.st_subir), () -> mostrarEntrada(null));
        View primeira = null;
        for (String nome : compartilhamentos) {
            View item = opcao(R.drawable.st_ic_servidor, nome, () -> mostrarPasta(nome, ""));
            if (primeira == null) primeira = item;
        }
        if (compartilhamentos.isEmpty()) aviso(getString(R.string.st_servidor_sem_compartilhamentos), R.color.st_texto_apagado);
        if (tv) (primeira != null ? primeira : voltar).requestFocus();
        rolagem.scrollTo(0, 0);
    }

    /** Pasta {@code relativo} do compartilhamento: Adicionar esta pasta, Voltar uma pasta e as subpastas. */
    private void mostrarPasta(String compartilhamento, String relativo) {
        titulo.setText(relativo.isEmpty() ? compartilhamento : ArquivosDaBiblioteca.nome(relativo));
        String exibido = exibir(compartilhamento, relativo);
        dica.setText(exibido);
        limpar();
        ocupado = true;
        Context app = getApplicationContext();
        final String ipLista = ip;
        rede.execute(() -> {
            List<String> subpastas = new ArrayList<>();
            boolean abriu = true;
            try {
                for (ArquivosDaBiblioteca.Entrada e : ArquivosDaBiblioteca.de(app).listarEndereco(
                        Servidores.enderecoDaPasta(ipLista, compartilhamento, relativo))) {
                    if (e.pasta) subpastas.add(e.nome);
                }
            } catch (IOException | RuntimeException e) {
                Log.w(LOG, "pasta do servidor: " + e.getClass().getSimpleName());
                abriu = false;
            }
            final boolean ok = abriu;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                ocupado = false;
                limpar();
                View adicionar = opcao(R.drawable.st_ic_check, getString(R.string.st_adicionar_esta_pasta),
                        () -> adicionar(compartilhamento, relativo, exibido));
                opcao(R.drawable.st_ic_voltar, getString(R.string.st_subir), () -> {
                    if (relativo.isEmpty()) entrar();
                    else {
                        String acima = ArquivosDaBiblioteca.acima(relativo);
                        mostrarPasta(compartilhamento, acima == null ? "" : acima);
                    }
                });
                for (String sub : subpastas) {
                    opcao(R.drawable.st_ic_pastas, sub, () -> mostrarPasta(compartilhamento, relativo.isEmpty() ? sub : relativo + "/" + sub));
                }
                if (!ok) aviso(getString(R.string.st_servidor_nao_abriu), R.color.st_aviso_claro);
                else if (subpastas.isEmpty()) aviso(getString(R.string.st_sem_subpastas), R.color.st_texto_apagado);
                if (tv) adicionar.requestFocus();
                rolagem.scrollTo(0, 0);
            });
        });
    }

    /** "Servidor / midias / Filmes": como a pasta aparece em Pastas. */
    private String exibir(String compartilhamento, String relativo) {
        StringBuilder texto = new StringBuilder(getString(R.string.st_selo_servidor)).append(" / ").append(compartilhamento);
        for (String parte : relativo.split("/")) {
            if (!parte.isEmpty()) texto.append(" / ").append(parte);
        }
        return texto.toString();
    }

    /** Cadastra o servidor (ou acha o do mesmo IP e compartilhamento) e a pasta; a biblioteca varre de novo. */
    private void adicionar(String compartilhamento, String relativo, String exibido) {
        if (ocupado) return;
        Modelo.Servidor salvo = repositorio.salvarServidor(-1, ip, compartilhamento, usuario, senha);
        if (salvo == null) {
            Toast.makeText(this, R.string.st_servidor_nao_salvou, Toast.LENGTH_SHORT).show();
            return;
        }
        boolean entrou = repositorio.adicionarPasta(Origem.SERVIDOR, Servidores.caminho(salvo.id, relativo));
        Toast.makeText(this, entrou ? getString(R.string.st_pasta_adicionada, exibido)
                : getString(R.string.st_pasta_downloads_recusada), Toast.LENGTH_SHORT).show();
        finish();
    }

    // ---- Remover servidor ----

    private void mostrarRemover() {
        if (editando == null || ocupado) return;
        if (caixaAberta != null) sairDaCaixa(caixaAberta);
        String nome = getString(R.string.st_servidor_linha, editando.ip, editando.compartilhamento);
        titulo.setText(R.string.st_remover_servidor_titulo);
        dica.setText(getString(R.string.st_remover_servidor_dica, nome));
        limpar();
        opcao(R.drawable.st_ic_fechar, getString(R.string.st_remover), () -> {
            repositorio.removerServidor(editando.id);
            Toast.makeText(this, getString(R.string.st_servidor_removido, nome), Toast.LENGTH_SHORT).show();
            finish();
        });
        View cancelar = opcao(R.drawable.st_ic_voltar, getString(R.string.st_cancelar), () -> {
            if (soRemover) finish();
            else mostrarEntrada(null);
        });
        // Remover não pode ser o OK sem querer: a TV começa em Cancelar
        if (tv) cancelar.requestFocus();
        rolagem.scrollTo(0, 0);
    }

    // ---- Peças ----

    /** Tira tudo da lista (os campos saem do pai para voltar depois, com o texto digitado). */
    private void limpar() {
        opcoes.removeAllViews();
    }

    private View opcao(@DrawableRes int icone, String texto, Runnable acao) {
        return OpcaoMenu.adicionar(opcoes, icone, texto, () -> {
            if (!ocupado) acao.run();
        });
    }

    private void aviso(String texto, int cor) {
        TextView linha = new TextView(this);
        linha.setText(texto);
        linha.setTextColor(ContextCompat.getColor(this, cor));
        linha.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(
                tv ? R.dimen.st_tv_texto_menu_dica : R.dimen.st_tablet_texto_menu_dica));
        int recuo = getResources().getDimensionPixelSize(tv ? R.dimen.st_tv_menu_padding : R.dimen.st_tablet_menu_padding) * 2 / 3;
        linha.setPadding(recuo, recuo / 2, recuo, recuo / 2);
        opcoes.addView(linha);
    }

    /** Rótulo e a caixa embaixo. */
    private void campo(@StringRes int rotulo, EditText caixa) {
        Resources res = getResources();
        int recuo = res.getDimensionPixelSize(tv ? R.dimen.st_tv_menu_padding : R.dimen.st_tablet_menu_padding) * 2 / 3;
        TextView nome = new TextView(this);
        nome.setText(rotulo);
        nome.setTypeface(ResourcesCompat.getFont(this, R.font.st_inter_bold));
        nome.setTextColor(ContextCompat.getColor(this, R.color.st_texto_secundario));
        nome.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(tv ? R.dimen.st_tv_texto_menu_dica : R.dimen.st_tablet_texto_menu_dica));
        nome.setPadding(recuo, opcoes.getChildCount() > 0 ? recuo / 2 : 0, recuo, recuo / 3);
        opcoes.addView(nome);
        if (caixa.getParent() instanceof ViewGroup) ((ViewGroup) caixa.getParent()).removeView(caixa);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                res.getDimensionPixelSize(tv ? R.dimen.st_tv_campo_altura : R.dimen.st_tablet_campo_altura));
        lp.bottomMargin = recuo / 2;
        opcoes.addView(caixa, lp);
    }

    private EditText caixaDeTexto(@StringRes int exemplo, int entrada, boolean ehSenha) {
        Resources res = getResources();
        EditText caixa = new EditText(this);
        caixa.setBackgroundResource(R.drawable.st_campo_texto);
        caixa.setTypeface(ResourcesCompat.getFont(this, R.font.st_inter_semibold));
        caixa.setTextColor(ContextCompat.getColor(this, R.color.st_texto));
        caixa.setHintTextColor(ContextCompat.getColor(this, R.color.st_texto_apagado));
        if (exemplo != 0) caixa.setHint(exemplo);
        caixa.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(tv ? R.dimen.st_tv_texto_campo : R.dimen.st_tablet_texto_campo));
        int padding = res.getDimensionPixelSize(tv ? R.dimen.st_tv_campo_padding : R.dimen.st_tablet_campo_padding);
        caixa.setPadding(padding, 0, padding, 0);
        caixa.setSingleLine(true);
        caixa.setGravity(Gravity.CENTER_VERTICAL);
        caixa.setInputType(entrada);
        if (ehSenha) caixa.setTransformationMethod(PasswordTransformationMethod.getInstance());
        // A senha não vai para o preenchimento automático nem para sugestões
        if (ehSenha && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) caixa.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        caixa.setImeOptions(EditorInfo.IME_ACTION_DONE | EditorInfo.IME_FLAG_NO_FULLSCREEN);
        if (tv) {
            caixa.setForeground(ContextCompat.getDrawable(this, R.drawable.st_foco_tv));
            ligarCaixaTv(caixa, ehSenha);
        }
        return caixa;
    }

    // ---- Caixa de texto na TV (como no Editar) ----

    /** O foco só realça: sem teclado e com as setas andando entre as caixas. OK entra e abre o teclado da TV. */
    private void ligarCaixaTv(EditText caixa, boolean ehSenha) {
        final int entrada = caixa.getInputType();
        modoFoco(caixa, ehSenha);
        caixa.setOnClickListener(v -> {
            if (caixaAberta == caixa) abrirTeclado(caixa);
            else entrarNaCaixa(caixa, entrada, ehSenha);
        });
        caixa.setOnFocusChangeListener((v, temFoco) -> {
            if (!temFoco) sairDaCaixa(caixa);
        });
        caixa.setOnEditorActionListener((v, acao, evento) -> {
            if (acao != EditorInfo.IME_ACTION_DONE) return false;
            sairDaCaixa(caixa);
            return true;
        });
    }

    private static void modoFoco(EditText caixa, boolean ehSenha) {
        caixa.setKeyListener(SEM_TECLAS);
        caixa.setMovementMethod(null);
        caixa.setCursorVisible(false);
        caixa.setLongClickable(false);
        if (ehSenha) caixa.setTransformationMethod(PasswordTransformationMethod.getInstance());
    }

    private void entrarNaCaixa(EditText caixa, int entrada, boolean ehSenha) {
        if (caixaAberta != null && caixaAberta != caixa) sairDaCaixa(caixaAberta);
        caixaAberta = caixa;
        voltarDaCaixa.setEnabled(true);
        caixa.setInputType(entrada);
        if (ehSenha) caixa.setTransformationMethod(PasswordTransformationMethod.getInstance());
        caixa.setMovementMethod(ArrowKeyMovementMethod.getInstance());
        caixa.setCursorVisible(true);
        caixa.setSelection(caixa.getText().length());
        abrirTeclado(caixa);
    }

    private void abrirTeclado(EditText caixa) {
        InputMethodManager teclado = getSystemService(InputMethodManager.class);
        if (teclado != null) teclado.showSoftInput(caixa, 0);
    }

    private void sairDaCaixa(EditText caixa) {
        if (caixaAberta != caixa) return;
        caixaAberta = null;
        voltarDaCaixa.setEnabled(false);
        InputMethodManager teclado = getSystemService(InputMethodManager.class);
        if (teclado != null) teclado.hideSoftInputFromWindow(caixa.getWindowToken(), 0);
        modoFoco(caixa, caixa == campoSenha);
    }
}
