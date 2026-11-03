"""Rigged Zoldrax cultists with 64 texels/block atlases and editable Blockbench bones."""
from pathlib import Path
from PIL import Image
import json,uuid,hashlib,numpy as np
from model_export import write_model
from texture_materials import paint as base_paint,noise
ROOT=Path('src/main/resources/assets/zoldrax')
PIVOTS={'head':[0,29,0],'body':[0,24,0],'rightArm':[-7,27,0],'leftArm':[7,27,0],'rightLeg':[-3,13,0],'leftLeg':[3,13,0],'cape':[0,27,-4]}

def paint(cube,face,w,h,cubes):
 kind=cube['material'];mapped={'plate':'metal','chain':'metal','cloth':'cloth','red':'cloth','cape':'cloth','bone':'stone','gold':'metal','skin':'stone','dark':'cloth','wood':'wood','eye':'ember','fire':'ember','stain':'metal','ruby':'ember'}
 targets={'plate':(49,43,57),'chain':(132,128,142),'cloth':(27,23,31),'red':(116,22,30),'cape':(75,13,24),'bone':(185,163,133),'gold':(166,118,33),'skin':(167,119,86),'dark':(12,9,13),'wood':(61,31,21),'eye':(250,89,18),'fire':(246,125,18),'stain':(106,21,26),'ruby':(196,18,23)}
 cube['material']=mapped[kind];image=np.asarray(base_paint(cube,face,w,h,cubes)).astype(float);cube['material']=kind
 original={'stone':(39,35,43),'metal':(36,36,37),'cloth':(97,86,69),'wood':(74,48,31),'ember':(170,32,8)}[mapped[kind]]
 image=image/np.array(original)*np.array(targets[kind])
 y,x=np.indices((h,w));u=(x+.5)/w;v=(y+.5)/h
 if kind in ['cloth','red','cape']:
  weave=((x+y)%2)*1.5-.75;fold=np.sin(u*np.pi*7)*2
  image+=(weave+fold)[...,None]
 if kind=='cape' and face in ['south','north']:
  # Horned cult mark, a central stem and hooks; intentionally part of the textile.
  stem=(abs(u-.5)<.045)&(v>.33)&(v<.83)
  arms=(abs(v-(.45+abs(u-.5)*.55))<.04)&(u>.22)&(u<.78)
  horns=((abs(u-.22)<.04)|(abs(u-.78)<.04))&(v>.28)&(v<.59)
  sigil=stem|arms|horns
  image[sigil]=np.array([189,46,42])+np.sin(y[sigil,None])*.7
  border=(u<.065)|(u>.935)|(v>.93)
  image[border]*=np.array([1.45,1.15,1.1])
 if kind in ['plate','chain','gold']:
  edge=np.minimum.reduce([x,w-1-x,y,h-1-y]);image+=((edge>1)&(edge<4))[...,None]*(9 if kind=='plate' else 14)
 if kind=='bone':
  rng=np.random.default_rng(int.from_bytes(hashlib.sha256((cube['name']+face).encode()).digest()[:8],'big'))
  image+=noise(rng,w,h,5)[...,None]*8
 if kind=='eye':
  core=(abs(u-.5)<.25)&(abs(v-.5)<.3);image[core]=[255,189,55]
 return Image.fromarray(np.uint8(image.clip(0,255)))

