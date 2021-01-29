package ch.sebpiller.epg.scrapper.playtvfr;

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

public class PlayTvFrEpgScrapperTest {
    private static final Logger LOG = LoggerFactory.getLogger(PlayTvFrEpgScrapperTest.class);
    private int i = 0;

    @Test
    public void testScrapeFromLocal() throws IOException {
        Document doc = Jsoup.parse(getClass().getResourceAsStream("/sample_playtvfr.html"), StandardCharsets.UTF_8.name(), "");

        PlayTvFrEpgScrapper scrapper = new PlayTvFrEpgScrapper();

        // do not scrape details
        scrapper.scrapeDocument(doc, x -> false, e -> {
            this.i++;
            LOG.info("{}", e);

            return true;
        });

        assertThat(this.i).isEqualTo(15);
    }

    @Test
    public void testScrapeDetailsFromLocal() throws IOException {
        PlayTvFrEpgScrapper scrapper = new PlayTvFrEpgScrapper();
        EpgInfo info;

        Document doc = Jsoup.parse(getClass().getResourceAsStream("/sample_details_8_playtvfr_rougetv.html"), StandardCharsets.UTF_8.name(), "");
        scrapper.parseDetails(doc, info = new EpgInfo());
        LOG.info("{}", info);
        //assertThat(info.getCategory()).isNotNull();
        assertThat(info.getAudience()).isEqualTo(Audience.EIGHTEEN);
    }

}