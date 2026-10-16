package com.regear;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/**
 * Two items appear in the creative "Combat" tab:
 *   Regear: Hotbar Kit     -> fills hotbar + netherite armor
 *   Regear: Inventory Kit  -> fills the 27 inventory slots
 * Hold one and right-click. It is used up. Creative mode only.
 */
public class RegearClient implements ClientModInitializer {
    private static final String HOTBAR_NAME = "Regear: Hotbar Kit";
    private static final String INV_NAME = "Regear: Inventory Kit";

    @Override
    public void onInitializeClient() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {
            entries.add(marker(HOTBAR_NAME, Formatting.AQUA));
            entries.add(marker(INV_NAME, Formatting.LIGHT_PURPLE));
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack held = player.getStackInHand(hand);
            String kind = kindOf(held);
            if (kind == null) return ActionResult.PASS;
            if (world.isClient()) {
                if (!player.isCreative()) {
                    player.sendMessage(Text.literal("Regear only works in creative mode."), true);
                } else if (kind.equals(HOTBAR_NAME)) {
                    giveHotbarKit(player, world);
                } else {
                    giveInventoryKit(player, world);
                }
            }
            return ActionResult.FAIL;
        });
    }

    // ---------- marker items ----------
    private static ItemStack marker(String name, Formatting color) {
        ItemStack s = new ItemStack(Items.NETHER_STAR);
        s.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name).formatted(color));
        return s;
    }

    private static String kindOf(ItemStack s) {
        if (s == null || s.isEmpty() || s.getItem() != Items.NETHER_STAR) return null;
        String n = s.getName().getString();
        if (n.equals(HOTBAR_NAME) || n.equals(INV_NAME)) return n;
        return null;
    }

    // ---------- kits ----------
    private static void giveHotbarKit(PlayerEntity p, World w) {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        ench(sword, w, Enchantments.SHARPNESS, 5);
        ench(sword, w, Enchantments.KNOCKBACK, 1);

        put(p, slotHotbar(0), sword);
        put(p, slotHotbar(1), new ItemStack(Items.END_CRYSTAL, 64));
        put(p, slotHotbar(2), new ItemStack(Items.OBSIDIAN, 64));
        put(p, slotHotbar(3), new ItemStack(Items.RESPAWN_ANCHOR, 64));
        put(p, slotHotbar(4), new ItemStack(Items.GLOWSTONE, 64));
        put(p, slotHotbar(5), new ItemStack(Items.ENDER_PEARL, 16));
        put(p, slotHotbar(6), new ItemStack(Items.TOTEM_OF_UNDYING));
        put(p, slotHotbar(7), new ItemStack(Items.GOLDEN_APPLE, 48));
        put(p, slotHotbar(8), new ItemStack(Items.DIAMOND_PICKAXE));

        // netherite armor: helmet/chestplate Prot 4, leggings/boots Blast Prot 4, all Unbreaking 3 + Mending
        put(p, 5, armor(Items.NETHERITE_HELMET, w, Enchantments.PROTECTION));
        put(p, 6, armor(Items.NETHERITE_CHESTPLATE, w, Enchantments.PROTECTION));
        put(p, 7, armor(Items.NETHERITE_LEGGINGS, w, Enchantments.BLAST_PROTECTION));
        put(p, 8, armor(Items.NETHERITE_BOOTS, w, Enchantments.BLAST_PROTECTION));

        clearMarkers(p);
        p.sendMessage(Text.literal("Hotbar kit + armor given."), true);
    }

    private static void giveInventoryKit(PlayerEntity p, World w) {
        // 3 rows x 9, top row = container slots 9..17
        Item T = Items.TOTEM_OF_UNDYING;
        ItemStack[] kit = new ItemStack[27];
        for (int i = 0; i < 27; i++) kit[i] = ItemStack.EMPTY;
        // row 1
        for (int i = 0; i < 7; i++) kit[i] = new ItemStack(T);
        kit[7] = new ItemStack(Items.EXPERIENCE_BOTTLE, 64);
        kit[8] = new ItemStack(Items.EXPERIENCE_BOTTLE, 64);
        // row 2
        for (int i = 9; i < 17; i++) kit[i] = new ItemStack(T);
        kit[17] = new ItemStack(Items.EXPERIENCE_BOTTLE, 36);
        // row 3
        kit[18] = new ItemStack(T);
        kit[19] = new ItemStack(Items.END_CRYSTAL, 64);
        kit[20] = new ItemStack(Items.OBSIDIAN, 64);
        kit[21] = new ItemStack(Items.ENDER_PEARL, 16);
        kit[22] = new ItemStack(Items.ENDER_PEARL, 16);
        kit[23] = new ItemStack(Items.ENDER_PEARL, 16);
        kit[24] = new ItemStack(T);
        kit[25] = new ItemStack(T);
        kit[26] = new ItemStack(T);
        for (int i = 0; i < 27; i++) put(p, 9 + i, kit[i]);

        clearMarkers(p);
        p.sendMessage(Text.literal("Inventory kit given."), true);
    }

    // ---------- helpers ----------
    /** Container slot id of hotbar slot i (0..8) in the player's screen handler. */
    private static int slotHotbar(int i) {
        return 36 + i;
    }

    private static void put(PlayerEntity p, int slotId, ItemStack stack) {
        MinecraftClient mc = MinecraftClient.getInstance();
        p.playerScreenHandler.getSlot(slotId).setStack(stack.copy());
        if (mc.interactionManager != null) mc.interactionManager.clickCreativeStack(stack.copy(), slotId);
    }

    /** Removes any leftover Regear items so the used one "goes away". */
    private static void clearMarkers(PlayerEntity p) {
        for (int i = 0; i < 9; i++) {
            if (kindOf(p.getInventory().getStack(i)) != null) put(p, slotHotbar(i), ItemStack.EMPTY);
        }
        if (kindOf(p.getOffHandStack()) != null) put(p, 45, ItemStack.EMPTY);
    }

    private static ItemStack armor(Item item, World w, RegistryKey<Enchantment> prot) {
        ItemStack s = new ItemStack(item);
        ench(s, w, prot, 4);
        ench(s, w, Enchantments.UNBREAKING, 3);
        ench(s, w, Enchantments.MENDING, 1);
        return s;
    }

    private static void ench(ItemStack stack, World w, RegistryKey<Enchantment> key, int level) {
        RegistryEntry<Enchantment> e = w.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(key);
        stack.addEnchantment(e, level);
    }
}
