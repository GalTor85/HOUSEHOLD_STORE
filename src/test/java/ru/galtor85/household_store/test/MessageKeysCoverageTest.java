package ru.galtor85.household_store.test;

import org.junit.jupiter.api.*;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Message Keys Coverage Tests")
class MessageKeysCoverageTest {

    // =========================================================================
    // CONFIGURATION CONSTANTS
    // =========================================================================

    private static final String MESSAGE_BASENAME = "classpath:messages";
    private static final String MESSAGE_ENCODING = "UTF-8";
    private static final boolean FALLBACK_TO_SYSTEM_LOCALE = false;

    private static final String BASE_PROPERTIES_FILE = "messages.properties";
    private static final String PROPERTIES_EXTENSION = ".properties";
    private static final String PROPERTIES_PREFIX = "messages";
    private static final String LOCALE_PROPERTIES_PATTERN = "messages_[a-z]{2}(_[A-Z]{2})?\\.properties";
    private static final String SOURCE_MAIN_JAVA = "src/main/java";
    private static final String RESOURCES_DIRECTORY = "src/main/resources";
    private static final String CLASS_PATH_PREFIX = "classpath:";

    private static final String[] SOURCE_ROOT_CANDIDATES = {
            SOURCE_MAIN_JAVA, "../" + SOURCE_MAIN_JAVA, "../../" + SOURCE_MAIN_JAVA
    };

    private static final Set<String> MESSAGE_SERVICE_METHODS = Set.of(
            "get", "getRequired", "getWithDefault", "exists", "getLn"
    );
    private static final String MESSAGE_SERVICE_METHODS_PATTERN = String.join("|", MESSAGE_SERVICE_METHODS);

    private static final Set<String> VALIDATION_ANNOTATIONS = Set.of(
            "NotBlank", "NotNull", "NotEmpty", "Size", "Min", "Max",
            "Pattern", "Email", "Positive", "Negative", "PositiveOrZero",
            "NegativeOrZero", "AssertTrue", "AssertFalse", "Past", "Future",
            "PastOrPresent", "FutureOrPresent", "DecimalMin", "DecimalMax",
            "Digits", "Length"
    );
    private static final String VALIDATION_ANNOTATIONS_PATTERN = String.join("|", VALIDATION_ANNOTATIONS);

    private static final Set<String> MESSAGE_ANNOTATIONS = Set.of(
            "ResponseStatus", "ExceptionHandler", "ControllerAdvice"
    );
    private static final String MESSAGE_ANNOTATIONS_PATTERN = String.join("|", MESSAGE_ANNOTATIONS);

    // Regex patterns
    private static final Pattern STATIC_KEY_PATTERN;
    private static final Pattern MESSAGE_SOURCE_PATTERN;
    private static final Pattern VALIDATION_ANNOTATION_PATTERN;
    private static final Pattern VALIDATION_ANNOTATION_SIMPLE_PATTERN;
    private static final Pattern RESPONSE_STATUS_PATTERN;
    private static final Pattern DYNAMIC_KEY_PATTERN;
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{(\\d+)\\}");
    private static final Pattern NON_NUMERIC_PLACEHOLDER_PATTERN = Pattern.compile("\\{[^0-9}]|\\{[0-9]+[^0-9}]");
    private static final Pattern EMPTY_BRACES_PATTERN = Pattern.compile("\\{\\s*\\}");

    // Output formatting
    private static final int LINE_SEPARATOR_LENGTH = 90;
    private static final int MAX_KEYS_TO_DISPLAY = 1000;
    private static final int MAX_LINE_LENGTH = 80;
    private static final String LINE_SEPARATOR = "═";

    private static final int MAX_PARENT_DEPTH = 5;

