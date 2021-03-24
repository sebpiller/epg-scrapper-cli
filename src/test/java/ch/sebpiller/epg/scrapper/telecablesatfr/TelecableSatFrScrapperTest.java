package ch.sebpiller.epg.scrapper.telecablesatfr;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Telecable Sat FR Scrapper test")
class TelecableSatFrScrapperTest {
    private static final Logger LOG = LoggerFactory.getLogger(TelecableSatFrScrapperTest.class);
    private int i = 0;

    @Test
    void testScrapeFromLocal() throws IOException {
        Document[] docs = {
                Jsoup.parse(getClass().getResourceAsStream("/telecablesatfr/sample_afternoon.html"), StandardCharsets.UTF_8.name(), ""),
                Jsoup.parse(getClass().getResourceAsStream("/telecablesatfr/sample_morning.html"), StandardCharsets.UTF_8.name(), ""),
                Jsoup.parse(getClass().getResourceAsStream("/telecablesatfr/sample_noon.html"), StandardCharsets.UTF_8.name(), ""),
        };

        TelecableSatFrScrapper scrapper = new TelecableSatFrScrapper() {

        };

        // do not scrape details
        for (Document doc : docs) {
            scrapper.scrapeDocument(doc, e -> {
                this.i++;
                LOG.info("{}", e);

                return true;
            });
        }

        assertThat(this.i).isEqualTo(14);
    }
}