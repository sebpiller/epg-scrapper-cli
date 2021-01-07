package ch.sebpiller.epgscrapper.tsr;

import ch.sebpiller.epgscrapper.Channel;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class TsrEpgScrapperIntegrationTest {
    private static final Logger LOG = LoggerFactory.getLogger(TsrEpgScrapperIntegrationTest.class);

    List<Channel> acceptedChannels = Arrays.asList(Channel.RTS1, Channel.RTS2);

    @Test
    public void testScrapeFromTsr() {
        TsrEpgScrapper scrapper = new TsrEpgScrapper();
        scrapper.scrapeEpg(e -> {
            if (acceptedChannels.contains(e.getChannel()))
                LOG.info("found {}", e);
        });
    }
}