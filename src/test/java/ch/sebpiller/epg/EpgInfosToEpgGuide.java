package ch.sebpiller.epg;

import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Ignore
public class EpgInfosToEpgGuide {

    @Test
    public void test() throws Exception {
        ObjectInputStream ois = new ObjectInputStream(getClass().getResourceAsStream("/all_epg_info.data"));
        List<EpgInfo> infos = (List<EpgInfo>) ois.readObject();

        System.out.println(infos.size());

        Document doc = buildDocumentFromEpgInfo(infos);
        Transformer t = TransformerFactory.newInstance().newTransformer();
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        t.transform(new DOMSource(doc), new StreamResult(System.out));
        t.transform(new DOMSource(doc), new StreamResult(new FileOutputStream("/home/spiller//tv/config/data/epg.xml")));
    }

    private Document buildDocumentFromEpgInfo(List<EpgInfo> infos) throws ParserConfigurationException {
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = db.newDocument();

        // <tv source-info-url="https://api.telerama.fr" source-data-url="https://api.telerama.fr" generator-info-name="XMLTV" generator-info-url="http://mythtv-fr.org/">
        Element root = doc.createElement("tv");
        root.setAttribute("source-info-url", "https://www.rts.ch/programme-tv/");
        root.setAttribute("source-data-url", "https://www.rts.ch/programme-tv/");
        root.setAttribute("generator-info-name", "spidy-tv-guide");
        root.setAttribute("generator-info-url", "https://home.sebpiller.ch;me@sebpiller.ch");
        doc.appendChild(root);

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
            dn.setTextContent(c.name());
            channel.appendChild(dn);

            root.appendChild(channel);
        }

        /*
          <programme start="20210107000500 +0100" stop="20210107005500 +0100" channel="C2.api.telerama.fr">
            <title>New York Unité Spéciale</title>
            <sub-title>Le contrat du silence</sub-title>
            <desc lang="fr">Saison:5 - Episode:19 - Un homme, soupçonné d'être un pédophile, est entre les mains de la justice. Malheureusement, les parents de sa victime refusent de collaborer : ils craignent que l'interrogatoire de leur fils ne le fasse basculer dans la folie. Les policiers tentent de les convaincre de la nécessité d'une telle démarche. Mais ils découvrent que les parents ont touché un million de dollars lors d'un accord entre les avocats de la victime et ceux du présumé coupable. Par ailleurs, un autre enfant témoigne contre l'accusé. Mais il semble bien que ses accusations aient été portées dans l'unique but de toucher de l'argent...</desc>
            <category lang="fr">série policière</category>
            <length units="minutes">50</length>
            <episode-num system="xmltv_ns">4.18.</episode-num>
            <audio>
              <stereo>bilingual</stereo>
            </audio>
            <previously-shown />
            <rating system="CSA">
              <value>-12</value>
              <icon src="http://upload.wikimedia.org/wikipedia/commons/thumb/b/b5/Moins12.svg/200px-Moins12.svg.png" />
            </rating>
          </programme>
         */

        int i = 0;
        for (EpgInfo info : infos) {
            // if (++i > 100) break;
            Element programme = doc.createElement("programme");
            programme.setAttribute("channel", info.getChannel().name());
            programme.setAttribute("start", formatLocalDateTimeForEpg(info.getTimeStart()));
            programme.setAttribute("stop", formatLocalDateTimeForEpg(info.getTimeStop()));
            root.appendChild(programme);

            Element title = doc.createElement("title");
            title.setTextContent(info.getTitle());
            programme.appendChild(title);

            // <category lang="fr">série policière</category>
            if (info.getCategory() != null) {
                Element category = doc.createElement("category");
                category.setTextContent(info.getCategory());
                programme.appendChild(category);
            }

            Element credits = doc.createElement("credits");
            programme.appendChild(credits);

            if (info.getDirectors() != null) {
                info.getDirectors().forEach(d -> {
                    Element x = doc.createElement("director");
                    x.setTextContent(d);
                    credits.appendChild(x);
                });
            }

            if (info.getActors() != null) {
                info.getActors().forEach(d -> {
                    Element x = doc.createElement("actor");
                    x.setTextContent(d);
                    credits.appendChild(x);
                });
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

        return doc;
    }

    private String formatLocalDateTimeForEpg(ZonedDateTime ldt) {
        // "20210107000500+0100"
        return DateTimeFormatter.ofPattern("yyyyMMddHHmmssZ").format(ldt);
    }
}
