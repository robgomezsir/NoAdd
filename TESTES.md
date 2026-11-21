# Testes — NoAdd

## Já verificado neste build (ambiente sem aparelho)

- [x] `:app:assembleDebug`, `:app:assembleRelease` e `:app:bundleRelease` compilam
      sem erro (R8 e lint vital do release inclusive).
- [x] APK de release **sem nenhuma permissão** (`aapt2 dump permissions`).
- [x] `minSdkVersion 24`, `targetSdkVersion 36`, `applicationId com.noadd.app`,
      rótulo "NoAdd", ícone presente, idiomas `--_--`, `en`, `es`.
- [x] Assinatura de release conferida com `apksigner verify`: APK Signature
      Scheme v2 válido, certificado `CN=NoAdd`, RSA 4096.
- [x] Tamanho: APK assinado 78.769 bytes, AAB assinado 105.159 bytes.
- [x] Todos os 13 tokens de cor do vidro existem em `attrs.xml` e estão
      definidos nas paletas Light, Dark e Auto (checagem estática).
- [x] Algoritmo de número executado em JVM (22 casos, todos OK): DDI com/sem `+`,
      parênteses/hífens/espaços, `00` internacional, zero de tronco, número já com
      DDI, 9º dígito em celular de 8 dígitos, fixo de 10 dígitos, Portugal e EUA,
      extração de texto compartilhado e de link `wa.me`, formatação e país do DDI.

## Testes manuais no aparelho (pendentes)

### Número

- [ ] Digitar `11 99999-9999` (país Brasil) → abre com `+55 11 99999-9999`.
- [ ] Colar `+55 (11) 99999-9999` → mesmo resultado, sem duplicar o 55.
- [ ] Colar `5511999999999` → mesmo resultado.
- [ ] Digitar `(11) 9999-9999` (celular antigo, 8 dígitos) → abre com o 9º dígito inserido.
- [ ] Digitar `011 99999-9999` → zero de tronco removido.
- [ ] Fixo `11 3333-4444` → não ganha 9º dígito.
- [ ] Número curto (`119999`) → aparece o aviso “parece incompleto”, mas ainda abre.
- [ ] Trocar país para Portugal e testar `912 345 678` → `+351 912 345 678`.

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

- [ ] Tema Sistema (claro e escuro), Claro e Escuro: o fundo azul-marinho, o
      vidro, os fios e o texto mudam juntos, sem nenhuma tela com cor errada.
- [ ] O painel de vidro aparece com a entrada de 260ms ao abrir o app (sem
      piscar e sem travar a rolagem).
- [ ] O aurora vai até atrás da barra de status e da barra de navegação, e
      nenhum texto/botão fica coberto por elas.
- [ ] Contraste do número, rótulos e botões legível nos dois temas, inclusive
      ao sol (vidro claro sobre fundo claro não pode "sumir").
- [ ] Botões têm feedback de toque (ripple) e continuam com 56dp de altura.

### Acessibilidade e tema

- [ ] Fonte do sistema no maior tamanho → nenhum texto cortado, botões ainda tocáveis.
- [ ] TalkBack: país, campo de número, mensagem e botões com rótulos compreensíveis.
- [ ] Tema Sistema (claro e escuro), Claro forçado e Escuro forçado.
- [ ] Giro da tela → número e mensagem continuam preenchidos.
