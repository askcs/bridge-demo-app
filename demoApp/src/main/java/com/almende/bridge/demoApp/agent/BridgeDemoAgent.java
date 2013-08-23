package com.almende.bridge.demoApp.agent;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;

import com.almende.bridge.demoApp.EveService;
import com.almende.bridge.demoApp.R;
import com.almende.bridge.demoApp.event.StateEvent;
import com.almende.bridge.types.SitRep;
import com.almende.bridge.types.Task;
import com.almende.bridge.types.TeamStatus;
import com.almende.eve.agent.Agent;
import com.almende.eve.monitor.Poll;
import com.almende.eve.monitor.Push;
import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.rpc.annotation.Name;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.eve.transport.AsyncCallback;
import com.almende.eve.transport.xmpp.XmppService;
import com.almende.util.TypeUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.greenrobot.event.EventBus;

@Access(AccessType.PUBLIC)
public class BridgeDemoAgent extends Agent {
	private static final String	VERSION	= "5";
	private static final String	TASK	= "CurrentTask";
	private static final String	SITREP	= "Sitrep";
	private static final String	STATUS	= "Status";
	private static final String	CLOUD	= "Cloud";
	private static Context		context	= null;
	
	public static void setContext(Context context) {
		BridgeDemoAgent.context = context;
	}
	
	public static String getBaseVersion() {
		return VERSION;
	}
	
	@Override
	public String getVersion() {
		return VERSION;
	}
	
	public TeamStatus getTeamStatus() throws IOException {
		System.err.println("Get Team Status called!");
		ObjectReader teamStatusReader = JOM.getInstance().reader(
				TeamStatus.class);
		
		if (getState().containsKey(STATUS)) {
			String teamStatusString = getState().get(STATUS, String.class);
			System.err.println("Found team Status:" + teamStatusString);
			TeamStatus teamStatus = teamStatusReader
					.readValue(teamStatusString);
			Task task = getTask();
			if (teamStatus.getDeploymentStatus().equals(TeamStatus.WITHDRAWN)) {
				// don't change assignment
			} else if (task == null) {
				teamStatus.setDeploymentStatus(TeamStatus.UNASSIGNED);
			} else if (task.getStatus().equals(Task.NOTCONFIRMED)) {
				teamStatus.setDeploymentStatus(TeamStatus.ASSIGNED);
			} else if (task.getStatus().equals(Task.CONFIRMED)) {
				teamStatus.setDeploymentStatus(TeamStatus.ACTIVE);
			} else if (task.getStatus().equals(Task.COMPLETE)) {
				// // TODO: implement 20 minute buffer "POST" state.
				teamStatus.setDeploymentStatus(TeamStatus.UNASSIGNED);
			} else {
				System.err.println("Couldn't determine teamstate, task state:"
						+ task.getStatus());
			}
			if (EveService.mLocationClient != null
					&& EveService.mLocationClient.isConnected()) {
				Location mCurrentLocation = EveService.mLocationClient
						.getLastLocation();
				if (mCurrentLocation != null) {
					teamStatus.setLat(Double.valueOf(
							mCurrentLocation.getLatitude()).toString());
					teamStatus.setLon(Double.valueOf(
							mCurrentLocation.getLongitude()).toString());
				}
			}
			
			return teamStatus;
		} else {
			return null;
		}
	}
	
	public void subscribeMonitor() throws MalformedURLException {
		System.err.println("Calling subscribeMonitor()");
		List<String> urls = getUrls();
		URI myUrl = getFirstUrl();
		for (String item : urls) {
			try {
				System.out.println("Url:" + item);
				if (item.startsWith("xmpp")) {
					myUrl = new URI(item);
					break;
				}
			} catch (URISyntaxException e) {
			}
		}
		if (myUrl.getScheme().startsWith("xmpp")) {
			String username = myUrl.toString().split("@")[0].substring(4);
			String host = myUrl.toString().split("@")[1].replaceAll("[:/].*",
					"");
			
			URI cloudUri = URI.create(myUrl.getScheme() + username + "@" + host
					+ "/" + CLOUD);
			
			String monitorId = getResultMonitorFactory().create("taskMonitor",cloudUri,
					"getTask", JOM.createObjectNode(), "wrapTask",
					new Poll(600000), new Push().onEvent("taskUpdated"));
			
			System.out.println("Monitor id:"
					+ monitorId
					+ " -> "
					+ getResultMonitorFactory().getMonitorById(monitorId)
							.toString());
			
		} else {
			System.err.println("SubscribeMonitor: XMPP not yet initialized?");
		}
	}
	
	public void initTask() throws JSONRPCException, IOException {
		getState().remove(TASK);
		getResultMonitorFactory().cancelAll();
		getScheduler().cancelAllTasks();
	}
	
	public Task getTask() throws JsonProcessingException, IOException {
		System.err.println("Calling getTask();");
		ObjectReader taskReader = JOM.getInstance().reader(Task.class);
		
		if (getState().containsKey(TASK)) {
			String task = getState().get(TASK, String.class);
			System.err.println("Found task:" + task);
			return taskReader.readValue(task);
		} else {
			return null;
		}
	}
	
