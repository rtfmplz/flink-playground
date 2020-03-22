package org.apache.flink.playgrounds.ops.clickcount.records;

import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * A Kafka {@link DeserializationSchema} to deserialize {@link ClickEvent}s from JSON.
 *
 */
public class ClickEventDeserializationSchema implements DeserializationSchema<ClickEvent> {

	private static final long serialVersionUID = 1L;

	private static final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public ClickEvent deserialize(byte[] message) throws IOException {
		return objectMapper.readValue(message, ClickEvent.class);
	}

	@Override
	public boolean isEndOfStream(ClickEvent nextElement) {
		return false;
	}

	@Override
	public TypeInformation<ClickEvent> getProducedType() {
		return TypeInformation.of(ClickEvent.class);
	}
}
