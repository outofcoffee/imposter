package io.gatehill.imposter.perf;

import static net.bytebuddy.asm.Advice.AllArguments;
import static net.bytebuddy.asm.Advice.Enter;
import static net.bytebuddy.asm.Advice.OnMethodEnter;
import static net.bytebuddy.asm.Advice.OnMethodExit;
import static net.bytebuddy.asm.Advice.Origin;

public class CallTimerAdvice {
    @OnMethodEnter
    static long enter() {
        return System.currentTimeMillis();
    }

    @OnMethodExit
    static void exit(@Origin String method, @Enter long start, @AllArguments Object[] args) {
        StringBuilder sb = new StringBuilder();
        for (Object arg : args) {
            sb.append(arg);
            sb.append(", ");
        }
        try {
            String entry = (System.currentTimeMillis() - start) + ",\"" + method + "\",\"" + sb + "\"";
            entry = entry.replaceAll("\\R", " ");
            Agent.writer.write(entry);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
