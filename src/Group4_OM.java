import genius.core.Bid;
import genius.core.bidding.BidDetails;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OpponentModel;
import genius.core.issue.*;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.Evaluator;
import genius.core.utility.EvaluatorDiscrete;

import java.util.*;
import java.util.Map.Entry;

/**
 * BOA framework implementation of the HardHeaded Frequency Model.
 * Which is modified to take into account the time of offers.
 * 
 * Default settings: l = 0.2; v = 1.0; m = 2.0; w_time = 0.5; w_frequency = 0.5
 * 
 * paper: https://ii.tudelft.nl/sites/default/files/boa.pdf
 */
public class Group4_OM extends OpponentModel {

	// The learning coefficient is the weight that is added each turn to the issue weights which changed.
	// It's a trade-off between concession speed and accuracy.
	private double learnCoef;

	// Value which is added to a value if it is found. Determines how fast the value weights converge.
	private int learnValueAddition;

	// Value which determines how lenient the offensive profile labeling is. Higher values are less lenient.
	private double profileDeterminationMoves;

	// The two parameters that determine the effect of both models.
	private double frequencyWeight;
	private double timeWeight;

	private int amountOfIssues;
	private double goldenValue;
	private boolean isOpponentCooperative;
	private List<Bid> offers;
	private List<Issue> issues;

	@Override
	public void init(NegotiationSession negotiationSession, Map<String, Double> parameters) {
		this.negotiationSession = negotiationSession;
		// Assign parameters to class
		if (parameters != null) {
			if (parameters.get("l") != null) {
				learnCoef = parameters.get("l");
			} else {
				learnCoef = 0.2;
			}
			if (parameters.get("m") != null) {
				profileDeterminationMoves = parameters.get("m");
			} else {
				profileDeterminationMoves = 4.0;
			}
			if (parameters.get("w_frequency") != null) {
				frequencyWeight = parameters.get("w_frequency");
			} else {
				frequencyWeight = 0.5;
			}
			if (parameters.get("w_time") != null) {
				timeWeight = parameters.get("w_time");
			} else {
				timeWeight = 0.5;
			}
		}
		if (timeWeight + frequencyWeight != 1.0) {
			timeWeight = 0.5;
			frequencyWeight = 0.5;
		}

		learnValueAddition = 1;
		opponentUtilitySpace = (AdditiveUtilitySpace) negotiationSession.getUtilitySpace().copy();
		amountOfIssues = opponentUtilitySpace.getDomain().getIssues().size();
		/*
		 * This is the value to be added to weights of unchanged issues before
		 * normalization. Also the value that is taken as the minimum possible
		 * weight, (therefore defining the maximum possible also).
		 */
		goldenValue = learnCoef / amountOfIssues;

		isOpponentCooperative = true;
		offers = new ArrayList<>();
		issues = opponentUtilitySpace.getDomain().getIssues();

		initializeModel();
	}

