package io.thingshub.subscribe;

import java.util.Map;
import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MatchedSubscriber {

	private final String subscriberId;

	private final String topicFilter;

	private final Map<String, Object> props;

	private final String group;

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		MatchedSubscriber that = (MatchedSubscriber) o;

		return Objects.equals(this.subscriberId, that.subscriberId) && Objects.equals(this.topicFilter, that.topicFilter)
				&& Objects.equals(this.group, that.group);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.subscriberId, this.topicFilter, this.props, this.group);
	}
}
