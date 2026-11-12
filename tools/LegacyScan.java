import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Counts textual markers of the Java 8 / Spring Boot 2.1 stack that the migration to Java 17 /
// Spring Boot 3.5 must remove or replace. Kind "quebra": the code fails to build, start or pass the
// tests on Boot 3.5 while the marker remains. Kind "obsoleto": still works on Boot 3.5, but is
// deprecated or replaced by the idiom of the target stack.
// Usage, with JDK 11+: java tools/LegacyScan.java <project-root>
// Output: one TSV line per marker and file, then totals per marker. Markers are textual evidence,
// not a compiler check; docs/CARACTERIZACAO_ORIGEM.md maps them to the official sources.
class LegacyScan {

    private record Marker(String id, String kind, Pattern pattern) { }

    private static final List<Marker> MARKERS = List.of(
            marker("M01-javax-jakarta", "quebra", "import\\s+javax\\.(persistence|validation|servlet|annotation|transaction)\\."),
            marker("M02-websecurityconfigureradapter", "quebra", "\\bWebSecurityConfigurerAdapter\\b"),
            marker("M03-antmatchers", "quebra", "\\.(antMatchers|mvcMatchers|regexMatchers)\\("),
            marker("M04-authorizerequests", "obsoleto", "\\.authorizeRequests\\(\\)"),
            marker("M05-security-and-chain", "obsoleto", "\\.and\\(\\)"),
            marker("M06-enablewebsecurity-sem-configuration", "quebra", null),
            marker("M07-csrf-sem-request-handler", "quebra", null),
            marker("M08-dialeto-versionado", "quebra", "org\\.hibernate\\.dialect\\.PostgreSQL\\d+Dialect"),
            marker("M09-propriedade-removida", "obsoleto", "initialization-mode"),
            marker("M10-java8", "quebra", "<java\\.version>1\\.8</java\\.version>|\\[1\\.8,1\\.9\\)"),
            marker("M11-boot-2.1", "quebra", "<version>2\\.1\\.\\d+\\.RELEASE</version>"),
            marker("M12-junit4", "quebra", "import\\s+org\\.junit\\.(Test|Rule|ClassRule|After|Before|AfterClass|BeforeClass|runner\\.RunWith)\\b"),
            marker("M13-spring-junit4", "quebra", "\\b(SpringRunner|SpringJUnit4ClassRunner|SpringClassRule|SpringMethodRule)\\b"),
            marker("M14-junitparams", "quebra", "\\bjunitparams\\b|JUnitParams"),
            marker("M15-testcontainers-classrule", "obsoleto", "@ClassRule\\s+public\\s+static\\s+final\\s+PostgreSQLContainer"),
            marker("M16-config-file-initializer", "quebra", "\\bConfigFileApplicationContextInitializer\\b"),
            marker("M17-localserverport-antigo", "quebra", "org\\.springframework\\.boot\\.web\\.server\\.LocalServerPort"),
            marker("M18-httpclient4", "quebra", "\\borg\\.apache\\.http\\.|<artifactId>httpclient</artifactId>"),
            marker("M19-context-initializer-teste", "obsoleto", "implements\\s+ApplicationContextInitializer"));

    private static Marker marker(String id, String kind, String regex) {
        return new Marker(id, kind, regex == null ? null : Pattern.compile(regex));
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new IllegalArgumentException("uso: java tools/LegacyScan.java <raiz-do-projeto>");
        }
        Path root = Path.of(args[0]).toAbsolutePath().normalize();
        List<Path> files = new ArrayList<>();
        if (Files.isRegularFile(root.resolve("pom.xml"))) {
            files.add(root.resolve("pom.xml"));
        }
        for (String dir : List.of("src/main", "src/test")) {
            if (Files.isDirectory(root.resolve(dir))) {
                try (Stream<Path> walk = Files.walk(root.resolve(dir))) {
                    files.addAll(walk.filter(Files::isRegularFile)
                            .filter(f -> f.toString().matches(".*\\.(java|yml|yaml|properties|xml)$"))
                            .sorted().collect(Collectors.toList()));
                }
            }
        }
        Map<String, Integer> totals = new TreeMap<>();
        for (Marker marker : MARKERS) {
            totals.put(marker.id(), 0);
        }
        System.out.println("marker\tkind\tfile\toccurrences");
        for (Path file : files) {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            String relative = root.relativize(file).toString().replace('\\', '/');
            for (Marker marker : MARKERS) {
                int n = count(marker, text);
                if (n > 0) {
                    totals.merge(marker.id(), n, Integer::sum);
                    System.out.printf("%s\t%s\t%s\t%d%n", marker.id(), marker.kind(), relative, n);
                }
            }
        }
        int all = 0;
        for (Marker marker : MARKERS) {
            int n = totals.get(marker.id());
            all += n;
            System.out.printf("TOTAL\t%s\t%s\t%d%n", marker.kind(), marker.id(), n);
        }
        System.out.printf("TOTAL\t-\tall\t%d%n", all);
    }

    private static int count(Marker marker, String text) {
        switch (marker.id()) {
            case "M06-enablewebsecurity-sem-configuration":
                return text.contains("@EnableWebSecurity") && !text.contains("@Configuration") ? 1 : 0;
            case "M07-csrf-sem-request-handler":
                return text.contains("CookieCsrfTokenRepository") && !text.contains("csrfTokenRequestHandler") ? 1 : 0;
            default:
                Matcher matcher = marker.pattern().matcher(text);
                int n = 0;
                while (matcher.find()) {
                    n++;
                }
                return n;
        }
    }
}
