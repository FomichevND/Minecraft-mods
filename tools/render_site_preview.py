"""Software preview of actual structure blocks using vanilla 1.20.1 block models/textures.
Requires Pillow/numpy and a locally installed vanilla client JAR (not redistributed).
Block-entity beds/chests/banners are schematic; lighting is illustrative, not a game screenshot.
"""
from pathlib import Path
from functools import lru_cache
from PIL import Image,ImageDraw,ImageFont
import numpy as np,json,zipfile,os
jar=Path(os.environ.get('MINECRAFT_CLIENT_JAR','/home/agent/.gradle/caches/fabric-loom/1.20.1/minecraft-client.jar'))
assets=zipfile.ZipFile(jar);root=Path('src/main/resources/assets/zoldrax')
@lru_cache(None)
def resource(path):return json.loads(assets.read('assets/minecraft/'+path+'.json'))
@lru_cache(None)
def model(name):
 name=name.replace('minecraft:','');d=resource('models/'+name)
 base={'textures':{}}
 if 'parent' in d and not d['parent'].startswith('builtin'):
  try:base=model(d['parent'])
  except KeyError:pass
 return {**base,**d,'textures':{**base.get('textures',{}),**d.get('textures',{})}}
@lru_cache(None)
def texture(name):
 if name.startswith('zoldrax:'):return np.asarray(Image.open(root/(name.split(':')[1]+'.png')).convert('RGBA'))
 import io
 return np.asarray(Image.open(io.BytesIO(assets.read('assets/minecraft/textures/'+name.replace('minecraft:','')+'.png'))).convert('RGBA'))
def resolve(name,textures):
 for _ in range(12):
  if not name.startswith('#'):return name
  name=textures[name[1:]]
 raise ValueError('Texture reference cycle')
def condition(rule,props):
 if 'OR' in rule:return any(condition(r,props) for r in rule['OR'])
 if 'AND' in rule:return all(condition(r,props) for r in rule['AND'])
 return all(props.get(k,'false') in v.split('|') for k,v in rule.items())
@lru_cache(None)
def block_models(name,properties):
 props=dict(properties)
 try:d=resource('blockstates/'+name)
 except KeyError:return []
 if 'variants' in d:
  for key,value in d['variants'].items():
   if all(props.get(k,v)==v for k,v in (x.split('=') for x in key.split(',') if x)):
    return [value[0] if isinstance(value,list) else value]
  return []
 return [v['apply'][0] if isinstance(v['apply'],list) else v['apply'] for v in d.get('multipart',[]) if 'when' not in v or condition(v['when'],props)]
def rotation(axis,degrees):
 a=np.radians(degrees);c,s=np.cos(a),np.sin(a)
 return np.array([[1,0,0],[0,c,-s],[0,s,c]]) if axis=='x' else (np.array([[c,0,s],[0,1,0],[-s,0,c]]) if axis=='y' else np.array([[c,-s,0],[s,c,0],[0,0,1]]))
faces={
 'south':([0,1,1],[1,1,1],[1,0,1],[0,0,1]),'north':([1,1,0],[0,1,0],[0,0,0],[1,0,0]),
 'east':([1,1,1],[1,1,0],[1,0,0],[1,0,1]),'west':([0,1,0],[0,1,1],[0,0,1],[0,0,0]),
 'up':([0,1,0],[1,1,0],[1,1,1],[0,1,1]),'down':([0,0,1],[1,0,1],[1,0,0],[0,0,0])}
