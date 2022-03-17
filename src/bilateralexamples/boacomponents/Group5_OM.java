package bilateralexamples.boacomponents;


import agents.bayesianopponentmodel.BayesianOpponentModel;
import agents.bayesianopponentmodel.OpponentModelUtilSpace;
import agents.similarity.Similarity;
import genius.core.Bid;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OpponentModel;
import genius.core.issue.Issue;
import genius.core.issue.ValueDiscrete;
import genius.core.utility.AdditiveUtilitySpace;

import java.util.*;

public class Group5_OM extends OpponentModel {
    private BayesianOpponentModel model;
    private int startingBidIssue = 0;
    private Set<Bid> offers;

    public void init(NegotiationSession var1, Map<String, Double> var2) {
        this.negotiationSession = var1;
        this.model = new BayesianOpponentModel((AdditiveUtilitySpace)var1.getUtilitySpace());
        if (var2.get("m") != null) {
            this.model.setMostProbableUSHypsOnly((Double)var2.get("m") > 0.0D);
        } else {
            this.model.setMostProbableUSHypsOnly(false);
            System.out.println("Constant \"m\" was not set. Assumed default value.");
        }

        while(!this.testIndexOfFirstIssue(var1.getUtilitySpace().getDomain().getRandomBid((Random)null), this.startingBidIssue)) {
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

    public void updateModel(Bid var1, double var2) {
        try {
            this.model.updateBeliefs(var1);
        } catch (Exception var5) {
            var5.printStackTrace();
        }

    }

    public double getBidEvaluation(Bid var1) {
        try {
            return this.model.getNormalizedUtility(var1);
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
        return "Group5_OM";
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
}
