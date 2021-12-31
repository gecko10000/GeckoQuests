package gecko10000.GeckoQuests.guis;

import gecko10000.GeckoQuests.GeckoQuests;
import gecko10000.GeckoQuests.misc.QuestManager;
import gecko10000.GeckoQuests.objects.Quest;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import redempt.redlib.inventorygui.InventoryGUI;
import redempt.redlib.inventorygui.ItemButton;
import redempt.redlib.inventorygui.PaginationPanel;
import redempt.redlib.itemutils.ItemBuilder;
import redempt.redlib.misc.ChatPrompt;
import redempt.redlib.misc.FormatUtils;
import redempt.redlib.misc.Task;

import java.util.Collections;
import java.util.stream.Collectors;

public class MainEditor {

    private final InventoryGUI gui;
    private final PaginationPanel panel;
    private static final int SIZE = 54;

    public MainEditor() {
        this.gui = new InventoryGUI(Bukkit.createInventory(null, SIZE, FormatUtils.color("&2Quest Editor")));
        this.panel = new PaginationPanel(gui);
        setup();
    }

    private void setup() {
        gui.setDestroyOnClose(false);
        gui.setReturnsItems(false);
        gui.fill(0, SIZE, InventoryGUI.FILLER);
        panel.addSlots(0, SIZE - 9);
        refreshButtons();
        gui.addButton(SIZE - 5, ItemButton.create(
                new ItemBuilder(Material.GREEN_STAINED_GLASS_PANE)
                        .setName(FormatUtils.color("&2Create Quest")), evt -> createQuest((Player) evt.getWhoClicked())
        ));
    }

    public void refreshButtons() {
        panel.clear();
        panel.addPagedButtons(QuestManager.quests().values().stream().map(this::button).collect(Collectors.collectingAndThen(
                Collectors.toList(), l -> {Collections.reverse(l); return l;}
        )));
    }

    private ItemButton button(Quest quest) {
        return ItemButton.create(new ItemBuilder(quest.getCollectionItem())
                .setName(FormatUtils.color("&b" + quest.getName()))
                .setLore(FormatUtils.color(
                        "&3Requires " + quest.getAmount() + "x " + quest.getCollectionItem().getType())), evt -> {
            switch (evt.getClick()) {
                case SHIFT_RIGHT,SHIFT_LEFT -> {
                    QuestManager.removeQuest(quest);
                    refreshButtons();
                }
                default -> new QuestEditor(quest).open(evt.getWhoClicked());
            }
        });
    }

    public InventoryGUI getGui() {
        return gui;
    }

    public void createQuest(Player player) {
        player.closeInventory();
        ChatPrompt.prompt(player, FormatUtils.color("&aEnter a name for the new quest:"), r -> Task.syncDelayed(() -> {
            Quest quest = new Quest(r);
            QuestManager.addQuest(quest);
            refreshButtons();
            GeckoQuests.get().saveQuests();
            new QuestEditor(quest).open(player);
        }), reason -> {
            if (reason == ChatPrompt.CancelReason.PLAYER_CANCELLED) {
                Task.syncDelayed(() -> gui.open(player));
            }
        });
    }

}
