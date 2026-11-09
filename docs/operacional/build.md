# Build — StarkTech Media Player

Build só no WSL2 (Ubuntu): o Gradle chama `make` e scripts bash antes de compilar (`MediaLib/build.gradle:146-155`, `FileCoreLibrary/build.gradle:111-120`). No Windows nativo não roda sem alterar o código do Nova (`make`, `uname`, scripts bash, sem `gradlew.bat`). Build só com autorização.

## 1. Ambiente (WSL Ubuntu 26.04, instalado em 30/10/2026)

| Item | Versão | Onde |
|---|---|---|
| Pacotes apt | `build-essential python3 python3-pip python3-setuptools ninja-build maven file wget curl unzip git pkg-config meson nasm openjdk-17-jdk-headless rsync` (lista do Nova sem `openjdk-8`, mais `rsync` para a montagem) | WSL |
| JDK | OpenJDK 17.0.20 (`/usr/lib/jvm/java-17-openjdk-amd64`) — o `core.mk` procura `java-17` em `update-alternatives` | WSL |
| repo | launcher em `~/bin/repo` | WSL |
| Command-line tools | `commandlinetools-linux-15859902_latest.zip` (sdkmanager 22.0) | `~/Android/Sdk/cmdline-tools/latest` |
| Plataforma | `platforms;android-37.0` (compileSdk 37) | `~/Android/Sdk` |
| Build-tools | 36.0.0 | `~/Android/Sdk` |
| Platform-tools | 37.0.1 | `~/Android/Sdk` |
| NDK | r30 LTS = `ndk;30.0.16248370` (um só: o `make` usa o maior instalado em `$SDK/ndk/`) | `~/Android/Sdk/ndk` |
| CMake | não instalado (só para regenerar nativos) | — |
| Gradle | 9.5.0, baixado pelo wrapper de `Video/` | `~/.gradle` |
| AGP | 9.3.3 · compileSdk 37 · minSdk 23 · targetSdk 36 | `Video/build.gradle` |

Variáveis no `~/.bashrc` (lidas em `AVP/core.mk:39-49`):

```bash
export ANDROID_SDK_ROOT=$HOME/Android/Sdk
export ANDROID_HOME=$ANDROID_SDK_ROOT
export PATH=$HOME/bin:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$PATH
```

## 2. Comando

No WSL:

```bash
cd /mnt/d/Dev/starktech-media-player/Video
./gradlew -Puniversal assembleNoamazonDebug
```

- `-Puniversal`: um APK com as 4 ABIs e versão fixa (sem a data no nome da versão).
- Chamado do Windows (`wsl -e bash -lc "…"`), o shell não é interativo e o `~/.bashrc` não exporta as variáveis da seção 1: o Gradle para com "SDK location not found". Exportar `ANDROID_SDK_ROOT` e `ANDROID_HOME` no próprio comando.
- Alternativa equivalente ao fluxo do Nova: `make` na raiz (APKs por ABI, versão com data).
- Antes do Java, o Gradle roda o `make` dos nativos (avos, AudioCompress, libnativehelper, JNI do FileCoreLibrary) com os binários prontos de `native/prebuilt/`.
- O `assemble…` depende do lint (`Video/build.gradle:355-357`; `abortOnError = true` em `:307`).
- Builds de teste da F1, fases C a H: sem lint (Gustavo, 30/10/2026), pela linha de comando, sem mexer no `build.gradle`. O `-x` tira o `lintNoamazonDebug` e as tarefas que só ele usa (`lintAnalyze…`, `lintReport…`, modelos de lint), conferido com `--dry-run`:

```bash
./gradlew -Puniversal assembleNoamazonDebug -x lintNoamazonDebug
```

