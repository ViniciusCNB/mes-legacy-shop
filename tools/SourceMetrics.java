import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

// Physical lines, including comments; this is not a language-aware SLOC counter.
class SourceMetrics {
    public static void main(String[] args) throws IOException {
        Path root = Path.of("").toAbsolutePath().normalize();
        if (!Files.isRegularFile(root.resolve("pom.xml")) || !Files.isDirectory(root.resolve("src/main/java"))) {
            throw new IllegalArgumentException("Execute a partir da raiz de mes-legacy-shop");
        }
        Map<String, List<Path>> groups = new LinkedHashMap<>();
        groups.put("production_java", files(root, "src/main/java"));
        groups.put("internal_test_java", files(root, "src/test/java"));
        groups.put("external_test_java", files(root, "evaluation/src/test/java"));
        groups.put("configuration_infrastructure", files(root,
                ".gitignore", ".gitattributes", ".env.example", ".mvn", "mvnw", "mvnw.cmd",
                "pom.xml", "compose.yaml", "src/main/resources", "src/test/resources",
                "evaluation/pom.xml", "evaluation/compose.yaml", "evaluation/fixtures",
                "evaluation/run.ps1", "evaluation/run.sh", "evaluation/SNAPSHOT.sha256", "tools/SourceMetrics.java"));
        groups.put("documentation", files(root, "README.md", "AGENTS.md", "docs", "evaluation/README.md"));
        System.out.println("group,files,physical_lines,nonblank_lines");
        for (Map.Entry<String, List<Path>> group : groups.entrySet()) {
            long lines = 0;
            long nonblank = 0;
            for (Path file : group.getValue()) {
                List<String> content = Files.readAllLines(file, StandardCharsets.UTF_8);
                lines += content.size();
                nonblank += content.stream().filter(line -> !line.isBlank()).count();
            }
            System.out.printf("%s,%d,%d,%d%n", group.getKey(), group.getValue().size(), lines, nonblank);
        }
    }

    private static List<Path> files(Path root, String... names) throws IOException {
        List<Path> files = new ArrayList<>();
        for (String name : names) {
            Path path = root.resolve(name);
            if (Files.isDirectory(path)) {
                try (Stream<Path> tree = Files.walk(path)) {
                    tree.filter(file -> Files.isRegularFile(file) && !Files.isSymbolicLink(file))
                            .sorted().forEach(files::add);
                }
            } else if (Files.isRegularFile(path)) {
                files.add(path);
            }
        }
        return files;
    }
}
