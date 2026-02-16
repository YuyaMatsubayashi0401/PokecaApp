package PCG_db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private final String url = "jdbc:postgresql://localhost:5432/pokeca_db";
    private final String user = "postgres";
    private final String password = "postgrestest"; // お使いの環境に合わせてパスワードを設定してください

    // --- 【既存機能】デッキ保存（新規） ---
    public int saveDeck(String deckName, List<Card> cards) throws SQLException {
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            conn.setAutoCommit(false);
            String sql = "INSERT INTO decks (deck_name) VALUES (?) RETURNING deck_id";
            int deckId = -1;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, deckName);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) deckId = rs.getInt(1);
            }
            insertCards(conn, deckId, cards);
            conn.commit();
            return deckId;
        }
    }

    // --- 【既存機能】デッキ上書き ---
    public void updateDeck(int deckId, List<Card> cards) throws SQLException {
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM deck_cards WHERE deck_id = ?")) {
                ps.setInt(1, deckId);
                ps.executeUpdate();
            }
            insertCards(conn, deckId, cards);
            conn.commit();
        }
    }

    // --- 【既存機能】デッキ削除 ---
    public void deleteDeck(int deckId) throws SQLException {
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM decks WHERE deck_id = ?")) {
                ps.setInt(1, deckId);
                ps.executeUpdate();
            }
        }
    }

    // --- 【既存機能】デッキ内カード読込 ---
    public List<Card> fetchCards(int deckId) throws SQLException {
        List<Card> cards = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            String sql = "SELECT card_name, category FROM deck_cards WHERE deck_id = ? ORDER BY slot_number";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, deckId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                cards.add(new Card(rs.getString("card_name"), rs.getString("category")));
            }
        }
        return cards;
    }

    // --- 【既存機能】共通挿入メソッド ---
    private void insertCards(Connection conn, int deckId, List<Card> cards) throws SQLException {
        String sql = "INSERT INTO deck_cards (deck_id, card_name, category, slot_number) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < cards.size(); i++) {
                Card c = cards.get(i);
                ps.setInt(1, deckId); ps.setString(2, c.name);
                ps.setString(3, c.category); ps.setInt(4, i + 1);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // --- 【既存機能】デッキ一覧取得 ---
    public List<String[]> getDeckList() throws SQLException {
        List<String[]> list = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            ResultSet rs = conn.createStatement().executeQuery("SELECT deck_id, deck_name FROM decks ORDER BY created_at DESC");
            while (rs.next()) {
                list.add(new String[]{String.valueOf(rs.getInt("deck_id")), rs.getString("deck_name")});
            }
        }
        return list;
    }

    // ============================================================
    // --- 【新規追加】ストレージ（在庫）管理用機能 ---
    // ============================================================

    // 全在庫データ取得
    public List<Object[]> fetchStorage() throws SQLException {
        List<Object[]> data = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            String sql = "SELECT card_id, card_name, category, quantity, reg_mark, tags FROM storage ORDER BY category, card_name";
            ResultSet rs = conn.createStatement().executeQuery(sql);
            while (rs.next()) {
                data.add(new Object[]{
                    rs.getInt("card_id"),
                    rs.getString("card_name"),
                    rs.getString("category"),
                    rs.getInt("quantity"),
                    rs.getString("reg_mark"),
                    rs.getString("tags")
                });
            }
        }
        return data;
    }

    // 在庫アイテムの保存（新規追加または更新）
    public void saveOrUpdateStorageItem(Object[] row) throws SQLException {
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            // ID（row[0]）が null の場合は新規 INSERT、ある場合は UPDATE
            if (row[0] == null) {
                String sql = "INSERT INTO storage (card_name, category, quantity, reg_mark, tags) VALUES (?,?,?,?,?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, (String)row[1]);
                    ps.setString(2, (String)row[2]);
                    ps.setInt(3, (Integer)row[3]);
                    ps.setString(4, (String)row[4]);
                    ps.setString(5, (String)row[5]);
                    ps.executeUpdate();
                }
            } else {
                String sql = "UPDATE storage SET card_name=?, category=?, quantity=?, reg_mark=?, tags=?, updated_at=CURRENT_TIMESTAMP WHERE card_id=?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, (String)row[1]);
                    ps.setString(2, (String)row[2]);
                    ps.setInt(3, (Integer)row[3]);
                    ps.setString(4, (String)row[4]);
                    ps.setString(5, (String)row[5]);
                    ps.setInt(6, (Integer)row[0]);
                    ps.executeUpdate();
                }
            }
        }
    }
}