/**
 * 
 */
package structures;

import java.util.HashMap;

import json.JSONException;
import json.JSONObject;

public class Post {

    double[] m_similarities;

	//unique review ID from Yelp
	String m_ID;		
	public void setID(String ID) {
		m_ID = ID;
	}
	
	public String getID() {
		return m_ID;
	}

	//author's displayed name
	String m_author;	
	public String getAuthor() {
		return m_author;
	}

	public void setAuthor(String author) {
		this.m_author = author;
	}
	
	//author's location
	String m_location;
	public String getLocation() {
		return m_location;
	}

	public void setLocation(String location) {
		this.m_location = location;
	}

	//review text content
	String m_content;
	public String getContent() {
		return m_content;
	}

	public void setContent(String content) {
		if (!content.isEmpty())
			this.m_content = content;
	}
	
	public boolean isEmpty() {
		return m_content==null || m_content.isEmpty();
	}

	//timestamp of the post
	String m_date;
	public String getDate() {
		return m_date;
	}

	public void setDate(String date) {
		this.m_date = date;
	}
	
	//overall rating to the business in this review
	double m_rating;
	public double getRating() {
		return m_rating;
	}

	public void setRating(double rating) {
		this.m_rating = rating;
	}

	public Post(String ID) {
		m_ID = ID;
	}
	
	String[] m_tokens; // we will store the tokens 
	public String[] getTokens() {
		return m_tokens;
	}
	
	public void setTokens(String[] tokens) {
		m_tokens = tokens;
	}
	
	HashMap<String, Token> m_vector; // suggested sparse structure for storing the vector space representation with N-grams for this document
	public HashMap<String, Token> getVct() {
		return m_vector;
	}
	
	public void setVct(HashMap<String, Token> vct) {
		m_vector = vct;
	}

	public double similiarity(Post p) {
		double numerator = 0.0;
		double denominatorA = 0.0;
		double denominatorB = 0.0;

		for (String i : m_vector.keySet()) {
			numerator += m_vector.get(i).getVal1() * p.getVct().get(i).getVal1();
			denominatorA += m_vector.get(i).getVal1() * m_vector.get(i).getVal1();
            denominatorB += p.getVct().get(i).getVal1() * p.getVct().get(i).getVal1();
		}

		denominatorA = Math.sqrt(denominatorA);
		denominatorB = Math.sqrt(denominatorB);

		if(denominatorA != 0.0 && denominatorB !=0.0)
		    return(numerator/(denominatorA*denominatorB));
		else {
		    return(0.0);
        }
	}
	
	public Post(JSONObject json) {
		try {
			m_ID = json.getString("ReviewID");
			setAuthor(json.getString("Author"));
			
			setDate(json.getString("Date"));			
			setContent(json.getString("Content"));
			setRating(json.getDouble("Overall"));
			setLocation(json.getString("Author_Location"));
			m_similarities = new double[5];

		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	public JSONObject getJSON() throws JSONException {
		JSONObject json = new JSONObject();
		
		json.put("ReviewID", m_ID);//must contain
		json.put("Author", m_author);//must contain
		json.put("Date", m_date);//must contain
		json.put("Content", m_content);//must contain
		json.put("Overall", m_rating);//must contain
		json.put("Author_Location", m_location);//must contain
		
		return json;
	}
}
