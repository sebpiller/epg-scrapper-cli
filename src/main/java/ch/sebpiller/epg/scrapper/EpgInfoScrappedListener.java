package ch.sebpiller.epg.scrapper;

import ch.sebpiller.epg.EpgInfo;

@FunctionalInterface
public interface EpgInfoScrappedListener {
    void epgInfoScrapped(EpgInfo scrapped);
}
