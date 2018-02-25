/**
 * 
 */
package structures;

import java.util.HashMap;

/**
 * @author hongning
 * Suggested structure for constructing N-gram language model
 */
public class LanguageModel {

	int m_N; // N-gram
	int m_V; // the vocabular size
	HashMap<String, Token> m_model; // sparse structure for storing the maximum likelihood estimation of LM with the seen N-grams
	LanguageModel m_reference; // pointer to the reference language model for smoothing purpose
	
	double m_lambda; // parameter for linear interpolation smoothing
	double m_delta; // parameter for absolute discount smoothing
	
	public LanguageModel(int N, int V) {
		m_N = N;
		m_model = new HashMap<String, Token>();
	}
	
	public double calcMLProb(String token) {
		// return m_model.get(token).getValue(); // should be something like this
		return 0;
	}

	public double calcLinearSmoothedProb(String token) {
		if (m_N>1) 
			return (1.0-m_lambda) * calcMLProb(token) + m_lambda * m_reference.calcLinearSmoothedProb(token);
		else
			return 0; // please use additive smoothing to smooth a unigram language model
	}
	
	//We have provided you a simple implementation based on unigram language model, please extend it to bigram (i.e., controlled by m_N)
	public String sampling() {
		double prob = Math.random(); // prepare to perform uniform sampling
		for(String token:m_model.keySet()) {
			prob -= calcLinearSmoothedProb(token);
			if (prob<=0)
				return token;
		}
		return null; //How to deal with this special case?
	}
	
	//We have provided you a simple implementation based on unigram language model, please extend it to bigram (i.e., controlled by m_N)
	public double logLikelihood(Post review) {
		double likelihood = 0;
		for(String token:review.getTokens()) {
			likelihood += Math.log(calcLinearSmoothedProb(token));
		}
		return likelihood;
	}
}
