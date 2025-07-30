package rpg.stats.rpg_stats.managers;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerData implements ConfigurationSerializable {
    private int level;
    private float xp;
    private int availablePoints;
    private int strength;
    private int dexterity;
    private int constitution;
    private int mana;
    private int maxMana;
    private String playerClass;
    private final Map<String, Integer> metadata = new ConcurrentHashMap<>();

    public PlayerData() {
        this(1, 0.0f, 0, 1, 1, 1, 100, 100, "none");
    }

    public PlayerData(int level, float xp, int availablePoints,
                      int strength, int dexterity, int constitution,
                      int mana, int maxMana, @NotNull String playerClass) {
        setLevel(level);
        setXp(xp);
        setAvailablePoints(availablePoints);
        setStrength(strength);
        setDexterity(dexterity);
        setConstitution(constitution);
        setMaxMana(maxMana);
        setMana(mana);
        setPlayerClass(playerClass);

        // Initialize default metadata
        setMetadata("inteligencia", 1);
        setMetadata("sabiduria", 1);
        setMetadata("precision", 1);
        setMetadata("agilidad", 1);
    }

    // Getters and setters with validation
    public int getLevel() { return level; }
    public float getXp() { return xp; }
    public int getAvailablePoints() { return availablePoints; }
    public int getStrength() { return strength; }
    public int getDexterity() { return dexterity; }
    public int getConstitution() { return constitution; }
    public int getMana() { return mana; }
    public int getMaxMana() { return maxMana; }
    public @NotNull String getPlayerClass() { return playerClass; }

    public int getMetadata(String key) {
        return metadata.getOrDefault(key, 0);
    }

    public void setMetadata(String key, int value) {
        metadata.put(key.toLowerCase(), Math.max(0, value));
    }


    public boolean hasMetadata(String key) {
        return metadata.containsKey(key.toLowerCase());
    }

    public Map<String, Integer> getAllMetadata() {
        return new HashMap<>(metadata);
    }

    public void setLevel(int level) {
        this.level = Math.max(1, level);
    }

    public void setXp(float xp) {
        this.xp = Math.max(0, xp);
    }

    public void setAvailablePoints(int availablePoints) {
        this.availablePoints = Math.max(0, availablePoints);
    }

    public void setStrength(int strength) {
        this.strength = Math.max(0, strength);
    }

    public void setDexterity(int dexterity) {
        this.dexterity = Math.max(0, dexterity);
    }

    public void setConstitution(int constitution) {
        this.constitution = Math.max(0, constitution);
    }

    public void setMana(int mana) {
        this.mana = Math.min(Math.max(0, mana), this.maxMana);
    }

    public void setMaxMana(int maxMana) {
        this.maxMana = Math.max(10, maxMana);
        this.mana = Math.min(this.mana, this.maxMana);
    }

    public void setPlayerClass(@NotNull String playerClass) {
        this.playerClass = playerClass;
    }

    public void addXp(float amount) {
        setXp(this.xp + amount);
    }

    public void levelUp() {
        setLevel(this.level + 1);
        setAvailablePoints(this.availablePoints + 3);
        setXp(0);
        setMaxMana(this.maxMana + 10);
        setMana(this.maxMana);
    }

    public boolean spendPoints(int amount) {
        if (canSpendPoints(amount)) {
            setAvailablePoints(this.availablePoints - amount);
            return true;
        }
        return false;
    }

    public boolean canSpendPoints(int amount) {
        return this.availablePoints >= amount && amount > 0;
    }

    public void resetStats() {
        setLevel(1);
        setXp(0);
        setAvailablePoints(0);
        setStrength(1);
        setDexterity(1);
        setConstitution(1);
        setMaxMana(100);
        setMana(100);
        setPlayerClass("none");
        metadata.clear();

        // Reset default metadata
        setMetadata("inteligencia", 1);
        setMetadata("sabiduria", 1);
        setMetadata("precision", 1);
        setMetadata("agilidad", 1);
    }

    @NotNull
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("level", level);
        data.put("xp", xp);
        data.put("available-points", availablePoints);

        Map<String, Object> attrs = new HashMap<>();
        attrs.put("strength", strength);
        attrs.put("dexterity", dexterity);
        attrs.put("constitution", constitution);
        data.put("attributes", attrs);

        data.put("mana", mana);
        data.put("max-mana", maxMana);
        data.put("class", playerClass);

        if (!metadata.isEmpty()) {
            data.put("metadata", new HashMap<>(metadata));
        }

        return data;
    }

    public static PlayerData deserialize(Map<String, Object> data) {
        PlayerData pd = new PlayerData();
        pd.level = (int) data.getOrDefault("level", 1);
        pd.xp = ((Number) data.getOrDefault("xp", 0.0f)).floatValue();
        pd.availablePoints = (int) data.getOrDefault("available-points", 0);

        Map<String, Object> attrs = (Map<String, Object>) data.getOrDefault("attributes", new HashMap<>());
        pd.strength = (int) attrs.getOrDefault("strength", 1);
        pd.dexterity = (int) attrs.getOrDefault("dexterity", 1);
        pd.constitution = (int) attrs.getOrDefault("constitution", 1);

        pd.mana = (int) data.getOrDefault("mana", 100);
        pd.maxMana = (int) data.getOrDefault("max-mana", 100);
        pd.playerClass = (String) data.getOrDefault("class", "none");

        Map<String, Object> meta = (Map<String, Object>) data.getOrDefault("metadata", new HashMap<>());
        meta.forEach((key, value) -> pd.metadata.put(key, (int) value));

        return pd;
    }


    @Override
    public String toString() {
        return String.format(
                "PlayerData[Nivel=%d, XP=%.1f, Puntos=%d, Fuerza=%d, Destreza=%d, Constitución=%d, Maná=%d/%d, Clase=%s]",
                level, xp, availablePoints, strength, dexterity, constitution, mana, maxMana, playerClass
        );
    }
}