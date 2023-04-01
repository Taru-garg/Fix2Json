package org.Fix2json.generator;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import quickfix.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;

public class JsonGenerator {

    @SuppressWarnings("unchecked")
    private @NotNull JSONObject toJson(@NotNull Message message, DataDictionary dd) throws FieldNotFound {
        JSONObject jsonFixMessage = new JSONObject();
        jsonFixMessage.put( // Get the JSONified Header part of the FIX message
                "Header",
                this.getJsonifiedMessage(
                        message.getHeader(),
                        dd
                )
        );
        jsonFixMessage.put( // Get the JSONified Body of the FIX message
                "Body",
                this.getJsonifiedMessage(
                        message,
                        dd
                )
        );
        jsonFixMessage.put( // Get the JSONified Trailer part of the FIX message
                "Trailer",
                this.getJsonifiedMessage(
                        message.getTrailer(),
                        dd
                )
        );
        return jsonFixMessage;
    }
    @SuppressWarnings("unchecked")
    private JSONObject getJsonifiedMessage(FieldMap fieldMap, DataDictionary dd) throws FieldNotFound {
        JSONObject jsonObject = new JSONObject();
        Iterator<Field<?>> iterator = fieldMap.iterator();
        while(iterator.hasNext()) {
            Field<?> field = iterator.next();
            if( !dd.getFieldType(field.getTag()).equals(FieldType.NUMINGROUP) ){
                jsonObject.put(
                        FixParser.getHumanFieldName(field.getTag(), dd),
                        fieldMap.getString(field.getTag())
                );
            }
        }
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public void generate(@NotNull Fix2JsonCommand args) throws Exception {
        FixParser fixParser = new FixParser(args.fixVersion);
        ArrayList<Message> rawMessages = fixParser.getMessagesFromFile(args.messageFile, args.delimiter);
        JSONArray jsonArray = new JSONArray();
        for (var message : rawMessages) {
            jsonArray.add(
                    this.toJson(
                            message,
                            fixParser.mdataDictionary
                    )
            );
        }
        System.out.println(jsonArray);
        this.writeJsonMessagesToFile(args.jsonFile.toString(), jsonArray);
    }

    private void writeJsonMessagesToFile(String jsonFile, JSONArray jsonMessages) throws IOException {
        FileWriter writer = new FileWriter(jsonFile, false);
        JSONArray.writeJSONString(jsonMessages, writer);
        writer.close();
    }

    public static class FixParser {

        public DataDictionary mdataDictionary;

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
        public static String getHumanFieldName(int field, DataDictionary dd) {
            return dd.getFieldName(field);
        }

        @Contract(pure = true)
        public @NotNull ArrayList<Message> getMessagesFromFile(
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

        // TODO: Add a validation to check the version of FIX message given and the data dictionary loaded

    }
}
