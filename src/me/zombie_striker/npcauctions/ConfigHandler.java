package me.zombie_striker.npcauctions;

import java.io.File;
import java.io.IOException;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

public class ConfigHandler {

	public File f;
	public YamlConfiguration config;

	public String getMessage(Keys k, String def) {
		if (!config.contains(k.s)) {
			config.set(k.s, def);
			return ChatColor.translateAlternateColorCodes('&', def);
		}
		return ChatColor.translateAlternateColorCodes('&',
				config.getString(k.s));
	}

	public static ConfigHandler init(Main main) {
		ConfigHandler c = new ConfigHandler();
		c.f = new File(main.getDataFolder(), "messages.yml");
		if (!c.f.exists())
			try {
				c.f.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		c.config = YamlConfiguration.loadConfiguration(c.f);
		return c;
	}

	public void save() {
		try {
			config.save(f);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public enum Keys {
		NoPerm("NoPerm"), Title("MainTitle"), AddedToAuc("AddedToAuctionHouse"), CancelAuctionInv(
				"CancelAuctionInvTitle"), AddAuctionInv("AddNewAuctionInvTitle"), CancelGen(
				"CancelAuctionGeneralMessage"), CancelAuctionInvalid(
				"CancelAuctionInvalidInput"), CancelAuctionExisting(
				"CancelExistingAuctionBecauseItsUnfinished"), CancelOwn(
				"CancelBidBecauseItsThePlayers"), AddAuc(
				"SuccessfullyAddedAuction"), WonAuction("BidderWonAuction"), WonAucEarn(
				"AuctionEndedSold"), NoBids("AuctionEndedNoBids"), BidIncrease(
				"InitialIncreaseBidding"), BiddingStart("InitialStarttingBid"), BiddingDur(
				"InitialBiddingDurration"), BiddingBuy("InitialBuyItNow"), VillagersName(
				"NPCName"), ItemNext("Item_NextPage"), ItemPrev(
				"Item_PrevPage"), ItemAdd("Item_AddItemToAuctionHouse"), ItemCancel(
				"Item_CancelExisitngAuction"), IncreaseMin("Min-Increase"),IncreaseMax("Max-Increase"),LoreTime(
				"ItemLore_Time_Remaining"), LoreBuyItNow("ItemLore_Buy_it_Now"), loreBuyItNowHow(
				"ItemLore_Buy_Ity_Now_Message"), LoreOwner(
				"ItemLore_Created_By"), CAnnotBidOwnAuction(
				"CancelCannotBidOwnAuction"), BuyIUtNowNo(
				"Buy_It_Now_Optional_Say_No"), BuyItNowSetTo(
				"Buy_It_Now_Price_Set"),Blacklisted("BlacklistedMaterial"),MAX_HOURS(
						"MaximumHoursForBids"),OutBid(
						"OutBid"),broadcastAuction("BroacastAuctionOnSubmit"),broadcastAuctionMesssage("BroacastAuctionOnSubmitMessage"),limitAuctions("EnableAuctionLimitPerPlayer"),overlimit("AuctionLimitReached");
		public String s;

		private Keys(String j) {
			s = j;
		}
	}
}
