package dev.darkblade.croppy.core.menu;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.menu.ClickTypeFilter;
import dev.darkblade.croppy.api.menu.ConditionalTemplate;
import dev.darkblade.croppy.api.menu.MenuAction;
import dev.darkblade.croppy.api.menu.MenuActionType;
import dev.darkblade.croppy.api.menu.MenuDefinition;
import dev.darkblade.croppy.api.menu.MenuItemTemplate;
import dev.darkblade.croppy.api.menu.MenuRegistry;
import dev.darkblade.croppy.api.menu.SlotDefinition;
import dev.darkblade.croppy.api.menu.SlotType;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Parses and loads YAML configuration files into {@link MenuDefinition} layouts.
 */
public final class YamlMenuLoader {

    private final Yaml yaml;

    public YamlMenuLoader() {
        LoaderOptions options = new LoaderOptions();
        this.yaml = new Yaml(new SafeConstructor(options));
    }

    /**
     * Parses a single menu definition from an input stream.
     *
     * @param in         the input stream
     * @param sourceName name of source file or resource for diagnostic logging
     * @return the parsed menu definition
     * @throws IllegalArgumentException if required fields are missing or invalid
     */
    @SuppressWarnings("unchecked")
    public MenuDefinition load(InputStream in, String sourceName) {
        Objects.requireNonNull(in, "in cannot be null");
        Object loaded = yaml.load(in);
        if (!(loaded instanceof Map<?, ?> rawMap)) {
            throw new IllegalArgumentException("YAML content in " + sourceName + " must be a key-value mapping");
        }

        Map<String, Object> root = (Map<String, Object>) rawMap;

        // 1. ID / Key
        Object rawId = root.get("id");
        if (rawId == null || rawId.toString().isBlank()) {
            throw new IllegalArgumentException("Missing or empty 'id' field in menu definition " + sourceName);
        }
        String idStr = rawId.toString().trim();
        Key key = idStr.contains(":") ? Key.parse(idStr) : Key.of("croppy", idStr);

        // 2. Title
        String title = getString(root, "title", getString(root, "name", key.value()));

        // 3. Pattern & Rows
        Object rawPattern = root.get("pattern");
        if (!(rawPattern instanceof List<?> patternList) || patternList.isEmpty()) {
            throw new IllegalArgumentException("Missing or empty 'pattern' list in menu definition " + sourceName);
        }

        int rows = getInt(root, "rows", patternList.size());
        if (rows < 1 || rows > 6) {
            throw new IllegalArgumentException("Menu rows must be between 1 and 6 in " + sourceName + ", got: " + rows);
        }

        if (patternList.size() != rows) {
            throw new IllegalArgumentException("Pattern lines count (" + patternList.size() + ") does not match rows count (" + rows + ") in " + sourceName);
        }

        List<String> sanitizedPattern = new ArrayList<>();
        for (int r = 0; r < patternList.size(); r++) {
            Object lineObj = patternList.get(r);
            if (lineObj == null) {
                throw new IllegalArgumentException("Pattern line " + r + " cannot be null in " + sourceName);
            }
            String line = lineObj.toString().trim();
            String sanitized = line.replace(" ", "");
            if (sanitized.length() != 9) {
                throw new IllegalArgumentException("Pattern row " + r + " length must be exactly 9, got " + sanitized.length() + ": '" + line + "' in " + sourceName);
            }
            sanitizedPattern.add(sanitized);
        }

        // 4. Elements
        Map<Character, SlotDefinition> elements = new HashMap<>();
        Object rawElements = root.get("elements");
        if (rawElements instanceof Map<?, ?> elementsMap) {
            for (Map.Entry<?, ?> entry : elementsMap.entrySet()) {
                if (entry.getKey() == null || !(entry.getValue() instanceof Map<?, ?> valMap)) {
                    continue;
                }
                String charKeyStr = entry.getKey().toString().trim();
                if (charKeyStr.isEmpty()) {
                    continue;
                }
                char symbol = charKeyStr.charAt(0);
                Map<String, Object> elemMap = (Map<String, Object>) valMap;

                SlotDefinition slotDef = parseSlotDefinition(elemMap, sourceName, symbol);
                elements.put(symbol, slotDef);
            }
        }

        // 5. Properties
        Map<String, String> properties = new HashMap<>();
        Object rawProps = root.get("properties");
        if (rawProps instanceof Map<?, ?> pMap) {
            for (Map.Entry<?, ?> entry : pMap.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    properties.put(entry.getKey().toString(), entry.getValue().toString());
                }
            }
        }

