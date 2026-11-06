"""Static release gate for the seven Ternor models and their volumetric props."""
from pathlib import Path
from PIL import Image
import gzip,json,sys

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/zoldrax'
DATA=ROOT/'src/main/resources/data/zoldrax'
NAMES=['garden_canopy','storage_shelf','animal_feeder','animal_trough','stone_forge','forge_bellows','quenching_tub']
minimum=[60,20,15,22,34,30,18]
errors=[]
def fail(message):errors.append(message)
for name,count in zip(NAMES,minimum):
 full='ternor_'+name;design=ROOT/'design/blockbench/ternor_decor'/f'{full}.bbmodel';runtime=ASSETS/'models/blockbench/decor'/f'{full}.bbmodel';texture=ASSETS/'textures/block/zoldrax'/f'{full}.png'
 if not design.exists() or not runtime.exists():fail(f'missing model {full}');continue
 if design.read_bytes()!=runtime.read_bytes():fail(f'design/runtime mismatch {full}')
 model=json.loads(runtime.read_text());elements=model.get('elements',[])
 if len(elements)<count:fail(f'{full}: only {len(elements)} cubes')
 if model.get('meta',{}).get('model_format')!='free':fail(f'{full}: not editable free model')
 if not model.get('textures',[{}])[0].get('source','').startswith('data:image/png;base64,'):fail(f'{full}: texture not embedded')
 groups={e.get('zoldrax_group') for e in elements}
 if name in ('animal_trough','quenching_tub') and 'water' not in groups:fail(f'{full}: no dynamic water group')
 if name=='stone_forge' and not any(e.get('zoldrax_group')=='flame' and e.get('zoldrax_emissive') for e in elements):fail('forge has no emissive volumetric flame')
 if name=='forge_bellows' and sum(e.get('zoldrax_group')=='pump' for e in elements)<8:fail('bellows has no articulated folds')
 if not texture.exists() or Image.open(texture).size!=(64,64):fail(f'{full}: texture is not 64x64')
 for suffix in ('','_part'):
  for folder in ('blockstates',):
   if not (ASSETS/folder/f'{full+suffix}.json').exists():fail(f'missing {folder}/{full+suffix}')
 if not (DATA/'recipes'/f'{full}.json').exists():fail(f'missing recipe {full}')
index=json.loads((ASSETS/'ternor_display_models.json').read_text())
if len(index)!=41:fail(f'expected 41 props, got {len(index)}')
for item,row in index.items():
 name=row['model'];design=ROOT/'design/blockbench/ternor_props'/f'{name}.bbmodel';runtime=ASSETS/'models/blockbench/ternor_props'/f'{name}.bbmodel'
 if not design.exists() or not runtime.exists() or design.read_bytes()!=runtime.read_bytes():fail(f'broken prop source {item}')
 elif len(json.loads(runtime.read_text())['elements'])<3:fail(f'flat prop {item}')
for name in ('workshop','farmyard','storehouse'):
 path=DATA/'structures/ternor'/f'{name}.nbt'
 if not path.exists() or not gzip.decompress(path.read_bytes()).startswith(b'\x0a'):fail(f'invalid structure {name}')
for name in ('quench_iron','quench_gold','quench_copper'):
 recipe=json.loads((DATA/'recipes'/f'{name}.json').read_text())
 if recipe.get('type')!='zoldrax:quenching' or not recipe.get('require_hot') or not recipe.get('tempered'):fail(f'invalid quenching recipe {name}')
if errors:
 print('\n'.join('ERROR '+e for e in errors));sys.exit(1)
print(f'Validated 7 editable decor models, {len(index)} volumetric props, 3 settlement templates and 3 quenching recipes.')
