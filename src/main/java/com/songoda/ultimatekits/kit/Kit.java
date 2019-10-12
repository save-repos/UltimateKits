package com.songoda.ultimatekits.kit;

import com.songoda.core.compatibility.CompatibleMaterial;
import com.songoda.core.compatibility.CompatibleSound;
import com.songoda.core.configuration.Config;
import com.songoda.core.gui.Gui;
import com.songoda.core.gui.GuiManager;
import com.songoda.core.hooks.EconomyManager;
import com.songoda.core.utils.TextUtils;
import com.songoda.ultimatekits.UltimateKits;
import com.songoda.ultimatekits.gui.AnimatedKitGui;
import com.songoda.ultimatekits.gui.ConfirmBuyGui;
import com.songoda.ultimatekits.gui.PreviewKitGui;
import com.songoda.ultimatekits.key.Key;
import com.songoda.ultimatekits.kit.type.KitContentCommand;
import com.songoda.ultimatekits.kit.type.KitContentEconomy;
import com.songoda.ultimatekits.kit.type.KitContentItem;
import com.songoda.ultimatekits.settings.Settings;
import com.songoda.ultimatekits.utils.ArmorType;
import com.songoda.ultimatekits.utils.Methods;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Created by songoda on 2/24/2017.
 */
public class Kit {

    private final String name, showableName;
    private static UltimateKits plugin;
    private double price = 0;
    private String link, title = null;
    private long delay = 0L;
    private boolean hidden = false;
    private CompatibleMaterial displayItem = null;
    private List<KitItem> contents = new ArrayList<>();
    private KitAnimation kitAnimation = KitAnimation.NONE;

    public Kit(String name) {
        if (plugin == null)
            plugin = UltimateKits.getInstance();
        this.name = name;
        this.showableName = TextUtils.formatText(name, true);
    }

    public void buy(Player player, GuiManager manager) {
        if (hasPermission(player) && plugin.getConfig().getBoolean("Main.Allow Players To Receive Kits For Free If They Have Permission")) {
            processGenericUse(player, false);
            return;
        }

        if (!player.hasPermission("ultimatekits.buy." + name)) {
            UltimateKits.getInstance().getLocale().getMessage("command.general.noperms")
                    .sendPrefixedMessage(player);
            return;
        }

        if (link != null) {
            player.sendMessage("");
            plugin.getLocale().newMessage("&a" + link).sendPrefixedMessage(player);
            player.sendMessage("");
            player.closeInventory();
        } else if (price != 0) {
            manager.showGUI(player, new ConfirmBuyGui(plugin, player, this, null));
        } else {
            UltimateKits.getInstance().getLocale().getMessage("command.general.noperms")
                    .sendPrefixedMessage(player);
        }
    }

    private List<ItemStack> getItemContents() {
        List<ItemStack> items = new ArrayList<>();
        for (KitItem item : this.getContents()) {
            if (item.getContent() instanceof KitContentItem)
                items.add(((KitContentItem) item.getContent()).getItemStack());
        }
        return items;
    }

    private boolean hasRoom(Player player) {
        int space = 0;

        for (ItemStack content : player.getInventory().getContents()) {
            if (content == null) {
                space++;
            }
        }

        int spaceNeeded = getItemContents().size();

        return space >= spaceNeeded;
    }

    public void processKeyUse(Player player) {
        ItemStack item = player.getItemInHand();
        if (item.getType() != Material.TRIPWIRE_HOOK || !item.hasItemMeta()) {
            return;
        }
        Key key = plugin.getKeyManager().getKey(ChatColor.stripColor(item.getItemMeta().getLore().get(0)).replace(" Key", ""));

        if (!item.getItemMeta().getDisplayName().equals(plugin.getLocale().getMessage("interface.key.title")
                .processPlaceholder("kit", showableName).getMessage())
                && !item.getItemMeta().getDisplayName().equals(plugin.getLocale().getMessage("interface.key.title")
                .processPlaceholder("kit", "Any").getMessage())) {
            plugin.getLocale().getMessage("event.crate.wrongkey").sendPrefixedMessage(player);
            return;
        }
        if (giveKit(player, key)) {
            plugin.getLocale().getMessage("event.key.success")
                    .processPlaceholder("kit", showableName).sendPrefixedMessage(player);
            if (player.getInventory().getItemInHand().getAmount() != 1) {
                ItemStack is = item;
                is.setAmount(is.getAmount() - 1);
                player.setItemInHand(is);
            } else {
                player.setItemInHand(null);
            }
        }
    }

