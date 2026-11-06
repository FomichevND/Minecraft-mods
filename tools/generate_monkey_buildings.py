"""Four independent buildings from the user's kitchen/store/house/pavilion references.
Floor footprints are 8x6, 7x6, 7x6 and 5x5; templates include stairs/eaves.
No full-size Monkey King monument is placed in these small decorative buildings.
"""
from pathlib import Path
from generate_cult_sites import Site,Byte,Float,js

class Building(Site):
 def __init__(self,name,w,d):
  super().__init__('monkey_buildings/'+name,22,w+6,d+8)
  self.w=w;self.d=d;self.x=3;self.z=3;self.front=3+d-1
  self.fill(2,9,2,w+3,20,d+3,'air')
  for x in range(3,3+w):
   for z in range(3,3+d):
    self.fill(x,0,z,x,7,z,'stone');self.put(x,8,z,'mossy_stone_bricks' if (x+z)%4==0 else 'stone_bricks')
    self.put(x,9,z,'stone_bricks' if x in [3,w+2] or z in [3,d+2] else 'jungle_planks')
  cx=3+w//2
  for x in [cx-1,cx] if w>5 else [cx]:
   for step in range(3):
    z=self.front+1+step;y=9-step
    self.fill(x,0,z,x,y-1,z,'mossy_stone_bricks');self.stairs(x,y,z,'north','stone_brick')
 def npc(self,name,x,y,z):
  super().npc(name,x,y,z);self.entities[-1]['nbt']['HomeResident']=Byte(1)
 def banner(self,x,y,z,facing='south'):
  self.put(x,y,z,'red_wall_banner',{'facing':facing},{'id':'minecraft:banner','Patterns':[{'Color':4,'Pattern':'zbanana'}]})
 def loot(self,x,y,z,kind='supplies',facing='south'):
  self.put(x,y,z,'chest',{'facing':facing,'type':'single','waterlogged':False},{'id':'minecraft:chest','LootTable':'zoldrax:chests/monkey_supplies'})
 def frame(self):
  for x in [self.x,self.x+self.w-1]:
   for z in [self.z,self.front]:
    self.fill(x,10,z,x,12,z,'bamboo_block',{'axis':'y'})
    self.put(x,9,z,'chiseled_stone_bricks')
  for z in [self.z,self.front]:self.fill(self.x,12,z,self.x+self.w-1,12,z,'jungle_log',{'axis':'x'})
  for x in [self.x,self.x+self.w-1]:self.fill(x,12,self.z,x,12,self.front,'jungle_log',{'axis':'z'})
 def roof(self):
  bx,bz,w,d=self.x,self.z,self.w,self.d
  for X in range(bx-1,bx+w+1):
   off=X-bx;rise=min(off+1,w-off);y=12+rise//2;facing='east' if off<(w-1)/2 else 'west'
   for Z in range(bz-1,bz+d+1):
    trim=Z in [bz-1,bz+d] or rise==0
    if rise%2:
     self.put(X,y,Z,'bamboo_planks' if trim else 'red_nether_bricks')
    else:self.stairs(X,y,Z,facing,'bamboo' if trim else 'red_nether_brick')
   if bx<=X<bx+w:
    for Z in [bz,self.front]:
     if y>12:self.fill(X,12,Z,X,y-1,Z,'jungle_planks')
  for x in [bx-1,bx+w]:
   for z in [bz-1,bz+d]:self.put(x,12,z,'gold_block')
  cx=bx+w//2;top=12+min(cx-bx+1,w-(cx-bx))//2
  for z in [bz-1,bz+d]:
   for x in range(cx-1,cx+2):self.put(x,top+1,z,'gold_block')
   self.put(cx,top+1,z,'red_wool');self.put(cx,top+2,z,'yellow_carpet')
 def entrance_headroom(self):
  # The NPC hitbox is under two blocks, but crowns/hats extend above it.
  cx=self.x+self.w//2
  for x in ([cx-1,cx] if self.w>5 else [cx]):self.put(x,12,self.front,'air')
 def hip_roof(self):
  # Four-sided stepped canopy, distinct from the gabled residential roofs.
  for ring in range(3):
   loX=self.x-1+ring;hiX=self.x+self.w-ring;loZ=self.z-1+ring;hiZ=self.z+self.d-ring;y=12+ring
   self.fill(loX,y,loZ,hiX,y,hiZ,'jungle_planks' if ring==0 else 'red_nether_bricks')
   mat='bamboo' if ring==0 else 'red_nether_brick'
   for X in range(loX+1,hiX):self.stairs(X,y,loZ,'south',mat);self.stairs(X,y,hiZ,'north',mat)
   for Z in range(loZ+1,hiZ):self.stairs(loX,y,Z,'east',mat);self.stairs(hiX,y,Z,'west',mat)
  for X in [self.x-1,self.x+self.w]:
   for Z in [self.z-1,self.z+self.d]:self.put(X,12,Z,'gold_block')
  self.fill(self.x+1,12,self.z+1,self.x+self.w-2,12,self.front-1,'air')
  self.put(self.x+2,15,self.z+2,'gold_block')
 def lamp(self,x,z):
  self.put(x,12,z,'chain',{'axis':'y','waterlogged':False});self.put(x,11,z,'lantern',{'hanging':True,'waterlogged':False})
 def rail(self):
  cx=self.x+self.w//2
  for x in range(self.x+1,self.x+self.w-1):
   if x in ([cx-1,cx] if self.w>5 else [cx]):continue
   self.put(x,10,self.front,'bamboo_fence',{'waterlogged':False})
  for x in [self.x,self.x+self.w-1]:
   for z in range(self.z+1,self.front):self.put(x,10,z,'bamboo_fence',{'waterlogged':False})
 def walls(self):
  # The front porch occupies the last row of the reference footprint.
  back=self.z;door=self.front-1;cx=self.x+self.w//2
  self.fill(self.x,10,back,self.x+self.w-1,11,back,'jungle_planks')
  for x in [self.x,self.x+self.w-1]:
   self.fill(x,10,back,x,11,door,'jungle_planks')
   self.put(x,11,back+2,'jungle_trapdoor',{'facing':'east' if x==self.x else 'west','half':'bottom','open':True,'powered':False,'waterlogged':False})
   self.put(x,10,back+2,'bamboo_block',{'axis':'y'})
  self.fill(self.x,10,door,self.x+self.w-1,11,door,'jungle_planks')
  for x in [cx-1,cx]:
   for y,half in [(10,'lower'),(11,'upper')]:self.put(x,y,door,'jungle_door',{'facing':'south','half':half,'hinge':'left' if x==cx-1 else 'right','open':False,'powered':False})
  for x in [self.x,self.x+self.w-1]:
   for z in [back,door]:self.fill(x,10,z,x,12,z,'stripped_jungle_log',{'axis':'y'})
 def decor(self):
  for x in [self.x+1,self.x+self.w-2]:self.banner(x,12,self.front+1)
  for x in [self.x+1,self.x+self.w-2]:self.lamp(x,self.front)
 def banana(self,x,y,z):
  self.entities.append({'pos':[x+.5,y+.2,z+.5],'blockPos':[x,y,z],'nbt':{
   'id':'minecraft:item_display','Invulnerable':Byte(1),'item':{'id':'zoldrax:golden_banana','Count':Byte(1)},'item_display':'ground',
   'transformation':{'translation':[Float(0),Float(0),Float(0)],'scale':[Float(.8),Float(.8),Float(.8)],'left_rotation':[Float(0),Float(0),Float(0),Float(1)],'right_rotation':[Float(0),Float(0),Float(0),Float(1)]}}})
 def save(self):
  for (x,y,z),(state,_) in self.blocks.items():
   if not state['Name'].endswith('_fence'):continue
   for side,dx,dz in [('east',1,0),('west',-1,0),('north',0,-1),('south',0,1)]:
    n=self.blocks.get((x+dx,y,z+dz),({'Name':'minecraft:air'},None))[0]['Name']
    state.setdefault('Properties',{})[side]=str(n.endswith(('_fence','_log','_planks','_block')) or n=='minecraft:stone_bricks').lower()
  self.entrance_headroom()
  super().save()
  import json
  path=Path('docs')/(self.name+'-layout.json');layout=json.loads(path.read_text());layout['entities']=self.entities;layout['population']=[e['nbt']['id'] for e in self.entities if e['nbt']['id'] in ['zoldrax:banana_herald','zoldrax:temple_gatherer']];path.write_text(json.dumps(layout))

