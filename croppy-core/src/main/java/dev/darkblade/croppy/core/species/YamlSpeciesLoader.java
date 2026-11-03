package dev.darkblade.croppy.core.species;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.species.CropDisplay;
import dev.darkblade.croppy.api.species.CropRequirements;
import dev.darkblade.croppy.api.species.HarvestDefinition;
import dev.darkblade.croppy.api.species.Model3DDefinition;
import dev.darkblade.croppy.api.species.RotationMode;
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
import java.util.Locale;
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
            rawBlock = displayMap.get("base_block");
        }
        if (rawBlock == null || rawBlock.toString().isBlank()) {
            if ("model_3d".equalsIgnoreCase(displayType)) {
                rawBlock = "minecraft:air";
            } else {
                throw new IllegalArgumentException("Missing required 'display.block' in: " + sourceName);
            }
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

        Object rawRotationMode = displayMap.get("rotation_mode");
        RotationMode rotationMode = RotationMode.CARDINAL_4;
        if (rawRotationMode != null) {
            try {
                rotationMode = RotationMode.valueOf(rawRotationMode.toString().trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid rotation_mode '" + rawRotationMode + "' in: " + sourceName);
            }
        }

        CropDisplay display = new CropDisplay(
                displayType,
                rawBlock.toString().trim().toLowerCase(Locale.ROOT),
                rawSeedItem.toString().trim().toLowerCase(Locale.ROOT),
                rotationMode
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
            if (progressObj == null) {
                progressObj = stageMap.get("progress");
            }
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

            Model3DDefinition model3D = null;
            Object modelObj = stageMap.get("model");
            if (modelObj == null) {
                modelObj = stageMap.get("model_3d");
            }
            if (modelObj instanceof Map<?, ?> modelMap) {
                model3D = parseModel3D(modelMap, stageId, sourceName);
            }

            stages.add(new StageDefinition(stageId, progressRequired, blockAge, model3D));
        }

        // 4. Requirements
        int minLight = 9;
        int waterFluidLevel = 0;
        Object rawRequirements = root.get("requirements");
        if (rawRequirements instanceof Map<?, ?> reqMap) {
            Object lightObj = reqMap.get("min_light");
            if (lightObj instanceof Number num) {
                minLight = num.intValue();
            }
            Object waterObj = reqMap.get("water_fluid_level");
            if (waterObj instanceof Number num) {
                waterFluidLevel = num.intValue();
            }
        }
        if (minLight < 0 || minLight > 15) {
            throw new IllegalArgumentException("min_light must be between 0 and 15 in: " + sourceName);
        }
        if (waterFluidLevel < 0 || waterFluidLevel > 8) {
            throw new IllegalArgumentException("water_fluid_level must be between 0 and 8 in: " + sourceName);
        }
        CropRequirements requirements = new CropRequirements(minLight, waterFluidLevel);

        // 5. Harvest
        List<YieldEntry> yieldEntries = new ArrayList<>();
        Object rawHarvest = root.get("harvest");
        if (rawHarvest instanceof Map<?, ?> harvestMap) {
            Object rawYieldList = harvestMap.get("yield");
            if (rawYieldList == null) {
                rawYieldList = harvestMap.get("drops");
            }
            if (rawYieldList instanceof List<?> yList) {
                for (Object yObj : yList) {
                    if (yObj instanceof Map<?, ?> yMap) {
                        Object itemObj = yMap.get("item");
                        if (itemObj == null || itemObj.toString().isBlank()) {
                            continue;
                        }
                        String itemId = itemObj.toString().trim().toLowerCase(Locale.ROOT);

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

    private Model3DDefinition parseModel3D(Map<?, ?> map, String stageId, String sourceName) {
        Object rawItemModel = map.get("item_model");
        if (rawItemModel == null || rawItemModel.toString().isBlank()) {
            rawItemModel = map.get("model");
        }
        if (rawItemModel == null || rawItemModel.toString().isBlank()) {
            rawItemModel = map.get("item_id");
        }
        if (rawItemModel == null || rawItemModel.toString().isBlank()) {
            throw new IllegalArgumentException("Stage '" + stageId + "' model is missing 'item_model' in: " + sourceName);
        }
        Key itemModelKey = Key.parse(rawItemModel.toString().trim());

        float scaleX = 1.0f;
        float scaleY = 1.0f;
        float scaleZ = 1.0f;
        Object scaleObj = map.get("scale");
        if (scaleObj instanceof List<?> scaleList && scaleList.size() >= 3) {
            scaleX = parseNumber(scaleList.get(0), 1.0f);
            scaleY = parseNumber(scaleList.get(1), 1.0f);
            scaleZ = parseNumber(scaleList.get(2), 1.0f);
        } else if (scaleObj instanceof Number num) {
            scaleX = num.floatValue();
            scaleY = num.floatValue();
            scaleZ = num.floatValue();
        }

        float transX = 0.0f;
        float transY = 0.0f;
        float transZ = 0.0f;
        Object transObj = map.get("translation");
        if (transObj instanceof List<?> transList && transList.size() >= 3) {
            transX = parseNumber(transList.get(0), 0.0f);
            transY = parseNumber(transList.get(1), 0.0f);
            transZ = parseNumber(transList.get(2), 0.0f);
        }

        float hitboxWidth = 0.8f;
        float hitboxHeight = 0.8f;
        Object hitboxObj = map.get("hitbox");
        if (hitboxObj instanceof Map<?, ?> hitboxMap) {
            Object widthObj = hitboxMap.get("width");
            if (widthObj instanceof Number num) {
                hitboxWidth = num.floatValue();
            }
            Object heightObj = hitboxMap.get("height");
            if (heightObj instanceof Number num) {
                hitboxHeight = num.floatValue();
            }
        } else {
            Object widthObj = map.get("hitbox_width");
            if (widthObj instanceof Number num) {
                hitboxWidth = num.floatValue();
            }
            Object heightObj = map.get("hitbox_height");
            if (heightObj instanceof Number num) {
                hitboxHeight = num.floatValue();
            }
        }

        return new Model3DDefinition(itemModelKey, scaleX, scaleY, scaleZ, transX, transY, transZ, hitboxWidth, hitboxHeight);
    }

    private float parseNumber(Object obj, float fallback) {
        if (obj instanceof Number num) {
            return num.floatValue();
        }
        return fallback;
    }

    /**
     * Loads all YAML species files from a directory.
     *
     * @param directory the directory to scan
     * @param logger logger for diagnostic output
     * @return list of loaded species
     */
    public List<Species> loadDirectory(Path directory, Logger logger) {
        Objects.requireNonNull(directory, "Directory cannot be null");
        List<Species> loaded = new ArrayList<>();

        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            return loaded;
        }

        try (Stream<Path> stream = Files.walk(directory, 1)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".yml") || p.toString().endsWith(".yaml"))
                    .forEach(path -> {
                        try (InputStream in = Files.newInputStream(path)) {
                            Species species = load(in, path.getFileName().toString());
                            loaded.add(species);
                            if (logger != null) {
                                logger.info("Loaded crop species: " + species.getKey());
                            }
                        } catch (Exception e) {
                            if (logger != null) {
                                logger.log(Level.SEVERE, "Failed to load species from: " + path, e);
                            }
                        }
                    });
        } catch (IOException e) {
            if (logger != null) {
                logger.log(Level.SEVERE, "Failed to read species directory: " + directory, e);
            }
        }

        return loaded;
    }
}
