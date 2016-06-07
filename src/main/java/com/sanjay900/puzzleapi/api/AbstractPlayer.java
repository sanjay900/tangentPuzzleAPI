package com.sanjay900.puzzleapi.api;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public abstract class AbstractPlayer {
	private UUID uuid;
	public AbstractPlayer(Player pl) {
		this.uuid = pl.getUniqueId();
	}
	public Player getPlayer() {
		return Bukkit.getPlayer(uuid);
	}
}
