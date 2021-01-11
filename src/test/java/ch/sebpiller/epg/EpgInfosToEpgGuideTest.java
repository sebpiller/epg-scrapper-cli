package ch.sebpiller.epg;

import ch.sebpiller.epg.producer.XmlTvEpgProducer;
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

public class EpgInfosToEpgGuideTest {

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

        Document doc = buildDocumentFromEpgInfo(infos);
        Transformer t = TransformerFactory.newInstance().newTransformer();
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        t.transform(new DOMSource(doc), new StreamResult(System.out));
        t.transform(new DOMSource(doc), new StreamResult(new FileOutputStream("/home/spiller/tv/config/data/epg.xml")));
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

}
