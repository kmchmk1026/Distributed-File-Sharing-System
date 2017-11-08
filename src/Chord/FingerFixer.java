/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Chord;

/**
 *
 * @author Irfad Hussain
 */
public class FingerFixer extends Thread {

    private static int fingerToFixNext = -1;

    private Node thisNode;
    private FingerTable fingerTable;

    private boolean waitingForSuccussor;
    private String succossorReply;

    public FingerFixer(Node thisNode) {
        this.setName("FingerFixer-Thread");
        this.thisNode = thisNode;
        this.fingerTable = thisNode.getFingerTable();
    }

    public void setSuccossorReply(String reply) {
        this.succossorReply = reply;
    }
    
    public void setWaitingForSuccessor(boolean waitState){
        this.waitingForSuccussor = waitState;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(1000);
                this.thisNode.echo("FingerFixer running...\n");
                if (!waitingForSuccussor) {
                    fingerToFixNext++;
                    if (fingerToFixNext >= NodeImpl.MAX_FINGERS) {
                        System.out.println("FingerFixer New iteration...");
                        Thread.sleep(5000);
                        fingerToFixNext = 0;
                    }
                    Node fingerEntry = thisNode.findSuccessorOf(fingerToFixNext, (thisNode.getID() + (int) Math.pow(2, fingerToFixNext)) % NodeImpl.MAX_NODES, thisNode.getIp(), thisNode.getPort());
                    if (fingerEntry == null) {
                        waitingForSuccussor = true;
                        Thread.sleep(60 * 1000);
                    } else {
                        System.out.println("FixFinger: Update finger " + fingerToFixNext + " of " + thisNode.getID() + " from  " + fingerTable.getNodeAt(fingerToFixNext).getID() + " to " + fingerEntry.getID());
                        fingerTable.updateEntry(fingerToFixNext, fingerEntry);
                        thisNode.getGUI().UpdateFingerTable(fingerToFixNext, fingerEntry);
                    }
                }
            } catch (InterruptedException ex) {
                if (waitingForSuccussor) {
                    String[] replyParts = succossorReply.split(" ");
                    int fingerIndex = Integer.parseInt(replyParts[0]);
                    fingerTable.updateEntry(fingerIndex, new NodeImpl(null, replyParts[1], Integer.parseInt(replyParts[2]), thisNode.getBSip(), thisNode.getBSport()));
                    thisNode.getGUI().UpdateFingerTable(fingerToFixNext, fingerTable.getNodeAt(fingerIndex));
                    System.out.println("FixFinger: Update finger " + fingerIndex + " of " + thisNode.getID() + " to  " + fingerTable.getNodeAt(fingerIndex).getID());
                    waitingForSuccussor = false;
                }
            }
        }
    }

}
