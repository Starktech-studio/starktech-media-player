# Motor — StarkTech Media Player

> Lógica principal da F2, isolada da interface: capítulos do arquivo, edição dos dados, tipo por pasta e TMDB. Plano e decisões (D1 a D8): `PROJETOS\PLANOS\STARKTECH MEDIA PLAYER\003 - F2 Motor.md`. As telas seguem com os dados simulados (`docs/arquitetura/interface.md`); a ligação com a biblioteca real é da F3.

## 1. Onde está

| Parte | Arquivos |
|---|---|
| Pacote do app | `Video/src/main/java/br/com/starktechstudio/mediaplayer/motor/`: `Capitulos`, `BancoStarkTech`, `Edicoes`, `ImagemDoAparelho`, `ArquivosDoAparelho`, `Nomes`, `Pastas`, `Classificador`, `ChaveTmdb` |
| Testes de unidade | `Video/src/test/java/br/com/starktechstudio/mediaplayer/motor/` (JUnit; banco com Robolectric, na memória) |
| Motor nativo | `native/avos`: `public/avos_mp_metadata.h`, `avos_mr_metadata.h`, `Include/file_info_priv.h`, `avos_mp_priv.h`, `Source/avos_mp.c`, `avos_mp_video.c`, `avos_mp_audio.c`, `avos_mr.c`, `stream_parser_ffmpeg.c` |
| Ponte Java do Nova | `MediaLib/src/com/archos/medialib/IMediaPlayer.java`, `IMediaMetadataRetriever.java` (chaves dos capítulos) |
| Chaves | `MediaLib/build.gradle` (`chaveTmdb()`, `resValue`); `docs/operacional/build.md` §2 |
| Conferência no emulador (só debug) | `Video/src/debug/java/br/com/starktechstudio/mediaplayer/debug/CapitulosReceiver.java` |

## 2. Capítulos (etapa 1)

| Item | Como é |
|---|---|
| Origem | O motor já lia os capítulos do arquivo (MKV, MP4 e demais formatos do FFmpeg). Agora o player (`avos_mp_fillmetadata`, a partir do `STREAM`) e o leitor de dados sem tocar o vídeo (`_get_info_FFMPEG` → `FILE_INFO.chapters`, até 256) publicam |
| Chaves | `NB_CHAPTER` = 9003; capítulo `i`: 40000 + `i` × 3 + campo (0 título — vazio se o arquivo não dá nome —, 1 início em ms, 2 fim em ms). Iguais no player (`IMediaPlayer`, número) e no leitor (`IMediaMetadataRetriever`, texto) |
| Fim que falta | Início do capítulo seguinte ou duração do arquivo (motor e `Capitulos`) |
| `Capitulos` | `doPlayer(MediaMetadata)`, `doLeitor(IMediaMetadataRetriever)`; `naPosicao`, `irPara(n)`, `anterior` (D1: depois de 3 s volta ao início do capítulo; antes, vai ao anterior), `proximo`; 1,5 s de tolerância para o quadro-chave que cai antes do início. Sem capítulos: lista vazia, a tela fica com −10 s / +10 s |
| Conferir no emulador | APK debug: `adb shell am broadcast -n br.com.starktechstudio.mediaplayer/br.com.starktechstudio.mediaplayer.debug.CapitulosReceiver --es caminho <arquivo>` e `adb logcat -s StarkTechCapitulos` |

## 3. Banco do app (`starktech.db`, D2)

Separado do banco do Nova (que segue com a biblioteca e os dados do TMDB). `BancoStarkTech`, versão 1.

| Tabela | Colunas | O que guarda |
|---|---|---|
| `edicao` | `chave`, `campo`, `valor`, `alterado` (chave primária: `chave` + `campo`) | Um campo editado à mão por linha: TITULO, TITULO_ORIGINAL, TIPO, ANO, DURACAO, GENEROS, SINOPSE, CAPA, FUNDO. Sem linha = não editado |
| `pasta` | `caminho` (chave), `origem` (HD_EXTERNO / INTERNO), `tipo` (FILMES / SERIES / SHOWS / IGNORAR; nulo = "escolha o tipo"), `escolhido`, `alterado` | Pastas da biblioteca e o tipo de cada uma |

## 4. Edição dos dados (etapa 2)

