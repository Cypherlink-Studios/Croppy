package dev.darkblade.croppy.platform.paper.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

/**
 * Accessor for configurable plugin settings, inspection tool properties, and MiniMessage templates.
 */
public class PluginConfig {

    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private Material toolMaterial;
    private String toolName;
    private List<String> toolLore;
    private String prefix;

    public void load(FileConfiguration config) {
        String matName = config.getString("inspection-tool.material", "SPYGLASS");
        Material mat = Material.matchMaterial(matName);
        this.toolMaterial = (mat != null) ? mat : Material.SPYGLASS;
        this.toolName = config.getString("inspection-tool.name", "<gradient:#70e000:#38b000><bold>Crop Inspector</bold></gradient>");
        this.toolLore = config.getStringList("inspection-tool.lore");
        if (this.toolLore.isEmpty()) {
            this.toolLore = List.of(
                    "<gray>Right-click any crop to view",
                    "<gray>its current growth status."
            );
        }
        this.prefix = config.getString("messages.prefix", "<gradient:#70e000:#38b000><bold>Croppy</bold></gradient> <dark_gray>» <gray>");
    }

    public Material getToolMaterial() {
        return toolMaterial;
    }

    public Component getToolName() {
        return miniMessage.deserialize(toolName);
    }

    public List<Component> getToolLore() {
        return toolLore.stream().map(miniMessage::deserialize).toList();
    }

    public Component getMessage(FileConfiguration config, String key, TagResolver... placeholders) {
        String raw = config.getString("messages." + key, "<red>Missing message: " + key);
        return miniMessage.deserialize(prefix + raw, placeholders);
    }

    public Component getMessageWithoutPrefix(FileConfiguration config, String key, TagResolver... placeholders) {
        String raw = config.getString("messages." + key, "<red>Missing message: " + key);
        return miniMessage.deserialize(raw, placeholders);
    }
}
