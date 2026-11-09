# Backlog — StarkTech Media Player

> Pendências fora do plano da fase em curso, separadas pela fase em que entram. Feito, sai daqui e vai para o plano da fase.

## F3 — Integração

| # | Item | Origem |
|---|---|---|
| 1 | TV sem a tela do acesso a todos os arquivos (a TCL de teste): legenda em arquivo só funciona depois de liberar por adb (`operacional/build.md` §3). Para quem instala pelo site, avaliar outro caminho nessas TVs | Legenda em arquivo (05/11/2026) |
| 2 | Colher os dados da TV para fechar Continuar e Capítulos (C2, C5 e K1, §1 do plano 004): `lastUpdateTime` do pacote, o botão depois de assistir mais de 1 min e voltar, log logo depois de uma falha, banco da TV e `ffprobe` num show sem capítulos | Plano 004 (04/11/2026) |
| 3 | Novidades da TV sem caminho para Detalhes: só há Assistir, e o Menu da capa (segurar OK) não tem essa opção | Plano 004 (04/11/2026) |
| 4 | Artista em dois grupos: a capa de cada seção mostra a contagem da seção, e a tela lista todos os shows dele | Plano 004 (04/11/2026) |
| 5 | Selo HDR da capa: conferir com um vídeo HDR de verdade. O código lê a cor do arquivo pelo motor, mas no teste do emulador os vídeos gerados saíram sem a marca de HDR, e o selo não foi visto | Versão 1 (09/11/2026) |
| 6 | Tablet: conferir os capítulos do Detalhe rolando a página inteira e a nova página do artista, e tirar os prints para o README, que hoje só tem os da TV | Versão 1 (09/11/2026) |

## F4 — Empacotamento

| # | Item | Origem |
|---|---|---|
| 1 | Imagens do Nova ainda no código: busca e canal da TV, recomendações e widget — trocar pelas da StarkTech | F1 fase B (30/10/2026) |
| 2 | Ícone do Nova no sabor `manageexternalstorage` (`Video/src/manageexternalstorage/`) — trocar pelo ícone StarkTech | F1 fase B (30/10/2026) |
| 3 | Lint completo de novo (os builds de teste das fases C a H saem sem lint; o lint roda no fim da F1, na fase I, e aqui) | Gustavo, 30/10/2026 |
| 4 | Telas e recursos do Nova sem entrada desde a fase C: `res/xml/shortcuts.xml` sem uso; `UiChoiceDialog` sem uso na entrada. Remover ou fechar | F1 fase C (30/10/2026) |
| 5 | Fechamento intermitente na abertura a frio, logo após instalar (tablet, Android 16): `IllegalArgumentException: Activity client record must not be null … TopResumedActivityChangeItem`, na troca `EntryActivity` (abre a principal e chama `finish()` no `onCreate`) → `PrincipalTabletActivity`. Visto uma vez; a segunda abertura funcionou | F1 fase E (31/10/2026) |
| 6 | Sobre: textos completos das licenças, oferta do código-fonte LGPL, conferir o logo do TMDB, versão do Lucide e licença do jcifs-ng (`docs/arquitetura/licencas.md` §7) | F1 fase I (31/10/2026) |
| 7 | Decidir TheAudioDB e Deezer antes do APK público: as condições conhecidas restringem app distribuído (`docs/arquitetura/licencas.md` §8) | Plano 004 (04/11/2026) |
| 8 | Textos sem uso: `st_ir_para`, `st_mais_10` e `st_menos_10` | Plano 004 (04/11/2026) |
| 9 | Comentário de `res/drawable/st_ponto.xml` desatualizado: fala do anel de foco na TV, e os pontos não têm mais foco | Plano 004 (04/11/2026) |

## F5 — Publicação

| # | Item | Origem |
|---|---|---|
| 1 | Criar o repositório público `starktech-media-player-versoes` e publicar a release com o APK e o `version.json` (`docs/operacional/build.md` §7). Antes de abrir ao público: `docs/operacional/liberar-ao-publico.md` | Versão 1 (09/11/2026) |
