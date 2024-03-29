package ch.sebpiller.epg.scrapper.programmetvnet;

import ch.sebpiller.epg.Channel;
import ch.sebpiller.epg.EpgInfo;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import ch.sebpiller.epg.scrapper.IntegrationTest;
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

@DisplayName("ProgrammeTV Net Scrapper integration")
//@EnabledIfEnvironmentVariable(named = "HOSTNAME", matches = "jenkins.*")
class ProgrammeTvNetEpgScrapperIntegrationTest extends IntegrationTest {
    private static final Logger LOG = LoggerFactory.getLogger(ProgrammeTvNetEpgScrapperIntegrationTest.class);

    @Test
    void testScrapeFromProgrammeTvNet() throws IOException {
        List<EpgInfo> allInfos = new ArrayList<>(25_000);

        long start = System.currentTimeMillis();
        ProgrammeTvNetEpgScrapper scrapper = new ProgrammeTvNetEpgScrapper();
        scrapper.scrapeEpg(c -> c == Channel.MANGAS, e -> {
            allInfos.add(e);

            if (allInfos.size() % 100 == 0) {
                LOG.info("found {} in {}s", allInfos.size(), (System.currentTimeMillis() - start) / 1_000);
            }

            return true;
        });

        Assertions.assertThat(allInfos).isNotEmpty().hasSizeGreaterThan(100);
        System.out.println("Found " + allInfos.size() + " items");

       /* ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("all_programmetvnet_epg_info.data"));
        oos.writeObject(allInfos);
        oos.close();*/
    }
}