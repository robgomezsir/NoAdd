# Testes — NoAdd

## Verificado neste build (checagens estáticas)

- [x] `:app:assembleDebug`, `:app:assembleRelease` e `:app:bundleRelease` compilam
      sem erro (R8 e lint vital do release inclusive).
- [x] APK de release **sem nenhuma permissão** (`aapt2 dump permissions` só
      devolve o nome do pacote).
- [x] `versionCode 1`, `versionName 1.0`, `targetSdkVersion 36`, rótulo "NoAdd",
      idiomas `--_--`, `en`, `es`.
- [x] Assinatura de release conferida com `apksigner verify`: APK Signature
      Scheme v2 válido, certificado `CN=NoAdd`, RSA 4096.
- [x] Tamanho: APK assinado 101.533 bytes, AAB assinado 129.634 bytes,
      `app-debug.apk` 983.984 bytes. O maior bloco do APK é o `classes.dex`
      (63.084 bytes), seguido de `kotlin/*.kotlin_builtins` (29.399) e
      `resources.arsc` (20.228); o ícone em PNG soma ~24 KB no fonte.
- [x] Todos os 13 tokens de cor do vidro existem em `attrs.xml` e estão
      definidos nas paletas Light, Dark e Auto.
- [x] Algoritmo de número executado em JVM (22 casos, todos OK): DDI com/sem `+`,
      parênteses/hífens/espaços, `00` internacional, zero de tronco, número já com
      DDI, 9º dígito em celular de 8 dígitos, fixo de 10 dígitos, Portugal e EUA,
      extração de texto compartilhado e de link `wa.me`, formatação e país do DDI.

## Verificado no aparelho — moto g56 5G (depuração sem fio)

Conexão: `adb pair` + `adb connect 192.168.15.5:39563`; APK de release assinado
instalado por cima da versão existente (dados preservados).

- [x] **Sem faixa de título**: nenhuma das duas telas mostra a barra com "NoAdd"
      (`windowActionBar=false` / `windowNoTitle=true`) nem o wordmark no conteúdo.
- [x] **Menu dentro do painel**: a cápsula de vidro no topo do `glassSheet` abre
      Agenda, Configurações e Sobre (itens lidos por `uiautomator dump`).
- [x] **Agenda**: título com o botão de menu ao lado, lista com o número recente,
      menu com Importar / Exportar / Apagar tudo; voltar retorna à tela principal.
- [x] **Conteúdo centralizado**: `glassSheet` em `[59,498][1021,1901]` numa tela
      de 1080x2400 → centro em 1199,5 px (centro exato: 1200).
- [x] **Botões dentro do painel**: "Abrir conversa", "Compartilhar link",
      "Copiar link", "Ligar" e "Criar atalho" na mesma folha de vidro.
- [x] **Criar atalho** abre o diálogo "Nome do atalho" com OK/Cancelar.
- [x] **Ligar → diálogo "Ligar por"** com WhatsApp e Telefone.
      - "Telefone" abre `com.google.android.dialer` com `5571991452913` preenchido
        (nenhuma chamada feita).
      - "WhatsApp" abre `com.whatsapp/.Conversation` na conversa do número, sem
        mensagem enviada.
- [x] **Ícone novo** (arte do balão verde com anel vermelho) visível na gaveta de
      apps, sem artefatos.
- [x] Nenhum crash do `com.noadd.app` no `logcat` durante a sessão.

## Pendentes no aparelho

### Número

- [ ] Digitar `11 99999-9999` (país Brasil) → abre com `+55 11 99999-9999`.
- [ ] Colar `+55 (11) 99999-9999` → mesmo resultado, sem duplicar o 55.
- [ ] Colar `5511999999999` → mesmo resultado.
- [ ] Digitar `(11) 9999-9999` (celular antigo, 8 dígitos) → abre com o 9º dígito inserido.
- [ ] Digitar `011 99999-9999` → zero de tronco removido.
- [ ] Fixo `11 3333-4444` → não ganha 9º dígito.
- [ ] Número curto (`119999`) → aparece o aviso “parece incompleto”, mas ainda abre.
- [ ] Trocar país para Portugal e testar `912 345 678` → `+351 912 345 678`.
- [ ] Campo vazio + qualquer ação (incluindo Ligar) → aviso “Digite um número primeiro”.

