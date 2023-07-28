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
    @EventHandler
    public void onCropGrow(BlockGrowEvent e) {
    	Logger.print("BlockGrowEvent detected, 'Crops' fUpgrade activation procedure has begun");
    	Logger.print("Block" + e.getBlock().getType() + "detected in onCropGrow(): Old Type");
    	Logger.print("Block" + e.getNewState().getType() + "detected in onCropGrow(): New Type");
        FLocation floc = FLocation.wrap(e.getBlock().getLocation());
        Faction factionAtLoc = Board.getInstance().getFactionAt(floc);
        if (!factionAtLoc.isWilderness()) {
            int level = factionAtLoc.getUpgrade("Crops");
            int chance = FactionsPlugin.getInstance().getFileManager().getUpgrades().getConfig().getInt("fupgrades.MainMenu.Crops.Crop-Boost.level-" + level);
            if (level == 0 || chance == 0) {
            	return;
            }
            int randomNum = ThreadLocalRandom.current().nextInt(0, 100);
            if (randomNum <= chance) {
            	Logger.print("'Crops' fUpgrade, 'growCrop()', activated: 'randomNum' is less than or equal to 'chance'");
            	this.growCrop(e);
            }}}
    
    Set<Material> manualCrops;
    Set<Material> autoCrops;
    Set<Material> autoAgeableCrops;
    Set<Material> fruitCrops;
    BlockGrowEvent cropGrowEvent;
    Block blockCrop;
    Material typeCropNew;
    BlockData dataCropNew;
    private void growCrop(BlockGrowEvent e) {
    	Logger.print("Block" + e.getBlock().getType() + "detected in growCrop(): Old Type");
    	Logger.print("Block" + e.getNewState().getType() + "detected in growCrop(): New Type");
    	Logger.print("'growCrop()' has begun");
    	Logger.print("'0' Ticks have passed");
    	this.manualCrops = EnumSet.of(Material.WHEAT, Material.BEETROOTS, Material.CARROTS, Material.POTATOES, Material.NETHER_WART, Material.COCOA);
    	this.autoCrops = EnumSet.of(Material.CACTUS, Material.SUGAR_CANE, Material.MELON, Material.PUMPKIN);
    	this.autoAgeableCrops = EnumSet.of(Material.SUGAR_CANE, Material.CACTUS);
    	this.fruitCrops = EnumSet.of(Material.PUMPKIN, Material.MELON);
    	this.cropGrowEvent = e;
    	this.blockCrop = cropGrowEvent.getNewState().getBlock();
    	this.typeCropNew= cropGrowEvent.getNewState().getType();
    	this.dataCropNew= cropGrowEvent.getNewState().getBlockData();
    	if ((!manualCrops.contains(typeCropNew)) && (!autoCrops.contains(typeCropNew))) { 
    		return;
    		
    		}else if (manualCrops.contains(typeCropNew)) {
    			Logger.print("Manual Crop Detected: 'growManual()', has been activated");	
    			growManual(cropGrowEvent);
    		
    		}else if (autoCrops.contains(typeCropNew)) {
    			if (fruitCrops.contains(typeCropNew)) {
    				Logger.print("Fruit Detected: 'growFruit()', has been activated");
    				growFruit(cropGrowEvent);
    			}else if (autoAgeableCrops.contains(typeCropNew)) {
    				growAutoAgeable(cropGrowEvent);
    				Logger.print("autoAgeableCrop Detected: 'growAutoAgeable()', has been activated");
    				   				
    		}}else {Logger.print("Unknown Crop Detected: Fix");}
    		}	
    Material typeAutoAgeable;		
    Block blockAutoAgeable;
    private void growAutoAgeable(BlockGrowEvent cropGrowEvent) {
    	if (!autoAgeableCrops.contains(typeCropNew)){
    		Logger.print("autoAgeableCrop has been detected, but not actually: Fix");
    		return;
    		
    	} else {
    		this.blockAutoAgeable = blockCrop;
    		this.typeAutoAgeable = typeCropNew;
    		Block above =  blockCrop.getLocation().add(0.0, 1.0, 0.0).getBlock();
    		Material aboveType = above.getType();
    		Material below2Type =blockAutoAgeable.getLocation().add(0.0, -2.0, 0.0).getBlock().getType();
    		if (typeAutoAgeable == Material.SUGAR_CANE) {
    			if ((aboveType == Material.AIR) && (below2Type != Material.SUGAR_CANE)) {
    				Logger.print("AIR has been detected ABOVE for _" + typeAutoAgeable);	
    				above.setType(Material.SUGAR_CANE);
    			}else {
    				Logger.print( typeAutoAgeable + "does NOT have AIR ABOVE to grow into");
    				return;
    			}
    		
    	}else if (typeAutoAgeable == Material.CACTUS) {
    		 if (checkForNeighbors() == true) {
    			Block blockCactus = blockCrop;
    			Location cactusLocation = blockCrop.getLocation();
    			Material typeCactus = cactusLocation.getBlock().getType();
    			if (typeCactus == Material.AIR) {
    				blockCactus.setType(Material.CACTUS);
    				blockCactus.breakNaturally();
    			}
       		}else if((aboveType == Material.AIR) && (below2Type != Material.CACTUS)) {
       			above.setType(Material.CACTUS);
       		} 
    	}}	    				
    		}   	    				
    		    			
    private boolean checkForNeighbors() {
    	if (blockAutoAgeable.getRelative(BlockFace.NORTH).getType() != Material.AIR) {  
    		Logger.print("Neighbor has been detected NORTH for _" + typeAutoAgeable);
    	return true;
    	}else if (blockAutoAgeable.getRelative(BlockFace.SOUTH).getType() != Material.AIR) {  
    		Logger.print("Neighbor has been detected SOUTH for _" + typeAutoAgeable );
        	return true;
    	}else if (blockAutoAgeable.getRelative(BlockFace.EAST).getType() != Material.AIR) { 
    		Logger.print("Neighbor has been detected EAST for _" + typeAutoAgeable );
        	return true;
    	}else if (blockAutoAgeable.getRelative(BlockFace.WEST).getType() != Material.AIR) {
    		Logger.print("Neighbor has been detected WEST for _" + typeAutoAgeable );
        	return true;
    	}else {
    		Logger.print("Neighbor for _" + typeAutoAgeable + "NOT DETECTED" );
    		return false;
    	}
    }
    
    private void growManual(BlockGrowEvent cropGrowEvent) {
    	Logger.print("'growManual()' has begun");
    	if (!manualCrops.contains(typeCropNew)){
    		Logger.print("Manual crop has been detected, but not actually: fix");
    		return;
    	} else {
    	cropGrowEvent.setCancelled(true);
    	Logger.print("cropGrowEvent" + "is cancelled_" + cropGrowEvent.isCancelled());	
    	BlockData dataManualCrop = dataCropNew;
    	Material typeManualCrop = typeCropNew;
    	Block blockManualCrop = blockCrop;
    	 int ageManualCrop =((Ageable) dataManualCrop).getAge();
    	 Logger.print(typeManualCrop + "'s Old Age is_" + ((Ageable)cropGrowEvent.getBlock().getBlockData()).getAge());
    	 int maxAgeManualCrop =((Ageable) dataManualCrop).getMaximumAge();
    	 int newAge =Math.min(ageManualCrop + 1, maxAgeManualCrop);
    	 if ((ageManualCrop + 1) > maxAgeManualCrop) {
    	Logger.print(typeCropNew + "'s possible NewAge" + (ageManualCrop + 1 )+ "is greater than max age_" + maxAgeManualCrop + "so its actual newAge is_" + newAge);
    	 }
    	 ((Ageable)dataManualCrop).setAge(newAge);
    	 Logger.print("New Age_" + newAge + "_has been set for_" + typeManualCrop);
    	 blockManualCrop.setBlockData((BlockData)(Ageable)dataManualCrop);
    	 Logger.print("New Age_" + newAge + "_has been applied to_" + typeManualCrop);
    	 Logger.print("If BlockGrowEvent was not cancelled_" + typeManualCrop + "'s Age would now have been_" + ageManualCrop );
    	 
    	}
    }

    private Set<Material> fruitSpawnable;
    private Block blockFruit;
    private Material fruit;
    private Material typeAttachedStem;
    
    private void growFruit(BlockGrowEvent cropGrowEvent) {
    	Material typeFruit = typeCropNew;
    	if (!fruitCrops.contains(typeFruit)) {
    		Logger.print("Fruit has been detected, but not actually: fix");
    		return;
    	} else {
    	scheduler.runTask(plugin, () -> {
    	Logger.print("1 Tick have passed");
    	Logger.print("Block" + cropGrowEvent.getBlock().getType() + "detected in growFruit()");
    	Logger.print("growFruit() has begun");	 		
    		this.fruitSpawnable = EnumSet.of(Material.DIRT, Material.GRASS_BLOCK, Material.PODZOL, Material.COARSE_DIRT, Material.MYCELIUM, Material.ROOTED_DIRT, Material.MOSS_BLOCK, Material.FARMLAND);
    		if (blockCrop == null) {
    			return;
    		}
    		this.blockFruit = blockCrop;
    		 if (typeFruit == Material.PUMPKIN) {
        		this.fruit = Material.PUMPKIN;
        		this.typeAttachedStem = Material.ATTACHED_PUMPKIN_STEM;
        		Logger.print("Pumpkin has been detected");
        		growExtraFruit();
        		
        	} else if (typeFruit == Material.MELON) {
        		this.fruit = Material.MELON;
        		this.typeAttachedStem = Material.ATTACHED_MELON_STEM;
        		Logger.print("Melon has been detected");
        		growExtraFruit();
        	
        	}});
    	
    	}	
    }
    Block blockStemAttached;
    
    private  Block getAttachedStem() {
    	if ((typeAttachedStem == blockFruit.getRelative(BlockFace.NORTH).getType()) 
				&& ((((Directional) (blockFruit.getRelative(BlockFace.NORTH)).getBlockData()).getFacing() == BlockFace.NORTH.getOppositeFace()))) {
    				this.blockStemAttached = blockFruit.getRelative(BlockFace.NORTH);
    				Logger.print("Attached Stem Found NORTH");
    		
    	}else if ((typeAttachedStem == blockFruit.getRelative(BlockFace.SOUTH).getType()) 
				&& ((((Directional) (blockFruit.getRelative(BlockFace.SOUTH)).getBlockData()).getFacing() == BlockFace.SOUTH.getOppositeFace()))){
					this.blockStemAttached = blockFruit.getRelative(BlockFace.SOUTH);
					Logger.print("Attached Stem Found SOUTH");
					
    	}else if ((typeAttachedStem == blockFruit.getRelative(BlockFace.EAST).getType()) 
				&& ((((Directional) (blockFruit.getRelative(BlockFace.EAST)).getBlockData()).getFacing() == BlockFace.EAST.getOppositeFace()))){
					this.blockStemAttached = blockFruit.getRelative(BlockFace.EAST);
					Logger.print("Attached Stem Found EAST");
				
    	}else if ((typeAttachedStem == blockFruit.getRelative(BlockFace.WEST).getType()) 
				&& ((((Directional) (blockFruit.getRelative(BlockFace.WEST)).getBlockData()).getFacing() == BlockFace.WEST.getOppositeFace()))){
					this.blockStemAttached = blockFruit.getRelative(BlockFace.WEST);
					Logger.print("Attached Stem Found WEST");
				
    	}else {Logger.print("Attached Stem Not Found");
               
    	}return blockStemAttached;
    }
    
    private void growExtraFruit() {
    	Block blockStemAttached = getAttachedStem();
		if (fruitSpawnable.contains(blockStemAttached.getRelative(BlockFace.NORTH).getLocation().add(0.0, -1.0, 0.0).getBlock().getType())
    			&& ((blockStemAttached.getRelative(BlockFace.NORTH).getType()) == (Material.AIR))){ 
					blockStemAttached.getRelative(BlockFace.NORTH).setType(fruit);
					Logger.print("ExtraFruit" + fruit + " grown NORTH of attached stem");
					
				} else if (fruitSpawnable.contains(blockStemAttached.getRelative(BlockFace.SOUTH).getLocation().add(0.0, -1.0, 0.0).getBlock().getType())
		    			&& ((blockStemAttached.getRelative(BlockFace.SOUTH).getType()) == (Material.AIR))){
							blockStemAttached.getRelative(BlockFace.SOUTH).setType(fruit);
							Logger.print("ExtraFruit" + fruit + " grown SOUTH of attached stem");
							
				} else if (fruitSpawnable.contains(blockStemAttached.getRelative(BlockFace.EAST).getLocation().add(0.0, -1.0, 0.0).getBlock().getType())
		    			&& ((blockStemAttached.getRelative(BlockFace.EAST).getType()) == (Material.AIR))){ 
							blockStemAttached.getRelative(BlockFace.EAST).setType(fruit);
							Logger.print("ExtraFruit" + fruit + " grown EAST of attached stem");
							
				} else if (fruitSpawnable.contains(blockStemAttached.getRelative(BlockFace.WEST).getLocation().add(0.0, -1.0, 0.0).getBlock().getType())
		    			&& ((blockStemAttached.getRelative(BlockFace.WEST).getType()) == (Material.AIR))){ 
							blockStemAttached.getRelative(BlockFace.WEST).setType(fruit);
							Logger.print("ExtraFruit" + fruit + " grown WEST of attached stem");		
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
