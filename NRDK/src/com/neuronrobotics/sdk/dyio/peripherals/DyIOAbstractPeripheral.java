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

import com.neuronrobotics.sdk.commands.bcs.io.SetChannelValueCommand;
import com.neuronrobotics.sdk.common.ISendable;
import com.neuronrobotics.sdk.common.InvalidResponseException;
import com.neuronrobotics.sdk.dyio.DyIOChannel;
import com.neuronrobotics.sdk.dyio.DyIOChannelMode;
import com.neuronrobotics.sdk.dyio.IDyIOChannel;

/**
 * 
 */
public abstract class DyIOAbstractPeripheral implements IDyIOChannel {
	private IDyIOChannel channel;
	private boolean enabled = false;
	
	/**
	 * DyIOAbstractPeripheral.
	 * 
	 * @param channel
	 *            The channel object to set up as whatever peripheral is needed
	 */
	public DyIOAbstractPeripheral(IDyIOChannel channel) {
		this.channel = channel;
		this.enabled = true;
	}
	
	/**
	 * isEnabled.
	 * 
	 * @return if the channel is enabled
	 */
	public boolean isEnabled() {
		return enabled;
	}
	
	/**
	 * setMode.
	 * 
	 * @param mode
	 *            the DyIO mode to set the channel to
	 * @return if the set worked. Not all channels have all peripherals
	 * @throws InvalidResponseException
	 */
	public boolean setMode(DyIOChannelMode mode)throws InvalidResponseException {
		return setMode(mode, false);
	}
	
	/**
	 * setMode.
	 * 
	 * @param mode
	 *            the DyIO mode to set the channel to
	 * @param async
	 *            If the channel should be set into async mode
	 * @return if the set worked. Not all channels have all peripherals
	 * @throws InvalidResponseException
	 */
	@Override
	public boolean setMode(DyIOChannelMode mode, boolean async)throws InvalidResponseException {
		return channel.setMode(mode, async);
	}
	
	/**
	 * getChannel.
	 * 
	 * @return returns the channel object
	 */
	@Override
	public DyIOChannel getChannel() {
		return channel.getChannel();
	}
	
	/**
	 * getMode.
	 * 
	 * @return returns the mode of this channel
	 */
	@Override
	public DyIOChannelMode getMode() {
		return channel.getMode();
	}
	
	/**
	 * setValue.
	 * 
	 * @param value
	 *            Sets this value to the channel
	 * @return if the set worked
	 * @throws InvalidResponseException
	 */
	@Override
	public boolean setValue(int value) throws InvalidResponseException {
		return channel.setValue(value);
	}
	
	/**
	 * setValue.
	 * 
	 * @param value
	 *            Sets this value to the channel
	 * @return if the set worked
	 * @throws InvalidResponseException
	 */
	@Override
	public boolean setValue(ISendable value)throws InvalidResponseException {
		return channel.setValue(value);
	}
	
	/**
	 * getValue.
	 * 
	 * @return the value of the channel on the DyIO
	 * @throws InvalidResponseException
	 */
	@Override
	public int getValue()throws InvalidResponseException {
		return channel.getValue();
	}
	
	
	/**
	 * SavePosition.
	 * 
	 * @param pos
	 *            the position to set as the new starting point for the channel
	 * @return if the save worked or not.
	 */
	public boolean SavePosition(int pos){
		try {
			DyIOChannelMode mode = getChannel().getMode();
			switch(mode){
			case SERVO_OUT:
			case PWM_OUT :
				getChannel().send(new SetChannelValueCommand(getChannel().getNumber(), pos , getMode(), true));
				return true;
			default:
				return false;
			}
			
		} catch (InvalidResponseException e) {
			return false;
		}
	}

	public boolean flush() {
		return getChannel().flush();
	}
}