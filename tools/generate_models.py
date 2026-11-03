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
    # 80 x 64 x 112 model units: exactly 5 x 4 x 7 Minecraft blocks.
    box('basalt_plinth',-40,0,-24,80,3,64)
    for x in range(-40,40,8):
        for z in range(-24,40,8):box(f'foundation_{x}_{z}',x,3,z,8,4,8)
    box('upper_step',-37,7,-21,74,3,58)
    box('rear_pedestal',-32,10,-19,64,5,27)
    box('central_spine',-12,15,-18,24,46 if stage==0 else 58,18)
    # Ritual stairs lead into a low, open sacrificial basin.
    box('stair_lower',-20,7,33,40,3,7)
    box('stair_upper',-18,10,29,36,3,8)
    box('brazier_floor',-16,13,13,32,3,22)
    box('brazier_rim_left',-20,13,11,4,10,27)
    box('brazier_rim_right',16,13,11,4,10,27)
    box('brazier_rim_front',-16,13,34,32,7,4)
    box('brazier_back',-16,13,10,32,7,4)
    for side in [-1,1]:
        cx=side*27
        box(f'column_base_{side}',cx-8,10,-13,16,6,24)
        for y in range(16,44 if stage==0 else 65,8):
            box(f'carved_column_{side}_{y}',cx-6,y,-10,12,7,18)
        box(f'column_collar_{side}',cx-8,44 if stage==0 else 65,-13,16,5,24)
        if stage>0:
            box(f'armoured_shoulder_{side}',cx-9,70,-15,18,11,24)
            box(f'shoulder_inner_{side}',cx-8,81,-13,16,4,19)
            box(f'inner_support_{side}',side*17-3,19,-9,6,47,12)
    if stage==0:
        box('unfinished_skull',-12,61,-17,24,13,20)
        for side in [-1,1]:
            x=side*34-2
            box(f'wood_scaffold_front_{side}',x,7,15,4,73,4,'wood')
            box(f'wood_scaffold_back_{side}',x,7,-18,4,73,4,'wood')
            for y in [25,43,61,77]:
                box(f'wood_crossbar_{side}_{y}',x,y,-18,4,3,37,'wood')
        box('wood_overhead',-36,80,-15,72,3,6,'wood')
        box('wood_work_platform',-35,44,-17,70,3,15,'wood')
        for x in [-24,16]:box(f'torn_dust_cloth_{x}',x,12,19,8,23,1,'cloth')
        box('hanging_cloth',-9,35,2,18,26,1,'cloth')
    else:
        # Demonic mask: recessed mouth, angular brow, cheeks and hanging fangs.
        box('neck',-9,68,-12,18,10,15)
        box('skull_rear',-15,79,-18,30,20,21)
        box('forehead_crown',-11,96,-15,22,6,17)
        for side in [-1,1]:
            box(f'brow_outer_{side}',-16 if side<0 else 7,89,3,9,6,5)
            box(f'brow_inner_{side}',-9 if side<0 else 1,86,4,8,5,5)
            box(f'cheekbone_{side}',-15 if side<0 else 9,78,3,6,10,6)
            box(f'cheek_spike_{side}',-12 if side<0 else 8,73,4,4,8,4)
        box('nose_bridge',-3,81,4,6,11,7)
        box('nose_tip',-2,78,8,4,6,6)
        box('jaw',-9,72,0,18,5,9)
        box('chin',-5,67,0,10,6,7)
        for x in [-8,-4,2,6]:box(f'upper_fang_{x}',x,76,7,2,7,2,'bone')
        for x in [-6,4]:box(f'lower_fang_{x}',x,75,7,2,3,2,'bone')
        # Broad crescent horns: grow outwards, curl upward, then turn in.
        horn=[(14,91,10,8),(22,94,9,8),(29,99,9,7),(33,104,7,6),(30,109,8,3),(25,110,6,2)]
        for side in [-1,1]:
            for i,(xx,y,w,h) in enumerate(horn):
                if stage==1 and i>2:break
                box(f'great_horn_{side}_{i}',xx if side>0 else -xx-w,y,-13,w,h,13 if i<2 else 9)
            box(f'crown_outer_{side}',side*9-2,99,-10,4,8,5)
            box(f'banner_rod_{side}',side*27-8,65,12,16,3,3,'metal')
            box(f'banner_cloth_{side}',side*27-6,29,13,12,36,1,'banner')
            box(f'banner_tail_{side}',side*27-4,25,13,8,4,1,'banner')
        box('breastplate_outer',-14,38,0,28,29,5)
        box('breastplate_inlay',-11,41,5,22,23,2,'metal')
        if stage==2:
            box('central_crown_spike',-2,102,-9,4,10,5)
            for side in [-1,1]:
                # True open chain links alternating in depth and width.
                for i in range(8):
                    cx=side*35;y=25+i*5
                    if i%2==0:
                        box(f'chain_a_{side}_{i}',cx-2,y,6,1,5,1,'metal')
                        box(f'chain_b_{side}_{i}',cx+1,y,6,1,5,1,'metal')
                        box(f'chain_c_{side}_{i}',cx-1,y,6,2,1,1,'metal')
                        box(f'chain_d_{side}_{i}',cx-1,y+4,6,2,1,1,'metal')
                    else:
                        box(f'chain_depth_a_{side}_{i}',cx,y,4,1,5,1,'metal')
                        box(f'chain_depth_b_{side}_{i}',cx,y,7,1,5,1,'metal')
                        box(f'chain_depth_c_{side}_{i}',cx,y,5,1,1,2,'metal')
                        box(f'chain_depth_d_{side}_{i}',cx,y+4,5,1,1,2,'metal')
                box(f'glow_eye_outer_{side}',-12 if side<0 else 7,85,8.05,5,2,1,'ember')
                box(f'glow_eye_inner_{side}',-7 if side<0 else 2,83,8.05,5,2,1,'ember')
                for j in range(3):box(f'glow_cheek_vein_{side}_{j}',side*(10-j)-.5,77-j*3,9.05,1,3,1,'ember')
                box(f'glow_rune_arm_{side}',side*6-2,48,7.05,4,2,1,'ember')
                box(f'glow_rune_prong_{side}',side*8-1,49,7.05,2,6,1,'ember')
                for j in range(3):box(f'glow_foundation_crack_{side}_{j}',side*(20+j*3),3,39,3,1,1,'ember')
            box('glow_rune_stem',-1,42,7.05,2,19,1,'ember')
            box('glow_rune_crown',-4,58,7.05,8,2,1,'ember')
            box('glow_coals',-15,16,15,30,1,18,'ember')
    img=Image.new('RGBA',(SIZE,SIZE),(0,0,0,255));dr=ImageDraw.Draw(img)
    rng=random.Random(840+stage);elements=[];u=v=row=2
    palette={'stone':(54,48,62),'wood':(101,65,36),'cloth':(146,126,103),'banner':(88,12,22),'metal':(37,32,40),'bone':(123,110,103),'ember':(232,41,16)}
    for idx,c in enumerate(cubes):
        x,y,z=c['from_'];xx,yy,zz=c['to'];w,h,d=[max(1,round(n)) for n in (xx-x,yy-y,zz-z)]
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
    model={'meta':{'format_version':'4.10','model_format':'modded_entity','box_uv':True},'name':f'Zoldrax stage {stage+1}','model_identifier':f'zoldrax_stage_{stage+1}','visible_box':[7,9,0],'resolution':{'width':SIZE,'height':SIZE},'elements':elements,'outliner':[e['uuid'] for e in elements],'textures':[{'name':f'stage_{stage+1}.png','uuid':str(uuid.uuid5(uuid.NAMESPACE_URL,f'zoldrax/texture{stage}')),'id':'0','width':SIZE,'height':SIZE,'uv_width':SIZE,'uv_height':SIZE,'source':texture,'mode':'bitmap','saved':False}]}
    model_dir=OUT/'models/blockbench';tex_dir=OUT/'textures/entity';model_dir.mkdir(parents=True,exist_ok=True);tex_dir.mkdir(parents=True,exist_ok=True)
    (model_dir/f'stage_{stage+1}.bbmodel').write_text(json.dumps(model,indent=2))
    img.save(tex_dir/f'stage_{stage+1}.png')
    print(f'Stage {stage+1}: {len(elements)} cubes, atlas bounds y={v+row}/{SIZE}')
for stage in range(3):make(stage)
