package com.zhi.framework.web.exception;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import com.zhi.common.constant.HttpStatus;
import com.zhi.common.core.domain.AjaxResult;
import com.zhi.common.exception.DemoModeException;
import com.zhi.common.exception.ServiceException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 全局异常处理器单元测试。
 *
 * <p>控制器不做 try/catch，异常一律冒泡到这里，因此这里就是接口错误响应的真实契约：
 * 业务异常保留自带 code，其余统一 500/403/404，且对外只暴露可展示的文案。</p>
 */
class GlobalExceptionHandlerTest
{
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/system/article/1");

    /** 只提供 MethodParameter 所需的可反射方法 */
    static class Sample
    {
        public void getById(Long id)
        {
        }
    }

    private static MethodParameter idParameter() throws Exception
    {
        return new MethodParameter(Sample.class.getDeclaredMethod("getById", Long.class), 0);
    }

    private static int codeOf(AjaxResult result)
    {
        return (Integer) result.get(AjaxResult.CODE_TAG);
    }

    private static String msgOf(AjaxResult result)
    {
        return (String) result.get(AjaxResult.MSG_TAG);
    }

    @Test
    void accessDeniedBecomesForbiddenWithoutLeakingInternalReason()
    {
        AjaxResult result = handler.handleAccessDeniedException(
                new AccessDeniedException("A token was passed to the authorization manager"), request);

        assertThat(codeOf(result)).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(msgOf(result)).isEqualTo("没有权限，请联系管理员授权");
    }

    @Test
    void unsupportedMethodKeepsSpringMessage()
    {
        AjaxResult result = handler.handleHttpRequestMethodNotSupported(
                new HttpRequestMethodNotSupportedException("PUT"), request);

        assertThat(codeOf(result)).isEqualTo(HttpStatus.ERROR);
        assertThat(msgOf(result)).contains("PUT");
    }

    @Test
    void serviceExceptionKeepsItsOwnCode()
    {
        AjaxResult result = handler.handleServiceException(
                new ServiceException("邮箱未配置", HttpStatus.BAD_REQUEST), request);

        assertThat(codeOf(result)).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(msgOf(result)).isEqualTo("邮箱未配置");
    }

    @Test
    void serviceExceptionWithoutCodeFallsBackTo500()
    {
        AjaxResult result = handler.handleServiceException(new ServiceException("文章不存在"), request);

        assertThat(codeOf(result)).isEqualTo(HttpStatus.ERROR);
        assertThat(msgOf(result)).isEqualTo("文章不存在");
    }

    @Test
    void missingPathVariableNamesTheVariable() throws Exception
    {
        MissingPathVariableException e = new MissingPathVariableException("id", idParameter());

        assertThat(msgOf(handler.handleMissingPathVariableException(e, request)))
                .isEqualTo("请求路径中缺少必需的路径变量[id]");
    }

    @Test
    void typeMismatchEscapesHtmlInputAndNamesRequiredType() throws Exception
    {
        MethodArgumentTypeMismatchException e = new MethodArgumentTypeMismatchException(
                "<script>alert(1)</script>", Long.class, "id", idParameter(), new IllegalStateException("bad"));

        AjaxResult result = handler.handleMethodArgumentTypeMismatchException(e, request);

        String message = msgOf(result);
        assertThat(message).contains("id").contains(Long.class.getName());
        assertThat(message).doesNotContain("<script>alert(1)</script>");
    }

    @Test
    void typeMismatchWithoutRequiredTypeStillProducesReadableMessage() throws Exception
    {
        MethodArgumentTypeMismatchException e = new MethodArgumentTypeMismatchException(
                "abc", null, "status", idParameter(), new IllegalStateException("bad"));

        assertThat(msgOf(handler.handleMethodArgumentTypeMismatchException(e, request)))
                .contains("status").contains("未知类型").contains("abc");
    }

