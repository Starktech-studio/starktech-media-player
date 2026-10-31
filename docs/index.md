# Documentação — StarkTech Media Player

> Catálogo do `docs/`. Ler primeiro e abrir só o arquivo da tarefa. Padrão de pastas na skill `starktech-base`.
> Nunca gravar senha, token, chave ou dado de acesso — só dizer onde está guardado.

| Pasta / arquivo | O que contém |
|---|---|
| `arquitetura/interface.md` | Telas novas no código: pacotes, regra TV × tablet, medidas, recursos, navegação e rotas, dados simulados e o que ligar na F3 |
| `arquitetura/licencas.md` | Licença de cada pasta do Nova e de cada nativo do APK; obrigações para distribuir |
| `backlog.md` | Pendências fora do plano da fase em curso, separadas pela fase em que entram (hoje: F4) |
| `imagens/f0/tablet.png` | F0 — tela inicial do APK debug sem alteração no emulador de tablet (Tablet_API37, 2560×1600) |
| `imagens/f0/tv.png` | F0 — tela inicial do APK debug sem alteração no emulador de TV (TV_1080p_API36, 1920×1080) |
| `imagens/f1/a-tablet-inicio.png` | F1 fase A — tela inicial no tablet com pacote, nome e idioma novos (ainda a tela do Nova), em pt-BR com o sistema em inglês |
| `imagens/f1/a-tv-inicio.png` | F1 fase A — tela inicial na TV com pacote, nome e idioma novos (ainda a tela do Nova), em pt-BR com o sistema em inglês |
| `imagens/f1/b-tablet-launcher.png` | F1 fase B — gaveta de apps do tablet com o ícone provisório (triângulo de play azul sobre `#020617`) |
| `imagens/f1/b-tv-launcher.png` | F1 fase B — tela inicial da TV com o banner provisório (ícone + "StarkTech Media Player" em Inter) |
| `imagens/f1/c-tablet-navegacao-hd.png` | F1 fase C — tela principal nova do tablet: barra lateral com logo (Início ativo), marca e selo verde "HD externo"; conteúdo em construção |
| `imagens/f1/c-tablet-navegacao-sem-hd.png` | F1 fase C — a mesma tela com o selo vermelho "Sem HD ✕" (alternado por toque longo, só no APK debug) |
| `imagens/f1/c-tv-navegacao.png` | F1 fase C — tela principal nova da TV: barra do topo com a pílula Filmes ativa e o foco (borda branca) no conteúdo, em construção |
| `paginas/INTERFACE.md` | F1 — telas aprovadas (TV e tablet), comportamentos, regras e identidade visual |
| `paginas/telas-f1/` | Código HTML de cada tela do Claude Design (referência visual exata: cores, tamanhos, textos) |
| `operacional/build.md` | Ambiente (WSL, JDK, SDK, NDK), comando de build, saída do APK e teste no aparelho |
| `operacional/nova-manifest-v6.6.2.xml` | Manifesto da release v6.6.2 do Nova, com os commits fixos (cópia sem alteração) |
| `operacional/origem-nova.md` | Registro de origem: repositório, ramo e commit de cada pasta; roteiro para atualizar do Nova |

Criou, moveu ou apagou arquivo no `docs/`: atualizar este catálogo na mesma tarefa.
