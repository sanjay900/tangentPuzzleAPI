package com.sanjay900.puzzleapi.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.material.MaterialData;

import lombok.Getter;

public class PlotTypeRegistry {
	public static List<PlotType> plotTypes = new ArrayList<>();
	static {
		plotTypes.add(new Default());
	}
	public static <T extends Enum<T> & PlotType> void registerPlotTypes(Class<T> enumType) {
		plotTypes.addAll(Arrays.asList(enumType.getEnumConstants()));
	}
	public static PlotType getType(String name) {
		return plotTypes.stream().filter(plotType -> plotType.getName().equalsIgnoreCase(name)).findFirst().orElse(getDefault());
	}
	public static PlotType getDefault() {
		return plotTypes.get(0);
	}
	@Getter
	public static class Default implements PlotType{
		private PlotGame game = new PlotGame("None",0);
		private String name = "Default";
		private MaterialData innerWall = new MaterialData(Material.STONE);
		private MaterialData outerWall = new MaterialData(Material.STONE);
		private MaterialData roof = new MaterialData(Material.STONE);
		private MaterialData floor = new MaterialData(Material.STONE);
	}
}
