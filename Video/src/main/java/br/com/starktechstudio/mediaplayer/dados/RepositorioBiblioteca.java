// Copyright 2026 StarkTech Studio. Todos os direitos reservados.

package br.com.starktechstudio.mediaplayer.dados;

import android.content.Context;

import androidx.annotation.Nullable;

import java.util.List;
import java.util.Map;

import br.com.starktechstudio.mediaplayer.dados.Modelo.Campo;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Capitulo;
import br.com.starktechstudio.mediaplayer.dados.Modelo.DadosTecnicos;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Episodio;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Faixa;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Origem;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Pasta;
import br.com.starktechstudio.mediaplayer.dados.Modelo.PreviaTmdb;
import br.com.starktechstudio.mediaplayer.dados.Modelo.PessoaElenco;
import br.com.starktechstudio.mediaplayer.dados.Modelo.ResultadoTmdb;
import br.com.starktechstudio.mediaplayer.dados.Modelo.SecaoShows;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Temporada;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Tipo;
import br.com.starktechstudio.mediaplayer.dados.Modelo.TipoFaixa;
import br.com.starktechstudio.mediaplayer.dados.Modelo.TipoPasta;
import br.com.starktechstudio.mediaplayer.dados.Modelo.Titulo;
import br.com.starktechstudio.mediaplayer.motor.Capitulos;

/**
 * Única porta de dados das telas novas. As telas nunca falam com o banco do Nova.
 * F3: {@link RepositorioReal} — pastas do aparelho, motor e TMDB (docs/arquitetura/motor.md).
 * As leituras vêm da memória e servem na thread da tela; os métodos marcados "rede" não.
 */
public interface RepositorioBiblioteca {

    /** Implementação em uso. */
    static RepositorioBiblioteca atual(Context contexto) {
        return RepositorioReal.instancia(contexto);
    }

    // ---- Avisos ----

    /** {@code ouvinte} roda na thread da tela quando a biblioteca muda (varredura, TMDB, HD). */
    void ouvir(Runnable ouvinte);

    void deixarDeOuvir(Runnable ouvinte);

    /** Varredura ou busca no TMDB em andamento. */
    boolean atualizando();

    /** Quanto da atualização em andamento já foi (0 a 100); -1 = biblioteca em dia. */
    int progresso();

    /** {@code ouvinte} roda na thread da tela quando o progresso muda (só o indicador; as telas não releem). */
    void ouvirAndamento(Runnable ouvinte);

    void deixarDeOuvirAndamento(Runnable ouvinte);

    // ---- Biblioteca (Início e abas) ----

    /** Slides de "Novidades na biblioteca" (destaque da TV). */
    List<Titulo> destaques();

    /** Filmes, shows e episódios em andamento, do último visto ao mais antigo. */
    List<Titulo> continuarAssistindo();

    List<Titulo> favoritos();

    /** Fileira da TV: só filmes. */
    List<Titulo> filmesAdicionadosRecentemente();

    /** Tablet: filmes, séries e shows, em páginas de 5. */
    List<Titulo> adicionadosRecentemente();

    /** Abas Filmes, Séries e Shows, em ordem alfabética. */
    List<Titulo> titulos(Tipo tipo);

    /** Busca da TV (D11). */
    List<Titulo> buscar(String consulta);

    /**
     * Título pelo id; null se não existir (episódio: {@link #episodio}). Vale também para a capa de
     * coletânea e de artista: {@link #opcoesCapa}, {@link #escolherCapa}, {@link #escolherFundo},
     * {@link #salvarEdicao} e {@link #restaurarOriginal} servem para trocar a capa delas no Editar.
     */
    Titulo titulo(String id);

    /** O título sem a edição à mão (prévia do Restaurar original no Editar); null se não existir. */
    Titulo tituloOriginal(String id);

    /** A biblioteca não tem nenhum título (para o aviso "Adicione uma pasta"). */
    boolean vazia();

    // ---- Coletânea (D5): o Início segue filme a filme ----

    /**
     * Aba Filmes, em ordem alfabética: os filmes soltos e, no lugar de 2 ou mais filmes da mesma
     * coletânea, a capa dela ({@link Titulo#ehColetanea()}, com {@link Titulo#numeroFilmes}).
     */
    List<Titulo> filmesEColetaneas();

