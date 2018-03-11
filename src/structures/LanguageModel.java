/**
 * 
 */
package structures;

import java.util.HashMap;
import java.util.*;
import java.util.stream.Collectors;

public class LanguageModel {

	int m_N; // N-gram
	int m_V; // the vocabulary size
	HashMap<String, Token> m_model; // sparse structure for storing the maximum likelihood estimation of LM with the seen N-grams
	LanguageModel m_reference; // pointer to the reference language model for smoothing purpose

	double m_lambda; // parameter for linear interpolation smoothing
	double m_delta; // parameter for absolute discount smoothing

	public LanguageModel(int N, int V, HashMap<String, Token> model, LanguageModel reference) {
		m_N = N;
		m_V = V;
		m_lambda = 0.9;
		m_delta = 0.1;
		m_model = model;
		m_reference = reference;
	}

	public double calcMLProb(String token) {
		if (m_N == 1)
			return (m_model.get(token).getVal1() / (double) m_V);
		else
			return (m_model.get(token).getVal1() / m_reference.m_model.get(m_model.get(token).getPrefix()).getVal1());
	}

	public double calcAdditiveSmoothedProb(String token) {
		//additive smoothing for unigram case
		double count;
		if (m_model.get(token) == null) {
			count = 0.0;
		} else {
			count = m_model.get(token).getVal1();
		}

		double vocabularySize = m_model.size();        // number of unique words in corpus
		double corpusSize = (double) m_V;           // number of words in corpus

		double prob = (count + m_delta) / (corpusSize + m_delta * vocabularySize);
		return prob;
	}

	public double calcLinearSmoothedProb(String token) {
		if (m_N > 1 && !m_model.containsKey(token)) {
			return (m_reference.calcAdditiveSmoothedProb(token.split("-")[0]) * m_reference.calcAdditiveSmoothedProb(token.split("-")[1]));
		}
		else if (m_N > 1) {
			return (m_lambda * calcAdditiveSmoothedProb(token) + (1 - m_lambda) * m_reference.calcLinearSmoothedProb(m_model.get(token).getPostfix()));
		}
		else {
			return m_lambda*calcAdditiveSmoothedProb(token);
		}
	}
	public double calcAbsDiscountingProb(String token, int S) {
		if (m_N > 1 && !m_model.containsKey(token))
			return calcAdditiveSmoothedProb(token.split("-")[0])*calcAdditiveSmoothedProb(token.split("-")[1]);
		else {
			String prefix = m_model.get(token).getPrefix();
			String postfix = m_model.get(token).getPostfix();

			double bigramCount = m_model.get(token).getVal1();
			double prefixCount = m_reference.m_model.get(prefix).getVal1();

			double lambda = m_delta * S / prefixCount;
			return Math.max(bigramCount - m_delta, 0) / prefixCount + lambda * m_reference.calcAdditiveSmoothedProb(postfix);
		}
	}

	public String samplingLinear(String prev) {
		double prob = Math.random();

		if (m_N > 1) {
			for (String token : m_model.keySet()) {
				if (m_model.get(token).getPrefix().equals(prev)) {
					prob -= calcLinearSmoothedProb(token);
					if (prob <= 0)
						return token;
				}
			}
		} else {
			for (String token : m_model.keySet()) {
				prob -= calcLinearSmoothedProb(token);
				if (prob <= 0)
					return token;
			}
		}
		return samplingLinear(prev);
	}

	public String samplingAD(int S, ArrayList<String> submodel, double p) {
		double prob;

		if (p == -1.0)
			prob = Math.random(); // prepare to perform uniform sampling
		else
			prob = p;

		if (m_N > 1 && p == -1.0 && S != 0) {
			for (String token : submodel) {
				prob -= calcAbsDiscountingProb(token, S);
				if (prob <= 0)
					return token;
			}
		} else {
			for (String token : m_model.keySet()) {
				prob -= calcAbsDiscountingProb(token, S);
				if (prob <= 0)
					return token;
			}
		}
		return samplingAD(S, submodel, p);
	}

	//We have provided you a simple implementation based on unigram language model, please extend it to bigram (i.e., controlled by m_N)
	public double logLikelihood(Post review, String smoothingMethod, HashMap<String, Token> dict) {

		double prob;
		double likelihood = 0.0;
		// additive smoothing case
		if (smoothingMethod.equals("a")) {
			for (String token : review.getTokens()) {
				likelihood += Math.log(calcAdditiveSmoothedProb(token));
			}
		}
		// linear interpolation smoothing case
		else if (smoothingMethod.equals("l")) {
			if(review.getTokens().length > 2) {
				String prev = review.getTokens()[0];
				for (int i = 1; i < review.getTokens().length; i++) {
					String current = review.getTokens()[i];
					String token = prev + "-" + current;
					likelihood += Math.log(calcLinearSmoothedProb(token));
					prev = current;
				}
			}
		}
		// absolute discount smoothing case
		else {
			for (int i=1; i<review.getTokens().length-1; i++) {
				String prev = review.getTokens()[i-1];
				String current = review.getTokens()[i];
				String token = prev + "-" + current;

				ArrayList<String> nexts = dict.keySet().stream()
						.filter(key -> prev.equals(dict.get(key).getPrefix()))
						.collect(Collectors.toCollection(ArrayList::new));
				int S = nexts.size(); // number of seen unique postfixes that follow target prefix

				likelihood += Math.log(calcAbsDiscountingProb(token, S));
			}
		}

		return likelihood;
	}
}
