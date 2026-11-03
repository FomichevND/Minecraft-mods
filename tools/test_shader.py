"""Compile the shipped shader on EGL OpenGL and compare pixels at different camera pitches.
Requires moderngl, numpy, and the Minecraft 1.20.1 client jar as the first argument.
"""
from pathlib import Path
import sys,zipfile,numpy as np,moderngl
shader=Path('src/main/resources/assets/zoldrax/shaders/core')
with zipfile.ZipFile(sys.argv[1]) as z:
 def preprocess(s):
  import re
  return re.sub(r'#moj_import <([^>]+)>',lambda m:re.sub(r'^#version.*$', '', z.read('assets/minecraft/shaders/include/'+m[1]).decode(), flags=re.M),s)
 vertex=preprocess((shader/'monument.vsh').read_text());fragment=preprocess((shader/'monument.fsh').read_text())
ctx=moderngl.create_standalone_context(backend='egl',require=330)
p=ctx.program(vertex_shader=vertex,fragment_shader=fragment)
fbo=ctx.simple_framebuffer((32,32));fbo.use()
white=ctx.texture((1,1),4,bytes([255,255,255,255]));white.use(0)
overlay=ctx.texture((1,1),4,bytes([0,0,0,255]));overlay.use(1)
p['Sampler0']=0;p['Sampler1']=1
p['ProjMat'].write(np.eye(4,dtype='f4').tobytes())
p['ColorModulator']=(1,1,1,1);p['FogStart']=100.;p['FogEnd']=200.
results=[]
quad=np.array([[-.8,-.8,0],[-.8,.8,0],[.8,-.8,0],[.8,.8,0]],dtype='f4')
for angle in [-70,0,70]:
 a=np.deg2rad(angle);c,s=np.cos(a),np.sin(a)
 view=np.array([[1,0,0,0],[0,c,-s,0],[0,s,c,0],[0,0,0,1]],dtype='f4')
 p['ModelViewMat'].write(view.T.tobytes())
 vertices=quad@view[:3,:3]
 bindings=[(ctx.buffer(vertices.astype('f4').tobytes()),'3f','Position'),
 (ctx.buffer(np.tile([.65,.65,.65,1],(4,1)).astype('f4').tobytes()),'4f','Color'),
 (ctx.buffer(np.zeros((4,2),dtype='f4').tobytes()),'2f','UV0'),
 (ctx.buffer(np.zeros((4,2),dtype='i4').tobytes()),'2i','UV1')]
 vao=ctx.vertex_array(p,bindings);fbo.clear();vao.render(mode=moderngl.TRIANGLE_STRIP)
 pixels=np.frombuffer(fbo.read(components=3),dtype='u1').reshape(32,32,3);results.append(pixels[12:20,12:20].copy())
assert np.array_equal(results[0],results[1]) and np.array_equal(results[1],results[2]),'Camera pitch changes shading'
assert np.all(results[1]>100),'Quad did not render'
print('Shader compiled on',ctx.info['GL_RENDERER'])
print('Identical surface colour at camera pitch -70, 0, +70 degrees:',results[1][0,0].tolist())
