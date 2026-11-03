"""Front/side/back orthographic renders of the actual NPC meshes and texture atlases."""
from pathlib import Path
from PIL import Image,ImageDraw
import json,numpy as np
ROOT=Path('src/main/resources/assets/zoldrax');W,H=600,850

def render(variant,direction):
 m=json.loads((ROOT/f'models/blockbench/{variant}.bbmodel').read_text());texture=np.asarray(Image.open(ROOT/f'textures/entity/{variant}.png').convert('RGB'));texel_scale=texture.shape[0]/m['resolution']['height']
 pixels=np.zeros((H,W,3),dtype='u1');pixels[:]=(24,21,28);depth=np.full((H,W),-1e10)
 view=np.array(direction,dtype=float);view/=np.linalg.norm(view);right=np.cross([0,1,0],view);right/=np.linalg.norm(right);up=np.cross(view,right)
 def project(points):
  p=np.asarray(points)-[0,21,0];return np.stack([W/2+(p@right)*15,H/2-(p@up)*15,p@view],axis=1)
 def triangle(points,uv):
  v=project(points);x0=max(0,int(np.floor(v[:,0].min())));x1=min(W-1,int(np.ceil(v[:,0].max())));y0=max(0,int(np.floor(v[:,1].min())));y1=min(H-1,int(np.ceil(v[:,1].max())))
  if x1<x0 or y1<y0:return
  x,y=np.meshgrid(np.arange(x0,x1+1)+.5,np.arange(y0,y1+1)+.5);a,b,c=v;den=(b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1])
  if abs(den)<1e-7:return
  w0=((b[1]-c[1])*(x-c[0])+(c[0]-b[0])*(y-c[1]))/den;w1=((c[1]-a[1])*(x-c[0])+(a[0]-c[0])*(y-c[1]))/den;w2=1-w0-w1;z=w0*a[2]+w1*b[2]+w2*c[2]
  part=depth[y0:y1+1,x0:x1+1];mask=(w0>=0)&(w1>=0)&(w2>=0)&(z>part);uv=np.asarray(uv)*texel_scale
  u=(w0*uv[0,0]+w1*uv[1,0]+w2*uv[2,0]).astype(int).clip(0,texture.shape[1]-1);v=(w0*uv[0,1]+w1*uv[1,1]+w2*uv[2,1]).astype(int).clip(0,texture.shape[0]-1)
  pixels[y0:y1+1,x0:x1+1][mask]=texture[v,u][mask];part[mask]=z[mask]
 for c in m['elements']:
  x,y,z=c['from'];X,Y,Z=c['to'];inflate=c.get('inflate',0);x-=inflate;y-=inflate;z-=inflate;X+=inflate;Y+=inflate;Z+=inflate
  faces={
   'south':([(x,Y,Z),(X,Y,Z),(X,y,Z),(x,y,Z)],[0,0,1]),'north':([(X,Y,z),(x,Y,z),(x,y,z),(X,y,z)],[0,0,-1]),
   'east':([(X,Y,Z),(X,Y,z),(X,y,z),(X,y,Z)],[1,0,0]),'west':([(x,Y,z),(x,Y,Z),(x,y,Z),(x,y,z)],[-1,0,0]),
   'up':([(x,Y,z),(X,Y,z),(X,Y,Z),(x,Y,Z)],[0,1,0]),'down':([(x,y,Z),(X,y,Z),(X,y,z),(x,y,z)],[0,-1,0])}
  for face,(points,normal) in faces.items():
   if np.dot(normal,view)<=0:continue
   u,v,U,V=c['faces'][face]['uv'];uv=[(u,v),(U,v),(U,V),(u,V)]
   for inds in [[0,1,2],[0,2,3]]:triangle([points[i] for i in inds],[uv[i] for i in inds])
 return Image.fromarray(pixels)

if __name__=='__main__':
 Path('docs').mkdir(exist_ok=True)
 for variant in ['chain_punisher','blood_priest']:
  canvas=Image.new('RGB',(3*W,H))
  for i,(label,view) in enumerate([('FRONT',(.16,.12,1)),('SIDE',(1,.13,.1)),('BACK',(-.16,.12,-1))]):
   image=render(variant,view);d=ImageDraw.Draw(image);d.text((18,18),variant.upper()+' / '+label,fill=(213,190,162));canvas.paste(image,(i*W,0))
  canvas.save(f'docs/{variant}-1.5.0-preview.png')
