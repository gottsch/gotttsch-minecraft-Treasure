/**
 * 
 */
package com.someguyssoftware.treasure2.loot;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Table;
import com.someguyssoftware.gottschcore.loot.LootTableMaster2;
import com.someguyssoftware.gottschcore.loot.LootTableShell;
import com.someguyssoftware.gottschcore.mod.IMod;
import com.someguyssoftware.treasure2.Treasure;
import com.someguyssoftware.treasure2.enums.Rarity;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootParameterSets;

/**
 * Custom Loot Table (custom data) locations:
 * MC1.12 - (Minecraft) / saves / (world) / data /  loot_tables / (modID) / 
 * MC1.13+ (Minecraft) / saves / (world) / datapacks / (modID) / data / (modID) / loot_tables /
 * @author Mark Gottschling on Dec 2, 2020
 *
 */
public class TreasureLootTableMaster2 extends LootTableMaster2 {
	public static enum ManagedTableType {
		CHEST,
		INJECT
	}

	public static Logger LOGGER = LogManager.getLogger(Treasure.LOGGER.getName());


	//	public static final String CUSTOM_LOOT_TABLES_RESOURCE_PATH = "/loot_tables/";
	public static final String CUSTOM_LOOT_TABLE_KEY = "CUSTOM";

	/*
	 * relative location of chest loot tables - in resource path or file system.
	 * these are required folders.
	 */
//	@Deprecated
//	public static final List<String> CHEST_LOOT_TABLE_FOLDER_LOCATIONS = ImmutableList.of(
//			"chests/common",
//			"chests/uncommon",
//			"chests/scarce",
//			"chests/rare",
//			"chests/epic"
//			);

	/*
	 *  list of special loot table locations.
	 *  this is a required folder.
	 */
//	@Deprecated
//	public static final List<String> SPECIAL_CHEST_LOOT_TABLE_FOLDER_LOCATIONS = ImmutableList.of(
//			"chests/special"
//			);

	/*
	 *  list of inject loot table locations. items from these loot tables will be injected into the final item pool for chests
	 *  these are required folders.
	 */	
//	@Deprecated
//	public static final List<String> INJECT_LOOT_TABLE_FOLDER_LOCATIONS = ImmutableList.of(
//			"inject/common",
//			"inject/uncommon",
//			"inject/scarce",
//			"inject/rare",
//			"inject/epic"
//			);

	/*
	 * relative location of other loot tables - in resource path or file system.
	 * these are supporting loot tables that each contains a pool of items to pull from.
	 * this is what treasure2 uses to organize items, it is not necessary to follow this format
	 * in modded / added loot tables or supporting pools
	 */
//	@Deprecated
//	public static final List<String> POOL_LOOT_TABLE_FOLDER_LOCATIONS = ImmutableList.of(
//			"pools/treasure",
//			"pools/armor",
//			"pools/food",
//			"pools/items",
//			"pools/potions",
//			"pools/tools"
//			);

	/*
	 * Guava Table of loot table ResourceLocations for Chests based on LootTableManager-key and Rarity 
	 */
	private final Table<String, Rarity, List<ResourceLocation>> CHEST_LOOT_TABLES_RESOURCE_LOCATION_TABLE = HashBasedTable.create();

	/*
	 * Guava Table of loot table ResourceLocations for Injects based on a category-key and Rarity
	 */
	@Deprecated
	private final Table<String, Rarity, List<ResourceLocation>> INJECT_LOOT_TABLES_RESOURCE_LOCATION_TABLE = HashBasedTable.create();


	/*
	 * Guava Table of LootTableShell for Chests based on LootTableManager-key and Rarity
	 */
	private final Table<String, Rarity, List<LootTableShell>> CHEST_LOOT_TABLES_TABLE = HashBasedTable.create();

	/*
	 * Map of LootTableShell for Chests base on ResourceLocation
	 */
	private final Map<ResourceLocation, LootTableShell> CHEST_LOOT_TABLES_MAP = new HashMap<>();

