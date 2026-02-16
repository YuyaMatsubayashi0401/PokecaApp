package PCG_db;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.sql.Connection;
import java.sql.DriverManager;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

/**
 * ポケカ総合管理システム - メインメニュー
 * 全ボタンのイベントリスナーを実装した完全版コード
 */
public class MainMenu extends JFrame {

    public MainMenu() {
        // --- ウィンドウ基本設定 ---
        setTitle("ポケカ総合管理システム");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(480, 820);
        setLayout(new BorderLayout(10, 10));

        // --- タイトルパネル ---
        JPanel titlePanel = new JPanel(new GridLayout(2, 1));
        titlePanel.setBackground(new Color(45, 45, 45));
        
        JLabel titleLabel = new JLabel("Pokeca MANAGER", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(Color.WHITE);
        
        JLabel subLabel = new JLabel("総合メインメニュー", JLabel.CENTER);
        subLabel.setFont(new Font("Meiryo", Font.PLAIN, 14));
        subLabel.setForeground(Color.LIGHT_GRAY);
        
        titlePanel.add(titleLabel);
        titlePanel.add(subLabel);
        titlePanel.setBorder(new EmptyBorder(20, 0, 20, 0));
        add(titlePanel, BorderLayout.NORTH);

        // --- メインボタンパネル ---
        JPanel buttonPanel = new JPanel(new GridLayout(9, 1, 10, 10)); 
        buttonPanel.setBorder(new EmptyBorder(20, 40, 20, 40));

        // ボタンのインスタンス化
        JButton inventoryBtn = new JButton("📦 ストレージ・在庫管理");
        JButton editorBtn    = new JButton("📝 デッキ作成・エディタ");
        JButton analyzerBtn  = new JButton("📊 デッキバランス比較分析");
        JButton probBtn      = new JButton("📈 安定度・アクセス確率診断");
        JButton simulatorBtn = new JButton("🎲 マリガン・シミュレータ");
        JButton drawSimBtn   = new JButton("🃏 ドロー・一人回しシミュ");
        JButton fieldSimBtn  = new JButton("💥 盤面ダメカン・シミュレータ");
        JButton exitBtn      = new JButton("❌ アプリを終了する");

        // フォントとスタイルの適用
        Font btnFont = new Font("Meiryo", Font.BOLD, 14);
        JButton[] buttons = {inventoryBtn, editorBtn, analyzerBtn, probBtn, simulatorBtn, drawSimBtn, fieldSimBtn, exitBtn};
        for (JButton btn : buttons) {
            btn.setFont(btnFont);
            btn.setFocusPainted(false); // フォーカス枠を非表示にしてスッキリさせる
        }
        
        fieldSimBtn.setBackground(new Color(255, 250, 205)); // 特徴的な色
        exitBtn.setForeground(new Color(200, 50, 50));     // 終了ボタンは赤文字

        // --- 各機能の起動イベント（全ボタン実装済み） ---
        
        // 1. 在庫管理
        inventoryBtn.addActionListener(e -> new InventoryManager().setVisible(true));
        
        // 2. デッキエディタ
        editorBtn.addActionListener(e -> new DeckEditor().setVisible(true));
        
        // 3. デッキバランス分析
        analyzerBtn.addActionListener(e -> new DeckBalanceAnalyzer().setVisible(true));
        
        // 4. 確率診断
        probBtn.addActionListener(e -> new ProbabilityAnalyzer().setVisible(true));
        
        // 5. マリガンシミュレータ
        simulatorBtn.addActionListener(e -> new PokecaSimulator().setVisible(true));
        
        // 6. ドローシミュレータ
        drawSimBtn.addActionListener(e -> new DrawSimulator().setVisible(true));
        
        // 7. ダメカンシミュレータ
        fieldSimBtn.addActionListener(e -> new FieldSimulator().setVisible(true));

        // 終了ボタン
        exitBtn.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this, "終了しますか？", "確認", JOptionPane.YES_NO_OPTION) == 0) {
                System.exit(0);
            }
        });

        // パネルへボタンを追加
        buttonPanel.add(inventoryBtn);
        buttonPanel.add(editorBtn);
        buttonPanel.add(analyzerBtn);
        buttonPanel.add(probBtn);
        buttonPanel.add(simulatorBtn);
        buttonPanel.add(drawSimBtn);
        buttonPanel.add(fieldSimBtn);
        buttonPanel.add(new JLabel("")); // スペーサー
        buttonPanel.add(exitBtn);
        add(buttonPanel, BorderLayout.CENTER);

        // --- フッター（デバッグ・設定支援ツール） ---
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setBackground(new Color(240, 240, 240));
        
        JPanel debugPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        debugPanel.setOpaque(false);
        
        JButton sqlGuideBtn = new JButton("🛠 セットアップ手順・SQL");
        JButton testConnBtn = new JButton("🔌 DB接続テスト");
        
        sqlGuideBtn.setFont(new Font("Meiryo", Font.PLAIN, 11));
        testConnBtn.setFont(new Font("Meiryo", Font.PLAIN, 11));
        
        sqlGuideBtn.addActionListener(e -> showSqlGuide());
        testConnBtn.addActionListener(e -> testDatabaseConnection());

        debugPanel.add(sqlGuideBtn);
        debugPanel.add(testConnBtn);

        JLabel footerLabel = new JLabel("v2026.1.1  ", JLabel.RIGHT);
        footerLabel.setFont(new Font("Arial", Font.ITALIC, 11));
        
        footerPanel.add(debugPanel, BorderLayout.WEST);
        footerPanel.add(footerLabel, BorderLayout.EAST);
        add(footerPanel, BorderLayout.SOUTH);

        setLocationRelativeTo(null); // 画面中央に表示
    }

    /**
     * データベース接続テスト
     */
    private void testDatabaseConnection() {
        String url = "jdbc:postgresql://localhost:5432/pokeca_db";
        String user = "postgres";
        String pass = "postgrestest"; 

        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            JOptionPane.showMessageDialog(this, 
                "【接続成功】\nPostgreSQLとの通信に成功しました。\nシステムを利用可能です。", 
                "DB接続テスト", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "【接続失敗】\n" + e.getMessage() + "\n\n" +
                "以下の項目を確認してください：\n" +
                "1. PostgreSQLサービスが起動しているか\n" +
                "2. 'pokeca_db' という名前のDBが存在するか\n" +
                "3. ユーザー名・パスワードが合っているか\n" +
                "4. JDBCドライバ(postgresql.jar)がビルドパスに含まれているか", 
                "DB接続テスト", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * SQLガイドウィンドウの表示
     */
    private void showSqlGuide() {
        JDialog dialog = new JDialog(this, "データベースセットアップガイド", true);
        dialog.setSize(600, 700);
        dialog.setLayout(new BorderLayout());

        JTextArea guideText = new JTextArea();
        guideText.setEditable(false);
        guideText.setBackground(new Color(245, 250, 255));
        guideText.setFont(new Font("Meiryo", Font.PLAIN, 13));
        guideText.setMargin(new Insets(15, 15, 15, 15));
        guideText.setText(
            "【導入手順】\n" +
            "1. PostgreSQLをインストールし、サーバーを起動します。\n" +
            "2. pgAdminなどで 'pokeca_db' というデータベースを作成します。\n" +
            "3. 下記のSQLをコピーし、クエリエディタで実行してください。\n" +
            "4. Eclipse側で 'postgresql-xxx.jar' をビルドパスに追加してください。\n\n" +
            "※注意: 各IDはSERIAL型のため、自動で採番されます。"
        );

        String sqlCode = 
            "-- 1. デッキ親テーブル\n" +
            "CREATE TABLE decks (\n" +
            "    deck_id SERIAL PRIMARY KEY,\n" +
            "    deck_name VARCHAR(100) NOT NULL,\n" +
            "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP\n" +
            ");\n\n" +
            "-- 2. デッキ子テーブル (60枚のカード)\n" +
            "CREATE TABLE deck_cards (\n" +
            "    deck_card_id SERIAL PRIMARY KEY,\n" +
            "    deck_id INTEGER REFERENCES decks(deck_id) ON DELETE CASCADE,\n" +
            "    card_name VARCHAR(100) NOT NULL,\n" +
            "    category VARCHAR(50) NOT NULL,\n" +
            "    slot_number INTEGER NOT NULL\n" +
            ");\n\n" +
            "-- 3. 在庫管理用テーブル\n" +
            "CREATE TABLE storage (\n" +
            "    card_id SERIAL PRIMARY KEY,\n" +
            "    card_name VARCHAR(100) NOT NULL,\n" +
            "    category VARCHAR(50),\n" +
            "    quantity INTEGER DEFAULT 0,\n" +
            "    reg_mark VARCHAR(10),\n" +
            "    tags TEXT,\n" +
            "    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP\n" +
            ");";

        JTextArea sqlArea = new JTextArea(sqlCode);
        sqlArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        sqlArea.setEditable(false);
        sqlArea.setMargin(new Insets(10, 10, 10, 10));
        sqlArea.setBackground(Color.BLACK);
        sqlArea.setForeground(Color.GREEN);

        JButton copyBtn = new JButton("SQL文をクリップボードにコピー");
        copyBtn.setFont(new Font("Meiryo", Font.BOLD, 14));
        copyBtn.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(sqlCode), null);
            JOptionPane.showMessageDialog(dialog, "コピーしました。クエリエディタに貼り付けてください。");
        });

        dialog.add(guideText, BorderLayout.NORTH);
        dialog.add(new JScrollPane(sqlArea), BorderLayout.CENTER);
        dialog.add(copyBtn, BorderLayout.SOUTH);
        
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    public static void main(String[] args) {
        try {
            // OS標準の見た目に設定
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
        SwingUtilities.invokeLater(() -> new MainMenu().setVisible(true));
    }
}