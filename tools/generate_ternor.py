"""Editable Ternor monument: 4 x 4 x 8 blocks, three construction stages."""
from pathlib import Path
from PIL import Image,ImageDraw
import hashlib,numpy as np
from model_export import write_model
from texture_materials import paint as base_paint,noise

def paint(cube,face,w,h,cubes):
 kind=cube['material']
 mapping={'stone':'stone','iron':'metal','beard':'stone','moss':'stone','obsidian':'stone','gold':'metal','amber':'ember','banner':'cloth'}
 if kind not in mapping:return base_paint(cube,face,w,h,cubes)
 cube['material']=mapping[kind];image=np.asarray(base_paint(cube,face,w,h,cubes)).astype(float);cube['material']=kind
 baseline={'stone':(39,35,43),'metal':(36,36,37),'ember':(170,32,8),'cloth':(97,86,69)}[mapping[kind]]
 target={'stone':(85,88,81),'iron':(133,137,129),'beard':(143,147,139),'moss':(55,78,40),'obsidian':(25,17,37),'gold':(159,116,48),'amber':(240,151,36),'banner':(47,29,71)}[kind]
 image=image/np.array(baseline)*np.array(target)
 yy,xx=np.indices((h,w));u=(xx+.5)/w;v=(yy+.5)/h
 if kind=='beard':image*=1-((xx%5)==0)[...,None]*.22
 if kind=='iron':
  edge=np.minimum.reduce([xx,w-1-xx,yy,h-1-yy]);image+=((edge>1)&(edge<3))[...,None]*5
 if kind=='obsidian':
  rng=np.random.default_rng(int.from_bytes(hashlib.sha256((cube['name']+face).encode()).digest()[:8],'big'))
  cloud=noise(rng,w,h,8);vein=abs(cloud)<.017;image+=vein[...,None]*np.array([8,3,14])
 if kind=='banner':
  diamond=abs(u-.5)/.3+abs(v-.36)/.2
  rune=((diamond>.82)&(diamond<1.08))|((abs(u-.5)<.035)&(v>.12)&(v<.9))
  image[rune]=[185,139,67]
 return Image.fromarray(np.uint8(image.clip(0,255)))

