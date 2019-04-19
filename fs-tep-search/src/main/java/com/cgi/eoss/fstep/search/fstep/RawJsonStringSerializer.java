package com.cgi.eoss.fstep.search.fstep;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;


public class RawJsonStringSerializer extends StdSerializer<RawJsonString> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5147609744389144227L;

	public RawJsonStringSerializer(Class<RawJsonString> t) {
		super(t);
	}

	public RawJsonStringSerializer() {
		super(RawJsonString.class);
	}

	@Override
	public void serialize(RawJsonString rawJsonString, JsonGenerator jgen, SerializerProvider sp)
			throws IOException, JsonGenerationException {
		jgen.writeRawValue(rawJsonString.getRawJson());
	}
}