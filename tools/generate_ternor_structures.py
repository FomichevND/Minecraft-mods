"""Reusable Ternor settlement rooms containing complete custom multiblocks.

These templates are intentionally not registered for natural generation yet: they are
stable integration pieces for future settlements and can already be placed with
`/place template zoldrax:ternor/<name>`.
"""
from pathlib import Path
from generate_cult_sites import Site

KINDS={
 'garden_canopy':(3,3,3,True),'storage_shelf':(3,1,3,True),
 'animal_feeder':(2,1,1,True),'animal_trough':(2,1,2,True),
 'stone_forge':(3,2,3,False),'forge_bellows':(2,1,2,False),
 'quenching_tub':(1,1,1,True),
}

class TernorRoom(Site):
 def __init__(self,name,width,length):
  super().__init__('ternor/'+name,8,width,length)
  self.fill(0,0,0,width-1,0,length-1,'stone_bricks')
  for x in range(1,width-1):
   for z in range(1,length-1):self.put(x,0,z,'mossy_stone_bricks' if (x*7+z*3)%11==0 else 'oak_planks')
  for x,z in [(0,0),(width-1,0),(0,length-1),(width-1,length-1)]:
   self.put(x,1,z,'stone_brick_wall',{'east':'none','north':'none','south':'none','up':True,'waterlogged':False,'west':'none'})
 def decor(self,x,y,z,name,water=0):
  width,depth,height,lit=KINDS[name]
  props={'facing':'south','part':0,'chain_length':1,'lit':lit}
  nbt={'id':'zoldrax:cult_decor'}
  if water:nbt['TernorWater']=water
  self.put(x,y,z,'zoldrax:ternor_'+name,props,nbt)
  for part in range(1,width*depth*height):
   dx=part%width;dy=part//(width*depth);dz=(part//width)%depth
   child=dict(props);child['part']=part
   self.put(x+dx,y+dy,z+dz,'zoldrax:ternor_'+name+'_part',child)

def workshop():
 s=TernorRoom('workshop',12,7);s.decor(1,1,1,'stone_forge');s.decor(5,1,1,'forge_bellows');s.decor(8,1,1,'quenching_tub',4)
 s.put(10,1,1,'barrel',{'facing':'up','open':False});s.put(10,1,4,'anvil',{'facing':'south'});s.save()
def farmyard():
 s=TernorRoom('farmyard',10,8);s.decor(1,1,1,'garden_canopy');s.decor(5,1,1,'animal_feeder');s.decor(5,1,4,'animal_trough',4)
 s.put(8,1,5,'composter',{'level':0});s.put(8,1,1,'hay_block',{'axis':'y'});s.save()
def storehouse():
 s=TernorRoom('storehouse',8,5);s.decor(2,1,1,'storage_shelf');s.put(6,1,1,'barrel',{'facing':'up','open':False});s.put(6,1,3,'chest',{'facing':'south','type':'single','waterlogged':False},{'id':'minecraft:chest'});s.save()

if __name__=='__main__':workshop();farmyard();storehouse()
