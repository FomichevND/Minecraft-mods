"""Reference-inspired, bone-rigged Monkey King followers and separate held tools."""
from pathlib import Path
from PIL import Image,ImageDraw
import json,uuid,hashlib,numpy as np
from model_export import write_model
R=Path('src/main/resources/assets/zoldrax')
P={'head':[0,24,0],'body':[0,24,0],'rightArm':[-6,22,0],'leftArm':[6,22,0],'rightLeg':[-2,12,0],'leftLeg':[2,12,0],'cape':[0,23,-3]}
COL={'skin':(225,161,115),'hair':(82,48,28),'red':(173,30,38),'ivory':(221,211,182),'gold':(237,178,32),'green':(47,112,35),'brown':(103,68,40),'wicker':(151,99,42),'dark':(40,24,22),'white':(249,236,209),'ruby':(199,34,28),'banana':(252,204,40),'cape':(155,27,35),'fruit':(190,44,27)}
def paint(c,face,w,h,others):
 color=np.array(COL[c['material']],float);y,x=np.indices((h,w));rng=np.random.default_rng(int.from_bytes(hashlib.sha256((c['name']+face).encode()).digest()[:8],'big'))
 noise=rng.normal(0,1.5,(h,w));edge=np.minimum.reduce([x,w-1-x,y,h-1-y]);shade=np.where(edge<2,-6,np.where(edge<5,3,0));a=np.broadcast_to(color,(h,w,3)).copy()+(noise+shade)[...,None]
 kind=c['material']
 if kind=='wicker':a+=(((y//6)%2)*3+np.where((x+(y//8%2)*4)%16<2,-22,0)+np.where(y%8<2,-19,0))[...,None]
 if kind in ['ivory','red','cape']:
  a+=(np.sin(x/max(1,w)*20)*3+((x+y)%3)*.7)[...,None]
  seam=(x<3)|(x>w-4)|(y<3)|(y>h-4)
  a[seam]=np.array(COL[kind])*.78
  if kind in ['red','cape']:
   embroidery=((y%24<2)&(x%12<7))|((x%24<2)&(y%12<7))
   a[embroidery&((y<9)|(y>h-10))]=[232,181,51]
 if kind=='gold':a+=(np.sin(y/max(1,h)*6.28)*14+np.cos(x*.3)*3)[...,None]
 if kind=='brown':a+=(np.sin(y*.7)*3+np.sin(x*.19)*4)[...,None]
 if kind=='hair':a+=(np.sin(x*.4)*5)[...,None]
 # All facial features belong to the front surface of the head cube.
 # No raised eyes, teeth, cheeks, nose or beard geometry.
 if c['name']=='face' and face=='north':
  u=(x+.5)/w;v=(y+.5)/h
  def mark(mask,col):a[mask]=col
  mark(((u<.15)|(u>.85))&(v<.26),COL['hair'])
  for center in [.28,.72]:
   mark((abs(u-center)<.14)&(v>.37)&(v<.57),(250,246,225))
   mark((abs(u-center)<.055)&(v>.39)&(v<.56),(83,54,31))
   mark((abs(u-center)<.025)&(v>.40)&(v<.55),(28,22,20))
   mark((abs(u-center+.025)<.017)&(v>.40)&(v<.44),(255,255,245))
   mark((abs(u-center)<.15)&(v>.29)&(v<.33),COL['hair'])
  mark((abs(u-.5)<.06)&(v>.57)&(v<.66),(193,123,85))
  mark((abs(u-.5)<.21)&(v>.74)&(v<.86),(91,43,30))
  mark((abs(u-.5)<.17)&(v>.74)&(v<.79),(251,241,216))
 if kind=='cape' and face in ['south','north']:
  u=(x+.5)/w;v=(y+.5)/h
  banana=((u-.47)**2/.27**2+(v-.44)**2/.27**2<1)&~((u-.55)**2/.25**2+(v-.33)**2/.27**2<1)
  border=(u<.06)|(u>.94)|(v>.93);a[banana|border]=[247,191,38]
 return Image.fromarray(np.uint8(a.clip(0,255)))
def make(herald):
 name='banana_herald' if herald else 'temple_gatherer';c=[]
 def b(n,x,y,z,w,h,d,m='gold',bone='body'):c.append({'name':n,'from_':[x,y,z],'to':[x+w,y+h,z+d],'material':m,'bone':bone})
 b('shirt',-4,12,-2,8,12,4,'red' if herald else 'ivory')
 b('neck',-2,23,-2,4,2,4,'skin');b('face',-4,24,-4,8,8,8,'skin','head')
 b('hair_cap',-4,30,-4,8,2,8,'hair','head');b('hair_back',-4,25,-4,8,6,1,'hair','head')
 for x in [-5,4]:b('ear'+str(x),x,26,-1,1,3,2,'skin','head')
 for x in [-4,3]:b('sideburn'+str(x),x,25,0,1,6,3,'hair','head')
 for side in [-1,1]:
  arm='rightArm' if side<0 else 'leftArm';leg='rightLeg' if side<0 else 'leftLeg';x=-8 if side<0 else 4
  b('sleeve'+arm,x,19,-2,4,5,4,'ivory',arm);b('arm'+arm,x,12,-2,4,7,4,'skin',arm);b('bracelet'+arm,x,14,-2,4,1,4,'gold',arm)
  lx=-4.25 if side<0 else .25
  b('leg'+leg,lx,1,-2,4,11,4,'skin',leg);b('shorts'+leg,lx,8,-2,4,5,4,'red' if herald else 'brown',leg)
  b('short_hem'+leg,lx,8,-2,4,1,4,'gold' if herald else 'ivory',leg)
  b('sandal'+leg,lx,0,-2,4,1,5,'brown',leg);b('sandal_strap'+leg,lx,2,-2,4,1,4,'ivory',leg)
 b('belt',-4,12,-3,8,2,6,'green' if herald else 'brown');b('buckle',-1,12,3,2,2,1,'gold')
 b('apron',-3,7,3,6,6,1,'cape');b('green_sash',1,6,4,1,7,1,'green')
 b('collar',-4,23,-3,8,1,6,'gold');b('amulet_rim',-2,20,3,4,4,1,'gold');b('amulet_jade',-1,21,4,2,2,1,'green')
 if herald:
  b('crown_band',-5,32,-5,10,1,10,'gold','head')
  for x in [-4,0,3]:
   for z in [-4,4]:b('crown_point'+str(x)+str(z),x,33,z,2,2,1,'gold','head')
  b('crown_jewel',0,33,5,1,1,1,'ruby','head')
  b('ceremonial_cape',-5,11,-4,10,12,1,'cape','cape')
  b('left_shoulder',4,23,-3,5,2,6,'red','leftArm');b('shoulder_gold_fringe',4,22,2,5,1,1,'gold','leftArm')
 else:
  b('straw_brim',-7,32,-6,14,1,12,'wicker','head');b('hat_crown',-4,33,-4,8,2,8,'wicker','head');b('hat_band',-4,33,-5,8,1,1,'red','head');b('hat_buckle',-1,33,-6,2,2,1,'gold','head')
  b('basket',-5,12,-8,10,11,5,'wicker');b('basket_top_rim',-6,23,-9,12,1,7,'wicker')
  for x in [-5,4]:b('basket_corner'+str(x),x,12,-9,1,11,1,'brown')
  for x in [-3,2]:b('strap'+str(x),x,14,2,1,9,1,'brown')
  b('basket_temple_flag',-2,14,-9,4,6,1,'cape')
  for i in range(5):b('fruit'+str(i),-4+i*2,24,-7+(i%2),2,2+i%2,2,'fruit' if i%2==0 else 'green')
  for i in range(4):b('banana_curve'+str(i),-4+i,25+(1 if i in [0,3] else 0),-9,1,1,2,'banana')
  for x in [-5,4]:b('belt_pouch'+str(x),x,11,-1,2,3,3,'brown')
 write_model(c,0,name+'_',paint,size=256,texel_scale=4)
 path=R/f'models/blockbench/{name}_stage_1.bbmodel';m=json.loads(path.read_text());path.unlink();(R/f'textures/entity/{name}_stage_1.png').replace(R/f'textures/entity/{name}.png')
 m['name']='Банановый глашатай' if herald else 'Храмовый добытчик';m['zoldrax_bones']=P;m['model_identifier']=name;m['textures'][0]['name']=name+'.png';groups={bone:[] for bone in P}
 for e in m['elements']:
  e['faces']['north'],e['faces']['south']=e['faces']['south'],e['faces']['north']
 for cube,e in zip(c,m['elements']):e['zoldrax_bone']=cube['bone'];groups[cube['bone']].append(e['uuid']);e['inflate']=.04 if cube['material']=='hair' else (.06 if cube['name'].startswith('short_hem') else (.03 if cube['name'].startswith(('shorts','sandal_strap')) else (.02 if cube['material'] in ['gold','cape'] else 0)))
 m['outliner']=[{'name':bone,'uuid':str(uuid.uuid5(uuid.NAMESPACE_URL,name+bone)),'origin':pivot,'rotation':[0,0,0],'children':groups[bone],'export':True} for bone,pivot in P.items()]
 (R/f'models/blockbench/{name}.bbmodel').write_text(json.dumps(m,ensure_ascii=False,indent=2))
def tool(name,horn):
 im=Image.new('RGBA',(64,64));d=ImageDraw.Draw(im)
 for i,col in enumerate(['#e6af24','#775033','#c4c5d1','#40312a']):d.rectangle((0,i*16,63,i*16+15),fill=col)
 im.save(R/f'textures/item/{name}.png');el=[]
 def b(n,lo,hi,k):el.append({'name':n,'from':lo,'to':hi,'faces':{f:{'uv':[0,k*4,4,k*4+4],'texture':'#tool'} for f in ['up','down','north','south','east','west']}})
 if horn:
  b('mouthpiece',[7,0,7],[9,3,9],0);b('tube',[6,3,6],[10,9,10],0)
  for y,w in [(9,6),(11,8),(13,10)]:
   lo=8-w/2;hi=8+w/2
   for axis in [0,2]:
    for edge in [lo,hi-1]:
     p=[lo,y,lo];q=[hi,y+2,hi];p[axis]=edge;q[axis]=edge+1;b('bell'+str(y)+str(axis)+str(edge),p,q,0)
  b('bell_dark',[5,11,5],[11,12,11],3)
 else:
  b('grip',[7,0,7],[9,7,9],1);b('pommel',[6,0,6],[10,1,10],0);b('guard',[4,7,6],[12,8,10],0);b('blade',[6,8,7],[10,20,9],2);b('tip',[7,20,7],[10,22,9],2)
 (R/f'models/item/{name}.json').write_text(json.dumps({'parent':'minecraft:block/block','textures':{'tool':'zoldrax:item/'+name,'particle':'zoldrax:item/'+name},'elements':el,'display':{'thirdperson_righthand':{'rotation':[90,0,0],'translation':[0,0,0],'scale':[.8,.8,.8]},'thirdperson_lefthand':{'rotation':[90,0,0],'translation':[0,0,0],'scale':[.8,.8,.8]},'gui':{'rotation':[20,210,-15],'scale':[.8,.8,.8]},'firstperson_righthand':{'rotation':[0,0,-15],'translation':[0,0,0],'scale':[.8,.8,.8]}}},indent=2))
if __name__=='__main__':
 for h in [True,False]:make(h)
 tool('herald_horn',True);tool('harvest_machete',False)
 for lang in ['ru_ru','en_us']:
  p=R/f'lang/{lang}.json';a=json.loads(p.read_text());ru=lang=='ru_ru'
  for k,en,ru_name in [('banana_herald','Banana Herald','Банановый глашатай'),('temple_gatherer','Temple Gatherer','Храмовый добытчик')]:a['entity.zoldrax.'+k]=ru_name if ru else en;a['item.zoldrax.'+k+'_spawn_egg']=('Яйцо призыва: '+ru_name) if ru else en+' Spawn Egg';(R/f'models/item/{k}_spawn_egg.json').write_text('{"parent":"minecraft:item/template_spawn_egg"}')
  for k,en,ru_name in [('herald_horn','Herald Horn','Рожок глашатая'),('harvest_machete','Harvest Machete','Мачете добытчика')]:a['item.zoldrax.'+k]=ru_name if ru else en
  p.write_text(json.dumps(a,ensure_ascii=False,indent=2))