	/*
	 * 
	 */
	private final Map<SpecialLootTables, LootTableShell> SPECIAL_LOOT_TABLES_MAP = new HashMap<>();

	/*
	 * 
	 */
	private final Table<String, Rarity, List<LootTableShell>> INJECT_LOOT_TABLES_TABLE = HashBasedTable.create();

	/*
	 * 
	 */
	//	public static TreasureLootTableMaster2 instance;

	/**
	 * 
	 * @param mod
	 */
	public TreasureLootTableMaster2(IMod mod) {
		super(mod);

		// initialize the maps
		for (Rarity r : Rarity.values()) {
			CHEST_LOOT_TABLES_RESOURCE_LOCATION_TABLE.put(CUSTOM_LOOT_TABLE_KEY, r, new ArrayList<ResourceLocation>());
			CHEST_LOOT_TABLES_TABLE.put(CUSTOM_LOOT_TABLE_KEY, r, new ArrayList<LootTableShell>());
		}
	}

	// TODO possibly remove or get new name
	// TEMP
	public void init(ServerWorld world) {
		//		Path path = Paths.get(world.getSaveHandler().getWorldDirectory().getPath(), "data", "loot_tables");
		Path path = Paths.get(world.getSaveHandler().getWorldDirectory().getPath(), "datapacks", "treasure2");
		setWorldDataBaseFolder(path.toFile());
		// TODO create pack.mcmeta if not already exist
		createPack();
		//		new LootContext.Builder(world).build(LootParameterSets.GENERIC);
	}

	/**
	 * 
	 */
	@Override
	public void clear() {
		super.clear();
		CHEST_LOOT_TABLES_TABLE.clear();
		CHEST_LOOT_TABLES_RESOURCE_LOCATION_TABLE.clear();
		CHEST_LOOT_TABLES_MAP.clear();
		SPECIAL_LOOT_TABLES_MAP.clear();
		INJECT_LOOT_TABLES_TABLE.clear();
		INJECT_LOOT_TABLES_RESOURCE_LOCATION_TABLE.clear();
	}

	protected void createPack() {
		Path packPath = Paths.get(getWorldDataBaseFolder().getPath(), "pack.mcmeta");
		try {
			// check if file already exists
			if (Files.notExists(packPath)) { 
				// TODO in future copy file from resources
				FileUtils.write(packPath.toFile(), "{\r\n" + 
						"  \"pack\": {\r\n" + 
						"    \"pack_format\": 5,\r\n" + 
						"    \"description\": \"treasure2\"\r\n" + 
						"  }\r\n" + 
						"}", Charset.defaultCharset());
			}
		} catch (Exception e) {
			Treasure.LOGGER.error("Unable to create pack.mcmeta:", e);
		}
	}
	
	/**
	 * TODO move this to GottschCore as this will be the defacto way to do it in 1.15+
	 * NOTE this doesn't check the versions of the resource file vs the file system file - just checks for the existance on the file system.
	 * @param modID
	 * @param resourceRelativePaths
	 */
	public void buildAndExpose(String basePath, String modID, List<String> resourceRelativePaths) {
		resourceRelativePaths.forEach(resourceRelativePath -> {
			Treasure.LOGGER.debug("TreasureLootTableMaster2 | buildAndExpose | processing relative resource path -> {}", resourceRelativePath);
			// this represents the entire file path
			Path fileSystemFilePath = Paths.get(getMod().getConfig().getConfigFolder(), getMod().getId(), "mc1_15", basePath, resourceRelativePath);
			Treasure.LOGGER.debug("TreasureLootTableMaster2 | buildAndExpose | file system path -> {}", fileSystemFilePath.toString());
			try {
				// check if file already exists
				if (Files.notExists(fileSystemFilePath)) { 
//					Path resourcePath = Paths.get(basePath, modID, resourceRelativePath);
					Path resourcePath = Paths.get("data", modID, basePath, resourceRelativePath);
					Treasure.LOGGER.debug("TreasureLootTableMaster2 | buildAndExpose | full resource path -> {}", resourcePath.toString());
					FileUtils.copyInputStreamToFile(Objects.requireNonNull(Treasure.class.getClassLoader().getResourceAsStream(resourcePath.toString())),
							fileSystemFilePath.toFile());
					// TODO add a flag if a file was copied over - save it in the manager
				}
			} catch (Exception e) {
				Treasure.LOGGER.error("Copying loot table resources error:", e);
			}
		});
	}

