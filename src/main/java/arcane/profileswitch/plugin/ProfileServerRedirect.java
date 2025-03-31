package arcane.profileswitch.plugin;

import fr.phoenixdevt.profiles.ProfileProvider;
import fr.phoenixdevt.profiles.event.ProfileCreateEvent;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public class ProfileServerRedirect extends JavaPlugin implements Listener {

    private ProfileProvider provider;
    private final Set<UUID> placeholderTriggered = new HashSet<>();
    private final Set<UUID> createdRecently = new HashSet<>();
    private final Map<UUID, Location> lockedLocations = new HashMap<>();

    private String profilePrefix;
    private String serverOnProfileCreate;
    private String serverOnProfileSelect;
    private String kickMessage;
    private long profileCreateDelayTicks;
    private long profileSelectDelayTicks;
    private long kickDelayTicks;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();

        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        Bukkit.getPluginManager().registerEvents(this, this);
        provider = Bukkit.getServicesManager().load(ProfileProvider.class);
        startPlaceholderWatcher();
    }

    private void loadSettings() {
        FileConfiguration config = getConfig();
        profilePrefix = config.getString("profile-prefix", "Profile N").toLowerCase();
        serverOnProfileCreate = config.getString("server-on-profile-create", "lobby");
        serverOnProfileSelect = config.getString("server-on-profile-select", "RPG");
        kickMessage = config.getString("kick-message", "Server is full!");
        profileCreateDelayTicks = config.getLong("profile-create-delay", 40L);
        profileSelectDelayTicks = config.getLong("profile-select-delay", 100L);
        kickDelayTicks = config.getLong("kick-delay", 100L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("profileswitch.bypass")) return;

        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 255, true, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 255, true, false));
        lockedLocations.put(player.getUniqueId(), player.getLocation().clone());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("profileswitch.bypass")) return;

        Location locked = lockedLocations.get(player.getUniqueId());
        if (locked != null && event.getTo() != null &&
                (locked.getX() != event.getTo().getX() ||
                        locked.getY() != event.getTo().getY() ||
                        locked.getZ() != event.getTo().getZ() ||
                        locked.getYaw() != event.getTo().getYaw() ||
                        locked.getPitch() != event.getTo().getPitch())) {
            event.setTo(locked);
        }
    }

    @EventHandler
    public void onProfileCreate(ProfileCreateEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("profileswitch.bypass")) return;

        UUID id = player.getUniqueId();
        createdRecently.add(id);

        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (player.isOnline()) sendToServer(player, serverOnProfileCreate);
            scheduleKickCheck(player);
        }, profileCreateDelayTicks);

        Bukkit.getScheduler().runTaskLater(this, () -> createdRecently.remove(id), 200L);
    }

    private void startPlaceholderWatcher() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID id = player.getUniqueId();
                    if (player.hasPermission("profileswitch.bypass")) continue;
                    if (placeholderTriggered.contains(id) || createdRecently.contains(id)) continue;

                    String profile = PlaceholderAPI.setPlaceholders(player, "%mmoprofiles_current_profile_name%");
                    if (profile != null && profile.toLowerCase().startsWith(profilePrefix)) {
                        placeholderTriggered.add(id);
                        sendToServer(player, serverOnProfileSelect);
                        scheduleKickCheck(player);
                        Bukkit.getScheduler().runTaskLater(ProfileServerRedirect.this, () -> placeholderTriggered.remove(id), profileSelectDelayTicks);
                    }
                }
            }
        }.runTaskTimer(this, 20L, 20L);
    }

    private void sendToServer(Player player, String serverName) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (DataOutputStream data = new DataOutputStream(out)) {
            data.writeUTF("Connect");
            data.writeUTF(serverName);
            player.sendPluginMessage(this, "BungeeCord", out.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void scheduleKickCheck(Player player) {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (player.isOnline() && !player.hasPermission("profileswitch.bypass")) {
                player.kickPlayer(kickMessage);
            }
        }, kickDelayTicks);
    }
}
