/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.security.web.method.annotation;

import java.lang.annotation.Annotation;

import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.expression.BeanResolver;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Allows resolving the {@link Authentication#getPrincipal()} using the
 * {@link AuthenticationPrincipal} annotation. For example, the following
 * {@link Controller}:
 *
 * <pre>
 * &#64;Controller
 * public class MyController {
 *     &#64;MessageMapping("/im")
 *     public void im(@AuthenticationPrincipal CustomUser customUser) {
 *         // do something with CustomUser
 *     }
 * }
 * </pre>
 *
 * <p>
 * Will resolve the CustomUser argument using {@link Authentication#getPrincipal()} from
 * the {@link SecurityContextHolder}. If the {@link Authentication} or
 * {@link Authentication#getPrincipal()} is null, it will return null. If the types do not
 * match, null will be returned unless
 * {@link AuthenticationPrincipal#errorOnInvalidType()} is true in which case a
 * {@link ClassCastException} will be thrown.
 *
 * <p>
 * Alternatively, users can create a custom meta annotation as shown below:
 *
 * <pre>
 * &#064;Target({ ElementType.PARAMETER })
 * &#064;Retention(RetentionPolicy.RUNTIME)
 * &#064;AuthenticationPrincipal
 * public @interface CurrentUser {
 * }
 * </pre>
 *
 * <p>
 * The custom annotation can then be used instead. For example:
 *
 * <pre>
 * &#64;Controller
 * public class MyController {
 *     &#64;MessageMapping("/im")
 *     public void im(@CurrentUser CustomUser customUser) {
 *         // do something with CustomUser
 *     }
 * }
 * </pre>
 *
 * @author Rob Winch
 * @since 4.0
 */
public final class AuthenticationPrincipalArgumentResolver implements HandlerMethodArgumentResolver {

	private SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder
		.getContextHolderStrategy();

	private ExpressionParser parser = new SpelExpressionParser();

	private BeanResolver beanResolver;

	/**
	 * 检查方法参数是否包含 {@link AuthenticationPrincipal} 注解
	 * @param parameter the method parameter to check
	 * @return
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return findMethodAnnotation(AuthenticationPrincipal.class, parameter) != null;
	}

	/**
	  * 解析方法参数，将当前认证主体的 principal 对象绑定到控制器方法参数
	  * <p>
	  * 从安全上下文中获取认证信息，提取 principal，并根据注解配置进行可选的
	  * SpEL 表达式求值和类型检查
	  * </p>
	  * @param parameter 方法参数，包含参数类型和注解信息
	  * @param mavContainer 模型和视图容器，本方法不使用此参数
	  * @param webRequest 原生 Web 请求，本方法不使用此参数
	  * @param binderFactory 数据绑定器工厂，本方法不使用此参数
	  * @return 解析后的 principal 对象，如果认证为 null、表达式求值为 null 或类型不匹配时返回 null
	  * @throws ClassCastException 当 principal 类型与方法参数类型不匹配且注解配置了 errorOnInvalidType 为 true 时抛出
	  */
	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
		Authentication authentication = this.securityContextHolderStrategy.getContext().getAuthentication();
		if (authentication == null) {
			return null;
		}
		Object principal = authentication.getPrincipal();
		AuthenticationPrincipal annotation = findMethodAnnotation(AuthenticationPrincipal.class, parameter);
		String expressionToParse = annotation.expression();
		if (StringUtils.hasLength(expressionToParse)) {
			StandardEvaluationContext context = new StandardEvaluationContext();
			context.setRootObject(principal);
			context.setVariable("this", principal);
			context.setBeanResolver(this.beanResolver);
			Expression expression = this.parser.parseExpression(expressionToParse);
			principal = expression.getValue(context);
		}
		if (principal != null && !ClassUtils.isAssignable(parameter.getParameterType(), principal.getClass())) {
			if (annotation.errorOnInvalidType()) {
				throw new ClassCastException(principal + " is not assignable to " + parameter.getParameterType());
			}
			return null;
		}
		return principal;
	}

	/**
	 * Sets the {@link BeanResolver} to be used on the expressions
	 * @param beanResolver the {@link BeanResolver} to use
	 */
	public void setBeanResolver(BeanResolver beanResolver) {
		this.beanResolver = beanResolver;
	}

	/**
	 * Sets the {@link SecurityContextHolderStrategy} to use. The default action is to use
	 * the {@link SecurityContextHolderStrategy} stored in {@link SecurityContextHolder}.
	 *
	 * @since 5.8
	 */
	public void setSecurityContextHolderStrategy(SecurityContextHolderStrategy securityContextHolderStrategy) {
		Assert.notNull(securityContextHolderStrategy, "securityContextHolderStrategy cannot be null");
		this.securityContextHolderStrategy = securityContextHolderStrategy;
	}

	/**
	 * Obtains the specified {@link Annotation} on the specified {@link MethodParameter}.
	 * @param annotationClass the class of the {@link Annotation} to find on the
	 * {@link MethodParameter}
	 * @param parameter the {@link MethodParameter} to search for an {@link Annotation}
	 * @return the {@link Annotation} that was found or null.
	 */
	private <T extends Annotation> T findMethodAnnotation(Class<T> annotationClass, MethodParameter parameter) {
		T annotation = parameter.getParameterAnnotation(annotationClass);
		if (annotation != null) {
			return annotation;
		}
		Annotation[] annotationsToSearch = parameter.getParameterAnnotations();
		for (Annotation toSearch : annotationsToSearch) {
			annotation = AnnotationUtils.findAnnotation(toSearch.annotationType(), annotationClass);
			if (annotation != null) {
				return annotation;
			}
		}
		return null;
	}

}
