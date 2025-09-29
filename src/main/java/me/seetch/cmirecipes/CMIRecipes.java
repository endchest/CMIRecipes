package me.seetch.cmirecipes;

import com.Zrips.CMI.CMI;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.Zrips.CMILib.Recipes.CMIRecipe;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class CMIRecipes extends JavaPlugin {

    private FileConfiguration config;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        getCommand("cmirecipes").setExecutor(this);
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                openRecipesGUI((Player) sender);
                return true;
            } else {
                sender.sendMessage(getConfigString("messages.console_error"));
                return true;
            }
        } else if (args.length == 1) {
            if (sender.hasPermission("cmirecipes.others")) {
                Player target = Bukkit.getPlayerExact(args[0]);
                if (target == null) {
                    String message = getConfigString("messages.player_not_found");
                    message = message.replace("%player%", args[0]);
                    sender.sendMessage(message);
                    return true;
                }
                openRecipesGUI(target);
            } else {
                sender.sendMessage(getConfigString("messages.no_permission_others"));
            }
            return true;
        } else {
            String message = getConfigString("messages.wrong_usage");
            message = message.replace("%usage%", getConfigString("command.usage"));
            sender.sendMessage(message);
            return true;
        }
    }

    private void openRecipesGUI(Player player) {
        String title = getConfigString("gui-pattern.main-menu.title");
        List<String> pattern = getConfigStringList("gui-pattern.main-menu.pattern");
        int rows = config.getInt("gui-pattern.main-menu.rows", 6);
        List<String> lore = getConfigStringList("gui-pattern.main-menu.lore");

        Gui gui = Gui.gui()
                .title(Component.text(title))
                .rows(rows)
                .create();

        Map<String, CMIRecipe> recipes = CMI.getInstance().getRecipeManager().getCustomRecipes();

        if (recipes.isEmpty()) {
            player.sendMessage(getConfigString("messages.no_recipes"));
            return;
        }

        int index = 0;
        for (int row = 0; row < pattern.size(); row++) {
            String line = pattern.get(row);
            for (int col = 0; col < line.length(); col++) {
                char c = line.charAt(col);
                int slot = row * 9 + col;

                if (c == 'R' && index < recipes.size()) {
                    CMIRecipe recipe = (CMIRecipe) recipes.values().toArray()[index];
                    ItemStack result = recipe.getResult();

                    String displayName = getDisplayName(result);

                    ItemBuilder itemBuilder = ItemBuilder.from(result);

                    if (displayName != null) {
                        itemBuilder.setName(displayName);
                    }

                    itemBuilder.setLore(lore);

                    GuiItem guiItem = itemBuilder.asGuiItem(event -> openRecipeDetails(player, recipe));

                    gui.setItem(slot, guiItem);
                    index++;
                }
            }
        }

        gui.open(player);
    }

    private void openRecipeDetails(Player player, CMIRecipe recipe) {
        String title = getConfigString("gui-pattern.detail-menu.title");

        int rows = config.getInt("gui-pattern.detail-menu.rows", 6);
        List<String> recipePattern = getConfigStringList("gui-pattern.detail-menu.recipe-pattern");

        Gui detailGui = Gui.gui()
                .title(Component.text(title))
                .rows(rows)
                .create();

        List<ItemStack> ingredients = getIngredients(recipe);
        int ingIndex = 0;

        for (int row = 0; row < recipePattern.size(); row++) {
            String line = recipePattern.get(row);
            for (int col = 0; col < line.length(); col++) {
                int slot = row * 9 + col;
                char c = line.charAt(col);

                if (c == 'I') {
                    if (ingIndex < ingredients.size()) {
                        ItemStack ingredient = ingredients.get(ingIndex);
                        if (ingredient != null && ingredient.getType() != Material.AIR) {
                            detailGui.setItem(slot, ItemBuilder.from(ingredient).asGuiItem());
                        }
                        ingIndex++;
                    }
                } else if (c == 'R') {
                    ItemStack result = recipe.getResult();
                    String displayName = getDisplayName(result);
                    ItemBuilder itemBuilder = ItemBuilder.from(result);

                    if (displayName != null) {
                        itemBuilder.setName(displayName);
                    }

                    detailGui.setItem(slot, itemBuilder.asGuiItem());
                } else if (c == 'B') {
                    String backMaterial = getConfigString("gui-pattern.detail-menu.back-button.material");
                    String backName = getConfigString("gui-pattern.detail-menu.back-button.name");
                    List<String> backLore = getConfigStringList("gui-pattern.detail-menu.back-button.lore");

                    Material material = Material.getMaterial(backMaterial.toUpperCase());
                    if (material == null) material = Material.ARROW;

                    GuiItem backItem = ItemBuilder.from(material)
                            .setName(backName)
                            .setLore(backLore)
                            .asGuiItem(event -> openRecipesGUI(player));

                    detailGui.setItem(slot, backItem);
                }
            }
        }

        detailGui.open(player);
    }

    private List<ItemStack> getIngredients(CMIRecipe recipe) {
        List<ItemStack> ingredients = new ArrayList<>();
        java.util.HashMap<Integer, ItemStack> ingredientMap = recipe.getIngridients();

        if (ingredientMap != null) {
            for (int i = 1; i <= 9; i++) {
                ItemStack item = ingredientMap.get(i);
                if (item != null) {
                    ingredients.add(item);
                }
            }
        }

        ingredients.removeIf(item -> item == null || item.getType() == Material.AIR);
        return ingredients;
    }

    private String getDisplayName(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return meta.getDisplayName();
        }
        return null;
    }

    private String getConfigString(String path) {
        return config.getString(path);
    }

    private List<String> getConfigStringList(String path) {
        return config.getStringList(path);
    }
}