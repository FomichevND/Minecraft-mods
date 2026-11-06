from pathlib import Path
import json
from generate_ternor_decor import NAMES
R=Path('src/main/resources');A=R/'assets/zoldrax';D=R/'data/zoldrax'
ru=['Садовый навес Тернора','Складской стеллаж Тернора','Кормушка Тернора','Поилка Тернора','Каменный очаг-кузня','Кузнечные меха Тернора','Закалочная бадья'];en=['Ternor Garden Canopy','Ternor Storage Shelf','Ternor Animal Feeder','Ternor Animal Trough','Ternor Stone Forge','Ternor Forge Bellows','Ternor Quenching Tub']
ru_tips=['ПКМ растением — посадить; Shift+ПКМ пустой рукой — забрать.','ПКМ — 27 слотов хранения.','ПКМ — 9 слотов корма; обслуживает коров, овец и свиней.','Ведро воды — наполнить. Животные пьют с ограничением частоты.','ПКМ — ремонт за материалы и опыт; Shift+ПКМ — плавка.','ПКМ — один цикл и 10 секунд ускорения ближайшей кузни.','Ведро — наполнить; ПКМ горячим слитком — закалить.']
en_tips=['Use a plant to insert; sneak-use empty-handed to retrieve.','Use to open 27 storage slots.','Use for 9 feed slots; supports cows, sheep and pigs.','Use a water bucket to fill; animals drink on cooldown.','Use for material/XP repair; sneak-use for smelting.','Use for one cycle and 10 seconds of nearby forge boost.','Bucket to fill; use a hot ingot to quench.']
for lang,labels,tips in [('ru_ru',ru,ru_tips),('en_us',en,en_tips)]:
 p=A/'lang'/f'{lang}.json';d=json.loads(p.read_text());d['itemGroup.zoldrax.ternor_decor']='Поселение Тернора' if lang=='ru_ru' else 'Ternor Settlement'
 for name,label,tip in zip(NAMES,labels,tips):d['block.zoldrax.ternor_'+name]=label;d['tooltip.zoldrax.ternor_'+name]=tip
 d['tooltip.zoldrax.hot_blank']='Горячая заготовка: закалите в бадье до остывания.' if lang=='ru_ru' else 'Hot blank: quench in a tub before it cools.';d['tooltip.zoldrax.tempered']='Закалено в бадье Тернора.' if lang=='ru_ru' else 'Quenched in a Ternor tub.'
 p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n')
patterns=[['PLP','BWB','SMS'],['PIP','PWP','PIP'],['SSS','PWP','SSS'],['SLS','SBS','SSS'],['SSS','FAI','SWS'],['PLI','BWB','PIP'],['SBS','IWI','PPP']]
items={'P':'oak_planks','L':'lantern','B':'oak_log','W':'blue_wool','S':'stone_bricks','M':'mossy_stone_bricks','I':'iron_ingot','F':'furnace','A':'anvil'}
for name,pattern in zip(NAMES,patterns):
 id='ternor_'+name;used=set(''.join(pattern))-{' '};(D/'recipes'/f'{id}.json').write_text(json.dumps({'type':'minecraft:crafting_shaped','category':'decorations','pattern':pattern,'key':{k:{'item':'minecraft:'+items[k]} for k in used},'result':{'item':'zoldrax:'+id,'count':1}},indent=2))
 for suffix in ['','_part']:(D/'loot_tables/blocks'/f'{id+suffix}.json').write_text(json.dumps({'type':'minecraft:block','pools':[] if suffix else [{'rolls':1,'conditions':[{'condition':'minecraft:survives_explosion'}],'entries':[{'type':'minecraft:item','name':'zoldrax:'+id}]}]}))
for metal in ['iron','gold','copper']:(D/'recipes'/f'quench_{metal}.json').write_text(json.dumps({'type':'zoldrax:quenching','ingredient':{'item':'minecraft:'+metal+'_ingot'},'result':{'item':'minecraft:'+metal+'_ingot','count':1},'water':1,'require_hot':True,'tempered':True},indent=2))
p=R/'data/minecraft/tags/blocks/mineable/pickaxe.json';d=json.loads(p.read_text());d['values']=[v for v in d['values'] if not v.startswith('zoldrax:ternor_')]+['zoldrax:ternor_'+n+s for n in NAMES for s in ['','_part']];p.write_text(json.dumps(d,indent=2))