    /** Os filmes da coletânea, por ano. Vazio se ela não existe ou ficou com menos de 2. */
    List<Titulo> filmesDaColetanea(String idColetanea);

    /** Nomes das coletâneas da biblioteca, mesmo com um filme só (sugestões do ajuste à mão). */
    List<String> nomesDeColetanea();

    /**
     * Ajuste à mão: {@code nome} põe o filme nessa coletânea; "" tira de coletânea; null volta ao que
     * o TMDB diz. Restaurar original também desfaz. Para saber a atual: {@link Titulo#colecaoNome}.
     */
    void definirColetanea(String idFilme, String nome);

    // ---- Shows por artista e por grupo (D7) ----

    /**
     * Aba Shows, em ordem alfabética, com "Sem artista" por último: capas de artista
     * ({@link Titulo#ehArtista()}, com {@link Titulo#numeroShows}); sem imagem, o bloco de cor.
     */
    List<Titulo> artistas();

    /** Os shows do artista, em ordem alfabética, sem repetir o nome dele no título. */
    List<Titulo> showsDoArtista(String idArtista);

    /**
     * Aba Shows com "por grupo" ligado ({@code Preferencias.showsPorGrupo}): uma seção por subpasta de
     * Shows, em ordem alfabética, com "Sem grupo" por último.
     */
    List<SecaoShows> gruposDeShows();

    // ---- Detalhe, série e player ----

    /** Capítulos do arquivo (título ou episódio); lista vazia = sem capítulos (só −10 s / +10 s). */
    List<Capitulo> capitulos(String id);

    List<Temporada> temporadas(String idSerie);

    Episodio episodio(String idEpisodio);

    DadosTecnicos dadosTecnicos(String idTitulo);

    List<PessoaElenco> elenco(String idTitulo);

    /** Faixas lidas do arquivo; a escolha no player usa as do próprio player. */
    List<Faixa> faixas(String id, TipoFaixa tipo);

    /**
     * Faixa escolhida à mão para o vídeo; sem ela, a última escolhida na série ({@code idSerie}, nulo
     * fora de série). Nula = as preferências.
     */
    @Nullable
    String faixaEscolhida(String id, @Nullable String idSerie, TipoFaixa tipo);

    /** Guarda a escolha para a próxima vez, no vídeo e na série dele. */
    void guardarFaixa(String id, @Nullable String idSerie, TipoFaixa tipo, String faixa);

    /**
     * Vídeo aberto na tela que a varredura ainda não leu, ou cuja leitura falhou: lê agora (capítulos,
     * duração, faixas), fora da fila e fora da tela; avisa os ouvintes quando acabar.
     */
    void lerDadosDoArquivo(String id);

    /** Capítulos que o player achou ao abrir um vídeo que ficou sem eles na biblioteca. */
    void guardarCapitulos(String id, Capitulos capitulos);

    /** Duração que o player achou ao abrir um vídeo que ficou sem ela na biblioteca. */
    void guardarDuracao(String id, long duracaoMs);

    // ---- Editar e Buscar no TMDB ----

    /** Opções de capa: referências {@code arquivo:}, {@code url:} (TMDB) ou {@code cor:}. */
    List<String> opcoesCapa(String idTitulo);

    List<String> opcoesFundo(String idTitulo);

    /**
     * Rede. Capa de artista no Editar: as fotos do Deezer para escolher, em referências {@code url:};
     * sem nada gravado (quem grava é {@link #escolherCapa}). Vazia = não há; nula = sem resposta.
     */
    @Nullable
    List<String> fotosDoArtista(String idArtista);

    /**
     * Rede. "Buscar artista" no Editar: os artistas da biblioteca com esse nome, depois os do Deezer.
     * {@code ResultadoTmdb.id} é o nome e {@code sinopse}, a linha de apoio.
     */
    List<ResultadoTmdb> buscarArtista(String idArtista, String consulta);

