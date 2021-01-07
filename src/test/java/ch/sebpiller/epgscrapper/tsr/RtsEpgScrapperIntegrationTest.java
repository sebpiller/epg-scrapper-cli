package ch.sebpiller.epgscrapper.tsr;

import ch.sebpiller.epgscrapper.Channel;
import ch.sebpiller.epgscrapper.EpgInfo;
import org.jsoup.Jsoup;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Ignore
public class RtsEpgScrapperIntegrationTest {
    private static final Logger LOG = LoggerFactory.getLogger(RtsEpgScrapperIntegrationTest.class);

    private List<Channel> acceptedChannels = Arrays.asList(Channel.RTS1);

    @Test
    public void displayScheduleFromRts() throws IOException {
        LOG.debug("\n{}", Jsoup.connect("https://hummingbird.rts.ch/hummingbird-ajax/programme-tv/schedules?dayStr=2021-01-07&limit=6&offset=0&filter=none").get());
    }

    @Test
    public void displayScheduleDetailFromRts() throws IOException {
        LOG.debug("\n{}", Jsoup.connect("https://hummingbird.rts.ch/hummingbird-ajax/programme-tv/medias/imedia-243919772").get());
    }

    int i = 0;

    @Test
    public void testScrapeFromRts() throws IOException {
        List<EpgInfo> allInfos = new ArrayList<>(10 * 1024);

        long start = System.currentTimeMillis();
        RtsEpgScrapper scrapper = new RtsEpgScrapper();
        scrapper.scrapeEpg(e -> {
            i++;
            allInfos.add(e);

            if (allInfos.size() % 100 == 0) {
                LOG.info("found {} in {}s", allInfos.size(), (System.currentTimeMillis() - start) / 1_000);
            }
        });

        allInfos.stream().filter(e -> e.getChannel() == Channel.RTS1).forEach(System.out::println);

        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("all_epg_info.data"));
        oos.writeObject(allInfos);
        oos.close();
    }
}