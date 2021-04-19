package ch.sebpiller.epg.producer;

import ch.sebpiller.epg.Audience;
import ch.sebpiller.epg.Channel;
import ch.sebpiller.epg.EpgInfo;
import ch.sebpiller.epg.scrapper.EpgScrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@DisplayName("EPG Info Producer test")
class EpgInfosToEpgGuideTest {
    private static final Logger LOG = LoggerFactory.getLogger(EpgInfosToEpgGuideTest.class);

    private List<EpgInfo> infos;

    @BeforeEach
    void exportSerializedObjectsToEpgGuide() throws Exception {
        infos = new ArrayList<>();

        EpgInfo info = new EpgInfo(Channel.RTS1);
        info.setCategory("Youth");
        info.setTitle("The title");
        info.setSubtitle("The subtitle");
        info.setTimeStart(ZonedDateTime.now());
        info.setTimeStop(ZonedDateTime.now().plus(2, ChronoUnit.HOURS));
        info.setDescription("The description");
        info.setAudience(Audience.ALL);
        info.setActors(Arrays.asList("Tom Hanks", "Jennifer Lopez", "Jésus-Christ"));
        info.setDirectors(Arrays.asList("Quentin Tarantino"));
        info.setProducers(Arrays.asList("Bill Cosby"));
        info.setScenarists(Arrays.asList("John Malkovich"));
        info.setEpisode("S01E04");

        infos.add(info);
    }

    private int i;

    @Test
    void exportUsingDom() throws Exception {
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = db.newDocument();

        // <tv source-info-url="https://api.telerama.fr" source-data-url="https://api.telerama.fr" generator-info-name="XMLTV" generator-info-url="http://mythtv-fr.org/">
        Element root = doc.createElement("tv");
        root.setAttribute("source-info-url", "https://www.rts.ch/programme-tv/");
        root.setAttribute("source-data-url", "https://www.rts.ch/programme-tv/");
        root.setAttribute("generator-info-name", "spidy-tv-guide");
        root.setAttribute("generator-info-url", "https://home.sebpiller.ch;me@sebpiller.ch");
        doc.appendChild(root);

        XmlTvEpgProducer producer = new XmlTvEpgProducer();
        producer.dumpChannels(doc, root);
        infos.forEach(epgInfo -> producer.dumpInfo(doc, root, epgInfo));

        Transformer t = TransformerFactory.newInstance().newTransformer();
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        t.transform(new DOMSource(doc), new StreamResult(System.out));
    }

    @Test
    void exportUsingSax() throws Exception {
        i = 0;

        // Fake scrappers using a List<EpgInfo> as source
        List<EpgScrapper> scrappers = Arrays.asList(
                (filterChannel, listener) -> {
                    for (EpgInfo info : infos) {
                        listener.epgInfoScrapped(info);
                    }
                }
        );

        long start = System.currentTimeMillis();

        try (OutputStream os = new FileOutputStream("target/pouet.xml");
             SaxXmlTvEpgProducer xmlProducer = new SaxXmlTvEpgProducer(os)) {
            scrappers.parallelStream().forEach(epgScrapper -> epgScrapper.scrapeEpg(x -> true, x -> {
                this.i++;
                if (this.i % 100 == 0) {
                    LOG.info("scrapped {} infos in {}s", this.i, (System.currentTimeMillis() - start) / 1_000);
                }

                return xmlProducer.epgInfoScrapped(x);
            }));
        }
    }

}