- Lint completo (comando da seção 2 sem o `-x`): uma vez no fim da F1 (fase I) e de novo na F4 (`docs/backlog.md`).
- Chave da API do TMDB (desde a F2): fora do repositório, na linha `starktech.tmdb.chave=` do `~/.gradle/gradle.properties` do WSL (permissão 600; o valor fica no documento do TMDB da pasta de gestão). O `MediaLib/build.gradle` (`chaveTmdb()`) a põe no APK na hora do build e nos testes que chamam o TMDB. Sem a linha, o build passa, a chave fica vazia e esses testes são pulados. Trakt e OpenSubtitles: desligados, sem chave (D5 da F2). Nunca gravar a chave no repositório, no `docs/` ou num commit.
- Chave da TheAudioDB (capa de artista, plano 004): fora do repositório, na linha `starktech.theaudiodb.chave=` do mesmo `~/.gradle/gradle.properties`. O `Video/build.gradle` a põe no APK na hora do build (`st_chave_theaudiodb`). Sem a linha, o build passa e a capa de artista usa só o Deezer, que responde sem chave. Nunca gravar a chave no repositório, no `docs/` ou num commit.

## 3. Saída e instalação

- APK: `Video/build/outputs/apk/noamazon/debug/` (nome na linha `generated:` que o build imprime): `br.com.starktechstudio.mediaplayer-1-0.1.0-debug.apk`.
- Pacote `br.com.starktechstudio.mediaplayer`, versão 0.1.0 (versionCode 1), desde a F1 fase A (subir a versão: §6); só pt-BR no APK (`androidResources.localeFilters` em `Video/build.gradle`).
- Cópia de teste em `apks/` na raiz (fora do Git), com o nome `StarkTech Media Player 0.1.0 (teste).apk`; cada build de teste substitui a anterior.
- Instalar pelo Windows: `adb install -r <apk>`. Pacote, authorities, `taskAffinity` e `sharedUserId` diferentes dos do Nova: instala ao lado do Nova (`org.courville.nova`), sem desinstalar.
- Legenda em arquivo precisa do acesso a todos os arquivos. Aparelho com a tela desse acesso: o app pede ao abrir. TV sem ela (a TCL de teste): liberar por adb depois de instalar (vale até desinstalar): `adb shell appops set --uid br.com.starktechstudio.mediaplayer MANAGE_EXTERNAL_STORAGE allow`.
- Desde a F2 (etapa 4) o APK não tem mais as chaves do Nova: o `MediaLib/res/values/donottranslate.xml` saiu e os valores de exemplo nos comentários e testes do MediaLib foram trocados. O APK de teste continua só nesta máquina até a F4 (assinatura e teste em aparelho limpo).

## 4. NDK

Se o `ndk-build` recusar o NDK r30 (`APP_PLATFORM := android-21` em `native/avos/jni/Application.mk:22`): instalar `ndk;29.0.14206865` (versão do Docker do Nova) e remover o r30, porque o `make` usa o maior instalado.

```bash
sdkmanager "ndk;29.0.14206865" && sdkmanager --uninstall "ndk;30.0.16248370"
```

## 5. Resultado

