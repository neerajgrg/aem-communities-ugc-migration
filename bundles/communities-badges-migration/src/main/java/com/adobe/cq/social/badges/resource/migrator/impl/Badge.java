package com.adobe.cq.social.badges.resource.migrator.impl;

import java.util.Calendar;

public class Badge implements Comparable<Badge>{
	
	

	String path;
	Calendar earnedDate;
	int score;
	
	
	public Badge(String path, Calendar earnedDate, int score) {
		super();
		this.path = path;
		this.earnedDate = earnedDate;
		this.score = score;
	}

	public String getPath() {
		return path;
	}


	public void setPath(String path) {
		this.path = path;
	}


	public Calendar getEarnedDate() {
		return earnedDate;
	}


	public void setEarnedDate(Calendar earnedDate) {
		this.earnedDate = earnedDate;
	}


	public int getScore() {
		return score;
	}


	public void setScore(int score) {
		this.score = score;
	}


	@Override
	public int compareTo(Badge o) {
		
		return this.score - o.getScore();
	}
	
	@Override
	public String toString() {
		return "Badge [path=" + path + ", earnedDate=" + earnedDate + ", score=" + score + "]";
	}

	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Badge other = (Badge) obj;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		return true;
	}
    
}
