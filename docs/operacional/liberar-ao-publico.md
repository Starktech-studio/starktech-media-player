# Liberar ao público — StarkTech Media Player

> O que precisa estar resolvido antes de divulgar o app para qualquer pessoa. O repositório é público desde 09/11/2026, com o código e as versões; a divulgação no site ainda não foi feita. **Com o APK numa release pública, qualquer pessoa com o link baixa o app, e os pontos abaixo já contam a partir daí.** Termos conferidos em 09/11/2026: **reconferir na hora de liberar**, porque mudam sem aviso. Este texto é um levantamento técnico, não parecer jurídico.

## 1. Situação

| Assunto | Situação | O que falta |
|---|---|---|
| TMDB | Precisa de ajuste | Chave dentro do APK e cache de até 6 meses (§2); logo conferido |
| fanart.tv e MusicBrainz | Pronto (1.1.1) | Uso não comercial; crédito em Sobre (§2) |
| Deezer e TheAudioDB | Saíram do app (1.1.1) | Nada |
| Wikipédia e Commons | Pronto | Crédito em Sobre e na página do artista (§2) |
| Licenças do código | Quase pronto (1.1.1) | Conferir o código-fonte LGPL no repositório (§3) |
| Chave de assinatura | Pronta | Nada (`build.md` §7) |
| Repositório | Público desde 09/11/2026 | Decidir a licença do código próprio (§5) |

## 2. Serviços que o app consulta

### TMDB (capas, fundos, sinopses)

