package com.almende.bridge.agents_old;

import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.rpc.annotation.Name;

@Access(AccessType.PUBLIC)
public class MonitoredTeamAgent extends TeamAgent implements MonitoredTeam {

	public void init(){
		myState	= getState();
	}
	
	@Override
	public Integer getAmount(){
		return myState.get("amount",Integer.class);
	}

	@Override
	public Boolean isComplete(){
		return toDo()<= 0;
	}

	@Override
	public Integer toDo() {
		return getAmount()-getAvailable().size();
	}

	@Override
	public void setAmount(@Name("amount") Integer amount) {
		myState.put("amount",amount);
	}
	
}
