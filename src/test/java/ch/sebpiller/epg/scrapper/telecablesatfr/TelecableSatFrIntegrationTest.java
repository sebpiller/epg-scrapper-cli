package ch.sebpiller.epg.scrapper.telecablesatfr;

import ch.sebpiller.epg.EpgInfo;
import ch.sebpiller.epg.scrapper.playtvfr.PlayTvFrEpgScrapper;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public class TelecableSatFrIntegrationTest {
    private static final Logger LOG = LoggerFactory.getLogger(TelecableSatFrIntegrationTest.class);

    // TODO implement
    //@Ignore("tobe implemented")
    @Test
    public void testScrapeFromTelecableSatFr() throws IOException {
        List<EpgInfo> allInfos = new ArrayList<>(25_000);

        long start = System.currentTimeMillis();
        TelecableSatFrScrapper scrapper = new TelecableSatFrScrapper();
        scrapper.scrapeEpg(c -> true, i -> true, e -> {
            allInfos.add(e);

            if (allInfos.size() % 100 == 0) {
                LOG.info("found {} in {}s", allInfos.size(), (System.currentTimeMillis() - start) / 1_000);
            }

            return true;
        });

        allInfos.forEach(System.out::println);

        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("all_telecablesatfr_epg_info.data"));
        oos.writeObject(allInfos);
        oos.close();
    }
}