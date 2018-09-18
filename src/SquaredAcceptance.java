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
 * Acceptance class
 * 
 * Created by Job Zoon
 */
public class SquaredAcceptance extends AcceptanceStrategy {

    /**
     * Empty constructor for the BOA framework.
     */
    public SquaredAcceptance() {
    }

    public SquaredAcceptance(NegotiationSession negoSession, OfferingStrategy strat) {
        this.negotiationSession = negoSession;
        this.offeringStrategy = strat;
    }

    @Override
    public void init(NegotiationSession negoSession, OfferingStrategy strat, OpponentModel opponentModel,
                     Map<String, Double> parameters) throws Exception {
        this.negotiationSession = negoSession;
        this.offeringStrategy = strat;
    }

    @Override
    public Actions determineAcceptability() {
        double myFirstBid = negotiationSession.getOwnBidHistory().getFirstBidDetails().getMyUndiscountedUtil();
        double opponentsFirstBid = negotiationSession.getOpponentBidHistory().getFirstBidDetails()
                .getMyUndiscountedUtil();
        double nextMyBidUtil = offeringStrategy.getNextBid().getMyUndiscountedUtil();
        double lastOpponentBidUtil = negotiationSession.getOpponentBidHistory().getLastBidDetails()
                .getMyUndiscountedUtil();

        double percentageTimeLeft = negotiationSession.getTimeline().getTotalTime() -
                negotiationSession.getTimeline().getCurrentTime();

        double minimumOffer = 1;
        if (myFirstBid/2 > opponentsFirstBid) {
            minimumOffer = myFirstBid/2;
        } else {
            minimumOffer = opponentsFirstBid;
        }

        double startingDifference = myFirstBid - opponentsFirstBid;

        if (lastOpponentBidUtil >= nextMyBidUtil) {
            return Actions.Accept;
        } else if (lastOpponentBidUtil >= Math.sqrt(percentageTimeLeft*startingDifference) + minimumOffer) {
            return Actions.Accept;
        }
        return Actions.Reject;
    }

    @Override
    public String getName() {
        return "SquaredAcceptance";
    }
}