    static {
        STATIC_KEY_PATTERN = Pattern.compile(
                "messageService\\s*\\.\\s*(" + MESSAGE_SERVICE_METHODS_PATTERN + ")\\s*\\(\\s*\"([^\"]+)\""
        );
        MESSAGE_SOURCE_PATTERN = Pattern.compile(
                "messageSource\\s*\\.\\s*getMessage\\s*\\(\\s*\"([^\"]+)\""
        );
        VALIDATION_ANNOTATION_PATTERN = Pattern.compile(
                "@(" + VALIDATION_ANNOTATIONS_PATTERN + ")\\s*\\([^)]*message\\s*=\\s*\"\\{([^}]+)\\}\""
        );
        VALIDATION_ANNOTATION_SIMPLE_PATTERN = Pattern.compile(
                "message\\s*=\\s*\"\\{([^}]+)\\}\""
        );
        RESPONSE_STATUS_PATTERN = Pattern.compile(
                "@(" + MESSAGE_ANNOTATIONS_PATTERN + ")\\s*\\([^)]*reason\\s*=\\s*\"\\{([^}]+)\\}\""
        );
        DYNAMIC_KEY_PATTERN = Pattern.compile(
                "messageService\\s*\\.\\s*(" + MESSAGE_SERVICE_METHODS_PATTERN + ")\\s*\\([^)]*\\+[^)]*\\)"
        );
    }

    // =========================================================================
    // FIELDS
    // =========================================================================

    private static Map<String, Properties> messagesByFile;
    private static Map<String, Set<String>> keysByFile;
    private static Set<String> staticKeysFromCode;
    private static Set<String> annotationKeysFromCode;
    private static Set<String> dynamicKeyWarnings;
    private static List<String> propertyFiles;

    // =========================================================================
    // SETUP
    // =========================================================================

