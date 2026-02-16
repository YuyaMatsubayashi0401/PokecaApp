package PCG_db;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

public class InventoryManager extends JFrame {
    private DatabaseManager db = new DatabaseManager();
    private JTable table;
    private DefaultTableModel model;
    private TableRowSorter<DefaultTableModel> sorter;

    private final String[] categories = {"たねポケモン", "1進化ポケモン", "2進化ポケモン", "グッズ", "ポケモンのどうぐ", "サポート", "スタジアム", "基本エネルギー", "特殊エネルギー"};

    public InventoryManager() {
        setTitle("ポケカ・ストレージ管理");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // 1. テーブルモデルの設定
        String[] columns = {"ID", "カード名", "カテゴリ", "枚数", "レギュ", "タグ"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public Class<?> getColumnClass(int col) {
                return (col == 0 || col == 3) ? Integer.class : String.class;
            }
            @Override
            public boolean isCellEditable(int row, int col) {
                return col != 0;
            }
        };

        table = new JTable(model);
        table.setRowHeight(30);
        
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        // カテゴリ列をComboBoxに
        table.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(new JComboBox<>(categories)));

        // 2. 検索・フィルタ用パネル
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.setBorder(BorderFactory.createTitledBorder("検索・フィルタ"));

        JTextField searchField = new JTextField(15);
        JComboBox<String> categoryFilter = new JComboBox<>();
        categoryFilter.addItem("すべてのカテゴリ");
        for (String cat : categories) categoryFilter.addItem(cat);

        Runnable filterAction = () -> {
            String text = searchField.getText();
            String cat = (String) categoryFilter.getSelectedItem();
            
            List<RowFilter<Object, Object>> filters = new ArrayList<>();
            if (text.trim().length() > 0) {
                filters.add(RowFilter.regexFilter("(?i)" + text, 1)); 
            }
            if (!cat.equals("すべてのカテゴリ")) {
                filters.add(RowFilter.regexFilter("^" + cat + "$", 2));
            }
            
            if (filters.isEmpty()) {
                sorter.setRowFilter(null);
            } else {
                sorter.setRowFilter(RowFilter.andFilter(filters));
            }
        };

        searchField.addCaretListener(e -> filterAction.run());
        categoryFilter.addActionListener(e -> filterAction.run());

        filterPanel.add(new JLabel("カード名:"));
        filterPanel.add(searchField);
        filterPanel.add(new JLabel(" カテゴリ:"));
        filterPanel.add(categoryFilter);

        // 3. 操作パネル（下部）
        JPanel btnPanel = new JPanel();
        JButton addBtn = new JButton("新規行追加");
        JButton bulkBtn = new JButton("テキスト一括入力");
        JButton saveBtn = new JButton("変更をDBへ保存");
        JButton plusBtn = new JButton("枚数＋1");
        JButton minusBtn = new JButton("枚数－1");
        
        addBtn.addActionListener(e -> {
            model.addRow(new Object[]{null, "新規カード", "たねポケモン", 0, "G", ""});
        });

        bulkBtn.addActionListener(e -> openImportDialog());
        
        saveBtn.addActionListener(e -> {
            try {
                if (table.isEditing()) table.getCellEditor().stopCellEditing();
                for (int i = 0; i < model.getRowCount(); i++) {
                    Object[] row = new Object[6];
                    for (int j = 0; j < 6; j++) row[j] = model.getValueAt(i, j);
                    db.saveOrUpdateStorageItem(row);
                }
                JOptionPane.showMessageDialog(this, "保存完了！");
                loadData();
            } catch (SQLException ex) { ex.printStackTrace(); }
        });

        plusBtn.addActionListener(e -> updateQty(1));
        minusBtn.addActionListener(e -> updateQty(-1));

        btnPanel.add(createMenuButton());
        btnPanel.add(addBtn);
        btnPanel.add(bulkBtn);
        btnPanel.add(plusBtn);
        btnPanel.add(minusBtn);
        btnPanel.add(saveBtn);

        // --- ★下部バー（右下にヘルプを追加） ---
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.add(btnPanel, BorderLayout.CENTER);
        
