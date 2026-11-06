"""Deterministic voxel reconstructions of the Monkey King furniture and food references."""
from pathlib import Path
import json,math,uuid,base64,io,random,zipfile,os
from PIL import Image,ImageDraw
from generate_zoldrax_decor import Model,rotated_bounds
ROOT=Path('src/main/resources/assets/zoldrax')
NAMES=['food_shelf','preparation_table','supply_basket','drying_rack','courtyard_gong','festival_bench','notice_board','festival_drum']
COLORS=[(153,101,54),(191,141,83),(116,146,43),(210,173,99),(175,31,38),(231,174,34),(255,211,63),(168,27,35),(121,121,114),(227,199,149),(91,58,31),(184,119,30),(66,126,38),(225,210,168),(154,159,159),(235,220,181)]
MATS=['wood','cutwood','bamboo','rope','red','gold','lightgold','banner','stone','cream','darkwood','bronze','green','whitebanner','metal','paper']
def texture(name,colors=COLORS):
 rng=random.Random(name);im=Image.new('RGBA',(64,64))
 for i,col in enumerate(colors):
  x0=i%4*16;y0=i//4*16
  for y in range(16):
   for x in range(16):
    n=rng.choice([-7,-3,0,0,3,7])+(7 if x==0 or y==0 else -8 if x==15 or y==15 else 0)
    if i in [0,1,2,10] and y%5==0:n-=12
    im.putpixel((x0+x,y0+y),tuple(max(0,min(255,c+n)) for c in col)+(255,))
 if name.startswith('monkey_king_'):
  jar=Path(os.environ.get('MINECRAFT_CLIENT_JAR','/workspace/work/gradle-home/caches/fabric-loom/1.20.1/minecraft-client.jar'))
  tiles=['stripped_jungle_log','jungle_planks','bamboo_block','oak_planks','red_wool','gold_block','yellow_terracotta','red_wool','stone','sandstone_top','spruce_planks','orange_terracotta','moss_block','white_wool','iron_block','birch_planks']
  from PIL import ImageEnhance
  with zipfile.ZipFile(jar) as source:
   for i,tile in enumerate(tiles):
    tile_image=Image.open(io.BytesIO(source.read('assets/minecraft/textures/block/'+tile+'.png'))).convert('RGBA').crop((0,0,16,16))
    if i in [5,6]:
     # Warm brass trim; close to vanilla terracotta, without neon yellow.
     gold=ImageEnhance.Color(tile_image.convert('RGB')).enhance(.55)
     tile_image=Image.blend(gold,Image.new('RGB',(16,16),(151,119,52)),.35).convert('RGBA')
    elif i==2:tile_image=ImageEnhance.Color(tile_image).enhance(.7)
    im.paste(tile_image,(i%4*16,i//4*16))
 d=ImageDraw.Draw(im)
 for tile in [7,13]:
  x=tile%4*16;y=tile//4*16
  d.polygon([(x+3,y+8),(x+5,y+11),(x+8,y+12),(x+11,y+10),(x+13,y+6),(x+13,y+3),(x+11,y+7),(x+8,y+9),(x+5,y+9)],fill=(195,163,65));d.point((x+12,y+3),fill=(139,91,27))
 return im

def write(m,folder='monkey_king_decor',colors=COLORS):
 im=texture(m.name,colors);size=32 if folder=='food_models' else 64
 if size==32:im=im.resize((32,32),Image.Resampling.NEAREST)
 buf=io.BytesIO();im.save(buf,format='PNG');data=buf.getvalue();elements=[]
 for c in m.c:
  i=MATS.index(c['material']);u=i%4*16+.5;v=i//4*16+.5
  faces={f:{'uv':[a*(size/64) for a in [u,v,u+15,v+15]],'texture':0} for f in ['north','south','east','west','up','down']}
  e={k:c[k] for k in ['name','from','to','origin','rotation']};e.update(type='cube',uuid=str(uuid.uuid5(uuid.NAMESPACE_URL,m.name+c['name'])),faces=faces,box_uv=False,zoldrax_group=c['group'],zoldrax_emissive=False);elements.append(e)
 groups=[{'name':g,'uuid':str(uuid.uuid5(uuid.NAMESPACE_URL,m.name+g)),'origin':[0,0,0],'rotation':[0,0,0],'children':[e['uuid'] for e in elements if e['zoldrax_group']==g]} for g in dict.fromkeys(c['group'] for c in m.c)]
 model={'meta':{'format_version':'4.10','model_format':'free','box_uv':False},'name':m.name,'resolution':{'width':size,'height':size},'elements':elements,'outliner':groups,'textures':[{'id':'0','name':m.name+'.png','width':size,'height':size,'uv_width':size,'uv_height':size,'source':'data:image/png;base64,'+base64.b64encode(data).decode()}]}
 text=json.dumps(model,indent=2);p=Path('design/blockbench')/folder;p.mkdir(parents=True,exist_ok=True);(p/(m.name+'.bbmodel')).write_text(text)
 p=ROOT/'models/blockbench'/('food' if folder=='food_models' else 'decor');p.mkdir(parents=True,exist_ok=True);(p/(m.name+'.bbmodel')).write_text(text)
 p=ROOT/'textures/block/zoldrax';p.mkdir(parents=True,exist_ok=True);(p/(m.name+'.png')).write_bytes(data)
 if folder!='food_models':
  for suffix in ['','_part']:(ROOT/'blockstates'/f'{m.name+suffix}.json').write_text(json.dumps({'variants':{'':{'model':'zoldrax:block/'+m.name}}}))
  (ROOT/'models/block'/f'{m.name}.json').write_text(json.dumps({'parent':'builtin/entity','textures':{'particle':'zoldrax:block/zoldrax/'+m.name}}))
  (ROOT/'models/item'/f'{m.name}.json').write_text(json.dumps({'parent':'builtin/entity','display':{'gui':{'rotation':[20,45,0]},'ground':{'scale':[.6,.6,.6]},'firstperson_righthand':{'rotation':[0,45,0],'scale':[.8,.8,.8]}}}))
 print(m.name,len(m.c))
 return m

def box(m,n,x,y,z,w,h,d,mat='wood',group='body',solid=False):return m.box(n,x,y,z,w,h,d,mat,group,collision=solid)
def banner(m,n,x,y,z,w=9,h=12,mat='banner'):
 box(m,n,x,y,z,w,h,.5,mat);box(m,n+'_tail',x+2,y-2,z,w-4,2,.5,'red' if mat=='banner' else 'cream')
def posts(m,width=48,height=26):
 for x in [2,width-6]:
  box(m,'leg'+str(x),x,0,3,4,height,10,'wood',solid=True)
  for y in [1,height-3]:box(m,f'cap{x}_{y}',x-1,y,2,6,2,12,'gold')
def banana(m,cx,cy,z,scale=1,group='body'):
 for i,(x,y) in enumerate([(-5,1),(-4,-1),(-2,-2),(0,-2),(2,-1),(3,1),(4,3)]):box(m,'banana'+str(i),cx+x*scale,cy+y*scale,z,2*scale,2*scale,.65,'bronze' if group=='gong' else 'gold',group)
def furniture(name):
 m=Model('monkey_king_'+name)
 if name=='food_shelf':
  posts(m,height=28);box(m,'display_surface',0,10,1,48,2,14,'cutwood',solid=True);box(m,'lower_back',5,3,2,38,6,2,'wood',solid=True)
  box(m,'canopy',0,28,0,48,2,16,'red',solid=True)
  for x in range(0,48,8):box(m,'roof_edge'+str(x),x-.0625,27.5,-.0625,3.125,3,16.125,'gold')
  box(m,'crown',21,30,5,6,2,5,'gold');banner(m,'shelf_banner',19,17,15.1,10,12)
 elif name=='preparation_table':
  posts(m,height=15);box(m,'tabletop',0,15,0,48,2,16,'cutwood',solid=True);box(m,'cutting_board',13,17,4,16,.7,9,'wood',solid=True);banner(m,'table_banner',19,5,16.05,10,10)
  for x in [0,44]:box(m,'table_corner'+str(x),x-.0625,14.9375,-.0625,4.125,3.125,16.125,'gold')
  box(m,'utensil_holder',38,17,5,6,4,6,'wood');box(m,'knife_handle',39,21,7,1,6,1,'darkwood');box(m,'knife_blade',37,25,7,3,6,.5,'metal');box(m,'spoon_handle',42,21,8,1,8,1,'gold');box(m,'spoon_bowl',41,28,7.5,3,3,2,'lightgold')
 elif name=='supply_basket':
  box(m,'basket_bottom',1,0,1,14,2,14,'wood',solid=True)
  for y in [2,5,8,11]:
   for z in [1,13]:box(m,f'weave_horizontal{y}_{z}',1,y,z,14,2,2,'cutwood',solid=True)
   for x in [1,13]:box(m,f'weave_side{y}_{x}',x+(-.0625 if x==1 else .0625),y,2,2,2,12,'wood',solid=True)
  for x in [2,6,10,13]:
   for z in [1,13.2]:box(m,f'weave_vertical{x}_{z}',x,1,z-.0625,1,12,1.925,'bamboo')
  for x,z,w,d in [(0,0,16,2),(0,14,16,2),(0,2,2,12),(14,2,2,12)]:box(m,f'rim{x}_{z}',x,13,z,w,2,d,'gold',solid=True)
  banner(m,'basket_cloth',5,4,15.1,6,10,'whitebanner')
 elif name=='drying_rack':
  for x in [2,42]:
   box(m,'stone_foot'+str(x),x-1,0,3,6,3,10,'stone',solid=True);box(m,'bamboo_pole'+str(x),x,3,6,4,27,4,'bamboo',solid=True)
   for y in [10,17,27]:box(m,f'wrap{x}_{y}',x-.4,y,5.6,4.8,2,4.8,'red' if y==17 else 'gold')
  box(m,'crossbar',0,29,5,48,3,6,'bamboo',solid=True)
  for i,x in enumerate([7,15,23,31,39]):
   box(m,'hook_rope'+str(i),x,25,6,1,4,1,'rope');box(m,'hook_vertical'+str(i),x,22,9,1,5,1,'gold');box(m,'hook_tip'+str(i),x,22,8,1,1,1,'gold')
 elif name=='courtyard_gong':
  posts(m,height=45);box(m,'top_beam',0,42,2,48,4,12,'wood',solid=True);box(m,'base_beam',1,0,3,46,3,10,'wood',solid=True)
  for x in [1,42]:box(m,'gold_cap'+str(x),x-.0625,44,.9375,5.125,3,14.125,'gold');banner(m,'side_flag'+str(x),x-1,15,14,6,18)
  for x in [17,29]:box(m,'hanging_rope'+str(x),x,33,7,1,10,1,'rope')
  for y in range(-11,12):
   w=math.floor(math.sqrt(121-y*y));box(m,'gong_ring'+str(y),24-w,22+y,7,2*w+1,1,2.5,'gold','gong',solid=True)
   if abs(y)<9:
    q=math.floor(math.sqrt(81-y*y));box(m,'gong_face'+str(y),24-q,22+y,9.6,2*q+1,1,.8,'lightgold','gong')
  banana(m,24,23,10.5,1,'gong');box(m,'mallet_shaft',43,9,14,1,12,1,'wood');box(m,'mallet_head',41,8,13,5,4,3,'cream')
 elif name=='festival_bench':
  posts(m,height=11);box(m,'seat',0,10,2,48,3,13,'red',solid=True);box(m,'seat_front_gold',0,10,15,48,2,1,'gold');box(m,'back',4,13,1,40,12,3,'wood',solid=True);banner(m,'bench_banner',17,14,4.2,14,11)
  for x in [0,43]:box(m,'arm'+str(x),x,15,3,5,3,13,'cutwood',solid=True);box(m,'arm_gold'+str(x),x-.0625,14.9375,13.9375,5.125,3.125,2.125,'gold')
 elif name=='notice_board':
  posts(m,height=43);box(m,'document_board',5,9,4,38,31,2,'wood',solid=True)
  for y in range(10,40,5):box(m,'board_plank'+str(y),5,y,6.01,38,.4,.2,'darkwood')
  box(m,'roof_beam',0,42,2,48,3,11,'cutwood',solid=True)
  for x in [0,42]:box(m,'roof_end'+str(x),x-.0625,40,.9375,6.125,5,13.125,'gold')
  box(m,'crest',19,45,4,10,2,6,'gold');banner(m,'header_banner',19,34,8,10,12);banner(m,'lower_pennant',21,4,8,6,5)
  for i,(cx,cy) in enumerate([(14.4,28),(33.6,28),(24,13.6)]):
   box(m,'paper_sheet'+str(i),cx-4,cy-5,6.85,8,10,.18,'paper','paper'+str(i));box(m,'paper_pin'+str(i),cx-.4,cy+4,7.05,.8,.8,.3,'red','paper'+str(i))
 elif name=='festival_drum':
  for x in [2,26]:
   box(m,'drum_leg'+str(x),x,0,4,4,8,24,'wood',solid=True);box(m,'leg_gold'+str(x),x-1,0,3,6,2,26,'gold')
  box(m,'cradle',3,4,4,26,3,24,'wood',solid=True)
  for y in range(-12,13):
   w=math.floor(math.sqrt(144-y*y));box(m,'red_shell'+str(y),16-w,18+y,7,2*w+1,1,17,'red',solid=True)
   box(m,'gold_rim'+str(y),16-w,18+y,24.1,2*w+1,1,1.2,'gold','drum')
   if abs(y)<10:
    q=math.floor(math.sqrt(100-y*y));box(m,'membrane'+str(y),16-q,18+y,25.4,2*q+1,1,.6,'cream','drum')
  banana(m,16,19,26.1,1,'drum');banner(m,'drum_flag',12,1,28,8,6)
  for x in [6,25]:box(m,'tension_pin'+str(x),x,14,24.8,2,4,2,'gold')
 return write(m)

def geometry(models):
 lines=['package ru.zoldrax;','/** Generated from the editable Monkey King furniture models. */','final class MonkeyDecorGeometry {']
 for name,solid in [('COLLISION',True),('OUTLINE',False)]:
  lines.append('static final double[][][] '+name+'={')
  for m in models:
   boxes=[rotated_bounds(c) for c in (m.collision if solid else m.c)];lines.append('new double[][]{'+','.join('{'+','.join(str(round(v,5)) for v in b)+'}' for b in boxes)+'},')
  lines.append('};')
 lines.append('}');Path('src/main/java/ru/zoldrax/MonkeyDecorGeometry.java').write_text('\n'.join(lines))
if __name__=='__main__':geometry([furniture(n) for n in NAMES])
