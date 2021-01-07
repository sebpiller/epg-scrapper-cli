package ch.sebpiller.epgscrapper;

public interface EpgScrapper {
    void scrapeEpg(EpgInfoScrappedListener listener);
}