        JPanel helpWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton helpBtn = new JButton("❓ 操作説明");
        helpBtn.setFont(new Font("Meiryo", Font.BOLD, 12));
        helpBtn.addActionListener(e -> showHelp());
        helpWrap.add(helpBtn);
        bottomBar.add(helpWrap, BorderLayout.EAST);

        // 配置
        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.add(filterPanel, BorderLayout.CENTER);
        
        add(topContainer, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottomBar, BorderLayout.SOUTH);

        loadData();
        setSize(1100, 750);
        setLocationRelativeTo(null);
    }

    private void showHelp() {
        String msg = "【ストレージ管理 操作マニュアル】\n\n" +
                     "■ データの閲覧・検索\n" +
                     "・[検索窓]に名前を入れると、リアルタイムで絞り込みます。\n" +
                     "・[カテゴリ]を選択すると、特定の種別のみ表示します。\n\n" +
                     "■ 編集と追加\n" +
                     "・表の各セル（名前、枚数、タグ等）は直接書き換え可能です。\n" +
                     "・[枚数＋1 / －1]：選択中の行の枚数を素早く変更します。\n" +
                     "・[テキスト一括入力]：リストをまとめて投入できます。\n" +
                     "   書式例：『ピカチュウ,4』『ナンジャモ,2』\n" +
                     "   ※カンマなしの場合は1枚として扱われます。\n\n" +
                     "■ 保存の重要性\n" +
                     "・画面上で行った追加や変更は、[変更をDBへ保存]ボタンを\n" +
                     "   押すまで確定されません。作業後は必ず保存してください。\n\n" +
                     "■ 表のソート\n" +
                     "・ヘッダー部分（IDや枚数など）をクリックすると昇順/降順に並び替わります。";

        JTextArea area = new JTextArea(msg);
        area.setFont(new Font("Meiryo", Font.PLAIN, 13));
        area.setEditable(false);
        area.setOpaque(false);
        area.setMargin(new Insets(10, 10, 10, 10));
        JOptionPane.showMessageDialog(this, new JScrollPane(area), "ストレージ管理ヘルプ", JOptionPane.INFORMATION_MESSAGE);
    }

    private void openImportDialog() {
        JTextArea textArea = new JTextArea(15, 40);
        textArea.setToolTipText("例：\nカルボウ,4\nナンジャモ\n(1行に1カード)");
        JScrollPane scrollPane = new JScrollPane(textArea);
        
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("カード名,枚数 の形式で入力してください（枚数なしは1枚）:"), BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(this, panel, 
                "一括インポート", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String input = textArea.getText().trim();
            if (input.isEmpty()) return;

            String[] lines = input.split("\\n");
            int count = 0;
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String name;
                int qty = 1;

                if (line.contains(",") || line.contains("、")) {
                    String[] parts = line.split("[,、]");
                    name = parts[0].trim();
                    if (parts.length > 1) {
                        try { qty = Integer.parseInt(parts[1].trim()); } catch (NumberFormatException e) { qty = 1; }
                    }
                } else {
                    name = line;
                }
                model.addRow(new Object[]{null, name, "たねポケモン", qty, "G", ""});
                count++;
            }
            JOptionPane.showMessageDialog(this, count + " 件をテーブルに追加しました。「保存」を押すとDBに反映されます。");
        }
    }

    private void loadData() {
        try {
            model.setRowCount(0);
            List<Object[]> data = db.fetchStorage();
            for (Object[] row : data) model.addRow(row);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void updateQty(int amount) {
        int viewRow = table.getSelectedRow();
        if (viewRow != -1) {
            int modelRow = table.convertRowIndexToModel(viewRow);
            int current = (int) model.getValueAt(modelRow, 3);
            model.setValueAt(Math.max(0, current + amount), modelRow, 3);
        }
    }

    private JButton createMenuButton() {
        JButton menuBtn = new JButton("🏠 メインメニュー");
        menuBtn.setBackground(new java.awt.Color(230, 230, 250));
        menuBtn.addActionListener(e -> {
            this.dispose();
            for (java.awt.Frame frame : java.awt.Frame.getFrames()) {
                if (frame.getClass().getSimpleName().equals("MainMenu")) {
                    frame.setVisible(true);
                    frame.toFront();
                    return;
                }
            }
        });
        return menuBtn;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new InventoryManager().setVisible(true));
    }
}