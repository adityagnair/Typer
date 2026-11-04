package typer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

/** Types text into the currently focused application using keyboard events. */
public final class TextTyperApp {
    private final JFrame frame = new JFrame("Text Typer — quick panel");
    private final JTextArea text = new JTextArea(8, 38);
    private final JSpinner interval = new JSpinner(new SpinnerNumberModel(35, 0, 1000, 5));
    private final JSpinner tabWidth = new JSpinner(new SpinnerNumberModel(4, 1, 16, 1));
    private final JSpinner countdownSetting = new JSpinner(new SpinnerNumberModel(4, 0, 30, 1));
    private final JCheckBox editorAutoIndent = new JCheckBox("Let editor auto-indent", false);
    private final JLabel status = new JLabel("Enter text, then start the countdown.");
    private final JButton start = new JButton("Start typing");
    private final JButton cancel = new JButton("Cancel");
    private JWindow stopWindow;
    private Robot robot;
    private Timer timer;
    private String pendingText;
    private int position;
    private int countdown;

    private TextTyperApp() {
        try {
            robot = new Robot();
        } catch (AWTException e) {
            JOptionPane.showMessageDialog(null, "Could not create keyboard controller: " + e.getMessage(),
                    "Text Typer", JOptionPane.ERROR_MESSAGE);
            return;
        }
        buildWindow();
    }

