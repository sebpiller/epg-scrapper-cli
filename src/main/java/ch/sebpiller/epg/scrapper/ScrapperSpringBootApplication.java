package ch.sebpiller.epg.scrapper;

import org.apache.commons.lang3.Validate;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import picocli.CommandLine;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@SpringBootApplication
public class ScrapperSpringBootApplication {
    static class VersionProvider implements CommandLine.IVersionProvider {
        public static final String ARTIFACT_ID = "epg-scrapper-cli";

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

                    return new String[]{
                            props.getProperty("version") + " (built on " + props.getProperty("timestamp") + ")"
                    };
                } catch (IOException e) {
                    System.err.println("unable to load file " + name);
                }
            }

            return new String[]{"unknown"};
        }
    }

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(ScrapperSpringBootApplication.class, args)));
    }
}
