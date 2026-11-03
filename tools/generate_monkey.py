"""Three editable 6x5x5 Blockbench stages and matching Monkey King atlases."""
from pathlib import Path
from PIL import Image,ImageDraw
import numpy as np
from texture_materials import paint as stone_paint
from model_export import write_model

def paint(cube,face,w,h,cubes):
    kind=cube['material']
    mapped={'gold':'metal','jade':'stone','fur':'stone','face':'stone','mouth':'stone','leaf':'stone','ruby':'ember'}
    if kind=='banner':
        cube['material']='cloth'
        image=np.asarray(stone_paint(cube,face,w,h,cubes)).astype(float)
        cube['material']=kind
        image=image/np.array([97,86,69])*np.array([80,15,28])
        u,v=np.meshgrid((np.arange(w)+.5)/w,(np.arange(h)+.5)/h)
        outer=((u-.5)/.35)**2+((v-.47)/.25)**2<1
        inner=((u-.46)/.36)**2+((v-.37)/.24)**2<1
        banana=outer&~inner
        image[banana]=[185,131,35]
        border=(abs(u-.06)<.015)|(abs(u-.94)<.015)|(abs(v-.04)<.015)|(abs(v-.96)<.015)
        image[border]=[140,95,28]
        return Image.fromarray(np.uint8(image.clip(0,255)))
    if kind not in mapped:return stone_paint(cube,face,w,h,cubes)
    cube['material']=mapped[kind]
    image=np.asarray(stone_paint(cube,face,w,h,cubes)).astype(float)
    cube['material']=kind
    targets={'gold':(174,117,26),'jade':(56,78,48),'fur':(86,70,55),'face':(143,119,88),'mouth':(30,15,13),'leaf':(39,85,27),'ruby':(200,28,14)}
    baseline={'stone':(39,35,43),'metal':(36,36,37),'ember':(170,32,8)}[mapped[kind]]
    image=image/np.array(baseline)*np.array(targets[kind])
    return Image.fromarray(np.uint8(image.clip(0,255)))

def make(stage):
    c=[]
    def box(n,x,y,z,w,h,d,m='stone'):c.append({'name':n,'from_':[x,y,z],'to':[x+w,y+h,z+d],'material':m})
    # Anchor spans x=-2..3 and z=-2..2, matching the block placement rule.
    box('jungle_plinth',-40,0,-40,96,4,80,'jade')
    for x in range(-40,56,12):
        for z in range(-40,40,10):box(f'foundation_{x}_{z}',x,4,z,12,4,10)
    box('throne_dais',-34,8,-34,84,4,64,'jade')
    box('front_step',-16,8,30,48,4,10)
    box('throne_back',-24,12,-31,64,stage==0 and 38 or 48,9,'jade')
    box('throne_seat',-21,12,-22,58,13,40)
    for side in [-1,1]:
        cx=8+side*33
        box(f'armrest_pillar_{side}',cx-7,12,-17,14,21,34)
        box(f'armrest_cap_{side}',cx-8,33,-19,16,4,38,'gold' if stage>0 else 'stone')
    if stage==0:
        box('rough_belly',-11,25,-20,38,23,28)
        box('rough_head',-9,50,-19,34,19,24)
        for side in [-1,1]:
            x=8+side*42
            box(f'wood_upright_{side}',x-2,8,-29,4,64,4,'wood')
            box(f'wood_upright_front_{side}',x-2,8,24,4,64,4,'wood')
            for y in [24,44,64]:box(f'wood_tie_{side}_{y}',x-2,y,-29,4,3,57,'wood')
        box('wood_overhead',-36,72,-26,88,3,6,'wood')
        box('dust_cloth',-4,29,9,24,21,1,'cloth')
    else:
        skin='fur' if stage==2 else 'stone'
        face='face' if stage==2 else 'stone'
        box('round_belly',-12,25,-19,40,27,31,skin)
        box('chest_patch',-5,31,12,26,19,3,face)
        for side in [-1,1]:
            x=8+side*22
            box(f'thigh_{side}',x-9,17,-1,18,13,26,skin)
            box(f'foot_{side}',x-9,12,19,18,9,13,face)
            for toe in range(3):box(f'toe_{side}_{toe}',x-8+toe*5,12,31,4,5,2,face)
            box(f'upper_arm_{side}',x-7,36,-14,14,19,20,skin)
            box(f'forearm_{side}',x-7,30,-1,14,11,23,skin)
            box(f'hand_{side}',x-8,26,18,16,10,12,face)
            for digit in range(3):box(f'finger_{side}_{digit}',x-7+digit*5,26,29,4,8,3,face)
            box(f'bracelet_{side}',x-8,34,10,16,4,5,'gold')
        box('neck',-3,49,-10,22,8,18,skin)
        box('head',-10,53,-21,36,22,29,skin)
        box('head_top',-6,74,-17,28,3,22,skin)
        for side in [-1,1]:
            cx=8+side*24
            box(f'ear_outer_{side}',cx-7,56,-9,14,16,11,skin)
            box(f'ear_inner_{side}',cx-4,59,1,8,10,2,face)
            box(f'eye_patch_{side}',8+side*8-6,64,7,12,9,3,face)
            box(f'eyebrow_{side}',8+side*8-5,71,10,10,2,2,'mouth')
            box(f'eye_{side}',8+side*8-2,67,10,4,3,2,'mouth')
        box('muzzle',-8,55,8,32,10,8,face)
        box('wide_smile',-4,55,16,24,7,1,'mouth')
        box('smile_top_teeth',-2,60,17,20,2,1,'bone')
        box('smile_lower_lip',-2,53,13,20,2,5,face)
        box('nose',3,62,13,10,4,6,skin)
        box('nostril_left',4,63,19,2,1,1,'mouth');box('nostril_right',10,63,19,2,1,1,'mouth')
        box('crown_band',-12,75,-19,40,3,26,'gold')
        for x in [-12,-3,8,19,25]:box(f'crown_spike_{x}',x,78,-17,3,2,6,'gold')
        box('crown_ruby',5,76,7,6,4,2,'ruby')
        box('collar',-10,49,-9,36,4,22,'gold')
        box('pendant',3,44,14,10,8,3,'gold')
        box('glow_pendant_gem' if stage==2 else 'pendant_gem',5,46,17,6,4,1,'ruby')
        box('belt',-13,26,-18,42,4,33,'gold')
        box('belt_buckle',4,25,15,8,6,3,'gold')
        for side in [-1,1]:
            x=8+side*35
            box(f'banner_rod_{side}',x-7,49,-28,14,2,2,'gold')
            box(f'banner_{side}',x-6,23,-25,12,26,1,'banner')
        # Curled tail behind the throne.
        for i,(xx,yy,zz,ww,hh,dd) in enumerate([(31,24,-33,6,5,10),(32,28,-38,6,12,6),(25,36,-39,12,5,6),(22,30,-39,5,8,6)]):box(f'tail_{i}',xx,yy,zz,ww,hh,dd,skin)
        if stage==2:
            for side in [-1,1]:
                x=8+side*37
                for j in range(7):
                    box(f'vine_{side}_{j}',x,10+j*6,-20,2,6,2,'leaf')
                    box(f'leaf_{side}_{j}',x-3+(j%2)*3,11+j*6,-18,5,3,2,'leaf')
                # Golden banana reliefs hang from both armrests.
                for i in range(4):box(f'banana_relief_{side}_{i}',8+side*33-6+i*3,21-abs(i-1)*2,20,4,3,2,'gold')
            box('royal_rug',-7,9,20,30,1,18,'banner')
    write_model(c,stage,'monkey_',paint)

