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

	public static String s_NOPERM = "&6 You do not have permission to use this command";
	public static String s_Title = ChatColor.AQUA + "[" + ChatColor.WHITE + "Auction House" + ChatColor.AQUA + "]";
	public static String s_InvTitleCancel = " Cancel which auction?";
	public static String s_InvTitleAdd = " What do you want to auction?";
	public static String s_AuctionEneededNoBids = " Your auction ended. No one bid.";
	public static String s_WonAuction = " You have won the Auction!";
	public static String s_WonEarning = " Your auction ended, earning you $%price%.";
	public static String s_BidIncrease = " What will be the bid increase? (Min:$1, Max:$1000)";
	public static String s_BidStarting = " What would be the starting bid?";
	public static String s_BidDurration = " And how long should the auction last? (Using the format hoursH minutesM secondsS)";
	public static String s_BidBuyItNow = "[OPTIONAL] Do you want a Buy-It-Now for your item? If so, type in the price. ";
	public static String s_addedToAuctionHouse = " Added the auction to the auction house.";
	public static String s_cancelUnfinAuction = " Canceling Auction: Invalid input.";
	public static String s_cancelExisting = " Canceling existing auction.";
	public static String s_cancelGen = " Canceling auction...";
	public static String s_cancelOwn = " You cant bid on your own auction!";
	public static String s_outBid = " You have been outbid!";

	public static String s_VillagerName = "&6[Auction House]";

	public static String s_ItemAdd = "&2[Create new Auction]";
	public static String s_ItemRemove = "&c[Cancel an Auction]";
	public static String s_ItemNex = "&2[Next Page]";
	public static String s_ItemPrev = "&c[Previous Page]";

	public static String s_loretime = "Time Remaining: ";
	public static String s_lorebuyitnow = "&a&lBuy-It-Now : [$%price%]";
	public static String s_lorebuyitnowhow = "&a&lRight click to Buy-It-Now";
	public static String s_loreowner = "Created by: ";
	public static String s_cannotbidown = "&c You cannot bid on your own auction!";
	public static String s_buyitnowoptional = "If NOT, type in \"No\" ";
	public static String s_buyitnowset = " Set the Buy-It_now to $%price%";

	public static int s_MAX_BID_TIME = 24;

	public static boolean enableBroadcasting = false;
	public static String s_broadcastMessage = " %player% is auctioning %amount% %material% starting at %cost%";
	public static boolean limitAmount = false;
	public static String s_overlimit = " You cannot auction this many items at once! Wait till one of your other auctions is finished.";

	public static Inventory[] gui = new Inventory[20];
	public static final String prefix = ChatColor.AQUA + "[" + ChatColor.WHITE + "NPCAuctions" + ChatColor.AQUA + "]"
			+ ChatColor.WHITE;

	public ItemStack bufferstack;
	public ItemStack nextpage;
	public ItemStack prevpage;

	public ItemStack addAuc;
	public ItemStack cancelAuc;

	public static Economy econ = null;

	public List<Auction> auctions = new ArrayList<>();

	public HashMap<UUID, Auction> auctionWaitingMap = new HashMap<>();

	public HashMap<UUID, Integer> auctionWaitingStage = new HashMap<UUID, Integer>();

	public static boolean USE_VILLAGERS = true;

	public static final int TICK_SPEED = 5;

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

	@Override
	public void onDisable() {
		for (Auction a : auctions) {
			getConfig().set("Auctions." + a.owner.toString() + "." + a.auctionID + "." + "price", a.currentPrice);
			getConfig().set("Auctions." + a.owner.toString() + "." + a.auctionID + "." + "buyitnow", a.buyitnow);
			getConfig().set("Auctions." + a.owner.toString() + "." + a.auctionID + "." + "increase", a.biddingPrice);
			if (a.lastBid != null)
				getConfig().set("Auctions." + a.owner.toString() + "." + a.auctionID + "." + "lastbidder",
						a.lastBid.toString());
			getConfig().set("Auctions." + a.owner.toString() + "." + a.auctionID + "." + "timeleft",
					a.quarterSecondsLeft);
			getConfig().set("Auctions." + a.owner.toString() + "." + a.auctionID + "." + "item", a.is);
		}
		saveConfig();
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onEnable() {
		final Main m = this;

		try {
			Bukkit.getPluginManager().registerEvents(new TraitEventHandler(), this);
		} catch (Exception | Error e) {
		}

		new BukkitRunnable() {

			@Override
			public void run() {
				if (!setupEconomy()) {
					getLogger().severe("- Disabled due to no Vault not being found!");
					getServer().getPluginManager().disablePlugin(m);
					Bukkit.broadcastMessage(prefix + "Shutting down due to missng Vault dependancy");
					return;
				}
			}
		}.runTaskLater(this, 20);

		if (getServer().getPluginManager().getPlugin("Citizens") == null
				|| getServer().getPluginManager().getPlugin("Citizens").isEnabled() == false) {
			getLogger().log(Level.SEVERE, "Citizens 2.0 not found or not enabled");
			USE_VILLAGERS = true;
		} else {
			USE_VILLAGERS = false;
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

			s_VillagerName = c.getMessage(Keys.VillagersName, s_VillagerName);
			s_outBid = c.getMessage(Keys.OutBid, s_outBid);

			s_ItemAdd = c.getMessage(Keys.ItemAdd, s_ItemAdd);
			s_ItemRemove = c.getMessage(Keys.ItemCancel, s_ItemRemove);
			s_ItemNex = c.getMessage(Keys.ItemNext, s_ItemNex);
			s_ItemPrev = c.getMessage(Keys.ItemPrev, s_ItemPrev);

			s_lorebuyitnow = c.getMessage(Keys.LoreBuyItNow, s_lorebuyitnow);
			s_lorebuyitnowhow = c.getMessage(Keys.loreBuyItNowHow, s_lorebuyitnowhow);
			s_loreowner = c.getMessage(Keys.LoreOwner, s_loreowner);
			s_loretime = c.getMessage(Keys.LoreTime, s_loretime);
			s_cannotbidown = c.getMessage(Keys.CAnnotBidOwnAuction, s_cannotbidown);
			s_buyitnowoptional = c.getMessage(Keys.BuyIUtNowNo, s_buyitnowoptional);
			s_buyitnowset = c.getMessage(Keys.BuyItNowSetTo, s_buyitnowset);

			enableBroadcasting = Boolean.valueOf(c.getMessage(Keys.broadcastAuction, enableBroadcasting + ""));
			s_broadcastMessage = c.getMessage(Keys.broadcastAuctionMesssage, s_broadcastMessage);
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

		try {
			Bukkit.getPluginManager().registerEvents(new VillagerAuction(this), this);
		} catch (Error | Exception e) {
		}

		try {
			getCommand("spawnAuction").setExecutor(new SACommand(this));
			getCommand("NPCAuctionEntity").setExecutor(new SA2Command());
			getCommand("removeAllAuctionHouses").setExecutor(new SA3Command());
		} catch (Error | Exception e) {
			e.printStackTrace();
		}

		bufferstack = new ItemStack(Material.STAINED_GLASS_PANE, 1, DyeColor.GRAY.getWoolData());
		ItemMeta im = bufferstack.getItemMeta();
		im.setDisplayName(" ");
		bufferstack.setItemMeta(im);

		nextpage = new ItemStack(Material.WOOL, 1, DyeColor.GREEN.getWoolData());
		ItemMeta im2 = nextpage.getItemMeta();
		im2.setDisplayName(s_ItemNex);
		nextpage.setItemMeta(im2);

		prevpage = new ItemStack(Material.WOOL, 1, DyeColor.RED.getWoolData());
		ItemMeta im3 = prevpage.getItemMeta();
		im3.setDisplayName(s_ItemPrev);
		prevpage.setItemMeta(im3);

		addAuc = new ItemStack(Material.WOOL, 1, DyeColor.BLUE.getWoolData());
		ItemMeta im4 = addAuc.getItemMeta();
		im4.setDisplayName(s_ItemAdd);
		addAuc.setItemMeta(im4);
		cancelAuc = new ItemStack(Material.WOOL, 1, DyeColor.RED.getWoolData());
		ItemMeta im5 = cancelAuc.getItemMeta();
		im5.setDisplayName(s_ItemRemove);
		cancelAuc.setItemMeta(im5);

		for (int i = 0; i < 20; i++) {
			gui[i] = Bukkit.createInventory(null, 54, s_Title + " Page: " + (i + 1));
		}
		if (getConfig().contains("Auctions"))
			for (String owners : getConfig().getConfigurationSection("Auctions").getKeys(false)) {
				for (String ids : getConfig().getConfigurationSection("Auctions." + owners).getKeys(false)) {
					UUID uuid = UUID.fromString(owners);
					Auction a = new Auction(
							(ItemStack) getConfig().get("Auctions." + owners + "." + ids + "." + "item"), uuid,
							Bukkit.getOfflinePlayer(uuid).getName(), Integer.parseInt(ids));

					if (!Bukkit.getOfflinePlayer(uuid).isOnline())
						a.ownerOnline = false;

					a.currentPrice = getConfig().getInt("Auctions." + owners + "." + ids + "." + "price");
					a.buyitnow = getConfig().getInt("Auctions." + owners + "." + ids + "." + "buyitnow");
					a.biddingPrice = getConfig().getInt("Auctions." + owners + "." + ids + "." + "increase");
					if (getConfig().contains("Auctions." + owners + "." + ids + "." + "lastbidder"))
						a.lastBid = UUID.fromString(
								getConfig().getString("Auctions." + owners + "." + ids + "." + "lastbidder"));
					a.quarterSecondsLeft = getConfig().getInt("Auctions." + owners + "." + ids + "." + "timeleft");
					auctions.add(a);
				}
			}

		getConfig().set("Auctions", null);
		saveConfig();

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
					for (Auction a : ending)
						auctions.remove(a);
					ending.clear();
				}
				for (int i = 0; i < 20; i++)
					updatePage(gui[i], i, i < Math.min(20, (auctions.size() / 36)), i > 0);
			}
		}.runTaskTimer(this, 1, 4);

		// Download the API dependancy
		if (Bukkit.getPluginManager().getPlugin("PluginConstructorAPI") == null)
			// new DependencyDownloader(this, 276723);
			GithubDependDownloader.autoUpdate(this,
					new File(getDataFolder().getParentFile(), "PluginConstructorAPI.jar"), "ZombieStriker",
					"PluginConstructorAPI", "PluginConstructorAPI.jar");

		new Metrics(this);
		GithubUpdater.autoUpdate(this, "ZombieStriker", "NPCAuctions", "NPCAuctions.jar");
		// new Updater(this, 277093, getConfig().getBoolean("auto-update"));

	}

	@SuppressWarnings("unchecked")
	public void win(Auction a) {
		OfflinePlayer creator = Bukkit.getOfflinePlayer(a.owner);
		if (a.lastBid == null) {
			if (creator.isOnline()) {
				((Player) creator).sendMessage(prefix + s_AuctionEneededNoBids);
				((Player) creator).getInventory().addItem(a.is);
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
			econ.depositPlayer(creator, a.currentPrice);

			if (lastbid.isOnline()) {
				((Player) lastbid).sendMessage(prefix + s_WonAuction);
				((Player) lastbid).getInventory().addItem(a.is);
			} else {
				List<ItemStack> items = (List<ItemStack>) getConfig().get(a.owner.toString() + ".recievedItems");
				if (items == null)
					items = new ArrayList<ItemStack>();
				items.add(a.is);
				getConfig().set(a.lastBid.toString() + ".recievedItems", items);
				saveConfig();
			}
			if (creator.isOnline()) {
				((Player) creator).sendMessage(prefix + s_WonEarning.replaceAll("%price%", a.currentPrice + ""));
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
		new BukkitRunnable() {
			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				if (getConfig().contains(e.getPlayer().getUniqueId().toString() + ".recievedItems")) {
					List<ItemStack> items = (List<ItemStack>) getConfig()
							.get(e.getPlayer().getUniqueId().toString() + ".recievedItems");
					for (ItemStack is : items)
						e.getPlayer().getInventory().addItem(is);
					getConfig().set(e.getPlayer().getUniqueId().toString() + ".recievedItems", null);
					saveConfig();
				}
			}
		}.runTaskLater(this, 2);
	}

	@EventHandler
	public void price(AsyncPlayerChatEvent e) {
		if (auctionWaitingMap.containsKey(e.getPlayer().getUniqueId())) {
			e.setCancelled(true);
			if (!auctionWaitingStage.containsKey(e.getPlayer().getUniqueId())) {
				try {
					Integer i = Integer.parseInt(e.getMessage());
					if (i < 0)
						i = -i;
					auctionWaitingMap.get(e.getPlayer().getUniqueId()).currentPrice = i;
					auctionWaitingStage.put(e.getPlayer().getUniqueId(), 1);
					e.getPlayer().sendMessage(prefix + s_BidIncrease);
				} catch (Exception e2) {
					e.getPlayer().sendMessage(prefix + s_cancelUnfinAuction+"  : "+e.getMessage());
					e.getPlayer().getInventory().addItem(auctionWaitingMap.get(e.getPlayer().getUniqueId()).is);
					auctionWaitingMap.remove(e.getPlayer().getUniqueId());
				}
			} else if (auctionWaitingStage.get(e.getPlayer().getUniqueId()) == 1) {
				try {
					Integer i = Integer.parseInt(e.getMessage());
					if (i > 1000)
						i = 1000;
					if (i < 1)
						i = 1;
					auctionWaitingMap.get(e.getPlayer().getUniqueId()).biddingPrice = i;
					auctionWaitingStage.put(e.getPlayer().getUniqueId(), 2);
					e.getPlayer().sendMessage(prefix + s_BidDurration);

				} catch (Exception e2) {
					e.getPlayer().sendMessage(prefix + s_cancelUnfinAuction+"  : "+e.getMessage());
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
							if(parts.length>0)
								hours = Integer.parseInt(parts[0]);
							if(parts.length>1)
								minutes = Integer.parseInt(parts[1]);
							if(parts.length>2)
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

					int ticks = ((((hours * 60) + minutes) * 60) + seconds) * 5;
					if (ticks > s_MAX_BID_TIME * 60 * 60 * 5)
						ticks = s_MAX_BID_TIME * 60 * 60 * 5;

					auctionWaitingMap.get(e.getPlayer().getUniqueId()).setWait(ticks);

					auctionWaitingStage.put(e.getPlayer().getUniqueId(), 3);
					e.getPlayer().sendMessage(prefix + s_BidBuyItNow);
					e.getPlayer().sendMessage(prefix + s_buyitnowoptional);

				} catch (Exception e2) {
					e2.printStackTrace();
					e.getPlayer().sendMessage(prefix + s_cancelUnfinAuction+"  : "+e.getMessage());
					e.getPlayer().getInventory().addItem(auctionWaitingMap.get(e.getPlayer().getUniqueId()).is);
					auctionWaitingMap.remove(e.getPlayer().getUniqueId());
					auctionWaitingStage.remove(e.getPlayer().getUniqueId());
				}
			} else if (auctionWaitingStage.get(e.getPlayer().getUniqueId()) == 3) {
				try {
					int k = auctionWaitingMap.get(e.getPlayer().getUniqueId()).buyitnow = Math
							.abs(Integer.parseInt(e.getMessage()));
					e.getPlayer().sendMessage(prefix + s_buyitnowset.replaceAll("%price%", k + ""));
				} catch (Exception e2) {
				}
				Auction aa = auctionWaitingMap.get(e.getPlayer().getUniqueId());
				auctions.add(aa);
				auctionWaitingMap.remove(e.getPlayer().getUniqueId());
				auctionWaitingStage.remove(e.getPlayer().getUniqueId());
				e.getPlayer().sendMessage(prefix + s_addedToAuctionHouse);
				if (enableBroadcasting) {
					Bukkit.broadcastMessage(prefix + s_broadcastMessage.replaceAll("%player%", e.getPlayer().getName())
							.replaceAll("%amount%", "" + aa.is.getAmount())
							.replaceAll("%material%", "" + aa.is.getType().name())
							.replaceAll("%cost%", aa.biddingPrice + ""));
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
					if (a.owner == e.getWhoClicked().getUniqueId()) {
						cc.add(c(a));
						am++;
					}
				}
				Inventory newInv = Bukkit.createInventory(null, (((am / 9) + 1) * 9), s_InvTitleCancel);
				for (ItemStack is : cc)
					newInv.addItem(is);
				e.getWhoClicked().openInventory(newInv);

			} else if (e.getSlot() == 52 && (slotcheck)) {
				int k = (Integer.parseInt(e.getWhoClicked().getOpenInventory().getTitle().split("Page:")[1].trim()) - 1)
						- 1;
				if (k >= 0)
					e.getWhoClicked().openInventory(gui[k]);
			} else if (e.getSlot() == 53 && (slotcheck) && (!buffercheck)) {
				int k = (Integer.parseInt(e.getWhoClicked().getOpenInventory().getTitle().split("Page:")[1].trim()) - 1)
						+ 1;
				if (k < gui.length)
					e.getWhoClicked().openInventory(gui[k]);

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
						int k = Integer.parseInt(ChatColor.stripColor(aak.getItemMeta().getLore().get(0)));
						Auction aa = null;
						for (Auction a : auctions) {
							if (a.auctionID == k) {
								aa = a;
								break;
							}
						}
						if (aa.owner == e.getWhoClicked().getUniqueId()) {
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
											econ.depositPlayer(ofp, aa.currentPrice);
											if (ofp.isOnline()) {
												((Player) ofp).sendMessage(prefix + s_outBid);
											}
										}

										aa.lastBid = e.getWhoClicked().getUniqueId();
										econ.withdrawPlayer(((Player) e.getWhoClicked()), aa.buyitnow);
										win(aa);
										auctions.remove(aa);
									}
									return;
								}
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
										((Player) ofp).sendMessage(prefix + s_outBid);
									}
								}

								aa.currentPrice += aa.biddingPrice;
								aa.lastBid = e.getWhoClicked().getUniqueId();
								econ.withdrawPlayer(((Player) e.getWhoClicked()), aa.currentPrice);
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
					aak = e.getClickedInventory().getItem(e.getSlot());
				} catch (Error | Exception e2) {
					aak = e.getInventory().getItem(e.getSlot());
				}
				if (aak != null) {
					int k = Integer.parseInt(ChatColor.stripColor(aak.getItemMeta().getLore().get(0)));
					Auction aa = null;
					for (Auction a : auctions) {
						if (a.auctionID == k) {
							aa = a;
							break;
						}
					}
					if (aa.owner == e.getWhoClicked().getUniqueId()) {
						e.getWhoClicked().closeInventory();
						e.getWhoClicked().sendMessage(prefix + s_cancelGen);
						e.getWhoClicked().getInventory().addItem(aa.is);
						auctions.remove(aa);
					}
				}
			} catch (Exception e2) {
				e2.printStackTrace();
			}

		} else if (e.getWhoClicked().getOpenInventory().getTitle().equalsIgnoreCase(s_InvTitleAdd)) {
			boolean itemnull = false;
			ItemStack slotis = null;
			try {
				if(e.getClickedInventory().getSize() <= e.getSlot() || e.getSlot() < 0)
					return;
			} catch (Error | Exception er) {
				if(e.getView().getBottomInventory().getSize() <= e.getSlot() || e.getSlot() < 0)
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
				Auction a = new Auction(slotis, e.getWhoClicked().getUniqueId(), e.getWhoClicked().getName(),
						(int) (Math.random() * 99999));
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
		ItemMeta temp2 = temp.getItemMeta();

		double time = (((double) a.quarterSecondsLeft) / TICK_SPEED);
		DecimalFormat df = new DecimalFormat("0.0");

		List<String> lore = new ArrayList<>();
		lore.add(ChatColor.BLACK + "" + a.auctionID);
		lore.add(ChatColor.GREEN + "Price : [$" + a.currentPrice + "+" + a.biddingPrice + "]");
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

		lore.add((a.quarterSecondsLeft / TICK_SPEED < 60 ? ChatColor.RED : ChatColor.GOLD) + s_loretime + time2);
		lore.add((a.ownerOnline ? ChatColor.GREEN : ChatColor.RED) + s_loreowner + a.ownerName);
		if (a.is != null && a.is.hasItemMeta() && a.is.getItemMeta().hasLore())
			lore.addAll(a.is.getItemMeta().getLore());
		temp2.setLore(lore);
		temp.setItemMeta(temp2);
		return temp;
	}
}
