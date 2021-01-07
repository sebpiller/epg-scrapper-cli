package ch.sebpiller.epgscrapper.tsr;

import ch.sebpiller.epgscrapper.EpgInfo;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RtsEpgScrapperTest {
    private static final Logger LOG = LoggerFactory.getLogger(RtsEpgScrapperTest.class);

    private int i;

    @Before
    public void setUp() {
        i = 0;
    }

    @Test
    public void testEpisodePattern() {
        assertThat("Saison 7 (12/22)".matches(RtsEpgScrapper.EPISODE_PATTERN)).isTrue();
    }

    @Test
    public void testScrapeFromLocal() throws IOException {
        Document doc = Jsoup.parse(getClass().getResourceAsStream("/sample_rts.html"), StandardCharsets.UTF_8.name(), "");

        // we use a modified version of the class which do not scrape details
        RtsEpgScrapper scrapper = new RtsEpgScrapper() {
            @Override
            void scrapeDetails(String detailUriId, EpgInfo info) {
                // noop
            }
        };

        scrapper.scrapeDocument(doc, e -> {
            i++;
            if (e.getEpisode() != null) {
                LOG.info("{}", i);
            }
        });

        assertThat(i).isEqualTo(266);
    }

}
