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

	private double a;
	private double b;
	
    /**
     * Empty constructor for the BOA framework.
     */
    public SquaredAcceptance() {
    }

    public SquaredAcceptance(NegotiationSession negoSession, OfferingStrategy strat, double alpha, double beta) {
        this.negotiationSession = negoSession;
        this.offeringStrategy = strat;
        this.a = alpha;
        this.b = beta;
    }

    @Override
    public void init(NegotiationSession negoSession, OfferingStrategy strat, OpponentModel opponentModel,
                     Map<String, Double> parameters) throws Exception {
        this.negotiationSession = negoSession;
        this.offeringStrategy = strat;
        
        if (parameters.get("a") != null || parameters.get("b") != null) {
			a = parameters.get("a");
			b = parameters.get("b");
		} else {
			a = 1.2;
			b = 0.02;
		}
    }
    
	@Override
	public String printParameters() {
		String str = "[a: " + a + "]";
		return str;
	}

    @Override
    public Actions determineAcceptability() {
    	// Get my first bid utility if I already offered a bid
    	double myFirstBid = 1;
    	if (negotiationSession.getTimeline().getCurrentTime() > 1) {
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
        if (myFirstBid/a > opponentsBestBid) {
            minimumOffer = myFirstBid/a;
        } else {
            minimumOffer = opponentsBestBid;
        }

        // The difference in my utility between my first bid and the opponent best bid.
        double startingDifference = myFirstBid - opponentsBestBid;
        
        // An acceptable offer: the closer we come to the end of a negotiation, the lower it gets. In the last 2% of rounds it is
        // equal to the minimumoffer variable.
        double acceptableOffer = minimumOffer;
        if (percentageTimeLeft > b) {
        	acceptableOffer = Math.sqrt(percentageTimeLeft*startingDifference) + minimumOffer;
        }
        
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
	public Set<BOAparameter> getParameterSpec() {

		Set<BOAparameter> set = new HashSet<BOAparameter>();
		set.add(new BOAparameter("a", 1.2,
				"Acceptable bid becomes starting offer divided by a if that is higher than the timedependent bid"));
		
		set.add(new BOAparameter("b", 0.02,
				"The last b percentage of rounds, the agent will accept offers equal to its minimum offer variable"));

		return set;
	}

    @Override
    public String getName() {
        return "SquaredAcceptance";
    }
}
