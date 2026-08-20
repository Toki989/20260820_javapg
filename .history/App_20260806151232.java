import com.sun.net.httpserver.HttpServer; // 小さなWebサーバーを作る機能を読み込みます
import java.io.IOException; // 入出力時のエラーを扱う機能を読み込みます
import java.io.OutputStream; // ブラウザへデータを送る機能を読み込みます
import java.net.InetSocketAddress; // 待ち受けるポート番号を指定する機能を読み込みます
import java.net.URLDecoder; // ：URL用に変換された文字を元に戻す機能を読み込みます
import java.nio.charset.StandardCharsets; // UTF-8を指定する機能を読み込みます
import java.util.ArrayList; // ：あとからTodoを追加できるListを作る機能を読み込みます
import java.util.List; // ：複数のTodoを順番に入れるListを使えるようにします

class Todo { // ★変更 Todo（1件分のやること）の設計図です
    private int id; // ★変更 何番のTodoかを保存します
    private String title; // ★変更 やることを保存します
    private boolean done; // ★変更 終わったかを保存します

    public Todo(int id, String title, boolean done) { // ★変更 Todoを1件作るときに値を受け取ります
        this.id = id; // ★変更 受け取った番号を保存します
        this.title = title; // ★変更 受け取ったやることを保存します
        this.done = done; // ★変更 受け取った終了状態を保存します
    } // ★変更 Todoを作る処理の終わりです

    public int getId() { // ★変更 idを読み出すメソッド（処理）です
        return id; // ★変更 idを返します
    } // ★変更 getIdの終わりです

    public String getTitle() { // ★変更 titleを読み出すメソッド（処理）です
        return title; // ★変更 titleを返します
    } // ★変更 getTitleの終わりです

    public boolean isDone() { // ★変更 doneを読み出すメソッド（処理）です
        return done; // ★変更 doneを返します
    } // ★変更 isDoneの終わりです

    public void setDone(boolean done) { // ★変更 doneを書き換えるメソッド（処理）です
        this.done = done; // ★変更 doneを新しい値にします
    } // ★変更 setDoneの終わりです
} // ★変更 Todoクラスの終わりです

public class App { // Appという名前のプログラムを定義します
    static List<Todo> todos = new ArrayList<>(); // ★変更 Todoを貯めるList（順番に保存する箱）です
    static int nextId = 1; // ★変更 次に使う1からの番号です

    public static void main(String[] args) throws IOException { // プログラムの開始地点です
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0); // 8080番ポートでWebサーバーを作ります
        todos.add(new Todo(nextId++, "牛乳を買う", false)); // ★変更 1件目のサンプルを追加します
        todos.add(new Todo(nextId++, "卵を買う", true)); // ★変更 2件目のサンプルを追加します

        server.createContext("/", exchange -> { // 「/」へのアクセスを受け取ったときの処理です
            String path = exchange.getRequestURI().getPath(); // アクセスされたパスを取り出します
            String message;

            // パスを if / else if / else で比べます
            if (path.equals("/add") && exchange.getRequestMethod().equals("POST")) { // ：フォームから「/add」へ送られたPOSTかを調べます
                String formData = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8); // ：送られたフォームの内容をUTF-8で読み取ります
                String encodedTodo = formData.startsWith("todo=") ? formData.substring("todo=".length()) : ""; // ：「todo=」より後ろの値を取り出します
                String todo = URLDecoder.decode(encodedTodo, StandardCharsets.UTF_8); // ：日本語などのURL用表記を元の文字に戻します
                if (!todo.isBlank()) { // ：空の入力ではないかを調べます
                    todos.add(new Todo(nextId++, todo, false)); // ★変更 番号を付け、未完了のTodoを1件作って追加します
                } // ：空の入力を追加しないための確認を終えます
                exchange.getResponseHeaders().set("Location", "/"); // ：追加後の移動先が「/」だとブラウザへ伝えます
                exchange.sendResponseHeaders(303, -1); // ：303で「/」へ移動するようブラウザへ伝えます
                exchange.close(); // ：この応答を閉じます
                return; // ：通常のページを返す処理へ進まないよう、ここで終えます
            } else if (path.equals("/hello")) {
                String query = exchange.getRequestURI().getRawQuery(); // ：URLの「?」より後ろの部分を取り出します
                System.out.println("query = " + query);
                String name; // 修正：挨拶に使う名前を入れる変数を用意します
                if (query == null) { // 修正：URLに「?name=」がなく、queryに値がないかを調べます
                    name = "ゲスト"; // 修正：値がない場合は「ゲスト」を使います
                } else {
                    String encodedName = query.substring("name=".length()); // ：「name=」より後ろの、変換された名前を切り出します
                    name = URLDecoder.decode(encodedName, StandardCharsets.UTF_8); // ：%ではじまる表記を元の文字に戻します
                }
                message = "こんにちは、" + name + "さん！"; // ：取り出した名前を応答に入れます
            } else if (path.equals("/bye")) {
                message = "さようなら！";
            } else if (path.equals("/good")) {
                message = "元気！";
            } else if (path.equals("/")) { // 変更：「/」へのアクセスかを調べます
                String html = "<form method='post' action='/add'>" // ：Todoを「/add」へ送るフォームを作ります
                        + "<input name='todo'>" // ：todoという名前の入力欄を作ります
                        + "<button type='submit'>追加</button>" // ：入力内容を送るボタンを作ります
                        + "</form><ul>"; // ：フォームを閉じ、箇条書きを始めます
                for (Todo todo : todos) { // ★変更 Todoを1件ずつ順番に取り出します
                    html += "<li>" + todo.getTitle() + (todo.isDone() ? " ✔" : "") + "</li>"; // ★変更
                                                                                              // titleを表示し、完了済みだけにチェックを付けます
                } // ：すべてのTodoを取り出し終えます
                html += "</ul>"; // ：箇条書き全体の終わりをつなげます
                message = html; // ：組み立てたHTMLを応答に使います
            } else {
                message = "ページが見つかりません";
            }

            byte[] response = message.getBytes(StandardCharsets.UTF_8); // 文字をUTF-8のデータに変換します
            if (path.equals("/")) { // 変更：「/」の応答かを調べます
                exchange.getResponseHeaders().set("Content-Type", "text/html ; charset=UTF-8"); // ：HTMLをUTF-8で返すことを伝えます
            } else { // 変更：「/」以外の場合です
                exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8"); // 今までどおり通常の文字をUTF-8で返します
            } // ：Content-Typeの選び分けを終えます
            exchange.sendResponseHeaders(200, response.length); // 正常終了を表す200とデータの長さを送ります

            try (OutputStream output = exchange.getResponseBody()) { // ブラウザへ送るための通り道を開きます
                output.write(response); // ブラウザへ文字のデータを送ります
            } // 送信の通り道を自動で閉じます
        }); // 「/」への処理の登録を終えます

        server.start(); // Webサーバーの待ち受けを開始します
        System.out.println("サーバー起動: http://localhost:8080 （止めるときは Ctrl+C）"); // 起動したことをターミナルへ表示します
    } // mainの定義を終えます
} // Appの定義を終えます
