"""Voxel reconstruction of the seven supplied Ternor references, vanilla light and materials."""
from pathlib import Path
from PIL import Image,ImageDraw,ImageEnhance
import json,math,io,zipfile,base64,uuid,os
from generate_zoldrax_decor import Model,rotated_bounds
ROOT=Path('src/main/resources/assets/zoldrax');NAMES=['garden_canopy','storage_shelf','animal_feeder','animal_trough','stone_forge','forge_bellows','quenching_tub']
MATS=['stone','edge','moss','wood','planks','iron','gold','blue','banner','leather','dark','fire','water','leaf','dirt','paper']
def atlas():
 im=Image.new('RGBA',(64,64));jar=Path(os.environ.get('MINECRAFT_CLIENT_JAR','/workspace/work/gradle-home/caches/fabric-loom/1.20.1/minecraft-client.jar'))
 with zipfile.ZipFile(jar) as z:
  for i,tile in enumerate(['stone_bricks','smooth_stone','mossy_stone_bricks','stripped_oak_log','oak_planks','iron_block','yellow_terracotta','blue_wool','blue_wool','brown_terracotta','deepslate_tiles','orange_concrete','blue_concrete','oak_leaves','dirt','white_wool']):
   p=Image.open(io.BytesIO(z.read('assets/minecraft/textures/block/'+tile+'.png'))).convert('RGBA').crop((0,0,16,16))
   if i==6:p=ImageEnhance.Color(p).enhance(.65)
   im.paste(p,(i%4*16,i//4*16))
 d=ImageDraw.Draw(im);x=0;y=32;d.rectangle((x,y,x+15,y+15),outline=(179,147,65),width=1)
 # Ternor's eight-toothed sun/gear, with a clear blue centre.
 d.ellipse((x+4,y+4,x+11,y+11),fill=(190,154,61))
 for dx,dy in [(7,2),(7,12),(2,7),(12,7),(3,3),(11,3),(3,11),(11,11)]:d.rectangle((x+dx,y+dy,x+dx+1,y+dy+1),fill=(190,154,61))
 d.rectangle((x+6,y+6,x+9,y+9),fill=(46,66,115))
 # Block-entity meshes do not receive vanilla biome tinting. Bake a restrained
 # leaf green into the atlas instead of leaving oak leaves bright grey.
 leaf=im.crop((16,48,32,64));green=Image.new('RGBA',(16,16),(68,105,48,255))
 leaf=Image.blend(leaf,green,.62);im.paste(leaf,(16,48))
 return im

def write(m,folder='ternor_decor'):
 image=getattr(m,"image",None) or atlas();buf=io.BytesIO();image.save(buf,format='PNG');png=buf.getvalue();elements=[]
 for c in m.c:
  i=MATS.index(c['material']);u=i%4*16+.5;v=i//4*16+.5
  faces={f:{'uv':[u,v,u+15,v+15],'texture':0} for f in ['north','south','east','west','up','down']};e={k:c[k] for k in ['name','from','to','rotation','origin']};e.update(type='cube',uuid=str(uuid.uuid5(uuid.NAMESPACE_URL,m.name+c['name'])),faces=faces,box_uv=False,zoldrax_group=c['group'],zoldrax_emissive=c['emissive']);elements.append(e)
 model={'meta':{'format_version':'4.10','model_format':'free','box_uv':False},'name':m.name,'resolution':{'width':64,'height':64},'elements':elements,'outliner':[{'name':g,'uuid':str(uuid.uuid5(uuid.NAMESPACE_URL,m.name+g)),'origin':[0,0,0],'rotation':[0,0,0],'children':[e['uuid'] for e in elements if e['zoldrax_group']==g]} for g in dict.fromkeys(c['group'] for c in m.c)],'textures':[{'id':'0','name':m.name+'.png','width':64,'height':64,'uv_width':64,'uv_height':64,'source':'data:image/png;base64,'+base64.b64encode(png).decode()}]}
 text=json.dumps(model,indent=2);design=Path('design/blockbench')/folder;design.mkdir(parents=True,exist_ok=True);(design/(m.name+'.bbmodel')).write_text(text)
 runtime=ROOT/'models/blockbench'/('decor' if folder=='ternor_decor' else 'ternor_props');runtime.mkdir(parents=True,exist_ok=True);(runtime/(m.name+'.bbmodel')).write_text(text);(ROOT/'textures/block/zoldrax'/(m.name+'.png')).write_bytes(png)
 if folder=='ternor_decor':
  for suffix in ['','_part']:(ROOT/'blockstates'/f'{m.name+suffix}.json').write_text(json.dumps({'variants':{'':{'model':'zoldrax:block/'+m.name}}}))
  (ROOT/'models/block'/f'{m.name}.json').write_text(json.dumps({'parent':'builtin/entity','textures':{'particle':'zoldrax:block/zoldrax/'+m.name}}))
  (ROOT/'models/item'/f'{m.name}.json').write_text(json.dumps({'parent':'builtin/entity','display':{'gui':{'rotation':[20,45,0]},'ground':{'scale':[.6,.6,.6]},'firstperson_righthand':{'rotation':[0,45,0],'scale':[.8,.8,.8]}}}))
 print(m.name,len(m.c));return m

def box(m,n,x,y,z,w,h,d,mat='stone',solid=False,group='body',rot=None,origin=None,emit=False):return m.box(n,x,y,z,w,h,d,mat,group,emit,rot,origin,solid)
def flag(m,n,x,y,z,w=9,h=14):
 box(m,n,x,y,z,w,h,.35,'banner');box(m,n+'_tail',x+2,y-2,z,w-4,2,.35,'blue');box(m,n+'_rod',x-.5,y+h,z-.2,w+1,.8,.8,'gold')
def stud(m,n,x,y,z):box(m,n,x,y,z,1.1,1.1,.45,'iron')
def lantern(m,n,x,y,z):
 box(m,n+'_hook',x,y+8,z,1,4,1,'iron');box(m,n+'_cap',x-2,y+6,z-2,5,1,5,'iron');box(m,n+'_base',x-2,y,z-2,5,1,5,'iron');box(m,n+'_core',x-1,y+1,z-1,3,5,3,'fire',emit=True)
 for dx in [-2,2]:
  for dz in [-2,2]:box(m,n+f'_bar{dx}{dz}',x+dx,y,z+dz,.65,7,.65,'iron')
def planter(m,n,x,z,w=11,d=11):
 box(m,n+'_floor',x,0,z,w,1,d,'wood',True)
 for xx,zz,ww,dd in [(x,z,w,1),(x,z+d-1,w,1),(x,z+1,1,d-2),(x+w-1,z+1,1,d-2)]:box(m,n+f'_wall{xx}{zz}',xx,1,zz,ww,9,dd,'planks',True)
 box(m,n+'_soil',x+1,5,z+1,w-2,1,d-2,'dirt',True)
def basin(m):
 box(m,'bottom',1,0,1,30,2,14,'stone',True)
 for x,z,w,d in [(0,0,32,2),(0,14,32,2),(0,2,2,12),(30,2,2,12)]:
  box(m,f'low_wall{x}_{z}',x,2,z,w,5,d,'stone',True);box(m,f'bevel{x}_{z}',x-.1,7,z-.1,w+.2,1.5,d+.2,'edge',True)
 for x in [0,27]:
  box(m,'corner'+str(x),x,0,0,5,13,5,'moss' if x==0 else 'stone',True);stud(m,'corner_pin'+str(x),x+2,8,5.1)
 flag(m,'trough_flag',12,1,16.15,8,9)
def furniture(n):
 m=Model('ternor_'+n)
 if n=='garden_canopy':
  for x in [1,42]:
   for z in [1,42]:
    box(m,f'foot{x}_{z}',x-1,0,z-1,7,5,7,'moss',True);box(m,f'post{x}_{z}',x,5,z,5,39,5,'wood',True);box(m,f'iron_band{x}_{z}',x-.1,7,z-.1,5.2,2,5.2,'iron')
  for x,z,w,d in [(0,0,48,6),(0,42,48,6),(0,6,6,36),(42,6,6,36)]:box(m,f'beam{x}_{z}',x,43,z,w,5,d,'planks',True)
  for x in [10,22,34]:box(m,'roof_lath'+str(x),x,45,3,2,2,42,'wood')
  planter(m,'front_left',3,3);planter(m,'front_right',34,3);planter(m,'rear',14,33,20,11)
  lantern(m,'left_lantern',8,26,8);lantern(m,'right_lantern',39,26,8);flag(m,'canopy_flag',20,30,6.2,9,14)
  for i,x in enumerate([7,14,29,39]):
   box(m,'leaves'+str(i),x,46,3,5,3,7,'leaf',group='greenery')
   for j in range(3):box(m,f'vine{i}_{j}',x+(j%2),39-j*3,6,1.5,4,1,'leaf',group='greenery')
 elif n=='storage_shelf':
  for x in [0,43]:box(m,'upright'+str(x),x,0,2,5,46,10,'wood',True)
  for j,y in enumerate([3,18,33]):
   box(m,'shelf'+str(j),1,y,1,46,2,14,'planks',True)
   for x in [0,42]:box(m,f'corner{j}_{x}',x-.08,y-.1,.8,6,2.2,14.4,'iron');stud(m,f'bolt{j}_{x}',x+2,y+.3,15.3)
  box(m,'top_beam',0,45,1,48,3,14,'wood',True);flag(m,'shelf_flag',5,34,15.4,9,13)
  for j in range(4):box(m,'side_moss'+str(j),.1,25-j*3,12,1,4,1,'leaf')
 elif n in ['animal_feeder','animal_trough']:
  basin(m)
  if n=='animal_feeder':box(m,'wooden_liner',2,2.02,2,28,.5,12,'planks',True)
  else:
   box(m,'lantern_post',28,8,1,2,13,2,'wood',True);lantern(m,'trough_lantern',28.5,19,1.5)
   box(m,'water_surface',2.1,3,2.1,27.8,.12,11.8,'water',group='water')
 elif n=='stone_forge':
  box(m,'forge_foundation',0,0,0,32,4,31,'moss',True)
  for x in [2,24]:
   for y in [4,12,20]:box(m,f'pillar{x}_{y}',x,y,7,7,8,19,'stone',True)
  box(m,'back_wall',8,4,4,16,24,5,'dark',True);box(m,'firebed',9,4,9,15,2,15,'dark',True)
  box(m,'lintel',0,28,6,33,5,23,'edge',True);box(m,'lower_chimney',5,33,8,24,6,18,'stone',True);box(m,'upper_chimney',10,39,11,14,5,12,'edge',True);box(m,'chimney_pot',12,44,13,10,4,8,'iron',True)
  box(m,'front_step',6,0,26,22,5,6,'stone',True);flag(m,'forge_badge',13,27,29.2,7,5)
  for x in [3,25]:stud(m,'forge_bolt'+str(x),x,30,29.2)
  # Separate anvil and a tool rail to the right, joined by the foundation.
  box(m,'anvil_foot',33,0,9,14,3,16,'edge',True);box(m,'anvil_stem',36,3,13,8,7,8,'iron',True);box(m,'anvil_table',32,10,8,16,4,17,'iron',True);box(m,'anvil_horn',30,11,10,4,2,10,'iron',True)
  box(m,'tool_rail',32,18,4,16,2,2,'wood',True)
  for x in [34,43]:box(m,'tool_handle'+str(x),x,14,4,1,13,1,'wood');box(m,'tool_head'+str(x),x-1,25,3,3,4,2,'iron')
  for i,(x,z,h) in enumerate([(10,15,9),(16,13,13),(21,17,8)]):
   for j in range(h//3):box(m,f'fire{i}_{j}',x+j*.2,6+j*3,z,3-j*.3,3.1,3-j*.3,'fire',group='flame',emit=True)
 elif n=='forge_bellows':
  box(m,'frame_bottom',13,0,1,34,3,14,'wood',True)
  for x in [15,42]:box(m,'frame_leg'+str(x),x,0,3,4,23,10,'wood',True);box(m,'leg_band'+str(x),x-.1,2,2.9,4.2,3,10.2,'iron')
  box(m,'bellows_base',10,5,3,34,2,10,'leather',True)
  # Stepped leather wedges, hollow between the two boards at the nozzle.
  for i in range(7):box(m,'leather_fold'+str(i),10+i*4.5,7+i*.9,3,4.5,2+i*.35,10,'leather',group='pump')
  box(m,'top_platen',34,19,1,13,4,14,'planks',True,group='pump');flag(m,'bellows_flag',38,6,15.3,8,16)
  box(m,'nozzle',5,5,6,8,4,4,'iron',True);box(m,'hinge_post',1,0,1,3,28,3,'wood',True);box(m,'hinge_beam',1,26,1,23,3,3,'wood',True)
  for y in [15,19,23]:
   for x,z,w,d in [(21,2,3,.5),(21,4.5,3,.5),(21,2.5,.5,2),(23.5,2.5,.5,2)]:box(m,f'chain{y}_{x}_{z}',x,y,z,w,3,d,'iron')
  box(m,'stone_pivot_foot',0,0,0,6,5,6,'stone',True)
 elif n=='quenching_tub':
  box(m,'wooden_tub',1,0,1,14,10,14,'wood',True)
  # Open interior: replace the solid wooden body with separate walls and a floor.
  m.c.pop();m.collision.pop();box(m,'tub_floor',1,0,1,14,2,14,'wood',True)
  for x,z,w,d in [(1,1,14,2),(1,13,14,2),(1,3,2,10),(13,3,2,10)]:box(m,f'tub_wall{x}_{z}',x,2,z,w,9,d,'planks',True)
  for y in [2,8]:
   for x,z,w,d in [(0.8,.8,14.4,.8),(.8,14.4,14.4,.8),(.8,1.6,.8,12.8),(14.4,1.6,.8,12.8)]:box(m,f'band{y}_{x}_{z}',x,y,z,w,1.5,d,'iron')
  for x,z,w,d in [(0,0,16,2),(0,14,16,2),(0,2,2,12),(14,2,2,12)]:box(m,f'stone_rim{x}_{z}',x,11,z,w,2,d,'moss' if x==0 else 'stone',True)
  flag(m,'tub_flag',5,2,15.3,6,8);box(m,'water_surface',3,3,3,10,.12,10,'water',group='water')
 if n=='forge_bellows':
  for c in m.c:
   for key in ['from','to','origin']:c[key]=[v*2/3 for v in c[key]]
 return write(m)
def geometry(models):
 lines=['package ru.zoldrax;','final class TernorDecorGeometry {']
 for field,solid in [('COLLISION',True),('OUTLINE',False)]:
  lines.append('static final double[][][] '+field+'={')
  for m in models:lines.append('new double[][]{'+','.join('{'+','.join(str(round(v,5)) for v in rotated_bounds(c))+'}' for c in (m.collision if solid else m.c))+'},')
  lines.append('};')
 lines.append('}');Path('src/main/java/ru/zoldrax/TernorDecorGeometry.java').write_text('\n'.join(lines))
if __name__=='__main__':geometry([furniture(n) for n in NAMES])
