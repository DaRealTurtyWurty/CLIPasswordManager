package dev.turtywurty.clipasswordmanager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.turtywurty.clipasswordmanager.data.PasswordEntry;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class PasswordManager {
    private final Scanner scanner = new Scanner(System.in);
    private final CLIParseUtils parseUtils = new CLIParseUtils(this.scanner);
    private final Path passwordEntriesPath = Path.of("~/password_entries.json");
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
                Please select all that apply (e.g. "1, 2, 3")
                1. Username
                2. Email
                3. Password
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

        List<Integer> actionsList = List.of(actionsArr);
        if(actionsList.contains(69)) {
            run();
            return;
        }

        Map<String, String> supplied = new HashMap<>();
        actionsList.forEach(action -> {
            switch (action) {
                case 1 -> supplied.put("username", createEntry("username"));
                case 2 -> supplied.put("email", createEntry("email"));
                case 3 -> supplied.put("password", createEntry("password"));
                default -> System.out.printf("The number you entered (%d) does not correspond to a valid action!%n", action);
            }
        });

        // required name
        System.out.println("Enter a name for this entry: ");
        String name = this.scanner.nextLine();
        if(name.isBlank()) {
            System.out.println("Your entry name cannot be blank!");
            createEntry();
            return;
        }

        // required website
        System.out.println("Enter a website for this entry: ");
        String website = this.scanner.nextLine();
        if(website.isBlank()) {
            System.out.println("Your entry website cannot be blank!");
            createEntry();
            return;
        }

        // validate website
        if(!website.startsWith("http://") && !website.startsWith("https://")) {
            System.out.println("Your entry website must be a valid website!");
            createEntry();
            return;
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

        var builder = new PasswordEntry.Builder(name, website)
                .username(supplied.getOrDefault("username", null))
                .email(supplied.getOrDefault("email", null))
                .password(supplied.getOrDefault("password", null))
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

    }

    private void savePasswordEntry(PasswordEntry entry) throws IOException {
        if(Files.notExists(this.passwordEntriesPath)) {
            Files.createFile(this.passwordEntriesPath);
            Files.writeString(this.passwordEntriesPath, this.gson.toJson(new JsonArray()));
        }

        JsonArray data = this.gson.fromJson(Files.readString(this.passwordEntriesPath), JsonArray.class);

        var entryJson = new JsonObject();
        entryJson.addProperty("name", entry.getName());

        entryJson.addProperty("website", entry.getWebsite());

        if(entry.getUsername() != null)
            entryJson.addProperty("username", entry.getUsername());

        if(entry.getEmail() != null)
            entryJson.addProperty("email", entry.getEmail());

        if(entry.getPassword() != null)
            entryJson.addProperty("password", this.passwordEncoder.encode(entry.getPassword()));

        if(entry.getDescription() != null)
            entryJson.addProperty("description", entry.getDescription());

        if(entry.getTags() != null) {
            var tags = new JsonArray();
            entry.getTags().forEach(tags::add);
            entryJson.add("tags", tags);
        }

        if(entry.getNotes() != null) {
            var notes = new JsonArray();
            entry.getNotes().forEach(notes::add);
            entryJson.add("notes", notes);
        }

        data.add(entryJson);
        Files.writeString(this.passwordEntriesPath, this.gson.toJson(data));
    }
}
