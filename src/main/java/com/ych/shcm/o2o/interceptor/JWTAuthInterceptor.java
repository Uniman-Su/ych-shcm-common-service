package com.ych.shcm.o2o.interceptor;

import java.lang.reflect.Method;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.ych.shcm.o2o.annotation.JWTAuth;
import com.ych.shcm.o2o.service.JWTService;

/**
 * 处理JWT验证与授权
 * <p>
 * Created by U on 2017/7/18.
 */
public class JWTAuthInterceptor extends HandlerInterceptorAdapter {

    private static final Logger logger = LoggerFactory.getLogger(JWTAuthInterceptor.class);

    public static final String JWT_HEADER_NAME = "Authorization";

    public static final String JWT_HEADER_PREFIX = "Bearer ";

    /**
     * JWT存放验证结果的属性名称
     */
    public static final String JWT_AUTHORIZE_RESULT_ATTR_NAME = "JWTAuthorizeResult";

    /**
     * JWT的属性名称
     */
    public static final String JWT_ATTR_NAME = "JSONWebToken";

    @Autowired
    private JWTService jwtService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            Method method = handlerMethod.getMethod();

            JWTAuth annotation = null;
            if (method.isAnnotationPresent(JWTAuth.class)) {
                annotation = method.getAnnotation(JWTAuth.class);
            } else if (handlerMethod.getBeanType().isAnnotationPresent(JWTAuth.class)) {
                annotation = handlerMethod.getBeanType().getAnnotation(JWTAuth.class);
            }

            if (annotation != null) {
                String header = StringUtils.trimToNull(request.getHeader(JWT_HEADER_NAME));
                if (header != null && header.startsWith(JWT_HEADER_PREFIX)) {
                    String token = header.substring(JWT_HEADER_PREFIX.length());

                    JWTService.AuthorizeResult result = jwtService.authorize(token, annotation.value().length > 0 ? annotation.value() : annotation.issuer());
                    if (!result.isSuccess()) {
                        logger.error("JWT authorization failed:{}", token);
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "JWT authorization failed");
                        return false;
                    }

                    request.setAttribute(JWT_ATTR_NAME, token);
                    request.setAttribute(JWT_AUTHORIZE_RESULT_ATTR_NAME, result);
                } else {
                    return false;
                }
            }
        }

        return true;
    }
}
