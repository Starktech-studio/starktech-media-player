# DADOS — STARKTECH MEDIA PLAYER

StarkTech · Apps · Criado em 30/10/2026

Dados oficiais do app: identificação, caminhos, GitHub e textos. Base para o setup (F0) e, depois, para o site da StarkTech (página `/apps`). Mudou uma decisão, muda aqui e na cópia da raiz do código.

---

## 1. Identificação

| Campo | Valor |
|---|---|
| Categoria | Vídeo e reprodutores (sugestão) |
| Desenvolvedor | StarkTech Studio |
| Idioma | Só pt-BR |
| Nome | StarkTech Media Player |
| Pacote | `br.com.starktechstudio.mediaplayer` |
| Plataforma | Android — TV e tablet (celular vem junto, mesmo APK) |
| Preço | Gratuito |
| Distribuição | APK baixado pelo site (sem loja) |
| Situação | Abertura — F0 a iniciar |
| Versão | 0.1.0 (versionCode 1) |

---

## 2. O que é

Player e biblioteca de mídia. Lê o HD ou disco externo, separa filmes, séries e shows com capa, sinopse e dados técnicos, e reproduz MKV e MP4 em 4K. Base: fork do Nova Video Player, com interface refeita.

| Função | Detalhe |
|---|---|
| Biblioteca | Pastas locais e USB, separadas em filmes, séries e shows |
| Capítulos | Anterior/próximo capítulo (controle remoto na TV, toque no tablet) e lista com o nome de cada capítulo do MKV |
| Dados do título | Capa, sinopse, elenco, temporadas e episódios — TMDB |
| Dados técnicos | Resolução, HDR, codec, faixas de áudio e legendas |
| Edição manual | Corrigir título, ano, sinopse, capa e episódio quando o TMDB errar |
| Interface | Limpa e moderna, com comportamento próprio na TV (foco e controle remoto) e no tablet (toque) |

---

## 3. Decisões (30/10/2026)

- Base: fork do Nova Video Player (Apache 2.0), mantendo motor e biblioteca e refazendo a interface.
- Distribuição: APK pelo site, sem Google Play e sem `PREENCHIMENTO` de loja por enquanto.
- Escopo da versão 1: interface refeita + capítulos + edição manual dos dados.
- Gratuito.
- Idioma: só pt-BR.
- Sem login e sem Firebase: app local; internet só para buscar dados no TMDB.
- Versão PC: depois da versão 1 (ver planejamento).

---

## 4. Caminhos

| Item | Onde |
|---|---|
| Código | `D:\Dev\starktech-media-player` (criada em 30/10/2026) |
| Gestão | `OneDrive\Trabalho\Starktech\PROJETOS\APLICAÇÕES\STARKTECH MEDIA PLAYER\` |
| GitHub | `https://github.com/Starktech-studio/starktech-media-player` — privado, criado em 30/10/2026 |
| Planejamento | `Starktech\PLANEJAMENTO\STARKTECH MEDIA PLAYER\` (P-002) |
| Planos do Claude Code | `Starktech\PROJETOS\PLANOS\STARKTECH MEDIA PLAYER\` |
| Site | Card na página `/apps` do starktechstudio.com.br, com o link do APK |

---

## 5. Stack

| Parte | Stack |
|---|---|
| Base | Nova Video Player — sub-repositórios `aos-Video` (interface), `aos-MediaLib` (biblioteca e TMDB), `aos-FileCoreLibrary` (arquivos), `aos-avos` (motor nativo com FFmpeg) e nativos do manifesto `default.xml` |
| Linguagem | Java (interface do Nova) + C nativo (motor) |
| TV | Leanback (já no Nova) |
| SDK | minSdk 23 · targetSdk 36 (valores do Nova em 30/10/2026) |

Versões e ferramentas (Android Studio, NDK, `repo`, WSL): confirmar na F0, antes de instalar.

---

## 6. GitHub

Repositório privado único com o código do Nova copiado (fork pelo GitHub não serve: fork de repositório público não pode ser privado). Manter `LICENSE` e avisos de cada sub-repositório.

Nome do repositório:
```
starktech-media-player
```

Descrição:
```
StarkTech Media Player — player e biblioteca de mídia para Android TV e tablet, com capas, sinopse, capítulos e edição dos dados. Baseado no Nova Video Player.
```

Visibilidade:
```
Private
```

URL do remote (HTTPS):
```
https://github.com/Starktech-studio/starktech-media-player.git
```

Mensagem do primeiro commit:
```
Setup inicial: README, CLAUDE.md e .gitignore
```

README.md inicial:
```md
# StarkTech Media Player

Player e biblioteca de mídia para Android TV e tablet: filmes, séries e shows com capa, sinopse, dados técnicos e capítulos.

## Situação
Em desenvolvimento — F0 (setup).

## Base
Fork do Nova Video Player (Apache 2.0) — https://github.com/nova-video-player/aos-AVP

---
© StarkTech Studio
```

---

## 7. TMDB

| Item | Situação |
|---|---|
| Chave da API | Própria — a criar. Guardada no gerenciador de senhas; no código, fora do Git |
| Licença | Uso comercial exige licença do TMDB — confirmar se app gratuito da empresa se enquadra |
| Créditos | Logo e aviso do TMDB na tela Sobre (exigência dos termos) |

---

## 8. Publicação no site (/apps)

Cadastro no `/admin-starktech` → Apps → Novo app. Conferir na F5 se o formulário tem campo para link de APK.

Nome:
```
StarkTech Media Player
```

Endereço no site (depois de `/apps/`):
```
media-player
```

Plataforma:
```
Android TV · Tablet · Gratuito
```

Resumo (sugestão):
```
Sua biblioteca de filmes e séries na TV e no tablet, com capas, sinopse, capítulos e reprodução de MKV e MP4 em 4K. Gratuito.
```

Destaques: definir na F5, só com o que existir na versão publicada.

---

## 9. Ícones e imagens

Padrão da família (`starktech-design`): escudo + lobo do Conversor, cor e selo próprios. Pacote Android: ícones, banner da TV 1280×720 e tela de abertura em `ÍCONES/android/`.

| Item | Situação |
|---|---|
| Cor do escudo | Preto, com linhas e lobo em branco (escolhido em 31/10/2026) |
| Selo | Redondo, com triângulo de play |

---

## 10. A definir

- Licença de cada sub-repositório e dos nativos.
