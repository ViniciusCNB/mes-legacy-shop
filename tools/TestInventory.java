import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Inventory of the internal test suite: one line per test method with the number of executed cases
// (from Surefire/Failsafe XML reports), assertion call sites in the method body and skip markers.
// Usage, from the repository root and with JDK 11+:
//   java tools/TestInventory.java <test-source-dir> <failsafe-reports-dir>
// Assertion call sites are textual: calls named assert*, plus the suite helpers error(...) and unchanged(...).
// They are a comparison aid between two versions of the same suite, not a semantic measure.
class TestInventory {

    private static final Pattern TEST_ANNOTATION = Pattern.compile("@(Test|ParameterizedTest)\\b");
    private static final Pattern METHOD = Pattern.compile("\\b(?:public\\s+|protected\\s+|private\\s+)?void\\s+(\\w+)\\s*\\(");
    private static final Pattern ASSERTION = Pattern.compile("\\b(assert\\w*|error|unchanged)\\s*\\(");
    private static final Pattern SKIP = Pattern.compile("@(Ignore|Disabled)\\b|\\bassume\\w*\\s*\\(");
    private static final Pattern TESTCASE = Pattern.compile(
            "<testcase\\s+name=\"([^\"]*)\"\\s+classname=\"([^\"]*)\"[^>]*?(/>|>(.*?)</testcase>)", Pattern.DOTALL);

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            throw new IllegalArgumentException("uso: java tools/TestInventory.java <fontes-de-teste> <relatorios-failsafe>");
        }
        Map<String, int[]> rows = new TreeMap<>();
        try (Stream<Path> files = Files.walk(Path.of(args[0]))) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).collect(Collectors.toList())) {
                scanSource(file, rows);
            }
        }
        try (Stream<Path> reports = Files.list(Path.of(args[1]))) {
            for (Path report : reports.filter(f -> f.getFileName().toString().matches("TEST-.*\\.xml"))
                    .collect(Collectors.toList())) {
                scanReport(report, rows);
            }
        }
        System.out.println("class\tmethod\tcases_executed\tcases_skipped\tassertion_sites\tskip_markers");
        int[] total = new int[4];
        for (Map.Entry<String, int[]> row : rows.entrySet()) {
            int[] v = row.getValue();
            System.out.printf("%s\t%d\t%d\t%d\t%d%n", row.getKey().replace('#', '\t'), v[0], v[1], v[2], v[3]);
            for (int i = 0; i < 4; i++) {
                total[i] += v[i];
            }
        }
        System.out.printf("TOTAL\t%d methods\t%d\t%d\t%d\t%d%n", rows.size(), total[0], total[1], total[2], total[3]);
    }

    private static void scanSource(Path file, Map<String, int[]> rows) throws IOException {
        String source = Files.readString(file, StandardCharsets.UTF_8);
        String className = file.getFileName().toString().replace(".java", "");
        Matcher annotation = TEST_ANNOTATION.matcher(source);
        while (annotation.find()) {
            Matcher method = METHOD.matcher(source);
            if (!method.find(annotation.end())) {
                continue;
            }
            int open = source.indexOf('{', method.end());
            int close = matchingBrace(source, open);
            String header = source.substring(annotation.start(), open);
            String body = source.substring(open, close + 1);
            int[] row = rows.computeIfAbsent(className + "#" + method.group(1), key -> new int[4]);
            row[2] = count(ASSERTION, body);
            row[3] = count(SKIP, header) + count(SKIP, body);
        }
    }

    private static void scanReport(Path report, Map<String, int[]> rows) throws IOException {
        String xml = Files.readString(report, StandardCharsets.UTF_8);
        Matcher testcase = TESTCASE.matcher(xml);
        while (testcase.find()) {
            String className = testcase.group(2).substring(testcase.group(2).lastIndexOf('.') + 1);
            Matcher name = Pattern.compile("^(\\w+)").matcher(testcase.group(1));
            if (!name.find()) {
                continue;
            }
            int[] row = rows.computeIfAbsent(className + "#" + name.group(1), key -> new int[4]);
            String content = testcase.group(4) == null ? "" : testcase.group(4);
            if (content.contains("<skipped")) {
                row[1]++;
            } else {
                row[0]++;
            }
        }
    }

    private static int matchingBrace(String source, int open) {
        int depth = 0;
        for (int i = open; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '"') {
                i = endOfString(source, i);
            } else if (c == '{') {
                depth++;
            } else if (c == '}' && --depth == 0) {
                return i;
            }
        }
        throw new IllegalStateException("chave sem fechamento");
    }

    private static int endOfString(String source, int quote) {
        for (int i = quote + 1; i < source.length(); i++) {
            if (source.charAt(i) == '\\') {
                i++;
            } else if (source.charAt(i) == '"') {
                return i;
            }
        }
        return source.length();
    }

    private static int count(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        int n = 0;
        while (matcher.find()) {
            n++;
        }
        return n;
    }
}
