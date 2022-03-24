package bilateralexamples.boacomponents;


import agents.bayesianopponentmodel.BayesianOpponentModel;
import agents.bayesianopponentmodel.OpponentModelUtilSpace;
import agents.similarity.SimilarityFunction;
import genius.core.Bid;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OpponentModel;
import genius.core.issue.Issue;
import genius.core.issue.ValueDiscrete;
import genius.core.utility.AdditiveUtilitySpace;

import java.util.*;

public class Group4_OM extends OpponentModel {
    private BayesianOpponentModel model;
    private int startingBidIssue = 0;
    private Set<Bid> offers;
    private double attribute_weights[];


    public void init(NegotiationSession session, Map<String, Double> var2) {
        this.negotiationSession = session;
        this.model = new BayesianOpponentModel((AdditiveUtilitySpace)session.getUtilitySpace());
        if (var2.get("m") != null) {
            this.model.setMostProbableUSHypsOnly((Double)var2.get("m") > 0.0D);
        } else {
            this.model.setMostProbableUSHypsOnly(false);
            System.out.println("Constant \"m\" was not set. Assumed default value.");
        }

        while(!this.testIndexOfFirstIssue(session.getUtilitySpace().getDomain().getRandomBid((Random)null), this.startingBidIssue)) {
            ++this.startingBidIssue;
        }

    }

    private boolean testIndexOfFirstIssue(Bid var1, int var2) {
        try {
            ValueDiscrete var3 = (ValueDiscrete)var1.getValue(var2);
            return true;
        } catch (Exception var4) {
            return false;
        }
    }

    public void updateModel(Bid bid_1, double time) {
        try {
            this.model.updateBeliefs(bid_1);
        } catch (Exception var5) {
            var5.printStackTrace();
        }

    }

    public double getBidEvaluation(Bid bid_1) {
        try {
            return this.model.getNormalizedUtility(bid_1);
        } catch (Exception var3) {
            var3.printStackTrace();
            return 0.0D;
        }
    }

    public double getWeight(Issue var1) {
        return this.model.getNormalizedWeight(var1, this.startingBidIssue);
    }

    public AdditiveUtilitySpace getOpponentUtilitySpace() {
        return new OpponentModelUtilSpace(this.model);
    }

    public void cleanUp() {
        super.cleanUp();
    }

    public String getName() {
        return "Group4_OM";
    }

    public Set<BOAparameter> getParameterSpec() {
        HashSet var1 = new HashSet();
        var1.add(new BOAparameter("m", 0.0D, "If higher than 0 the most probable hypothesis is only used"));
        return var1;
    }

    public double distance_preference(double relation_1, double relation_2) {
        for (Bid offer : offers) {

        }
        List<Bid> sortedList = new ArrayList<>(offers);
        return 0.0;
    }

    public double similarity(Bid bid_1, Bid bid_2) {
        return bid_1.getDistance(bid_2);
    }

    public double equivalence_operator(Bid bid_1, Bid bid_2) {
        double nrOfIssues = bid_1.getIssues().size();
        double unequalValues = nrOfIssues - bid_1.countEqualValues(bid_2);
        return unequalValues/nrOfIssues;
    }

}
