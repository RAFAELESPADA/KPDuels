# Sistema de Eventos - KPDuels

Implementado sobre o sistema atual do KPDuels, sem reutilizar as estatísticas de duelos.

## Instalação

1. Substitua/atualize os fontes do projeto pelos arquivos desta source.
2. Mantenha no classpath as dependências já usadas pelo projeto: Spigot/Paper API 1.21.11, PlaceholderAPI, HikariCP, WorldEdit e KP-CORE.
3. Compile usando Java 21 (necessário para o ambiente 1.21.x).
4. Atualize `plugin.yml` e `config.yml` junto com o JAR.
5. Na primeira inicialização, o plugin cria automaticamente a tabela `kpduels_event_stats` no banco configurado em `mysql`. Se o usuário do banco não tiver permissão `CREATE`, execute `eventos_mysql.sql` manualmente.

## Comandos

### Jogadores

- `/evento entrar` - entra na sala quando ela está aberta e teleporta para o spawn de eventos.
- `/evento sair` - deixa o evento, restaura inventário/armadura/XP/efeitos e teleporta para a saída.

### Administradores (`event.admin`)

- `/set-eventos-spawn` - define o ponto de entrada dos participantes.
- `/set-eventos-saida` - define para onde os participantes voltam ao sair ou encerrar o evento.
- `/seteventlocal sumo|1v1|mdr|lava` - salva o local atual para uma modalidade.
- `/evento abrir` e `/evento fechar` - abre/fecha a sala. O broadcast global roda a cada 30 segundos enquanto aberta e é cancelado ao fechar.
- `/evento pvp on|off` - liga/desliga o PvP entre participantes. O PvP só é permitido depois do início do evento.
- `/evento iniciar` - inicia a contagem regressiva configurada (padrão 10 até 0, no chat e na tela) e inicia o evento ao chegar a zero.
- `/evento parar` - encerra o evento e devolve todos os participantes/espectadores para a saída.
- `/puxarevento sumo` (ou `1v1`, `mdr`, `lava`) - teleporta os membros do evento para o local configurado e envia as instruções daquela modalidade.
- `/setwinner <jogador>` - registra uma vitória no MySQL, anuncia o vencedor, executa os comandos de vencedor e encerra o evento.

## Placeholders

- `%kpduels_esta_evento%` - retorna `true` enquanto o jogador estiver na lista do evento, inclusive como espectador; `false` após sair, desconectar ou o evento encerrar.

## Espectadores

Ao morrer durante o evento, o jogador reaparece no spawn de eventos e passa para o modo espectador. Ele pode voar, fica invisível para todos que não são espectadores, continua vendo os participantes e recebe uma bússola para abrir a lista de jogadores vivos. A cama vermelha sai do evento e devolve o jogador à saída configurada. Ao desconectar, o jogador é removido do evento e seu inventário original é restaurado antes do salvamento de dados.

## MySQL

A tabela `kpduels_event_stats` armazena:

- `uuid`: UUID do jogador (chave primária)
- `player_name`: último nome conhecido
- `kills`: eliminações feitas contra participantes durante um evento iniciado e com PvP ativado
- `wins`: vitórias atribuídas por `/setwinner`
- `updated_at`: última atualização

Cada kill/vitória usa incremento SQL atômico, e não altera `duel_stats`.

## Comandos automáticos

Edite `config.yml`:

```yaml
events:
  commands:
    on-stop:
      - "say %player% saiu do evento"
    on-winner:
      - "give %player% diamond 1"
```

`on-stop` executa no console para cada jogador que estava no evento quando ele foi encerrado. `on-winner` executa adicionalmente apenas para o vencedor. Placeholders disponíveis: `%player%` e `%uuid%`.

As instruções de cada local ficam em `events.pull-locations.<local>.instructions` e aceitam `%player%`, `%local%` e `%event%`.


## Confrontos automáticos de 1v1 e Sumo

Para cada modalidade, configure três pontos: a área de espera e as duas posições dos confrontos. Exemplo para 1v1:

1. Vá até a área de espera e execute `/seteventlocal 1v1`.
2. Vá até a primeira posição de luta e execute `/seteventlocal 1v1 loc1`.
3. Vá até a segunda posição de luta e execute `/seteventlocal 1v1 loc2`.
4. Repita os três comandos para `sumo`, usando `/seteventlocal sumo`, `/seteventlocal sumo loc1` e `/seteventlocal sumo loc2`.

Fluxo de uso:

