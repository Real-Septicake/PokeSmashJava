package io.github.septicake.pokeapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class VersionInfo(
    val id: Int,
    val name: String,
    @SerialName("version_group")
    val versionGroup: JsonObject,
    val names: List<LocalizedName>
)
