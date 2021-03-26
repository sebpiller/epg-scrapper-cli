package ch.sebpiller.epg.scrapper.telecablesatfr;

import ch.sebpiller.epg.EpgInfo;
import ch.sebpiller.epg.scrapper.IntegrationTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

@DisplayName("Telecable Sat FR Scrapper integration")
@Disabled("TelecableSatFrScrapper is deprecated")
@EnabledIfEnvironmentVariable(named = "HOSTNAME", matches = "jenkins.*")
@Deprecated
class TelecableSatFrScrapperIntegrationTest extends IntegrationTest {
    private static final Logger LOG = LoggerFactory.getLogger(TelecableSatFrScrapperIntegrationTest.class);

    // TODO implement
    //@Ignore("tobe implemented")
    @Test
    void testScrapeFromTelecableSatFr() throws IOException {
        List<EpgInfo> allInfos = new ArrayList<>(25_000);

        long start = System.currentTimeMillis();
        TelecableSatFrScrapper scrapper = new TelecableSatFrScrapper();
        scrapper.scrapeEpg(c -> true, e -> {
            allInfos.add(e);

            if (allInfos.size() % 100 == 0) {
                LOG.info("found {} in {}s", allInfos.size(), (System.currentTimeMillis() - start) / 1_000);
            }

            return true;
        });

        allInfos.forEach(System.out::println);

        /*ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("all_telecablesatfr_epg_info.data"));
        oos.writeObject(allInfos);
        oos.close();*/
    }
}