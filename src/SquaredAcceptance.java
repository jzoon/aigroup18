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
    	// Get my first bid utility if I already offered a bid
    	double myFirstBid = 1;
    	if (negotiationSession.getTimeline().getCurrentTime() == 0) {
    		myFirstBid = negotiationSession.getOwnBidHistory().getFirstBidDetails().getMyUndiscountedUtil();
    	} 
    
    	// Get opponents best bid utility
        double opponentsBestBid = negotiationSession.getOpponentBidHistory().getBestBidDetails()
                .getMyUndiscountedUtil();
        
        // Get my next bid utility
        double nextMyBidUtil = offeringStrategy.getNextBid().getMyUndiscountedUtil();
        
        // Get opponents last bid utility
        double lastOpponentBidUtil = negotiationSession.getOpponentBidHistory().getLastBidDetails()
                .getMyUndiscountedUtil();

        // Percentage time left, number between 1 and 0.
        double percentageTimeLeft = (negotiationSession.getTimeline().getTotalTime() -
                negotiationSession.getTimeline().getCurrentTime())/negotiationSession.getTimeline().getTotalTime();

        // Minimum offer is the opponents best bid, or my opening bid divided by variable alpha. (which one is highest)
        double minimumOffer = 1;
        if (myFirstBid/1.2 > opponentsBestBid) {
            minimumOffer = myFirstBid/1.2;
        } else {
            minimumOffer = opponentsBestBid;
        }

        // The difference in my utility between my first bid and the opponent best bid.
        double startingDifference = myFirstBid - opponentsBestBid;
        
        // An acceptable offer: the closer we come to the end of a negotiation, the lower it gets. In the last round it is
        // equal to the minimumoffer variable.
        double acceptableOffer = Math.sqrt(percentageTimeLeft*startingDifference) + minimumOffer;

        // Accept an offer if it is better than my next bid OR if it is better than the acceptableOffer variable
        if (lastOpponentBidUtil >= nextMyBidUtil && lastOpponentBidUtil >= opponentsBestBid) {
            return Actions.Accept;
        } else if (lastOpponentBidUtil >= acceptableOffer) {
            return Actions.Accept;
        }
        
        // else: reject
        return Actions.Reject;
    }

    @Override
    public String getName() {
        return "SquaredAcceptance";
    }
}
