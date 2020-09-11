import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;

public class Upgrade {

    public static void upgrade() throws IOException {
        JOptionPane.showMessageDialog(null,
                "Please Backup the old ScreenConnect directory \n" +
                        "Please click ok and choose the ScreenConnect directory",
                "ScreenConnect potable Upgrade", JOptionPane.INFORMATION_MESSAGE);

        //opens a folder chooser via powershell commend and gets back the selected directory in byte array
        byte[] inputBytes = Runtime.getRuntime().exec("powershell \"(new-object -COM 'Shell.Application').BrowseForFolder(0,'Please choose a folder.',0,0).self.path\"").getInputStream().readAllBytes();
        String selectedDir = new String(inputBytes).trim();

        try {
            Path screenConnectDirectory = Path.of(selectedDir);
            Path installationDir = Path.of(".\\ScreenConnect");
            Path temp = Files.createTempDirectory("ScreenConnect");
            System.out.println("Temp file: " + temp);

            long createTime = new File(".\\ScreenConnect\\bin\\ScreenConnect.Core.dll").lastModified();

            backup(installationDir, temp, createTime);
            copy(screenConnectDirectory, installationDir);
            restore(temp, installationDir);

            JOptionPane.showMessageDialog(null,
                    "upgrade successful!",
                    "ScreenConnect potable Upgrade", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "There was an error during upgrade \n" +
                            "Please restore your old from your backup",
                    "ScreenConnect potable Upgrade", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void backup(Path installationDir, Path tempDir, long createTime) throws IOException {
        Files.walk(installationDir).forEach(subInstallation -> {

            long lestModified = subInstallation.toFile().lastModified();
            System.out.println("Backup : " + subInstallation);

            try {
                Path subTemp = tempDir.resolve(installationDir.relativize(subInstallation));
                if (Files.isDirectory(subInstallation)) {
                    if (!Files.exists(subTemp))
                        Files.createDirectory(subTemp);
                    return;
                }
                if (lestModified > createTime + 120000) {
                    Files.copy(subInstallation, subTemp, StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("Backup: " + subTemp);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        deleteEmptyDirs(tempDir);
    }


    private static void copy(Path source, Path dest) throws IOException {
        Files.walk(source).forEach(subSource -> {
            try {
                Path subDest = dest.resolve(source.relativize(subSource));
                if (Files.isDirectory(subSource)) {
                    if (!Files.exists(subDest))
                        Files.createDirectory(subDest);
                    return;
                }
                Files.copy(subSource, subDest, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Copy: " + subDest);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static void restore(Path tempDir, Path installationDir) throws IOException {
        Files.walk(tempDir).forEach(subTemp -> {
            try {
                Path subInstallation = installationDir.resolve(tempDir.relativize(subTemp));
                if (Files.isDirectory(subTemp)) {
                    if (!Files.exists(subInstallation))
                        Files.createDirectory(subInstallation);
                    return;
                }
                if (!installationDir.toFile().exists() || shouldAlwaysBackedUp(subTemp.getFileName().toString())) {
                    Files.copy(subTemp, subInstallation, StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("Restore: " + subInstallation);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        deleteEmptyDirs(installationDir);
    }

    private static void deleteEmptyDirs(Path source) throws IOException {
        Files.walk(source).sorted(Collections.reverseOrder()).forEach(path -> {

            try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(source)) {
                if (!dirStream.iterator().hasNext())
                    Files.delete(source);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private static boolean shouldAlwaysBackedUp(String fileName) {
        return fileName.equals("Session") ||
                fileName.equals("Toolbox") ||
                fileName.equals("SessionGroup.xml") ||
                fileName.equals("Role.xml") ||
                fileName.equals("SessionEventTrigger.xml") ||
                fileName.equals("License.xml") ||
                fileName.equals("User.xml") ||
                fileName.equals("ExtensionConfiguration.xml") ||
                fileName.equals("App_Extensions") ||
                fileName.equals("httplistener") ||
                fileName.equals("App_Code") ||
                fileName.equals("web.config") ||
                fileName.equals("Logo.png") ||
                fileName.equals("LogoIcon.png") ||
                fileName.equals("Default.resx") ||
                fileName.equals("app.config") ||
                fileName.startsWith("Session.") ||
                (fileName.startsWith("Override.") && fileName.endsWith("resx"));

    }
}
