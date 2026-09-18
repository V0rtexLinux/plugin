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
    private static final int SLOTS = 19;

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

        if (apply && getConfig().getBoolean("executed", false)) {
            sender.sendMessage("§cEste programa ja foi executado antes e esta travado por seguranca.");
            return true;
        }

        LuckPerms luckPerms = LuckPermsProvider.get();
        UserManager userManager = luckPerms.getUserManager();

        sender.sendMessage("§7Carregando dados de todos os jogadores conhecidos, aguarde...");

        List<OfflinePlayer> candidates = Arrays.stream(Bukkit.getOfflinePlayers())
                .filter(p -> p.getFirstPlayed() > 0)
                .filter(p -> !isStaff(userManager, p.getUniqueId()))
                .sorted(Comparator.comparingLong(OfflinePlayer::getFirstPlayed))
                .limit(SLOTS)
                .collect(Collectors.toList());

        sender.sendMessage("§6[AlphaGiveaway] " + candidates.size() + " jogador(es) elegivel(is) encontrados:");
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
        }

        getConfig().set("executed", true);
        saveConfig();

        Bukkit.broadcastMessage("§d[ALPHA] O rank ALPHA foi distribuido para os " + candidates.size() +
                " jogadores fundadores do servidor!");
        sender.sendMessage("§aFeito. Programa travado - nao pode ser executado novamente.");
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
}                                                           
