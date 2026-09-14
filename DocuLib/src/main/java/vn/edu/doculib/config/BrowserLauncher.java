package vn.edu.doculib.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.net.URI;
import java.util.Locale;

@Component
@ConditionalOnProperty(name = "app.open-browser", havingValue = "true")
public class BrowserLauncher {

    private static final Logger LOGGER = LoggerFactory.getLogger(BrowserLauncher.class);

    private final int serverPort;

    public BrowserLauncher(@Value("${server.port:8080}") int serverPort) {
        this.serverPort = serverPort;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void openBrowser() {
        String applicationUrl = "http://localhost:" + serverPort;
        try {
            if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac")) {
                new ProcessBuilder("/usr/bin/open", applicationUrl).start();
                return;
            }

            if (GraphicsEnvironment.isHeadless() || !Desktop.isDesktopSupported()) {
                return;
            }

            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(URI.create(applicationUrl));
            }
        } catch (Exception exception) {
            LOGGER.warn("Không thể tự động mở trình duyệt tại cổng {}", serverPort, exception);
        }
    }
}
