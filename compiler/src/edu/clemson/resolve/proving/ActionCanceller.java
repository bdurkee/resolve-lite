package edu.clemson.resolve.proving;

public class ActionCanceller {

    public boolean running;

    public ActionCanceller() {
        running = true;
    }

    public void cancel() {
        running = false;
    }

    public boolean amRunning() {
        return running;
    }
}
