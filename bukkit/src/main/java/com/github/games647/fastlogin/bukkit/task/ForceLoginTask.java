package com.github.games647.fastlogin.bukkit.task;

import com.github.games647.fastlogin.bukkit.BukkitLoginSession;
import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.core.PremiumStatus;
import com.github.games647.fastlogin.core.message.SuccessMessage;
import com.github.games647.fastlogin.core.shared.FastLoginCore;
import com.github.games647.fastlogin.core.shared.ForceLoginManagement;
import com.github.games647.fastlogin.core.shared.LoginSession;

import java.util.concurrent.ExecutionException;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

public class ForceLoginTask extends ForceLoginManagement<Player, CommandSender, BukkitLoginSession, FastLoginBukkit> {

    public ForceLoginTask(FastLoginCore<Player, CommandSender, FastLoginBukkit> core, Player player) {
        super(core, player, getSession(core.getPlugin(), player));
    }

    private static BukkitLoginSession getSession(FastLoginBukkit plugin, Player player) {
        //remove the bungeecord identifier if there is ones
        String id = '/' + player.getAddress().getAddress().getHostAddress() + ':' + player.getAddress().getPort();
        return plugin.getLoginSessions().remove(id);
    }

    @Override
    public void run() {
        //blacklist this target player for BungeeCord ID brute force attacks
        FastLoginBukkit plugin = core.getPlugin();
        player.setMetadata(core.getPlugin().getName(), new FixedMetadataValue(plugin, true));

        super.run();

        PremiumStatus status = PremiumStatus.CRACKED;
        if (isOnlineMode()) {
            status = PremiumStatus.PREMIUM;
        }

        plugin.getPremiumPlayers().put(player.getUniqueId(), status);
    }

    @Override
    public void onForceActionSuccess(LoginSession session) {
        if (core.getPlugin().isBungeeEnabled()) {
            core.getPlugin().sendPluginMessage(player, new SuccessMessage());
        }
    }

    @Override
    public String getName(Player player) {
        return player.getName();
    }

    @Override
    public boolean isOnline(Player player) {
        try {
            //the player-list isn't thread-safe
            return Bukkit.getScheduler().callSyncMethod(core.getPlugin(), player::isOnline).get();
        } catch (InterruptedException | ExecutionException ex) {
            core.getPlugin().getLog().error("Failed to perform thread-safe online check for {}", player, ex);
            return false;
        }
    }

    @Override
    public boolean isOnlineMode() {
        if (session == null) {
            return false;
        }

        return session.isVerified() && player.getName().equals(session.getUsername());
    }
}
