package com.cornspies.fusionfactions.supplydrop;

import org.jspecify.annotations.Nullable;

import com.cornspies.fusionfactions.registry.ModBlockEntityTypes;
import com.cornspies.fusionfactions.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

// The landed crate. Unbreakable and immovable (D17) through its block properties, see ModBlocks.
public class SupplyCrateBlock extends BaseEntityBlock {
    public static final EnumProperty<CrateRarity> RARITY = EnumProperty.create("rarity", CrateRarity.class);

    public SupplyCrateBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(RARITY, CrateRarity.COMMON));
    }

    public static BlockState stateFor(CrateRarity rarity) {
        return ModBlocks.SUPPLY_CRATE.get().defaultBlockState().setValue(RARITY, rarity);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RARITY);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SupplyCrateBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        // D13: anyone can open a crate as soon as it has landed.
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SupplyCrateBlockEntity crate) {
            player.openMenu(crate);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntityTypes.SUPPLY_CRATE.get(), SupplyCrateBlockEntity::serverTick);
    }
}
