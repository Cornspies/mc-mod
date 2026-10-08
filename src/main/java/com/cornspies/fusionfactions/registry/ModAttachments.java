package com.cornspies.fusionfactions.registry;

import java.util.function.Supplier;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.weapon.Trigger;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, FusionFactions.MODID);

    // Whether a player holds the trigger (Trigger). Not saved.
    public static final Supplier<AttachmentType<Trigger.State>> TRIGGER = ATTACHMENT_TYPES.register("trigger",
            () -> AttachmentType.builder(Trigger.State::new).build());

    private ModAttachments() {
    }
}
