package gecko10000.GeckoQuests.guis;

import eu.endercentral.crazy_advancements.advancement.AdvancementDisplay;
import eu.endercentral.crazy_advancements.advancement.AdvancementVisibility;
import gecko10000.GeckoQuests.GeckoQuests;
import gecko10000.GeckoQuests.misc.Utils;
import gecko10000.GeckoQuests.objects.Quest;
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
    private static final int SIZE = 54;

    private InventoryGUI gui;
    private PaginationPanel childrenPanel;
    private final Quest quest;
    private final Map<UUID, InventoryGUI> previous = new HashMap<>();

    public QuestEditor(Quest quest) {
        this.quest = quest;
        this.gui = editors.computeIfAbsent(quest, q -> new InventoryGUI(Bukkit.createInventory(null, SIZE, guiName())));
        setup();
    }

    private void setup() {
        this.childrenPanel = new PaginationPanel(gui);
        gui.setReturnsItems(false);
        gui.setDestroyOnClose(false);
        childrenPanel.addSlots(SIZE - 18, SIZE - 9);
        gui.fill(0, SIZE, InventoryGUI.FILLER);
        staticButtons();
        refresh();
    }

    private void staticButtons() {
        gui.addButton(SIZE - 9, ItemButton.create(new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                .setName(FormatUtils.color("&cBack")), evt -> {
            Player player = (Player) evt.getWhoClicked();
            previous.getOrDefault(player.getUniqueId(), GeckoQuests.get().getMainEditor().getGui()).open(player);
        }));
        gui.addButton(SIZE - 6, ItemButton.create(new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                .setName(FormatUtils.color("&cPrevious Page")), evt -> childrenPanel.prevPage()));
        gui.addButton(SIZE - 5, ItemButton.create(new ItemBuilder(Material.GREEN_STAINED_GLASS_PANE)
                .setName(FormatUtils.color("&2Add Child Quest")), evt -> {
            new QuestPicker(evt.getWhoClicked(), "&2Adding Child to " + quest.getName(), new HashSet<>(), q -> {
                quest.addChild(q);
                updateChildren();
            }, gui);
        }));
        gui.addButton(SIZE - 4, ItemButton.create(new ItemBuilder(Material.LIME_STAINED_GLASS_PANE)
                .setName(FormatUtils.color("&aNext Page")), evt -> childrenPanel.nextPage()));
    }

    //private Set<Quest>

    private void refresh() {
        quest.updateAdvancement();
        GeckoQuests.get().saveQuests();
        GeckoQuests.get().getMainEditor().refreshButtons();
        updateChildren();
        gui.addButton(0, ItemButton.create(new ItemBuilder(Material.WRITTEN_BOOK)
                .addItemFlags(ItemFlag.values())
                .setName(FormatUtils.color("&bName"))
                .addLore(color("&2Defines internal name.", "&2Currently &b" + quest.getName() + "&2.",
                        "", "&aClick to change")), evt -> {
            edit(evt.getWhoClicked(), "&aEnter a new name for \"" + quest.getName() + "\":", r -> {
                quest.setName(r);
                this.gui = editors.compute(quest, (k, v) -> new InventoryGUI(Bukkit.createInventory(null, SIZE, guiName())));
                setup();
            });
        }));
        gui.addButton(1, ItemButton.create(new ItemBuilder(quest.getIcon())
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
        gui.addButton(2, ItemButton.create(new ItemBuilder(Material.NAME_TAG)
                .setName(FormatUtils.color("&bTitle"))
                .addLore(color("&2The title shown in", "&2the advancement GUI.",
                        "&2Currently &b" + quest.getTitle() + "&2.", "", "&aClick to change.")), evt -> {
            edit(evt.getWhoClicked(), "&aEnter a new title for \"" + quest.getName() + "\":", quest::setTitle);
        }));
        gui.addButton(3, ItemButton.create(new ItemBuilder(Material.WRITABLE_BOOK)
                .setName(FormatUtils.color("&bDescription"))
                .addLore(color("&2The description shown", "&2in the advancement GUI.", "&2Currently:"))
                .addLore(color(FormatUtils.lineWrap(quest.getDescription(), 30)
                        .stream().map(s -> "&b" + s).toArray(String[]::new)))
                .addLore(color("", "&aClick to change.")), evt -> {
            edit(evt.getWhoClicked(), "&aEnter a new description for \"" + quest.getName() + "\":", quest::setDescription);
                }));
        gui.addButton(4, ItemButton.create(new ItemBuilder(frameMaterials.get(quest.getFrame()))
                .setName(FormatUtils.color("&bFrame"))
                .addLore(color("&2The frame of the", "&2advancement in the GUI.",
                        "&2Currently &b" + FormatUtils.toTitleCase(quest.getFrame().toString().replace('_', ' ')) + "&2.",
                        "", "&aClick to change.")), evt -> {
            quest.setFrame(nextFrame(quest.getFrame()));
            refresh();
        }));
        gui.addButton(5, ItemButton.create(new ItemBuilder(visibilityMaterials.get(quest.getVisibility()))
                .setName(FormatUtils.color("&bVisibility"))
                .addLore(color("&2The visibility of", "&2the advancement in the GUI.",
                        "&2Currently &b" + FormatUtils.toTitleCase(quest.getVisibility().getName().replace('_', ' ')) + "&2.",
                        "", "&aClick to change.")), evt -> {
            quest.setVisibility(nextVisibility(quest.getVisibility()));
            refresh();
        }));
        gui.addButton(6, ItemButton.create(new ItemBuilder(Material.NETHER_SPROUTS)
                .setName(FormatUtils.color("&bX Offset"))
                .addLore(color("&2The x position of the", "&2advancement relative to the",
                        "&2previous one. Currently &b" + quest.getX(), "", "&aClick to change.")), evt -> {
            edit(evt.getWhoClicked(), "&aEnter a new x offset for \"" + quest.getName() + "\":", r -> {
                try {
                    quest.setX(Float.parseFloat(r));
                    refresh();
                } catch (NumberFormatException ignored) {}
            });
        }));
        gui.addButton(7, ItemButton.create(new ItemBuilder(Material.CHAIN)
                .setName(FormatUtils.color("&bY Offset"))
                .addLore(color("&2The y position of the", "&2advancement relative to the",
                        "&2previous one. Currently &b" + quest.getY(), "", "&aClick to change.")), evt -> {
            edit(evt.getWhoClicked(), "&aEnter a new y offset for \"" + quest.getName() + "\":", r -> {
                try {
                    quest.setY(Float.parseFloat(r));
                    refresh();
                } catch (NumberFormatException ignored) {}
            });
        }));
        if (quest.isRoot()) {
            // texture button
            gui.addButton(8, ItemButton.create(new ItemBuilder(Material.PAPER)
                    .setName(FormatUtils.color("&bTexture"))
                    .addLore(color("&2The texture of the", "&2advancement tab background.",
                            "&2Only for root advancements.", "&2Currently &b" + quest.getTexture(),
                            "", "&aClick to change.")), evt -> edit(evt.getWhoClicked(),
                    "&aEnter a new texture string for \"" + quest.getName() + "\":",
                    quest::setTexture)));
        } else {
            // parent button
            Quest parent = quest.getParent();
            gui.addButton(8, ItemButton.create(new ItemBuilder(parent.editorItem())
                    .addLore(color("", "&aClick to go to quest.")), evt -> new QuestEditor(parent).open(evt.getWhoClicked(), gui)));
        }
    }

    private void updateChildren() {
        childrenPanel.clear();
        quest.getChildren().stream()
                .map(q -> ItemButton.create(q.editorItem().addLore(
                        color("", "&aClick to go to quest.", "&aShift click to remove.")), evt -> {
                    switch (evt.getClick()) {
                        case SHIFT_RIGHT,SHIFT_LEFT -> quest.removeChild(q);
                        default -> new QuestEditor(q).open(evt.getWhoClicked(), gui);
                    }
                    updateChildren();
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
        return FormatUtils.color("&1Editing &9&n" + quest.getName());
    }

    public void open(HumanEntity ent) {
        open(ent, null);
    }

    public void open(HumanEntity ent, InventoryGUI previous) {
        gui.open((Player) ent);
        if (previous == null) {
            this.previous.remove(ent.getUniqueId());
        } else if (!previous.equals(gui)) {
            this.previous.put(ent.getUniqueId(), previous);
        }
    }

    public void edit(HumanEntity ent, String prompt, Consumer<String> consumer) {
        Player player = (Player) ent;
        player.closeInventory();
        ChatPrompt.prompt(player, FormatUtils.color(prompt), consumer.andThen(s -> {
            quest.updateAdvancement();
            refresh();
            reopen(player);
        }), r -> {
            if (r == ChatPrompt.CancelReason.PLAYER_CANCELLED) {
                reopen(player);
            }
        });
    }

    private void reopen(Player player) {
        Task.syncDelayed(() -> open(player, previous.get(player.getUniqueId())));
    }

    private List<String> color(String... list) {
        return Stream.of(list)
                .map(FormatUtils::color)
                .collect(Collectors.toList());
    }

}