        return new MenuDefinition(key, title, rows, sanitizedPattern, elements, properties);
    }

    @SuppressWarnings("unchecked")
    private SlotDefinition parseSlotDefinition(Map<String, Object> map, String sourceName, char symbol) {
        String typeStr = getString(map, "type", "DECORATION").toUpperCase(Locale.ROOT);
        SlotType type;
        try {
            type = SlotType.valueOf(typeStr);
        } catch (IllegalArgumentException ex) {
            type = SlotType.DECORATION;
        }

        MenuItemTemplate defaultTemplate = null;
        Object rawItem = map.get("item");
        if (rawItem instanceof Map<?, ?> itemMap) {
            defaultTemplate = parseItemTemplate((Map<String, Object>) itemMap);
        }

        // Conditional States
        List<ConditionalTemplate> states = new ArrayList<>();
        Object rawStates = rootGetList(map, "states");
        if (rawStates instanceof List<?> stateList) {
            for (Object stateObj : stateList) {
                if (stateObj instanceof Map<?, ?> stateMap) {
                    Map<String, Object> sMap = (Map<String, Object>) stateMap;
                    String condition = getString(sMap, "condition", "");
                    MenuItemTemplate stateItem = null;
                    Object stateRawItem = sMap.get("item");
                    if (stateRawItem instanceof Map<?, ?> stItemMap) {
                        stateItem = parseItemTemplate((Map<String, Object>) stItemMap);
                    }
                    if (!condition.isBlank() && stateItem != null) {
                        states.add(new ConditionalTemplate(condition, stateItem));
                    }
                }
            }
        }

        // Actions
        List<MenuAction> actions = new ArrayList<>();
        Object rawActions = rootGetList(map, "actions");
        if (rawActions instanceof List<?> actionList) {
            for (Object actObj : actionList) {
                if (actObj instanceof Map<?, ?> actMap) {
                    Map<String, Object> aMap = (Map<String, Object>) actMap;
                    String clickStr = getString(aMap, "click", "ALL");
                    ClickTypeFilter click = ClickTypeFilter.parse(clickStr);

                    String actionTypeStr = getString(aMap, "type", "STATION_ACTION").toUpperCase(Locale.ROOT);
                    MenuActionType actionType;
                    try {
                        actionType = MenuActionType.valueOf(actionTypeStr);
                    } catch (IllegalArgumentException ex) {
                        actionType = MenuActionType.STATION_ACTION;
                    }

                    String target = getString(aMap, "target", getString(aMap, "action", getString(aMap, "command", getString(aMap, "sound", ""))));
                    String condition = getString(aMap, "condition", null);

                    actions.add(new MenuAction(click, actionType, target, condition));
                }
            }
        }

        // Slot Properties
        Map<String, String> slotProperties = new HashMap<>();
        Object rawProps = map.get("properties");
        if (rawProps instanceof Map<?, ?> pMap) {
            for (Map.Entry<?, ?> entry : pMap.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    slotProperties.put(entry.getKey().toString(), entry.getValue().toString());
                }
            }
        }

        return new SlotDefinition(type, defaultTemplate, states, actions, slotProperties);
    }

    @SuppressWarnings("unchecked")
    private MenuItemTemplate parseItemTemplate(Map<String, Object> map) {
        String material = getString(map, "material", "minecraft:stone");
        int amount = getInt(map, "amount", 1);
        String name = getString(map, "name", getString(map, "display_name", ""));

        List<String> lore = new ArrayList<>();
        Object rawLore = map.get("lore");
        if (rawLore instanceof List<?> lList) {
            for (Object line : lList) {
                if (line != null) {
                    lore.add(line.toString());
                }
            }
        }

        int customModelData = getInt(map, "custom_model_data", getInt(map, "model_data", 0));
        boolean glow = getBoolean(map, "glow", getBoolean(map, "enchanted", false));

        return new MenuItemTemplate(material, amount, name, lore, customModelData, glow);
    }

    /**
     * Loads a menu definition directly from a file path.
     *
     * @param path the file path
     * @return parsed menu definition
     * @throws IOException if an I/O error occurs
     */
    public MenuDefinition load(Path path) throws IOException {
        Objects.requireNonNull(path, "path cannot be null");
        try (InputStream in = Files.newInputStream(path)) {
            return load(in, path.getFileName().toString());
        }
    }

    /**
     * Recursively loads all menu YAML files from a directory into the given registry.
     *
     * @param directory directory to search
     * @param registry  registry to populate
     * @param logger    logger for status messages
     * @return count of loaded menus
     */
    public int loadAll(Path directory, MenuRegistry registry, Logger logger) {
        Objects.requireNonNull(directory, "directory cannot be null");
        Objects.requireNonNull(registry, "registry cannot be null");
        if (!Files.isDirectory(directory)) {
            return 0;
        }

        int loadedCount = 0;
        try (Stream<Path> stream = Files.walk(directory)) {
            List<Path> files = stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".yml") || p.getFileName().toString().endsWith(".yaml"))
                    .toList();

            for (Path file : files) {
                try {
                    MenuDefinition def = load(file);
                    registry.register(def);
                    loadedCount++;
                } catch (Exception ex) {
                    if (logger != null) {
                        logger.log(Level.SEVERE, "Failed to load menu definition from " + file, ex);
                    }
                }
            }
        } catch (IOException ex) {
            if (logger != null) {
                logger.log(Level.SEVERE, "Error reading menu directory " + directory, ex);
            }
        }

        return loadedCount;
    }

    private String getString(Map<String, Object> map, String key, String def) {
        Object val = map.get(key);
        return (val != null) ? val.toString().trim() : def;
    }

    private int getInt(Map<String, Object> map, String key, int def) {
        Object val = map.get(key);
        if (val instanceof Number n) {
            return n.intValue();
        }
        if (val != null) {
            try {
                return Integer.parseInt(val.toString().trim());
            } catch (NumberFormatException ignored) {}
        }
        return def;
    }

    private boolean getBoolean(Map<String, Object> map, String key, boolean def) {
        Object val = map.get(key);
        if (val instanceof Boolean b) {
            return b;
        }
        if (val != null) {
            return Boolean.parseBoolean(val.toString().trim());
        }
        return def;
    }

    private Object rootGetList(Map<String, Object> map, String key) {
        return map.get(key);
    }
}
