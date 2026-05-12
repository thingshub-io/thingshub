package io.thingshub.dashboard.server;

import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

import io.thingshub.http.annotation.HasAuthority;

/**
 * <p>
 * Check user's authority
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Aspect
public class CheckAuthorityAspect {

	@Pointcut("@annotation(io.thingshub.transport.http.annotation.HasAuthority)")
	public void checkAuthorityPointCut() {
	}

	@Around("checkAuthorityPointCut()")
	public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		Method method = signature.getMethod();
		HasAuthority theAnnotation = method.getAnnotation(HasAuthority.class);
		if (theAnnotation == null) {
			return joinPoint.proceed();
		}

		UserInfo currentUser = UserContextHolder.getCurrentUser();
		if (currentUser == null || currentUser.getAuthorities() == null) {
			throw new RuntimeException("没有操作权限");
		}

		String authorityId = theAnnotation.value();
		boolean hasAuthority = false;
		if (currentUser.getAuthorities().contains("0") || currentUser.getAuthorities().contains(authorityId)) {
			hasAuthority = true;
		}

		if (!hasAuthority) {
			throw new RuntimeException("没有操作权限");
		}

		return joinPoint.proceed();
	}

}