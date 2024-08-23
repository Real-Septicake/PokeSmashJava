package io.github.septicake.cloud.manager

import io.github.septicake.PokeSmashBot
import io.github.septicake.cloud.postprocessors.ChannelRestrictionPostprocessor
import io.github.septicake.cloud.postprocessors.GuildOnlyPostprocessor
import io.github.septicake.cloud.postprocessors.UserPermissionPostprocessor
import org.incendo.cloud.discord.jda5.JDA5CommandManager
import org.incendo.cloud.discord.jda5.JDAInteraction
import org.incendo.cloud.discord.jda5.JDAInteraction.InteractionMapper
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.meta.CommandMeta
import org.incendo.cloud.meta.SimpleCommandMeta

class PokeCloudCommandManager(
    bot: PokeSmashBot
) : JDA5CommandManager<JDAInteraction>(
    ExecutionCoordinator.asyncCoordinator(),
    InteractionMapper.identity()
) {
   init {
       registerCommandPreProcessor(PokeCommandPreprocessor())

       registerCommandPostProcessor(ChannelRestrictionPostprocessor<JDAInteraction>(bot))
       registerCommandPostProcessor(UserPermissionPostprocessor<JDAInteraction>(bot))
       registerCommandPostProcessor(GuildOnlyPostprocessor<JDAInteraction>())
   }

    override fun hasPermission(sender: JDAInteraction, permission: String): Boolean  = true

    override fun createDefaultCommandMeta(): CommandMeta = SimpleCommandMeta.empty()
}
