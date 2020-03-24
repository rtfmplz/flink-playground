package org.apache.flink.playgrounds.ops.clickcount.records;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;
import java.util.Objects;

/**
 * A simple event recording a click on a {@link ClickEvent#page} at time {@link ClickEvent#timestamp}.
 *
 */
public class ClickEvent {

	//using java.util.Date for better readability
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss:SSS")
	private Date timestamp;
	private String page;
	private int id;

	public ClickEvent() {
	}

	public ClickEvent(final Date timestamp, final String page, final int id) {
		this.timestamp = timestamp;
		this.page = page;
		this.id = id;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(final Date timestamp) {
		this.timestamp = timestamp;
	}

	public String getPage() {
		return page;
	}

	public void setPage(final String page) {
		this.page = page;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final ClickEvent that = (ClickEvent) o;
		return Objects.equals(timestamp, that.timestamp) && Objects.equals(page, that.page) && Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(timestamp, page, id);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("ClickEvent{");
		sb.append("timestamp=").append(timestamp);
		sb.append(", page='").append(page).append('\'');
		sb.append(", id='").append(id).append('\'');
		sb.append('}');
		return sb.toString();
	}
}
