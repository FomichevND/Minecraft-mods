"""Source-mesh views, clearly separate from real Minecraft screenshots."""
from pathlib import Path
from PIL import Image,ImageDraw
import json
from render_decor_preview import render,W,H
root=Path('src/main/resources/assets/zoldrax/models/blockbench');out=Path('docs/monkey-decor/source');out.mkdir(parents=True,exist_ok=True)
for p in (root/'decor').glob('monkey_king_*.bbmodel'):
 m=json.loads(p.read_text());canvas=Image.new('RGB',(W*4,H))
 for i,(label,view) in enumerate([('FRONT',(.1,.2,1)),('SIDE',(1,.15,.03)),('BACK',(-.1,.2,-1)),('TOP',(0,1,.001))]):
  im=render(m,view);ImageDraw.Draw(im).text((10,10),p.stem+' / '+label,fill='white');canvas.paste(im,(i*W,0))
 canvas.save(out/(p.stem+'.png'))
paths=sorted((root/'food').glob('*.bbmodel'));sheet=Image.new('RGB',(W*7,H*6))
for i,p in enumerate(paths):
 im=render(json.loads(p.read_text()),(.5,.5,1));ImageDraw.Draw(im).text((10,10),p.stem,fill='white');sheet.paste(im,((i%7)*W,(i//7)*H))
sheet.save(out/'all-42-food-models.jpg',quality=90)
