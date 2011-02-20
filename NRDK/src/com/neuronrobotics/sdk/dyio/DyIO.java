/*******************************************************************************
 * Copyright 2010 Neuron Robotics, LLC
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.neuronrobotics.sdk.dyio;

import java.util.ArrayList;
import java.util.Collection;

import com.neuronrobotics.sdk.commands.bcs.io.GetChannelModeCommand;
import com.neuronrobotics.sdk.commands.bcs.io.SetAllChannelValuesCommand;
import com.neuronrobotics.sdk.commands.bcs.io.setmode.SetChannelModeCommand;
import com.neuronrobotics.sdk.commands.bcs.pid.DyPID.ConfigureDynamicPIDCommand;
import com.neuronrobotics.sdk.commands.neuronrobotics.dyio.InfoCommand;
import com.neuronrobotics.sdk.common.BowlerAbstractConnection;
import com.neuronrobotics.sdk.common.BowlerAbstractDevice;
import com.neuronrobotics.sdk.common.BowlerDatagram;
import com.neuronrobotics.sdk.common.ByteList;
import com.neuronrobotics.sdk.common.ISendable;
import com.neuronrobotics.sdk.common.InvalidConnectionException;
import com.neuronrobotics.sdk.common.InvalidResponseException;
import com.neuronrobotics.sdk.common.Log;
import com.neuronrobotics.sdk.common.MACAddress;
import com.neuronrobotics.sdk.dyio.dypid.DyPIDConfiguration;
import com.neuronrobotics.sdk.genericdevice.GenericPIDDevice;
import com.neuronrobotics.sdk.pid.IPIDControl;
import com.neuronrobotics.sdk.pid.IPIDEventListener;
import com.neuronrobotics.sdk.pid.PIDConfiguration;
import com.neuronrobotics.sdk.util.ThreadUtil;

/**
 * 
 */
public class DyIO extends BowlerAbstractDevice implements IPIDControl {

	private ArrayList<IDyIOEventListener> listeners = new ArrayList<IDyIOEventListener>();
	private ArrayList<DyIOChannel> channels = new ArrayList<DyIOChannel>();
	
	private byte [] firmware = {0, 0, 0};
	private String info = "Unknown";
	
	private DyIOPowerState bankAState;
	private DyIOPowerState bankBState;
	
	private boolean cachedMode=false;
	private GenericPIDDevice pid = new GenericPIDDevice();
	/**
	 * Default Constructor.
	 * Builds a generic DyIO that has the default broadcast address and no default connection.
	 */
	public DyIO() {
		setAddress(new MACAddress(MACAddress.BROADCAST));
		pid.setAddress(new MACAddress(MACAddress.BROADCAST));
	}

	/**
	 * Builds a DyIO that has the given address and no default connection.
	 * @param address
	 */
	public DyIO(MACAddress address) {
		setAddress(address);
		pid.setAddress(address);
	}
	
	/**
	 * Builds a DyIO with the given connection and the broadcast address.
	 * @param connection
	 */
	public DyIO(BowlerAbstractConnection connection) {
		setAddress(new MACAddress(MACAddress.BROADCAST));
		setConnection(connection);
		pid.setConnection(connection);
		pid.setAddress(new MACAddress(MACAddress.BROADCAST));
	}

	/**
	 * Builds a DyIO with the given address and connection.
	 * @param address
	 * @param connection
	 */
	public DyIO(MACAddress address, BowlerAbstractConnection connection) {
		setAddress(address);
		setConnection(connection);
		pid.setConnection(connection);
		pid.setAddress(address);
	}

	/**
	 * Returns the DyIO channel associated with a channel number.
	 * 
	 * @param channel
	 *            - a channel number
	 * @return
	 */
	public DyIOChannel getChannel(int channel) {
		validateChannel(channel);
		return channels.get(channel);
	}
	
	/**
	 * 
	 * 
	 * @param channel
	 * @param mode
	 * @return
	 */
	public boolean setMode(int channel, DyIOChannelMode mode) {
		return getChannel(channel).setMode(mode);
	}

