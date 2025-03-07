package de.oliver.fancynpcs.v1_19_4;

import com.google.common.collect.ImmutableList;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.datafixers.util.Pair;
import de.oliver.fancylib.ReflectionUtils;
import de.oliver.fancynpcs.api.FancyNpcsPlugin;
import de.oliver.fancynpcs.api.Npc;
import de.oliver.fancynpcs.api.NpcAttribute;
import de.oliver.fancynpcs.api.NpcData;
import de.oliver.fancynpcs.api.events.NpcSpawnEvent;
import de.oliver.fancynpcs.api.utils.NpcEquipmentSlot;
import io.papermc.paper.adventure.PaperAdventure;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import me.dave.chatcolorhandler.ModernChatColorHandler;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_19_R3.CraftServer;
import org.bukkit.craftbukkit.v1_19_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_19_R3.util.CraftNamespacedKey;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public class Npc_1_19_4 extends Npc {

    private final String localName;
    private final UUID uuid;
    private Entity npc;
    private Display.TextDisplay sittingVehicle;

    public Npc_1_19_4(NpcData data) {
        super(data);

        this.localName = generateLocalName();
        this.uuid = UUID.randomUUID();
    }

    @Override
    public void create() {
        MinecraftServer minecraftServer = ((CraftServer) Bukkit.getServer()).getServer();
        ServerLevel serverLevel = ((CraftWorld) data.getLocation().getWorld()).getHandle();
        GameProfile gameProfile = new GameProfile(uuid, localName);

        if (data.getType() == org.bukkit.entity.EntityType.PLAYER) {
            npc = new ServerPlayer(minecraftServer, serverLevel, new GameProfile(uuid, ""));
            ((ServerPlayer) npc).gameProfile = gameProfile;

            if (data.getSkin() != null && data.getSkin().isLoaded()) {
                // sessionserver.mojang.com/session/minecraft/profile/<UUID>?unsigned=false
                ((ServerPlayer) npc).getGameProfile().getProperties().replaceValues("textures", ImmutableList.of(new Property("textures", data.getSkin().getValue(), data.getSkin().getSignature())));
            }
        } else {
            EntityType<?> nmsType = BuiltInRegistries.ENTITY_TYPE.get(CraftNamespacedKey.toMinecraft(data.getType().getKey()));
            EntityType.EntityFactory factory = (EntityType.EntityFactory) ReflectionUtils.getValue(nmsType, MappingKeys1_19_4.ENTITY_TYPE__FACTORY.getMapping()); // EntityType.factory
            npc = factory.create(nmsType, serverLevel);
        }
    }

    @Override
    public void spawn(Player player) {
        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();

        if (npc == null) {
            return;
        }

        if (data.isOnlyVisibleToEnabled() && !data.getOnlyVisibleToPlayers().contains(player.getUniqueId().toString())) {
            return;
        }

        if (!data.getLocation().getWorld().getName().equalsIgnoreCase(serverPlayer.getLevel().getWorld().getName())) {
            return;
        }

        NpcSpawnEvent spawnEvent = new NpcSpawnEvent(this, player);
        spawnEvent.callEvent();
        if (spawnEvent.isCancelled()) {
            return;
        }


        if (npc instanceof ServerPlayer npcPlayer) {
            EnumSet<ClientboundPlayerInfoUpdatePacket.Action> actions = EnumSet.noneOf(ClientboundPlayerInfoUpdatePacket.Action.class);
            actions.add(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER);
            actions.add(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME);
            if (data.isShowInTab()) {
                actions.add(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED);
            }

            ClientboundPlayerInfoUpdatePacket playerInfoPacket = new ClientboundPlayerInfoUpdatePacket(actions, List.of(npcPlayer));
            serverPlayer.connection.send(playerInfoPacket);

            if (data.isSpawnEntity()) {
                npc.setPos(data.getLocation().x(), data.getLocation().y(), data.getLocation().z());
            }
        }

        ClientboundAddEntityPacket addEntityPacket = new ClientboundAddEntityPacket(npc);
        serverPlayer.connection.send(addEntityPacket);

        isVisibleForPlayer.put(player.getUniqueId(), true);

        update(player);
    }

    @Override
    public void remove(Player player) {
        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();

        if (npc instanceof ServerPlayer npcPlayer) {
            ClientboundPlayerInfoRemovePacket playerInfoRemovePacket = new ClientboundPlayerInfoRemovePacket(List.of((npcPlayer.getUUID())));
            serverPlayer.connection.send(playerInfoRemovePacket);
        }

        // remove entity
        ClientboundRemoveEntitiesPacket removeEntitiesPacket = new ClientboundRemoveEntitiesPacket(npc.getId());
        serverPlayer.connection.send(removeEntitiesPacket);

        // remove sitting vehicle
        if (sittingVehicle != null) {
            ClientboundRemoveEntitiesPacket removeSittingVehiclePacket = new ClientboundRemoveEntitiesPacket(sittingVehicle.getId());
            serverPlayer.connection.send(removeSittingVehiclePacket);
        }

        isVisibleForPlayer.remove(player.getUniqueId());
    }

    @Override
    public void lookAt(Player player, Location location) {
        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();

        npc.setRot(location.getYaw(), location.getPitch());
        npc.setYHeadRot(location.getYaw());
        npc.setXRot(location.getPitch());
        npc.setYRot(location.getYaw());

        ClientboundTeleportEntityPacket teleportEntityPacket = new ClientboundTeleportEntityPacket(npc);
        serverPlayer.connection.send(teleportEntityPacket);

        float angelMultiplier = 256f / 360f;
        ClientboundRotateHeadPacket rotateHeadPacket = new ClientboundRotateHeadPacket(npc, (byte) (location.getYaw() * angelMultiplier));
        serverPlayer.connection.send(rotateHeadPacket);
    }

    @Override
    public void update(Player player) {
        if (!isVisibleForPlayer.getOrDefault(player.getUniqueId(), false)) {
            return;
        }

        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();

        PlayerTeam team = new PlayerTeam(serverPlayer.getScoreboard(), "npc-" + localName);
        team.getPlayers().clear();
        team.getPlayers().add(npc instanceof ServerPlayer npcPlayer ? npcPlayer.getGameProfile().getName() : npc.getStringUUID());

        boolean isTeamCreatedForPlayer = isTeamCreated.getOrDefault(serverPlayer.getUUID(), false);
        serverPlayer.connection.send(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, !isTeamCreatedForPlayer));

        if (!isTeamCreatedForPlayer) {
            isTeamCreated.put(serverPlayer.getUUID(), true);
        }

        if (!data.isCollidable()) {
            team.setCollisionRule(Team.CollisionRule.NEVER);
        }

        team.setColor(PaperAdventure.asVanilla(data.getGlowingColor()));

        net.kyori.adventure.text.Component displayName;
        if (data.getDisplayName().equalsIgnoreCase("<empty>")) displayName = MiniMessage.miniMessage().deserialize("<white>NPC</white>");
        else displayName = ModernChatColorHandler.translate(data.getDisplayName(), serverPlayer.getBukkitEntity());
        Component vanillaComponent = PaperAdventure.asVanilla(displayName);
        if (!(npc instanceof ServerPlayer)) {
            npc.setCustomName(vanillaComponent);
        }

        if (data.getDisplayName().equalsIgnoreCase("<empty>")) {
            npc.setCustomNameVisible(false);
            team.setNameTagVisibility(Team.Visibility.NEVER);
        } else {
            npc.setCustomNameVisible(true);
            team.setNameTagVisibility(Team.Visibility.ALWAYS);
        }

        if (npc instanceof ServerPlayer npcPlayer) {
            team.setPlayerPrefix(vanillaComponent);
            npcPlayer.listName = vanillaComponent;

            EnumSet<ClientboundPlayerInfoUpdatePacket.Action> actions = EnumSet.noneOf(ClientboundPlayerInfoUpdatePacket.Action.class);
            actions.add(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME);
            if (data.isShowInTab()) {
                actions.add(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED);
            }

            ClientboundPlayerInfoUpdatePacket playerInfoPacket = new ClientboundPlayerInfoUpdatePacket(actions, List.of(npcPlayer));
            serverPlayer.connection.send(playerInfoPacket);
        }

        npc.setGlowingTag(data.isGlowing());

        if (data.getEquipment() != null && data.getEquipment().size() > 0) {
            List<Pair<EquipmentSlot, ItemStack>> equipmentList = new ArrayList<>();

            for (NpcEquipmentSlot slot : data.getEquipment().keySet()) {
                equipmentList.add(new Pair<>(EquipmentSlot.byName(slot.toNmsName()), CraftItemStack.asNMSCopy(data.getEquipment().get(slot))));
            }

            ClientboundSetEquipmentPacket setEquipmentPacket = new ClientboundSetEquipmentPacket(npc.getId(), equipmentList);
            serverPlayer.connection.send(setEquipmentPacket);
        }

        if (npc instanceof ServerPlayer) {
            // Enable second layer of skin (https://wiki.vg/Entity_metadata#Player)
            npc.getEntityData().set(net.minecraft.world.entity.player.Player.DATA_PLAYER_MODE_CUSTOMISATION, (byte) (0x01 | 0x02 | 0x04 | 0x08 | 0x10 | 0x20 | 0x40));
        }

        data.applyAllAttributes(this);

        refreshEntityData(player);

        if (data.isSpawnEntity() && data.getLocation() != null) {
            move(player);
        }

        NpcAttribute playerPoseAttr = FancyNpcsPlugin.get().getAttributeManager().getAttributeByName(org.bukkit.entity.EntityType.PLAYER, "pose");
        if (data.getAttributes().containsKey(playerPoseAttr) && data.getAttributes().get(playerPoseAttr).equals("sitting")) {
            setSitting(serverPlayer);
        }
    }

    @Override
    protected void refreshEntityData(Player player) {
        if (!isVisibleForPlayer.getOrDefault(player.getUniqueId(), false)) {
            return;
        }

        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();

        Int2ObjectMap<SynchedEntityData.DataItem<?>> itemsById = (Int2ObjectMap<SynchedEntityData.DataItem<?>>) ReflectionUtils.getValue(npc.getEntityData(), MappingKeys1_19_4.SYNCHED_ENTITY_DATA__ITEMS_BY_ID.getMapping()); // itemsById
        List<SynchedEntityData.DataValue<?>> entityData = new ArrayList<>();
        for (SynchedEntityData.DataItem<?> dataItem : itemsById.values()) {
            entityData.add(dataItem.value());
        }
        ClientboundSetEntityDataPacket setEntityDataPacket = new ClientboundSetEntityDataPacket(npc.getId(), entityData);
        serverPlayer.connection.send(setEntityDataPacket);
    }

    public void move(Player player) {
        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();

        npc.setPosRaw(data.getLocation().x(), data.getLocation().y(), data.getLocation().z());
        npc.setRot(data.getLocation().getYaw(), data.getLocation().getPitch());
        npc.setYHeadRot(data.getLocation().getYaw());
        npc.setXRot(data.getLocation().getPitch());
        npc.setYRot(data.getLocation().getYaw());

        ClientboundTeleportEntityPacket teleportEntityPacket = new ClientboundTeleportEntityPacket(npc);
        ReflectionUtils.setValue(teleportEntityPacket, MappingKeys1_19_4.CLIENTBOUND_TELEPORT_ENTITY_PACKET__X.getMapping(), data.getLocation().x()); // 'x'
        ReflectionUtils.setValue(teleportEntityPacket, MappingKeys1_19_4.CLIENTBOUND_TELEPORT_ENTITY_PACKET__Y.getMapping(), data.getLocation().y()); // 'y'
        ReflectionUtils.setValue(teleportEntityPacket, MappingKeys1_19_4.CLIENTBOUND_TELEPORT_ENTITY_PACKET__Z.getMapping(), data.getLocation().z()); // 'z'
        serverPlayer.connection.send(teleportEntityPacket);

        float angelMultiplier = 256f / 360f;
        ClientboundRotateHeadPacket rotateHeadPacket = new ClientboundRotateHeadPacket(npc, (byte) (data.getLocation().getYaw() * angelMultiplier));
        serverPlayer.connection.send(rotateHeadPacket);
    }

    public void setSitting(ServerPlayer serverPlayer) {
        if (sittingVehicle == null) {
            sittingVehicle = new Display.TextDisplay(EntityType.TEXT_DISPLAY, ((CraftWorld) data.getLocation().getWorld()).getHandle());
        }

        sittingVehicle.setPos(data.getLocation().x(), data.getLocation().y(), data.getLocation().z());

        ClientboundAddEntityPacket addEntityPacket = new ClientboundAddEntityPacket(sittingVehicle);
        serverPlayer.connection.send(addEntityPacket);

        sittingVehicle.passengers = ImmutableList.of(npc);

        ClientboundSetPassengersPacket packet = new ClientboundSetPassengersPacket(sittingVehicle);
        serverPlayer.connection.send(packet);
    }

    @Override
    public float getEyeHeight() {
        return npc.getEyeHeight();
    }

    @Override
    public int getEntityId() {
        return npc.getId();
    }

    public Entity getNpc() {
        return npc;
    }
}
