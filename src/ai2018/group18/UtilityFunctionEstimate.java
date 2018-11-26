package ai2018.group18;

import genius.core.Bid;
import genius.core.issue.Value;

import java.util.List;
import java.util.Map;

public class UtilityFunctionEstimate {
    private List<Bid> rankingList;

    public UtilityFunctionEstimate() {

    }

    public void estimateValueWeigts(List<Bid> bidOrder) {

        // for every bid from the ranked list
        for (int i = 0; i < bidOrder.size(); i++) {
            Map<Integer, Value> bidValues = bidOrder.get(i).getValues();
        }


    }

    public void estimateUtilityWeights(List<Bid> bidOrder) {

    }
}
