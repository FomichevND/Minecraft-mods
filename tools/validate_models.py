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
 assert image.size==(model['textures'][0]['width'],model['textures'][0]['height'])
 assert image.width/model['resolution']['width']==image.height/model['resolution']['height']==2
 occupied=[]
 for cube in model['elements']:
  w,h,d=[b-a for a,b in zip(cube['from'],cube['to'])]
  assert all(n>0 for n in (w,h,d))
  u,v=cube['uv_offset'];right,bottom=u+2*(w+d),v+h+d
  assert 0<=u<right<=model['resolution']['width'] and 0<=v<bottom<=model['resolution']['height'], cube['name']
  for a,b,c,e in occupied:assert right<=a or c<=u or bottom<=b or e<=v, 'UV regions overlap'
  occupied.append((u,v,right,bottom))
  for face in cube['faces'].values():
   x1,y1,x2,y2=face['uv'];assert 0<=min(x1,x2)<=max(x1,x2)<=model['resolution']['width'] and 0<=min(y1,y2)<=max(y1,y2)<=model['resolution']['height']
 print(f'Stage {stage}: {len(occupied)} cube unwraps valid; embedded/runtime PNG identical')

# Full-size footprint and crown height are a public placement contract.
model=json.loads((root/'models/blockbench/stage_3.bbmodel').read_text())
lo=[min(e['from'][i] for e in model['elements']) for i in range(3)]
hi=[max(e['to'][i] for e in model['elements']) for i in range(3)]
assert lo==[-40,0,-24] and hi==[40,112,40], (lo,hi)
print('Completed model bounds verified: 5 x 4 blocks, 7 blocks high')

for stage in range(1,4):
 model=json.loads((root/f'models/blockbench/monkey_stage_{stage}.bbmodel').read_text())
 image=Image.open(root/f'textures/entity/monkey_stage_{stage}.png')
 embedded=Image.open(io.BytesIO(base64.b64decode(model['textures'][0]['source'].split(',')[1])))
 assert image.tobytes()==embedded.tobytes() and image.size==(2048,2048)
 rects=[]
 for e in model['elements']:
  w,h,d=[b-a for a,b in zip(e['from'],e['to'])];u,v=e['uv_offset'];U,V=u+2*(w+d),v+h+d
  assert 0<=u<U<=1024 and 0<=v<V<=1024
  for a,b,A,B in rects:assert U<=a or A<=u or V<=b or B<=v
  rects.append((u,v,U,V))
 lo=[min(e['from'][i] for e in model['elements']) for i in range(3)]
 hi=[max(e['to'][i] for e in model['elements']) for i in range(3)]
 assert lo[0]==-40 and hi[0]==56 and lo[2]==-40 and hi[2]==40 and hi[1]<=80
 if stage==3:assert hi[1]==80
 print(f'Monkey King stage {stage}: UVs/PNG consistent; bounds {lo} to {hi}')
banana=Image.open(root/'textures/item/golden_banana.png')
assert banana.size==(32,32) and banana.mode=='RGBA' and banana.getextrema()[3]==(0,255)
print('Golden banana icon validated')