    public void processPurchaseUse(Player player) {
        if (!EconomyManager.isEnabled()) return;

        if (!player.hasPermission("ultimatekits.buy." + name)) {
            UltimateKits.getInstance().getLocale().getMessage("command.general.noperms")
                    .sendPrefixedMessage(player);
            return;
        } else if (!EconomyManager.hasBalance(player, price)) {
            plugin.getLocale().getMessage("event.claim.cannotafford")
                    .processPlaceholder("kit", showableName).sendPrefixedMessage(player);
            return;
        }
        if (this.delay > 0) {
            if (getNextUse(player) == -1) {
                plugin.getLocale().getMessage("event.claim.nottwice").sendPrefixedMessage(player);
            } else if (getNextUse(player) != 0) {
                plugin.getLocale().getMessage("event.claim.delay")
                        .processPlaceholder("time", Methods.makeReadable(this.getNextUse(player)))
                        .sendPrefixedMessage(player);
                return;
            }
        }
        if (giveKit(player)) {
            EconomyManager.withdrawBalance(player, price);
            if (delay != 0)
                updateDelay(player); //updates delay on buy

            plugin.getLocale().getMessage("event.claim.purchasesuccess")
                    .processPlaceholder("kit", showableName).sendPrefixedMessage(player);
        }

    }

    public void processGenericUse(Player player, boolean forced) {
        if (getNextUse(player) == -1 && !forced) {
            plugin.getLocale().getMessage("event.claim.nottwice").sendPrefixedMessage(player);
        } else if (getNextUse(player) <= 0 || forced) {
            if (giveKit(player)) {
                updateDelay(player);
                if (kitAnimation == KitAnimation.NONE)
                    plugin.getLocale().getMessage("event.claim.givesuccess")
                            .processPlaceholder("kit", showableName).sendPrefixedMessage(player);
            }
        } else {
            plugin.getLocale().getMessage("event.claim.delay")
                    .processPlaceholder("time", Methods.makeReadable(getNextUse(player)))
                    .sendPrefixedMessage(player);
        }
    }

    @SuppressWarnings("Duplicates")
    public void display(Player player, GuiManager manager, Gui back) {
        if (!player.hasPermission("previewkit.use")
                && !player.hasPermission("previewkit." + name)
                && !player.hasPermission("ultimatekits.use")
                && !player.hasPermission("ultimatekits." + name)) {
            UltimateKits.getInstance().getLocale().getMessage("command.general.noperms")
                    .sendPrefixedMessage(player);
            return;
        }
        if (name == null) {
            plugin.getLocale().getMessage("command.kit.kitdoesntexist").sendPrefixedMessage(player);
            return;
        }

        plugin.getLocale().getMessage("event.preview.kit")
                .processPlaceholder("kit", showableName).sendPrefixedMessage(player);
        manager.showGUI(player, new PreviewKitGui(plugin, player, this, back));
    }

    public void saveKit(List<ItemStack> items) {
        List<KitItem> list = new ArrayList<>();
        for (ItemStack is : items) {
            if (is != null && is.getType() != Material.AIR) {

                if (is.getItemMeta().hasLore()) {
                    ItemMeta meta = is.getItemMeta();
                    List<String> newLore = new ArrayList<>();
                    for (String line : meta.getLore()) {
                        if (TextUtils.convertFromInvisibleString(line).equals("----")) break;
                        newLore.add(line);
                    }
                    meta.setLore(newLore);
                    is.setItemMeta(meta);
                }

                if (is.getType() == Material.PAPER && ChatColor.stripColor(is.getItemMeta().getDisplayName()).endsWith("Command")) {
                    StringBuilder command = new StringBuilder();
                    for (String line : is.getItemMeta().getLore()) {
                        command.append(line);
                    }
                    list.add(new KitItem(is, ChatColor.stripColor(command.toString())));
                } else if (is.getType() == Material.PAPER && ChatColor.stripColor(is.getItemMeta().getDisplayName()).endsWith("Money")) {
                    String money = is.getItemMeta().getLore().get(0);
                    list.add(new KitItem(is, ChatColor.stripColor(money)));
                } else {
                    list.add(new KitItem(is));
                }
            }
        }
        contents = list;
        plugin.saveKits();
    }


