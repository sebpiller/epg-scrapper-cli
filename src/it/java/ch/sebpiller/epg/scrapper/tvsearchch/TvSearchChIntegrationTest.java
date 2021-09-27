package ch.sebpiller.epg.scrapper.tvsearchch;


import ch.sebpiller.epg.Channel;
import ch.sebpiller.epg.EpgInfo;
import ch.sebpiller.epg.scrapper.IntegrationTest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.ArrayList;
import java.util.List;

@DisplayName("TvSearch CH Scrapper integration")
//@EnabledIfEnvironmentVariable(named = "HOSTNAME", matches = "jenkins.*")
class TvSearchChIntegrationTest extends IntegrationTest {
    private TvSearchCh test;

    @BeforeEach
    public void setUp() {
        test = new TvSearchCh();
    }

    @Test
    void test_tvsearchch() {
        List<EpgInfo> allInfos = new ArrayList<>(150);
        test.scrapeEpg(x -> x == Channel.ROUGETV, scrapped -> {
            allInfos.add(scrapped);
            return true;
        });

        Assertions.assertThat(allInfos).isNotEmpty().hasSizeGreaterThan(50);
        System.out.println("Found " + allInfos.size() + " items");
    }
}