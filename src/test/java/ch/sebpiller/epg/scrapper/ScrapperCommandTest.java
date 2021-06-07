package ch.sebpiller.epg.scrapper;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles(profiles = {"test"})
@SpringBootTest(classes = ScrapperSpringBootApplication.class)
class ScrapperCommandTest {
    @Autowired
    ScrapperSpringBootApplication app;

    private ScrapperCommand scrapperCommand;
    private Integer exitCode;

    @Disabled("this test fails since the forked vm returns 2 as an error code, interpreted by surefire as a failure.")
    @Test
    void test_main_ko() {
        // bad call (no mandatory option specified) should lead to a different return code
        ScrapperSpringBootApplication.main(new String[]{});
        //assertThat(exitCode).isNotZero();
    }

    @Disabled("this test takes way to much time (integration test doing the full job).")
    @Test
    void test_main_ok() {
        ScrapperSpringBootApplication.main(new String[]{"--output", "target/pouet.xml"});
        //assertThat(exitCode).isNotZero();
    }
}