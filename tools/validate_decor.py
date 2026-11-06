"""Asset validation for eight editable Minecraft decor meshes."""
from pathlib import Path
import base64,json,hashlib,math
from PIL import Image
root=Path('src/main/resources');assets=root/'assets/zoldrax'
names=['throne','brazier','skull_lamp','chain_lantern','weapon_rack','chest','lectern','offering_pedestal'];hashes=set();total=0
for short in names:
 name='zoldrax_'+short;p=assets/f'models/blockbench/decor/{name}.bbmodel';m=json.loads(p.read_text());assert p.read_bytes()==Path(f'design/blockbench/{name}.bbmodel').read_bytes();tex=(assets/f'textures/block/zoldrax/{name}.png').read_bytes();assert base64.b64decode(m['textures'][0]['source'].split(',')[1])==tex;assert Image.open(assets/f'textures/block/zoldrax/{name}.png').size==(256,256)
 assert len(m['elements'])>=40;uuids=[];geometry=[]
 for c in m['elements']:
  uuids.append(c['uuid']);assert all(c['to'][i]>c['from'][i] for i in range(3));assert all(math.isfinite(x) for k in ['from','to','rotation','origin'] for x in c[k]);assert set(c['faces'])=={'up','down','north','south','east','west'}
  for face in c['faces'].values():assert len(face['uv'])==4 and all(0<=v<=256 for v in face['uv'])
  geometry.append((c['from'],c['to'],c['rotation']));total+=1
 assert len(uuids)==len(set(uuids));assert set(uuids)=={child for g in m['outliner'] for child in g['children']};hashes.add(hashlib.sha256(repr(geometry).encode()).hexdigest())
 for lang in ['ru_ru','en_us']:
  text=json.loads((assets/f'lang/{lang}.json').read_text());assert text[f'block.zoldrax.{name}'];assert text[f'tooltip.zoldrax.{name}'];assert text['itemGroup.zoldrax.cult_decor']
 assert json.loads((assets/f'models/item/{name}.json').read_text())['display']['gui']['rotation']==[20,45,0]
 recipe=json.loads((root/f'data/zoldrax/recipes/{name}.json').read_text());assert recipe['result']=={'item':'zoldrax:'+name,'count':1};assert set(''.join(recipe['pattern']))-{' '}==set(recipe['key'])
 loot=json.loads((root/f'data/zoldrax/loot_tables/blocks/{name}_part.json').read_text());assert not loot.get('pools')
 print(name,len(m['elements']),'cuboids: OK')
assert len(hashes)==8
print(f'8 distinct editable models, {total} cuboids, 8 embedded 256px textures, recipes, loot and RU/EN: OK')
