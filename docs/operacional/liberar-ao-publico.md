# Liberar ao público — StarkTech Media Player

> O que precisa estar resolvido antes de o app sair do uso próprio e ir para qualquer pessoa. Hoje ele é de uso próprio e o código é privado. Termos conferidos em 09/11/2026: **reconferir na hora de liberar**, porque mudam sem aviso. Este texto é um levantamento técnico, não parecer jurídico.

## 1. Situação

| Assunto | Situação | O que falta |
|---|---|---|
| TMDB | Precisa de ajuste | Cache de até 6 meses e logo aprovado (§2) |
| Deezer | **Impede** a liberação como está | Tirar do app ou conseguir autorização (§2) |
| TheAudioDB | Sem chave, não é usado | Só com plano pago, se for para loja (§2) |
| Wikipédia e Commons | Quase pronto | Crédito das fotos (§2) |
| Licenças do código | Falta | Textos em Sobre e código-fonte LGPL (§3) |
| Chave de assinatura | Pronta | Nada (`build.md` §7) |
| Repositório | Privado | Só se quiser abrir o código (§5) |

## 2. Serviços que o app consulta

### TMDB (capas, fundos, sinopses)

[Termos](https://www.themoviedb.org/api-terms-of-use)

- **Uso:** a licença não cobre uso comercial. Cobrar pelo app ou ganhar dinheiro com ele, inclusive anúncio, pede contrato com o TMDB, que pode cobrar. App gratuito e sem anúncio fica fora dos exemplos de uso comercial, mas os termos não falam dele com todas as letras e o TMDB decide o que é comercial: **confirmar com o TMDB**.
- **Crédito:** logo do TMDB, menos destacado que a marca do app, e aviso de que o app usa o TMDB sem ser endossado por ele. O Sobre já tem os dois; falta conferir se o `tmdb_banner.webp`, herdado do Nova, é um logo aprovado.
- **Cache:** os termos limitam a 6 meses a guarda do que vem do TMDB. **O app guarda sem prazo.** Antes de liberar, buscar de novo o que passar de 6 meses (`tmdb.buscado` já tem a data).
- **Outros:** ao fim da licença, parar de usar a chave e apagar o que veio do TMDB. Treinar IA com esse conteúdo é proibido sem contrato.

### Deezer (capa do artista, opções de foto, busca por nome)

[Termos](https://developers.deezer.com/termsofuse)

- **Impede a liberação como está.** Os termos limitam o uso a fim privado, familiar e não comercial; outro uso pede aprovação prévia do Deezer, que também pode cortar o acesso sem aviso.
- **Caminhos:** (a) pedir autorização por escrito; (b) tirar o Deezer e deixar a capa vir da Wikipédia e da imagem do usuário; (c) trocar pela TheAudioDB paga. O ponto único é `motor/BuscaArtista`.
- **Recomendação:** (b), que não custa nada e não depende de ninguém. O texto e as fotos da Wikipédia já estão no app.

### TheAudioDB (foto, fundo e país do artista)

- **Hoje não é usada:** a chave não está configurada, e sem ela o app nem chama o serviço.
- **Termos:** a chave gratuita é para desenvolvimento; publicar em loja de apps pede assinatura paga. A cópia conferida é de terceiros ([arquivo dos termos](https://tostracker.app/document/theaudiodbcom-tos)): confirmar o texto e o preço em theaudiodb.com.

### Wikipédia e Wikimedia Commons (texto e fotos do artista)

- **Texto:** CC BY-SA, exige crédito. A página do artista já mostra "Texto da Wikipédia (CC BY-SA)".
- **Fotos:** cada arquivo do Commons tem a sua licença, em geral pedindo crédito ao autor. O app só baixa a foto que o usuário escolhe, para o aparelho dele, e o Sobre traz o crédito geral. Para publicar qualquer uma delas em material do app, conferir a licença daquele arquivo.
- **Identificação:** a [política de User-Agent](https://foundation.wikimedia.org/wiki/Policy:User-Agent_policy) pede nome do app e contato, e o app já envia (`BuscaArtista.IDENTIFICACAO`).

### GitHub (versão nova)

Só lê o `version.json` e baixa o APK publicado. Com o repositório privado, o app instalado não alcança esses arquivos: ver §5.

## 3. Licenças do código

Detalhe de cada biblioteca: `docs/arquitetura/licencas.md`.

| Obrigação | Por quê | Situação |
|---|---|---|
| Manter `LICENSE`, `NOTICE` e cabeçalhos, com "Modificado por StarkTech Studio" | Apache-2.0 do Nova, §4 | Em dia |
| "Nova" só no crédito | Apache-2.0, §6 (não cede a marca) | Falta tirar as imagens do Nova do código (backlog F4 #1) |
| Textos completos das licenças em Sobre | Apache, LGPL, OFL, ISC, BSD, MIT, FTL, BSL, zlib, PNG | **Falta** |
| Oferecer o código-fonte LGPL | FFmpeg, AudioCompress, jcifs-ng e FriBidi, que está estático no motor: inclui `native/avos` com as mudanças da StarkTech | **Falta** |
| Confirmar dependências Java, versões dos binários e a licença do jcifs-ng | Para os avisos saírem certos | **Falta** |

## 4. O app em si

- **Acesso a todos os arquivos:** fora de loja, basta o usuário permitir. Na Google Play, essa permissão passa por análise e pode ser recusada. TV sem a tela dessa permissão fica sem legenda em arquivo (backlog F3 #1).
- **Atualização:** ligar a verificação ao abrir é seguro, e a instalação sempre pede a confirmação do Android. TV sem a tela de "instalar apps desconhecidos": testar, porque pode ser preciso instalar à mão.
- **Privacidade:** o app não tem login nem coleta dados, mas chama TMDB, Deezer, Wikipédia e GitHub. Ao publicar, deixar isso escrito numa página de privacidade do site.
- **Qualidade:** lint completo e teste em aparelho limpo (F4), mais o fechamento intermitente ao abrir logo após instalar (backlog F4 #5).

## 5. Repositório

- **Código privado e app público:** repositório público separado só com as versões (APK e `version.json`), sem nenhum código. É o caminho mais simples, e o app só troca o endereço (`ENDERECO_VERSION_JSON`).
- **Abrir o código:** resolve de uma vez a oferta do código-fonte LGPL, mas expõe todo o código próprio. Antes, limpar o histórico: `DADOS - STARKTECH MEDIA PLAYER.md` e os `docs/` internos estão em commits antigos; apagar o arquivo agora não os tira de lá. Mais simples: começar um repositório novo, sem histórico.
- **Prints do README:** as capas de filme nos prints são do TMDB. Com repositório público, manter o crédito do TMDB ou trocar por prints sem capas.

## 6. Ordem sugerida

1. Tirar o Deezer ou conseguir a autorização.
2. Buscar de novo o que passar de 6 meses do TMDB e conferir o logo.
3. Completar o Sobre e publicar o pacote de código-fonte LGPL.
4. Lint, teste em aparelho limpo e imagens do Nova.
5. Página de privacidade, repositório de versões e card em `/apps` (DADOS §8).
