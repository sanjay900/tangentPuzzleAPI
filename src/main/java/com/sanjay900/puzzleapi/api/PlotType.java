package com.sanjay900.puzzleapi.api;

import org.bukkit.material.MaterialData;


public interface PlotType {
	public PlotGame getGame();
	public String getName();
	public MaterialData getInnerWall();
	public MaterialData getOuterWall();
	public MaterialData getRoof();
	public MaterialData getFloor();
}
