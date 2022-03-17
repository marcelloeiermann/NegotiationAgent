package bilateralexamples.boacomponents;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import genius.core.boaframework.AcceptanceStrategy;
import genius.core.boaframework.Actions;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OfferingStrategy;
import genius.core.boaframework.OpponentModel;

/**
 * This Acceptance Condition will accept an opponent bid if the utility is
 * higher than the bid the agent is ready to present.
 *
 * Decoupling Negotiating Agents to Explore the Space of Negotiation Strategies
 * T. Baarslag, K. Hindriks, M. Hendrikx, A. Dirkzwager, C.M. Jonker
 *
 */
public class AC_new extends AcceptanceStrategy {

    private double a;
    private double b;
    private double t;
    private double c;

    /**
     * Empty constructor for the BOA framework.
     */
    public AC_new() {
    }

    public AC_new(NegotiationSession negoSession, OfferingStrategy strat,
                   double alpha, double beta, double c, double tau) {
        this.negotiationSession = negoSession;
        this.offeringStrategy = strat;
        this.a = alpha;
        this.b = beta;
        this.c = c;
        this.t = tau;

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
        } else {
            a = 1.02;
            b = 0;
            c = 0.98;
            t = 0.99;
        }
    }

    @Override
    public String printParameters() {
        String str = "[a: " + a + " b: " + b + " c: " + c + " t: " + t + "]";
        return str;
    }

    @Override
    public Actions determineAcceptability() {
        double nextMyBidUtil = offeringStrategy.getNextBid()
                .getMyUndiscountedUtil();
        double lastOpponentBidUtil = negotiationSession.getOpponentBidHistory()
                .getLastBidDetails().getMyUndiscountedUtil();

        if ((a * lastOpponentBidUtil + b >= nextMyBidUtil) || (negotiationSession.getTime() >= t) || (lastOpponentBidUtil >= c)) {
            return Actions.Accept;
        }
        return Actions.Reject;
    }

    @Override
    public Set<BOAparameter> getParameterSpec() {

        Set<BOAparameter> set = new HashSet<BOAparameter>();
        set.add(new BOAparameter("a", 1.0,
                "Accept when the opponent's utility * a + b is greater than the utility of our current bid"));
        set.add(new BOAparameter("b", 0.0,
                "Accept when the opponent's utility * a + b is greater than the utility of our current bid"));
        set.add(new BOAparameter("t", 0.0,
                "Accept when the passed time of the round is higher or equal to t"));
        set.add(new BOAparameter("c", 0.0,
                "Accept when the opponent's utility is higher than c. (c should be set pretty high)"));

        return set;
    }

    @Override
    public String getName() {
        return "AC_new strategy";
    }
}