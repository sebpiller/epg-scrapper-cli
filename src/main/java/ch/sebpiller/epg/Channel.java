package ch.sebpiller.epg;

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * Lists the supported TV channels with their known aliases.
 */
public enum Channel {
    RTS1("tsr1", "rts un"),
    RTS2("tsr2", "rts deux"),
    TF1,
    TF1_SERIES_FILMS,
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
    AB1,
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
    ;

    private final String[] aliases;

    Channel(String... aliases) {
        this.aliases = aliases;
    }

    /**
     * Fetch a channel by its Java name or one of its known aliases. Returns null when not found.
     */
    public static Channel valueOfAliases(String alias) {
        Objects.requireNonNull(alias);

        try {
            return valueOf(alias.toUpperCase());
        } catch (IllegalArgumentException iae) {
            for (Channel c : values()) {
                if (StringUtils.equalsAnyIgnoreCase(alias, c.aliases)) {
                    return c;
                }
            }
        }

        return null;
    }
}
