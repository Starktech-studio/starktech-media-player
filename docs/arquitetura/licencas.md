# Licenças — StarkTech Media Player

Base: Nova Video Player release v6.6.2 (27/10/2026). Levantamento de 30/10/2026 (plano `001 - F0 Setup.md`). Mudou nativo, biblioteca ou forma de ligação: atualizar este arquivo.

## 1. Licença de cada pasta (repositório do Nova)

Fonte: API da organização (`api.github.com/orgs/nova-video-player/repos`, campo `license`), conferida nos arquivos quando indicado.

| Pasta | Projeto | Licença do repositório |
|---|---|---|
| `AVP` | aos-AVP | Apache-2.0 (`LICENSE.txt`) |
| `Video` | aos-Video | Apache-2.0 (`LICENSE.txt`) |
| `MediaLib` | aos-MediaLib | Apache-2.0 (`LICENSE.txt`) |
| `FileCoreLibrary` | aos-FileCoreLibrary | Apache-2.0 (`LICENSE.txt`) |
| `native/avos` | aos-avos | Apache-2.0 (`LICENSE.txt`) |
| `native/audiocompress` | aos-audiocompress | LGPL-2.1 (`COPYING`) |
| `native/ffmpeg-android-builder` | ffmpeg-android-builder-nobin | não encontrado (sem arquivo de licença) |
| `native/dav1d-android-builder` | dav1d-android-builder-nobin | não encontrado (sem arquivo de licença) |
| `native/opus-android-builder` | opus-android-builder-nobin | Apache-2.0 |
| `native/libmysofa-android-builder` | libmysofa-android-builder | Apache-2.0 |
| `native/zlib-android-builder` | zlib-android-builder | Apache-2.0 |
| `native/libpng-android-builder` | libpng-android-builder | Apache-2.0 |
| `native/libxml2-android-builder` | libxml2-android-builder | Apache-2.0 |
| `native/libfreetype-android-builder` | libfreetype-android-builder | Apache-2.0 |
| `native/libfribidi-android-builder` | libfribidi-android-builder | Apache-2.0 |
| `native/harfbuzz-android-builder` | harfbuzz-android-builder | Apache-2.0 |
| `native/libunibreak-android-builder` | libunibreak-android-builder | Apache-2.0 |
| `native/fontconfig-android-builder` | fontconfig-android-builder | Apache-2.0 |
| `native/libass-android-builder` | libass-android-builder | Apache-2.0 |
| `native/openssl-android-builder` | openssl-android-builder-nobin | Apache-2.0 |
| `native/libnativehelper` | aos-libnativehelper | Apache-2.0 (`MODULE_LICENSE_APACHE2` + `NOTICE`, código do AOSP) |
| `native/boost` | aos-boost-android-builder | não encontrado (sem arquivo de licença) |
| `native/libtorrent-android-builder` | aos-libtorrent-android-builder | Apache-2.0 |
| `native/torrentd` | torrentd-nobin | Apache-2.0 (`LICENSE.txt`) |
| `native/prebuilt/ffmpeg` | prebuilt-ffmpeg | Apache-2.0 (repositório); binários: seção 2 |
| `native/prebuilt/opus` | prebuilt-opus | idem |
| `native/prebuilt/dav1d` | prebuilt-dav1d | idem |
| `native/prebuilt/openssl` | prebuilt-openssl | idem |
| `native/prebuilt/libmysofa` | prebuilt-libmysofa | idem |
| `native/prebuilt/torrentd` | prebuilt-torrentd | idem |
| `native/prebuilt/zlib` | prebuilt-zlib | idem + `LICENSE.zlib` |
| `native/prebuilt/libpng` | prebuilt-libpng | idem + `LICENSE.libpng` |
| `native/prebuilt/libxml2` | prebuilt-libxml2 | idem + `LICENSE.libxml2` |
| `native/prebuilt/freetype` | prebuilt-freetype | idem + `LICENSE.freetype` |
| `native/prebuilt/fribidi` | prebuilt-fribidi | idem + `LICENSE.fribidi` |
| `native/prebuilt/harfbuzz` | prebuilt-harfbuzz | idem + `LICENSE.harfbuzz` |
| `native/prebuilt/libunibreak` | prebuilt-libunibreak | idem + `LICENSE.libunibreak` |
| `native/prebuilt/fontconfig` | prebuilt-fontconfig | idem + `LICENSE.fontconfig` |
| `native/prebuilt/libass` | prebuilt-libass | idem + `LICENSE.libass` |

