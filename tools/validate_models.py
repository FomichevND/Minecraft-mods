"""Regression checks for UV overflow, overlapping cube unwraps and texture consistency."""
from pathlib import Path
import base64,json,io
from PIL import Image
root=Path('src/main/resources/assets/zoldrax')
for stage in range(1,4):
 model=json.loads((root/f'models/blockbench/stage_{stage}.bbmodel').read_text())
 image=Image.open(root/f'textures/entity/stage_{stage}.png')
 embedded=Image.open(io.BytesIO(base64.b64decode(model['textures'][0]['source'].split(',')[1])))
 assert image.tobytes()==embedded.tobytes(), 'Blockbench and runtime textures differ'
 assert image.size==(model['resolution']['width'],model['resolution']['height'])
 occupied=[]
 for cube in model['elements']:
  w,h,d=[b-a for a,b in zip(cube['from'],cube['to'])]
  assert all(n>0 for n in (w,h,d))
  u,v=cube['uv_offset'];right,bottom=u+2*(w+d),v+h+d
  assert 0<=u<right<=image.width and 0<=v<bottom<=image.height, cube['name']
  for a,b,c,e in occupied:assert right<=a or c<=u or bottom<=b or e<=v, 'UV regions overlap'
  occupied.append((u,v,right,bottom))
  for face in cube['faces'].values():
   x1,y1,x2,y2=face['uv'];assert 0<=min(x1,x2)<=max(x1,x2)<=image.width and 0<=min(y1,y2)<=max(y1,y2)<=image.height
 print(f'Stage {stage}: {len(occupied)} cube unwraps valid; embedded/runtime PNG identical')