def kitchen():
 s=Building('kitchen',8,6);x,z=s.x,s.z;s.frame();s.rail()
 # Rear cooking wall, a pair of working furnaces and a separate smoker.
 for X in range(x+3,x+7):s.put(X,10,z,'stone_bricks')
 for X in [x+3,x+4]:s.put(X,10,z,'furnace',{'facing':'south','lit':False})
 s.put(x+5,10,z,'smoker',{'facing':'south','lit':False});s.fill(x+4,11,z,x+4,16,z,'stone_bricks');s.slab(x+4,17,z,'stone_brick')
 s.put(x+6,10,z,'crafting_table');s.barrel(x+1,10,z+1);s.barrel(x+1,10,z+2)
 for Z in [z+3,z+4]:s.barrel(x+1,10,Z);s.trapdoor(x+1,11,Z,'south',False,'jungle')
 s.put(x+2,11,z+4,'potted_red_tulip');s.put(x+2,10,z+4,'jungle_planks')
 s.loot(x+6,10,z+2);s.put(x+6,10,z+4,'composter',{'level':0});s.put(x+6,11,z+4,'potted_bamboo')
 s.roof();s.fill(x+4,11,z,x+4,16,z,'stone_bricks');s.slab(x+4,17,z,'stone_brick');s.decor();s.banana(x+1,11,z+3);s.npc('temple_gatherer',x+2,10,z+3);s.save()

