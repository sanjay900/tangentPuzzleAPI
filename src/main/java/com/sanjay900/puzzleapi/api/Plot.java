package com.sanjay900.puzzleapi.api;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.conversations.Conversable;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.material.MaterialData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers.TitleAction;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.sanjay900.puzzleapi.PuzzleAPI;
import com.sanjay900.puzzleapi.worldgen.PlotChunkGenerator;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.regions.CuboidRegion;

import lombok.Getter;
import lombok.Setter;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
@Getter
@Setter
public abstract class Plot{
	protected UUID owner;
	protected String title;
	protected String subTitle;
	protected int coordX;
	protected int coordZ;
	protected World world;
	protected PlotType type;
	protected PlotType lastType;
	protected List<UUID> helpers = new ArrayList<>();
	protected ArrayList<AbstractPlayer> players = new ArrayList<>();
	protected Location[] startLoc = new Location[4];
	protected NPC[] npcs = new NPC[4];
	protected PlotStatus status = PlotStatus.STOPPED;
	protected int playerCount = 4;
	protected Scoreboard board;
	protected PuzzleAPI apiplugin = PuzzleAPI.getInstance();
	protected PlotLevelEnd end;
	protected List<PlotObject> objects = new ArrayList<>();
	NPCRegistry registry = CitizensAPI.getNPCRegistry();
	public Plot(int coordX, int coordZ, World world) {
		if (!(world.getGenerator() instanceof PlotChunkGenerator)) return;
		this.world = world;
		owner = null;
		title = "Change Me";
		subTitle = "Change Me";
		this.coordX = coordX;
		this.coordZ = coordZ;
		this.type = this.lastType = PlotTypeRegistry.getDefault();
		Location corner = new Location(world,coordX*getWidth()*16+10,3,coordX*getWidth()*16+10);
		NPCRegistry registry = CitizensAPI.getNPCRegistry();
		for (int i = 1; i <=playerCount;i++) {
			this.startLoc[i-1]=corner.clone();
			NPC player = registry.createNPC(EntityType.PLAYER, "Player "+i+" Spawn");
			player.spawn(corner);
			npcs[i-1] = player;
		}
		init();
	}
	public Plot(Location loc) {

		this(((int) Math.ceil((double)loc.getBlockX()/16d/(double)(((PlotChunkGenerator)loc.getWorld().getGenerator()).size/16)))-1,
				((int) Math.ceil((double)loc.getBlockZ()/16d/(double)(((PlotChunkGenerator)loc.getWorld().getGenerator()).size/16)))-1,
				loc.getWorld()
				);
	}
	public abstract void init();
	public void setPlayerCount(int playerCount) {
		startLoc = new Location[playerCount];
		npcs = new NPC[playerCount];
		Location corner = new Location(world,coordX*getWidth()*16+10,3,coordX*getWidth()*16+10);
		for (int i = 0; i <playerCount;i++) {
			this.startLoc[i]=corner.clone();
			NPC wolf = registry.createNPC(EntityType.PLAYER, "Player "+(i+1)+" Spawn");
			wolf.spawn(corner);
			npcs[i] = wolf;
		}
		this.playerCount = playerCount;
	}
	public void save() {
		//plugin.pl.updateScoreboards(this);
		//TODO: Save plot data - Save extra info as well. plugin.plotManager.savePlot(this);
		//Possibly have save abstract?
	}
	public boolean setType(final PlotType type) {
		if (this.type != type) {
			this.lastType = this.type;
			this.type = type;
			Bukkit.getScheduler().runTask(apiplugin,() -> {
				for (int x = getChunkX(); x < getChunkX()+getWidth(); x++) {
					for (int z = getChunkZ(); z < getChunkZ()+getWidth(); z++) {
						getGenerator().makeFloor(world, null, type.getFloor(), x, z);
						getGenerator().makeRoof(world, null, type.getRoof(), x, z);
						getGenerator().makeWall(null, x, z, type.getOuterWall(), type.getInnerWall(), world);
					}
				}
				for (int x = getChunkX(); x < getChunkX()+getWidth(); x++) {
					getGenerator().makeWall(null, x,getChunkZ()+getWidth(), type.getOuterWall(), type.getInnerWall(), world, true, false);
				}
				for (int z = getChunkZ(); z < getChunkZ()+getWidth(); z++) {
					getGenerator().makeWall(null, getChunkX()+getWidth(),z, type.getOuterWall(), type.getInnerWall(), world, false, true);
				}
				if (lastType == PlotTypeRegistry.getDefault()) return;
			}
					);
			return true;
		}
		return false;
	}
	public int getWidth() {
		return getGenerator().size/ 16;
	}
	public PlotChunkGenerator getGenerator() {
		return ((PlotChunkGenerator)world.getGenerator());
	}
	public int getChunkX() {
		return coordX*getWidth();
	}
	public int getChunkZ() {
		return coordZ*getWidth();
	}
	@SuppressWarnings("deprecation")
	public void replaceBlocks(MaterialData orig, MaterialData replace) {
		EditSession es = WorldEdit.getInstance().getEditSessionFactory().getEditSession(new BukkitWorld(world), 4096);
		CuboidRegion r = new CuboidRegion(new Vector((getChunkX()*16)+7,3,(getChunkZ()*16)+7),new Vector((getChunkX()*16)+getGenerator().size-1,getGenerator().height-1,(getChunkZ()*16)+getGenerator().size-1));
		HashSet<BaseBlock> mask = new HashSet<>();
		mask.add(new BaseBlock(orig.getItemTypeId(),orig.getData()));
		try {
			es.replaceBlocks(r, mask, new BaseBlock(replace.getItemTypeId(),replace.getData()));
		} catch (MaxChangedBlocksException e) {
		}

	}
	public PlotLocation hasLocation(Location loc) {
		int coordX = ((int) Math.ceil((double)loc.getBlockX()/16d/(double)getWidth()))-1;
		int coordZ = ((int) Math.ceil((double)loc.getBlockZ()/16d/(double)getWidth()))-1;
		int locRelX = loc.getBlockX()-coordX*16*getWidth();
		int locRelZ = loc.getBlockZ()-coordZ*16*getWidth();
		if (coordX == this.coordX && coordZ == this.coordZ) {
			if (locRelX < getGenerator().size && locRelZ < getGenerator().size && locRelX > 6 && locRelZ > 6 ) {
				return PlotLocation.INPLOT;
			}
			if (locRelX == getGenerator().size || locRelZ == getGenerator().size || locRelX > 4 || locRelZ > 4 ) {
				return PlotLocation.WALL;
			}
			return PlotLocation.PATH;
		}
		return PlotLocation.NONE;
	}
	public void printInformationId(CommandSender sender,int id) {
		Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
				"tellraw {player} {\"text\":\"\",\"extra\":[{\"text\":\"Plot {id} - Coords: {coords}\",\"color\":\"dark_green\",\"bold\":\"true\",\"underlined\":\"true\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/plot warp {id}\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":{\"text\":\"\",\"extra\":[{\"text\":\"Click to warp to plot {coords}\",\"color\":\"yellow\"}]}}}]}"
				.replace("{player}",sender.getName())
				.replace("{id}",id+"")
				.replace("{coords}","["+coordX+","+coordZ+"]")
				.replace("{title}",getTitle())
				.replace("{subtitle}",getSubTitle())
				.replace("{type}",type == PlotTypeRegistry.getDefault()?"None":getType().getName())
				);

	}
	public void printInformation(CommandSender sender) {
		sender.sendMessage("==============="+ChatColor.YELLOW+"Plot Information:"+ChatColor.RESET+"===============");
		sender.sendMessage("Plot: ["+coordX+","+coordZ+"]");
		if (type == PlotTypeRegistry.getDefault()) {
			sender.sendMessage("Type: None");
		} else {
			sender.sendMessage("Type: "+type.getName().toLowerCase());
		}
		if (owner != null)
			sender.sendMessage("Owner: "+Bukkit.getOfflinePlayer(owner).getName());
		else
			sender.sendMessage("Owner: Nobody");
		sender.sendMessage("Title: "+title);
		sender.sendMessage("Sub Title: "+subTitle);
	}
	public void printInformation(Conversable sender) {
		sender.sendRawMessage("==============="+ChatColor.YELLOW+"Plot Information:"+ChatColor.RESET+"===============");
		sender.sendRawMessage("Plot: ["+coordX+","+coordZ+"]");
		if (type == PlotTypeRegistry.getDefault()) {
			sender.sendRawMessage("Type: None");
		} else {
			sender.sendRawMessage("Type: "+type.getName().toLowerCase());
		}
		if (owner != null)
			sender.sendRawMessage("Owner: "+Bukkit.getOfflinePlayer(owner).getName());
		else
			sender.sendRawMessage("Owner: Nobody");
		sender.sendRawMessage("Title: "+title);
		sender.sendRawMessage("Sub Title: "+subTitle);
	}
	public void startGame(final Player... pls) {
		if (!status.equals(PlotStatus.STOPPED)) return;
		status = PlotStatus.STARTING;
		if (players.isEmpty()) {
			for (NPC npc : npcs) {
				if (npc != null) {
					npc.despawn();
				}
			}
			WrappedChatComponent titlec = WrappedChatComponent.fromChatMessage(title)[0];
			WrappedChatComponent subtitlec = WrappedChatComponent.fromChatMessage(subTitle)[0];
			PacketContainer title = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.TITLE);
			PacketContainer subtitle = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.TITLE);
			title.getTitleActions().write(0, TitleAction.TITLE);
			subtitle.getTitleActions().write(0, TitleAction.SUBTITLE);
			title.getChatComponents().write(0, titlec);
			subtitle.getChatComponents().write(0, subtitlec);
			for (int i = 0; i < pls.length && i <= playerCount; i++) {
				Player p = pls[i];
				p.teleport(startLoc[i]);
				try {
					ProtocolLibrary.getProtocolManager().sendServerPacket(p, title);
					ProtocolLibrary.getProtocolManager().sendServerPacket(p, subtitle);
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
				p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS,100 ,1));
				p.setWalkSpeed(0);
			}
			for (PlotObject p: objects) {
				p.despawn();
			}
			Bukkit.getScheduler().runTaskLater(apiplugin, new Runnable(){

				@Override
				public void run() {
					status = PlotStatus.STARTED;
					for (Player p: pls) {
						p.removePotionEffect(PotionEffectType.BLINDNESS);
						p.setWalkSpeed(0.2f);
					}

					for (PlotObject p: objects) {
						if (p.isAutoSpawn())
						p.spawn();
					}
					for (int i = 0; i < pls.length && i <= playerCount; i++) {
						AbstractPlayer player = createPlayer(pls[i],i);
						players.add(player);
					}
				}}, 100L);

		}
	}
	public abstract AbstractPlayer createPlayer(Player pl, int i);
	public void stopGame() {
		status = PlotStatus.STOPPED;
		players.clear();
		
		for (NPC npc : npcs) {
			if (npc != null) {
				npc.spawn(npc.getStoredLocation());
			}
		}
	}
	public enum PlotLocation {
		INPLOT,WALL,PATH,NONE
	}
	public enum PlotStatus {
		STOPPED,STARTING,STARTED
	}
	public void respawn() {
		if (status != PlotStatus.STARTED) return;
		status = PlotStatus.STARTING;
		WrappedChatComponent titlec = WrappedChatComponent.fromChatMessage(title)[0];
		WrappedChatComponent subtitlec = WrappedChatComponent.fromChatMessage(subTitle)[0];
		PacketContainer title = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.TITLE);
		PacketContainer subtitle = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.TITLE);
		title.getTitleActions().write(0, TitleAction.TITLE);
		subtitle.getTitleActions().write(0, TitleAction.SUBTITLE);
		title.getChatComponents().write(0, titlec);
		subtitle.getChatComponents().write(0, subtitlec);
		for (int i = 0; i < players.size(); i++) {
			players.get(i).getPlayer().teleport(startLoc[i]);
			Player p = players.get(i).getPlayer();
			try {
				ProtocolLibrary.getProtocolManager().sendServerPacket(p, title);
				ProtocolLibrary.getProtocolManager().sendServerPacket(p, subtitle);
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
			p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS,100,1));
			p.setWalkSpeed(0);
		}
		for (PlotObject p: objects) {
			p.despawn();
		}
		Bukkit.getScheduler().runTaskLater(apiplugin, () -> {
				status = PlotStatus.STARTED;
				for (AbstractPlayer p: players) {
					p.getPlayer().removePotionEffect(PotionEffectType.BLINDNESS);
					p.getPlayer().setWalkSpeed(1);
				}
				for (PlotObject p: objects) {
					if (p.isAutoSpawn())
					p.spawn();
				}
			}, 100L);

	}
	public void setStartLoc(Location location, int which) {
		if (which > playerCount) return;
		location = location.getBlock().getLocation().add(0.5,0,0.5);
		startLoc[which-1] = location;
		npcs[which-1].teleport(location, TeleportCause.PLUGIN);

	}
	public void setHelpers(List<String> helperlist) {
		for (String helper: helperlist) {
			helpers.add(UUID.fromString(helper));
		}
	}
	public void addHelper(UUID uuid) {
		helpers.add(uuid);
		save();
	}
	public void removeHelper(UUID uuid) {
		if (!helpers.contains(uuid)) return;
		helpers.remove(uuid);
		save();
	}
	public List<String> getHelpersString() {
		List<String> helperlist = new ArrayList<>();
		for (UUID helper: helpers) {
			helperlist.add(helper.toString());
		}
		return helperlist;
	}
	private List<UUID> requestPlayers = new ArrayList<>();
	private List<UUID> acceptPlayers = new ArrayList<>();
	public void requestGame(Player sender, ArrayList<UUID> players) {
		cancelRequest(sender);
		requestPlayers = players;
		sender.sendMessage("Asking players if they want to play. Wait for them to respond.");
		for (UUID u: requestPlayers) {
			if (Bukkit.getPlayer(u) == null){
				cancelRequest(sender);
				return;
			}
			Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
					"tellraw {player} {\"text\":\"\",\"extra\":[{\"text\":\"{sender}\",\"color\":\"yellow\"},{\"text\":\" would like to play a game of \"},{\"text\":\"Wonderland\",\"color\":\"yellow\",\"bold\":\"true\"},{\"text\":\" With you! \"},{\"text\":\"Click to accept\",\"color\":\"green\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/plot accept\"}},{\"text\":\" or \"},{\"text\":\"deny.\",\"color\":\"red\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/plot deny\"}}]}"
					.replace("{player}", Bukkit.getPlayer(u).getDisplayName())
					.replace("{sender}", sender.getDisplayName())
					);
		}
	}
	public void cancelRequest(Player sender) {
		for (UUID p : acceptPlayers) {
			Bukkit.getPlayer(p).sendMessage(sender.getDisplayName()+" Has cancelled their request to play a game of wonderland.");
		}
		requestPlayers.clear();
		acceptPlayers.clear();
		return;
	}
	public boolean denyRequest(Player sender) {
		if (!requestPlayers.contains(sender.getUniqueId())) return false;
		for (UUID p : acceptPlayers) {
			Bukkit.getPlayer(p).sendMessage(sender.getDisplayName()+" denied their request to start this game, and so it can't start.");		
		}
		Bukkit.getPlayer(owner).sendMessage(sender.getDisplayName()+" denied their request to start this game, and so it can't start.");		
		requestPlayers.clear();
		acceptPlayers.clear();
		return true;
	}
	public boolean acceptRequest(Player sender) {
		if (!requestPlayers.contains(sender.getUniqueId())) return false;
		acceptPlayers.add(sender.getUniqueId());
		if (requestPlayers.size() == acceptPlayers.size()) {
			Player[] players = new Player[acceptPlayers.size()+1];
			int i = 0;
			for (UUID p : acceptPlayers) {
				Bukkit.getPlayer(p).sendMessage("All players have accepted. Starting game.");
				players[i++]=Bukkit.getPlayer(p);
			}
			players[i++]=Bukkit.getPlayer(owner);
			Bukkit.getPlayer(owner).sendMessage("All players have accepted. Starting game.");
			startGame(players);
		} else {
			for (UUID p : acceptPlayers) {
				Bukkit.getPlayer(p).sendMessage(acceptPlayers.size() +" out of "+requestPlayers.size()+" have joined the game!");		
			}
			Bukkit.getPlayer(owner).sendMessage(acceptPlayers.size() +" out of "+requestPlayers.size()+" have joined the game!");	
		}

		return true;
	}
	public void addObject(PlotObject p) {
		objects.add(p);
	}


}
