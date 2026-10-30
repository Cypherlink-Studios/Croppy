package dev.darkblade.croppy.core.species;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.species.CropDisplay;
import dev.darkblade.croppy.api.species.CropRequirements;
import dev.darkblade.croppy.api.species.HarvestDefinition;
import dev.darkblade.croppy.api.species.Species;
import dev.darkblade.croppy.api.species.StageDefinition;
import dev.darkblade.croppy.api.species.YieldEntry;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Parses and strictly validates YAML definitions for crop species.
 */
public class YamlSpeciesLoader {

    private final Yaml yaml;

    public YamlSpeciesLoader() {
        LoaderOptions options = new LoaderOptions();
        this.yaml = new Yaml(new SafeConstructor(options));
    }

    /**
     * Parses a species definition from an input stream.
     *
     * @param in the input stream containing YAML
     * @param sourceName name of the source (for error messages)
     * @return the parsed and validated species
     * @throws IllegalArgumentException if the YAML is invalid or violates constraints
     */
    @SuppressWarnings("unchecked")
    public Species load(InputStream in, String sourceName) {
        Objects.requireNonNull(in, "Input stream cannot be null");
        Object loaded = yaml.load(in);
        if (!(loaded instanceof Map<?, ?> rawMap)) {
            throw new IllegalArgumentException("YAML content in " + sourceName + " must be a key-value mapping");
        }

        Map<String, Object> root = (Map<String, Object>) rawMap;

        // 1. Key and Name
        Object rawId = root.get("id");
        if (rawId == null || rawId.toString().isBlank()) {
            throw new IllegalArgumentException("Missing required 'id' in species file: " + sourceName);
        }
        Key key = Key.parse(rawId.toString().trim());

        String name = root.getOrDefault("name", key.value()).toString();

        // 2. Display
        Object rawDisplay = root.get("display");
        if (!(rawDisplay instanceof Map<?, ?> displayMap)) {
            throw new IllegalArgumentException("Missing required 'display' section in: " + sourceName);
        }
        Object rawType = displayMap.get("type");
        String displayType = rawType != null ? rawType.toString() : "vanilla";
        Object rawBlock = displayMap.get("block");
        if (rawBlock == null || rawBlock.toString().isBlank()) {
            throw new IllegalArgumentException("Missing required 'display.block' in: " + sourceName);
        }
        Object rawSeedItem = displayMap.get("seed_item");
        if (rawSeedItem == null || rawSeedItem.toString().isBlank()) {
            rawSeedItem = displayMap.get("seed");
        }
        if (rawSeedItem == null || rawSeedItem.toString().isBlank()) {
            rawSeedItem = displayMap.get("seed_id");
        }
        if (rawSeedItem == null || rawSeedItem.toString().isBlank()) {
            rawSeedItem = root.get("seed");
        }
        if (rawSeedItem == null || rawSeedItem.toString().isBlank()) {
            throw new IllegalArgumentException("Missing required 'display.seed_item' or 'seed' in: " + sourceName);
        }
        CropDisplay display = new CropDisplay(
                displayType,
                rawBlock.toString().trim().toLowerCase(),
                rawSeedItem.toString().trim().toLowerCase()
        );

        // 3. Stages
        Object rawStages = root.get("stages");
        if (!(rawStages instanceof List<?> stageList) || stageList.isEmpty()) {
            throw new IllegalArgumentException("Species must have at least one stage under 'stages' in: " + sourceName);
        }

        List<StageDefinition> stages = new ArrayList<>();
        Set<String> stageIds = new HashSet<>();

        for (int i = 0; i < stageList.size(); i++) {
            Object item = stageList.get(i);
            if (!(item instanceof Map<?, ?> stageMap)) {
                throw new IllegalArgumentException("Stage #" + i + " must be a map in: " + sourceName);
            }

            Object stageIdObj = stageMap.get("id");
            if (stageIdObj == null || stageIdObj.toString().isBlank()) {
                throw new IllegalArgumentException("Stage #" + i + " is missing 'id' in: " + sourceName);
            }
            String stageId = stageIdObj.toString().trim();
            if (!stageIds.add(stageId)) {
                throw new IllegalArgumentException("Duplicate stage id '" + stageId + "' in: " + sourceName);
            }

            double progressRequired = 0.0;
            Object progressObj = stageMap.get("progress_required");
            if (progressObj instanceof Number num) {
                progressRequired = num.doubleValue();
            }
            if (progressRequired < 0) {
                throw new IllegalArgumentException("Stage '" + stageId + "' progress_required cannot be negative in: " + sourceName);
            }

            int blockAge = 0;
            Object stageDisplayObj = stageMap.get("display");
            if (stageDisplayObj instanceof Map<?, ?> stageDisplayMap) {
                Object ageObj = stageDisplayMap.get("block_age");
                if (ageObj instanceof Number ageNum) {
                    blockAge = ageNum.intValue();
                }
            }

            stages.add(new StageDefinition(stageId, progressRequired, blockAge));
        }

        // 4. Requirements
        int minLight = 9;
        int waterFluidLevel = 0;
        Object rawRequirements = root.get("requirements");
        if (rawRequirements instanceof Map<?, ?> reqMap) {
            Object lightObj = reqMap.get("min_light");
            if (lightObj instanceof Number lNum) {
                minLight = lNum.intValue();
            }
            Object waterObj = reqMap.get("water_fluid_level");
            if (waterObj instanceof Number wNum) {
                waterFluidLevel = wNum.intValue();
            }
        }
        CropRequirements requirements = new CropRequirements(minLight, waterFluidLevel);

        // 5. Harvest
        List<YieldEntry> yieldEntries = new ArrayList<>();
        Object rawHarvest = root.get("harvest");
        if (rawHarvest instanceof Map<?, ?> harvestMap) {
            Object rawYieldList = harvestMap.get("yield");
            if (rawYieldList instanceof List<?> yList) {
                for (Object yObj : yList) {
                    if (yObj instanceof Map<?, ?> yMap) {
                        Object itemObj = yMap.get("item");
                        if (itemObj == null || itemObj.toString().isBlank()) {
                            continue;
                        }
                        String itemId = itemObj.toString().trim().toLowerCase();

                        int min = 1;
                        int max = 1;

                        Object minObj = yMap.get("min");
                        if (minObj instanceof Number mNum) {
                            min = mNum.intValue();
                            max = min;
                        }
                        Object maxObj = yMap.get("max");
                        if (maxObj instanceof Number maxNum) {
                            max = maxNum.intValue();
                        }

                        // Also support amount string "1-3" or integer
                        Object amountObj = yMap.get("amount");
                        if (amountObj != null) {
                            String amtStr = amountObj.toString().trim();
                            if (amtStr.contains("-")) {
                                String[] parts = amtStr.split("-");
                                min = Integer.parseInt(parts[0].trim());
                                max = Integer.parseInt(parts[1].trim());
                            } else {
                                min = Integer.parseInt(amtStr);
                                max = min;
                            }
                        }

                        yieldEntries.add(new YieldEntry(itemId, min, max));
                    }
                }
            }
        }
        HarvestDefinition harvest = new HarvestDefinition(yieldEntries);

        return new SpeciesDefinition(key, name, display, stages, requirements, harvest);
    }

    /**
     * Loads all species definitions from .yml and .yaml files in the specified directory.
     *
     * @param directory directory containing species configuration files
     * @param logger logger to output validation warnings
     * @return list of successfully loaded species
     */
    public List<Species> loadDirectory(Path directory, Logger logger) {
        List<Species> loaded = new ArrayList<>();
        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            return loaded;
        }

        try (Stream<Path> stream = Files.walk(directory, 1)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".yml") || p.toString().endsWith(".yaml"))
                    .forEach(file -> {
                        try (InputStream in = Files.newInputStream(file)) {
                            Species species = load(in, file.getFileName().toString());
                            loaded.add(species);
                            if (logger != null) {
                                logger.info("Loaded crop species: " + species.getKey() + " (" + species.getName() + ")");
                            }
                        } catch (Exception ex) {
                            if (logger != null) {
                                logger.log(Level.SEVERE, "Failed to load species from " + file.getFileName() + ": " + ex.getMessage(), ex);
                            }
                        }
                    });
        } catch (IOException e) {
            if (logger != null) {
                logger.log(Level.SEVERE, "Error reading species directory: " + directory, e);
            }
        }

        return loaded;
    }
}
