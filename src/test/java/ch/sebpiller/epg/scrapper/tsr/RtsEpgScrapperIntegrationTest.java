package ch.sebpiller.epg.scrapper.tsr;

import ch.sebpiller.epg.EpgInfo;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@EnabledIfEnvironmentVariable(named = "HOSTNAME", matches = "jenkins.*")
class RtsEpgScrapperIntegrationTest {
    private static final Logger LOG = LoggerFactory.getLogger(RtsEpgScrapperIntegrationTest.class);

    @Test
    void displayScheduleFromRts() throws IOException {
        LOG.debug("\n{}", Jsoup.connect("https://hummingbird.rts.ch/hummingbird-ajax/programme-tv/schedules?dayStr=2021-01-07&limit=6&offset=0&filter=none").get());
    }

    @Test
    void displayScheduleDetailFromRts() throws IOException {
        LOG.debug("\n{}", Jsoup.connect("https://hummingbird.rts.ch/hummingbird-ajax/programme-tv/medias/imedia-243919772").get());
    }

    @Test
    void testScrapeFromRts() throws IOException {
        List<EpgInfo> allInfos = new ArrayList<>(25_000);
        final ZonedDateTime inAWeek = ZonedDateTime.now().plus(7, ChronoUnit.DAYS);

        long start = System.currentTimeMillis();
        RtsEpgScrapper scrapper = new RtsEpgScrapper();
        scrapper.scrapeEpg(c -> true, e -> {
            allInfos.add(e);

            if (allInfos.size() % 100 == 0) {
                LOG.info("found {} in {}s", allInfos.size(), (System.currentTimeMillis() - start) / 1_000);
            }


            return e.getTimeStart().isBefore(inAWeek);
        });

        allInfos.forEach(System.out::println);

        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("all_epg_info.data"));
        oos.writeObject(allInfos);
        oos.close();
    }
}