[Termos](https://www.themoviedb.org/api-terms-of-use)

- **Uso:** a licença não cobre uso comercial. Cobrar pelo app ou ganhar dinheiro com ele, inclusive anúncio, pede contrato com o TMDB, que pode cobrar. App gratuito e sem anúncio fica fora dos exemplos de uso comercial, mas os termos não falam dele com todas as letras e o TMDB decide o que é comercial: **confirmar com o TMDB**.
- **Crédito:** logo do TMDB, menos destacado que a marca do app, e aviso de que o app usa o TMDB sem ser endossado por ele. O Sobre já tem os dois. O `tmdb_banner.webp` é o "TMDB" com a pílula em degradê, um dos logos da [página de marca do TMDB](https://www.themoviedb.org/about/logos-attribution) (conferido em 09/11/2026).
- **Cache:** os termos limitam a 6 meses a guarda do que vem do TMDB. **O app guarda sem prazo.** Antes de liberar, buscar de novo o que passar de 6 meses (`tmdb.buscado` já tem a data).
- **Chave:** fica fora do repositório, mas entra no APK no build (`tmdb_api_key`), e quem tem o APK consegue extraí-la. Foi pedida como uso pessoal (DADOS §7): antes de divulgar, conferir com o TMDB se ela serve para o app. Se houver abuso, gerar outra e publicar uma versão nova; quem não atualizar fica sem dados do TMDB.
- **Outros:** ao fim da licença, parar de usar a chave e apagar o que veio do TMDB. Treinar IA com esse conteúdo é proibido sem contrato.

### fanart.tv (foto e fundo do artista, 1.1.1)

[Chave](https://fanart.tv/get-an-api-key/)

- **Uso:** "Do not use our API for commercial use without written consent" e não usar a API dentro de outra API para terceiros. App gratuito, sem anúncio e sem venda: dentro do uso permitido. Se um dia o app gerar receita, pedir a autorização por escrito.
- **Chave:** de projeto, fora do repositório, entra no APK no build (`st_chave_fanart`); quem tem o APK consegue extraí-la. Se houver abuso, gerar outra e publicar uma versão nova.
- **Crédito:** no Sobre ("fotos e fundos do fanart.tv").

### MusicBrainz (achar o artista pelo nome)

- **Uso:** dados em domínio público (CC0), sem chave. Um pedido por segundo e identificação no pedido (`BuscaArtista.IDENTIFICACAO`), que o app já segue. Crédito no Sobre.

### Deezer e TheAudioDB

Saíram na 1.1.1: os termos do Deezer só permitem uso privado e familiar, e a chave gratuita da TheAudioDB não vale para app distribuído. Quem tinha foto deles volta a ser buscado (banco v6); a foto antiga só troca se achar outra.

### Wikipédia e Wikimedia Commons (texto e fotos do artista)

- **Texto:** CC BY-SA, exige crédito. A página do artista já mostra "Texto da Wikipédia (CC BY-SA)".
- **Fotos:** cada arquivo do Commons tem a sua licença, em geral pedindo crédito ao autor. O app só baixa a foto que o usuário escolhe, para o aparelho dele, e o Sobre traz o crédito geral. Para publicar qualquer uma delas em material do app, conferir a licença daquele arquivo.
- **Identificação:** a [política de User-Agent](https://foundation.wikimedia.org/wiki/Policy:User-Agent_policy) pede nome do app e contato, e o app já envia (`BuscaArtista.IDENTIFICACAO`).

### GitHub (versão nova)

Só lê o `version.json` e baixa o APK publicado, nas releases do repositório do app, sem login: ver §5.

## 3. Licenças do código

Detalhe de cada biblioteca: `docs/arquitetura/licencas.md`.

| Obrigação | Por quê | Situação |
|---|---|---|
| Manter `LICENSE`, `NOTICE` e cabeçalhos, com "Modificado por StarkTech Studio" | Apache-2.0 do Nova, §4 | Em dia |
| "Nova" só no crédito | Apache-2.0, §6 (não cede a marca) | Feito (1.1.1): as imagens de marca do Nova foram trocadas pelas da StarkTech |
| Textos completos das licenças em Sobre | Apache, LGPL, OFL, ISC, BSD, MIT, FTL, BSL, zlib, PNG, CDDL, Bouncy Castle | Feito (1.1.1): OK num aviso abre o texto (`LicencaActivity`) |
| Oferecer o código-fonte LGPL | FFmpeg, AudioCompress, jcifs-ng e FriBidi, que está estático no motor: inclui `native/avos` com as mudanças da StarkTech | **Em parte:** o repositório público já traz `native/avos` e os scripts de build. O Sobre diz onde está (1.1.1). Falta conferir se o código do FFmpeg com os patches, do AudioCompress e do FriBidi está completo |
| Confirmar dependências Java, versões dos binários e a licença do jcifs-ng | Para os avisos saírem certos | Dependências Java e jcifs-ng (LGPL) feitos (1.1.1, `licencas.md` §9). Falta a versão exata de cada binário pronto |

## 4. O app em si

- **Acesso a todos os arquivos:** fora de loja, basta o usuário permitir. Na Google Play, essa permissão passa por análise e pode ser recusada. Sem o acesso (recusado ou TV sem a tela), a legenda em arquivo vem do índice de mídia do Android (1.1.1).
- **Músicas (1.5):** pede `READ_MEDIA_AUDIO` e usa um serviço em primeiro plano do tipo `mediaPlayback` só enquanto a música toca. Na Google Play, os dois precisam ser declarados no formulário do app; fora de loja, nada a fazer.
- **Atualização:** ligar a verificação ao abrir é seguro, e a instalação sempre pede a confirmação do Android. TV sem a tela de "instalar apps desconhecidos": testar, porque pode ser preciso instalar à mão.
- **Privacidade:** o app não tem login nem coleta dados, mas chama TMDB, MusicBrainz, fanart.tv, Wikipédia e GitHub. Ao publicar, deixar isso escrito numa página de privacidade do site.
- **Qualidade:** lint completo e teste em aparelho limpo (F4), mais o fechamento intermitente ao abrir logo após instalar (backlog F4 #5).

## 5. Repositório

Um repositório só, público desde 09/11/2026: o código fica nele, e as versões (APK e `version.json`), nas releases. É de lá que o app lê a versão nova (`ENDERECO_VERSION_JSON`).

- **O que fica visível:** o código, os `docs/`, o `CLAUDE.md` e todo o histórico, inclusive o `DADOS - STARKTECH MEDIA PLAYER.md` em commits antigos. Apagar um arquivo agora não o tira do histórico; para isso, só reescrevendo o histórico ou começando um repositório novo.
- **Chaves:** conferido em 09/11/2026, nos 10 commits existentes: a chave do TMDB, a senha da assinatura e o `.jks` não aparecem em nenhum. Os commits antigos ainda têm as chaves que vieram no código do Nova, retiradas na F2; não são da StarkTech.
- **Código próprio:** os arquivos da StarkTech dizem "Todos os direitos reservados": dá para ler, mas não há licença para reutilizar. Os do Nova seguem em Apache-2.0. Decidir se o código próprio ganha uma licença aberta.
- **Prints do README:** as capas de filme nos prints são do TMDB. O README mantém o crédito do TMDB; se ele sair, trocar por prints sem capas.

## 6. Ordem sugerida

1. Buscar de novo o que passar de 6 meses do TMDB e confirmar a chave com o TMDB.
2. Conferir o código-fonte LGPL no repositório.
3. Lint e teste em aparelho limpo.
4. Página de privacidade e card em `/apps` (DADOS §8).
