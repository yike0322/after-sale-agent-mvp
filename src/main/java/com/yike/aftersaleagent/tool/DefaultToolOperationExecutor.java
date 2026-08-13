package com.yike.aftersaleagent.tool;

import com.yike.aftersaleagent.common.api.ErrorCode;
import com.yike.aftersaleagent.common.exception.BusinessException;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.stereotype.Component;

@Component
public class DefaultToolOperationExecutor implements ToolOperationExecutor {
    private static final long TIMEOUT_SECONDS = 2L;

    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    @Override
    public <T> T execute(Callable<T> operation) {
        Future<T> future = executorService.submit(operation);
        try {
            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException exception) {
            future.cancel(true);
            throw new BusinessException(ErrorCode.TOOL_TIMEOUT);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.TOOL_EXECUTION_FAILED);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new BusinessException(ErrorCode.TOOL_EXECUTION_FAILED);
        }
    }

    @PreDestroy
    void close() {
        executorService.shutdownNow();
    }
}
