package PCG_db;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * Pokeca Battle Engine "GEMINI-ZERO"
 * 既存の枠組みを捨て、対戦体験の再現に特化したスタンドアロン・シミュレータ
 */
public class PokecaZeroEngine extends JFrame {
    // ゲーム状態
    private List<String> deck = new ArrayList<>();
    private List<String> hand = new ArrayList<>();
    private List<String> side = new ArrayList<>();
    private Map<String, Integer> field = new LinkedHashMap<>(); // ポケモン名 -> ダメージ
    private int turn = 1;
    private String logBuffer = "";

    // UIコンポーネント
    private JTextArea logDisplay;
    private JPanel handPanel, fieldPanel, sidePanel;
    private JLabel statusLabel;

    public PokecaZeroEngine() {
        setTitle("Pokeca Battle Engine [GEMINI-ZERO]");
        setSize(1100, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(new Color(20, 30, 40));
        setLayout(new BorderLayout(10, 10));

        setupUI();
        initGame();
        
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void setupUI() {
        // --- ログパネル (左側) ---
        logDisplay = new JTextArea();
        logDisplay.setBackground(new Color(10, 15, 20));
        logDisplay.setForeground(new Color(150, 255, 150));
        logDisplay.setFont(new Font("Monospaced", Font.PLAIN, 13));
        logDisplay.setEditable(false);
        logDisplay.setMargin(new Insets(10, 10, 10, 10));
        JScrollPane logScroll = new JScrollPane(logDisplay);
        logScroll.setPreferredSize(new Dimension(450, 0));
        logScroll.setBorder(new TitledBorder(new LineBorder(Color.GRAY), "BATTLE LOG", 0, 0, null, Color.WHITE));
        add(logScroll, BorderLayout.WEST);

        // --- メインバトルエリア ---
        JPanel mainArea = new JPanel(new GridLayout(3, 1, 5, 5));
        mainArea.setOpaque(false);

        // 相手の場（シミュレーション用）
        JPanel oppPanel = new JPanel(new FlowLayout());
        oppPanel.setOpaque(false);
        oppPanel.setBorder(new TitledBorder(null, "OPPONENT FIELD", 0, 0, null, Color.RED));
        oppPanel.add(createCardUI("相手のバトルポケモン", "HP 280", Color.DARK_GRAY));

        // 自分の場
        fieldPanel = new JPanel(new FlowLayout());
        fieldPanel.setOpaque(false);
        fieldPanel.setBorder(new TitledBorder(null, "YOUR FIELD", 0, 0, null, Color.CYAN));

        // 手札
        handPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        handPanel.setOpaque(false);
        handPanel.setBorder(new TitledBorder(null, "YOUR HAND", 0, 0, null, Color.YELLOW));

        mainArea.add(oppPanel);
        mainArea.add(fieldPanel);
        mainArea.add(handPanel);
        add(mainArea, BorderLayout.CENTER);

        // --- 下部操作バー ---
        JPanel controlBar = new JPanel(new BorderLayout());
        controlBar.setBackground(new Color(45, 52, 54));
        
        statusLabel = new JLabel(" TURN 1 - 準備中...", JLabel.CENTER);
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 16));
        
        JButton nextBtn = new JButton("次のアクションを実行 ⏩");
        nextBtn.addActionListener(e -> processGameStep());
        
        controlBar.add(statusLabel, BorderLayout.CENTER);
        controlBar.add(nextBtn, BorderLayout.EAST);
        add(controlBar, BorderLayout.SOUTH);
    }

    private void initGame() {
        // デッキ作成 (仮)
        for (int i = 0; i < 15; i++) deck.add("たねポケモンV");
        for (int i = 0; i < 10; i++) deck.add("進化ポケモン");
        for (int i = 0; i < 20; i++) deck.add("グッズ/サポート");
        for (int i = 0; i < 15; i++) deck.add("エネルギー");
        Collections.shuffle(deck);

        appendLog("=== GAME START ===");
        appendLog("山札をシャッフルしました。");
        
        // セットアップ
        drawInitialHand();
    }

    private void drawInitialHand() {
        appendLog("手札を7枚引きます...");
        for (int i = 0; i < 7; i++) hand.add(deck.remove(0));
        updateHandUI();

        // マリガン判定（たねがいるか）
        boolean hasBasic = hand.stream().anyMatch(c -> c.contains("たね"));
        if (!hasBasic) {
            appendLog("たねポケモンがいません！引き直します。");
            deck.addAll(hand);
            hand.clear();
            initGame();
        } else {
            appendLog("たねポケモンを確認。対戦準備完了。");
            // サイド設置
            for (int i = 0; i < 6; i++) side.add(deck.remove(0));
            appendLog("サイドを6枚セットしました。");
        }
    }

    private void processGameStep() {
        switch(turn) {
            case 1: // たねを場に出す
                String basic = hand.stream().filter(c -> c.contains("たね")).findFirst().get();
                hand.remove(basic);
                field.put(basic, 0);
                appendLog(basic + " をバトル場に出しました。");
                updateFieldUI();
                updateHandUI();
                statusLabel.setText(" TURN 1 - 自分の番 ");
                turn++;
                break;
            case 2: // ドロー・攻撃
                String card = deck.remove(0);
                hand.add(card);
                appendLog("山札から1枚引きました: " + card);
                appendLog("バトルポケモンのワザ！ 相手に 120 ダメージ！");
                updateHandUI();
                turn++;
                break;
            default:
                appendLog("ターン " + (turn/2 + 1) + " : 激しい攻防が続いています...");
                turn++;
        }
    }

    private void updateHandUI() {
        handPanel.removeAll();
        for (String c : hand) {
            handPanel.add(createCardUI(c, "", new Color(241, 196, 15)));
        }
        handPanel.revalidate();
        handPanel.repaint();
    }

    private void updateFieldUI() {
        fieldPanel.removeAll();
        for (String name : field.keySet()) {
            fieldPanel.add(createCardUI(name, "HP 220", new Color(52, 152, 219)));
        }
        fieldPanel.revalidate();
        fieldPanel.repaint();
    }

    private JPanel createCardUI(String name, String hp, Color bg) {
        JPanel card = new JPanel(new BorderLayout());
        card.setPreferredSize(new Dimension(100, 130));
        card.setBackground(bg);
        card.setBorder(new LineBorder(Color.WHITE, 2));
        
        JLabel nl = new JLabel("<html><center>" + name + "</center></html>", JLabel.CENTER);
        nl.setForeground(Color.BLACK);
        JLabel hl = new JLabel(hp, JLabel.RIGHT);
        
        card.add(nl, BorderLayout.CENTER);
        card.add(hl, BorderLayout.SOUTH);
        return card;
    }

    private void appendLog(String msg) {
        logDisplay.append(" > " + msg + "\n");
    }

    public static void main(String[] args) {
        new PokecaZeroEngine();
    }
}