# Unicode Negative Font Advances Reference

This reference provides standard binary-weighted Unicode characters used for horizontal cursor shifting in Minecraft 1.21.8 GUI layouts.

---

## 1. Character Map (Private Use Area)

| Unicode Code Point | Character Representation | Advance (Pixels) | Direction |
|---|---|---|---|
| `\uF801` | Private Use | -1 | Left Shift |
| `\uF802` | Private Use | -2 | Left Shift |
| `\uF804` | Private Use | -4 | Left Shift |
| `\uF808` | Private Use | -8 | Left Shift |
| `\uF810` | Private Use | -16 | Left Shift |
| `\uF820` | Private Use | -32 | Left Shift |
| `\uF840` | Private Use | -64 | Left Shift |
| `\uF880` | Private Use | -128 | Left Shift |

---

## 2. Common Shift Combinations for Inventory Titles

Standard Chest GUIs have an 8-pixel horizontal margin before the inventory title starts.

| Desired Offset | Binary Decomposition | Unicode String Sequence |
|---|---|---|
| `-8 px` (Neutralize left margin) | -8 | `\uF808` |
| `-16 px` | -16 | `\uF810` |
| `-24 px` | -16 + -8 | `\uF810\uF808` |
| `-72 px` | -64 + -8 | `\uF840\uF808` |
| `-128 px` | -128 | `\uF880` |
| `-176 px` (Full chest width compensation) | -128 + -32 + -16 | `\uF880\uF820\uF810` |

---

## 3. Title Formatter Pattern (MiniMessage / Paper)

```java
// Example in Paper 1.21.8 using MiniMessage:
// Shifts cursor -8 pixels and renders custom texture \uE001 with custom title
String title = "<white>\uF808\uE001</white><shift:-166><black>Culinary Compendium</black>";
Component inventoryTitle = MiniMessage.miniMessage().deserialize(title);
```
