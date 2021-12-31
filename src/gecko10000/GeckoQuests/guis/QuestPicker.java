package gecko10000.GeckoQuests.guis;

import gecko10000.GeckoQuests.misc.QuestManager;
import gecko10000.GeckoQuests.objects.Quest;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import redempt.redlib.inventorygui.InventoryGUI;
import redempt.redlib.inventorygui.ItemButton;
import redempt.redlib.inventorygui.PaginationPanel;
import redempt.redlib.itemutils.ItemBuilder;
import redempt.redlib.itemutils.ItemUtils;
import redempt.redlib.misc.FormatUtils;

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class QuestPicker {

    private final Player player;
    private final int size;
    private final InventoryGUI gui;
    private final PaginationPanel panel;
    private final Collection<Quest> quests;
    private final Consumer<Quest> action;
    private final InventoryGUI previous;

    public QuestPicker(HumanEntity ent, String guiName, Set<Quest> ignoredQuests, Consumer<Quest> action, InventoryGUI previous) {
        this.player = (Player) ent;
        this.action = action;
        this.previous = previous;
        this.quests = QuestManager.quests().values();
        quests.removeAll(ignoredQuests);

        this.size = Math.min(54, 9 + ItemUtils.minimumChestSize(quests.size()));
        this.gui = new InventoryGUI(Bukkit.createInventory(null, size, FormatUtils.color(guiName)));
        this.panel = new PaginationPanel(gui);
        setup();
        gui.open(player);
    }

    private void setup() {
        gui.fill(0, size, InventoryGUI.FILLER);
        panel.addSlots(0, size - 9);
        quests.stream().map(this::questButton).forEach(panel::addPagedButton);
        gui.addButton(size - 9, ItemButton.create(new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                .setName(FormatUtils.color("&cBack")), evt -> previous.open(player)));
    }

    private ItemButton questButton(Quest quest) {
        return ItemButton.create(new ItemBuilder(quest.getIcon())
                .setName(FormatUtils.color("&b" + quest.getName()))
                .setLore()
                .addLore(FormatUtils.lineWrap(quest.getDescription(), 35).stream()
                        .map(s -> "&2" + s)
                        .map(FormatUtils::color)
                        .collect(Collectors.toList())
                ), evt -> action.andThen(q -> previous.open(player)).accept(quest));
    }

}
