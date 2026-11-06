from pathlib import Path
import json
from generate_monkey_decor import NAMES
R=Path('src/main/resources');A=R/'assets/zoldrax';D=R/'data/zoldrax'
ru=['Полка фруктовых даров','Разделочный стол добытчика','Корзина общих припасов','Бамбуковая сушилка','Гонг двора','Праздничная скамья','Доска указов глашатая','Барабан праздника']
en=['Fruit Offering Shelf','Gatherer Preparation Table','Shared Supply Basket','Bamboo Drying Rack','Courtyard Gong','Festival Bench','Herald Notice Board','Festival Drum']
for lang,labels in [('ru_ru',ru),('en_us',en)]:
 p=A/'lang'/f'{lang}.json';d=json.loads(p.read_text());d['itemGroup.zoldrax.monkey_decor']='Двор Короля Обезьян' if lang=='ru_ru' else 'Monkey King Court'
 for n,label in zip(NAMES,labels):
  d['block.zoldrax.monkey_king_'+n]=label;d['tooltip.zoldrax.monkey_king_'+n]=('ПКМ — использовать или положить предмет; Shift + ПКМ пустой рукой — забрать.' if lang=='ru_ru' else 'Use to interact or place one item; sneak-use with an empty hand to retrieve.')
 p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n')
for i,n in enumerate(NAMES):
 name='monkey_king_'+n
 key={'P':{'item':'minecraft:jungle_planks'},'B':{'item':'minecraft:bamboo'},'R':{'item':'minecraft:red_wool'},'G':{'item':'minecraft:gold_nugget'}}
 pattern=['GRG','PPP','B B']
 if n=='supply_basket':pattern=['B B','BRB','BBB'];key={'B':{'item':'minecraft:bamboo'},'R':{'item':'minecraft:yellow_wool'}}
 if n=='courtyard_gong':pattern=['PPP','BGB','BRB'];key['G']={'item':'minecraft:gold_ingot'}
 if n=='notice_board':pattern=['GRG','PPP','B B']
 if n=='festival_drum':pattern=['GRG','PLP','B B'];key['L']={'item':'minecraft:leather'}
 patterns=[['GRG','PPP','BPB'],['PPP','BRB','G G'],['B B','BRB','BBB'],['BGB','R R','B B'],['PPP','BGB','BRB'],['RPR','PGP','B B'],['GRG','PAP','B B'],['GRG','PLP','B B']]
 pattern=patterns[i];key={k:{'item':'minecraft:'+v} for k,v in {'P':'jungle_planks','B':'bamboo','R':'red_wool','G':'gold_nugget','A':'paper','L':'leather'}.items()}
 used=set(''.join(pattern))-{' '};key={k:v for k,v in key.items() if k in used}
 (D/'recipes'/f'{name}.json').write_text(json.dumps({'type':'minecraft:crafting_shaped','category':'decorations','pattern':pattern,'key':key,'result':{'item':'zoldrax:'+name,'count':1}},indent=2))
 for suffix in ['','_part']:(D/'loot_tables/blocks'/f'{name+suffix}.json').write_text(json.dumps({'type':'minecraft:block','pools':[] if suffix else [{'rolls':1,'conditions':[{'condition':'minecraft:survives_explosion'}],'entries':[{'type':'minecraft:item','name':'zoldrax:'+name}]}]}))
p=R/'data/minecraft/tags/blocks/mineable/axe.json';p.write_text(json.dumps({'replace':False,'values':['zoldrax:monkey_king_'+n+s for n in NAMES for s in ['','_part']]}))
