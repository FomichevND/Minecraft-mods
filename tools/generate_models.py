"""Generate editable Blockbench models and their matching, pixel-density-correct atlases.
Run from repository root with Python 3 + Pillow. Runtime reads these same .bbmodel files.
"""
from pathlib import Path
from PIL import Image, ImageDraw
import json, random, base64, io, uuid
OUT=Path('src/main/resources/assets/zoldrax')
SIZE=1024

def make(stage):
    cubes=[]
    def box(name,x,y,z,w,h,d,material='stone'):
        cubes.append(dict(name=name,from_=[x,y,z],to=[x+w,y+h,z+d],material=material))
    # Broad stepped plinth, with individual stones rather than a stretched slab.
    box('plinth',-24,0,-24,48,3,48)
    for x in range(-24,24,8):
        for z in range(-24,24,8):box(f'foundation_{x}_{z}',x,3,z,8,4,8)
    box('upper_step',-21,7,-21,42,3,42)
    box('spine',-10,10,-17,20,40 if stage==0 else 53,13)
    for side in [-1,1]:
        x=-21 if side<0 else 13
        box(f'column_foot_{side}',x-1,10,-12,10,5,15)
        for y in range(15,39 if stage==0 else 55,8):box(f'column_{side}_{y}',x,y,-10,8,7,11)
        if stage>0:
            box(f'column_cap_{side}',x-2,54,-13,12,6,17)
            box(f'shoulder_{side}',x-1,60,-15,11,9,19)
    # Brazier is part of the model, with a hollow opening.
    box('brazier_floor',-12,10,6,24,3,16)
    box('brazier_front',-14,10,21,28,6,3)
    box('brazier_rim_left',-15,10,6,3,9,18)
    box('brazier_rim_right',12,10,6,3,9,18)
    box('brazier_back',-12,10,5,24,5,3)
    if stage==0:
        box('unfinished_head',-9,50,-16,18,13,16)
        for side in [-1,1]:
            x=side*22-1
            box(f'wood_scaffold_{side}',x,7,-3,2,56,2,'wood')
            for y in [20,36,52]:box(f'wood_brace_{side}_{y}',min(x,0),y,-3,abs(x)+2,2,2,'wood')
        box('wood_crossbeam',-23,63,-3,46,2,3,'wood')
        box('hanging_cloth',-5,22,-3,10,24,1,'cloth')
    else:
        box('neck',-7,61,-11,14,8,11)
        box('skull',-11,69,-15,22,15,15)
        box('brow_left',-12,78,-1,11,4,4)
        box('brow_right',1,78,-1,11,4,4)
        box('cheek_left',-11,69,0,5,9,4)
        box('cheek_right',6,69,0,5,9,4)
        box('nose',-2,70,0,4,8,6)
        box('jaw',-7,65,-4,14,4,9)
        for x in [-6,-2,2,6]:box(f'fang_{x}',x-1,68,4,2,4,2,'bone')
        for side in [-1,1]:
            # Stepped outward horns sweep up and back toward the head.
            for i,(xx,y,w,h) in enumerate([(11,78,8,7),(18,81,7,8),(23,86,6,8),(23,93,5,6),(19,97,6,3)]):
                if stage==1 and i>2:break
                box(f'horn_{side}_{i}',xx if side>0 else -xx-w,y,-11,w,h,8 if i<2 else 5)
            box(f'banner_rod_{side}',side*17-5,52,5,10,2,2,'metal')
            box(f'banner_{side}',side*17-4,23,6,8,29,1,'banner')
        box('chest_plate',-9,38,-3,18,23,3)
        if stage==2:
            box('crown',-2,84,-10,4,10,5)
            for side in [-1,1]:
                for i in range(7):
                    box(f'chain_{side}_{i}',side*24-1,23+i*5,0,2,4,2,'metal')
                box(f'glow_eye_{side}',-8 if side<0 else 3,75,3.05,5,2,1,'ember')
            box('glow_rune_stem',-1,39,.1,2,18,1,'ember')
            box('glow_rune_cross',-6,45,.1,12,2,1,'ember')
            box('glow_coals',-11,13,9,22,1,12,'ember')
    img=Image.new('RGBA',(SIZE,SIZE),(0,0,0,255));dr=ImageDraw.Draw(img)
    rng=random.Random(840+stage);elements=[];u=v=row=2
    palette={'stone':(43,39,51),'wood':(101,65,36),'cloth':(146,126,103),'banner':(88,12,22),'metal':(40,40,46),'bone':(123,110,103),'ember':(232,41,16)}
    for idx,c in enumerate(cubes):
        x,y,z=c['from_'];xx,yy,zz=c['to'];w,h,d=int(xx-x),int(yy-y),int(zz-z)
        uw=2*(w+d);uh=h+d
        if u+uw+2>SIZE:u=2;v+=row+3;row=0
        assert v+uh+2<SIZE
        col=palette[c['material']]
        for py in range(uh):
            for px in range(uw):
                noise=rng.randint(-6,6);color=tuple(max(0,min(255,k+noise)) for k in col)
                if c['material']=='stone':
                    if py%8==0 or (px+(8 if (py//8)%2 else 0))%16==0:color=(24,21,31)
                    elif py%8==1:color=tuple(k+9 for k in col)
                elif c['material']=='wood' and px%4==0:color=(66,41,24)
                elif c['material']=='banner':
                    if px%8 in (0,7):color=(166,57,36)
                    if (px%8 in (3,4) and py%29 in range(5,24)) or (py%29 in (12,13) and px%8 in range(1,7)):color=(210,55,30)
                elif c['material']=='ember' and rng.random()<.12:color=(255,151,41)
                img.putpixel((u+px,v+py),(*color,255))
        uid=str(uuid.uuid5(uuid.NAMESPACE_URL,f'zoldrax/stage{stage}/{c["name"]}'))
        faces={face:{'uv':uv,'texture':0} for face,uv in {
            'east':[u,v+d,u+d,v+d+h], 'north':[u+d,v+d,u+d+w,v+d+h],
            'west':[u+d+w,v+d,u+2*d+w,v+d+h], 'south':[u+2*d+w,v+d,u+2*d+2*w,v+d+h],
            'up':[u+d,v,u+d+w,v+d], 'down':[u+d+w,v,u+d+2*w,v+d]}.items()}
        elements.append({'name':c['name'],'type':'cube','uuid':uid,'from':c['from_'],'to':c['to'],'origin':[0,0,0],'rotation':[0,0,0],'uv_offset':[u,v],'box_uv':True,'faces':faces,'rescale':False,'locked':False,'visibility':True,'export':True})
        u+=uw+3;row=max(row,uh)
    stream=io.BytesIO();img.save(stream,format='PNG')
    texture='data:image/png;base64,'+base64.b64encode(stream.getvalue()).decode()
    model={'meta':{'format_version':'4.10','model_format':'modded_entity','box_uv':True},'name':f'Zoldrax stage {stage+1}','model_identifier':f'zoldrax_stage_{stage+1}','visible_box':[7,8,0],'resolution':{'width':SIZE,'height':SIZE},'elements':elements,'outliner':[e['uuid'] for e in elements],'textures':[{'name':f'stage_{stage+1}.png','uuid':str(uuid.uuid5(uuid.NAMESPACE_URL,f'zoldrax/texture{stage}')),'id':'0','width':SIZE,'height':SIZE,'uv_width':SIZE,'uv_height':SIZE,'source':texture,'mode':'bitmap','saved':False}]}
    model_dir=OUT/'models/blockbench';tex_dir=OUT/'textures/entity';model_dir.mkdir(parents=True,exist_ok=True);tex_dir.mkdir(parents=True,exist_ok=True)
    (model_dir/f'stage_{stage+1}.bbmodel').write_text(json.dumps(model,indent=2))
    img.save(tex_dir/f'stage_{stage+1}.png')
    print(f'Stage {stage+1}: {len(elements)} cubes, atlas bounds y={v+row}/{SIZE}')
for stage in range(3):make(stage)
