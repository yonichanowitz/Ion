package net.horizonsend.ion.server.features.custom.items.type.weapon.blaster

import net.horizonsend.ion.common.database.schema.nations.Nation
import net.horizonsend.ion.server.features.cache.PlayerCache
import net.horizonsend.ion.server.features.custom.items.CustomItemRegistry
import net.horizonsend.ion.server.features.custom.items.CustomItemRegistry.customItem
import net.horizonsend.ion.server.features.custom.items.component.CustomComponentTypes
import net.horizonsend.ion.server.features.world.IonWorld.Companion.hasFlag
import net.horizonsend.ion.server.features.world.WorldFlag
import net.horizonsend.ion.server.listener.SLEventListener
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import kotlin.math.roundToInt

class BlasterListeners : SLEventListener() {
	@Suppress("Unused")
	@EventHandler(priority = EventPriority.LOWEST)
	fun onDeath(event: PlayerDeathEvent) {
		val victim = event.player
		val killer = event.entity.killer ?: return
		val customItem = killer.inventory.itemInMainHand.customItem ?: return

		if (customItem !is Blaster<*>) return

		val arena: String = if (killer.world.hasFlag(WorldFlag.ARENA)) "<#555555>[<#ffff66>Arena<#555555>]<reset> " else ""

		val blaster = customItem.displayName
		val victimColor = if (victim.hasMetadata("NPC")) "<#FFFFFF>" else "<#" + Integer.toHexString((PlayerCache[victim].nationOid?.let { Nation.findById(it) }?.color ?: 16777215)) + ">"

		val killerColor = "<#" + Integer.toHexString((PlayerCache[killer].nationOid?.let { Nation.findById(it) }?.color ?: 16777215)) + ">"

		val distance = killer.location.distance(victim.location)
		val verb = when (customItem.identifier) {
			"SNIPER" -> "sniped"
			"SHOTGUN" -> "blasted"
			"RIFLE" -> "shot"
			"SUBMACHINE_BLASTER" -> "shredded"
			"PISTOL" -> "pelted"
			else -> "shot"
		}

		val newMessage = MiniMessage.miniMessage()
			.deserialize(
				"$arena$victimColor${victim.name}<reset> was $verb by $killerColor${killer.name}<reset> from ${distance.roundToInt()} blocks away, using "
			)
			.append(blaster)

		event.deathMessage(newMessage)
	}

	@EventHandler
	fun onPlayerItemHoldEvent(event: PlayerItemHeldEvent) {
		val itemStack = event.player.inventory.getItem(event.newSlot) ?: return
		val customItem = itemStack.customItem as? Blaster<*> ?: return

		// adding a potion effect because it takes ages for that attack cooldown to come up
		event.player.addPotionEffect(PotionEffect(PotionEffectType.HASTE, 20, 5, false, false, false))

		val ammunition = customItem.ammoComponent.getAmmo(itemStack)

		event.player.sendActionBar(
			Component.text(
				"Ammo: $ammunition / ${customItem.balancing.capacity}",
				NamedTextColor.RED
			)
		)
	}

	@EventHandler(priority = EventPriority.LOWEST)
	@Suppress("Unused")
	fun onLeftClick(event: PlayerInteractEvent) {
		if (event.item == null) return

		val customItem = event.item?.customItem ?: return
		when (event.action) {
			Action.LEFT_CLICK_AIR, Action.LEFT_CLICK_BLOCK -> {
				if (customItem is Blaster<*>) event.isCancelled = true
			}

			else -> return // Unknown Action Enum - We probably don't care, silently fail
		}
	}

	@EventHandler
	fun onPrepareItemCraftEvent(event: PrepareItemCraftEvent) {
		if (!event.isRepair) return // Will always be a combination of 2 items.

		val craftedItems = event.inventory.matrix.filter { it?.customItem is Magazine }.filterNotNull()

		// Only magazines of the same type accepted
		if (craftedItems.isEmpty() ||
			craftedItems.first().customItem?.identifier != craftedItems.last().customItem?.identifier) {
			event.inventory.result = null
			return
		}

		val resultItem = craftedItems.first().customItem as Magazine
		val totalAmmo = craftedItems.sumOf { resultItem.getComponent(CustomComponentTypes.AMMUNITION_STORAGE).getAmmo(it) }.coerceIn(0..resultItem.balancing.capacity)
		val resultItemStack = CustomItemRegistry.getByIdentifier(resultItem.identifier)!!.constructItemStack()
		resultItem.getComponent(CustomComponentTypes.AMMUNITION_STORAGE).setAmmo(resultItemStack, resultItem, totalAmmo)

		event.inventory.result = resultItemStack
	}
}
