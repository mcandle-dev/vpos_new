package com.example.apidemo.model;

/**
 * Member class to hold customer information
 * Currently hardcoded for demo purposes
 */
public class Member {
    private String name;
    private String memberId;
    private String grade;        // VIP, GOLD, SILVER, etc.
    private int points;
    private String serviceUuid;

    // Default constructor with hardcoded demo data
    public Member() {
        this.name = "김준호";
        this.memberId = "HD2023091234";
        this.grade = "VIP";
        this.points = 125000;
        this.serviceUuid = "";
    }

    // Constructor with all fields
    public Member(String name, String memberId, String grade, int points, String serviceUuid) {
        this.name = name;
        this.memberId = memberId;
        this.grade = grade;
        this.points = points;
        this.serviceUuid = serviceUuid;
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getMemberId() {
        return memberId;
    }

    public String getGrade() {
        return grade;
    }

    public int getPoints() {
        return points;
    }

    public String getServiceUuid() {
        return serviceUuid;
    }

    // Setters
    public void setName(String name) {
        this.name = name;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public void setServiceUuid(String serviceUuid) {
        this.serviceUuid = serviceUuid;
    }

    // Display formatted customer name
    public String getDisplayName() {
        return name + " 고객님 ✔";
    }

    // Display formatted grade and member ID
    public String getDisplayGradeInfo() {
        return grade + " ⭐ | " + memberId;
    }

    // Display formatted points
    public String getDisplayPoints() {
        return String.format("%,dP", points);
    }
}
