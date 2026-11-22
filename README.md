# NoAdd

App Android nativo que abre uma conversa no WhatsApp com qualquer número, sem
salvar contato. Sem anúncios, sem analytics, sem permissões e sem bibliotecas de
terceiros.

- `applicationId`: `com.noadd.app`
- `minSdk 24` / `targetSdk 36` / `compileSdk 36`
- Kotlin + AGP 8.13.0 + Gradle 8.13, JDK 17
- **Zero `<uses-permission>`** no manifest
- Nenhuma dependência de runtime (só o `kotlin-stdlib` que o plugin adiciona)

> Este app não é afiliado nem endossado pelo WhatsApp Inc.

## Download

APK assinado na release mais recente:
<https://github.com/robgomezsir/NoAdd/releases/latest> (`app-release.apk`, ~196 KB,
Android 7.0+).

## Tamanho medido (release com R8 + shrinkResources)

| Artefato | Bytes | Aprox. |
| --- | ---: | ---: |
| `app-release.apk` (assinado) | 200.589 | 196 KiB |
| `app-release.aab` (assinado) | 255.707 | 250 KiB |
| `app-debug.apk` (referência) | 1.100.414 | 1,05 MiB |

Meta do projeto: APK/AAB abaixo de 300 KB — atendida com folga. O maior bloco do
APK é o `classes.dex` (126.144 bytes descomprimidos), seguido dos metadados do
`kotlin-stdlib` e do `resources.arsc` (50.660 bytes). O ícone em PNG soma ~56 KB
no fonte (12 arquivos: camada adaptativa, monocromática e os mipmaps legados) e o
lote (três passos, leitor do arquivo de treinamento e modelos) acrescentou ~35 KB
ao `classes.dex` e ~30 KB ao `resources.arsc` (novas telas e strings em três
idiomas).

Idiomas embarcados: pt-BR (padrão), en, es (`androidResources.localeFilters`).

## Sistema visual — "Abissal"

Vidro sobre azul-marinho, tudo desenhado em código: o fundo é um gradiente
vertical com dois brilhos radiais por shaders (`AuroraView.kt`) e o vidro é
preenchimento translúcido + fio de 1dp + raio de 26dp. A **única imagem** do app
é o ícone, derivado da arte do balão (ver abaixo).

- **Sem faixa de título**: as três paletas de tema definem
  `android:windowActionBar=false` e `android:windowNoTitle=true`, então nem a
  barra do sistema nem um wordmark dentro do conteúdo mostram "NoAdd". A tela
  abre direto no painel.
- **Menu dentro do painel**: uma cápsula de vidro de 48dp com o ícone de três
  linhas (`ic_menu.xml`) no topo do painel abre, por `PopupMenu`, Agenda,
  Configurações e Sobre. Na Agenda o mesmo botão fica ao lado do título e abre
  Importar, Exportar e Apagar tudo.
- **Conteúdo centralizado**: a `ScrollView` usa `fillViewport` e o container tem
  `gravity="center_vertical"`, com `Ui.applySymmetricSystemBarsPadding`
  equalizando o inset superior e o inferior para o painel ficar no centro exato
  da tela (medido em 1199,5 px de uma tela de 2400 px no moto g56 5G). Em telas
  baixas ou com o teclado aberto o conteúdo rola normalmente.
- **Ações dentro da folha de vidro**: "Abrir conversa" (cápsula sólida de
  destaque), "Compartilhar link" / "Copiar link", "Ligar" / "Criar atalho", todas
  com 56dp de altura e feedback de toque.
- **Cores** (tokens de tema em `res/values/attrs.xml`, definidos nas paletas
  `NoAdd.Palette.Light` (Geada), `NoAdd.Palette.Dark` (Abissal) e
  `NoAdd.Palette.Auto`): `canvasTop/Bottom` (azul-marinho), `glowPrimary`
  (azul), `glowSecondary` (ciano), `glass`, `glassStrong`, `stroke`,
  `hairline`, `ink`, `inkMuted`, `accent` (ciano gelo no escuro, azul profundo
  no claro), `accentInk` e `ripple`.
