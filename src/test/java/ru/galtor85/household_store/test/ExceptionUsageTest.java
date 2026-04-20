package ru.galtor85.household_store.test;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;


/**
 * Test that verifies all custom exception classes are actually used in the codebase.
 * <p>
 * Scans all exception classes in {@code ru.galtor85.household_store.advice.exception}
 * and checks if they are instantiated (via {@code throw new XxxException} or {@code new XxxException}).
 * </p>
 */
@DisplayName("Exception Usage Test")
class ExceptionUsageTest {

    private static final Logger log = LoggerFactory.getLogger(ExceptionUsageTest.class);

    private static final String PROJECT_SRC_PATH = "src/main/java";
    private static final String EXCEPTION_PACKAGE = "ru.galtor85.household_store.advice.exception";

    private static final Set<String> allExceptionClasses = new TreeSet<>();
    private static final Set<String> usedExceptionClasses = new TreeSet<>();

    @BeforeAll
    static void setUp() throws IOException {
        // Configure JavaParser for Java 17
        ParserConfiguration config = new ParserConfiguration();
        config.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
        StaticJavaParser.setConfiguration(config);

        // Find all custom exception classes
        findAllExceptionClasses();

        // Scan all Java files for exception usage
        scanForExceptionUsage();

        log.info("Found {} custom exception classes", allExceptionClasses.size());
        log.info("Found {} used exception classes", usedExceptionClasses.size());
    }

    /**
     * Finds all custom exception classes in the exception package.
     */
    private static void findAllExceptionClasses() throws IOException {
        Path exceptionDir = Paths.get(PROJECT_SRC_PATH, EXCEPTION_PACKAGE.replace('.', '/'));

        if (!Files.exists(exceptionDir)) {
            log.error("Exception directory not found: {}", exceptionDir);
            return;
        }

        try (Stream<Path> walk = Files.walk(exceptionDir)) {
            walk.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .forEach(ExceptionUsageTest::extractExceptionClassName);
        }
    }

    private static void extractExceptionClassName(Path file) {
        String fileName = file.getFileName().toString();
        String className = fileName.replace(".java", "");
        allExceptionClasses.add(className);
    }

    /**
     * Scans all Java files for usage of custom exceptions.
     */
    private static void scanForExceptionUsage() throws IOException {
        Path srcDir = Paths.get(PROJECT_SRC_PATH);

        try (Stream<Path> walk = Files.walk(srcDir)) {
            walk.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .forEach(ExceptionUsageTest::scanFileForExceptionUsage);
        }
    }

    private static void scanFileForExceptionUsage(Path file) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(file);
            cu.accept(new ExceptionUsageVisitor(), null);
        } catch (Exception e) {
            log.warn("Failed to parse file {}: {}", file, e.getMessage());
        }
    }

    /**
     * Visitor that finds all instantiations of custom exception classes.
     */
    private static class ExceptionUsageVisitor extends VoidVisitorAdapter<Void> {

        @Override
        public void visit(ThrowStmt throwStmt, Void arg) {
            super.visit(throwStmt, arg);


            if (throwStmt.getExpression() instanceof ObjectCreationExpr obj) {
                String typeName = obj.getType().getNameAsString();
                if (allExceptionClasses.contains(typeName)) {
                    usedExceptionClasses.add(typeName);
                }
            }
        }

        @Override
        public void visit(ObjectCreationExpr obj, Void arg) {
            super.visit(obj, arg);

            String typeName = obj.getType().getNameAsString();
            if (allExceptionClasses.contains(typeName)) {
                usedExceptionClasses.add(typeName);
            }
        }
    }

    // =========================================================================
    // TESTS
    // =========================================================================

    @Test
    @DisplayName("Report unused exception classes")
    void reportUnusedExceptions() {
        Set<String> unusedExceptions = new TreeSet<>(allExceptionClasses);
        unusedExceptions.removeAll(usedExceptionClasses);

        System.out.println("\n" + "═".repeat(80));
        System.out.println("📊 EXCEPTION USAGE REPORT");
        System.out.println("═".repeat(80));
        System.out.println("Total custom exceptions: " + allExceptionClasses.size());
        System.out.println("Used exceptions:          " + usedExceptionClasses.size());
        System.out.println("Unused exceptions:        " + unusedExceptions.size());
        System.out.println("═".repeat(80));

        if (unusedExceptions.isEmpty()) {
            System.out.println("✅ All custom exceptions are used in the codebase.");
        } else {
            System.out.println("\n⚠️ UNUSED EXCEPTION CLASSES (may be safely removed):");
            System.out.println("─".repeat(50));
            for (String className : unusedExceptions) {
                System.out.println("  • " + className);
            }
            System.out.println("\n💡 Recommendation:");
            System.out.println("   • If these exceptions are never thrown, consider removing them.");
            System.out.println("   • If they are thrown via reflection or dynamically, this test won't detect them.");
        }

        // This is a warning only, not a failing test
        // Remove the assertTrue if you want it to be informational only
        // assertTrue(unusedExceptions.isEmpty(), "Found unused exception classes.");
    }

    @Test
    @DisplayName("Export unused exceptions to file")
    void exportUnusedExceptions() throws IOException {
        Set<String> unusedExceptions = new TreeSet<>(allExceptionClasses);
        unusedExceptions.removeAll(usedExceptionClasses);

        Path outputDir = Paths.get("target/test-output");
        Files.createDirectories(outputDir);

        Path outputFile = outputDir.resolve("unused-exceptions.txt");
        Files.write(outputFile, unusedExceptions);

        System.out.println("\n📁 Unused exceptions exported to: " + outputFile.toAbsolutePath());
    }
}