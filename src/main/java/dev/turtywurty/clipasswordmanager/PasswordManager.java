package dev.turtywurty.clipasswordmanager;

import com.google.gson.*;
import dev.turtywurty.clipasswordmanager.data.PasswordEntry;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.scrypt.SCryptPasswordEncoder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class PasswordManager {
    private final Scanner scanner = new Scanner(System.in);
    private final CLIParseUtils parseUtils = new CLIParseUtils(this.scanner);
    private final Path passwordEntriesPath = Path.of(System.getProperty("user.home"), "password_entries.json");
    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public static void main(String[] args) {
        var manager = new PasswordManager();
        while(true) {
            manager.run();
        }
    }

    private void run() {
        System.out.println("""
            Please select an action to perform:
            1. Create a new password entry
            2. Retrieve a password entry
            3. Exit""");

        Optional<Integer> optAction = this.parseUtils.tryReadInt();

        if(optAction.isEmpty()) {
            run();
            return;
        }

        int action = optAction.get();

        if(action < 1 || action > 3) {
            System.out.println("The number you entered does not correspond to a valid action!");
            run();
            return;
        }

        scanner.nextLine();

        switch (action) {
            case 1 -> createEntry();
            case 2 -> retrieveEntry();
            case 3 -> System.exit(0);
            default -> {
                System.out.println("The number you entered does not correspond to a valid action!");
                run();
            }
        }
    }

    private void createEntry() {
        System.out.println("""
                Please select all that apply (e.g. "1, 2")
                1. Username
                2. Email
                69. Cancel""");

        Optional<Integer[]> actions = this.parseUtils.tryReadInts();
        if(actions.isEmpty()) {
            createEntry();
            return;
        }

        Integer[] actionsArr = actions.get();
        if(actionsArr.length == 0) {
            System.out.println("You must select at least one option!");
            createEntry();
            return;
        }

        if(Arrays.stream(actionsArr).anyMatch(action -> action == 69)) {
            run();
            return;
        }

        Map<String, String> supplied = new HashMap<>();
        for (Integer action : actionsArr) {
            switch (action) {
                case 1 -> supplied.put("username", createEntry("username"));
                case 2 -> supplied.put("email", createEntry("email"));
                default -> System.out.printf("The number you entered (%d) does not correspond to a valid action!%n", action);
            }
        }

        // required password
        supplied.put("password", createEntry("password"));

        // required name
        System.out.println("Enter a name for this entry: ");
        String name = this.scanner.nextLine();
        if(name.isBlank()) {
            System.out.println("Your entry name cannot be blank!");
            createEntry();
            return;
        }

        // optional website
        System.out.println("Enter a website for this entry: ");
        String website = this.scanner.nextLine();
        if(website.isBlank()) {
            website = null;
            return;
        } else {
            // validate website
            if(!website.startsWith("http://") && !website.startsWith("https://")) {
                System.out.println("Your entry website must be a valid website!");
                createEntry();
                return;
            }
        }

        // optional description
        System.out.println("Enter a description for this entry (optional): ");
        String description = this.scanner.nextLine();
        if(description.isBlank()) {
            description = null;
        }

        // optional tags
        System.out.println("Enter tags for this entry (optional): ");
        String tagsStr = this.scanner.nextLine();
        String[] tags = tagsStr.split(", ");
        if(tags.length == 1 && tags[0].isBlank() || tagsStr.isBlank()) {
            tags = new String[0];
        }

        // optional note
        System.out.println("Enter a note for this entry (optional): ");
        String note = this.scanner.nextLine();
        if(note.isBlank()) {
            note = null;
        }

        var builder = new PasswordEntry.Builder(name, supplied.get("password"))
                .website(website)
                .username(supplied.getOrDefault("username", null))
                .email(supplied.getOrDefault("email", null))
                .description(description)
                .tags(tags)
                .notes(note);

        PasswordEntry entry = builder.build();
        try {
            savePasswordEntry(entry);
            System.out.println("Your entry has been saved!");
        } catch (IOException exception) {
            System.out.println("An error occurred while saving your entry!");
            exception.printStackTrace();
        }
    }

    private String createEntry(String name) {
        System.out.printf("Please enter your %s: ", name);
        String value = this.scanner.nextLine();
        if(value.isBlank()) {
            System.out.printf("Your %s cannot be blank!%n", name);
            return createEntry(name);
        }

        switch (name) {
            case "email" -> {
                if(!value.contains("@")) {
                    System.out.println("Your email must contain an '@' symbol!");
                    return createEntry(name);
                }
            }
            case "password" -> {
                if(value.length() < 5) {
                    System.out.println("Your password must be at least 5 characters long!");
                    return createEntry(name);
                }
            }
        }

        return value;
    }

    private void retrieveEntry() {
        System.out.println("""
                Please select a way to retrieve your entry:
                1. Name
                2. Website
                3. Username
                4. Email
                69. Cancel""");

        int action = this.parseUtils.tryReadInt().orElse(-1);

        if(action < 1 || action > 4 && action != 69) {
            System.out.println("The number you entered does not correspond to a valid action!");
            retrieveEntry();
            return;
        }

        if(action == 69) {
            run();
            return;
        }

        Optional<PasswordEntry> entry;
        switch (action) {
            case 1 -> entry = retrieveEntry("name");
            case 2 -> entry = retrieveEntry("website");
            case 3 -> entry = retrieveEntry("username");
            case 4 -> entry = retrieveEntry("email");
            default -> {
                System.out.printf("The number you entered (%d) does not correspond to a valid action!%n", action);
                run();
                return;
            }
        };

        if(entry.isEmpty()) {
            System.out.println("No entry was found with the given information!");
            retrieveEntry();
            return;
        }

        System.out.println("Your entry has been found!");
        System.out.println(entry.get());
    }

    private Optional<PasswordEntry> retrieveEntry(String name) {
        try {
            if (Files.notExists(this.passwordEntriesPath)) {
                return Optional.empty();
            }

            String content = Files.readString(this.passwordEntriesPath);
            JsonArray data = this.gson.fromJson(content, JsonArray.class);
            List<PasswordEntry> entries = new ArrayList<>();
            for (JsonElement element : data) {
                JsonObject object = element.getAsJsonObject();
                PasswordEntry.Builder builder =
                        new PasswordEntry.Builder(
                                object.get("name").getAsString(),
                                object.get("password").getAsString());

                if (object.has("website"))
                    builder.website(object.get("website").getAsString());

                if (object.has("username"))
                    builder.username(object.get("username").getAsString());

                if (object.has("email"))
                    builder.email(object.get("email").getAsString());

                if (object.has("description"))
                    builder.description(object.get("description").getAsString());

                if (object.has("tags")) {
                    JsonArray tags = object.get("tags").getAsJsonArray();
                    List<String> tagsList = new ArrayList<>();
                    for (JsonElement tag : tags) {
                        tagsList.add(tag.getAsString());
                    }

                    builder.tags(tagsList.toArray(new String[0]));
                }

                if (object.has("notes")) {
                    JsonArray notes = object.get("notes").getAsJsonArray();
                    List<String> notesList = new ArrayList<>();
                    for (JsonElement note : notes) {
                        notesList.add(note.getAsString());
                    }

                    builder.notes(notesList.toArray(new String[0]));
                }

                entries.add(builder.build());
            }

            return entries.stream().filter(entry -> {
                switch (name) {
                    case "name" -> {
                        System.out.println("Enter the name of the entry you want to retrieve: ");
                        String value = this.scanner.nextLine();
                        return entry.getName().equalsIgnoreCase(value);
                    }
                    case "website" -> {
                        System.out.println("Enter the website of the entry you want to retrieve: ");
                        String value = this.scanner.nextLine();
                        return entry.getWebsite().equalsIgnoreCase(value);
                    }
                    case "username" -> {
                        System.out.println("Enter the username of the entry you want to retrieve: ");
                        String value = this.scanner.nextLine();
                        return entry.getUsername().equalsIgnoreCase(value);
                    }
                    case "email" -> {
                        System.out.println("Enter the email of the entry you want to retrieve: ");
                        String value = this.scanner.nextLine();
                        return entry.getEmail().equalsIgnoreCase(value);
                    }
                    default -> throw new IllegalStateException("Unexpected value: " + name);
                }
            }).findFirst();
        } catch (IOException exception) {
            exception.printStackTrace();
            return Optional.empty();
        }
    }

    private void savePasswordEntry(PasswordEntry entry) throws IOException {
        if(Files.notExists(this.passwordEntriesPath)) {
            Files.createDirectories(this.passwordEntriesPath.getParent());
            Files.createFile(this.passwordEntriesPath);
            Files.writeString(this.passwordEntriesPath, this.gson.toJson(new JsonArray()));
        }

        JsonArray data = this.gson.fromJson(Files.readString(this.passwordEntriesPath), JsonArray.class);

        var entryJson = new JsonObject();
        entryJson.addProperty("name", entry.getName());
        entryJson.addProperty("website", entry.getWebsite());

        if(entry.getUsername() != null && !entry.getUsername().isEmpty())
            entryJson.addProperty("username", entry.getUsername());

        if(entry.getEmail() != null && !entry.getEmail().isEmpty())
            entryJson.addProperty("email", entry.getEmail());

        if(entry.getPassword() != null && !entry.getPassword().isEmpty())
            entryJson.addProperty("password", this.passwordEncoder.encode(entry.getPassword()));

        if(entry.getDescription() != null && !entry.getDescription().isEmpty())
            entryJson.addProperty("description", entry.getDescription());

        if(entry.getTags() != null && !entry.getTags().isEmpty()) {
            var tags = new JsonArray();
            entry.getTags().forEach(tags::add);
            entryJson.add("tags", tags);
        }

        if(entry.getNotes() != null && !entry.getNotes().isEmpty()) {
            var notes = new JsonArray();
            entry.getNotes().forEach(notes::add);
            entryJson.add("notes", notes);
        }

        data.add(entryJson);
        Files.writeString(this.passwordEntriesPath, this.gson.toJson(data));
    }
}