	/**
	 * 
	 * @param modID
	 * @param locations
	 */
	public void registerChests(String modID, List<String> locations) {
		for (String location : locations) {
			// get loot table files as ResourceLocations from the file system location
			List<ResourceLocation> resourceLocations = getLootTablesResourceLocations(modID, location);
			// load each ResourceLocation as LootTable and map it.
			for (ResourceLocation resourceLocation : resourceLocations) {
				Path path = Paths.get(resourceLocation.getPath());
				LOGGER.debug("path to resource loc -> {}", path.toString());
				// map the loot table resource location
				Rarity key = Rarity.valueOf(path.getName(path.getNameCount()-2).toString().toUpperCase());
				// add to resourcemap
				CHEST_LOOT_TABLES_RESOURCE_LOCATION_TABLE.get(CUSTOM_LOOT_TABLE_KEY, key).add(resourceLocation);
				// create loot table
				LOGGER.debug("loading loot table shell resource loc -> {}, {}", getWorldDataBaseFolder().getPath(), path.toString());
				Optional<LootTableShell> lootTable = loadLootTable(getWorldDataBaseFolder(), resourceLocation);
				if (lootTable.isPresent()) {
					// add resource location to table
					lootTable.get().setResourceLocation(resourceLocation);
					// add loot table to map
					CHEST_LOOT_TABLES_TABLE.get(CUSTOM_LOOT_TABLE_KEY, key).add(lootTable.get());
					LOGGER.debug("tabling loot table: {} {} -> {}", CUSTOM_LOOT_TABLE_KEY, key, resourceLocation);
					CHEST_LOOT_TABLES_MAP.put(resourceLocation, lootTable.get());
				}
				else {
					LOGGER.debug("unable to load loot table from -> {} : {}", getWorldDataBaseFolder(), resourceLocation);
				}
			}		
		}		
	}

	/**
	 * 
	 * @param modID
	 * @param locations
	 */
	public void registerSpecials(String modID, List<String> locations) {
		for (String location : locations) {
			List<ResourceLocation> specialLocations = getLootTablesResourceLocations(modID, location);
			LOGGER.debug("size of special chest loot table locations -> {}", specialLocations.size());
			// load each ResourceLocation as LootTable and map it.
			for (ResourceLocation resourceLocation : specialLocations) {
				Path path = Paths.get(resourceLocation.getPath());
				LOGGER.debug("path to special resource loc -> {}", path.toString());
				// create loot table
				Optional<LootTableShell> lootTable = loadLootTable(getWorldDataBaseFolder(), resourceLocation);
				if (lootTable.isPresent()) {
					// add resource location to table
					lootTable.get().setResourceLocation(resourceLocation);
					// add to map
					SpecialLootTables specialLootTables = SpecialLootTables.valueOf(com.google.common.io.Files.getNameWithoutExtension(path.getName(path.getNameCount()-1).toString().toUpperCase()));
					LOGGER.debug("special loot tables enum -> {}", specialLootTables);
					// add to special map
					SPECIAL_LOOT_TABLES_MAP.put(specialLootTables, lootTable.get());
					LOGGER.debug("tabling special loot table: {} -> {}", specialLootTables, resourceLocation);
					// add to the resource location -> lootTableShell map
					CHEST_LOOT_TABLES_MAP.put(resourceLocation, lootTable.get());
				}
				else {
					LOGGER.debug("unable to load special loot table from -> {} : {}", getWorldDataBaseFolder(), resourceLocation);
				}
			}
		}
	}

