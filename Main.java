import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;

public class Main {
    private static String[] args;

    private static final boolean portable = new File(".\\ScreenConnect").exists();
    private static Process[] beachShellScripts;

    public static void main(String[] args) throws IOException, InterruptedException, AWTException {
        Main.args = args;
        runTheServer(true);
        addTrayIcon();
        handleScripts(true);
    }

    private static void addTrayIcon() throws AWTException, IOException {
        SystemTray tray = SystemTray.getSystemTray();
        TrayIcon trayIcon = new TrayIcon(ImageIO.read(Main.class.getResourceAsStream("/ScreenConnect.png")));
        trayIcon.setImageAutoSize(true);

        MenuItem menuExit = new MenuItem("exit");
        menuExit.addActionListener(e -> {
            try {
                runTheServer(false);
                handleScripts(false);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            tray.remove(trayIcon);
            System.exit(0);
        });

        MenuItem menuUpgrade = new MenuItem("Upgrade");
        menuUpgrade.addActionListener(e -> {
            try {
                runTheServer(false);
                Upgrade.upgrade();
                runTheServer(true);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });

        MenuItem menuGithub = new MenuItem("GitHub");
        menuGithub.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new URL("http://github.com/abamoshe/ConnectWise_portable").toURI());
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        });

        PopupMenu popupMenu = new PopupMenu();
        popupMenu.add(menuExit);
        popupMenu.addSeparator();
        if (portable) {
            popupMenu.add(menuUpgrade);
            popupMenu.addSeparator();
        }
        popupMenu.add(menuGithub);
        trayIcon.setPopupMenu(popupMenu);
        tray.add(trayIcon);
    }

    private static void runTheServer(boolean startOrStop) throws IOException {
        if (startOrStop) {
            if (portable) {
                createServices();
                startServices();
                if (!(args.length > 0 && args[0].equals("--delete=false"))) deleteServices();
            } else {
                startServices();
            }
        } else {
            stopServices();
        }

    }

    private static void createServices() throws IOException {
        String serviceExe = new File(".\\ScreenConnect\\bin\\ScreenConnect.Service.exe").getAbsolutePath();
        exec("sc create \"ScreenConnect Session Manager\" binPath= \"" + serviceExe + "\" " +
                "type= share start= delayed-auto group= \"Remote Control\" DisplayName= \"ScreenConnect Session Manager\"");
        exec("sc create \"ScreenConnect Web Server\" binPath= \"" + serviceExe + "\" " +
                "type= share start= delayed-auto group= \"Remote Control\" DisplayName= \"ScreenConnect Web Server\" depend= \"ScreenConnect Session Manager\"");
        exec("sc create \"ScreenConnect Relay\" binPath= \"" + serviceExe + "\" type= share start= delayed-auto group= \"Remote Control\" " +
                "DisplayName= \"ScreenConnect Relay\" depend= \"ScreenConnect Session Manager\"");
    }

    private static void deleteServices() throws IOException {
        exec("sc delete \"ScreenConnect Session Manager\"");
        exec("sc delete \"ScreenConnect Web Server\"");
        exec("sc delete \"ScreenConnect Relay\"");
    }

    private static void startServices() throws IOException {
        exec("net start \"ScreenConnect Session Manager\"");
        exec("net start \"ScreenConnect Web Server\"");
        exec("net start \"ScreenConnect Relay\"");
    }

    private static void stopServices() throws IOException {
        exec("net stop \"ScreenConnect Web Server\"");
        exec("net stop \"ScreenConnect Relay\"");
        exec("net stop \"ScreenConnect Session Manager\"");
    }

    private static void exec(String cmd) throws IOException {
        Process p = Runtime.getRuntime().exec(cmd);
        System.out.println(new String(p.getInputStream().readAllBytes()));
        System.out.println(new String(p.getErrorStream().readAllBytes()));
    }

    private static void handleScripts(boolean startOrStop) {
        if (startOrStop) {
            File[] scriptFiles = new File(".").listFiles(
                    pathname -> pathname.getName().endsWith(".bat") ||
                            pathname.getName().endsWith(".cmd"));
            beachShellScripts = new Process[scriptFiles.length];

            for (int i = 0; i < scriptFiles.length; i++)
                try {
                    beachShellScripts[i] = Runtime.getRuntime().exec(scriptFiles[i].toString());
                } catch (IOException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(null,
                            "There was an error on start " + scriptFiles[i],
                            "ScreenConnect potable Upgrade", JOptionPane.ERROR_MESSAGE);
                }
        } else {
            for (Process p : beachShellScripts)
                p.destroy();
        }
    }
}
