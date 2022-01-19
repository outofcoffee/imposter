package io.gatehill.imposter.perf;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Agent {
    public static PerfWriter writer;

    public static void premain(String agentArgs, Instrumentation inst) {
        initWriter(agentArgs);

        new AgentBuilder.Default()
                .with(new AgentBuilder.InitializationStrategy.SelfInjection.Eager())
                .type(
                        ElementMatchers.nameStartsWith("io.gatehill.imposter.")
                )
                .transform((builder, typeDescription, classLoader, module) -> builder
                        .method(ElementMatchers.any())
                        .intercept(Advice.to(CallTimerAdvice.class))
                )
                .installOn(inst);
    }

    private static void initWriter(String agentArgs) {
        if (agentArgs == null) {
            System.out.println("Writing timings to stdout");
            writer = System.out::println;

        } else {
            System.out.printf("Writing timings to %s\n", agentArgs);

            final ConcurrentLinkedQueue<String> entries = new ConcurrentLinkedQueue<>();
            writer = entries::add;

            // single writer
            new Thread(() -> {
                final OutputStreamWriter osw;
                try {
                    final OutputStream os = Files.newOutputStream(Paths.get(agentArgs));
                    osw = new OutputStreamWriter(os);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                while (true) {
                    try {
                        final String entry = entries.poll();
                        if (null == entry) {
                            Thread.sleep(5);
                        } else {
                            osw.write(entry + "\n");
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to write to timings file", e);
                    }
                }
            }).start();

            writer.write("duration,method,args");
        }
    }
}