## 2. Nativos que entram no APK

Versões: as do builder no commit da release. O próprio Nova avisa que a revisão do builder não prova como o binário pronto foi gerado (`native/prebuilt/libass/README.md:40`): a versão exata de cada binário pronto fica não confirmada.

| Biblioteca | Versão (builder) | Como entra no APK | Licença | Fonte da licença |
|---|---|---|---|---|
| avos (motor) | release v6.6.2 | `libavos.so` | Apache-2.0 | `native/avos/LICENSE.txt` |
| AudioCompress | `3a3f932` | `libaudiocompress.so`, dinâmica (`native/audiocompress/ndkbuild.mk:17`; `native/avos/jni/Android.mk:50-52`) | LGPL-2.1 | `native/audiocompress/COPYING` |
| libnativehelper (AOSP) | `ab46fb5` | `libnvpnativehelper.so` (`Video/build.gradle:268-271`) | Apache-2.0 + NOTICE | `native/libnativehelper/NOTICE` |
| FFmpeg | n9.0.2 (`native/ffmpeg-android-builder/build.sh:79`) + 4 patches do Nova (`build.sh:115-120`) | 6 `.so` dinâmicas: libavcodec, libavformat, libavfilter, libavutil, libswresample, libswscale | LGPL 2.1 ou posterior: configurado sem `--enable-gpl`, `--enable-version3` e `--enable-nonfree` | `native/ffmpeg-android-builder/config_full.sh:2-31`, `build.sh:130-145`; FFmpeg n9.0.2 `configure:4838-4849`, `LICENSE.md:1-11` |
| dav1d | 1.5.4 (`native/dav1d-android-builder/build.sh:112`) | `libdav1d.so` | BSD-2-Clause | videolan/dav1d@1.5.4 `COPYING` |
| Opus | `084bb73` (`native/opus-android-builder/build.sh:31-34`) | `libopus.so` | BSD-3-Clause | xiph/opus `COPYING` |
| libmysofa | 1.3.5 (`native/libmysofa-android-builder/build.sh:22`) | `libmysofa.so` | BSD-3-Clause | hoene/libmysofa@v1.3.5 `LICENSE` |
| OpenSSL | 4.1.0 (`native/openssl-android-builder/build.sh:61`) | estática (`libcrypto.a`, `libssl.a`) no FFmpeg e no torrentd (`native/libtorrent-android-builder/build.sh:66-68`) | Apache-2.0 | openssl/openssl@openssl-4.1.0 `LICENSE.txt` |
| libass | 0.17.5 | estática no `libavos.so` (`native/avos/ndkbuild.mk:59-60`) | ISC | `native/prebuilt/libass/LICENSE.libass` |
| FreeType | VER-2-14-3 | estática no `libavos.so` | FTL ou GPLv2, à escolha; FTL exige crédito na documentação | `native/prebuilt/freetype/LICENSE.freetype:9-26`; freetype `docs/FTL.TXT:37-38`, `:51-52` |
| FriBidi | 1.0.17 | estática no `libavos.so` | LGPL-2.1 | `native/prebuilt/fribidi/LICENSE.fribidi` |
| HarfBuzz | 15.0.0 | estática no `libavos.so` | "Old MIT" | `native/prebuilt/harfbuzz/LICENSE.harfbuzz:1` |
| fontconfig | 2.18.3 | estática no `libavos.so` | licença permissiva própria (COPYING do fontconfig) | `native/prebuilt/fontconfig/LICENSE.fontconfig` |
| libunibreak | 8.0 | estática no `libavos.so` | tipo zlib | `native/prebuilt/libunibreak/LICENSE.libunibreak` |
| libxml2 | 2.15.4 | estática no `libavos.so` | MIT | `native/prebuilt/libxml2/LICENSE.libxml2` |
| libpng | 1.6.59 | estática no `libavos.so` | PNG Reference Library License v2 | `native/prebuilt/libpng/LICENSE.libpng` |
| zlib | 1.3.2 | estática no `libavos.so` | zlib | `native/prebuilt/zlib/LICENSE.zlib` |
| libtorrent | 2.1.2 (`native/libtorrent-android-builder/build.sh:46`) | no executável torrentd (`libtorrentd.so`) | BSD-3-Clause | arvidn/libtorrent@v2.1.2 `LICENSE` |
| Boost | 1.92.0 (`native/boost/build.sh:3`) | no torrentd | BSL-1.0 | boostorg/boost@boost-1.92.0 `LICENSE_1_0.txt` |