| Data | Comando | Resultado |
|---|---|---|
| 30/10/2026 | `./gradlew --no-daemon -Puniversal assembleNoamazonDebug` (código do Nova v6.6.2 sem alteração, em `/mnt/d`) | `BUILD SUCCESSFUL` em 24 min (primeira execução, com o download do Gradle e das dependências); lint passou. APK `org.courville.nova-6060002-6.6.2-debug.apk`, 105 MB, 4 ABIs (arm64-v8a, armeabi-v7a, x86, x86_64), pacote `org.courville.nova`, 6.6.2 / 6060002, targetSdk 36. Cópia em `apks/`. Teste no aparelho: não feito — o APK com as chaves do Nova não sai desta máquina |
| 30/10/2026 | `./gradlew -Puniversal assembleNoamazonDebug` (F1 fase A: pacote, versão, nome e idioma; daemon novo) | `BUILD SUCCESSFUL` em 18 min (65 de 131 tarefas executadas): APK pronto em ~6 min, o lint levou ~12 min; lint passou. APK `br.com.starktechstudio.mediaplayer-1-0.1.0-debug.apk`, 4 ABIs, pacote `br.com.starktechstudio.mediaplayer`, 0.1.0 / 1, idiomas no APK: padrão, `pt` e `pt-BR` |
| 30/10/2026 | idem, incremental (uma classe Java alterada, daemon aberto) | `BUILD SUCCESSFUL` em 9 min 21 s (16 de 131 tarefas executadas): APK pronto em ~2 min, o resto é lint. APK com 103 MB. Instalado nos emuladores de TV e tablet, ao lado do Nova |
| 30/10/2026 | idem, incremental (F1 fase B: fonte Inter, cores, estilos, fundos, ícone, banner e logo; só recursos e manifesto; daemon aberto) | `BUILD SUCCESSFUL` em 9 min 52 s (27 de 131 tarefas executadas): APK pronto em ~2 min 45 s, o resto é lint; lint passou (0 erros; os avisos novos são só `UnusedResources` dos recursos `st_*`, usados a partir da fase C). APK com 104 MB (109.264.587 bytes; as 5 fontes Inter somam ~2 MB). Ícone `res/mipmap-anydpi-v26/st_ic_launcher.xml` e banner `res/drawable-xxxhdpi-v4/st_banner.png` conferidos no `aapt2 dump badging`. Instalado nos emuladores de TV e tablet |
| 30/10/2026 | `./gradlew -Puniversal assembleNoamazonDebug -x lintNoamazonDebug` (F1 fase C: dados simulados, navegação e rotas; primeiro build sem lint; daemon aberto) | `BUILD SUCCESSFUL` em 3 min 2 s (20 de 82 tarefas executadas; nenhuma de lint). APK com 109.422.546 bytes |
| 30/10/2026 | idem, incremental (uma classe nova e duas alteradas) | `BUILD SUCCESSFUL` em 2 min 16 s (15 de 82 tarefas executadas). APK com 110.858.770 bytes (~106 MB). Instalado nos emuladores de TV e tablet |
| 30/10/2026 | `./gradlew -Puniversal assembleNoamazonDebug -x lintNoamazonDebug` (F1 fase D: Início, listas, Favoritos e slide; 7 classes novas, 9 layouts, 8 desenhos; daemon aberto) | `BUILD SUCCESSFUL` em 3 min 8 s (15 de 82 tarefas executadas). APK com 109.476.082 bytes |
| 30/10/2026 | idem, incrementais (correções depois do teste nos emuladores) | `BUILD SUCCESSFUL` em 1 min 46 s, 54 s e 1 min 1 s (14, 11 e 7 de 82 tarefas). APK final com 109.636.849 bytes (~105 MB). Instalado nos emuladores de TV e tablet |
| 31/10/2026 | `./gradlew -Puniversal assembleNoamazonDebug` (F1 fases E a I e ajustes; lint completo do fechamento da F1) | Primeira tentativa: lint com 1 erro (`GestureBackNavigation`: tecla Voltar tratada no `dispatchKeyEvent` do player da TV), corrigido. Final: `BUILD SUCCESSFUL` em 10 min 19 s; lint com 0 erros e 69 avisos (nenhum bloqueia). Builds de teste sem lint do dia: 1 a 2 min cada. APK com ~110 MB. Instalado nos emuladores de TV e tablet |
| 31/10/2026 | `./gradlew -Puniversal assembleNoamazonDebug` e `testNoamazonDebugUnitTest --tests 'br.com.starktechstudio.mediaplayer.motor.*'` (fechamento da F2: motor nativo com capítulos, pacote `motor`, chave do TMDB pelo Gradle) | Primeira tentativa: erro de compilação em `Capitulos` (dois métodos `vazio()`), corrigido. Final: `BUILD SUCCESSFUL` em 11 min 52 s, com o motor nativo recompilado; lint com 0 erros e 70 avisos. Testes do `motor`: 45 de 45 passaram (4 min). APK com ~116 MB. Instalado nos emuladores de TV e tablet |

Avisos do build, sem erro:
- NDK r30 aceitou o `APP_PLATFORM android-21` do avos, só com aviso: o risco do NDK (seção 4) não ocorreu.
- `stripNoamazonDebugDebugSymbols`: o AGP não removeu os símbolos de depuração das `.so` (não há `ndkVersion` nos `build.gradle`) e empacotou como vieram. Ver na F4, no APK de release.

## 6. Versão nova e `version.json`

Subir a versão a cada APK novo, em `Video/build.gradle` ou no comando, sempre com `-Puniversal` (sem ele, a data entra no nome e no `versionCode`):

