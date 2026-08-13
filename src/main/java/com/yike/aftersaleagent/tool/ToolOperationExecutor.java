package com.yike.aftersaleagent.tool;

import java.util.concurrent.Callable;

public interface ToolOperationExecutor {
    <T> T execute(Callable<T> operation);
}
