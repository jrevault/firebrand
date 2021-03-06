/*
 * Copyright (C) 2012 47 Degrees, LLC
 * http://47deg.com
 * hello@47deg.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.firebrandocm.dao.cql.clauses;

/**
 * TTL <seconds>
 * Optional clause to specify a time-to-live (TTL) period for an inserted or updated column. TTL columns
 * are automatically marked as deleted (with a tombstone) after the requested amount of time has expired.
 */
public class TTL implements WriteOption {
    /* Fields */

	private long seconds;

    /* Constructors */

	public TTL(long seconds) {
		this.seconds = seconds;
	}

    /* Canonical Methods */

	@Override
	public String toString() {
		return String.format("TTL %s", seconds);
	}
}
