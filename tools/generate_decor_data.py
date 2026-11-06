"""Recipes, tags, loot, bilingual labels and custom vanilla-sound mixtures."""
from pathlib import Path
import json
from generate_zoldrax_decor import NAMES
R=Path('src/main/resources');A=R/'assets/zoldrax';D=R/'data/zoldrax'
def save(path,data):path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')
ru=['Трон Карателя','Жаровня Воздаяния','Черепной светильник','Подвесной цепной фонарь','Оружейная стойка Золдракса','Сундук кровавых даров','Пюпитр проклятий','Алтарная подставка для подношений']
en=['Punisher’s Throne','Brazier of Retribution','Skull Lamp','Hanging Chain Lantern','Zoldrax Weapon Rack','Chest of Blood Offerings','Lectern of Curses','Offering Pedestal']
ru_tips=['ПКМ — сесть или встать. Shift — встать.','Пустая рука + ПКМ — включить или погасить огонь.','ПКМ — включить или выключить свет.','ПКМ — свет. Shift + ПКМ пустой рукой — длина цепи.','ПКМ с оружием — разместить; пустая рука — забрать. Три крепления.','27 слотов. Поддерживает воронки.','ПКМ с книгой — разместить; ПКМ — читать; Shift + ПКМ — забрать.','ПКМ с предметом — разместить один; Shift + ПКМ — забрать.']
en_tips=['Use to sit or stand. Sneak to dismount.','Use with an empty hand to toggle the decorative fire.','Use to toggle the light.','Use to toggle. Sneak-use with an empty hand to change the chain length.','Use with a weapon to store it; empty hand to retrieve. Three mounts.','27 slots. Supports hoppers.','Use a book to place it; use to read; sneak-use to retrieve.','Use an item to display one; sneak-use to retrieve it.']
messages={
 'seat_occupied':('Трон уже занят.','The throne is occupied.'),'rack_full':('Все три крепления заняты.','All three weapon mounts are occupied.'),
 'rack_weapon':('Здесь можно разместить мечи, топоры и оружие из тега стойки.','This rack accepts swords, axes and tagged weapons.'),
 'lectern_book':('Положите книгу или письменную книгу.','Place a book or a written book.'),
 'take_offering':('Shift + ПКМ — забрать подношение.','Sneak-use to retrieve the displayed offering.'),
 'chain_blocked':('Для длинной цепи не хватает свободного места снизу.','There is not enough room below for the longer chain.')}
for lang,names,tips in [('ru_ru',ru,ru_tips),('en_us',en,en_tips)]:
 p=A/'lang'/f'{lang}.json';data=json.loads(p.read_text());data['itemGroup.zoldrax.cult_decor']='Культ Золдракса' if lang=='ru_ru' else 'Cult of Zoldrax'
 for name,label,tip in zip(NAMES,names,tips):data['block.zoldrax.'+name]=label;data['tooltip.zoldrax.'+name]=tip
 for key,values in messages.items():data['message.zoldrax.'+key]=values[0 if lang=='ru_ru' else 1]
 data['entity.zoldrax.decor_seat']='Сиденье трона' if lang=='ru_ru' else 'Throne seat';save(p,data)
recipes=[
 (['BCB','SWS','PIP'],{'B':'bone_block','C':'chain','S':'polished_blackstone_bricks','W':'red_wool','P':'polished_blackstone','I':'iron_ingot'}),
 (['BCB','NAN','PPP'],{'B':'bone','C':'chain','N':'red_nether_bricks','A':'campfire','P':'polished_blackstone'}),
 ([' B ','RCR','PAP'],{'B':'bone_block','R':'redstone','C':'chain','P':'polished_blackstone','A':'amethyst_shard'}),
 ([' C ','BLB',' N '],{'C':'chain','B':'bone','L':'lantern','N':'red_nether_bricks'}),
 (['BCB','IRI','PPP'],{'B':'bone_block','C':'chain','I':'iron_ingot','R':'red_wool','P':'polished_blackstone'}),
 (['BIB','NWN','PCP'],{'B':'bone','I':'iron_ingot','N':'red_nether_bricks','W':'chest','C':'chain','P':'polished_blackstone'}),
 (['BLB','CRC','PPP'],{'B':'bone','L':'lectern','C':'chain','R':'red_wool','P':'polished_blackstone'}),
 (['BAB','NRN','PPP'],{'B':'bone','A':'amethyst_shard','N':'red_nether_bricks','R':'redstone','P':'polished_blackstone'})]
for name,(pattern,key) in zip(NAMES,recipes):
 save(D/'recipes'/f'{name}.json',{'type':'minecraft:crafting_shaped','category':'decorations','pattern':pattern,'key':{k:{'item':'minecraft:'+v} for k,v in key.items()},'result':{'item':'zoldrax:'+name,'count':1}})
 save(D/'loot_tables/blocks'/f'{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'conditions':[{'condition':'minecraft:survives_explosion'}],'entries':[{'type':'minecraft:item','name':'zoldrax:'+name}]}]})
 save(D/'loot_tables/blocks'/f'{name}_part.json',{'type':'minecraft:block','pools':[]})
weapons=['minecraft:'+mat+'_'+tool for mat in ['wooden','stone','iron','golden','diamond','netherite'] for tool in ['sword','axe']]+['minecraft:trident']
save(D/'tags/items/rack_weapons.json',{'replace':False,'values':weapons})
save(D/'tags/items/lectern_books.json',{'replace':False,'values':['minecraft:book','minecraft:writable_book','minecraft:written_book',{'id':'#c:scrolls','required':False}]})
save(D/'tags/items/pedestal_gems.json',{'replace':False,'values':['minecraft:diamond','minecraft:emerald','minecraft:amethyst_shard','minecraft:prismarine_crystals',{'id':'#c:gems','required':False}]})
p=D/'tags/blocks/mineable/pickaxe.json';save(p,{'replace':False,'values':['zoldrax:'+n+s for n in NAMES for s in ['', '_part']]})
# Vanilla block tags live in the minecraft namespace.
p2=R/'data/minecraft/tags/blocks/mineable/pickaxe.json';save(p2,json.loads(p.read_text()));p.unlink()
save(A/'sounds.json',{'decor_chest_open':{'subtitle':'subtitles.block.chest.open','sounds':[{'name':'minecraft:block.ender_chest.open','type':'event','volume':.7},{'name':'minecraft:block.chain.place','type':'event','volume':.35,'pitch':.7}]},'decor_chest_close':{'subtitle':'subtitles.block.chest.close','sounds':[{'name':'minecraft:block.ender_chest.close','type':'event','volume':.7},{'name':'minecraft:block.chain.hit','type':'event','volume':.3,'pitch':.7}]}})