	public SitRep getSitRep() throws JsonProcessingException, IOException {
		System.err.println("Calling getsitRep();");
		ObjectReader taskReader = JOM.getInstance().reader(SitRep.class);
		
		if (getState().containsKey(SITREP)) {
			String siteRep = getState().get(SITREP, String.class);
			System.err.println("Found sitrep:" + siteRep);
			
			return taskReader.readValue(siteRep);
		} else {
			return null;
		}
	}
	
	public void wrapTask(@Name("result") String task) throws IOException {
		setTask(TypeUtil.inject(Task.class, JOM.getInstance().readTree(task)));
	}
	
	public void setTask(@Name("task") Task task) throws IOException {
		if (task != null) {
			Task oldTask = getTask();
			if (oldTask == null || !oldTask.eq(task)) {
				getState()
						.put(TASK, JOM.getInstance().writeValueAsString(task));
				getEventsFactory().trigger("newTask");
				EventBus.getDefault().post(new StateEvent(getId(), "newTask"));
			} else {
				System.out.println("Repeated receival of task.");
			}
		} else {
			System.err.println("Received empty/null task");
		}
	}
	
	public void setSitrep(@Name("sitRep") SitRep sitRep) throws IOException {
		if (sitRep != null) {
			SitRep oldSitRep = getSitRep();
			if (oldSitRep == null || !oldSitRep.eq(sitRep)) {
				getState().put(SITREP,
						JOM.getInstance().writeValueAsString(sitRep));
				getEventsFactory().trigger("newSitRep");
				EventBus.getDefault()
						.post(new StateEvent(getId(), "newSitRep"));
			} else {
				System.out.println("Repeated receival of sitRep.");
			}
		} else {
			System.err.println("Received empty/null sitRep");
		}
	}
	
	public void setTeamStatus(@Name("teamStatus") TeamStatus teamStatus)
			throws IOException {
		if (teamStatus != null) {
			TeamStatus oldTeamStatus = getTeamStatus();
			if (oldTeamStatus == null || !oldTeamStatus.eq(teamStatus)) {
				getState().put(STATUS,
						JOM.getInstance().writeValueAsString(teamStatus));
				getEventsFactory().trigger("newTeamStatus");
				EventBus.getDefault().post(
						new StateEvent(getId(), "newTeamStatus"));
			} else {
				System.out.println("Repeated receival of teamStatus.");
			}
		} else {
			System.err.println("Received empty/null teamStatus");
		}
	}
	
	public void updateTaskStatus(@Name("task") Task task) throws IOException {
		if (task != null) {
			Task oldTask = getTask();
			if (oldTask == null) {
				System.out
						.println("Warning: updateTaskStatus() called with task, but old task can't be found!");
				oldTask = task;
			}
			if (oldTask.eq(task)) {
				getState()
						.put(TASK, JOM.getInstance().writeValueAsString(task));
				getEventsFactory().trigger("taskUpdated");
				EventBus.getDefault().post(
						new StateEvent(getId(), "taskUpdated"));
			} else {
				System.out
						.println("Warning: Not updating task status, because another task is found!");
			}
		} else {
			System.err
					.println("Warning: Not updating task status, empty/null task given.");
		}
	}
	
	public void updateTeamStatus(@Name("team") TeamStatus team)
			throws IOException {
		if (team != null) {
			getState().put(STATUS, JOM.getInstance().writeValueAsString(team));
			// TODO: use real data
			// DummyData.getInstance().setTeamStatus(team);
			getEventsFactory().trigger("teamStatusUpdated");
			EventBus.getDefault().post(
					new StateEvent(getId(), "teamStatusUpdated"));
			
		} else {
			System.err
					.println("Warning: Not updating team status, empty/null team status given.");
		}
	}
	
	public void delTask() {
		getState().remove(TASK);
	}
	
	public void delTeamStatus() {
		getState().remove(STATUS);
	}
	
	public void reconnect() {
		try {
			XmppService xmppService = (XmppService) getAgentHost()
					.getTransportService("xmpp");
			xmppService.disconnect(getId());
			SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(context);
			String username = prefs.getString(
					context.getString(R.string.xmppUsername_key), "unset");
			String password = prefs.getString(
					context.getString(R.string.xmppPassword_key), "unset");
			String resource = prefs.getString(
					context.getString(R.string.xmppResource_key), null);
			xmppService.connect(getId(), username, password, resource);
		} catch (Exception e) {
			System.err.println("Failed to (re-)connection XMPP connection");
			e.printStackTrace();
		}
	}
	
	public void callRedirect() {
		ObjectNode params = JOM.createObjectNode();
		params.put("address", "+31624495602");
		params.put("url",
				"http://ask70.ask-cs.nl/~ask/askfastdemo/redirect?phone=0107421239");
		params.put("adapterID", "fe8aeeb0-3fb3-11e2-be8a-00007f000001");
		params.put("publicKey", "askfast1@ask-cs.com");
		params.put("privateKey", "47cdebf0-7131-11e2-8945-060dc6d9dd94");
		try {
			sendAsync(URI.create("http://ask-charlotte.appspot.com/rpc"),
					"outboundCall", params, new AsyncCallback<Void>() {
						public void onSuccess(Void result) {
						}
						
						public void onFailure(Exception exception) {
						}
					}, Void.class);
		} catch (Exception e) {
			System.err.println("Failed to call outboundCall.");
			e.printStackTrace();
		}
	}
}
