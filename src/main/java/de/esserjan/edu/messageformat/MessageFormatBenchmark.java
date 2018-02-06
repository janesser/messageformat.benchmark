package de.esserjan.edu.messageformat;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.stringtemplate.v4.ST;

import com.github.javafaker.ChuckNorris;
import com.github.javafaker.DateAndTime;
import com.github.javafaker.Faker;
import com.github.pwittchen.kirai.library.Kirai;

/**
 * Execute by {@link org.openjdk.jmh.Main}.
 * 
 * @author jesser@gmx.de
 */
@Warmup(iterations = 1)
@Measurement(iterations = 2)
@Fork(2)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class MessageFormatBenchmark {
    private static final Pattern SF_CONVERT = Pattern.compile("(?<!')\\{([0-9]+)\\}");

    private static final ChuckNorris CHUCK_NORRIS = new Faker().chuckNorris();
    private static final DateAndTime DATE = new Faker().date();

    public static enum TestPatterns {
        NO_PATTERN("1NO_PATTERN.", null), //
        SIMPLE_PATTERN("2SIMPLE_PATTERN {0}.", new Supplier<Object[]>() {

            public Object[] get() {
                return new Object[] { CHUCK_NORRIS.fact() };
            }
        }), //
        COMPLEX_PATTERN("3COMPLEX_PATTERN '{3}' {2} {1} {0}.", new Supplier<Object[]>() {

            public Object[] get() {
                return new Object[] { //
                        CHUCK_NORRIS.fact(), //
                        CHUCK_NORRIS.fact(), //
                        CHUCK_NORRIS.fact(), //
                };
            }
        }), //
        FORMATTING_PATTERN("4FORMATTING_PATTERN {0} {1}.", new Supplier<Object[]>() {

            public Object[] get() {
                return new Object[] { //
                        CHUCK_NORRIS.fact(), //
                        DATE.birthday(), //
                };
            }
        }), //
        ;

        private final String pattern;
        private final Supplier<Object[]> argSupplier;

        private final String format;
        private final String namedCurly;
        private final String namedTagged;

        private TestPatterns(String pattern, Supplier<Object[]> argSupplier) {
            this.pattern = pattern;
            this.argSupplier = argSupplier;

            this.format = SF_CONVERT.matcher(pattern).replaceAll("%s");
            this.namedCurly = SF_CONVERT.matcher(pattern.replace("{3}", "3")).replaceAll("{arg$1}");
            this.namedTagged = SF_CONVERT.matcher(pattern).replaceAll("<arg$1>");
        }

        public Object[] getArgs() {
            if (argSupplier == null)
                return null;
            else
                return argSupplier.get();
        }

        public String getPattern() {
            return pattern;
        }

        public String getFormat() {
            return format;
        }

        public String getNamedCurly() {
            return namedCurly;
        }

        public String getNamedTagged() {
            return namedTagged;
        }

    }

    @Param({ "NO_PATTERN", "SIMPLE_PATTERN", "COMPLEX_PATTERN", "FORMATTING_PATTERN" })
    private TestPatterns pattern;

    @Benchmark
    public void testIcuMF(Blackhole bh) {
        String formatted = com.ibm.icu.text.MessageFormat.format(pattern.getPattern(), pattern.getArgs());
        bh.consume(formatted);
    }

    @Benchmark
    public void testJdkMF(Blackhole bh) {
        String formatted = java.text.MessageFormat.format(pattern.getPattern(), pattern.getArgs());
        bh.consume(formatted);
    }

    // TODO add blackhole-appender
    private static final Logger LOGGER = Logger.getRootLogger();

    @Benchmark
    public void testLogMF(Blackhole bh) {
        org.apache.log4j.LogMF.error(LOGGER, pattern.getPattern(), pattern.getArgs());
    }

    @Benchmark
    public void testSF(Blackhole bh) {
        String formatted = String.format(pattern.getFormat(), pattern.getArgs());
        bh.consume(formatted);
    }

    @Benchmark
    public void testKirai(Blackhole bh) {
        Kirai kirai = com.github.pwittchen.kirai.library.Kirai.from(pattern.getNamedCurly());
        Object[] args = pattern.getArgs();
        if (args != null)
            for (int j = 0; j < args.length; j++) {
                kirai.put("arg" + j, args[j]);
            }
        final CharSequence formatted = kirai.format();
        bh.consume(formatted);
    }

    @Benchmark
    public void testST(Blackhole bh) {
        ST st = new org.stringtemplate.v4.ST(pattern.getNamedTagged());
        Object[] args = pattern.getArgs();
        if (args != null)
            for (int j = 0; j < args.length; j++) {
                st.add("arg" + j, args[j]);
            }
        String formatted = st.render();
        bh.consume(formatted);
    }

}
