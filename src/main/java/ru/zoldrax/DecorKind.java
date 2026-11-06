package ru.zoldrax;

public enum DecorKind {
 THRONE("zoldrax_throne",2,3,0,0), BRAZIER("zoldrax_brazier",1,2,0,15),
 SKULL_LAMP("zoldrax_skull_lamp",1,2,0,12), CHAIN_LANTERN("zoldrax_chain_lantern",1,3,0,14),
 WEAPON_RACK("zoldrax_weapon_rack",2,3,3,0), CHEST("zoldrax_chest",1,2,27,0),
 LECTERN("zoldrax_lectern",1,2,1,0), OFFERING_PEDESTAL("zoldrax_offering_pedestal",1,2,1,0),
 FOOD_SHELF("monkey_king_food_shelf",3,2,6,0), PREPARATION_TABLE("monkey_king_preparation_table",3,2,4,0),
 SUPPLY_BASKET("monkey_king_supply_basket",1,1,9,0), DRYING_RACK("monkey_king_drying_rack",3,2,5,0),
 COURTYARD_GONG("monkey_king_courtyard_gong",3,3,0,0), FESTIVAL_BENCH("monkey_king_festival_bench",3,2,0,0),
 NOTICE_BOARD("monkey_king_notice_board",3,3,3,0), FESTIVAL_DRUM("monkey_king_festival_drum",2,2,0,0),
 GARDEN_CANOPY("ternor_garden_canopy",3,3,6,12), STORAGE_SHELF("ternor_storage_shelf",3,3,27,0),
 ANIMAL_FEEDER("ternor_animal_feeder",2,1,9,0), ANIMAL_TROUGH("ternor_animal_trough",2,2,0,12),
 STONE_FORGE("ternor_stone_forge",3,3,3,13), BELLOWS("ternor_forge_bellows",2,2,0,0), QUENCHING_TUB("ternor_quenching_tub",1,1,0,0);
 public final String id;public final int width,height,slots,light;
 DecorKind(String id,int width,int height,int slots,int light){this.id=id;this.width=width;this.height=height;this.slots=slots;this.light=light;}
 public int height(int length){return this==CHAIN_LANTERN?length+2:height;}
 public int cells(int length){return width*depth()*height(length);}
 public int emitter(int length){return this==CHAIN_LANTERN?length:(this==BRAZIER||this==SKULL_LAMP?1:0);}
 public boolean monkey(){return ordinal()>=8&&ordinal()<16;}
 public boolean ternor(){return ordinal()>=16;}
 public int depth(){return this==GARDEN_CANOPY?3:this==FESTIVAL_DRUM||this==STONE_FORGE?2:1;}
 public boolean foodDisplay(){return this==FOOD_SHELF||this==PREPARATION_TABLE||this==SUPPLY_BASKET||this==DRYING_RACK;}
 public boolean hanging(){return this==CHAIN_LANTERN;}
}
