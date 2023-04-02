package org.Fix2json.generator;

import org.jetbrains.annotations.NotNull;
import picocli.CommandLine;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.FileAlreadyExistsException;
import java.util.Arrays;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "Fix2Json Parser",
        mixinStandardHelpOptions = true,
        version = "Fix2Json Parser 1.0.0",
        description = "Convert FIX messages to JSON Messages"
)
public class Fix2JsonCommand implements Callable<Integer> {

    private static final String[] SUPPORTED_FIX_VERSIONS = {
            "4.2", "4.4"
    };
    @CommandLine.Option(
            names = {"-fv", "--fix-version"},
            description = "FIX version to use while parsing the FIX message",
            defaultValue = "4.4"
    )
    String fixVersion;

    @CommandLine.Option(
            names = {"-mf", "--message-file"},
            description = "File Path containing FIX messages " +
                    "Each Line must only contain one message" +
                    "separated by \\x01",
            required = true
    )
    File messageFile;

    @CommandLine.Option(
            names = {"-jf", "--json-file"},
            description = "File Path to write json messages",
            required = true
    )
    File jsonFile;

    @CommandLine.Option(names = {"-d", "--delimiter"},
            description = "Delimiter to be used while processing fix messages",
            defaultValue = "|")
    String delimiter;

    private static boolean checkFileExists(File file) {
        return file.exists() && !file.isDirectory();
    }

    private static void validateArguments(@NotNull Fix2JsonCommand args) throws Exception {
        if (!Fix2JsonCommand.checkFileExists(args.messageFile)) {
            throw new FileNotFoundException(
                    "Message file not found"
            );
        }
        if (Fix2JsonCommand.checkFileExists(args.jsonFile)) {
            throw new FileAlreadyExistsException(
                    args.jsonFile.toString(),
                    null,
                    "File already exists, cannot overwrite"
            );
        }
        if (!Arrays.asList(SUPPORTED_FIX_VERSIONS).contains(args.fixVersion)) {
            throw new IllegalArgumentException(
                    "FIX Version not supported"
            );
        }
    }

    @Override
    public Integer call() {
        try {
            Fix2JsonCommand.validateArguments(this);
            JsonGenerator generator = new JsonGenerator();
            generator.generate(this);
            System.out.printf("%s File generated, Containing JSON equivalent of fix messages in %s\n", jsonFile, messageFile);
            return 0;
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return -1;
        }
    }

}
