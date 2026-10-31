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
- O `assemble…` depende do lint (`abortOnError = true`). Só para diagnóstico, se o lint travar: `./gradlew -Puniversal assembleNoamazonDebug -x lintNoamazonDebug`.

## 3. Saída e instalação

- APK: `Video/build/outputs/apk/noamazon/debug/` (nome na linha `generated:` que o build imprime): `br.com.starktechstudio.mediaplayer-1-0.1.0-debug.apk`.
- Pacote `br.com.starktechstudio.mediaplayer`, versão 0.1.0 (versionCode 1), desde a F1 fase A; só pt-BR no APK (`androidResources.localeFilters` em `Video/build.gradle`).
- Cópias de teste em `apks/` na raiz (fora do Git).
- Instalar pelo Windows: `adb install -r <apk>`. Pacote, authorities, `taskAffinity` e `sharedUserId` diferentes dos do Nova: instala ao lado do Nova (`org.courville.nova`), sem desinstalar.
- Enquanto o APK tiver as chaves do Nova (`MediaLib/res/values/donottranslate.xml`), ele não sai desta máquina.

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

Avisos do build, sem erro:
- NDK r30 aceitou o `APP_PLATFORM android-21` do avos, só com aviso: o risco do NDK (seção 4) não ocorreu.
- `stripNoamazonDebugDebugSymbols`: o AGP não removeu os símbolos de depuração das `.so` (não há `ndkVersion` nos `build.gradle`) e empacotou como vieram. Ver na F4, no APK de release.
