package com.example.apidemo.model;

/**
 * Member class to hold customer information
 * Currently hardcoded for demo purposes
 */
public class Member {
    private String name;
    private String memberId;
    private String memberCode;   // Short code like "2200"
    private String grade;        // VIP, GOLD, SILVER, etc.
    private int points;
    private String cardNumber;   // Card number like "9410-1234-5678-9012"
    private String serviceUuid;

    // Default constructor with hardcoded demo data
    public Member() {
        this.name = "김준호";
        this.memberId = "HD2023091234";
        this.memberCode = "2200";
        this.grade = "VIP";
        this.points = 125000;
        this.cardNumber = "9410-1234-5678-9012";
        this.serviceUuid = "";
    }

    // Constructor with all fields
    public Member(String name, String memberId, String memberCode, String grade, int points, String cardNumber, String serviceUuid) {
        this.name = name;
        this.memberId = memberId;
        this.memberCode = memberCode;
        this.grade = grade;
        this.points = points;
        this.cardNumber = cardNumber;
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

    public String getMemberCode() {
        return memberCode;
    }

    public String getCardNumber() {
        return cardNumber;
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

    public void setMemberCode(String memberCode) {
        this.memberCode = memberCode;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    // Display formatted customer name: "김준호 (2200)님"
    public String getDisplayName() {
        return name + " (" + memberCode + ")님";
    }

    // Display formatted grade and card number: "VIP | 9410-1234-5678-9012"
    public String getDisplayCardInfo() {
        return grade + " | " + cardNumber;
    }

    // Display formatted grade and member ID (legacy)
    public String getDisplayGradeInfo() {
        return grade + " ⭐ | " + memberId;
    }

    // Display formatted points
    public String getDisplayPoints() {
        return String.format("%,dP", points);
    }
}
