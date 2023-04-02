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

@SuppressWarnings("unchecked")
public class JsonGenerator {

    private @NotNull JSONObject toJson(@NotNull Message message, @NotNull DataDictionary dd) throws FieldNotFound {
        JSONObject jsonFixMessage = new JSONObject();
        jsonFixMessage.put( // Get the jsonified Header part of the FIX message
                "Header",
                this.getJsonifiedMessage(
                        message.getHeader(),
                        dd
                )
        );
        jsonFixMessage.put( // Get the jsonified Body of the FIX message
                "Body",
                this.getJsonifiedMessage(
                        message,
                        dd
                )
        );
        jsonFixMessage.put( // Get the jsonified Trailer part of the FIX message
                "Trailer",
                this.getJsonifiedMessage(
                        message.getTrailer(),
                        dd
                )
        );
        return jsonFixMessage;
    }

    private @NotNull JSONObject getJsonifiedMessage(@NotNull FieldMap fieldMap, DataDictionary dd) throws FieldNotFound {
        JSONObject jsonObject = new JSONObject();
        Iterator<Field<?>> iterator = fieldMap.iterator();
        while (iterator.hasNext()) {
            Field<?> field = iterator.next();
            if (!dd.getFieldType(field.getTag()).equals(FieldType.NUMINGROUP)) {
                jsonObject.put(
                        FixParser.getHumanFieldName(field.getTag(), dd),
                        fieldMap.getString(field.getTag())
                );
            }
        }

        Iterator<Integer> groupKeyIterator = fieldMap.groupKeyIterator();
        while (groupKeyIterator.hasNext()) {
            JSONArray groupJsonObject = new JSONArray();
            Integer groupField = groupKeyIterator.next();
            String humanReadableGroupName = FixParser.getHumanFieldName(groupField, dd);
            Group group = new Group(groupField, 0);
            int i = 1;
            while (fieldMap.hasGroup(i, groupField)) {
                fieldMap.getGroup(i, group);
                groupJsonObject.add(this.getJsonifiedMessage(group, dd));
                i++;
            }
            jsonObject.put(humanReadableGroupName, groupJsonObject);
        }

        return jsonObject;
    }

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
        public static String getHumanFieldName(int field, @NotNull DataDictionary dd) {
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
                if(!this.validateMessageVersion(fixMessage)) {
                    throw new InvalidMessage(
                            "Message version & Data Dictionary Version don't match "
                            + rawMessage
                    );
                }
                return fixMessage;
            } catch (InvalidMessage m) {
                // Prints : Skipping Message \Italics -> "8=FIX4.4|...|10=92|" \Italics.
                System.err.printf("Skipping message \"\033[3m%s\033[3m\"\n", m.getMessage());
            } catch (FieldNotFound e) {
                System.err.printf("Couldn't parse message \"\033[3m%s\033[3m\"\n", e.getMessage());
            }
            return null;
        }

        private boolean validateMessageVersion(@NotNull Message message) throws FieldNotFound {
            int MESSAGE_VERSION_TAG = 8;
            return message.getHeader().getString(MESSAGE_VERSION_TAG).equals(this.mdataDictionary.getFullVersion());
        }

    }
}
