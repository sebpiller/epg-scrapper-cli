package ch.sebpiller.epg.scrapper;

import ch.sebpiller.epg.Channel;

import java.util.function.Predicate;

public interface EpgScrapper {
    void scrapeEpg(Predicate<Channel> filterChannel, EpgInfoScrappedListener listener);
}
