package io.thingshub.config;

import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;

@Builder
@Getter
public class TenantSettings {

	public static int DFT_MAX_MESSAGE_EXPIRY_INTERVAL = 7 * 24 * 60 * 60; // seconds

	public static int DFT_MAX_SESSION_EXPIRY_INTERVAL = 24 * 60 * 60; // seconds

	private String tenant;

	@Default
	private boolean mqtt3Enabled = true;

	@Default
	private boolean mqtt4Enabled = true;

	@Default
	private boolean mqtt5Enabled = true;

	@Default
	public boolean payloadFormatValidationEnabled = true;

	@Default
	private boolean retainEnabled = true;

	@Default
	private boolean wildcardSubscriptionEnabled = true;

	@Default
	private boolean subscriptionIdentifierEnabled = true;

	@Default
	private boolean sharedSubscriptionEnabled = true;

	@Default
	private MqttQoS maxMqttQoS = MqttQoS.valueOf(1);

	@Default
	private int maxSessionExpiryInterval = DFT_MAX_SESSION_EXPIRY_INTERVAL; // seconds

	@Default
	private int maxTopicLevelLength = 40;

	@Default
	private int maxTopicLevels = 16;

	@Default
	private int maxTopicLength = 255;

	@Default
	private int maxPacketSize = 1 << 15;

	@Default
	private int maxTopicAlias = 10;

	@Default
	private long inboundBandwidth = 1 << 19L;// bytes

	@Default
	private long outboundBandwidth = 1 << 19L;// bytes

	@Default
	private int receiveMaximum = 256;

//    public final int maxMsgPerSec;

	@Default
	private int maxResendTimes = 3;

//    public int resendTimeout;

	@Default
	private int maxTopicFiltersPerSub = 10;
//    public final int inboxQueueLength;
//    public final boolean inboxDropOldest;
//    public final int retainMatchLimit;

	@Default
	private int maxMessageExpiryInterval = DFT_MAX_MESSAGE_EXPIRY_INTERVAL; // seconds

}