    private void buildWindow() {
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(new EmptyBorder(10, 10, 10, 10));
        JLabel heading = new JLabel("Quick Type");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 15f));
        content.add(heading, BorderLayout.NORTH);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        text.setTabSize((Integer) tabWidth.getValue());
        text.setToolTipText("Paste or enter text here. Leading spaces and line breaks are typed as key events.");
        content.add(new JScrollPane(text), BorderLayout.CENTER);

        JPanel controls = new JPanel(new GridLayout(0, 2, 6, 6));
        controls.add(new JLabel("Speed (ms/character)"));
        controls.add(interval);
        controls.add(new JLabel("Tab width (spaces)"));
        controls.add(tabWidth);
        controls.add(new JLabel("Start countdown (seconds)"));
        controls.add(countdownSetting);
        controls.add(editorAutoIndent);
        controls.add(new JLabel("(skip input indentation after each newline)"));
        controls.add(start);
        controls.add(cancel);
        JPanel bottom = new JPanel(new BorderLayout(4, 6));
        bottom.add(controls, BorderLayout.CENTER);
        bottom.add(status, BorderLayout.SOUTH);
        content.add(bottom, BorderLayout.SOUTH);

        tabWidth.addChangeListener(e -> text.setTabSize((Integer) tabWidth.getValue()));
        start.addActionListener(e -> begin());
        cancel.addActionListener(e -> stop("Cancelled."));
        cancel.setEnabled(false);
        frame.setContentPane(content);
        frame.pack();
        frame.setResizable(false);
        frame.setAlwaysOnTop(false);
        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration();
        Rectangle bounds = gc.getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
        frame.setLocation(bounds.x + bounds.width - frame.getWidth() - insets.right - 20,
                bounds.y + bounds.height - frame.getHeight() - insets.bottom - 50);
        stopWindow = new JWindow(frame);
        stopWindow.setFocusableWindowState(false);
        stopWindow.setAlwaysOnTop(true);
        JButton stopTyping = new JButton("STOP TYPING");
        stopTyping.setFocusable(false);
        stopTyping.setToolTipText("Stop sending keyboard events immediately");
        stopTyping.addActionListener(e -> stop("Stopped."));
        stopWindow.add(stopTyping);
        stopWindow.pack();
        frame.setVisible(true);
    }

    private void begin() {
        if (text.getText().isEmpty()) {
            status.setText("Enter some text first.");
            return;
        }
        // Normalize line endings, and turn tabs into literal spaces so indentation
        // is independent of the destination editor's tab-stop setting.
        pendingText = text.getText().replace("\r\n", "\n").replace('\r', '\n')
                .replace("\t", " ".repeat((Integer) tabWidth.getValue()));
        String unsupported = firstUnsupported(pendingText);
        if (unsupported != null) {
            status.setText("Cannot simulate this character with standard keyboard events: " + unsupported);
            return;
        }

        position = 0;
        countdown = (Integer) countdownSetting.getValue();
        start.setEnabled(false);
        cancel.setEnabled(true);
        if (countdown == 0) {
            startTyping();
            return;
        }
        status.setText("Switch to the destination and focus its text field. Typing starts in " + countdown + " seconds…");
        timer = new Timer(1000, e -> {
            countdown--;
            if (countdown > 0) {
                status.setText("Switch to the destination and focus its text field. Typing starts in " + countdown + " seconds…");
            } else {
                startTyping();
            }
        });
        timer.start();
    }

    private void startTyping() {
        if (timer != null) timer.stop();
        int delay = (Integer) interval.getValue();
        timer = new Timer(delay, tick -> typeNext());
        timer.setInitialDelay(0);
        timer.start();
        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration();
        Rectangle bounds = gc.getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
        stopWindow.setLocation(bounds.x + bounds.width - stopWindow.getWidth() - insets.right - 20,
                bounds.y + bounds.height - stopWindow.getHeight() - insets.bottom - 20);
        stopWindow.setVisible(true);
        status.setText("Typing… Use Cancel to stop.");
    }

    private void typeNext() {
        // When the destination editor auto-indents, leave leading whitespace on
        // subsequent lines to the editor. Never use Backspace to guess/remove it:
        // editors may treat one Backspace as a whole indentation level.
        if (editorAutoIndent.isSelected() && position > 0 && pendingText.charAt(position - 1) == '\n') {
            while (position < pendingText.length() && pendingText.charAt(position) == ' ') position++;
        }
        if (position >= pendingText.length()) {
            stop("Finished typing.");
            return;
        }
        char ch = pendingText.charAt(position++);
        if (ch == '\r') return;
        if (ch == '\n') {
            press(KeyEvent.VK_ENTER, false);
        } else if (ch == '\t') {
            press(KeyEvent.VK_TAB, false);
        } else {
            KeyStroke stroke = toKeyStroke(ch);
            press(stroke.keyCode, stroke.shift);
        }
        if (position >= pendingText.length()) {
            // Let the final key event be delivered before resetting the controls.
            SwingUtilities.invokeLater(() -> stop("Finished typing."));
        }
    }

    private void press(int keyCode, boolean shift) {
        if (shift) robot.keyPress(KeyEvent.VK_SHIFT);
        robot.keyPress(keyCode);
        robot.keyRelease(keyCode);
        if (shift) robot.keyRelease(KeyEvent.VK_SHIFT);
    }

    private void stop(String message) {
        if (timer != null) timer.stop();
        timer = null;
        if (stopWindow != null) stopWindow.setVisible(false);
        start.setEnabled(true);
        cancel.setEnabled(false);
        status.setText(message);
    }

    private static String firstUnsupported(String value) {
        for (char ch : value.toCharArray()) {
            if (ch == '\r' || ch == '\n' || ch == '\t' || toKeyStroke(ch) != null) continue;
            return "'" + ch + "'";
        }
        return null;
    }

    private static final class KeyStroke {
        final int keyCode;
        final boolean shift;
        KeyStroke(int keyCode, boolean shift) { this.keyCode = keyCode; this.shift = shift; }
    }

    private static KeyStroke toKeyStroke(char ch) {
        if (ch >= 'a' && ch <= 'z') return new KeyStroke(KeyEvent.VK_A + ch - 'a', false);
        if (ch >= 'A' && ch <= 'Z') return new KeyStroke(KeyEvent.VK_A + ch - 'A', true);
        if (ch >= '0' && ch <= '9') return new KeyStroke(KeyEvent.VK_0 + ch - '0', false);
        if (ch == ' ') return new KeyStroke(KeyEvent.VK_SPACE, false);
        Map<Character, KeyStroke> symbols = SYMBOLS;
        return symbols.get(ch);
    }

    private static final Map<Character, KeyStroke> SYMBOLS = createSymbols();

    private static Map<Character, KeyStroke> createSymbols() {
        Map<Character, KeyStroke> map = new HashMap<>();
        add(map, '-', KeyEvent.VK_MINUS, false); add(map, '_', KeyEvent.VK_MINUS, true);
        add(map, '=', KeyEvent.VK_EQUALS, false); add(map, '+', KeyEvent.VK_EQUALS, true);
        add(map, '[', KeyEvent.VK_OPEN_BRACKET, false); add(map, '{', KeyEvent.VK_OPEN_BRACKET, true);
        add(map, ']', KeyEvent.VK_CLOSE_BRACKET, false); add(map, '}', KeyEvent.VK_CLOSE_BRACKET, true);
        add(map, '\\', KeyEvent.VK_BACK_SLASH, false); add(map, '|', KeyEvent.VK_BACK_SLASH, true);
        add(map, ';', KeyEvent.VK_SEMICOLON, false); add(map, ':', KeyEvent.VK_SEMICOLON, true);
        add(map, '\'', KeyEvent.VK_QUOTE, false); add(map, '"', KeyEvent.VK_QUOTE, true);
        add(map, ',', KeyEvent.VK_COMMA, false); add(map, '<', KeyEvent.VK_COMMA, true);
        add(map, '.', KeyEvent.VK_PERIOD, false); add(map, '>', KeyEvent.VK_PERIOD, true);
        add(map, '/', KeyEvent.VK_SLASH, false); add(map, '?', KeyEvent.VK_SLASH, true);
        add(map, '`', KeyEvent.VK_BACK_QUOTE, false); add(map, '~', KeyEvent.VK_BACK_QUOTE, true);
        int[] numberKeys = {KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4, KeyEvent.VK_5,
                KeyEvent.VK_6, KeyEvent.VK_7, KeyEvent.VK_8, KeyEvent.VK_9, KeyEvent.VK_0};
        char[] shifted = {'!', '@', '#', '$', '%', '^', '&', '*', '(', ')'};
        for (int i = 0; i < shifted.length; i++) add(map, shifted[i], numberKeys[i], true);
        return Map.copyOf(map);
    }

    private static void add(Map<Character, KeyStroke> map, char ch, int keyCode, boolean shift) {
        map.put(ch, new KeyStroke(keyCode, shift));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(TextTyperApp::new);
    }
}
