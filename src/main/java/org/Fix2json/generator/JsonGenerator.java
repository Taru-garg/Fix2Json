package org.Fix2json.generator;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import quickfix.ConfigError;
import quickfix.DataDictionary;
import quickfix.InvalidMessage;
import quickfix.Message;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class JsonGenerator {
    public void generate(@NotNull Fix2JsonCommand args) throws Exception {
        FixParser fixParser = new FixParser(args.fixVersion);
        ArrayList<Message> messages = fixParser.getMessagesFromFile(args.messageFile, args.delimiter);

    }

    private static class FixParser {

        private final DataDictionary mdataDictionary;

        public FixParser(String fixVersion) throws ConfigError {
            this.mdataDictionary = new DataDictionary(FixParser.getDataDictPathFromVer(fixVersion));
        }

        @Contract(pure = true)
        private static @NotNull String getDataDictPathFromVer(String fixVersion) {
            final String baseDataDictionaryPath = "src/main/resources/FIX";
            fixVersion = fixVersion.replace(".", "");
            return baseDataDictionaryPath + fixVersion + ".xml";
        }

        @Contract(pure = true)
        public String getHumanFieldName(int field) {
            return mdataDictionary.getFieldName(field);
        }

        @Contract(pure = true)
        public ArrayList<Message> getMessagesFromFile(
                                                      @NotNull File messageFile,
                                                      @NotNull String delimiter) throws IOException {
            final ArrayList<Message> messages = new ArrayList<>();
            int totalLines = 0;
            BufferedReader reader = new BufferedReader(new FileReader(messageFile.toString()));
            String rawMessage;
            while ((rawMessage = reader.readLine()) != null) {
                if (this.getMessageFromString(rawMessage, delimiter) != null) {
                    messages.add(this.getMessageFromString(rawMessage, delimiter));
                }
                totalLines++;
            }
            System.out.printf("Successfully Parsed %d of %d messages\n", messages.size(), totalLines);
            return messages;
        }

        private @Nullable Message getMessageFromString(@NotNull String rawMessage, @NotNull String delimiter) {
            try {
                rawMessage = rawMessage.replace(delimiter, "\u0001");
                Message fixMessage = new Message();
                fixMessage.fromString(
                        rawMessage,
                        this.mdataDictionary,
                        true
                );
                return fixMessage;
            } catch (InvalidMessage m) {
                // Prints : Skipping Message \Italics -> "8=FIX4.4|...|10=92|" \Italics.
                System.err.printf("Skipping message \"\033[3m%s\033[3m\"\n", m.getMessage());
            }
            return null;
        }
    }
}
