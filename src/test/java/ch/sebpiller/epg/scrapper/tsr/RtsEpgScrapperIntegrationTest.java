package ch.sebpiller.epg.scrapper.tsr;

import ch.sebpiller.epg.Channel;
import ch.sebpiller.epg.EpgInfo;
import org.jsoup.Jsoup;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RtsEpgScrapperIntegrationTest {
    private static final Logger LOG = LoggerFactory.getLogger(RtsEpgScrapperIntegrationTest.class);
    int i = 0;
    private List<Channel> acceptedChannels = Arrays.asList(Channel.RTS1);

    @Test
    public void displayScheduleFromRts() throws IOException {
        LOG.debug("\n{}", Jsoup.connect("https://hummingbird.rts.ch/hummingbird-ajax/programme-tv/schedules?dayStr=2021-01-07&limit=6&offset=0&filter=none").get());
    }

    @Test
    public void displayScheduleDetailFromRts() throws IOException {
        LOG.debug("\n{}", Jsoup.connect("https://hummingbird.rts.ch/hummingbird-ajax/programme-tv/medias/imedia-243919772").get());
    }

    @Test
    public void testScrapeFromRts() throws IOException {
        List<EpgInfo> allInfos = new ArrayList<>(25_000);

        long start = System.currentTimeMillis();
        RtsEpgScrapper scrapper = new RtsEpgScrapper();
        scrapper.scrapeEpg(c -> true, i -> true, e -> {
            i++;
            allInfos.add(e);

            if (allInfos.size() % 100 == 0) {
                LOG.info("found {} in {}s", allInfos.size(), (System.currentTimeMillis() - start) / 1_000);
            }
        });

        allInfos.stream().forEach(System.out::println);

        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("all_epg_info.data"));
        oos.writeObject(allInfos);
        oos.close();
    }
}