	/**
	 * 
	 * @param modID
	 * @param locations
	 */
	public void registerInjects(String modID, List<String> locations) {
		for (String location : locations) {
			List<ResourceLocation> resourceLocations = getLootTablesResourceLocations(modID, location);
			for (ResourceLocation resourceLocation : resourceLocations) {
				Path path = Paths.get(resourceLocation.getPath());
				LOGGER.debug("path to inject resource loc -> {}", path.toString());
				// map the loot table resource location
				Rarity rarity = Rarity.valueOf(path.getName(path.getNameCount()-2).toString().toUpperCase());
				// load loot table to get categories
				// create loot table
				Optional<LootTableShell> lootTable = loadLootTable(getWorldDataBaseFolder(), resourceLocation);
				if (lootTable.isPresent()) {
					// add resource location to table
					lootTable.get().setResourceLocation(resourceLocation);
					LOGGER.debug("loaded inject loot table shell -> {}", resourceLocation);
					List<String> keys = lootTable.get().getCategories();
					keys.forEach(key -> {
						LOGGER.debug("using inject key to table -> {}", key);
						key = key.isEmpty() ? "general" : key;
						if (!INJECT_LOOT_TABLES_RESOURCE_LOCATION_TABLE.containsRow(key)) {
							// initialize 
							for (Rarity r : Rarity.values()) {
								INJECT_LOOT_TABLES_RESOURCE_LOCATION_TABLE.put(key, r, new ArrayList<ResourceLocation>());
								INJECT_LOOT_TABLES_TABLE.put(key, r, new ArrayList<LootTableShell>());
							}
						}
						INJECT_LOOT_TABLES_RESOURCE_LOCATION_TABLE.get(key, rarity).add(resourceLocation);	
						INJECT_LOOT_TABLES_TABLE.get(key, rarity).add(lootTable.get());
						LOGGER.debug("tabling inject loot table: {} {} -> {}", key, rarity, resourceLocation);
					});
				}
			}
		}
	}

//	/**
//	 * Call in WorldEvent.Load event handler.
//	 * Overide this method if you have a different cache mechanism.
//	 * @param world
//	 * @param modID
//	 */
//	@Deprecated
//	public void register(String modID) {
//		// copy all folders/files from config to world data
//		moveLootTables(modID, "");
//
//		for (String location : CHEST_LOOT_TABLE_FOLDER_LOCATIONS) {
//			// get loot table files as ResourceLocations from the file system location
//			List<ResourceLocation> resourceLocations = getLootTablesResourceLocations(modID, location);
//			// load each ResourceLocation as LootTable and map it.
//			for (ResourceLocation resourceLocation : resourceLocations) {
//				Path path = Paths.get(resourceLocation.getPath());
//				LOGGER.debug("path to resource loc -> {}", path.toString());
//				// map the loot table resource location
//				Rarity key = Rarity.valueOf(path.getName(path.getNameCount()-2).toString().toUpperCase());
//				// add to resourcemap
//				CHEST_LOOT_TABLES_RESOURCE_LOCATION_TABLE.get(CUSTOM_LOOT_TABLE_KEY, key).add(resourceLocation);
//				// create loot table
//				Optional<LootTableShell> lootTable = loadLootTable(getWorldDataBaseFolder(), resourceLocation);
//				if (lootTable.isPresent()) {
//					// add resource location to table
//					lootTable.get().setResourceLocation(resourceLocation);
//					// add loot table to map
//					CHEST_LOOT_TABLES_TABLE.get(CUSTOM_LOOT_TABLE_KEY, key).add(lootTable.get());
//					LOGGER.debug("tabling loot table: {} {} -> {}", CUSTOM_LOOT_TABLE_KEY, key, resourceLocation);
//					CHEST_LOOT_TABLES_MAP.put(resourceLocation, lootTable.get());
//				}
//				else {
//					LOGGER.debug("unable to load loot table from -> {} : {}", getWorldDataBaseFolder(), resourceLocation);
//				}
//				// register it with MC
//				//				Seems that you don't have to manually register loot tables anymore
//				//				ResourceLocation vanillaLoc = LootTables.register(resourceLocation);
//				//				LOGGER.debug("vanillaLoc -> {}", vanillaLoc);
//			}		
//		}
//
//		/*
//		 *  register special loot tables
//		 */
//		for (String location : SPECIAL_CHEST_LOOT_TABLE_FOLDER_LOCATIONS) {
//			List<ResourceLocation> specialLocations = getLootTablesResourceLocations(modID, location);
//			LOGGER.debug("size of special chest loot table locations -> {}", specialLocations.size());
//			// load each ResourceLocation as LootTable and map it.
//			for (ResourceLocation resourceLocation : specialLocations) {
//				Path path = Paths.get(resourceLocation.getPath());
//				LOGGER.debug("path to special resource loc -> {}", path.toString());
//				// create loot table
//				Optional<LootTableShell> lootTable = loadLootTable(getWorldDataBaseFolder(), resourceLocation);
//				if (lootTable.isPresent()) {
//					// add resource location to table
//					lootTable.get().setResourceLocation(resourceLocation);
//					// add to map
//					SpecialLootTables specialLootTables = SpecialLootTables.valueOf(com.google.common.io.Files.getNameWithoutExtension(path.getName(path.getNameCount()-1).toString().toUpperCase()));
//					LOGGER.debug("special loot tables enum -> {}", specialLootTables);
//					// add to special map
//					SPECIAL_LOOT_TABLES_MAP.put(specialLootTables, lootTable.get());
//					LOGGER.debug("tabling special loot table: {} -> {}", specialLootTables, resourceLocation);
//					// add to the resource location -> lootTableShell map
//					CHEST_LOOT_TABLES_MAP.put(resourceLocation, lootTable.get());
//					// register with vanilla
//					//					LootTableList.register(resourceLocation);
//				}
//				else {
//					LOGGER.debug("unable to load special loot table from -> {} : {}", getWorldDataBaseFolder(), resourceLocation);
//				}
//			}
//		}
//
//		/*
//		 * register inject loot tables
//		 * 
//		 */
//		for (String location : INJECT_LOOT_TABLE_FOLDER_LOCATIONS) {
//			List<ResourceLocation> resourceLocations = getLootTablesResourceLocations(modID, location);
//			for (ResourceLocation resourceLocation : resourceLocations) {
//				Path path = Paths.get(resourceLocation.getPath());
//				LOGGER.debug("path to inject resource loc -> {}", path.toString());
//				// map the loot table resource location
//				Rarity rarity = Rarity.valueOf(path.getName(path.getNameCount()-2).toString().toUpperCase());
//				// load loot table to get categories
//				// create loot table
//				Optional<LootTableShell> lootTable = loadLootTable(getWorldDataBaseFolder(), resourceLocation);
//				if (lootTable.isPresent()) {
//					// add resource location to table
//					lootTable.get().setResourceLocation(resourceLocation);
//					LOGGER.debug("loaded inject loot table shell -> {}", resourceLocation);
//					List<String> keys = lootTable.get().getCategories();
//					keys.forEach(key -> {
//						LOGGER.debug("using inject key to table -> {}", key);
//						key = key.isEmpty() ? "general" : key;
//						if (!INJECT_LOOT_TABLES_RESOURCE_LOCATION_TABLE.containsRow(key)) {
//							// initialize 
//							for (Rarity r : Rarity.values()) {
//								INJECT_LOOT_TABLES_RESOURCE_LOCATION_TABLE.put(key, r, new ArrayList<ResourceLocation>());
//								INJECT_LOOT_TABLES_TABLE.put(key, r, new ArrayList<LootTableShell>());
//							}
//						}
//						INJECT_LOOT_TABLES_RESOURCE_LOCATION_TABLE.get(key, rarity).add(resourceLocation);	
//						INJECT_LOOT_TABLES_TABLE.get(key, rarity).add(lootTable.get());
//						LOGGER.debug("tabling inject loot table: {} {} -> {}", key, rarity, resourceLocation);
//					});
//				}
//				//				LootTableList.register(resourceLocation);
//			}
//		}
//	}

