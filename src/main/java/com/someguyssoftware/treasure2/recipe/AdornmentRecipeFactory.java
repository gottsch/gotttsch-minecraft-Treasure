/**
 * 
 */
package com.someguyssoftware.treasure2.recipe;

import com.google.gson.JsonObject;
import com.someguyssoftware.treasure2.Treasure;
import com.someguyssoftware.treasure2.capability.CharmableCapabilityProvider;
import com.someguyssoftware.treasure2.capability.ICharmCapability;
import com.someguyssoftware.treasure2.capability.ICharmableCapability;
import com.someguyssoftware.treasure2.item.IAdornment;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.CraftingHelper.ShapedPrimer;
import net.minecraftforge.common.crafting.IRecipeFactory;
import net.minecraftforge.common.crafting.JsonContext;
import net.minecraftforge.oredict.ShapedOreRecipe;

/**
 * @author Mark Gottschling on Apr 25, 2021
 *
 */
public class AdornmentRecipeFactory implements IRecipeFactory {

	@Override
	public IRecipe parse(JsonContext context, JsonObject json) {
        ShapedOreRecipe recipe = ShapedOreRecipe.factory(context, json);

        ShapedPrimer primer = new ShapedPrimer();
        primer.width = recipe.getWidth();
        primer.height = recipe.getHeight();
        primer.mirrored = JsonUtils.getBoolean(json, "mirrored", true);
        primer.input = recipe.getIngredients();

        return new BejewelAdornmentRecipe(new ResourceLocation(Treasure.MODID, "adornment_recipe_factory"), recipe.getRecipeOutput(), primer);
	}

	
	public static class BejewelAdornmentRecipe extends ShapedOreRecipe {

		/**
		 * 
		 * @param group
		 * @param result
		 * @param primer
		 */
		public BejewelAdornmentRecipe(ResourceLocation group, ItemStack result, ShapedPrimer primer) {
			super(group, result, primer);
		}
		
		@Override
		public ItemStack getCraftingResult(InventoryCrafting inventoryCrafting) {
			// create a copy of the current output result as defined by the recipe json file
//			ItemStack result = this.output.copy();
			ItemStack output = super.getCraftingResult(inventoryCrafting);
			
			ItemStack adornmentStack = ItemStack.EMPTY;
			
			// find all adornment ingredients
			for (int inventoryIndex = 0; inventoryIndex < inventoryCrafting.getSizeInventory(); inventoryIndex++) {
				ItemStack inventoryStack = inventoryCrafting.getStackInSlot(inventoryIndex);
				if (!inventoryStack.isEmpty() && inventoryStack.getItem() instanceof IAdornment) {
					adornmentStack = inventoryStack;
					break;
				}
			}
			
            ICharmableCapability adornmentCharmableCap = adornmentStack.getCapability(CharmableCapabilityProvider.CHARMABLE_CAPABILITY, null);
            ICharmCapability adornmentCharmCap = adornmentStack.getCapability(CharmableCapabilityProvider.CHARM_CAPABILITY, null);
            Treasure.logger.debug("adornment charm instances -> {}", adornmentCharmCap.getCharmInstances().size());
            
            ICharmCapability outputCharmCap = output.getCapability(CharmableCapabilityProvider.CHARM_CAPABILITY, null);
            ICharmableCapability outputCharmableCap = output.getCapability(CharmableCapabilityProvider.CHARMABLE_CAPABILITY, null);
            Treasure.logger.debug("new output charm instances -> {}", outputCharmCap.getCharmInstances().size());
            
            outputCharmCap.getCharmInstances().addAll(adornmentCharmCap.getCharmInstances());
            outputCharmableCap.setSlots(adornmentCharmableCap.getSlots());
            
            return output;
		}

	}
}
