package at.feldim2425.moreoverlays.lightoverlay;

import at.feldim2425.moreoverlays.api.lightoverlay.LightScannerBase;
import at.feldim2425.moreoverlays.config.Config;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.stream.Collectors;

public class LightScannerVanilla extends LightScannerBase {

    private static final AxisAlignedBB TEST_BB = new AxisAlignedBB(0.6D / 2D, 0, 0.6D / 2D, 1D - 0.6D / 2D, 1D, 1D - 0.6D / 2D);

    private final List<EntityType<?>> typesToCheck;

    public LightScannerVanilla() {
		this.typesToCheck = ForgeRegistries.ENTITIES.getValues().stream().filter((type) -> type.isSummonable() && type.getClassification() == EntityClassification.MONSTER).collect(Collectors.toList());
    }

    private static boolean checkCollision(final BlockPos pos, final World world) {
        final BlockState block1 = world.getBlockState(pos);

        if (block1.isNormalCube(world, pos) || (!Config.light_IgnoreLayer.get() && world.getBlockState(pos.up()).isNormalCube(world, pos.up()))) //Don't check because a check on normal Cubes will/should return false ( 99% collide ).
            return false;
        else if (world.isAirBlock(pos) && (Config.light_IgnoreLayer.get() || world.isAirBlock(pos.up())))  //Don't check because Air has no Collision Box
            return true;

        final AxisAlignedBB bb = LightScannerVanilla.TEST_BB.offset(pos.getX(), pos.getY(), pos.getZ());
        if (world.getCollisionShapes(null, bb).count() == 0 && !world.containsAnyLiquid(bb)) {
            if (Config.light_IgnoreLayer.get())
                return true;
            else {
                final AxisAlignedBB bb2 = bb.offset(0, 1, 0);
                return world.getCollisionShapes(null, bb2).count() == 0 && !world.containsAnyLiquid(bb2);
            }
        }
        return false;
    }

    @Override
    public byte getSpawnModeAt(final BlockPos pos, final World world) {
        if (world.getLightFor(LightType.BLOCK, pos) >= Config.light_SaveLevel.get())
            return 0;

        BlockPos blockPos = pos.down();

        if (world.isAirBlock(blockPos)) {
            return 0;
        }

        if (!LightScannerVanilla.checkCollision(pos, world))
            return 0;

        BlockState state = world.getBlockState(blockPos);
        Block block = state.getBlock();
        if (!Config.light_SimpleEntityCheck.get()) {
            boolean hasSpawnable = false;
            for (EntityType<?> type : typesToCheck) {
                if (block.canCreatureSpawn(state, world, blockPos, EntitySpawnPlacementRegistry.PlacementType.ON_GROUND, type)) {
                    hasSpawnable = true;
                    break;
                }
            }

            if (!hasSpawnable) {
                return 0;
            }
        } else if (!block.canCreatureSpawn(state, world, blockPos, EntitySpawnPlacementRegistry.PlacementType.ON_GROUND, EntityType.ZOMBIE)) {
            return 0;
        }

        if (world.getLightFor(LightType.SKY, pos) >= Config.light_SaveLevel.get())
            return 1;

        return 2;
    }
}