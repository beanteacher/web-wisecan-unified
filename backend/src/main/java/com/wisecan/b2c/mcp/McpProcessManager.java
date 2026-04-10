package com.wisecan.b2c.mcp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * MCP 서버 stdio 프로세스 spawn / health-check / 종료 관리.
 */
@Slf4j
@Component
public class McpProcessManager implements DisposableBean {

    @Value("${wisecan.mcp.command}")
    private String mcpCommand;

    @Value("${wisecan.mcp.timeout-ms:10000}")
    private long timeoutMs;

    private volatile Process process;

    /**
     * MCP 프로세스를 시작하고 반환한다. 이미 실행 중이면 기존 프로세스를 반환.
     */
    public synchronized Process startProcess() {
        if (isAlive()) {
            return process;
        }
        try {
            log.info("MCP 프로세스 시작: {}", mcpCommand);
            ProcessBuilder pb = new ProcessBuilder(mcpCommand.split("\\s+"));
            pb.redirectErrorStream(false);
            process = pb.start();
            log.info("MCP 프로세스 PID: {}", process.pid());
            return process;
        } catch (IOException e) {
            throw new McpException("MCP 프로세스 시작 실패: " + mcpCommand, e);
        }
    }

    /**
     * 프로세스가 살아있는지 확인한다.
     */
    public boolean isAlive() {
        return process != null && process.isAlive();
    }

    /**
     * 프로세스를 정상 종료하고, 타임아웃 초과 시 강제 종료한다.
     */
    public synchronized void stopProcess() {
        if (process == null) {
            return;
        }
        try {
            log.info("MCP 프로세스 종료 요청");
            process.destroy();
            boolean terminated = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            if (!terminated) {
                log.warn("MCP 프로세스 정상 종료 타임아웃 — 강제 종료");
                process.destroyForcibly();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        } finally {
            process = null;
        }
    }

    /**
     * 현재 프로세스를 반환한다. 시작되지 않았거나 종료된 경우 null.
     */
    public Process getProcess() {
        return isAlive() ? process : null;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    @Override
    public void destroy() {
        stopProcess();
    }
}
