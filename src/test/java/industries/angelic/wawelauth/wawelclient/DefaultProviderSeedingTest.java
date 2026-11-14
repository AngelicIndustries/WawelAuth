package industries.angelic.wawelauth.wawelclient;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Map;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class DefaultProviderSeedingTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void writesMissingFiles() throws IOException {
        File directory = folder.getRoot();

        ProviderRegistry.seedDefaultProviders(directory, bundled("v1"));

        Assert.assertEquals("v1", read(directory));
    }

    @Test
    public void updatesFilesNobodyEdited() throws IOException {
        File directory = folder.getRoot();
        ProviderRegistry.seedDefaultProviders(directory, bundled("v1"));

        ProviderRegistry.seedDefaultProviders(directory, bundled("v2"));

        Assert.assertEquals("v2", read(directory));
    }

    @Test
    public void keepsEditedFiles() throws IOException {
        File directory = folder.getRoot();
        ProviderRegistry.seedDefaultProviders(directory, bundled("v1"));
        write(directory, "edited");

        ProviderRegistry.seedDefaultProviders(directory, bundled("v2"));

        Assert.assertEquals("edited", read(directory));
    }

    @Test
    public void adoptsCurrentFilesWrittenBeforeHashesWereKept() throws IOException {
        File directory = folder.getRoot();
        write(directory, "v1");

        ProviderRegistry.seedDefaultProviders(directory, bundled("v1"));
        ProviderRegistry.seedDefaultProviders(directory, bundled("v2"));

        Assert.assertEquals("v2", read(directory));
    }

    @Test
    public void keepsDifferingFilesWithoutARecord() throws IOException {
        File directory = folder.getRoot();
        write(directory, "edited before hashes were kept");

        ProviderRegistry.seedDefaultProviders(directory, bundled("v2"));

        Assert.assertEquals("edited before hashes were kept", read(directory));
    }

    @Test
    public void recordsTheBundledProvidersWithoutTouchingCurrentCopies() throws Exception {
        File directory = folder.getRoot();
        for (String name : new String[] { "elyby.json", "littleskin.json", "microsoft.json" }) {
            Files.copy(
                ProviderRegistry.class.getResourceAsStream("/assets/wawelauth/default-providers/" + name),
                new File(directory, name).toPath());
        }
        long modified = new File(directory, "elyby.json").lastModified();

        java.lang.reflect.Method seed = ProviderRegistry.class
            .getDeclaredMethod("seedDefaultProviderDirectoryIfNeeded", File.class);
        seed.setAccessible(true);
        seed.invoke(null, directory);

        Assert.assertEquals(modified, new File(directory, "elyby.json").lastModified());
        String hashes = new String(
            Files.readAllBytes(new File(directory, ".seeded-sha256").toPath()),
            StandardCharsets.ISO_8859_1);
        for (String name : new String[] { "elyby.json", "littleskin.json", "microsoft.json" }) {
            Assert.assertTrue(hashes, hashes.contains(name + "="));
        }
    }

    private static Map<String, byte[]> bundled(String content) {
        return Collections.singletonMap("provider.json", content.getBytes(StandardCharsets.UTF_8));
    }

    private static void write(File directory, String content) throws IOException {
        Files.write(new File(directory, "provider.json").toPath(), content.getBytes(StandardCharsets.UTF_8));
    }

    private static String read(File directory) throws IOException {
        return new String(Files.readAllBytes(new File(directory, "provider.json").toPath()), StandardCharsets.UTF_8);
    }
}
