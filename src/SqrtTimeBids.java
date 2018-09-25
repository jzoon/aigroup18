import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import genius.core.bidding.BidDetails;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OMStrategy;
import genius.core.boaframework.OfferingStrategy;
import genius.core.boaframework.OpponentModel;
import genius.core.boaframework.OutcomeSpace;
import genius.core.boaframework.SortedOutcomeSpace;

/**
 * Bidding class
 * 
 * Created by Job Zoon
 */
public class SqrtTimeBids extends OfferingStrategy {
	OutcomeSpace outcomeSpace;
	BidDetails opponentsBestBid;
	
	@Override
	public void init(NegotiationSession negoSession, OpponentModel model, OMStrategy oms,
			Map<String, Double> parameters) throws Exception {
		super.init(negoSession, parameters);
		outcomeSpace = new SortedOutcomeSpace(negotiationSession.getUtilitySpace());
	}
	
	@Override
	public BidDetails determineOpeningBid() {
		return negotiationSession.getMaxBidinDomain();
	}
	
	@Override
	public BidDetails determineNextBid() {
		if (negotiationSession.getTimeline().getCurrentTime() + 5 > negotiationSession.getTimeline().getTotalTime()) {
			return negotiationSession.getOpponentBidHistory().getBestBidDetails();
		} else if (negotiationSession.getOpponentBidHistory().size() > 0) {
			return outcomeSpace.getBidNearUtility(determineBiddingUtility());
		} else {
			return outcomeSpace.getBidNearUtility(negotiationSession.getMaxBidinDomain().getMyUndiscountedUtil());
		}
	}
	
	public double determineBiddingUtility() {
		double minimumOffer = negotiationSession.getOpponentBidHistory().getBestBidDetails().getMyUndiscountedUtil();
		double difference = negotiationSession.getMaxBidinDomain().getMyUndiscountedUtil() - minimumOffer;
		double percentageTimeLeft = (negotiationSession.getTimeline().getTotalTime() -
                negotiationSession.getTimeline().getCurrentTime())/negotiationSession.getTimeline().getTotalTime();

		double acceptableOffer = Math.sqrt(percentageTimeLeft*difference) + minimumOffer;
		
		if (acceptableOffer <= 1) {
			return acceptableOffer;
		} else {
			return negotiationSession.getMaxBidinDomain().getMyUndiscountedUtil();
		}
	}
	
	@Override
	public Set<BOAparameter> getParameterSpec() {
		Set<BOAparameter> set = new HashSet<BOAparameter>();
		
		return set;
	}
	
	public NegotiationSession getNegotiationSession() {
		return negotiationSession;
	}
	
	@Override
	public String getName() {
		return "Group18_BS";
	}
}
