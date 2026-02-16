package PCG_db;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

public class DeckEditor extends JFrame {
    private DatabaseManager db = new DatabaseManager();
    private JTable table;
    private DefaultTableModel model;
    private int currentDeckId = -1;
    private JLabel countLabel = new JLabel("枚数: 0/60");

    private final String[] categories = {"たねポケモン", "1進化ポケモン", "2進化ポケモン", "グッズ", "ポケモンのどうぐ", "サポート", "スタジアム", "基本エネルギー", "特殊エネルギー"};
    private final String[] energyTypes = {"基本草エネルギー", "基本炎エネルギー", "基本水エネルギー", "基本雷エネルギー", "基本超エネルギー", "基本闘エネルギー", "基本悪エネルギー", "基本鋼エネルギー"};

    public DeckEditor() {
        setTitle("ポケカ・デッキ調整エディタ");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        String[] columns = {"レギュ落", "No", "カード名", "カテゴリ"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) return Boolean.class;
                return super.getColumnClass(columnIndex);
            }
            @Override
            public boolean isCellEditable(int row, int column) {
                return column != 1;
            }
        };

        table = new JTable(model);
        table.setRowHeight(25);

        TableColumn catColumn = table.getColumnModel().getColumn(3);
        catColumn.setCellEditor(new DefaultCellEditor(new JComboBox<>(categories)));

        CustomRowRenderer renderer = new CustomRowRenderer();
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        setupPopupMenu();

        JPanel topPanel = new JPanel();
        JButton loadBtn = new JButton("デッキ読込");
        JButton importBtn = new JButton("一括インポート");
        JButton cleanBtn = new JButton("🧹 名前のみに整形");
        JButton renumBtn = new JButton("🔢 フルネームに再構成");
        JButton exportBtn = new JButton("📋 テキスト出力");
        JButton energyBtn = new JButton("基本エネ投入");
        JButton saveBtn = new JButton("上書き保存");
        JButton saveAsBtn = new JButton("別名で保存");
        JButton clearBtn = new JButton("全クリア");
        JButton menuBtn = createMenuButton();

        importBtn.addActionListener(e -> importFromText());
        cleanBtn.addActionListener(e -> cleanAllCardNames());
        renumBtn.addActionListener(e -> {
            rebuildFullNames();
            JOptionPane.showMessageDialog(this, "「カテゴリ No.数字 カード名」の形式で再構成しました。");
        });

        exportBtn.addActionListener(e -> exportToText());
        energyBtn.addActionListener(e -> addBasicEnergies());
        loadBtn.addActionListener(e -> loadDeck());
        saveBtn.addActionListener(e -> saveAction(false));
        saveAsBtn.addActionListener(e -> saveAction(true));
        clearBtn.addActionListener(e -> {
            if(JOptionPane.showConfirmDialog(this, "リストを空にしますか？") == 0) {
                model.setRowCount(0);
                updateCount();
            }
        });

        topPanel.add(menuBtn); 
        topPanel.add(loadBtn);
        topPanel.add(importBtn);
        topPanel.add(cleanBtn);
        topPanel.add(renumBtn);
        topPanel.add(exportBtn);
        topPanel.add(energyBtn);
        topPanel.add(saveBtn);
        topPanel.add(saveAsBtn);
        topPanel.add(clearBtn);
        topPanel.add(countLabel);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setBackground(new Color(235, 235, 235));
        bottomPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        
        JButton helpBtn = new JButton("❓ 操作マニュアル (Help)");
        helpBtn.setFont(new Font("Meiryo", Font.BOLD, 12));
        helpBtn.addActionListener(e -> showHelp());
        
        bottomPanel.add(new JLabel("Deck Editor System v1.1  "));
        bottomPanel.add(helpBtn);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        setSize(1400, 800);
        setLocationRelativeTo(null);
    }

    private void showHelp() {
        String helpMsg = 
            "【デッキエディタ 操作マニュアル】\n\n" +
            "■ 基本操作\n" +
            "・[カード名][カテゴリ]はセルを直接クリックして変更できます。\n" +
            "・[レギュ落]をチェックすると、その行の文字が赤く表示されます。\n" +
            "・右クリック：選択した行のカテゴリを一括変更したり、削除ができます。\n\n" +
            "■ 整形機能について\n" +
            "・[🧹 名前のみに整形]: 装飾を除去し純粋な名前だけにします。\n" +
            "・[🔢 フルネームに再構成]: カテゴリ・Noを付与した正式名称に更新します。\n\n" +
            "■ インポート・エクスポート\n" +
            "・[一括インポート]: 「カード名,枚数」の形式で流し込めます。\n" +
            "   ※区切り記号は カンマ(,) スラッシュ(/) ドット(.) コロン(:) 空白 に対応しています。\n" +
            "・[📋 テキスト出力]: 現在のデッキをカテゴリ別に集計して表示します。\n\n" +
            "■ 保存のルール\n" +
            "・デッキの合計枚数が「60枚」ちょうどの時のみ保存可能です。";

        JTextArea area = new JTextArea(helpMsg);
        area.setFont(new Font("Meiryo", Font.PLAIN, 13));
        area.setEditable(false);
        area.setOpaque(false);
        area.setMargin(new Insets(10, 10, 10, 10));

        JOptionPane.showMessageDialog(this, new JScrollPane(area), "ヘルプ・使い方", JOptionPane.QUESTION_MESSAGE);
    }

    private void cleanAllCardNames() {
        int count = 0;
        String catJoined = String.join("|", categories);
        Pattern pattern = Pattern.compile("^(" + catJoined + ")\\s+No\\.\\d+\\s+");

        for (int i = 0; i < model.getRowCount(); i++) {
            String original = (String) model.getValueAt(i, 2);
            Matcher matcher = pattern.matcher(original);
            if (matcher.find()) {
                String cleaned = matcher.replaceFirst("").trim();
                model.setValueAt(cleaned, i, 2);
                count++;
            }
        }
        JOptionPane.showMessageDialog(this, count + "件のカード名を純粋な名前に整形しました。");
    }

    private void rebuildFullNames() {
        String catJoined = String.join("|", categories);
        Pattern pattern = Pattern.compile("^(" + catJoined + ")\\s+No\\.\\d+\\s+");

        for (int i = 0; i < model.getRowCount(); i++) {
            int no = i + 1;
            String category = (String) model.getValueAt(i, 3);
            String currentName = (String) model.getValueAt(i, 2);
            
            Matcher matcher = pattern.matcher(currentName);
            String pureName = matcher.replaceFirst("").trim();

            String newFullName = String.format("%s No.%d %s", category, no, pureName);
            
            model.setValueAt(newFullName, i, 2); 
            model.setValueAt(no, i, 1);
        }
    }

    private void setupPopupMenu() {
        JPopupMenu popupMenu = new JPopupMenu();
        for (String cat : categories) {
            JMenuItem item = new JMenuItem("📁 全て '" + cat + "' に変更");
            item.addActionListener(e -> batchUpdateValue(3, cat));
            popupMenu.add(item);
        }
        popupMenu.addSeparator();
        JMenuItem regOn = new JMenuItem("🚩 選択した行を 'レギュ落' にする");
        regOn.addActionListener(e -> batchUpdateValue(0, true));
        popupMenu.add(regOn);
        JMenuItem regOff = new JMenuItem("🏳️ 選択した行の 'レギュ落' を解除");
        regOff.addActionListener(e -> batchUpdateValue(0, false));
        popupMenu.add(regOff);
        popupMenu.addSeparator();
        JMenuItem deleteItem = new JMenuItem("🗑️ 選択した行を削除");
        deleteItem.setForeground(Color.RED);
        deleteItem.addActionListener(e -> {
            int[] rows = table.getSelectedRows();
            for (int i = rows.length - 1; i >= 0; i--) model.removeRow(rows[i]);
            rebuildFullNames(); 
            updateCount();
        });
        popupMenu.add(deleteItem);

        table.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { if (e.isPopupTrigger()) showMenu(e); }
            public void mouseReleased(MouseEvent e) { if (e.isPopupTrigger()) showMenu(e); }
            private void showMenu(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row >= 0 && !table.isRowSelected(row)) table.setRowSelectionInterval(row, row);
                popupMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        });
    }

    private void batchUpdateValue(int col, Object value) {
        int[] rows = table.getSelectedRows();
        for (int row : rows) model.setValueAt(value, row, col);
    }

    private void exportToText() {
        if (model.getRowCount() == 0) return;
        Map<String, Map<String, Integer>> summary = new LinkedHashMap<>();
        for (String cat : categories) summary.put(cat, new LinkedHashMap<>());

        String catJoined = String.join("|", categories);
        Pattern pattern = Pattern.compile("^(" + catJoined + ")\\s+No\\.\\d+\\s+");

        for (int i = 0; i < model.getRowCount(); i++) {
            String fullName = (String) model.getValueAt(i, 2);
            String cat = (String) model.getValueAt(i, 3);
            String pureName = pattern.matcher(fullName).replaceFirst("").trim();
            
            if (summary.containsKey(cat)) {
                Map<String, Integer> cardCounts = summary.get(cat);
                cardCounts.put(pureName, cardCounts.getOrDefault(pureName, 0) + 1);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("【デッキレシピ】\n");
        for (String cat : categories) {
            Map<String, Integer> cards = summary.get(cat);
            if (!cards.isEmpty()) {
                sb.append("\n▼ ").append(cat).append("\n");
                for (Map.Entry<String, Integer> entry : cards.entrySet()) {
                    sb.append(entry.getKey()).append("  ×").append(entry.getValue()).append("\n");
                }
            }
        }

        JTextArea area = new JTextArea(sb.toString());
        area.setFont(new Font("Meiryo", Font.PLAIN, 14));
        area.setMargin(new java.awt.Insets(10,10,10,10));
        
        JButton copyBtn = new JButton("クリップボードにコピー");
        copyBtn.addActionListener(e -> {
            java.awt.datatransfer.StringSelection ss = new java.awt.datatransfer.StringSelection(sb.toString());
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
            JOptionPane.showMessageDialog(null, "コピーしました！");
        });

        JFrame frame = new JFrame("テキスト出力");
        frame.setSize(400, 700);
        frame.setLayout(new BorderLayout());
        frame.add(new JScrollPane(area), BorderLayout.CENTER);
        frame.add(copyBtn, BorderLayout.SOUTH);
        frame.setLocationRelativeTo(this);
        frame.setVisible(true);
    }

    private JButton createMenuButton() {
        JButton menuBtn = new JButton("🏠 メインメニュー");
        menuBtn.setBackground(new Color(230, 230, 250));
        menuBtn.addActionListener(e -> {
            this.dispose();
            for (java.awt.Frame frame : java.awt.Frame.getFrames()) {
                if (frame instanceof MainMenu) { frame.setVisible(true); frame.toFront(); return; }
            }
            new MainMenu().setVisible(true);
        });
        return menuBtn;
    }

    // ★アップデート箇所：複数区切り文字対応版のインポート
    private void importFromText() {
        JTextArea textArea = new JTextArea(15, 35);
        int res = JOptionPane.showConfirmDialog(this, new JScrollPane(textArea), "インポート", JOptionPane.OK_CANCEL_OPTION);
        if (res != JOptionPane.OK_OPTION) return;

        String[] lines = textArea.getText().split("\n");
        for (String line : lines) {
            String input = line.trim();
            if (input.isEmpty()) continue;

            String name = input;
            int count = 1;

            // 正規表現で「カンマ、スラッシュ、ドット、コロン（全半角）、空白」のいずれかで分割
            // 複数の記号が連続していても1つの区切りとして扱う（+）
            String[] parts = input.split("[、,/.:：\\s]+");
            
            if (parts.length >= 2) {
                name = parts[0].trim();
                try {
                    count = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException e) {
                    // 2つ目の要素が数字でない場合は、1枚として扱う
                    count = 1;
                }
            }

            if (name.isEmpty()) continue;

            for (int i = 0; i < count; i++) {
                if (model.getRowCount() >= 60) break;
                model.addRow(new Object[]{false, model.getRowCount() + 1, name, "たねポケモン"});
            }
        }
        updateCount();
    }

    private void addBasicEnergies() {
        JComboBox<String> combo = new JComboBox<>(energyTypes);
        String countStr = JOptionPane.showInputDialog(this, combo, "枚数", JOptionPane.QUESTION_MESSAGE);
        if (countStr != null) {
            try {
                int count = Integer.parseInt(countStr.trim());
                String type = (String) combo.getSelectedItem();
                for (int i = 0; i < count; i++) {
                    if (model.getRowCount() >= 60) break;
                    model.addRow(new Object[]{false, model.getRowCount() + 1, type, "基本エネルギー"});
                }
                updateCount();
            } catch (Exception e) {}
        }
    }

    private void updateCount() { countLabel.setText("枚数: " + model.getRowCount() + "/60"); }

    private void loadDeck() {
        try {
            List<String[]> list = db.getDeckList();
            if (list.isEmpty()) return;
            String[] options = list.stream().map(d -> d[0] + ":" + d[1]).toArray(String[]::new);
            String sel = (String) JOptionPane.showInputDialog(this, "読込", "選択", 1, null, options, options[0]);
            if (sel != null) {
                currentDeckId = Integer.parseInt(sel.split(":")[0]);
                List<Card> cards = db.fetchCards(currentDeckId);
                model.setRowCount(0);
                for (int i = 0; i < cards.size(); i++) model.addRow(new Object[]{false, i + 1, cards.get(i).name, cards.get(i).category});
                updateCount();
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void saveAction(boolean isSaveAs) {
        if (table.isEditing()) table.getCellEditor().stopCellEditing();
        if (model.getRowCount() != 60) {
            JOptionPane.showMessageDialog(this, "60枚にしてください。");
            return;
        }
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < 60; i++) cards.add(new Card(model.getValueAt(i, 2).toString(), model.getValueAt(i, 3).toString()));
        try {
            if (isSaveAs) {
                String name = JOptionPane.showInputDialog(this, "新規名:");
                if (name != null) currentDeckId = db.saveDeck(name, cards);
            } else if (currentDeckId != -1) {
                db.updateDeck(currentDeckId, cards);
                JOptionPane.showMessageDialog(this, "保存完了");
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    class CustomRowRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            Boolean out = (Boolean) table.getValueAt(row, 0);
            c.setForeground(out != null && out ? Color.RED : (isSelected ? Color.WHITE : Color.BLACK));
            return c;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DeckEditor().setVisible(true));
    }
}