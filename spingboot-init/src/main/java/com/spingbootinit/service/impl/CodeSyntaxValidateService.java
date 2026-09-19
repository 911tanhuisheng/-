package com.spingbootinit.service.impl;

import com.spingbootinit.model.vo.codevalidate.CodeSyntaxDiagnosticVO;
import com.spingbootinit.model.vo.codevalidate.CodeSyntaxValidateResponseVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 在服务器上调用 g++ / javac / python 做<strong>语法级</strong>检查，供前端 Monaco 标红。
 * 若本机未安装对应工具，返回空 diagnostics 并在 notice 中说明。
 */
@Service
@Slf4j
public class CodeSyntaxValidateService {

    private static final int MAX_STDERR = 96_000;
    private static final int TIMEOUT_SEC = 12;

    /** 工作目录下短文件名 a.cpp，stderr 里可能是相对路径或绝对路径 */
    private static final Pattern GCC_LINE = Pattern.compile(
            "^(?<file>\\S+\\.(?:cpp|cc|cxx|c\\+\\+)):(?<line>\\d+):(?<col>\\d+):\\s*(?:fatal\\s+)?(?<sev>error|warning):\\s*(?<msg>.+)$",
            Pattern.CASE_INSENSITIVE);

    /**
     * javac 一行常见形态：
     * Main.java:10: error: ...
     * Main.java:10:15: error: ...
     * C:\\Temp\\xxx\\Main.java:10: error: ...
     * 若仅匹配行首 \\S+\\.java，遇到 Windows 路径里含空格或格式差异时会丢诊断。
     */
    /** 中文版 JDK 可能输出「错误」「警告」而非 error/warning */
    private static final Pattern JAVAC_LINE_IN_LINE = Pattern.compile(
            "(?m)(?:.*[/\\\\])?(?<file>[a-zA-Z_][a-zA-Z0-9_$]*\\.java):(?<line>\\d+):(?:(?<col>\\d+):)?\\s*(?<sev>error|warning|错误|警告):\\s*(?<msg>.+)");

    private static final Pattern PY_FILE_LINE = Pattern.compile("^File\\s+\"[^\"]+\",\\s*line\\s+(?<line>\\d+)", Pattern.MULTILINE);

    public CodeSyntaxValidateResponseVO validate(String language, String code) {
        if (!StringUtils.hasText(code)) {
            return CodeSyntaxValidateResponseVO.builder().diagnostics(List.of()).build();
        }
        String lang = language.trim().toLowerCase(Locale.ROOT);
        try {
            return switch (lang) {
                case "cpp" -> runCpp(code);
                case "java" -> runJava(code);
                case "python" -> runPython(code);
                default -> CodeSyntaxValidateResponseVO.builder()
                        .diagnostics(List.of())
                        .notice("不支持的语言: " + lang)
                        .build();
            };
        } catch (Exception e) {
            log.debug("syntax validate failed: {}", e.getMessage());
            return CodeSyntaxValidateResponseVO.builder()
                    .diagnostics(List.of())
                    .notice("语法检查异常: " + e.getMessage())
                    .build();
        }
    }

    private CodeSyntaxValidateResponseVO runCpp(String code) throws IOException {
        Path dir = Files.createTempDirectory("myoj-cpp-");
        try {
            Path src = dir.resolve("a.cpp");
            Files.writeString(src, code, StandardCharsets.UTF_8);
            List<String> cmd = List.of("g++", "-fsyntax-only", "-std=c++17", "-Wall", "a.cpp");
            ProcResult pr = runProcess(dir, cmd);
            if (pr.exitMissingCommand()) {
                return noticeOnly("未检测到 g++，服务器无法为 C++ 提供语法标红（请安装 MinGW/gcc 并加入 PATH）");
            }
            List<CodeSyntaxDiagnosticVO> list = parseGccLike(pr.stderr(), "a.cpp");
            return CodeSyntaxValidateResponseVO.builder().diagnostics(list).build();
        } finally {
            deleteDir(dir);
        }
    }

    private CodeSyntaxValidateResponseVO runJava(String code) throws IOException {
        String base = detectJavaClassName(code);
        Path dir = Files.createTempDirectory("myoj-java-");
        try {
            Path src = dir.resolve(base + ".java");
            Files.writeString(src, code, StandardCharsets.UTF_8);
            String javacExe = resolveJavacExecutable();
            List<String> cmd = new ArrayList<>();
            cmd.add(javacExe);
            cmd.add("-encoding");
            cmd.add("UTF-8");
            cmd.add("-Xlint:none");
            cmd.add(base + ".java");
            ProcResult pr = runProcess(dir, cmd);
            if (pr.exitMissingCommand()) {
                return noticeOnly(
                        "未检测到 javac（仅配置 JRE 不够）。请安装 JDK，或将 JAVA_HOME 指向 JDK，并把 %JAVA_HOME%\\bin 加入 PATH");
            }
            List<CodeSyntaxDiagnosticVO> list = parseJavac(pr.stderr());
            // javac 失败但一行都没解析出来（编码/格式差异）时，至少把首行错误带到第 1 行，避免前端完全无红线
            if (pr.exitCode() != 0 && list.isEmpty() && StringUtils.hasText(pr.stderr())) {
                String hint = abbrev(cleanCompilerMessage(firstSignificantLine(pr.stderr())), 800);
                list = new ArrayList<>(List.of(markerLine(1, 1, hint, "ERROR")));
            }
            return CodeSyntaxValidateResponseVO.builder().diagnostics(list).build();
        } finally {
            deleteDir(dir);
        }
    }

