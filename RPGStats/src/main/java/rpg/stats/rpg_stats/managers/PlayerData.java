package rpg.stats.rpg_stats.managers;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class PlayerData implements ConfigurationSerializable {
    private int level;
    private float xp;
    private int availablePoints;
    private int strength;
    private int dexterity;
    private int constitution;
    private String playerClass;

    // Constructor para nuevos jugadores (valores por defecto)
    public PlayerData(int defaultLevel, float defaultXP, int defaultAvailablePoints, int defaultStrength, int defaultDexterity, int defaultConstitution, String defaultClass) {
        this(1, 0.0f, 0, 0, 0, 0, "none");

    }

    // Constructor completo

    public PlayerData(int level, float xp, int availablePoints,
                      int strength, int dexterity, int constitution,
                      @NotNull String playerClass, int mana, int maxMana) {
        this.level = level;
        this.xp = xp;
        this.availablePoints = availablePoints;
        this.strength = strength;
        this.dexterity = dexterity;
        this.constitution = constitution;
        this.playerClass = playerClass;
        this.mana = mana;
        this.maxMana = maxMana;
    }

    // Getters y Setters
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = Math.max(1, level); }

    public float getXp() { return xp; }
    public void setXp(float xp) { this.xp = Math.max(0, xp); }

    public int getMana() { return mana; }
    public void setMana(int mana) {
        this.mana = Math.max(0, Math.min(mana, maxMana));
    }

    public int getMaxMana() { return maxMana; }
    public void setMaxMana(int maxMana) {
        this.maxMana = Math.max(1, maxMana);
        this.mana = Math.min(mana, maxMana);
    }

    public int getAvailablePoints() {
        return availablePoints;
    }

    public void setAvailablePoints(int availablePoints) {
        this.availablePoints = Math.max(0, availablePoints);
    }

    public int getStrength() {
        return strength;
    }

    public void setStrength(int strength) {
        this.strength = Math.max(0, strength);
    }

    public int getDexterity() {
        return dexterity;
    }

    public void setDexterity(int dexterity) {
        this.dexterity = Math.max(0, dexterity);
    }

    public int getConstitution() {
        return constitution;
    }

    public void setConstitution(int constitution) {
        this.constitution = Math.max(0, constitution);
    }

    @NotNull
    public String getPlayerClass() {
        return playerClass;
    }

    public void setPlayerClass(@NotNull String playerClass) {
        this.playerClass = playerClass;
    }

    // ===== Métodos útiles =====
    public void addXp(float amount) {
        this.xp += amount;
    }

    public void levelUp() {
        this.level++;
        this.availablePoints += 3; // 3 puntos por subir de nivel
    }

    public boolean spendPoints(int amount) {
        if (availablePoints >= amount) {
            availablePoints -= amount;
            return true;
        }
        return false;
    }

    // Método para resetear stats (usado en /rpgadmin reset)
    public void resetStats() {
        this.level = 1;
        this.xp = 0;
        this.availablePoints = 0;
        this.strength = 0;
        this.dexterity = 0;
        this.constitution = 0;
        this.playerClass = "none";
    }
    // ===== Serialización (guardar) =====
    @NotNull
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("level", level);
        data.put("xp", xp);
        data.put("availablePoints", availablePoints);
        data.put("strength", strength);
        data.put("dexterity", dexterity);
        data.put("constitution", constitution);
        data.put("class", playerClass);  // "class" es palabra reservada, usamos minúscula
        return data;
    }

    // ===== Deserialización (cargar) =====
    public static PlayerData deserialize(@NotNull Map<String, Object> data) {
        return new PlayerData(
                (int) data.getOrDefault("level", 1),
                ((Number) data.getOrDefault("xp", 0.0f)).floatValue(),
                (int) data.getOrDefault("availablePoints", 0),
                (int) data.getOrDefault("strength", 0),
                (int) data.getOrDefault("dexterity", 0),
                (int) data.getOrDefault("constitution", 0),
                (String) data.getOrDefault("class", "none")
        );
    }
}