def make(variant):
 cubes=[];is_priest=variant=='blood_priest'
 def box(name,x,y,z,w,h,d,mat='plate',bone='body'):
  assert w==int(w) and h==int(h) and d==int(d)
  cubes.append({'name':name,'from_':[x,y,z],'to':[x+w,y+h,z+d],'material':mat,'bone':bone})
 def link(name,x,y,z,bone='body',metal='chain',depth=False):
  if not depth:
   box(name+'a',x,y,z,1,3,1,metal,bone);box(name+'b',x+2,y,z,1,3,1,metal,bone)
   box(name+'c',x+1,y,z,1,1,1,metal,bone);box(name+'d',x+1,y+2,z,1,1,1,metal,bone)
  else:
   box(name+'a',x,y,z,1,3,1,metal,bone);box(name+'b',x,y,z+2,1,3,1,metal,bone)
   box(name+'c',x,y,z+1,1,1,1,metal,bone);box(name+'d',x,y+2,z+1,1,1,1,metal,bone)
 def skull(name,x,y,z,bone,size=5):
  box(name+'_cranium',x,y+2,z,size,4,2,'bone',bone)
  box(name+'_socketL',x+1,y+3,z+2,1,2,1,'dark',bone);box(name+'_socketR',x+size-2,y+3,z+2,1,2,1,'dark',bone)
  box(name+'_nose',x+size//2,y+2,z+2,1,1,1,'dark',bone)
  for i in range(3):box(name+f'_tooth{i}',x+1+i,y,z+1,1,2,1,'bone',bone)
 # Shared humanoid underlayer. All accessories remain attached to their bone.
 box('padded_torso',-5,14,-3,10,14,6,'cloth')
 for side in [-1,1]:
  leg='rightLeg' if side<0 else 'leftLeg';arm='rightArm' if side<0 else 'leftArm'
  x=-5 if side<0 else 1
  box('trouser_'+leg,x,2,-2,4,12,4,'cloth',leg)
  box('boot_'+leg,x-1,0,-3,6,4,7,'plate',leg)
  box('boot_toe_'+leg,x-1,0,3,6,2,2,'plate',leg)
  box('upper_arm_'+arm,-10 if side<0 else 6,19,-2,4,9,5,'cloth',arm)
  box('hand_'+arm,-10 if side<0 else 6,13,-2,4,6,5,'skin',arm)
 if not is_priest:
  box('neck_scarf',-5,27,-4,10,2,8,'red')
  box('layered_cuirass',-6,16,-4,12,11,8,'plate')
  box('red_tabard',-4,5,4,8,21,1,'cape')
  for y in [18,22,26]:
   box(f'chest_lameL_{y}',-6,y,4,3,3,1,'plate');box(f'chest_lameR_{y}',3,y,4,3,3,1,'plate')
  box('waist_belt',-6,14,-4,12,2,9,'wood');box('belt_buckle',-2,14,5,4,3,1,'chain')
  box('buckle_dark',-1,15,6,2,1,1,'dark')
  for side in [-1,1]:
   bone='rightArm' if side<0 else 'leftArm';x=-11 if side<0 else 5
   box('pauldron_'+bone,x,26,-4,7,4,9,'plate',bone)
   box('shoulder_lame_'+bone,x,24,-4,7,2,9,'red',bone)
   box('shoulder_ridge_'+bone,x,29,-3,7,1,7,'chain',bone)
   box('forearm_guard_'+bone,-11 if side<0 else 5,13,-3,6,7,7,'plate',bone)
   for y in [13,18]:box(f'cuff_{bone}_{y}',-11 if side<0 else 5,y,-3,6,1,7,'chain',bone)
   box('knuckles_'+bone,-11 if side<0 else 5,12,3,6,3,2,'plate',bone)
   leg='rightLeg' if side<0 else 'leftLeg';x=-6 if side<0 else 0
   box('greave_'+leg,x,4,2,6,7,2,'plate',leg);box('knee_'+leg,x,11,2,6,3,2,'chain',leg)
   for j in range(3):box(f'hip_lame_{side}_{j}',-7 if side<0 else 4,8+j*2,-3,3,3,8,'plate')
  skull('shoulder_skull',-11,27,5,'rightArm')
  for i in range(9):link(f'chest_chain_{i}',-5+i,25-i,5,depth=bool(i%2))
  for i in range(4):link(f'collar_chain_{i}',-5+i*3,27-abs(i-1),4)
  box('helmet_shell',-5,29,-5,10,9,10,'plate','head')
  box('visor_shadow',-4,29,5,8,6,1,'dark','head')
  box('helmet_brow',-5,35,5,10,2,1,'plate','head')
  for x in [-4,-2,0,2,4]:box(f'visor_bar_{x}',x,29,6,1,6,1,'plate','head')
  box('glow_eye_left',-3,34,5.9,2,1,1,'eye','head');box('glow_eye_right',1,34,5.9,2,1,1,'eye','head')
  box('helmet_crimson_crest',-1,36,-4,2,3,10,'red','head')
  box('helmet_nose_guard',-1,29,7,2,7,1,'plate','head')
  box('cape_main',-7,5,-6,14,23,1,'cape','cape')
  for i in [0,2,3]:box(f'cape_ragged_{i}',-7+i*4,3-i%2,-6,3,3,1,'red','cape')
  for side in [-1,1]:box(f'cape_border_{side}',-7 if side<0 else 6,5,-7,1,23,1,'red','cape')
  for i in range(5):link(f'back_chain_{i}',-7+i*3,26-i//2,-8)
  for i in range(4):link(f'weapon_chain_{i}',-11,5+i*3,1,'rightArm',depth=bool(i%2))
  box('hook_spine',-14,1,1,2,6,2,'chain','rightArm');box('hook_lower_blade',-13,0,1,8,2,2,'chain','rightArm')
  box('hook_point',-6,2,1,2,4,2,'chain','rightArm');box('hook_tip',-7,5,1,2,1,2,'chain','rightArm')
  box('hook_red_edge',-12,0,3,7,1,1,'stain','rightArm')
 else:
  box('robe_core',-6,4,-4,12,21,8,'cloth')
  box('robe_hem',-8,2,-4,16,6,9,'cloth')
  box('ritual_front',-4,3,5,8,22,1,'cape')
  for side in [-1,1]:
   box(f'robe_red_side_{side}',-8 if side<0 else 6,3,4,2,22,2,'red')
   box(f'shoulder_mantle_{side}',-11 if side<0 else 5,25,-4,6,4,10,'cloth','rightArm' if side<0 else 'leftArm')
   box(f'mantle_piping_{side}',-11 if side<0 else 5,25,5,6,2,1,'red','rightArm' if side<0 else 'leftArm')
   arm='rightArm' if side<0 else 'leftArm'
   box('wide_sleeve_'+arm,-11 if side<0 else 5,18,-3,6,8,7,'cloth',arm)
   box('sleeve_trim_'+arm,-11 if side<0 else 5,18,3,6,2,1,'red',arm)
  box('belt',-6,15,-4,12,2,10,'wood');skull('belt_skull',0,13,6,'body')
  box('neck_scarf',-5,27,-4,10,2,8,'red')
  for i in range(5):link(f'gold_necklace_{i}',-5+i*2,26-abs(2-i),4,metal='gold')
  box('amulet_rim',-2,22,5,4,5,1,'gold');box('glow_amulet_ruby',-1,23,6,2,3,1,'ruby')
  box('hood_back',-5,29,-5,10,10,2,'cloth','head')
  box('hood_top',-5,37,-3,10,2,8,'cloth','head')
  for side in [-1,1]:
   box(f'hood_side_{side}',-5 if side<0 else 3,28,-3,2,10,8,'cloth','head')
   box(f'hood_crimson_border_{side}',-5 if side<0 else 4,29,5,1,9,1,'red','head')
   box(f'horn_root_{side}',-8 if side<0 else 5,36,-1,3,3,3,'bone','head')
   box(f'horn_arc_{side}',-10 if side<0 else 8,38,-1,2,4,2,'bone','head')
   box(f'horn_tip_{side}',-9 if side<0 else 6,42,-1,3,1,2,'bone','head')
  box('hood_rim',-5,38,4,10,1,2,'red','head')
  box('ritual_mask',-3,30,3,6,7,2,'bone','head')
  box('mask_socketL',-3,33,5,2,2,1,'dark','head');box('mask_socketR',1,33,5,2,2,1,'dark','head')
  box('glow_eyeL',-3,33,6,2,1,1,'eye','head');box('glow_eyeR',1,33,6,2,1,1,'eye','head')
  box('mask_nose',-1,30,5,2,4,2,'bone','head')
  for x in [-3,0,2]:box(f'mask_jaw_{x}',x,28,4,1,4,2,'bone','head')
  box('priest_cape',-7,3,-6,14,25,1,'cape','cape')
  for x in [-7,5]:box(f'cape_red_edge_{x}',x,3,-7,2,25,1,'red','cape')
  box('staff_gripping_hand',-13,18,-2,4,4,5,'skin','rightArm')
  box('right_hand_closed_fingers',-12,15,-2,4,4,5,'skin','rightArm')
  box('staff_shaft',-13,0,0,2,36,2,'wood','rightArm')
  for y in [1,15,29]:box(f'staff_gold_band_{y}',-14,y,-1,4,2,4,'gold','rightArm')
  box('staff_brazier',-17,34,-3,10,3,8,'plate','rightArm')
  box('staff_brazier_rim',-18,37,-4,12,1,10,'gold','rightArm')
  for i,(x,y,h) in enumerate([(-16,38,3),(-13,38,6),(-10,38,4)]):box(f'glow_flame_{i}',x,y,-1,2,h,3,'fire','rightArm')
  for x in [-17,-8]:box(f'brazier_iron_edge_{x}',x,34,-3,1,4,8,'plate','rightArm')
 # Weapons are actual equipped items now; retain only the armour chains.
 remove=('staff_','brazier_iron_edge','glow_flame_') if is_priest else ('weapon_chain_','hook_')
 cubes[:]=[c for c in cubes if not c['name'].startswith(remove)]
 write_model(cubes,0,variant+'_',paint,size=256,texel_scale=4)
 src=ROOT/f'models/blockbench/{variant}_stage_1.bbmodel';model=json.loads(src.read_text());src.unlink()
 oldtex=ROOT/f'textures/entity/{variant}_stage_1.png';oldtex.replace(ROOT/f'textures/entity/{variant}.png')
 model['name']='Цепной каратель' if not is_priest else 'Жарокровный жрец';model['model_identifier']=variant;model['textures'][0]['name']=variant+'.png'
 model['zoldrax_bones']=PIVOTS
 groups={bone:[] for bone in PIVOTS}
 for c,e in zip(cubes,model['elements']):
  e['zoldrax_bone']=c['bone'];groups[c['bone']].append(e['uuid'])
  # Standard Blockbench inflation separates layered coplanar fabrics/armour.
  e['inflate']={'plate':.02,'chain':.06,'red':.04,'gold':.08,'cape':.015,'ruby':.04}.get(c['material'],0)
  if 'mantle' in c['name'] or 'brazier_iron_edge' in c['name']:e['inflate']+=.03
 model['outliner']=[{'name':bone,'uuid':str(uuid.uuid5(uuid.NAMESPACE_URL,variant+bone)),'origin':pivot,'rotation':[0,0,0],'children':groups[bone],'export':True,'isOpen':True} for bone,pivot in PIVOTS.items()]
 (ROOT/f'models/blockbench/{variant}.bbmodel').write_text(json.dumps(model,ensure_ascii=False,indent=2))
 print(variant,len(cubes),'rigged cubes; atlas 1024px, 64 texels/block')

if __name__=='__main__':
 for variant in ['chain_punisher','blood_priest']:make(variant)