def make(stage):
 cubes=[]
 def box(name,x,y,z,w,h,d,mat='stone'):cubes.append({'name':name,'from_':[x,y,z],'to':[x+w,y+h,z+d],'material':mat})
 box('foundation',-24,0,-24,64,4,64)
 for x in range(-24,40,8):
  for z in range(-24,40,8):box(f'base_stone_{x}_{z}',x,4,z,8,4,8)
 box('upper_plinth',-21,8,-21,58,4,58)
 box('front_step',-9,8,31,34,3,9)
 for side in [-1,1]:
  x=8+side*13-7
  box(f'foot_{side}',x-2,12,-10,18,8,29)
  box(f'leg_{side}',x,20,-10,14,45,18,'iron' if stage>0 else 'stone')
 box('pelvis',-10,60,-12,36,10,22,'iron' if stage>0 else 'stone')
 box('torso',-12,70,-14,40,34,25,'iron' if stage>0 else 'stone')
 box('neck',0,104,-6,16,5,15,'iron' if stage>0 else 'stone')
 box('head',-4,109,-10,24,17,21,'iron' if stage>0 else 'stone')
 box('head_cap',-5,126,-11,26,2,23,'iron' if stage>0 else 'stone')
 if stage==0:
  for side in [-1,1]:
   x=8+side*27
   box(f'rough_arm_{side}',x-5,75,-7,10,24,16)
   box(f'wood_frame_{side}',x-2,8,25,4,112,4,'wood')
   for y in [36,66,96,118]:box(f'wood_cross_tie_{side}_{y}',min(x,8),y,25,abs(x-8)+2,3,3,'wood')
  box('wood_workplatform',-18,65,13,52,3,18,'wood')
  box('sword_blank',4,12,20,8,58,5)
  box('dust_cover',-2,79,12,20,21,1,'cloth')
 else:
  for side in [-1,1]:
   x=8+side*24
   box(f'shoulder_{side}',x-8,94,-12,16,13,24,'iron')
   box(f'upper_arm_{side}',x-6,78,-8,12,18,18,'iron')
   box(f'forearm_{side}',x-6 if side<0 else 10,87,3,29,9,20,'iron')
   box(f'palm_{side}',0 if side<0 else 8,88,18,8,9,10,'iron')
   if stage==2:
    box(f'shoulder_trim_{side}',x-8,104,-12,16,2,24,'gold')
    box(f'knee_plate_{side}',8+side*13-6,47,8,12,14,3,'iron')
    for digit in range(3):box(f'finger_{side}_{digit}',(0 if side<0 else 8)+digit*2,88,27,2,6,1,'iron')
  box('sword_handle',6,68,20,4,24,4,'wood')
  for y in range(69,89,4):box(f'grip_ring_{y}',5,y,19,6,1,6,'iron')
  box('pommel',3,92,17,10,4,10,'gold')
  box('sword_guard',-4,64,17,24,4,10,'iron')
  box('blade',2,20,20,12,44,5,'obsidian')
  box('blade_edge_left',0,24,20,2,36,4,'obsidian')
  box('blade_edge_right',14,24,20,2,36,4,'obsidian')
  box('blade_tip',5,12,21,6,8,3,'obsidian')
  box('blade_tip_shoulder',3,18,20,10,6,5,'obsidian')
  box('nose',5,110,11,6,10,6,'iron')
  for side in [-1,1]:
   box(f'brow_{side}',8+side*6-5,119,11,10,3,3,'iron')
   box(f'glow_eye_{side}',8+side*6-3,117,12,6,2,2,'amber')
  # Separate carved strands converge to a long pointed beard, behind the hands.
  for i in range(7):
   x=-3+i*3;length=15+int((3-abs(i-3))*3)
   box(f'beard_strand_{i}',x,112-length,12,3,length,5,'beard')
  if stage==1:
   cubes[:]=[c for c in cubes if not(c['name'].startswith('beard_strand_') and int(c['name'].split('_')[-1]) in [0,1,5,6])]
   box('wood_temporary_brace',-18,50,27,52,3,3,'wood')
   box('work_cloth',19,69,24,12,15,1,'cloth')
  if stage==2:
   box('chest_emblem',2,78,11,12,7,2,'gold')
   box('glow_chest_rune',7,78,13,2,7,1,'amber')
   for j in range(6):
    x=6+(j%3)*2;y=26+j*5
    box(f'glow_obsidian_rune_{j}',x,y,25,1,3,1,'amber')
   for side in [-1,1]:
    x=8+side*25
    box(f'banner_pole_{side}',x,12,16,2,53,2,'iron')
    box(f'banner_top_{side}',x-5,63,16,12,2,2,'iron')
    box(f'banner_{side}',x-4,32,18,10,31,1,'banner')
    for y in range(22,98,9):
     box(f'vine_{side}_{y}',x-5,y,-11,2,8,2,'moss')
     box(f'leaf_{side}_{y}',x-6,y+2,-9,4,3,2,'moss')
   for i,(x,z) in enumerate([(-19,-19),(29,-19),(-19,29),(29,29)]):
    box(f'lantern_base_{i}',x,12,z,8,2,8,'iron')
    box(f'glow_lantern_{i}',x+1,14,z+1,6,8,6,'amber')
    for dx,dz in [(0,0),(6,0),(0,6),(6,6)]:box(f'lantern_bar_{i}_{dx}_{dz}',x+dx,14,z+dz,2,8,2,'iron')
    box(f'lantern_cap_{i}',x,22,z,8,2,8,'iron')
   for i,(x,z) in enumerate([(-22,-17),(24,-22),(-22,17),(25,32)]):box(f'base_moss_{i}',x,8,z,10,1,7,'moss')
 write_model(cubes,stage,'ternor_',paint)

if __name__=='__main__':
 for stage in range(3):make(stage)
 out=Path('src/main/resources/assets/zoldrax/textures/entity');out.mkdir(parents=True,exist_ok=True)
 for name,col in [('guardian_wreath',(63,108,40)),('guardian_flowers',(230,190,55))]:
  rng=np.random.default_rng(15);arr=np.zeros((64,64,4),dtype='u1');arr[:,:,:3]=np.array(col);arr[:,:,:3]=np.clip(arr[:,:,:3].astype(float)+rng.integers(-14,15,(64,64,1)),0,255);arr[:,:,3]=255;Image.fromarray(arr).save(out/(name+'.png'))
 item=Path('src/main/resources/assets/zoldrax/textures/item');image=Image.new('RGBA',(32,32));d=ImageDraw.Draw(image)
 d.rectangle((5,10,26,17),fill='#90938b');d.rectangle((11,3,21,11),fill='#b1b4a8');d.rectangle((12,6,14,7),fill='#f6ba43');d.rectangle((18,6,20,7),fill='#f6ba43');d.polygon([(11,11),(21,11),(18,18),(14,18)],fill='#d1d2c5');d.rectangle((10,17,15,29),fill='#7c8179');d.rectangle((18,17,23,29),fill='#7c8179');d.rectangle((16,13,18,29),fill='#291c3f');image.save(item/'ternor_monument.png')
