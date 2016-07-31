package edu.clemson.resolve.proving;

import java.math.BigInteger;

/**
 * Created by daniel on 7/31/16.
 */
public class Metrics {
    public BigInteger numProofsConsidered;
    public BigInteger numTimesBacktracked;

    public long ruleCount, rulesTried;
    public ProverListener progressListener;

    public ActionCanceller actionCanceller;

    private long myProofDuration, myTimeout;

    public Metrics(long duration, long timeout) {
        clear();
        myProofDuration = duration;
        myTimeout = timeout;
    }

    public BigInteger getNumProofsConsidered() {
        return numProofsConsidered;
    }

    public void incrementProofsConsidered() {
        numProofsConsidered = numProofsConsidered.add(BigInteger.ONE);
    }

    public void accumulate(Metrics m) {
        numProofsConsidered = numProofsConsidered.add(m.numProofsConsidered);
        numTimesBacktracked = numTimesBacktracked.add(m.numTimesBacktracked);
    }

    public long getTimeout() {
        return myTimeout;
    }

    public long getProofDuration() {
        return myProofDuration;
    }

    public void clear() {
        numTimesBacktracked = BigInteger.ZERO;
        numProofsConsidered = BigInteger.ZERO;
        ruleCount = 0;
        rulesTried = 0;
        myProofDuration = 0;
        myTimeout = 0;
    }
}
