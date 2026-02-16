package PCG_db;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

public class DeckBalanceAnalyzer extends JFrame {
    private DatabaseManager db = new DatabaseManager();
    private JPanel chartContainer;

    // --- カラーパレットの定義 ---
    private static final Map<String, Color> COLOR_MAP = new HashMap<>();
    static {
        COLOR_MAP.put("たねポケモン", new Color(255, 140, 0));   
        COLOR_MAP.put("1進化ポケモン", new Color(255, 180, 50));  
        COLOR_MAP.put("2進化ポケモン", new Color(255, 220, 150)); 
        COLOR_MAP.put("グッズ", new Color(50, 205, 50));        
        COLOR_MAP.put("ポケモンのどうぐ", new Color(34, 139, 34)); 
        COLOR_MAP.put("サポート", new Color(30, 144, 255));      
        COLOR_MAP.put("スタジアム", new Color(220, 20, 60));      
        COLOR_MAP.put("基本エネルギー", new Color(128, 128, 128)); 
        COLOR_MAP.put("特殊エネルギー", new Color(70, 70, 70));    
    }

    public DeckBalanceAnalyzer() {
        // 日本語フォントとテーマの設定
        StandardChartTheme theme = (StandardChartTheme)StandardChartTheme.createJFreeTheme();
        Font jFont = new Font("Meiryo", Font.PLAIN, 12);
        theme.setExtraLargeFont(new Font("Meiryo", Font.BOLD, 16));
        theme.setLargeFont(new Font("Meiryo", Font.PLAIN, 14));
        theme.setRegularFont(jFont);
        ChartFactory.setChartTheme(theme);

        setTitle("ポケカ・デッキバランス比較アナライザー");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1200, 800);
        setLayout(new BorderLayout());

        // --- 上部操作パネル ---
        JPanel topPanel = new JPanel();
        JButton loadBtn = new JButton("比較するデッキを選択 (最大4つ)");
        loadBtn.addActionListener(e -> selectAndAnalyze());
        
        topPanel.add(loadBtn);
        topPanel.add(createMenuButton()); 
        
        add(topPanel, BorderLayout.NORTH);

        chartContainer = new JPanel();
        chartContainer.setBackground(Color.LIGHT_GRAY); 
        add(chartContainer, BorderLayout.CENTER);
        setLocationRelativeTo(null);
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

    private void selectAndAnalyze() {
        try {
            List<String[]> allDecks = db.getDeckList();
            if (allDecks.isEmpty()) return;
            
            // 選択されたデッキの {ID, 名前} のペアをリストにする
            List<String[]> selectedDecks = askSelectedDecks(allDecks);
            if (selectedDecks.isEmpty()) return;
            
            displayCharts(selectedDecks);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private List<String[]> askSelectedDecks(List<String[]> allDecks) {
        List<String[]> selected = new ArrayList<>();
        String[] options = allDecks.stream().map(d -> d[0] + ":" + d[1]).toArray(String[]::new);
        
        for (int i = 0; i < 4; i++) {
            String sel = (String) JOptionPane.showInputDialog(this, (i+1) + "つ目のデッキを選択", "デッキ選択", 
                JOptionPane.PLAIN_MESSAGE, null, options, null);
            if (sel == null) break;
            
            // "ID:名前" の形式から分割して保存
            String[] parts = sel.split(":", 2);
            selected.add(parts);
        }
        return selected;
    }

    private void displayCharts(List<String[]> deckInfos) throws SQLException {
        chartContainer.removeAll();
        int count = deckInfos.size();
        if (count == 0) return;
        
        if (count <= 2) chartContainer.setLayout(new GridLayout(1, count, 5, 5));
        else chartContainer.setLayout(new GridLayout(2, 2, 5, 5));

        for (String[] info : deckInfos) {
            int id = Integer.parseInt(info[0]);
            String name = info[1];
            List<Card> cards = db.fetchCards(id);
            chartContainer.add(createDeckSummaryPanel(name, cards));
        }
        chartContainer.revalidate();
        chartContainer.repaint();
    }

    private JPanel createDeckSummaryPanel(String deckName, List<Card> cards) {
        JPanel p = new JPanel(new GridLayout(2, 1));
        
        // 境界線と余白の設定
        Border line = BorderFactory.createLineBorder(Color.GRAY, 1);
        Border margin = BorderFactory.createEmptyBorder(10, 10, 10, 10);
        p.setBorder(BorderFactory.createCompoundBorder(line, margin));
        p.setBackground(Color.WHITE);

        Map<String, Integer> stats = new HashMap<>();
        for (Card c : cards) {
            stats.put(c.category, stats.getOrDefault(c.category, 0) + 1);
        }

        DefaultCategoryDataset barDataset = new DefaultCategoryDataset();
        DefaultPieDataset pieDataset = new DefaultPieDataset();
        
        stats.forEach((cat, val) -> {
            barDataset.addValue(val, cat, ""); 
            pieDataset.setValue(cat, val);
        });

        // グラフタイトルに取得した「デッキ名」を反映
        JFreeChart barChart = ChartFactory.createBarChart(deckName, "カテゴリ", "枚数", barDataset, PlotOrientation.VERTICAL, true, true, false);
        CategoryPlot barPlot = (CategoryPlot) barChart.getPlot();
        BarRenderer renderer = (BarRenderer) barPlot.getRenderer();
        
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setShadowVisible(false);
        renderer.setDefaultItemLabelsVisible(true);

        for (int i = 0; i < barDataset.getRowCount(); i++) {
            String category = (String) barDataset.getRowKey(i);
            renderer.setSeriesPaint(i, COLOR_MAP.getOrDefault(category, Color.LIGHT_GRAY));
        }

        JFreeChart pieChart = ChartFactory.createPieChart("内訳割合", pieDataset, true, true, false);
        PiePlot plot = (PiePlot) pieChart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        
        for (Object key : pieDataset.getKeys()) {
            String category = (String) key;
            plot.setSectionPaint(category, COLOR_MAP.getOrDefault(category, Color.LIGHT_GRAY));
        }

        p.add(new ChartPanel(barChart));
        p.add(new ChartPanel(pieChart));
        return p;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DeckBalanceAnalyzer().setVisible(true));
    }
}