	/**
	 * 
	 * 
	 * @param channel
	 * @param mode
	 * @param async
	 * @return
	 */
	public boolean setMode(int channel, DyIOChannelMode mode, boolean async) {
		return getChannel(channel).setMode(mode, async);
	}
	
	/**
	 * 
	 * 
	 * @param channel
	 * @return
	 */
	public DyIOChannelMode getMode(int channel) {
		return getChannel(channel).getMode();
	}
	
	/**
	 * 
	 * 
	 * @param channel
	 * @param value
	 * @return
	 */
	public boolean setValue(int channel, int value) {
		return getChannel(channel).setValue(value);
	}
	
	/**
	 * 
	 * 
	 * @param channel
	 * @param value
	 * @return
	 */
	public boolean setValue(int channel, ISendable value) {
		return getChannel(channel).setValue(value);
	}

	/**
	 * 
	 * 
	 * @param channel
	 * @return
	 */
	public int getValue(int channel) {
		return getChannel(channel).getValue();
	}
	
	/**
	 * 
	 * 
	 * @return
	 */
	public String getInfo(){
		return info;
	}
	
	/**
	 * 
	 * 
	 * @param info
	 */
	public void setInfo(String info){
		if(send(new InfoCommand(info)) == null) {
			return;
		}
		this.info = info;
	}
	
	/**
	 * 
	 * 
	 * @return
	 */
	public byte [] getFirmwareRev(){
		return firmware;
	}
	
	/**
	 * 
	 * 
	 * @return
	 */
	public String getFirmwareRevString(){
		String revFmt = "%02d.%02d.%03d";
		return "Firmware Revision: v" + String.format(revFmt, firmware[0], firmware[1], firmware[2]);
	}

	/**
	 * 
	 * 
	 * @return
	 */
	public Collection<DyIOChannel> getChannels() {
		ArrayList<DyIOChannel> c = new ArrayList<DyIOChannel>();
		c.addAll(channels);
		return c;
	}
	
	/**
	 * 
	 * 
	 * @param channel
	 */
	public void resync(int channel) {
		getChannel(channel).resync(false);
	}
	
	/**
	 * Sync the state cache with the live device. 
	 * 
	 * @return true if the sync was successful
	 */
	public boolean resync() {
		if(!getConnection().isConnected()) {
			System.err.println("No connection");
			return false;
		}
		Log.info("Re-syncing...");
		BowlerDatagram response;
		if (!haveFirmware()){
			firmware = getRevisions().get(0).getBytes();
			/*
			response = send(new InfoFirmwareRevisionCommand());
			if (response != null && response.getData().size() > 2) {
				firmware = response.getData().getBytes(0, 3);
			}
			*/
		}
		if(info.contains("Unknown")){
			response = send(new InfoCommand());
			if (response != null) {
				info = response.getData().asString();
			}
		}
		
		try {
			response = send(new GetChannelModeCommand());
		} catch (Exception e) {
			if (channels.size()==0){
				Log.error("Initilization failed once, retrying");
				try{
					response = send(new GetChannelModeCommand());
				}catch(Exception e2){
					e2.printStackTrace();
					throw new DyIOCommunicationException("DyIO failed to report during initialization. Could not determine DyIO configuration: "+e2.getMessage());
				}
			}
			else
				return false;
		}
		
		if(getAddress().equals(new MACAddress(MACAddress.BROADCAST))) {
			setAddress(response.getAddress());
		}
		
		if (channels.size()==0){
			channels.clear();
		}
		if (response.getData().size()<24)
			throw new DyIOCommunicationException("Not enough channels, not a valid DyIO"+response.toString());
		for (int i = 0; i < response.getData().size(); i++){
			DyIOChannelMode cm = DyIOChannelMode.get(response.getData().getByte(i));
			boolean editable = true;
			if(cm == null) {
				cm = DyIOChannelMode.DIGITAL_OUT;
				editable = false;
				try {
					send(new SetChannelModeCommand(i, cm));
				} catch(Exception e) {
					throw new DyIOCommunicationException("Setting a pin to Digital In failed");
				}
			}
			try{
				channels.get(i).update(this, i, cm, editable);
			}catch(IndexOutOfBoundsException e){
				channels.add(new DyIOChannel(this, i, cm, editable));
			}
		}
		if (channels.size()==0)
			throw new DyIOCommunicationException("DyIO failed to report during initialization");
		
		return true;
	}
	
	
	private boolean haveFirmware() {
		if (firmware[0]==0 && firmware[1]==0 && firmware[2]==0)
			return false;
		else 
			return true;
	}

