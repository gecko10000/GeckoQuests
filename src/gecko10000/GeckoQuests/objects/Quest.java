package gecko10000.GeckoQuests.objects;

import gecko10000.GeckoQuests.GeckoQuests;
import gecko10000.GeckoQuests.misc.Utils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import redempt.redlib.configmanager.ConfigManager;
import redempt.redlib.configmanager.annotations.ConfigMappable;
import redempt.redlib.configmanager.annotations.ConfigPath;
import redempt.redlib.configmanager.annotations.ConfigValue;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ConfigMappable
public class Quest {

    @ConfigPath
    private String uuid = UUID.randomUUID().toString();

    @ConfigValue
    private String name;

    @ConfigValue
    private Set<UUID> children = ConfigManager.set(UUID.class);

    @ConfigValue
    private ItemStack collectionItem = new ItemStack(Material.BARRIER);

    @ConfigValue
    private long amount = Long.MAX_VALUE;

    private Quest parent;

    private Quest() {}

    public Quest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Quest setName(String name) {
        this.name = name;
        return this;
    }

    public UUID getUUID() {
        return UUID.fromString(uuid);
    }

    public Quest addChild(Quest quest) {
        children.add(quest.getUUID());
        quest.parent = this;
        return this;
    }

    public Set<Quest> getChildren() {
        return children.stream()
                .map(GeckoQuests.getInstance().getQuests()::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public Quest updateParentOfChildren() {
        children.stream()
                .map(GeckoQuests.getInstance().getQuests()::get)
                .filter(Objects::nonNull)
                .forEach(q -> q.parent = this);
        return this;
    }

    public ItemStack getCollectionItem() {
        return collectionItem;
    }

    public Quest setCollectionItem(ItemStack item) {
        if (!Utils.isEmpty(item)) {
            this.collectionItem = item;
        }
        return this;
    }

    public long getAmount() {
        return amount;
    }

    public Quest setAmount(long amount) {
        this.amount = Math.max(amount, 1);
        return this;
    }

    public Quest getParent() {
        return parent;
    }

}
