package ch.sebpiller.epg.producer;

import ch.sebpiller.epg.Channel;
import ch.sebpiller.epg.EpgInfo;
import ch.sebpiller.epg.scrapper.EpgInfoScrappedListener;
import com.sun.xml.txw2.output.IndentingXMLStreamWriter;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class SaxXmlTvEpgProducer implements EpgInfoScrappedListener, AutoCloseable {
    public static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmssZ");
    private final XMLStreamWriter out;

    private static final boolean indent = false;

    public SaxXmlTvEpgProducer(OutputStream os) throws XMLStreamException {
        out = indent ?
                new IndentingXMLStreamWriter(
                        XMLOutputFactory.newInstance().createXMLStreamWriter(
                                new OutputStreamWriter(os, StandardCharsets.UTF_8)
                        )
                ) :
                XMLOutputFactory.newInstance().createXMLStreamWriter(
                        new OutputStreamWriter(os, StandardCharsets.UTF_8)
                )
        ;

        out.writeStartDocument();
        out.writeStartElement("tv");
        out.writeAttribute("source-info-url", "https://www.rts.ch/programme-tv/");
        out.writeAttribute("source-data-url", "https://www.rts.ch/programme-tv/");
        out.writeAttribute("generator-info-name", "spidy-tv-guide");
        out.writeAttribute("generator-info-url", "https://home.sebpiller.ch;me@sebpiller.ch");

        for (Channel c : Channel.values()) {
            out.writeStartElement("channel");
            out.writeAttribute("id", c.name());

            out.writeStartElement("display-name");
            out.writeCharacters(c.name());
            out.writeEndElement();

            out.writeStartElement("icon");
            out.writeCharacters(c.name());
            out.writeEndElement();

            out.writeEndElement();
        }
    }

    @Override
    public synchronized boolean epgInfoScrapped(EpgInfo info) {
        try {
            doWriteInfo(info);
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    private void doWriteInfo(EpgInfo info) throws XMLStreamException {
        out.writeStartElement("programme");
        out.writeAttribute("channel", info.getChannel().name());
        out.writeAttribute("start", formatLocalDateTimeForEpg(info.getTimeStart()));
        out.writeAttribute("stop", formatLocalDateTimeForEpg(info.getTimeStop()));

        out.writeStartElement("title");
        out.writeCharacters(info.getTitle());
        out.writeEndElement();

        if (info.getCategory() != null) {
            out.writeStartElement("category");
            out.writeCharacters(info.getCategory());
            out.writeEndElement();
        }

        out.writeStartElement("credits");
        if (info.getDirectors() != null) {
            info.getDirectors().forEach(d -> {
                try {
                    out.writeStartElement("director");
                    out.writeCharacters(d);
                    out.writeEndElement();
                } catch (XMLStreamException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        if (info.getActors() != null) {
            info.getActors().forEach(d -> {
                try {
                    out.writeStartElement("actor");
                    out.writeCharacters(d);
                    out.writeEndElement();
                } catch (XMLStreamException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        out.writeEndElement();

        ///////////
        if (info.getAudience() != null) {
            out.writeStartElement("rating");
            out.writeAttribute("system", "FSK");

            out.writeStartElement("value");
            switch (info.getAudience()) {
                case SEVEN:
                    out.writeCharacters("7");
                    break;
                case TEN:
                    out.writeCharacters("10");
                    break;
                case TWELVE:
                    out.writeCharacters("12");
                    break;
                case SIXTEEN:
                    out.writeCharacters("16");
                    break;
                case EIGHTEEN:
                    out.writeCharacters("18");
                    break;
                case ALL:
                    out.writeCharacters("All");
                    break;
                default:
                    out.writeCharacters("Unknown");
            }

            out.writeEndElement();
            out.writeEndElement();
        }

        ///////////////
        if (info.getSubtitle() != null) {
            out.writeStartElement("sub-title");
            out.writeCharacters(info.getSubtitle());
            out.writeEndElement();
        }

        //////////////
        if (info.getDescription() != null) {
            out.writeStartElement("desc");
            out.writeAttribute("lang", "fr");
            out.writeCharacters(info.getDescription());
            out.writeEndElement();
        }

        /////////////
        // <episode-num system="xmltv_ns">4.18.</episode-num>
        if (info.getEpisode() != null) {
            out.writeStartElement("episode-num");
            out.writeAttribute("system", "onscreen");
            out.writeCharacters(info.getEpisode());
            out.writeEndElement();
        }

        out.writeEndElement();
    }

    private String formatLocalDateTimeForEpg(ZonedDateTime ldt) {
        // "20210107000500+0100"
        return ldt == null ? null : DATETIME.format(ldt);
    }

    @Override
    public void close() throws Exception {
        out.writeEndElement();
        out.writeEndDocument();
        out.close();
    }
}
