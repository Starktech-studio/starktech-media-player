// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.dados;

import android.content.Context;

import java.util.List;
import java.util.Map;

import br.com.starktechstudio.mediaplayer.dados.Modelo.Campo;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Capitulo;
import br.com.starktechstudio.mediaplayer.dados.Modelo.DadosTecnicos;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Episodio;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Faixa;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Pasta;
import br.com.starktechstudio.mediaplayer.dados.Modelo.PessoaElenco;
import br.com.starktechstudio.mediaplayer.dados.Modelo.ResultadoTmdb;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Temporada;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Tipo;
import br.com.starktechstudio.mediaplayer.dados.Modelo.TipoFaixa;
import br.com.starktechstudio.mediaplayer.dados.Modelo.TipoPasta;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Titulo;

/**
 * Única porta de dados das telas novas. As telas nunca falam com o banco do Nova.
 * F1: {@link RepositorioSimulado}. F3: implementação ligada aos loaders do Nova
 * (docs/arquitetura/interface.md §5).
 */
public interface RepositorioBiblioteca {

    /** Implementação em uso. */
    static RepositorioBiblioteca atual(Context contexto) {
        return RepositorioSimulado.instancia(contexto);
    }

    // ---- Biblioteca (Início e abas) ----

    /** Slides de "Novidades na biblioteca" (destaque da TV). */
    List<Titulo> destaques();

    List<Titulo> continuarAssistindo();

    List<Titulo> favoritos();

    /** Fileira da TV: só filmes. */
    List<Titulo> filmesAdicionadosRecentemente();

    /** Tablet: filmes, séries e shows, em páginas de 5. */
    List<Titulo> adicionadosRecentemente();

    /** Abas Filmes, Séries e Shows. */
    List<Titulo> titulos(Tipo tipo);

    /** Busca da TV (D11). */
    List<Titulo> buscar(String consulta);

    /** Título pelo id; null se não existir. */
    Titulo titulo(String id);

    // ---- Detalhe, série e player ----

    /** Capítulos do arquivo; lista vazia = sem capítulos (só −10 s / +10 s). */
    List<Capitulo> capitulos(String idTitulo);

    List<Temporada> temporadas(String idSerie);

    Episodio episodio(String idEpisodio);

    DadosTecnicos dadosTecnicos(String idTitulo);

    List<PessoaElenco> elenco(String idTitulo);

    List<Faixa> faixas(String idTitulo, TipoFaixa tipo);

    // ---- Editar e Buscar no TMDB ----

    /** Opções de capa (cores); a escolhida está em {@link Titulo#corCapa}. */
    List<Integer> opcoesCapa(String idTitulo);

    /** Opções de fundo (cores); a escolhida está em {@link Titulo#corFundo}. */
    List<Integer> opcoesFundo(String idTitulo);

    List<ResultadoTmdb> buscarTmdb(String idTitulo, String consulta);

    // ---- Pastas ----

    List<Pasta> pastas();

    /** Vídeos sem tipo ou fora do padrão; cada um abre Editar. */
    List<Titulo> naoIdentificados();

    /** Situação do HD externo (selo do tablet). */
    boolean hdConectado();

    // ---- Ações ----

    void definirFavorito(String idTitulo, boolean favorito);

    /** Título ou episódio. */
    void definirAssistido(String id, boolean assistido);

    /** Título ou episódio. */
    void definirPosicao(String id, int segundos);

    /** Grava os campos da tela Editar; os que mudaram ficam marcados como "editados à mão". */
    void salvarEdicao(String idTitulo, Map<Campo, String> valores);

    /** Apaga a edição à mão, a escolha do TMDB, da capa e do fundo. */
    void restaurarOriginal(String idTitulo);

    /** Troca título, sinopse, capa e fundo pelo resultado, sem tocar nos campos editados à mão. */
    void selecionarResultadoTmdb(String idTitulo, String idResultado);

    void escolherCapa(String idTitulo, int cor);

    void escolherFundo(String idTitulo, int cor);

    void definirTipoPasta(String idPasta, TipoPasta tipo);

    /** Só para teste no APK debug (selo do HD). */
    void definirHdConectado(boolean conectado);

    /** Atualizar biblioteca: na F1 só o aviso na tela. */
    void atualizarBiblioteca();
}
