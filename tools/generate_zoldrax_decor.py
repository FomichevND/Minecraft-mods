"""Editable voxel reconstruction of the eight supplied Zoldrax furniture concepts.
256px atlases, actual skull sockets/teeth, tapered horns and open chain links.
The rear/hidden surfaces necessarily reconstruct what the two images cannot show.
"""
from pathlib import Path
import json, math, hashlib, uuid, base64, io, random
from PIL import Image,ImageDraw
OUT=Path('src/main/resources/assets/zoldrax'); DESIGN=Path('design/blockbench')
NAMES=['zoldrax_throne','zoldrax_brazier','zoldrax_skull_lamp','zoldrax_chain_lantern','zoldrax_weapon_rack','zoldrax_chest','zoldrax_lectern','zoldrax_offering_pedestal']
MATS=['stone','edge','metal','crimson','bone','chain','cloth','banner','cushion','rune','fire','crystal','paper','leather','dark','glass']
COLORS=[(57,52,67),(86,77,96),(62,54,67),(91,36,52),(211,184,147),(124,126,137),(132,17,38),(143,21,39),(157,27,48),(255,48,15),(255,76,14),(223,23,54),(218,195,154),(99,20,31),(28,22,31),(138,20,35)]

def atlas(name):
 rng=random.Random(name);im=Image.new('RGBA',(256,256));p=im.load()
 for k,(mat,col) in enumerate(zip(MATS,COLORS)):
  ox=k%4*64;oy=k//4*64
  for y in range(64):
   for x in range(64):
    # Coarse pixels, light bevels and directional material variation; no black silhouettes.
    grain=rng.choice([-4,-2,0,0,2,4]);edge=min(x,y,63-x,63-y)
    light=(10 if edge<2 else -7 if edge<4 else 0)+grain
    if mat in ['stone','edge']:light+=(-8 if (y//4)%8==0 or (x//4+(y//32%2)*8)%16==0 else 0)
    if mat=='bone':light+=int(math.sin(x*.19)*7)+(5 if y<14 else 0)
    if mat in ['metal','chain','crimson']:light+=int(math.cos((x+y)*.09)*8)
    if mat in ['cloth','banner','cushion']:light+=int(math.sin(x*.28)*5)
    if mat=='fire':light+=int((63-y)/63*40)
    if mat=='crystal':light+=int((x<31)*18-(y>43)*16)
    if mat=='paper':light+=(-26 if 7<y%12<10 and 8<x<55 else 0)
    if mat=='rune':light=grain
    rgb=tuple(max(0,min(255,c+light)) for c in col);p[ox+x,oy+y]=(*rgb,255)
  if mat=='banner':
   d=ImageDraw.Draw(im);d.rectangle((ox+4,oy+3,ox+59,oy+60),outline=(197,48,49),width=2)
   glyph=[(32,52),(32,22),(16,22),(10,10),(10,5)]
   d.line([(ox+x,oy+y) for x,y in glyph],fill=(255,76,20),width=4)
   d.line([(ox+32,oy+22),(ox+48,oy+22),(ox+54,oy+10),(ox+54,oy+5)],fill=(255,76,20),width=4)
   d.line([(ox+22,oy+37),(ox+42,oy+37)],fill=(255,76,20),width=3)
 return im

class Model:
 def __init__(self,name):self.name=name;self.c=[];self.collision=[]
 def box(self,n,x,y,z,w,h,d,mat='stone',group='body',emit=False,rot=None,origin=None,collision=False):
  c={'name':n,'from':[x,y,z],'to':[x+w,y+h,z+d],'material':mat,'group':group,'emissive':emit,'rotation':rot or [0,0,0],'origin':origin or [x+w/2,y+h/2,z+d/2]}
  self.c.append(c)
  if collision:self.collision.append(c)
  return c
 def horn(self,n,points,width=3,group='body'):
  for i,(a,b) in enumerate(zip(points,points[1:])):
   dx,dy,dz=[b[j]-a[j] for j in range(3)];length=math.sqrt(dx*dx+dy*dy+dz*dz);t=width*(1-i/(len(points)-1))+.35
   # All reference horns use a single plane, avoiding ambiguous compound rotations.
   if abs(dz)<.001:rot=[0,0,math.degrees(math.atan2(-dx,dy))]
   else:rot=[math.degrees(math.atan2(dz,dy)),0,0]
   self.box(n+f'_segment_{i}',a[0]-t/2,a[1],a[2]-t/2,t,length,t,'bone',group,rot=rot,origin=a)
 def link(self,n,x,y,z,w=2.6,h=3.8,t=.52,plane='xy',group='body'):
  bars=[(-w/2,-h/2,-t/2,w,t,t),(-w/2,h/2-t,-t/2,w,t,t),(-w/2,-h/2+t,-t/2,t,h-2*t,t),(w/2-t,-h/2+t,-t/2,t,h-2*t,t)]
  for i,(X,Y,Z,W,H,D) in enumerate(bars):
   if plane=='yz':X,Z=Z,X;W,D=D,W
   self.box(n+f'_bar_{i}',x+X,y+Y,z+Z,W,H,D,'chain',group)
 def chain(self,n,x,z,y0,y1,group='body'):
  count=max(2,math.ceil(abs(y1-y0)/2.7)+1)
  for i in range(count):self.link(n+f'_link_{i}',x,y0+(y1-y0)*i/max(1,count-1),z,plane='xy' if i%2==0 else 'yz',group=group)
 def skull(self,n,cx,y,z,s=1,group='body'):
  def b(label,x,Y,Z,w,h,d,mat='bone',emit=False):self.box(n+'_'+label,cx+x*s,y+Y*s,z+Z*s,w*s,h*s,d*s,mat,group,emit)
  b('cranial_back',-3,0,-4.2,6,4.6,1.4);b('forehead',-3,3.3,-3.6,6,1.6,3.8);b('crown',-2.3,4.8,-3.2,4.6,.65,3)
  for side in [-1,1]:
   x=-3.1 if side<0 else 1.9;b('temple_'+str(side),x,.3,-3.2,1.2,3,3.4)
   x=-2.6 if side<0 else .65;b('eye_socket_'+str(side),x,1.5,-.3,2,1.7,.4,'dark');b('eye_ember_'+str(side),x+.36,1.9,-.08,1.25,.8,.22,'rune',True)
   b('cheek_'+str(side),-3 if side<0 else 1.7,-.5,-2.4,1.3,1.5,2.5)
  b('nose_bridge',-.42,.3,-.45,.84,2.3,.8);b('nose_socket',-.45,-.2,-.12,.9,.8,.16,'dark')
  b('mouth_shadow',-2,-1.5,-.6,4,1.8,.8,'dark');b('jaw',-2.5,-2.1,-2,5,.8,2.2)
  for i in range(5):b('tooth_'+str(i),-2+i*.85,-1.7,-.02,.62,1.2,.65)
 def rune(self,n,cx,y,z,w=6,h=10,group='body'):
  self.box(n+'_stem',cx-.35,y,z,.7,h*.74,.14,'rune',group,True)
  self.box(n+'_cross',cx-w*.28,y+h*.32,z,.56*w,.6,.14,'rune',group,True)
  self.box(n+'_fork',cx-w*.43,y+h*.71,z,w*.86,.65,.14,'rune',group,True)
  for s in [-1,1]:
   self.box(n+'_horn_'+str(s),cx+s*w*.41-.3,y+h*.72,z,.65,h*.28,.14,'rune',group,True,rot=[0,0,-s*18])
 def banner(self,n,cx,y,z,w,h,group='body'):
  self.box(n+'_cloth',cx-w/2,y,z,w,h,.35,'banner',group)
  # Unequal angular tails are actual pieces, not a rectangular texture decal.
  self.box(n+'_tail_left',cx-w/2,y-2,z,w*.3,2.2,.35,'cloth',group)
  self.box(n+'_tail_right',cx+w*.2,y-1.3,z,w*.3,1.6,.35,'cloth',group)
  self.box(n+'_mount',cx-w/2-.5,y+h,z-.15,w+1,.8,.8,'crimson',group)
 def plinth(self,n,w=16,d=16):
  self.box(n+'_foot',0,0,0,w,2,d,'stone',collision=True);self.box(n+'_bevel',1,2,1,w-2,1.5,d-2,'edge',collision=True)
  for x in range(2,w-1,4):self.box(n+'_front_stone_'+str(x),x,.15,d-.1,3.3,1.5,.25,'edge')
 def bowl(self,n,y,group='body'):
  self.box(n+'_floor',3,y-1,3,10,1.4,10,'crimson',group,collision=True)
  for X,Z,W,D in [(3,1,10,2),(3,13,10,2),(1,3,2,10),(13,3,2,10),(2,2,2,2),(12,2,2,2),(2,12,2,2),(12,12,2,2)]:self.box(n+f'_rim_{X}_{Z}',X,y,Z,W,2.4,D,'edge',group,collision=True)
 def write(self):
  image=atlas(self.name);stream=io.BytesIO();image.save(stream,format='PNG');data=stream.getvalue();elements=[]
  for idx,c in enumerate(self.c):
   a,b=c['from'],c['to'];w,h,d=[b[i]-a[i] for i in range(3)];mat=MATS.index(c['material']);u=mat%4*64+1;v=mat//4*64+1
   faces={}
   for face,W,H in [('north',w,h),('south',w,h),('east',d,h),('west',d,h),('up',w,d),('down',w,d)]:
    U=min(62,max(1,W*2));V=min(62,max(1,H*2))
    if c['material']=='banner' or c['material']=='paper' and face=='up':U=V=62
    faces[face]={'uv':[u,v,u+U,v+V],'texture':0}
   e={k:c[k] for k in ['name','from','to','rotation','origin']};e.update({'type':'cube','uuid':str(uuid.uuid5(uuid.NAMESPACE_URL,self.name+c['name'])),'faces':faces,'box_uv':False,'visibility':True,'export':True,'zoldrax_group':c['group'],'zoldrax_emissive':c['emissive']});elements.append(e)
  groups=[]
  for group in dict.fromkeys(c['group'] for c in self.c):
   groups.append({'name':group,'uuid':str(uuid.uuid5(uuid.NAMESPACE_URL,self.name+group)),'origin':[8,14,3] if group=='lid' else [0,0,0],'rotation':[0,0,0],'children':[e['uuid'] for e in elements if e['zoldrax_group']==group],'export':True})
  model={'meta':{'format_version':'4.10','model_format':'free','box_uv':False},'name':self.name,'model_identifier':self.name,'resolution':{'width':256,'height':256},'elements':elements,'outliner':groups,'textures':[{'id':'0','name':self.name+'.png','width':256,'height':256,'uv_width':256,'uv_height':256,'source':'data:image/png;base64,'+base64.b64encode(data).decode()}], 'zoldrax_lid_pivot':[8,14,3]}
  target=OUT/'models/blockbench/decor'/f'{self.name}.bbmodel';target.parent.mkdir(parents=True,exist_ok=True);text=json.dumps(model,ensure_ascii=False,indent=2);target.write_text(text);DESIGN.mkdir(parents=True,exist_ok=True);(DESIGN/f'{self.name}.bbmodel').write_text(text)
  target=OUT/'textures/block/zoldrax'/f'{self.name}.png';target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(data)
  for suffix in ['', '_part']:
   p=OUT/'blockstates'/f'{self.name+suffix}.json';p.write_text(json.dumps({'variants':{'':{'model':'zoldrax:block/'+self.name}}},indent=2))
  (OUT/'models/block'/f'{self.name}.json').write_text(json.dumps({'parent':'builtin/entity','textures':{'particle':'zoldrax:block/zoldrax/'+self.name}},indent=2))
  (OUT/'models/item'/f'{self.name}.json').write_text(json.dumps({'parent':'builtin/entity','gui_light':'front','display':{'gui':{'rotation':[20,45,0],'scale':[1,1,1]},'ground':{'translation':[0,2,0],'scale':[.6,.6,.6]},'thirdperson_righthand':{'rotation':[75,45,0],'scale':[.7,.7,.7]},'firstperson_righthand':{'rotation':[0,225,0],'scale':[.8,.8,.8]}}},indent=2))
  print(self.name,len(self.c),'separate cuboids; 256x256 atlas; groups',len(groups))
  return self

def throne():
 m=Model(NAMES[0]);m.plinth('throne',32,16)
 m.box('rear_tower',2,3,1,28,31,4,'stone',collision=True);m.box('back_bevel',3,31,.9,26,4,4.2,'edge')
 m.box('back_cushion',6,13,5,20,18,1.4,'cushion');m.banner('great_back_banner',16,13,6.45,11,18)
 m.box('seat_stone',5,3.5,4,22,6.5,10,'stone',collision=True);m.box('seat_velvet',6,10,5,20,2,9,'cushion',collision=True)
 m.box('front_step',8,2.9,14,16,1.6,2,'edge',collision=True);m.box('front_red_runner',13,4.55,13,6,.18,3,'cloth');m.box('velvet_seat_fall',13,4.6,14.04,6,5.4,.18,'cloth');m.box('step_runner_fall',13,2.9,16.03,6,1.7,.16,'cloth')
 for s,x in [(-1,2),(1,26)]:
  m.box('arm_post_'+str(s),x,3,6,4,10,8,'stone',collision=True);m.box('arm_velvet_'+str(s),x,12.7,5,4,1.3,9,'cushion',collision=True);m.skull('arm_skull_'+str(s),x+2,11,15,.75)
  m.chain('side_chain_'+str(s),x+2,7,17,30)
  pts=[(x+2,29,3),(x+2+s*2,32,3),(x+2+s*1.2,35,3),(x+2-s*.4,38,3)];m.horn('outer_horn_'+str(s),pts,2.4)
 for s,x in [(-1,10),(1,22)]:m.horn('crown_horn_'+str(s),[(x,33,2),(x+s*2,35,2),(x+s*3,38,2),(x+s*1.5,40,2)],2.4)
 m.rune('foot_rune',16,.5,16.06,4,2.2);return m

def brazier():
 m=Model(NAMES[1]);m.plinth('brazier');m.box('octagonal_stem',4,3,4,8,10,8,'stone',collision=True);m.box('stem_belt',3.5,8,3.5,9,1.5,9,'crimson',collision=True);m.bowl('ritual_bowl',14)
 for s in [-1,1]:
  for z in [4,12]:
   x=4 if s<0 else 12;m.horn('bowl_horn_'+str(s)+'_'+str(z),[(x,15,z),(x+s*2,18,z),(x+s*2.5,21,z),(x+s*1,23,z)],2.5)
 m.skull('front_skull',8,10,15.3,.95)
 for x in [3,13]:m.banner('bowl_banner_'+str(x),x,4,14.9,3,9)
 m.rune('base_rune',8,.6,16.05,3.5,2)
 for i,(x,z,h) in enumerate([(7,7,8),(10,8,5),(5,9,4),(9,5,6)]):
  for j in range(h//2):m.box(f'flame_tongue_{i}_{j}',x-j*.2,16+j*2,z-j*.12,max(.7,3-j*.6),2.1,max(.7,3-j*.6),'fire','flame',True)
 return m

def skull_lamp():
 m=Model(NAMES[2]);m.plinth('lamp');m.box('runed_lower_column',3,3,3,10,6.5,10,'stone',collision=True);m.rune('column_rune',8,3.5,13.1,5,5)
 for y in [9,21]:m.box('stem_collar_'+str(y),3, y,3,10,1.6,10,'edge',collision=True)
 m.box('dark_rod',6,10,6,4,12,.35,'glass',collision=True);m.box('crimson_rod',6.4,10,6.4,3.2,12,3.2,'rune','body',True)
 for x in [4,11]:m.box('iron_rod_'+str(x),x,10,5,1,12,6,'metal',collision=True)
 m.skull('great_skull',8,23.5,14.4,1.35)
 for s,x in [(-1,3),(1,13)]:m.horn('head_horn_'+str(s),[(x,22,8),(x+s*1.5,24,8),(x+s*1.8,27,8),(x,29,8)],2)
 return m

def lantern():
 m=Model(NAMES[3]);m.box('ceiling_rosette',4,14.7,4,8,1.3,8,'edge',collision=True);m.chain('suspension',8,8,13,1,'chain');m.chain('extension',8,8,-2,-14,'extension')
 m.box('cage_upper_cap',1.5,-1,1.5,13,2,13,'edge','cage',collision=True);m.box('cage_lower_cap',1.5,-15,1.5,13,2,13,'edge','cage',collision=True)
 for x in [2.5,12.3]:
  for z in [2.5,12.3]:m.box(f'cage_post_{x}_{z}',x,-13,z,1.2,12,1.2,'metal','cage',collision=True)
 for y in [-10,-5]:
  for z in [2.5,12.5]:m.box(f'cage_band_{y}_{z}',2.5,y,z,11,.8,.8,'crimson','cage')
 m.box('unlit_core',5,-12,5,6,10,.35,'glass','cage');m.box('infernal_core',5.4,-11.5,5.4,5.2,9,5.2,'fire','cage',True)
 for s,x in [(-1,4),(1,12)]:m.horn('cage_horn_'+str(s),[(x,0,8),(x+s*2,2,8),(x+s*2.5,5,8),(x+s,6.5,8)],2,'cage')
 m.horn('lower_bone_spike',[(8,-14,8),(8,-17,8),(8,-20,8),(8,-22,8)],3,'cage')
 m.box('front_emblem',6,-1.2,14,4,2,.5,'crimson','cage');m.rune('front_rune',8,-.8,14.55,2,1.5,'cage');return m

def rack():
 m=Model(NAMES[4]);m.plinth('rack',32,16);m.box('foot_lip',1,3,1,30,1.4,14,'edge',collision=True)
 for x in [1,27]:
  m.box('upright_'+str(x),x,4,4,4,24,6,'stone',collision=True)
  for y in [7,18,26]:m.box(f'post_ring_{x}_{y}',x-.3,y,3.7,4.6,1.2,6.6,'crimson')
 m.box('iron_crossbeam',1,26,4,30,3,6,'metal',collision=True);m.box('lower_crossbeam',3,6,5,26,1.4,5,'crimson',collision=True)
 for s,x in [(-1,3),(1,29)]:m.horn('post_horn_'+str(s),[(x,27,7),(x+s*1.6,30,7),(x+s,33,7),(x-s,35,7)],2.4)
 m.skull('crowning_skull',16,28.5,10.8,1)
 for x in [7,25]:m.banner('rack_banner_'+str(x),x,9,10.4,5,15)
 for x in [2,12,20,30]:m.chain('hanging_chain_'+str(x),x,11.5,9,25)
 for x in [8,16,24]:
  m.box('weapon_hook_'+str(x),x-.6,20,11,1.2,1.2,3,'chain');m.box('weapon_lower_catch_'+str(x),x-1,7,11,2,1.3,2.2,'crimson')
 m.rune('rack_foot_rune',16,.6,16.05,4,2);return m

def chest():
 m=Model(NAMES[5]);m.plinth('chest');m.box('metal_box',1,3,1,14,10.5,14,'crimson',collision=True)
 for x in [1,13]:m.box('stone_corner_'+str(x),x-.15,3,.85,2.3,11,14.3,'stone',collision=True)
 m.box('rear_hinge_beam',1,12,1,14,2,2,'chain');m.box('red_lid',1,14,3,14,3,12,'cushion','lid')
 for x in [1,13]:m.box('lid_iron_strap_'+str(x),x,14,3,2,3.2,12,'metal','lid')
 m.skull('chest_skull',8,7.4,15.3,1.05)
 for s,x in [(-1,4),(1,12)]:m.horn('chest_horn_'+str(s),[(x,14,4),(x+s*2,17,4),(x+s*2.2,20,4),(x-s*.2,22,4)],2.7)
 for i,(x,y) in enumerate([(1.8,11),(3,8.5),(5,6),(8,5),(11,6),(13,8.5),(14.2,11)]):m.link('front_draped_chain_'+str(i),x,y,15.5,2.7,3.2,.6)
 m.banner('left_banner',2.9,4,15.1,2.8,8);m.banner('right_banner',13.1,4,15.1,2.8,8)
 for side in [-1,1]:
  start=len(m.c);m.banner('side_banner_'+str(side),8,4,15.15,6,8)
  for c in m.c[start:]:c['rotation']=[0,side*90,0];c['origin']=[8,0,8]
  for j,(z,y) in enumerate([(3,10),(5,8),(8,6.5),(11,8),(13,10)]):m.link('side_chain_'+str(side)+'_'+str(j),.5 if side<0 else 15.5,y,z,2.7,3.2,.5,plane='yz')
 for x in [6.8,8,9.2]:m.box('blood_slit_'+str(x),x,3.4,15.07,.4,4.5,.16,'rune','body',True)
 m.rune('chest_base_rune',8,.5,16.05,3,1.6)
 # Raised emissive cracks across the actual moving lid.
 paths=[((8,5),(8,13)),((8,8),(4,5)),((8,8),(12,5)),((4,5),(3,4)),((12,5),(13,4)),((8,11),(11,13))]
 for i,(a,b) in enumerate(paths):
  dx,dz=b[0]-a[0],b[1]-a[1];length=math.hypot(dx,dz);m.box('lid_magic_crack_'+str(i),a[0],17.05,a[1]-.22,length,.1,.44,'rune','lid',True,rot=[0,math.degrees(math.atan2(-dz,dx)),0],origin=[a[0],17.05,a[1]])
 return m

def lectern():
 m=Model(NAMES[6]);m.plinth('lectern');m.box('ritual_column',4,3,4,8,13,8,'stone',collision=True);m.box('upper_collar',2,15,2,12,2,12,'edge',collision=True)
 m.banner('lectern_banner',8,5,12.8,7,10);m.rune('lectern_base_rune',8,.4,16.02,4,2.2)
 for x in [2,14]:m.chain('lectern_chain_'+str(x),x,12,5,17)
 m.box('inclined_reading_desk',1,19,2,14,2,12,'edge',rot=[25,0,0],origin=[8,20,8],collision=True)
 for s,x in [(-1,3),(1,13)]:m.horn('desk_horn_'+str(s),[(x,18,7),(x+s*1.5,20,7),(x+s*1.8,23,7),(x,24.5,7)],2)
 for n,x,w in [('left_cover',1.3,6.5),('right_cover',8.2,6.5)]:m.box(n,x,21.1,3, w,.6,10,'leather','book',rot=[25,0,0],origin=[8,20,8])
 for n,x in [('left_pages',1.8),('right_pages',8.3)]:m.box(n,x,21.7,3.5,5.8,.9,9,'paper','book',rot=[25,0,0],origin=[8,20,8])
 m.box('book_spine',7.65,21,3,.7,1.3,10,'crimson','book',rot=[25,0,0],origin=[8,20,8]);return m

def pedestal():
 m=Model(NAMES[7]);m.plinth('pedestal');m.box('runic_stem',4,3,4,8,11,8,'stone',collision=True);m.box('stem_core',6,4,12,4,8,.3,'rune','body',True);m.bowl('offering_tray',14)
 for s,x in [(-1,4),(1,12)]:m.horn('altar_horn_'+str(s),[(x,15,7),(x+s*2,18,7),(x+s*2,21,7),(x,23,7)],2.6)
 m.skull('altar_skull',8,10,15.3,.9)
 for x in [3,13]:m.banner('altar_banner_'+str(x),x,5,15,3,9)
 m.rune('pedestal_base_rune',8,.5,16.05,4,2)
 for i,(y,w,h) in enumerate([(17,3,1.8),(18.8,5,3),(21.8,3.2,2),(23.8,1.2,1.6)]):m.box('infernal_crystal_'+str(i),8-w/2,y,8-w/2,w,h,w,'crystal','crystal',True,rot=[0,45,0])
 return m

# Collision/selection boxes are generated from the same cuboids; no client classes on server.
def rotated_bounds(c):
 corners=[];r=[math.radians(v) for v in c['rotation']];o=c['origin']
 for x in [c['from'][0],c['to'][0]]:
  for y in [c['from'][1],c['to'][1]]:
   for z in [c['from'][2],c['to'][2]]:
    x,y,z=x-o[0],y-o[1],z-o[2]
    y,z=y*math.cos(r[0])-z*math.sin(r[0]),y*math.sin(r[0])+z*math.cos(r[0])
    x,z=x*math.cos(r[1])+z*math.sin(r[1]),-x*math.sin(r[1])+z*math.cos(r[1])
    x,y=x*math.cos(r[2])-y*math.sin(r[2]),x*math.sin(r[2])+y*math.cos(r[2]);corners.append((x+o[0],y+o[1],z+o[2]))
 return [min(p[i] for p in corners) for i in range(3)]+[max(p[i] for p in corners) for i in range(3)]

def java_shapes(models):
 lines=['package ru.zoldrax;','/** Generated by tools/generate_zoldrax_decor.py from the editable models. */','final class DecorGeometry {',' static final double[][][] COLLISION={']
 for m in models:
  entries=[]
  for c in m.collision:
   b=rotated_bounds(c);entries.append('{'+','.join(f'{n:.5f}' for n in b)+'}')
  lines.append('  new double[][]{'+','.join(entries)+'},')
 lines+=[' };',' static final double[][][] OUTLINE={']
 for m in models:
  boxes=[rotated_bounds(c) for c in m.c if c['group'] not in ['flame','book','crystal','extension']]
  lines.append('  new double[][]{'+','.join('{'+','.join(f'{n:.5f}' for n in b)+'}' for b in boxes)+'},')
 lines+=[' };']
 for field,predicate in [('LANTERN_STATIC',lambda c:c['group'] not in ['cage','extension']),('LANTERN_CAGE',lambda c:c['group']=='cage'),('LANTERN_EXTENSION',lambda c:c['group']=='extension'),('LANTERN_STATIC_COLLISION',lambda c:c in models[3].collision and c['group']!='cage'),('LANTERN_CAGE_COLLISION',lambda c:c in models[3].collision and c['group']=='cage')]:
  boxes=[rotated_bounds(c) for c in models[3].c if predicate(c)]
  lines.append(' static final double[][] '+field+'={'+','.join('{'+','.join(f'{n:.5f}' for n in b)+'}' for b in boxes)+'};')
 lines+=['}'];Path('src/main/java/ru/zoldrax/DecorGeometry.java').write_text('\n'.join(lines)+'\n')
 # Full cage geometry bounds are separate so lantern length can be changed on server.
 cage=models[3];p=Path('src/main/resources/data/zoldrax/decor_shapes');p.mkdir(parents=True,exist_ok=True)
 (p/'lantern.json').write_text(json.dumps({'cage':[rotated_bounds(c) for c in cage.c if c['group']=='cage'],'extension':[rotated_bounds(c) for c in cage.c if c['group']=='extension']}))
if __name__=='__main__':
 models=[f().write() for f in [throne,brazier,skull_lamp,lantern,rack,chest,lectern,pedestal]];java_shapes(models)
