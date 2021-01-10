package ch.sebpiller.epg.scrapper.ocs;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class OcsEpgScrapperTest {
    private static final Logger LOG = LoggerFactory.getLogger(OcsEpgScrapperTest.class);
    private int i=0;
    @Test
    public void testScrapeFromLocal() throws IOException {
        Document doc = Jsoup.parse(getClass().getResourceAsStream("/sample_ocs.html"), StandardCharsets.UTF_8.name(), "");

        OcsEpgScrapper scrapper = new OcsEpgScrapper();

        // do not scrape details
        scrapper.scrapeDocument(doc, c -> true, x -> false, e -> {
            i++;
            LOG.info("{}", e);

            return true;
        });

        assertThat(i).isEqualTo(71);
    }
}