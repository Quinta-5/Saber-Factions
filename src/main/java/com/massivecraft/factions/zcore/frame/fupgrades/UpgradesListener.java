package com.massivecraft.factions.zcore.frame.fupgrades;

import com.massivecraft.factions.Board;   
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.FactionsPlugin;
import com.massivecraft.factions.util.FastMath;
import com.massivecraft.factions.util.Logger;
import com.massivecraft.factions.zcore.frame.fupgrades.provider.stackers.RoseStackerProvider;
import com.massivecraft.factions.zcore.frame.fupgrades.provider.stackers.WildStackerProvider;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.block.data.Directional;
import org.bukkit.scheduler.BukkitScheduler;

public class UpgradesListener implements Listener {

    /**
     * @author Illyria Team, Atilt
     */

    private WildStackerProvider wildStackerProvider;
    private RoseStackerProvider roseStackerProvider;

    public void init() {
        Plugin wildStacker = Bukkit.getPluginManager().getPlugin("WildStacker");
        if (wildStacker != null) {
            this.wildStackerProvider = new WildStackerProvider();
        }
        Plugin roseStacker = Bukkit.getPluginManager().getPlugin("RoseStacker");
        if (roseStacker != null) {
            this.roseStackerProvider = new RoseStackerProvider();
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
       Entity killer = e.getEntity().getKiller();
        if (!(killer instanceof Player)) return;

        FLocation floc = FLocation.wrap(e.getEntity().getLocation());
        Faction faction = Board.getInstance().getFactionAt(floc);
        if (!faction.isWilderness()) {
            int level = faction.getUpgrade("EXP");
            double multiplier = FactionsPlugin.getInstance().getFileManager().getUpgrades().getConfig().getDouble("fupgrades.MainMenu.EXP.EXP-Boost.level-" + level);
            if (level != 0 && multiplier > 0.0) {
                this.spawnMoreExp(e, multiplier);
            }
        }
    }

    private void spawnMoreExp(EntityDeathEvent e, double multiplier) {
        double newExp = e.getDroppedExp() * multiplier;
        e.setDroppedExp((int) newExp);
    }

    @EventHandler
    public void onSpawn(SpawnerSpawnEvent e) {
        FLocation floc = FLocation.wrap(e.getLocation());
        Faction factionAtLoc = Board.getInstance().getFactionAt(floc);
        if (!factionAtLoc.isWilderness()) {
            int level = factionAtLoc.getUpgrade("Spawners");
            if (level == 0) return;
            this.lowerSpawnerDelay(e, FactionsPlugin.getInstance().getFileManager().getUpgrades().getConfig().getDouble("fupgrades.MainMenu.Spawners.Spawner-Boost.level-" + level));
        }
    }

    private void lowerSpawnerDelay(SpawnerSpawnEvent e, double multiplier) {
        CreatureSpawner spawner = e.getSpawner();
        int delay = spawner.getDelay() - FastMath.round(e.getSpawner().getDelay() * multiplier);

        if (this.wildStackerProvider != null && !this.wildStackerProvider.setDelay(spawner, delay)) {
            Logger.print("Unable obtain WildStacker instance. Plugin found: " + (Bukkit.getPluginManager().getPlugin(this.wildStackerProvider.pluginName()) != null), Logger.PrefixType.FAILED);
        } else if (this.roseStackerProvider != null && !this.roseStackerProvider.setDelay(spawner.getBlock(), delay)) {
            Logger.print("Missing expected spawner at: " + spawner.getX() + ", " + spawner.getY() + ", " + spawner.getZ(), Logger.PrefixType.FAILED);
        }
    }
    
    Plugin plugin = FactionsPlugin.getPlugin(FactionsPlugin.class);
    BukkitScheduler scheduler = Bukkit.getScheduler();

Set<Material> shrooms = EnumSet.of(Material.RED_MUSHROOM, Material.BROWN_MUSHROOM);

@EventHandler
public void onBlockSpread(BlockSpreadEvent e) {
	Location location = e.getSource().getLocation();
	if (willActivate(location)) {
            this.spreadShroom(e);
        }
    }
	
private boolean willActivate(Location e) {
	FLocation floc = FLocation.wrap(e);
    Faction factionAtLoc = Board.getInstance().getFactionAt(floc);
    if (!factionAtLoc.isWilderness()) {
        int level = factionAtLoc.getUpgrade("Crops");
        int chance = FactionsPlugin.getInstance().getFileManager().getUpgrades().getConfig().getInt("fupgrades.MainMenu.Crops.Crop-Boost.level-" + level);
        if (level == 0 || chance == 0) {  
        	return false;
        }
        int randomNum = ThreadLocalRandom.current().nextInt(0, 100);
        if (randomNum <= chance) {
        	return true;
        }
     } return false;
}

	
    Block source;
    Material typeShroom;
	
    private void spreadShroom(BlockSpreadEvent e){
    	Logger.print("spreadShroom check 1");
    	this.source= e.getSource();
    	Location location = e.getBlock().getLocation();   	
    	scheduler.runTask(plugin, () -> {
    		Location sourceLocation =e.getSource().getLocation();
    		this.typeShroom = location.getBlock().getType();
    		if (shrooms.contains(typeShroom)== true) {
    				Location shroomSpace = getSpaceForShroom(sourceLocation);
    				shroomSpace.getBlock().setType(typeShroom);
    			}
    		
    	});
    }
	
        
    Set<Material> mushroomGrowBlock = EnumSet.of(Material.MYCELIUM, Material.PODZOL, Material.CRIMSON_NYLIUM, Material.WARPED_NYLIUM);
	
    private boolean canSurvive(Block e){
    	Material below = e.getLocation().add(0.0, -1.0, 0.0).getBlock().getType();
    	Material origin= e.getType();
    	byte lightLevel =e.getLightLevel();
    	if (origin == Material.AIR) {	
    		if(mushroomGrowBlock.contains(below))  {
    		return true;
    	}else if ((lightLevel < 13)  && (below.isSolid() == true)){
    		return true;
    	}
      }return false;
    }

    Location spaceForShroom;
	
    private Location getSpaceForShroom(Location location) {
    	for (int x = 1; x >= -1; x--) {
    		for (int y = 1; y >= -1; y--) {
    			for (int z = 1; z >= -1; z--) { 
    				if (canSurvive(location.getBlock().getRelative(x, y, z)) == true) {
    					this.spaceForShroom = location.getBlock().getRelative(x, y, z).getLocation();
    				}
    			}
    		}
    	}return spaceForShroom;
    }	
	
@EventHandler
    public void onCropGrow(BlockGrowEvent e) {
    	Location location = e.getBlock().getLocation();
    	if (willActivate(location)) {
            	this.growCrop(e);
            }
          }
    
    BlockGrowEvent cropGrowEvent;
    Set<BlockFace> blockFaces = EnumSet.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST);
    Set<Material> manualCrops = EnumSet.of(Material.WHEAT, Material.BEETROOTS, Material.CARROTS, Material.POTATOES, Material.NETHER_WART, Material.COCOA);;
    Set<Material> autoCrops = EnumSet.of(Material.CACTUS, Material.SUGAR_CANE, Material.MELON, Material.PUMPKIN);;
    Set<Material> autoAgeableCrops = EnumSet.of(Material.SUGAR_CANE, Material.CACTUS);
    Set<Material> fruitCrops = EnumSet.of(Material.PUMPKIN, Material.MELON);
    Block blockCrop;
    Material typeCropNew;
    BlockData dataCropNew;
    Location locationCrop;
    
