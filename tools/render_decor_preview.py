"""Orthographic review of the editable source meshes (not Minecraft screenshots)."""
from pathlib import Path
from PIL import Image, ImageDraw
import numpy as np, json, math
ROOT=Path('src/main/resources/assets/zoldrax'); W=400;H=460

def render(model,view):
 tex=np.asarray(Image.open(ROOT/f"textures/block/zoldrax/{model['name']}.png").convert('RGB'))
 pixels=np.full((H,W,3),(29,25,32),dtype=np.uint8);depth=np.full((H,W),-1e10)
 view=np.array(view,dtype=float);view/=np.linalg.norm(view);right=np.cross([0,1,0],view);right/=np.linalg.norm(right);up=np.cross(view,right)
 points=np.array([c[k] for c in model['elements'] if c['zoldrax_group']!='extension' for k in ['from','to']]);center=(points.min(0)+points.max(0))/2;scale=350/max(np.ptp(points@up),np.ptp(points@right))
 def triangle(points,uv,shade):
  p=np.asarray(points)-center;v=np.stack([W/2+p@right*scale,H/2-p@up*scale,p@view],1)
  x0=max(0,int(v[:,0].min()));x1=min(W-1,int(v[:,0].max()+1));y0=max(0,int(v[:,1].min()));y1=min(H-1,int(v[:,1].max()+1))
  if x1<x0 or y1<y0:return
  x,y=np.meshgrid(np.arange(x0,x1+1)+.5,np.arange(y0,y1+1)+.5);a,b,c=v;den=(b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1])
  if abs(den)<1e-7:return
  w0=((b[1]-c[1])*(x-c[0])+(c[0]-b[0])*(y-c[1]))/den;w1=((c[1]-a[1])*(x-c[0])+(a[0]-c[0])*(y-c[1]))/den;w2=1-w0-w1;z=w0*a[2]+w1*b[2]+w2*c[2];part=depth[y0:y1+1,x0:x1+1];mask=(w0>=0)&(w1>=0)&(w2>=0)&(z>part);uv=np.array(uv)
  u=(w0*uv[0,0]+w1*uv[1,0]+w2*uv[2,0]).astype(int).clip(0,255);v=(w0*uv[0,1]+w1*uv[1,1]+w2*uv[2,1]).astype(int).clip(0,255);pixels[y0:y1+1,x0:x1+1][mask]=(tex[v,u]*shade).clip(0,255).astype('u1')[mask];part[mask]=z[mask]
 for c in model['elements']:
  if c['zoldrax_group']=='extension':continue
  x,y,z=c['from'];X,Y,Z=c['to'];rx,ry,rz=np.radians(c['rotation']);o=np.array(c['origin']);
  R=np.array([[math.cos(rz),-math.sin(rz),0],[math.sin(rz),math.cos(rz),0],[0,0,1]])@np.array([[math.cos(ry),0,math.sin(ry)],[0,1,0],[-math.sin(ry),0,math.cos(ry)]])@np.array([[1,0,0],[0,math.cos(rx),-math.sin(rx)],[0,math.sin(rx),math.cos(rx)]])
  faces={'south':([(x,Y,Z),(x,y,Z),(X,y,Z),(X,Y,Z)],[0,0,1]),'north':([(X,Y,z),(X,y,z),(x,y,z),(x,Y,z)],[0,0,-1]),'east':([(X,Y,Z),(X,y,Z),(X,y,z),(X,Y,z)],[1,0,0]),'west':([(x,Y,z),(x,y,z),(x,y,Z),(x,Y,Z)],[-1,0,0]),'up':([(x,Y,z),(x,Y,Z),(X,Y,Z),(X,Y,z)],[0,1,0]),'down':([(x,y,Z),(x,y,z),(X,y,z),(X,y,Z)],[0,-1,0])}
  for f,(pts,n) in faces.items():
   n=R@np.array(n)
   if n@view<=0:continue
   pts=(np.array(pts)-o)@R.T+o;u,v,U,V=c['faces'][f]['uv'];uv=[(u,v),(u,V),(U,V),(U,v)];shade=1 if c['zoldrax_emissive'] else .68+.32*max(0,n@np.array([.3,.8,.5]))
   for inds in [[0,1,2],[0,2,3]]:triangle([pts[i] for i in inds],[uv[i] for i in inds],shade)
 return Image.fromarray(pixels)

if __name__=='__main__':
 out=Path('docs/decor');out.mkdir(parents=True,exist_ok=True);sheet=Image.new('RGB',(W*4,H*8))
 for row,path in enumerate(sorted((ROOT/'models/blockbench/decor').glob('*.bbmodel'))):
  model=json.loads(path.read_text());canvas=Image.new('RGB',(W*4,H))
  for i,(label,view) in enumerate([('FRONT',(.15,.2,1)),('SIDE',(1,.15,.03)),('BACK',(-.15,.2,-1)),('TOP',(.001,1,.001))]):
   im=render(model,view);ImageDraw.Draw(im).text((12,12),model['name'].replace('zoldrax_','')+' / '+label,fill=(238,210,173));canvas.paste(im,(i*W,0))
  canvas.save(out/(path.stem+'-source.png'));sheet.paste(canvas,(0,row*H))
 sheet.resize((1200,2760)).save(out/'all-source-views.jpg',quality=90)
