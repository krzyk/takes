/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2014-2016 Yegor Bugayenko
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.takes.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.EqualsAndHashCode;
import org.takes.misc.Utf8InputStreamReader;
import org.takes.misc.Utf8OutputStreamWriter;

/**
 * Command line options.
 *
 * <p>The class is immutable and thread-safe.
 *
 * @author Yegor Bugayenko (yegor@teamed.io)
 * @version $Id$
 * @since 0.2
 */
@EqualsAndHashCode(of = "map")
final class Options {

    /**
     * Map of arguments and their values.
     */
    private final Map<String, String> map;

    /**
     * Constructs an {@code Options} with the specified arguments.
     * @param args Arguments
     * @since 0.9
     */
    Options(final String... args) {
        this(Arrays.asList(args));
    }

    /**
     * Constructs an {@code Options} with the specified arguments.
     * @param args Arguments
     */
    Options(final Iterable<String> args) {
        this.map = Options.asMap(args);
    }

    /**
     * Is it a daemon?
     * @return TRUE if yes
     */
    public boolean isDaemon() {
        return this.map.get("daemon") != null;
    }

    /**
     * Get the socket to listen to.
     * @return Socket
     * @throws IOException If fails
     */
    public ServerSocket socket() throws IOException {
        final String port = this.map.get("port");
        if (port == null) {
            throw new IllegalArgumentException("--port must be specified");
        }
        final ServerSocket socket;
        if (port.matches("\\d+")) {
            socket = new ServerSocket(Integer.parseInt(port));
        } else {
            final File file = new File(port);
            if (file.exists()) {
                final Reader reader = new Utf8InputStreamReader(
                    new FileInputStream(file)
                );
                try {
                    // @checkstyle MagicNumber (1 line)
                    final char[] chars = new char[8];
                    final int length = reader.read(chars);
                    socket = new ServerSocket(
                        Integer.parseInt(new String(chars, 0, length))
                    );
                } finally {
                    reader.close();
                }
            } else {
                socket = new ServerSocket(0);
                final Writer writer = new Utf8OutputStreamWriter(
                    new FileOutputStream(file)
                );
                try {
                    writer.append(Integer.toString(socket.getLocalPort()));
                } finally {
                    writer.close();
                }
            }
        }
        return socket;
    }

    /**
     * Are we in hit-refresh mode?
     * @return TRUE if this mode is ON
     * @since 0.9
     */
    public boolean hitRefresh() {
        return this.map.containsKey("hit-refresh");
    }

    /**
     * Get the lifetime in milliseconds.
     * @return Port number
     */
    public long lifetime() {
        final String value = this.map.get("lifetime");
        final long msec;
        if (value == null) {
            msec = Long.MAX_VALUE;
        } else {
            msec = Long.parseLong(value);
        }
        return msec;
    }

    /**
     * Get the threads.
     * @return Threads
     */
    public int threads() {
        final String value = this.map.get("threads");
        final int threads;
        if (value == null) {
            threads = Runtime.getRuntime().availableProcessors() << 2;
        } else {
            threads = Integer.parseInt(value);
        }
        return threads;
    }

    /**
     * Get the max latency in milliseconds.
     * @return Latency
     */
    public long maxLatency() {
        final String value = this.map.get("max-latency");
        final long msec;
        if (value == null) {
            msec = Long.MAX_VALUE;
        } else {
            msec = Long.parseLong(value);
        }
        return msec;
    }

    /**
     * Convert the provided arguments into a Map.
     * @param args Arguments to parse.
     * @return Map A map containing all the arguments and their values.
     * @throws IllegalStateException If an argument doesn't match with the
     *  expected format which is {@code --([a-z\-]+)(=.+)?}.
     */
    private static Map<String, String> asMap(final Iterable<String> args) {
        final Map<String, String> map = new HashMap<>(0);
        final Pattern ptn = Pattern.compile("--([a-z\\-]+)(=.+)?");
        for (final String arg : args) {
            final Matcher matcher = ptn.matcher(arg);
            if (!matcher.matches()) {
                throw new IllegalStateException(
                    String.format("can't parse this argument: '%s'", arg)
                );
            }
            final String value = matcher.group(2);
            if (value == null) {
                map.put(matcher.group(1), "");
            } else {
                map.put(matcher.group(1), value.substring(1));
            }
        }
        return map;
    }
}
