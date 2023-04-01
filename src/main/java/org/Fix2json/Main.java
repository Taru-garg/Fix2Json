package org.Fix2json;

import org.Fix2json.generator.Fix2JsonCommand;
import picocli.CommandLine;

public class Main {
    public static void main(String[] args) {
        new CommandLine(new Fix2JsonCommand()).execute(args);
    }
}

