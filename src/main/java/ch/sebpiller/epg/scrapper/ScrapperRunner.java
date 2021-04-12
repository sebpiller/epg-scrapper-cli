package ch.sebpiller.epg.scrapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.util.Objects;

@Component
public class ScrapperRunner implements CommandLineRunner, ExitCodeGenerator {

    private final ScrapperCommand scrapperCommand;

    private final CommandLine.IFactory factory;

    private int exitCode;

    @Autowired
    public ScrapperRunner(ScrapperCommand scrapperCommand, CommandLine.IFactory factory) {
        this.scrapperCommand = Objects.requireNonNull(scrapperCommand);
        this.factory = Objects.requireNonNull(factory);
    }

    @Override
    public void run(String... args) throws Exception {
        exitCode = new CommandLine(scrapperCommand, factory).execute(args);
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}