	/**
	 * Add an IDyIOEventListener that will be contacted with an IDyIOEvent on
	 * each incoming data event.
	 * 
	 * @param l
	 */
	public void addDyIOEventListener(IDyIOEventListener l) {
		if(listeners.contains(l)) {
			return;
		}
		
		listeners.add(l);
	}
	
	/**
	 * Removes an IDyIOEventListener from being contacted on each new
	 * IDyIOEvent.
	 * 
	 * @param l
	 */
	public void removeDyIOEventListener(IDyIOEventListener l) {
		if(!listeners.contains(l)) {
			return;
		}
		
		listeners.remove(l);
	}
	
	/**
	 * Clears out all current IDyIOEventListeners.
	 */
	public void removeAllDyIOEventListeners() {
		listeners.clear();
	}
	
	/**
	 * Contact all of the listeners with the given event.
	 * 
	 * @param e
	 *            - the event to fire to all listeners
	 */
	public void fireDyIOEvent(IDyIOEvent e) {
		for(IDyIOEventListener l : listeners) {
			l.onDyIOEvent(e);
		}
	}
	
	/**
	 * Validates a that a given channel is in the proper range.
	 * 
	 * @param channel
	 */
	protected void validateChannel(int channel) {
		int syncs=0;
		while(channels.size() == 0){
			Log.error("Valadate will fail, no channels, resyncing");
			resync();
			syncs++;
			if(syncs>5){
				throw new DyIOCommunicationException("DyIO failed to report number of channels");
			}
			ThreadUtil.wait(200);
			
		}
		if (channel < 0 || channel > channels.size()) {
			throw new IndexOutOfBoundsException("DyIO channels must be between 0 and " + channels.size());
		}
	}
	
	/**
	 * 
	 * 
	 * @param printing
	 * @param debug
	 */
	public void SetPrintModes(boolean printing,boolean debug){
		//BOB, do not remove, i need this in here. 
		Log.enableSystemPrint(printing);
		Log.enableDebugPrint(debug);
	}
	
	/**
	 * 
	 */
	public void enableDebug() {
		SetPrintModes(true,true);
	}
	
	/**
	 * 
	 */
	public void disableDebug() {
		SetPrintModes(false,false);
	}
	
	public DyIOPowerState getBankAState() {
		return bankAState;
	}

	public DyIOPowerState getBankBState() {
		return bankBState;
	}
	
	public boolean getCachedMode() {
		return cachedMode;
	}
	
	public void setCachedMode(boolean mode) {
		cachedMode=mode;
		for(DyIOChannel d:channels) {
			d.setCachedMode(mode);
			if(mode)
				try {
					d.setCachedValue(d.getValue());
				}catch(Exception e){
					d.setCachedValue(128);
				}
		}
	}
	
	public void flushCache(float time) {
		//System.out.println("Updating all channels");
		int [] values = new int[channels.size()];
		int i=0;
		for(DyIOChannel d:channels) {
			values[i++]=d.getCachedValue();
			//d.flush();
		}
		for(int i1=0;i1<5;i1++) {
			try {
				send(new SetAllChannelValuesCommand(time,values));
				return;
			}catch (InvalidResponseException e1) {
				//System.err.println("Failed to update all, retrying");
			}
		}
		
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		
		String chFmt = "%02d - %-20s   %02d - %-20s\n";
		
		String s = getInfo() + "\n" + getFirmwareRevString()+ " \nMAC: " + getAddress() + "\n";
		for(int i = 0; i < channels.size()/2; i++) {
			s += String.format(chFmt, 
					           channels.size() - 1 - i, 
					           channels.get(channels.size()-1-i).getMode(), 
					           i, 
					           channels.get(i).getMode());
		}
		
		return s;
	}
	
