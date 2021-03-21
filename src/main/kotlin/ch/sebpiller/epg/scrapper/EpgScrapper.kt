package ch.sebpiller.epg.scrapper

import ch.sebpiller.epg.Channel
import java.util.function.Predicate

interface EpgScrapper {
    fun scrapeEpg(filterChannel: Predicate<Channel>, listener: EpgInfoScrappedListener)
}