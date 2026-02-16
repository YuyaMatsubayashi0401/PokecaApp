package PCG_db;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class PokecaSimulator extends JFrame {
    private DatabaseManager db = new DatabaseManager();
    private int currentDeckId = -1; 
    private List<Card> customDeck = new ArrayList<>();

    private final String[] categories = {"たねポケモン", "1進化ポケモン", "2進化ポケモン", "グッズ", "ポケモンのどうぐ", "サポート", "スタジアム", "基本エネルギー", "特殊エネルギー"};
    private final JTextField[] categoryCounts = new JTextField[categories.length];
    private JLabel statusLabel = new JLabel("新規作成中");

    public PokecaSimulator() {
        setTitle("ポケカシミュレーターDB");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // EXITからDISPOSEに変更（メニュー併用のため）
        setLayout(new BorderLayout(10, 10));

        // メイン入力パネル
        JPanel inputPanel = new JPanel(new GridLayout(10, 2, 5, 5));
        for (int i = 0; i < categories.length; i++) {
            inputPanel.add(new JLabel(" " + categories[i]));
            categoryCounts[i] = new JTextField("");
            categoryCounts[i].addActionListener(e -> ((JTextField)e.getSource()).transferFocus());
            inputPanel.add(categoryCounts[i]);
        }

        // ボタンパネル
        JPanel btnPanel = new JPanel(new GridLayout(2, 4, 5, 5)); // ヘルプ追加のため列を調整
        JButton setupBtn = new JButton("カード名登録");
        JButton runBtn = new JButton("抽選開始");
        JButton saveBtn = new JButton("新規保存");
        JButton updateBtn = new JButton("上書き保存");
        JButton deleteBtn = new JButton("削除");
        JButton loadBtn = new JButton("読込");
        JButton helpBtn = new JButton("❓ ヘルプ");
        
        setupBtn.addActionListener(e -> openNameSetupDialog());
        runBtn.addActionListener(e -> startLottery());
        saveBtn.addActionListener(e -> saveDeck(false));
        updateBtn.addActionListener(e -> saveDeck(true));
        deleteBtn.addActionListener(e -> deleteDeck());
        loadBtn.addActionListener(e -> loadDeck());
        helpBtn.addActionListener(e -> showHelp());

        btnPanel.add(createMenuButton());
        btnPanel.add(setupBtn); 
        btnPanel.add(runBtn); 
        btnPanel.add(saveBtn);
        btnPanel.add(updateBtn); 
        btnPanel.add(deleteBtn); 
        btnPanel.add(loadBtn);
        btnPanel.add(helpBtn);

        add(inputPanel, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);
        add(statusLabel, BorderLayout.NORTH);
        
        pack(); 
        setLocationRelativeTo(null);
    }

    private void showHelp() {
        String msg = "【シミュレーター 操作ガイド】\n\n" +
                     "■ 1. 枚数入力\n" +
                     "・各カテゴリの枚数を入力し、合計を「60枚」にします。\n\n" +
                     "■ 2. カード名登録（必須）\n" +
                     "・[カード名登録]ボタンを押し、具体的なカード名を入力します。\n" +
                     "・これを行わないと、抽選や保存ができません。\n\n" +
                     "■ 3. 抽選開始と統計\n" +
                     "・[抽選開始]で、手札7枚とサイド6枚をランダムに抽出します。\n" +
                     "・同時に「1万回の試行」を行い、初手に『たねポケモン』が\n" +
                     "  来る確率（たね率）を計算して表示します。\n\n" +
                     "■ 4. マリガン機能\n" +
                     "・結果画面で[マリガン]を押すと、現在のデッキで再度引き直せます。\n\n" +
                     "■ 5. データの管理\n" +
                     "・[読込]で過去のデッキを呼び出し、[上書き]で更新可能です。";

        JTextArea area = new JTextArea(msg);
        area.setFont(new Font("Meiryo", Font.PLAIN, 13));
        area.setEditable(false);
        area.setOpaque(false);
        area.setMargin(new Insets(10, 10, 10, 10));
        JOptionPane.showMessageDialog(this, new JScrollPane(area), "シミュレーターの使いかた", JOptionPane.INFORMATION_MESSAGE);
    }

    private void startLottery() {
        if (customDeck.size() != 60) {
            JOptionPane.showMessageDialog(this, "60枚のカード名登録が必要です。");
            return;
        }
        showResultDialog();
    }

    private void showResultDialog() {
        List<Card> deck = new ArrayList<>(customDeck);
        Collections.shuffle(deck);
        
        List<Card> hand = new ArrayList<>(deck.subList(0, 7));
        List<Card> side = new ArrayList<>(deck.subList(7, 13));
        double prob = calculateProb();
        new ResultDialog(this, hand, side, prob).setVisible(true);
    }

    private double calculateProb() {
        int hit = 0;
        for (int i = 0; i < 10000; i++) {
            List<Card> sim = new ArrayList<>(customDeck);
            Collections.shuffle(sim);
            for (Card c : sim.subList(0, 7)) {
                if (c.category.equals("たねポケモン")) { hit++; break; }
            }
        }
        return (double) hit / 100;
    }

    class ResultDialog extends JDialog {
        public ResultDialog(Frame owner, List<Card> hand, List<Card> side, double prob) {
            super(owner, "抽選結果と統計", true);
            setLayout(new BorderLayout());

            boolean hasTane = hand.stream().anyMatch(c -> c.category.equals("たねポケモン"));

            StringBuilder sb = new StringBuilder("<html><body style='padding:10px;'>");
            sb.append("<h2 style='color:blue;'>統計: たね率 ").append(String.format("%.2f", prob)).append("%</h2>");
            
            if (!hasTane) {
                sb.append("<h3 style='color:red;'>たねがありません（マリガン対象）</h3>");
            }

            sb.append("<hr><h3>今回の手札 (7枚)</h3><ul>");
            for (Card c : hand) {
                String color = c.category.equals("たねポケモン") ? "green" : "black";
                sb.append("<li><font color='").append(color).append("'>").append(c.name).append("</font></li>");
            }
            sb.append("</ul><h3>サイド (6枚)</h3><ul>");
            for (Card c : side) sb.append("<li>").append(c.name).append("</li>");
            sb.append("</ul></body></html>");

            add(new JScrollPane(new JLabel(sb.toString())), BorderLayout.CENTER);

            JPanel p = new JPanel();
            JButton retryBtn = new JButton(hasTane ? "再抽選" : "マリガンして引き直す");
            retryBtn.addActionListener(e -> {
                dispose(); 
                showResultDialog();
            });
            
            JButton closeBtn = new JButton("終了");
            closeBtn.addActionListener(e -> dispose());

            p.add(retryBtn);
            p.add(closeBtn);
            add(p, BorderLayout.SOUTH);

            setSize(400, 650);
            setLocationRelativeTo(owner);
        }
    }

    private void saveDeck(boolean isUpdate) {
        if (customDeck.size() != 60) {
            JOptionPane.showMessageDialog(this, "まず枚数入力と名前登録を完了してください。");
            return;
        }
        try {
            if (isUpdate && currentDeckId != -1) {
                db.updateDeck(currentDeckId, customDeck);
                JOptionPane.showMessageDialog(this, "上書き完了");
            } else {
                String name = JOptionPane.showInputDialog(this, "新規デッキ名:");
                if (name != null && !name.isEmpty()) {
                    currentDeckId = db.saveDeck(name, customDeck);
                    statusLabel.setText("編集中: " + name);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadDeck() {
        try {
            List<String[]> list = db.getDeckList();
            if (list.isEmpty()) {
                JOptionPane.showMessageDialog(this, "保存されたデッキがありません。");
                return;
            }
            String[] options = list.stream().map(d -> d[0] + ":" + d[1]).toArray(String[]::new);
            String sel = (String)JOptionPane.showInputDialog(this, "読込", "選択", 1, null, options, options[0]);
            if (sel != null) {
                currentDeckId = Integer.parseInt(sel.split(":")[0]);
                customDeck = db.fetchCards(currentDeckId);
                // フィールドへの反映（枚数集計）
                refreshCounts();
                statusLabel.setText("編集中: " + sel.split(":")[1]);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // 読込時に現在の枚数テキストフィールドを更新する補助メソッド
    private void refreshCounts() {
        for (int i = 0; i < categories.length; i++) {
            final String cat = categories[i];
            long count = customDeck.stream().filter(c -> c.category.equals(cat)).count();
            categoryCounts[i].setText(count > 0 ? String.valueOf(count) : "");
        }
    }

    private void deleteDeck() {
        if (currentDeckId == -1) return;
        if (JOptionPane.showConfirmDialog(this, "このデッキを削除しますか？") != 0) return;
        try {
            db.deleteDeck(currentDeckId);
            currentDeckId = -1; customDeck.clear();
            for (JTextField tf : categoryCounts) tf.setText("");
            statusLabel.setText("新規作成中");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void openNameSetupDialog() {
        JPanel p = new JPanel(); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        List<JTextField> tfs = new ArrayList<>();
        List<String> cats = new ArrayList<>();
        int total = 0;
        for (int i = 0; i < categories.length; i++) {
            int c = getCount(categoryCounts[i]);
            if (c > 0) {
                p.add(new JLabel("--- " + categories[i] + " ---"));
                for (int j = 0; j < c; j++) {
                    // 既存データがある場合はその名前をセット
                    String existingName = "";
                    int currentIdx = total + j;
                    if (currentIdx < customDeck.size()) {
                        existingName = customDeck.get(currentIdx).name;
                    } else {
                        existingName = categories[i] + " No." + (j+1);
                    }

                    JTextField t = new JTextField(existingName, 20);
                    t.addActionListener(e -> t.transferFocus());
                    tfs.add(t); cats.add(categories[i]); p.add(t);
                    p.add(Box.createVerticalStrut(2));
                }
                total += c;
            }
        }
        if (total != 60) { JOptionPane.showMessageDialog(this, "合計を60枚にしてください。現在:"+total); return; }
        
        JScrollPane sp = new JScrollPane(p); sp.setPreferredSize(new Dimension(350, 500));
        sp.getVerticalScrollBar().setUnitIncrement(16);
        
        if (JOptionPane.showConfirmDialog(this, sp, "カード名登録", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            customDeck.clear();
            for (int i = 0; i < tfs.size(); i++) {
                customDeck.add(new Card(tfs.get(i).getText(), cats.get(i)));
            }
            JOptionPane.showMessageDialog(this, "60枚の名前を登録しました。抽選が可能です。");
        }
    }

    private int getCount(JTextField f) { 
        try { return Integer.parseInt(f.getText().trim()); } catch (Exception e) { return 0; } 
    }

    private JButton createMenuButton() {
        JButton menuBtn = new JButton("🏠 メインメニュー");
        menuBtn.setBackground(new java.awt.Color(230, 230, 250));
        menuBtn.addActionListener(e -> {
            this.dispose();
            boolean found = false;
            for (java.awt.Frame frame : java.awt.Frame.getFrames()) {
                if (frame instanceof MainMenu) {
                    frame.setVisible(true);
                    frame.toFront();
                    found = true;
                    break;
                }
            }
            if (!found) new MainMenu().setVisible(true);
        });
        return menuBtn;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PokecaSimulator().setVisible(true));
    }
}