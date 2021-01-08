package ch.sebpiller.epg.scrapper;

import ch.sebpiller.epg.EpgInfo;

@FunctionalInterface
public interface EpgInfoScrappedListener {
    /**
     * @return true to continue the scrape, false to stop the process.
     */
    boolean epgInfoScrapped(EpgInfo scrapped);
}
