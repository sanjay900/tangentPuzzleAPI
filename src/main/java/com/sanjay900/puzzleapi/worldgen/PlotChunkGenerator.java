package com.sanjay900.puzzleapi.worldgen;

import java.util.Random;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.material.MaterialData;

import com.sanjay900.puzzleapi.api.PlotTypeRegistry;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.bukkit.BukkitWorld;

public class PlotChunkGenerator extends ChunkGenerator {
	public int size;
	public int height = 25;
	public PlotChunkGenerator(String worldName, String id) {
		try {
			size = Integer.valueOf(id);
			if (size % 16 != 0) {
				size = Math.round(size/16)*16;
			}
		} catch (NumberFormatException  ex) {
			size = 64;
		}
	}

	@Override
	public byte[][] generateBlockSections(World world, Random random, int chunkX, int chunkZ, BiomeGrid biomeGrid)
	{
		byte[][] result = new byte[world.getMaxHeight() / 16][]; //world height / chunk part height (=16, look above)
		makeFloor(null,result, PlotTypeRegistry.getDefault().getFloor(), chunkX, chunkZ);
		makeWall(result, chunkX, chunkZ, PlotTypeRegistry.getDefault().getOuterWall(), PlotTypeRegistry.getDefault().getInnerWall(), null);
		return result;
	}
	public void makeWall(byte[][] result, int chunkX, int chunkZ, MaterialData outerWall, MaterialData innerWall, World world) {
		makeWall(result, chunkX, chunkZ, outerWall, innerWall, world, false, false);
	}
	public void makeWall(byte[][] result, int chunkX, int chunkZ, MaterialData outerWall, MaterialData innerWall, World world, boolean second, boolean second2) {
		int width = size / 16;
		if (chunkX % width ==0&&chunkZ % width ==0) {
			if (world == null) {
				for(int y = 1; y <= height; y++)
				{

					setBlock(world, result, 0, y, 0, outerWall,chunkX,chunkZ);
					setBlock(world, result, 6, y, 0, outerWall,chunkX,chunkZ);
					setBlock(world, result, 6, y, 6, outerWall,chunkX,chunkZ);
					setBlock(world, result, 0, y, 6, outerWall,chunkX,chunkZ);

				}
			}
			for(int x = 7; x < 16; x++)
			{
				for(int y = 1; y <= height; y++)
				{
					if (second ||world == null) {
						setBlock(world, result, x, y, 0, innerWall,chunkX,chunkZ);
						setBlock(world, result, x, y, 1, outerWall,chunkX,chunkZ);
					}
					if ((!second&&!second2) || world == null) {
						setBlock(world, result, x, y, 5, outerWall,chunkX,chunkZ);
						setBlock(world, result, x, y, 6, innerWall,chunkX,chunkZ);
					}
				}
			}
			for(int z = 7; z < 16; z++)
			{
				for(int y = 1; y <= height; y++)
				{
					if (second2 ||world == null) {
						setBlock(world, result, 0, y, z, innerWall,chunkX,chunkZ);
						setBlock(world, result, 1, y, z, outerWall,chunkX,chunkZ);
					}
					if ((!second&&!second2)|| world == null) {
						setBlock(world, result, 5, y, z, outerWall,chunkX,chunkZ);
						setBlock(world, result, 6, y, z, innerWall,chunkX,chunkZ);
					}
				}
			}

		}
		else if (chunkX % width ==0) {
			for(int z = 0; z < 16; z++)
			{
				for(int y = 1; y <= height; y++)
				{
					if (second2 ||second  || world == null) {
						setBlock(world, result, 0, y, z, innerWall,chunkX,chunkZ);
						setBlock(world, result, 1, y, z, outerWall,chunkX,chunkZ);
					}
					if ((!second&&!second2) || world == null) {
						setBlock(world, result, 5, y, z, outerWall,chunkX,chunkZ);
						setBlock(world, result, 6, y, z, innerWall,chunkX,chunkZ);
					}
				}
			}
		}else if (chunkZ % width ==0) {
			for(int x = 0; x < 16; x++)
			{
				for(int y = 1; y <= height; y++)
				{
					if (second2 ||second || world == null) {
						setBlock(world, result, x, y, 0, innerWall,chunkX,chunkZ);
						setBlock(world, result, x, y, 1, outerWall,chunkX,chunkZ);
					}
					if ((!second&&!second2) || world == null) {
						setBlock(world, result, x, y, 5, outerWall,chunkX,chunkZ);
						setBlock(world, result, x, y, 6, innerWall,chunkX,chunkZ);
					}
				}
			}
		}

	}
	public void makeRoof(World world,byte[][] result,MaterialData roof, int chunkX, int chunkZ) {
		int width = size / 16;
		for(int x = chunkX % width ==0?6:0; x < 16; x++)
		{
			for(int z = chunkZ % width ==0?6:0; z < 16; z++)
			{
				setBlock(world, result, x, height, z, roof,chunkX,chunkZ);
			}
		}


	}
	@SuppressWarnings("deprecation")
	public void makeFloor(World world,byte[][] result,MaterialData floor, int chunkX, int chunkZ) {
		int width = size / 16;
		if (world == null) {
			for(int x = 0; x < 16; x++)
			{
				for(int z = 0; z < 16; z++)
				{
					setBlock(world, result, x, 0, z, new MaterialData(Material.BEDROCK,(byte) 0),chunkX,chunkZ);
					for(int y = 1; y <= 2; y++)
					{
						setBlock(world, result, x, y, z, new MaterialData(Material.STONE,(byte) 0),chunkX,chunkZ);
					}
				}
			}
		}
		for(int x = chunkX % width ==0?6:0; x < 16; x++)
		{
			for(int z = chunkZ % width ==0?6:0; z < 16; z++)
			{
				setBlock(world, result, x, 2, z, floor,chunkX,chunkZ);
			}
		}


	}

	@SuppressWarnings("deprecation")
	void setBlock(World world, byte[][] result, int x, int y, int z, MaterialData data, int chunkX, int chunkZ) {
		if (world == null) {
			// is this chunk part already initialized?
			if (result[y >> 4] == null) {
				// Initialize the chunk part
				result[y >> 4] = new byte[4096];
			}
			// set the block (look above, how this is done)
			result[y >> 4][((y & 0xF) << 8) | (z << 4) | x] = (byte) data.getItemTypeId() ;
		} else {
			EditSession es = WorldEdit.getInstance().getEditSessionFactory().getEditSession(new BukkitWorld(world), 4096);
			es.smartSetBlock(new Vector((chunkX*16)+x, y, (chunkZ*16)+z), new BaseBlock(data.getItemTypeId(), data.getData()));

		}
	}
}
