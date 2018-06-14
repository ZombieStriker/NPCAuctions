package me.zombie_striker.npcauctions;

import org.bukkit.Material;

public class BlackListedItem {

	private Material m;
	private short d;
	
	public BlackListedItem(Material mat, short data) {
		this.m = mat;
		this.d = data;
	}
	
	public Material getMat() {
		return m;
	}
	public short getData() {
		return d;
	}
}
