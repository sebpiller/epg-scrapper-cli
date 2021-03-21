package ch.sebpiller.epg.scrapper.ocs

import ch.sebpiller.epg.Channel
import ch.sebpiller.epg.EpgInfo
import ch.sebpiller.epg.scrapper.EpgInfoScrappedListener
import ch.sebpiller.epg.scrapper.EpgScrapper
import ch.sebpiller.epg.scrapper.ScrappingException
import org.apache.commons.lang3.StringUtils
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.slf4j.LoggerFactory
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.function.Consumer
import java.util.function.Predicate

/**
 * Scrape information using https://www.ocs.fr/grille-tv.
 */
class OcsEpgScrapper : EpgScrapper {
    private val lastEpgs = Collections.synchronizedMap(HashMap<Channel, EpgInfo>())
    override fun scrapeEpg(filterChannel: Predicate<Channel>, listener: EpgInfoScrappedListener) {
        lastEpgs.clear()
        var dayFetch = LocalDate.now()
        var doc: Document
        var hasMoreData: Boolean
        do {
            try {
                doc = Jsoup.connect(ROOT_URL + "date=" + DAYSTR_FORMAT.format(dayFetch)).get()
                hasMoreData = StringUtils.isNotBlank(doc.getElementById("dateInitEpg").attr("value"))
            } catch (e: IOException) {
                throw ScrappingException(e)
            }
            if (LOG.isTraceEnabled) LOG.trace("============================= {}", DAYSTR_FORMAT.format(dayFetch))
            if (hasMoreData) {
                scrapeDocument(doc, filterChannel, listener)
                dayFetch = dayFetch.plusDays(1) // tomorrow
            }
        } while (hasMoreData)
        LOG.info("scrapper {} has completed", this)
    }

    fun scrapeDocument(doc: Document, filterChannel: Predicate<Channel>, listener: EpgInfoScrappedListener) {
        // <input type="hidden" id="dateInitEpg" value="20210116"/>
        val attr = doc.getElementById("dateInitEpg").attr("value")
        val date = LocalDate.from(DAYSTR_FORMAT.parse(attr))
        val div = doc.selectFirst(".grille-chaines")
        val channelsIds = arrayOfNulls<Channel>(div.childrenSize())
        var i = 0
        for (child in div.children()) {
            channelsIds[i++] = Channel.valueOfAliases(child.selectFirst("h2.pageTitle").text())
        }
        val grilleProgrammes = doc.getElementById("grille-programmes")
        for (j in channelsIds.indices) {
            val c = channelsIds[j]
            if (!filterChannel.test(c!!)) {
                continue
            }
            val li = grilleProgrammes.child(j)
            var lastTime: LocalTime? = null
            var ld = date
            for (a in li.getElementsByClass("linkPageData")) {
                val info = EpgInfo(c)
                val from = a.selectFirst(".program-horaire").text()
                val split = from.split(":".toRegex()).toTypedArray()
                val time = LocalTime.of(split[0].toInt(), split[1].toInt())
                if (lastTime != null && time.isBefore(lastTime)) {
                    ld = ld.plusDays(1)
                }
                lastTime = time
                val zdt = ZonedDateTime.of(ld, time, ZoneId.of("Europe/Zurich"))
                info.timeStart = zdt
                info.title = a.selectFirst(".program-title").text()
                info.category = resolveCategory(a.selectFirst(".program-subtitle span").text())


                // data-detaillink="https://www.ocs.fr/programme/LAPROIEXXXXW0051426">
                val detailUri = a.attr("data-detaillink")
                if ("#" != detailUri) {
                    parseDetails(detailUri, info)
                }
                lastEpgs.computeIfPresent(c) { channel: Channel?, epgInfo: EpgInfo ->
                    epgInfo.timeStop = zdt
                    if (!listener.epgInfoScrapped(epgInfo)) {
                        // FIXME support 'abort the loop'
                    }
                    epgInfo
                }
                lastEpgs[c] = info
            }
        }
    }

    private fun parseDetails(uri: String, info: EpgInfo) {
        try {
            parseDetails(Jsoup.connect(uri).get(), info)
        } catch (e: HttpStatusException) {
            // ignore 403 errors
            if (e.statusCode == 403) {
                return
            }
            if (e.statusCode == 404) {
                LOG.warn("not found {}", uri, e)
                return
            }
            throw ScrappingException(e)
        } catch (e: IOException) {
            throw ScrappingException(e)
        }
    }

    fun parseDetails(doc: Document, info: EpgInfo) {
        val dis = doc.selectFirst(".presentation.display p")
        info.description = dis?.text()
        val actors: MutableList<String> = ArrayList()
        doc.select("div.infos div.casting ul li").forEach(Consumer { element: Element ->
            val text = element.text()
            if (StringUtils.isNotBlank(text)) {
                if (text.indexOf('(') >= 0) {
                    actors.add(text.substring(0, text.indexOf('(')).trim { it <= ' ' })
                } else {
                    actors.add(text)
                }
            }
        })
        info.actors = actors
    }

    private fun resolveCategory(text: String): String {
        // FIXME improve mapping of category between rts and xmltv
        return when (text.toLowerCase()) {
            "film", "téléfilm", "film tv" -> "Movie / Drama"
            "x" -> "News / Current affairs"
            "série", "court-métrage" -> "Show / Games"
            "sport", "sports" -> "Sports"
            "xxxxx" -> "Children's / Youth"
            "xxx" -> "Music"
            "yyx" -> "Art / Culture"
            "h" -> "Social / Political issues / Economics"
            "documentaire" -> "Education / Science / Factual topics"
            "magazine" -> "Leisure hobbies"
            "k" -> "Special characteristics"
            else -> text
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(OcsEpgScrapper::class.java)

        /**
         * The format to insert day to fetch in the web url.
         */
        private val DAYSTR_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd")

        // 20210116
        private const val ROOT_URL = "https://grille.ocs.fr/white?embed=&"
    }
}