# KPDuels — Eventos Avançados (Paper 1.21.11)

## Novidades

Sete modalidades adicionadas ao sistema de eventos existente:

| Modalidade | Comando para configurar | Funcionamento |
|---|---|---|
| Lucky Block | `/seteventlocal luckyblock` | Aparecem **blocos de ouro temporários** próximos ao local (mesma altura, apenas em ar sobre chão sólido); quebre ou clique com botão direito para recompensas aleatórias. PvP habilitado após countdown. Os blocos são removidos ao finalizar/desligar, e há um journal para recuperar blocos após reinício inesperado. |
| Arena | `/seteventlocal arena` | Combate todos-contra-todos com espada, ferro e maçãs douradas. O último sobrevivente vence. |
| Batalha de Sopas | `/seteventlocal sopa` | Espada, armadura e inventário de sopas; usa o sistema de consumo de sopas já existente. Último sobrevivente vence. |
| Arqueiros | `/seteventlocal arco` | Combate de arco/flechas, armadura de malha, eliminação. |
| Parkour | `/seteventlocal parkour`, `/seteventlocal parkour finish` | Vitória do primeiro a chegar perto do finish; PvP desativado. |
| Corrida | `/seteventlocal corrida`, `/seteventlocal corrida finish` | Corrida sem PvP até o ponto de chegada. |
| Rei do Morro | `/seteventlocal rei_do_morro`, `/seteventlocal rei_do_morro point` | Capture **sozinho** o ponto por 30 segundos acumulados; área disputada não pontua. Com PvP. |

**O ponto principal (`location`) é obrigatório para todas as modalidades.** `finish` ou `point` deve estar no **mesmo mundo** da respectiva `location`; caso contrário, o puxão será recusado. Não há coordenadas inventadas e as arenas dos duelos não são modificadas.

## Organizando um evento

1. Escolha mundos e coordenadas **exclusivos para os eventos** e configure com `/seteventlocal <modo>` no lugar desejado. Para corrida, parkour e Rei do Morro, configure também os pontos extras.
2. Configure `/set-eventos-spawn` (sala de espera) e `/set-eventos-saida` (destino após saída).
3. `/evento abrir`; jogadores executam `/evento entrar`.
4. `/eventoadmin` abre o inventário de administração; clique **Modalidades** e escolha uma opção (ou use `/puxarevento <modo>` / `/eventoadmin modo <modo>`). Os participantes são teleportados e recebem um kit no local configurado.
5. `/evento iniciar` começa o countdown; ao chegar a zero o modo é ativado automaticamente.
6. `/evento parar` encerra sem vencedor; `/setwinner <jogador>` encerra com vencedor. Parkour, Corrida, Rei do Morro e eliminações gerais também podem definir vencedor automaticamente.

**Atenção:** selecionar um modo antes de haver jogadores não os transporta automaticamente quando entrarem depois. Após receber os jogadores, selecione novamente o modo ou use `/puxarevento <modo>`.

## Kits a partir do inventário (event-kits.yml)

- `/eventokit salvar <nome>` — salva os 36 slots principais, os quatro slots da armadura e o item da mão secundária, incluindo metadados dos itens.
- `/eventokit listar` — lista kits disponíveis.
- `/eventokit dar <jogador> <nome>` — aplica o kit a **qualquer jogador online**, dentro ou fora de eventos, a qualquer momento.
- `/eventokit todos <nome>` — aplica o kit somente aos participantes ativos do evento.
- `/eventokit vincular <modo> <nome>` — aplica automaticamente este kit quando os jogadores forem puxados para a modalidade. Use `padrao` no lugar do nome para usar o kit embutido.
- `/eventokit apagar <nome>` — apaga kit salvo.

**Importante:** a aplicação de um kit substitui o inventário, armadura e offhand do alvo. Fora do evento não há salvamento/restauração automática do inventário anterior. Dentro do evento, o inventário original foi capturado quando o jogador entrou e é restaurado ao sair (como já era feito pelo sistema original).

## Painel administrativo `/eventoadmin`

Controles disponíveis no GUI:

- Abrir/fechar sala, iniciar/parar, alternar PvP;
- escolher e puxar participantes para os sete novos modos;
- listar kits e aplicar aos participantes;
- alterar countdown em intervalos de 5 segundos;
- habilitar/desabilitar broadcast;
- definir spawn e saída na posição atual do administrador (fora de eventos em andamento);
- teleportar o administrador ao spawn;
- repor Lucky Blocks e visualizar o bloqueio de clima/hora;
- ciclar clima e dia/noite **apenas no mundo de evento configurado**.

Subcomandos (também aceitam console quando apropriado):

```
/eventoadmin status
/eventoadmin modos
/eventoadmin modo <nome>
/eventoadmin countdown <0..120>
/eventoadmin broadcast on|off
/eventoadmin mundo habilitar|desabilitar
/eventoadmin clima limpo|chuva|trovoada
/eventoadmin hora dia|noite
/eventoadmin blocos
/eventoadmin ajuda
```

**Segurança de mundos:** os controles de clima e hora **ficam BLOQUEADOS por padrão** (`events.admin.world-controls-enabled: false`). É preciso habilitar conscientemente com `/eventoadmin mundo habilitar`. Clima e hora são alterações **globais para aquele mundo**, não apenas para os participantes; se o mundo também hospeda outros minigames, não habilite ou isole o evento em um mundo próprio. O painel nunca executa alterações em todos os mundos.

## Proteções e convivência com outros minigames

- Blocos do Lucky Block são criados **somente em AIR**, na camada Y e raio relativos à coordenada configurada, **sem substituição de blocos existentes**. Somente blocos rastreados recebem prêmio e são removidos após o fim ou desligamento do plugin; um journal `event-luckyblocks.yml` permite recuperar resíduos após queda do servidor (caso o mundo já esteja carregado).
- Participantes dos modos extras não podem colocar ou quebrar blocos comuns; PvP é permitido somente entre participantes ativos pelo sistema atual.
- **Outros plugins de proteção (WorldGuard etc.) podem bloquear a quebra dos Lucky Blocks:** configure permissões na região específica do evento. O plugin não reverte cancelamentos de proteção para permitir quebras arbitrárias.
- Os modos antigos (Sumo, 1v1, Lava e MDR), as arenas do duelo, os inventários dos participantes e as estatísticas MySQL foram mantidos.
- Teste em ambiente de homologação, faça backup de config/inventários e migre arquivos Java para sua rotina de compilação Paper 1.21.11. Este ZIP é **código-fonte**, não contém um JAR compilado.

## Personalização em config.yml

```
events:
  extra:
    finish-radius: 2.5
    luckyblock:
      radius: 12
      max-blocks: 12
      spawn-interval-seconds: 20
    hill:
      radius: 4
      points-to-win: 30
    modes:
      arena:
        kit: ""   # nome do kit salvo, ou vazio para kit padrão
```

**Permissões:** `event.admin` (e compatibilidade de código com `events.admin`) permite administrar eventos e kits. `events.blockedcmds` ignora a whitelist dos comandos do evento. Não atribua permissões administrativas ao público.
