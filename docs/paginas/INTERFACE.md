# INTERFACE — StarkTech Media Player

> Telas e comportamentos aprovados na F1 (30/10/2026). Fonte visual: Claude Design "StarkTech Media Player — Telas F1" (https://claude.ai/artifact/KS8n6K5Dg7W5UXgfbhKh9A). Cópia do código de cada tela em `telas-f1/` (HTML com estilos inline: cores, tamanhos e textos exatos).
> Filmes, capítulos e números das telas são exemplo. Capas aparecem como blocos de cor: no app são as do TMDB.

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
| `telas-f1/Main.dc.html` | Biblioteca | Menu no topo (Início, Filmes, Séries, Shows, Pastas) + busca e configurações. Destaque "Novidades na biblioteca" em slide que troca a cada 10 s (pontos clicáveis). Fileiras: Continuar assistindo, Favoritos, Filmes adicionados recentemente (selo "Assistido"). Rodapé com logo, versão e site |
| `telas-f1/tv-detalhe.dc.html` | Detalhe do filme | Fundo (imagem) atrás. "Editar" no topo, no lugar da busca. Progresso + "Marcar como assistido". Botões: Continuar, Do início, Capítulos, Áudio e legendas, Favoritar. Dados técnicos e fileira de capítulos |
| `telas-f1/tv-serie.dc.html` | Série | Temporadas e episódios montados pela pasta (`Nome / Temporada N / 01.mkv`); nome do episódio vem do TMDB; estado por episódio: assistido, em andamento, novo |
| `telas-f1/tv-player.dc.html` | Player | Lista de capítulos à direita (atual marcado, vistos com ✓). Barra com marcas dos capítulos. Botões com nome: Cap. anterior, −10 s, Pausar, +10 s, Próx. capítulo. "Próximo capítulo em…" à esquerda. Áudio, Legendas e Capítulos à direita |
| `telas-f1/tv-editar.dc.html` | Editar | Capa e fundo escolhidos entre as opções do TMDB. Campos: título, título original, tipo (Filme/Série/Show), ano, duração, gêneros, sinopse. Buscar no TMDB · Restaurar original · Salvar |
| `telas-f1/tv-busca-tmdb.dc.html` | Buscar no TMDB | Lista de resultados para escolher o título certo; ao selecionar, campos editados à mão não são trocados |
| `telas-f1/tv-menu-capa.dc.html` | Menu da capa | Toque em OK na capa abre o Detalhe. Segurar OK abre o menu: Assistir, Marcar como assistido, Favoritos, Trocar capa, Trocar fundo, Editar |
| `telas-f1/tv-pastas.dc.html` | Pastas | Pastas por origem (HD externo, interno). Tipo de cada pasta: Filmes, Séries, Shows, Ignorar. Nomes iguais são reconhecidos sozinhos. "Não identificados" com Revisar. Adicionar pasta · Atualizar biblioteca |

Foco na TV: borda branca de 4 px no item em foco (capas também aumentam 6%).

## 3. Telas — tablet (toque, 1280×800 deitado)

| Arquivo | Tela | Comportamento |
|---|---|---|
| `telas-f1/tablet-biblioteca.dc.html` | Biblioteca | Barra lateral com ícones grandes. Sem busca e sem filtros. Ícone do HD no topo: verde "HD externo" conectado, vermelho "Sem HD ✕" desconectado. Continuar assistindo. Adicionados recentemente: 5 por vez, arrastar traz mais 5 (pontos de página) |
| `telas-f1/tablet-detalhe.dc.html` | Detalhe | Capa e ações em cima; abas Capítulos, Dados técnicos, Elenco |
| `telas-f1/tablet-player.dc.html` | Player | Título grande no topo. Controles embaixo, somem após 5 s (toque mostra de novo). Centro: Cap. anterior, −10 s, Pausar, +10 s, Próx. capítulo. Direita: Áudio, Legendas, Capítulos (abre faixa de capítulos que se arrasta para o lado, sem "Ver todos") |
| `telas-f1/tablet-editar.dc.html` | Editar | Igual à TV + "Do aparelho" para capa e fundo |
| `telas-f1/tablet-pastas.dc.html` | Pastas | Igual à TV, compacta |

Gestos do player no tablet (sem dica na tela): dois toques na lateral = ±10 s · arrastar para os lados = avançar/voltar · arrastar para cima/baixo = volume.

## 4. Regras

| Regra | Detalhe |
|---|---|
| Botões de capítulo | Avançam e voltam capítulo, nunca o título. Arquivo sem capítulos: os botões somem e ficam só −10 s / +10 s |
| Tipo do conteúdo | Vem do tipo da pasta. Séries: `Nome / Temporada N / 01.mkv`. Fora do padrão: "Não identificados", corrige em Editar |
| Show | O TMDB não tem categoria de shows: dados à mão quando não encontrar |
| Capa | JPG, PNG ou WebP · vertical 2:3 · ideal 1000×1500 px |
| Fundo | JPG, PNG ou WebP · horizontal 16:9 · ideal 1920×1080 px |
| Nome | "Editar" (não "Editar dados") |

## 5. Ainda sem desenho

- Configurações e Sobre (créditos do Nova, licenças, aviso do TMDB): seguir este padrão.
- Ícone do app (cor e selo): pendente; usar provisório.
