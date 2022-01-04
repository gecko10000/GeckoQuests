package gecko10000.GeckoQuests.guis;

import eu.endercentral.crazy_advancements.advancement.AdvancementDisplay;
import eu.endercentral.crazy_advancements.advancement.AdvancementVisibility;
import gecko10000.GeckoQuests.GeckoQuests;
import gecko10000.GeckoQuests.misc.QuestManager;
import gecko10000.GeckoQuests.misc.Utils;
import gecko10000.GeckoQuests.objects.Quest;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import redempt.redlib.inventorygui.InventoryGUI;
import redempt.redlib.inventorygui.ItemButton;
import redempt.redlib.inventorygui.PaginationPanel;
import redempt.redlib.itemutils.ItemBuilder;
import redempt.redlib.itemutils.ItemUtils;
import redempt.redlib.misc.ChatPrompt;
import redempt.redlib.misc.FormatUtils;
import redempt.redlib.misc.Task;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class QuestEditor {

    private static final Map<Quest, InventoryGUI> editors = new HashMap<>();
    private static final int SIZE = 36;
    private static final Map<UUID, Stack<InventoryGUI>> previous = new HashMap<>();

    private InventoryGUI gui;
    private PaginationPanel childrenPanel;
    private final Quest quest;

    public QuestEditor(Quest quest) {
        this.quest = quest;
        this.gui = editors.computeIfAbsent(quest, q -> new InventoryGUI(Bukkit.createInventory(null, SIZE, guiName())));
        setup();
    }

    private void setup() {
        this.childrenPanel = new PaginationPanel(gui, InventoryGUI.FILLER);
        gui.setReturnsItems(false);
        gui.setDestroyOnClose(false);
        gui.fill(0, SIZE, InventoryGUI.FILLER);
        childrenPanel.addSlots(SIZE - 18, SIZE - 9);
        staticButtons();
        refresh();
    }

    private void staticButtons() {
        gui.addButton(SIZE - 9, ItemButton.create(new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                .setName(FormatUtils.color("&cBack")), evt -> {
            Player player = (Player) evt.getWhoClicked();
            Stack<InventoryGUI> gui = previous.get(player.getUniqueId());
            if (gui == null || gui.size() <= 1) {
                GeckoQuests.get().getMainEditor().getGui().open(player);
            } else {
                gui.pop();
                gui.peek().open(player);
            }
        }));
        gui.addButton(SIZE - 6, ItemButton.create(new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                .setName(FormatUtils.color("&cPrevious Page")), evt -> childrenPanel.prevPage()));
        gui.addButton(SIZE - 5, ItemButton.create(new ItemBuilder(Material.GREEN_STAINED_GLASS_PANE)
                .setName(FormatUtils.color("&2Add Child Quest")), evt -> {
            new QuestPicker(evt.getWhoClicked(), "&2Adding Child to " + quest.getName(), new HashSet<>(), q -> {
                quest.addChild(q);
                q.updateAdvancement(true);
                updateChildren();
            }, gui);
        }));
        gui.addButton(SIZE - 4, ItemButton.create(new ItemBuilder(Material.LIME_STAINED_GLASS_PANE)
                .setName(FormatUtils.color("&aNext Page")), evt -> childrenPanel.nextPage()));
    }

    private void refresh() {
        quest.updateAdvancement(true);
        GeckoQuests.get().saveQuests();
        GeckoQuests.get().getMainEditor().refreshButtons();
        updateChildren();
        // name
        gui.addButton(0, ItemButton.create(new ItemBuilder(Material.WRITTEN_BOOK)
                .addItemFlags(ItemFlag.values())
                .setName(FormatUtils.color("&bName"))
                .addLore(color("&2Defines advancement title", "&2and quest name.", "&2Currently &b" + quest.getName() + "&2.",
                        "", "&aClick to change")), evt -> {
            edit(evt.getWhoClicked(), "&aEnter a new name for \"" + quest.getName() + "&a\":",
                    quest.getName(), r -> {
                quest.setName(r);
                this.gui = editors.compute(quest, (k, v) -> new InventoryGUI(Bukkit.createInventory(null, SIZE, guiName())));
                setup();
            });
        }));
        // description
        gui.addButton(1, ItemButton.create(new ItemBuilder(Material.WRITABLE_BOOK)
                .setName(FormatUtils.color("&bDescription"))
                .addLore(color("&2The description shown", "&2in the advancement GUI.", "&2Currently:"))
                .addLore(color(FormatUtils.lineWrap(quest.getDescription(), 30).stream()
                        .map(s -> "&b" + s)
                        .toArray(String[]::new)))
                .addLore(color("", "&aClick to change.")), evt -> {
            edit(evt.getWhoClicked(), "&aEnter a new description for \"" + quest.getName() + "&a\":",
                    quest.getDescription(), quest::setDescription);
        }));
        // icon
        gui.addButton(2, ItemButton.create(new ItemBuilder(quest.getIcon())
                .setName(FormatUtils.color("&bIcon"))
                .setLore()
                .addLore(color("&2The icon shown in", "&2the advancement GUI.",
                        "", "&aDrag+drop to change", "&aMiddle click to get")), evt -> {
            if (evt.getClick() == ClickType.MIDDLE) {
                ItemUtils.give((Player) evt.getWhoClicked(), quest.getIcon());
                return;
            }
            ItemStack cursor = evt.getCursor();
            if (!Utils.isEmpty(cursor)) {
                quest.setIcon(cursor);
                refresh();
            }
        }));
        // sync
        gui.addButton(4, ItemButton.create(new ItemBuilder(quest.isSyncItem() ? Material.LIME_STAINED_GLASS : Material.RED_STAINED_GLASS)
                .setName(FormatUtils.color("&bSync Items"))
                .addLore(color("&2Defines whether the", "&2icon and collection",
                        "&2item should be the same.", "&2Currently &b" + quest.isSyncItem())), evt -> {
            quest.setSyncItem(!quest.isSyncItem());
            refresh();
        }));
        // collection item
        gui.addButton(6, ItemButton.create(new ItemBuilder(quest.getCollectionItem())
                .setName(FormatUtils.color("&bCollection Item"))
                .setLore()
                .addLore(color("&2Defines the item the", "&2player needs to collect",
                        "", "&aDrag+drop to change", "&aMiddle click to get")), evt -> {
            if (evt.getClick() == ClickType.MIDDLE) {
                ItemUtils.give((Player) evt.getWhoClicked(), quest.getCollectionItem());
                return;
            }
            ItemStack cursor = evt.getCursor();
            if (!Utils.isEmpty(cursor)) {
                quest.setCollectionItem(cursor);
                refresh();
            }
        }));
        // amount
        gui.addButton(7, ItemButton.create(new ItemBuilder(Material.COMPARATOR)
                .setName(FormatUtils.color("&bAmount"))
                .addLore(color("&2The amount of items", "&2players will need", "&2to collect.",
                        "&2Currently &b" + quest.getAmount(), "", "&aClick to change.")
                ), evt -> edit(evt.getWhoClicked(), "&aEnter the new amount for \"" + quest.getName() + "&a\":",
                quest.getAmount() + "", r -> {
                    try {
                        quest.setAmount(Long.parseLong(r));
                    } catch (NumberFormatException ignored) {}
                })));
        gui.addButton(8, ItemButton.create(new ItemBuilder(quest.isExactItem() ? Material.ENCHANTED_BOOK : Material.BOOK)
                .setName(FormatUtils.color("&bExact Item Match"))
                .addLore(color("&2Whether or not the", "&2collected items must", "&2match exactly.",
                        "&2Currently &b" + quest.isExactItem(), "", "&aClick to toggle.")), evt -> {
            quest.setExactItem(!quest.isExactItem());
            refresh();
        }));
        if (quest.isRoot()) {
            // texture button
            gui.addButton(9, ItemButton.create(new ItemBuilder(Material.PAPER)
                    .setName(FormatUtils.color("&bTexture"))
                    .addLore(color("&2The texture of the", "&2advancement tab background.",
                            "&2Only for root advancements.", "&2Currently &b" + quest.getTexture(),
                            "", "&aClick to change.")), evt -> edit(evt.getWhoClicked(),
                    "&aEnter a new texture string for \"" + quest.getName() + "&a\":",
                    quest.getTexture(),
                    quest::setTexture)));
        } else {
            // parent button
            Quest parent = quest.getParent();
            gui.addButton(9, ItemButton.create(new ItemBuilder(parent.editorItem())
                    .addLore(color("", "&aClick to go to quest.")), evt -> new QuestEditor(parent).open(evt.getWhoClicked(), gui)));
        }
        // frame
        gui.addButton(10, ItemButton.create(new ItemBuilder(frameMaterials.get(quest.getFrame()))
                .setName(FormatUtils.color("&bFrame"))
                .addLore(color("&2The frame of the", "&2advancement in the GUI.",
                        "&2Currently &b" + FormatUtils.toTitleCase(quest.getFrame().toString().replace('_', ' ')) + "&2.",
                        "", "&aClick to change.")), evt -> {
            quest.setFrame(nextFrame(quest.getFrame()));
            refresh();
        }));
        // visibility
        gui.addButton(11, ItemButton.create(new ItemBuilder(visibilityMaterials.get(quest.getVisibility()))
                .setName(FormatUtils.color("&bVisibility"))
                .addLore(color("&2The visibility of", "&2the advancement in the GUI.",
                        "&2Currently &b" + FormatUtils.toTitleCase(quest.getVisibility().getName().replace('_', ' ')) + "&2.",
                        "", "&aClick to change.")), evt -> {
            quest.setVisibility(nextVisibility(quest.getVisibility()));
            refresh();
        }));
        // x
        gui.addButton(16, ItemButton.create(new ItemBuilder(Material.NETHER_SPROUTS)
                .setName(FormatUtils.color("&bX Offset"))
                .addLore(color("&2The x position of the", "&2advancement relative to the",
                        "&2previous one. Currently &b" + quest.getX(), "", "&aClick to change.")), evt -> {
            edit(evt.getWhoClicked(), "&aEnter a new x offset for \"" + quest.getName() + "&a\":",
                    quest.getX() + "", r -> {
                        try {
                            quest.setX(Float.parseFloat(r));
                        } catch (NumberFormatException ignored) {}
                    });
        }));
        // y
        gui.addButton(17, ItemButton.create(new ItemBuilder(Material.CHAIN)
                .setName(FormatUtils.color("&bY Offset"))
                .addLore(color("&2The y position of the", "&2advancement relative to the",
                        "&2previous one. Currently &b" + quest.getY(), "", "&aClick to change.")), evt -> {
            edit(evt.getWhoClicked(), "&aEnter a new y offset for \"" + quest.getName() + "&a\":",
                    quest.getY() + "", r -> {
                        try {
                            quest.setY(Float.parseFloat(r));
                        } catch (NumberFormatException ignored) {}
                    });
        }));
    }

    private void updateChildren() {
        childrenPanel.clear();
        quest.getChildren().stream()
                .map(q -> ItemButton.create(q.editorItem().addLore(
                        color("", "&aClick to go to quest.", "&aShift click to remove.")), evt -> {
                    switch (evt.getClick()) {
                        case SHIFT_RIGHT,SHIFT_LEFT -> {
                            quest.removeChild(q);
                            q.updateAdvancement(true);
                            QuestManager.updateAdvancements();
                            updateChildren();
                        }
                        default -> new QuestEditor(q).open(evt.getWhoClicked(), gui);
                    }
                })).forEach(childrenPanel::addPagedButton);
    }

    private final Map<AdvancementDisplay.AdvancementFrame, Material> frameMaterials = new EnumMap<>(Map.of(
            AdvancementDisplay.AdvancementFrame.TASK, Material.ITEM_FRAME,
            AdvancementDisplay.AdvancementFrame.GOAL, Material.GLOW_ITEM_FRAME,
            AdvancementDisplay.AdvancementFrame.CHALLENGE, Material.END_PORTAL_FRAME
    ));

    private AdvancementDisplay.AdvancementFrame nextFrame(AdvancementDisplay.AdvancementFrame frame) {
        return AdvancementDisplay.AdvancementFrame.values()[(frame.ordinal() + 1) % 3];
    }

    private final Map<AdvancementVisibility, Material> visibilityMaterials = Map.of(
            AdvancementVisibility.HIDDEN, Material.TINTED_GLASS,
            AdvancementVisibility.ALWAYS, Material.SMOOTH_QUARTZ,
            AdvancementVisibility.PARENT_GRANTED, Material.WHITE_STAINED_GLASS,
            AdvancementVisibility.VANILLA, Material.GLASS
    );

    private final List<AdvancementVisibility> visibilities = new ArrayList<>(Set.of(
            AdvancementVisibility.HIDDEN,
            AdvancementVisibility.ALWAYS,
            AdvancementVisibility.PARENT_GRANTED,
            AdvancementVisibility.VANILLA
    ));
    private AdvancementVisibility nextVisibility(AdvancementVisibility vis) {
        return visibilities.get((visibilities.indexOf(vis) + 1) % 4);
    }

    private String guiName() {
        return FormatUtils.color("&2Editing &3&n" + quest.getName());
    }

    public void open(HumanEntity ent) {
        open(ent, null);
    }

    public void open(HumanEntity ent, InventoryGUI previous) {
        gui.open((Player) ent);
        UUID uuid = ent.getUniqueId();
        if (previous == null) {
            QuestEditor.previous.put(uuid, new Stack<>());
        }
        if (!gui.equals(previous)) {
            QuestEditor.previous.computeIfAbsent(uuid, id -> new Stack<>()).push(gui);
        }
    }

    public void edit(HumanEntity ent, String prompt, String previousValue, Consumer<String> consumer) {
        Player player = (Player) ent;
        player.closeInventory();
        TextComponent editMessage = new TextComponent(FormatUtils.color(prompt));
        if (previousValue != null) {
            editMessage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(FormatUtils.color("&aClick to paste previous value into chat"))));
            editMessage.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, previousValue));
        }
        player.spigot().sendMessage(editMessage);
        ChatPrompt.prompt(player, null, consumer.andThen(s -> {
            quest.updateAdvancement(true);
            refresh();
            reopen(player);
        }), r -> {
            if (r == ChatPrompt.CancelReason.PLAYER_CANCELLED) {
                reopen(player);
            }
        });
    }

    private void reopen(Player player) {
        Task.syncDelayed(() -> open(player, gui));
    }

    private List<String> color(String... list) {
        return Stream.of(list)
                .map(FormatUtils::color)
                .collect(Collectors.toList());
    }

}