    @BeforeAll
    static void setUp() throws IOException {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));

        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename(MESSAGE_BASENAME);
        messageSource.setDefaultEncoding(MESSAGE_ENCODING);
        messageSource.setFallbackToSystemLocale(FALLBACK_TO_SYSTEM_LOCALE);

        messagesByFile = new LinkedHashMap<>();
        keysByFile = new LinkedHashMap<>();
        propertyFiles = new ArrayList<>();
        staticKeysFromCode = new TreeSet<>();
        annotationKeysFromCode = new TreeSet<>();
        dynamicKeyWarnings = new TreeSet<>();

        discoverAllPropertyFiles();
        extractKeysAndWarningsFromSourceCode();
    }

    private static void discoverAllPropertyFiles() throws IOException {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        String basePath = CLASS_PATH_PREFIX + BASE_PROPERTIES_FILE;
        Resource baseResource = resolver.getResource(basePath);
        if (baseResource.exists()) {
            Properties props = loadProperties(baseResource.getInputStream());
            registerPropertyFile(BASE_PROPERTIES_FILE, props);
        }

        String pattern = CLASS_PATH_PREFIX + PROPERTIES_PREFIX + "_*.properties";
        Resource[] resources = resolver.getResources(pattern);
        for (Resource resource : resources) {
            String filename = resource.getFilename();
            if (filename != null && filename.matches(LOCALE_PROPERTIES_PATTERN)) {
                Properties props = loadProperties(resource.getInputStream());
                registerPropertyFile(filename, props);
            }
        }

        scanFilesystemForPropertyFiles();
    }

    private static void scanFilesystemForPropertyFiles() throws IOException {
        Path resourcesPath = Paths.get(RESOURCES_DIRECTORY);
        if (!Files.exists(resourcesPath)) return;

        try (Stream<Path> walk = Files.walk(resourcesPath)) {
            walk.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().startsWith(PROPERTIES_PREFIX) &&
                            p.getFileName().toString().endsWith(PROPERTIES_EXTENSION))
                    .forEach(p -> {
                        String filename = p.getFileName().toString();
                        if (!propertyFiles.contains(filename)) {
                            try (BufferedReader reader = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                                Properties props = new Properties();
                                props.load(reader);
                                registerPropertyFile(filename, props);
                            } catch (IOException ignored) {}
                        }
                    });
        }
    }

    private static void registerPropertyFile(String filename, Properties props) {
        messagesByFile.put(filename, props);
        keysByFile.put(filename, new TreeSet<>(props.stringPropertyNames()));
        propertyFiles.add(filename);
    }

    private static Properties loadProperties(InputStream input) throws IOException {
        Properties props = new Properties();
        try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            props.load(reader);
        }
        return props;
    }

    private static void extractKeysAndWarningsFromSourceCode() throws IOException {
        Path sourceRoot = findSourceRoot();
        if (sourceRoot == null) return;

        try (Stream<Path> walk = Files.walk(sourceRoot)) {
            walk.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .forEach(MessageKeysCoverageTest::extractFromFile);
        }
    }

    private static Path findSourceRoot() {
        for (String path : SOURCE_ROOT_CANDIDATES) {
            Path p = Paths.get(path);
            if (Files.exists(p) && Files.isDirectory(p)) return p;
        }
        Path current = Paths.get("").toAbsolutePath();
        for (int i = 0; i < MAX_PARENT_DEPTH; i++) {
            Path candidate = current.resolve(SOURCE_MAIN_JAVA);
            if (Files.exists(candidate)) return candidate;
            current = current.getParent();
            if (current == null) break;
        }
        return null;
    }

    private static void extractFromFile(Path file) {
        try {
            String content = Files.readString(file);
            String filename = file.getFileName().toString();

            Matcher staticMatcher = STATIC_KEY_PATTERN.matcher(content);
            while (staticMatcher.find()) staticKeysFromCode.add(staticMatcher.group(2));

            Matcher sourceMatcher = MESSAGE_SOURCE_PATTERN.matcher(content);
            while (sourceMatcher.find()) staticKeysFromCode.add(sourceMatcher.group(1));

            Matcher validationMatcher = VALIDATION_ANNOTATION_PATTERN.matcher(content);
            while (validationMatcher.find()) annotationKeysFromCode.add(validationMatcher.group(2));

            Matcher simpleMatcher = VALIDATION_ANNOTATION_SIMPLE_PATTERN.matcher(content);
            while (simpleMatcher.find()) {
                String key = simpleMatcher.group(1);
                if (key != null && !key.isEmpty() && !key.matches("\\d+")) {
                    annotationKeysFromCode.add(key);
                }
            }

            Matcher responseMatcher = RESPONSE_STATUS_PATTERN.matcher(content);
            while (responseMatcher.find()) annotationKeysFromCode.add(responseMatcher.group(2));

            Matcher dynamicMatcher = DYNAMIC_KEY_PATTERN.matcher(content);
            while (dynamicMatcher.find()) {
                String match = dynamicMatcher.group();
                dynamicKeyWarnings.add(filename + ": " + truncate(match, MAX_LINE_LENGTH));
            }
        } catch (IOException ignored) {}
    }

    private static Set<String> getAllKeysFromCode() {
        Set<String> all = new TreeSet<>();
        all.addAll(staticKeysFromCode);
        all.addAll(annotationKeysFromCode);
        return all;
    }

    private static String truncate(String str, int max) {
        if (str == null) return "null";
        return str.length() <= max ? str : str.substring(0, max - 3) + "...";
    }

    private static List<String> validatePlaceholders(String message) {
        List<String> issues = new ArrayList<>();
        if (message == null) {
            issues.add("message is null");
            return issues;
        }
        if (EMPTY_BRACES_PATTERN.matcher(message).find()) issues.add("empty braces {}");

        long open = message.chars().filter(ch -> ch == '{').count();
        long close = message.chars().filter(ch -> ch == '}').count();
        if (open != close) issues.add("unmatched braces");

        if (NON_NUMERIC_PLACEHOLDER_PATTERN.matcher(message).find()) {
            issues.add("non-numeric placeholder");
        }

        List<Integer> indices = new ArrayList<>();
        Matcher m = PLACEHOLDER_PATTERN.matcher(message);
        while (m.find()) indices.add(Integer.parseInt(m.group(1)));

        for (int i = 1; i < indices.size(); i++) {
            if (indices.get(i) < indices.get(i - 1)) {
                issues.add("placeholders out of order: " + indices);
                break;
            }
        }
        return issues;
    }

    // =========================================================================
    // TESTS (в правильном порядке)
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("1. Statistics")
    void showStatistics() {
        System.out.println("\n" + LINE_SEPARATOR.repeat(LINE_SEPARATOR_LENGTH));
        System.out.println("📊 STATISTICS");
        System.out.println(LINE_SEPARATOR.repeat(LINE_SEPARATOR_LENGTH));
        System.out.println("  Static keys from MessageService: " + staticKeysFromCode.size());
        System.out.println("  Keys from annotations: " + annotationKeysFromCode.size());
        System.out.println("  Total keys from code: " + getAllKeysFromCode().size());
        System.out.println("  Dynamic key constructions: " + dynamicKeyWarnings.size());
        System.out.println("  Property files: " + propertyFiles.size());
        for (String f : propertyFiles) {
            System.out.println("    • " + f + ": " + keysByFile.get(f).size() + " keys");
        }
        System.out.println(LINE_SEPARATOR.repeat(LINE_SEPARATOR_LENGTH));
    }

    @Test
    @Order(2)
    @DisplayName("2. Dynamic key constructions (manual check required)")
    void reportDynamicKeyWarnings() {
        System.out.println("\n" + LINE_SEPARATOR.repeat(LINE_SEPARATOR_LENGTH));
        System.out.println("⚠️ DYNAMIC KEY CONSTRUCTIONS");
        System.out.println(LINE_SEPARATOR.repeat(LINE_SEPARATOR_LENGTH));

        if (dynamicKeyWarnings.isEmpty()) {
            System.out.println("✅ No dynamic key constructions found");
            return;
        }

        System.out.println("These keys are built dynamically - verify manually:");
        System.out.println("─".repeat(LINE_SEPARATOR_LENGTH));
        int count = 0;
        for (String warning : dynamicKeyWarnings) {
            if (++count > MAX_KEYS_TO_DISPLAY) {
                System.out.println("  ... and " + (dynamicKeyWarnings.size() - MAX_KEYS_TO_DISPLAY) + " more");
                break;
            }
            System.out.println("  " + count + ". " + warning);
        }
    }

    @Test
    @Order(3)
    @DisplayName("3. Missing keys (in code but NOT in properties)")
    void reportMissingKeys() {
        System.out.println("\n" + LINE_SEPARATOR.repeat(LINE_SEPARATOR_LENGTH));
        System.out.println("❌ MISSING KEYS (in code but NOT in properties)");
        System.out.println(LINE_SEPARATOR.repeat(LINE_SEPARATOR_LENGTH));

        Set<String> allCodeKeys = getAllKeysFromCode();
        boolean hasMissing = false;

        for (String filename : propertyFiles) {
            Set<String> fileKeys = keysByFile.get(filename);
            Set<String> missing = new TreeSet<>(allCodeKeys);
            missing.removeAll(fileKeys);

            if (!missing.isEmpty()) {
                hasMissing = true;
                System.out.println("\n📁 " + filename);
                System.out.println("   Missing: " + missing.size() + " keys");
                System.out.println("   ─".repeat(50));
                int count = 0;
                for (String key : missing) {
                    if (++count > MAX_KEYS_TO_DISPLAY) {
                        System.out.println("   ... and " + (missing.size() - MAX_KEYS_TO_DISPLAY) + " more");
                        break;
                    }
                    System.out.println("   " + count + ". " + key);
                }
            }
        }

        if (!hasMissing) {
            System.out.println("✅ No missing keys");
        }
    }

    @Test
    @Order(4)
    @DisplayName("4. Annotation keys (from validation annotations)")
    void showAnnotationKeys() {
        System.out.println("\n" + LINE_SEPARATOR.repeat(LINE_SEPARATOR_LENGTH));
        System.out.println("🏷️ ANNOTATION KEYS");
        System.out.println(LINE_SEPARATOR.repeat(LINE_SEPARATOR_LENGTH));

        if (annotationKeysFromCode.isEmpty()) {
            System.out.println("✅ No annotation keys found");
            return;
        }

        System.out.println("Keys found in validation annotations (@NotBlank, @Size, @Pattern, etc.):");
        System.out.println("─".repeat(LINE_SEPARATOR_LENGTH));
        int count = 0;
        for (String key : annotationKeysFromCode) {
            if (++count > MAX_KEYS_TO_DISPLAY) {
                System.out.println("... and " + (annotationKeysFromCode.size() - MAX_KEYS_TO_DISPLAY) + " more");
                break;
            }
            System.out.println("  " + count + ". " + key);
        }
    }

    @Test
    @Order(5)
    @DisplayName("5. Orphaned keys (in properties but NOT in code, including annotations)")
    void reportOrphanedKeys() {
        System.out.println("\n" + LINE_SEPARATOR.repeat(LINE_SEPARATOR_LENGTH));
        System.out.println("⚠️  ORPHANED KEYS (in properties, but NOT in code)");
        System.out.println(LINE_SEPARATOR.repeat(LINE_SEPARATOR_LENGTH));
        System.out.println("   ATTENTION! Dynamic access is possible.");
        System.out.println("   Check if these keys are used via:");
        System.out.println("   • Dynamic key construction (e.g., \"prefix.\" + variable)");
        System.out.println("   • Reflection or runtime loading");
        System.out.println("   • External configuration references");
        System.out.println(LINE_SEPARATOR.repeat(LINE_SEPARATOR_LENGTH));

        // ВАЖНО: используем getAllKeysFromCode() который включает аннотации
        Set<String> allCodeKeys = getAllKeysFromCode();
        boolean hasOrphaned = false;

        for (String filename : propertyFiles) {
            Set<String> fileKeys = keysByFile.get(filename);
            Set<String> orphaned = new TreeSet<>(fileKeys);
            orphaned.removeAll(allCodeKeys);

            if (!orphaned.isEmpty()) {
                hasOrphaned = true;
                System.out.println("\n📁 " + filename);
                System.out.println("   Orphaned: " + orphaned.size() + " keys (not used anywhere in code)");
                System.out.println("   ─".repeat(50));
                int count = 0;
                for (String key : orphaned) {
                    if (++count > MAX_KEYS_TO_DISPLAY) {
                        System.out.println("   ... and " + (orphaned.size() - MAX_KEYS_TO_DISPLAY) + " more");
                        break;
                    }
                    System.out.println("   " + count + ". " + key);
                }
            }
        }

        if (!hasOrphaned) {
            System.out.println("✅ No orphaned keys - all property keys are used in code");
        } else {
            System.out.println("\n💡 Consider removing these keys or adding them to code");
        }
    }

    @Test
    @Order(6)
    @DisplayName("6. Invalid placeholders")
    void reportInvalidPlaceholders() {
        System.out.println("\n" + LINE_SEPARATOR.repeat(LINE_SEPARATOR_LENGTH));
        System.out.println("❌ INVALID PLACEHOLDERS");
        System.out.println(LINE_SEPARATOR.repeat(LINE_SEPARATOR_LENGTH));

        boolean hasInvalid = false;

        for (String filename : propertyFiles) {
            Properties props = messagesByFile.get(filename);
            Map<String, String> invalid = new LinkedHashMap<>();

            for (String key : props.stringPropertyNames()) {
                String value = props.getProperty(key);
                List<String> issues = validatePlaceholders(value);
                if (!issues.isEmpty()) {
                    invalid.put(key, value + " → " + String.join(", ", issues));
                }
            }

            if (!invalid.isEmpty()) {
                hasInvalid = true;
                System.out.println("\n📁 " + filename);
                System.out.println("   Invalid: " + invalid.size() + " placeholders");
                System.out.println("   ─".repeat(50));
                int count = 0;
                for (Map.Entry<String, String> entry : invalid.entrySet()) {
                    if (++count > MAX_KEYS_TO_DISPLAY) {
                        System.out.println("   ... and " + (invalid.size() - MAX_KEYS_TO_DISPLAY) + " more");
                        break;
                    }
                    System.out.println("   " + count + ". " + entry.getKey());
                    System.out.println("      → " + truncate(entry.getValue(), 70));
                }
            }
        }

        if (!hasInvalid) {
            System.out.println("✅ No invalid placeholders");
        }
    }

    @Test
    @Order(7)
    @DisplayName("7. Empty values")
    void reportEmptyValues() {
        System.out.println("\n" + LINE_SEPARATOR.repeat(LINE_SEPARATOR_LENGTH));
        System.out.println("❌ EMPTY VALUES");
        System.out.println(LINE_SEPARATOR.repeat(LINE_SEPARATOR_LENGTH));

        boolean hasEmpty = false;

        for (String filename : propertyFiles) {
            Properties props = messagesByFile.get(filename);
            Set<String> empty = new TreeSet<>();

            for (String key : props.stringPropertyNames()) {
                String value = props.getProperty(key);
                if (value == null || value.trim().isEmpty()) {
                    empty.add(key);
                }
            }

            if (!empty.isEmpty()) {
                hasEmpty = true;
                System.out.println("\n📁 " + filename);
                System.out.println("   Empty values: " + empty.size() + " keys");
                System.out.println("   ─".repeat(50));
                int count = 0;
                for (String key : empty) {
                    if (++count > MAX_KEYS_TO_DISPLAY) {
                        System.out.println("   ... and " + (empty.size() - MAX_KEYS_TO_DISPLAY) + " more");
                        break;
                    }
                    System.out.println("   " + count + ". " + key);
                }
            }
        }

        if (!hasEmpty) {
            System.out.println("✅ No empty values");
        }
    }
}