- **Tipografia**: fonte do sistema em `sans-serif-medium` para rótulos e número;
  rótulos em caixa alta com `letterSpacing` 0.16. Nenhuma fonte embarcada de
  propósito: uma família display acrescentaria ~100 KB a um app de ~100 KB.
- **Movimento**: uma entrada só — a folha sobe 18dp e aparece em 260ms.
- **Limite honesto**: é vidro *simulado* (translucidez + realce), não desfoque
  real de fundo. Blur de backdrop exigiria `RenderEffect`/blur de janela
  (API 31+) por painel, o que sai do orçamento de simplicidade.

### Ícone

Derivado da arte do balão — balão verde-limão com a ponta, anel vermelho de
proibido e contato creme. O desenho é recortado pelo retângulo da arte (sem a
margem branca do arquivo original), reduzido com Lanczos e paletizado em 96
cores, o que mantém o traço nítido em 48px por poucos KB:

- `res/drawable-xxxhdpi/ic_launcher_foreground.png` (432px, arte a 60% do lado,
  dentro da zona segura) sobre o fundo branco (`icon_bg`) do ícone adaptativo;
- `res/drawable-xxxhdpi/ic_launcher_monochrome.png` para ícones temáticos: o
  balão sólido em branco, com o contato e o corte vazados (alfa);
- `res/mipmap-mdpi…xxxhdpi/ic_launcher{,_round}.png` (48–192px) para Android 7,
  com a arte a 80% do lado sobre branco;
- `ic_shortcut.xml` (vetorial) na mesma paleta, para o atalho estático.

A geração é reproduzível: `python arte/gerar_icones.py` relê
`arte/icone.webp` (o desenho de origem) e grava os 12 PNGs.

## Requisitos

- JDK 17
- Android SDK com `platform-tools`, `platforms;android-36`, `build-tools;36.0.0`.
  Neste ambiente o SDK é gerenciado pelo CLI `android`:

```bash
android sdk install "platform-tools" "platforms;android-36" "build-tools;36.0.0"
```

O caminho do SDK fica em `local.properties` (`sdk.dir=...`), que não vai para o git.

## Compilar

```bash
./gradlew :app:assembleDebug      # APK de debug, instala e testa
./gradlew :app:assembleRelease    # APK de release assinado (instalável)
./gradlew :app:bundleRelease      # AAB assinado, para a Play Store
./gradlew :app:installDebug       # com um aparelho conectado via adb
```

Saídas:

- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release.apk`
- `app/build/outputs/bundle/release/app-release.aab`

## Assinatura

O release já sai assinado neste ambiente. A chave está em `noadd-release.jks`
(alias `noadd`, RSA 4096, validade 10.000 dias) e as senhas em
`keystore.properties`; os dois arquivos estão no `.gitignore`.

> **Faça backup de `noadd-release.jks` + `keystore.properties`.** Sem eles não é
> possível publicar atualizações com a mesma identidade — atualização assinada
> com outra chave é recusada pelo Android e pela Play Store.

```bash
./gradlew :app:assembleRelease   # app-release.apk assinado (instalável no aparelho)
./gradlew :app:bundleRelease     # app-release.aab assinado (upload na Play Store)
"$ANDROID_HOME/build-tools/36.0.0/apksigner.bat" verify -v --print-certs \
  app/build/outputs/apk/release/app-release.apk