    /**
     * Rede (com {@code foto}). Os shows de {@code idArtista} passam para o artista {@code nome}: com o
     * mesmo nome de outra pasta, as duas viram uma só. Restaurar original do artista desfaz.
     * {@code foto}: endereço da capa dele; nula = fica a que ele tem ou a que a busca achar.
     */
    void definirArtista(String idArtista, String nome, @Nullable String foto);

    /** Rede, fora da tela. Página do artista sem texto: procura uma vez por abertura do app; avisa os ouvintes. */
    void lerDadosDoArtista(String idArtista);

    /** Há chave do TMDB no APK (sem ela, a busca avisa em vez de buscar). */
    boolean temChaveTmdb();

    /** Rede. Resultados da pesquisa; o ligado ao título vem com {@link ResultadoTmdb#atual}. */
    List<ResultadoTmdb> buscarTmdb(String idTitulo, String consulta);

    /**
     * Rede. Como o título fica com o resultado escolhido, sem gravar nada (prévia do Editar: o Salvar
     * grava, fechar descarta); nulo se não deu para buscar.
     */
    PreviaTmdb previaResultadoTmdb(String idTitulo, String idResultado);

    /** Rede. Troca título, sinopse, capa e fundo pelo resultado, sem tocar nos campos editados à mão. */
    boolean selecionarResultadoTmdb(String idTitulo, String idResultado);

    /** Grava os campos da tela Editar; os que mudaram ficam marcados como "editados à mão". */
    void salvarEdicao(String idTitulo, Map<Campo, String> valores);

    /** Apaga a edição à mão, a escolha do TMDB, da capa e do fundo. */
    void restaurarOriginal(String idTitulo);

    /**
     * Rede (com {@code url:}). Capa escolhida no Editar: {@code arquivo:} (aparelho), {@code url:}
     * (opção do TMDB, baixada aqui) ou {@code cor:}. Fica "editada à mão". false = não baixou.
     */
    boolean escolherCapa(String idTitulo, String referencia);

    boolean escolherFundo(String idTitulo, String referencia);

    // ---- Pastas ----

    List<Pasta> pastas();

    /** Vídeos sem tipo ou fora do padrão; cada um abre Editar. */
    List<Titulo> naoIdentificados();

    /** Situação do HD externo (selo do tablet). */
    boolean hdConectado();

    /** O app pode ler os vídeos do aparelho (permissão dada). */
    boolean podeLerVideos();

    // ---- Ações ----

    /** Título (série: a série inteira). */
    void definirFavorito(String idTitulo, boolean favorito);

    /** Título, série (todos os episódios) ou episódio. */
    void definirAssistido(String id, boolean assistido);

    /** Título ou episódio. */
    void definirPosicao(String id, int segundos);

    /**
     * Nota do filme, para as estrelas do Detalhe (D6): de 0 a 5; null tira a nota. Só em filme
     * assistido ({@link Titulo#podeDarNota()}); false = não gravou. Para ler: {@link Titulo#nota}, que
     * vem nula se o filme deixou de estar assistido (a nota fica guardada e volta com ele).
     */
    boolean definirNota(String idFilme, Integer nota);

    void definirTipoPasta(String idPasta, TipoPasta tipo);

    /** Adicionar pasta (D15): {@code caminho} de verdade; nome igual a Filmes, Séries ou Shows já vem com o tipo. */
    void adicionarPasta(Origem origem, String caminho);

    /** Tira a pasta da biblioteca (os vídeos dela saem; os arquivos ficam onde estão). */
    void removerPasta(String idPasta);

    /** Varre as pastas e busca no TMDB o que falta, fora da tela; avisa os ouvintes a cada passo. */
    void atualizarBiblioteca();

    /**
     * Atualizar biblioteca, pedido na tela: procura os vídeos novos nas pastas e busca no TMDB o que
     * falta, tentando de novo o que não achou ou veio sem capa; no fim, mostra o resultado.
     */
    void atualizarAgora();

    /** "12 atualizados · 2 não identificados", da última atualização; nulo antes da primeira. */
    String resultadoDaAtualizacao();

    /**
     * Ao voltar ao app: fora da tela, atualiza a biblioteca se o HD mudou ou se já faz um tempo. Com
     * "Atualizar ao abrir" desligado, só acompanha o HD, sem procurar nada.
     */
    void conferirBiblioteca();
}
