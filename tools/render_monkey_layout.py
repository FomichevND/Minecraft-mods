"""Plot actual native generation evidence exported by MonkeyFollowerTests."""
import json,re
from pathlib import Path
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from matplotlib.patches import Rectangle
LABELS={'court_temple':'Монумент','valley_temple':'Монумент','herald_hall':'Зал глашатаев','gatherer_hall':'Зал добытчиков','storehouse':'Склад','kitchen':'Кухня','watch':'Вышка','festival':'Помост','market':'Рынок','farm':'Огород','garden':'Сад','rest':'Отдых'}
COLORS={'court_temple':'#e0b951','valley_temple':'#e0b951','farm':'#85a962','garden':'#5d986b','rest':'#b4b88a'}
fig,axes=plt.subplots(1,2,figsize=(15,8),facecolor='#f2f0e8')
for ax,name,title in zip(axes,['banana_court','great_appetite_valley'],['Банановый двор','Долина Великого Аппетита']):
 source=Path(f'build/gametest/{name}-generated.json')
 if not source.exists():source=Path(f'docs/{name}-1.9.0-generation.json')
 evidence=json.loads(source.read_text())
 Path(f'docs/{name}-1.9.0-generation.json').write_text(json.dumps(evidence,indent=2))
 anchor=evidence[0];polygons=[]
 for item in evidence:
  model=re.search(r'monkey/(\w+)',item['template'])[1];w,h,d=json.loads(Path(f'docs/monkey/{model}-layout.json').read_text())['size']
  def rotate(x,z):
   for _ in range({'NONE':0,'CLOCKWISE_90':1,'CLOCKWISE_180':2,'COUNTERCLOCKWISE_90':3}[item['rotation']]):x,z=-z,x
   return x+item['x']-anchor['x'],z+item['z']-anchor['z']
  corners=[rotate(x,z) for x,z in [(0,0),(w-1,0),(w-1,d-1),(0,d-1)]]
  lo=[min(p[i] for p in corners) for i in [0,1]];hi=[max(p[i] for p in corners) for i in [0,1]]
  ax.add_patch(Rectangle(lo,hi[0]-lo[0],hi[1]-lo[1],facecolor=COLORS.get(model,'#c39169'),edgecolor='#3d4e42',linewidth=1.5))
  ax.text((lo[0]+hi[0])/2,(lo[1]+hi[1])/2,LABELS[model]+f'\ny={item["y"]+10}',ha='center',va='center',fontsize=9,color='#23372c')
  polygons+=corners
 ax.set_xlim(min(p[0] for p in polygons)-4,max(p[0] for p in polygons)+4);ax.set_ylim(max(p[1] for p in polygons)+4,min(p[1] for p in polygons)-4)
 ax.set_aspect('equal');ax.set_facecolor('#dce4cf');ax.grid(alpha=.17);ax.set_title(title,fontsize=18,pad=18,color='#23372c');ax.set_xlabel('Блоки по X');ax.set_ylabel('Блоки по Z')
fig.suptitle('Свободные поселения · 1.9.0',fontsize=24,color='#23372c')
fig.text(.5,.025,'Фактические площадки из серверного теста генерации. y — уровень входа. Пространство между ними сохраняет природный рельеф.',ha='center',fontsize=10,color='#47564c')
fig.tight_layout(rect=[0,.05,1,.94]);fig.savefig('docs/monkey-settlements-1.9.0-layout.png',dpi=160)
