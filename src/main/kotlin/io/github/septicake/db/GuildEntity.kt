/*
 * PolyhedralBot - A Discord bot for the Polyhedral Development discord server
 * Copyright (c) 2021-2021 solonovamax <solonovamax@12oclockpoint.com>
 *
 * The file GuildEntity.kt is part of PolyhedralBot
 * Last modified on 09-10-2021 11:27 p.m.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * POLYHEDRALBOT IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.github.septicake.db

import io.github.septicake.db.GuildTable.long
import io.github.septicake.db.GuildTable.text
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.SizedIterable

object GuildTable : LongIdTable("GUILD_DATA") {
    val name = text("name")
    val polls = long("polls")
    val pokemonPosition = long("pokemon_position")
    val smashes = long("smashes")
    val passes = long("passes")
    val pollsChannel = long("polls_channel")
}

class GuildEntity(id: EntityID<Long>) : LongEntity(id) {
    var name: String by GuildTable.name
    var polls: Long by GuildTable.polls
    var pokemonPosition: Long by GuildTable.pokemonPosition
    var smashes: Long by GuildTable.smashes
    var passes: Long by GuildTable.passes
    var pollsChannel: Long by GuildTable.pollsChannel

    companion object : LongEntityClass<GuildEntity>(GuildTable)
}