```

- O APK usa **APK Signature Scheme v2**, suficiente para `minSdk 24` (o v1/JAR só
  é necessário abaixo do Android 7).
- O `.aab` **não** é instalável no aparelho: para teste manual use o
  `app-release.apk`.
- Se o aparelho já tem uma versão assinada com outra chave (por exemplo o
  `app-debug.apk`), desinstale antes de instalar: assinaturas diferentes não se
  substituem.
- Para trocar de chave, altere `storeFile`/`storePassword`/`keyAlias`/
  `keyPassword` em `keystore.properties`; o build lê esse arquivo.
- Na Play Store, suba o `.aab` com **Play App Signing** ativado.

## Lote (envio semi-manual)

O chip **Lote** na tela principal abre três passos: importar a lista, escrever a
mensagem e percorrer a fila. Quem toca em enviar é sempre o usuário, no WhatsApp:
o app só monta o link e abre uma conversa por vez.

- **Lista**: CSV/TXT (`Nome;Sobrenome;Telefone`, `Nome;Telefone` ou só telefones),
  colagem do Excel, contato da agenda ou digitação. Números lidos vão para a
  fila; os que não puderam ser lidos ficam em "Para revisar".
- **Nome e sobrenome**: os cabeçalhos `Nome` e `Sobrenome` são lidos em colunas
  separadas; uma coluna única `Nome` com o nome completo é dividida no último
  espaço. Cada contato guarda as duas partes (exibidas como `fullName`).
- **Saudação padrão**: na primeira abertura a mensagem já vem com
  `Olá, {nome}, tudo bem?` (traduzida em en/es). Se o usuário apagar o texto, ele
  continua vazio — a saudação **não** volta por cima da escolha dele.
  `{nome}` vira o nome completo e `{primeiro_nome}` o primeiro nome de cada
  contato; sem nome, a vírgula sobrando sai ("Olá, tudo bem?").
- **Dados do treinamento (segundo arquivo)**: o botão "Carregar dados do
  treinamento", abaixo do campo de mensagem, abre o seletor do sistema
  (`ACTION_OPEN_DOCUMENT`, nenhuma permissão) e lê um CSV/TXT com
  `PRODUTO;CARGO;ENDERECO;HORA;DATA;PERIODO;INSTRUTOR`. O separador (`;`, `,` ou
  tabulação) é detectado, o BOM UTF-8 é ignorado e os cabeçalhos casam sem
  diferenciar maiúsculas/acentos e em qualquer ordem (`ENDERECO` = `endereço`).
  O app mostra um resumo para conferência — campos vazios destacados, com aviso
  e opção de continuar —, preenche a mensagem com o modelo do RH (emojis e
  quebras de linha preservados, só `{nome}` continua variável) e guarda o texto
  como **"Modelo de treinamento"** na lista de modelos. `DATA` e `HORA` ficam
  como estão no arquivo; um serial de data do Excel vira `dd/MM/aaaa`. Nesta
  versão o arquivo deve ter **uma linha de dados**; com várias, a primeira é
  usada e o app avisa. Exemplo em [`exemplos/treinamento.csv`](exemplos/treinamento.csv).
- **Prévia**: mostra a mensagem final do contato atual, com "Ver exemplo de
  outro contato", contador de caracteres, tamanho do link e aviso quando o link
  passa de 2.000 caracteres. Variável desconhecida fica no texto e é sinalizada.
- **Fila**: status Pendente / Aberto / Enviado / Pulado, limite por sessão,
  lembrete por intervalo e "avançar ao voltar" (opcional). Contatos, revisão,
  progresso e a turma carregada ficam em SharedPreferences (`noadd_batch`) para
  retomar depois.
- **Sem permissões**: importar e exportar usam o Storage Access Framework, e
  abrir a conversa usa `https://wa.me/<DDI><número>?text=<mensagem>` com
  URL-encode UTF-8 — emojis, acentos e `%0A` chegam íntegros.

## Estrutura