if __name__=='__main__':
    for stage in range(3):make(stage)
    out=Path('src/main/resources/assets/zoldrax/textures/item');out.mkdir(parents=True,exist_ok=True)
    image=Image.new('RGBA',(32,32));d=ImageDraw.Draw(image)
    # Curved golden banana, dark contour, luminous peel, leaf and red royal seal.
    d.polygon([(3,11),(6,14),(10,18),(15,19),(20,17),(24,12),(25,5),(29,5),(29,15),(26,23),(20,28),(12,29),(6,25),(2,18)],fill='#53320e')
    d.polygon([(4,13),(9,19),(15,22),(21,20),(26,14),(27,7),(28,15),(24,23),(18,27),(11,27),(6,23),(3,17)],fill='#e8a91d')
    d.line([(5,16),(10,22),(16,24),(22,21),(26,16)],fill='#fff08a',width=2)
    d.line([(7,24),(13,28),(20,26),(25,22)],fill='#ae6512',width=1)
    d.polygon([(24,5),(24,2),(28,1),(30,3),(28,6)],fill='#b98416')
    d.polygon([(22,5),(18,2),(17,5),(21,7)],fill='#5b8b27')
    d.rectangle((14,23,17,26),fill='#87430d');d.rectangle((15,24,16,25),fill='#e4421b')
    d.point((9,8),fill='#fff3a2');d.line((7,8,11,8),fill='#fff3a2');d.line((9,6,9,10),fill='#fff3a2')
    image.save(out/'golden_banana.png')
    icon=Image.new('RGBA',(32,32));d=ImageDraw.Draw(icon)
    d.rectangle((5,8,26,25),fill='#65533e');d.rectangle((10,14,22,23),fill='#b6986e');d.rectangle((8,5,24,9),fill='#d7a222');d.rectangle((9,2,12,6),fill='#f2cc43');d.rectangle((21,2,24,6),fill='#f2cc43');d.rectangle((15,1,18,6),fill='#f2cc43');d.rectangle((13,15,15,17),fill='#2a1b16');d.rectangle((20,15,22,17),fill='#2a1b16');d.rectangle((13,20,23,22),fill='#40211b');d.rectangle((6,26,26,29),fill='#7a786a')
    icon.save(out/'monkey_monument.png')
