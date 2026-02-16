package PCG_db;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

public class FieldSimulator extends JFrame {
    private PokemonSlot myBattle, oppBattle;
    private List<PokemonSlot> myBench = new ArrayList<>(), oppBench = new ArrayList<>();
    private JPanel board;
    private static List<PokemonData> templates = new ArrayList<>();

    public FieldSimulator() {
        setTitle("ポケカ盤面シミュレータ");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1200, 800); // ヘルプバー分、少し高さを拡張
        setLayout(new BorderLayout());

        if (templates.isEmpty()) {
            templates.add(new PokemonData("たねポケモン", 70));
            templates.add(new PokemonData("1進化ポケモン", 100));
            templates.add(new PokemonData("2進化ポケモン", 150));
            templates.add(new PokemonData("exポケモン", 250));
            templates.add(new PokemonData("メガexポケモン", 300));
        }

        board = new JPanel(new GridLayout(2, 1, 0, 10));
        board.setBackground(new Color(245, 245, 240)); 
        board.setBorder(new EmptyBorder(10, 10, 10, 10));

        setupFields();
        add(board, BorderLayout.CENTER);
        add(createControlPanel(), BorderLayout.EAST);
        add(createTopBar(), BorderLayout.NORTH);
        
        // ★右下のヘルプバーを追加
        add(createBottomBar(), BorderLayout.SOUTH);

