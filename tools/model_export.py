from pathlib import Path
from PIL import Image
import json,random,base64,io,uuid
from texture_materials import paint,TEXEL_SCALE
OUT=Path('src/main/resources/assets/zoldrax')
SIZE=1024
def write_model(cubes,stage,prefix='',paint=paint,size=1024,texel_scale=TEXEL_SCALE):
    SIZE=size;TEXEL_SCALE=texel_scale
    img=Image.new('RGBA',(SIZE*TEXEL_SCALE,SIZE*TEXEL_SCALE),(0,0,0,255))
    rng=random.Random(840+stage);elements=[];u=v=row=2
    palette={'stone':(54,48,62),'wood':(101,65,36),'cloth':(146,126,103),'banner':(88,12,22),'metal':(37,32,40),'bone':(123,110,103),'ember':(232,41,16)}
    for idx,c in enumerate(cubes):
        x,y,z=c['from_'];xx,yy,zz=c['to'];w,h,d=[max(1,round(n)) for n in (xx-x,yy-y,zz-z)]
        uw=2*(w+d);uh=h+d
        if u+uw+2>SIZE:u=2;v+=row+3;row=0
        assert v+uh+2<SIZE
        uid=str(uuid.uuid5(uuid.NAMESPACE_URL,f'zoldrax/{prefix}stage{stage}/{c["name"]}'))
        faces={face:{'uv':uv,'texture':0} for face,uv in {
            'east':[u,v+d,u+d,v+d+h], 'north':[u+d,v+d,u+d+w,v+d+h],
            'west':[u+d+w,v+d,u+2*d+w,v+d+h], 'south':[u+2*d+w,v+d,u+2*d+2*w,v+d+h],
            'up':[u+d,v,u+d+w,v+d], 'down':[u+d+w,v,u+d+2*w,v+d]}.items()}
        for face,data in faces.items():
            a,b,A,B=data['uv']
            image=paint(c,face,(A-a)*TEXEL_SCALE,(B-b)*TEXEL_SCALE,cubes)
            img.paste(image,(a*TEXEL_SCALE,b*TEXEL_SCALE))
        elements.append({'name':c['name'],'type':'cube','uuid':uid,'from':c['from_'],'to':c['to'],'origin':[0,0,0],'rotation':[0,0,0],'uv_offset':[u,v],'box_uv':True,'faces':faces,'rescale':False,'locked':False,'visibility':True,'export':True})
        u+=uw+3;row=max(row,uh)
    stream=io.BytesIO();img.save(stream,format='PNG')
    texture='data:image/png;base64,'+base64.b64encode(stream.getvalue()).decode()
    model={'meta':{'format_version':'4.10','model_format':'modded_entity','box_uv':True},'name':f'{prefix or "Zoldrax "}stage {stage+1}','model_identifier':f'zoldrax_{prefix}stage_{stage+1}','visible_box':[7,9,0],'resolution':{'width':SIZE,'height':SIZE},'elements':elements,'outliner':[e['uuid'] for e in elements],'textures':[{'name':f'{prefix}stage_{stage+1}.png','uuid':str(uuid.uuid5(uuid.NAMESPACE_URL,f'zoldrax/{prefix}texture{stage}')),'id':'0','width':SIZE*TEXEL_SCALE,'height':SIZE*TEXEL_SCALE,'uv_width':SIZE,'uv_height':SIZE,'source':texture,'mode':'bitmap','saved':False}]}
    model_dir=OUT/'models/blockbench';tex_dir=OUT/'textures/entity';model_dir.mkdir(parents=True,exist_ok=True);tex_dir.mkdir(parents=True,exist_ok=True)
    (model_dir/f'{prefix}stage_{stage+1}.bbmodel').write_text(json.dumps(model,indent=2))
    img.save(tex_dir/f'{prefix}stage_{stage+1}.png')
    print(f'Stage {stage+1}: {len(elements)} cubes, atlas bounds y={v+row}/{SIZE}')
