"""Deterministic material painting: chipped volcanic stone, crevice AO and weathering.
All shading is baked in model coordinates, so it cannot change with camera pitch.
"""
import hashlib
import numpy as np
from PIL import Image,ImageDraw,ImageFilter
TEXEL_SCALE=2

def noise(rng,w,h,cell):
    image=Image.fromarray((rng.random((max(2,h//cell+2),max(2,w//cell+2)))*255).astype('uint8'))
    return np.asarray(image.resize((w,h),Image.Resampling.BILINEAR),dtype=float)/255-.5

def paint(cube,face,w,h,cubes):
    name=cube['name'];material=cube['material']
    seed=int.from_bytes(hashlib.sha256((name+'/'+face).encode()).digest()[:8],'big')
    rng=np.random.default_rng(seed)
    x,y,z=cube['from_'];X,Y,Z=cube['to']
    uu,vv=np.meshgrid((np.arange(w)+.5)/w,(np.arange(h)+.5)/h)
    if face=='south':px=x+uu*(X-x);py=Y-vv*(Y-y);pz=np.full_like(px,Z+.2)
    elif face=='north':px=X-uu*(X-x);py=Y-vv*(Y-y);pz=np.full_like(px,z-.2)
    elif face=='east':pz=Z-uu*(Z-z);py=Y-vv*(Y-y);px=np.full_like(pz,X+.2)
    elif face=='west':pz=z+uu*(Z-z);py=Y-vv*(Y-y);px=np.full_like(pz,x-.2)
    elif face=='up':px=x+uu*(X-x);pz=z+vv*(Z-z);py=np.full_like(px,Y+.2)
    else:px=x+uu*(X-x);pz=Z-vv*(Z-z);py=np.full_like(px,y-.2)
    fine=rng.random((h,w))-.5
    grain=noise(rng,w,h,3);cloud=noise(rng,w,h,12);broad=noise(rng,w,h,28)
    # A controlled palette close to Minecraft blackstone, without the purple cast.
    base=np.array({'stone':(30,30,33),'metal':(36,36,37),'wood':(74,48,31),'cloth':(97,86,69),
      'banner':(61,10,18),'bone':(107,97,78),'ember':(116,19,9)}[material],float)
    style=''
    if material=='stone':
        if any(t in name for t in ['skull','brow','cheek','nose','chin','jaw','crown','neck']):base=np.array([35,34,35.]);style='carved'
        elif 'horn' in name:base=np.array([25,26,29.]);style='horn'
        elif 'column' in name or 'support' in name:base=np.array([32,32,34.]);style='pillar'
        elif 'brazier' in name:base=np.array([24,23,25.]);style='scorched'
        elif 'shoulder' in name or 'breastplate' in name:base=np.array([29,28,31.]);style='armour'
        else:style='masonry'
    variation=fine*5+grain*13+cloud*15+broad*15
    rgb=base[None,None,:]+variation[...,None]
    # Fine mineral veins, pits and irregular volcanic speckles.
    veins=np.abs(grain*.75+cloud*.65+broad*.3)<.014
    if material=='stone':
        rgb+=veins[...,None]*np.array([4,4,5])
        rgb-=((rng.random((h,w))<.035)&(grain<0))[...,None]*9
        if style=='masonry':
            # Thin irregular courses on broad masonry only, never across the mask.
            if face in ['north','south','east','west']:
                courses=(py+cloud*1.2)%12
                joints=((px if face in ['north','south'] else pz)+((py//12)%2)*11+cloud*1.5)%24
                seam=(courses<.35)|(joints<.3)
                rgb-=seam[...,None]*10
        if style=='horn':
            grooves=(np.sin(py*2.2+cloud*1.4+px*.25)*.5+.5)**8
            rgb-=grooves[...,None]*9
        if style=='pillar':
            groove=(np.cos((px if face in ['north','south'] else pz)*1.05)>.88)
            rgb-=groove[...,None]*4
    # Edge wear: narrow battered edges, rather than bright outlines around every brick.
    edge=np.minimum.reduce([uu*w,(1-uu)*w,vv*h,(1-vv)*h])/TEXEL_SCALE
    edge_dark=np.exp(-edge/1.0)
    chipped=(edge<1.6)&(grain+cloud*.4>.04)
    if material in ['stone','metal','bone']:
        rgb-=edge_dark[...,None]*7
        rgb+=chipped[...,None]*np.array([18,17,15])
    elif material=='wood':
        streak=np.sin(px*5+grain*3)*5
        rgb+=streak[...,None];rgb-=edge_dark[...,None]*12
    elif material in ['banner','cloth']:
        # Cloth weave and broad vertical folds, deliberately desaturated.
        folds=np.cos(uu*16+cloud)*7
        weave=((np.indices((h,w)).sum(axis=0)%2)*2-1)*1.8
        rgb+=(folds+weave)[...,None];rgb-=edge_dark[...,None]*10
        if material=='banner':
            stem=(abs(uu-.5)<.065)&(vv>.16)&(vv<.85)
            cross=(abs(vv-.43)<.035)&(uu>.17)&(uu<.83)
            prongs=((abs(uu-.22)<.045)|(abs(uu-.78)<.045))&(vv>.22)&(vv<.44)
            sigil=stem|cross|prongs
            rgb[sigil]=np.array([137,59,38])+variation[sigil,None]*.8
            # Worn gold thread and fraying; symbols are dyed, not emissive.
            stitch=(abs(uu-.08)<.015)|(abs(uu-.92)<.015)
            rgb[stitch]=np.array([87,57,35])+fine[stitch,None]*8
            rgb*=1-np.maximum(0,vv-.7)[...,None]*.55
    elif material=='metal':
        # Hammered iron with scratched highlights and subdued rust in recesses.
        rgb+=grain[...,None]*12
        rust=(cloud+grain*.5>.19)
        rgb[rust]=rgb[rust]*.7+np.array([52,27,14])*.3
        if 'breastplate' in name:
            rgb-=((edge<2.2)&(edge>1.2))[...,None]*11
            rgb+=((edge<3)&(edge>2.2))[...,None]*7
    elif material=='bone':
        rgb+=np.sin(py*2+grain*2)[...,None]*6
        rgb-=np.exp(-edge/2)[...,None]*13
    elif material=='ember':
        if 'coals' in name:
            # Dark crust islands divided by red-hot fissures, not a flat lava tile.
            hot=(np.abs(grain+cloud*.7)<.065)
            rgb=np.broadcast_to([26.,10.,8.],(h,w,3)).copy()+variation[...,None]*.35
            rgb[hot]=np.array([213,52,12])+np.maximum(0,fine[hot,None])*np.array([30,90,25])
        else:
            core=(edge>.6)
            rgb=np.broadcast_to([120.,12.,5.],(h,w,3)).copy()
            rgb[core]=np.array([223,60,14])+variation[core,None]*.7
        return Image.fromarray(np.uint8(rgb.clip(0,255)))
    # Baked crevice/contact shadows from the actual neighbouring model pieces.
    distance=np.full((h,w),5.)
    for other in cubes:
        if other is cube or other['name'].startswith('glow_'):continue
        lo=other['from_'];hi=other['to']
        if X+5<lo[0] or hi[0]+5<x or Y+5<lo[1] or hi[1]+5<y or Z+5<lo[2] or hi[2]+5<z:continue
        dx=np.maximum(np.maximum(lo[0]-px,px-hi[0]),0)
        dy=np.maximum(np.maximum(lo[1]-py,py-hi[1]),0)
        dz=np.maximum(np.maximum(lo[2]-pz,pz-hi[2]),0)
        distance=np.minimum(distance,np.sqrt(dx*dx+dy*dy+dz*dz))
    ao=.52+.48*np.minimum(distance/3.2,1)
    orientation={'up':1.15,'down':.48,'south':1.,'north':.70,'east':.83,'west':.78}[face]
    rgb*=ao[...,None]*orientation
    if material in ['stone','metal']:
        soot=np.exp(-(((px/21)**2)+((py-19)/13)**2+((pz-25)/18)**2))
        rgb*=1-.3*soot[...,None]
        # Damp/soil staining gathers around the lowest courses.
        rgb*=1-(np.maximum(0,9-py)/9*.18)[...,None]
    image=Image.fromarray(np.uint8(rgb.clip(0,255)))
    dr=ImageDraw.Draw(image)
    # Sparse fractures and abrasions, seeded per piece and per face.
    if material in ['stone','bone'] and w>=8 and h>=8:
        for _ in range(max(1,w*h//2200)):
            cx=int(rng.integers(1,w-1));cy=int(rng.integers(1,h-1));pts=[(cx,cy)]
            for i in range(int(rng.integers(3,9))):
                cx+=int(rng.integers(-2,3));cy+=int(rng.integers(1,4));pts.append((cx,cy))
            dr.line([(a+1,b) for a,b in pts],fill=(45,43,42),width=1)
            dr.line(pts,fill=(10,10,12),width=1)
    if material=='metal' and w>=4 and h>=4:
        for _ in range(max(1,w*h//900)):
            a,b=int(rng.integers(w)),int(rng.integers(h))
            dr.line([(a,b),(a+int(rng.integers(2,7)),b+1)],fill=(68,65,59),width=1)
    return image
