package Spring;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

public class ResourceFileSearcher {
    private static final String SPROPERTIES_FILE_NAME = "sproperties.properties";

    public static Properties getPropertiesFile() throws IOException {
        URL resource = ResourceFileSearcher.class.getClassLoader().getResource("");
        if (resource == null)
            throw new IOException("Resource folder not found");

        File file = new File(resource.getFile());
        File spropertiesFile = searchForFile(file, SPROPERTIES_FILE_NAME);
        if (spropertiesFile == null) {
            throw new IOException("sproperties.properties file not found in resource folder");
        }
        Properties properties = new Properties();
        InputStream inputStream = new FileInputStream(spropertiesFile);
        properties.load(inputStream);
        return properties;
    }

    public static File searchForFile(File folder, String fileName) {
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                if (file.isFile() && file.getName().equals(fileName)) {
                    return file;
                }
                if (file.isDirectory()) {
                    File foundFile = searchForFile(file, fileName);
                    if (foundFile != null) {
                        return foundFile;
                    }
                }
            }
        }
        return null;
    }
}
