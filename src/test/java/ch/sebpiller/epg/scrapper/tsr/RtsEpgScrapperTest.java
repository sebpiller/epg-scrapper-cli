package ch.sebpiller.epg.scrapper.tsr;

import ch.sebpiller.epg.Channel;
import ch.sebpiller.epg.EpgInfo;
import ch.sebpiller.epg.scrapper.EpgInfoScrappedListener;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RTS Scrapper test")
class RtsEpgScrapperTest {
    private static final Logger LOG = LoggerFactory.getLogger(RtsEpgScrapperTest.class);

    private int i;

    @BeforeEach
    public void setUp() {
        this.i = 0;
    }

    @Test
    void testEpisodePattern() {
        assertThat("Saison 7 (12/22)").matches(RtsEpgScrapper.EPISODE_PATTERN_STR);
    }

    @Test
    void testScrapeFromLocal() throws IOException {
        Document doc = Jsoup.parse(getClass().getResourceAsStream("/sample_rts.html"), StandardCharsets.UTF_8.name(), "");

        RtsEpgScrapper scrapper = new RtsEpgScrapper() {
            @Override
            void parseDetails(String detailUriId, EpgInfo info) {
                // noop
            }
        };

        // do not scrape details
        scrapper.scrapeDocument(doc, c -> true, e -> {
            this.i++;
            LOG.info("{}", e);

            return true;
        });

        assertThat(this.i).isEqualTo(266);
    }

    @Test
    void testScrapeDetailsFromLocal() throws IOException {
        RtsEpgScrapper scrapper = new RtsEpgScrapper();
        EpgInfo info;

        Document doc;

        doc = Jsoup.parse(getClass().getResourceAsStream("/sample_details_1_rts.html"), StandardCharsets.UTF_8.name(), "");
        scrapper.parseDetails(info = new EpgInfo(Channel.M6), doc);
        LOG.info("{}", info);
        assertThat(info.getCategory()).isNotNull();
        assertThat(info.getDescription()).isNotNull();

        doc = Jsoup.parse(getClass().getResourceAsStream("/sample_details_2_rts.html"), StandardCharsets.UTF_8.name(), "");
        scrapper.parseDetails(info = new EpgInfo(Channel.RTS1), doc);
        LOG.info("{}", info);
        assertThat(info.getDescription()).isNotNull();
        assertThat(info.getActors()).isNotNull().isNotEmpty().hasSizeGreaterThan(3);

        doc = Jsoup.parse(getClass().getResourceAsStream("/sample_details_3_rts.html"), StandardCharsets.UTF_8.name(), "");
        scrapper.parseDetails(info = new EpgInfo(Channel.RTS1), doc);
        LOG.info("{}", info);
        assertThat(info.getDescription()).isNotNull();
    }

}
