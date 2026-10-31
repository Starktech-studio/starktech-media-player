# Backlog — StarkTech Media Player

> Pendências fora do plano da fase em curso, separadas pela fase em que entram. Feito, sai daqui e vai para o plano da fase.

## F3 — Integração

| # | Item | Origem |
|---|---|---|
| 1 | Player: trocar a reprodução simulada pelo player do Nova (`PlayerActivity`/`PlayerController`), com os controles novos; faixas de áudio / legenda escolhidas de verdade | F1 fase F (31/10/2026) |
| 2 | Configurações (áudio, legenda, tamanho, retomar) ligadas às preferências do player do Nova | F1 fase I (31/10/2026) |
| 3 | Player e telas do Nova ligados: esconder baixar legendas (OpenSubtitles) e o Trakt, desligados e sem chave (D5 da F2) | F2 etapa 4 (31/10/2026) |
| 4 | Buscar no TMDB de verdade: sem chave (`motor/ChaveTmdb.temChave`), avisar em vez de buscar | F2 etapa 4 (31/10/2026) |
| 5 | Ligar o motor da F2 às telas (`docs/arquitetura/motor.md` §7): capítulos no player e no Detalhe, pastas e varredura na biblioteca, chave do título pelo caminho, capa e fundo do aparelho fora do Editar | F2 etapa 5 (31/10/2026) |
| 6 | Adicionar pasta de verdade: a tela com os armazenamentos e as pastas do aparelho (hoje pastas de exemplo e a nova só na memória do app); o motor já tem as duas partes (`motor/ArquivosDoAparelho` e `motor/Pastas.adicionar`). Decisão do Gustavo: F3 | F1 fase H (31/10/2026) |
| 7 | Tela de carregamento esperar a biblioteca real (hoje espera os dados simulados: `comum/TelaCarregamento.carregar`) | F2 fechamento (31/10/2026) |

## F4 — Empacotamento

| # | Item | Origem |
|---|---|---|
| 1 | Imagens do Nova ainda no código: busca e canal da TV, recomendações e widget — trocar pelas da StarkTech | F1 fase B (30/10/2026) |
| 2 | Ícone do Nova no sabor `manageexternalstorage` (`Video/src/manageexternalstorage/`) — trocar pelo ícone StarkTech | F1 fase B (30/10/2026) |
| 3 | Lint completo de novo (os builds de teste das fases C a H saem sem lint; o lint roda no fim da F1, na fase I, e aqui) | Gustavo, 30/10/2026 |
| 4 | Telas e recursos do Nova sem entrada desde a fase C: `MainActivityLeanback` segue `exported="true"` sem `intent-filter` (qualquer app a abre pelo nome); `res/xml/shortcuts.xml` sem uso; `UiChoiceDialog` sem uso na entrada. Remover ou fechar | F1 fase C (30/10/2026) |
| 5 | Fechamento intermitente na abertura a frio, logo após instalar (tablet, Android 16): `IllegalArgumentException: Activity client record must not be null … TopResumedActivityChangeItem`, na troca `EntryActivity` (abre a principal e chama `finish()` no `onCreate`) → `PrincipalTabletActivity`. Visto uma vez; a segunda abertura funcionou | F1 fase E (31/10/2026) |
| 6 | Sobre: textos completos das licenças, oferta do código-fonte LGPL, conferir o logo do TMDB, versão do Lucide e licença do jcifs-ng (`docs/arquitetura/licencas.md` §7) | F1 fase I (31/10/2026) |