	@Override
	public void updateModel(Bid opponentBid, double time) {
		// Store the opponent bid in a list of offers
		offers.add(opponentBid);
		if (negotiationSession.getOpponentBidHistory().size() < 2) {
			return;
		}
		int numberOfUnchanged = 0;
		BidDetails oppBid = negotiationSession.getOpponentBidHistory()
				.getHistory()
				.get(negotiationSession.getOpponentBidHistory().size() - 1);
		BidDetails prevOppBid = negotiationSession.getOpponentBidHistory()
				.getHistory()
				.get(negotiationSession.getOpponentBidHistory().size() - 2);
		HashMap<Integer, Integer> lastDiffSet = determineDifference(prevOppBid, oppBid);

		// Count the number of changes in value
		for (Integer i : lastDiffSet.keySet()) {
			if (lastDiffSet.get(i) == 0)
				numberOfUnchanged++;
		}

		// The total sum of weights before normalization.
		double totalSum = 1D + goldenValue * numberOfUnchanged;
		// The maximum possible weight
		double maximumWeight = 1D - (amountOfIssues) * goldenValue / totalSum;

		// Re-weighing issues while making sure that the sum remains 1
		for (Integer i : lastDiffSet.keySet()) {
			Objective issue = opponentUtilitySpace.getDomain().getObjectivesRoot().getObjective(i);
			double weight = opponentUtilitySpace.getWeight(i);
			double newWeight;

			if (lastDiffSet.get(i) == 0 && weight < maximumWeight) {
				newWeight = (weight + goldenValue) / totalSum;
			} else {
				newWeight = weight / totalSum;
			}
			opponentUtilitySpace.setWeight(issue, newWeight);
		}

		// Then for each issue value that has been offered last time, a constant
		// value is added to its corresponding ValueDiscrete.
		try {
			for (Entry<Objective, Evaluator> e : opponentUtilitySpace.getEvaluators()) {
				EvaluatorDiscrete value = (EvaluatorDiscrete) e.getValue();
				IssueDiscrete issue = ((IssueDiscrete) e.getKey());
				/*
				 * Add constant learnValueAddition to the current preference of
				 * the value to make it more important
				 */
				ValueDiscrete issuevalue = (ValueDiscrete) oppBid.getBid().getValue(issue.getNumber());
				Integer eval = value.getEvaluationNotNormalized(issuevalue);
				value.setEvaluation(issuevalue, (learnValueAddition + eval));
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		determineCooperative(profileDeterminationMoves);
	}

	@Override
	public double getBidEvaluation(Bid bid) {
		double result = 0;
		try {
			// Combine the frequency utility with the time utility
			double freqUtil = opponentUtilitySpace.getUtility(bid);
			double issueUtil = getIssueTimeUtility(bid);
			result = freqUtil * frequencyWeight + issueUtil * timeWeight;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	@Override
	public String getName() {
		return "Group4 - Opponent Model";
	}

	@Override
	public Set<BOAparameter> getParameterSpec() {
		Set<BOAparameter> set = new HashSet<BOAparameter>();
		set.add(new BOAparameter("l", 0.2, "The learning coefficient determines how quickly the issue weights are learned"));
		set.add(new BOAparameter("m", 2.0, "Checks after how many non-conceding opponent moves the modeler should consider the opponent as non-cooperative"));
		set.add(new BOAparameter("w_frequency", 0.5 , "Weight of the frequency model utility"));
		set.add(new BOAparameter("w_time", 0.5 , "Weight of time model utility"));
		return set;
	}

	/**
	 * Init to flat weight and flat evaluation distribution
	 */
	private void initializeModel() {
		double commonWeight = 1D / amountOfIssues;

		for (Entry<Objective, Evaluator> e : opponentUtilitySpace
				.getEvaluators()) {

			opponentUtilitySpace.unlock(e.getKey());
			e.getValue().setWeight(commonWeight);
			try {
				// Set all value weights to one (they are normalized when
				// calculating the utility)
				for (ValueDiscrete vd : ((IssueDiscrete) e.getKey())
						.getValues())
					((EvaluatorDiscrete) e.getValue()).setEvaluation(vd, 1);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	/**
	 * Determines the difference between bids. For each issue, it is determined
	 * if the value changed. If this is the case, a 1 is stored in a hashmap for
	 * that issue, else a 0.
	 * 
	 * @param first
	 *            bid of the opponent
	 * @param second
	 *            bid
	 * @return The difference between bids
	 */
	private HashMap<Integer, Integer> determineDifference(BidDetails first,
			BidDetails second) {

		HashMap<Integer, Integer> diff = new HashMap<Integer, Integer>();
		try {
			for (Issue i : opponentUtilitySpace.getDomain().getIssues()) {
				Value value1 = first.getBid().getValue(i.getNumber());
				Value value2 = second.getBid().getValue(i.getNumber());
				diff.put(i.getNumber(), (value1.equals(value2)) ? 0 : 1);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return diff;
	}

	/**
	 * Determines is the opponent is playing cooperative based on the amount of repeat offers.
	 */
	private void determineCooperative(double noMoves) {
		// If the opponent is found at least once to be offensive, then it will always be considered as offensive.
		if(!isOpponentCooperative) {
			return;
		}
		List<BidDetails> offerHist = negotiationSession.getOpponentBidHistory().getHistory();
		isOpponentCooperative = true;
		if (offerHist.size() > noMoves) {
			for (int i = offerHist.size()-(int)noMoves; i < offerHist.size(); i++) {
				// Check if offers are repeated
				if (offerHist.get(i-1).getMyUndiscountedUtil() != offerHist.get(i).getMyUndiscountedUtil() && !offerHist.get(i-1).getBid().equals(offerHist.get(i).getBid())){
					isOpponentCooperative = false;
					break;
				}
			}
		}
	}

	/**
	 * @return If the opponent is cooperative
	 */
	public boolean getOpponentCooperative() { return isOpponentCooperative; }

	/**
	 * Evaluation function to give a time-based utility per issue value.
	 * However, this sometimes fails, so it can switch to the closest general bid if that happens.
	 *
	 * @return Utility of the given bid
	 */
	private double getIssueTimeUtility(Bid bid_1) {
		List<Double> t = new ArrayList<>(Collections.nCopies(amountOfIssues+1, 0.0));
		double closest_value = -1;
		double closest_index = 0;
		if (!offers.isEmpty()) {
			for (int i = offers.size() - 1; i > 0; i--) {
				Bid bid_2 = offers.get(i);
				// Time utility per issue calculation
				for (Issue j : issues) {
					Value value1 = bid_1.getValue(j.getNumber());
					Value value2 = bid_2.getValue(j.getNumber());
					// As in the report, here "&& t.get(j.getNumber()) == 0.0" could be included
					// Here the relative position of a bid is stored in the array
					if (value1.equals(value2)) {
						t.set(j.getNumber(), 1.0 - i / offers.size());
					} else if (t.get(j.getNumber()) != 0.0) {
						t.set(j.getNumber(), 0.0);
					}
				}
				// Time utility per bid calculation
				double distance = bid_1.getDistance(bid_2);
				if (distance < closest_value || closest_value == -1) {
					closest_index = i;
					closest_value = distance;
				}
			}
			// Average the stored relative positions
			OptionalDouble average = t.stream().mapToDouble(a->a).average();
			// Fallback calculation to general bid utility
			if (average.isPresent() && average.getAsDouble() == 0.0) {
				return 1.0 - closest_index / offers.size();
			}
			return average.isPresent() ? average.getAsDouble(): 1.0;
		}
		return 1.0;
	}
}