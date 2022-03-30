//package bilateralexamples.boacomponents;

import java.util.List;

import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import genius.core.bidding.BidDetails;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OMStrategy;
import genius.core.boaframework.OpponentModel;
import genius.core.boaframework.SortedOutcomeSpace;
import genius.core.utility.UtilitySpace;

/**
 * This class uses an opponent model to determine the next bid for the opponent,
 * while taking the opponent's preferences into account. The opponent model is
 * used to select the best bid.
 * 
 */
public class Group4_OMS extends OMStrategy {

	/**
	 * when to stop updating the opponentmodel. Note that this value is not
	 * exactly one as a match sometimes lasts slightly longer.
	 */
	double updateThreshold = 1.1;

	private double ownWeight;
	private double opponentWeight;

	/**
	 * Initializes the opponent model strategy. If a value for the parameter t
	 * is given, then it is set to this value. Otherwise, the default value is
	 * used.
	 * 
	 * @param negotiationSession
	 *            state of the negotiation.
	 * @param model
	 *            opponent model used in conjunction with this opponent modeling
	 *            strategy.
	 * @param parameters
	 *            set of parameters for this opponent model strategy.
	 */
	@Override
	public void init(NegotiationSession negotiationSession, OpponentModel model, Map<String, Double> parameters) {
		super.init(negotiationSession, model, parameters);
		if (parameters.get("t") != null) {
			updateThreshold = parameters.get("t").doubleValue();
		} else {
			System.out.println("OMStrategy assumed t = 1.1");
		}
		if (parameters.get("ownWeight") != null) {
			this.ownWeight = parameters.get("ownWeight").doubleValue();
		} else {
			this.ownWeight = 0.7;
		}
		if (parameters.get("opponentWeight") != null) {
			this.opponentWeight = parameters.get("opponentWeight").doubleValue();
		} else {
			this.opponentWeight = 0.3;
		}
	}

	/**
	 * Returns the best bid for the opponent given a set of similarly preferred
	 * bids.
	 * 
	 * @param list
	 *            of the bids considered for offering.
	 * @return bid to be offered to opponent.
	 */
	@Override
	public BidDetails getBid(List<BidDetails> allBids) {

		// 1. If there is only a single bid, return this bid
		if (allBids.size() == 1) {
			return allBids.get(0);
		}
		double bestUtil = -1;
		BidDetails bestBid = allBids.get(0);

		// 2. Check that not all bids are assigned at utility of 0
		// to ensure that the opponent model works. If the opponent model
		// does not work, offer a random bid.
		boolean allWereZero = true;
		// 3. Determine the best bid on the basis of the decision metric
		for (BidDetails bid : allBids) {
			double utilityOpponent = model.getBidEvaluation(bid.getBid());
			if (utilityOpponent > 0.0001) {
				allWereZero = false;
			}
			double utiityAgent = bid.getMyUndiscountedUtil();
			double decisionMetricValue = decisionMetric(utiityAgent, utilityOpponent);
			if(decisionMetricValue > bestUtil) {
				bestBid = bid;
				bestUtil = decisionMetricValue;
			}
		}
		// 4. The opponent model did not work, therefore, offer a random bid.
		if (allWereZero) {
			Random r = new Random();
			return allBids.get(r.nextInt(allBids.size()));
		}
		return bestBid;
	}

	/**
	 * The opponent model may be updated, unless the time is higher than a given
	 * constant.
	 * 
	 * @return true if model may be updated.
	 */
	@Override
	public boolean canUpdateOM() {
		return negotiationSession.getTime() < updateThreshold;
	}

	@Override
	public Set<BOAparameter> getParameterSpec() {
		Set<BOAparameter> set = new HashSet<BOAparameter>();
		set.add(new BOAparameter("t", 1.1 , "Time after which the OM should not be updated"));
		set.add(new BOAparameter("ownWeight", 0.7 , "Weight of the agent's own utility"));
		set.add(new BOAparameter("opponentWeight", 0.3 , "Weight of the opponent's utility"));
		return set;
	}

	@Override
	public String getName() {
		return "Group4 - Opponent Model Strategy";
	}

	/**
	 * Calculate the value of the decision metric given the utilities of the agent and the opponent.
	 * @param ownUtility
	 * 			The value of the utility of the agent.
	 * @param opponentUtility
	 * 			The value of the utility of the opponent.
	 * @return  The value of the decision metric.
	 */
	public double decisionMetric(double ownUtility, double opponentUtility) {
		return (double)(this.ownWeight * ownUtility + this.opponentWeight * opponentUtility) / (this.ownWeight + this.opponentWeight);
	}
}