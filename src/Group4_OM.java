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
 * BOA framework implementation of the HardHeaded Frequecy Model.
 * 
 * Default: learning coef l = 0.2; learnValueAddition v = 1.0
 * 
 * paper: https://ii.tudelft.nl/sites/default/files/boa.pdf
 */
public class Group4_OM extends OpponentModel {

	/*
	 * the learning coefficient is the weight that is added each turn to the
	 * issue weights which changed. It's a trade-off between concession speed
	 * and accuracy.
	 */
	private double learnCoef;
	/*
	 * value which is added to a value if it is found. Determines how fast the
	 * value weights converge.
	 */
	private int learnValueAddition;
	private int amountOfIssues;
	private double goldenValue;
	private boolean isOpponentCooperative;
	private List<Bid> offers;

	@Override
	public void init(NegotiationSession negotiationSession, Map<String, Double> parameters) {
		this.negotiationSession = negotiationSession;
		if (parameters != null && parameters.get("l") != null) {
			learnCoef = parameters.get("l");
		} else {
			learnCoef = 0.2;
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
		this.offers = new ArrayList<>();

		initializeModel();
	}

	@Override
	public void updateModel(Bid opponentBid, double time) {
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

		// count the number of changes in value
		for (Integer i : lastDiffSet.keySet()) {
			if (lastDiffSet.get(i) == 0)
				numberOfUnchanged++;
		}

		// The total sum of weights before normalization.
		double totalSum = 1D + goldenValue * numberOfUnchanged;
		// The maximum possible weight
		double maximumWeight = 1D - (amountOfIssues) * goldenValue / totalSum;

		// re-weighing issues while making sure that the sum remains 1
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
				 * add constant learnValueAddition to the current preference of
				 * the value to make it more important
				 */
				ValueDiscrete issuevalue = (ValueDiscrete) oppBid.getBid().getValue(issue.getNumber());
				Integer eval = value.getEvaluationNotNormalized(issuevalue);
				value.setEvaluation(issuevalue, (learnValueAddition + eval));
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		determineCooperative(4);
	}

	@Override
	public double getBidEvaluation(Bid bid) {
		double result = 0;
		try {
			result = (opponentUtilitySpace.getUtility(bid) + getTimeUtility(bid)) / 2;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	@Override
	public String getName() {
		return "Group4_OM2";
	}

	@Override
	public Set<BOAparameter> getParameterSpec() {
		Set<BOAparameter> set = new HashSet<BOAparameter>();
		set.add(new BOAparameter("l", 0.2, "The learning coefficient determines how quickly the issue weights are learned"));
		set.add(new BOAparameter("profileDeterminationMoves", 4.0, "Checks after how many non-conceding opponent moves the modeler should consider the opponent as non-cooperative"));
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
				// set all value weights to one (they are normalized when
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
	 * @return
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
	 * Determines the most similar previous bid and how recent that one was made.
	 * Based on the recency it returns a value between 0 and 1 for the time utility.
	 */
	private double getTimeUtility(Bid bid_1) {
		double closest_value = -1;
		double closest_index = 0;
		if (!offers.isEmpty()) {
			for (int i = 0; i < offers.size(); i++) {
				Bid bid_2 = offers.get(i);
				double distance = bid_1.getDistance(bid_2);
				if (distance < closest_value || closest_value == -1){
					closest_index = i;
					closest_value = distance;
				}
			}
			return 1.0 - closest_index / offers.size();
		}
		return 1.0;
	}

	private void determineCooperative(int noMoves) {

		// If the opponent is found at least once to be offensive, then it will always be concidered as offensive.
		if(!isOpponentCooperative) {
			return;
		}

		List<BidDetails> offerHist = negotiationSession.getOpponentBidHistory().getHistory();
		isOpponentCooperative = true;
		if (offerHist.size() > noMoves) {
			for (int i = offerHist.size()-noMoves; i < offerHist.size(); i++) {
				if (offerHist.get(i-1).getMyUndiscountedUtil() != offerHist.get(i).getMyUndiscountedUtil() && !offerHist.get(i-1).getBid().equals(offerHist.get(i).getBid())){
					isOpponentCooperative = false;
					break;
				}
			}
		}
	}

	public boolean getOpponentCooperative() { return isOpponentCooperative; }

	private double getIssueTimeUtility(Issue issue) {
		if (!offers.isEmpty()) {
			for (Bid bid_2 : offers) {
				bid_2.getValue(issue);
			}
			for (Bid bid_2 : offers) {
				Value value1 = bid_2.getValue(issue);
				Value value2 = bid_2.getValue(issue);
				value1.equals(value2);
			}
		}

		return 0;
	}
}