        setLocationRelativeTo(null);
    }

    private JPanel createBottomBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bar.setBackground(new Color(230, 230, 230));
        
        JButton helpBtn = new JButton("❓ 操作マニュアル (Help)");
        helpBtn.setFont(new Font("Meiryo", Font.BOLD, 12));
        helpBtn.addActionListener(e -> showHelp());
        
        bar.add(new JLabel("PokeCa Simulation Engine v1.1  "));
        bar.add(helpBtn);
        return bar;
    }

    private void showHelp() {
        String helpMsg = "【ポケカ盤面シミュレータ 操作説明書】\n\n" +
            "■ ポケモンの配置と編集\n" +
            "・各スロットをクリックすると名前とHPを設定できます。\n" +
            "・[名前入力] -> Enter -> [HP入力] -> Enter の順で高速登録が可能です。\n" +
            "・HP入力欄は自動で全選択されるため、そのまま数字を打てば「0」を上書きできます。\n\n" +
            "・テンプレートに名前とHPを登録すると場から簡単に何度でも呼び出せます。\n\n" +
            "■ わざ・特性の使用\n" +
            "・[通常わざ]: 相手バトル場に即座にダメージを与えます。\n" +
            "・[ドラパルト]: バトル場へ200ダメージ後、ベンチへ60を自由に振り分けます。\n" +
            "・[アドレナブレイン]: 自分側のダメカンを相手側へ10～30移動させます。\n\n" +
            "■ 盤面の整理\n" +
            "・[画面反転]: 対戦相手の視点に切り替えます。\n" +
            "・[盤面リセット]: 全回復、または特定のポケモンを「まんたん」状態に戻せます。\n" +
            "・HPが0以下になるとスロットが自動的にグレーアウト（気絶状態）になります。\n\n" +
            "※テンプレートはアプリを閉じるまで保持されます。";

        JTextArea textArea = new JTextArea(helpMsg);
        textArea.setFont(new Font("Meiryo", Font.PLAIN, 13));
        textArea.setEditable(false);
        textArea.setOpaque(false);
        
        JOptionPane.showMessageDialog(this, new JScrollPane(textArea), "ヘルプ・使い方", JOptionPane.QUESTION_MESSAGE);
    }

    // --- 以下、既存のロジック ---

    private void setupFields() {
        board.removeAll();
        board.add(createSideWrapper("相手の場", true));
        board.add(createSideWrapper("自分の場", false));
        board.revalidate();
    }

    private JPanel createSideWrapper(String title, boolean isOpponent) {
        JPanel side = new JPanel(new BorderLayout());
        side.setOpaque(false);
        side.setBorder(new TitledBorder(new LineBorder(Color.LIGHT_GRAY), title));
        JPanel benchP = new JPanel(new GridLayout(1, 5, 5, 0));
        benchP.setOpaque(false);
        List<PokemonSlot> targetList = isOpponent ? oppBench : myBench;
        targetList.clear();
        for (int i = 0; i < 5; i++) {
            PokemonSlot s = new PokemonSlot("ベンチ", isOpponent);
            targetList.add(s); benchP.add(s);
        }
        PokemonSlot battleS = new PokemonSlot("バトル", isOpponent);
        if(isOpponent) oppBattle = battleS; else myBattle = battleS;
        JPanel battleW = new JPanel(new FlowLayout());
        battleW.setOpaque(false); battleW.add(battleS);
        if(isOpponent) { side.add(benchP, BorderLayout.NORTH); side.add(battleW, BorderLayout.CENTER); }
        else { side.add(battleW, BorderLayout.CENTER); side.add(benchP, BorderLayout.SOUTH); }
        return side;
    }

    private JToolBar createTopBar() {
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        JButton backBtn = new JButton("🏠 メインメニュー");
        backBtn.addActionListener(e -> { new MainMenu().setVisible(true); this.dispose(); });
        bar.add(backBtn);
        bar.add(Box.createHorizontalGlue());
        JButton flipBtn = new JButton("🔄 画面反転");
        flipBtn.addActionListener(e -> flipAllData());
        bar.add(flipBtn);
        return bar;
    }

    private JPanel createControlPanel() {
        JPanel side = new JPanel();
        side.setPreferredSize(new Dimension(240, 0));
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JButton regBtn = new JButton("✨ テンプレ登録");
        regBtn.setMaximumSize(new Dimension(220, 35));
        regBtn.addActionListener(e -> {
            String n = JOptionPane.showInputDialog(this, "ポケモン名:");
            String h = JOptionPane.showInputDialog(this, "最大HP:");
            if(n != null && h != null) {
                try { templates.add(new PokemonData(n, Integer.parseInt(h))); } catch(Exception ex){}
            }
        });
        
        JButton resetBtn = new JButton("♻️ 盤面リセット・回復");
        resetBtn.setMaximumSize(new Dimension(220, 35));
        resetBtn.setBackground(new Color(230, 255, 230));
        resetBtn.addActionListener(e -> executeResetMenu());

        side.add(regBtn);
        side.add(Box.createVerticalStrut(5));
        side.add(resetBtn);
        side.add(Box.createVerticalStrut(20));

        String[] labels = {"⚔️ 通常わざ", "🐉 ドラパルト", "💣 ボム(50)", "💣 ボム(130)", "🧠 アドレナブレイン"};
        for(String l : labels) {
            JButton b = new JButton(l);
            b.setMaximumSize(new Dimension(220, 40));
            if(l.contains("通常")) b.addActionListener(e -> {
                String v = JOptionPane.showInputDialog("ダメージ:", "100");
                if (v != null) try { oppBattle.addDamage(Integer.parseInt(v)); } catch(Exception ex){}
            });
            if(l.contains("ドラパ")) b.addActionListener(e -> executeDragapult());
            if(l.contains("50")) b.addActionListener(e -> executeCursedBomb(50));
            if(l.contains("130")) b.addActionListener(e -> executeCursedBomb(130));
            if(l.contains("アドレナ")) b.addActionListener(e -> executeMoveDamage());
            side.add(b); side.add(Box.createVerticalStrut(10));
        }
        return side;
    }

    private void executeResetMenu() {
        String[] options = {"場全体を全回復", "特定のポケモンを選んで回復", "キャンセル"};
        int res = JOptionPane.showOptionDialog(this, "盤面リセット", "リセット", 0, 1, null, options, options[0]);
        if (res == 0) {
            for (PokemonSlot s : getAllSlots()) { s.currentDmg = 0; s.updateDisplay(); }
        } else if (res == 1) {
            List<PokemonSlot> damaged = getAllSlots().stream().filter(s -> s.maxHp > 0 && s.currentDmg > 0).collect(Collectors.toList());
            if (damaged.isEmpty()) return;
            PokemonSlot sel = (PokemonSlot) JOptionPane.showInputDialog(this, "選択", "回復", 3, null, damaged.toArray(), damaged.get(0));
            if (sel != null) { sel.currentDmg = 0; sel.updateDisplay(); }
        }
    }

    private List<PokemonSlot> getAllSlots() {
        List<PokemonSlot> all = new ArrayList<>();
        all.add(myBattle); all.addAll(myBench);
        all.add(oppBattle); all.addAll(oppBench);
        return all;
    }

    private void flipAllData() {
        swapSlot(myBattle, oppBattle);
        for(int i=0; i<5; i++) swapSlot(myBench.get(i), oppBench.get(i));
    }

    private void swapSlot(PokemonSlot s1, PokemonSlot s2) {
        String tmpN = s1.pName; int tmpM = s1.maxHp; int tmpC = s1.currentDmg;
        s1.pName = s2.pName; s1.maxHp = s2.maxHp; s1.currentDmg = s2.currentDmg;
        s2.pName = tmpN; s2.maxHp = tmpM; s2.currentDmg = tmpC;
        s1.updateDisplay(); s2.updateDisplay();
    }

    private void executeDragapult() {
        oppBattle.addDamage(200);
        List<PokemonSlot> targets = oppBench.stream().filter(s -> s.maxHp > 0).collect(Collectors.toList());
        if (targets.isEmpty()) return;
        JPanel p = new JPanel(new GridLayout(0, 2, 5, 5));
        JTextField[] fs = new JTextField[targets.size()];
        FocusAdapter selector = new FocusAdapter() { @Override public void focusGained(FocusEvent e) { ((JTextField)e.getSource()).selectAll(); } };
        for (int i = 0; i < targets.size(); i++) {
            p.add(new JLabel(targets.get(i).pName + ":"));
            fs[i] = new JTextField("0", 5);
            fs[i].addFocusListener(selector);
            p.add(fs[i]);
        }
        if (JOptionPane.showConfirmDialog(this, p, "振分", 2) == 0) {
            try { for (int i = 0; i < fs.length; i++) targets.get(i).addDamage(Integer.parseInt(fs[i].getText()));
            } catch (Exception e) {}
        }
    }

    private void executeCursedBomb(int dmg) {
        List<PokemonSlot> active = getAllSlots().stream()
                .filter(s -> s.maxHp > 0 && (s == oppBattle || oppBench.contains(s)))
                .collect(Collectors.toList());
        if (active.isEmpty()) return;
        PokemonSlot sel = (PokemonSlot) JOptionPane.showInputDialog(this, "対象", "ボム", 3, null, active.toArray(), active.get(0));
        if (sel != null) sel.addDamage(dmg);
    }

    private void executeMoveDamage() {
        List<PokemonSlot> sources = getAllSlots().stream().filter(s -> s.currentDmg > 0).collect(Collectors.toList());
        List<PokemonSlot> targets = getAllSlots().stream().filter(s -> s.maxHp > 0).collect(Collectors.toList());
        if(sources.isEmpty()) return;
        PokemonSlot src = (PokemonSlot) JOptionPane.showInputDialog(this, "移動元", "アドレナ", 3, null, sources.toArray(), sources.get(0));
        if(src == null) return;
        String amtStr = (String) JOptionPane.showInputDialog(this, "量", "移動", 3, null, new String[]{"10","20","30"}, "10");
        if(amtStr == null) return;
        PokemonSlot dest = (PokemonSlot) JOptionPane.showInputDialog(this, "移動先", "アドレナ", 3, null, targets.toArray(), targets.get(0));
        if(dest != null) { src.addDamage(-Integer.parseInt(amtStr)); dest.addDamage(Integer.parseInt(amtStr)); }
    }

    static class PokemonData {
        String name; int hp;
        PokemonData(String n, int h) { name = n; hp = h; }
        @Override public String toString() { return name + " (HP" + hp + ")"; }
    }

    class PokemonSlot extends JPanel {
        String pName = ""; int maxHp = 0, currentDmg = 0;
        private JLabel infoLbl = new JLabel("---", JLabel.CENTER);
        private JPanel damageArea = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 2));
        private final Color originalBg;

        public PokemonSlot(String type, boolean isOpponent) {
            setPreferredSize(new Dimension(140, 145));
            originalBg = isOpponent ? new Color(255, 235, 235) : new Color(235, 245, 255);
            setBackground(originalBg);
            setBorder(new LineBorder(Color.LIGHT_GRAY, 1));
            setLayout(new BorderLayout());
            infoLbl.setFont(new Font("Meiryo", Font.BOLD, 10));
            add(infoLbl, BorderLayout.NORTH);
            damageArea.setOpaque(false);
            add(damageArea, BorderLayout.CENTER);
            addMouseListener(new MouseAdapter() { public void mouseClicked(MouseEvent e) { edit(); } });
        }

        public void addDamage(int v) { currentDmg += v; if(currentDmg < 0) currentDmg = 0; updateDisplay(); }

        void updateDisplay() {
            infoLbl.setText(pName + " [" + (maxHp - currentDmg) + "/" + maxHp + "]");
            damageArea.removeAll();
            int t = currentDmg;
            int[] vals = {100, 50, 10};
            Color[] cols = {new Color(220, 50, 50), new Color(218, 165, 32), new Color(255, 140, 0)};
            for(int i=0; i<3; i++) {
                int count = t / vals[i]; t %= vals[i];
                for(int j=0; j<count; j++) damageArea.add(createBadge(cols[i], String.valueOf(vals[i])));
            }
            if (maxHp > 0 && (maxHp - currentDmg) <= 0) setBackground(Color.DARK_GRAY);
            else setBackground(originalBg);
            revalidate(); repaint();
        }

        private JLabel createBadge(Color bg, String txt) {
            JLabel l = new JLabel(txt, JLabel.CENTER); l.setPreferredSize(new Dimension(28, 28));
            l.setOpaque(true); l.setBackground(bg); l.setForeground(Color.WHITE);
            l.setFont(new Font("Arial", Font.BOLD, 10)); l.setBorder(new LineBorder(Color.WHITE, 1));
            return l;
        }

        private void edit() {
            JTextField nF = new JTextField(pName);
            JTextField hF = new JTextField(String.valueOf(maxHp));
            FocusAdapter selector = new FocusAdapter() { @Override public void focusGained(FocusEvent e) { ((JTextField)e.getSource()).selectAll(); } };
            nF.addFocusListener(selector); hF.addFocusListener(selector);
            nF.addActionListener(e -> hF.requestFocus());
            DefaultComboBoxModel<Object> m = new DefaultComboBoxModel<>();
            m.addElement("--- テンプレ選択 ---");
            for(PokemonData d : templates) m.addElement(d);
            JComboBox<Object> cb = new JComboBox<>(m);
            cb.addActionListener(e -> {
                if(cb.getSelectedItem() instanceof PokemonData) {
                    PokemonData d = (PokemonData) cb.getSelectedItem();
                    nF.setText(d.name); hF.setText(String.valueOf(d.hp)); hF.requestFocus();
                }
            });
            Object[] msg = {"テンプレ:", cb, "名前:", nF, "最大HP:", hF};
            JOptionPane pane = new JOptionPane(msg, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
            JDialog dialog = pane.createDialog(null, "編集");
            dialog.addWindowListener(new WindowAdapter() { @Override public void windowOpened(WindowEvent e) { nF.requestFocusInWindow(); } });
            dialog.setVisible(true);
            if (pane.getValue() != null && (int)pane.getValue() == JOptionPane.OK_OPTION) {
                try { pName = nF.getText(); maxHp = Integer.parseInt(hF.getText()); updateDisplay(); } catch(Exception ex){}
            }
        }
        @Override public String toString() { 
            return pName.isEmpty() ? "未設定" : pName + " (HP:" + (maxHp-currentDmg) + "/" + maxHp + ")"; 
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FieldSimulator().setVisible(true));
    }
}