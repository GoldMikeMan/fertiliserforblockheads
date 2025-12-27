package com.goldmike.fertilizerforblockheads;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
public final class fertilizerRightClickHandler {
    private static final int RICH = 1;    // green
    private static final int HEALTHY = 2; // red
    private static final int STABLE = 4;  // yellow
    private static final ResourceLocation GREEN = rl("farmingforblockheads", "green_fertilizer");
    private static final ResourceLocation RED = rl("farmingforblockheads", "red_fertilizer");
    private static final ResourceLocation YELL = rl("farmingforblockheads", "yellow_fertilizer");
    private static final ResourceLocation VANILLA_FARMLAND = rl("minecraft", "farmland");
    private static final ResourceLocation FFB_RICH = rl("farmingforblockheads", "fertilised_farmland_rich");
    private static final ResourceLocation FFB_HEALTHY = rl("farmingforblockheads", "fertilised_farmland_healthy");
    private static final ResourceLocation FFB_STABLE = rl("farmingforblockheads", "fertilised_farmland_stable");
    private static final ResourceLocation FFB_RICH_STABLE = rl("farmingforblockheads", "fertilised_farmland_rich_stable");
    private static final ResourceLocation FFB_HEALTHY_STABLE = rl("farmingforblockheads", "fertilised_farmland_healthy_stable");
    private static final ResourceLocation OUR_RICH_HEALTHY =
            rl(FertilizerForBlockheads.MODID, "fertilised_farmland_rich_healthy");
    private static final ResourceLocation OUR_RICH_HEALTHY_STABLE =
            rl(FertilizerForBlockheads.MODID, "fertilised_farmland_rich_healthy_stable");
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock e) {
        Level level = e.getLevel();
        if (level.isClientSide()) return;
        ItemStack held = e.getItemStack();
        ResourceLocation heldId = ForgeRegistries.ITEMS.getKey(held.getItem());
        if (heldId == null) return;
        int add;
        if (heldId.equals(GREEN)) add = RICH;
        else if (heldId.equals(RED)) add = HEALTHY;
        else if (heldId.equals(YELL)) add = STABLE;
        else return;
        BlockPos pos = e.getPos();
        BlockState state = level.getBlockState(pos);
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (blockId == null) return;
        int current = flagsFromBlock(blockId);
        if (current < 0) return;
        int next = current | add;
        if (next == current) return;
        ResourceLocation targetId = blockFromFlags(next);
        if (targetId == null) return;
        var targetBlock = ForgeRegistries.BLOCKS.getValue(targetId);
        if (targetBlock == null) return;
        BlockState newState = targetBlock.defaultBlockState();
        if (state.hasProperty(FarmBlock.MOISTURE) && newState.hasProperty(FarmBlock.MOISTURE)) {
            newState = newState.setValue(FarmBlock.MOISTURE, state.getValue(FarmBlock.MOISTURE));
        }
        level.setBlock(pos, newState, 3);
        if (!e.getEntity().getAbilities().instabuild) {
            held.shrink(1);
        }
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
    }
    private static int flagsFromBlock(ResourceLocation id) {
        if (id.equals(VANILLA_FARMLAND)) return 0;
        if (id.equals(FFB_RICH)) return RICH;
        if (id.equals(FFB_HEALTHY)) return HEALTHY;
        if (id.equals(FFB_STABLE)) return STABLE;
        if (id.equals(FFB_RICH_STABLE)) return RICH | STABLE;
        if (id.equals(FFB_HEALTHY_STABLE)) return HEALTHY | STABLE;
        if (id.equals(OUR_RICH_HEALTHY)) return RICH | HEALTHY;
        if (id.equals(OUR_RICH_HEALTHY_STABLE)) return RICH | HEALTHY | STABLE;
        return -1;
    }
    private static ResourceLocation blockFromFlags(int flags) {
        return switch (flags) {
            case 0 -> VANILLA_FARMLAND;
            case RICH -> FFB_RICH;
            case HEALTHY -> FFB_HEALTHY;
            case STABLE -> FFB_STABLE;
            case (RICH | STABLE) -> FFB_RICH_STABLE;
            case (HEALTHY | STABLE) -> FFB_HEALTHY_STABLE;
            case (RICH | HEALTHY) -> OUR_RICH_HEALTHY;
            case (RICH | HEALTHY | STABLE) -> OUR_RICH_HEALTHY_STABLE;
            default -> null;
        };
    }
    private static ResourceLocation rl(String namespace, String path) {
        ResourceLocation id = ResourceLocation.tryBuild(namespace, path);
        if (id == null) throw new IllegalArgumentException("Invalid ResourceLocation: " + namespace + ":" + path);
        return id;
    }
    private fertilizerRightClickHandler() {}
}