Nenhum componente GPL encontrado entre os nativos do manifesto. libyuv e libcurl não estão no manifesto v6.6 (`native/avos/avos_config_android.mk` sem `LIBYUV`). Dependências Java (Maven/JitPack) não estão no manifesto e não foram levantadas; entre elas está o jcifs-ng (`FileCoreLibrary/build.gradle:158`), cujo fork na organização é LGPL-2.1.

## 3. O que as licenças obrigam (APK gratuito, baixado pelo site)

| Licença | Obrigação | Base |
|---|---|---|
| Apache-2.0 (Nova, avos, libnativehelper, OpenSSL) | Texto da licença junto do APK; manter avisos de copyright; marcar arquivos alterados; incluir o NOTICE onde houver (libnativehelper); a marca "Nova"/"Archos" não é cedida | Apache-2.0 §4(a)-(d) e §6 (`LICENSE.txt` de cada pasta) |
| LGPL-2.1 (FFmpeg, AudioCompress, FriBidi; jcifs-ng a confirmar) | Texto da licença e aviso no app; código-fonte das bibliotecas e das modificações (FFmpeg n9.0.2 + 4 patches + scripts de build) disponível no mesmo lugar do download ou por oferta escrita; termos do app não podem proibir engenharia reversa para depurar essas bibliotecas | LGPL-2.1 §6 (`native/audiocompress/COPYING`) |
| LGPL-2.1 com ligação estática (FriBidi no `libavos.so`) | Permitir religar: publicar também o código do motor (`native/avos`, com as mudanças da StarkTech) e os scripts de build. A interface Java não precisa ser aberta | LGPL-2.1 §6(a) |
| BSD, ISC, MIT, Old MIT, fontconfig, libpng, libunibreak | Reproduzir aviso de copyright e licença na documentação do binário (tela de avisos de terceiros) | arquivos de licença da seção 2 |
| FreeType (FTL) | Crédito na documentação: "Portions of this software are copyright © <ano> The FreeType Project (https://freetype.org). All rights reserved." | `FTL.TXT:37-38`, `:51-52` |
| zlib, BSL-1.0 | Sem aviso obrigatório em binário; crédito recomendado | `LICENSE.zlib`; `LICENSE_1_0.txt` |
| Tela Sobre | Créditos ("Baseado no Nova Video Player"), avisos de terceiros um por biblioteca + "Ver todos", logo e aviso do TMDB | skill `starktech-apps` §5 e §10; DADOS §7 |

## 4. Riscos de licença

| # | Risco | Encaminhamento |
|---|---|---|
| 1 | FriBidi (LGPL-2.1) ligado estaticamente no `libavos.so` (`native/avos/ndkbuild.mk:59-60`): obriga a publicar o código do motor | Tratar na F4 |
| 2 | OpenSSL 4.1.0 é Apache-2.0. O `LICENSE.md` do FFmpeg diz que Apache-2.0 é incompatível com LGPL v2.1 (`:108-111`), mas trata o OpenSSL como compatível (`:125-127`) e o `configure` não exige `--enable-version3` (`configure:2066-2158`). Leitura final não confirmada | Avaliar na F4 (opção conservadora: tratar o FFmpeg do APK como LGPL v3) |
| 3 | Tela de licenças do Nova desatualizada (`Video/res/xml/preferences_licences.xml`): lista libcurl e libyuv (fora do manifesto) e não lista dav1d, Opus, OpenSSL, libmysofa, libass, FreeType, FriBidi, HarfBuzz, fontconfig, libunibreak, libxml2, libpng, zlib | Refazer na F4 com a seção 2 |

## 5. Pendências da F4

- Levantar as dependências Java (Maven/JitPack) e suas licenças; confirmar a do jcifs-ng.
- Confirmar a versão exata de cada binário pronto de `native/prebuilt/`.
- Pacote de código-fonte LGPL no site (FFmpeg n9.0.2 + patches + scripts de build, AudioCompress, FriBidi e o motor `native/avos` com as mudanças da StarkTech).
- Chaves do Nova em `MediaLib/res/values/donottranslate.xml` (TMDB, Trakt, OpenSubtitles): trocar pela chave própria antes de qualquer distribuição.

