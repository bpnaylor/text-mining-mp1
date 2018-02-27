package structures;

public class Token {

	int m_id; // the numerical ID you assigned to this token/N-gram
	public int getID() {
		return m_id;
	}

	public void setID(int id) {
		this.m_id = id;
	}

	String m_token; // the actual text content of this token/N-gram
	public String getToken() {
		return m_token;
	}

	public void setToken(String token) {
		this.m_token = token;
	}

	double m_ttf; // frequency or count of this token/N-gram
	public double getValue() {
		return m_ttf;
	}

	public void setTTF(double ttf) {
		this.m_ttf = ttf;
	}

	double m_df; // frequency or count of this token/N-gram
	public double getDF() {
		return m_ttf;
	}

	public void setDF(double df) {
		this.m_df = df;
	}
	//default constructor
	public Token(String token) {
		m_token = token;
		m_id = -1;
		m_ttf = 0;
		m_df = 0;
	}

	//default constructor
	public Token(int id, String token) {
		m_token = token;
		m_id = id;
		m_ttf = 0;
		m_df = 0;
	}
}
