MODO GLADIATOR - KPDuels
========================

COMO FUNCIONA
-------------
- Ao iniciar uma partida, o plugin escolhe uma posição aleatória no mundo configurado
  em gladiator.world (por padrão, Duels).
- Em mundos normais, procura terreno sólido e coloca a plataforma logo acima dele.
- Em mundos void, usa gladiator.floating-y e só constrói se todo o volume estiver vazio.
- A jaula tem piso de stone bricks, paredes e teto de vidro (glass), com 56x56 blocos
  de largura (8 vezes os 7x7 originais) e 7 blocos de altura.
- Os dois jogadores são teleportados automaticamente para lados opostos, próximos às paredes.
- No Gladiator, cada jogador só consegue quebrar os blocos que ele próprio colocou.
  As paredes, o teto, o piso e os blocos colocados pelo adversário ficam protegidos.
- Não é necessário criar uma arena Gladiator com /setduelarena nem definir spawns.
- A posição aleatória evita o spawn, jogadores próximos e áreas de arenas cadastradas.
- Ao terminar por vitória, morte ou desconexão, a jaula desaparece e todos os blocos
  originais da área são restaurados.
- Água e lava colocadas com baldes, inclusive os blocos alcançados pelo fluxo, também
  são removidas/restauradas ao encerrar o duelo; líquidos não podem escapar da arena.

CONFIGURAÇÃO
------------
No config.yml:

  gladiator:
    world: Duels
    random-radius: 5000
    min-distance-from-spawn: 150
    attempts: 50
    floating-y: 80

random-radius define a distância máxima em blocos em torno do spawn do mundo, em uma
área quadrada. min-distance-from-spawn evita construir perto do lobby. attempts define
quantas posições serão verificadas. floating-y é a altura da plataforma em mundos void.

KIT GLADIATOR
-------------
- Armadura completa de ferro equipada e duas armaduras extras completas no inventário.
- Hotbar: espada de diamante com Sharpness I, balde de água, balde de lava,
  64 tábuas de carvalho, quatro sopas e 64 pedregulhos.
- Duas fileiras de recraft: 64 tigelas, três pilhas de 64 cogumelos marrons
  e um machado de ferro por fileira, além de uma armadura extra em cada uma.
- Última fileira: dois baldes de lava e sete sopas.
- Cada sopa recupera até 7 pontos de vida (3,5 corações) e 6 pontos de fome.
- A sopa consumida vira uma tigela, que também pode ser usada no recraft.
- A regeneração natural de vida por fome é desativada durante os modos Soup e Gladiator.

OBSERVAÇÃO
----------
O mundo precisa estar carregado e definido em gladiator.world. O nome é resolvido sem
considerar maiúsculas/minúsculas, então Duels e duels funcionam igualmente.

DESAFIOS DIRETOS COM BLAZE ROD
- No lobby spawnd, clique com o botão direito usando a Blaze Rod em outro jogador.
- O menu de kits será aberto já vinculado ao jogador clicado.
- Ao escolher um kit, o desafio será enviado imediatamente para esse jogador.
- O fluxo alternativo (abrir o menu primeiro, escolher o kit e depois clicar no jogador) continua funcionando.

INVENTÁRIO DO GLADIATOR
-----------------------
O inventário inteiro segue a referência visual: a hotbar contém espada de diamante
com Sharpness I, água, lava, 64 tábuas, quatro sopas e 64 pedregulhos; as duas
fileiras seguintes contêm armaduras extras, tigelas, cogumelos marrons e machados;
a última fileira contém dois baldes de lava e sete sopas.
