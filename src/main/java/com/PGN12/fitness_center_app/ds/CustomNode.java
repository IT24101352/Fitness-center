package com.PGN12.fitness_center_app.ds;
import com.PGN12.fitness_center_app.model.Member;

// Node class for the linked list used in ManualRenewalQueue
public class CustomNode {
    public Member data; // The data in the node (Member object)
    public CustomNode next;

    public CustomNode(Member data) {
        this.data = data;
        this.next = null;
    }
}
