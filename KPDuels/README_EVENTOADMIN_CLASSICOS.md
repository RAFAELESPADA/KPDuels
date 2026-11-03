# KPDuels - Eventos clássicos no painel /eventoadmin

## Acesso

- `/eventoadmin` > **Eventos clássicos (5)**: Sumo, 1v1, MDR, Lava e Spawn.
- `/eventoadmin` > **Eventos novos (7)**: Lucky Block, Arena, Sopa, Arco, Parkour, Corrida e Rei do Morro.
- Há atalhos de navegação entre os dois menus.

## Clique para puxar jogadores

O menu clássico usa a mesma rotina do `/puxarevento`:

| Item do menu | Ação | Configuração |
| --- | --- | --- |
| Puxar Sumo | `/puxarevento sumo` | `/seteventlocal sumo` e `/seteventlocal sumo loc1`, `loc2` |
| Puxar 1v1 | `/puxarevento 1v1` | `/seteventlocal 1v1` e `/seteventlocal 1v1 loc1`, `loc2` |
| Puxar MDR | `/puxarevento mdr` | `/seteventlocal mdr` e, quando houver admin, `/seteventlocal mdr-admin` |
| Puxar Lava | `/puxarevento lava` | `/seteventlocal lava` (a lógica antiga transporta à arena ao começar) |
| Puxar Spawn | `/puxarevento spawn` | `/seteventlocal spawn` (diferente de `/set-eventos-spawn`) |

### Confrontos

O painel também inclui os botões **Iniciar confronto 1v1** e **Iniciar confronto Sumo**.
Esses botões executam os mesmos métodos de `/evento puxar1v1` e `/evento puxarsumo`.
O evento deve estar iniciado e os participantes devem ter sido puxados antes.
O agendamento automático dos confrontos ao iniciar o evento continua funcionando.

## Novos subcomandos

- `/eventoadmin classicos` (aliases: `antigos`, `legacy`): abre o menu clássico.
- `/eventoadmin puxar <modo>`: puxa participantes para qualquer modo reconhecido.
- `/eventoadmin modo <modo>`: agora também reconhece Sumo, 1v1, MDR, Lava e Spawn.
- `/eventoadmin confronto <1v1|sumo>`: inicia confronto manual após o início do evento.

## Proteção das coordenadas

Nenhum local foi regravado. Os cliques consultam `events.pull-locations.<modo>.location`
e reutilizam a rotina de teleporte do plugin. Os pares usam os pontos `loc1` e `loc2`.
Se um local não estiver configurado ou o mundo não estiver carregado, o transporte é recusado.
As permissões `event.admin`/`events.admin` continuam exigidas.

## Instalação

Substitua os arquivos-fonte no projeto e compile com suas dependências locais do Paper 1.21.11 (Java 21).
**Este ZIP não é um JAR pronto para instalação.** Faça backup e teste antes de colocar em produção.

Os arquivos `.class` antigos do ZIP enviado foram omitidos porque não representam as novas alterações; recompilar o projeto é obrigatório.