```
app/src/main/
  AndroidManifest.xml            zero permissões; queries do WhatsApp; alias tel: desativado
  kotlin/com/noadd/app/
    AuroraView.kt                fundo aurora azul-marinho (shaders, sem imagens)
    MainActivity.kt              tela principal, menu no painel, Ligar, intents, configurações, sobre
    AgendaActivity.kt            recentes/fixados, importar/exportar
    Phone.kt                     normalização, 9º dígito BR, validação branda, extração
    Countries.kt                 tabela embutida de países (bandeira derivada do ISO)
    ChatIntents.kt               link wa.me, compartilhar, copiar, pacotes instalados
    Batch.kt                     lote: leitura da lista, nomes, personalização, relatório
    BatchActivity.kt             três passos do lote, dados do treinamento, modelos e ajustes
    BatchStore.kt                persistência do lote (SharedPreferences + JSON)
    Training.kt                  leitura do arquivo de treinamento e modelo do RH
    Prefs.kt                     SharedPreferences + org.json (sem banco, sem rede)
    Shortcuts.kt                 atalho fixo via ShortcutManager
    Ui.kt                        tema, insets e padding simétrico das barras
  res/layout/                    activity_main, activity_agenda, activity_batch,
                                 item_entry, item_batch_*, dialog_settings, dialog_batch_settings
  res/menu/                      main, agenda, batch (inflados pelo PopupMenu)
  exemplos/treinamento.csv      modelo do segundo arquivo (dados do treinamento)
  res/xml/                       shortcuts, backup_rules, data_extraction_rules
  res/drawable/                  vidro (bg_*, btn_*) e ícones vetoriais (ic_menu, ic_shortcut)
  res/drawable-xxxhdpi/          ícone adaptativo em PNG (arte do balão) + monocromático
  res/mipmap-mdpi…xxxhdpi/       ícones legados 48–192px (arte original com o balão)
  res/mipmap-anydpi-v26/         ícone adaptativo (fundo branco + camada monocromática)
  res/values{,-en,-es}/          strings; values-night e values-v29 para tema
  res/values/attrs.xml           tokens de cor do vidro (13 atributos de tema)
  res/values/colors.xml          cor de fundo do ícone (branco)
```

## Decisões e limites conhecidos

- **Tema**: `Theme.DeviceDefault.DayNight` (API 29+) com variantes manuais para
  API 24–28, sem ActionBar. O usuário escolhe Sistema/Claro/Escuro. Paleta
  Material You dinâmica completa não é possível sem Material Components, o que
  estouraria o orçamento de tamanho; o app usa o acento do sistema.
- **Ligar**: o botão abre um diálogo com **WhatsApp** e **Telefone**. O WhatsApp
  não expõe intent público de chamada de voz para um número que não está na
  agenda (o `whatsapp://call` do app é interno e a chamada só é disparável pelo
  próprio WhatsApp ou por um contato salvo com o mimetype
  `vnd.android.cursor.item/vnd.com.whatsapp.voip.call`, o que exigiria
  `READ_CONTACTS`). Por isso a opção WhatsApp abre a conversa, onde ficam os
  botões de chamada de voz e vídeo. A opção Telefone usa `ACTION_DIAL`, que
  apenas abre o discador com o número — discar não exige permissão.
- **Sem permissões**: atalhos usam `ShortcutManager.requestPinShortcut` (não
  precisa de `INSTALL_SHORTCUT`); copiar usa `ClipboardManager`; exportar `.txt`
  usa `ACTION_CREATE_DOCUMENT`; importar é por texto colado.
- **Privacidade**: `allowBackup=false` e regras que excluem SharedPreferences de
  backup em nuvem e de transferência entre aparelhos.
- **Nome**: "NoAdd" (lê-se "no add": sem anúncios), sem referência a marcas de
  terceiros. O pacote é `com.noadd.app`. Para trocar nome ou pacote depois,
  altere `app_name`, `applicationId`, `namespace`, as declarações `package` em
  `app/src/main/kotlin/com/noadd/app/` e os `targetClass`/`targetPackage` de
  `res/xml/shortcuts.xml`.
- **Apoiar o projeto**: o item só aparece na tela Sobre quando
  `R.string.support_url` tiver uma URL. A política de pagamentos do Google Play
  só permite link externo em doação/tip genuíno (100% para o criador, sem
  benefício digital); qualquer vantagem dentro do app exigiria Play Billing.
- **Verificação**: o app foi instalado e exercitado em um moto g56 5G via
  depuração sem fio (`adb pair` + `adb connect`), junto com as checagens
  estáticas de build, permissões, assinatura e tamanho. O que foi conferido —
  e o que continua pendente — está em [`TESTES.md`](TESTES.md).

## Licença

Distribuído sob a licença **MIT** — veja [`LICENSE`](LICENSE). Em resumo: você
pode usar, copiar, modificar, publicar, distribuir, sublicenciar e vender o
software, desde que mantenha o aviso de copyright e a permissão em todas as
cópias ou partes substanciais. O software vem **sem garantias**.

O nome e o ícone do NoAdd identificam este projeto; a licença cobre o código, não
implica endosso e não concede direitos sobre marcas de terceiros (WhatsApp é
marca da Meta Platforms, Inc.).
