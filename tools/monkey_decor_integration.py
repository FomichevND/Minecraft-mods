"""Insert real multiblock furniture into new settlement templates only."""
from generate_cult_sites import Byte
CONFIG={'food_shelf':(3,2,1),'preparation_table':(3,2,1),'supply_basket':(1,1,1),'drying_rack':(3,2,1),'courtyard_gong':(3,3,1),'festival_bench':(3,2,1),'notice_board':(3,3,1),'festival_drum':(2,2,2)}
def place(s,kind,x,y,z,foods=()):
 w,h,d=CONFIG[kind];id='zoldrax:monkey_king_'+kind
 # Ensure every foot has local support, without making a shared settlement platform.
 for dx in range(w):
  for dz in range(d):s.put(x+dx,y-1,z+dz,'jungle_planks')
 for dy in range(h):
  for dz in range(d):
   for dx in range(w):
    part=dy*w*d+dz*w+dx;nbt={'id':'zoldrax:cult_decor','Items':[{'Slot':Byte(i),'id':'minecraft:'+item,'Count':Byte(1)} for i,item in enumerate(foods)]} if part==0 else None
    s.put(x+dx,y+dy,z+dz,id+('_part' if part else ''),{'facing':'south','part':part,'lit':True,'chain_length':1},nbt)
def decorate_component(s,name):
 if name in ['court_temple','valley_temple']:
  place(s,'food_shelf',2,2,7,('apple','melon_slice','bread'));place(s,'festival_bench',3,2,10)
 elif name=='festival':place(s,'courtyard_gong',3,3,3);place(s,'festival_drum',8,3,7)
 elif name=='herald_hall':place(s,'notice_board',4,2,13)
 elif name=='kitchen':place(s,'preparation_table',5,2,7,('bread','carrot'))
 elif name=='storehouse':place(s,'supply_basket',5,2,7,('apple','carrot','potato'));place(s,'supply_basket',9,2,7,('bread',))
 elif name=='gatherer_hall':place(s,'drying_rack',5,2,13,('cod','beef','carrot','dried_kelp','salmon'))
