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

    /** Sets the threshold regarding when to
     *  start conceding (eg. 90% of the time)
     *  Takes values between 0 and 1 and it is only
     *  enabled during the offensive profile
     */
    private double concedeThreshold;

    /** Minimum target utility */
    private double minUtility;

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

        // Assign parameters to class
        if (parameters.get("concedeThreshold") != null)
            this.concedeThreshold = parameters.get("concedeThreshold");
        else
            this.concedeThreshold = 0.90;

        // Assign parameters to class
        if (parameters.get("minUtility") != null)
            this.minUtility = parameters.get("minUtility");
        else
            this.minUtility = 0.50;

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
        double utilityGoal = 1;

        // What is the agent's profile?
        if (isOpponentCooperative) {

            /**
             * Cooperative Profile
             * The agent concedes with more ease over time as it tries to find
             * an offer that both users will agree on
             */
            utilityGoal = p(time);
        } else {

            /**
             * Offensive Profile
             * The agent does not concede over time but concedes only at the end (configurable)
             * In addition the agent disappears towards the end in order to scare the opponent
             */

            // Step 1: Check whether the agent should scare the opponent
            if (time >= scareThreshold) {
                System.out.println("Scare opponent!");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // Step 2: Do not concede unless a configurable amount of time passed
            if (time >= concedeThreshold) {
                utilityGoal = p(time);
            } else {
                utilityGoal = 0.90;
            }
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
        set.add(new BOAparameter("concedeThreshold", 0.90, "Offensive profile concede time threshold"));
        set.add(new BOAparameter("min", 0.50, "Minimum utility"));
        return set;
    }

    @Override
    public String getName() {
        return "Group 4 - Bidding strategy";
    }


    /**
     * Makes sure the target utility with in the acceptable range according to
     * the domain. Goes from 1 to minimum Utility!
     *
     * @param t
     * @return double
     */
    public double p(double t) {
        return minUtility + (1 - minUtility) * (1 -  Math.pow(t, 1.0 / 1.0));
    }
}