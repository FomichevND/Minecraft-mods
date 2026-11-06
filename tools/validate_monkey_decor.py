from pathlib import Path
import json,base64,math
from PIL import Image
root=Path('src/main/resources/assets/zoldrax');counts={};allmeshes=[]
for folder,pattern in [('decor','monkey_king_*.bbmodel'),('food','*.bbmodel')]:
 for p in (root/'models/blockbench'/folder).glob(pattern):
  m=json.loads(p.read_text());image=root/'textures/block/zoldrax'/(p.stem+'.png');assert Image.open(image).size==(64,64) if folder=='decor' else Image.open(image).size==(32,32);assert base64.b64decode(m['textures'][0]['source'].split(',')[1])==image.read_bytes()
  design=Path('design/blockbench')/('monkey_king_decor' if folder=='decor' else 'food_models')/p.name;assert design.read_bytes()==p.read_bytes()
  ids=[]
  for c in m['elements']:
   ids.append(c['uuid']);assert all(c['to'][i]>c['from'][i] for i in range(3));assert all(math.isfinite(v) for key in ['from','to','rotation','origin'] for v in c[key]);assert all(0<=v<=m['resolution']['width'] for f in c['faces'].values() for v in f['uv'])
  assert len(ids)==len(set(ids));assert set(ids)=={u for g in m['outliner'] for u in g['children']};counts[folder]=counts.get(folder,0)+1;allmeshes.append(m)
assert counts=={'decor':8,'food':42},counts
foods=json.loads((root/'food_models.json').read_text());catalog=json.loads(Path('docs/monkey-decor/vanilla-food-catalog.json').read_text());assert len(catalog)==40 and all(id in foods for id in catalog)
recipes=[]
for p in (root/'models/blockbench/decor').glob('monkey_king_*.bbmodel'):
 for lang in ['ru_ru','en_us']:assert 'block.zoldrax.'+p.stem in json.loads((root/f'lang/{lang}.json').read_text())
 r=json.loads(Path(f'src/main/resources/data/zoldrax/recipes/{p.stem}.json').read_text());recipes.append((r['pattern'],r['key']));assert r['result']['item']=='zoldrax:'+p.stem
assert len({json.dumps(r,sort_keys=True) for r in recipes})==8
print('8 furniture + 42 food meshes; embedded PNGs, UV, editable copies, 40 registry foods, recipes and translations: OK')
