package ch.sebpiller.epg.producer;

import ch.sebpiller.epg.Channel;
import ch.sebpiller.epg.EpgInfo;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class XmlTvEpgProducer {
    public static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmssZ");

    public Element createRootElement(Document doc) {
        Element root = doc.createElement("tv");
        root.setAttribute("source-info-url", "https://www.rts.ch/programme-tv/");
        root.setAttribute("source-data-url", "https://www.rts.ch/programme-tv/");
        root.setAttribute("generator-info-name", "spidy-tv-guide");
        root.setAttribute("generator-info-url", "https://home.sebpiller.ch;me@sebpiller.ch");
        doc.appendChild(root);
        return root;
    }

    public void dumpChannels(Document doc, Element root) {
        for (Channel c : Channel.values()) {
              /*<channel id="C2.api.telerama.fr">
                <display-name>13eme RUE</display-name>
                <icon src="https://television.telerama.fr/sites/tr_master/files/sheet_media/tv/500x500/2.png" />
              </channel>*/
            Element channel = doc.createElement("channel");
            channel.setAttribute("id", c.name());

            Element dn = doc.createElement("display-name");
            dn.setTextContent(c.name());
            channel.appendChild(dn);

            Element icon = doc.createElement("icon");
            icon.setTextContent(c.name());
            channel.appendChild(icon);

            root.appendChild(channel);
        }
    }

    public void dumpInfo(Document doc, Element root, EpgInfo info) {
        Element programme = doc.createElement("programme");
        programme.setAttribute("channel", info.getChannel().name());
        programme.setAttribute("start", formatLocalDateTimeForEpg(info.getTimeStart()));
        programme.setAttribute("stop", formatLocalDateTimeForEpg(info.getTimeStop()));
        root.appendChild(programme);

        Element title = doc.createElement("title");
        title.setTextContent(info.getTitle());
        programme.appendChild(title);

        if (info.getCategory() != null) {
            Element category = doc.createElement("category");
            category.setTextContent(info.getCategory());
            programme.appendChild(category);
        }

        Element credits = doc.createElement("credits");
        boolean appendCredits = false;

        if (info.getDirectors() != null) {
            appendCredits = true;
            info.getDirectors().forEach(d -> {
                Element x = doc.createElement("director");
                x.setTextContent(d);
                credits.appendChild(x);
            });
        }

        if (info.getActors() != null) {
            appendCredits = true;
            info.getActors().forEach(d -> {
                Element x = doc.createElement("actor");
                x.setTextContent(d);
                credits.appendChild(x);
            });
        }

        if (appendCredits) {
            programme.appendChild(credits);
        }

        if (info.getAudience() != null) {
            Element rating = doc.createElement("rating");
            rating.setAttribute("system", "FSK");

            Element value = doc.createElement("value");

            switch (info.getAudience()) {
                case SEVEN:
                    value.setTextContent("7");
                    break;
                case TEN:
                    value.setTextContent("10");
                    break;
                case TWELVE:
                    value.setTextContent("12");
                    break;
                case SIXTEEN:
                    value.setTextContent("16");
                    break;
                case EIGHTEEN:
                    value.setTextContent("18");
                    break;
                case ALL:
                    value.setTextContent("All");
                    break;
                default:
                    value.setTextContent("Unknown");
            }

            rating.appendChild(value);
            programme.appendChild(rating);
        }

        if (info.getSubtitle() != null) {
            Element subtitle = doc.createElement("sub-title");
            subtitle.setTextContent(info.getSubtitle());
            programme.appendChild(subtitle);
        }

        if (info.getDescription() != null) {
            Element desc = doc.createElement("desc");
            desc.setAttribute("lang", "fr");
            desc.setTextContent(info.getDescription());
            programme.appendChild(desc);
        }

        // <episode-num system="xmltv_ns">4.18.</episode-num>
        if (info.getEpisode() != null) {
            Element episode = doc.createElement("episode-num");
            episode.setAttribute("system", "onscreen");
            episode.setTextContent(info.getEpisode());
            programme.appendChild(episode);
        }
    }

    private String formatLocalDateTimeForEpg(ZonedDateTime ldt) {
        // "20210107000500+0100"
        return ldt == null ? null : DATETIME.format(ldt);
    }
}
