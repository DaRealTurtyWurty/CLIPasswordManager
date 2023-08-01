package dev.turtywurty.clipasswordmanager;

import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class CLIParseUtils {
    private final Scanner scanner;

    public CLIParseUtils(Scanner scanner) {
        this.scanner = scanner;
    }

    public Optional<Integer> tryReadInt(int radix, Consumer<Exception> exceptionHandler) {
        try {
            return Optional.of(scanner.nextInt(radix));
        } catch (Exception exception) {
            exceptionHandler.accept(exception);
            return Optional.empty();
        }
    }

    public Optional<Integer> tryReadInt(int radix) {
        return tryReadInt(radix, exception -> System.out.println("Please enter a valid number!"));
    }

    public Optional<Integer> tryReadInt(Consumer<Exception> exceptionHandler) {
        return tryReadInt(this.scanner.radix(), exceptionHandler);
    }

    public Optional<Integer> tryReadInt() {
        return tryReadInt(this.scanner.radix());
    }

    // TODO: Fix Java's bullshit scanners
    public Optional<Integer[]> tryReadInts(int radix, Consumer<Exception> exceptionHandler) {
        try {
            String[] split = scanner.nextLine().split(",");
            var ints = new Integer[split.length];

            for (int index = 0; index < split.length; index++) {
                String string = split[index].trim();
                if(string.isEmpty())
                    continue;

                ints[index] = Integer.parseInt(string, radix);
            }

            return Optional.of(ints);
        } catch (Exception exception) {
            exceptionHandler.accept(exception);
            return Optional.empty();
        }
    }

    public Optional<Integer[]>  tryReadInts(int radix) {
        return tryReadInts(radix, exception -> {
            System.out.println("Please enter a valid number!");
            exception.printStackTrace();
        });
    }

    public Optional<Integer[]> tryReadInts(Consumer<Exception> exceptionHandler) {
        return tryReadInts(this.scanner.radix(), exceptionHandler);
    }

    public Optional<Integer[]>  tryReadInts() {
        return tryReadInts(this.scanner.radix());
    }
}
