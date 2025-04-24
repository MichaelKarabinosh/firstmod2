package net.dannykandmichaelk.firstmod.entity;

import net.dannykandmichaelk.firstmod.FirstMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import javax.swing.text.html.parser.Entity;

public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, FirstMod.MOD_ID);

    public static final RegistryObject<EntityType<MrDasEntity>> MRDAS =
            ENTITY_TYPES.register("Mr Das", () -> EntityType.Builder.of(MrDasEntity::new, MobCategory.CREATURE).sized(1.5f,15f).build("Mr Das"));

    public static void register(IEventBus eventBus){
        ENTITY_TYPES.register(eventBus);
    }
}
