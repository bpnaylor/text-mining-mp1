package structures;

public class Token {

	String m_id; // the numerical ID you assigned to this token/N-gram
	public String getID() {
		return m_id;
	}

	public void setID(String id) {
		this.m_id = id;
	}

	String m_token; // the actual text content of this token/N-gram
	public String getToken() {
		return m_token;
	}

	public void setToken(String token) {
		this.m_token = token;
	}

	double m_val1; // frequency or count of this token/N-gram
	public double getVal1() {
		return m_val1;
	}

	public void setVal1(double v1) {
		this.m_val1 = v1;
	}

	double m_val2; // frequency or count of this token/N-gram
	public double getVal2() {
		return m_val2;
	}

	public void setVal2(double v2) {
		this.m_val2 = v2;
	}

    //default constructor
	public Token(String token) {
		m_token = token;
		m_id = "-1";
		m_val1 = 0;
		m_val2 = 0;
	}

	//default constructor
	public Token(String token, String id) {
		m_token = token;
		m_id = id;
		m_val1 = 0;
		m_val2 = 0;
	}
}
