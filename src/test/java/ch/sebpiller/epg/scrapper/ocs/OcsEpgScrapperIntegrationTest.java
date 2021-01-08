package ch.sebpiller.epg.scrapper.ocs;

import ch.sebpiller.epg.EpgInfo;
import ch.sebpiller.epg.scrapper.tsr.RtsEpgScrapper;
import org.jsoup.Jsoup;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class OcsEpgScrapperIntegrationTest {
    private static final Logger LOG = LoggerFactory.getLogger(OcsEpgScrapperIntegrationTest.class);

    @Test
    public void testScrapeFromOcs() throws IOException {
        List<EpgInfo> allInfos = new ArrayList<>(25_000);
        final ZonedDateTime inAWeek = ZonedDateTime.now().plus(7, ChronoUnit.DAYS);

        long start = System.currentTimeMillis();
        OcsEpgScrapper scrapper = new OcsEpgScrapper();
        scrapper.scrapeEpg(c -> true, i -> true, e -> {
            allInfos.add(e);

            if (allInfos.size() % 100 == 0) {
                LOG.info("found {} in {}s", allInfos.size(), (System.currentTimeMillis() - start) / 1_000);
            }

            return e.getTimeStart().isBefore(inAWeek);
        });

        allInfos.forEach(System.out::println);

        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("all_ocs_epg_info.data"));
        oos.writeObject(allInfos);
        oos.close();
    }
}