### Ligar

- [ ] Com WhatsApp e WhatsApp Business instalados → diálogo de pacote com as três opções.
- [ ] Sem nenhum WhatsApp instalado → diálogo “WhatsApp não encontrado” com
      opção de abrir no navegador, sem crash.
- [ ] Sem app de telefone no aparelho → toast “Nenhum app de telefone encontrado”.
- [ ] Conferir que a opção WhatsApp abre a conversa e que o botão de chamada do
      WhatsApp funciona a partir dela (a chamada de voz não é disparável por
      intent de terceiros).

### Mensagem e links

- [ ] Mensagem longa com acentos e emoji (ex.: `Olá! 😀 Testando — ação, coração, 100%`) chega íntegra no campo da conversa.
- [ ] Mensagem vazia → link sem `?text=`.
- [ ] "Copiar link" → colar em outro app e conferir o `https://wa.me/...`.
- [ ] "Compartilhar link" → menu de compartilhamento do sistema abre com o link.

### WhatsApp instalado / ausente

- [ ] Com só o WhatsApp comum: abre direto (ou pelo seletor padrão do sistema).
- [ ] Com WhatsApp e Business instalados: diálogo com as três opções;
      "Padrão do sistema" abre o seletor do Android.
- [ ] Sem nenhum dos dois: diálogo “WhatsApp não encontrado” com opção de abrir no
      navegador, sem crash.
- [ ] Desinstalar/desativar o WhatsApp e repetir a abertura.

### Entradas externas

- [ ] Compartilhar um texto com número de outro app (ex.: Notas) → número preenchido.
- [ ] Selecionar um número em um app e usar "Selecionar texto" → número preenchido.
- [ ] Configurações → ativar "Abrir links de telefone (tel:)" → o NoAdd aparece
      como opção para um link `tel:`; abrir preenche e já abre a conversa.
- [ ] Desativar a opção → some das opções de `tel:`.

### Atalhos

- [ ] "Criar atalho" com apelido → launcher pede confirmação, atalho aparece.
- [ ] Abrir o atalho → conversa daquele número abre direto.
- [ ] Atalho estático "Abrir chat" (toque longo no ícone) → abre no campo de número.
- [ ] Criar o mesmo atalho duas vezes → mensagem de que já existe.
- [ ] Testar em Android 7 (sem atalhos fixos) → mensagem clara, sem crash.

### Agenda e configurações

- [ ] Recentes guardam os últimos números e respeitam o limite (mude para 5 e confira).
- [ ] Fixar com apelido, ver o item no topo, abrir por ele.
- [ ] Desafixar e remover.
- [ ] "Salvar números recentes" desligado → nada novo entra na lista.
- [ ] "Manter o código do país" desligado → após abrir, campo limpo e país volta ao Brasil.
- [ ] "Abrir sozinho ao colar" desligado (padrão) → colar número válido não abre.
      Ligado → colar número válido abre a conversa.
- [ ] Importar texto colado (`Apelido; número`) → itens entram na agenda.
- [ ] Exportar → compartilhar como texto e salvar `.txt` (escolher pasta pelo SAF).
- [ ] Apagar tudo → lista vazia com mensagem de vazio.

### Visual (UI "Abissal")

- [ ] Tema Claro forçado e Escuro forçado: fundo, vidro, fios e texto mudam juntos
      (só o tema escuro/sistema foi usado no teste no aparelho).
- [ ] O painel aparece com a entrada de 260ms ao abrir o app (sem piscar).
- [ ] O aurora vai até atrás da barra de status e da barra de navegação, e o
      painel não fica coberto por elas.
- [ ] Centralização com o teclado aberto e em paisagem (o painel deve rolar sem
      cortar botões).
- [ ] Contraste de rótulos, número e botões nos dois temas, inclusive ao sol.
- [ ] Ícone temático (Material You) quando o launcher aplica a camada monocromática.

### Acessibilidade

- [ ] Fonte do sistema no maior tamanho → nenhum texto cortado, botões ainda tocáveis.
- [ ] TalkBack: país, campo de número, mensagem, menu e botões com rótulos
      compreensíveis (o botão de menu tem `contentDescription` "Menu").
- [ ] Giro da tela → número e mensagem continuam preenchidos.
