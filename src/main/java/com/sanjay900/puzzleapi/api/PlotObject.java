package com.sanjay900.puzzleapi.api;

import org.bukkit.Location;

import lombok.Getter;

@Getter
public abstract class PlotObject {
	private boolean autoSpawn = false;
	protected Location location;
	public PlotObject(boolean autoSpawn) {
		this.autoSpawn = autoSpawn;
	}
	public abstract void spawn();
	public abstract void despawn();
}
