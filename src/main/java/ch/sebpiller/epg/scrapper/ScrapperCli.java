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

@Command(
        name = "java -jar epg-scrapper.jar",
        footer = "NO Copyright - 2021",
        description = "Fetch data from multiple sources to populate an EPG.",
        sortOptions = false,
        versionProvider = ScrapperCli.VersionProvider.class,
        header = {"spidy-tv-guide - epg scrapper - CLI"}
)
public class ScrapperCli implements Callable<Integer> {
    public static final String ARTIFACT_ID = "epg-scrapper";
    static ScrapperCli scrapperCli;

    private final Function<Integer, Void> exitCodeListener;

    static class VersionProvider implements CommandLine.IVersionProvider {
        @Override
        public String[] getVersion() {
            String implementationVersion = getClass().getPackage().getImplementationVersion();
            if (implementationVersion != null) {
                return new String[]{implementationVersion};
            }

            String name = "/" + ARTIFACT_ID + ".version";
            InputStream versionsInfo = getClass().getResourceAsStream(name);
            if (versionsInfo == null) {
                System.err.println("unable to find version info file.");
            } else {
                try (InputStream is = versionsInfo) {
                    Properties props = new Properties();
                    props.load(is);

                    Validate.isTrue(
                            ARTIFACT_ID.equals(props.getProperty("artifact")),
                            "version file does not reference artifact " + ARTIFACT_ID);

                    return new String[]{props.getProperty("version") + " (built on " + props.getProperty("timestamp") + ")"};
                } catch (IOException e) {
                    System.err.println("unable to load file " + name);
                }
            }

            return new String[]{"unknown"};
        }
    }

    String getVersion() {
        return new VersionProvider().getVersion()[0];
    }

    private static final Logger LOG = LoggerFactory.getLogger(ScrapperCli.class);

    @Option(
            names = {"-v", "--version"},
            description = "Print version information to the console and exit.",
            arity = "0",
            versionHelp = true,
            type = Boolean.class
    )
    private Boolean cliParamVersion;

    @Option(
            names = {"-o", "--output"},
            description = "The file to produce.",
            arity = "1",
            required = true
    )
    private String cliParamOutput;

    ScrapperCli() {
        this.exitCodeListener = exitCode -> {
            System.exit(exitCode);
            return null;
        };
    }

    ScrapperCli(Function<Integer, Void> exitCodeListener) {
        this.exitCodeListener = Objects.requireNonNull(exitCodeListener);
    }

    public static void main(String[] args) {
        // For testability: create an instance of this class only if none has been provided by tests.
        if (scrapperCli == null) {
            scrapperCli = new ScrapperCli();
        }

        int exitCode = 0;

        CommandLine commandLine = new CommandLine(scrapperCli);

        try {
            commandLine.parseArgs(args);
        } catch (PicocliException pce) {
            commandLine.usage(System.out);
            exitCode = -1;
            LOG.error("error: ", pce);
        }

        if (commandLine.isUsageHelpRequested()) {
            commandLine.usage(System.out);
        } else if (commandLine.isVersionHelpRequested()) {
            commandLine.printVersionHelp(System.out);
        } else {
            exitCode = commandLine.execute(args);
        }

        LOG.info("exiting with exit code {}", exitCode);
        scrapperCli.exitCodeListener.apply(exitCode);
    }

    private int i;

    @Override
    public Integer call() {
        this.i = 0;
        String s = StringUtils.join(ScrapperCli.class.getAnnotation(Command.class).header(), "\n");
        s = s + " " + "version: " + getVersion() + "\n";

        LOG.info("booting app...\n{}\n", s);

        // Validation of the injected configuration
        validateThis();

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

        Set<ConstraintViolation<ScrapperCli>> violations = validator.validate(this);
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
