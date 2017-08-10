package com.ych.shcm.o2o.action;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import com.ych.shcm.o2o.interceptor.JWTAuthInterceptor;
import com.ych.shcm.o2o.service.JWTService;

/**
 * 基础的Action
 * <p>
 * Created by U on 2017/7/18.
 */
public class BaseAction {

    /**
     * @return HttpServletRequest 对象
     */
    protected HttpServletRequest getRequest() {
        return (HttpServletRequest) RequestContextHolder.currentRequestAttributes().resolveReference(RequestAttributes.REFERENCE_REQUEST);
    }

    /**
     * @return JWT的认证结果
     */
    protected <T extends JWTService.AuthorizeResult> T getAuthorizeResult() {
        return (T) getRequest().getAttribute(JWTAuthInterceptor.JWT_AUTHORIZE_RESULT_ATTR_NAME);
    }

    /**
     * @return 应用上下文的请求URL
     */
    protected String getAppContextUlr() {
        HttpServletRequest request = getRequest();
        StringBuilder url = new StringBuilder();
        url.append(request.getScheme()).append("://").append(request.getServerName());
        if (request.getServerPort() != 80) {
            url.append(':').append(request.getServerPort());
        }
        url.append(request.getContextPath());
        return url.toString();
    }

    /**
     * @return 请求方的IP地址
     */
    protected String getRemoteAddr() {
        HttpServletRequest request = getRequest();

        String ip = request.getHeader("x-forwarded-for");

        if (ip == null) {
            ip = request.getHeader("x-real-ip");
        }

        if (ip == null) {
            ip = request.getRemoteAddr();
        }

        return ip;
    }


}
