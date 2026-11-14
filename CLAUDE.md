# CLAUDE.md — StarkTech Media Player

## Regras
- Responder sempre em português do Brasil, curto e direto, com bom senso: o mínimo que resolve; explicação longa só se eu pedir.
- Nunca inventar; faltando dado, perguntar com opções e texto livre.
- Fazer só o que foi pedido; build e deploy só com autorização.
- Em correção, mostrar só a parte alterada.
- Em análise e retorno: não citar o que está certo; só o erro, o que fazer e o prompt de correção.
- Documentação em `docs/`, no padrão da skill `starktech-base`: ler `docs/index.md` primeiro e abrir só o que a tarefa pede; criou, moveu ou apagou arquivo, atualizar o `index.md`. Nunca gravar senha, token, chave ou dado de acesso.

## Contexto
Player e biblioteca de mídia para Android TV e tablet (o mesmo APK roda no celular), gratuito, só pt-BR, distribuído por APK no site · fork do Nova Video Player v6.6.2 (Apache 2.0) num repositório único · código em `D:\Dev\starktech-media-player` · GitHub `Starktech-studio/starktech-media-player` (público desde 09/11/2026: código e versões) · estado: F1 e F2 concluídas em 31/10/2026; F3 (biblioteca real: pastas, TMDB, player do Nova) feita em 31/10/2026, em teste no tablet do Gustavo; ajustes da TV (plano 004) no código em 04/11/2026; versão 1.0.0 publicada em 09/11/2026 nas releases do GitHub (selos, filtro, faixas guardadas, página do artista, juntar pastas, atualização pelo GitHub), de uso próprio, a testar na TV; ajustes da 1.1 no código em 09/11/2026 (Pastas em Configurações, relógio, Início em amarelo, situação da atualização, marcas de 10 min no player, favoritos no Início do tablet, abas escolhidas em Configurações, aba TV Shows, prévia do artista no Editar), publicada nas releases em 09/11/2026, a testar na TV e no tablet; versão 1.1.1 (Detalhes nas Novidades, legenda pelo índice de mídia sem o acesso a todos os arquivos, selo 4K ou 1080p, fotos de artista do fanart.tv e MusicBrainz no lugar de Deezer e TheAudioDB, licenças completas no Sobre, imagens do Nova trocadas, aba Músicas sem motor, janela de atualização nova) publicada nas releases em 09/11/2026, a testar; versão 1.5.0 (plano 005: Músicas com motor e player de áudio em segundo plano, legenda rápida no player, limpar o cache) publicada nas releases em 10/11/2026, sem teste em aparelho ainda; versão 1.5.1 (plano 006: vídeos em Músicas por artista e álbum, sub-aba Vídeos, descrição e selos do artista e do álbum, nav com Início primeiro e o resto em ordem alfabética) publicada nas releases em 12/11/2026, sem teste em aparelho ainda; versão 1.6.0 (plano 007: aleatório guardado e nos vídeos, Tocar tudo aleatório, trocar capa do artista, álbum e playlist, discos separados, vídeo anterior e próximo também pelo volante) publicada nas releases em 13/11/2026, sem teste em aparelho ainda; versão 1.7.0 (plano 008: artistas em linhas, página do artista com abas Músicas e Vídeos, cards de vídeo, Aleatório no player do vídeo de música, vídeo anterior em dois toques) publicada nas releases em 13/11/2026, sem teste em aparelho ainda; plano futuro: servidor próprio na 2.0 (`docs/backlog.md`). Gestão: `OneDrive\Trabalho\Starktech\PROJETOS\APLICAÇÕES\STARKTECH MEDIA PLAYER\` · planejamento: `OneDrive\Trabalho\Starktech\PLANEJAMENTO\STARKTECH MEDIA PLAYER\` · planos: `OneDrive\Trabalho\Starktech\PROJETOS\PLANOS\STARKTECH MEDIA PLAYER\` · dados oficiais: `DADOS - STARKTECH MEDIA PLAYER.md`, na pasta `DOCUMENTOS` da gestão · antes de abrir ao público: `docs/operacional/liberar-ao-publico.md`.

---

## 1. Pastas

| Pasta | Origem (Nova) | O que é |
|---|---|---|
| `AVP/` | aos-AVP | Manifestos, `core.mk` (make dos nativos) e scripts de ambiente |
| `design/icone/` | — (StarkTech) | Ícone do app em 1024 px, cor e monocromático (origem dos ícones Android) |
| `FileCoreLibrary/` | aos-FileCoreLibrary | Acesso a arquivos: local, USB, rede |
| `Makefile` | cópia de `AVP/Makefile` | Entrada do `make`; inclui `AVP/core.mk` |
| `MediaLib/` | aos-MediaLib | Banco de mídia, busca de dados (TMDB, Trakt) e ponte Java do motor |
| `native/avos/` | aos-avos | Motor nativo em C, com FFmpeg |
| `native/prebuilt/` | prebuilt-* | Binários prontos de terceiros (FFmpeg, dav1d, OpenSSL, libass…) — vão no Git |
| `native/` (demais) | builders do Nova | Scripts que geram os binários de `native/prebuilt/` |
| `Video/` | aos-Video | App: interface de celular, tablet e TV (Leanback); projeto Gradle principal |

Repositório, ramo e commit de origem de cada pasta: `docs/operacional/origem-nova.md`.

## 2. Regras do código

- Pastas do Nova sem `.git` próprio. Atualizar do Nova só pelo roteiro de `docs/operacional/origem-nova.md`; a tag `nova-v6.6.2` marca o código original.
- Manter `LICENSE`, `COPYING`, `NOTICE` e cabeçalhos de copyright de cada pasta. Arquivo do Nova alterado: manter o cabeçalho Apache-2.0 e acrescentar nele a linha "Modificado por StarkTech Studio em AAAA" (Apache 2.0, §4b); arquivo sem cabeçalho ganha a linha como comentário no topo.
- Marca: StarkTech. "Nova" só no crédito "Baseado no Nova Video Player" (a Apache 2.0 não cede a marca, §6).
- Git desta pasta só pelo WSL (`git` dentro do Ubuntu, em `/mnt/d/Dev/starktech-media-player`). Nunca usar o Git do Windows nesta pasta (nem Git Bash, nem o Git do Android Studio ou do VS Code no Windows): com `core.autocrlf=true` e `core.symlinks=false` ele troca o fim de linha dos scripts e quebra os links simbólicos. Configuração local do repositório: `core.autocrlf=false` e `core.symlinks=true`; `.gitattributes` com `* -text`.
- Bit de execução: em `/mnt/d` o Git não enxerga `chmod` (`core.fileMode=false`). Script novo que precisa rodar: `git update-index --chmod=+x <arquivo>`.
- Nunca ignorar `*.so` ou `*.a` no `.gitignore`: os binários de `native/prebuilt/` vão no Git.
- Chaves (TMDB, fanart.tv, assinatura do APK): fora do Git, do `docs/` e de commit. A do TMDB fica na linha `starktech.tmdb.chave=` do `~/.gradle/gradle.properties` do WSL e entra no APK pelo build (`MediaLib/build.gradle`, `docs/operacional/build.md` §2); a do fanart.tv (fotos de artista), na linha `starktech.fanart.chave=` do mesmo arquivo (`Video/build.gradle`), com cópia em `DOCUMENTOS\FANART - STARKTECH MEDIA PLAYER.md` da gestão. A assinatura do APK fica em `~/.starktech/` no WSL, com cópia na pasta `DOCUMENTOS` da gestão (`docs/operacional/build.md` §7): sem ela, o app publicado não atualiza mais. As chaves do Nova saíram do código na F2; Trakt e OpenSubtitles estão desligados, sem chave.
- Licenças de terceiros e obrigações: `docs/arquitetura/licencas.md`. Mudou nativo, biblioteca ou forma de ligação: atualizar.

## 3. Build

- Só no WSL2 (Ubuntu): o Gradle chama `make` e scripts bash antes de compilar (`MediaLib/build.gradle:146-155`, `FileCoreLibrary/build.gradle:111-120`).
- Stack: JDK 17 · AGP 9.3.3 · Gradle 9.5.0 (wrapper em `Video/`) · compileSdk 37 · minSdk 23 · targetSdk 36 · NDK e SDK em `~/Android/Sdk` (WSL).
- Comando, saída e teste: `docs/operacional/build.md`. APK em `Video/build/outputs/apk/noamazon/debug/`; cópias de teste em `apks/` (fora do Git).
- Desde o fim da F2, todo build pedido é de teste e sem lint (`./gradlew -Puniversal assembleNoamazonDebug -x lintNoamazonDebug`), com o APK copiado para `apks/` como `StarkTech Media Player 0.1.0 (teste).apk`, substituindo o anterior. Lint completo só se o Gustavo pedir e na F4.
- Versão publicada: `./gradlew -Puniversal assembleNoamazonRelease`, com o `versionCode` maior que o anterior, e o APK em `apks/` como `StarkTech Media Player X.Y.Z.apk` (`docs/operacional/build.md` §7).
- Build só com autorização.

## 4. Fases

| Fase | Entrega |
|---|---|
| F0 — Setup | Repositório com o Nova v6.6.2 sem alteração, `CLAUDE.md`, `.gitignore`, `docs/`, APK de teste |
| F1 — Interface | Telas com dados simulados |
| F2 — Motor | Lógica principal, isolada da interface |
| F3 — Integração | Interface ligada ao motor: fluxo completo, erros, salvar |
| F4 — Empacotamento | Pacote, ícones, avisos de licença, teste em aparelho limpo |
| F5 — Publicação | APK nas releases do repositório e card na página `/apps` (DADOS §8) |