    /** 优先 JAVA_HOME\\bin\\javac(.exe)，否则依赖 PATH 里的 javac（需完整 JDK） */
    private static String resolveJavacExecutable() {
        String home = System.getenv("JAVA_HOME");
        if (StringUtils.hasText(home)) {
            Path jdkHome = Path.of(home.trim());
            Path win = jdkHome.resolve("bin").resolve("javac.exe");
            Path nix = jdkHome.resolve("bin").resolve("javac");
            if (Files.isRegularFile(win)) {
                return win.toString();
            }
            if (Files.isRegularFile(nix)) {
                return nix.toString();
            }
        }
        return "javac";
    }

    private static String firstSignificantLine(String stderr) {
        for (String raw : stderr.split("\n")) {
            String s = raw.trim();
            if (!s.isEmpty() && !s.startsWith("^")) {
                return s;
            }
        }
        return stderr.trim();
    }

    private CodeSyntaxValidateResponseVO runPython(String code) throws IOException {
        Path dir = Files.createTempDirectory("myoj-py-");
        try {
            Path src = dir.resolve("a.py");
            Files.writeString(src, code, StandardCharsets.UTF_8);
            ProcResult pr = runProcess(dir, "python", "-m", "py_compile", "a.py");
            if (pr.exitMissingCommand()) {
                pr = runProcess(dir, "python3", "-m", "py_compile", "a.py");
            }
            if (pr.exitMissingCommand()) {
                return noticeOnly("未检测到 python/python3，服务器无法为 Python 提供语法标红");
            }
            List<CodeSyntaxDiagnosticVO> list = parsePython(pr.stderr());
            return CodeSyntaxValidateResponseVO.builder().diagnostics(list).build();
        } finally {
            deleteDir(dir);
        }
    }

    private static String detectJavaClassName(String code) {
        Matcher m = Pattern.compile("public\\s+class\\s+(\\w+)").matcher(code);
        if (m.find()) {
            return m.group(1);
        }
        m = Pattern.compile("(?:^|\\n)\\s*class\\s+(\\w+)").matcher(code);
        if (m.find()) {
            return m.group(1);
        }
        return "Main";
    }

    private record ProcResult(int exitCode, String stderr, boolean missingCommand) {
        boolean exitMissingCommand() {
            return missingCommand;
        }
    }

