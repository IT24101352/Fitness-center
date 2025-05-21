package com.PGN12.fitness_center_app.ds;

import com.PGN12.fitness_center_app.model.Member;
import java.util.NoSuchElementException;

public class ManualRenewalQueue {
    private CustomNode front;
    private CustomNode rear;
    private int size;

    public ManualRenewalQueue() {
        this.front = null;
        this.rear = null;
        this.size = 0;
    }

    // Method to add a member to the queue (enqueue)
    public void enqueue(Member member) {
        CustomNode newNode = new CustomNode(member);
        this.size++;
        if (this.rear == null) { // If queue is empty, then new node is front and rear both
            this.front = this.rear = newNode;
            return;
        }
        // Add the new node at the end of queue and change rear
        this.rear.next = newNode;
        this.rear = newNode;
    }

    // Method to remove a member from the queue (dequeue)
    public Member dequeue() {
        if (this.front == null) { // If queue is empty
            throw new NoSuchElementException("Queue is empty, cannot dequeue.");
        }
        this.size--;
        Member memberData = this.front.data;
        this.front = this.front.next;

        // If front becomes NULL, then change rear also as NULL
        if (this.front == null) {
            this.rear = null;
        }
        return memberData;
    }

    // Method to check if the queue is empty
    public boolean isEmpty() {
        return (this.front == null);
    }

    // Method to get the front member of the queue without removing (peek)
    public Member peek() {
        if (this.front == null) {
            throw new NoSuchElementException("Queue is empty, cannot peek.");
        }
        return this.front.data;
    }

    // Method to get the current size of the queue
    public int size() {
        return this.size;
    }
}