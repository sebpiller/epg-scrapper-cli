package ch.sebpiller.epg.scrapper.teleboy;


import ch.sebpiller.epg.Channel;
import ch.sebpiller.epg.EpgInfo;
import ch.sebpiller.epg.scrapper.EpgInfoScrappedListener;
import ch.sebpiller.epg.scrapper.EpgScrapper;

import java.util.function.Predicate;

// eg. https://www.teleboy.ch/fr/programme/chaines/365/rougetv?date=2021-01-10
public class TeleboyEpgScrapper implements EpgScrapper {
    @Override
    public void scrapeEpg(Predicate<Channel> filterChannel, Predicate<EpgInfo> scrapeDetails, EpgInfoScrappedListener listener) {

    }
}
