/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.flume.interceptor;

import java.nio.charset.Charset;
import java.util.List;

import org.apache.flume.Context;
import org.apache.flume.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;

public class SubInterceptor implements Interceptor {

	private static final Logger logger = LoggerFactory.getLogger(SubInterceptor.class);

	private final Charset charset;

	private SubInterceptor(Charset charset) {
		this.charset = charset;
	}

	@Override
	public void initialize() {
	}

	@Override
	public void close() {
	}

	@Override
	public Event intercept(Event event) {
		String origBody = new String(event.getBody(), charset);
		if (origBody.indexOf("{") >= 0) {
			origBody = origBody.substring(origBody.indexOf("{"));
		}
		event.setBody(origBody.getBytes(charset));
		return event;
	}

	@Override
	public List<Event> intercept(List<Event> events) {
		for (Event event : events) {
			intercept(event);
		}
		return events;
	}

	public static class Builder implements Interceptor.Builder {
		private static final String CHARSET_KEY = "charset";

		private Charset charset = Charsets.UTF_8;

		@Override
		public void configure(Context context) {

			if (context.containsKey(CHARSET_KEY)) {
				// May throw IllegalArgumentException for unsupported charsets.
				charset = Charset.forName(context.getString(CHARSET_KEY));
			}
		}

		@Override
		public Interceptor build() {
			return new SubInterceptor(charset);
		}
	}
}
