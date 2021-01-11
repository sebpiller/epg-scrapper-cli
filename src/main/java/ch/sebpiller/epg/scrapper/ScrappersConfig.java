package ch.sebpiller.epg.scrapper;

import ch.sebpiller.epg.Channel;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static ch.sebpiller.epg.Channel.*;

/**
 * Contains the list of channels provided by each type of scrappers.
 */
public class ScrappersConfig {
    private ScrappersConfig() {}

    public static final Set<Channel> RTS_CHANNELS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            RTS1,
            RTS2,
            TF1,
            TF1_SERIES_FILMS,
            FRANCE2,
            FRANCE3,
            FRANCE4,
            FRANCE5,
            TV5MONDE,
            ARTE,
            M6,
            W9,
            SIXTER,
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
            CANALPLUS,
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
            RAI_UNO,
            RAI_DUE,
            RTP_INTERNATIONAL,
            TVE,
            BBC_WORLD,
            CNN,
            BFM_TV,
            CNEWS,
            LCP
    )));

    public static final Set<Channel> OCS_CHANNELS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            OCS_MAX,
            OCS_CHOC,
            OCS_CITY,
            OCS_GEANTS
    )));
}
