package io.thingshub.service;

import io.thingshub.entity.GroupSelection;
import io.thingshub.ioc.Service;
import io.thingshub.service.GroupSelectionService.GroupSelectionKey;
import io.thingshub.service.base.BaseService;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * Selection in Group
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Service
public class GroupSelectionService extends BaseService<GroupSelectionKey, GroupSelection> {

	@AllArgsConstructor
	@Getter
	public static class GroupSelectionKey {

		private String dataId;

		private String group;

	}

}
