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
package com.neuronrobotics.sdk.mock.dyio;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.neuronrobotics.sdk.common.Log;

/**
 * 
 */
public class MockDyIO implements IMockDevice {

	private StringInputStream sis = new StringInputStream();
	private StringOutputStream sos = new StringOutputStream();
	
	/* (non-Javadoc)
	 * @see com.neuronrobotics.sdk.mock.dyio.IMockDevice#getInputStream()
	 */
	@Override
	public InputStream getInputStream() {
		return sis;
	}

	/* (non-Javadoc)
	 * @see com.neuronrobotics.sdk.mock.dyio.IMockDevice#getOutputStream()
	 */
	@Override
	public OutputStream getOutputStream() {
		return sos;
	}
	
	private class StringInputStream extends InputStream {
		
		private StringBuilder buffer = new StringBuilder();
		
		@Override
		public int read() throws IOException {
			return buffer.length();
		}
	}
	
	private class StringOutputStream extends OutputStream {
		@Override
		public void write(int b) throws IOException {
			Log.info("MC << " + b);
		}
	}
	
}