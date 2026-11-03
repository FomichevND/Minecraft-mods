"""Annotated top-down diagram from the actual generated template blocks (not a game screenshot)."""
import json
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
FONT='/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
def font(n):return ImageFont.truetype(FONT,n)
image=Image.new('RGB',(1260,880),'#16161d');draw=ImageDraw.Draw(image)
draw.text((30,18),'Святилища Золдракса — схема построек',font=font(28),fill='#f1d8c3')
draw.text((30,58),'Вид сверху по блокам шаблонов. Статуя показана условным контуром.',font=font(17),fill='#b1a8ac')
for i,(name,title,altar,features) in enumerate([
 ('blood_shrine','Кровавое капище',(15,9),[(1,(7,22),'Шатёр жреца'),(2,(25,9),'Склад драгоценностей'),(3,(25,22),'Клетки'),(4,(6,8),'Дозорный пост')]),
 ('chain_camp','Лагерь цепных карателей',(15,7),[(1,(7,23),'Казарма'),(2,(25,9),'Кузня цепей'),(3,(25,24),'Склад припасов'),(4,(6,6),'Клетка'),(5,(5,13),'Сторожевая башня')])]):
 x0=30+i*630;y0=160;scale=17
 data=json.loads(Path(f'docs/{name}-layout.json').read_text());tops={}
 for b in data['blocks']:
  x,y,z=b['pos'];key=(x,z)
  if key not in tops or y>tops[key][0]:tops[key]=(y,b['state']['Name'].split(':')[-1])
 draw.text((x0,112),title,font=font(23),fill='#ecd5bf')
 for (x,z),(y,n) in tops.items():
  color='#35333c'
  if 'red_wool' in n or 'banner' in n:color='#802b37'
  elif 'dark_oak' in n:color='#604436'
  elif 'iron' in n or 'chain' in n:color='#85828a'
  elif 'campfire' in n or 'lantern' in n:color='#ffac38'
  elif 'chest' in n or 'barrel' in n:color='#b37a3a'
  elif 'basalt' in n:color='#272631'
  elif 'stairs' in n or 'blackstone_slab' in n:color='#655b6a'
  draw.rectangle((x0+x*scale,y0+z*scale,x0+(x+1)*scale-1,y0+(z+1)*scale-1),fill=color)
 ax,az=altar
 draw.rectangle((x0+(ax-2)*scale,y0+(az-1)*scale,x0+(ax+3)*scale,y0+(az+3)*scale),fill='#422832',outline='#ff5641',width=3)
 draw.text((x0+(ax-1)*scale,y0+az*scale),'А',font=font(22),fill='#ffcf75')
 for no,(x,z),label in features:
  px=x0+x*scale+8;py=y0+z*scale+8
  draw.ellipse((px-12,py-12,px+12,py+12),fill='#e1c6a0',outline='#15131b',width=2)
  draw.text((px-6,py-11),str(no),font=font(17),fill='#201b20')
 for j,(no,_,label) in enumerate(features):draw.text((x0,710+j*25),f'{no}. {label}',font=font(17),fill='#d6ccd0')
 draw.text((x0,685),'А — готовый алтарь и жаровня',font=font(17),fill='#ffb06b')
 draw.text((x0,842),'3 жреца + 2 карателя' if i==0 else '2 жреца + 3 карателя',font=font(18),fill='#e4ad86')
image.save('docs/cult-sites-layout.png')
