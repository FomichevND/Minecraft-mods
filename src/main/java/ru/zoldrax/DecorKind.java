package ru.zoldrax;

public enum DecorKind {
 THRONE("zoldrax_throne",2,3,0,0), BRAZIER("zoldrax_brazier",1,2,0,15),
 SKULL_LAMP("zoldrax_skull_lamp",1,2,0,12), CHAIN_LANTERN("zoldrax_chain_lantern",1,3,0,14),
 WEAPON_RACK("zoldrax_weapon_rack",2,3,3,0), CHEST("zoldrax_chest",1,2,27,0),
 LECTERN("zoldrax_lectern",1,2,1,0), OFFERING_PEDESTAL("zoldrax_offering_pedestal",1,2,1,0);
 public final String id;public final int width,height,slots,light;
 DecorKind(String id,int width,int height,int slots,int light){this.id=id;this.width=width;this.height=height;this.slots=slots;this.light=light;}
 public int height(int length){return this==CHAIN_LANTERN?length+2:height;}
 public int cells(int length){return width*height(length);}
 public int emitter(int length){return this==CHAIN_LANTERN?length:(this==BRAZIER||this==SKULL_LAMP?1:0);}
 public boolean hanging(){return this==CHAIN_LANTERN;}
}
