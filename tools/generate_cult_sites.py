"""Deterministic vanilla structure templates, native jigsaw pools and loot tables.
Only builds resources; runtime generation/rotation/entity placement is Minecraft's.
"""
from pathlib import Path
import json,gzip,struct,random
ROOT=Path('src/main/resources/data/zoldrax')
class Byte(int):pass
class Long(int):pass
class Float(float):pass
def tag(value):
 if isinstance(value,Byte):return 1
 if isinstance(value,Long):return 4
 if isinstance(value,Float):return 5
 if isinstance(value,int):return 3
 if isinstance(value,float):return 6
 if isinstance(value,str):return 8
 if isinstance(value,list):return 9
 if isinstance(value,dict):return 10
 raise TypeError(type(value))
def string(s):b=s.encode();return struct.pack('>H',len(b))+b
def payload(v):
 t=tag(v)
 if t in (1,3,4,5,6):return struct.pack({1:'>b',3:'>i',4:'>q',5:'>f',6:'>d'}[t],v)
 if t==8:return string(v)
 if t==9:return bytes([tag(v[0]) if v else 10])+struct.pack('>i',len(v))+b''.join(payload(x) for x in v)
 if t==10:return b''.join(bytes([tag(x)])+string(k)+payload(x) for k,x in v.items())+b'\0'
def dump(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(gzip.compress(b'\x0a\0\0'+payload(v),mtime=0))
def js(p,v):p=ROOT/p;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,indent=2))