    private void growCrop(BlockGrowEvent e) {
    	this.cropGrowEvent = e;
    	this.blockCrop = cropGrowEvent.getNewState().getBlock();
    	this.typeCropNew= cropGrowEvent.getNewState().getType();
    	this.dataCropNew= cropGrowEvent.getNewState().getBlockData();
    	this.locationCrop = cropGrowEvent.getNewState().getBlock().getLocation();
    	if ((!manualCrops.contains(typeCropNew)) && (!autoCrops.contains(typeCropNew))) { 
    		return;
    		}else if (manualCrops.contains(typeCropNew)) {
    			growManual(cropGrowEvent);
    		}else if (fruitCrops.contains(typeCropNew)) {
				growFruit(cropGrowEvent);
    		}else if (autoAgeableCrops.contains(typeCropNew)) {
    			growAutoAgeable(cropGrowEvent);
    		}
    }
    					
    Block blockAutoAgeable;
    
    private void growAutoAgeable(BlockGrowEvent cropGrowEvent) {
    		Block above =  blockCrop.getLocation().add(0.0, 1.0, 0.0).getBlock();
    		Material aboveType = above.getType();
    		Material below2Type =blockCrop.getLocation().add(0.0, -2.0, 0.0).getBlock().getType();
    		if (typeCropNew == Material.SUGAR_CANE) {
    			if ((aboveType == Material.AIR) && (below2Type != Material.SUGAR_CANE)) {	
    				above.setType(Material.SUGAR_CANE);
    			}
    		}else if (typeCropNew == Material.CACTUS) {
    		 if (getNeighbors() == true) {
    				blockCrop.setType(Material.CACTUS);
    				blockCrop.breakNaturally();
       		}else if((aboveType == Material.AIR) && (below2Type != Material.CACTUS)) {
       			above.setType(Material.CACTUS);
       		}
    	}
    }
      		    		
private boolean getNeighbors() {
	for (BlockFace blockface : blockFaces)
			if (blockCrop.getRelative(blockface).getType() != Material.AIR) {
				return true;	
			} return false; 		
	}

