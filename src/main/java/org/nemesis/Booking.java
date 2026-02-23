package org.nemesis;

public class Booking {
    public int id;
    public String guestName;
    public String roomType;
    // Jackson needs a default constructor
    public Booking() {}
    public Booking(int id, String guestName, String roomType) {
        this.id = id;
        this.guestName = guestName;
        this.roomType = roomType;
    }
}