    private ProcResult runProcess(Path workDir, String... command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(workDir.toFile());
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String out = readLimited(p.getInputStream());
            boolean finished = p.waitFor(TIMEOUT_SEC, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                return new ProcResult(-1, "语法检查超时（>" + TIMEOUT_SEC + "s）", false);
            }
            int code = p.exitValue();
            return new ProcResult(code, out, false);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ProcResult(-1, "语法检查已中断", false);
        } catch (IOException e) {
            log.debug("spawn {}: {}", String.join(" ", command), e.getMessage());
            return new ProcResult(-1, "", true);
        }
    }

    private ProcResult runProcess(Path workDir, List<String> command) {
        return runProcess(workDir, command.toArray(new String[0]));
    }

    /**
     * 编译器 stderr 编码：Windows 中文版 javac/g++ 多为 GBK；
     * JDK 17+ 默认 Charset 常为 UTF-8，用 defaultCharset 读会中文乱码。
     */
    private static Charset charsetForCompilerOutput() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            return Charset.forName("GBK");
        }
        return StandardCharsets.UTF_8;
    }

    private static String readLimited(java.io.InputStream in) throws IOException {
        byte[] raw = in.readNBytes(MAX_STDERR);
        Charset primary = charsetForCompilerOutput();
        String text = new String(raw, primary);
        // 若仍出现替换符，再试 UTF-8（少数环境已 chcp 65001）
        if (text.indexOf('\uFFFD') >= 0) {
            String utf8 = new String(raw, StandardCharsets.UTF_8);
            if (utf8.indexOf('\uFFFD') < 0) {
                return utf8;
            }
        }
        return text;
    }

    private static List<CodeSyntaxDiagnosticVO> parseGccLike(String stderr, String shortName) {
        List<CodeSyntaxDiagnosticVO> out = new ArrayList<>();
        for (String rawLine : stderr.split("\n")) {
            String line = rawLine.trim();
            Matcher m = GCC_LINE.matcher(line);
            if (!m.matches()) {
                continue;
            }
            if (!m.group("file").endsWith(shortName)) {
                continue;
            }
            int ln = parseIntSafe(m.group("line"), 1);
            int col = Math.max(1, parseIntSafe(m.group("col"), 1));
            String sev = "error".equalsIgnoreCase(m.group("sev")) ? "ERROR" : "WARNING";
            String msg = cleanCompilerMessage(m.group("msg").trim());
            out.add(markerLine(ln, col, msg, sev));
        }
        if (out.isEmpty() && (stderr.contains("error:") || stderr.contains("错误"))) {
            // 回退：整段首行作为第 1 行错误（路径含冒号导致正则不匹配时）
            String one =
                    stderr.lines()
                            .filter(l -> l.contains("error:") || l.contains("错误"))
                            .findFirst()
                            .orElse(null);
            if (one != null) {
                out.add(markerLine(1, 1, cleanCompilerMessage(one.trim()), "ERROR"));
            }
        }
        return out;
    }

    /** 从 javac stderr 提取诊断（支持行内路径、中文版「错误:」） */
    private static List<CodeSyntaxDiagnosticVO> parseJavac(String stderr) {
        List<CodeSyntaxDiagnosticVO> out = new ArrayList<>();
        Matcher m = JAVAC_LINE_IN_LINE.matcher(stderr);
        while (m.find()) {
            String file = m.group("file");
            if (file == null || !file.endsWith(".java")) {
                continue;
            }
            int ln = parseIntSafe(m.group("line"), 1);
            String colG = m.group("col");
            int col = colG != null && !colG.isEmpty() ? Math.max(1, parseIntSafe(colG.trim(), 1)) : 1;
            String sevRaw = m.group("sev");
            boolean isErr =
                    sevRaw != null
                            && (sevRaw.equalsIgnoreCase("error")
                                    || "错误".equals(sevRaw));
            String sev = isErr ? "ERROR" : "WARNING";
            String msg = cleanCompilerMessage(m.group("msg").trim());
            out.add(markerLine(ln, col, msg, sev));
        }
        return out;
    }

    /** 去掉「Main.java:1: 错误:」等前缀，只保留可读说明 */
    private static String cleanCompilerMessage(String raw) {
        if (raw == null || raw.isEmpty()) {
            return raw;
        }
        String s = raw.trim();
        Matcher tail = Pattern.compile(
                "(?:error|warning|错误|警告)\\s*[:：]\\s*(?<msg>.+)$",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
                .matcher(s);
        if (tail.find()) {
            return tail.group("msg").trim();
        }
        return s;
    }

    private static String abbrev(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    private static List<CodeSyntaxDiagnosticVO> parsePython(String stderr) {
        List<CodeSyntaxDiagnosticVO> out = new ArrayList<>();
        Matcher m = PY_FILE_LINE.matcher(stderr);
        int lastLine = -1;
        while (m.find()) {
            int ln = parseIntSafe(m.group("line"), 1);
            lastLine = ln;
        }
        if (lastLine > 0) {
            String msg = stderr.lines().filter(s -> s.contains("Error") || s.contains("error")).reduce((a, b) -> b).orElse("语法错误");
            out.add(markerLine(lastLine, 1, msg.trim(), "ERROR"));
        } else if (stderr.contains("SyntaxError") || stderr.contains("IndentationError")) {
            out.add(CodeSyntaxDiagnosticVO.builder()
                    .startLineNumber(1)
                    .startColumnNumber(1)
                    .endLineNumber(1)
                    .endColumnNumber(2)
                    .message(stderr.trim().lines().findFirst().orElse("Python 语法错误"))
                    .severity("ERROR")
                    .build());
        }
        return out;
    }

    private static CodeSyntaxDiagnosticVO markerLine(int line, int col, String message, String severity) {
        return CodeSyntaxDiagnosticVO.builder()
                .startLineNumber(line)
                .startColumnNumber(col)
                .endLineNumber(line)
                .endColumnNumber(col + 1)
                .message(message)
                .severity(severity)
                .build();
    }

    private static int parseIntSafe(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static CodeSyntaxValidateResponseVO noticeOnly(String notice) {
        return CodeSyntaxValidateResponseVO.builder().diagnostics(List.of()).notice(notice).build();
    }

    private static void deleteDir(Path dir) {
        try {
            if (!Files.exists(dir)) {
                return;
            }
            try (var walk = Files.walk(dir)) {
                walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) {
                    }
                });
            }
        } catch (IOException ignored) {
        }
    }
}
