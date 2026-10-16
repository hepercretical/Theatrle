package com.theatrle.app;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Display punctuation, word boundaries, and guessable letters/digits remain separate. */
public final class MovieTitle {
    public final String display, answer;
    public final List<String> words;
    public final int[] wordAt;
    public final int wordCount;
    public MovieTitle(String title) {
        display = Normalizer.normalize(title, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .toUpperCase(Locale.ROOT).replace('\u2013', '-').replace('\u2014', '-').trim();
        words = java.util.Collections.unmodifiableList(java.util.Arrays.asList(display.split("[\\s-]+")));
        wordCount = words.size();
        StringBuilder letters = new StringBuilder();
        ArrayList<Integer> indices = new ArrayList<>();
        for (int word = 0; word < words.size(); word++) for (char ch : words.get(word).toCharArray()) if (isLetter(ch)) {
            letters.append(ch); indices.add(word);
        }
        answer = letters.toString();
        if (answer.isEmpty()) throw new IllegalArgumentException("Unplayable title: " + title);
        wordAt = new int[indices.size()];
        for (int i = 0; i < wordAt.length; i++) wordAt[i] = indices.get(i);
    }
    public static boolean isLetter(char ch) { return ch >= 'A' && ch <= 'Z' || ch >= '0' && ch <= '9'; }
    public static int symbolIndex(char ch) { return ch >= 'A' && ch <= 'Z' ? ch-'A' : ch-'0'+26; }
}
