# INTERFACE — StarkTech Media Player

> Telas e comportamentos aprovados na F1 (30/10/2026). Fonte visual: Claude Design "StarkTech Media Player — Telas F1" (https://claude.ai/artifact/KS8n6K5Dg7W5UXgfbhKh9A). Cópia do código de cada tela em `telas-f1/` (HTML com estilos inline: cores, tamanhos e textos exatos).
> Filmes, capítulos e números das telas são exemplo. Capas aparecem como blocos de cor: no app são as do TMDB.
> Ajustes da TV (plano 004, 04/11/2026) já entram aqui: onde o HTML de `telas-f1/` for diferente (Novidades, player, Editar), vale este texto.

## 1. Identidade

| Item | Valor |
|---|---|
| Superfície | Escura — fundo `#020617`, cartão `#0f172a`, borda `#1e293b` |
| Cores | Azul `#3b82f6` (destaque), azul botão `#2563eb`, ciano `#06b6d4`, texto `#ffffff`, secundário `#cbd5e1`, apagado `#94a3b8` |
| Estados | Assistido `#10b981` · aviso `#f59e0b` · erro/sem HD `#ef4444` |
| Fonte | Inter (títulos 900, botões 800, texto 500–700) |
| Cantos | Botão 14 px · cartão 16–24 px · destaque 32 px |
| Ícones | Traço (estilo Lucide), nunca emoji |
| Idioma | Só pt-BR |
| Logo | Logo StarkTech no rodapé da TV e no topo da barra lateral do tablet |

## 2. Telas — TV (controle remoto, 1920×1080)

| Arquivo | Tela | Comportamento |
|---|---|---|
| `telas-f1/Main.dc.html` | Biblioteca | Abre com o foco na pílula Início e Novidades à vista. Menu no topo (Início em amarelo e as abas ligadas em Configurações, Navegação) + busca e configurações + relógio HH:mm no canto direito, também no Detalhe, na Série e em Coletânea e artista. Destaque "Novidades na biblioteca": slide que troca sozinho no tempo de Configurações (10 s de fábrica) com o foco fora dele; botões Assistir e Detalhes (1.1.1); ← no Assistir e → no Detalhes passam o item em laço, com transição, e entre os dois a seta leva ao outro; os pontos só indicam, sem foco. Foco em Assistir expande o item (área maior, com o fundo no lugar da capa) e toca a prévia em vídeo, sem som: a expansão com prévia é só em Novidades. O 1º ↓ recolhe e o 2º desce à fileira; segurar OK abre o Menu da capa. Fileiras: Continuar assistindo, Favoritos, Filmes adicionados recentemente (selo "Assistido"). Rodapé com logo, versão e site |
| `telas-f1/tv-detalhe.dc.html` | Detalhe do filme | Fundo (imagem) atrás. "Editar" no topo, no lugar da busca. Progresso + "Marcar como assistido". Botões: Continuar, Do início, Capítulos, Áudio e legendas, Favoritar. Filme assistido: nota de 0 a 5 estrelas ao lado de "Marcar como não assistido". Dados técnicos e fileira de capítulos |
| `telas-f1/tv-serie.dc.html` | Série | Temporadas e episódios montados pela pasta (`Nome / Temporada N / 01.mkv`); nome do episódio vem do TMDB; estado por episódio: assistido, em andamento, novo |
| — (sem desenho) | Coletânea | 2 ou mais filmes da mesma coleção do TMDB viram uma capa na aba Filmes, que abre a lista deles por ano, no padrão da Série. Filme sozinho fica solto; o Início segue filme a filme. Editar da capa: nome, capa e fundo |
| — (sem desenho) | Artista e grupos | Aba Shows lista artistas, com a foto e a contagem; a capa abre os shows dele, no padrão da Série. Sem " - " no nome do arquivo: "Sem artista", por último. "Por grupo" ligado em Configurações: seções por subpasta (`Shows/Nacionais`, `Shows/Internacionais`), com "Sem grupo" por último |
| `telas-f1/tv-player.dc.html` | Player | Lista de capítulos à direita (atual marcado, vistos com ✓). Barra com marcas dos capítulos: ↑ dos botões leva o foco a ela; ← / → movem 10 s e, segurando, 30 s depois de 2 s e 60 s depois de 5 s; o ponto anda na tela e a busca é ao soltar. Botões com nome: Cap. anterior, Pausar, Próx. capítulo (sem −10 s / +10 s: com os controles ocultos, as setas ← → pulam o tempo de Configurações, 10 s de fábrica). "Próximo capítulo em…" à esquerda. Áudio, Legendas, Zoom e Capítulos à direita. Voltar fecha a lista de capítulos; depois, recolhe os controles; com eles ocultos, o 1º mostra um aviso por 3 s e o 2º vai ao Detalhe ou, em episódio, à Série Legendas termina em "Carregar legenda": navegador do app a partir da pasta do vídeo (.srt, .ass, .ssa, .vtt), e a escolhida entra ligada. |
| `telas-f1/tv-editar.dc.html` | Editar | Capa e fundo escolhidos entre as opções do TMDB. Campos: título, título original, tipo (Filme/Série/Show), ano, duração, gêneros, sinopse. Na TV, o foco só realça a caixa; OK entra, seleciona o texto e abre o teclado. Filme: campo Coletânea, com Escolher, Tirar e Voltar ao TMDB. Buscar no TMDB · Restaurar original · Salvar |
| `telas-f1/tv-busca-tmdb.dc.html` | Buscar no TMDB | Lista de resultados para escolher o título certo; ao selecionar, campos editados à mão não são trocados |
| `telas-f1/tv-menu-capa.dc.html` | Menu da capa | Toque em OK na capa abre o Detalhe. Segurar OK abre o menu: Assistir, Marcar como assistido, Favoritos, Trocar capa, Trocar fundo, Editar |
| `telas-f1/tv-pastas.dc.html` | Pastas | Fora do menu: em Configurações, Biblioteca de mídia, Pastas da biblioteca. Pastas por origem (HD externo, interno). Tipo de cada pasta: os das abas ligadas em Navegação (Filmes, Séries, TV Shows, Desenhos, Shows, Stand-up) e Ignorar. Nomes iguais são reconhecidos sozinhos. "Não identificados" com Revisar. Adicionar pasta · Atualizar biblioteca |

Foco na TV: borda branca de 4 px no item em foco (capas também aumentam 6%).

## 3. Telas — tablet (toque, 1280×800 deitado)

| Arquivo | Tela | Comportamento |
|---|---|---|
| `telas-f1/tablet-biblioteca.dc.html` | Biblioteca | Barra lateral com ícones grandes, Início em amarelo. Relógio HH:mm no canto superior direito, em todas as abas. Sem busca e sem filtros. Ícone do HD no topo: verde "HD externo" conectado, vermelho "Sem HD ✕" desconectado; sem HD e com servidor em casa (2.0), o ícone do servidor com "Servidor" ou "Servidor fora". Continuar assistindo. Favoritos. Adicionados recentemente: 5 por vez, arrastar traz mais 5 (pontos de página) |
| `telas-f1/tablet-detalhe.dc.html` | Detalhe | Favoritar e Editar no topo (também na Série): o favorito vai para a fileira do Início. Capa e ações em cima, sempre inteiras: título em até 2 linhas com "…", sinopse com até 3 linhas, justificada, e "Ver mais" para o resto (os botões nunca descem); abas Capítulos, Dados técnicos, Elenco, com o conteúdo rolando sozinho embaixo. Tocar num capítulo abre o player no início dele. Filme assistido: nota no topo (tocar na estrela; "0" dá 0 estrelas; "Tirar nota") Puxar rola a página inteira: os capítulos sobem sobre a capa e o título, e dá para ver todos. |
| `telas-f1/tablet-player.dc.html` | Player | Título grande no topo. Controles embaixo, somem no tempo de Configurações (5 s de fábrica) junto com a barra do sistema (tela cheia; toque mostra de novo). Esquerda: Capítulos (abre faixa de capítulos que se arrasta para o lado, sem "Ver todos"), Áudio, Legendas. Centro: Cap. anterior, Pausar, Próx. capítulo. Direita: "Próximo: 02 · … em 00:28" e Expandir. Saindo com o vídeo tocando, segue em miniatura (picture-in-picture). Botões de mídia de fora do app (carro, fone Bluetooth): próxima / anterior trocam de capítulo; tocar / pausar valem Legendas termina em "Carregar legenda": navegador do app a partir da pasta do vídeo (.srt, .ass, .ssa, .vtt), e a escolhida entra ligada. |
| `telas-f1/tablet-editar.dc.html` | Editar | Igual à TV + "Do aparelho" para capa e fundo |
| `telas-f1/tablet-pastas.dc.html` | Pastas | Igual à TV, compacta |

Gestos do player no tablet (sem dica na tela): dois toques na lateral = pulo de Configurações (10 s de fábrica) · arrastar para os lados = avançar/voltar · arrastar para cima/baixo = volume.

Do plano 004, no tablet só o comum: listas de Coletânea, Artista e grupos, nota, Configurações e atualização. Novidades, prévia, barra com foco, Voltar duas vezes e botão Zoom são da TV.

## 4. Regras

| Regra | Detalhe |
|---|---|
| Botões de capítulo | Avançam e voltam capítulo, nunca o título (no episódio, quem troca de título são os botões de episódio, da 1.8). Arquivo sem capítulos: os botões somem e fica só Pausar no centro; filme e desenho ganham marcas de 10 em 10 minutos, com os mesmos botões e a mesma lista, só no player (o Detalhe não mostra) |
| Abas do menu | Configurações, Navegação: cada aba ligada ou desligada. De fábrica: Filmes, Séries, Desenhos e Shows; TV Shows e Stand-up, só se ligar, ou ao adicionar uma pasta com esse nome. Início e Ajustes ficam sempre. Desligar só tira do menu e dos tipos de pasta: os títulos continuam no Início e na busca |
| Músicas na 1.6 | Tocar tudo aleatório; o aleatório fica ligado até desligar (também nos vídeos de música); Vídeo anterior e Próximo vídeo no player e nos botões de fora (volante, fone, controle); Trocar foto (artista: opções e Do aparelho) e Trocar capa (álbum, playlist); álbum com Disco 1, Disco 2 |
| Músicas na 1.7 | Sub-aba Artistas em linhas (foto redonda, nome, álbuns · músicas · vídeos); sem a sub-aba Vídeos; página do artista com as abas Músicas (Ouvir, Aleatório) e Vídeos (Assistir, Aleatório, álbuns em vídeo e cards dos vídeos); Aleatório no player do vídeo de música, à esquerda de Áudio; Vídeo anterior em dois toques |
| Episódios no player (1.8) | Em episódio, Ep. anterior e Próx. episódio nas pontas do centro (com capítulos: Ep. anterior · Cap. anterior · Pausar · Próx. capítulo · Próx. episódio); o próximo só se houver, de temporada em temporada, pulando o episódio sem arquivo, com ou sem o automático. Anterior: no 1º episódio, volta ao começo; nos outros, um toque volta ao começo do episódio e dois toques (em até 3 s) vão ao anterior. Abre de onde parou (assistido, do começo). Teclas de mídia do controle, volante e fone: episódio |
| Desenhos (1.8) | Arquivo solto: filme, como antes. Pasta com o nome do desenho e episódios (`Nome / Temporada N / 01.mkv`, `Nome / 01.mkv` como Temporada 1, S01E01, 1x01, E01, "Parte 1"): um desenho só, na tela da Série com o rótulo "Desenho", na aba Desenhos (fora de Séries e TV Shows). Filmes de desenho da mesma coleção do TMDB: capa de coletânea na aba Desenhos, separada da de Filmes |
| Coletânea de desenhos como série (2.0) | Editar da coletânea de desenhos, "Mostrar como": Como filmes (o padrão, quando ninguém escolheu) ou Como série (episódios). Como série: a capa vira um desenho com episódios, na tela da Série (Temporada 1), na TV e no tablet, com o nome, a capa, o fundo e o favorito da coletânea; o player passa de episódio. Número do episódio: o do nome do arquivo (na frente, "01 - Nome", ou S01E02, 1x02, E02, Ep 2, Episódio 2, Parte 2), com o nome que vem depois dele; sem número, pela ordem alfabética dos nomes dos arquivos, depois dos numerados. A escolha fica guardada por coletânea; Restaurar original volta a Como filmes |
| Série, Coleção e artista (1.8) | Série: a descrição no lugar do aviso da pasta (TV, até 3 linhas com "…"; tablet, até 3 linhas e "Ver mais"); sem descrição, nada. Editar só com o ícone: na TV, na fileira de botões (Série: entre o principal e Favoritar; Coleção e artista: no fim, depois de Ver mais); no tablet, no canto direito do topo (Série, Coleção, artista e Detalhe) |
| Detalhes na TV (2.0) | Detalhe, Série e Coletânea e artista: topo só com a marca e o relógio, sem nav e sem nada com foco; ↑ do alto do conteúdo não sai dele; Voltar sai. Tablet como estava |
| Sinopse na TV (2.0) | Detalhe e Série: até 3 linhas com "…"; Coletânea e artista: até 4. Justificada (Android 8 em diante; no 6 e no 7, alinhada à esquerda) e até a margem direita do conteúdo, alinhada ao relógio. Selos, botões e o bloco Vídeo / Áudio / Legendas como estavam |
| Servidor em casa (2.0) | Pastas › Adicionar servidor: IP, usuário e senha e Entrar (diz se o servidor não responde, se o usuário ou a senha não valem ou se não abriu); depois, escolher o compartilhamento e a pasta e Adicionar esta pasta. Grupo "Servidor em casa" em Pastas: Conectado / Fora de alcance (ou Digite a senha de novo), Editar servidor e Remover servidor. Fora de alcance: os vídeos dele somem como os do HD e voltam quando ele volta; em Músicas, ficam só as baixadas. A senha fica cifrada no aparelho |
| Downloads de Músicas (2.0) | Só no tablet e no celular. Menu da música: Baixar ou Remover download. Álbum, artista e playlist: Baixar o que falta; enquanto baixa, "Baixando 3 de 12" e Cancelar downloads. Notificação com o progresso e Cancelar. Pasta `Music/StarkTech Media Player` (memória interna de fábrica, ou cartão / pendrive em Configurações › Músicas), com as mesmas pastas do servidor. Filtro "Só baixadas" no topo de Músicas. Configurações › Músicas: Onde baixar, Espaço das baixadas e Remover todas as baixadas |
| Origem (2.0) | Só com algum servidor cadastrado (ao alcance ou não); sem servidor, nem o ícone nem o selo, e as capas mudam ao cadastrar ou remover um. Capa de Músicas (álbum, playlist): ícone pequeno no canto, na cor da origem, sem texto. Detalhes (Detalhe, Série, Coletânea, álbum, artista e playlist): selo com nome, ícone e cor. Servidor: ciano, ícone de servidor. Baixada: verde, ícone de download. Aparelho: azul `#60A5FA`, ícone de HD. Coleção com origens diferentes: a da maioria. Capas e cartões de vídeo: nada |
| Tempo total em Músicas (2.0) | Álbum, álbum em vídeo e playlist: selo com o tempo de tudo o que têm, músicas e vídeos ("15 min"; de 60 min em diante, "1 h 15 min"), junto dos outros selos; a duração que não se sabe conta 0, e sem tempo nenhum o selo some. A linha de dados do álbum e da playlist deixou de repetir o tempo. A página do artista não tem |
| Ordem do nav (1.5.1) | Início sempre primeiro, em destaque; as outras abas ligadas em ordem alfabética; no tablet, Ajustes no fim |
| Vídeos em Músicas (1.5.1) | Vídeos na pasta de Músicas (`Artista / Vídeos / 01 - Música.mp4`; álbum em vídeo: `Artista / Ao Vivo (2019) / 01 - Música.mp4`), em sequência pelo número. Sub-aba Vídeos; página do artista com Álbuns e Vídeos, selos ("Músicas · MP3", "Vídeos · 1080p") e a descrição da Wikipédia; álbum com o selo e a descrição. Tocam no player de vídeo, um atrás do outro; fora do Início |
| Músicas (1.5) | Aba com a barra "Tocando agora" e as sub-abas Artistas, Álbuns e Playlists, em grade de capas quadradas (artista com a foto redonda). Pasta do tipo Músicas (`Artista / Álbum / 01 - Faixa.mp3`; MP3, M4A, AAC, FLAC, OGG, Opus, WAV). Capa: a da música ou a imagem da pasta. Álbum e playlist com Tocar e Aleatório; segurar uma faixa: Tocar a seguir, Pôr na fila, Adicionar à playlist. Toca fora do app e com a tela desligada, com notificação e os botões do fone e do controle. Desligada de fábrica; liga ao adicionar uma pasta de Músicas |
| Legenda rápida (1.5) | No player, botão Ajustar à esquerda quando o vídeo tem legenda: atraso de 0,1 em 0,1 s (guardado no título) e tamanho (vale para todos) |
| Limpar o cache (1.5) | Configurações > App: apaga imagens baixadas, cópias de legenda e o APK de atualização |
| TV Shows | Igual a Séries em tudo (`Nome / Temporada N / 01.mkv`, tela da série, TMDB de TV), em aba própria, pelo tipo da pasta |
| Situação da atualização | No alto das páginas, só o símbolo e o nome: "Atualizando 42%", "Atualizado" ou "Não atualizado" (TMDB sem resposta). O aviso ao terminar diz só se atualizou ou não. "Não identificados" fica só na caixa de Pastas, com Revisar |
| Tipo do conteúdo | Vem do tipo da pasta. Séries: `Nome / Temporada N / 01.mkv`. Fora do padrão: "Não identificados", corrige em Editar |
| Show | O TMDB não tem categoria de shows: dados à mão quando não encontrar |
| Capa | JPG, PNG ou WebP · vertical 2:3 · ideal 1000×1500 px |
| Fundo | JPG, PNG ou WebP · horizontal 16:9 · ideal 1920×1080 px |
| Nome | "Editar" (não "Editar dados") |
| Nota | Só em filme assistido: inteira, de 0 a 5; sem nota é diferente de 0 |
| Atualização | Configurações, seção App: Atualizar automaticamente, Versão instalada e Versão disponível, com o aviso de que está na última ou de que há versão nova, e o botão Verificar ou Baixar e instalar. Na TV (2.0), a Versão instalada sai da lista e vai para o painel da marca, e a linha Versão disponível inteira recebe o foco e o OK (o botão é só o rótulo), com o realce das outras linhas. Ao abrir o app, com o automático ligado, avisa a versão nova uma vez. |

## 5. Ainda sem desenho

- Coletânea e artista: como o Detalhe, com capa, nome, chips, texto do artista (Wikipédia), Editar e, embaixo, os shows em destaque. No Editar do artista, "Buscar artista" troca o nome e junta as pastas.
- Abas Filmes, Séries, Desenhos, Shows e Stand-up: grade de capas, com o filtro "Só não assistidos" no topo. Capa com selo 4K ou 1080p e a nota do filme.
- Configurações e Sobre (créditos do Nova, licenças, aviso do TMDB): seguir este padrão. Configurações em seções, cada uma com a própria subtela, agrupadas com título (2.0, ajuste de 21/11/2026), na cor e na fonte do app:
  - Biblioteca de mídia, em destaque e primeiro: Pastas da biblioteca, os servidores cadastrados (cada um com Conectado / Fora de alcance ou Digite a senha de novo, abre o Editar servidor) e Adicionar servidor, direto na lista; depois, a seção Biblioteca (atualização e pasta de imagens).
  - Aparência e navegação: Início (só na TV), Navegação, Shows.
  - Músicas (2.0, só fora da TV): Músicas.
  - Reprodução: Legendas, Player, Séries.
  - Sistema: App.
  Grupos depois do destaque e seções dentro de cada um em ordem alfabética. Opção com muitos valores abre em seletor. Voltar retorna da subtela à lista de seções. Na TV, todo item com foco tem o realce das linhas.
- Painel da marca nas Configurações (2.0, só na TV): à direita da lista, sem foco, o logo StarkTech sobre um brilho azul que pulsa devagar (parado com "remover animações"), "StarkTech Studio®", "StarkTech Media Player · versão X" (do pacote) e um cartão com a versão instalada em destaque e as novidades dela (as da 2.0.0, guardadas no app; outra versão mostra que não informou as novidades). O tablet segue sem o painel.
- Ícone do app (cor e selo): pendente; usar provisório.
