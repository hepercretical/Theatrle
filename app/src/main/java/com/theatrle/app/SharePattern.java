package com.theatrle.app;

/** Plain text results preserve the actual word structure and all four feedback colors. */
public final class SharePattern {
    public static String create(GameEngine game) {
        StringBuilder out=new StringBuilder("Theatrle ").append(game.isWon()?game.getGuessCount():"X").append("/6\n");
        String[] icons={"", "\uD83D\uDFE9", "\uD83D\uDFE8", "\uD83D\uDFEA", "\u2B1B"};
        for (String guess:game.getGuesses()) {
            int[] states=game.evaluate(guess); int index=0;
            for (int word=0; word<game.getMovie().words.size();word++) {
                if(word>0)out.append(' ');
                for(char ch:game.getMovie().words.get(word).toCharArray()) if(MovieTitle.isLetter(ch))out.append(icons[states[index++]]);
            }
            out.append('\n');
        }
        return out.toString();
    }
    private SharePattern() { }
}
