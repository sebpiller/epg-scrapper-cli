package ch.sebpiller.epg.scrapper.tvsearchch;

import org.junit.Before;
import org.junit.Test;

public class TvSearchChIntegrationTest {

    private TvSearchCh test;

    @Before
    public void setUp() {
        test = new TvSearchCh();
    }

    @Test
    public void test_tvsearchch() {
        test.scrapeEpg(x -> true, x -> true, scrapped -> {
            System.out.println(scrapped);
            return true;
        });
    }

}