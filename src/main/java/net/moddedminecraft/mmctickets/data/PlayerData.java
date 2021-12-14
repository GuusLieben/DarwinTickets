package net.moddedminecraft.mmctickets.data;

import com.google.common.reflect.TypeToken;

import net.moddedminecraft.mmctickets.util.PlayerDataUtil;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;

public class PlayerData extends PlayerDataUtil {

    public PlayerData(UUID playerUUID, String playerName, int bannedStatus) {
        super(playerUUID, playerName, bannedStatus);
    }

    public static class PlayerDataSerializer implements TypeSerializer<PlayerData> {

        @Override
        public PlayerData deserialize(@NotNull TypeToken<?> token, ConfigurationNode node) {
            return new PlayerData(
                    UUID.fromString(node.getNode("uuid").getString()),
                    node.getNode("name").getString(),
                    node.getNode("bannedstatus").getInt());
        }

        @Override
        public void serialize(@NotNull TypeToken<?> token, PlayerData playerData, ConfigurationNode node) {
            node.getNode("uuid").setValue(playerData.playerUUID.toString());
            node.getNode("name").setValue(playerData.playerName);
            node.getNode("bannedstatus").setValue(playerData.bannedStatus);
        }
    }
}