	/* (non-Javadoc)
	 * @see com.neuronrobotics.sdk.common.BowlerAbstractDevice#isAvailable()
	 */
	@Override
	public boolean isAvailable() throws InvalidConnectionException {
		return resync();
	}

	/* (non-Javadoc)
	 * @see com.neuronrobotics.sdk.common.IBowlerDatagramListener#onAllResponse(com.neuronrobotics.sdk.common.BowlerDatagram)
	 */
	@Override
	public void onAllResponse(BowlerDatagram data) {
		IDyIOEvent e = new DyIOAsyncEvent(data);
		for(IDyIOEventListener l : listeners) {
			l.onDyIOEvent(e);
		}
		pid.onAllResponse(data);
	}

	/* (non-Javadoc)
	 * @see com.neuronrobotics.sdk.common.IBowlerDatagramListener#onAsyncResponse(com.neuronrobotics.sdk.common.BowlerDatagram)
	 */
	@Override
	public void onAsyncResponse(BowlerDatagram data) {
		Log.info("<< Async\n"+data.toString());
		if(data.getRPC().equals("gchv")) {
			ByteList bl = data.getData();
			
			Byte b = bl.pop();
			if(b == null) {
				return;
			}
			DyIOChannel c = getChannel(b);
			c.fireChannelEvent(new DyIOChannelEvent(c, bl));
			return;
		}
		
		if(data.getRPC().equals("_pwr")) {
			ByteList bl = data.getData();
			if(bl.size() != 4) {
				return;
			}
			bankAState = DyIOPowerState.valueOf(bl.get(0));
			bankBState = DyIOPowerState.valueOf(bl.get(1));
			
			fireDyIOEvent(new DyIOPowerEvent(bankAState, bankBState));
			return;
		}
		pid.onAsyncResponse(data);
	}

	/**
	 * PID controller (new as of 0.3.6)
	 */
	
	
	public boolean ConfigureDynamicPIDChannels(DyPIDConfiguration config){
		return send(new ConfigureDynamicPIDCommand(config))!=null;
	}
	public DyPIDConfiguration getDyPIDConfiguration(char group){
		BowlerDatagram conf = send(new ConfigureDynamicPIDCommand( group) );
		DyPIDConfiguration back=new DyPIDConfiguration(conf);
		return back;
	}
	
	public boolean ResetPIDChannel(int group) {
		return pid.ResetPIDChannel(group);
	}
	@Override
	public boolean ResetPIDChannel(int group, int valueToSetCurrentTo) {
		return pid.ResetPIDChannel(group, valueToSetCurrentTo);
	}
	public boolean SetPIDSetPoint(int group,int setpoint, double seconds){
		return pid.SetPIDSetPoint(group, setpoint,seconds);
	}
	public boolean SetAllPIDSetPoint(int []setpoints, double seconds){
		return pid.SetAllPIDSetPoint(setpoints,seconds);
	}
	public int GetPIDPosition(int group) {
		return pid.GetPIDPosition(group);
	}
	public int [] GetAllPIDPosition() {
		return pid.GetAllPIDPosition();
	}
	public boolean ConfigurePIDController(PIDConfiguration config) {
		return pid.ConfigurePIDController(config);
	}
	public PIDConfiguration getPIDConfiguration(int group) {
		return pid.getPIDConfiguration(group);
	}
	public void addPIDEventListener(IPIDEventListener l) {
		pid.addPIDEventListener(l);
	}
	@Override
	public boolean connect(){
		if(super.connect()){
			pid.setConnection(getConnection());
			pid.setAddress(getAddress());
			pid.connect();
			startHeartBeat(2000);
			return true;
		}
		return false;
	}

	
}