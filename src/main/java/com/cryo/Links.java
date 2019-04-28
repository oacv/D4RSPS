package com.cryo;

import com.cryo.db.impl.AccountConnection;
import com.cryo.db.impl.FriendsChatConnection;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.PrivateChannel;
import net.dv8tion.jda.core.requests.RestAction;

import java.util.ArrayList;

public class Links {

    public static void sendWorldNews(String news) {
        ArrayList<Long> channelIds = DiscordBot.getInstance().getWorldNewsChannels();
        channelIds.forEach(id -> DiscordBot.getInstance().getJda().getTextChannelById(id).sendMessage("[World News]" + news).queue());
    }

    public static Object linkFriendsChat(String owner, long discordId) {
        Object[] data = FriendsChatConnection.connection().handleRequest("get-friends-chat", discordId);
        Object[] data2 = FriendsChatConnection.connection().handleRequest("get-discord-channel", owner);
        if (data != null) return "This discord is already linked with an in-game friends chat.";
        if (data2 != null) return "That friends chat is already linked with a discord channel";
        data = FriendsChatConnection.connection().handleRequest("link-friends-chat", owner, discordId);
        if (data == null) return false;
        return true;
    }

    public static boolean linkDiscordAccount(String username, String randomString) {
        Object[] data = AccountConnection.connection().handleRequest("verify", username, randomString);
        boolean linked = data != null;
        if (!linked) return false;
        long discordId = (long) data[0];
        DiscordBot.getInstance().getRoleManager().recheckRoles(DiscordBot.getInstance().getJda().getUserById(discordId));
        return true;
    }

    public static void handleServerMessage(String owner, String displayName, String message) {
        Object[] data = FriendsChatConnection.connection().handleRequest("get-discord-channel", owner);
        if (data == null) return;
        long discordId = (long) data[0];
        DiscordBot.sendMessage(discordId, "[Server]**" + displayName + "**: " + message);
    }

    public static void handleDiscordMessage(Message message) {
        if (message.getAuthor().getIdLong() == DiscordBot.getInstance().getJda().getSelfUser().getIdLong()) return;
        Object[] data = FriendsChatConnection.connection().handleRequest("get-friends-chat", message.getChannel().getIdLong());
        if (data == null) return;
        String owner = (String) data[0];
        data = AccountConnection.connection().handleRequest("get-username", message.getAuthor().getIdLong());
        if (data == null) {
            message.delete().queue();
            RestAction<PrivateChannel> action = message.getAuthor().openPrivateChannel();
            action.queue(privateChannel -> privateChannel.sendMessage("You must have your in-game account linked in order to type in a friends chat linked channel.").queue());
            return;
        }
        String username = (String) data[0];
        message.delete().queue();
        String displayName = DiscordBot.getInstance().getHelper().getDisplayName(username);
        message.getChannel().sendMessage("[Discord]**" + displayName + "**: " + message.getContentRaw()).queue();
        DiscordBot.getInstance().getHelper().sendFriendsChatMessage(owner, username, message.getContentRaw());
    }
}
