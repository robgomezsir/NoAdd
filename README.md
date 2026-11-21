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

## Tamanho medido (release com R8 + shrinkResources)

| Artefato | Bytes | Aprox. |
| --- | ---: | ---: |
| `app-release.apk` (assinado) | 78.769 | 77 KiB |
| `app-release.aab` (assinado) | 105.159 | 103 KiB |
| `app-debug.apk` (referência) | 960.330 | 938 KiB |

Meta do projeto: APK/AAB abaixo de 300 KB — atendida com folga. O maior bloco do
APK é o `classes.dex` (~58 KB descomprimido), seguido do `resources.arsc`
(~16 KB) e dos metadados do `kotlin-stdlib` (`kotlin/*.kotlin_builtins`).

Idiomas embarcados: pt-BR (padrão), en, es (`androidResources.localeFilters`).

## Sistema visual — "Abissal"

Vidro sobre azul-marinho. Nada de imagens: o fundo é um gradiente vertical com
dois brilhos radiais desenhados por shaders (`AuroraView.kt`), e o vidro é
feito de preenchimento translúcido + fio de 1dp + raio de 26dp.

- **Cores** (tokens de tema em `res/values/attrs.xml`, definidos nas paletas
  `NoAdd.Palette.Light` (Geada), `NoAdd.Palette.Dark` (Abissal) e
  `NoAdd.Palette.Auto`): `canvasTop/Bottom` (azul-marinho), `glowPrimary`
  (azul), `glowSecondary` (ciano), `glass`, `glassStrong`, `stroke`,
  `hairline`, `ink`, `inkMuted`, `accent` (ciano gelo no escuro, azul profundo
  no claro), `accentInk` e `ripple`.
- **Tipografia**: sistema em `sans-serif-medium` para wordmark, rótulos e
  número; rótulos em caixa alta com `letterSpacing` 0.16 e o wordmark com 0.34.
  Nenhuma fonte embarcada de propósito: uma família display acrescentaria
  ~100 KB a um app de 78 KB.
- **Composição**: uma única folha de vidro com o número como herói, ações em
  cápsulas (uma sólida de destaque e duas de vidro) e muito espaço negativo.
- **Movimento**: uma entrada só — a folha sobe 18dp e aparece em 260ms.
- **Limite honesto**: é vidro *simulado* (translucidez + realce), não
  desfoque real de fundo. Blur de backdrop exigiria `RenderEffect`/blur de
  janela (API 31+) por painel, o que sai do orçamento de simplicidade.

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

## Estrutura

```
app/src/main/
  AndroidManifest.xml            zero permissões; queries do WhatsApp; alias tel: desativado
  kotlin/com/noadd/app/
    AuroraView.kt                fundo aurora azul-marinho (shaders, sem imagens)
    MainActivity.kt              tela principal, intents, configurações, sobre
    AgendaActivity.kt            recentes/fixados, importar/exportar
    Phone.kt                     normalização, 9º dígito BR, validação branda, extração
    Countries.kt                 tabela embutida de países (bandeira derivada do ISO)
    ChatIntents.kt               link wa.me, compartilhar, copiar, pacotes instalados
    Prefs.kt                     SharedPreferences + org.json (sem banco, sem rede)
    Shortcuts.kt                 atalho fixo via ShortcutManager
    Ui.kt                        tema e insets do sistema
  res/layout/                    activity_main, activity_agenda, item_entry, dialog_settings
  res/menu/                      main, agenda
  res/xml/                       shortcuts, backup_rules, data_extraction_rules
  res/drawable|mipmap-anydpi*/   vidro (bg_*, btn_*), ícone adaptativo + monochrome
  res/values{,-en,-es}/          strings; values-night e values-v29 para tema
  res/values/attrs.xml           tokens de cor do vidro (13 atributos de tema)
```

## Decisões e limites conhecidos

- **Tema**: `Theme.DeviceDefault.DayNight` (API 29+) com variantes manuais para
  API 24–28. O usuário escolhe Sistema/Claro/Escuro. Paleta Material You
  dinâmica completa não é possível sem Material Components, o que estouraria o
  orçamento de tamanho; o app usa o acento do sistema.
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
- **Verificação**: este ambiente não tem aparelho nem emulador. A compilação, as
  permissões do APK e o algoritmo de normalização foram verificados; o
  comportamento de UI, atalhos e intents precisa ser testado em um celular
  (`TESTES.md`).
