package PCG_db;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class DrawSimulator extends JFrame {
    private DatabaseManager db = new DatabaseManager();
    private List<Card> masterDeck = new ArrayList<>();
    private List<Card> deck = new ArrayList<>();
    private List<Card> hand = new ArrayList<>();
    private List<Card> side = new ArrayList<>();
    private List<Card> discardPile = new ArrayList<>();

    private JTextArea displayArea;
    private JLabel infoLabel;

    public DrawSimulator() {
        setTitle("ポケカ・ドローシミュレーター");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(700, 800);
        setLayout(new BorderLayout());

        JPanel northPanel = new JPanel(new GridLayout(2, 1));
        infoLabel = new JLabel("デッキを読み込んでください", JLabel.CENTER);
        JButton loadBtn = new JButton("DBからデッキを読み込む");
        loadBtn.addActionListener(e -> loadFromDB());
        northPanel.add(loadBtn);
        northPanel.add(infoLabel);
        add(northPanel, BorderLayout.NORTH);

        displayArea = new JTextArea();
        displayArea.setEditable(false);
        displayArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        add(new JScrollPane(displayArea), BorderLayout.CENTER);

        // --- 操作ボタンエリア (4行3列 = 合計12枠) ---
        JPanel southPanel = new JPanel(new GridLayout(4, 3, 5, 5));

        JButton drawBtn = new JButton("1枚ドロー");
        JButton gearBtn = new JButton("山札上確認(ギア/トレシュ等)");
        JButton drBtn = new JButton("博士の研究(捨7)");
        JButton judgeBtn = new JButton("ジャッジマン(戻4)"); 
        JButton zeiyuBtn = new JButton("ゼイユ(捨5)");
        JButton lillie6Btn = new JButton("リーリエ(戻6)");
        JButton lillie8Btn = new JButton("リーリエ(戻8)");
        JButton getSideBtn = new JButton("サイドを1枚取る"); 
        JButton checkSideBtn = new JButton("サイド確認");
        JButton resetBtn = new JButton("対戦準備(初期化)");
        JButton shuffleBtn = new JButton("山札を混ぜる");

        // 各ボタンのロジック設定
        drawBtn.addActionListener(e -> drawCards(1));
        gearBtn.addActionListener(e -> peekAndAddCard());
        drBtn.addActionListener(e -> executeSupport("DISCARD", 7, false));
        judgeBtn.addActionListener(e -> executeSupport("SHUFFLE", 4, true)); // ジャッジマン処理
        zeiyuBtn.addActionListener(e -> executeSupport("DISCARD", 5, false));
        lillie6Btn.addActionListener(e -> executeSupport("SHUFFLE", 6, true));
        lillie8Btn.addActionListener(e -> executeSupport("SHUFFLE", 8, true));
        
        // サイド取得ロジック
        getSideBtn.addActionListener(e -> {
            if (side.isEmpty()) {
                JOptionPane.showMessageDialog(this, "サイドがもうありません！勝利です！");
                return;
            }
            int randomIndex = (int)(Math.random() * side.size());
            Card picked = side.remove(randomIndex);
            hand.add(picked);
            JOptionPane.showMessageDialog(this, "サイドから「" + picked.name + "」を取りました。");
            updateDisplay();
        });

        checkSideBtn.addActionListener(e -> showSide());
        resetBtn.addActionListener(e -> setupGame());
        shuffleBtn.addActionListener(e -> {
            Collections.shuffle(deck);
            JOptionPane.showMessageDialog(this, "山札をシャッフルしました。");
        });

        // ボタン追加
        southPanel.add(drawBtn);
        southPanel.add(gearBtn);
        southPanel.add(resetBtn);
        southPanel.add(drBtn);
        southPanel.add(judgeBtn);
        southPanel.add(zeiyuBtn);
        southPanel.add(lillie6Btn);
        southPanel.add(lillie8Btn);
        southPanel.add(getSideBtn);
        southPanel.add(checkSideBtn);
        southPanel.add(shuffleBtn);
        southPanel.add(createMenuButton());

        add(southPanel, BorderLayout.SOUTH);
        setLocationRelativeTo(null);
    }

    private JButton createMenuButton() {
        JButton menuBtn = new JButton("🏠 メインメニュー");
        menuBtn.setBackground(new Color(230, 230, 250));
        menuBtn.addActionListener(e -> {
            this.dispose();
            for (java.awt.Frame frame : java.awt.Frame.getFrames()) {
                if (frame instanceof MainMenu) {
                    frame.setVisible(true);
                    frame.toFront();
                    return;
                }
            }
            new MainMenu().setVisible(true);
        });
        return menuBtn;
    }

    private void peekAndAddCard() {
        if (deck.isEmpty()) {
            JOptionPane.showMessageDialog(this, "山札がありません。");
            return;
        }
        Integer[] counts = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        Integer n = (Integer) JOptionPane.showInputDialog(this, "山札の上から何枚確認しますか？", "効果", JOptionPane.QUESTION_MESSAGE,
                null, counts, 7);
        if (n == null) return;

        int actualPeek = Math.min(n, deck.size());
        List<Card> peekingCards = new ArrayList<>();
        for (int i = 0; i < actualPeek; i++) peekingCards.add(deck.remove(0));

        Object[] options = new Object[peekingCards.size() + 1];
        for (int i = 0; i < peekingCards.size(); i++)
            options[i] = (i + 1) + ": " + peekingCards.get(i).name;
        options[peekingCards.size()] = "手札に加えない";

        String selected = (String) JOptionPane.showInputDialog(this, "選んでください", "サーチ", JOptionPane.PLAIN_MESSAGE, null,
                options, options[0]);

        if (selected != null && !selected.equals("手札に加えない")) {
            int selectedIdx = -1;
            for (int i = 0; i < options.length; i++) {
                if (options[i].equals(selected)) {
                    selectedIdx = i;
                    break;
                }
            }
            Card pickedCard = peekingCards.remove(selectedIdx);
            hand.add(pickedCard);
        }
        // 残ったカードを山札の下に戻す
        deck.addAll(peekingCards);
        updateDisplay();
    }

    private void loadFromDB() {
        try {
            List<String[]> list = db.getDeckList();
            if (list.isEmpty()) {
                JOptionPane.showMessageDialog(this, "デッキなし");
                return;
            }
            String[] options = list.stream().map(d -> d[0] + ":" + d[1]).toArray(String[]::new);
            String sel = (String) JOptionPane.showInputDialog(this, "選択", "読込", 1, null, options, options[0]);
            if (sel != null) {
                masterDeck = db.fetchCards(Integer.parseInt(sel.split(":")[0]));
                if (masterDeck.size() != 60) {
                    JOptionPane.showMessageDialog(this, "注意: デッキが60枚ではありません(" + masterDeck.size() + "枚)");
                }
                setupGame();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupGame() {
        if (masterDeck.isEmpty()) return;
        deck = new ArrayList<>(masterDeck);
        Collections.shuffle(deck);
        hand.clear();
        side.clear();
        discardPile.clear();
        
        // 初期手札7枚、サイド6枚
        int initialHand = Math.min(deck.size(), 7);
        for (int i = 0; i < initialHand; i++) hand.add(deck.remove(0));
        
        int initialSide = Math.min(deck.size(), 6);
        for (int i = 0; i < initialSide; i++) side.add(deck.remove(0));
        
        updateDisplay();
    }

    private void drawCards(int count) {
        for (int i = 0; i < count; i++) {
            if (!deck.isEmpty()) hand.add(deck.remove(0));
        }
        updateDisplay();
    }

    private void executeSupport(String type, int count, boolean shuffle) {
        if (type.equals("DISCARD")) {
            discardPile.addAll(hand);
            hand.clear();
        } else if (type.equals("SHUFFLE")) {
            deck.addAll(hand);
            hand.clear();
            Collections.shuffle(deck);
        } else if (type.equals("BOTTOM")) {
            deck.addAll(hand);
            hand.clear();
        }
        drawCards(count);
    }

    private void showSide() {
        StringBuilder sb = new StringBuilder("--- サイド (残り" + side.size() + "枚) ---\n");
        for (Card c : side) sb.append(c.toString()).append("\n");
        JOptionPane.showMessageDialog(this, sb.toString());
    }

    private void updateDisplay() {
        StringBuilder sb = new StringBuilder();
        sb.append("===== 手札 (").append(hand.size()).append("枚) =====\n");
        for (Card c : hand) sb.append("・").append(c.toString()).append("\n");
        
        sb.append("\n===== トラッシュ (").append(discardPile.size()).append("枚) =====\n");
        if (!discardPile.isEmpty()) {
            sb.append("最後: ").append(discardPile.get(discardPile.size() - 1)).append("\n");
        }
        
        infoLabel.setText(String.format("山札: %d | サイド: %d | トラッシュ: %d", 
                          deck.size(), side.size(), discardPile.size()));
        displayArea.setText(sb.toString());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DrawSimulator().setVisible(true));
    }
}