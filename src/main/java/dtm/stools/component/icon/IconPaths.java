package dtm.stools.component.icon;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.util.Map;
import java.util.stream.Collectors;

public final class IconPaths {
    private static final Map<Character, Path2D> PATHS = Map.ofEntries(
            Map.entry('A', "0,8 3,0 6,8|1,5 5,5"),
            Map.entry('B', "0,8 0,0 4,0 6,1 6,3 4,4 0,4|4,4 6,5 6,7 4,8 0,8"),
            Map.entry('C', "6,1 4,0 2,0 0,2 0,6 2,8 4,8 6,7"),
            Map.entry('D', "0,8 0,0 3,0 6,2 6,6 3,8 0,8"),
            Map.entry('E', "6,0 0,0 0,8 6,8|0,4 5,4"),
            Map.entry('F', "6,0 0,0 0,8|0,4 5,4"),
            Map.entry('G', "6,1 4,0 2,0 0,2 0,6 2,8 6,8 6,4 3,4"),
            Map.entry('H', "0,0 0,8|6,0 6,8|0,4 6,4"),
            Map.entry('I', "1,0 5,0|3,0 3,8|1,8 5,8"),
            Map.entry('J', "1,0 6,0 6,6 4,8 2,8 0,6"),
            Map.entry('K', "0,0 0,8|6,0 0,5|2,3.5 6,8"),
            Map.entry('L', "0,0 0,8 6,8"),
            Map.entry('M', "0,8 0,0 3,4 6,0 6,8"),
            Map.entry('N', "0,8 0,0 6,8 6,0"),
            Map.entry('O', "2,0 4,0 6,2 6,6 4,8 2,8 0,6 0,2 2,0"),
            Map.entry('P', "0,8 0,0 4,0 6,1 6,3 4,4 0,4"),
            Map.entry('Q', "2,0 4,0 6,2 6,6 4,8 2,8 0,6 0,2 2,0|4,6 7,9"),
            Map.entry('R', "0,8 0,0 4,0 6,1 6,3 4,4 0,4|3,4 6,8"),
            Map.entry('S', "6,1 4,0 2,0 0,1 0,3 2,4 4,4 6,5 6,7 4,8 2,8 0,7"),
            Map.entry('T', "0,0 6,0|3,0 3,8"),
            Map.entry('U', "0,0 0,6 2,8 4,8 6,6 6,0"),
            Map.entry('V', "0,0 3,8 6,0"),
            Map.entry('W', "0,0 1,8 3,4 5,8 6,0"),
            Map.entry('X', "0,0 6,8|6,0 0,8"),
            Map.entry('Y', "0,0 3,4 6,0|3,4 3,8"),
            Map.entry('Z', "0,0 6,0 0,8 6,8"),
            Map.entry('a', "0,3 4,3 6,4 6,8|6,5 1,5 0,6 0,7 1,8 4,8 6,6"),
            Map.entry('b', "0,0 0,8|0,4 2,3 4,3 6,5 6,6 4,8 2,8 0,7"),
            Map.entry('c', "6,4 4,3 2,3 0,5 0,6 2,8 4,8 6,7"),
            Map.entry('d', "6,0 6,8|6,4 4,3 2,3 0,5 0,6 2,8 4,8 6,7"),
            Map.entry('f', "2,8 2,1 3,0 5,0|0,3 5,3"),
            Map.entry('i', "3,0 3,0.2|2,3 3,3 3,8|1,8 5,8"),
            Map.entry('x', "0,3 6,8|6,3 0,8"),
            Map.entry('0', "2,0 4,0 6,2 6,6 4,8 2,8 0,6 0,2 2,0"),
            Map.entry('1', "1,2 3,0 3,8|0,8 6,8"),
            Map.entry('2', "0,1 2,0 4,0 6,1 6,3 0,8 6,8"),
            Map.entry('3', "0,0 6,0 3,4 5,4 6,5 6,7 4,8 1,8 0,7"),
            Map.entry('4', "5,8 5,0 0,5 6,5"),
            Map.entry('5', "6,0 0,0 0,4 4,4 6,5 6,7 4,8 0,8"),
            Map.entry('6', "6,0 2,0 0,2 0,6 2,8 4,8 6,6 6,5 4,4 0,4"),
            Map.entry('7', "0,0 6,0 2,8"),
            Map.entry('8', "2,0 4,0 6,1 6,3 4,4 2,4 0,3 0,1 2,0|2,4 0,5 0,7 2,8 4,8 6,7 6,5 4,4"),
            Map.entry('9', "6,4 2,4 0,3 0,1 2,0 4,0 6,2 6,6 4,8 0,8"),
            Map.entry('#', "2,0 1,8|5,0 4,8|0,3 6,3|0,6 6,6"),
            Map.entry('$', "6,1 1,1 0,2 0,3 6,5 6,6 5,7 0,7|3,0 3,8"),
            Map.entry('%', "0,8 6,0|0,0 2,0 2,2 0,2 0,0|4,6 6,6 6,8 4,8 4,6"),
            Map.entry('+', "0,4 6,4|3,1 3,7"),
            Map.entry('−', "0,4 6,4"),
            Map.entry('=', "0,2 6,2|0,6 6,6"),
            Map.entry('!', "3,0 3,5|3,7.8 3,8"),
            Map.entry('?', "0,1 2,0 4,0 6,1 6,3 3,5 3,6|3,7.8 3,8"),
            Map.entry(',', "3,7 2,9"),
            Map.entry('(', "4,0 2,2 2,6 4,8"),
            Map.entry(')', "2,0 4,2 4,6 2,8"),
            Map.entry('{', "5,0 3,0 2,1 2,3 0,4 2,5 2,7 3,8 5,8"),
            Map.entry('}', "1,0 3,0 4,1 4,3 6,4 4,5 4,7 3,8 1,8"),
            Map.entry('<', "5,0 1,4 5,8"),
            Map.entry('>', "1,0 5,4 1,8"),
            Map.entry('_', "0,8 6,8"),
            Map.entry('Σ', "6,0 0,0 4,4 0,8 6,8"),
            Map.entry('π', "0,2 6,2|2,2 1,8|5,2 5,8 6,8")
    ).entrySet().stream().collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> path(entry.getValue())));

    private IconPaths() {}

    public static double width(String symbols, double size) {
        return symbols.isEmpty() ? 0 : (symbols.length() * 8 - 2) * size * 0.075;
    }

    public static void draw(Graphics2D graphics, String symbols, double x, double baseline, double size, int style) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.translate(x, baseline - size * 0.6);
            g.scale(size * 0.075, size * 0.075);
            if ((style & 2) != 0) { g.translate(1.2, 0); g.shear(-0.15, 0); }
            g.setStroke(new BasicStroke((style & 1) != 0 ? 1.3f : 0.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < symbols.length(); i++) {
                char symbol = symbols.charAt(i);
                if (symbol != ' ') g.draw(PATHS.getOrDefault(symbol, PATHS.get('?')));
                g.translate(8, 0);
            }
        } finally {
            g.dispose();
        }
    }

    private static Path2D path(String strokes) {
        Path2D path = new Path2D.Double();
        for (String stroke : strokes.split("\\|")) {
            boolean first = true;
            for (String point : stroke.split(" ")) {
                String[] coordinates = point.split(",");
                double x = Double.parseDouble(coordinates[0]), y = Double.parseDouble(coordinates[1]);
                if (first) path.moveTo(x, y); else path.lineTo(x, y);
                first = false;
            }
        }
        return path;
    }
}
