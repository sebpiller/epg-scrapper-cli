package ch.sebpiller.epg.producer

import ch.sebpiller.epg.Audience
import ch.sebpiller.epg.Channel
import ch.sebpiller.epg.EpgInfo
import ch.sebpiller.epg.scrapper.EpgInfoScrappedListener
import com.sun.xml.txw2.output.IndentingXMLStreamWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.function.Consumer
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamException
import javax.xml.stream.XMLStreamWriter

class SaxXmlTvEpgProducer(os: OutputStream?) : EpgInfoScrappedListener, AutoCloseable {
    private val out: XMLStreamWriter

    init {
        var o = XMLOutputFactory.newInstance().createXMLStreamWriter(OutputStreamWriter(os, StandardCharsets.UTF_8))
        if (indent) {
            o = IndentingXMLStreamWriter(o)
        }

        out = o
        with(out) {
            writeStartDocument()
            writeStartElement("tv")
            writeAttribute("source-info-url", "https://www.rts.ch/programme-tv/")
            writeAttribute("source-data-url", "https://www.rts.ch/programme-tv/")
            writeAttribute("generator-info-name", "spidy-tv-guide")
            writeAttribute("generator-info-url", "https://github.com/sebpiller/sebpiller")
        }

        writeChannelList()
    }

    @Throws(XMLStreamException::class)
    private fun writeChannelList() {
        for (channel in Channel.values()) {
            out.writeStartElement("channel")
            out.writeAttribute("id", channel.name)

            out.writeStartElement("display-name")
            out.writeCharacters(channel.name)
            out.writeEndElement()

            out.writeStartElement("icon")
            out.writeCharacters(channel.name)
            out.writeEndElement()

            out.writeEndElement()
        }
    }

    @Synchronized
    override fun epgInfoScrapped(info: EpgInfo): Boolean {
        try {
            doWriteInfo(info)
        } catch (e: XMLStreamException) {
            throw RuntimeException(e)
        }
        return true
    }

    @Throws(XMLStreamException::class)
    private fun doWriteInfo(info: EpgInfo) {
        out.writeStartElement("programme")
        out.writeAttribute("channel", info.channel?.name)
        out.writeAttribute("start", formatZonedDateTimeForEpg(info.timeStart))
        out.writeAttribute("stop", formatZonedDateTimeForEpg(info.timeStop))
        writeTag("title", info.title)
        writeTag("sub-title", info.subtitle)
        writeTag("category", info.category)
        writeDescriptionInfo(info)
        writeCreditsInfo(info)
        writeAudienceInfo(info)
        writeEpisodeInfo(info)
        out.writeEndElement()
    }

    @Throws(XMLStreamException::class)
    private fun writeCreditsInfo(info: EpgInfo) {
        out.writeStartElement("credits")

        info.directors?.forEach(Consumer { d: String ->
            try {
                out.writeStartElement("director")
                out.writeCharacters(d)
                out.writeEndElement()
            } catch (e: XMLStreamException) {
                throw RuntimeException(e)
            }
        })

        info.actors?.forEach(Consumer { d: String ->
            try {
                out.writeStartElement("actor")
                out.writeCharacters(d)
                out.writeEndElement()
            } catch (e: XMLStreamException) {
                throw RuntimeException(e)
            }
        })

        out.writeEndElement()
    }

    @Throws(XMLStreamException::class)
    private fun writeAudienceInfo(info: EpgInfo) {
        if (info.audience != null) {
            out.writeStartElement("rating")
            out.writeAttribute("system", "FSK")
            out.writeStartElement("value")
            when (info.audience) {
                Audience.SEVEN -> out.writeCharacters("7")
                Audience.TEN -> out.writeCharacters("10")
                Audience.TWELVE -> out.writeCharacters("12")
                Audience.SIXTEEN -> out.writeCharacters("16")
                Audience.EIGHTEEN -> out.writeCharacters("18")
                Audience.ALL -> out.writeCharacters("All")
                else -> out.writeCharacters("Unknown")
            }
            out.writeEndElement()
            out.writeEndElement()
        }
    }

    @Throws(XMLStreamException::class)
    private fun writeTag(tag: String, text: String?) {
        if (text != null) {
            out.writeStartElement(tag)
            out.writeCharacters(text ?: "")
            out.writeEndElement()
        }
    }

    @Throws(XMLStreamException::class)
    private fun writeDescriptionInfo(info: EpgInfo) {
        if (info.description != null) {
            out.writeStartElement("desc")
            out.writeAttribute("lang", "fr")
            out.writeCharacters(info.description)
            out.writeEndElement()
        }
    }

    @Throws(XMLStreamException::class)
    private fun writeEpisodeInfo(info: EpgInfo) {
        // <episode-num system="xmltv_ns">4.18.</episode-num>
        if (info.episode != null) {
            out.writeStartElement("episode-num")
            out.writeAttribute("system", "onscreen")
            out.writeCharacters(info.episode)
            out.writeEndElement()
        }
    }

    private fun formatZonedDateTimeForEpg(ldt: ZonedDateTime?): String? {
        // "20210107000500+0100"
        return if (ldt == null) null else DATETIME.format(ldt)
    }

    @Throws(Exception::class)
    override fun close() {
        out.writeEndElement()
        out.writeEndDocument()
        out.close()
    }

    companion object {
        val DATETIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmssZ")
        private const val indent = false
    }
}