	/**
	 * 
	 * @param modID
	 * @param location
	 */
	protected void moveLootTables(String modID, String location) {
		Path configFilePath = Paths.get(getMod().getConfig().getConfigFolder(), modID, "mc1_15", LOOT_TABLES_FOLDER, location).toAbsolutePath();
//		Path worldDataFilePath = Paths.get(getWorldDataBaseFolder().toString(), modID, location).toAbsolutePath();
		Path worldDataFilePath = Paths.get(getWorldDataBaseFolder().toString(), "data", modID, "loot_tables", location).toAbsolutePath();

		Set<String> fileList = new HashSet<>();
		try {
			Files.walkFileTree(configFilePath, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					// grab everything after loot_tables
					String destinationStr = dir.toString();			        	
					String partial = destinationStr.substring(destinationStr.indexOf(LOOT_TABLES_FOLDER) + LOOT_TABLES_FOLDER.length());
					Path destinationFilePath = Paths.get(worldDataFilePath.toString(), partial);
					LOGGER.debug("destination folder to be tested/created -> {}", destinationFilePath.toString());
					if (Files.notExists(destinationFilePath)) {
						try {
							Files.createDirectories(destinationFilePath);
						} catch (IOException e) {
							LOGGER.warn("Unable to create world data loot tables folder \"{}\"", destinationFilePath.toString());
						}
					}					
					return super.preVisitDirectory(dir, attrs);
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
						throws IOException {
					LOGGER.debug("walking file -> {}", file.toString());
					fileList.add(file.getFileName().toString());
					String destinationStr = file.toString();			        	
					String partial = destinationStr.substring(destinationStr.indexOf(LOOT_TABLES_FOLDER) + LOOT_TABLES_FOLDER.length());
					Path destinationFilePath = Paths.get(worldDataFilePath.toString(), partial);
					LOGGER.debug("new destination -> {}", destinationFilePath.toString());
					if (Files.notExists(destinationFilePath)) {
						// copy from resource/classpath to file path
						try {
							Files.copy(file, destinationFilePath, StandardCopyOption.REPLACE_EXISTING);
						}
						catch(IOException e ) {
							LOGGER.error(String.format("could not copy file %s to %s", file.toString(), destinationFilePath.toString()), e);
						}
					}
					else {
						boolean isCurrent  = isWorldDataVersionCurrent(file, destinationFilePath);
						LOGGER.debug("is world data loot table {} current -> {}", destinationFilePath, isCurrent);
						if (!isCurrent) {
							Files.move(
									destinationFilePath, 
									Paths.get(destinationFilePath.getFileName().toString() + ".bak").toAbsolutePath(), 
									StandardCopyOption.REPLACE_EXISTING);
							Files.copy(file, destinationFilePath);
						}
					}
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			LOGGER.error(String.format("an errored while file walking the location -> %s:", configFilePath), e);
			return;
		}
	}

	/**
	 * 
	 * @param rarity
	 * @return
	 */
	public List<LootTableShell> getLootTableByRarity(Rarity rarity) {
		// get all loot tables by column key
		List<LootTableShell> tables = new ArrayList<>();
		Map<String, List<LootTableShell>> mapOfLootTables = CHEST_LOOT_TABLES_TABLE.column(rarity);
		// convert to a single list
		for(Entry<String, List<LootTableShell>> n : mapOfLootTables.entrySet()) {
			Treasure.LOGGER.debug("Adding table shell entry to loot table list -> {} {}: size {}", rarity, n.getKey(), n.getValue().size());
			tables.addAll(n.getValue());
		}
		return tables;
	}

	/**
	 * 
	 * @param location
	 * @return
	 */
	public Optional<LootTableShell> getLootTableByResourceLocation(ResourceLocation location) {
		LootTableShell lootTableShell = CHEST_LOOT_TABLES_MAP.get(location);
		return Optional.ofNullable(lootTableShell);
	}

	/**
	 * 
	 * @param tableType
	 * @param rarity
	 * @return
	 */
	public List<LootTableShell> getLootTableByRarity(ManagedTableType tableType, Rarity rarity) {
		Treasure.LOGGER.debug("managed table type -> {}", tableType);
		Table<String, Rarity, List<LootTableShell>> table = (tableType == ManagedTableType.CHEST) ? CHEST_LOOT_TABLES_TABLE : INJECT_LOOT_TABLES_TABLE;
		// get all loot tables by column key
		List<LootTableShell> tables = new ArrayList<>();
		Map<String, List<LootTableShell>> mapOfLootTables = table.column(rarity);
		// convert to a single list
		for(Entry<String, List<LootTableShell>> n : mapOfLootTables.entrySet()) {
			Treasure.LOGGER.debug("Adding table shell entry to loot table list -> {} {}: size {}", rarity, n.getKey(), n.getValue().size());
			tables.addAll(n.getValue());
		}
		return tables;
	}

	/**
	 * 
	 * @param tableType
	 * @param key
	 * @param rarity
	 * @return
	 */
	public List<LootTableShell> getLootTableByKeyRarity(ManagedTableType tableType, String key, Rarity rarity) {
		Table<String, Rarity, List<LootTableShell>> table = (tableType == ManagedTableType.CHEST) ? CHEST_LOOT_TABLES_TABLE : INJECT_LOOT_TABLES_TABLE;
		// get all loot tables by column key
		List<LootTableShell> tables = table.get(key, rarity);
		return tables;
	}

	/**
	 * 
	 * @param rarity
	 * @return
	 */
	public List<ResourceLocation> getLootTableResourceByRarity(Rarity rarity) {
		// get all loot tables by column key
		List<ResourceLocation> tables = new ArrayList<>();
		Map<String, List<ResourceLocation>> mapOfLootTableResourceLocations = CHEST_LOOT_TABLES_RESOURCE_LOCATION_TABLE.column(rarity);
		// convert to a single list
		for(Entry<String, List<ResourceLocation>> n : mapOfLootTableResourceLocations.entrySet()) {
			tables.addAll(n.getValue());
		}
		return tables;		
	}

	/**
	 * 
	 * @param tableEnum
	 * @return
	 */
	public LootTableShell getSpecialLootTable(SpecialLootTables table) {
		Treasure.LOGGER.debug("searching for special loot table --> {}", table);

		LootTableShell lootTable = SPECIAL_LOOT_TABLES_MAP.get(table);
		return lootTable;
	}

	/**
	 * 
	 * @param lootTableShell
	 * @param defaultRarity
	 * @return
	 */
	public Rarity getEffectiveRarity(LootTableShell lootTableShell, Rarity defaultRarity) {
		return !StringUtils.isNullOrEmpty(lootTableShell.getRarity()) ? Rarity.getByValue(lootTableShell.getRarity().toLowerCase()) : defaultRarity;
	}

	/*
	 * Enum of special loot tables (not necessarily chests)
	 */
	public enum SpecialLootTables {
		WITHER_CHEST,
		SKULL_CHEST,
		GOLD_SKULL_CHEST,
		CRYSTAL_SKULL_CHEST,
		CAULDRON_CHEST,
		SILVER_WELL,
		GOLD_WELL,
		WHITE_PEARL_WELL,
		BLACK_PEARL_WELL;

		/**
		 * 
		 * @return
		 */
		public static List<String> getNames() {
			List<String> names = EnumSet.allOf(SpecialLootTables.class).stream().map(x -> x.name()).collect(Collectors.toList());
			return names;
		}		
	}
}