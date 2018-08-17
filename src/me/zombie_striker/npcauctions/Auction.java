package me.zombie_striker.npcauctions;

import java.util.UUID;

import org.bukkit.inventory.ItemStack;

public class Auction {

	public ItemStack is;
	public UUID owner;
	public String ownerName;
	public boolean ownerOnline = true;
	public int quarterSecondsLeft;
	public double buyitnow = -1;
	public UUID lastBid;
	public double currentPrice;
	public int auctionID = -1;
	public double biddingPrice = 10;

	public Auction(ItemStack is, UUID owner, String ownername, int id) {
		this.is = is;
		this.owner = owner;
		this.auctionID = id;
		this.ownerName = ownername;
	}

	public boolean tickAuc() {
		quarterSecondsLeft--;
		return quarterSecondsLeft <= 0;
	}

	public void setWait(int ticks) {
		this.quarterSecondsLeft = ticks;
	}

	public boolean hasBuyItNow() {
		return buyitnow != -1;
	}
}
