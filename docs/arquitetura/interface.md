# Interface — StarkTech Media Player

> Como as telas novas da F1 estão montadas no código. Telas e comportamentos aprovados: `docs/paginas/INTERFACE.md`; referência visual: `docs/paginas/telas-f1/*.html`. Estado: F1 fase C (base, navegação e dados simulados), 30/10/2026.

## 1. Pacotes

Código novo em `Video/src/main/java/br/com/starktechstudio/mediaplayer/` (o `R` continua `com.archos.mediacenter.video.R`, `Video/build.gradle:101`).

| Pacote | O que tem |
|---|---|
| `dados` | `Modelo` (Titulo, Capitulo, Temporada, Episodio, Pasta, ResultadoTmdb, Faixa, PessoaElenco, DadosTecnicos e os enums), `RepositorioBiblioteca` (interface), `RepositorioSimulado`, `EstadoSimulado` |
| `comum` | `Aparelho` (TV ou tablet), `Navegacao` (abas e rotas), `FocoTv`, `Marca`, `BordasSistema`, `EmConstrucaoActivity`, `EmConstrucaoFragment` e, nas fases seguintes, as telas iguais nos dois aparelhos |
| `tv` | `PrincipalTvActivity` e as telas só da TV |
| `tablet` | `PrincipalTabletActivity` e as telas só do tablet |

Arquivos novos com "Copyright 2026 StarkTech Studio. Todos os direitos reservados."; arquivos do Nova alterados mantêm o Apache-2.0 e ganham "Modificado por StarkTech Studio em 2026".

## 2. TV × tablet

| Regra | Detalhe |
|---|---|
| Critério | `Aparelho.ehTv` = `FEATURE_LEANBACK` (D3), sem a escolha manual do Nova (`UiChoiceDialog`) |
| Classes | Separadas onde o comportamento muda (Principal, Início, Detalhe, Série, Player); comuns onde é igual (Editar, Buscar no TMDB, Pastas, Menu da capa, Configurações, Sobre) (D2) |
| Layouts | `st_tv_*` / `st_tablet_*`, escolhidos no código; layout comum (`st_em_construcao`) ajusta medidas no código |
| Tema | `Theme.StarkTech.Tv` / `Theme.StarkTech.Tablet` (`res/values/st_estilos.xml`); tela comum chama `setTheme(Aparelho.tema(this))` antes do `super.onCreate` |
| Classe base | `AppCompatActivity` (fonte Inter de `res/font` na API 23–25) |
| Celular | Layout do tablet, deitado (D23): `PrincipalTabletActivity` com `sensorLandscape` (tablets com API 36 ignoram a trava) |
| Barras do sistema | Com targetSdk 36 o app desenha sob a barra de status e a de navegação (edge-to-edge obrigatório). `BordasSistema.aplicar` soma a área das barras ao padding das views escolhidas (no tablet, a barra lateral e a coluna de conteúdo; o fundo vai até a borda). Na TV não há barras |
| Foco (TV) | `FocoTv.aplicar` (borda branca 2 dp, `st_foco_tv`) e `aplicarComEscala` (mais 6%, capas); sem o realce padrão do sistema |

## 3. Medidas

Emuladores com 320 dpi: TV 1920×1080 = 960×540 dp; tablet 2560×1600 = 1280×800 dp.

| Aparelho | Conversão | Exemplo |
|---|---|---|
| TV | px do HTML ÷ 2 = dp/sp | margens 80 × 44 px → 40 × 22 dp; capa 196×294 → 98×147 dp |
| Tablet | px do HTML = dp/sp | barra lateral 112 dp; capa 196×294 dp |

Medidas comuns em `res/values/st_dimens.xml`; cantos e bordas que mudam entre TV e tablet vêm dos atributos do tema (`stCantoBotao`, `stCantoCartao`, `stCantoSelo`, `stBorda`, `stBordaCampo`), usados pelos fundos `res/drawable/st_*.xml` — por isso esses fundos só funcionam com contexto de tema StarkTech.

## 4. Recursos

| Tipo | Padrão |
|---|---|
| Prefixo | `st_` em tudo que é novo em `Video/res/` |
| Textos | `res/values/st_textos_<fase>.xml`, já em pt-BR, com `tools:locale="pt-BR"` (D4) |
| Ícones | `res/drawable/st_ic_*.xml`: SVG do Lucide (ISC, D6) convertidos à mão em VectorDrawable 24×24, traço 2, branco; a cor vem do `app:tint` de quem usa. Círculos e retângulos viram `path`; comandos sempre explícitos (sem `lineto` implícito depois de `M`) |
| Logo StarkTech | `res/drawable-nodpi/st_logo.png` (256 px): só no topo da barra lateral do tablet e no rodapé da TV (D5) |

## 5. Navegação e rotas