    public List<ItemStack> getReadableContents(Player player, boolean preview, boolean commands, boolean moveable) {
        List<ItemStack> stacks = new ArrayList<>();
        for (KitItem item : getContents()) {
            if ((!item.getSerialized().startsWith("/") && !item.getSerialized().startsWith(Settings.CURRENCY_SYMBOL.getString())) || commands) { //ToDO: I doubt this is correct.
                ItemStack stack = moveable ? item.getMoveableItem() : item.getItem();
                if (preview) stack = item.getItemForDisplay();
                if (stack == null) continue;

                ItemStack fin = stack;
                if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI") && stack.getItemMeta().getLore() != null) {
                    ArrayList<String> lore2 = new ArrayList<>();
                    ItemMeta meta2 = stack.getItemMeta();
                    for (String lor : stack.getItemMeta().getLore()) {
                        lor = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, lor.replace(" ", "_")).replace("_", " ");
                        lore2.add(lor);
                    }
                    meta2.setLore(lore2);
                    fin.setItemMeta(meta2);
                }
                stacks.add(fin);
            }
        }
        return stacks;
    }

    private boolean giveKit(Player player) {
        return giveKit(player, null);
    }

    private boolean giveKit(Player player, Key key) {
        if (Settings.NO_REDEEM_WHEN_FULL.getBoolean() && !hasRoom(player)) {
            plugin.getLocale().getMessage("event.claim.full").sendPrefixedMessage(player);
            return false;
        }
        if (Settings.SOUNDS_ENABLED.getBoolean() && kitAnimation == KitAnimation.NONE)
            CompatibleSound.ENTITY_PLAYER_LEVELUP.play(player, 0.6F, 15.0F);

        List<KitItem> innerContents = new ArrayList<>(getContents());
        int amt = innerContents.size();
        int amtToGive = key == null ? amt : key.getAmt();

        if (amt != amtToGive || kitAnimation != KitAnimation.NONE)
            Collections.shuffle(innerContents);

        return generateRandomItem(innerContents, amtToGive, player, -1);
    }

    private boolean generateRandomItem(List<KitItem> innerContents, int amtToGive, Player player, int forceSelect) {
        int canChoose = 0;
        for (KitItem item : innerContents) {
            if (amtToGive == 0) break;
            int ch = canChoose++ == forceSelect || item.getChance() == 0 ? 100 : item.getChance();
            double rand = Math.random() * 100;
            if (rand < ch || ch == 100) {

                if (item.getContent() instanceof KitContentEconomy) {
                    try {
                        EconomyManager.deposit(player, ((KitContentEconomy) item.getContent()).getAmount());
                        plugin.getLocale().getMessage("event.claim.eco")
                                .processPlaceholder("amt", Methods.formatEconomy(((KitContentEconomy) item.getContent()).getAmount()))
                                .sendPrefixedMessage(player);
                    } catch (NumberFormatException ex) {
                        ex.printStackTrace();
                    }
                    amtToGive--;
                    continue;
                } else if (item.getContent() instanceof KitContentCommand) {
                    String parsed = ((KitContentCommand) item.getContent()).getCommand();
                    parsed = parsed.replace("{player}", player.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
                    amtToGive--;
                    continue;
                }

                ItemStack parseStack = ((KitContentItem) item.getContent()).getItemStack();

                if (parseStack.hasItemMeta() && parseStack.getItemMeta().hasLore()) {
                    ItemMeta meta = parseStack.getItemMeta();
                    List<String> newLore = new ArrayList<>();
                    for (String str : parseStack.getItemMeta().getLore()) {
                        str = str.replace("{PLAYER}", player.getName()).replace("<PLAYER>", player.getName());
                        newLore.add(str);
                    }
                    meta.setLore(newLore);
                    parseStack.setItemMeta(meta);
                }

                if (parseStack.getType() == Material.AIR) continue;

                amtToGive--;

                if (kitAnimation != KitAnimation.NONE) {
                    // TODO: this is a very bad way to solve this problem.
                    // Giving the player kit rewards really should be done outside of the Kit class.
                    plugin.getGuiManager().showGUI(player, new AnimatedKitGui(plugin, player, this, item.getItem()));
                    return true;
                } else {
                    if (Settings.AUTO_EQUIP_ARMOR.getBoolean() && ArmorType.equip(player, item.getItem())) continue;

                    Map<Integer, ItemStack> overfilled = player.getInventory().addItem(item.getItem());
                    for (ItemStack item2 : overfilled.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), item2);
                    }
                }
            }
        }

        if (amtToGive != 0 && canChoose != 0 && forceSelect == -1) {
            return generateRandomItem(innerContents, amtToGive, player, (int) (Math.random() * canChoose));
        } else if (amtToGive != 0) {
            return false;
        }

        player.updateInventory();
        return true;
    }

    public void updateDelay(Player player) {
        plugin.getDataFile().set("Kits." + name + ".delays." + player.getUniqueId().toString(), System.currentTimeMillis());
    }

    public Long getNextUse(Player player) {
        String configSectionPlayer = "Kits." + name + ".delays." + player.getUniqueId().toString();
        Config config = plugin.getDataFile();

        if (!config.contains(configSectionPlayer)) {
            return 0L;
        } else if (this.delay == -1) return -1L;

        long last = config.getLong(configSectionPlayer);
        long delay = (long) this.delay * 1000;

        return (last + delay) >= System.currentTimeMillis() ? (last + delay) - System.currentTimeMillis() : 0L;
    }

    public boolean hasPermission(Player player) {
        if (player.hasPermission("uc.kit." + name.toLowerCase())) return true;
        if (player.hasPermission("essentials.kit." + name.toLowerCase())) return true;
        if (player.hasPermission("ultimatekits.kit." + name.toLowerCase())) return true;
        return false;
    }

    public double getPrice() {
        return price;
    }

    public Kit setPrice(double price) {
        this.price = price;
        return this;
    }

    public String getLink() {
        return link;
    }

    public Kit setLink(String link) {
        this.link = link;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public Kit setTitle(String title) {
        this.title = title;
        return this;
    }

    public long getDelay() {
        return delay;
    }

    public Kit setDelay(long delay) {
        this.delay = delay;
        return this;
    }

    public List<KitItem> getContents() {
        return this.contents;
    }

    public Kit setContents(List<KitItem> contents) {
        this.contents = contents;
        return this;
    }

    public String getName() {
        return name;
    }

    public String getShowableName() {
        return showableName;
    }

    public CompatibleMaterial getDisplayItem() {
        return displayItem;
    }

    public void setDisplayItem(ItemStack item) {
        this.displayItem = item != null ? CompatibleMaterial.getMaterial(item) : null;
    }

    public Kit setDisplayItem(CompatibleMaterial material) {
        this.displayItem = material;
        return this;
    }

    public boolean isHidden() {
        return hidden;
    }

    public Kit setHidden(boolean hidden) {
        this.hidden = hidden;
        return this;
    }

    public KitAnimation getKitAnimation() {
        return kitAnimation;
    }

    public Kit setKitAnimation(KitAnimation kitAnimation) {
        this.kitAnimation = kitAnimation;
        return this;
    }

    @Override
    public int hashCode() {
        return 31 * (name != null ? name.hashCode() : 0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Kit)) return false;

        Kit kit = (Kit) o;
        return Objects.equals(name, kit.name);
    }

}
