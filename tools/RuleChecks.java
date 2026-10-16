import com.theatrle.app.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Dependency-free regressions, runnable with the project's Java 17 toolchain. */
public final class RuleChecks {
    static int checks;
    static void check(boolean value,String message) { checks++; if(!value)throw new AssertionError(message); }
    static GameEngine game(String title) { return new GameEngine(Collections.singletonList(title)); }
    static void score(String title,String guess,int...expected) {
        check(Arrays.equals(game(title).evaluate(guess),expected),title+" / "+guess+" feedback mismatch");
    }
    public static void main(String[] args) throws Exception {
        int green=GameEngine.CORRECT,yellow=GameEngine.YELLOW,purple=GameEngine.PRESENT,grey=GameEngine.ABSENT;
        score("AB CA","CAAA",purple,yellow,grey,green);
        score("AA BB","AAAA",green,green,grey,grey);
        score("AB BA","BAAB",yellow,yellow,yellow,yellow);
        score("AB CD","CDAB",purple,purple,purple,purple);
        score("AAB BC","BAAAA",yellow,green,yellow,grey,grey);
        score("SPIDER-MAN","SPIDERMAN",green,green,green,green,green,green,green,green,green);
        score("1917","1917",green,green,green,green);
        GameEngine loss=game("AB CD");
        check(!loss.submit(),"Incomplete guess accepted");
        for(int i=0;i<6;i++) { for(char ch:"ZZZZ".toCharArray())loss.addLetter(ch);check(loss.submit(),"Guess rejected"); }
        check(loss.isFinished()&&!loss.isWon()&&loss.getGuessCount()==6,"Six-guess loss");
        loss.addLetter('A');loss.removeLetter();check(!loss.submit()&&loss.getCurrent().isEmpty(),"Finished input changed");
        GameEngine win=game("AB CD");for(char ch:"ABCD".toCharArray())win.addLetter(ch);check(win.submit()&&win.isWon(),"Win");
        GameEngine restore=game("AB CD");restore.restore("AB CD",Arrays.asList("ZZZZ"),"AB");
        check(restore.getGuessCount()==1&&restore.getCurrent().equals("AB"),"Restore lost input");
        restore.removeLetter();check(restore.getCurrent().equals("A"),"Delete");
        restore.newGame();check(restore.getGuesses().isEmpty()&&restore.getCurrent().isEmpty(),"Reset");
        ArrayList<String> csv=FilmCsv.read(new BufferedReader(new StringReader("title\n\"The Good, the Bad and the Ugly\"\n\"1917\"\n\"A \"\"Quoted\"\" Title\"\n")));
        check(csv.get(0).equals("The Good, the Bad and the Ugly")&&csv.get(1).equals("1917")&&csv.get(2).equals("A \"Quoted\" Title"),"CSV string preservation");
        Random random=new Random(17);
        for(int trial=0;trial<10000;trial++) {
            StringBuilder title=new StringBuilder(),guess=new StringBuilder();
            for(int i=0;i<8;i++) { if(i==3||i==5)title.append(' '); title.append((char)('A'+random.nextInt(3))); guess.append((char)('A'+random.nextInt(4))); }
            GameEngine engine=game(title.toString());int[] states=engine.evaluate(guess.toString());
            for(char ch='A';ch<='D';ch++) {
                int answerCount=0,colored=0,guessCount=0;
                for(int i=0;i<8;i++) { if(engine.getAnswer().charAt(i)==ch)answerCount++; if(guess.charAt(i)==ch) {guessCount++;if(states[i]!=grey)colored++;} }
                check(colored==Math.min(answerCount,guessCount),"Letter count exceeded or unused");
                for(int word=0;word<3;word++) {
                    int answerInWord=0,unmatchedGuess=0,greens=0,yellows=0;
                    for(int i=0;i<8;i++)if(engine.getMovie().wordAt[i]==word) {
                        if(engine.getAnswer().charAt(i)==ch)answerInWord++;
                        if(guess.charAt(i)==ch) {if(states[i]==green)greens++;else unmatchedGuess++;if(states[i]==yellow)yellows++;}
                    }
                    check(yellows==Math.min(answerInWord-greens,unmatchedGuess),"Yellow priority/budget");
                }
            }
            for(int i=0;i<8;i++)check((states[i]==green)==(guess.charAt(i)==engine.getAnswer().charAt(i)),"Green priority");
        }
        List<String> films=FilmCsv.read(Files.newBufferedReader(Path.of("app/src/main/res/raw/films.csv"),StandardCharsets.UTF_8));
        check(films.size()==300,"Catalogue size");
        for(String film:films) for(float width:new float[]{240,320,360,480,720}) {
            MovieTitle title=new MovieTitle(film);TitleLayout layout=TitleLayout.create(title,width,48,5,24);int next=0;
            for(float used:layout.widths)check(used<=width+.01,"Row overflow: "+film);
            for(TitleLayout.Cell cell:layout.cells) {
                check(cell.x>=0&&cell.x+48<=width+.01,"Tile overflow: "+film);
                if(cell.letterIndex>=0)check(cell.letterIndex==next++,"Lost title letter");
            }
            check(next==title.answer.length(),"Layout changed title length");
        }
        TitleLayout spaced=TitleLayout.create(new MovieTitle("THE-MAN"),480,48,5,24);
        check(spaced.cells.get(3).x-(spaced.cells.get(2).x+48)==24,"Missing physical word gap");
        TitleLayout wrapped=TitleLayout.create(new MovieTitle("AB CDEF"),240,48,5,24);
        check(wrapped.cells.get(2).row==1&&wrapped.cells.get(5).row==1,"Word should wrap intact");
        System.out.println("Passed "+checks+" assertions, including 10,000 randomized scoring cases and 1,500 catalogue layouts.");
    }
}
