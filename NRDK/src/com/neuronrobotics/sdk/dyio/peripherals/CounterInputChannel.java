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
package com.neuronrobotics.sdk.dyio.peripherals;

import java.util.ArrayList;

import com.neuronrobotics.sdk.common.ByteList;
import com.neuronrobotics.sdk.dyio.DyIOChannel;
import com.neuronrobotics.sdk.dyio.DyIOChannelEvent;
import com.neuronrobotics.sdk.dyio.DyIOChannelMode;
import com.neuronrobotics.sdk.dyio.IChannelEventListener;

/**
 * 
 */
public class CounterInputChannel extends DyIOAbstractPeripheral implements IChannelEventListener {
	private ArrayList<ICounterInputListener> listeners = new ArrayList<ICounterInputListener>();
	
	/**
	 * CounterChannel.
	 * 
	 * @param channel
	 *            The channel object to set up as counter
	 */
	public CounterInputChannel(DyIOChannel channel) {
		super(channel);
		init(channel,false);
	}
	
	/**
	 * 
	 * 
	 * @param channel
	 * @param isAsync
	 */
	public CounterInputChannel(DyIOChannel channel,boolean isAsync) {
		super(channel);
		init(channel,isAsync);
	}
	
	private void init(DyIOChannel channel,boolean isAsync){
		DyIOChannelMode mode = DyIOChannelMode.COUNT_IN_INT;
		channel.addChannelEventListener(this);
		if(!channel.setMode(mode, isAsync)) {
			throw new DyIOPeripheralException("Could not set channel " + channel + " to " + mode + " mode.");
		}
		channel.resync(true);
	}
	
	/**
	 * addCounterInputListener.
	 * 
	 * @param l
	 *            add this listener to this channels event listeners
	 */
	public void addCounterInputListener(ICounterInputListener l) {
		if(listeners.contains(l)) {
			return;
		}
		
		listeners.add(l);
	}
	
	/**
	 * removeCounterInputListener.
	 * 
	 * @param l
	 *            remove this listener to this channels event listeners
	 */
	public void removeCounterInputListener(ICounterInputListener l) {
		if(!listeners.contains(l)) {
			return;
		}
		
		listeners.add(l);
	}
	
	/**
	 * 
	 */
	public void removeAllCounterInputListeners() {
		listeners.clear();
	}
	
	/**
	 * 
	 * 
	 * @param value
	 */
	protected void fireOnCounterInput(int value) {
		for(ICounterInputListener l : listeners) {
			l.onCounterValueChange(this, value);
		}
	}
	
	/**
	 * onChannelEvent Send the counter value to all the listening objects.
	 * 
	 * @param e
	 */
	@Override
	public void onChannelEvent(DyIOChannelEvent e) {
		byte []  b = e.getData().getBytes();
		int data = ByteList.convertToInt(b,true);
		fireOnCounterInput(data);
	}
	
	/**
	 * 
	 * 
	 * @param isAsync
	 */
	public void setAsync(boolean isAsync) {
		setMode(DyIOChannelMode.COUNT_IN_INT, isAsync);
	}
	
	/* (non-Javadoc)
	 * @see com.neuronrobotics.sdk.dyio.peripherals.DyIOAbstractPeripheral#setValue(int)
	 */
	@Override
	public boolean setValue(int value){
		ByteList b = new ByteList();
		b.addAs32(value);
		return setValue(b);
	}
}