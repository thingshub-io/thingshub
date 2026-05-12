package io.thingshub.dashboard.server;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

/**
 * <p>
 * System User Info
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */
@Data
public class UserInfo implements Serializable {

	private static final long serialVersionUID = 2356935440234846391L;

	private Long id;

	private String name;

	private String nick;

	private String mobile;

	private String avatar;

	private Long tenantId;

	private Integer status;

	private List<String> authorities;

}
