import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import genius.core.bidding.BidDetails;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.NoModel;
import genius.core.boaframework.OMStrategy;
import genius.core.boaframework.OfferingStrategy;
import genius.core.boaframework.OpponentModel;
import genius.core.boaframework.SortedOutcomeSpace;

/**
 * This is an abstract class used to implement a TimeDependentAgent Strategy
 * adapted from [1] [1] S. Shaheen Fatima Michael Wooldridge Nicholas R.
 * Jennings Optimal Negotiation Strategies for Agents with Incomplete
 * Information http://eprints.ecs.soton.ac.uk/6151/1/atal01.pdf
 *
 * The default strategy was extended to enable the usage of opponent models.
 */
public class Group4_BS extends OfferingStrategy {

    /** Outcome space */
    private SortedOutcomeSpace outcomespace;

    /** Sets the threshold regarding when to
     *  scare the opponent (eg. 90% of the time)
     *  Takes values between 0 and 1
     */
    private double scareThreshold;

    /** Used to determine whether the agent should be cooperative or not */
    private boolean isOpponentCooperative;

    /**
     * Method which initializes the agent by setting all parameters.
     */
    @Override
    public void init(NegotiationSession negoSession,
                     OpponentModel model,
                     OMStrategy oms,
                     Map<String, Double> parameters) {

        super.init(negoSession, parameters);
        this.negotiationSession = negoSession;
        outcomespace = new SortedOutcomeSpace(negotiationSession.getUtilitySpace());
        negotiationSession.setOutcomeSpace(outcomespace);

        // Assign parameters to class
        if (parameters.get("scareThreshold") != null)
            this.scareThreshold = parameters.get("scareThreshold");
        else
            this.scareThreshold = 0.90;

        this.opponentModel = model;
        this.omStrategy = oms;

        // TODO: Need to find a way to calculate this
        this.isOpponentCooperative = false;
    }

    @Override
    public BidDetails determineOpeningBid() {
        return determineNextBid();
    }

    /**
     * Simple offering strategy which retrieves the target utility and looks for
     * the nearest bid if no opponent model is specified. If an opponent model
     * is specified, then the agent return a bid according to the opponent model
     * strategy.
     */
    @Override
    public BidDetails determineNextBid() {

        // TODO:
        // 1. Find whether the model is offensive or cooperative
        // 2. Check the time if it's close to 90% and apply scare attacks
        // 3. Try to find an offer that supports both users
        double time = negotiationSession.getTime();
        double utilityGoal = 0.80;

        // What is the agent's profile?
        if (isOpponentCooperative) {

            // Cooperative Profile
            // Step 1: Concede little by little every turn
            utilityGoal = p(time);

            // Step 2: Calculate a new utility goal

        } else {

            // Offensive Profile

            // Step 1: Check whether the agent should scare the opponent
            if (time >= scareThreshold) {
                System.out.println("Scare opponent!");
            }

            // Step 2: Do not concede unless a configurable amount of time passed

            utilityGoal = 0.80;
        }

        // if there is no opponent model available
        if (opponentModel instanceof NoModel) {
            nextBid = negotiationSession.getOutcomeSpace().getBidNearUtility(utilityGoal);
        } else {
            nextBid = omStrategy.getBid(outcomespace, utilityGoal);
        }
        return nextBid;
    }

    public NegotiationSession getNegotiationSession() {
        return negotiationSession;
    }

    @Override
    public Set<BOAparameter> getParameterSpec() {
        Set<BOAparameter> set = new HashSet<BOAparameter>();
        set.add(new BOAparameter("scareThreshold", 0.90, "Scare opponent time threshold"));

        return set;
    }

    @Override
    public String getName() {
        return "Group 4 - Bidding strategy";
    }


    /**
     * Makes sure the target utility with in the acceptable range according to
     * the domain. Goes from Pmax to Pmin!
     *
     * @param t
     * @return double
     */
    public double p(double t) {
        return 0.50 + (1 - 0.50) * (1 - t);
    }
}