package com.sanjay900.puzzleapi;

import lombok.Getter;

import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

import com.sanjay900.puzzleapi.api.PlotTypeRegistry;
import com.sanjay900.puzzleapi.worldgen.PlotChunkGenerator;

public class PuzzleAPI extends JavaPlugin{
	@Getter
	public static PuzzleAPI instance;
	public PlotTypeRegistry plotTypes = new PlotTypeRegistry();
	@Override
	public ChunkGenerator getDefaultWorldGenerator(String worldName, String id)
	{
		return new PlotChunkGenerator(worldName,id);
	}
	@Override
	public void onEnable() {
		instance = this;
	}
	
}
