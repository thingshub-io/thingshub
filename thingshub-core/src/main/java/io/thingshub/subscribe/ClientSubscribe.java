package io.thingshub.subscribe;

import java.util.Map;
import java.util.Objects;

import io.thingshub.service.model.ClientType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientSubscribe {

	private String clientId;

	private ClientType clientType;

	private String group;

	private String topicFilter;

	private Map<String, Object> props;

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		ClientSubscribe that = (ClientSubscribe) o;

		return Objects.equals(this.clientId, that.clientId) && Objects.equals(this.group, that.group)
				&& Objects.equals(this.topicFilter, that.topicFilter);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.clientId, this.group, this.topicFilter);
	}

	@Override
	public String toString() {
		return "{clientId='" + this.clientId + "', group='" + this.group + "', topicFilter='" + topicFilter + "', props='" + props.toString() + "'}";
	}
}
