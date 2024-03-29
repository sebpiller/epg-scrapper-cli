package ch.sebpiller.epg

import org.apache.commons.lang3.StringUtils
import java.util.*

/**
 * Lists the supported TV channels with their known aliases.
 */
enum class Channel(vararg aliases: String) {
    RTS1("tsr1", "rts un"),
    RTS2("tsr2", "rts deux"),
    TF1,
    TF1_SERIES_FILMS("tf1 séries films"),
    FRANCE2("fr2"),
    FRANCE3("fr3"),
    FRANCE4("fr4"),
    FRANCE5("fr5"),
    TV5MONDE,
    ARTE,
    M6,
    W9,
    SIXTER("6ter", "sister"),
    TMC,
    EURONEWS,
    C8,
    RTL9,
    TFX,
    NRJ12,
    AB1("ab 1"),
    CSTAR,
    CHERIE25,
    NUMERO23,
    CANALPLUS("canal+"),
    RMCDECOUVERTE,
    EUROSPORT,
    LEQUIPE21,
    PLANETEPLUS,
    GULLI,
    ANIMAUX,
    MTV,
    TV8MONTBLANC,
    SRF1,
    SRF2,
    ZDF,
    ARD,
    RTLTELEVISION,
    RSI1,
    RSI2,
    RAI_UNO("rai1"),
    RAI_DUE("rai2"),
    RTP_INTERNATIONAL,
    TVE,
    BBC_WORLD,
    CNN,
    BFM_TV("bfmtv"),
    CNEWS,
    LCP,

    // OCS
    OCS_MAX("ocs max"),
    OCS_CHOC("ocs choc"),
    OCS_CITY("ocs city"),
    OCS_GEANTS("ocs geants"),

    // ProgrammeTV net
    MANGAS,
    THIRTEENTH_STREET("13eme rue", "13e rue", "13th rue"),
    SYFY,
    AB3("ab 3"),
    NATGEO("national geographic"),
    NATGEOWILD("natgeo wild", "nat geo wild"),
    LCI("la chaine info", "la chaîne info", "LCI - La Chaîne Info"),
    DISNEY_CHANNEL("disney channel"),
    DISNEY_JUNIOR("disney junior"),
    VOYAGE,
    USHUAIA("ushuaia tv", "Ushuaïa TV"),
    PARAMOUNT("paramount channel"),
    FRANCE24("France 24"),
    EQUIDIA,
    CHASSE_PECHE("chasse et pêche", "chasse & pêche", "chasse et peche", "chasse & peche"),

    // PlayTV.fr
    ROUGETV("rouge-tv", "rouge tv")

    ;

    private val aliases: Array<String> = arrayOf(*aliases)

    companion object {
        /**
         * Fetch a channel by its Java name or one of its known aliases. Returns null when not found.
         */
        @JvmStatic
        fun valueOfAliases(alias: String): Channel? {
            return try {
                valueOf(alias.toUpperCase())
            } catch (iae: IllegalArgumentException) {
                val findAny = Arrays
                        .stream(values())
                        .filter { channel -> StringUtils.equalsAnyIgnoreCase(alias, *channel.aliases) }
                        .findAny()

                if (findAny.isPresent) {
                    findAny.get()
                } else {
                    null
                }
            }
        }
    }
}