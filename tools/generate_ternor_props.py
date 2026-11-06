"""Editable volumetric plants and representative stored materials, using real vanilla tag IDs."""
from pathlib import Path
from PIL import ImageDraw
import json,zipfile
from generate_ternor_decor import Model,box,write,atlas,ROOT
jar='/workspace/work/gradle-home/caches/fabric-loom/1.20.1/minecraft-client.jar'
with zipfile.ZipFile(jar) as z:flowers=[v.split(':')[1] for v in json.loads(z.read('data/minecraft/tags/blocks/small_flowers.json'))['values'] if isinstance(v,str) and not v.startswith('#')]
plants=flowers+['oak_sapling','spruce_sapling','birch_sapling','jungle_sapling','acacia_sapling','dark_oak_sapling','cherry_sapling','mangrove_propagule','fern','grass','dead_bush','azalea','flowering_azalea','brown_mushroom','red_mushroom','vine','glow_berries','sweet_berries']
colors={'dandelion':(214,176,51),'poppy':(173,44,38),'blue_orchid':(80,153,181),'allium':(158,103,168),'azure_bluet':(219,218,189),'red_tulip':(177,47,42),'orange_tulip':(204,125,52),'white_tulip':(223,216,192),'pink_tulip':(203,145,166),'oxeye_daisy':(221,219,196),'cornflower':(69,103,178),'lily_of_the_valley':(222,224,212),'wither_rose':(54,47,48),'torchflower':(202,105,44),'red_mushroom':(174,48,44),'brown_mushroom':(139,98,71)}
index={}
for name in plants:
 m=Model('plant_'+name);image=atlas();d=ImageDraw.Draw(image);d.rectangle((16,48,31,63),fill=(65,108,43));d.rectangle((48,48,63,63),fill=colors.get(name,(95,139,59)))
 m.image=image
 if name in flowers:
  box(m,'stem',3.6,0,3.6,.8,7,.8,'leaf');box(m,'leaf_left',1,2.5,3,3,.6,2,'leaf');box(m,'leaf_right',4,4,3,3,.6,2,'leaf')
  for i,(x,z) in enumerate([(1,3),(5,3),(3,1),(3,5)]):box(m,'petal'+str(i),x,7,z,2,1.1,2,'paper')
  box(m,'flower_centre',3,7.2,3,2,1.1,2,'gold')
 elif 'mushroom' in name:
  box(m,'stem',3,0,3,2,5,2,'paper');box(m,'cap',1,5,1,6,2,6,'paper');box(m,'cap_top',2,7,2,4,1,4,'paper')
 elif name in ['vine','glow_berries']:
  for i in range(5):box(m,'vine'+str(i),3+(i%2),i*2,3,1,3,1,'leaf');box(m,'vine_leaf'+str(i),2+(i%2)*2,i*2,2,2,1,2,'leaf')
 else:
  box(m,'stem',3.5,0,3.5,1,5,1,'wood')
  for i,(x,y,z,w) in enumerate([(1,3,1,6),(0,5,2,8),(2,7,2,4)]):box(m,'foliage'+str(i),x,y,z,w,2,4,'leaf')
 write(m,'ternor_props');index['minecraft:'+name]={'model':m.name,'plant':True}
for name,kind in [('wheat','wheat'),('wheat_seeds','seeds'),('beetroot_seeds','seeds'),('pumpkin_seeds','seeds'),('melon_seeds','seeds'),('torchflower_seeds','seeds'),('iron_ingot','ingots'),('gold_ingot','ingots'),('copper_ingot','ingots')]:
 m=Model('stored_'+name)
 if kind=='wheat':
  for i in range(5):
   box(m,'stalk'+str(i),1+i,0,3+(i%2),.5,7,.5,'gold')
   for y in [5,7,9]:box(m,f'grain{i}_{y}',.7+i,y,2.7+(i%2),1.3,1.4,1.3,'gold')
 elif kind=='seeds':
  for i in range(12):box(m,'seed'+str(i),1+(i%4)*1.6,(i//4)*.7,1+(i%3)*1.8,1.5,.65,1,'gold' if name!='melon_seeds' else 'paper')
 else:
  for i,(x,y,z) in enumerate([(0,0,0),(0,0,4),(2,2,2)]):box(m,'ingot'+str(i),x,y,z,6,1.8,3,'iron' if name=='iron_ingot' else 'gold' if name=='gold_ingot' else 'leather')
 write(m,'ternor_props');index['minecraft:'+name]={'model':m.name,'plant':False}
(ROOT/'ternor_display_models.json').write_text(json.dumps(index,indent=2))
Path('docs/ternor').mkdir(exist_ok=True);Path('docs/ternor/supported-plants.json').write_text(json.dumps(plants,indent=2))
