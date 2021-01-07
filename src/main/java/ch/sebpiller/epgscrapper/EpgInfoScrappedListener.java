package ch.sebpiller.epgscrapper;

@FunctionalInterface
public interface EpgInfoScrappedListener {
    void epgInfoScrapped(EpgInfo scrapped);
}
