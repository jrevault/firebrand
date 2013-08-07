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

package org.firebrandocm.dao.impl;

import org.firebrandocm.dao.TypeConverter;

import java.nio.ByteBuffer;
import java.util.Date;

/**
 * Type converter to serialize to and from ByteBuffer's
 */
public class DateTypeConverter implements TypeConverter<Date> {
    /* Fields */

	private TypeConverter<Long> longTypeConverter = new LongTypeConverter();

    /* Interface Implementations */


// --------------------- Interface TypeConverter ---------------------

	public ByteBuffer toValue(Date value) throws Exception {
		return longTypeConverter.toValue(value.getTime());
	}

	public Date fromValue(ByteBuffer value, Class<Date> targetType) throws Exception {
		Long time = longTypeConverter.fromValue(value, Long.class);
		return time != null ? new Date(time) : null;
	}
}
