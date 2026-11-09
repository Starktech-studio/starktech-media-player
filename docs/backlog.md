# Backlog — StarkTech Media Player

> Pendências fora do plano da fase em curso, separadas pela fase em que entram. Feito, sai daqui e vai para o plano da fase.

## F3 — Integração

| # | Item | Origem |
|---|---|---|
| 1 | Artista em dois grupos: a capa de cada seção mostra a contagem da seção, e a tela lista todos os shows dele | Plano 004 (04/11/2026) |
| 2 | Tablet: conferir os capítulos do Detalhe rolando a página inteira e a nova página do artista, e tirar os prints para o README, que hoje só tem os da TV | Versão 1 (09/11/2026) |

## Próximas versões

| # | Item | Versão | Origem |
|---|---|---|---|
| 1 | Player: botão de configurar a legenda, do lado esquerdo, quando o vídeo tiver legenda. Configuração rápida: adiantar ou atrasar a legenda e aumentar ou diminuir o tamanho; o resto continua em Configurações | — | Gustavo, 09/11/2026 |
| 2 | Configurações: limpar o cache | — | Gustavo, 09/11/2026 |
| 3 | Refazer os prints do README: os de hoje ainda mostram Pastas no nav e não têm o relógio | — | Versão 1.1 (09/11/2026) |
| 4 | Músicas, como uma biblioteca de música por streaming: artistas com os álbuns, álbuns com as faixas e playlists criadas no app. A aba e as sub-abas Artistas, Álbuns e Playlists já existem, sem motor (1.1.1); falta o motor: pasta do tipo Músicas, leitura das tags, capas e o player de áudio | 1.5 | Gustavo, 09/11/2026 |
| 5 | Servidor próprio da StarkTech: guardar as músicas (e depois os vídeos) num servidor nosso, de onde o app baixa e toca. Só registro: o servidor ainda vai ser montado | 2.0 | Gustavo, 09/11/2026 |

## F4 — Empacotamento

| # | Item | Origem |
|---|---|---|
| 1 | Lint completo de novo (os builds de teste das fases C a H saem sem lint; o lint roda no fim da F1, na fase I, e aqui) | Gustavo, 30/10/2026 |
| 2 | Telas e recursos do Nova sem entrada desde a fase C: `res/xml/shortcuts.xml` sem uso; `UiChoiceDialog` sem uso na entrada. Remover ou fechar | F1 fase C (30/10/2026) |
| 3 | Fechamento intermitente na abertura a frio, logo após instalar (tablet, Android 16): `IllegalArgumentException: Activity client record must not be null … TopResumedActivityChangeItem`, na troca `EntryActivity` (abre a principal e chama `finish()` no `onCreate`) → `PrincipalTabletActivity`. Visto uma vez; a segunda abertura funcionou | F1 fase E (31/10/2026) |
| 4 | Textos sem uso: `st_ir_para`, `st_mais_10` e `st_menos_10` | Plano 004 (04/11/2026) |
| 5 | Comentário de `res/drawable/st_ponto.xml` desatualizado: fala do anel de foco na TV, e os pontos não têm mais foco | Plano 004 (04/11/2026) |
| 6 | Confirmar a versão exata de cada binário pronto de `native/prebuilt/` (`docs/arquitetura/licencas.md` §2) | F1 fase I (31/10/2026) |

## F5 — Publicação

| # | Item | Origem |
|---|---|---|
| 1 | Card na página `/apps` do site (DADOS §8). Antes de divulgar: `docs/operacional/liberar-ao-publico.md` | Versão 1 (09/11/2026) |
