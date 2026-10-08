package com.cornspies.fusionfactions.registry;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.missile.MissileEntity;
import com.cornspies.fusionfactions.supplydrop.FallingCrateEntity;
import com.cornspies.fusionfactions.weapon.BulletEntity;

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

    public static final DeferredHolder<EntityType<?>, EntityType<BulletEntity>> BULLET = ENTITY_TYPES.registerEntityType("bullet",
            BulletEntity::new, MobCategory.MISC, builder -> builder
                    .noLootTable()
                    .sized(0.125F, 0.125F)
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .noSummon());

    // Only spawned by missile flights (DESIGN.md §6.5). Tracked from far away, since it's meant to be seen.
    public static final DeferredHolder<EntityType<?>, EntityType<MissileEntity>> MISSILE = ENTITY_TYPES.registerEntityType("missile",
            MissileEntity::new, MobCategory.MISC, builder -> builder
                    .noLootTable()
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(16)
                    .updateInterval(5)
                    .fireImmune()
                    .noSummon());

    private ModEntityTypes() {
    }
}