| Ponto | Como está |
|---|---|
| Entrada | Launcher (`LAUNCHER` e `LEANBACK_LAUNCHER`, ambos no alias `…browser.BrowserActivityVideo`) → `EntryActivity` → `Navegacao.telaPrincipal` → `PrincipalTvActivity` ou `PrincipalTabletActivity`. O `MainActivityLeanback` perdeu o `intent-filter`; os atalhos do launcher do Nova saíram (D21). Telas do Nova continuam no código, sem entrada |
| Abas | `Navegacao.Aba`: Início, Filmes, Séries, Shows, Pastas (TV: pílulas do topo) + Ajustes (tablet: barra lateral). Conteúdo por `Navegacao.conteudoDaAba` |
| Rotas | `Navegacao.abrirBusca`, `abrirConfiguracoes`, `abrirTitulo`, `abrirReproducao`, `abrirEditar`, `abrirNaoIdentificados`, `abrirSobre`. Tela que ainda não existe abre `EmConstrucaoActivity` com o nome da tela |
| Teclas (TV) | ← / → entre as pílulas (a pílula em foco vira a aba ativa); ↓ do topo desce ao primeiro item do conteúdo; ↑ no alto do conteúdo e Voltar no conteúdo voltam à pílula ativa; Voltar no topo sai do app |
| Tablet | Toque nos itens da barra lateral; marca e selo do HD só na aba Início (nas outras o desenho começa pelo título da tela) |

Estado ao fim da fase C: todas as abas e rotas em construção; cada fase seguinte troca a sua rota em `Navegacao`.

## 6. Dados simulados

| Peça | Papel |
|---|---|
| `RepositorioBiblioteca` | Única porta de dados das telas: listas (destaques, continuar assistindo, favoritos, adicionados recentemente, abas, busca), detalhe (capítulos, temporadas, dados técnicos, elenco, faixas), Editar (opções de capa e fundo, resultados do TMDB), pastas, não identificados, HD; ações (favorito, assistido, posição, salvar edição, restaurar original, escolher resultado, capa, fundo, tipo da pasta). `RepositorioBiblioteca.atual(contexto)` devolve a implementação em uso |
| `RepositorioSimulado` | Java puro, sem `ContentResolver`, `VideoStore`, `ScraperStore` nem loaders. Os exemplos das 13 telas (8 filmes, capítulos, cores e números do desenho); textos entre colchetes viraram exemplos fictícios (D8): 3 séries ("Horizonte Azul" com 3 temporadas e 24 episódios), 4 shows, 5 pastas, 5 não identificados, resultados do TMDB. Tears of Steel, Sintel e os demais filmes têm 8 capítulos; Cosmos Laundromat e Big Buck Bunny não têm |
| `EstadoSimulado` | Estado das ações em arquivo próprio (`getSharedPreferences("st_simulado")`): favorito, assistido, posição, campos editados à mão, capa, fundo, resultado do TMDB, tipo da pasta, HD. Não abre as preferências padrão nem o banco do Nova |
| Regras | Escolher resultado do TMDB troca título, sinopse, capa e fundo, sem tocar nos campos editados à mão; Restaurar original apaga edição, capa, fundo e resultado; progresso arredondado para cima (04:12 de 12:14 = 35%, como no desenho) |
| HD (teste) | No APK debug, toque longo no selo do HD do tablet alterna conectado / desconectado (grava no `st_simulado`) |

Conferência em cada fase: `git diff --stat nova-v6.6.2 -- MediaLib FileCoreLibrary native` mostra só os arquivos da fase A. Na F4: `RepositorioSimulado` fora do APK de distribuição.

## 7. Ligação na F3

A F3 cria uma implementação de `RepositorioBiblioteca` ligada ao Nova e troca o `atual(contexto)`. Caminhos do Nova a partir de `Video/src/main/java/com/archos/mediacenter/video/`.

| Dado / ação | No Nova |
|---|---|
| Continuar assistindo | `browser/loader/WatchingUpNextLoader.java` ou `LastPlayedLoader.java` |
| Adicionados recentemente | `browser/loader/LastAddedLoader.java` (filtrar filmes na fileira da TV) |
| Filmes, Séries | `browser/loader/MoviesLoader.java`, `AllTvshowsLoader.java`; Shows não existe no Nova |
| Temporadas e episódios | `browser/loader/SeasonsLoader.java`, `EpisodesLoader.java`; o reconhecimento por pasta (`MediaLib/…/preprocess/TvShowPathMatcher.java`) não aceita "Temporada N / 01.mkv" |
| Favoritos | Coluna `Archos_favorite_track` (`MediaLib/…/video/VideoStore.java`) sem uso na interface do Nova, ou listas do Nova (`ListsLoader`); gravação não confirmada |
| Assistido, progresso | `browser/adapters/object/Video.java`; ações em `leanback/details/VideoActionAdapter.java` |
| Dados técnicos, legendas, elenco | `leanback/details/FileDetailsRow.java`, `SubtitlesDetailsRow.java`, `CastRow.java` |
| Buscar no TMDB, capa e fundo | `leanback/scrapping/ManualVideoScrappingSearchFragment.java`, `info/VideoInfoPosterChooserFragment.java`, `VideoInfoBackdropChooserFragment.java` |
| Pastas, HD, Ignorar | `FileCoreLibrary/…/ExtStorageManager.java`; lista negra `MediaLib/…/video/Blacklist.java`. Tipo por pasta: não existe (F2) |
| Não identificados | `browser/loader/NonScrapedVideosLoader.java` |
| Atualizar biblioteca | `NetworkAutoRefresh.forceRescan` |
| Capítulos, edição livre, tipo por pasta | Não chegam ao Java: F2 |
| Player | `player/PlayerActivity.java`, `PlayerController.java` (a F1 não toca) |