- `/puxarevento 1v1` ou `/puxarevento sumo`: leva os participantes para a respectiva área de espera e registra a modalidade para cada jogador.
- `/evento puxar1v1` ou `/evento puxarsumo`: inicia um confronto com os dois primeiros participantes que foram puxados para aquela modalidade. Eles são enviados para `loc1` e `loc2`. Como existe apenas um par de posições por modalidade, somente um confronto pode ocupar essa arena por vez; os demais ficam aguardando.
- Os dois recebem countdown de 5 até 0 no chat e na tela. Durante o countdown, o PvP fica bloqueado; ao chegar em 0, somente os dois jogadores do confronto podem se atacar.
- Quando um deles morre, ele respawna na posição do seu lado e os dois são reposicionados nos lados originais. Um novo countdown de 5 a 0 começa. O jogador não vira espectador nesse modo.

O PvP dos confrontos 1v1/Sumo é independente do toggle geral `/evento pvp on|off`. Fora desses pares, continua valendo o controle geral do evento. Kills válidas dentro dos confrontos continuam sendo registradas na tabela de estatísticas de eventos do MySQL.

## Comandos automáticos e whitelist de comandos

Após `/evento join` (ou `/evento entrar`) ser concluído com sucesso, o plugin executa cada linha de `events.commands.on-join-player` **como o próprio jogador**, respeitando as permissões dele. Placeholders disponíveis: `%player%`, `%uuid%` e `%event%`. Exemplo:

```yaml
events:
  commands:
    on-join-player:
      - "kit evento"
      - "spawn-evento %player%"
```

A whitelist `events.command-whitelist` bloqueia comandos para participantes e espectadores. Em `allowed`, informe apenas o comando para liberar todas as subopções (por exemplo `msg`) ou um caminho específico para liberar somente aquela ação (por exemplo `evento sair`). A permissão `events.blockedcmds` ignora o bloqueio; por padrão, ela é concedida a operadores e pode ser atribuída a grupos pela sua plataforma de permissões.

```yaml
events:
  command-whitelist:
    enabled: true
    allowed:
      - "evento sair"
      - "evento leave"
      - "msg"
    blocked-message: "&cEsse comando não está permitido durante o evento."
```

## Placeholders de estatísticas do evento

Com PlaceholderAPI habilitado, o plugin registra:

- `%kpduels_esta_evento%`: `true` quando o jogador está na sala do evento; caso contrário, `false`.
- `%kpduels_event_kills%`: kills acumuladas em eventos.
- `%kpduels_event_deaths%`: mortes acumuladas em eventos.

As kills e mortes de evento são armazenadas em `kpduels_event_stats`, separadas das estatísticas normais de duelos. As mortes são contabilizadas sempre que um membro morre dentro do evento, inclusive em confrontos 1v1/Sumo. A primeira leitura de estatísticas é carregada em background e retorna `0` enquanto o cache ainda está carregando.


## Proteção antes do início do evento

Todos os participantes ficam invulneráveis enquanto o evento não tiver sido
efetivamente iniciado. O estado só é liberado quando `/evento start` termina
o countdown global e chega a `0`. Durante esse período são cancelados danos
de queda, void, fogo, lava, sufocamento, afogamento, fome, mobs, explosões,
projéteis e demais fontes. Estados perigosos persistentes (como fogo, veneno,
Wither, congelamento e falta de ar) também são limpos quando detectados para
que não causem dano imediatamente após o countdown.

## Kits dos confrontos 1v1 e Sumo

- **1v1:** recebe espada de diamante no primeiro slot, 35 sopas nos demais slots e armadura completa de ferro. As sopas curam ao clicar com o botão direito e viram tigelas; a regeneração natural por fome fica desativada nesse modo. O kit é reaplicado quando um round reinicia.
- **Sumo:** inventário e armadura são limpos. Os ataques entre os dois adversários continuam válidos e aplicam knockback, mas causam zero dano de vida. Outras fontes de dano também são bloqueadas durante o confronto.
- O knockback do Sumo pode ser ajustado em `events.pair-matches.sumo-knockback.horizontal` e `vertical` no `config.yml`.


## Atualização: kit Lava e tutorial

- Ao executar `/puxarevento lava`, participantes ativos recebem 36 sopas (os 36 espaços de armazenamento, sem ocupar armadura ou mão secundária) e ficam sem armadura.
- O clique direito com sopa é tratado pelo `SoupListener` para jogadores puxados para Lava, recuperando vida/fome e transformando a sopa em tigela.
- Regeneração natural por saturação é desativada no modo Lava para manter a sopa como fonte de cura.
- `/comocriarevento` mostra no chat o passo a passo de configuração, abertura, início, modalidades e encerramento.
