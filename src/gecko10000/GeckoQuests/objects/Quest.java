package gecko10000.GeckoQuests.objects;

import eu.endercentral.crazy_advancements.JSONMessage;
import eu.endercentral.crazy_advancements.NameKey;
import eu.endercentral.crazy_advancements.advancement.Advancement;
import eu.endercentral.crazy_advancements.advancement.AdvancementDisplay;
import eu.endercentral.crazy_advancements.advancement.AdvancementVisibility;
import eu.endercentral.crazy_advancements.advancement.criteria.Criteria;
import gecko10000.GeckoQuests.misc.QuestManager;
import gecko10000.GeckoQuests.misc.Utils;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import redempt.redlib.config.annotations.ConfigMappable;
import redempt.redlib.config.annotations.ConfigName;
import redempt.redlib.config.annotations.ConfigPath;
import redempt.redlib.itemutils.ItemBuilder;
import redempt.redlib.misc.FormatUtils;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ConfigMappable
public class Quest {

    @ConfigPath
    private UUID uuid = UUID.randomUUID();
    private Set<UUID> children = new LinkedHashSet<>();
    @ConfigName("collection-item")
    private ItemStack collectionItem = new ItemStack(Material.BARRIER);
    private long amount = Long.MAX_VALUE;
    @ConfigName("sync-item")
    private boolean syncItem = true;
    @ConfigName("exact-item")
    private boolean exactItem = true;
    private Display display = new Display();

    private transient Quest parent;

    @ConfigMappable
    private static class Display {

        private ItemStack icon = new ItemStack(Material.BARRIER);
        private String name = "Name";
        private String description = "Description";
        private AdvancementDisplay.AdvancementFrame frame = AdvancementDisplay.AdvancementFrame.TASK;
        private AdvancementVisibility visibility = AdvancementVisibility.ALWAYS;
        private float x = 1;
        private float y;
        private String texture;

        private AdvancementDisplay getDisplay() {
            return new AdvancementDisplay(icon, toJson(name), toJson(description), frame, texture, visibility);
        }

        private JSONMessage toJson(String input) {
            input = FormatUtils.color(input);
            BaseComponent[] converted = TextComponent.fromLegacyText(input);
            BaseComponent component = new TextComponent();
            for (BaseComponent c : converted) {
                component.addExtra(c);
            }
            return new JSONMessage(component);
        }

    }

    private Quest() {}

    public Quest(String name) {
        setName(name);
    }

    public UUID getUUID() {
        return uuid;
    }

    public Quest addChild(Quest quest) {
        children.add(quest.uuid);
        quest.parent = this;
        return this;
    }

    public Quest removeChild(Quest quest) {
        children.remove(quest.uuid);
        quest.parent = null;
        return this;
    }

    public Set<Quest> getChildren() {
        return children.stream()
                .map(QuestManager.quests()::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public LinkedHashSet<Quest> getAllChildren(LinkedHashSet<Quest> set) {
        set.add(this);
        getChildren().forEach(q -> q.getAllChildren(set));
        return set;
    }

    public Quest updateParentOfChildren() {
        children.stream()
                .map(QuestManager.quests()::get)
                .filter(Objects::nonNull)
                .forEach(q -> q.parent = this);
        return this;
    }

    public ItemBuilder editorItem() {
        return new ItemBuilder(getIcon())
                .setName(FormatUtils.color("&b" + getName()))
                .addLore(FormatUtils.lineWrap(getDescription(), 35).stream()
                        .map(s -> "&2" + s)
                        .map(FormatUtils::color)
                        .collect(Collectors.toList()));
    }

    public ItemStack getCollectionItem() {
        return collectionItem;
    }

    public Quest setCollectionItem(ItemStack item) {
        if (!Utils.isEmpty(item)) {
            item = new ItemStack(new ItemBuilder(item).setCount(1));
            this.collectionItem = item;
            if (syncItem) {
                this.display.icon = item;
            }
        }
        return this;
    }

    public ItemStack getIcon() {
        return this.display.icon;
    }

    public Quest setIcon(ItemStack item) {
        if (!Utils.isEmpty(item)) {
            item = new ItemStack(new ItemBuilder(item).setCount(1));
            this.display.icon = item;
            if (syncItem) {
                this.collectionItem = item;
            }
        }
        return this;
    }

    public boolean isSyncItem() {
        return syncItem;
    }

    public Quest setSyncItem(boolean syncItem) {
        this.syncItem = syncItem;
        return this;
    }

    public boolean isExactItem() {
        return exactItem;
    }

    public Quest setExactItem(boolean exactItem) {
        this.exactItem = exactItem;
        return this;
    }

    public long getAmount() {
        return amount;
    }

    public Quest setAmount(long amount) {
        this.amount = Math.max(amount, 1);
        return this;
    }

    public String getName() {
        return this.display.name;
    }

    public Quest setName(String title) {
        this.display.name = title;
        return this;
    }

    public String getDescription() {
        return this.display.description;
    }

    public Quest setDescription(String description) {
        this.display.description = description;
        return this;
    }

    public AdvancementDisplay.AdvancementFrame getFrame() {
        return this.display.frame;
    }

    public Quest setFrame(AdvancementDisplay.AdvancementFrame frame) {
        this.display.frame = frame;
        return this;
    }

    public AdvancementVisibility getVisibility() {
        return this.display.visibility;
    }

    public Quest setVisibility(AdvancementVisibility visibility) {
        this.display.visibility = visibility;
        return this;
    }

    public float getX() {
        return this.display.x;
    }

    public Quest setX(float x) {
        this.display.x = x;
        return this;
    }

    public float getY() {
        return this.display.y;
    }

    public Quest setY(float y) {
        this.display.y = y;
        return this;
    }

    public String getTexture() {
        return this.display.texture;
    }

    public Quest setTexture(String texture) {
        this.display.texture = texture;
        return this;
    }

    public Quest getParent() {
        return parent;
    }

    public Quest getRoot() {
        return parent == null ? this : parent.getRoot();
    }

    public boolean isRoot() {
        return parent == null;
    }

    private transient Advancement advancement;

    public Advancement getAdvancement() {
        return advancement == null ? updateAdvancement(false) : advancement;
    }

    public Advancement updateAdvancement(boolean send) {
        AdvancementDisplay display = this.display.getDisplay();
        Advancement parentAdv = parent == null ? null : parent.getAdvancement();
        display.setPositionOrigin(parentAdv);
        display.setCoordinates(this.display.x, this.display.y);
        Advancement advancement = new Advancement(parentAdv, nameKey(), display);
        advancement.setCriteria(new Criteria(isRoot() ? 1 : 100));
        this.advancement = advancement;
        if (send) {
            QuestManager.updateAdvancements();
        }
        return advancement;
    }

    public NameKey nameKey() {
        return new NameKey("gq", uuid.toString() + getFrame());
    }

}
