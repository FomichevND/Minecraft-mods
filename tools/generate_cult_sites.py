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
 def building(self,x,z,w,d,kind):
  # Stone footings, dark oak posts, accessible front and deep pitched red roof.
  self.fill(x,2,z,x+w-1,2,z+d-1,'dark_oak_planks')
  for X in [x,x+w-1]:
   for Z in [z,z+d-1]:self.pillar(X,3,Z,4,'dark_oak_log')
  if kind!='tent':
   self.fill(x,3,z,x+w-1,5,z,'dark_oak_planks');self.fill(x,3,z,x,4,z+d-1,'dark_oak_planks');self.fill(x+w-1,3,z,x+w-1,4,z+d-1,'dark_oak_planks')
  material='red_wool' if kind in ['tent','barracks'] else 'dark_oak_planks'
  for row in range((w+1)//2):
   self.fill(x+row,5+row,z-1,x+row,5+row,z+d,material)
   self.fill(x+w-1-row,5+row,z-1,x+w-1-row,5+row,z+d,material)
  for X in [x+1,x+w-2]:self.light(X,3,z+d-1)
  if kind in ['tent','barracks']:
   self.put(x+1,3,z+1,'red_bed',{'part':'foot','facing':'north','occupied':False});self.put(x+1,3,z,'red_bed',{'part':'head','facing':'north','occupied':False})
   self.put(x+w-2,3,z+1,'red_bed',{'part':'foot','facing':'north','occupied':False});self.put(x+w-2,3,z,'red_bed',{'part':'head','facing':'north','occupied':False})
   self.put(x+w//2,3,z+2,'lectern',{'facing':'south','has_book':False,'powered':False})
   for X in range(x+2,x+w-2):self.put(X,3,z+d-2,'red_carpet')
  self.banner(x+w//2,5,z+d,'south')
 def cage(self,x,z,w=6,d=6):
  self.fill(x,2,z,x+w-1,2,z+d-1,'cracked_polished_blackstone_bricks')
  for y in range(3,6):
   for X in range(x,x+w):
    for Z in range(z,z+d):
     if X in [x,x+w-1] or Z in [z,z+d-1]:
      self.put(X,y,Z,'iron_bars',{'east':X<x+w-1,'west':X>x,'north':Z>z,'south':Z<z+d-1,'waterlogged':False})
  self.fill(x,6,z,x+w-1,6,z+d-1,'dark_oak_slab',{'type':'bottom','waterlogged':False})
  mid=x+w//2
  for y,half in [(3,'lower'),(4,'upper')]:self.put(mid,y,z+d-1,'iron_door',{'facing':'south','half':half,'hinge':'left','open':False,'powered':False})
  self.put(mid-1,3,z+d,'stone_button',{'face':'wall','facing':'south','powered':False})
 def tower(self,x,z):
  for X in [x,x+4]:
   for Z in [z,z+4]:self.pillar(X,2,Z,10,'dark_oak_log')
  self.fill(x,9,z,x+4,9,z+4,'dark_oak_planks')
  self.fill(x-1,13,z-1,x+5,13,z+5,'polished_blackstone_slab',{'type':'bottom','waterlogged':False})
  for X in range(x,x+5):
   for Z in range(z,z+5):
    if X in [x,x+4] or Z in [z,z+4]:self.put(X,10,Z,'dark_oak_fence',{'waterlogged':False})
  self.fill(x+1,2,z-1,x+1,9,z-1,'dark_oak_planks')
  for y in range(2,10):self.put(x+1,y,z,'ladder',{'facing':'south','waterlogged':False})
  self.put(x+1,10,z,'air');self.light(x+3,10,z+3);self.banner(x+2,8,z+5)
 def altar(self,x,y,z):
  self.fill(x-4,2,z-3,x+5,y-1,z+6,'polished_blackstone_bricks')
  for j in range(y-2):self.fill(x-2,2+j,z+7-j,x+2,2+j,z+7-j,'polished_blackstone_stairs',{'facing':'north','half':'bottom','shape':'straight','waterlogged':False})
  self.put(x,y,z,'zoldrax:monument',{'facing':'south'},{'id':'zoldrax:monument','Stage':2,'LastOffering':Long(-1),'CultRitualUntil':Long(-1),'Devotees':[]})
  for X in [x-4,x+5]:
   self.pillar(X,y,z-1,7);self.put(X,y+7,z-1,'chiseled_polished_blackstone');self.banner(X,y+5,z,'south');self.light(X,y,z+5)
 def npc(self,name,x,y,z):self.entities.append({'pos':[float(x)+.5,float(y),float(z)+.5],'blockPos':[x,y,z],'nbt':{'id':'zoldrax:'+name,'PersistenceRequired':Byte(1),'Rotation':[Float(0),Float(0)]}})
 def base(self):
  # Explicit air clears only this generated footprint in NEW chunks.
  self.fill(0,0,0,30,self.height-1,30,'air')
  self.fill(0,0,0,30,0,30,'basalt');self.fill(1,1,1,29,1,29,'polished_blackstone_bricks')
  for x in range(1,30):
   for z in range(1,30):
    n=self.rng.random()
    if n<.12:self.put(x,1,z,'cracked_polished_blackstone_bricks')
    elif n<.16:self.put(x,1,z,'red_nether_bricks')
  for X in [1,29]:
   for Z in range(2,29,4):self.pillar(X,2,Z,3);self.light(X,5,Z)
  for x in range(2,29,4):
   for z in [1,29]:
    if 12<=x<=18:continue
    self.pillar(x,2,z,3);self.light(x,5,z)
  for z in [29,30]:self.fill(13,1,z,17,1,z,'air')
  self.fill(13,0,30,17,0,30,'polished_blackstone_stairs',{'facing':'north','half':'bottom','shape':'straight','waterlogged':False})
  self.fill(13,1,29,17,1,29,'polished_blackstone_stairs',{'facing':'north','half':'bottom','shape':'straight','waterlogged':False})
 def save(self):
  # Store fence/bar connections explicitly, so generated railings are complete.
  for (x,y,z),(state,nbt) in self.blocks.items():
   if state['Name'] not in ['minecraft:dark_oak_fence','minecraft:iron_bars']:continue
   props=state.setdefault('Properties',{})
   for side,dx,dz in [('east',1,0),('west',-1,0),('north',0,-1),('south',0,1)]:
    neighbor=self.blocks.get((x+dx,y,z+dz),({'Name':'minecraft:air'},None))[0]['Name']
    props[side]=str(neighbor in [state['Name'],'minecraft:dark_oak_log','minecraft:dark_oak_planks','minecraft:polished_blackstone_bricks']).lower()
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
 s=Site('blood_shrine',18);s.base();s.altar(15,4,9)
 s.building(3,18,9,8,'tent');s.loot(9,3,20)
 s.building(21,5,8,8,'storage');s.loot(22,3,7);s.loot(26,3,7);s.put(25,3,10,'barrel',{'facing':'up','open':False})
 s.cage(22,19,7,7);s.tower(4,6)
 for x in [11,20]:s.brazier(x,2,22)
 for x in range(8,23):s.put(x,11,8,'chain',{'axis':'x','waterlogged':False})
 for x,z in [(3,16),(21,16)]:s.pillar(x,2,z,5);s.banner(x,5,z+1)
 s.npc('blood_priest',13,4,13);s.npc('blood_priest',18,4,13);s.npc('blood_priest',7,3,23)
 s.npc('chain_punisher',6,10,8);s.npc('chain_punisher',21,2,23)
 s.save()

def camp():
 s=Site('chain_camp',16);s.base();s.altar(15,3,7)
 s.building(2,19,10,8,'barracks');s.loot(9,3,21,'supplies')
 s.building(21,5,8,9,'forge');s.put(23,3,7,'blast_furnace',{'facing':'south','lit':False});s.put(26,3,7,'furnace',{'facing':'south','lit':False});s.put(25,3,11,'anvil',{'facing':'north'});s.brazier(25,2,16)
 for x in [23,25,27]:s.put(x,5,9,'chain',{'axis':'y','waterlogged':False})
 s.building(21,21,8,7,'storage');s.loot(22,3,23,'supplies');s.loot(26,3,23);s.put(24,3,25,'barrel',{'facing':'up','open':False})
 s.cage(3,4,6,6);s.tower(3,11)
 for z in range(8,18):s.put(27,9,z,'chain',{'axis':'z','waterlogged':False})
 s.npc('blood_priest',13,3,11);s.npc('blood_priest',6,3,24)
 s.npc('chain_punisher',5,10,13);s.npc('chain_punisher',24,2,18);s.npc('chain_punisher',16,2,23)
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
