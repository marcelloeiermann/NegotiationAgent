import genius.core.boaframework.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This Acceptance Condition will accept an opponent bid if the utility is
 * higher than the bid the agent is ready to present.
 *
 * Decoupling Negotiating Agents to Explore the Space of Negotiation Strategies
 * T. Baarslag, K. Hindriks, M. Hendrikx, A. Dirkzwager, C.M. Jonker
 *
 */
public class Group4_AS extends AcceptanceStrategy {

    private double a;
    private double b;
    private double t;
    private double c;
    private double tt;

    /**
     * Empty constructor for the BOA framework.
     */
    public Group4_AS() {
    }

    public Group4_AS(NegotiationSession negoSession, OfferingStrategy strat,
                  double alpha, double beta, double c, double tau, double tt) {
        this.negotiationSession = negoSession;
        this.offeringStrategy = strat;
        this.a = alpha;
        this.b = beta;
        this.c = c;
        this.t = tau;
        this.tt = tt;

    }

    @Override
    public void init(NegotiationSession negoSession, OfferingStrategy strat,
                     OpponentModel opponentModel, Map<String, Double> parameters)
            throws Exception {
        this.negotiationSession = negoSession;
        this.offeringStrategy = strat;
        if (parameters.get("a") != null || parameters.get("b") != null) {
            a = parameters.get("a");
            b = parameters.get("b");
            c = parameters.get("c");
            t = parameters.get("t");
            tt = parameters.get("tt");

        } else {
            a = 1.02;
            b = 0;
            c = 0.95;
            t = 0.99;
            tt = 0.8;


        }

    }

    @Override
    public String printParameters() {
        String str = "[a: " + a + " b: " + b + " c: " + c + " t: " + t + "]";
        return str;
    }

    //Determines if an opponents bid will be accepted based on the utility and time passed
    @Override
    public Actions determineAcceptability() {
        double nextMyBidUtil = offeringStrategy.getNextBid()
                .getMyUndiscountedUtil();
        double lastOpponentBidUtil = negotiationSession.getOpponentBidHistory()
                .getLastBidDetails().getMyUndiscountedUtil();
        //Bid is accepted if one of three conditions apply (chapter 2.1 in report)
        double maxBidUtil =  negotiationSession.getOpponentBidHistory().getBestBidDetails().getMyUndiscountedUtil();
        if ((a * lastOpponentBidUtil + b >= nextMyBidUtil) || ((negotiationSession.getTime() >= t)
                && ( lastOpponentBidUtil >= maxBidUtil * 0.9)) || (lastOpponentBidUtil >= c)) {
            return Actions.Accept;
        }
        return Actions.Reject;
    }

    @Override
    public Set<BOAparameter> getParameterSpec() {

        Set<BOAparameter> set = new HashSet<BOAparameter>();
        set.add(new BOAparameter("a", 1.02,
                "Accept when the opponent's utility * a + b is greater than the utility of our current bid"));
        set.add(new BOAparameter("b", 0.0,
                "Accept when the opponent's utility * a + b is greater than the utility of our current bid"));
        set.add(new BOAparameter("t", 0.99,
                "Accept when the passed time of the round is higher or equal to t " +
                        "(and bid is at least tt% of maximum given bid)"));
        set.add(new BOAparameter("tt", 0.8,
                "After t passed, accepts bid if it is at least tt% of maximum given bid"));
        set.add(new BOAparameter("c", 0.95,
                "Accept when the opponent's utility is higher than c. (c should be set pretty high)"));

        return set;
    }

    @Override
    public String getName() {
        return "Group4_Acceptance_Strategy_v7";
    }
}