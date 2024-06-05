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

package ca.tweetzy.auctionhouse.commands;

import ca.tweetzy.auctionhouse.AuctionHouse;
import ca.tweetzy.auctionhouse.auction.AuctionPlayer;
import ca.tweetzy.auctionhouse.guis.core.GUIAuctionHouseV2;
import ca.tweetzy.core.commands.AbstractCommand;
import ca.tweetzy.core.utils.TextUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The current file has been created by Kiran Hart
 * Date Created: January 12 2021
 * Time Created: 6:40 p.m.
 * Usage of any code found within this class is prohibited unless given explicit permission otherwise
 */
public class CommandAuctionHouse extends AbstractCommand {

	public CommandAuctionHouse() {
		super(CommandType.PLAYER_ONLY, "auctionhouse");
	}

	@Override
	protected ReturnType runCommand(CommandSender sender, String... args) {
		if (sender instanceof Player) {
			Player player = (Player) sender;

			if (CommandMiddleware.handle(player) == ReturnType.FAILURE) return ReturnType.FAILURE;

			final AuctionHouse instance = AuctionHouse.getInstance();
			if (instance.getAuctionPlayerManager().getPlayer(player.getUniqueId()) == null) {
				instance.getLocale().newMessage(TextUtils.formatText("&cCould not find auction player instance for&f: &e" + player.getName() + "&c creating one now.")).sendPrefixedMessage(Bukkit.getConsoleSender());
				instance.getAuctionPlayerManager().addPlayer(new AuctionPlayer(player));
			}

			if (args.length == 0) {
				instance.getGuiManager().showGUI(player, new GUIAuctionHouseV2(instance.getAuctionPlayerManager().getPlayer(player.getUniqueId())));
				return ReturnType.SUCCESS;
			}

			if (args.length == 1 && instance.getCommandManager().getSubCommands("auctionhouse").stream().noneMatch(cmd -> cmd.equalsIgnoreCase(StringUtils.join(args, ' ').trim()))) {
				if (args[0].equalsIgnoreCase("NaN")) return ReturnType.FAILURE;
				instance.getGuiManager().showGUI(player, new GUIAuctionHouseV2(instance.getAuctionPlayerManager().getPlayer(player.getUniqueId()), StringUtils.join(args, ' ').trim()));
			}
		}
		return ReturnType.SUCCESS;
	}

	@Override
	protected List<String> onTab(CommandSender sender, String... args) {
		final Player player = (Player) sender;
		return AuctionHouse.getInstance().getCommandManager().getAllCommands().stream().filter(cmd -> cmd.getPermissionNode() == null || player.hasPermission(cmd.getPermissionNode())).map(AbstractCommand::getSyntax).collect(Collectors.toList());
	}

	@Override
	public String getPermissionNode() {
		return "auctionhouse.cmd";
	}

	@Override
	public String getSyntax() {
		return AuctionHouse.getInstance().getLocale().getMessage("commands.syntax.auctionhouse").getMessage();
	}

	@Override
	public String getDescription() {
		return AuctionHouse.getInstance().getLocale().getMessage("commands.description.auctionhouse").getMessage();
	}
}
