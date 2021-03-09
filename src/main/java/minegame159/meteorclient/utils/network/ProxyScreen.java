package minegame159.meteorclient.utils.network;

import io.netty.channel.ChannelHandler;
import io.netty.handler.proxy.Socks4ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import minegame159.meteorclient.accounts.gui.WAccountField;
import minegame159.meteorclient.gui.screens.WindowScreen;
import minegame159.meteorclient.gui.widgets.WButton;
import minegame159.meteorclient.gui.widgets.WCheckbox;
import minegame159.meteorclient.gui.widgets.WDropbox;
import minegame159.meteorclient.gui.widgets.WLabel;

import java.net.InetSocketAddress;

public class ProxyScreen extends WindowScreen {
    private static String ip, port;

    private static final WLabel errorLabel = new WLabel("");

    private static final WDropbox<ProxyType> dropbox = new WDropbox<>(ProxyType.SOCKS_4);
    private static final WCheckbox active = new WCheckbox(false);

    public ProxyScreen() {

        super("Proxy", true);
        final WLabel connection = add(new WLabel("Connected to: ")).fillX().expandX().getWidget();
        row();
        add(new WLabel("Active:"));
        add(active);
        row();
        add(dropbox);
        row();
        add(new WLabel("IP Address:"));

        WAccountField ipField = add(new WAccountField("", 400)).getWidget();
        ipField.setFocused(true);
        row();


        add(new WLabel("Port:"));
        WAccountField portField = add(new WAccountField("", 400)).getWidget();
        row();
        add(errorLabel);
        row();

        WButton add = add(new WButton("Connect")).fillX().expandX().getWidget();
        add.action = () -> {
            setInfoText("");
            ip = ipField.getText();
            port = portField.getText();

            try {

                if (Integer.parseInt(port) < 0) {
                    throw new IllegalArgumentException("Port must be greater than 0");
                }
            } catch (Exception e) {
                setInfoText("Invalid port! " + e.getMessage());
                e.printStackTrace();
            }
            try {
                if (ip.isEmpty()) {
                    throw new IllegalArgumentException("Ip Address must not be empty");
                }
                new InetSocketAddress(ip, 1);
            } catch (Exception e) {
                setInfoText(String.format("Invalid IP Address! %s / %s", e.getClass().getSimpleName(), e.getMessage()));
                e.printStackTrace();

            }
            if (errorLabel.getText().isEmpty()) {
                final String format = String.format("Connected to: %s:%s", ip, port);
                connection.setText(format);
                System.out.println(format);

            } else {
                connection.setText("Error! Fix the settings!");
            }


        };

    }

    private static String getIp() {
        return ip;
    }

    private static String getPort() {
        return port;
    }

    public static void setInfoText(String text) {
        if (!text.isEmpty()) {
            System.out.println(text);
        }
        errorLabel.setText(text);
    }

    private static ProxyType getProxyType() {
        return dropbox.getValue();
    }

    public static boolean isSet() {
        return !ip.isEmpty() && !port.isEmpty();
    }

    public enum ProxyType {
        SOCKS_4, SOCKS_5;

        public static ChannelHandler create() {
            switch (ProxyScreen.getProxyType()) {
                case SOCKS_4:
                    return new Socks4ProxyHandler(new InetSocketAddress(getIp(), Integer.parseInt(getPort())));
                case SOCKS_5:
                    return new Socks5ProxyHandler(new InetSocketAddress(getIp(), Integer.parseInt(getPort())));
            }
            return null;
        }

        @Override
        public String toString() {
            final String name = this.name();
            return (name.charAt(0) +
                    name.toLowerCase()
                            .substring(1))
                    .replace("_", " ");
        }

    }

    public static boolean isActive() {
        return active.checked;
    }
}
