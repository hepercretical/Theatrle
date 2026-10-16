package com.theatrle.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

/** Single-column CSV reader preserving quoted commas, quotes, spaces and numeric strings. */
public final class FilmCsv {
    private FilmCsv() { }
    public static ArrayList<String> read(BufferedReader reader) throws IOException {
        ArrayList<String> films = new ArrayList<>();
        String line = reader.readLine();
        if (line == null || !line.replace("\uFEFF", "").equals("title")) throw new IOException("Expected title header");
        while ((line = reader.readLine()) != null) {
            if (line.isEmpty()) continue;
            String title = line;
            if (line.startsWith("\"")) {
                if (!line.endsWith("\"")) throw new IOException("Unclosed CSV quote");
                title = line.substring(1, line.length() - 1).replace("\"\"", "\"");
            } else if (line.contains(",") || line.contains("\"")) throw new IOException("Unquoted CSV field");
            if (title.isEmpty()) throw new IOException("Empty film title");
            films.add(title);
        }
        return films;
    }
}
