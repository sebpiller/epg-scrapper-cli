package ch.sebpiller.epg.scrapper.tsr;

import ch.sebpiller.epg.EpgInfo;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class RtsEpgScrapperTest {
    private static final Logger LOG = LoggerFactory.getLogger(RtsEpgScrapperTest.class);

    private int i;

    @Before
    public void setUp() {
        this.i = 0;
    }

    @Test
    public void testEpisodePattern() {
        assertThat("Saison 7 (12/22)").matches(RtsEpgScrapper.EPISODE_PATTERN_STR);
    }

    @Test
    public void testScrapeFromLocal() throws IOException {
        Document doc = Jsoup.parse(getClass().getResourceAsStream("/sample_rts.html"), StandardCharsets.UTF_8.name(), "");

        RtsEpgScrapper scrapper = new RtsEpgScrapper();

        // do not scrape details
        scrapper.scrapeDocument(doc, c -> true, i -> false, e -> {
            this.i++;
            //if (e.getEpisode() != null)
            LOG.info("{}", e);

            return true;
        });

        assertThat(this.i).isEqualTo(266);
    }

    @Test
    public void testScrapeDetailsFromLocal() throws IOException {
        RtsEpgScrapper scrapper = new RtsEpgScrapper();
        EpgInfo info;

        Document doc;

        doc = Jsoup.parse(getClass().getResourceAsStream("/sample_details_1_rts.html"), StandardCharsets.UTF_8.name(), "");
        scrapper.parseDetails(info = new EpgInfo(), doc);
        LOG.info("{}", info);
        assertThat(info.getCategory()).isNotNull();
        assertThat(info.getDescription()).isNotNull();

        doc = Jsoup.parse(getClass().getResourceAsStream("/sample_details_2_rts.html"), StandardCharsets.UTF_8.name(), "");
        scrapper.parseDetails(info = new EpgInfo(), doc);
        LOG.info("{}", info);
        assertThat(info.getDescription()).isNotNull();
        assertThat(info.getActors()).isNotNull().isNotEmpty().hasSizeGreaterThan(3);

        doc = Jsoup.parse(getClass().getResourceAsStream("/sample_details_3_rts.html"), StandardCharsets.UTF_8.name(), "");
        scrapper.parseDetails(info = new EpgInfo(), doc);
        LOG.info("{}", info);
        assertThat(info.getDescription()).isNotNull();
    }

}
