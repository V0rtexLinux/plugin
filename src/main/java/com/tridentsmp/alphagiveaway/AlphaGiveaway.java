package com.tridentsmp.alphagiveaway;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;

public class AlphaGiveaway extends JavaPlugin implements CommandExecutor {

    // Grupos considerados staff/dev/etc - NUNCA recebem o rank alpha por este programa
    private static final Set<String> STAFF_GROUPS = new HashSet<>(Arrays.asList(
            "owner", "dono", "admin", "mod", "helper", "builder",
            "admdeveloper", "ajudante-"
    ));

    private static final String RANK_GROUP = "alpha";
    private static final int TOTAL_SLOTS = 18;

    @Override
    public void onEnable() {
        getCommand("alphagiveaway").setExecutor(this);
        getLogger().info("AlphaGiveaway carregado. Use /alphagiveaway dryrun para conferir a lista antes de aplicar.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0 || (!args[0].equalsIgnoreCase("run") && !args[0].equalsIgnoreCase("dryrun"))) {
            sender.sendMessage("Uso: /alphagiveaway <dryrun|run>");
            return true;
        }

        boolean apply = args[0].equalsIgnoreCase("run");

        Set<UUID> granted = getGrantedUuids();
        int remaining = TOTAL_SLOTS - granted.size();

        if (remaining <= 0) {
            sender.sendMessage("§cAs 19 vagas do rank ALPHA ja foram todas distribuidas. Programa encerrado.");
            return true;
        }

        LuckPerms luckPerms = LuckPermsProvider.get();
        UserManager userManager = luckPerms.getUserManager();

        sender.sendMessage("§7Carregando dados de todos os jogadores conhecidos, aguarde...");

        List<OfflinePlayer> candidates = Arrays.stream(Bukkit.getOfflinePlayers())
                .filter(p -> p.getFirstPlayed() > 0)
                .filter(p -> !granted.contains(p.getUniqueId()))
                .filter(p -> !isStaff(userManager, p.getUniqueId()))
                .sorted(Comparator.comparingLong(OfflinePlayer::getFirstPlayed))
                .limit(remaining)
                .collect(Collectors.toList());

        sender.sendMessage("§6[AlphaGiveaway] " + candidates.size() + " novo(s) elegivel(is) encontrados (" +
                granted.size() + "/" + TOTAL_SLOTS + " ja concedidos antes):");
        int i = 1;
        for (OfflinePlayer p : candidates) {
            sender.sendMessage("§7" + (i++) + ". §f" + p.getName() +
                    " §7(primeiro join: " + new Date(p.getFirstPlayed()) + ")");
        }

        if (!apply) {
            sender.sendMessage("§eNenhuma alteracao foi feita (dryrun). Use /alphagiveaway run para aplicar de verdade.");
            return true;
        }

        for (OfflinePlayer p : candidates) {
            User user = userManager.loadUser(p.getUniqueId()).join();
            user.data().add(InheritanceNode.builder(RANK_GROUP).build());
            userManager.saveUser(user);
            granted.add(p.getUniqueId());
        }

        saveGrantedUuids(granted);

        Bukkit.broadcastMessage("§d[ALPHA] O rank ALPHA foi distribuido para mais " + candidates.size() +
                " jogador(es) fundador(es) do servidor! (" + granted.size() + "/" + TOTAL_SLOTS + ")");
        sender.sendMessage("§aFeito. " + granted.size() + "/" + TOTAL_SLOTS +
                " vagas preenchidas. Rode de novo mais tarde para completar o restante.");
        return true;
    }

    // Considera staff se o grupo primario OU qualquer grupo herdado estiver na lista STAFF_GROUPS
    private boolean isStaff(UserManager userManager, UUID uuid) {
        User user = userManager.loadUser(uuid).join();
        if (STAFF_GROUPS.contains(user.getPrimaryGroup().toLowerCase())) return true;
        for (Node node : user.getNodes()) {
            if (node instanceof InheritanceNode) {
                String group = ((InheritanceNode) node).getGroupName().toLowerCase();
                if (STAFF_GROUPS.contains(group)) return true;
            }
        }
        return false;
    }

    private Set<UUID> getGrantedUuids() {
        List<String> raw = getConfig().getStringList("grantedUuids");
        Set<UUID> result = new HashSet<>();
        for (String s : raw) {
            try {
                result.add(UUID.fromString(s));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return result;
    }

    private void saveGrantedUuids(Set<UUID> uuids) {
        List<String> raw = uuids.stream().map(UUID::toString).collect(Collectors.toList());
        getConfig().set("grantedUuids", raw);
        saveConfig();
    }
}
