package net.horizonsend.ion.server.features.enviornment.effects

import net.horizonsend.ion.common.utils.miscellaneous.squared
import net.horizonsend.ion.server.IonServerComponent
import net.horizonsend.ion.server.features.gear.powerarmor.PowerArmorManager
import net.horizonsend.ion.server.features.gear.powerarmor.PowerArmorModule
import net.horizonsend.ion.server.features.misc.getPower
import net.horizonsend.ion.server.features.misc.removePower
import net.horizonsend.ion.server.miscellaneous.utils.PerPlayerCooldown
import net.horizonsend.ion.server.miscellaneous.utils.Tasks
import net.horizonsend.ion.server.miscellaneous.utils.Vec3i
import net.horizonsend.ion.server.miscellaneous.utils.distanceSquared
import net.horizonsend.ion.server.miscellaneous.utils.isInside
import net.horizonsend.ion.server.miscellaneous.utils.isWater
import net.horizonsend.ion.server.miscellaneous.utils.listen
import net.horizonsend.ion.server.features.custom.blocks
import net.horizonsend.ion.server.features.custom.items
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.concurrent.TimeUnit

object SpaceMechanics : IonServerComponent() {
	override fun onEnable() {
		Tasks.syncRepeat(10, 10) {
			for (player in Bukkit.getOnlinePlayers()) {
				if (player.gameMode != GameMode.SURVIVAL || player.isDead || !player.hasGravity()) {
					continue
				}

				val space = SpaceWorlds.contains(player.world)

				if (isInside(player.eyeLocation, 1)) {
					continue
				}

				checkRadioactive(player)
			}
		}

		listen<EntityDamageEvent> { event ->
			if (SpaceWorlds.contains(event.entity.world) && event.cause == EntityDamageEvent.DamageCause.FALL) {
				event.isCancelled = true
			}
		}
	}

	// function that checks if player is holding, or has in their inventory any of the cutom.blocks ['URANIUM_ORE ', 'URANIUM_BLOCK', 'ENRICHED_URANIUM_BLOCK', 'RAW_URANIUM_BLOCK']
    // if they are holding any of these blocks, or has them in their inventory, run the checkRadioactive function
    private fun checkRadioactive(player: Player) {
        if (isWearingSpaceSuit(player)) {
            return
        }

        if (checkEnvioronmentModule(player)) {
            return
        }

        if (player.inventory.contains(Material.RAW_URANIUM) || player.inventory.contains(Material.URANIUM) || player.inventory.contains(Material.URANIUM_ORE) || player.inventory.contains(Material.URANIUM_BLOCK) || player.inventory.contains(Material.ENRICHED_URANIUM_BLOCK) || player.inventory.contains(Material.RAW_URANIUM_BLOCK)) {
            player.addPotionEffect(PotionEffect(PotionEffectType.CONFUSION, 200, 0, false, false))
        }
    }

	private val enviornmentModulePowerCooldown = PerPlayerCooldown(1, TimeUnit.SECONDS)

	private fun checkEnvioronmentModule(player: Player): Boolean {
		val helmet = player.inventory.helmet
			?: return false

		if (!PowerArmorManager.hasModule(helmet, PowerArmorModule.ENVIRONMENT)) {
			return false
		}

		val powerUsage = 10

		if (getPower(helmet) < powerUsage) {
			return false
		}

		enviornmentModulePowerCooldown.tryExec(player) {
			removePower(helmet, powerUsage)
		}
		return true
	}
}

