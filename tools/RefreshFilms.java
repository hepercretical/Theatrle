import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** Regenerates the 300-film snapshot from IMDb's public non-commercial datasets. */
public class RefreshFilms {
    static BufferedReader source(String name) throws Exception {
        URLConnection connection = new URL("https://datasets.imdbws.com/" + name + ".tsv.gz").openConnection();
        connection.setConnectTimeout(30000); connection.setReadTimeout(120000);
        return new BufferedReader(new InputStreamReader(new GZIPInputStream(connection.getInputStream(),65536),StandardCharsets.UTF_8),65536);
    }
    public static void main(String[] args) throws Exception {
        Map<String,Integer> votes = new HashMap<>();
        System.out.println("Reading IMDb vote counts...");
        try (BufferedReader reader = source("title.ratings")) {
            String line; reader.readLine();
            while ((line=reader.readLine())!=null) {
                String[] parts=line.split("\t"); int count=Integer.parseInt(parts[2]);
                if(count>=100000) votes.put(parts[0],count);
            }
        }
        System.out.println("Matching feature films against " + votes.size() + " heavily voted titles...");
        List<String[]> films=new ArrayList<>();
        try (BufferedReader reader=source("title.basics")) {
            String line; reader.readLine(); long count=0;
            while ((line=reader.readLine())!=null) {
                int tab=line.indexOf('\t');
                if(tab>0 && votes.containsKey(line.substring(0,tab))) {
                    String[] parts=line.split("\t");
                    if(parts[1].equals("movie") && parts[4].equals("0")) films.add(parts);
                }
                if(++count%3000000==0)System.out.println("Scanned " + count + " titles...");
            }
        }
        films.sort(Comparator.<String[]>comparingInt(f->votes.get(f[0])).reversed().thenComparing(f->f[0]));
        if(films.size()<300 || votes.get(films.get(299)[0])<=100000)throw new IllegalStateException("Insufficient candidate pool");
        StringBuilder csv=new StringBuilder("title\n"), evidence=new StringBuilder("rank\timdb_id\tyear\tvotes\ttitle\n");
        for(int i=0;i<300;i++) {
            String[] f=films.get(i);
            csv.append('"').append(f[2].replace("\"","\"\"")).append("\"\n");
            evidence.append(i+1).append('\t').append(f[0]).append('\t').append(f[5]).append('\t').append(votes.get(f[0])).append('\t').append(f[2]).append('\n');
        }
        Files.createDirectories(Path.of("docs"));
        Files.writeString(Path.of("app/src/main/res/raw/films.csv"),csv,StandardCharsets.UTF_8);
        Files.writeString(Path.of("docs/film-ranking.tsv"),evidence,StandardCharsets.UTF_8);
        System.out.println("Generated 300 films on " + LocalDate.now() + "; rank 300 has " + votes.get(films.get(299)[0]) + " votes.");
    }
}
