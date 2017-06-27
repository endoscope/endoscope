package com.github.endoscope.cdiui;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.Arrays.stream;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;

@Path("/endoscope/threads")
@Produces("application/json;charset=utf-8")
public class ThreadsController {
    @GET
    public List<ThreadInfoData> all(@QueryParam("q") String query) {
        return Thread.getAllStackTraces()
                .entrySet()
                .stream()
                .map(toThreadInfo())
                .filter(it -> containsInStack(it, query))
                .collect(toList());
    }

    private boolean containsInStack(ThreadInfoData it, String query) {
        return isNull(query) || it.getStacktrace().stream().filter(line -> line.contains(query)).findFirst().isPresent();
    }

    private Function<Map.Entry<Thread, StackTraceElement[]>, ThreadInfoData> toThreadInfo() {
        ThreadMXBean mxBean = ManagementFactory.getThreadMXBean();
        return (Map.Entry<Thread, StackTraceElement[]> entry) -> {
            Thread thread = entry.getKey();
            StackTraceElement[] elements = entry.getValue();
            ThreadInfo info = mxBean.getThreadInfo(thread.getId());
            return new ThreadInfoData(info.getThreadId(),
                    info.getThreadName(),
                    info.getBlockedCount(),
                    info.getBlockedTime(),
                    info.getWaitedCount(),
                    info.getWaitedTime(),
                    info.getThreadState(),
                    formatLines(elements)
            );
        };

    }

    private String formatLine(StackTraceElement e) {
        return e.getClassName() + "." + e.getMethodName() + "(" + e.getFileName() + ":" + e.getLineNumber() + ")";
    }

    private List<String> formatLines(StackTraceElement[] elements) {
        return stream(elements).map(e -> formatLine(e)).collect(toList());
    }

    public class ThreadInfoData {
        private final long id;
        private final long blockedCount;
        private final String threadName;
        private final long blockedTime;
        private final long waitedCount;
        private final long waitedTime;
        private final Object threadState;
        private final List<String> stacktrace;

        public ThreadInfoData(long id, String threadName, long blockedCount, long blockedTime, long waitedCount, long waitedTime, Thread.State threadState, List<String> stacktrace) {
            this.id = id;
            this.threadName = threadName;
            this.blockedCount = blockedCount;
            this.blockedTime = blockedTime;
            this.waitedCount = waitedCount;
            this.waitedTime = waitedTime;
            this.threadState = threadState;
            this.stacktrace = stacktrace;
        }

        public long getId() {
            return id;
        }

        public long getBlockedCount() {
            return blockedCount;
        }

        public String getThreadName() {
            return threadName;
        }

        public long getBlockedTime() {
            return blockedTime;
        }

        public long getWaitedCount() {
            return waitedCount;
        }

        public long getWaitedTime() {
            return waitedTime;
        }

        public Object getThreadState() {
            return threadState;
        }

        public List<String> getStacktrace() {
            return stacktrace;
        }
    }
}