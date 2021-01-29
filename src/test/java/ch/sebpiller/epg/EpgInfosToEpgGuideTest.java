package ch.sebpiller.epg;

import ch.sebpiller.epg.producer.SaxXmlTvEpgProducer;
import ch.sebpiller.epg.producer.XmlTvEpgProducer;
import ch.sebpiller.epg.scrapper.EpgScrapper;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

public class EpgInfosToEpgGuideTest {
    private static final Logger LOG = LoggerFactory.getLogger(EpgInfosToEpgGuideTest.class);

    @Test
    public void exportSerializedObjectsToEpgGuide() throws Exception {
        ObjectInputStream ois = new ObjectInputStream(getClass().getResourceAsStream("/all_epg_info.data"));
        List<EpgInfo> infos = (List<EpgInfo>) ois.readObject();
        ois.close();

        ois = new ObjectInputStream(getClass().getResourceAsStream("/all_ocs_epg_info.data"));
        List<EpgInfo> infos2 = (List<EpgInfo>) ois.readObject();
        ois.close();

        infos.addAll(infos2);

        ois = new ObjectInputStream(getClass().getResourceAsStream("/all_programmetvnet_epg_info.data"));
        List<EpgInfo> infos3 = (List<EpgInfo>) ois.readObject();
        ois.close();

        infos.addAll(infos3);

        ois = new ObjectInputStream(getClass().getResourceAsStream("/all_playtvfr_epg_info.data"));
        List<EpgInfo> infos4 = (List<EpgInfo>) ois.readObject();
        ois.close();

        infos.addAll(infos4);

        exportUsingSax(infos);

        /*Document doc = buildDocumentFromEpgInfo(infos);
        Transformer t = TransformerFactory.newInstance().newTransformer();
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        t.transform(new DOMSource(doc), new StreamResult(System.out));
        t.transform(new DOMSource(doc), new StreamResult(new FileOutputStream("/home/spiller/tv/config/data/epg.xml")));*/
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

        XmlTvEpgProducer epgInfoDumper = new XmlTvEpgProducer();
        epgInfoDumper.dumpChannels(doc, root);
        infos.forEach(epgInfo -> epgInfoDumper.dumpInfo(doc, root, epgInfo));

        return doc;
    }

    private int i;

    private void exportUsingSax(List<EpgInfo> infos) {
        i = 0;

        // Fake scrappers using a List<EpgInfo> as source
        List<EpgScrapper> scrappers = Arrays.asList(
                (filterChannel, scrapeDetails, listener) -> {
                    for (EpgInfo info : infos) {
                        listener.epgInfoScrapped(info);
                    }
                }
        );

        long start = System.currentTimeMillis();

        try (OutputStream os = new FileOutputStream("target/pouet.xml");
             SaxXmlTvEpgProducer xmlProducer = new SaxXmlTvEpgProducer(os)) {
            scrappers.parallelStream().forEach(epgScrapper -> epgScrapper.scrapeEpg(x -> true, x -> true, x -> {
                this.i++;
                if (this.i % 100 == 0) {
                    LOG.info("scrapped {} infos in {}s", this.i, (System.currentTimeMillis() - start) / 1_000);
                }

                return xmlProducer.epgInfoScrapped(x);
            }));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
