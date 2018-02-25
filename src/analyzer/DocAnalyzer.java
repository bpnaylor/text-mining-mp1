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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;
import org.tartarus.snowball.ext.porterStemmer;

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
	
	//you can store the loaded reviews in this arraylist for further processing
	ArrayList<Post> m_reviews;
	
	//you might need something like this to store the counting statistics for validating Zipf's and computing IDF
	HashMap<String, Token> m_stats;	
	
	//we have also provided a sample implementation of language model in src.structures.LanguageModel
	Tokenizer m_tokenizer;
	
	//this structure is for language modeling
	LanguageModel m_langModel;
	
	public DocAnalyzer(String tokenModel, int N) throws InvalidFormatException, FileNotFoundException, IOException {
		m_N = N;
		m_reviews = new ArrayList<Post>();
		m_tokenizer = new TokenizerME(new TokenizerModel(new FileInputStream(tokenModel)));
		m_stopwords = new HashSet<String>();
		m_stats = new HashMap<String, Token>();
	}
	
	//sample code for loading a list of stopwords from file
	//you can manually modify the stopword file to include your newly selected words
	public void LoadStopwords(String filename) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
			String line;

			while ((line = reader.readLine()) != null) {
				//it is very important that you perform the same processing operation to the loaded stopwords
				//otherwise it won't be matched in the text content
				line = SnowballStemming(Normalization(line)).trim();
				if (!line.isEmpty())
					m_stopwords.add(line);
			}
			reader.close();
			System.out.format("Loading %d stopwords from %s\n", m_stopwords.size(), filename);
		} catch(IOException e){
			System.err.format("[Error]Failed to open file %s!!", filename);
		}
	}
	
	public void analyzeDocument(JSONObject json) {		
		try {
			JSONArray jarray = json.getJSONArray("Reviews");

            for(int i=0; i<jarray.length(); i++) {
				Post review = new Post(jarray.getJSONObject(i));
				
				String[] tokens = Tokenize(review.getContent());
                ArrayList<String> bigrams = new ArrayList<String>();
                HashMap<String,Token> vector = new HashMap<String,Token>();

                tokens[0] = SnowballStemming(Normalization(tokens[0])).trim();

                for (int j=1; j<tokens.length; j++) {
                    //normalizing and stemming
                    tokens[j] = SnowballStemming(Normalization(tokens[j])).trim();

//                    //uncomment for unigram version
//                    if(!m_stopwords.contains(tokens[j]) && tokens[j].length()>0) {
//                        bigrams.add(tokens[j]);
//                    }

                    //create bigram if both tokens non-empty and non-stopword
                    if(!m_stopwords.contains(tokens[j]) && tokens[j].length()>0 && !m_stopwords.contains(tokens[j-1]) && tokens[j-1].length()>0){
                        bigrams.add(tokens[j-1] + "-" + tokens[j]);
                    }
                }

                //create vector
                for (int j=0; j<bigrams.size(); j++) {
                    String key = bigrams.get(j);
                    Token m = m_stats.get(key);
                    Token t = vector.get(key);

                    if (t == null) {
                        t = new Token(key);
                        t.setValue(1);
                        vector.put(key,t);
                    } else {
                        t.setValue(t.getValue()+1);
                    }

                    if (m==null) {
                        m = new Token(key);
                        m.setValue(1);
                        m_stats.put(key,m);
                    }
                    else {
                        m.setValue(m.getValue()+1);
                    }
                }

//                System.out.println(bigrams.size() - vector.size());
//                System.out.println(m_stats.size());

                review.setTokens(bigrams.toArray(new String[bigrams.size()]));
                review.setVct(vector);
                m_reviews.add(review);
            }
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	public void createLanguageModel() {
		m_langModel = new LanguageModel(m_N, m_stats.size());
		
		for(Post review:m_reviews) {
			String[] tokens = Tokenize(review.getContent());

			//TODO: copy analyzeDocument()
			//TODO: update counts in LM structure (to perform MLE)
		}
	}
	
	//sample code for loading a json file
	public JSONObject LoadJson(String filename) {
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
	
	// sample code for demonstrating how to recursively load files in a directory 
	public void LoadDirectory(String folder, String suffix) {
        File dir = new File(folder);
        int size = m_reviews.size();
		for (File f : dir.listFiles()) {
			if (f.isFile() && f.getName().endsWith(suffix))
				analyzeDocument(LoadJson(f.getAbsolutePath()));
			else if (f.isDirectory())
				LoadDirectory(f.getAbsolutePath(), suffix);
		}
		size = m_reviews.size() - size;
        System.out.println("Loading " + size + " review documents from " + folder);
    }

	//sample code for demonstrating how to use Snowball stemmer
	public String SnowballStemming(String token) {
		SnowballStemmer stemmer = new englishStemmer();
		stemmer.setCurrent(token);
		if (stemmer.stem())
			return stemmer.getCurrent();
		else
			return token;
	}
	
	//sample code for demonstrating how to use Porter stemmer
	public String PorterStemming(String token) {
		porterStemmer stemmer = new porterStemmer();
		stemmer.setCurrent(token);
		if (stemmer.stem())
			return stemmer.getCurrent();
		else
			return token;
	}
	
	//sample code for demonstrating how to perform text normalization
	//you should implement your own normalization procedure here
	public String Normalization(String token) {
		// remove all non-word characters
		// please change this to removing all English punctuation
		token = token.replaceAll("\\W+", "");
		token = token.replaceAll("\\p{P}","");

		// convert to lower case
		token = token.toLowerCase(); 
		
		// add a line to recognize integers and doubles via regular expression
        // and convert the recognized integers and doubles to a special symbol "NUM"
        token = token.replaceAll("(\\d+(?:\\.\\d+)?)","NUM");

		return token;
	}
	
	String[] Tokenize(String text) {
		return m_tokenizer.tokenize(text);
	}

	public void TokenizerDemon(String text) {
		System.out.format("Token\tNormalization\tSnowball Stemmer\tPorter Stemmer\n");
		for(String token:m_tokenizer.tokenize(text)){
			System.out.format("%s\t%s\t%s\t%s\n", token, Normalization(token), SnowballStemming(token), PorterStemming(token));
		}
	}

	public static void main(String[] args) throws InvalidFormatException, FileNotFoundException, IOException {

		String tokenizer_path = args[0];
		String data_path = args[1];
        String stopwords_path = args[2];
        String data_type = args[3];

		DocAnalyzer analyzer = new DocAnalyzer(tokenizer_path,2);

		analyzer.LoadStopwords(stopwords_path);

        //entry point to deal with a collection of documents
		analyzer.LoadDirectory(data_path, data_type);
    }
}
