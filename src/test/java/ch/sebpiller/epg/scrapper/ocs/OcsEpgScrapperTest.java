package ch.sebpiller.epg.scrapper.ocs;

import ch.sebpiller.epg.EpgInfo;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class OcsEpgScrapperTest {
    private static final Logger LOG = LoggerFactory.getLogger(OcsEpgScrapperTest.class);
    private int i = 0;

    @Test
    void testScrapeFromLocal() throws IOException {
        Document doc = Jsoup.parse(getClass().getResourceAsStream("/sample_ocs.html"), StandardCharsets.UTF_8.name(), "");

        OcsEpgScrapper scrapper = new OcsEpgScrapper();

        // do not scrape details
        scrapper.scrapeDocument(doc, c -> true, e -> {
            i++;
            LOG.info("{}", e);

            return true;
        });

        assertThat(i).isEqualTo(71);
    }

    @Test
    void testScrapeDetailsFromLocal() throws IOException {
        OcsEpgScrapper scrapper = new OcsEpgScrapper();
        EpgInfo info;

        Document doc;

        doc = Jsoup.parse(getClass().getResourceAsStream("/sample_details_4_ocs.html"), StandardCharsets.UTF_8.name(), "");
        scrapper.parseDetails(doc, info = new EpgInfo());
        LOG.info("{}", info);
        //assertThat(info.getCategory()).isNotNull();
        assertThat(info.getDescription()).isNotNull();

        // this is an empty, crappy document that get returned sometimes by the website. We need to accept (yet ignore) such malformed documents.
        doc = Jsoup.parse(getClass().getResourceAsStream("/sample_details_5_ocs_malformed.html"), StandardCharsets.UTF_8.name(), "");
        scrapper.parseDetails(doc, info = new EpgInfo());
        LOG.info("{}", info);
        //  assertThat(info.getCategory()).isNotNull();
        assertThat(info.getDescription()).isNull();
    }

}