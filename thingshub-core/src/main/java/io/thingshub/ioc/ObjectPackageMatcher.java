package io.thingshub.ioc;

import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matcher;

public class ObjectPackageMatcher<T> implements Matcher<T> {

	private final String pkg;

	public ObjectPackageMatcher(final String pkg) {
		this.pkg = pkg;
	}

	@Override
	public boolean matches(final T o) {
		final Class<?> type = o instanceof TypeLiteral ? ((TypeLiteral) o).getRawType() : o.getClass();

		return IocUtils.isPackageValid(type) && type.getPackage().getName().startsWith(pkg);
	}
}
