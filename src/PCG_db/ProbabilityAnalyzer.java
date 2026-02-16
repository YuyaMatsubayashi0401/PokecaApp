package PCG_db;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

public class ProbabilityAnalyzer extends JFrame {
    private DatabaseManager db = new DatabaseManager();
    private JTable table;
    private DefaultTableModel model;
    private List<Card> currentDeckCards = new ArrayList<>();
    private JLabel statusLabel = new JLabel("デッキを読み込んでください");

    public ProbabilityAnalyzer() {
        setTitle("ポケカ統計分析システム");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(900, 650);
        setLayout(new BorderLayout(10, 10));

        // テーブルモデルの設定
        String[] columns = {"カード名", "カテゴリ", "キーカード (現物)", "アクセス札 (サーチ)"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex >= 2) return Boolean.class;
                return String.class;
            }
            @Override
            public boolean isCellEditable(int row, int column) {
                return column >= 2;
            }
        };

        table = new JTable(model);
        table.setRowHeight(25);
        table.getTableHeader().setReorderingAllowed(false);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // 操作パネル
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton loadBtn = new JButton("デッキ読み込み");
        JButton runBtn = new JButton("📊 試行開始 (10,000回)");
        JButton helpBtn = new JButton("❓ 操作ガイド");
        JButton menuBtn = createMenuButton();

        loadBtn.addActionListener(e -> loadDeckData());
        runBtn.addActionListener(e -> runSimulation());
        helpBtn.addActionListener(e -> showHelp());

        topPanel.add(menuBtn);
        topPanel.add(loadBtn);
        topPanel.add(runBtn);
        topPanel.add(helpBtn);
        topPanel.add(statusLabel);
        add(topPanel, BorderLayout.NORTH);

        setLocationRelativeTo(null);
    }

    private void showHelp() {
        String msg = "【統計分析 操作ガイド】\n\n" +
                     "■ 1. デッキの読み込み\n" +
                     "保存された60枚のデッキを選択して展開します。\n\n" +
                     "■ 2. 分析パラメータの設定（チェックボックス）\n" +
                     "・[キーカード]: 盤面に準備したい特定のカードを指定します。\n" +
                     "・[アクセス札]: ボール系やサーチ札など、キーカードを呼べるカードを指定します。\n\n" +
                     "■ 3. 試行内容 (10,000 samples)\n" +
                     "モンテカルロ法を用い、シャッフル後の山札から以下の確率を算出します。\n" +
                     "・初手現物率: 最初の7枚に「キーカード」が含まれる確率。\n" +
                     "・アクセス込率: 最初の7枚に「キーカード」または「アクセス札」がある確率。\n" +
                     "・サイド全落ち率: 指定したキーカードの「現物すべて」がサイド(6枚)にある確率。\n\n" +
                     "※分析にはデッキが正確に60枚である必要があります。";

        JTextArea area = new JTextArea(msg);
        area.setFont(new Font("Meiryo", Font.PLAIN, 13));
        area.setEditable(false);
        area.setOpaque(false);
        area.setMargin(new Insets(10, 10, 10, 10));
        JOptionPane.showMessageDialog(this, new JScrollPane(area), "分析システムの仕様", JOptionPane.INFORMATION_MESSAGE);
    }

    private void loadDeckData() {
        try {
            List<String[]> list = db.getDeckList();
            if (list.isEmpty()) return;
            String[] options = list.stream().map(d -> d[0] + ":" + d[1]).toArray(String[]::new);
            String sel = (String) JOptionPane.showInputDialog(this, "分析するデッキを選択", "ロード", 
                    JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
            
            if (sel != null) {
                int id = Integer.parseInt(sel.split(":")[0]);
                currentDeckCards = db.fetchCards(id);
                model.setRowCount(0);
                
                // チェック操作を容易にするため、表示はカード名でユニークにする
                Set<String> processedNames = new HashSet<>();
                for (Card c : currentDeckCards) {
                    if (processedNames.add(c.name)) {
                        model.addRow(new Object[]{c.name, c.category, false, false});
                    }
                }
                statusLabel.setText("デッキ: " + sel.split(":")[1] + " (" + currentDeckCards.size() + "枚)");
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void runSimulation() {
        if (currentDeckCards.size() != 60) {
            JOptionPane.showMessageDialog(this, "60枚のデッキを読み込んでください。");
            return;
        }

        List<String> keyNames = new ArrayList<>();
        List<String> accessNames = new ArrayList<>();

        for (int i = 0; i < model.getRowCount(); i++) {
            if ((Boolean) model.getValueAt(i, 2)) keyNames.add((String) model.getValueAt(i, 0));
            if ((Boolean) model.getValueAt(i, 3)) accessNames.add((String) model.getValueAt(i, 0));
        }

        if (keyNames.isEmpty()) {
            JOptionPane.showMessageDialog(this, "キーカードを少なくとも1つ選択してください。");
            return;
        }

        new ResultWindow(currentDeckCards, keyNames, accessNames).setVisible(true);
    }

    private JButton createMenuButton() {
        JButton menuBtn = new JButton("🏠 メインメニュー");
        menuBtn.addActionListener(e -> {
            this.dispose();
            for (java.awt.Frame f : java.awt.Frame.getFrames()) {
                if (f.getClass().getSimpleName().equals("MainMenu")) { f.setVisible(true); f.toFront(); return; }
            }
        });
        return menuBtn;
    }

    class ResultWindow extends JFrame {
        public ResultWindow(List<Card> masterDeck, List<String> keys, List<String> access) {
            setTitle("統計解析結果");
            setSize(480, 580);
            setLayout(new BorderLayout());
            setLocationRelativeTo(null);

            JTextArea area = new JTextArea();
            area.setFont(new Font("Monospaced", Font.PLAIN, 14));
            area.setEditable(false);
            area.setBackground(new Color(245, 245, 245));
            area.setMargin(new Insets(20, 20, 20, 20));

            Map<String, Integer> keyCountInDeck = new HashMap<>();
            for (Card c : masterDeck) {
                if (keys.contains(c.name)) {
                    keyCountInDeck.put(c.name, keyCountInDeck.getOrDefault(c.name, 0) + 1);
                }
            }

            int trials = 10000;
            int hitKey = 0;
            int hitAny = 0;
            int totalKeySideOut = 0;

            for (int i = 0; i < trials; i++) {
                List<Card> deck = new ArrayList<>(masterDeck);
                Collections.shuffle(deck);

                List<Card> hand = deck.subList(0, 7);
                List<Card> side = deck.subList(7, 13);

                boolean hasKeyInHand = hand.stream().anyMatch(c -> keys.contains(c.name));
                boolean hasAccessInHand = hand.stream().anyMatch(c -> access.contains(c.name));

                if (hasKeyInHand) hitKey++;
                if (hasKeyInHand || hasAccessInHand) hitAny++;

                boolean isKeyDead = false;
                for (String keyName : keys) {
                    long countInSide = side.stream().filter(c -> c.name.equals(keyName)).count();
                    int totalInDeck = keyCountInDeck.getOrDefault(keyName, 0);
                    if (countInSide >= totalInDeck && totalInDeck > 0) {
                        isKeyDead = true; 
                        break;
                    }
                }
                if (isKeyDead) totalKeySideOut++;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("統計結果\n");
            sb.append("====================================\n\n");
            sb.append("🔑 分析ターゲット:\n");
            keyCountInDeck.forEach((name, count) -> sb.append(String.format(" ・%-18s [%d枚]\n", name, count)));
            
            sb.append("\n[10,000 回算出 ]\n");
            sb.append("------------------------------------\n");
            sb.append(String.format("■ 初手現物率(7枚)     :  %6.2f%%\n", (hitKey / (double)trials) * 100));
            sb.append(String.format("■ アクセス札込(触れる):  %6.2f%%\n", (hitAny / (double)trials) * 100));
            sb.append("------------------------------------\n");
            sb.append(String.format("■ サイド全落ち率      :  %6.2f%%\n", (totalKeySideOut / (double)trials) * 100));
            sb.append("------------------------------------\n\n");
            sb.append("※全落ち率は選択したキーカードの\n いずれかが1枚も山札に残らない確率です。\n");

            area.setText(sb.toString());
            add(new JScrollPane(area), BorderLayout.CENTER);
            
            JButton closeBtn = new JButton("確認しました");
            closeBtn.addActionListener(e -> this.dispose());
            add(closeBtn, BorderLayout.SOUTH);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ProbabilityAnalyzer().setVisible(true));
    }
}