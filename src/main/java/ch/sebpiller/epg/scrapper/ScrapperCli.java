package ch.sebpiller.epg.scrapper;

import ch.sebpiller.epg.producer.XmlTvEpgProducer;
import ch.sebpiller.epg.scrapper.ocs.OcsEpgScrapper;
import ch.sebpiller.epg.scrapper.playtvfr.PlayTvFrEpgScrapper;
import ch.sebpiller.epg.scrapper.programmetvnet.ProgrammeTvNetEpgScrapper;
import ch.sebpiller.epg.scrapper.tsr.RtsEpgScrapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.PicocliException;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;

@Command(
        name = "java -jar epg-scrapper.jar",
        footer = "NO Copyright - 2020",
        description = "Fetch data from multiple sources to populate an EPG.",
        sortOptions = false,
        versionProvider = ScrapperCli.VersionProvider.class,
        header = {
                ""
        }
)
public class ScrapperCli implements Callable<Integer> {
    public static final String ARTIFACT_ID = "epg-scrapper";

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

                    assert props.getProperty("artifact").equals(ARTIFACT_ID);

                    return new String[]{props.getProperty("version") + " (built on " + props.getProperty("timestamp") + ")"};
                } catch (IOException e) {
                    System.err.println("unable to load file " + name);
                }
            }

            return new String[]{"unknown"};
        }
    }

    private String getVersion() {
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
            arity = "0"
    )
    private String cliParamOutput;

    private ScrapperCli() {
    }

    public static void main(String[] args) {
        int exitCode = 0;

        CommandLine commandLine = new CommandLine(new ScrapperCli());

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
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        String s = StringUtils.join(ScrapperCli.class.getAnnotation(Command.class).header(), "\n");

        s = s + "\n" + "version: " + getVersion() + "\n";

        LOG.info("booting app...\n{}\n", s);

        // Validation of the injected configuration
        validateThis();

        DocumentBuilder db;
        try {
            db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new AssertionError("java xml configuration is invalid: " + e, e);
        }

        Document doc = db.newDocument();

        XmlTvEpgProducer xmlTvEpgProducer = new XmlTvEpgProducer();
        Element root = xmlTvEpgProducer.createRootElement(doc);
        xmlTvEpgProducer.dumpChannels(doc, root);

        // All scrappers are run in parallel
        List<EpgScrapper> scrappers = Arrays.asList(
                new RtsEpgScrapper(),
                new OcsEpgScrapper(),
                new PlayTvFrEpgScrapper(),
                new ProgrammeTvNetEpgScrapper()
        );

        scrappers.parallelStream().forEach(epgScrapper -> epgScrapper.scrapeEpg(x -> true, x -> true, x -> {
            synchronized (xmlTvEpgProducer) {
                xmlTvEpgProducer.dumpInfo(doc, root, x);
            }

            return true;
        }));

        Transformer t;
        try {
            t = TransformerFactory.newInstance().newTransformer();
        } catch (TransformerConfigurationException e) {
            throw new AssertionError("java xml configuration is invalid: " + e, e);
        }

        t.setOutputProperty(OutputKeys.INDENT, "yes");
        t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        try (OutputStream os = new FileOutputStream(this.cliParamOutput)) {
            t.transform(new DOMSource(doc), new StreamResult(os));
        } catch (TransformerException | IOException e) {
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
