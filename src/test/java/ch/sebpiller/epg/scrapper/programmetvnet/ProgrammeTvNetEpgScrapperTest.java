package ch.sebpiller.epg.scrapper.programmetvnet;

import ch.sebpiller.epg.Channel;
import ch.sebpiller.epg.EpgInfo;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProgrammeTV Net Scrapper test")
class ProgrammeTvNetEpgScrapperTest {
    private static final Logger LOG = LoggerFactory.getLogger(ProgrammeTvNetEpgScrapperTest.class);
    private int i = 0;

    @Test
    void testScrapeFromLocal() throws IOException {
        Document doc = Jsoup.parse(getClass().getResourceAsStream("/sample_programmetvnet_mangas.html"), StandardCharsets.UTF_8.name(), "");

        ProgrammeTvNetEpgScrapper scrapper = new ProgrammeTvNetEpgScrapper() {
            @Override
            void parseDetails(String uri, EpgInfo info) {
                // noop
            }
        };

        // do not scrape details
        scrapper.scrapeDocument(doc, e -> {
            this.i++;
            LOG.info("{}", e);

            return true;
        });

        assertThat(this.i).isEqualTo(56);
    }

    @Test
    void testScrapeDetailsFromLocal() throws IOException {
        ProgrammeTvNetEpgScrapper scrapper = new ProgrammeTvNetEpgScrapper();
        EpgInfo info;

        Document doc = Jsoup.parse(getClass().getResourceAsStream("/sample_details_6_programmetvnet.html"), StandardCharsets.UTF_8.name(), "");
        scrapper.parseDetails(doc, info = new EpgInfo(Channel.TMC));
        LOG.info("{}", info);
        //assertThat(info.getCategory()).isNotNull();
        assertThat(info.getDescription()).isNotNull();
    }

}