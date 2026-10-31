# Documentação — StarkTech Media Player

> Catálogo do `docs/`. Ler primeiro e abrir só o arquivo da tarefa. Padrão de pastas na skill `starktech-base`.
> Nunca gravar senha, token, chave ou dado de acesso — só dizer onde está guardado.

| Pasta / arquivo | O que contém |
|---|---|
| `arquitetura/interface.md` | Telas novas no código: pacotes, regra TV × tablet, medidas, recursos, navegação e rotas, dados simulados e o que ligar na F3 |
| `arquitetura/licencas.md` | Licença de cada pasta do Nova e de cada nativo do APK; obrigações para distribuir |
| `backlog.md` | Pendências fora do plano da fase em curso, separadas pela fase em que entram (F2, F3 e F4) |
| `imagens/f0/tablet.png` | F0 — tela inicial do APK debug sem alteração no emulador de tablet (Tablet_API37, 2560×1600) |
| `imagens/f0/tv.png` | F0 — tela inicial do APK debug sem alteração no emulador de TV (TV_1080p_API36, 1920×1080) |
| `imagens/f1/a-tablet-inicio.png` | F1 fase A — tela inicial no tablet com pacote, nome e idioma novos (ainda a tela do Nova), em pt-BR com o sistema em inglês |
| `imagens/f1/a-tv-inicio.png` | F1 fase A — tela inicial na TV com pacote, nome e idioma novos (ainda a tela do Nova), em pt-BR com o sistema em inglês |
| `imagens/f1/b-tablet-launcher.png` | F1 fase B — gaveta de apps do tablet com o ícone provisório (triângulo de play azul sobre `#020617`) |
| `imagens/f1/b-tv-launcher.png` | F1 fase B — tela inicial da TV com o banner provisório (ícone + "StarkTech Media Player" em Inter) |
| `imagens/f1/c-tablet-navegacao-hd.png` | F1 fase C — tela principal nova do tablet: barra lateral com logo (Início ativo), marca e selo verde "HD externo"; conteúdo em construção |
| `imagens/f1/c-tablet-navegacao-sem-hd.png` | F1 fase C — a mesma tela com o selo vermelho "Sem HD ✕" (alternado por toque longo, só no APK debug) |
| `imagens/f1/c-tv-navegacao.png` | F1 fase C — tela principal nova da TV: barra do topo com a pílula Filmes ativa e o foco (borda branca) no conteúdo, em construção |
| `imagens/f1/d-tablet-filmes.png` | F1 fase D — aba Filmes do tablet: título e grade de capas 2:3 (5 colunas, 8 filmes, selo "Assistido") |
| `imagens/f1/d-tablet-inicio-pagina-2.png` | F1 fase D — Início do tablet depois de arrastar "Adicionados recentemente": página 2 de 3 (séries, filmes e show) e o ponto do meio ativo |
| `imagens/f1/d-tablet-inicio.png` | F1 fase D — Início do tablet: marca, selo "HD externo", Continuar assistindo (progresso e "Cap. 4 de 8") e Adicionados recentemente, página 1 de 3 |
| `imagens/f1/d-tv-filmes.png` | F1 fase D — aba Filmes da TV: grade de capas 2:3 (7 colunas) com o foco (borda branca + 6 %) na primeira capa |
| `imagens/f1/d-tv-inicio-fileiras.png` | F1 fase D — Início da TV rolado com o foco em Favoritos: fileiras Favoritos e Filmes adicionados recentemente e o rodapé com logo, versão e site |
| `imagens/f1/d-tv-inicio-slide-2.png` | F1 fase D — Início da TV 10 s depois, com o foco no menu: destaque no slide 2 (Spring, "Remover dos favoritos") |
| `imagens/f1/d-tv-inicio.png` | F1 fase D — Início da TV como no desenho: destaque "Novidades na biblioteca" (slide 1, foco em Assistir), Continuar assistindo e o começo de Favoritos |
| `imagens/f1/d-tv-series.png` | F1 fase D — aba Séries da TV: 3 capas de exemplo, foco na primeira |
| `imagens/f1/d-tv-shows.png` | F1 fase D — aba Shows da TV: 4 capas de exemplo, foco na primeira |
| `imagens/f1/fim-tablet-adicionar-pasta.png` | F1 final — tablet: Pastas com "Escolher pasta" aberto (HD externo e Armazenamento interno) |
| `imagens/f1/fim-tablet-ajustes.png` | F1 final — tablet: aba Ajustes (Configurações), com Informações de uso e Sobre |
| `imagens/f1/fim-tablet-audio.png` | F1 final — tablet: player com a escolha de Áudio aberta |
| `imagens/f1/fim-tablet-busca-tmdb.png` | F1 final — tablet: Buscar no TMDB sobre o Editar, com o resultado "Atual" |
| `imagens/f1/fim-tablet-detalhe.png` | F1 final — tablet: Detalhe de Tears of Steel, aba Capítulos |
| `imagens/f1/fim-tablet-editar.png` | F1 final — tablet: Editar com a capa e o fundo em destaque, outras opções e "Do aparelho" |
| `imagens/f1/fim-tablet-filmes.png` | F1 final — tablet: aba Filmes |
| `imagens/f1/fim-tablet-informacoes-uso.png` | F1 final — tablet: Informações de uso (cartões com o uso por toque) |
| `imagens/f1/fim-tablet-inicio.png` | F1 final — tablet: Início com a marca nova (ícone + StarkTech + Media Player), sem barra de status e sem logo na barra lateral |
| `imagens/f1/fim-tablet-menu-capa.png` | F1 final — tablet: Menu da capa (tocar e segurar) sobre a aba Filmes |
| `imagens/f1/fim-tablet-nao-identificados.png` | F1 final — tablet: Não identificados |
| `imagens/f1/fim-tablet-pastas.png` | F1 final — tablet: aba Pastas ("Conectado", tipos por pasta e Como organizar) |
| `imagens/f1/fim-tablet-player-capitulos.png` | F1 final — tablet: player com a faixa de capítulos aberta |
| `imagens/f1/fim-tablet-player.png` | F1 final — tablet: player com os controles (sem os botões −10 s / +10 s) |
| `imagens/f1/fim-tablet-serie.png` | F1 final — tablet: Série (Código Aurora), temporada e episódios |
| `imagens/f1/fim-tablet-series.png` | F1 final — tablet: aba Séries |
| `imagens/f1/fim-tablet-shows.png` | F1 final — tablet: aba Shows |
| `imagens/f1/fim-tablet-sobre.png` | F1 final — tablet: Sobre (ícone, versão, créditos, avisos de terceiros e TMDB) |
| `imagens/f1/fim-tv-adicionar-pasta.png` | F1 final — TV: Pastas com "Escolher pasta" aberto |
| `imagens/f1/fim-tv-audio.png` | F1 final — TV: player com a escolha de Áudio aberta |
| `imagens/f1/fim-tv-busca-tmdb.png` | F1 final — TV: Buscar no TMDB sobre o Editar |
| `imagens/f1/fim-tv-busca.png` | F1 final — TV: Buscar, com o teclado da TV aberto |
| `imagens/f1/fim-tv-configuracoes.png` | F1 final — TV: Configurações |
| `imagens/f1/fim-tv-detalhe.png` | F1 final — TV: Detalhe de Tears of Steel, com os capítulos |
| `imagens/f1/fim-tv-editar.png` | F1 final — TV: Editar com a capa e o fundo em destaque, outras opções e "Do HD" |
| `imagens/f1/fim-tv-filmes.png` | F1 final — TV: aba Filmes em ordem alfabética |
| `imagens/f1/fim-tv-informacoes-uso.png` | F1 final — TV: Informações de uso (cartões com o uso pelo controle) |
| `imagens/f1/fim-tv-inicio.png` | F1 final — TV: Início com a marca nova (ícone + StarkTech + Media Player) |
| `imagens/f1/fim-tv-menu-capa.png` | F1 final — TV: Menu da capa (segurar OK) sobre a aba Filmes |
| `imagens/f1/fim-tv-nao-identificados.png` | F1 final — TV: Não identificados |
| `imagens/f1/fim-tv-pastas.png` | F1 final — TV: aba Pastas |
| `imagens/f1/fim-tv-player-capitulos.png` | F1 final — TV: player com a lista de capítulos aberta (Agora, vistos ✓) |
| `imagens/f1/fim-tv-player.png` | F1 final — TV: player com os controles |
| `imagens/f1/fim-tv-serie.png` | F1 final — TV: Série (Código Aurora), temporada e episódios |
| `imagens/f1/fim-tv-series.png` | F1 final — TV: aba Séries em ordem alfabética |
| `imagens/f1/fim-tv-shows.png` | F1 final — TV: aba Shows em ordem alfabética |
| `imagens/f1/fim-tv-sobre.png` | F1 final — TV: Sobre |
| `paginas/INTERFACE.md` | F1 — telas aprovadas (TV e tablet), comportamentos, regras e identidade visual |
| `paginas/telas-f1/` | Código HTML de cada tela do Claude Design (referência visual exata: cores, tamanhos, textos) |
| `operacional/build.md` | Ambiente (WSL, JDK, SDK, NDK), comando de build, saída do APK e teste no aparelho |
| `operacional/nova-manifest-v6.6.2.xml` | Manifesto da release v6.6.2 do Nova, com os commits fixos (cópia sem alteração) |
| `operacional/origem-nova.md` | Registro de origem: repositório, ramo e commit de cada pasta; roteiro para atualizar do Nova |

Criou, moveu ou apagou arquivo no `docs/`: atualizar este catálogo na mesma tarefa.
