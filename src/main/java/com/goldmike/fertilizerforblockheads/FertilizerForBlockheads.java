package com.goldmike.fertilizerforblockheads;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
@Mod(FertilizerForBlockheads.MODID)
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FertilizerForBlockheads {
    public static final String MODID = "fertilizerforblockheads";
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    private static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, MODID);
    // New combo blocks (missing from Farming for Blockheads)
    public static final RegistryObject<Block> FERTILIZED_FARMLAND_RICH_HEALTHY =
            BLOCKS.register("fertilized_farmland_rich_healthy", () -> new ComboFarmlandBlock(false, true));
    public static final RegistryObject<Block> FERTILIZED_FARMLAND_RICH_HEALTHY_STABLE =
            BLOCKS.register("fertilized_farmland_rich_healthy_stable", () -> new ComboFarmlandBlock(true, false));
    // BlockItems so they can be crafted / held + show FFB-style tooltips
    @SuppressWarnings("unused")
    public static final RegistryObject<Item> FERTILIZED_FARMLAND_RICH_HEALTHY_ITEM =
            ITEMS.register("fertilized_farmland_rich_healthy",
                    () -> new ComboFarmlandItem(FERTILIZED_FARMLAND_RICH_HEALTHY.get(), new Item.Properties(),
                            true, true, false));
    @SuppressWarnings("unused")
    public static final RegistryObject<Item> FERTILIZED_FARMLAND_RICH_HEALTHY_STABLE_ITEM =
            ITEMS.register("fertilized_farmland_rich_healthy_stable",
                    () -> new ComboFarmlandItem(FERTILIZED_FARMLAND_RICH_HEALTHY_STABLE.get(), new Item.Properties(),
                            true, true, true));
    @SuppressWarnings("unused")
    private static final RegistryObject<Codec<? extends IGlobalLootModifier>> RICH_BONUS =
            LOOT_MODIFIERS.register("rich_bonus", () -> RichBonusLootModifier.CODEC);
    public FertilizerForBlockheads(FMLJavaModLoadingContext ctx) {
        IEventBus modBus = ctx.getModEventBus();
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        LOOT_MODIFIERS.register(modBus);
        ModRecipes.SERIALIZERS.register(modBus);
        modBus.addListener(FertilizerForBlockheads::addToCreativeTabs);
        FarmingForBlockheadsConfigBridge.load();
    }
    private static int rollExtraCount(RandomSource rand, double chance) {
        if (chance <= 0) return 0;
        int guaranteed = (int) Math.floor(chance);
        double remainder = chance - guaranteed;
        return guaranteed + (rand.nextDouble() < remainder ? 1 : 0);
    }
    static final class ComboFarmlandBlock extends FarmBlock {
        private final boolean stableNoTrample;
        private final boolean regresses;
        ComboFarmlandBlock(boolean stableNoTrample, boolean regresses) {
            super(BlockBehaviour.Properties.copy(Blocks.FARMLAND));
            this.stableNoTrample = stableNoTrample;
            this.regresses = regresses;
        }
        @Override
        public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
            if (stableNoTrample) {
                entity.causeFallDamage(fallDistance, 1.0F, level.damageSources().fall());
                return;
            }
            super.fallOn(level, state, pos, entity, fallDistance);
        }
        @Override
        public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            super.randomTick(state, level, pos, random);
            if (!level.getBlockState(pos).is(this)) return;
            if (!regresses) return;
            double chance = FarmingForBlockheadsConfigBridge.regressionChance;
            if (chance <= 0) return;
            if (random.nextDouble() < chance) {
                BlockState target = Blocks.FARMLAND.defaultBlockState();
                if (state.hasProperty(MOISTURE) && target.hasProperty(MOISTURE)) {
                    target = target.setValue(MOISTURE, state.getValue(MOISTURE));
                }
                level.setBlock(pos, target, 3);
            }
        }
    }
    static final class ComboFarmlandItem extends BlockItem {
        private static final ResourceLocation GREEN_FERTILIZER = rl("green_fertilizer");
        private static final ResourceLocation RED_FERTILIZER   = rl("red_fertilizer");
        private static final ResourceLocation YELLOW_FERTILIZER = rl("yellow_fertilizer");
        private static void addFertilizerTooltip(List<Component> tooltip, ResourceLocation fertilizerId,
                                                 @Nullable Level level, TooltipFlag flag) {
            Item fertilizer = ForgeRegistries.ITEMS.getValue(fertilizerId);
            if (fertilizer == null) return;
            List<Component> lines = new ArrayList<>();
            fertilizer.appendHoverText(new ItemStack(fertilizer), level, lines, flag);
            for (Component line : lines) {
                String s = line.getString();
                if (!s.isBlank()) {
                    tooltip.add(line.copy()); // keep FFB translation + styling
                    break;
                }
            }
        }
        private final boolean rich;
        private final boolean healthy;
        private final boolean stable;
        ComboFarmlandItem(Block block, Properties props, boolean rich, boolean healthy, boolean stable) {
            super(block, props);
            this.rich = rich;
            this.healthy = healthy;
            this.stable = stable;
        }
        private static ResourceLocation rl(String path) {
            ResourceLocation id = ResourceLocation.tryBuild("farmingforblockheads", path);
            if (id == null) throw new IllegalArgumentException("Invalid ResourceLocation: " + "farmingforblockheads" + ":" + path);
            return id;
        }
        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
            super.appendHoverText(stack, level, tooltip, flag);
            if (rich)    addFertilizerTooltip(tooltip, GREEN_FERTILIZER, level, flag);
            if (healthy) addFertilizerTooltip(tooltip, RED_FERTILIZER, level, flag);
            if (stable)  addFertilizerTooltip(tooltip, YELLOW_FERTILIZER, level, flag);
        }
    }
    @Mod.EventBusSubscriber(modid = FertilizerForBlockheads.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ComboGrowthHandler {
        @SubscribeEvent
        public static void onCropGrowPost(BlockEvent.CropGrowEvent.Post event) {
            LevelAccessor acc = event.getLevel();
            if (!(acc instanceof ServerLevel level)) return;
            BlockPos cropPos = event.getPos();
            BlockState cropState = event.getState();
            if (!(cropState.getBlock() instanceof CropBlock crop)) return;
            if (!cropState.is(BlockTags.CROPS)) return;
            BlockState soil = level.getBlockState(cropPos.below());
            boolean onOurSoil =
                    soil.is(FertilizerForBlockheads.FERTILIZED_FARMLAND_RICH_HEALTHY.get()) ||
                            soil.is(FertilizerForBlockheads.FERTILIZED_FARMLAND_RICH_HEALTHY_STABLE.get());
            if (!onOurSoil) return;
            double chance = FarmingForBlockheadsConfigBridge.bonusGrowthChance;
            int extra = rollExtraCount(level.random, chance);
            if (extra <= 0) return;
            int age = crop.getAge(cropState);
            int maxAge = crop.getMaxAge();
            if (age >= maxAge) return;
            int newAge = Math.min(maxAge, age + extra);
            if (newAge == age) return;
            level.setBlock(cropPos, crop.getStateForAge(newAge), 2);
        }
    }
    static final class RichBonusLootModifier extends LootModifier {
        public static final Codec<RichBonusLootModifier> CODEC =
                RecordCodecBuilder.create(inst ->
                        codecStart(inst).apply(inst, RichBonusLootModifier::new)
                );
        private RichBonusLootModifier(LootItemCondition[] conditions) {
            super(conditions);
        }
        @Override
        protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
            BlockState harvested = context.getParamOrNull(LootContextParams.BLOCK_STATE);
            Vec3 origin = context.getParamOrNull(LootContextParams.ORIGIN);
            if (harvested == null || origin == null) return generatedLoot;
            if (!(harvested.getBlock() instanceof CropBlock) && !harvested.is(BlockTags.CROPS)) return generatedLoot;
            BlockPos pos = BlockPos.containing(origin);
            BlockState soil = context.getLevel().getBlockState(pos.below());
            boolean onOurSoil =
                    soil.is(FERTILIZED_FARMLAND_RICH_HEALTHY.get()) ||
                            soil.is(FERTILIZED_FARMLAND_RICH_HEALTHY_STABLE.get());
            if (!onOurSoil) return generatedLoot;
            double chance = FarmingForBlockheadsConfigBridge.bonusCropChance;
            if (chance <= 0) return generatedLoot;
            RandomSource rand = context.getRandom();
            for (int idx = 0; idx < generatedLoot.size(); idx++) {
                ItemStack stack = generatedLoot.get(idx);
                if (stack.isEmpty()) continue;
                if (stack.is(Tags.Items.SEEDS)) continue;
                int add = rollExtraCount(rand, chance);
                if (add <= 0) continue;
                int room = stack.getMaxStackSize() - stack.getCount();
                int toGrow = Math.min(room, add);
                if (toGrow > 0) {
                    stack.grow(toGrow);
                    add -= toGrow;
                }
                while (add > 0) {
                    int chunk = Math.min(add, stack.getMaxStackSize());
                    generatedLoot.add(stack.copyWithCount(chunk));
                    add -= chunk;
                }
            }
            return generatedLoot;
        }
        @Override
        public Codec<? extends IGlobalLootModifier> codec() {
            return CODEC;
        }
    }
    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        ResourceLocation tabId = event.getTabKey().location();
        // Add to any Farming for Blockheads-owned tab
        if (!"farmingforblockheads".equals(tabId.getNamespace())) return;
        event.accept(FERTILIZED_FARMLAND_RICH_HEALTHY_ITEM);
        event.accept(FERTILIZED_FARMLAND_RICH_HEALTHY_STABLE_ITEM);
    }
}