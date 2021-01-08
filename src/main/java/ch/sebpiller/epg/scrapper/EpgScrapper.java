package ch.sebpiller.epg.scrapper;

import ch.sebpiller.epg.Channel;
import ch.sebpiller.epg.EpgInfo;

import java.util.function.Predicate;

public interface EpgScrapper {
    void scrapeEpg(Predicate<Channel> filterChannel, Predicate<EpgInfo> scrapeDetails, EpgInfoScrappedListener listener);
}
