package ch.sebpiller.epg.scrapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScrapperCliTest {
    private ScrapperCli scrapperCli;
    private Integer exitCode;

    @BeforeEach
    void setUp() {
        exitCode = null;

        // prevent the cli class to exit the JVM with a return code at the end of its main method.
        scrapperCli =  new ScrapperCli(exitCode -> {
            System.out.println("exit with " + exitCode);
            this.exitCode = exitCode;
            return null;
        });

        ScrapperCli.scrapperCli = scrapperCli;
    }

    @Disabled("this test fails if maven did not package the file before. So this is an unstable test. ")
    @Test
    void test_version() {
        assertThat(scrapperCli.getVersion())
                .isNotEmpty()
                .contains("built on");
    }

    @Test
    void test_main() {
        // bad call (no mandatory option specified) should lead to a different return code
        ScrapperCli.main(new String[0]);
        assertThat(exitCode).isNotZero();
    }
}