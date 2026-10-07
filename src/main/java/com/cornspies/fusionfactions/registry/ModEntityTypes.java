package com.cornspies.fusionfactions.registry;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.supplydrop.FallingCrateEntity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntityTypes {
    public static final DeferredRegister.Entities ENTITY_TYPES = DeferredRegister.createEntities(FusionFactions.MODID);

    // Only spawned by supply drops, so not summonable.
    public static final DeferredHolder<EntityType<?>, EntityType<FallingCrateEntity>> FALLING_CRATE = ENTITY_TYPES.registerEntityType("falling_crate",
            FallingCrateEntity::new, MobCategory.MISC, builder -> builder
                    .noLootTable()
                    .sized(0.98F, 0.98F)
                    .clientTrackingRange(16)
                    .updateInterval(10)
                    .fireImmune()
                    .noSummon());

    private ModEntityTypes() {
    }
}
