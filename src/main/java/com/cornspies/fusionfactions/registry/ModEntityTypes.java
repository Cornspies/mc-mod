package com.cornspies.fusionfactions.registry;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.missile.InterceptorRocketEntity;
import com.cornspies.fusionfactions.missile.MissileEntity;
import com.cornspies.fusionfactions.supplydrop.FallingCrateEntity;
import com.cornspies.fusionfactions.weapon.BulletEntity;
import com.cornspies.fusionfactions.weapon.GuidedRocketEntity;

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

    // Only spawned by missile flights (DESIGN.md §6.5). Tracked from far away, since it's meant to be seen. As big
    // as the drawn missile, so bullets hit where it is drawn.
    public static final DeferredHolder<EntityType<?>, EntityType<MissileEntity>> MISSILE = ENTITY_TYPES.registerEntityType("missile",
            MissileEntity::new, MobCategory.MISC, builder -> builder
                    .noLootTable()
                    .sized(0.8F, 5.4F)
                    .clientTrackingRange(16)
                    .updateInterval(5)
                    .fireImmune()
                    .noSummon());

    // An interceptor's homing shot (DESIGN.md §6.9). Drawn as its smoke trail only.
    // The rocket launcher's rocket (DESIGN.md §6b.6). Updated every tick so its turns look smooth; tracked from far,
    // since seeing it come is the counterplay.
    public static final DeferredHolder<EntityType<?>, EntityType<GuidedRocketEntity>> GUIDED_ROCKET = ENTITY_TYPES.registerEntityType("guided_rocket",
            GuidedRocketEntity::new, MobCategory.MISC, builder -> builder
                    .noLootTable()
                    .sized(0.3F, 0.3F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .fireImmune()
                    .noSummon());

    public static final DeferredHolder<EntityType<?>, EntityType<InterceptorRocketEntity>> INTERCEPTOR_ROCKET = ENTITY_TYPES.registerEntityType(
            "interceptor_rocket", InterceptorRocketEntity::new, MobCategory.MISC, builder -> builder
                    .noLootTable()
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .fireImmune()
                    .noSummon());

    private ModEntityTypes() {
    }
}
