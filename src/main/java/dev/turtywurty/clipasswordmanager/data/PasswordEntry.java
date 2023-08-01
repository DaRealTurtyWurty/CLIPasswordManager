package dev.turtywurty.clipasswordmanager.data;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
@ToString
@EqualsAndHashCode
public class PasswordEntry {
    private final String name;
    private final String website;
    private final List<String> tags = new ArrayList<>();
    private final List<String> notes = new ArrayList<>();

    @Setter
    private String username;
    @Setter
    private String email;
    @Setter
    private String password;
    @Setter
    private String description;

    private PasswordEntry(Builder builder) {
        this.name = builder.name;
        this.website = builder.website;
        this.username = builder.username;
        this.email = builder.email;
        this.password = builder.password;
        this.description = builder.description;
        this.tags.addAll(builder.tags);
        this.notes.addAll(builder.notes);
    }

    public void addTag(String tag) {
        this.tags.add(tag);
    }

    public void addTags(String... tags) {
        this.tags.addAll(Arrays.asList(tags));
    }

    public void removeTag(String tag) {
        this.tags.remove(tag);
    }

    public void removeTags(String... tags) {
        this.tags.removeAll(Arrays.asList(tags));
    }

    public void clearTags() {
        this.tags.clear();
    }

    public void addNote(String note) {
        this.notes.add(note);
    }

    public void addNotes(String... notes) {
        this.notes.addAll(Arrays.asList(notes));
    }

    public void removeNote(String note) {
        this.notes.remove(note);
    }

    public void removeNotes(String... notes) {
        this.notes.removeAll(Arrays.asList(notes));
    }

    public void clearNotes() {
        this.notes.clear();
    }

    public static class Builder {
        private final String name;
        private final String website;
        private final List<String> tags = new ArrayList<>();
        private final List<String> notes = new ArrayList<>();

        private String username;
        private String email;
        private String password;
        private String description;

        public Builder(String name, String website) {
            this.name = name;
            this.website = website;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder tags(String... tags) {
            this.tags.addAll(Arrays.stream(tags).filter(tag -> tag != null && !tag.isBlank()).toList());
            return this;
        }

        public Builder notes(String... notes) {
            this.notes.addAll(Arrays.stream(notes).filter(note -> note != null && !note.isBlank()).toList());
            return this;
        }

        public PasswordEntry build() {
            return new PasswordEntry(this);
        }
    }
}
