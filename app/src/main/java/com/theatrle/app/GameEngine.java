package com.theatrle.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Android-independent six-guess game; each guess has its own letter budget. */
public final class GameEngine {
    public static final int UNKNOWN = 0, CORRECT = 1, YELLOW = 2, PRESENT = 3, ABSENT = 4;
    public static final int MAX_GUESSES = 6;
    private final List<String> films;
    private final List<String> guesses = new ArrayList<>();
    private final Random random = new Random();
    private MovieTitle movie;
    private String current = "";

    public GameEngine(List<String> films) {
        if (films.isEmpty()) throw new IllegalArgumentException("Empty catalogue");
        this.films = new ArrayList<>(films);
        newGame();
    }
    public void newGame() {
        String previous = movie == null ? null : movie.display;
        String next;
        do { next = films.get(random.nextInt(films.size())); }
        while (films.size() > 1 && new MovieTitle(next).display.equals(previous));
        movie = new MovieTitle(next);
        guesses.clear();
        current = "";
    }
    public MovieTitle getMovie() { return movie; }
    public String getTitle() { return movie.display; }
    public String getAnswer() { return movie.answer; }
    public String getCurrent() { return current; }
    public List<String> getGuesses() { return Collections.unmodifiableList(guesses); }
    public int getGuessCount() { return guesses.size(); }
    public boolean isWon() { return !guesses.isEmpty() && guesses.get(guesses.size() - 1).equals(movie.answer); }
    public boolean isFinished() { return isWon() || guesses.size() == MAX_GUESSES; }
    public void addLetter(char letter) {
        letter = Character.toUpperCase(letter);
        if (!isFinished() && MovieTitle.isLetter(letter) && current.length() < movie.answer.length()) current += letter;
    }
    public void removeLetter() {
        if (!isFinished() && !current.isEmpty()) current = current.substring(0, current.length() - 1);
    }
    public boolean submit() {
        if (isFinished() || current.length() != movie.answer.length()) return false;
        guesses.add(current); current = ""; return true;
    }
    public int[] evaluate(String guess) {
        if (!guess.matches("[A-Z0-9]{" + movie.answer.length() + "}")) throw new IllegalArgumentException("Invalid guess");
        int[] states = new int[guess.length()];
        int[][] available = new int[movie.wordCount][36];
        for (int i = 0; i < movie.answer.length(); i++) available[movie.wordAt[i]][MovieTitle.symbolIndex(movie.answer.charAt(i))]++;
        // Reserve all green matches first.
        for (int i = 0; i < guess.length(); i++) if (guess.charAt(i) == movie.answer.charAt(i)) {
            states[i] = CORRECT;
            available[movie.wordAt[i]][MovieTitle.symbolIndex(guess.charAt(i))]--;
        }
        // Reserve all same-word yellow matches before cross-word purple matches.
        for (int i = 0; i < guess.length(); i++) {
            int letter = MovieTitle.symbolIndex(guess.charAt(i)), word = movie.wordAt[i];
            if (states[i] == UNKNOWN && available[word][letter] > 0) {
                states[i] = YELLOW; available[word][letter]--;
            }
        }
        for (int i = 0; i < guess.length(); i++) {
            if (states[i] != UNKNOWN) continue;
            states[i] = ABSENT;
            for (int word = 0; word < available.length; word++) {
                int letter = MovieTitle.symbolIndex(guess.charAt(i));
                if (word != movie.wordAt[i] && available[word][letter] > 0) {
                    states[i] = PRESENT; available[word][letter]--; break;
                }
            }
        }
        return states;
    }
    public Integer getKeyState(char key) {
        int best = UNKNOWN;
        for (String guess : guesses) {
            int[] states = evaluate(guess);
            for (int i = 0; i < guess.length(); i++) if (guess.charAt(i) == key && priority(states[i]) > priority(best)) best = states[i];
        }
        return best == UNKNOWN ? null : best;
    }
    private static int priority(int state) {
        return state == CORRECT ? 4 : state == YELLOW ? 3 : state == PRESENT ? 2 : state == ABSENT ? 1 : 0;
    }
    public void restore(String title, List<String> history, String input) {
        boolean found = false;
        for (String film : films) if (new MovieTitle(film).display.equals(title)) { found = true; break; }
        if (!found) return;
        movie = new MovieTitle(title); guesses.clear();
        for (String guess : history) {
            if (isFinished() || !guess.matches("[A-Z0-9]{" + movie.answer.length() + "}")) break;
            current = guess; submit();
        }
        current = !isFinished() && input.matches("[A-Z0-9]{0," + movie.answer.length() + "}") ? input : "";
    }
}
