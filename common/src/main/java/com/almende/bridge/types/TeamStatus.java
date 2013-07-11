package com.almende.bridge.types;

import java.io.Serializable;

public class TeamStatus implements Serializable {
	private static final long	serialVersionUID	= 6531452193120113009L;
	
	private String				teamId;
	private String				teamLeaderName;
	private String				teamStatus;
	private String				deploymentStatus;
	private String				lat;
	private String				lon;
	
	public TeamStatus() {
	}
	
	public TeamStatus(String id, String leaderName) {
		this.teamId = id;
		this.teamLeaderName = leaderName;
	}
	
	public String getTeamId() {
		return teamId;
	}
	
	public void setTeamId(String teamId) {
		this.teamId = teamId;
	}
	
	public String getTeamLeaderName() {
		return teamLeaderName;
	}
	
	public void setTeamLeaderName(String teamLeaderName) {
		this.teamLeaderName = teamLeaderName;
	}
	
	public String getTeamStatus() {
		return teamStatus;
	}
	
	public void setTeamStatus(String teamStatus) {
		this.teamStatus = teamStatus;
	}
	
	public String getDeploymentStatus() {
		return deploymentStatus;
	}
	
	public void setDeploymentStatus(String deploymentStatus) {
		this.deploymentStatus = deploymentStatus;
	}
	
	public String getLat() {
		return lat;
	}
	
	public void setLat(String lat) {
		this.lat = lat;
	}
	
	public String getLon() {
		return lon;
	}
	
	public void setLon(String lon) {
		this.lon = lon;
	}
}