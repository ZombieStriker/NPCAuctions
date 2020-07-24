package me.zombie_striker.npcauctions;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.zombie_striker.npcauctions.ConfigHandler.Keys;
import net.milkbowl.vault.economy.Economy;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class Main extends JavaPlugin implements Listener {

	// TODO: Make way to hold auctions in auction house

	public static String s_NOPERM = "&6 You do not have permission to use this command";
	public static String s_Title = ChatColor.AQUA + "[" + ChatColor.WHITE + "Auction House" + ChatColor.AQUA + "]";
	public static String s_InvTitleCancel = " Cancel which auction?";
	public static String s_InvTitleAdd = " Which item would you like to auction off?";
	public static String s_AuctionEneededNoBids = " Your %item% auction ended. No one bid.";
	public static String s_WonAuction = " You have won the %item% Auction!";
	public static String s_WonEarning = " Your auction ended, earning you $%price%.";
	public static String s_BidIncrease = " By how much will the bidding increase? (Min:$%min%, Max:$%max%)";
	public static String s_BidStarting = " What would be the starting bid?";
	public static String s_BidDurration = " How long should the auction last? (Using the format hoursH minutesM secondsS)";
	public static String s_BidBuyItNow = "[OPTIONAL] Do you want a Buy-It-Now for your item? If so, type in a valid the price.";
	public static String s_addedToAuctionHouse = " Added the auction to the auction house.";
	public static String s_cancelUnfinAuction = " Canceling Auction: Invalid input.";
	public static String s_cancelExisting = " Canceling existing auction.";
	public static String s_cancelGen = " Canceling auction...";
	public static String s_cancelOwn = " You cant bid on your own auction!";
	public static String s_cancelAlreadyBid = " You are the highest bidder on this auction!";
	public static String s_highestBidder = " Highest Bidder: %player%";
	public static String s_outBid = " You have been outbid for the %item% auction!";
	public static String s_rejoin_amount = " Since the last time you were on, you have recieved $%amount%!";
	public static String s_rejoin_items = " The following auctions ended while you were offline: ";
	public static String s_CLAIM_items = " Claiming auctions: ";
	public static String s_someoneBid = "%player% has bid $%bid% ($%amount%) for your %item% auction";
	public static String s_someoneBought = "%player% has bought your %item% auction for $%amount%.";
	public static String s_youBid = "You have bid $%bid% ($%amount%) for the %item% auction";
	public static String s_youBought = "You have bought the %item% auction for $%amount%.";
	public static String s_auctionCancelRefund = "The owner of the %item% auction canceled the auction. You have been refunded $%amount%.";
	public static String s_holdingWonAuction = "This auction will be held in the auction house till you collect it.";
	public static String s_Menu_page = " Page: ";

	public static String s_VillagerName = "&6[AuctionHouse]";

	public static String s_ItemAdd = "&2[Create new Auction]";
	public static String s_ItemRemove = "&c[Cancel Auction]";
	public static String s_ItemNex = "&2[Next Page]";
	public static String s_ItemPrev = "&c[Previous Page]";
	public static String s_ItemCollect = "&6[Collect Ended Auctions]";

	public static String s_loretime = "Time Remaining: ";
	public static String s_lorebuyitnow = "&a&lBuy-It-Now : [$%price%]";
	public static String s_lorebuyitnowhow = "&a&lRight click to Buy-It-Now";
	public static String s_loreowner = "Created by: ";
	public static String s_cannotbidown = "&c You cannot bid on your own auction!";
	public static String s_buyitnowoptional = "If NOT, type in \"No\" ";
	public static String s_buyitnowset = " Set the Buy-It_now to $%price%";
	public static String s_blacklistedmaterial = "&c %material% is not allowed to be auctioned!";

	public static String auctionhouseSkin = null;

	public static String s_lorePrice = "Price : [$%price%+%bid%]";

	public static boolean enableViewLastBid = false;
	public static double increaseMin = 1.0;
	public static double increaseMax = 1000.0;

	public static double refreshRate = 0.5;

	public static int s_MAX_BID_TIME = 24;

	public static List<UUID> removeAuctions = new ArrayList<UUID>();

	public static List<String> blacklistWorlds = new ArrayList<>();

	public static HashMap<UUID, Location> tpbackto = new HashMap<UUID, Location>();

	public static boolean enableBroadcasting = false;
	public static boolean messageOnBid = true;
	public static String s_broadcastMessage = " %player% is auctioning %amount% %material% starting at %cost%";
	public static boolean limitAmount = false;
	public static String s_overlimit = " You cannot auction this many items at once! Wait till one of your other auctions is finished.";

	public static Inventory[] gui = new Inventory[20];
	public static String prefix = ChatColor.AQUA + "[" + ChatColor.WHITE + "NPCAuctions" + ChatColor.AQUA + "]"
			+ ChatColor.WHITE;

	public ItemStack bufferstack;
	public ItemStack nextpage;
	public ItemStack prevpage;

	public ItemStack addAuc;
	public ItemStack cancelAuc;
	public ItemStack collectAuc;

	public static Economy econ = null;

	public List<Auction> auctions = new ArrayList<>();
	public List<BlackListedItem> blacklist = new ArrayList<BlackListedItem>();

	public HashMap<UUID, Auction> auctionWaitingMap = new HashMap<>();

	public HashMap<UUID, Integer> auctionWaitingStage = new HashMap<UUID, Integer>();

	public static boolean USE_VILLAGERS = true;

	public static Main instance;

	private boolean setupEconomy() {
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		econ = rsp.getProvider();
		return econ != null;
	}

	public void saveAuctionNoSave(Auction a) {

		getConfig().set("Auctions." + a.owner.toString() + "." + a.auctionID.toString() + "." + "price",
				a.currentPrice);
		getConfig().set("Auctions." + a.owner.toString() + "." + a.auctionID.toString() + "." + "buyitnow", a.buyitnow);
		getConfig().set("Auctions." + a.owner.toString() + "." + a.auctionID.toString() + "." + "increase",
				a.biddingPrice);
		if (a.lastBid != null)
			getConfig().set("Auctions." + a.owner.toString() + "." + a.auctionID.toString() + "." + "lastbidder",
					a.lastBid.toString());
		getConfig().set("Auctions." + a.owner.toString() + "." + a.auctionID.toString() + "." + "timeleftInTicks",
				a.ticksLeft);
		getConfig().set("Auctions." + a.owner.toString() + "." + a.auctionID.toString() + "." + "item", a.is);
	}

	@Override
	public void onDisable() {
		reloadConfig();
		for (Auction a : auctions) {
			saveAuctionNoSave(a);
		}
		saveConfig();
	}

	public void reloadVals() {

		if (getConfig().contains("Auction-RefreshRate-inSeconds")) {
			refreshRate = getConfig().getDouble("Auction-RefreshRate-inSeconds");
		} else {
			getConfig().set("Auction-RefreshRate-inSeconds", refreshRate);
			saveConfig();
		}

		if (getConfig().contains("NPCSkin_Username")) {
			auctionhouseSkin = getConfig().getString("NPCSkin_Username");
		} else {
			getConfig().set("NPCSkin_Username", "null");
			saveConfig();
		}

		if (getConfig().contains("UseVillager")) {
			USE_VILLAGERS = getConfig().getBoolean("UseVillager");
		} else {
			if (getServer().getPluginManager().getPlugin("Citizens") == null
					|| getServer().getPluginManager().getPlugin("Citizens").isEnabled() == false) {
				getLogger().log(Level.SEVERE, "Citizens 2.0 not found or not enabled");
				USE_VILLAGERS = true;
			} else {
				USE_VILLAGERS = false;
			}
			getConfig().set("UseVillager", USE_VILLAGERS);
			saveConfig();
		}
		if (!USE_VILLAGERS) {
			try {
				Bukkit.getPluginManager().registerEvents(new TraitEventHandler(), this);
			} catch (Exception | Error e) {
				getLogger().log(Level.SEVERE, "Citizens 2.0 not found or not enabled");
				e.printStackTrace();
				USE_VILLAGERS = true;
			}
		}
		ConfigHandler c = null;
		try {
			c = ConfigHandler.init(this);
			s_addedToAuctionHouse = c.getMessage(Keys.AddedToAuc, s_addedToAuctionHouse);
			s_AuctionEneededNoBids = c.getMessage(Keys.NoBids, s_AuctionEneededNoBids);
			s_BidBuyItNow = c.getMessage(Keys.BiddingBuy, s_BidBuyItNow);
			s_BidDurration = c.getMessage(Keys.BiddingDur, s_BidDurration);
			s_BidIncrease = c.getMessage(Keys.BidIncrease, s_BidIncrease);
			s_BidStarting = c.getMessage(Keys.BiddingStart, s_BidStarting);
			s_cancelExisting = c.getMessage(Keys.CancelAuctionExisting, s_cancelExisting);
			s_cancelGen = c.getMessage(Keys.CancelGen, s_cancelGen);
			s_cancelOwn = c.getMessage(Keys.CancelOwn, s_cancelOwn);
			s_cancelUnfinAuction = c.getMessage(Keys.CancelAuctionInvalid, s_cancelUnfinAuction);
			s_InvTitleAdd = c.getMessage(Keys.AddAuctionInv, s_InvTitleAdd);
			s_InvTitleCancel = c.getMessage(Keys.CancelAuctionInv, s_InvTitleCancel);
			s_NOPERM = c.getMessage(Keys.NoPerm, s_NOPERM);
			s_Title = c.getMessage(Keys.Title, s_Title);
			s_WonAuction = c.getMessage(Keys.WonAuction, s_WonAuction);
			s_WonEarning = c.getMessage(Keys.WonAucEarn, s_WonEarning);
			s_holdingWonAuction = c.getMessage(Keys.CollectAuction, s_holdingWonAuction);

			s_cancelAlreadyBid = c.getMessage(Keys.CANNOTBIDALRADYBID, s_cancelAlreadyBid);
			s_highestBidder = c.getMessage(Keys.HIGHESTBIDDER, s_highestBidder);

			s_Menu_page = c.getMessage(Keys.PAGE_SUFFIX, s_Menu_page);

			s_VillagerName = c.getMessage(Keys.VillagersName, s_VillagerName);
			s_outBid = c.getMessage(Keys.OutBid, s_outBid);
			s_someoneBid = c.getMessage(Keys.someonebid, s_someoneBid);
			s_someoneBought = c.getMessage(Keys.someonebought, s_someoneBought);
			s_youBid = c.getMessage(Keys.youbid, s_youBid);
			s_youBought = c.getMessage(Keys.youbought, s_youBought);
			s_auctionCancelRefund = c.getMessage(Keys.refundCanceled, s_auctionCancelRefund);
			s_rejoin_amount = c.getMessage(Keys.rejoin_amount, s_rejoin_amount);
			s_rejoin_items = c.getMessage(Keys.rejoin_items, s_rejoin_items);
			s_CLAIM_items = c.getMessage(Keys.CLAIM_items, s_CLAIM_items);

			prefix = c.getMessage(Keys.PREFIX, prefix);

			s_ItemAdd = c.getMessage(Keys.ItemAdd, s_ItemAdd);
			s_ItemRemove = c.getMessage(Keys.ItemCancel, s_ItemRemove);
			s_ItemNex = c.getMessage(Keys.ItemNext, s_ItemNex);
			s_ItemPrev = c.getMessage(Keys.ItemPrev, s_ItemPrev);
			s_ItemCollect = c.getMessage(Keys.ITEMCollect, s_ItemCollect);

			s_lorebuyitnow = c.getMessage(Keys.LoreBuyItNow, s_lorebuyitnow);
			s_lorebuyitnowhow = c.getMessage(Keys.loreBuyItNowHow, s_lorebuyitnowhow);
			s_loreowner = c.getMessage(Keys.LoreOwner, s_loreowner);
			s_loretime = c.getMessage(Keys.LoreTime, s_loretime);
			s_cannotbidown = c.getMessage(Keys.CAnnotBidOwnAuction, s_cannotbidown);
			s_buyitnowoptional = c.getMessage(Keys.BuyIUtNowNo, s_buyitnowoptional);
			s_buyitnowset = c.getMessage(Keys.BuyItNowSetTo, s_buyitnowset);
			s_lorePrice = c.getMessage(Keys.LorePrice, s_lorePrice);

			enableBroadcasting = Boolean.valueOf(c.getMessage(Keys.broadcastAuction, enableBroadcasting + ""));
			s_broadcastMessage = c.getMessage(Keys.broadcastAuctionMesssage, s_broadcastMessage);

			increaseMax = Double.parseDouble(c.getMessage(Keys.IncreaseMax, increaseMax + ""));
			increaseMin = Double.parseDouble(c.getMessage(Keys.IncreaseMin, increaseMin + ""));

			s_blacklistedmaterial = c.getMessage(Keys.Blacklisted, s_blacklistedmaterial);

			try {

				s_MAX_BID_TIME = Integer.parseInt(c.getMessage(Keys.MAX_HOURS, s_MAX_BID_TIME + ""));
			} catch (Exception e) {

			}

			limitAmount = Boolean.valueOf(c.getMessage(Keys.limitAuctions, limitAmount + ""));
			s_overlimit = c.getMessage(Keys.overlimit, s_overlimit);
		} catch (Error | Exception e) {
			e.printStackTrace();
		}
		try {
			c.save();
		} catch (Error | Exception e) {
			e.printStackTrace();
		}

		bufferstack = this.getConfigItemStack("NoItemSlot", QuickMaterialConversionClass.getGrayStained());
		ItemMeta im = bufferstack.getItemMeta();
		im.setDisplayName(" ");
		bufferstack.setItemMeta(im);

		nextpage = this.getConfigItemStack("NextPageIcon", QuickMaterialConversionClass.getGreenWool());
		ItemMeta im2 = nextpage.getItemMeta();
		im2.setDisplayName(s_ItemNex);
		nextpage.setItemMeta(im2);

		prevpage = this.getConfigItemStack("PrevPageIcon", QuickMaterialConversionClass.getRedWool());
		ItemMeta im3 = prevpage.getItemMeta();
		im3.setDisplayName(s_ItemPrev);
		prevpage.setItemMeta(im3);

		addAuc = this.getConfigItemStack("AddAuctionIcon", QuickMaterialConversionClass.getGreenWool());
		ItemMeta im4 = addAuc.getItemMeta();
		im4.setDisplayName(s_ItemAdd);
		addAuc.setItemMeta(im4);
		cancelAuc = this.getConfigItemStack("CancelAuctionIcon", QuickMaterialConversionClass.getRedWool());
		ItemMeta im5 = cancelAuc.getItemMeta();
		im5.setDisplayName(s_ItemRemove);
		cancelAuc.setItemMeta(im5);
		collectAuc = this.getConfigItemStack("CollectAuctionIcon", QuickMaterialConversionClass.getGoldBlock());
		ItemMeta im6 = collectAuc.getItemMeta();
		im6.setDisplayName(s_ItemCollect);
		collectAuc.setItemMeta(im6);

		if (!getConfig().contains("SendMessagesWhenSomeoneBid")) {
			getConfig().set("SendMessagesWhenSomeoneBid", messageOnBid);
			saveConfig();
		}
		if (!getConfig().contains("BlackListedWorlds")) {
			getConfig().set("BlackListedWorlds", blacklistWorlds);
			saveConfig();
		}
		blacklistWorlds = getConfig().getStringList("BlackListedWorlds");

		if (!getConfig().contains("autoUpdate")) {
			getConfig().set("autoUpdate", true);
			saveConfig();
		}
		if (!getConfig().contains("EnableViewLastBidder")) {
			getConfig().set("EnableViewLastBidder", false);
			enableViewLastBid = false;
			saveConfig();
		} else {
			enableViewLastBid = getConfig().getBoolean("EnableViewLastBidder");
		}
		if (!getConfig().contains("Blacklist")) {
			List<String> k = new ArrayList<String>();
			k.add("BEDROCK");
			getConfig().set("Blacklist", k);
			saveConfig();
		} else {
			for (String blacklist : getConfig().getStringList("Blacklist")) {
				try {
					String[] parts = blacklist.split(":");
					String mat = parts[0];
					Material material = Material.BEDROCK;
					short data = 0;
					if (parts.length > 1)
						data = Short.parseShort(parts[1]);
					try {
						material = Material.matchMaterial(mat);
					} catch (Error | Exception e4) {
						// material = Material.getMaterial(Integer.parseInt(mat));
					}
					BlackListedItem bli = new BlackListedItem(material, data);
					this.blacklist.add(bli);
				} catch (Error | Exception r5) {
				}
			}
		}
	}

	@Override
	public void onEnable() {
		final Main m = this;
		instance = m;
		new BukkitRunnable() {

			@Override
			public void run() {
				if (!setupEconomy()) {
					getLogger().severe("- Disabled due to no Vault or no economy being found!");
					getServer().getPluginManager().disablePlugin(m);
					Bukkit.broadcastMessage(prefix
							+ "Shutting down due to missng Vault dependancy (OR YOU ARE MISSING A PLUGIN THAT ADDS THE ECONOMY, NOT VAULT)");
					return;
				}
			}
		}.runTaskLater(this, 20);

		reloadVals();
		try {
			Bukkit.getPluginManager().registerEvents(new VillagerAuction(), this);
		} catch (Error | Exception e) {
		}
		try {
			NPCAuctionCommand npcc = new NPCAuctionCommand(this);
			getCommand("NPCAuction").setExecutor(npcc);
			getCommand("NPCAuction").setTabCompleter(npcc);
		} catch (Error | Exception e) {
			e.printStackTrace();
		}

		for (int i = 0; i < 20; i++) {
			gui[i] = Bukkit.createInventory(null, 54, s_Title + s_Menu_page + (i + 1));
		}
		if (getConfig().contains("Auctions"))
			for (String owners : getConfig().getConfigurationSection("Auctions").getKeys(false)) {
				for (String ids : getConfig().getConfigurationSection("Auctions." + owners).getKeys(false)) {
					UUID auctionUUID = null;
					try {
						auctionUUID = UUID.fromString(ids);
					} catch (Error | Exception e5) {
						auctionUUID = UUID.randomUUID();
					}
					UUID uuid = UUID.fromString(owners);
					Auction a = new Auction(
							(ItemStack) getConfig().get("Auctions." + owners + "." + ids + "." + "item"), uuid,
							Bukkit.getOfflinePlayer(uuid).getName(), auctionUUID);

					if (!Bukkit.getOfflinePlayer(uuid).isOnline())
						a.ownerOnline = false;

					a.currentPrice = getConfig().getDouble("Auctions." + owners + "." + ids + "." + "price");
					a.buyitnow = getConfig().getDouble("Auctions." + owners + "." + ids + "." + "buyitnow");
					a.biddingPrice = getConfig().getDouble("Auctions." + owners + "." + ids + "." + "increase");
					if (getConfig().contains("Auctions." + owners + "." + ids + "." + "lastbidder"))
						a.lastBid = UUID.fromString(
								getConfig().getString("Auctions." + owners + "." + ids + "." + "lastbidder"));
					a.ticksLeft = getConfig().contains("Auctions." + owners + "." + ids + "." + "timeleft")
							? getConfig().getInt("Auctions." + owners + "." + ids + "." + "timeleft") * 5
							: getConfig().getInt("Auctions." + owners + "." + ids + "." + "timeleftInTicks");
					auctions.add(a);
				}
			}
		// getConfig().set("Auctions", null);

		if (getConfig().contains("NPCS"))
			for (String npcuuids : getConfig().getConfigurationSection("NPCS").getKeys(false)) {
				try {
					tpbackto.put(UUID.fromString(npcuuids), (Location) getConfig().get("NPCS." + npcuuids));
				} catch (Error | Exception e34) {
				}
			}

		Bukkit.getPluginManager().registerEvents(this, this);
		new BukkitRunnable() {
			List<Auction> ending = new ArrayList<>();

			@Override
			public void run() {
				for (Auction a : auctions) {
					if (a.tickAuc()) {
						ending.add(a);
						win(a);
					}
				}
				if (ending.size() > 0) {
					for (Auction a : ending) {
						getConfig().set("Auctions." + a.owner.toString() + "." + a.auctionID.toString(), null);
						saveConfig();
						auctions.remove(a);
					}
					ending.clear();
				}

				for (int i = 0; i < 20; i++) {
					if (gui[i].getViewers().size() > 0)
						updatePage(gui[i], i, i < Math.min(20, (auctions.size() / 36)), i > 0);
				}
			}
		}.runTaskTimer(this, 1, (long) (refreshRate * 20));

		// Download the API dependancy
		if (Bukkit.getPluginManager().getPlugin("PluginConstructorAPI") == null)
			// new DependencyDownloader(this, 276723);
			GithubDependDownloader.autoUpdate(this,
					new File(getDataFolder().getParentFile(), "PluginConstructorAPI.jar"), "ZombieStriker",
					"PluginConstructorAPI", "PluginConstructorAPI.jar");

		new Metrics(this);
		if (getConfig().getBoolean("autoUpdate"))
			GithubUpdater.autoUpdate(this, "ZombieStriker", "NPCAuctions", "NPCAuctions.jar");
		// new Updater(this, 277093, getConfig().getBoolean("auto-update"));

	}

	public void win(Auction a) {
		win(a, false);
	}

	@SuppressWarnings("unchecked")
	public void win(Auction a, boolean withBuyItNow) {
		OfflinePlayer creator = Bukkit.getOfflinePlayer(a.owner);
		if (a.is == null) {
			return;
		}
		if (a.lastBid == null) {
			if (creator.isOnline()) {
				if (blacklistWorlds.contains(((Player) creator).getWorld().getName())) {
					((Player) creator).sendMessage(prefix + s_holdingWonAuction);
					List<ItemStack> items = (List<ItemStack>) getConfig().get(a.owner.toString() + ".recievedItems");
					if (items == null)
						items = new ArrayList<ItemStack>();
					items.add(a.is);
					getConfig().set(a.owner.toString() + ".recievedItems", items);
					saveConfig();
				} else {
					((Player) creator).sendMessage(prefix + s_AuctionEneededNoBids.replace("%item%",
							(a.is.getItemMeta().hasDisplayName() ? a.is.getItemMeta().getDisplayName()
									: a.is.getType().name()) + ".x." + a.is.getAmount()));
					if (((Player) creator).getInventory().firstEmpty() == -1) {
						((Player) creator).getWorld().dropItem(((Player) creator).getLocation(), a.is);
					} else {
						((Player) creator).getInventory().addItem(a.is);
					}
				}
			} else {
				List<ItemStack> items = (List<ItemStack>) getConfig().get(a.owner.toString() + ".recievedItems");
				if (items == null)
					items = new ArrayList<ItemStack>();
				items.add(a.is);
				getConfig().set(a.owner.toString() + ".recievedItems", items);
				saveConfig();
			}
		} else {
			OfflinePlayer lastbid = Bukkit.getOfflinePlayer(a.lastBid);
			// econ.withdrawPlayer(lastbid, a.currentPrice);
			// TODO: Commenting out. In order to make sure the user cant spend
			// this money elsewhere after they bid, the money will be
			// automatically removed when they bid.

			econ.depositPlayer(creator, withBuyItNow ? a.buyitnow : a.currentPrice);
			if (!creator.isOnline()) {
				double i = (withBuyItNow ? a.buyitnow : a.currentPrice);
				if (getConfig().contains(a.owner.toString() + ".offlineAmount"))
					i += getConfig().getDouble(a.owner.toString() + ".offlineAmount");
				getConfig().set(a.owner.toString() + ".offlineAmount", i);
				saveConfig();
			}
			getConfig().set("Auctions." + a.owner.toString() + "." + a.auctionID.toString(), null);
			saveConfig();
			if (lastbid.isOnline()) {
				if (blacklistWorlds.contains(((Player) lastbid).getWorld().getName())) {
					((Player) lastbid).sendMessage(prefix + s_holdingWonAuction);
					List<ItemStack> items = (List<ItemStack>) getConfig().get(a.owner.toString() + ".recievedItems");
					if (items == null)
						items = new ArrayList<ItemStack>();
					items.add(a.is);
					getConfig().set(a.lastBid.toString() + ".recievedItems", items);
					saveConfig();
				} else {
					((Player) lastbid).sendMessage(prefix + s_WonAuction.replace("%item%",
							(a.is.getItemMeta().hasDisplayName() ? a.is.getItemMeta().getDisplayName()
									: a.is.getType().name()) + ".x." + a.is.getAmount()));
					if (((Player) lastbid).getInventory().firstEmpty() == -1) {
						((Player) lastbid).getWorld().dropItem(((Player) lastbid).getLocation(), a.is);
					} else {
						((Player) lastbid).getInventory().addItem(a.is);
					}
				}
			} else {
				List<ItemStack> items = (List<ItemStack>) getConfig().get(a.lastBid.toString() + ".recievedItems");
				if (items == null)
					items = new ArrayList<ItemStack>();
				items.add(a.is);
				getConfig().set(a.lastBid.toString() + ".recievedItems", items);
				saveConfig();
			}
			if (creator.isOnline()) {
				((Player) creator).sendMessage(prefix
						+ s_WonEarning.replaceAll("%price%", withBuyItNow ? a.buyitnow + "" : a.currentPrice + ""));
			}
		}
	}

	public Inventory updatePage(Inventory in, int page, boolean nextPage, boolean prevPage) {

		for (int i = 0; i < 9; i++) {
			in.setItem(i, bufferstack);
			in.setItem(53 - i, bufferstack);
		}
		in.setItem(4, addAuc);
		in.setItem(5, cancelAuc);
		if (!blacklistWorlds.isEmpty())
			in.setItem(8, collectAuc);
		if (prevPage)
			in.setItem(52, prevpage);
		if (nextPage)
			in.setItem(53, nextpage);

		for (int i = 0; i < 36; i++) {
			if (i < Math.min(36, auctions.size() - (page * 36))) {
				Auction a = auctions.get(i + (page * 36));
				in.setItem(i + 9, c(a));
			} else {
				in.setItem(i + 9, new ItemStack(Material.AIR));
			}
		}

		return in;
	}

	@EventHandler
	public void onJoin(final PlayerJoinEvent e) {
		if (!blacklistWorlds.contains(e.getPlayer().getWorld().getName())) {
			new BukkitRunnable() {
				@SuppressWarnings("unchecked")
				@Override
				public void run() {
					boolean save = false;
					if (getConfig().contains(e.getPlayer().getUniqueId().toString() + ".offlineAmount")) {
						e.getPlayer().sendMessage(prefix + s_rejoin_amount.replace("%amount%",
								"" + getConfig().getDouble(e.getPlayer().getUniqueId().toString() + ".offlineAmount")));
						getConfig().set(e.getPlayer().getUniqueId().toString() + ".offlineAmount", null);
						save = true;
					}
					if (getConfig().contains(e.getPlayer().getUniqueId().toString() + ".recievedItems")) {
						List<ItemStack> items = (List<ItemStack>) getConfig()
								.get(e.getPlayer().getUniqueId().toString() + ".recievedItems");
						StringBuilder sb = new StringBuilder();
						for (ItemStack is : items) {
							if (is == null)
								continue;
							if (e.getPlayer().getInventory().firstEmpty() == -1) {
								e.getPlayer().getWorld().dropItem(e.getPlayer().getLocation(), is);
							} else {
								e.getPlayer().getInventory().addItem(is);
							}
							if (is.hasItemMeta() && is.getItemMeta().hasDisplayName()) {
								sb.append("\"" + is.getItemMeta().getDisplayName() + "\", ");
							} else {
								sb.append(
										is.getType().name() + (is.getAmount() > 1 ? ":" + is.getAmount() : "") + ", ");
							}
						}
						e.getPlayer().sendMessage(prefix + s_rejoin_items + sb.toString());
						getConfig().set(e.getPlayer().getUniqueId().toString() + ".recievedItems", null);
						save = true;
					}
					if (save)
						saveConfig();
				}
			}.runTaskLater(this, 2);
		}
	}

	@EventHandler
	public void price(AsyncPlayerChatEvent e) {
		if (auctionWaitingMap.containsKey(e.getPlayer().getUniqueId())) {
			e.setCancelled(true);
			if (!auctionWaitingStage.containsKey(e.getPlayer().getUniqueId())) {
				try {
					double i = Double.parseDouble(e.getMessage());
					if (i < 0)
						i = -i;
					auctionWaitingMap.get(e.getPlayer().getUniqueId()).currentPrice = i;
					auctionWaitingStage.put(e.getPlayer().getUniqueId(), 1);
					e.getPlayer().sendMessage(prefix + s_BidIncrease.replaceAll("%min%", increaseMin + "")
							.replaceAll("%max%", increaseMax + ""));
				} catch (Exception e2) {
					e.getPlayer().sendMessage(prefix + s_cancelUnfinAuction + "  : " + e.getMessage());
					e.getPlayer().getInventory().addItem(auctionWaitingMap.get(e.getPlayer().getUniqueId()).is);
					auctionWaitingMap.remove(e.getPlayer().getUniqueId());
				}
			} else if (auctionWaitingStage.get(e.getPlayer().getUniqueId()) == 1) {
				try {
					Double i = Double.parseDouble(e.getMessage());
					if (i > increaseMax)
						i = increaseMax;
					if (i < increaseMin)
						i = increaseMin;
					auctionWaitingMap.get(e.getPlayer().getUniqueId()).biddingPrice = i;
					auctionWaitingStage.put(e.getPlayer().getUniqueId(), 2);
					e.getPlayer().sendMessage(prefix + s_BidDurration);

				} catch (Exception e2) {
					e.getPlayer().sendMessage(prefix + s_cancelUnfinAuction + "  : " + e.getMessage());
					e.getPlayer().getInventory().addItem(auctionWaitingMap.get(e.getPlayer().getUniqueId()).is);
					auctionWaitingMap.remove(e.getPlayer().getUniqueId());
					auctionWaitingStage.remove(e.getPlayer().getUniqueId());
				}
			} else if (auctionWaitingStage.get(e.getPlayer().getUniqueId()) == 2) {
				try {
					int hours = 0;
					int minutes = 0;
					int seconds = 0;
					String lowercase = e.getMessage().toLowerCase().trim();
					Pattern pat = Pattern.compile("[hms]");
					Matcher m = pat.matcher(lowercase);
					if (m.find()) {
						if (!(lowercase.contains("h") || lowercase.contains("m") || lowercase.contains("s"))) {
							String[] parts = lowercase.split(" ");
							if (parts.length > 0)
								hours = Integer.parseInt(parts[0]);
							if (parts.length > 1)
								minutes = Integer.parseInt(parts[1]);
							if (parts.length > 2)
								seconds = Integer.parseInt(parts[2]);
						} else {
							if (lowercase.contains("h")) {
								hours = Integer.parseInt(lowercase.split("h")[0].trim());
								if (lowercase.split("h").length >= 2)
									lowercase = lowercase.split("h")[1];
							}
							if (lowercase.contains("m")) {
								minutes = Integer.parseInt(lowercase.split("m")[0].trim());
								if (lowercase.split("m").length >= 2)
									lowercase = lowercase.split("m")[1];
							}
							if (lowercase.contains("s")) {
								seconds = Integer.parseInt(lowercase.split("s")[0].trim());
								if (lowercase.split("s").length >= 2)
									lowercase = lowercase.split("s")[1];
							}
						}
					} else {
						minutes = Integer.parseInt(e.getMessage());
					}

					if (minutes < 0)
						minutes = -minutes;
					if (hours < 0)
						hours = -hours;
					if (seconds < 0)
						seconds = -seconds;
					int ticks = ((((hours * 60) + minutes) * 60) + seconds) * 20;
					if (ticks > s_MAX_BID_TIME * 60 * 60 * 20)
						ticks = s_MAX_BID_TIME * 60 * 60 * 20;

					auctionWaitingMap.get(e.getPlayer().getUniqueId()).setWait(ticks);

					auctionWaitingStage.put(e.getPlayer().getUniqueId(), 3);
					e.getPlayer().sendMessage(prefix + s_BidBuyItNow);
					e.getPlayer().sendMessage(prefix + s_buyitnowoptional);

				} catch (Exception e2) {
					e2.printStackTrace();
					e.getPlayer().sendMessage(prefix + s_cancelUnfinAuction + "  : " + e.getMessage());
					e.getPlayer().getInventory().addItem(auctionWaitingMap.get(e.getPlayer().getUniqueId()).is);
					auctionWaitingMap.remove(e.getPlayer().getUniqueId());
					auctionWaitingStage.remove(e.getPlayer().getUniqueId());
				}
			} else if (auctionWaitingStage.get(e.getPlayer().getUniqueId()) == 3) {
				try {
					double k = auctionWaitingMap.get(e.getPlayer().getUniqueId()).buyitnow = Math
							.abs(Double.parseDouble(e.getMessage()));
					e.getPlayer().sendMessage(prefix + s_buyitnowset.replaceAll("%price%", k + ""));
				} catch (Exception e2) {
				}
				Auction aa = auctionWaitingMap.get(e.getPlayer().getUniqueId());
				auctions.add(aa);
				auctionWaitingMap.remove(e.getPlayer().getUniqueId());
				auctionWaitingStage.remove(e.getPlayer().getUniqueId());
				e.getPlayer().sendMessage(prefix + s_addedToAuctionHouse);
				saveAuctionNoSave(aa);
				saveConfig();
				if (enableBroadcasting) {
					Bukkit.broadcastMessage(prefix + s_broadcastMessage.replaceAll("%player%", e.getPlayer().getName())
							.replaceAll("%amount%", "" + aa.is.getAmount())
							.replaceAll("%material%",
									"" + ((aa.is.hasItemMeta() && aa.is.getItemMeta().hasDisplayName())
											? aa.is.getItemMeta().getDisplayName()
											: aa.is.getType().name()))
							.replaceAll("%cost%", aa.currentPrice + ""));
				}
			}
		}
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		for (Auction a : auctions)
			if (a.owner.equals(e.getPlayer().getUniqueId()))
				a.ownerOnline = false;
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onClick(InventoryClickEvent e) {
		try {
			if (e.getClickedInventory() == null)
				return;
		} catch (Error | Exception e2) {
			if (e.getInventory() == null)
				return;
		}
		if (e.getWhoClicked().getOpenInventory().getTitle().startsWith(s_Title)) {
			e.setCancelled(true);
			try {
				if (!e.getClickedInventory().equals(e.getWhoClicked().getOpenInventory().getTopInventory()))
					return;
			} catch (Error | Exception e2) {
				if (!e.getInventory().equals(e.getWhoClicked().getOpenInventory().getTopInventory()))
					return;
			}
			try {
				if (e.getClickedInventory().getItem(e.getSlot()) == null)
					return;
			} catch (Error | Exception e2) {
				if (e.getInventory().getItem(e.getSlot()) == null)
					return;
			}

			boolean slotcheck = false;
			try {
				slotcheck = e.getClickedInventory().getItem(e.getSlot()) != null;
			} catch (Error | Exception e2) {
				slotcheck = e.getInventory().getItem(e.getSlot()) != null;
			}

			boolean buffercheck = false;
			try {
				buffercheck = e.getClickedInventory().getItem(e.getSlot()).isSimilar(bufferstack);
			} catch (Error | Exception e2) {
				buffercheck = e.getInventory().getItem(e.getSlot()).isSimilar(bufferstack);
			}

			if (e.getSlot() == 4) {
				int amountOfAuc = 0;
				for (Auction a : auctions) {
					if (a.owner.equals(e.getWhoClicked().getUniqueId()))
						amountOfAuc++;
				}
				if (limitAmount && (!e.getWhoClicked().hasPermission("npcauctions.bypasslimit"))) {
					for (int i = 100; i >= 0; i--) {
						if (e.getWhoClicked().hasPermission("npcauctions.auctionlimit." + i)) {
							if (amountOfAuc < i) {
								break;
							} else {
								e.getWhoClicked().sendMessage(prefix + s_overlimit);
								return;
							}
						}
						if (i == 0) {
							e.getWhoClicked().sendMessage(prefix + s_NOPERM);
							return;
						}
					}
				}
				Inventory newInv = Bukkit.createInventory(null, 9, s_InvTitleAdd);
				e.getWhoClicked().openInventory(newInv);
			} else if (e.getSlot() == 5) {

				List<ItemStack> cc = new ArrayList<>();
				int am = 0;

				for (Auction a : new ArrayList<>(auctions)) {
					if (a.owner.equals(e.getWhoClicked().getUniqueId())) {
						cc.add(c(a));
						am++;
					}
				}
				Inventory newInv = Bukkit.createInventory(null, (((am / 9) + 1) * 9), s_InvTitleCancel);
				for (ItemStack is : cc)
					newInv.addItem(is);
				e.getWhoClicked().openInventory(newInv);

			} else if (e.getSlot() == 8) {
				if (!blacklistWorlds.contains(e.getWhoClicked().getWorld().getName())) {
					boolean save = false;
					if (getConfig().contains(e.getWhoClicked().getUniqueId().toString() + ".offlineAmount")) {
						e.getWhoClicked().sendMessage(prefix + s_rejoin_amount.replace("%amount%", "" + getConfig()
								.getDouble(e.getWhoClicked().getUniqueId().toString() + ".offlineAmount")));
						getConfig().set(e.getWhoClicked().getUniqueId().toString() + ".offlineAmount", null);
						save = true;
					}
					if (getConfig().contains(e.getWhoClicked().getUniqueId().toString() + ".recievedItems")) {
						@SuppressWarnings("unchecked")
						List<ItemStack> items = (List<ItemStack>) getConfig()
								.get(e.getWhoClicked().getUniqueId().toString() + ".recievedItems");
						StringBuilder sb = new StringBuilder();
						for (ItemStack is : items) {
							if (is == null)
								continue;
							if (e.getWhoClicked().getInventory().firstEmpty() == -1) {
								e.getWhoClicked().getWorld().dropItem(e.getWhoClicked().getLocation(), is);
							} else {
								e.getWhoClicked().getInventory().addItem(is);
							}
							if (is.hasItemMeta() && is.getItemMeta().hasDisplayName()) {
								sb.append("\"" + is.getItemMeta().getDisplayName() + "\", ");
							} else {
								sb.append(
										is.getType().name() + (is.getAmount() > 1 ? ":" + is.getAmount() : "") + ", ");
							}
						}
						e.getWhoClicked().sendMessage(prefix + s_CLAIM_items + sb.toString());
						getConfig().set(e.getWhoClicked().getUniqueId().toString() + ".recievedItems", null);
						save = true;
					}
					if (save)
						saveConfig();
				}

			} else if (e.getSlot() == 52 && (slotcheck)) {
				int k = (Integer.parseInt(e.getWhoClicked().getOpenInventory().getTitle().split(s_Menu_page)[1].trim())
						- 1) - 1;
				if (k >= 0) {
					updatePage(gui[k], k, k < Math.min(20, (auctions.size() / 36)), k > 0);
					e.getWhoClicked().openInventory(gui[k]);
				}
			} else if (e.getSlot() == 53 && (slotcheck) && (!buffercheck)) {
				int k = (Integer.parseInt(e.getWhoClicked().getOpenInventory().getTitle().split(s_Menu_page)[1].trim())
						- 1) + 1;
				if (k < gui.length) {
					updatePage(gui[k], k, k < Math.min(20, (auctions.size() / 36)), k > 0);
					e.getWhoClicked().openInventory(gui[k]);
				}

			} else if (buffercheck) {
			} else {
				try {
					ItemStack aak = null;
					try {
						aak = e.getClickedInventory().getItem(e.getSlot());
					} catch (Error | Exception e2) {
						aak = e.getInventory().getItem(e.getSlot());
					}
					if (aak != null) {
						if (!aak.hasItemMeta() || !aak.getItemMeta().hasLore()) {
							e.setCancelled(true);
							return;
						}
						UUID k = UUID.fromString(ChatColor.stripColor(aak.getItemMeta().getLore().get(0)));
						Auction aa = null;
						for (Auction a : auctions) {
							if (a.auctionID.equals(k)) {
								aa = a;
								break;
							}
						}
						if (aa == null) {
							e.setCancelled(true);
							return;
						}

						if (aa.owner == null) {
							e.getWhoClicked().sendMessage(
									prefix + "The owner of the auction is somehow null. Don't know how that can be.");
						} else if (aa.owner.equals(e.getWhoClicked().getUniqueId())) {
							e.getWhoClicked().sendMessage(prefix + s_cannotbidown);
						} else {
							if (aa.hasBuyItNow()) {
								if (e.isRightClick()) {
									if (econ.getBalance((OfflinePlayer) e.getWhoClicked()) >= aa.buyitnow) {
										if (aa.owner.equals(e.getWhoClicked().getUniqueId())) {
											e.getWhoClicked().sendMessage(prefix + s_cannotbidown);
											return;
										}
										if (aa.lastBid != null) {
											OfflinePlayer ofp = Bukkit.getOfflinePlayer(aa.lastBid);
											econ.depositPlayer(ofp, aa.biddingPrice);
											if (ofp.isOnline()) {
												((Player) ofp).sendMessage(prefix + s_outBid.replace("%item%",
														(aa.is.getItemMeta().hasDisplayName()
																? aa.is.getItemMeta().getDisplayName()
																: aa.is.getType().name()) + ".x." + aa.is.getAmount()));
											}
										}
										Player ofowner = Bukkit.getPlayer(aa.owner);
										if (ofowner != null) {
											if (messageOnBid)
												ofowner.sendMessage(prefix + s_someoneBought
														.replace("%player%", e.getWhoClicked().getName())
														.replace("%amount%", "" + aa.buyitnow).replace("%item%",
																(aa.is.getItemMeta().hasDisplayName()
																		? aa.is.getItemMeta().getDisplayName()
																		: aa.is.getType().name()) + ".x."
																		+ aa.is.getAmount()));
										}
										// public static String s_someoneBid = "%player% has bid %amount% for your
										// %item% auction";

										aa.lastBid = e.getWhoClicked().getUniqueId();
										econ.withdrawPlayer(((Player) e.getWhoClicked()), aa.buyitnow);
										e.getWhoClicked().sendMessage(prefix
												+ s_youBought.replace("%amount%", "" + aa.buyitnow).replace("%item%",
														(aa.is.getItemMeta().hasDisplayName()
																? aa.is.getItemMeta().getDisplayName()
																: aa.is.getType().name()) + ".x." + aa.is.getAmount()));
										win(aa, true);
										auctions.remove(aa);
									}
									return;
								}
							}
							if (aa.lastBid != null && aa.lastBid.equals(e.getWhoClicked().getUniqueId())) {
								e.getWhoClicked().sendMessage(prefix + s_cancelAlreadyBid);
								return;
							}
							if (econ.getBalance((OfflinePlayer) e.getWhoClicked()) >= aa.currentPrice
									+ aa.biddingPrice) {

								if (aa.owner.equals(e.getWhoClicked().getUniqueId())) {
									e.getWhoClicked().sendMessage(prefix + s_cannotbidown);
									return;
								}
								if (aa.lastBid != null) {
									OfflinePlayer ofp = Bukkit.getOfflinePlayer(aa.lastBid);
									econ.depositPlayer(ofp, aa.currentPrice);
									if (ofp.isOnline()) {
										((Player) ofp).sendMessage(prefix + s_outBid.replace("%item%",
												(aa.is.getItemMeta().hasDisplayName()
														? aa.is.getItemMeta().getDisplayName()
														: aa.is.getType().name()) + ".x." + aa.is.getAmount()));
									}
								}

								Player ofowner = Bukkit.getPlayer(aa.owner);
								aa.currentPrice += aa.biddingPrice;
								if (ofowner != null) {
									if (messageOnBid)
										ofowner.sendMessage(prefix + s_someoneBid
												.replace("%player%", e.getWhoClicked().getName())
												.replace("%amount%", "" + aa.currentPrice)
												.replace("%bid%", "" + aa.biddingPrice).replace("%item%",
														(aa.is.getItemMeta().hasDisplayName()
																? aa.is.getItemMeta().getDisplayName()
																: aa.is.getType().name()) + ".x." + aa.is.getAmount()));
								}
								aa.lastBid = e.getWhoClicked().getUniqueId();
								econ.withdrawPlayer(((Player) e.getWhoClicked()), aa.currentPrice);
								e.getWhoClicked()
										.sendMessage(prefix + s_youBid.replace("%amount%", "" + aa.currentPrice)
												.replace("%bid%", "" + aa.biddingPrice).replace("%item%",
														(aa.is.getItemMeta().hasDisplayName()
																? aa.is.getItemMeta().getDisplayName()
																: aa.is.getType().name()) + ".x." + aa.is.getAmount()));
							}
						}
					}
				} catch (Exception e2) {
					e2.printStackTrace();
				}

			}
		} else if (e.getWhoClicked().getOpenInventory().getTitle().equalsIgnoreCase(s_InvTitleCancel)) {
			e.setCancelled(true);
			try {
				if (!e.getClickedInventory().equals(e.getWhoClicked().getOpenInventory().getTopInventory()))
					return;
			} catch (Error | Exception e2) {
				if (!e.getInventory().equals(e.getWhoClicked().getOpenInventory().getTopInventory()))
					return;
			}

			try {
				ItemStack aak = null;
				try {
					aak = e.getCurrentItem();// .getClickedInventory().getItem(e.getSlot());
				} catch (Error | Exception e2) {
					aak = e.getInventory().getItem(e.getSlot());
				}
				if (aak != null) {
					if (!aak.hasItemMeta() || !aak.getItemMeta().hasLore()) {
						e.setCancelled(true);
						return;
					}
					UUID k = UUID.fromString(ChatColor.stripColor(aak.getItemMeta().getLore().get(0)));
					Auction aa = null;
					for (Auction a : auctions) {
						if (a.auctionID.equals(k)) {
							aa = a;
							break;
						}
					}
					if (aa == null) {
						e.setCancelled(true);
						return;
					}
					if (aa.owner == null) {
						e.getWhoClicked().sendMessage(
								prefix + "The owner of the auction is somehow null. Don't know how that can be.");
					} else if (aa.owner.equals(e.getWhoClicked().getUniqueId())) {
						e.getWhoClicked().closeInventory();
						e.getWhoClicked().sendMessage(prefix + s_cancelGen);
						e.getWhoClicked().getInventory().addItem(aa.is);
						if (aa.lastBid != null) {
							econ.depositPlayer(Bukkit.getOfflinePlayer(aa.lastBid), aa.currentPrice);
							if (Bukkit.getPlayer(aa.lastBid) != null)
								Bukkit.getPlayer(aa.lastBid)
										.sendMessage(prefix + s_auctionCancelRefund
												.replace("%amount%", "" + aa.currentPrice).replace("%item%",
														(aa.is.getItemMeta().hasDisplayName()
																? aa.is.getItemMeta().getDisplayName()
																: aa.is.getType().name()) + ".x." + aa.is.getAmount()));
						}
						auctions.remove(aa);
						getConfig().set("Auctions." + aa.owner.toString() + "." + aa.auctionID.toString(), null);
						saveConfig();
					}
				}
			} catch (Exception e2) {
				e2.printStackTrace();
			}

		} else if (e.getWhoClicked().getOpenInventory().getTitle().equalsIgnoreCase(s_InvTitleAdd)) {
			boolean itemnull = false;
			ItemStack slotis = null;
			try {
				if (e.getClickedInventory().getSize() <= e.getSlot() || e.getSlot() < 0)
					return;
			} catch (Error | Exception er) {
				if (e.getView().getBottomInventory().getSize() <= e.getSlot() || e.getSlot() < 0)
					return;
			}

			try {
				itemnull = e.getClickedInventory().getItem(e.getSlot()) != null;
				slotis = e.getClickedInventory().getItem(e.getSlot());
			} catch (Error | Exception er) {
				itemnull = e.getView().getBottomInventory().getItem(e.getSlot()) != null;
				slotis = e.getView().getBottomInventory().getItem(e.getSlot());
			}
			if (itemnull) {
				for (BlackListedItem bli : blacklist) {
					if (bli.getMat() == slotis.getType()
							&& (bli.getData() == -1 || bli.getData() == slotis.getDurability())) {
						e.getWhoClicked().sendMessage(s_blacklistedmaterial);
						e.setCancelled(true);
						return;
					}
				}

				Auction a = new Auction(slotis, e.getWhoClicked().getUniqueId(), e.getWhoClicked().getName());
				e.getWhoClicked().getInventory().setItem(e.getWhoClicked().getInventory().first(slotis), null);

				if (auctionWaitingMap.containsKey(e.getWhoClicked().getUniqueId())) {
					e.getWhoClicked().getInventory().addItem(auctionWaitingMap.get(e.getWhoClicked().getUniqueId()).is);
					e.getWhoClicked().sendMessage(prefix + ChatColor.RED + s_cancelExisting);
				}

				e.getWhoClicked().closeInventory();
				e.getWhoClicked().sendMessage(prefix + s_BidStarting);

				auctionWaitingMap.put(e.getWhoClicked().getUniqueId(), a);
			}
		} else {
			if (e.getWhoClicked().getOpenInventory().getTitle().equalsIgnoreCase(s_InvTitleAdd))
				e.setCancelled(true);
		}
	}

	public ItemStack c(Auction a) {
		ItemStack temp = a.is.clone();
		ItemMeta temp2;
		if (temp != null && temp.getItemMeta() != null)
			temp2 = temp.getItemMeta();
		else {
			return new ItemStack(Material.AIR);
		}

		double time = (((double) a.ticksLeft) / 20);
		DecimalFormat df = new DecimalFormat("0.0");

		List<String> lore = new ArrayList<>();
		lore.add(ChatColor.BLACK + "" + a.auctionID.toString());
		lore.add(ChatColor.GREEN
				+ s_lorePrice.replaceAll("%price%", a.currentPrice + "").replaceAll("%bid%", a.biddingPrice + ""));
		// lore.add(ChatColor.GREEN + "Price : [$" + a.currentPrice + "+" +
		// a.biddingPrice + "]");
		if (a.hasBuyItNow()) {
			lore.add(s_lorebuyitnow.replaceAll("%price%", a.buyitnow + ""));
			lore.add(s_lorebuyitnowhow);
		}

		String time2;
		if (time > 3600) {
			time2 = ((int) (time / 3600)) + "h, " + ((int) (time / 60) - (((int) (time / 3600)) * 60)) + "m, "
					+ df.format(time % 60) + "s";

		} else if (time > 60) {
			time2 = +((int) (time / 60)) + "m, " + df.format(time % 60) + "s";
		} else {
			time2 = df.format(time % 60) + "s";
		}

		lore.add(((a.ticksLeft / 20) < 60 ? ChatColor.RED : ChatColor.GOLD) + s_loretime + time2);
		lore.add((a.ownerOnline ? ChatColor.GREEN : ChatColor.RED) + s_loreowner + a.ownerName);
		if (enableViewLastBid)
			if (a.lastBid != null)
				lore.add(s_highestBidder.replace("%player%", Bukkit.getOfflinePlayer(a.lastBid).getName()));
		if (a.is != null && a.is.hasItemMeta() && a.is.getItemMeta().hasLore())
			lore.addAll(a.is.getItemMeta().getLore());
		if (temp2 != null) {
			temp2.setLore(lore);
			temp.setItemMeta(temp2);
		} else {
			Bukkit.broadcastMessage(prefix + " The auction item " + temp.getType() + " by " + a.ownerName
					+ " was invalid and has been removed.");
			auctions.remove(a);
			Player online = Bukkit.getPlayer(a.owner);
			if (online == null) {
				if (Bukkit.getOnlinePlayers().size() > 0)
					online = ((Player) Bukkit.getOnlinePlayers().toArray()[0]);
			}
			if (online != null)
				online.getInventory().addItem(a.is);
		}
		return temp;
	}

	@SuppressWarnings("deprecation")
	public ItemStack getConfigItemStack(String pathname, ItemStack base) {
		if (getConfig().contains("Items." + pathname + ".Material")) {
			Material m = Material.matchMaterial(getConfig().getString("Items." + pathname + ".Material"));
			short durib = (short) getConfig().getInt("Items." + pathname + ".data");
			ItemStack is = new ItemStack(m, 1, durib);
			return is;
		}
		getConfig().set("Items." + pathname + ".Material", base.getType().name());
		getConfig().set("Items." + pathname + ".data", base.getDurability());
		saveConfig();
		return base;
	}
}
