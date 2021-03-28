package ch.sebpiller.epg.scrapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ScrapperCli integration")
@EnabledIfEnvironmentVariable(named = "HOSTNAME", matches = "jenkins.*")
class ScrapperCliIntegrationTest {
    private ScrapperCli scrapperCli;
    private Integer exitCode;

    @BeforeEach
    void setUp() {
        exitCode = null;

        // prevent the cli class to exit the JVM with a return code at the end of its main method.
        scrapperCli = new ScrapperCli(exitCode -> {
            System.out.println("exit with " + exitCode);
            this.exitCode = exitCode;
            return null;
        });

        ScrapperCli.scrapperCli = scrapperCli;
    }

    @Disabled("this test takes way too much time to run")
    @Test
    void test_main() {
        ScrapperCli.main(new String[]{"--output", "target/epg.xml"});
        assertThat(exitCode).isZero();
    }
}
