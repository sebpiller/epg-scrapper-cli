package ch.sebpiller.epg.scrapper.tvsearchch;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "HOSTNAME", matches = "jenkins.*")
class TvSearchChIntegrationTest {

    private TvSearchCh test;

    @BeforeEach
    public void setUp() {
        test = new TvSearchCh();
    }

    @Test
    void test_tvsearchch() {
        test.scrapeEpg(x -> true, scrapped -> {
            System.out.println(scrapped);
            return true;
        });
    }

}