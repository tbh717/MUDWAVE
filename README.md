# MUDWAVE
**How to?**: Run from the Main application. The console should spit out a "game ready" message when all the game setup 
has safely been completed. You should be able to connect then to the 4000 port.

**Commands**: Just type in "help" for a list of commands!

**Movement**: Move north, south, east, west, up, down.

**Item handling**: Equip things, unequip things (to some default fists), pick things up to add them to your inventory, 
drop things to add them to the current room (but other players can pick them up!). My philosophy for items, true to real
life, is that _anything_ can be used as a weapon, but not everything is going to do a great job at killing other things.

**Networking**: Yep. Totally cool. Hop in to see some cool splash text, make your character. Say things to people in your room, 
whisper to other players in your room.

**Shortest path**: Yep! Just type and get your own personalized Mapquest route.

**NPCs**: A handful of these bad boys (and girls) roam around. Got weapons, things to drop. You can fight 'em.

**Combat**: Fight other people to the death. Just type combat and get ready to watch a bunch of text roll by until someone dies.
Much like real life, upon death, your items fly to your combat partner and you get disconnected.

**ADTs**: Mutable linked list? In the main, keeping track of used names. Sorted linked list priority queue? It's in the data package,
but since I only needed one priority queue, and heaps are more efficient, it's not being used. BST-based map? In the room supervisor,
connecting room names to their actor references. Heap based priority queue? In the activity manager, queueing up activity (mostly
mindless NPC's and dudes wacking each other with soylent waste packets.)

**Theme**: Boy, made this up very sick after watching Blade Runner in bed. A bit loopy, but it's some mix of futuristic cyberpunk,
Bioshock-esque biopunk, and the slick, dark city-at-night aesthetic.
