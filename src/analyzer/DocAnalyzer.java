/**
 * 
 */
package analyzer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.FileWriter;
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
	//N-gram to be created
	int m_N;
	
	//a list of stopwords
	HashSet<String> m_stopwords;
	
    // all loaded reviews
	ArrayList<Post> m_reviews;
	
	// table of tokens
	HashMap<String, Token> m_stats;

	// table of tokens sorted by TTF
    List<Map.Entry<String, Token>> m_sorted;
	
	Tokenizer m_tokenizer;

	LanguageModel m_langModel;
	
	public DocAnalyzer(String tokenModel, int N) throws InvalidFormatException, FileNotFoundException, IOException {
		m_N = N;
		m_reviews = new ArrayList<Post>();
		m_tokenizer = new TokenizerME(new TokenizerModel(new FileInputStream(tokenModel)));
		m_stopwords = new HashSet<String>();
		m_stats = new HashMap<String, Token>();
	}
	
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
	public void loadDirectory(String folder, String suffix) {
        File dir = new File(folder);
        int size = m_reviews.size();
		for (File f : dir.listFiles()) {
			if (f.isFile() && f.getName().endsWith(suffix))
				analyzeDocument(loadJson(f.getAbsolutePath()));
			else if (f.isDirectory())
				loadDirectory(f.getAbsolutePath(), suffix);
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

    public void analyzeDocument(JSONObject json) {
        try {
            JSONArray jarray = json.getJSONArray("Reviews");

            for(int i=0; i<jarray.length(); i++) {
                Post review = new Post(jarray.getJSONObject(i));

                String[] tokens = tokenize(review.getContent());
                ArrayList<String> bigrams = new ArrayList<String>();
                HashMap<String,Token> vector = new HashMap<String,Token>();

                tokens[0] = snowballStemming(normalize(tokens[0])).trim();

                for (int j=1; j<tokens.length-1; j++) {
                    //normalizing and stemming
                    tokens[j] = snowballStemming(normalize(tokens[j])).trim();

                    //create valid bigrams
                    if(!m_stopwords.contains(tokens[j]) && tokens[j].length()>0 && !m_stopwords.contains(tokens[j-1]) && tokens[j-1].length()>0){
                        String key = tokens[j-1] + "-" + tokens[j];
                        bigrams.add(key);
                        checkDictionary(key);
                    }

                    //create valid unigrams
                    if(!m_stopwords.contains(tokens[j]) && tokens[j].length()>0) {
                        String key = tokens[j];
                        bigrams.add(key);
                        checkDictionary(key);
                    }
                }

                review.setTokens(bigrams.toArray(new String[bigrams.size()]));
                review.setVct(vector);
                m_reviews.add(review);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void checkDictionary(String key) {
        Token token = m_stats.get(key);

        if(token==null) {
            token = new Token(key);
            token.setTTF(1);
            m_stats.put(key,token);
        }
        else {
            token.setTTF(token.getValue()+1);
        }
    }

    public void sortTokensbyTTF() {
        m_sorted = new ArrayList<>(m_stats.entrySet());
        Collections.sort(m_sorted, (e1, e2) -> Double.compare(e2.getValue().getValue(), e1.getValue().getValue()));
    }

    public void exportCSV(String csv_path){

        try {
            FileWriter writer = new FileWriter(csv_path);
            writer.write("Token,Rank,TTF\n");

            for (int i=0; i<m_stats.size(); i++) {
                writer.write(m_sorted.get(i).getKey() + "," + (i+1) + "," + m_sorted.get(i).getValue().getValue() + "\n");
            }

            writer.flush();
            writer.close();
        }
        catch(IOException e) {
            System.out.println(e.toString());
        }
    }

    public void createLanguageModel() {
        m_langModel = new LanguageModel(m_N, m_stats.size());

        for(Post review:m_reviews) {
            String[] tokens = tokenize(review.getContent());

            //TODO: copy analyzeDocument()
            //TODO: update counts in LM structure (to perform MLE)
        }
    }

    public static void main(String[] args) throws InvalidFormatException, FileNotFoundException, IOException {

		String tokenizer_path = args[0];
		String data_path = args[1];
		String stopwords_path = args[2];
		String data_type = args[3];
		String cvs_path = args[4];

		DocAnalyzer analyzer = new DocAnalyzer(tokenizer_path,2);

		analyzer.loadStopwords(stopwords_path);
		analyzer.loadDirectory(data_path, data_type);   // calls analyzeDocument
		analyzer.sortTokensbyTTF();
		analyzer.exportCSV(cvs_path);

    }
}