## 6. Recursos da interface (F1)

| Recurso | Versão e origem | Onde está no código | Licença | Obrigação / quando |
|---|---|---|---|---|
| Fonte Inter | 4.1 (arquivos 4.001), release oficial `github.com/rsms/inter` v4.1 (16/11/2024): `Inter-4.1.zip`, sha256 `9883fdd4a49d4fb66bd8177ba6625ef9a64aa45899767dde3d36aa425756b11e`; TTF estáticos de `extras/ttf/` (Medium, SemiBold, Bold, ExtraBold, Black), sem alteração, só renomeados | `Video/res/font/st_inter_medium.ttf`, `st_inter_semibold.ttf`, `st_inter_bold.ttf`, `st_inter_extrabold.ttf`, `st_inter_black.ttf`; licença em `Video/res/raw/st_licenca_inter.txt` (cópia do `LICENSE.txt` do pacote) | OFL-1.1 ("Copyright (c) 2016 The Inter Project Authors") | Licença e aviso de copyright junto da fonte (feito no APK); aviso no Sobre (fase I); a fonte não pode ser vendida sozinha |
| Ícones de traço (D6) | Lucide, paths dos SVG do desenho (`docs/paginas/telas-f1/`); versão do Lucide não identificada | `Video/res/drawable/st_ic_*.xml` (fases C a F) | ISC (D6) | Aviso de copyright e texto da licença no Sobre (fase I); texto a copiar do repositório `lucide-icons/lucide` |
| Logo e aviso do TMDB | Logo do Nova `Video/res/drawable-xhdpi/tmdb_banner.webp` | Tela Sobre (fase I) | Termos de uso da API do TMDB | Conferir se é logo aprovado pelo TMDB e o texto do aviso (fase I / F4) |
| Ícone, banner, abertura e logo StarkTech | Próprios. Ícone, banner e splash definitivos (31/10/2026), de `APLICAÇÕES\STARKTECH MEDIA PLAYER\ÍCONES\android\res\` (escudo com o lobo e o play, família StarkTech); originais de 1024 px em `design/icone/` (`app-icon.png` e `app-icon-monocromatico.png`, este ainda sem uso no APK); logo de 256 px reduzido de `INSTITUCIONAL\MARCA\LOGO\LOGO - STARKTECH - 02 - SEM FUNDO.png` | `Video/res/mipmap-anydpi-v26/st_ic_launcher.xml` (fundo `#020617` + `mipmap-*/st_ic_launcher_frente.png`), `mipmap-*/st_ic_launcher.png`, `drawable-xhdpi/st_banner.png`, `drawable-*/st_splash.png` (em `drawable/st_abertura.xml`, tema `Theme.StarkTech.Abertura` da `EntryActivity`), `drawable-nodpi/st_icone_app.png` (marca do topo, 192 px reduzido de `design/icone/app-icon.png`), `drawable-nodpi/st_logo.png` (rodapé da TV) | StarkTech Studio | — |

## 7. O que o Sobre mostra (F1 fase I, 31/10/2026)

`comum/SobreActivity`: ícone, nome, versão (do pacote), descrição e site; Créditos "Baseado no Nova Video Player · Apache-2.0"; avisos de terceiros, um por biblioteca (nome e licença; os 7 primeiros e "Ver todos"): Nova, FFmpeg, dav1d, libass, FreeType (com o crédito exigido pela FTL), Inter (OFL-1.1, com o copyright), Lucide, Opus, libmysofa, OpenSSL, FriBidi, HarfBuzz, fontconfig, libunibreak, libxml2, libpng, zlib, AudioCompress, libnativehelper, libtorrent, Boost, jcifs-ng; logo do TMDB (o do Nova) e o aviso "Este produto usa a API do TMDB, mas não é endossado nem certificado pelo TMDB."; direitos.

Falta para a F4 (também no `docs/backlog.md`): textos completos de cada licença (Apache-2.0, LGPL-2.1, OFL-1.1, ISC, BSD, MIT, FTL, BSL-1.0, zlib, PNG), oferta do código-fonte LGPL (FFmpeg, AudioCompress, FriBidi, motor), conferir se o `tmdb_banner.webp` é um logo aprovado pelo TMDB, versão do Lucide e confirmação da licença do jcifs-ng.
