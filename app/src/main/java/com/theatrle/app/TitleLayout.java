package com.theatrle.app;

import java.util.ArrayList;
import java.util.List;

/** Whole words wrap first; only a word wider than the board splits across rows. */
public final class TitleLayout {
    public static final class Cell {
        public final char display;
        public final int letterIndex, row;
        public final float x;
        Cell(char display, int letterIndex, int row, float x) {
            this.display = display; this.letterIndex = letterIndex; this.row = row; this.x = x;
        }
    }
    public final List<Cell> cells = new ArrayList<>();
    public final List<Float> widths = new ArrayList<>();
    public int rows;
    public static TitleLayout create(MovieTitle movie, float width, float tile, float gap, float wordGap) {
        TitleLayout result = new TitleLayout();
        float x = 0;
        int row = 0, letterIndex = 0;
        width = Math.max(tile, width);
        for (String word : movie.words) {
            float wordWidth = word.length() * (tile + gap) - gap;
            if (x > 0 && x + wordGap + wordWidth > width) {
                result.widths.add(x); row++; x = 0;
            }
            if (x > 0) x += wordGap;
            for (int i = 0; i < word.length(); i++) {
                if (x > 0 && x + tile > width) {
                    result.widths.add(x-gap); row++; x = 0;
                }
                char ch = word.charAt(i);
                result.cells.add(new Cell(ch, MovieTitle.isLetter(ch) ? letterIndex++ : -1, row, x));
                x += tile;
                if (i + 1 < word.length()) x += gap;
            }
        }
        result.widths.add(x); result.rows = row + 1; return result;
    }
}
