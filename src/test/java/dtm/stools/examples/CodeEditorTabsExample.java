package dtm.stools.examples;

import com.formdev.flatlaf.FlatDarkLaf;
import dtm.stools.component.panels.editor.code.CodeEditor;
import dtm.stools.component.panels.editor.code.gutter.CodeEditorGutter;
import dtm.stools.component.panels.tab.TabConfig;
import dtm.stools.component.panels.tab.TabStyle;
import dtm.stools.component.panels.tab.TabbedPanel;
import dtm.stools.component.panels.editor.code.prototype.folding.FoldRule;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

public class CodeEditorTabsExample {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(CodeEditorTabsExample::launch);
    }

    private static void launch() {
        FlatDarkLaf.setup();

        JFrame frame = new JFrame("CodeEditor + Tabs example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 720);
        frame.setLocationRelativeTo(null);

        TabbedPanel tabs = buildTabbedPanel();
        tabs.addTab(new TabConfig("LongNavigation.java", "LongNavigation.java",
                buildEditor(buildLongNavigationSample(), "java")));
        tabs.addTab(new TabConfig("project.assets", "project.assets",
                buildEditor(SAMPLE_JSON, "json")));
        tabs.addTab(new TabConfig("Program.cs", "Program.cs",
                buildEditor(SAMPLE_CS, "cs")));
        tabs.addTab(new TabConfig("ConsoleApp1.csproj", "ConsoleApp1.csproj",
                buildEditor(SAMPLE_CSPROJ, "xml")));

        frame.setContentPane(tabs);
        frame.setVisible(true);
    }

    private static TabbedPanel buildTabbedPanel() {
        TabbedPanel tabs = new TabbedPanel(JTabbedPane.TOP);
        tabs.setScrollableTabsEnabled(true);
        tabs.setCloseButtonsVisible(true);

        TabStyle style = tabs.getTabStyle();
        style.setCloseButtonIconResource("/drawables/close.png");
        style.setCloseButtonIconSize(12);
        return tabs;
    }

    private static CodeEditor buildEditor(String text, String kind) {
        CodeEditor editor = new CodeEditor(text);
        editor.getTextArea().setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));

        CodeEditorGutter gutter = editor.getGutter();
        gutter.enableBreakpoint(true);
        gutter.enableBookmark(true);
        gutter.setPreviewOnHoverEnabled(true);

        editor.setFocusBorderEnabled(false);
        editor.setHighlightCurrentLine(true);
        editor.setCurrentLineColor(new Color(0x2A2F3A));

        editor.setFoldingEnabled(true);
        editor.getTextArea().setFoldPlaceholderWithSeparators(true);
        if ("json".equals(kind) || "cs".equals(kind) || "java".equals(kind)) {
            editor.addFoldRule(FoldRule.pair('{', '}'));
            editor.addFoldRule(FoldRule.pair('[', ']'));
        }
        if ("xml".equals(kind)) {
            editor.addFoldRule(FoldRule.xmlTags());
        }

        editor.setPreferredSize(new Dimension(900, 600));
        return editor;
    }

    private static String buildLongNavigationSample() {
        StringBuilder code = new StringBuilder("""
                package example.navigation;

                /**
                 * Use as setas para percorrer este arquivo.
                 * O scroll deve permanecer parado enquanto o caret estiver dentro da area visivel
                 * e deve acompanhar o caret somente quando ele alcancar uma das margens.
                 */
                public class LongNavigation {
                    public static void main(String[] args) {
                """);

        for (int i = 1; i <= 180; i++) {
            code.append("        System.out.println(\"Linha de teste ")
                    .append(String.format("%03d", i))
                    .append(": navegue verticalmente e horizontalmente para validar o comportamento do caret")
                    .append(i % 12 == 0
                            ? "; esta linha foi deixada propositalmente muito mais longa para tambem exercitar a margem horizontal do editor"
                            : "")
                    .append("\");\n");
        }

        return code.append("""
                    }
                }
                """).toString();
    }

    private static final String SAMPLE_JSON = """
            {
              "version": 3,
              "targets": {
                "net10.0": {}
              },
              "libraries": {},
              "projectFileDependencyGroups": {
                "net10.0": []
              },
              "packageFolders": {
                "C:\\\\Users\\\\danie\\\\.nuget\\\\packages\\\\": {}
              },
              "project": {
                "version": "1.0.0",
                "restore": {
                  "projectName": "ConsoleApp1",
                  "projectStyle": "PackageReference"
                }
              }
            }
            """;

    private static final String SAMPLE_CS = """
            using System;

            namespace ConsoleApp1 {
                public class Program {
                    public static void Main(string[] args) {
                        Console.WriteLine("Hello, World!");
                        for (int i = 0; i < 5; i++) {
                            Console.WriteLine($"i = {i}");
                        }
                    }
                }
            }
            """;

    private static final String SAMPLE_CSPROJ = """
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <OutputType>Exe</OutputType>
                <TargetFramework>net10.0</TargetFramework>
                <Nullable>enable</Nullable>
              </PropertyGroup>
            </Project>
            """;
}
