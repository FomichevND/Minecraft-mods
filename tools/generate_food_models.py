"""All edible IDs are read from the live Minecraft 1.20.1 registry dump."""
from pathlib import Path
import json,math
from generate_monkey_decor import Model,box,write,COLORS
source=Path('build/gametest/food-catalog.json');catalog=json.loads((source if source.exists() else Path('docs/monkey-decor/vanilla-food-catalog.json')).read_text());Path('docs/monkey-decor').mkdir(exist_ok=True);Path('docs/monkey-decor/vanilla-food-catalog.json').write_text(json.dumps(catalog,indent=2))
for item in catalog+['minecraft:cake','zoldrax:banana']:
 n=item.split(':')[1];m=Model('food_'+n);colors=list(COLORS);base=(177,103,49);leaf=(54,124,36)
 if 'apple' in n:base=(242,185,29) if 'golden' in n else (192,33,41)
 elif 'carrot' in n:base=(246,181,29) if 'golden' in n else (223,111,26)
 elif 'potato' in n:base=(132,143,55) if 'poisonous' in n else (188,134,68)
 elif n in ['beetroot','sweet_berries']:base=(166,32,51)
 elif n=='glow_berries':base=(240,167,32)
 elif n=='chorus_fruit':base=(155,70,139)
 elif 'salmon' in n:base=(172,80,57) if 'cooked' in n else (218,88,100)
 elif 'cod' in n:base=(184,136,74) if 'cooked' in n else (178,181,145)
 elif n=='tropical_fish':base=(241,139,44)
 elif n=='pufferfish':base=(227,184,53)
 elif n=='dried_kelp':base=(82,85,45)
 elif n=='rotten_flesh':base=(143,75,40)
 elif n=='spider_eye':base=(168,40,67)
 elif 'beef' in n or 'mutton' in n or 'porkchop' in n:base=(111,54,37) if 'cooked' in n else (201,76,82)
 elif 'chicken' in n or 'rabbit' in n:base=(173,93,45) if 'cooked' in n else (225,166,139)
 colors[0]=base;colors[1]=tuple(min(255,c+32) for c in base);colors[10]=tuple(max(0,c-36) for c in base);colors[2]=leaf
 if n in ['apple','golden_apple','enchanted_golden_apple','potato','baked_potato','poisonous_potato','beetroot','chorus_fruit']:
  box(m,'fruit_body',1,1,1,6,5,6);box(m,'fruit_top',2,6,2,4,1,4,'cutwood');box(m,'fruit_base',2,0,2,4,1,4)
  if 'potato' not in n:box(m,'stem',3.5,7,3.5,1,2,1,'darkwood');box(m,'leaf',4,7.2,3,3,.6,2,'bamboo')
  if n=='enchanted_golden_apple':
   for x,z in [(1,1),(6,5),(2,6)]:box(m,'enchanted_glint'+str(x),x,3,z,1.2,2,1.2,'red')
 elif 'carrot' in n or n=='banana':
  for i in range(4):box(m,'root_taper'+str(i),3-i*.6,i*1.5,3-i*.6,1.5+i*1.2,1.5,1.5+i*1.2)
  for i in range(3):box(m,'leaf'+str(i),2+i*1.6,6,3,1,3+(i%2),1,'bamboo')
 elif n in ['sweet_berries','glow_berries']:
  for i,(x,y,z) in enumerate([(1,0,2),(4,0,1),(2,3,3),(5,2,4)]):box(m,'berry'+str(i),x,y,z,3,3,3)
  box(m,'berry_leaves',1,6,2,6,1,4,'bamboo')
 elif n in ['cod','salmon','cooked_cod','cooked_salmon','tropical_fish','pufferfish']:
  if n=='pufferfish':
   box(m,'puffer_body',2,1,1,6,5,6);box(m,'puffer_top',3,6,2,4,1,4,'cutwood')
   for i in range(4):box(m,'puffer_spike'+str(i),2+i*1.5,3,7,.6,.6,1,'cream')
  else:
   box(m,'fish_body',1,1,2,8,3,3);box(m,'fish_belly',2,0,2.5,6,1,2,'cream');box(m,'tail',9,0,1.5,2,5,4);box(m,'dorsal_fin',4,4,3,3,1,1,'cutwood')
   if n=='tropical_fish':
    for x in [3,6]:box(m,'white_band'+str(x),x,.9,1.9,1,3.2,3.2,'cream')
  for z in [1.85,5.05]:box(m,'fish_eye'+str(z),1.2,2,z,.6,.7,.2,'darkwood')
 elif n in ['mushroom_stew','rabbit_stew','beetroot_soup','suspicious_stew']:
  box(m,'bowl_base',2,0,2,5,1,5,'darkwood')
  for x,z,w,d in [(1,1,7,1),(1,7,7,1),(1,2,1,5),(7,2,1,5)]:box(m,'bowl_rim'+str(x)+str(z),x,1,z,w,3,d,'wood')
  box(m,'soup',2,2.8,2,5,.4,5,'red' if n=='beetroot_soup' else 'gold')
  for i,(x,z) in enumerate([(3,3),(5,5),(3,5)]):box(m,'ingredients'+str(i),x,3.2,z,1,.4,1,'cream' if n=='mushroom_stew' else 'bamboo' if n=='suspicious_stew' else 'cutwood')
 elif n=='honey_bottle':
  box(m,'glass_body',2,0,2,5,6,5,'cream');box(m,'honey_fill',2.1,.5,1.9,4.8,4,5.2,'gold');box(m,'neck',3,6,3,3,2,3,'cream');box(m,'stopper',3,8,3,3,1,3,'wood')
 elif n=='melon_slice':
  for i in range(5):box(m,'melon_slice'+str(i),i*2,0,2,2,1+(4-abs(i-2)*2),2,'red');box(m,'rind'+str(i),i*2,-1,2,2,1,2,'bamboo')
  for i in [2,4,6]:box(m,'seed'+str(i),i,1.5,4.05,.6,.7,.2,'darkwood')
 elif n in ['bread','cookie','pumpkin_pie','cake']:
  if n=='bread':
   box(m,'loaf',0,0,2,11,3,5);box(m,'loaf_dome',1,3,2.6,9,1.2,3.8,'cutwood')
   for x in [2,5,8]:box(m,'bread_score'+str(x),x,3.7,2.5,.6,.6,4,'cream')
  elif n=='cake':
   box(m,'cake_body',0,0,0,10,4,10,'cream');box(m,'icing',0,4,0,10,1,10,'paper')
   for i,(x,z) in enumerate([(2,2),(6,2),(4,6)]):box(m,'cherry'+str(i),x,5,z,1,.5,1,'red')
  else:
   box(m,'pastry_base',1,0,1,8,1,8);box(m,'pastry_top',2,1,1,6,1,8,'gold' if n=='pumpkin_pie' else 'cutwood')
   for x,z in [(2,2),(5,3),(3,6)]:box(m,'chocolate_or_crust'+str(x),x,2,z,1,.3,1,'darkwood' if n=='cookie' else 'cream')
 elif n=='dried_kelp':
  for i in range(3):box(m,'kelp_fold'+str(i),1+i*.3,i*.7,1+i*.4,7,1,5,'wood')
 elif n=='spider_eye':
  box(m,'eye_body',2,0,2,5,4,4);box(m,'pupil',4,1,6,2,2,.5,'darkwood')
 else:
  box(m,'meat_cut',1,0,1,8,2,5);box(m,'raised_flesh',2,2,2,6,1.2,3,'cutwood')
  if n in ['chicken','cooked_chicken','rabbit','cooked_rabbit']:box(m,'bone',7,1,2.5,4,1.3,1.3,'cream');box(m,'bone_end',10,1,2,1,2,2,'cream')
  elif n=='rotten_flesh':box(m,'rotten_patch',2,3,2,2,.3,2,'bamboo')
 # Species-specific silhouettes rather than recolouring the same rectangular steak.
 if n in ['beef','cooked_beef']:
  m.c[0]['to']=[8,2,8];m.c[1]['to']=[7,3.2,7];box(m,'fat_edge',1,2.05,2,1,.35,5,'cream')
 elif n in ['mutton','cooked_mutton']:
  m.c[0]['to']=[11,2,4.5];m.c[1]['to']=[10,3.2,4];box(m,'tapered_tip',10,.3,1.5,2,1.4,2)
 elif n in ['porkchop','cooked_porkchop']:
  box(m,'chop_lobe',2,.5,5,5,1.5,2);box(m,'fat_rind',1,1.8,1,8,.4,.7,'cream')
 elif n in ['chicken','cooked_chicken']:
  box(m,'left_leg',0,0,5,3,2,4);box(m,'right_leg',6,0,5,3,2,4);box(m,'left_wing',0,1,1,2,2,4,'cutwood')
 elif n in ['rabbit','cooked_rabbit']:
  box(m,'rabbit_haunch',1,1,4,4,3,3);box(m,'rabbit_foreleg',0,0,0,3,1,4)
 if n=='banana':
  m.c=[]
  for i,(x,y) in enumerate([(0,2),(1,1),(3,0),(5,0),(7,1),(8,3)]):box(m,'banana_curve'+str(i),x,y,2,2,2,3,'gold')
 write(m,'food_models',colors)

Path("src/main/resources/assets/zoldrax/food_models.json").write_text(json.dumps({**{id:"food_"+id.split(":")[1] for id in catalog+["minecraft:cake"]},"zoldrax:golden_banana":"food_banana"},indent=2))
