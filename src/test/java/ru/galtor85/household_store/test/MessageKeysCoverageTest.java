package ru.galtor85.household_store.test;

import org.junit.jupiter.api.*;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
            SOURCE_MAIN_JAVA,
            "../" + SOURCE_MAIN_JAVA,
            "../../" + SOURCE_MAIN_JAVA
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

    // Regex patterns
    private static final Pattern STATIC_KEY_PATTERN;
    private static final Pattern MESSAGE_SOURCE_PATTERN;
    private static final Pattern VALIDATION_ANNOTATION_PATTERN;
    private static final Pattern VALIDATION_ANNOTATION_SIMPLE_PATTERN;
    private static final Pattern DYNAMIC_KEY_PATTERN;
    private static final Pattern ENUM_CONCAT_PATTERN;
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{(\\d+)}");
    private static final Pattern NON_NUMERIC_PLACEHOLDER_PATTERN = Pattern.compile("\\{[^0-9}]|\\{[0-9]+[^0-9}]");
    private static final Pattern EMPTY_BRACES_PATTERN = Pattern.compile("\\{\\s*}");

    // Output formatting
    private static final int LINE_SEPARATOR_LENGTH = 90;
    private static final int MAX_KEYS_TO_DISPLAY = 1000;
    private static final int MAX_LINE_LENGTH = 80;
    private static final String LINE_SEPARATOR = "═";

    private static final int MAX_PARENT_DEPTH = 5;

    static {
        STATIC_KEY_PATTERN = Pattern.compile(
                "(?:messageService|logMsg)\\s*\\.\\s*(" + MESSAGE_SERVICE_METHODS_PATTERN + ")\\s*\\(\\s*\"([^\"]+)\""
        );
        MESSAGE_SOURCE_PATTERN = Pattern.compile(
                "messageSource\\s*\\.\\s*getMessage\\s*\\(\\s*\"([^\"]+)\""
        );
        VALIDATION_ANNOTATION_PATTERN = Pattern.compile(
                "@(" + VALIDATION_ANNOTATIONS_PATTERN + ")\\s*\\([^)]*message\\s*=\\s*\"\\{([^}]+)}\""
        );
        VALIDATION_ANNOTATION_SIMPLE_PATTERN = Pattern.compile(
                "message\\s*=\\s*\"\\{([^}]+)}\""
        );
        DYNAMIC_KEY_PATTERN = Pattern.compile(
                "(?:messageService|logMsg)\\s*\\.\\s*(" + MESSAGE_SERVICE_METHODS_PATTERN + ")\\s*\\([^)]*\\+[^)]*\\)"
        );
        ENUM_CONCAT_PATTERN = Pattern.compile(
                "(?:messageService|logMsg)\\s*\\.\\s*get\\s*\\(\\s*\"([^\"]+)\"\\s*\\+\\s*(\\w+(?:\\.\\w+)*\\([^)]*\\)|\\.\\w+)"
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

    private static Map<String, Set<String>> enumValuesByPrefix;
    private static Set<String> generatedDynamicKeys;

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
        enumValuesByPrefix = new LinkedHashMap<>();
        generatedDynamicKeys = new TreeSet<>();

        discoverAllPropertyFiles();
        analyzeEnumClasses();
        extractKeysAndWarningsFromSourceCode();
        generateDynamicKeysFromEnums();
    }

    // =========================================================================
    // VALIDATION UTILS
    // =========================================================================

    /**
     * Checks if a key is valid (not ending with a dot).
     * Keys ending with a dot are prefixes used for dynamic concatenation.
     */
    private static boolean isValidKey(String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        return !key.endsWith(".");
    }

    /**
     * Gets all valid keys from code (filters out prefix keys ending with dot).
     */
    private static Set<String> getValidKeysFromCode() {
        Set<String> allKeys = getAllKeysFromCode();
        return allKeys.stream()
                .filter(MessageKeysCoverageTest::isValidKey)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    // =========================================================================
    // PROPERTIES FILES DISCOVERY
    // =========================================================================

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
        if (!Files.exists(resourcesPath) || !Files.isDirectory(resourcesPath)) {
            return;
        }

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
                            } catch (IOException ignored) {
                            }
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

    // =========================================================================
    // ENUM ANALYSIS
    // =========================================================================

    private static void analyzeEnumClasses() throws IOException {
        Path sourceRoot = findSourceRoot();
        if (sourceRoot == null) {
            return;
        }

        List<Path> enumFiles;
        try (Stream<Path> walk = Files.walk(sourceRoot)) {
            enumFiles = walk.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> {
                        try {
                            String content = Files.readString(p);
                            return content.contains("public enum ");
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .toList();
        }

        for (Path enumFile : enumFiles) {
            analyzeEnumFile(enumFile);
        }
    }

    private static void analyzeEnumFile(Path enumFile) {
        try {
            String content = Files.readString(enumFile);
            String className = enumFile.getFileName().toString().replace(".java", "");

            String prefix = determinePrefixForEnum(className);
            if (prefix == null) {
                return;
            }

            Pattern enumConstantPattern = Pattern.compile("^\\s*([A-Z][A-Z0-9_]*)\\s*[,(;]", Pattern.MULTILINE);
            Matcher matcher = enumConstantPattern.matcher(content);

            Set<String> values = new TreeSet<>();
            while (matcher.find()) {
                values.add(matcher.group(1));
            }

            if (!values.isEmpty()) {
                enumValuesByPrefix.put(prefix, values);
            }

            if (className.equals("Currency")) {
                enumValuesByPrefix.put("currency.name", values);
                enumValuesByPrefix.put("currency.symbol", values);
            }

        } catch (IOException ignored) {
        }
    }

    private static String determinePrefixForEnum(String className) {
        return switch (className) {
            case "BankAccountType" -> "bank.account.type";
            case "CartStatus" -> "cart.status";
            case "CellType" -> "cell.type";
            case "InvoiceStatus" -> "invoice.status";
            case "MovementType" -> "movement.type";
            case "OrderStatus" -> "order.status";
            case "OrderPaymentStatus" -> "order.payment.status";
            case "PaymentMethod" -> "payment.method";
            case "PaymentTransactionStatus" -> "payment.status";
            case "Role" -> "role.name";
            case "SupplierStatus" -> "supplier.status";
            case "TransactionType" -> "transaction.type";
            case "UserType" -> "usertype";
            default -> null;
        };
    }

    private static void generateDynamicKeysFromEnums() {
        for (Map.Entry<String, Set<String>> entry : enumValuesByPrefix.entrySet()) {
            String prefix = entry.getKey();
            for (String value : entry.getValue()) {
                String key = prefix + "." + value.toLowerCase();
                if (isValidKey(key)) {
                    generatedDynamicKeys.add(key);
                }
            }
        }
        generatedDynamicKeys.addAll(generateCompositeKeys());
    }

    private static Set<String> generateCompositeKeys() {
        Set<String> compositeKeys = new TreeSet<>();

        String[] orderTypes = {"purchase", "sales"};
        String[] paymentTypes = {"payment", "receipt"};
        String[] invoiceTypes = {"full", "partial"};
        String[] salesTypes = {"retail", "wholesale"};

        for (String invoiceType : invoiceTypes) {
            for (String orderType : orderTypes) {
                for (String paymentType : paymentTypes) {
                    compositeKeys.add("invoice." + invoiceType + "." + orderType + "." + paymentType);
                }
            }
        }

        for (String salesType : salesTypes) {
            compositeKeys.add("invoice.notes.sales." + salesType);
        }

        String[] reservationStatuses = {"active", "expired", "completed"};
        for (String status : reservationStatuses) {
            compositeKeys.add("order.reservation.status." + status);
        }

        String[] stockStatuses = {"in_stock", "low_stock", "out_of_stock"};
        for (String status : stockStatuses) {
            compositeKeys.add("stock.status." + status);
            compositeKeys.add("stock.message." + status);
        }

        String[] supplierErrorFields = {"email", "inn", "not_found"};
        for (String field : supplierErrorFields) {
            compositeKeys.add("manager.supplier.error." + field);
        }

        return compositeKeys;
    }

    // =========================================================================
    // SOURCE CODE EXTRACTION
    // =========================================================================

    private static Path findSourceRoot() {
        for (String path : SOURCE_ROOT_CANDIDATES) {
            Path p = Paths.get(path);
            if (Files.exists(p) && Files.isDirectory(p)) {
                return p;
            }
        }
        Path current = Paths.get("").toAbsolutePath();
        for (int i = 0; i < MAX_PARENT_DEPTH; i++) {
            Path candidate = current.resolve(SOURCE_MAIN_JAVA);
            if (Files.exists(candidate) && Files.isDirectory(candidate)) {
                return candidate;
            }
            current = current.getParent();
            if (current == null) {
                break;
            }
        }
        return null;
    }

    private static void extractKeysAndWarningsFromSourceCode() throws IOException {
        Path sourceRoot = findSourceRoot();
        if (sourceRoot == null) {
            return;
        }

        try (Stream<Path> walk = Files.walk(sourceRoot)) {
            walk.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .forEach(MessageKeysCoverageTest::extractFromFile);
        }
    }

    private static void extractFromFile(Path file) {
        try {
            String content = Files.readString(file);
            String filename = file.getFileName().toString();

            Matcher staticMatcher = STATIC_KEY_PATTERN.matcher(content);
            while (staticMatcher.find()) {
                String key = staticMatcher.group(2);
                if (isValidKey(key)) {
                    staticKeysFromCode.add(key);
                }
            }

            Matcher sourceMatcher = MESSAGE_SOURCE_PATTERN.matcher(content);
            while (sourceMatcher.find()) {
                String key = sourceMatcher.group(1);
                if (isValidKey(key)) {
                    staticKeysFromCode.add(key);
                }
            }

            Matcher validationMatcher = VALIDATION_ANNOTATION_PATTERN.matcher(content);
            while (validationMatcher.find()) {
                String key = validationMatcher.group(2);
                if (isValidKey(key)) {
                    annotationKeysFromCode.add(key);
                }
            }

            Matcher simpleMatcher = VALIDATION_ANNOTATION_SIMPLE_PATTERN.matcher(content);
            while (simpleMatcher.find()) {
                String key = simpleMatcher.group(1);
                if (key != null && !key.isEmpty() && !key.matches("\\d+") && isValidKey(key)) {
                    annotationKeysFromCode.add(key);
                }
            }

            Matcher dynamicMatcher = DYNAMIC_KEY_PATTERN.matcher(content);
            while (dynamicMatcher.find()) {
                String match = dynamicMatcher.group();
                dynamicKeyWarnings.add(filename + ": " + truncate(match, MAX_LINE_LENGTH));
            }

            Matcher enumConcatMatcher = ENUM_CONCAT_PATTERN.matcher(content);
            while (enumConcatMatcher.find()) {
                String prefix = enumConcatMatcher.group(1);
                String expr = enumConcatMatcher.group(2);
                dynamicKeyWarnings.add(filename + ": " + truncate(prefix + " + " + expr, MAX_LINE_LENGTH));
            }

        } catch (IOException ignored) {
        }
    }

    private static Set<String> getAllKeysFromCode() {
        Set<String> all = new TreeSet<>();
        all.addAll(staticKeysFromCode);
        all.addAll(annotationKeysFromCode);
        all.addAll(generatedDynamicKeys);
        return all;
    }

    private static String truncate(String str, int max) {
        if (str == null) {
            return "null";
        }
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
    // TESTS
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("1. Statistics")
    void showStatistics() {
        Set<String> validCodeKeys = getValidKeysFromCode();

        long prefixKeysCount = getAllKeysFromCode().stream()
                .filter(k -> k.endsWith("."))
                .count();

        Set<String> redundantKeys = new TreeSet<>(generatedDynamicKeys);
        redundantKeys.removeAll(staticKeysFromCode);
        redundantKeys.removeAll(annotationKeysFromCode);

        System.out.println("\n" + LINE_SEPARATOR.repeat(LINE_SEPARATOR_LENGTH));
        System.out.println("📊 STATISTICS");
        System.out.println(LINE_SEPARATOR.repeat(LINE_SEPARATOR_LENGTH));
        System.out.println("  Static keys from MessageService: " + staticKeysFromCode.size());
        System.out.println("  Keys from annotations:          " + annotationKeysFromCode.size());
        System.out.println("  Generated dynamic keys:         " + generatedDynamicKeys.size());
        System.out.println("  ───────────────────────────────────");
        System.out.println("  Prefix keys (excluded):         " + prefixKeysCount + " ❌ (ending with '.')");
        System.out.println("  ───────────────────────────────────");
        System.out.println("  Total VALID keys from code:     " + validCodeKeys.size());
        System.out.println("  Redundant dynamic keys:         " + redundantKeys.size() + " ⚠️ (may be unused)");
        System.out.println();
        System.out.println("  Dynamic key constructions:      " + dynamicKeyWarnings.size());
        System.out.println("  Property files:                 " + propertyFiles.size());
        for (String f : propertyFiles) {
            System.out.println("    • " + f + ": " + keysByFile.get(f).size() + " keys");
        }
        System.out.println(LINE_SEPARATOR.repeat(LINE_SEPARATOR_LENGTH));
    }

    @Test
    @Order(2)
    @DisplayName("2. Generated Dynamic Keys (from Enums)")
    void showGeneratedDynamicKeys() {
        System.out.println("\n" + LINE_SEPARATOR.repeat(LINE_SEPARATOR_LENGTH));
        System.out.println("🔑 GENERATED DYNAMIC KEYS (from Enums) - lowercase only");
        System.out.println(LINE_SEPARATOR.repeat(LINE_SEPARATOR_LENGTH));

        if (generatedDynamicKeys.isEmpty()) {
            System.out.println("❌ No dynamic keys generated");
            return;
        }

        System.out.println("Total generated keys: " + generatedDynamicKeys.size());
        System.out.println("─".repeat(LINE_SEPARATOR_LENGTH));

        int count = 0;
        for (String key : generatedDynamicKeys) {
            if (++count > MAX_KEYS_TO_DISPLAY) {
                System.out.println("  ... and " + (generatedDynamicKeys.size() - MAX_KEYS_TO_DISPLAY) + " more");
                break;
            }
            System.out.println("  " + count + ". " + key);
        }
    }

    @Test
    @Order(3)
    @DisplayName("3. Enum Values Found")
    void showEnumValues() {
        System.out.println("\n" + LINE_SEPARATOR.repeat(LINE_SEPARATOR_LENGTH));
        System.out.println("📋 ENUM VALUES FOUND");
        System.out.println(LINE_SEPARATOR.repeat(LINE_SEPARATOR_LENGTH));

        if (enumValuesByPrefix.isEmpty()) {
            System.out.println("❌ No enum values found");
            return;
        }

        for (Map.Entry<String, Set<String>> entry : enumValuesByPrefix.entrySet()) {
            System.out.println("\n  Prefix: " + entry.getKey());
            Set<String> lowerValues = entry.getValue().stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());
            System.out.println("  Values (lowercase): " + String.join(", ", lowerValues));
        }
    }

    @Test
    @Order(4)
    @DisplayName("4. Redundant keys (generated but NOT found in static code)")
    void reportRedundantKeys() {
        System.out.println("\n" + LINE_SEPARATOR.repeat(LINE_SEPARATOR_LENGTH));
        System.out.println("⚠️ REDUNDANT KEYS (Generated from enums, but NOT found in static code)");
        System.out.println(LINE_SEPARATOR.repeat(LINE_SEPARATOR_LENGTH));
        System.out.println("   These keys MAY be unused. Manual verification recommended.");
        System.out.println("   They are generated from enum values but not found as static calls.");
        System.out.println(LINE_SEPARATOR.repeat(LINE_SEPARATOR_LENGTH));

        Set<String> redundantKeys = new TreeSet<>(generatedDynamicKeys);
        redundantKeys.removeAll(staticKeysFromCode);
        redundantKeys.removeAll(annotationKeysFromCode);

        if (redundantKeys.isEmpty()) {
            System.out.println("✅ No redundant keys - all generated keys are used in code");
            return;
        }

        System.out.println("Total redundant keys: " + redundantKeys.size());
        System.out.println("─".repeat(LINE_SEPARATOR_LENGTH));

        Map<String, List<String>> groupedByPrefix = redundantKeys.stream()
                .collect(Collectors.groupingBy(
                        key -> {
                            int lastDot = key.lastIndexOf('.');
                            return lastDot > 0 ? key.substring(0, lastDot) : key;
                        },
                        TreeMap::new,
                        Collectors.toList()
                ));

        for (Map.Entry<String, List<String>> entry : groupedByPrefix.entrySet()) {
            System.out.println("\n  📁 " + entry.getKey() + ".*");
            for (String key : entry.getValue()) {
                String value = key.substring(key.lastIndexOf('.') + 1);
                System.out.println("      └── " + value);
            }
        }

        System.out.println("\n💡 Recommendation:");
        System.out.println("   • If these enum values are used dynamically, keep them");
        System.out.println("   • If they are never used, consider removing from properties");
    }

    @Test
    @Order(5)
    @DisplayName("5. Missing keys (in code but NOT in properties)")
    void reportMissingKeys() {
        System.out.println("\n" + LINE_SEPARATOR.repeat(LINE_SEPARATOR_LENGTH));
        System.out.println("❌ MISSING KEYS (in code but NOT in properties)");
        System.out.println(LINE_SEPARATOR.repeat(LINE_SEPARATOR_LENGTH));
        System.out.println("   Note: Keys ending with '.' are excluded (they are prefixes).");
        System.out.println(LINE_SEPARATOR.repeat(LINE_SEPARATOR_LENGTH));

        Set<String> validCodeKeys = getValidKeysFromCode();
        boolean hasMissing = false;

        for (String filename : propertyFiles) {
            Set<String> fileKeys = keysByFile.get(filename);
            Set<String> missing = new TreeSet<>(validCodeKeys);
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
            System.out.println("✅ No missing keys - all valid code keys are in properties");
        }
    }

    @Test
    @Order(6)
    @DisplayName("6. Orphaned keys (in properties but NOT in any code)")
    void reportOrphanedKeys() {
        System.out.println("\n" + LINE_SEPARATOR.repeat(LINE_SEPARATOR_LENGTH));
        System.out.println("👻 ORPHANED KEYS (in properties but NOT in any code - static or generated)");
        System.out.println(LINE_SEPARATOR.repeat(LINE_SEPARATOR_LENGTH));
        System.out.println("   These keys are in properties files but never referenced in code.");
        System.out.println("   They can be safely removed.");
        System.out.println(LINE_SEPARATOR.repeat(LINE_SEPARATOR_LENGTH));

        Set<String> validCodeKeys = getValidKeysFromCode();
        boolean hasOrphaned = false;

        for (String filename : propertyFiles) {
            Set<String> fileKeys = keysByFile.get(filename);
            Set<String> orphaned = new TreeSet<>(fileKeys);
            orphaned.removeAll(validCodeKeys);

            if (!orphaned.isEmpty()) {
                hasOrphaned = true;
                System.out.println("\n📁 " + filename);
                System.out.println("   Orphaned: " + orphaned.size() + " keys (can be removed)");
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
            System.out.println("✅ No orphaned keys - all property keys are referenced in code");
        }
    }

    @Test
    @Order(7)
    @DisplayName("7. Dynamic key constructions (manual check required)")
    void reportDynamicKeyWarnings() {
        System.out.println("\n" + LINE_SEPARATOR.repeat(LINE_SEPARATOR_LENGTH));
        System.out.println("🔧 DYNAMIC KEY CONSTRUCTIONS (Manual Check Required)");
        System.out.println(LINE_SEPARATOR.repeat(LINE_SEPARATOR_LENGTH));
        System.out.println("   These are keys built with concatenation - verify they are in properties.");
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
    @Order(8)
    @DisplayName("8. Invalid placeholders")
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
    @Order(9)
    @DisplayName("9. Export all keys to file")
    void exportAllKeysToFile() throws IOException {
        Set<String> validCodeKeys = getValidKeysFromCode();
        Set<String> allCodeKeys = getAllKeysFromCode();

        Set<String> redundantKeys = new TreeSet<>(generatedDynamicKeys);
        redundantKeys.removeAll(staticKeysFromCode);
        redundantKeys.removeAll(annotationKeysFromCode);

        Path outputDir = Paths.get("target/test-output");

        // =========================================================================
        // CLEANING OLD REPORT FILES
        // =========================================================================
        if (Files.exists(outputDir)) {
            System.out.println("\n🧹 Cleaning old report files...");
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(outputDir, "*.txt")) {
                int deletedCount = 0;
                for (Path file : stream) {
                    Files.delete(file);
                    deletedCount++;
                }
                System.out.println("   Deleted " + deletedCount + " old report files");
            } catch (IOException e) {
                System.out.println("   ⚠️ Failed to clean old files: " + e.getMessage());
            }
        }

        Files.createDirectories(outputDir);
        System.out.println("📁 Output directory: " + outputDir.toAbsolutePath());

        // =========================================================================
        // GENERATING REPORT FILES
        // =========================================================================

        // All valid keys
        Path outputFile = outputDir.resolve("all-message-keys.txt");
        Files.write(outputFile, validCodeKeys, StandardCharsets.UTF_8);
        System.out.println("✅ Exported " + validCodeKeys.size() + " valid keys to: all-message-keys.txt");

        // Static keys
        Path staticKeysFile = outputDir.resolve("static-keys-from-code.txt");
        Set<String> staticOnly = new TreeSet<>();
        staticOnly.addAll(staticKeysFromCode);
        staticOnly.addAll(annotationKeysFromCode);
        Files.write(staticKeysFile, staticOnly, StandardCharsets.UTF_8);
        System.out.println("✅ Exported " + staticOnly.size() + " static keys to: static-keys-from-code.txt");

        // Dynamic keys
        Path dynamicKeysFile = outputDir.resolve("generated-dynamic-keys.txt");
        Files.write(dynamicKeysFile, generatedDynamicKeys, StandardCharsets.UTF_8);
        System.out.println("✅ Exported " + generatedDynamicKeys.size() + " dynamic keys to: generated-dynamic-keys.txt");

        // Redundant keys
        if (!redundantKeys.isEmpty()) {
            Path redundantFile = outputDir.resolve("redundant-keys.txt");
            Files.write(redundantFile, redundantKeys, StandardCharsets.UTF_8);
            System.out.println("⚠️ Exported " + redundantKeys.size() + " redundant keys to: redundant-keys.txt");
        }

        // Prefix keys (excluded)
        long prefixKeysCount = allCodeKeys.stream().filter(k -> k.endsWith(".")).count();
        if (prefixKeysCount > 0) {
            Set<String> prefixKeys = allCodeKeys.stream()
                    .filter(k -> k.endsWith("."))
                    .collect(Collectors.toCollection(TreeSet::new));
            Path prefixFile = outputDir.resolve("prefix-keys-excluded.txt");
            Files.write(prefixFile, prefixKeys, StandardCharsets.UTF_8);
            System.out.println("❌ Exported " + prefixKeysCount + " prefix keys (excluded) to: prefix-keys-excluded.txt");
        }

        // Missing и Orphaned для каждого properties файла
        for (String filename : propertyFiles) {
            Set<String> fileKeys = keysByFile.get(filename);

            // Missing keys
            Set<String> missing = new TreeSet<>(validCodeKeys);
            missing.removeAll(fileKeys);
            if (!missing.isEmpty()) {
                Path missingFile = outputDir.resolve("missing-" + filename.replace(".properties", ".txt"));
                Files.write(missingFile, missing, StandardCharsets.UTF_8);
                System.out.println("❌ Missing " + missing.size() + " keys in " + filename + " → " + missingFile.getFileName());
            }

            // Orphaned keys
            Set<String> orphaned = new TreeSet<>(fileKeys);
            orphaned.removeAll(validCodeKeys);
            if (!orphaned.isEmpty()) {
                Path orphanedFile = outputDir.resolve("orphaned-" + filename.replace(".properties", ".txt"));
                Files.write(orphanedFile, orphaned, StandardCharsets.UTF_8);
                System.out.println("👻 Orphaned " + orphaned.size() + " keys in " + filename + " → " + orphanedFile.getFileName());
            }
        }

        // =========================================================================
        // SUMMARY OF GENERATED REPORT FILES
        // =========================================================================
        System.out.println("\n📋 Generated report files:");
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(outputDir, "*.txt")) {
            for (Path file : stream) {
                long size = Files.size(file);
                String sizeStr = size < 1024 ? size + " B" : (size / 1024) + " KB";
                System.out.println("   • " + file.getFileName() + " (" + sizeStr + ")");
            }
        }
    }
}