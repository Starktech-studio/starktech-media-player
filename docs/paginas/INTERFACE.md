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
| `telas-f1/tv-pastas.dc.html` | Pastas | Fora do menu: em Configurações, Biblioteca, Pastas da biblioteca. Pastas por origem (HD externo, interno). Tipo de cada pasta: os das abas ligadas em Navegação (Filmes, Séries, TV Shows, Desenhos, Shows, Stand-up) e Ignorar. Nomes iguais são reconhecidos sozinhos. "Não identificados" com Revisar. Adicionar pasta · Atualizar biblioteca |

Foco na TV: borda branca de 4 px no item em foco (capas também aumentam 6%).

## 3. Telas — tablet (toque, 1280×800 deitado)

| Arquivo | Tela | Comportamento |
|---|---|---|
| `telas-f1/tablet-biblioteca.dc.html` | Biblioteca | Barra lateral com ícones grandes, Início em amarelo. Relógio HH:mm no canto superior direito, em todas as abas. Sem busca e sem filtros. Ícone do HD no topo: verde "HD externo" conectado, vermelho "Sem HD ✕" desconectado. Continuar assistindo. Favoritos. Adicionados recentemente: 5 por vez, arrastar traz mais 5 (pontos de página) |
| `telas-f1/tablet-detalhe.dc.html` | Detalhe | Favoritar e Editar no topo (também na Série): o favorito vai para a fileira do Início. Capa e ações em cima, sempre inteiras: título em até 2 linhas com "…", sinopse com até 3 linhas, justificada, e "Ver mais" para o resto (os botões nunca descem); abas Capítulos, Dados técnicos, Elenco, com o conteúdo rolando sozinho embaixo. Tocar num capítulo abre o player no início dele. Filme assistido: nota no topo (tocar na estrela; "0" dá 0 estrelas; "Tirar nota") Puxar rola a página inteira: os capítulos sobem sobre a capa e o título, e dá para ver todos. |
| `telas-f1/tablet-player.dc.html` | Player | Título grande no topo. Controles embaixo, somem no tempo de Configurações (5 s de fábrica) junto com a barra do sistema (tela cheia; toque mostra de novo). Esquerda: Capítulos (abre faixa de capítulos que se arrasta para o lado, sem "Ver todos"), Áudio, Legendas. Centro: Cap. anterior, Pausar, Próx. capítulo. Direita: "Próximo: 02 · … em 00:28" e Expandir. Saindo com o vídeo tocando, segue em miniatura (picture-in-picture). Botões de mídia de fora do app (carro, fone Bluetooth): próxima / anterior trocam de capítulo; tocar / pausar valem Legendas termina em "Carregar legenda": navegador do app a partir da pasta do vídeo (.srt, .ass, .ssa, .vtt), e a escolhida entra ligada. |
| `telas-f1/tablet-editar.dc.html` | Editar | Igual à TV + "Do aparelho" para capa e fundo |
| `telas-f1/tablet-pastas.dc.html` | Pastas | Igual à TV, compacta |

Gestos do player no tablet (sem dica na tela): dois toques na lateral = pulo de Configurações (10 s de fábrica) · arrastar para os lados = avançar/voltar · arrastar para cima/baixo = volume.

Do plano 004, no tablet só o comum: listas de Coletânea, Artista e grupos, nota, Configurações e atualização. Novidades, prévia, barra com foco, Voltar duas vezes e botão Zoom são da TV.

## 4. Regras

| Regra | Detalhe |
|---|---|
| Botões de capítulo | Avançam e voltam capítulo, nunca o título. Arquivo sem capítulos: os botões somem e fica só Pausar no centro; filme e desenho ganham marcas de 10 em 10 minutos, com os mesmos botões e a mesma lista, só no player (o Detalhe não mostra) |
| Abas do menu | Configurações, Navegação: cada aba ligada ou desligada. De fábrica: Filmes, Séries, Desenhos e Shows; TV Shows e Stand-up, só se ligar, ou ao adicionar uma pasta com esse nome. Início e Ajustes ficam sempre. Desligar só tira do menu e dos tipos de pasta: os títulos continuam no Início e na busca |
| Músicas na 1.6 | Tocar tudo aleatório; o aleatório fica ligado até desligar (também nos vídeos de música); Vídeo anterior e Próximo vídeo no player e nos botões de fora (volante, fone, controle); Trocar foto (artista: opções e Do aparelho) e Trocar capa (álbum, playlist); álbum com Disco 1, Disco 2 |
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
| Atualização | Configurações, seção App: Atualizar automaticamente, Versão instalada e Versão disponível, com o aviso de que está na última ou de que há versão nova, e o botão Verificar ou Baixar e instalar. Ao abrir o app, com o automático ligado, avisa a versão nova uma vez. |

## 5. Ainda sem desenho

- Coletânea e artista: como o Detalhe, com capa, nome, chips, texto do artista (Wikipédia), Editar e, embaixo, os shows em destaque. No Editar do artista, "Buscar artista" troca o nome e junta as pastas.
- Abas Filmes, Séries, Desenhos, Shows e Stand-up: grade de capas, com o filtro "Só não assistidos" no topo. Capa com selo 4K ou 1080p e a nota do filme.
- Configurações e Sobre (créditos do Nova, licenças, aviso do TMDB): seguir este padrão. Configurações em seções, cada uma com a própria subtela, e opções em ordem alfabética: App, Biblioteca, Início (só na TV), Legendas, Player, Séries, Shows. Opção com muitos valores abre em seletor. Voltar retorna da subtela à lista de seções.
- Ícone do app (cor e selo): pendente; usar provisório.
