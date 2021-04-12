package ch.sebpiller.epg.scrapper;

import ch.sebpiller.epg.producer.SaxXmlTvEpgProducer;
import ch.sebpiller.epg.scrapper.ocs.OcsEpgScrapper;
import ch.sebpiller.epg.scrapper.programmetvnet.ProgrammeTvNetEpgScrapper;
import ch.sebpiller.epg.scrapper.tsr.RtsEpgScrapper;
import ch.sebpiller.epg.scrapper.tvsearchch.TvSearchCh;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.PicocliException;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Function;

@Component
@Command(
        name = "scrappercommand",
        footer = "NO Copyright - 2021",
        description = "Fetch data from multiple sources to populate an EPG.",
        sortOptions = false,
        versionProvider = ScrapperSpringBootApplication.VersionProvider.class,
        header = {"spidy-tv-guide - epg scrapper - CLI"},
        mixinStandardHelpOptions = true
)
public final class ScrapperCommand implements Callable<Integer> {

    static ScrapperCommand scrapperCommand; // test injection to test main method

    private static final Logger LOG = LoggerFactory.getLogger(ScrapperCommand.class);

    @Option(
            names = {"-o", "--output"},
            description = "The file to produce.",
            arity = "1",
            required = true
    )
    private String cliParamOutput;

    private int i;

    @Override
    public Integer call() {
        this.i = 0;

        // Validation of the injected configuration
        //validateThis();

        // All scrappers are run in parallel
        List<EpgScrapper> scrappers = Arrays.asList(
                new RtsEpgScrapper(),
                new OcsEpgScrapper(),
                new TvSearchCh(),
                new ProgrammeTvNetEpgScrapper()
        );

        long start = System.currentTimeMillis();

        try (OutputStream os = new FileOutputStream(cliParamOutput);
             SaxXmlTvEpgProducer xmlProducer = new SaxXmlTvEpgProducer(os)) {
            scrappers.parallelStream().forEach(epgScrapper -> epgScrapper.scrapeEpg(x -> true, x -> {
                this.i++;
                if (this.i % 100 == 0) {
                    LOG.info("scrapped {} infos in {}s", this.i, (System.currentTimeMillis() - start) / 1_000);
                }

                return xmlProducer.epgInfoScrapped(x);
            }));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return 0;
    }

    private void validateThis() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        Set<ConstraintViolation<ScrapperCommand>> violations = validator.validate(this);
        if (!violations.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            violations.forEach(cliConstraintViolation -> sb
                    .append(", '")
                    .append(cliConstraintViolation.getInvalidValue())
                    .append("' ")
                    .append(cliConstraintViolation.getMessage())
            );

            String s = sb.substring(2);

            LOG.error("invoked command was invalid: {}", s);
            throw new IllegalArgumentException("invalid configuration: " + s);
        }
    }
}
