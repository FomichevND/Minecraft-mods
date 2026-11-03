"""Standalone 3D held item, intentionally separate from the priest's body mesh."""
from pathlib import Path
from PIL import Image,ImageDraw
import json,random
r=Path('src/main/resources/assets/zoldrax');tex=r/'textures/item';tex.mkdir(parents=True,exist_ok=True)
image=Image.new('RGBA',(64,64));rng=random.Random(32)
colors=[(64,33,22),(130,88,26),(38,31,44),(235,101,18)]
for row,c in enumerate(colors):
 for y in range(row*16,row*16+16):
  for x in range(64):
   n=rng.randint(-6,6);image.putpixel((x,y),(*[max(0,min(255,v+n)) for v in c],255))
image.save(tex/'ritual_staff.png')
el=[]
def cube(name,lo,hi,kind):
 y=kind*4
 faces={d:{'uv':[0,y,4,y+4],'texture':'#staff'} for d in ['up','down','east','west','north','south']}
 el.append({'name':name,'from':lo,'to':hi,'faces':faces,'shade':True})
cube('shaft',[7,-8,7],[9,25,9],0)
for y in [-7,9,22]:cube('gold_band'+str(y),[6,y,6],[10,y+2,10],1)
cube('brazier',[4,23,4],[12,27,12],2)
cube('gold_rim',[3,27,3],[13,28,13],1)
for x,z in [(4,4),(10,4),(4,10),(10,10)]:cube('frame'+str(x)+str(z),[x,24,z],[x+2,28,z+2],2)
cube('fire_core',[6,28,6],[10,32,10],3)
cube('fire_left',[4,28,7],[6,30,9],3)
cube('fire_right',[10,28,6],[12,31,9],3)
model={'parent':'minecraft:block/block','textures':{'staff':'zoldrax:item/ritual_staff','particle':'zoldrax:item/ritual_staff'},'elements':el,'display':{
 'thirdperson_righthand':{'rotation':[90,0,0],'translation':[2,0,0],'scale':[1,1,1]},
 'thirdperson_lefthand':{'rotation':[90,0,0],'translation':[2,0,0],'scale':[1,1,1]},
 'firstperson_righthand':{'rotation':[0,0,-12],'translation':[1,-1,0],'scale':[.65,.65,.65]},
 'firstperson_lefthand':{'rotation':[0,0,12],'translation':[1,-1,0],'scale':[.65,.65,.65]},
 'gui':{'rotation':[25,215,-20],'translation':[0,-2,0],'scale':[.5,.5,.5]},
 'ground':{'rotation':[0,0,0],'translation':[0,5,0],'scale':[.4,.4,.4]},
 'fixed':{'rotation':[0,180,0],'translation':[0,-2,0],'scale':[.5,.5,.5]}}}
(r/'models/item/ritual_staff.json').write_text(json.dumps(model,indent=2))
print('Standalone staff item model generated')
