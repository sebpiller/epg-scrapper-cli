package ch.sebpiller.epg

import java.time.ZonedDateTime

/**
 * Models information about an EPG entry. An EPG is constituted by a lot of objects of this type.
 */
data class EpgInfo(val c: Channel? = null) {
    var channel: Channel? = null
    var title: String? = null

    /**
     * Normalized episode id (if any). Follow the format S&lt;xx>E&lt;yy> (eg. S01E04) or a plain number prefixed
     * with E (eg. E1234).
     */
    var episode: String? = null
    var subtitle: String? = null
    var description: String? = null
    var timeStart: ZonedDateTime? = null
    var timeStop: ZonedDateTime? = null
    var producers: List<String>? = null
    var directors: List<String>? = null
    var actors: List<String>? = null
    var scenarists: List<String>? = null
    var audience: Audience? = null
    var category: String? = null

    init {
        this.channel = c
    }
}