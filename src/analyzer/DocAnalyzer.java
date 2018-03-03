/**
 * 
 */
package analyzer;

import java.io.*;
//import java.util.Arrays;
import java.util.*;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import json.JSONArray;
import json.JSONException;
import json.JSONObject;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.InvalidFormatException;
import structures.LanguageModel;
import structures.Post;
import structures.Token;

public class DocAnalyzer {
	int m_N;    	                            //N-gram to be created
    HashSet<String> m_stopwords;                //a list of stopwords
	ArrayList<Post> m_reviews;                  // all loaded reviews
    HashMap<String, Token> m_stats;	            // table of tokens
    List<Map.Entry<String, Token>> m_sorted;   	// table of tokens sorted by TTF
    Tokenizer m_tokenizer;
//    LanguageModel m_langModel;
	
	public DocAnalyzer(String tokenModel, int N) throws InvalidFormatException, FileNotFoundException, IOException {
		m_N = N;
		m_reviews = new ArrayList<>();
		m_tokenizer = new TokenizerME(new TokenizerModel(new FileInputStream(tokenModel)));
		m_stopwords = new HashSet<>();
		m_stats = new HashMap<>();
	}

	// load stopwords
    public void loadStopwords(String filename) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
            String line;

            while ((line = reader.readLine()) != null) {
                line = snowballStemming(normalize(line)).trim();
                if (!line.isEmpty())
                    m_stopwords.add(line);
            }
            reader.close();
            System.out.format("Loading %d stopwords from %s\n", m_stopwords.size(), filename);
        } catch(IOException e){
            System.err.format("[Error]Failed to open file %s!!", filename);
        }
    }
    
    // load a json file
	public JSONObject loadJson(String filename) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
			StringBuffer buffer = new StringBuffer(1024);
			String line;
			
			while((line=reader.readLine())!=null) {
				buffer.append(line);
			}
			reader.close();
			
			return new JSONObject(buffer.toString());
		} catch (IOException e) {
			System.err.format("[Error]Failed to open file %s!", filename);
			e.printStackTrace();
			return null;
		} catch (JSONException e) {
			System.err.format("[Error]Failed to parse json file %s!", filename);
			e.printStackTrace();
			return null;
		}
	}
	
	// recursively load files in a directory
	public void loadDirectory(String folder, String suffix, String purpose) {
        File dir = new File(folder);
        int size = m_reviews.size();
		for (File f : dir.listFiles()) {
			if (f.isFile() && f.getName().endsWith(suffix)) {
			    if(purpose.equals("train"))
                    analyzeDocument(loadJson(f.getAbsolutePath()));
                else if (purpose.equals("test"))
                    encodeTestDocs(loadJson(f.getAbsolutePath()), m_reviews);
            }
			else if (f.isDirectory())
				loadDirectory(f.getAbsolutePath(), suffix, purpose);
		}
		size = m_reviews.size() - size;
        System.out.println("Loading " + size + " review documents from " + folder);
    }

    // tokenize a string
    String[] tokenize(String text) {
        return m_tokenizer.tokenize(text);
    }

    // stem a token
	public String snowballStemming(String token) {
		SnowballStemmer stemmer = new englishStemmer();
		stemmer.setCurrent(token);
		if (stemmer.stem())
			return stemmer.getCurrent();
		else
			return token;
	}

	// normalize a token
	public String normalize(String token) {

        //remove all non-word characters, English punctuation; replace integers, doubles with "NUM"
		return token.replaceAll("\\W+", "")
                .replaceAll("\\p{P}","")
                .toLowerCase()
                .replaceAll("(\\d+(?:\\.\\d+)?)","NUM");
	}

	// training
    public void analyzeDocument(JSONObject json) {
        try {
            JSONArray jarray = json.getJSONArray("Reviews");

            for(int i=0; i<jarray.length(); i++) {
                Post review = new Post(jarray.getJSONObject(i));

                String[] tokens = tokenize(review.getContent());
                ArrayList<String> reviewTokens = new ArrayList<>();

                tokens[0] = snowballStemming(normalize(tokens[0])).trim();

                for (int j=1; j<tokens.length-1; j++) {
                    //normalizing and stemming
                    tokens[j] = snowballStemming(normalize(tokens[j])).trim();

                    //create valid unigrams
                    if(!m_stopwords.contains(tokens[j]) && tokens[j].length()>0) {
                        String key = tokens[j];
                        reviewTokens.add(key);
                        checkDict(key, review.getID());

                        //create valid bigrams
                        if(!m_stopwords.contains(tokens[j-1]) && tokens[j-1].length()>0) {
                            key = tokens[j-1] + "-" + tokens[j];
                            checkDict(key, review.getID());
                        }
                    }
                }

                review.setTokens(reviewTokens.toArray(new String[reviewTokens.size()]));
                m_reviews.add(review);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void checkDict(String key, String id) {
        Token token = m_stats.get(key);

        if(token==null) {
            token = new Token(key, id);
            token.setVal1(1);
            token.setVal2(1);
            m_stats.put(key,token);
        }
        else {
            token.setVal1(token.getVal1()+1);
            if(!token.getID().equals(id)){
                token.setVal2(token.getVal2()+1);
                token.setID(id);
            }
        }
    }

    public void sortDictbyTTF() {
        m_sorted = new ArrayList<>(m_stats.entrySet());
        Collections.sort(m_sorted, (e1, e2) -> Double.compare(e2.getValue().getVal1(), e1.getValue().getVal1()));
    }

    public void sortDictbyDF() {
        m_sorted = new ArrayList<>(m_stats.entrySet());
        Collections.sort(m_sorted, (e1, e2) -> Double.compare(e2.getValue().getVal2(), e1.getValue().getVal2()));
    }

    public void exportCSV(String csv_path, String stat){

	    FileWriter writer = null;

        try {
            writer = new FileWriter(csv_path + stat + "_results.csv");

            if (stat.equals("ttf")) {
                writer.write("Token,Rank,TTF\n");

                for (int i = 0; i < m_stats.size(); i++) {
                    writer.write(m_sorted.get(i).getKey() + "," + (i + 1) + "," + m_sorted.get(i).getValue().getVal1() + "\n");
                }
            }

            if (stat.equals("df")) {
                writer.write("Token,Rank,DF\n");

                for (int i = 0; i < m_stats.size(); i++) {
                    writer.write(m_sorted.get(i).getKey() + "," + (i + 1) + "," + m_sorted.get(i).getValue().getVal2() + "\n");
                }
            }

            writer.flush();
            writer.close();
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        finally {
            if(writer != null) {
                try {
                    writer.close();
                }
                catch(IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void removeLowDF() {

	    int minDF = 50;
	    int i = m_stats.size()-1;
	    // System.out.println("Original dictionary size: " + (i+1));

	    while(m_sorted.get(i).getValue().getVal2() < minDF) {
	        m_stats.remove(m_sorted.get(i).getKey());
	        m_sorted.remove(i);
	        i--;
        }

        // System.out.println("New controlled dictionary size: " + (i+1));
    }

    public void printIDFs() {
	    int size = m_sorted.size();
	    int numIDFs = 50;
	    double idf;
	    double df;
        double n = (double)m_reviews.size();

	    System.out.println("Top " + numIDFs + " N-grams by DF: ");
	    for(int i = 0; i < numIDFs; i++) {
            df = m_sorted.get(i).getValue().getVal2();
	        idf = 1 + Math.log(n/df);
	        System.out.println(m_sorted.get(i).getKey() + "," + idf);
        }

        System.out.println("");
        System.out.println("Bottom 50 N-grams by DF: ");

        for(int i = size-1; i > size-numIDFs-1; i--) {
            df = m_sorted.get(i).getValue().getVal2();
            idf = 1 + Math.log(n/df);
            System.out.println(m_sorted.get(i).getKey() + "," + idf);
        }
    }

    public void printControlledDict() {
	    for (int i=0; i < m_sorted.size(); i++) {
	        System.out.println(m_sorted.get(i).getKey());
        }
    }

    // testing
    public void loadControlledDict(String file_path) {
	    BufferedReader reader = null;
        String line;

	    try {
	        reader = new BufferedReader(new FileReader(file_path));

	        while ((line = reader.readLine()) != null) {
	            m_stats.put(line.trim(), new Token(line.trim()));
            }
        }
        catch(IOException e) {
	        e.printStackTrace();
        }
        finally {
	        if(reader != null) {
	            try {
                    reader.close();
                }
                catch(IOException e) {
	                e.printStackTrace();
                }
            }
        }
    }

    public void encodeTestDocs(JSONObject json, ArrayList<Post> reviews) {
        try {
            JSONArray jarray = json.getJSONArray("Reviews");

            for(int i=0; i<jarray.length(); i++) {
                Post review = new Post(jarray.getJSONObject(i));
                String[] tokens = tokenize(review.getContent());
                HashMap<String, Token> vector = new HashMap<>();
                String key;
                double count;

                tokens[0] = snowballStemming(normalize(tokens[0])).trim();

                for (int j=1; j<tokens.length-1; j++) {
                    //normalizing and stemming
                    tokens[j] = snowballStemming(normalize(tokens[j])).trim();

                    key = tokens[j];
                    if(m_stats.containsKey(key)) {
                        Token t = vector.get(key);
                        vector.put(key, checkVect(key, t, review.getID()));
                    }

                    key = tokens[j-1] + "-" + tokens[j];
                    if(m_stats.containsKey(key)) {
                        Token t = vector.get(key);
                        vector.put(key, checkVect(key, t, review.getID()));
                    }
                }

                review.setVct(vector);
                reviews.add(review);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public Token checkVect(String key, Token t, String id) {

        if(t == null) {
            t = new Token(key, id);
            t.setVal1(1);
            t.setVal2(1);
        }
        else {
            t.setVal1(t.getVal1()+1);
            t.setVal2(1 + Math.log(t.getVal1()));
        }

        return(t);
    }

    public void loadQuery(String file_path) {
        File f = new File(file_path);
	    JSONObject json = loadJson(f.getAbsolutePath());
        ArrayList<Post> queryReviews = new ArrayList<>();

	    encodeTestDocs(json, queryReviews);

	    // TODO: best time to calculate cosine similarity
        // TODO: best way to find 3 most similar test docs to each query doc
        // TODO: clean up code

    }

    public static void main(String[] args) throws InvalidFormatException, FileNotFoundException, IOException {

		String tokenizer_path = args[0];
		String data_path = args[1];
		String stopwords_path = args[2];
		String data_type = args[3];
		String file_path = args[4];
        String test_path = args[5];
        String query_path = args[6];        // TODO: add to IDE command line args

		DocAnalyzer analyzer = new DocAnalyzer(tokenizer_path,2);

        /* 1.1 Understand Zipf's Law */
//        analyzer.loadStopwords(stopwords_path);
//        analyzer.loadDirectory(data_path, data_type, "train");   // calls analyzeDocument
//        analyzer.sortDictbyTTF();
//        analyzer.exportCSV(file_path, "ttf");

        /* 1.2 Construct a Controlled Vocabulary */
//        analyzer.loadStopwords(stopwords_path);
//        analyzer.loadDirectory(data_path, data_type, "train");   // calls analyzeDocument
//        analyzer.sortDictbyDF();
//        analyzer.exportCSV(file_path,"df");
//        analyzer.removeLowDF();
//        analyzer.printIDFs();
//        analyzer.printControlledDict();

        /* 1.3 Compute similarity between documents */
        analyzer.loadControlledDict("controlled_dict.txt");
        analyzer.loadDirectory(test_path, data_type,"test");    //calls encodeTestDocs
        analyzer.loadQuery(query_path);

	}
}