normals={'south':(0,0,1),'north':(0,0,-1),'east':(1,0,0),'west':(-1,0,0),'up':(0,1,0),'down':(0,-1,0)}
font='/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
def render(name,cutaway=False):
 W,H=1200,1020;pixels=np.empty((H,W,3),np.uint8);pixels[:]=(30,29,37);depth=np.full((H,W),-1e10)
 view=np.array([.8,.7,1.]);view/=np.linalg.norm(view);right=np.cross([0,1,0],view);right/=np.linalg.norm(right);up=np.cross(view,right)
 center=np.array([15.5,6.5,15.5]);scale=24
 data=json.loads(Path(f'docs/{name}-layout.json').read_text())
 blocks=data['blocks']
 if cutaway:
  blocks=[b for b in blocks if 1<=b['pos'][0]<=13 and 17<=b['pos'][2]<=29 and b['pos'][1]<7]
  # Slice the front and right walls above the plinth to expose furnishing.
  xmax=11;zmax=25 if name=='blood_shrine' else 26
  blocks=[b for b in blocks if not (b['pos'][1]>=3 and (b['pos'][0]==xmax or b['pos'][2]>=zmax))]
  center=np.array([7,3.3,22.5]);scale=66
 def triangle(points,uv,tex,shade):
  p=np.asarray(points)-center
  v=np.stack([W/2+(p@right)*scale,H/2-(p@up)*scale,p@view],axis=1)
  x0=max(0,int(np.floor(v[:,0].min())));x1=min(W-1,int(np.ceil(v[:,0].max())))
  y0=max(0,int(np.floor(v[:,1].min())));y1=min(H-1,int(np.ceil(v[:,1].max())))
  if x1<x0 or y1<y0:return
  x,y=np.meshgrid(np.arange(x0,x1+1)+.5,np.arange(y0,y1+1)+.5);a,b,c=v
  den=(b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1])
  if abs(den)<1e-7:return
  w0=((b[1]-c[1])*(x-c[0])+(c[0]-b[0])*(y-c[1]))/den;w1=((c[1]-a[1])*(x-c[0])+(a[0]-c[0])*(y-c[1]))/den;w2=1-w0-w1
  z=w0*a[2]+w1*b[2]+w2*c[2];part=depth[y0:y1+1,x0:x1+1];mask=(w0>=0)&(w1>=0)&(w2>=0)&(z>part)
  uv=np.asarray(uv);u=np.floor((w0*uv[0,0]+w1*uv[1,0]+w2*uv[2,0])*tex.shape[1]/16).astype(int)%tex.shape[1]
  t=np.floor((w0*uv[0,1]+w1*uv[1,1]+w2*uv[2,1])*tex.shape[1]/16).astype(int)%tex.shape[0]
  colors=tex[t,u];mask&=colors[:,:,3]>127
  pixels[y0:y1+1,x0:x1+1][mask]=(colors[:,:,:3]*shade).clip(0,255).astype(np.uint8)[mask];part[mask]=z[mask]
 def cube(lo,hi,fs,textures,transform,position,emissive=False):
  for face,desc in fs.items():
   normal=transform@np.array(normals[face]);dot=normal@view
   if dot<=.001:continue
   points=np.array(faces[face])*(np.array(hi)-lo)+lo;points=(points-8)@transform.T+8;points=points/16+position
   try:tex=texture(resolve(desc['texture'],textures))
   except KeyError:continue
   u,v,U,V=desc.get('uv',[0,0,16,16]);uv=[(u,v),(U,v),(U,V),(u,V)]
   turns=desc.get('rotation',0)//90
   if turns:uv=uv[-turns:]+uv[:-turns]
   shade=1 if emissive else (.65+.3*max(0,normal@np.array([-.3,.9,.6])))
   for idx in [(0,1,2),(0,2,3)]:triangle(points[list(idx)],[uv[j] for j in idx],tex,shade)
 for b in blocks:
  state=b['state'];position=np.array(b['pos']);n=state['Name'].split(':')[-1]
  if n=='monument':
   m=json.loads((root/'models/blockbench/stage_3.bbmodel').read_text())
   for e in m['elements']:
    fs={f:{'uv':[value*16/2048 for value in d['uv']],'texture':'zoldrax:textures/entity/stage_3'} for f,d in e['faces'].items()}
    # UV coordinates in bbmodel's logical 1024 atlas, actual PNG is 2048.
    fs={f:{**d,'uv':[v*2 for v in d['uv']]} for f,d in fs.items()}
    cube(np.array(e['from']),np.array(e['to']),fs,{},np.eye(3),position+np.array([.5,0,.5]),e['name'].startswith('glow_'))
   continue
  variants=block_models(n,tuple(sorted(state.get('Properties',{}).items())))
  for variant in variants:
   try:m=model(variant['model'])
   except KeyError:continue
   transform=rotation('y',-variant.get('y',0))@rotation('x',-variant.get('x',0))
   for e in m.get('elements',[]):
    # Current structural palette has axis-aligned elements; item-like rotated
    # candle flames etc. may be omitted by this illustrative renderer.
    cube(np.array(e['from']),np.array(e['to']),e['faces'],m.get('textures',{}),transform,position,'lantern' in n or 'candle' in n)
 image=Image.fromarray(pixels);d=ImageDraw.Draw(image)
 title=('Кровавое капище' if name=='blood_shrine' else 'Лагерь цепных карателей')+(' — разрез зала' if cutaway else '')
 d.text((30,25),title,font=ImageFont.truetype(font,28),fill='#edd1b6')
 d.text((30,68),'Модели и текстуры игровых блоков • программный предпросмотр',font=ImageFont.truetype(font,17),fill='#ada4ae')
 d.text((30,H-45),'Не скриншот игры. Кровати, сундуки и знамёна в превью скрыты.',font=ImageFont.truetype(font,16),fill='#ada4ae')
 path=f'docs/{name}-1.7.1'+('-interior' if cutaway else '-preview')+'.png';image.save(path);print(path,flush=True)
for site in ['blood_shrine','chain_camp']:
 render(site);render(site,True)