    private void growManual(BlockGrowEvent cropGrowEvent) {
    	cropGrowEvent.setCancelled(true);
    	 int age =((Ageable) dataCropNew).getAge();
    	 int maxAge =((Ageable) dataCropNew).getMaximumAge();
    	 int newAge =Math.min(age + 1, maxAge);
    	 ((Ageable)dataCropNew).setAge(newAge);
    	 blockCrop.setBlockData((BlockData)(Ageable)dataCropNew);   	 
    	}
    
    private Set<Material> fruitSpawnable = EnumSet.of(Material.DIRT, Material.GRASS_BLOCK, Material.PODZOL, Material.COARSE_DIRT, Material.MYCELIUM, Material.ROOTED_DIRT, Material.MOSS_BLOCK, Material.FARMLAND);
    private Material typeAttachedStem;
    
    private void growFruit(BlockGrowEvent cropGrowEvent) {
    	scheduler.runTask(plugin, () -> {
    		 if (typeCropNew == Material.PUMPKIN) {
        		this.typeAttachedStem = Material.ATTACHED_PUMPKIN_STEM;      		
        	} else if (typeCropNew == Material.MELON) {
        		this.typeAttachedStem = Material.ATTACHED_MELON_STEM;	
        	} growExtraFruit();
    	});
      }	
    
    Block blockStemAttached;
    
    private Block getAttachedStem() {
    	for (BlockFace blockface : blockFaces)
    			if ((typeAttachedStem == blockCrop.getRelative(blockface).getType()) 
    					&& ((((Directional) (blockCrop.getRelative(blockface)).getBlockData()).getFacing() == blockface.getOppositeFace()))) {
    					this.blockStemAttached = blockCrop.getRelative(blockface);		
    			} return blockStemAttached; 		
    	}

    private void growExtraFruit() {
    	Block blockAttachedStem = getAttachedStem();
    	for (BlockFace blockface : blockFaces)
    			if (fruitSpawnable.contains(blockAttachedStem.getRelative(blockface).getLocation().add(0.0, -1.0, 0.0).getBlock().getType())
    	    			&& ((blockAttachedStem.getRelative(blockface).getType()) == (Material.AIR))) {
    					blockAttachedStem.getRelative(blockface).setType(typeCropNew);
    					return;
    			} 		
    	}
      
    @EventHandler
    public void onWaterRedstone(BlockFromToEvent e) {
        FLocation floc = FLocation.wrap(e.getToBlock().getLocation());
        Faction factionAtLoc = Board.getInstance().getFactionAt(floc);

        if (!factionAtLoc.isWilderness()) {
            int level = factionAtLoc.getUpgrade("Redstone");
            if (level != 0) {
                if (level == 1) {
                    List<String> unbreakable = FactionsPlugin.getInstance().getConfig().getStringList("no-water-destroy.Item-List");
                    String block = e.getToBlock().getType().toString();
                    if (unbreakable.contains(block)) {
                        e.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerFallUpgrade(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player) {
            if (e.getCause() == EntityDamageEvent.DamageCause.FALL) {
                Player player = (Player) e.getEntity();
                FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);

                if (fPlayer.getFaction().isNormal()) {
                    int level = fPlayer.getFaction().getUpgrade("Fall-Damage");

                    FLocation fLocation = FLocation.wrap(player.getLocation());
                    if (Board.getInstance().getFactionAt(fLocation) == fPlayer.getFaction() && level > 0) {
                        e.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onArmorDamage(PlayerItemDamageEvent e) {
        if (FPlayers.getInstance().getByPlayer(e.getPlayer()) == null) return;

        if (e.getItem().getType().toString().contains("LEGGINGS") || e.getItem().getType().toString().contains("CHESTPLATE") || e.getItem().getType().toString().contains("HELMET") || e.getItem().getType().toString().contains("BOOTS")) {
            int lvl = FPlayers.getInstance().getByPlayer(e.getPlayer()).getFaction().getUpgrade("Armor");
            double drop = FactionsPlugin.getInstance().getFileManager().getUpgrades().getConfig().getDouble("fupgrades.MainMenu.Armor.Armor-HP-Drop.level-" + lvl);
            int newDamage = FastMath.round(e.getDamage() - e.getDamage() * drop);
            e.setDamage(newDamage);
        }
    }
}
