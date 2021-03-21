package ch.sebpiller.epg.producer

import ch.sebpiller.epg.EpgInfo
import ch.sebpiller.epg.Audience
import ch.sebpiller.epg.Channel
import java.time.ZonedDateTime
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.time.format.DateTimeFormatter
import java.util.function.Consumer

@Deprecated("use {@link SaxXmlTvEpgProducer} instead (less memory footprint).")
class XmlTvEpgProducer {
    fun createRootElement(doc: Document): Element {
        val root = doc.createElement("tv")
        root.setAttribute("source-info-url", "https://www.rts.ch/programme-tv/")
        root.setAttribute("source-data-url", "https://www.rts.ch/programme-tv/")
        root.setAttribute("generator-info-name", "spidy-tv-guide")
        root.setAttribute("generator-info-url", "https://home.sebpiller.ch;me@sebpiller.ch")
        doc.appendChild(root)
        return root
    }

    fun dumpChannels(doc: Document, root: Element) {
        for (channel in Channel.values()) {
            /*<channel id="C2.api.telerama.fr">
                <display-name>13eme RUE</display-name>
                <icon src="https://television.telerama.fr/sites/tr_master/files/sheet_media/tv/500x500/2.png" />
              </channel>*/
            val channelEl = doc.createElement("channel")
            channelEl.setAttribute("id", channel.name)
            val dn = doc.createElement("display-name")
            dn.textContent = channel.name
            channelEl.appendChild(dn)
            val icon = doc.createElement("icon")
            icon.textContent = channel.name
            channelEl.appendChild(icon)
            root.appendChild(channelEl)
        }
    }

    fun dumpInfo(doc: Document, root: Element, info: EpgInfo) {
        val programme = doc.createElement("programme")
        programme.setAttribute("channel", info.channel!!.name)
        programme.setAttribute("start", formatLocalDateTimeForEpg(info.timeStart))
        programme.setAttribute("stop", formatLocalDateTimeForEpg(info.timeStop))
        root.appendChild(programme)
        val title = doc.createElement("title")
        title.textContent = info.title
        programme.appendChild(title)
        if (info.category != null) {
            val category = doc.createElement("category")
            category.textContent = info.category
            programme.appendChild(category)
        }
        val credits = doc.createElement("credits")
        var appendCredits = false
        if (info.directors != null) {
            appendCredits = true
            info.directors!!.forEach(Consumer { d: String? ->
                val x = doc.createElement("director")
                x.textContent = d
                credits.appendChild(x)
            })
        }
        if (info.actors != null) {
            appendCredits = true
            info.actors!!.forEach(Consumer { d: String? ->
                val x = doc.createElement("actor")
                x.textContent = d
                credits.appendChild(x)
            })
        }
        if (appendCredits) {
            programme.appendChild(credits)
        }
        if (info.audience != null) {
            val rating = doc.createElement("rating")
            rating.setAttribute("system", "FSK")
            val value = doc.createElement("value")
            when (info.audience) {
                Audience.SEVEN -> value.textContent = "7"
                Audience.TEN -> value.textContent = "10"
                Audience.TWELVE -> value.textContent = "12"
                Audience.SIXTEEN -> value.textContent = "16"
                Audience.EIGHTEEN -> value.textContent = "18"
                Audience.ALL -> value.textContent = "All"
                else -> value.textContent = "Unknown"
            }
            rating.appendChild(value)
            programme.appendChild(rating)
        }
        if (info.subtitle != null) {
            val subtitle = doc.createElement("sub-title")
            subtitle.textContent = info.subtitle
            programme.appendChild(subtitle)
        }
        if (info.description != null) {
            val desc = doc.createElement("desc")
            desc.setAttribute("lang", "fr")
            desc.textContent = info.description
            programme.appendChild(desc)
        }

        // <episode-num system="xmltv_ns">4.18.</episode-num>
        if (info.episode != null) {
            val episode = doc.createElement("episode-num")
            episode.setAttribute("system", "onscreen")
            episode.textContent = info.episode
            programme.appendChild(episode)
        }
    }

    private fun formatLocalDateTimeForEpg(ldt: ZonedDateTime?): String? {
        // "20210107000500+0100"
        return if (ldt == null) null else DATETIME.format(ldt)
    }

    companion object {
        val DATETIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmssZ")
    }
}