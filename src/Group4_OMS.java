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

		// If there is only a single bid, return this bid.
		if (allBids.size() == 1) {
			return allBids.get(0);
		}

		// Make a list with good bids, which will contain eventually the bids with the highest utility for the own agent.
		int goodBidsSize = (allBids.size() / 2) + 1;
		BidDetails[] goodBids = initializeGoodBids(goodBidsSize, allBids.get(0));

		for (BidDetails bid : allBids) {
			double utilityAgent = bid.getMyUndiscountedUtil();

			if(goodBids[0] == null || utilityAgent > goodBids[0].getMyUndiscountedUtil()) {
				goodBids[0] = bid;
				sortGoodBids(goodBids);
			}
		}
		
		// Check that not all bids are assigned at utility of 0
		// to ensure that the opponent model works. If it works, find the 
		// highest utility of the opponent and save that bid.
		boolean allWereZero = true;
		double bestUtil = -1;
		BidDetails bestBid = goodBids[0];

		for (BidDetails bid : goodBids) {
			double utilityOpponent = model.getBidEvaluation(bid.getBid());
			if (utilityOpponent > 0.0001) {
				allWereZero = false;
			}
			if (utilityOpponent > bestUtil) {
				bestBid = bid;
				bestUtil = utilityOpponent;
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
		return set;
	}

	@Override
	public String getName() {
		return "Group4 - Opponent Model Strategy";
	}

	/**
	 * This initializes the array of good bids, where every bid has an utility of -1.
	 * @param size The length of the array of good bids.
	 * @param firstBid This is the first bid in the array allBids, which is used to initialize.
	 * @return returns the initialized array.
	 */
	public BidDetails[] initializeGoodBids(int size, BidDetails firstBid) {
		BidDetails[] bids = new BidDetails[size];
		firstBid.setMyUndiscountedUtil(-1);
		for (BidDetails bid : bids) {
			bid = firstBid;
		}
		return bids;
	}

	/**
	 * This sorts the array of good bids, from low to high.
	 * By doing this, it can easily be checked if a new bid should be placed in this array.
	 * @param goodBids The array that contains all good bids that should be considered for the opponent's utility.
	 */
	public void sortGoodBids(BidDetails[] goodBids) {
		BidDetails bidZero = goodBids[0];
		double utilityBidZero = bidZero.getMyUndiscountedUtil();
		for (int i = 1; i < goodBids.length; i++) {
			if(goodBids[i] == null || utilityBidZero > goodBids[i].getMyUndiscountedUtil()) {
				goodBids[i-1] = goodBids[i];
				goodBids[i] = bidZero;
			}
			else {
				break;
			}
		}
	}
}
