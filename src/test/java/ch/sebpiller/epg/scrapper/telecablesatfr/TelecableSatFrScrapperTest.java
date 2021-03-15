package ch.sebpiller.epg.scrapper.telecablesatfr;

import ch.sebpiller.epg.Audience;
import ch.sebpiller.epg.EpgInfo;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class TelecableSatFrScrapperTest {
    private static final Logger LOG = LoggerFactory.getLogger(TelecableSatFrScrapperTest.class);
    private int i = 0;

    @Test
    public void testScrapeFromLocal() throws IOException {
        Document[] docs = {
                Jsoup.parse(getClass().getResourceAsStream("/telecablesatfr/sample_afternoon.html"), StandardCharsets.UTF_8.name(), ""),
                Jsoup.parse(getClass().getResourceAsStream("/telecablesatfr/sample_morning.html"), StandardCharsets.UTF_8.name(), ""),
                Jsoup.parse(getClass().getResourceAsStream("/telecablesatfr/sample_noon.html"), StandardCharsets.UTF_8.name(), ""),
        };

        TelecableSatFrScrapper scrapper = new TelecableSatFrScrapper();

        // do not scrape details
        for (Document doc : docs)
            scrapper.scrapeDocument(doc, x -> false, e -> {
                this.i++;
                LOG.info("{}", e);

                return true;
            });

        assertThat(this.i).isEqualTo(16);
    }

    @Test
    public void testScrapeDetailsFromLocal() throws IOException {
        TelecableSatFrScrapper scrapper = new TelecableSatFrScrapper();
        EpgInfo info = new EpgInfo();

        Document doc = Jsoup.parse(getClass().getResourceAsStream("/sample_details_8_playtvfr_rougetv.html"), StandardCharsets.UTF_8.name(), "");
        //scrapper.parseDetails(doc, info = new EpgInfo());
        LOG.info("{}", info);
        assertThat(info.getCategory()).isNotNull();
        assertThat(info.getAudience()).isEqualTo(Audience.EIGHTEEN);
    }

}