    @Test
    void unknownRuntimeExceptionIsReportedAsError()
    {
        AjaxResult result = handler.handleRuntimeException(new RuntimeException("数据库错误"), request);

        assertThat(codeOf(result)).isEqualTo(HttpStatus.ERROR);
        // 原始消息不得回传客户端：该兜底会捕获 JDBC/MyBatis 异常，其消息常含表名/列名/SQL 片段
        assertThat(msgOf(result)).isNotEqualTo("数据库错误");
        assertThat(msgOf(result)).doesNotContain("数据库错误");
        assertThat(msgOf(result)).isEqualTo("系统繁忙，请稍后重试");
    }

    @Test
    void checkedExceptionIsReportedAsError()
    {
        AjaxResult result = handler.handleException(new java.io.IOException("磁盘不可用"), request);

        assertThat(codeOf(result)).isEqualTo(HttpStatus.ERROR);
        assertThat(msgOf(result)).doesNotContain("磁盘不可用");
        assertThat(msgOf(result)).isEqualTo("系统繁忙，请稍后重试");
    }

    /**
     * 回归守卫：把「表名/列名等 SQL 细节」这种典型 JDBC 异常消息喂给兜底 handler，
     * 断言响应体里绝不出现它们 —— 这正是本项目历史上真实发生过的泄漏形态。
     */
    @Test
    void fallbackHandlersMustNotLeakSqlDetails()
    {
        String jdbcLike = "Unknown column 'create_by' in 'field list'";
        assertThat(msgOf(handler.handleRuntimeException(new RuntimeException(jdbcLike), request)))
                .doesNotContain("create_by").doesNotContain("Unknown column").doesNotContain("field list");
        assertThat(msgOf(handler.handleException(new Exception(jdbcLike), request)))
                .doesNotContain("create_by").doesNotContain("Unknown column").doesNotContain("field list");
    }

    @Test
    void bindExceptionUsesFirstFieldMessage()
    {
        BindException e = new BindException(new Sample(), "sample");
        e.addError(new FieldError("sample", "title", "标题不能为空"));
        e.addError(new FieldError("sample", "content", "内容不能为空"));

        assertThat(msgOf(handler.handleBindException(e))).isEqualTo("标题不能为空");
    }

    @Test
    void invalidArgumentUsesFieldMessageAndFallsBackWhenOnlyGlobalError() throws Exception
    {
        BeanPropertyBindingResult withField = new BeanPropertyBindingResult(new Sample(), "sample");
        withField.addError(new FieldError("sample", "title", "标题长度超限"));
        assertThat(msgOf((AjaxResult) handler.handleMethodArgumentNotValidException(
                new MethodArgumentNotValidException(idParameter(), withField)))).isEqualTo("标题长度超限");

        BeanPropertyBindingResult globalOnly = new BeanPropertyBindingResult(new Sample(), "sample");
        globalOnly.addError(new ObjectError("sample", "整体校验失败"));
        assertThat(msgOf((AjaxResult) handler.handleMethodArgumentNotValidException(
                new MethodArgumentNotValidException(idParameter(), globalOnly)))).isEqualTo("参数验证失败");
    }

    @Test
    void demoModeRejectsTheOperation()
    {
        assertThat(msgOf(handler.handleDemoModeException(new DemoModeException())))
                .isEqualTo("演示模式，不允许操作");
    }

    @Test
    void missingHandlerAnswers404WithoutEchoingUrlOrHostOrStackDetails()
    {
        NoHandlerFoundException e = new NoHandlerFoundException("GET", "/api/not-exists", null);

        AjaxResult result = handler.handleNoHandlerFoundException(e, request);

        assertThat(codeOf(result)).isEqualTo(HttpStatus.NOT_FOUND);
        String msg = msgOf(result);
        assertThat(msg).isEqualTo("请求的页面或资源不存在");
        // 回归锁：这段文案曾写死 http://localhost:8080/api，真实域名下会误导用户
        assertThat(msg).doesNotContain("localhost").doesNotContain("8080");
        // 请求地址与异常细节只进日志，不回显给前端
        assertThat(msg).doesNotContain("/api/not-exists").doesNotContain("No handler found");
        assertThat(result.get(AjaxResult.DATA_TAG)).isNull();
    }
}
