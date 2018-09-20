import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import genius.core.bidding.BidDetails;
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
    	double myFirstBid = 1;
    	if (negotiationSession.getTimeline().getCurrentTime() == 0) {
    		myFirstBid = negotiationSession.getOwnBidHistory().getFirstBidDetails().getMyUndiscountedUtil();
    	} 
    	
        double opponentsBestBid = negotiationSession.getOpponentBidHistory().getBestBidDetails()
                .getMyUndiscountedUtil();
        double nextMyBidUtil = offeringStrategy.getNextBid().getMyUndiscountedUtil();
        double lastOpponentBidUtil = negotiationSession.getOpponentBidHistory().getLastBidDetails()
                .getMyUndiscountedUtil();

        double percentageTimeLeft = (negotiationSession.getTimeline().getTotalTime() -
                negotiationSession.getTimeline().getCurrentTime())/negotiationSession.getTimeline().getTotalTime();

        double minimumOffer = 1;
        if (myFirstBid/1.1 > opponentsBestBid) {
            minimumOffer = myFirstBid/1.1;
        } else {
            minimumOffer = opponentsBestBid;
        }

        double startingDifference = myFirstBid - opponentsBestBid;
        
        double acceptableOffer = Math.sqrt(percentageTimeLeft*startingDifference) + minimumOffer;

        if (lastOpponentBidUtil >= nextMyBidUtil && lastOpponentBidUtil >= opponentsBestBid) {
            return Actions.Accept;
        } else if (lastOpponentBidUtil >= acceptableOffer) {
            return Actions.Accept;
        }
        return Actions.Reject;
    }

    @Override
    public String getName() {
        return "SquaredAcceptance";
    }
}
