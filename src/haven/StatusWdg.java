package haven;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StatusWdg extends Widget {
    public static final boolean iswindows = System.getProperty("os.name").startsWith("Windows");
    private static final Tex hearthlingsplayingdef = Text.renderstroked(String.format("Players Online: %s", "Loading...")).tex();
    private static final Tex pingtimedef = Text.renderstroked(String.format("Ping: %s ms", "?")).tex();
    public static final Tex provincedef = Text.renderstroked(String.format("Province: %s", "?")).tex();
    public static final Tex realmdef = Text.renderstroked(String.format("Realm: %s", "?")).tex();
    public Tex players = hearthlingsplayingdef;
    public Tex pingtime = pingtimedef;
    private long lastPingUpdate = System.currentTimeMillis();
    // Windows IPv4:    Reply from 213.239.201.139: bytes=32 time=71ms TTL=127
    // Windows IPv6:    Reply from 2a01:4f8:130:7393::2: time=71ms
    // GNU ping IPv4:   64 bytes from ansgar.seatribe.se (213.239.201.139): icmp_seq=1 ttl=50 time=72.5 ms
    // GNU ping IPv6:   64 bytes from ansgar.seatribe.se: icmp_seq=1 ttl=53 time=15.3 ms
    private static final Pattern pattern = Pattern.compile(iswindows ? ".+?=(\\d+)[^ \\d\\s]" : ".+?time=(\\d+\\.?\\d*) ms");
    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private static Future<?> future;

    public final HttpStatus stat;

    public StatusWdg() {
        try {
            this.stat = new HttpStatus(new URI("http", Bootstrap.authserv.get().host, "/mt/srv-mon", null));
        } catch(URISyntaxException e) {
            throw(new RuntimeException(e));
        }
        if (future != null)
            future.cancel(true);
        future = executor.scheduleWithFixedDelay(this::startUpdater, 0, 5, TimeUnit.SECONDS);
    }

    private void updatepingtime() {
        String ping = "?";

        final List<String> command = new ArrayList<>();
        command.add("ping");
        command.add(iswindows ? "-n" : "-c");
        command.add("1");
        command.add("game.havenandhearth.com");

        final List<String> lines = new ArrayList<>();
        try (BufferedReader standardOutput = new BufferedReader(new InputStreamReader(new ProcessBuilder(command).start().getInputStream()))) {
            lines.addAll(standardOutput.lines().collect(Collectors.toList()));
        } catch (IOException e) {
        }

        StringBuilder output = new StringBuilder();
        lines.forEach(output::append);

        Matcher matcher = pattern.matcher(output.toString());
        if (matcher.find()) {
            ping = matcher.group(1);
        }

        if (ping.isEmpty())
            ping = "?";

        synchronized (this) {
            pingtime = Text.renderstroked(String.format("Ping: %s ms", ping)).tex();
        }
    }
    private void updatePlayers() {
        synchronized(stat) {
            if (!stat.syn || (stat.status == "")) {
                return;
            }
            if (stat.status == "up") {
                players = Text.renderstroked(String.format("Players Online: %s", stat.users)).tex();
            }
        }
    }

    private void startUpdater() {
        try {
            updatepingtime();
            updatePlayers();
        } catch (Exception e) {}
    }

    @Override
    public void draw(GOut g) {
        // ND: This is actually done in GameUI
    }

    @Override
    public void reqdestroy() {
        executor.shutdown();
        super.reqdestroy();
    }

    protected void added() {
        stat.start();
    }
    public void dispose() {
        stat.quit();
    }
}
