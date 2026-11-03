package ru.zoldrax;

import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.*;
import net.minecraft.util.Identifier;

/** Ambient-lit opaque model layer, deliberately without entity diffuse lights. */
public final class MonumentRenderLayer extends RenderLayer {
 public static net.minecraft.client.gl.ShaderProgram shader;
 private static final java.util.Map<Identifier,RenderLayer> LAYERS=new java.util.HashMap<>();
 private MonumentRenderLayer() {
  super("zoldrax_unused",VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,VertexFormat.DrawMode.QUADS,256,false,false,()->{},()->{});
 }
 public static RenderLayer get(Identifier texture) { return LAYERS.computeIfAbsent(texture,MonumentRenderLayer::create); }
 private static RenderLayer create(Identifier texture) {
  return RenderLayer.of("zoldrax_monument",VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
   VertexFormat.DrawMode.QUADS,256,false,false,MultiPhaseParameters.builder()
    .program(new RenderPhase.ShaderProgram(()->shader))
    .texture(new RenderPhase.Texture(texture,false,false))
    .transparency(NO_TRANSPARENCY).cull(ENABLE_CULLING)
    .lightmap(DISABLE_LIGHTMAP).overlay(ENABLE_OVERLAY_COLOR).build(false));
 }
}
