package me.zombie_striker.npcauctions;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class QuickMaterialConversionClass {

	@SuppressWarnings("deprecation")
	public static ItemStack getGrayStained() {
		Material m =null;
		if((m=Material.matchMaterial("GRAY_STAINED_GLASS_PANE"))==null) 
			return new ItemStack(Material.matchMaterial("STAINED_GLASS_PANE"),1,DyeColor.GRAY.getWoolData());
		return new ItemStack(m);
	}
	public static ItemStack getGoldBlock() {
		return new ItemStack(Material.GOLD_BLOCK);
	}
	@SuppressWarnings("deprecation")
	public static ItemStack getRedWool() {
		Material m =null;
		if((m=Material.matchMaterial("RED_WOOL"))==null) 
			return new ItemStack(Material.matchMaterial("WOOL"),1,DyeColor.RED.getWoolData());
		return new ItemStack(m);
	}
	@SuppressWarnings("deprecation")
	public static ItemStack getBlueWool() {
		Material m =null;
		if((m=Material.matchMaterial("BLUE_WOOL"))==null) 
			return new ItemStack(Material.matchMaterial("WOOL"),1,DyeColor.BLUE.getWoolData());
		return new ItemStack(m);
	}
	@SuppressWarnings("deprecation")
	public static ItemStack getGreenWool() {
		Material m =null;
		if((m=Material.matchMaterial("GREEN_WOOL"))==null) 
			return new ItemStack(Material.matchMaterial("WOOL"),1,DyeColor.GREEN.getWoolData());
		return new ItemStack(m);
	}
}
