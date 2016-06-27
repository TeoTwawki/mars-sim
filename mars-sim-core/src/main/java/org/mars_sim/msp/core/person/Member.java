package org.mars_sim.msp.core.person;

import java.util.List;

public class Member {
	
	private String name; 
	private String gender;
	private String mbti; 
	private String job; 
	private String destination;
	private String mainDish;
	private String sideDish;
	private String dessert;
	private String activity;

	
	public Member() {

	}

	public void setName(String value) {
		name = value;
	}
	
	public void setGender(String value) {
		gender = value;
	}
	
	public void setmbti(String value) {
		mbti = value;
	}
	
	public void setJob(String value) {
		job = value;
	} 
	
	public void setDestination(String value) {
		destination = value;
	}
	
	public void setMainDish(String value) {
		mainDish = value;
	}
	public void setSideDish(String value) {
		sideDish = value;
	}
	
	public void setDessert(String value) {
		dessert = value;
	}
	
	public void setActivity(String value) {
		activity = value;
	}


	public String getName() {
		return name;
	} 
	
	public String getGender() {
		return gender;
	}
	
	public String getmbti() {
		return mbti;
	}
	
	public String getJob() {
		return job;
	}  
	
	public String getDestination() {
		return destination;
	}
	
	public String getMainDish() {
		return mainDish;
	}
	
	public String getSideDish() {
		return sideDish;
	}
	
	public String getDessert() {
		return dessert;
	}
	
	public String getActivity() {
		return activity;
	}

	
	
}