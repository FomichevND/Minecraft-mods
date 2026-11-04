"""Independent rigid Monkey King buildings with local foundations and decoration.
Terrain placement is handled by MonkeySettlementStructure, never by a common platform.
"""
from pathlib import Path
from PIL import Image,ImageDraw
from generate_cult_sites import Site,Long,js
class MonkeySite(Site):
 def put(self,x,y,z,name,props=None,nbt=None):
  mapping={'polished_blackstone_bricks':'mossy_stone_bricks','cracked_polished_blackstone_bricks':'cracked_stone_bricks','chiseled_polished_blackstone':'chiseled_stone_bricks','polished_blackstone':'stone_bricks','basalt':'stone','polished_basalt':'jungle_log','stripped_dark_oak_log':'stripped_jungle_log','dark_oak_log':'jungle_log','crimson_planks':'jungle_planks','dark_oak_planks':'spruce_planks','crimson_door':'jungle_door','anvil':'smoker','smithing_table':'crafting_table'}
  if name in mapping:name=mapping[name]
  elif 'red_nether_brick' in name:pass
  elif 'polished_blackstone_brick_' in name:name=name.replace('polished_blackstone_brick_','bamboo_') if name.endswith(('stairs','slab')) else name.replace('polished_blackstone_brick_','stone_brick_')
  elif 'dark_oak_' in name:name=name.replace('dark_oak_','jungle_')
  elif name=='blackstone_stairs':name='stone_brick_stairs'
  super().put(x,y,z,name,props,nbt)
 def building(self,x,z,w,d,kind):
  super().building(x,z,w,d,kind)
  front=z+d-1;center=x+w//2
  # Replace the tall cult-hall roof with a lower jungle roof: alternate stairs
  # and solid tile rows form an uninterrupted shallow slope with golden bamboo trim.
  for X in range(x-1,x+w+1):
   for Z in range(z-1,front+3):
    for y in range(6,16):self.put(X,y,Z,'air')
  for X in [x,x+w-1]:
   self.fill(X,5,z,X,5,front,'jungle_log',{'axis':'z'})
  for X in range(x-1,x+w+1):
   off=X-x;rise=min(off+1,w-off);y=6+rise//2;facing='east' if off<(w-1)/2 else 'west'
   for Z in range(z-1,front+2):
    material='bamboo' if Z in [z-1,front+1] or rise==0 else 'red_nether_brick'
    self.put(X,y-1,Z,'bamboo_planks' if material=='bamboo' else 'red_nether_bricks')
    if rise%2==1 or off==(w-1)/2:self.put(X,y,Z,'bamboo_planks' if material=='bamboo' else 'red_nether_bricks')
    else:self.stairs(X,y,Z,facing,material)
   if x<=X<x+w and y>6:
    for Z in [z,front]:self.fill(X,6,Z,X,y-1,Z,'jungle_planks')
  self.fill(center-1,6,front,center,6,front,'chiseled_stone_bricks')
  for X in [x+1,x+w-2]:self.put(X,5,front+1,'jungle_log',{'axis':'z'});self.hanging_light(X,3,front+1)
  self.banner(center,6,front+1)
  if kind=='forge':
   chimney=x+w-2
   self.pillar(chimney,4,z+2,5,'mossy_stone_bricks');self.put(chimney,9,z+2,'chiseled_stone_bricks');self.slab(chimney,10,z+2,'bamboo')
  else:self.barrel(x+w-2,3,z+2)
  roof_y=6+min(center-x+1,w-(center-x))//2
  for Z in [z-1,front+1]:self.put(center,roof_y+1,Z,'yellow_terracotta');self.slab(center,roof_y+2,Z,'bamboo')
  for X,plant in [(x+1,'potted_bamboo'),(x+w-2,'potted_red_tulip')]:self.put(X,2,front+2,'mossy_stone_bricks');self.put(X,3,front+2,plant)
 def banner(self,x,y,z,facing='south'):
  self.put(x,y,z,'red_wall_banner',{'facing':facing},{'id':'minecraft:banner','Patterns':[{'Color':4,'Pattern':'zbanana'}]})
 def loot(self,x,y,z,kind='treasure',facing='south'):
  self.put(x,y,z,'chest',{'facing':facing,'type':'single','waterlogged':False},{'id':'minecraft:chest','LootTable':'zoldrax:chests/monkey_supplies'})
 def temple(self,x,y,z):
  self.fill(x-5,2,z-4,x+6,y-1,z+4,'mossy_stone_bricks')
  for X in range(x-2,x+4):self.stairs(X,y-1,z+5,'north','stone_brick')
  self.put(x,y,z,'zoldrax:monkey_monument',{'facing':'south'},{'id':'zoldrax:monkey_monument','Stage':2,'Food':'minecraft:air','Deadline':Long(-1),'NextRequest':Long(-1),'NextJoke':Long(-1)})
  self.loot(x,y,z+3)
  for X in [x-5,x+5]:
   self.pillar(X,y,z-2,4,'chiseled_stone_bricks');self.put(X,y+4,z-2,'gold_block');L=X+1 if X<x else X-1;self.put(L,y+4,z-2,'jungle_planks');self.hanging_light(L,y+2,z-2)
   self.banner(X,y+3,z-1)
 def watch(self,x,z):
  for X in [x,x+4]:
   for Z in [z,z+4]:self.pillar(X,2,Z,8,'jungle_log')
  self.fill(x,6,z,x+4,6,z+4,'spruce_planks')
  for X in range(x,x+5):
   for Z in range(z,z+5):
    if X in [x,x+4] or Z in [z,z+4]:self.put(X,7,Z,'jungle_fence',{'waterlogged':False})
  self.fill(x+1,2,z,x+1,8,z,'jungle_planks')
  for y in range(2,7):self.put(x+1,y,z+1,'ladder',{'facing':'south','waterlogged':False})
  self.fill(x-1,10,z-1,x+5,10,z+5,'jungle_slab',{'type':'bottom','waterlogged':False})
  self.fill(x,11,z,x+4,11,z+4,'yellow_terracotta');self.slab(x+2,12,z+2,'sandstone')
  self.fill(x,9,z+4,x+4,9,z+4,'jungle_log',{'axis':'x'});self.put(x+2,9,z+5,'jungle_planks');self.banner(x+2,9,z+6);self.hanging_light(x+2,7,z+5)
 def festival(self,x,z):
  for X in range(x,x+9):
   for Z in range(z,z+9):
    if (X in [x,x+8] and Z in [z,z+8]):continue
    self.put(X,2,Z,'chiseled_stone_bricks' if X in [x,x+8] or Z in [z,z+8] else ('yellow_terracotta' if (X+Z)%3==0 else 'jungle_planks'))
  for X in [x,x+8]:
   for Z in [z+1,z+7]:self.pillar(X,3,Z,2,'jungle_fence');self.light(X,5,Z)
  for X in range(x+2,x+7):self.stairs(X,2,z+9,'north','stone_brick')
 def farm(self,x,z,w,d):
  for X in range(x,x+w):
   for Z in range(z,z+d):
    edge=X in [x,x+w-1] or Z in [z,z+d-1]
    if edge:self.put(X,2,Z,'jungle_fence',{'waterlogged':False});continue
    if X==x+w//2:self.put(X,1,Z,'water',{'level':0});continue
    self.put(X,1,Z,'farmland',{'moisture':7});crop='carrots' if (X+Z)%2 else 'wheat';self.put(X,2,Z,crop,{'age':7})
  self.put(x+w//2,2,z,'jungle_fence_gate',{'facing':'south','open':True,'powered':False,'in_wall':False})
  self.put(x+w//2,1,z,'jungle_planks')
 def market(self,x,z):
  self.fill(x,2,z,x+6,2,z+4,'jungle_planks')
  for X in [x,x+6]:
   for Z in [z,z+4]:self.pillar(X,3,Z,4,'jungle_log')
  for X in range(x-1,x+8):
   for Z in range(z-1,z+6):self.slab(X,7,Z,'jungle' if X%2==0 else 'sandstone')
  for X in range(x+1,x+6):self.barrel(X,3,z+1)
  self.loot(x+1,3,z+3);self.loot(x+5,3,z+3);self.banner(x+3,6,z+5)
  self.put(x+3,6,z+4,'jungle_planks');self.hanging_light(x+3,4,z+4)
 def greenery(self,points):
  for x,z in points:
   self.put(x,1,z,'moss_block')
   self.pillar(x,2,z,6,'bamboo_block')
   for X in range(x-1,x+2):
    for Z in range(z-1,z+2):self.put(X,8,Z,'jungle_leaves',{'distance':1,'persistent':True,'waterlogged':False})
   self.put(x,9,z,'jungle_leaves',{'distance':1,'persistent':True,'waterlogged':False})
   for y in range(4,8):self.put(x,y,z+1,'vine',{'north':True,'south':False,'east':False,'west':False,'up':False})
 def save(self):
  # Explicit jungle-fence connections in frozen templates, including solid posts.
  for (x,y,z),(state,_) in self.blocks.items():
   if state['Name']!='minecraft:jungle_fence':continue
   for side,dx,dz in [('east',1,0),('west',-1,0),('north',0,-1),('south',0,1)]:
    n=self.blocks.get((x+dx,y,z+dz),({'Name':'minecraft:air'},None))[0]['Name'];state.setdefault('Properties',{})[side]=str(n in ['minecraft:jungle_fence','minecraft:jungle_log','minecraft:jungle_planks','minecraft:spruce_planks','minecraft:mossy_stone_bricks']).lower()
  super().save()
def foundation(s):
 # Local foundations only; the rest of the settlement is untouched terrain.
 # Eight buried layers cover the maximum accepted five-block footprint slope.
 for x in range(1,s.width-1):
  for z in range(1,s.length-1):
   s.put(x,0,z,'dirt');s.put(x,1,z,'grass_block',{'snowy':False})
   if z>=s.length-4 and abs(x-s.width//2)<=1:s.put(x,1,z,'mossy_cobblestone' if (x+z)%3==0 else 'dirt_path')

def component(name,w,d,draw):
 s=MonkeySite('monkey/'+name,24,w,d);foundation(s)
 # Clear only the individual footprint, keeping terrain between buildings intact.
 s.fill(1,2,1,w-2,20,d-2,'air');draw(s)
 # Translate the template upward to include deep, local buried footing.
 old=s.blocks;s.blocks={};s.height=32
 for x in range(1,w-1):
  for z in range(1,d-1):
   for y in range(8):s.put(x,y,z,'dirt' if y<6 else 'mossy_stone_bricks')
 for (x,y,z),value in old.items():s.blocks[x,y+8,z]=value
 for e in s.entities:e['pos'][1]+=8;e['blockPos'][1]+=8
 s.save()

def temple(s,valley):
 s.temple(8,4,6)
 for x,z in [(3,11),(12,11)]:s.put(x,2,z,'mossy_stone_bricks');s.put(x,3,z,'potted_bamboo')
 # Five residents at a safe, open shrine court; their existing AI finds jobs nearby.
 roles=['banana_herald']*(2 if valley else 3)+['temple_gatherer']*(3 if valley else 2)
 for role,(x,z) in zip(roles,[(4,13),(6,13),(8,13),(10,13),(12,13)]):s.npc(role,x,2,z)

def garden(s):
 s.greenery([(3,3),(7,6)])
 for x,z in [(2,6),(5,2),(6,8)]:s.put(x,2,z,'azalea');s.put(x+1,2,z,'melon')
 s.put(4,2,7,'mossy_stone_bricks');s.put(4,3,7,'potted_fern')

def rest(s):
 s.pillar(2,2,2,3,'jungle_fence');s.put(2,5,2,'jungle_planks');s.hanging_light(2,3,3)
 for x in range(4,7):s.stairs(x,2,3,'north','jungle');s.trapdoor(x,3,2,'north',material='jungle')
 s.barrel(5,2,5);s.put(6,2,5,'hay_block',{'axis':'y'});s.put(3,2,6,'potted_bamboo')

def court():
 component('court_temple',17,17,lambda s:temple(s,False))
 component('valley_temple',17,17,lambda s:temple(s,True))
 component('herald_hall',14,16,lambda s:s.building(3,3,8,8,'tent'))
 component('gatherer_hall',16,16,lambda s:s.building(3,3,10,8,'barracks'))
 component('storehouse',15,17,lambda s:s.building(3,3,9,9,'storage'))
 component('kitchen',14,16,lambda s:s.building(3,3,8,8,'forge'))
 component('watch',9,11,lambda s:s.watch(2,2))
 component('festival',13,15,lambda s:s.festival(2,2))
 component('market',11,11,lambda s:s.market(2,2))
 component('farm',13,13,lambda s:s.farm(2,2,9,9))
 component('garden',11,11,garden)
 component('rest',9,9,rest)

if __name__=='__main__':
 court()
 # Legacy monolithic NBT stays readable by already-saved structure starts.
 # New generation uses only the independent monkey/* templates.
 for old in ['banana_court','great_appetite_valley']:
  (Path('src/main/resources/data/zoldrax/worldgen/template_pool')/(old+'.json')).unlink(missing_ok=True)
 for name in ['banana_court','great_appetite_valley']:
  js(Path(f'worldgen/structure/{name}.json'),{'type':'zoldrax:monkey_settlement','valley':name=='great_appetite_valley','biomes':'#zoldrax:has_structure/monkey_sites','spawn_overrides':{},'step':'surface_structures','terrain_adaptation':'none'})
 js(Path('worldgen/structure_set/monkey_sites.json'),{'structures':[{'structure':'zoldrax:'+n,'weight':1} for n in ['banana_court','great_appetite_valley']],'placement':{'type':'minecraft:random_spread','spacing':22,'separation':9,'salt':52601928}})
 js(Path('tags/worldgen/biome/has_structure/monkey_sites.json'),{'replace':False,'values':['minecraft:jungle','minecraft:bamboo_jungle','minecraft:sparse_jungle','minecraft:savanna','minecraft:savanna_plateau']})
 js(Path('loot_tables/chests/monkey_supplies.json'),{'type':'minecraft:chest','pools':[{'rolls':{'type':'minecraft:uniform','min':3,'max':6},'entries':[{'type':'minecraft:item','name':'minecraft:'+n,'weight':w,'functions':[{'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':3,'max':12}}]} for n,w in [('bread',8),('carrot',7),('potato',7),('apple',4),('melon_slice',8),('wheat_seeds',5),('gold_nugget',1)]]}]})
 im=Image.new('RGBA',(64,64));d=ImageDraw.Draw(im);d.polygon([(4,4),(7,8),(12,9),(17,7),(20,3),(19,10),(15,15),(10,17),(6,15),(3,11)],fill='white');d.rectangle((18,2,20,4),fill='white');p=Path('src/main/resources/assets/zoldrax/textures/entity/banner');p.mkdir(parents=True,exist_ok=True);im.save(p/'banana.png')