def storehouse():
 s=Building('storehouse',7,6);x,z=s.x,s.z;s.frame();s.walls()
 for X in [x+1,x+3,x+5]:
  for y in [10,11]:s.loot(X,y,z+1)
 for X in [x+1,x+5]:
  for Z in [z+2,z+3]:
   for y in [10,11]:s.barrel(X,y,Z)
 for Z in [z+2,z+3]:s.put(x+3,10,Z,'red_carpet')
 s.roof();s.decor();s.npc('temple_gatherer',x+2,10,z+3);s.npc('temple_gatherer',x+4,10,z+3);s.save()

def herald_house():
 s=Building('herald_house',7,6);x,z=s.x,s.z;s.frame();s.walls();s.rail()
 s.fill(x+1,10,z+1,x+2,11,z+1,'bookshelf');s.loot(x+1,10,z+3)
 s.put(x+2,10,z+3,'lectern',{'facing':'south','has_book':True,'powered':False},{'id':'minecraft:lectern','Book':{'id':'minecraft:written_book','Count':Byte(1),'tag':{'title':'Банановые воззвания','author':'Банановый глашатай','pages':['{"text":"Да здравствует Король Обезьян!"}']}},'Page':0})
 for Z,part in [(z+1,'head'),(z+2,'foot')]:s.put(x+5,10,Z,'red_bed',{'facing':'north','occupied':False,'part':part},{'id':'minecraft:bed'})
 for Z in [z+2,z+3]:s.put(x+3,10,Z,'red_carpet')
 s.roof();s.decor();s.npc('banana_herald',x+4,10,z+3);s.save()

def pavilion():
 s=Building('pavilion',5,5);x,z=s.x,s.z;s.frame();s.rail()
 s.fill(x+1,10,z+1,x+3,10,z+1,'jungle_planks')
 for X in [x+1,x+3]:s.candles(X,11,z+1,2)
 s.put(x+2,11,z+1,'red_carpet');s.banner(x+2,10,z+2)
 s.hip_roof()
 for X in [x,x+4]:s.banner(X,12,s.front+1)
 for X in [x+1,x+3]:s.lamp(X,s.front)
 s.banana(x+2,11,z+1);s.npc('banana_herald',x+1,10,z+3);s.npc('temple_gatherer',x+3,10,z+3);s.save()

if __name__=='__main__':
 kitchen();storehouse();herald_house();pavilion()
 names=['kitchen','storehouse','herald_house','pavilion']
 for name in names:
  js(Path(f'worldgen/structure/monkey_{name}.json'),{'type':'zoldrax:monkey_building','template':name,'biomes':'#zoldrax:has_structure/monkey_sites','spawn_overrides':{},'step':'surface_structures','terrain_adaptation':'none'})
 js(Path('worldgen/structure_set/monkey_buildings.json'),{'structures':[{'structure':'zoldrax:monkey_'+n,'weight':1} for n in names],'placement':{'type':'minecraft:random_spread','spacing':18,'separation':7,'salt':52611004}})
