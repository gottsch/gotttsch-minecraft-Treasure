/**
 * 
 */
package com.someguyssoftware.treasure2.item;

import com.someguyssoftware.gottschcore.block.BlockContext;
import com.someguyssoftware.gottschcore.item.ModBlockItem;
import com.someguyssoftware.treasure2.block.SkeletonBlock;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;

/**
 * @author Mark Gottschling on Feb 2, 2019
 *
 */
public class SkeletonItem extends ModBlockItem {
	public static final int MAX_STACK_SIZE = 1;

	/**
	 * 
	 */
	public SkeletonItem(String modID, String name, Block block, Item.Properties properties) {
		super(modID, name, block, properties.maxStackSize(MAX_STACK_SIZE).group(TreasureItemGroups.MOD_ITEM_GROUP));
	}

	protected boolean placeBlock(BlockItemUseContext context, BlockState state) {
		BlockPos blockPos = context.getPos().offset(state.get(SkeletonBlock.FACING).getOpposite());
		BlockContext blockContext = new BlockContext(context.getWorld(), blockPos);
		if (blockContext.isAir() || blockContext.isReplaceable()) {
			return context.getWorld().setBlockState(context.getPos(), state, 26);
		}
		return false;
	}
}