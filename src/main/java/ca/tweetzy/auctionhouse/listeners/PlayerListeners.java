/*
 * Auction House
 * Copyright 2018-2022 Kiran Hart
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ca.tweetzy.auctionhouse.listeners;

import ca.tweetzy.auctionhouse.AuctionHouse;
import ca.tweetzy.auctionhouse.auction.AuctionPlayer;
import ca.tweetzy.auctionhouse.guis.GUIAuctionHouse;
import ca.tweetzy.auctionhouse.helpers.BundleUtil;
import ca.tweetzy.auctionhouse.helpers.PlayerHelper;
import ca.tweetzy.auctionhouse.helpers.UpdateChecker;
import ca.tweetzy.auctionhouse.settings.Settings;
import ca.tweetzy.core.compatibility.XMaterial;
import ca.tweetzy.core.utils.PlayerUtils;
import ca.tweetzy.core.utils.TextUtils;
import ca.tweetzy.flight.comp.Titles;
import ca.tweetzy.flight.comp.enums.ServerVersion;
import ca.tweetzy.flight.nbtapi.NBT;
import ca.tweetzy.flight.utils.Common;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * The current file has been created by Kiran Hart
 * Date Created: February 10 2021
 * Time Created: 1:33 p.m.
 * Usage of any code found within this class is prohibited unless given explicit permission otherwise
 */
public class PlayerListeners implements Listener {

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		final Player player = event.getEntity();
		final AuctionPlayer auctionPlayer = AuctionHouse.getInstance().getAuctionPlayerManager().getPlayer(player.getUniqueId());

		if (auctionPlayer != null) {
			// task id cancel
			Bukkit.getServer().getScheduler().cancelTask(auctionPlayer.getAssignedTaskId());

			if (auctionPlayer.getItemBeingListed() != null && player.getLocation().getWorld() != null) {
				if (!AuctionHouse.getInstance().getAuctionPlayerManager().isInSellProcess(player)) {

					if (!player.hasMetadata("AuctionHouseConfirmListing"))
						player.getLocation().getWorld().dropItemNaturally(player.getLocation(), auctionPlayer.getItemBeingListed());
				}
				auctionPlayer.setItemBeingListed(XMaterial.AIR.parseItem());
			}
		}

		AuctionHouse.getInstance().getAuctionPlayerManager().processSell(player);
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onPlayerJoin(PlayerJoinEvent e) {
		final Player player = e.getPlayer();
		final AuctionHouse instance = AuctionHouse.getInstance();
		Titles.sendTitle(player, 1, 1, 1, " ", " ");

		instance.getAuctionPlayerManager().addPlayer(player);


		// DUPE TRACKING
		for (ItemStack item : player.getInventory().getStorageContents()) {
			if (item == null || item.getType() == XMaterial.AIR.parseMaterial() || item.getAmount() == 0) continue;

			final boolean inventoryContainsListedItem = NBT.get(item, nbt -> (boolean) nbt.getBoolean("AuctionDupeTracking"));

			if (inventoryContainsListedItem) {
				player.getInventory().remove(item);
				Bukkit.getServer().getConsoleSender().sendMessage(Common.colorize("&8[&eAuctionHouse&8] &CRemoving duped item from " + player.getName() + "'s inventory!"));
			}
		}


		Bukkit.getServer().getScheduler().runTaskLaterAsynchronously(AuctionHouse.getInstance(), () -> {

			if (Settings.UPDATE_CHECKER.getBoolean() && instance.getStatus() == UpdateChecker.UpdateStatus.UNRELEASED_VERSION && player.isOp()) {
				instance.getLocale().newMessage(TextUtils.formatText(String.format("&dYou're running an unreleased version of Auction House &f(&c%s&f)", instance.getDescription().getVersion()))).sendPrefixedMessage(player);
			}
		}, 20);
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent e) {
		final Player player = e.getPlayer();
		final AuctionHouse instance = AuctionHouse.getInstance();

		AuctionHouse.getInstance().getAuctionPlayerManager().processSell(player);

		if (instance.getAuctionPlayerManager().getPlayer(player.getUniqueId()) != null && instance.getAuctionPlayerManager().getPlayer(player.getUniqueId()).getItemBeingListed() != null) {

			final ItemStack toGiveRaw = instance.getAuctionPlayerManager().getPlayer(player.getUniqueId()).getItemBeingListed();

			if (BundleUtil.isBundledItem(toGiveRaw)) {
				PlayerUtils.giveItem(player, BundleUtil.extractBundleItems(toGiveRaw));
			} else {
				PlayerUtils.giveItem(player, toGiveRaw);
			}

			instance.getAuctionPlayerManager().getPlayer(player.getUniqueId()).setItemBeingListed(null);
			instance.getAuctionPlayerManager().getPlayer(player.getUniqueId()).setPlayer(null);
		}


		instance.getAuctionPlayerManager().getSellHolding().remove(player.getUniqueId());
		instance.getLogger().info("Removing sell holding instance for user: " + player.getName());
	}

