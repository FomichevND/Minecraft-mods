"""Software orthographic preview of the actual bbmodel geometry and texture atlas.
This is a model preview, not a Minecraft screenshot. Requires Pillow and numpy.
"""
from pathlib import Path
import json,numpy as np
from PIL import Image,ImageDraw
root=Path('src/main/resources/assets/zoldrax');W,H=600,900
canvas=Image.new('RGB',(W*3,H),(24,21,28))
for stage in range(1,4):
 m=json.loads((root/f'models/blockbench/stage_{stage}.bbmodel').read_text());texture=np.asarray(Image.open(root/f'textures/entity/stage_{stage}.png').convert('RGB'));texel_scale=texture.shape[0]/m['resolution']['height']
 pixels=np.zeros((H,W,3),dtype=np.uint8);pixels[:]=(24,21,28);depth=np.full((H,W),-1e10)
 view=np.array([.55,.35,1.]);view/=np.linalg.norm(view)
 right=np.cross([0,1,0],view);right/=np.linalg.norm(right);up=np.cross(view,right)
 scale=6.1
 def project(points):
  p=np.asarray(points)-np.array([0,53,7]);return np.stack([W/2+(p@right)*scale,H/2-(p@up)*scale,p@view],axis=1)
 def triangle(points,uv,shade):
  v=project(points);x0=max(0,int(np.floor(v[:,0].min())));x1=min(W-1,int(np.ceil(v[:,0].max())))
  y0=max(0,int(np.floor(v[:,1].min())));y1=min(H-1,int(np.ceil(v[:,1].max())))
  if x1<x0 or y1<y0:return
  x,y=np.meshgrid(np.arange(x0,x1+1)+.5,np.arange(y0,y1+1)+.5)
  a,b,c=v;den=(b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1])
  if abs(den)<1e-7:return
  w0=((b[1]-c[1])*(x-c[0])+(c[0]-b[0])*(y-c[1]))/den
  w1=((c[1]-a[1])*(x-c[0])+(a[0]-c[0])*(y-c[1]))/den;w2=1-w0-w1
  z=w0*a[2]+w1*b[2]+w2*c[2]
  part=depth[y0:y1+1,x0:x1+1];mask=(w0>=0)&(w1>=0)&(w2>=0)&(z>part)
  uv=np.asarray(uv)*texel_scale;u=(w0*uv[0,0]+w1*uv[1,0]+w2*uv[2,0]).astype(int).clip(0,texture.shape[0]-1);v=(w0*uv[0,1]+w1*uv[1,1]+w2*uv[2,1]).astype(int).clip(0,texture.shape[0]-1)
  color=(texture[v,u]*shade).clip(0,255).astype('uint8')
  pixels[y0:y1+1,x0:x1+1][mask]=color[mask];part[mask]=z[mask]
 for cube in m['elements']:
  x,y,z=cube['from'];X,Y,Z=cube['to']
  faces={
   'south':([(x,Y,Z),(X,Y,Z),(X,y,Z),(x,y,Z)],1),
   'east':([(X,Y,Z),(X,Y,z),(X,y,z),(X,y,Z)],.78),
   'up':([(x,Y,z),(X,Y,z),(X,Y,Z),(x,Y,Z)],1.18)}
  for face,(points,shade) in faces.items():
   u,v,U,V=cube['faces'][face]['uv'];uv=[(u,v),(U,v),(U,V),(u,V)]
   for indices in [[0,1,2],[0,2,3]]:triangle([points[i] for i in indices],[uv[i] for i in indices],1)
 image=Image.fromarray(pixels);d=ImageDraw.Draw(image);d.text((20,20),f'ZOLDRAX / STAGE {stage} / MODEL PREVIEW',fill=(213,190,162));canvas.paste(image,((stage-1)*W,0))
Path('docs').mkdir(exist_ok=True);canvas.save('docs/zoldrax-1.2.2-preview.png')
