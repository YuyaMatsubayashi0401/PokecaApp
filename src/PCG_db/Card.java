package PCG_db;

/**
 * カード1枚の情報を保持するクラス
 * どこからでも参照できるように public にします
 */
public class Card {
    public String name;     // カード名
    public String category; // カテゴリ（たね、グッズなど）

    public Card(String name, String category) {
        this.name = name;
        this.category = category;
    }

    // デバッグや表示に便利なように toString をオーバーライドしておくと便利です
    @Override
    public String toString() {
        return String.format("[%s] %s", category, name);
    }
}