class Site:
 def __init__(self,name,height):self.name=name;self.height=height;self.blocks={};self.entities=[];self.rng=random.Random(77+height)
 def put(self,x,y,z,name,props=None,nbt=None):
  assert 0<=x<31 and 0<=z<31 and 0<=y<self.height,(x,y,z,name)
  state={'Name':name if ':' in name else 'minecraft:'+name}
  if props:state['Properties']={k:str(v).lower() for k,v in props.items()}
  self.blocks[x,y,z]=(state,nbt)
 def fill(self,x0,y0,z0,x1,y1,z1,name,props=None):
  for x in range(x0,x1+1):
   for y in range(y0,y1+1):
    for z in range(z0,z1+1):self.put(x,y,z,name,props)
 def pillar(self,x,y,z,h,name='polished_blackstone_bricks'):
  self.fill(x,y,z,x,y+h-1,z,name)
 def loot(self,x,y,z,kind='treasure',facing='south'):
  table='cult_treasure' if kind=='treasure' else 'camp_supplies'
  self.put(x,y,z,'chest',{'facing':facing,'type':'single','waterlogged':False},{'id':'minecraft:chest','LootTable':'zoldrax:chests/'+table})
 def light(self,x,y,z):self.put(x,y,z,'lantern',{'hanging':False,'waterlogged':False})
 def banner(self,x,y,z,facing='south'):
  self.put(x,y,z,'red_wall_banner',{'facing':facing},{'id':'minecraft:banner','Patterns':[{'Color':15,'Pattern':'mr'},{'Color':14,'Pattern':'mc'},{'Color':15,'Pattern':'bo'}]})
 def brazier(self,x,y,z):
  self.fill(x-1,y,z-1,x+1,y,z+1,'polished_blackstone_bricks');self.put(x,y+1,z,'campfire',{'facing':'north','lit':True,'signal_fire':False,'waterlogged':False})
  for dx,dz in [(-1,-1),(-1,1),(1,-1),(1,1)]:self.pillar(x+dx,y+1,z+dz,2,'polished_blackstone_wall')
 def stairs(self,x,y,z,facing,material='polished_blackstone_brick',half='bottom'):
  self.put(x,y,z,material+'_stairs',{'facing':facing,'half':half,'shape':'straight','waterlogged':False})
 def slab(self,x,y,z,material='polished_blackstone_brick',half='bottom'):
  self.put(x,y,z,material+'_slab',{'type':half,'waterlogged':False})
 def trapdoor(self,x,y,z,facing='south',opened=True,material='dark_oak'):
  self.put(x,y,z,material+'_trapdoor',{'facing':facing,'half':'bottom','open':opened,'powered':False,'waterlogged':False})
 def candles(self,x,y,z,count=3):self.put(x,y,z,'red_candle',{'candles':count,'lit':True,'waterlogged':False})
 def barrel(self,x,y,z):self.put(x,y,z,'barrel',{'facing':'up','open':False})
 def hanging_light(self,x,y,z):
  self.put(x,y+1,z,'chain',{'axis':'y','waterlogged':False});self.put(x,y,z,'lantern',{'hanging':True,'waterlogged':False})
 def table(self,x,z,length=3):
  # A one-block-high table, seats with upright backs, and candle/plate details.
  for X in range(x,x+length):self.slab(X,3,z,'dark_oak','top')
  for X in [x,x+length-1]:
   self.stairs(X,3,z-1,'north','dark_oak');self.trapdoor(X,4,z-1,'north')
   self.stairs(X,3,z+1,'south','dark_oak');self.trapdoor(X,4,z+1,'south')
   self.put(X,4,z,'heavy_weighted_pressure_plate',{'power':0})
  self.candles(x+length//2,4,z,2)
 def building(self,x,z,w,d,kind):
  # Enclosed crimson timber hall: masonry plinth, recessed panels, real doors,
  # glazed/barred windows, full gables, stair roof, eaves and furnished rooms.
  front=z+d-1;center=x+w//2
  self.fill(x,2,z,x+w-1,2,front,'polished_blackstone_bricks')
  self.fill(x+1,2,z+1,x+w-2,2,front-1,'dark_oak_planks')
  self.fill(x,3,z,x+w-1,6,z,'crimson_planks');self.fill(x,3,front,x+w-1,6,front,'crimson_planks')
  self.fill(x,3,z,x,6,front,'crimson_planks');self.fill(x+w-1,3,z,x+w-1,6,front,'crimson_planks')
  for X in [x,x+w-1]:
   for Z in [z,front]:self.pillar(X,3,Z,5,'stripped_dark_oak_log');self.put(X,6,Z,'chiseled_polished_blackstone')
  for Z in range(z+3,front,3):
   for X in [x,x+w-1]:
    self.pillar(X,3,Z,4,'polished_basalt')
  for Z in [z+2,front-2]:
   for X in [x,x+w-1]:
    for y in [4,5]:self.put(X,y,Z,'iron_bars',{'waterlogged':False})
    self.trapdoor(X,3,Z,'west' if X==x else 'east')
  # Two-wide doorway leaves the centre aisle accessible to the larger NPCs.
  for X in [center-1,center]:
   for y,half in [(3,'lower'),(4,'upper')]:self.put(X,y,front,'crimson_door',{'facing':'south','half':half,'hinge':'left' if X==center-1 else 'right','open':False,'powered':False})
   self.put(X,5,front,'air')
  self.fill(center-1,6,front,center,6,front,'chiseled_polished_blackstone')
  for X in range(x-1,x+w+1):
   off=X-x;rise=min(off+1,w-off);y=7+rise
   if off<(w-1)/2:facing='east'
   else:facing='west'
   for Z in range(z-1,front+2):
    # Black stone gable trim frames burgundy nether-brick roof tiles.
    mat='polished_blackstone_brick' if Z in [z-1,front+1] or rise==0 else 'red_nether_brick'
    if off==(w-1)/2:self.slab(X,y,Z,mat)
    else:self.stairs(X,y,Z,facing,mat)
   if x<=X<x+w:
    for Z in [z,front]:
     if y>7:self.fill(X,7,Z,X,y-1,Z,'crimson_planks')
  # Connected ridge, supported brackets and a covered front porch.
  for X in [x,x+w-1]:
   for Z in range(z,front+1):self.put(X,6,Z,'dark_oak_log',{'axis':'z'})
   self.stairs(X,6,front+1,'north',half='top')
  self.fill(x+1,2,front+1,x+w-2,2,front+1,'crimson_planks')
  for X in [center-1,center]:self.stairs(X,2,front+2,'north','blackstone')
  for X in [x+1,x+w-2]:
   self.put(X,6,front+1,'dark_oak_log',{'axis':'z'});self.put(X,5,front+1,'dark_oak_fence',{'waterlogged':False});self.hanging_light(X,3,front+1)
  # Roof vent with solid connection down to the wall, rather than a loose flue.
  chimney=x+w-2
  self.put(chimney,3,z+2,'furnace',{'facing':'south','lit':False});self.pillar(chimney,4,z+2,9,'polished_blackstone_bricks');self.put(chimney,13,z+2,'chiseled_polished_blackstone');self.slab(chimney,14,z+2)
  # Storage shelves along the back, kept clear of window bays and the aisle.
  self.fill(x+1,5,z+1,x+w-2,5,z+1,'dark_oak_slab',{'type':'top','waterlogged':False})
  self.barrel(x+1,3,z+1);self.barrel(x+2,3,z+1)
  self.candles(x+1,6,z+1,2);self.put(x+w-2,6,z+1,'flower_pot')
  self.banner(center,7,front+1)
  if kind in ['tent','barracks']:
   for X in [x+1,x+w-2]:
    self.put(X,3,z+3,'red_bed',{'part':'head','facing':'north','occupied':False});self.put(X,3,z+4,'red_bed',{'part':'foot','facing':'north','occupied':False})
   self.table(x+2,z+3,3 if w<10 else 4)
   self.put(x+w-2,3,front-1,'lectern',{'facing':'south','has_book':False,'powered':False})
   self.loot(x+1,3,front-1,'treasure' if kind=='tent' else 'supplies')
  if kind=='storage':
   for X in [x+1,x+w-2]:
    self.loot(X,3,z+3);self.barrel(X,3,front-2);self.trapdoor(X,4,front-2)
   self.loot(center,3,z+1,'supplies')
  if kind=='forge':
   for X,n in [(x+1,'blast_furnace'),(x+2,'furnace')]:self.put(X,3,z+1,n,{'facing':'south','lit':False})
   self.put(x+w-2,3,z+3,'anvil',{'facing':'north'});self.put(x+w-2,3,z+4,'smithing_table')
   self.put(center,3,front-2,'water_cauldron',{'level':3});self.loot(x+1,3,front-2,'supplies')
   for X in [center-1,center+1]:self.put(X,4,z+1,'chain',{'axis':'y','waterlogged':False})
 def cage(self,x,z,w=6,d=6):
  self.fill(x,2,z,x+w-1,2,z+d-1,'cracked_polished_blackstone_bricks')
  for y in range(3,6):
   for X in range(x,x+w):
    for Z in range(z,z+d):
     if X in [x,x+w-1] or Z in [z,z+d-1]:self.put(X,y,Z,'iron_bars',{'waterlogged':False})
  for X in [x,x+w-1]:
   for Z in [z,z+d-1]:self.pillar(X,3,Z,4,'polished_basalt');self.put(X,7,Z,'chiseled_polished_blackstone')
  for X in range(x,x+w):
   for Z in range(z,z+d):
    if X in [x,x+w-1] or Z in [z,z+d-1]:self.put(X,6,Z,'polished_blackstone_bricks')
    else:self.put(X,6,Z,'iron_bars',{'waterlogged':False})
  mid=x+w//2
  for X in [mid-1,mid+1]:self.pillar(X,3,z+d-1,3)
  for y,half in [(3,'lower'),(4,'upper')]:self.put(mid,y,z+d-1,'iron_door',{'facing':'south','half':half,'hinge':'left','open':False,'powered':False})
  self.put(mid-1,3,z+d,'stone_button',{'face':'wall','facing':'south','powered':False})
  self.put(x+1,3,z+1,'hay_block',{'axis':'y'});self.put(x+w-2,3,z+1,'cauldron');self.hanging_light(mid,4,z+1)
 def tower(self,x,z):
  # A continuous stone tower with projecting corbels and a crenellated crown.
  self.fill(x,2,z,x+4,2,z+4,'polished_blackstone_bricks')
  for y in range(3,9):
   for X in range(x,x+5):
    for Z in range(z,z+5):
     if X in [x,x+4] or Z in [z,z+4]:self.put(X,y,Z,'polished_blackstone_bricks')
  for X in [x,x+4]:
   for Z in [z,z+4]:self.pillar(X,2,Z,11,'polished_basalt');self.put(X,7,Z,'chiseled_polished_blackstone')
  for y in range(3,6):self.put(x+2,y,z+4,'air')
  self.stairs(x+2,5,z+4,'south',half='top');self.stairs(x+2,2,z+5,'north','blackstone')
  self.fill(x-1,9,z-1,x+5,9,z+5,'polished_blackstone_bricks')
  for X in range(x-1,x+6):
   for Z in range(z-1,z+6):
    if X in [x-1,x+5] or Z in [z-1,z+5]:
     self.put(X,10,Z,'polished_blackstone_brick_wall',{'up':True,'waterlogged':False})
     facing='east' if X==x-1 else ('west' if X==x+5 else ('south' if Z==z-1 else 'north'))
     self.stairs(X,8,Z,facing,half='top')
  # Upper corner piers and cross-beams visibly carry the crown at y=13.
  for X in [x,x+4]:
   for Z in [z,z+4]:self.pillar(X,10,Z,4,'polished_blackstone_bricks')
  for X in range(x,x+5):
   for Z in range(z,z+5):
    if X in [x,x+4] or Z in [z,z+4]:
     self.put(X,13,Z,'polished_blackstone_bricks')
     if (X+Z)%2==0:self.put(X,14,Z,'chiseled_polished_blackstone')
  for y in range(3,10):self.put(x+1,y,z+1,'ladder',{'facing':'south','waterlogged':False})
  self.put(x+1,9,z+1,'ladder',{'facing':'south','waterlogged':False})
  self.banner(x+2,7,z+5);self.light(x+3,10,z+3)
  self.hanging_light(x+2,6,z+5)
 def altar(self,x,y,z,grand=False):
  self.fill(x-5,2,z-3,x+5,y-1,z+6,'polished_blackstone_bricks')
  for j in range(y-2):
   for X in range(x-2,x+3):self.stairs(X,2+j,z+7-j,'north')
  self.put(x,y,z,'zoldrax:monument',{'facing':'south'},{'id':'zoldrax:monument','Stage':2,'LastOffering':Long(-1),'CultRitualUntil':Long(-1),'Devotees':[]})
  for X in [x-5,x+5]:
   self.pillar(X,y,z-2,6,'polished_basalt');self.put(X,y+3,z-1,'chiseled_polished_blackstone')
   self.stairs(X,y+6,z-2,'east' if X<x else 'west',half='top');self.banner(X,y+4,z-1)
   self.pillar(X,y,z+5,2);self.light(X,y+2,z+5)
   self.stairs(X-1 if X<x else X+1,y,z+5,'east' if X<x else 'west')
  if grand:
   for dx in range(-4,5):
    roof=y+6+min(2,4-abs(dx))
    self.put(x+dx,roof,z-2,'polished_blackstone_bricks');self.slab(x+dx,roof+1,z-2)
   self.put(x,y+8,z-1,'chiseled_polished_blackstone');self.put(x,y+9,z-2,'polished_blackstone_brick_wall',{'up':True,'waterlogged':False})
   for X in [x-3,x+3]:self.hanging_light(X,y+4,z-2)
  for X in [x-4,x+4]:
   self.put(X,y,z+2,'chiseled_polished_blackstone');self.candles(X,y+1,z+2)
 def npc(self,name,x,y,z):self.entities.append({'pos':[float(x)+.5,float(y),float(z)+.5],'blockPos':[x,y,z],'nbt':{'id':'zoldrax:'+name,'PersistenceRequired':Byte(1),'Rotation':[Float(0),Float(0)]}})
 def base(self):
  # A bordered courtyard, quiet masonry variation and a coherent processional path.
  self.fill(0,0,0,30,self.height-1,30,'air')
  self.fill(0,0,0,30,0,30,'basalt');self.fill(1,1,1,29,1,29,'polished_blackstone_bricks')
  for x in range(1,30):
   for z in range(1,30):
    if self.rng.random()<.18:self.put(x,1,z,'cracked_polished_blackstone_bricks')
    if x%6==0 or z%6==0:self.put(x,1,z,'polished_blackstone')
  for X in [0,30]:
   self.fill(X,1,0,X,2,30,'polished_blackstone_bricks')
   for Z in range(0,31,5):self.pillar(X,2,Z,3,'polished_basalt');self.slab(X,5,Z)
  for Z in [0,30]:
   for X in range(1,30):
    if Z==30 and 12<=X<=18:continue
    self.put(X,1,Z,'polished_blackstone_bricks');self.slab(X,2,Z)
  for z in range(16,30):
   for x in range(12,19):self.put(x,1,z,'red_nether_bricks' if 14<=x<=16 else 'polished_blackstone')
   if z%4==0:self.put(15,1,z,'chiseled_polished_blackstone')
  for X in [11,19]:
   self.pillar(X,2,28,4,'polished_basalt');self.put(X,5,28,'chiseled_polished_blackstone')
   self.stairs(X,6,28,'east' if X==11 else 'west',half='top')
  for X,Y in [(12,6),(13,7),(14,8),(15,8),(16,8),(17,7),(18,6)]:
   self.put(X,Y,28,'polished_blackstone_bricks');self.slab(X,Y+1,28)
  for X in [13,17]:self.hanging_light(X,5,28)
  for z in [29,30]:self.fill(13,1,z,17,1,z,'air')
  for X in range(13,18):self.stairs(X,0,30,'north');self.stairs(X,1,29,'north')
 def save(self):
  # Store fence/bar connections explicitly, so generated railings are complete.
  for (x,y,z),(state,nbt) in self.blocks.items():
   if state['Name'] not in ['minecraft:dark_oak_fence','minecraft:iron_bars']:continue
   props=state.setdefault('Properties',{})
   for side,dx,dz in [('east',1,0),('west',-1,0),('north',0,-1),('south',0,1)]:
    neighbor=self.blocks.get((x+dx,y,z+dz),({'Name':'minecraft:air'},None))[0]['Name']
    props[side]=str(neighbor in [state['Name'],'minecraft:dark_oak_log','minecraft:dark_oak_planks','minecraft:polished_blackstone_bricks','minecraft:polished_basalt','minecraft:crimson_planks','minecraft:stripped_dark_oak_log','minecraft:chiseled_polished_blackstone']).lower()
  for (x,y,z),(state,_) in self.blocks.items():
   if not state['Name'].endswith('_wall'):continue
   props=state.setdefault('Properties',{})
   for side,dx,dz in [('east',1,0),('west',-1,0),('north',0,-1),('south',0,1)]:
    neighbor=self.blocks.get((x+dx,y,z+dz),({'Name':'minecraft:air'},None))[0]['Name']
    props[side]='low' if neighbor in [state['Name'],'minecraft:polished_blackstone_bricks','minecraft:polished_basalt','minecraft:chiseled_polished_blackstone'] else 'none'
  palette=[];ids={};blocks=[]
  for pos,(state,nbt) in sorted(self.blocks.items()):
   key=json.dumps(state,sort_keys=True)
   if key not in ids:ids[key]=len(palette);palette.append(state)
   entry={'pos':list(pos),'state':ids[key]}
   if nbt:entry['nbt']=nbt
   blocks.append(entry)
  model={'DataVersion':3465,'size':[31,self.height,31],'palette':palette,'blocks':blocks,'entities':self.entities}
  dump(ROOT/f'structures/{self.name}.nbt',model)
  schematic={'name':self.name,'size':[31,self.height,31],'population':[e['nbt']['id'] for e in self.entities],
   'blocks':[{'pos':list(pos),'state':state} for pos,(state,_) in self.blocks.items() if state['Name']!='minecraft:air']}
  Path('docs').mkdir(exist_ok=True);Path(f'docs/{self.name}-layout.json').write_text(json.dumps(schematic))
  print(self.name,len(blocks),'template blocks;',len(self.entities),'followers')

def shrine():
 s=Site('blood_shrine',18);s.base();s.altar(15,4,9,grand=True)
 s.building(3,18,9,8,'tent')
 s.building(21,5,8,8,'storage')
 s.cage(22,19,7,7);s.tower(4,6)
 for x in [11,20]:s.brazier(x,2,22)
 # A small outdoor ritual desk, grounded jars and short supported lamp chains.
 s.fill(11,2,18,11,2,20,'polished_blackstone_bricks');s.candles(11,3,18);s.put(11,3,20,'flower_pot')
 for x,z in [(3,15),(22,16)]:s.pillar(x,2,z,2);s.light(x,4,z)
 s.npc('blood_priest',13,4,13);s.npc('blood_priest',18,4,13);s.npc('blood_priest',7,3,24)
 s.npc('chain_punisher',6,10,8);s.npc('chain_punisher',21,2,26)
 s.save()

def camp():
 s=Site('chain_camp',18);s.base();s.altar(15,3,7)
 s.building(2,19,10,8,'barracks')
 s.building(21,5,8,9,'forge');s.brazier(25,2,16)
 s.building(21,21,8,7,'storage')
 s.cage(3,4,6,6);s.tower(3,11)
 # Armour stand/work bench annex outside the forge, with accessible circulation.
 s.fill(20,2,18,22,2,18,'polished_blackstone_bricks');s.put(21,3,18,'grindstone',{'face':'floor','facing':'south'})
 s.barrel(28,2,18);s.barrel(28,3,18);s.trapdoor(28,4,18)
 s.npc('blood_priest',13,3,11);s.npc('blood_priest',6,3,25)
 s.npc('chain_punisher',5,10,13);s.npc('chain_punisher',23,2,19);s.npc('chain_punisher',16,2,23)
 s.save()

if __name__=='__main__':
 shrine();camp()
 for name in ['blood_shrine','chain_camp']:
  js(Path(f'worldgen/structure/{name}.json'),{'type':'minecraft:jigsaw','biomes':'#zoldrax:has_structure/cult_sites','max_distance_from_center':80,'project_start_to_heightmap':'WORLD_SURFACE_WG','size':1,'spawn_overrides':{},'start_height':{'absolute':0},'start_pool':'zoldrax:'+name,'step':'surface_structures','terrain_adaptation':'beard_thin','use_expansion_hack':False})
  js(Path(f'worldgen/template_pool/{name}.json'),{'fallback':'minecraft:empty','elements':[{'weight':1,'element':{'element_type':'minecraft:single_pool_element','location':'zoldrax:'+name,'processors':'minecraft:empty','projection':'rigid'}}]})
 js(Path('worldgen/structure_set/cult_sites.json'),{'structures':[{'structure':'zoldrax:blood_shrine','weight':1},{'structure':'zoldrax:chain_camp','weight':1}],'placement':{'type':'minecraft:random_spread','spacing':32,'separation':12,'salt':40731529}})
 js(Path('tags/worldgen/biome/has_structure/cult_sites.json'),{'replace':False,'values':['#minecraft:is_forest','#minecraft:is_taiga','minecraft:plains','minecraft:savanna','minecraft:badlands','minecraft:wooded_badlands','minecraft:meadow']})
 def loot(entries):return {'type':'minecraft:chest','pools':[{'rolls':{'type':'minecraft:uniform','min':3,'max':6},'entries':[{'type':'minecraft:item','name':'minecraft:'+name,'weight':weight,'functions':[{'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':lo,'max':hi}}]} for name,weight,lo,hi in entries]}]}
 js(Path('loot_tables/chests/cult_treasure.json'),loot([('gold_ingot',6,2,6),('emerald',4,1,4),('diamond',1,1,2),('obsidian',5,2,6),('redstone',7,4,12),('chain',4,2,6)]))
 js(Path('loot_tables/chests/camp_supplies.json'),loot([('bread',7,3,8),('cooked_beef',4,2,6),('iron_ingot',5,1,5),('chain',6,2,8),('coal',7,3,10),('gold_nugget',5,4,12)]))
