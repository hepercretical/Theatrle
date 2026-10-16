package com.theatrle.app;

import android.content.Context;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/** Loads string titles from the packaged CSV; never silently substitutes a different film. */
public final class FilmRepository {
    private FilmRepository() { }
    public static ArrayList<String> load(Context context) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                context.getResources().openRawResource(R.raw.films), StandardCharsets.UTF_8))) {
            ArrayList<String> result = FilmCsv.read(reader);
            if (result.size() != 300)
                throw new IllegalStateException("Catalogue must contain exactly 300 films");
            return result;
        } catch (java.io.IOException error) {
            throw new IllegalStateException("Cannot read the film catalogue", error);
        }
    }
}
