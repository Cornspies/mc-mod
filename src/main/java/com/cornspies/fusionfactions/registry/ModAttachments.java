package com.cornspies.fusionfactions.registry;

import java.util.function.Supplier;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.weapon.LockOn;
import com.cornspies.fusionfactions.weapon.Trigger;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, FusionFactions.MODID);

    // Whether a player holds the trigger (Trigger). Not saved.
    public static final Supplier<AttachmentType<Trigger.State>> TRIGGER = ATTACHMENT_TYPES.register("trigger",
            () -> AttachmentType.builder(Trigger.State::new).build());

    // A rocket launcher's lock-on (LockOn). Not saved.
    public static final Supplier<AttachmentType<LockOn.State>> LOCK_ON = ATTACHMENT_TYPES.register("lock_on",
            () -> AttachmentType.builder(LockOn.State::new).build());

    private ModAttachments() {
    }
}