```bash
./gradlew -Puniversal -Pstarktech.versionCode=N -Pstarktech.versionName=X.Y.Z assembleNoamazonDebug -x lintNoamazonDebug
```

O app compara o `versionCode` com o do `version.json` da última versão publicada (`motor/Atualizador.java`). O endereço fica só na constante `ENDERECO_VERSION_JSON`: `https://github.com/Starktech-studio/starktech-media-player/releases/latest/download/version.json`. O repositório é público desde 09/11/2026 e guarda o código e as versões. Enquanto não houver versão publicada, o app só avisa que não deu para verificar. Valores de exemplo:

```json
{
  "versao": "0.2.0",
  "versionCode": 2,
  "novidades": ["Novidade 1", "Novidade 2"],
  "apk": "https://github.com/Starktech-studio/starktech-media-player/releases/download/v0.2.0/StarkTech-Media-Player-0.2.0.apk"
}
```

| Regra | Como é |
|---|---|
| Campos | `versao`, `versionCode` (maior que 0) e `apk` obrigatórios; `novidades` opcional. Arquivo inválido: o app avisa que não deu para verificar |
| Novidades | A janela mostra até 8 linhas; o resto é cortado |
| APK | Só por https; mesmo pacote, mesma chave de assinatura e `versionCode` igual ao do json e maior que o instalado. Pacote ou `versionCode` diferente: o app recusa depois de baixar. Outra chave: o Android recusa |
| Chave | A definitiva já existe (§7). Quem tiver APK de teste antigo reinstala uma vez e perde o banco local |

## 7. Assinatura e versão publicada

Chave definitiva criada em 09/11/2026 (RSA 4096, válida por 100 anos). Toda versão publicada tem de sair com ela: com outra, o Android recusa instalar por cima, e quem instalou teria de desinstalar e perder os dados.

| O que | Onde |
|---|---|
| Chave e senha no WSL | `~/.starktech/starktech-media-player.jks` e `~/.starktech/media-player-assinatura.properties`, lidos pelo `Video/build.gradle`; fora do repositório |
| Cópia de segurança | Pasta de gestão, `DOCUMENTOS/ASSINATURA APK - STARKTECH MEDIA PLAYER` (`.jks` e `.md`, com como usar em outra máquina) |
| Impressão digital (SHA-256) | `D0:9C:83:E8:7C:1A:30:3C:82:5A:F5:99:16:3E:45:96:53:16:A8:3B:FC:BF:F9:2D:DB:10:44:B2:67:BC:58:A6` |

Com o arquivo no WSL, os builds de teste também saem com essa chave. A versão publicada não encolhe o código (`minifyEnabled = false`): o que vai para quem instala é o que foi testado.

```bash
./gradlew -Puniversal assembleNoamazonRelease
```

APK em `Video/build/outputs/apk/noamazon/release/`; cópia em `apks/` com o nome `StarkTech Media Player X.Y.Z.apk`. 1.0.0 = `versionCode` 2 (o 1 foi o dos APKs de teste).

Publicar uma versão:

1. Subir `versionCode` e `versionName` (§6) e gerar o APK.
2. No repositório `Starktech-studio/starktech-media-player`, criar uma release `vX.Y.Z` no commit do APK e anexar o APK, com nome sem espaços (`StarkTech-Media-Player-X.Y.Z.apk`), e o `version.json` apontando para ele. O da 1.0.0 está pronto em `apks/version.json`.
3. O anexo tem de se chamar `version.json`, e a release, ser a mais recente: é o que o endereço do app procura.

O Git do WSL não guarda login: o push usa o gerenciador de credenciais do Windows. A release sai pelo GitHub CLI do Windows, já logado na conta `Starktech-studio`.

```bash
git -c credential.helper= -c 'credential.helper=!"/mnt/c/Program Files/Git/mingw64/bin/git-credential-manager.exe"' push origin main
```

```powershell
gh release create vX.Y.Z -R Starktech-studio/starktech-media-player --target <commit> --latest StarkTech-Media-Player-X.Y.Z.apk version.json
```
