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
- [x] Tamanho: APK assinado 200.589 bytes, AAB assinado 255.707 bytes,
      `app-debug.apk` 1.100.414 bytes. O maior bloco do APK é o `classes.dex`
      (126.144 bytes), seguido de `resources.arsc` (50.660); o ícone em PNG soma
      ~56 KB no fonte.
- [x] Nenhuma permissão nova com o lote e o arquivo de treinamento: o
      `aapt2 dump permissions` do APK só devolve o nome do pacote, e o
      `AndroidManifest.xml` continua sem `<uses-permission>` (importar e abrir
      conversa usam SAF e `https://wa.me/...`).
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
- [x] **Ícone**: a arte atual (balão verde-limão com anel vermelho e contato
      creme) aparece em Informações do app, na gaveta e nos atalhos, sem
      artefatos; camada adaptativa sobre o branco de `icon_bg`, mipmaps legados a
      partir dos mesmos PNGs e monocromático com o balão sólido vazado.
- [x] Nenhum crash do `com.noadd.app` no `logcat` durante a sessão.

## Verificado no aparelho — lote (mensagem, nomes e treinamento)

Sessão de 22/11/2026 na mesma depuração sem fio (moto g56 5G), APK de release
assinado, lista montada com 3 contatos: “Maria Silva”, “Joao Pedro Souza” e um
número sem nome.

- [x] **Saudação padrão só na primeira vez**: com a mensagem apagada (campo
      vazio), voltei ao passo 1 e avancei de novo → o campo continuou vazio e a
      prévia mostrou “A mensagem está vazia.” O texto padrão não volta por cima
      da escolha do usuário.
- [x] **Dados do treinamento (1 linha, `;`)**: o resumo de conferência trouxe
      Produto, Cargo, Endereço (com a vírgula do endereço preservada), Hora, Data,
      Período e Instrutor; ao aplicar, a mensagem do campo foi preenchida com os
      valores, mantendo `{nome}` variável, emojis e quebras de linha.
- [x] **Prévia**: “Olá, Maria Silva, tudo bem? 😊 … Treinamento de Operador de
      Telemarketing … Endereço: Av. Paulista, 1000 …”; contador “459 caracteres na
      mensagem · 873 no link” (sem aviso, abaixo de 2.000).
- [x] **Ver exemplo de outro contato**: Maria Silva → Joao Pedro Souza → contato
      sem nome, que rendeu “Olá, tudo bem?” (vírgula sobrando removida).
- [x] **Link no WhatsApp**: o `logcat` mostrou
      `https://wa.me/5511999990000?text=…` com `%F0%9F%98%8A`, `%F0%9F%93%8B`,
      `%F0%9F%93%8D`, `%F0%9F%95%91`, `%F0%9F%93%85`, `%F0%9F%94%84`,
      `%F0%9F%91%A4`, `%F0%9F%AA%91`, `%E2%9A%A0%EF%B8%8F`, acentos em UTF-8 e
      quebras como `%0A`. O número de teste não existe no WhatsApp, então o
      WhatsApp abriu o seletor de contatos: nada foi enviado.
- [x] **Cabeçalho com acento e vírgula** (`Produto,Cargo,Endereço,Hora,Data,Período,Instrutor`):
      lido certo (Período “Tarde”, Instrutor “João Pereira”); aplicado, o texto e
      o resumo “Treinamento: ATENTO · Atendente · 26/11/2026 · 09:30” passaram a
      valer e continuaram salvos.
- [x] **Arquivo com várias linhas**: aviso “O arquivo tem 2 linhas de dados. Esta
      versão usa a primeira.” e a primeira turma foi aplicada (Analista / Ana
      Costa).
- [x] **Colunas faltando**: “Colunas não encontradas: Endereço, Hora, Período,
      Instrutor.” e os campos apareceram como “vazio”, na cor de destaque; a
      prévia passou a mostrar “Sem dado: Endereço, Hora, Período, Instrutor.”.
- [x] **Variável desconhecida**: `{foo}` digitado no texto continuou no corpo da
      mensagem e foi sinalizado com “Variável não reconhecida: {foo}.”.
- [x] **BOM e serial do Excel** (`tr6.csv`): o BOM UTF-8 foi ignorado e a data
      serial `46004` virou `13/12/2025` (dd/MM/aaaa); Hora `07:00` ficou intacta.
- [x] **Modelo de treinamento**: o texto aplicado aparece na lista de modelos como
      “Modelo de treinamento”.
- [x] **Retomada**: fechar o app (e reinstalar o APK por cima) preservou contatos,
      progresso, mensagem e o resumo “Treinamento: …” carregado.
- [x] **Sem crash** do `com.noadd.app` no `logcat` durante a sessão.
- [x] Bug encontrado e corrigido no teste: quando o arquivo tinha cabeçalho mas
      faltava uma coluna, o campo vazio lia a coluna vizinha (Endereço recebia a
      data). Agora campo sem coluna é vazio; o release foi rebuildado e
      reinstalado, e o caso foi repetido com sucesso.

## Pendentes no aparelho

### Lote e treinamento

- [ ] Envio real da mensagem de treinamento por um contato que exista no WhatsApp
      (só o link e a prévia foram conferidos; nada foi enviado).
- [ ] Mesma tela nos idiomas en e es (só pt-BR foi exercitado no aparelho).
- [ ] Escolha de turma quando o arquivo de treinamento tem várias linhas: nesta
      versão o app avisa e usa a primeira; a lista de turmas fica para a próxima.
- [ ] Arquivo de treinamento com DATA/HORA ausentes ou com hora serial do Excel
      (a data serial já foi convertida; a hora serial não é tratada).
- [ ] Importação do segundo arquivo por um provedor de nuvem (Drive/OneDrive) no
      seletor do sistema.

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