| Regra | Como é |
|---|---|
| Chave do título | F2: o id dos dados simulados. F3: caminho do arquivo (filme, show) ou da pasta da série |
| Editado à mão | Campo salvo na `Edicoes`; `Edicoes.juntar(base, edicao)`: o editado sempre vale sobre o original e o resultado do TMDB — a busca no TMDB nunca sobrescreve um campo editado (nem a capa e o fundo) |
| Restaurar original | Apaga toda a edição do título e as imagens do aparelho que só ele usava |
| Capa e fundo | Referência: `cor:#RRGGBB` (opções simuladas da F1), `arquivo:<caminho>` (imagem do aparelho); na F3, a imagem escolhida do TMDB |
| Imagem do aparelho (D3, D7) | JPG, PNG ou WebP pelos primeiros bytes, até 15 MB, qualquer proporção. Copiada para `files/imagens/` (`ImagemDoAparelho`); dimensões lidas sem abrir a imagem inteira. Tablet: seletor do sistema; TV (ou tablet sem seletor): `comum/NavegadorImagensActivity` (armazenamentos de verdade, só imagens; pede a permissão de ler imagens). Imagem trocada, restaurada ou copiada e não salva sai da pasta do app; arquivo fora dela nunca é apagado |

## 5. Tipo por pasta (etapa 3)

| Regra | Como é |
|---|---|
| Nomes (D4) | Sem diferenciar maiúscula, minúscula e acento. Filmes: Filmes, Filme, Movies, Films · Séries: Séries, Série, Series, TV Shows · Shows: Shows, Show, Concerts, Concertos. Outro nome: sem tipo ("escolha o tipo") |
| Tipo escolhido | Vale mais que o nome; adicionar de novo não troca |
| Pasta do arquivo | A cadastrada mais próxima acima (`Pastas.pastaDoArquivo`) |
| Filmes | "Nome (2012).mkv"; o ano também solto ("Nome.2012.1080p") ou no nome da pasta do filme; sem ano, o título é o nome |
| Séries | "Nome / Temporada N / 01.mkv". Temporada pela pasta: "Temporada N", "Temp N", "Season N", "T01", "S01". Episódio pelo arquivo: "01" (só o número), "S01E01", "1x01", "E01". "S01E01" e "1x01" trazem a temporada (valem sem a pasta dela; direto na pasta Séries, o nome da série vem antes do código). Ano no nome da pasta da série: "Maré Alta (2021)" |
| Shows | "Artista - Show (2019).mkv"; ano opcional; sem " - ", só o título |
| Não identificados | Com o motivo: pasta sem tipo, fora do padrão, sem temporada, sem episódio, pastas demais. Pastas Ignorar e arquivos que não são vídeo ficam de fora |
| Varredura | `Classificador.varrer(pastas)`: percorre as pastas (menos Ignorar), subpasta cadastrada fica com o tipo dela, ocultas e laços de link fora; `montarSeries` (temporadas e episódios em ordem, séries em ordem alfabética) e `naoIdentificados` |

## 6. TMDB e chaves (etapa 4)

| Item | Como é |
|---|---|
| Chave do TMDB | Fora do repositório: linha `starktech.tmdb.chave=` do `~/.gradle/gradle.properties` do WSL; entra no APK pelo `resValue` do `MediaLib/build.gradle`. Sem ela, fica vazia: o TMDB responde 401 e os scrapers do Nova tratam como `AUTH_ERROR`; `ChaveTmdb.temChave` deixa a busca avisar antes (F3) |
| Chaves do Nova | Fora do código: `MediaLib/res/values/donottranslate.xml` apagado, valores de exemplo trocados nos comentários e nos testes. Ficam só no histórico do commit da F0 (D6) |
| Trakt e OpenSubtitles | Desligados, sem chave (D5); as entradas são só das telas do Nova |
| Testes do Nova que chamam o TMDB | Leem a chave do Gradle e são pulados sem ela |

## 7. O que a F3 liga

| Parte | Como |
|---|---|
| Player | `Capitulos.doPlayer` com os metadados do player do Nova: lista, ir para, anterior / próximo |
| Detalhe | `Capitulos.doLeitor` para a lista de capítulos sem tocar o vídeo |
| Biblioteca | `Pastas` na aba Pastas; `Classificador.varrer` em "Atualizar biblioteca"; séries montadas e não identificados nas telas |
| Edição | Chave do título pelo caminho; capa e fundo do aparelho também fora do Editar |
| TMDB | Busca de verdade, com o aviso de sem chave |