	@EventHandler
	public void onCraftWithBundle(PrepareItemCraftEvent event) {
		final CraftingInventory inventory = event.getInventory();
		final ItemStack[] craftingItems = inventory.getMatrix();
		for (ItemStack item : craftingItems) {
			if (item == null || item.getType() == XMaterial.AIR.parseMaterial()) continue;
			if (BundleUtil.isBundledItem(item)) {
				inventory.setResult(XMaterial.AIR.parseItem());
			}
		}
	}

	@EventHandler
	public void onAuctionChestClick(PlayerInteractEvent e) {
		if (ServerVersion.isServerVersionBelow(ServerVersion.V1_14)) return;

		final Player player = e.getPlayer();
		final Block block = e.getClickedBlock();

		if (block == null || block.getType() != XMaterial.CHEST.parseMaterial()) return;
		final Chest chest = (Chest) block.getState();

		final AuctionHouse instance = AuctionHouse.getInstance();
		final NamespacedKey key = new NamespacedKey(instance, "AuctionHouseMarkedChest");
		if (chest.getPersistentDataContainer().has(key, PersistentDataType.BYTE)) {
			e.setUseInteractedBlock(Event.Result.DENY);
			e.setCancelled(true);

			if (instance.getAuctionBanManager().checkAndHandleBan(player)) {
				return;
			}

			if (!player.hasPermission("auctionhouse.useauctionchest")) return;

			instance.getGuiManager().showGUI(player, new GUIAuctionHouse(instance.getAuctionPlayerManager().getPlayer(player.getUniqueId())));
		}
	}

	@EventHandler
	public void onBundleClick(PlayerInteractEvent e) {
		final Player player = e.getPlayer();
		final ItemStack heldItem = PlayerHelper.getHeldItem(player);

		if (heldItem == null || (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK)) return;
		if (heldItem.getType() == XMaterial.AIR.parseMaterial()) return;
		if (!BundleUtil.isBundledItem(heldItem)) return;
		e.setCancelled(true);

		final List<ItemStack> items = BundleUtil.extractBundleItems(heldItem);

		if (heldItem.getAmount() >= 2) {
			heldItem.setAmount(heldItem.getAmount() - 1);
		} else {
			if (ServerVersion.isServerVersionAbove(ServerVersion.V1_8)) {
				player.getInventory().setItemInMainHand(XMaterial.AIR.parseItem());
			} else {
				player.getInventory().setItemInHand(XMaterial.AIR.parseItem());
			}
		}

		PlayerUtils.giveItem(player, items);
	}

	@EventHandler
	public void onInventoryClick(PrepareAnvilEvent event) {
		ItemStack stack = event.getResult();
		if (stack == null || stack.getType() == XMaterial.AIR.parseMaterial() || stack.getAmount() == 0) return;

		NBT.modify(stack, nbt -> {
			nbt.setBoolean("AuctionHouseRepaired", true);
		});

		event.setResult(stack);
	}
}
