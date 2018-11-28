package me.zombie_striker.npcauctions;

import java.util.UUID;

import org.bukkit.inventory.ItemStack;

public class Auction {

	public ItemStack is;
	public UUID owner;
	public String ownerName;
	public boolean ownerOnline = true;
	public int ticksLeft;
	public double buyitnow = -1;
	public UUID lastBid;
	public double currentPrice;
	public double biddingPrice = 10;

	public UUID auctionID = null;

	public Auction(ItemStack is, UUID owner, String ownername) {
		this(is, owner, ownername, UUID.randomUUID());
	}

	public Auction(ItemStack is, UUID owner, String ownername, UUID internalId) {
		this.is = is;
		this.owner = owner;
		this.auctionID = internalId;
		this.ownerName = ownername;
	}

	public boolean tickAuc() {
		ticksLeft -= (Main.refreshRate * 20);
		return ticksLeft <= 0;
	}

	public void setWait(int ticks) {
		this.ticksLeft = ticks;
	}

	public boolean hasBuyItNow() {
		return buyitnow != -1;
	}
}
