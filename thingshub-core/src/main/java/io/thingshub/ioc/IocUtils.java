package io.thingshub.ioc;

public abstract class IocUtils {

	public static boolean isPackageValid(final Class<?> type) {
		boolean res = false;
		if (type != null) {
			final Package packaj = type.getPackage();
			res = !(packaj == null || packaj.getName().startsWith("java"));
		}

		return res;
	}

}
